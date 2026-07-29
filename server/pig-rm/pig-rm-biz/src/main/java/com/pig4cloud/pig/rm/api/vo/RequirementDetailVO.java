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

import com.pig4cloud.pig.rm.api.entity.ApprovalRecord;
import com.pig4cloud.pig.rm.api.entity.Requirement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 需求详情 VO
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "需求详情VO")
public class RequirementDetailVO {

	@Schema(description = "需求申请单")
	private Requirement requirement;

	@Schema(description = "审批记录时间线")
	private List<ApprovalRecord> approvalRecords;

	@Schema(description = "关联开发任务摘要")
	private List<DevTaskSummaryVO> devTasks;

	@Schema(description = "发起人姓名")
	private String initiatorName;

	@Schema(description = "发起部门名称")
	private String initiatorDeptName;

}
