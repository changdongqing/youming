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

package com.pig4cloud.pig.rm.api.vo;

import com.pig4cloud.pig.rm.api.entity.ApprovalRecord;
import com.pig4cloud.pig.rm.api.entity.DevTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 开发任务详情 VO
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "开发任务详情VO")
public class DevTaskDetailVO {

	@Schema(description = "主键")
	private Long id;

	@Schema(description = "任务编号")
	private String taskCode;

	@Schema(description = "所属需求单ID")
	private Long requirementId;

	@Schema(description = "任务名称")
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

	@Schema(description = "详细设计")
	private String detailDesign;

	@Schema(description = "评审结论")
	private String reviewConclusion;

	@Schema(description = "评审意见")
	private String reviewRemark;

	@Schema(description = "任务状态")
	private String status;

	@Schema(description = "需求编号")
	private String requirementCode;

	@Schema(description = "需求标题")
	private String requirementTitle;

	@Schema(description = "负责人名称")
	private String assigneeName;

	@Schema(description = "评审记录")
	private List<ApprovalRecord> reviewRecords;

}
