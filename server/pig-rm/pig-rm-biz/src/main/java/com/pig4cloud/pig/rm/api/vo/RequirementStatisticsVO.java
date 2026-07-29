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
import java.util.Map;

/**
 * 需求统计 VO
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "需求统计VO")
public class RequirementStatisticsVO {

	@Schema(description = "需求总数")
	private Long totalCount;

	@Schema(description = "按来源统计")
	private List<Map<String, Object>> bySource;

	@Schema(description = "按状态统计")
	private List<Map<String, Object>> byStatus;

	@Schema(description = "按月统计")
	private List<Map<String, Object>> byMonth;

	@Schema(description = "设计工作量合计")
	private BigDecimal totalDesignWorkload;

}
