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

import com.pig4cloud.pig.rm.api.entity.Bug;
import com.pig4cloud.pig.rm.api.entity.TestExecution;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 测试任务单详情 VO
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "测试任务单详情VO")
public class TestTaskDetailVO {

	private Long id;

	private String taskCode;

	private Long devTaskId;

	private Long requirementId;

	private Long testerId;

	private String conclusion;

	private BigDecimal workload;

	private LocalDateTime testStartTime;

	private LocalDateTime testEndTime;

	private String status;

	@Schema(description = "执行记录列表")
	private List<TestExecution> executions;

	@Schema(description = "bug列表")
	private List<Bug> bugs;

	@Schema(description = "bug总数")
	private Long bugCount;

	@Schema(description = "未解决bug数")
	private Long unresolvedBugCount;

	@Schema(description = "需求编号")
	private String requirementCode;

	@Schema(description = "开发任务编号")
	private String devTaskCode;

}
