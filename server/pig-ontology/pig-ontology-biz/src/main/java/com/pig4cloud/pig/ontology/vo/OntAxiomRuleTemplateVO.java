/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 公理规则模板。
 *
 * @author youming
 */
@Data
@Schema(description = "公理规则模板")
public class OntAxiomRuleTemplateVO {

	@Schema(description = "模板编码")
	private String templateCode;

	@Schema(description = "模板版本")
	private Integer templateVersion;

	@Schema(description = "规则大类")
	private String category;

	@Schema(description = "规则子类")
	private String subType;

	@Schema(description = "形式化模式")
	private String formalizationMode;

	@Schema(description = "校验模式")
	private String validationMode;

	@Schema(description = "执行器编码")
	private String executorCode;

	@Schema(description = "绑定角色集合")
	private List<BindingRoleVO> bindingRoles;

	/**
	 * 模板绑定角色。
	 *
	 * @author youming
	 */
	@Data
	@Schema(description = "模板绑定角色")
	public static class BindingRoleVO {

		@Schema(description = "角色")
		private String role;

		@Schema(description = "目标类型")
		private String targetType;

		@Schema(description = "最小数量")
		private Integer minCount;

		@Schema(description = "最大数量")
		private Integer maxCount;

		@Schema(description = "描述")
		private String description;

	}

}
