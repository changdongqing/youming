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
 * 开发任务事件枚举
 *
 * @author youming
 * @date 2026-07-29
 */
@Getter
@AllArgsConstructor
public enum DevTaskEventEnum {

	START_DEV("开始开发"),
	SUBMIT_TEST("开发完成提测"),
	TEST_PASS("测试通过"),
	TEST_REJECT("测试驳回");

	private final String description;

}
