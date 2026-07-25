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

package com.pig4cloud.pig.ontology.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 属性模板（数据属性 + 对象属性，同构）
 *
 * @author pig
 * @date 2026-07-25
 */
@Data
@Schema(description = "属性模板")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_property_template")
public class PropertyTemplate extends Model<PropertyTemplate> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "唯一标识，如 name/contains/price")
	@NotBlank(message = "模板标识不能为空")
	private String templateCode;

	@Schema(description = "datatype / object")
	@NotBlank(message = "类型不能为空")
	@Pattern(regexp = "datatype|object", message = "类型必须为 datatype 或 object")
	private String kind;

	@Schema(description = "显示名")
	@NotBlank(message = "显示名不能为空")
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

	// 列名 enum_values（原 values 是 SQL 保留字，被 Druid 解析器误判为 INSERT VALUES 子句，故重命名）
	@TableField("enum_values")
	@Schema(description = "枚举值（逗号分隔），datatype 专属")
	private String enumValues;

	@Schema(description = "object 专属：one-to-many/many-to-one/...")
	private String defaultCardinality;

	@Schema(description = "builtin / custom")
	private String source;

	@Schema(description = "0/1 弃用标记")
	private String deprecated;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改人")
	private String updateBy;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改时间")
	private LocalDateTime updateTime;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;

}
