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

/**
 * 开发任务查询 DTO
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "开发任务查询DTO")
public class DevTaskQueryDTO {

	@Schema(description = "所属需求ID")
	private Long requirementId;

	@Schema(description = "负责人ID")
	private Long assigneeId;

	@Schema(description = "任务状态")
	private String status;

	@Schema(description = "模糊搜索任务名称")
	private String keyword;

}
