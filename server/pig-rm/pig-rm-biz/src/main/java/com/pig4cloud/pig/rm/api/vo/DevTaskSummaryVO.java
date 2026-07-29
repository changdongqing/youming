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

import java.time.LocalDate;

/**
 * 开发任务摘要 VO（需求详情页展示关联任务用）
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "开发任务摘要VO")
public class DevTaskSummaryVO {

	@Schema(description = "任务ID")
	private Long id;

	@Schema(description = "任务编号")
	private String taskCode;

	@Schema(description = "任务名称")
	private String taskName;

	@Schema(description = "任务状态")
	private String status;

	@Schema(description = "负责人名称")
	private String assigneeName;

	@Schema(description = "计划完成时间")
	private LocalDate planEndDate;

}
