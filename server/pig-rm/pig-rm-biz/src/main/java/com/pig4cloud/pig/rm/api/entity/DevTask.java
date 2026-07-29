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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 开发任务单
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "开发任务单")
@EqualsAndHashCode(callSuper = true)
@TableName("rm_dev_task")
public class DevTask extends Model<DevTask> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "任务编号 DEV-XXX")
	private String taskCode;

	@Schema(description = "所属需求单ID")
	@NotNull(message = "所属需求不能为空")
	private Long requirementId;

	@Schema(description = "任务名称")
	@NotBlank(message = "任务名称不能为空")
	private String taskName;

	@Schema(description = "任务描述")
	private String taskDesc;

	@Schema(description = "负责人ID")
	private Long assigneeId;

	@Schema(description = "计划开始时间")
	private LocalDate planStartDate;

	@Schema(description = "计划完成时间")
	private LocalDate planEndDate;

	@Schema(description = "实际开始时间")
	private LocalDate actualStartDate;

	@Schema(description = "实际完成时间")
	private LocalDate actualEndDate;

	@Schema(description = "详细设计（富文本）")
	private String detailDesign;

	@Schema(description = "评审结论 PASS/FAIL/PENDING")
	private String reviewConclusion;

	@Schema(description = "评审意见")
	private String reviewRemark;

	@Schema(description = "任务状态 PENDING_DEV/IN_DEV/PENDING_TEST/COMPLETED")
	private String status;

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
