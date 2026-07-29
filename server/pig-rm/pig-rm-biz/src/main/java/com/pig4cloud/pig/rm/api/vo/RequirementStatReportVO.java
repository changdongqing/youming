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
 * 需求统计报表 VO（RPT-02）
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "需求统计报表VO")
public class RequirementStatReportVO {

	@Schema(description = "需求总数")
	private Long totalCount;

	@Schema(description = "设计工作量合计")
	private BigDecimal totalDesignWorkload;

	@Schema(description = "按来源统计")
	private List<StatItem> bySource;

	@Schema(description = "按状态统计")
	private List<StatItem> byStatus;

	@Schema(description = "按月统计")
	private List<StatItem> byMonth;

	@Data
	@Schema(description = "统计项")
	public static class StatItem {

		private String key;

		private Long count;

	}

}
