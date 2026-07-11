/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 公理规则目标绑定请求。
 *
 * @author youming
 */
@Data
@Schema(description = "公理规则目标绑定请求")
public class OntAxiomRuleTargetDTO {

	@NotBlank(message = "绑定角色不能为空")
	@Schema(description = "绑定角色")
	private String bindingRole;

	@Min(value = 0, message = "绑定顺序不能小于0")
	@Schema(description = "绑定顺序")
	private Integer bindingOrder = 0;

	@NotBlank(message = "目标类型不能为空")
	@Pattern(regexp = "ENTITY_TYPE|DATA_PROPERTY|OBJECT_PROPERTY|UNIT_CATEGORY", message = "目标类型只能为ENTITY_TYPE、DATA_PROPERTY、OBJECT_PROPERTY或UNIT_CATEGORY")
	@Schema(description = "目标类型")
	private String targetType;

	@NotNull(message = "目标ID不能为空")
	@Schema(description = "目标ID")
	private Long targetId;

}
