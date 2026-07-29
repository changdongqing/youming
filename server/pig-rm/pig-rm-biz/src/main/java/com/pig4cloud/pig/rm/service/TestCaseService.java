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

package com.pig4cloud.pig.rm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.rm.api.entity.TestCase;

/**
 * 测试用例服务接口
 *
 * @author youming
 * @date 2026-07-29
 */
public interface TestCaseService extends IService<TestCase> {

	/** 用例分页（支持按需求筛选） */
	IPage<TestCase> page(Page page, Long requirementId, String keyword, String status);

	/** 新增用例（自动编号，关联需求） */
	R saveCase(TestCase testCase);

	/** 编辑用例 */
	R updateCase(TestCase testCase);

	/** 删除用例（仅草稿/已废弃可删） */
	R removeCase(Long id);

	/**
	 * 用例覆盖率统计
	 * 覆盖率 = 已关联用例的需求数 / 需求总数（按状态筛选开发中及以上）
	 */
	R coverage();

}
