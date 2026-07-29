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
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 任务创建 DTO（批量分解用）
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "任务创建DTO")
public class DevTaskSaveDTO {

	@NotBlank(message = "任务名称不能为空")
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

}
