/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.diff;

import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
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
 * V1 简化：由于 18-04/18-05 子表尚未实现，仅比较工程级和版本级字段。
 *
 * @author youming
 */
@Component
public class MappingChangeClassifier {

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

		return changes;
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
