/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.rm.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 表单字段配置（表单自定义配置）
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "表单字段配置")
@EqualsAndHashCode(callSuper = true)
@TableName("rm_form_field_config")
public class FormFieldConfig extends Model<FormFieldConfig> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "表单编码 REQUIREMENT/DEV_TASK/TEST_TASK")
	private String formCode;

	@Schema(description = "字段key")
	private String fieldKey;

	@Schema(description = "字段标签")
	private String fieldLabel;

	@Schema(description = "类型 text/richtext/date/number/select")
	private String fieldType;

	@Schema(description = "是否必填 0否 1是")
	private String required;

	@Schema(description = "是否可见 0否 1是")
	private String visible;

	@Schema(description = "列表是否可排序 0否 1是")
	private String sortable;

	@Schema(description = "关联字典编码")
	private String dictCode;

	@Schema(description = "排序")
	private Integer sortOrder;

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
	@Schema(description = "删除标记")
	private String delFlag;

}
