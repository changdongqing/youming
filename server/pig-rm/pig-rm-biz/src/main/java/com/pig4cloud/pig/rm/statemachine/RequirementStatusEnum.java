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
 * 需求状态枚举（对应 PRD 3.6 状态机）
 *
 * @author youming
 * @date 2026-07-29
 */
@Getter
@AllArgsConstructor
public enum RequirementStatusEnum {

	DRAFT("草稿"),
	PENDING_APPROVAL("待审批"),
	REJECTED("已驳回"),
	PENDING_REVIEW("待评审"),
	DESIGNING("设计中"),
	DESIGN_REVIEW("设计评审中"),
	SCHEDULING("排期中"),
	DEVELOPING("开发中"),
	TESTING("测试中"),
	PENDING_ACCEPTANCE("待验收"),
	COMPLETED("已完成"),
	RELEASED("已发布");

	private final String description;

}
