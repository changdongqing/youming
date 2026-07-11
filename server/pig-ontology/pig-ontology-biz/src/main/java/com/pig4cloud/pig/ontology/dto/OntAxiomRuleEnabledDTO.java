/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 公理规则启停请求。
 *
 * @author youming
 */
@Data
@Schema(description = "公理规则启停请求")
public class OntAxiomRuleEnabledDTO {

	@NotBlank(message = "启用状态不能为空")
	@Pattern(regexp = "[01]", message = "启用状态只能为0或1")
	@Schema(description = "启用状态，1启用0停用")
	private String enabled;

}
