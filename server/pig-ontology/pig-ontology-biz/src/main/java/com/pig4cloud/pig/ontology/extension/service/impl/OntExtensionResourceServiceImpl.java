/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.extension.dto.ExtensionResourceAssociateDTO;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionResource;
import com.pig4cloud.pig.ontology.extension.mapper.OntExtensionModuleMapper;
import com.pig4cloud.pig.ontology.extension.mapper.OntExtensionResourceMapper;
import com.pig4cloud.pig.ontology.extension.service.OntExtensionResourceService;
import com.pig4cloud.pig.ontology.extension.validator.ExtensionValidationReport;
import com.pig4cloud.pig.ontology.extension.validator.ExtensionValidationResult;
import com.pig4cloud.pig.ontology.extension.validator.ExtensionValidator;
import com.pig4cloud.pig.ontology.extension.vo.ExtensionResourceVO;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 扩展资源关联服务实现。
 *
 * @author youming
 */
@Service
@RequiredArgsConstructor
public class OntExtensionResourceServiceImpl extends ServiceImpl<OntExtensionResourceMapper, OntExtensionResource>
		implements OntExtensionResourceService {

	private final OntExtensionModuleMapper moduleMapper;

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	private final OntAxiomRuleMapper axiomRuleMapper;

	private final OntUnitMapper unitMapper;

	private final ExtensionValidator validator;

	@Override
	public List<ExtensionResourceVO> listByModule(Long moduleId, String resourceType) {
		List<OntExtensionResource> resources = this.list(Wrappers.<OntExtensionResource>lambdaQuery()
			.eq(OntExtensionResource::getModuleId, moduleId)
			.eq(OntExtensionResource::getDelFlag, "0")
			.eq(resourceType != null, OntExtensionResource::getResourceType, resourceType)
			.orderByAsc(OntExtensionResource::getResourceType)
			.orderByAsc(OntExtensionResource::getId));

		return resources.stream().map(r -> {
			ExtensionResourceVO vo = new ExtensionResourceVO();
			vo.setId(r.getId());
			vo.setModuleId(r.getModuleId());
			vo.setResourceType(r.getResourceType());
			vo.setResourceId(r.getResourceId());
			vo.setResourceIri(r.getResourceIri());
			vo.setResourceName(r.getResourceName());
			vo.setCreateTime(r.getCreateTime());
			return vo;
		}).toList();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<AssociateResult> associateResources(Long moduleId, ExtensionResourceAssociateDTO request) {
		OntExtensionModule module = moduleMapper.selectById(moduleId);
		if (module == null) {
			return R.failed("扩展模块不存在");
		}

		int associatedCount = 0;
		int skippedCount = 0;
		List<ExtensionValidationResult> allResults = new ArrayList<>();

		for (ExtensionResourceAssociateDTO.ResourceItem item : request.getResources()) {
			OntExtensionResource resource = new OntExtensionResource();
			resource.setModuleId(moduleId);
			resource.setResourceType(item.getResourceType());
			resource.setResourceId(item.getResourceId());

			// 快照IRI和名称
			snapshotResourceInfo(resource);
			if (resource.getResourceIri() == null && resource.getResourceName() == null) {
				skippedCount++;
				continue;
			}

			// 检查是否已关联
			long exists = this.count(Wrappers.<OntExtensionResource>lambdaQuery()
				.eq(OntExtensionResource::getModuleId, moduleId)
				.eq(OntExtensionResource::getResourceType, item.getResourceType())
				.eq(OntExtensionResource::getResourceId, item.getResourceId())
				.eq(OntExtensionResource::getDelFlag, "0"));
			if (exists > 0) {
				skippedCount++;
				continue;
			}

			// 执行合法性校验
			ExtensionValidationReport report = validator.validateResource(module, resource);
			allResults.addAll(report.getResults());

			// VIOLATION 级违规阻断保存（R1/R2/R5），WARNING 级仅提示不阻断（R3/R4）
			if (!report.getConforms()) {
				skippedCount++;
				continue;
			}

			// 保存关联
			resource.setDelFlag("0");
			this.save(resource);
			associatedCount++;
		}

		// 聚合校验报告
		boolean conforms = allResults.stream()
			.noneMatch(r -> "VIOLATION".equals(r.getSeverity()) && !r.getPassed());
		long violationCount = allResults.stream()
			.filter(r -> "VIOLATION".equals(r.getSeverity()) && !r.getPassed())
			.count();
		long warningCount = allResults.stream()
			.filter(r -> "WARNING".equals(r.getSeverity()) && !r.getPassed())
			.count();

		ExtensionValidationReport aggregatedReport = new ExtensionValidationReport();
		aggregatedReport.setConforms(conforms);
		aggregatedReport.setViolationCount(violationCount);
		aggregatedReport.setWarningCount(warningCount);
		aggregatedReport.setTriggeredAt(LocalDateTime.now());
		aggregatedReport.setResults(allResults);

		AssociateResult result = new AssociateResult();
		result.setAssociatedCount(associatedCount);
		result.setSkippedCount(skippedCount);
		result.setValidationReport(aggregatedReport);
		return R.ok(result);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> removeResource(Long moduleId, Long resourceId, String resourceType) {
		boolean updated = this.update(Wrappers.<OntExtensionResource>lambdaUpdate()
			.eq(OntExtensionResource::getModuleId, moduleId)
			.eq(OntExtensionResource::getResourceId, resourceId)
			.eq(OntExtensionResource::getResourceType, resourceType)
			.eq(OntExtensionResource::getDelFlag, "0")
			.set(OntExtensionResource::getDelFlag, "1"));
		if (!updated) {
			return R.failed("资源关联记录不存在");
		}
		return R.ok(true);
	}

	/**
	 * 从各资源表查询IRI和名称，快照到关联记录。
	 */
	private void snapshotResourceInfo(OntExtensionResource resource) {
		switch (resource.getResourceType()) {
			case "ENTITY_TYPE":
				OntEntityType type = entityTypeMapper.selectById(resource.getResourceId());
				if (type != null) {
					resource.setResourceIri(type.getIri());
					resource.setResourceName(type.getName());
				}
				break;
			case "DATA_PROPERTY":
				OntDataProperty dp = dataPropertyMapper.selectById(resource.getResourceId());
				if (dp != null) {
					resource.setResourceIri(dp.getIri());
					resource.setResourceName(dp.getName());
				}
				break;
			case "OBJECT_PROPERTY":
				OntObjectProperty op = objectPropertyMapper.selectById(resource.getResourceId());
				if (op != null) {
					resource.setResourceIri(op.getIri());
					resource.setResourceName(op.getName());
				}
				break;
			case "AXIOM_RULE":
				OntAxiomRule rule = axiomRuleMapper.selectById(resource.getResourceId());
				if (rule != null) {
					resource.setResourceIri(rule.getRuleCode());
					resource.setResourceName(rule.getName());
				}
				break;
			case "UNIT":
				OntUnit unit = unitMapper.selectById(resource.getResourceId());
				if (unit != null) {
					resource.setResourceIri(unit.getUnitCode());
					resource.setResourceName(unit.getUnitName());
				}
				break;
			default:
				break;
		}
	}

}
