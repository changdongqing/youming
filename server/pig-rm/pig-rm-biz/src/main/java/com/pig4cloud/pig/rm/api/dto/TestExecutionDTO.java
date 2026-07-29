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
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 测试执行 DTO
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "测试执行DTO")
public class TestExecutionDTO {

	@NotNull(message = "测试任务单ID不能为空")
	@Schema(description = "测试任务单ID")
	private Long testTaskId;

	@NotNull(message = "用例ID不能为空")
	@Schema(description = "用例ID")
	private Long caseId;

	@NotBlank(message = "执行结果不能为空")
	@Schema(description = "执行结果 PASS/FAIL/BLOCKED")
	private String result;

	@Schema(description = "执行备注")
	private String remark;

}
