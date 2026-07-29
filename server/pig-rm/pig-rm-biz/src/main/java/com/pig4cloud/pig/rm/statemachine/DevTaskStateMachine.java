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

import static com.pig4cloud.pig.rm.statemachine.DevTaskEventEnum.START_DEV;
import static com.pig4cloud.pig.rm.statemachine.DevTaskEventEnum.SUBMIT_TEST;
import static com.pig4cloud.pig.rm.statemachine.DevTaskEventEnum.TEST_PASS;
import static com.pig4cloud.pig.rm.statemachine.DevTaskEventEnum.TEST_REJECT;
import static com.pig4cloud.pig.rm.statemachine.DevTaskStatusEnum.COMPLETED;
import static com.pig4cloud.pig.rm.statemachine.DevTaskStatusEnum.IN_DEV;
import static com.pig4cloud.pig.rm.statemachine.DevTaskStatusEnum.PENDING_DEV;
import static com.pig4cloud.pig.rm.statemachine.DevTaskStatusEnum.PENDING_TEST;

/**
 * 开发任务状态机
 * <p>
 * PENDING_DEV → IN_DEV → PENDING_TEST → COMPLETED<br>
 * 测试驳回：PENDING_TEST → IN_DEV（返工）
 *
 * @author youming
 * @date 2026-07-29
 */
@Component
public class DevTaskStateMachine {

	private record StatusEvent(DevTaskStatusEnum status, DevTaskEventEnum event) {
	}

	private static final Map<StatusEvent, DevTaskStatusEnum> TRANSITIONS = Map.of(
			new StatusEvent(PENDING_DEV, START_DEV), IN_DEV,
			new StatusEvent(IN_DEV, SUBMIT_TEST), PENDING_TEST,
			new StatusEvent(PENDING_TEST, TEST_PASS), COMPLETED,
			new StatusEvent(PENDING_TEST, TEST_REJECT), IN_DEV);

	public DevTaskStatusEnum transit(DevTaskStatusEnum current, DevTaskEventEnum event) {
		DevTaskStatusEnum next = TRANSITIONS.get(new StatusEvent(current, event));
		if (next == null) {
			throw new CheckedException(
					"非法状态转移: " + current.getDescription() + " + " + event.getDescription());
		}
		return next;
	}

}
