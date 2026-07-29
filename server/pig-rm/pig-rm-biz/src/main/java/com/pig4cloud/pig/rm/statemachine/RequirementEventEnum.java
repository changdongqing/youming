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

package com.pig4cloud.pig.rm.statemachine;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 需求事件枚举（对应 PRD 3.6 状态机触发动作）
 *
 * @author youming
 * @date 2026-07-29
 */
@Getter
@AllArgsConstructor
public enum RequirementEventEnum {

	SUBMIT("提交"),
	APPROVE_PASS("审批通过"),
	APPROVE_REJECT("审批驳回"),
	RESUBMIT("重新提交"),
	REVIEW_PASS("讨论会通过"),
	REVIEW_FAIL("讨论会不通过"),
	DESIGN_SUBMIT("设计提交评审"),
	DESIGN_REVIEW_PASS("设计评审通过"),
	DESIGN_REVIEW_FAIL("设计评审不通过"),
	SCHEDULE_CONFIRM("排期确认"),
	DEV_COMPLETE("开发完成提测"),
	TEST_PASS("测试通过（含质量确认）"),
	TEST_REJECT("测试驳回"),
	ACCEPT("验收通过"),
	RELEASE("纳入发版");

	private final String description;

}
