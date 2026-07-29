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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 待办事项
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "待办事项")
@EqualsAndHashCode(callSuper = true)
@TableName("rm_todo")
public class Todo extends Model<Todo> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "待办归属人ID")
	@NotNull(message = "用户ID不能为空")
	private Long userId;

	@Schema(description = "单据类型")
	@NotBlank(message = "单据类型不能为空")
	private String billType;

	@Schema(description = "单据ID")
	@NotNull(message = "单据ID不能为空")
	private Long billId;

	@Schema(description = "待办类型")
	@NotBlank(message = "待办类型不能为空")
	private String todoType;

	@Schema(description = "待办标题")
	@NotBlank(message = "待办标题不能为空")
	private String title;

	@Schema(description = "跳转路径")
	private String url;

	@Schema(description = "状态 PENDING/DONE/CANCEL")
	private String status;

	@Schema(description = "截止时间（超期预警用）")
	private LocalDateTime dueTime;

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
