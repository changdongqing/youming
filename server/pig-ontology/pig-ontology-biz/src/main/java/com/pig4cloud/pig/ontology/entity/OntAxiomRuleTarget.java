/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公理规则目标绑定。
 *
 * @author youming
 */
@Data
@TableName("ont_axiom_rule_target")
@Schema(description = "公理规则目标绑定")
public class OntAxiomRuleTarget {

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键ID")
	private Long id;

	@Schema(description = "公理规则ID")
	private Long axiomRuleId;

	@Schema(description = "绑定角色")
	private String bindingRole;

	@Schema(description = "绑定顺序")
	private Integer bindingOrder;

	@Schema(description = "目标类型")
	private String targetType;

	@Schema(description = "实体类型ID")
	private Long entityTypeId;

	@Schema(description = "数据属性ID")
	private Long dataPropertyId;

	@Schema(description = "对象属性ID")
	private Long objectPropertyId;

	@Schema(description = "单位分类ID")
	private Long unitCategoryId;

}
