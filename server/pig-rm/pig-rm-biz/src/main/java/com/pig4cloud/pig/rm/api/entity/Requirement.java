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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 软件需求申请单
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "软件需求申请单")
@EqualsAndHashCode(callSuper = true)
@TableName("rm_requirement")
public class Requirement extends Model<Requirement> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "申请单编号 REQ-YYYYMMDD-XXX")
	private String reqCode;

	@Schema(description = "需求标题")
	@NotBlank(message = "需求标题不能为空")
	private String title;

	@Schema(description = "需求来源 ITERATION/CUSTOMER_DELIVERY/CUSTOMER_OPS/PRESALE")
	@NotBlank(message = "需求来源不能为空")
	private String source;

	@Schema(description = "关联客户/项目")
	private String customerProject;

	@Schema(description = "需求描述（富文本HTML）")
	@NotBlank(message = "需求描述不能为空")
	private String description;

	@Schema(description = "期望完成时间")
	@NotNull(message = "期望完成时间不能为空")
	private LocalDate expectCompleteDate;

	@Schema(description = "发起人ID")
	private Long initiatorId;

	@Schema(description = "发起部门ID")
	private Long initiatorDeptId;

	@Schema(description = "是否需讨论会评审 0否 1是")
	private String needReview;

	@Schema(description = "讨论会评审结论 PASS/FAIL")
	private String reviewConclusion;

	@Schema(description = "讨论会评审意见")
	private String reviewRemark;

	@Schema(description = "需求设计（富文本）")
	private String designContent;

	@Schema(description = "需求设计工作量（人日）")
	private BigDecimal designWorkload;

	@Schema(description = "计划设计完成时间")
	private LocalDate designPlanDate;

	@Schema(description = "实际设计完成时间")
	private LocalDate designActualDate;

	@Schema(description = "排期说明/时间风险标注")
	private String scheduleRemark;

	@Schema(description = "时间风险等级 NORMAL/WARN/OVERDUE")
	private String scheduleRisk;

	@Schema(description = "当前状态")
	private String status;

	@Schema(description = "验收结论 PASS/FAIL")
	private String acceptConclusion;

	@Schema(description = "验收意见")
	private String acceptRemark;

	@Schema(description = "系统自动完成时间")
	private LocalDateTime completeTime;

	@Schema(description = "优先级 HIGH/MEDIUM/LOW")
	private String priority;

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
