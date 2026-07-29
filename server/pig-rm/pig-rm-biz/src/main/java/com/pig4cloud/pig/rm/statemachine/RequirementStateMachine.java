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

import com.pig4cloud.pig.common.core.exception.CheckedException;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.pig4cloud.pig.rm.statemachine.RequirementEventEnum.ACCEPT;
import static com.pig4cloud.pig.rm.statemachine.RequirementEventEnum.APPROVE_PASS;
import static com.pig4cloud.pig.rm.statemachine.RequirementEventEnum.APPROVE_REJECT;
import static com.pig4cloud.pig.rm.statemachine.RequirementEventEnum.DESIGN_REVIEW_FAIL;
import static com.pig4cloud.pig.rm.statemachine.RequirementEventEnum.DESIGN_REVIEW_PASS;
import static com.pig4cloud.pig.rm.statemachine.RequirementEventEnum.DESIGN_SUBMIT;
import static com.pig4cloud.pig.rm.statemachine.RequirementEventEnum.DEV_COMPLETE;
import static com.pig4cloud.pig.rm.statemachine.RequirementEventEnum.RELEASE;
import static com.pig4cloud.pig.rm.statemachine.RequirementEventEnum.RESUBMIT;
import static com.pig4cloud.pig.rm.statemachine.RequirementEventEnum.REVIEW_FAIL;
import static com.pig4cloud.pig.rm.statemachine.RequirementEventEnum.REVIEW_PASS;
import static com.pig4cloud.pig.rm.statemachine.RequirementEventEnum.SCHEDULE_CONFIRM;
import static com.pig4cloud.pig.rm.statemachine.RequirementEventEnum.SUBMIT;
import static com.pig4cloud.pig.rm.statemachine.RequirementEventEnum.TEST_PASS;
import static com.pig4cloud.pig.rm.statemachine.RequirementEventEnum.TEST_REJECT;
import static com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum.COMPLETED;
import static com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum.DESIGNING;
import static com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum.DESIGN_REVIEW;
import static com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum.DEVELOPING;
import static com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum.DRAFT;
import static com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum.PENDING_ACCEPTANCE;
import static com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum.PENDING_APPROVAL;
import static com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum.PENDING_REVIEW;
import static com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum.REJECTED;
import static com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum.RELEASED;
import static com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum.SCHEDULING;
import static com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum.TESTING;

/**
 * 需求状态机（默认转移表）
 * <p>
 * 转移规则可被 rm_flow_node 配置覆盖（见 FlowEngine）。
 *
 * @author youming
 * @date 2026-07-29
 */
@Component
public class RequirementStateMachine {

	/**
	 * 转移表 key = (当前状态, 事件)
	 */
	private record StatusEvent(RequirementStatusEnum status, RequirementEventEnum event) {
	}

	private static final Map<StatusEvent, RequirementStatusEnum> TRANSITIONS = Map.ofEntries(
			Map.entry(new StatusEvent(DRAFT, SUBMIT), PENDING_APPROVAL),
			Map.entry(new StatusEvent(PENDING_APPROVAL, APPROVE_PASS), PENDING_REVIEW),
			Map.entry(new StatusEvent(PENDING_APPROVAL, APPROVE_REJECT), REJECTED),
			Map.entry(new StatusEvent(REJECTED, RESUBMIT), PENDING_APPROVAL),
			Map.entry(new StatusEvent(PENDING_REVIEW, REVIEW_PASS), DESIGNING),
			Map.entry(new StatusEvent(PENDING_REVIEW, REVIEW_FAIL), REJECTED),
			Map.entry(new StatusEvent(DESIGNING, DESIGN_SUBMIT), DESIGN_REVIEW),
			Map.entry(new StatusEvent(DESIGN_REVIEW, DESIGN_REVIEW_PASS), SCHEDULING),
			Map.entry(new StatusEvent(DESIGN_REVIEW, DESIGN_REVIEW_FAIL), DESIGNING),
			Map.entry(new StatusEvent(SCHEDULING, SCHEDULE_CONFIRM), DEVELOPING),
			Map.entry(new StatusEvent(DEVELOPING, DEV_COMPLETE), TESTING),
			Map.entry(new StatusEvent(TESTING, TEST_REJECT), DEVELOPING),
			Map.entry(new StatusEvent(TESTING, TEST_PASS), PENDING_ACCEPTANCE),
			Map.entry(new StatusEvent(PENDING_ACCEPTANCE, ACCEPT), COMPLETED),
			Map.entry(new StatusEvent(COMPLETED, RELEASE), RELEASED));

	/**
	 * 执行状态转移
	 * @param current 当前状态
	 * @param event 触发事件
	 * @return 目标状态
	 */
	public RequirementStatusEnum transit(RequirementStatusEnum current, RequirementEventEnum event) {
		RequirementStatusEnum next = TRANSITIONS.get(new StatusEvent(current, event));
		if (next == null) {
			throw new CheckedException(
					"非法状态转移: " + current.getDescription() + " + " + event.getDescription());
		}
		return next;
	}

	/**
	 * 检查转移是否合法（不抛异常）
	 */
	public boolean canTransit(RequirementStatusEnum current, RequirementEventEnum event) {
		return TRANSITIONS.containsKey(new StatusEvent(current, event));
	}

}
