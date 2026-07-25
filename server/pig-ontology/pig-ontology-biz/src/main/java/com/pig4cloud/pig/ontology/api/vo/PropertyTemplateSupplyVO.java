/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.ontology.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 属性模板供给视图（FR-5 / AC-5.2/5.7）
 * <p>
 * 供给接口的稳定化返回结构：供建模侧拉取，仅暴露建模侧需要的业务字段，
 * 屏蔽 createBy/updateBy/delFlag 等内部审计与状态字段，避免表结构变更破坏建模侧。
 *
 * @author pig
 * @date 2026-07-25
 */
@Data
@Schema(description = "属性模板供给视图")
public class PropertyTemplateSupplyVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主键")
	private Long id;

	@Schema(description = "唯一标识，建模侧据此挂 templateRef")
	private String templateCode;

	@Schema(description = "datatype / object")
	private String kind;

	@Schema(description = "显示名")
	private String label;

	@Schema(description = "业务说明")
	private String description;

	@Schema(description = "分组：basic/contact/monetary/temporal/status/containment/attribution")
	private String category;

	@Schema(description = "datatype 专属：string/integer/decimal/boolean/datetime")
	private String type;

	@Schema(description = "0/1，datatype 专属标识符")
	private String isIdentifier;

	@Schema(description = "预设 QUDT 单位 IRI")
	private String unitRef;

	@Schema(description = "枚举值（逗号分隔），datatype 专属")
	private String enumValues;

	@Schema(description = "object 专属：one-to-many/many-to-one/...")
	private String defaultCardinality;

	@Schema(description = "builtin / custom")
	private String source;

	@Schema(description = "0/1 弃用标记")
	private String deprecated;

}
