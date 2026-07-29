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

package com.pig4cloud.pig.rm.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 需求查询 DTO
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "需求查询DTO")
public class RequirementQueryDTO {

	@Schema(description = "需求来源")
	private String source;

	@Schema(description = "需求状态")
	private String status;

	@Schema(description = "发起人ID")
	private Long initiatorId;

	@Schema(description = "创建时间起")
	private LocalDate startDate;

	@Schema(description = "创建时间止")
	private LocalDate endDate;

	@Schema(description = "模糊搜索标题")
	private String keyword;

}
