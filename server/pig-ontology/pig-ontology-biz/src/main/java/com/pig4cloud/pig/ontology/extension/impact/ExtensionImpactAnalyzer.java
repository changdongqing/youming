/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.impact;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionResource;
import com.pig4cloud.pig.ontology.extension.mapper.OntExtensionModuleMapper;
import com.pig4cloud.pig.ontology.extension.mapper.OntExtensionResourceMapper;
import com.pig4cloud.pig.ontology.extension.vo.ExtensionImpactSummaryVO;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 扩展变更影响分析器。
 *
 * <p>跨表统计扩展模块关联资源的影响范围，
 * 用于删除/停用前的二次确认。
 *
 * @author youming
 */
@Component
@RequiredArgsConstructor
public class ExtensionImpactAnalyzer {

	private final OntExtensionModuleMapper moduleMapper;

	private final OntExtensionResourceMapper resourceMapper;

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntEntityInstanceMapper entityInstanceMapper;

	/**
	 * 分析扩展模块的变更影响。
	 * @param moduleId 模块ID
	 * @return 影响摘要
	 */
	public ExtensionImpactSummaryVO analyze(Long moduleId) {
		OntExtensionModule module = moduleMapper.selectById(moduleId);
		if (module == null) {
			return null;
		}

		ExtensionImpactSummaryVO summary = new ExtensionImpactSummaryVO();
		summary.setModuleId(moduleId);
		summary.setModuleCode(module.getModuleCode());

		// 1. 资源摘要
		List<OntExtensionResource> resources = resourceMapper.selectList(
			Wrappers.<OntExtensionResource>lambdaQuery()
				.eq(OntExtensionResource::getModuleId, moduleId)
				.eq(OntExtensionResource::getDelFlag, "0"));

		Map<String, Long> countByType = resources.stream()
			.collect(Collectors.groupingBy(
				OntExtensionResource::getResourceType,
				Collectors.counting()));

		ExtensionImpactSummaryVO.ResourceSummaryVO resourceSummary =
				new ExtensionImpactSummaryVO.ResourceSummaryVO();
		resourceSummary.setEntityType(countByType.getOrDefault("ENTITY_TYPE", 0L).intValue());
		resourceSummary.setDataProperty(countByType.getOrDefault("DATA_PROPERTY", 0L).intValue());
		resourceSummary.setObjectProperty(countByType.getOrDefault("OBJECT_PROPERTY", 0L).intValue());
		resourceSummary.setAxiomRule(countByType.getOrDefault("AXIOM_RULE", 0L).intValue());
		resourceSummary.setUnit(countByType.getOrDefault("UNIT", 0L).intValue());
		summary.setResourceSummary(resourceSummary);

		// 2. 实例影响（查询关联实体类型下的实例数）
		List<Long> entityTypeIds = resources.stream()
			.filter(r -> "ENTITY_TYPE".equals(r.getResourceType()))
			.map(OntExtensionResource::getResourceId)
			.toList();

		if (!entityTypeIds.isEmpty()) {
			List<OntEntityType> entityTypes = entityTypeMapper.selectBatchIds(entityTypeIds);
			List<ExtensionImpactSummaryVO.InstanceCountVO> instanceCounts = new ArrayList<>();
			int totalInstances = 0;

			for (OntEntityType type : entityTypes) {
				// OntEntityInstance 用 rdfTypeId(Long) 引用实体类型ID
				long count = entityInstanceMapper.selectCount(Wrappers.<OntEntityInstance>lambdaQuery()
					.eq(OntEntityInstance::getRdfTypeId, type.getId())
					.eq(OntEntityInstance::getDelFlag, "0"));
				if (count > 0) {
					ExtensionImpactSummaryVO.InstanceCountVO ic =
							new ExtensionImpactSummaryVO.InstanceCountVO();
					ic.setEntityTypeIri(type.getIri());
					ic.setCount((int) count);
					instanceCounts.add(ic);
					totalInstances += (int) count;
				}
			}

			ExtensionImpactSummaryVO.InstanceImpactVO instanceImpact =
					new ExtensionImpactSummaryVO.InstanceImpactVO();
			instanceImpact.setTotalInstances(totalInstances);
			instanceImpact.setInstancesByType(instanceCounts);
			summary.setInstanceImpact(instanceImpact);
		}
		else {
			ExtensionImpactSummaryVO.InstanceImpactVO instanceImpact =
					new ExtensionImpactSummaryVO.InstanceImpactVO();
			instanceImpact.setTotalInstances(0);
			instanceImpact.setInstancesByType(List.of());
			summary.setInstanceImpact(instanceImpact);
		}

		// 3. 导出影响（首期无活跃导出任务）
		ExtensionImpactSummaryVO.ExportImpactVO exportImpact =
				new ExtensionImpactSummaryVO.ExportImpactVO();
		exportImpact.setActiveExportTasks(0);
		exportImpact.setLastExportLogId(null);
		summary.setExportImpact(exportImpact);

		return summary;
	}

}
