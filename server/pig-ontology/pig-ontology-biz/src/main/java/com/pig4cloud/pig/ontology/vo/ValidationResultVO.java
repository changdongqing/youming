/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 校验结果明细VO。
 *
 * @author youming
 */
@Data
@Schema(description = "校验结果明细")
public class ValidationResultVO {

	@Schema(description = "结果ID")
	private Long id;

	@Schema(description = "严重程度：VIOLATION/WARNING/INFO")
	private String severity;

	@Schema(description = "违规实例IRI")
	private String focusNode;

	@Schema(description = "违规属性IRI")
	private String resultPath;

	@Schema(description = "违反规则名称")
	private String ruleName;

	@Schema(description = "违反规则编码")
	private String ruleCode;

	@Schema(description = "说明")
	private String message;

	@Schema(description = "期望值")
	private String expectedValue;

	@Schema(description = "实际值")
	private String actualValue;

	@Schema(description = "修复建议")
	private String suggestion;

}
