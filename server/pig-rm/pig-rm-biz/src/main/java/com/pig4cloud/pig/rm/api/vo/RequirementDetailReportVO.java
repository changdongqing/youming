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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 需求明细报表 VO（RPT-01）
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "需求明细报表VO")
public class RequirementDetailReportVO {

	@Schema(description = "申请单编号")
	private String reqCode;

	@Schema(description = "需求标题")
	private String title;

	@Schema(description = "需求来源")
	private String source;

	@Schema(description = "关联客户/项目")
	private String customerProject;

	@Schema(description = "期望完成时间")
	private LocalDate expectCompleteDate;

	@Schema(description = "发起人")
	private String initiatorName;

	@Schema(description = "当前状态")
	private String status;

	@Schema(description = "设计工作量(人日)")
	private BigDecimal designWorkload;

	@Schema(description = "计划设计完成")
	private LocalDate designPlanDate;

	@Schema(description = "实际设计完成")
	private LocalDate designActualDate;

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

}
