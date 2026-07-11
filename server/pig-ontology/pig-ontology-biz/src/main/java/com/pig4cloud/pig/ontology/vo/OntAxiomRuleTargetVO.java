/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公理规则目标绑定。
 *
 * @author youming
 */
@Data
@Schema(description = "公理规则目标绑定")
public class OntAxiomRuleTargetVO {

	@Schema(description = "主键ID")
	private Long id;

	@Schema(description = "绑定角色")
	private String bindingRole;

	@Schema(description = "绑定顺序")
	private Integer bindingOrder;

	@Schema(description = "目标类型")
	private String targetType;

	@Schema(description = "目标ID")
	private Long targetId;

	@Schema(description = "目标名称")
	private String name;

	@Schema(description = "目标标签")
	private String label;

	@Schema(description = "目标IRI")
	private String iri;

}
