/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.model;

import com.pig4cloud.pig.ontology.entity.OntValidationResult;
import lombok.Builder;
import lombok.Data;

/**
 * 内部校验结果模型（执行器产出）。
 *
 * @author youming
 */
@Data
@Builder
public class ValidationResult {

	/**
	 * 严重程度
	 */
	private Severity severity;

	/**
	 * 违规实例IRI
	 */
	private String focusNode;

	/**
	 * 违规属性IRI
	 */
	private String resultPath;

	/**
	 * 违反规则名称
	 */
	private String ruleName;

	/**
	 * 违反规则编码
	 */
	private String ruleCode;

	/**
	 * 说明
	 */
	private String message;

	/**
	 * 期望值
	 */
	private String expectedValue;

	/**
	 * 实际值
	 */
	private String actualValue;

	/**
	 * 修复建议
	 */
	private String suggestion;

	/**
	 * 严重程度枚举。
	 */
	public enum Severity {

		/**
		 * 违规（阻断发布/导出）
		 */
		VIOLATION,

		/**
		 * 警告
		 */
		WARNING,

		/**
		 * 信息
		 */
		INFO

	}

	/**
	 * 转换为持久化实体。
	 * @param reportId 报告ID
	 * @param sortOrder 排序序号
	 * @return 持久化实体
	 */
	public OntValidationResult toEntity(Long reportId, int sortOrder) {
		OntValidationResult entity = new OntValidationResult();
		entity.setReportId(reportId);
		entity.setSeverity(severity != null ? severity.name() : Severity.INFO.name());
		entity.setFocusNode(focusNode);
		entity.setResultPath(resultPath);
		entity.setRuleName(ruleName);
		entity.setRuleCode(ruleCode);
		entity.setMessage(message);
		entity.setExpectedValue(expectedValue);
		entity.setActualValue(actualValue);
		entity.setSuggestion(suggestion);
		entity.setSortOrder(sortOrder);
		return entity;
	}

}
