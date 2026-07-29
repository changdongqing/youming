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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 测试任务单
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "测试任务单")
@EqualsAndHashCode(callSuper = true)
@TableName("rm_test_task")
public class TestTask extends Model<TestTask> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "任务单编号 TST-XXX")
	private String taskCode;

	@Schema(description = "关联开发任务单ID")
	private Long devTaskId;

	@Schema(description = "关联需求单ID（冗余）")
	private Long requirementId;

	@Schema(description = "测试人员ID")
	private Long testerId;

	@Schema(description = "测试结论 PASS/FAIL/BLOCKED")
	private String conclusion;

	@Schema(description = "测试工作量（人日）——考核点5")
	private BigDecimal workload;

	@Schema(description = "测试开始时间")
	private LocalDateTime testStartTime;

	@Schema(description = "测试结束时间")
	private LocalDateTime testEndTime;

	@Schema(description = "状态 PENDING_TEST/IN_TEST/PASS/FAIL")
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
