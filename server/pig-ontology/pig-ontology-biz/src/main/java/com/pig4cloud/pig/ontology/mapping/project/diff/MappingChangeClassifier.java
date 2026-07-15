/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.diff;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.entity.OntFieldMapping;
import com.pig4cloud.pig.ontology.mapping.entity.OntRelationMapping;
import com.pig4cloud.pig.ontology.mapping.mapper.OntEntityMappingMapper;
import com.pig4cloud.pig.ontology.mapping.mapper.OntFieldMappingMapper;
import com.pig4cloud.pig.ontology.mapping.mapper.OntRelationMappingMapper;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 映射配置变更分类器（18-03 §12）。
 * <p>
 * 分类规则：
 * <table>
 *   <tr><th>变化</th><th>分类</th></tr>
 *   <tr><td>描述/排序变化</td><td>PATCH</td></tr>
 *   <tr><td>新增非必填字段映射</td><td>MINOR</td></tr>
 *   <tr><td>新增关系且missingTarget=PENDING</td><td>MINOR</td></tr>
 *   <tr><td>修改转换器、目标属性</td><td>MAJOR</td></tr>
 *   <tr><td>修改主键、IRI模板、目标类型</td><td>MAJOR_HIGH_RISK</td></tr>
 *   <tr><td>修改删除策略为SOFT_DELETE</td><td>MAJOR_HIGH_RISK</td></tr>
 *   <tr><td>修改数据源或源对象</td><td>MAJOR_HIGH_RISK</td></tr>
 * </table>
 * V1 简化：比较工程级、版本级、实体映射、字段映射和关系映射子表。
 *
 * @author youming
 */
@Component
@RequiredArgsConstructor
public class MappingChangeClassifier {

	private final OntEntityMappingMapper entityMappingMapper;

	private final OntFieldMappingMapper fieldMappingMapper;

	private final OntRelationMappingMapper relationMappingMapper;

	/** 变更分类：PATCH */
	public static final String PATCH = "PATCH";

	/** 变更分类：MINOR */
	public static final String MINOR = "MINOR";

	/** 变更分类：MAJOR */
	public static final String MAJOR = "MAJOR";

	/** 变更分类：MAJOR_HIGH_RISK */
	public static final String MAJOR_HIGH_RISK = "MAJOR_HIGH_RISK";

	/**
	 * 比较两个版本（含工程信息），返回变更项列表和整体分类。
	 * @param baseProject 基准工程
	 * @param baseVersion 基准版本
	 * @param compareProject 对比工程
	 * @param compareVersion 对比版本
	 * @return 变更项列表
	 */
	public List<ChangeRecord> classify(OntMappingProject baseProject, OntMappingVersion baseVersion,
			OntMappingProject compareProject, OntMappingVersion compareVersion) {
		List<ChangeRecord> changes = new ArrayList<>();

		// 工程级字段比较
		compareField(changes, "project.mappingName", baseProject.getMappingName(),
				compareProject.getMappingName(), PATCH);
		compareField(changes, "project.description", baseProject.getDescription(),
				compareProject.getDescription(), PATCH);
		compareField(changes, "project.scheduleCron", baseProject.getScheduleCron(),
				compareProject.getScheduleCron(), PATCH);
		compareField(changes, "project.securityLevelCode", baseProject.getSecurityLevelCode(),
				compareProject.getSecurityLevelCode(), MAJOR);

		// 版本级字段比较
		compareField(changes, "version.ontologyVersionConstraint", baseVersion.getOntologyVersionConstraint(),
				compareVersion.getOntologyVersionConstraint(), MAJOR_HIGH_RISK);
		compareField(changes, "version.releaseNotes", baseVersion.getReleaseNotes(),
				compareVersion.getReleaseNotes(), PATCH);

		// 实体映射和字段映射子表比较
		classifyEntityMappings(changes, baseVersion.getId(), compareVersion.getId());

		// 关系映射子表比较
		classifyRelationMappings(changes, baseVersion.getId(), compareVersion.getId());

		return changes;
	}

	/**
	 * 比较两个版本下的实体映射和字段映射子表。
	 */
	private void classifyEntityMappings(List<ChangeRecord> changes, Long baseVersionId, Long compareVersionId) {
		List<OntEntityMapping> baseMappings = entityMappingMapper.selectList(
				Wrappers.<OntEntityMapping>lambdaQuery()
						.eq(OntEntityMapping::getMappingVersionId, baseVersionId)
						.eq(OntEntityMapping::getDelFlag, "0"));

		List<OntEntityMapping> compareMappings = entityMappingMapper.selectList(
				Wrappers.<OntEntityMapping>lambdaQuery()
						.eq(OntEntityMapping::getMappingVersionId, compareVersionId)
						.eq(OntEntityMapping::getDelFlag, "0"));

		Map<String, OntEntityMapping> baseMap = new HashMap<>();
		for (OntEntityMapping em : baseMappings) {
			baseMap.put(em.getMappingCode(), em);
		}

		Map<String, OntEntityMapping> compareMap = new HashMap<>();
		for (OntEntityMapping em : compareMappings) {
			compareMap.put(em.getMappingCode(), em);
		}

		// 新增或修改的实体映射
		for (OntEntityMapping compareEm : compareMappings) {
			OntEntityMapping baseEm = baseMap.get(compareEm.getMappingCode());
			if (baseEm == null) {
				// 新增实体映射
				changes.add(new ChangeRecord(
						"entityMapping.+" + compareEm.getMappingCode(), "",
						compareEm.getMappingName(), MINOR));
			}
			else {
				// 比较已有实体映射的字段
				compareEntityMappingFields(changes, baseEm, compareEm);
				// 比较字段映射子表
				classifyFieldMappings(changes, baseEm.getId(), compareEm.getId(),
						"entityMapping." + compareEm.getMappingCode());
			}
		}

		// 删除的实体映射
		for (OntEntityMapping baseEm : baseMappings) {
			if (!compareMap.containsKey(baseEm.getMappingCode())) {
				changes.add(new ChangeRecord(
						"entityMapping.-" + baseEm.getMappingCode(), baseEm.getMappingName(),
						"", MAJOR));
			}
		}
	}

	/**
	 * 比较单个实体映射的字段变化。
	 */
	private void compareEntityMappingFields(List<ChangeRecord> changes, OntEntityMapping base,
			OntEntityMapping compare) {
		// MAJOR_HIGH_RISK: 主键、IRI模板、目标类型、数据源、源对象
		compareField(changes, "entityMapping." + base.getMappingCode() + ".keyColumns",
				base.getKeyColumns(), compare.getKeyColumns(), MAJOR_HIGH_RISK);
		compareField(changes, "entityMapping." + base.getMappingCode() + ".iriTemplate",
				base.getIriTemplate(), compare.getIriTemplate(), MAJOR_HIGH_RISK);
		compareField(changes, "entityMapping." + base.getMappingCode() + ".targetEntityTypeId",
				String.valueOf(base.getTargetEntityTypeId()),
				String.valueOf(compare.getTargetEntityTypeId()), MAJOR_HIGH_RISK);
		compareField(changes, "entityMapping." + base.getMappingCode() + ".sourceId",
				String.valueOf(base.getSourceId()), String.valueOf(compare.getSourceId()),
				MAJOR_HIGH_RISK);
		compareField(changes, "entityMapping." + base.getMappingCode() + ".sourceObject",
				base.getSourceObject(), compare.getSourceObject(), MAJOR_HIGH_RISK);

		// MAJOR_HIGH_RISK: 删除策略为SOFT_DELETE
		compareField(changes, "entityMapping." + base.getMappingCode() + ".deleteStrategy",
				base.getDeleteStrategy(), compare.getDeleteStrategy(), MAJOR_HIGH_RISK);

		// PATCH: 描述、排序
		compareField(changes, "entityMapping." + base.getMappingCode() + ".mappingName",
				base.getMappingName(), compare.getMappingName(), PATCH);
		compareField(changes, "entityMapping." + base.getMappingCode() + ".syncOrder",
				String.valueOf(base.getSyncOrder()), String.valueOf(compare.getSyncOrder()), PATCH);
	}

	/**
	 * 比较两个实体映射下的字段映射子表。
	 */
	private void classifyFieldMappings(List<ChangeRecord> changes, Long baseEntityMappingId,
			Long compareEntityMappingId, String pathPrefix) {
		List<OntFieldMapping> baseFields = fieldMappingMapper.selectList(
				Wrappers.<OntFieldMapping>lambdaQuery()
						.eq(OntFieldMapping::getEntityMappingId, baseEntityMappingId)
						.eq(OntFieldMapping::getDelFlag, "0"));

		List<OntFieldMapping> compareFields = fieldMappingMapper.selectList(
				Wrappers.<OntFieldMapping>lambdaQuery()
						.eq(OntFieldMapping::getEntityMappingId, compareEntityMappingId)
						.eq(OntFieldMapping::getDelFlag, "0"));

		Map<String, OntFieldMapping> baseMap = new HashMap<>();
		for (OntFieldMapping fm : baseFields) {
			baseMap.put(fm.getFieldMappingCode(), fm);
		}

		for (OntFieldMapping compareFm : compareFields) {
			OntFieldMapping baseFm = baseMap.get(compareFm.getFieldMappingCode());
			if (baseFm == null) {
				// 新增字段映射
				changes.add(new ChangeRecord(
						pathPrefix + ".fieldMapping.+" + compareFm.getFieldMappingCode(),
						"", compareFm.getFieldMappingName(), MINOR));
			}
			else {
				// MAJOR: 修改转换器、目标属性
				compareField(changes,
						pathPrefix + ".fieldMapping." + compareFm.getFieldMappingCode() + ".transformer",
						baseFm.getTransformer(), compareFm.getTransformer(), MAJOR);
				compareField(changes,
						pathPrefix + ".fieldMapping." + compareFm.getFieldMappingCode()
								+ ".targetDataPropertyId",
						String.valueOf(baseFm.getTargetDataPropertyId()),
						String.valueOf(compareFm.getTargetDataPropertyId()), MAJOR);
			}
		}

		// 删除的字段映射
		Map<String, OntFieldMapping> compareMap = new HashMap<>();
		for (OntFieldMapping fm : compareFields) {
			compareMap.put(fm.getFieldMappingCode(), fm);
		}
		for (OntFieldMapping baseFm : baseFields) {
			if (!compareMap.containsKey(baseFm.getFieldMappingCode())) {
				changes.add(new ChangeRecord(
						pathPrefix + ".fieldMapping.-" + baseFm.getFieldMappingCode(),
						baseFm.getFieldMappingName(), "", MAJOR));
			}
		}
	}

	/**
	 * 比较两个版本下的关系映射子表。
	 */
	private void classifyRelationMappings(List<ChangeRecord> changes, Long baseVersionId, Long compareVersionId) {
		List<OntRelationMapping> baseMappings = relationMappingMapper.selectList(
				Wrappers.<OntRelationMapping>lambdaQuery()
						.eq(OntRelationMapping::getMappingVersionId, baseVersionId)
						.eq(OntRelationMapping::getDelFlag, "0"));

		List<OntRelationMapping> compareMappings = relationMappingMapper.selectList(
				Wrappers.<OntRelationMapping>lambdaQuery()
						.eq(OntRelationMapping::getMappingVersionId, compareVersionId)
						.eq(OntRelationMapping::getDelFlag, "0"));

		Map<String, OntRelationMapping> baseMap = new HashMap<>();
		for (OntRelationMapping rm : baseMappings) {
			baseMap.put(rm.getMappingCode(), rm);
		}

		Map<String, OntRelationMapping> compareMap = new HashMap<>();
		for (OntRelationMapping rm : compareMappings) {
			compareMap.put(rm.getMappingCode(), rm);
		}

		for (OntRelationMapping compareRm : compareMappings) {
			OntRelationMapping baseRm = baseMap.get(compareRm.getMappingCode());
			if (baseRm == null) {
				// 新增关系映射
				changes.add(new ChangeRecord(
						"relationMapping.+" + compareRm.getMappingCode(), "",
						compareRm.getMappingName(), MINOR));
			}
			else {
				String prefix = "relationMapping." + compareRm.getMappingCode();
				// MAJOR_HIGH_RISK: relationMode 变化
				compareField(changes, prefix + ".relationMode",
						baseRm.getRelationMode(), compareRm.getRelationMode(), MAJOR_HIGH_RISK);
				// MAJOR: object_property_id 变化
				compareField(changes, prefix + ".objectPropertyId",
						String.valueOf(baseRm.getObjectPropertyId()),
						String.valueOf(compareRm.getObjectPropertyId()), MAJOR);
				// MAJOR_HIGH_RISK: subject/object entity_mapping_id 变化
				compareField(changes, prefix + ".subjectEntityMappingId",
						String.valueOf(baseRm.getSubjectEntityMappingId()),
						String.valueOf(compareRm.getSubjectEntityMappingId()), MAJOR_HIGH_RISK);
				compareField(changes, prefix + ".objectEntityMappingId",
						String.valueOf(baseRm.getObjectEntityMappingId()),
						String.valueOf(compareRm.getObjectEntityMappingId()), MAJOR_HIGH_RISK);
				// PATCH: 名称、排序
				compareField(changes, prefix + ".mappingName",
						baseRm.getMappingName(), compareRm.getMappingName(), PATCH);
				compareField(changes, prefix + ".syncOrder",
						String.valueOf(baseRm.getSyncOrder()),
						String.valueOf(compareRm.getSyncOrder()), PATCH);
			}
		}

		// 删除的关系映射
		for (OntRelationMapping baseRm : baseMappings) {
			if (!compareMap.containsKey(baseRm.getMappingCode())) {
				changes.add(new ChangeRecord(
						"relationMapping.-" + baseRm.getMappingCode(),
						baseRm.getMappingName(), "", MAJOR));
			}
		}
	}

	/**
	 * 从变更项列表中获取最高风险分类。
	 * @param changes 变更项列表
	 * @return 最高风险分类，无变更时返回 null
	 */
	public String getHighestClassification(List<ChangeRecord> changes) {
		if (changes.isEmpty()) {
			return null;
		}
		int maxRank = 0;
		String result = null;
		for (ChangeRecord change : changes) {
			int rank = rankOfClassification(change.classification());
			if (rank > maxRank) {
				maxRank = rank;
				result = change.classification();
			}
		}
		return result;
	}

	/**
	 * 比较单个字段并记录变更。
	 */
	private void compareField(List<ChangeRecord> changes, String field, String oldValue, String newValue,
			String classification) {
		if (!Objects.equals(oldValue, newValue)) {
			changes.add(new ChangeRecord(field,
					oldValue != null ? oldValue : "",
					newValue != null ? newValue : "",
					classification));
		}
	}

	/**
	 * 分类风险等级排序。
	 */
	private int rankOfClassification(String classification) {
		return switch (classification) {
			case PATCH -> 1;
			case MINOR -> 2;
			case MAJOR -> 3;
			case MAJOR_HIGH_RISK -> 4;
			default -> 0;
		};
	}

	/**
	 * 变更记录。
	 *
	 * @param field 变更字段路径
	 * @param oldValue 旧值
	 * @param newValue 新值
	 * @param classification 变更分类
	 */
	public record ChangeRecord(String field, String oldValue, String newValue, String classification) {
	}

}
