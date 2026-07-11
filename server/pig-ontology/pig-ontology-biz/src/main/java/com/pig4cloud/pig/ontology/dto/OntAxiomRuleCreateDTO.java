/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增公理规则请求。
 *
 * @author youming
 */
@Data
@Schema(description = "新增公理规则请求")
public class OntAxiomRuleCreateDTO {

	@Schema(description = "本体工程ID，首期缺省为核心工程")
	private Long ontologyId;

	@NotBlank(message = "规则编码不能为空")
	@Size(max = 64, message = "规则编码长度不能超过64")
	@Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "规则编码必须以大写字母开头，仅支持大写字母、数字和下划线")
	@Schema(description = "规则编码")
	private String ruleCode;

	@NotBlank(message = "规则名称不能为空")
	@Size(max = 128, message = "规则名称长度不能超过128")
	@Schema(description = "规则名称")
	private String name;

	@NotBlank(message = "规则大类不能为空")
	@Pattern(regexp = "ENTITY_TYPE|PROPERTY|RELATION", message = "规则大类只能为ENTITY_TYPE、PROPERTY或RELATION")
	@Schema(description = "规则大类")
	private String category;

	@NotBlank(message = "规则子类不能为空")
	@Schema(description = "规则子类")
	private String subType;

	@Size(max = 512, message = "描述长度不能超过512")
	@Schema(description = "描述")
	private String description;

	@NotBlank(message = "严重级别不能为空")
	@Pattern(regexp = "VIOLATION|WARNING|INFO", message = "严重级别只能为VIOLATION、WARNING或INFO")
	@Schema(description = "严重级别")
	private String severity;

	@NotBlank(message = "模板编码不能为空")
	@Schema(description = "模板编码")
	private String templateCode;

	@Schema(description = "配置JSON字符串")
	private String config;

	@NotEmpty(message = "规则目标不能为空")
	@Schema(description = "规则目标绑定集合")
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
