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
import java.util.List;

/**
 * 开发任务统计报表 VO（RPT-03）
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "开发任务统计报表VO")
public class DevTaskStatReportVO {

	@Schema(description = "任务总数")
	private Long totalTasks;

	@Schema(description = "已完成数")
	private Long completedTasks;

	@Schema(description = "完成率(%)")
	private BigDecimal completionRate;

	@Schema(description = "排期达成数（实际完成≤计划完成）")
	private Long onScheduleTasks;

	@Schema(description = "排期达成率(%)")
	private BigDecimal scheduleAchievementRate;

	@Schema(description = "按状态分布")
	private List<RequirementStatReportVO.StatItem> byStatus;

}
