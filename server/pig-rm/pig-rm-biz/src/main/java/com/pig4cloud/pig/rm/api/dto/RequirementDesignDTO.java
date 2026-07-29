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
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 需求设计 DTO
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "需求设计DTO")
public class RequirementDesignDTO {

	@NotNull(message = "需求ID不能为空")
	@Schema(description = "需求ID")
	private Long requirementId;

	@Schema(description = "需求设计内容（富文本）")
	private String designContent;

	@Schema(description = "设计工作量（人日）")
	private BigDecimal designWorkload;

	@Schema(description = "计划设计完成时间")
	private LocalDate designPlanDate;

}
