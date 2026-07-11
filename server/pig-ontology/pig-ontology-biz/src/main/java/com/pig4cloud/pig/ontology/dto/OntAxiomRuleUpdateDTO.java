/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 更新公理规则请求。
 *
 * @author youming
 */
@Data
@Schema(description = "更新公理规则请求")
public class OntAxiomRuleUpdateDTO {

	@NotNull(message = "公理规则ID不能为空")
	@Schema(description = "公理规则ID")
	private Long id;

	@Size(max = 128, message = "规则名称长度不能超过128")
	@Schema(description = "规则名称")
	private String name;

	@Size(max = 512, message = "描述长度不能超过512")
	@Schema(description = "描述")
	private String description;

	@Pattern(regexp = "VIOLATION|WARNING|INFO", message = "严重级别只能为VIOLATION、WARNING或INFO")
	@Schema(description = "严重级别")
	private String severity;

	@Schema(description = "配置JSON字符串")
	private String config;

	@Schema(description = "规则目标绑定集合，为null时不更新")
	private List<OntAxiomRuleTargetDTO> targets;

	@Schema(description = "自定义OWL公理")
	private String customOwlAxiom;

	@Schema(description = "自定义SHACL形状")
	private String customShaclShape;

	@Min(value = 0, message = "排序值不能小于0")
	@Schema(description = "排序")
	private Integer sortOrder;

	@Size(max = 255, message = "备注长度不能超过255")
	@Schema(description = "平台治理备注")
	private String remarks;

}
