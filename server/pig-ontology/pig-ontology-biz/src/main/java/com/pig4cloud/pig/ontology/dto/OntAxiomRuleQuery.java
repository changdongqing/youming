/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公理规则查询条件。
 *
 * @author youming
 */
@Data
@Schema(description = "公理规则查询条件")
public class OntAxiomRuleQuery {

	@Schema(description = "规则名称")
	private String name;

	@Schema(description = "规则编码")
	private String ruleCode;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "规则大类")
	private String category;

	@Schema(description = "规则子类")
	private String subType;

	@Schema(description = "状态")
	private String status;

	@Schema(description = "严重级别")
	private String severity;

	@Schema(description = "是否内置")
	private String isBuiltin;

	@Schema(description = "是否启用")
	private String isEnabled;

}
