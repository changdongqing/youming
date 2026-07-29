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
import com.pig4cloud.pig.rm.api.dto.BugSaveDTO;
import com.pig4cloud.pig.rm.api.dto.TestExecutionDTO;
import com.pig4cloud.pig.rm.api.dto.TestWorkloadDTO;
import com.pig4cloud.pig.rm.api.entity.Bug;
import com.pig4cloud.pig.rm.api.entity.DevTask;
import com.pig4cloud.pig.rm.api.entity.TestTask;

/**
 * 测试任务单服务接口
 *
 * @author youming
 * @date 2026-07-29
 */
public interface TestTaskService extends IService<TestTask> {

	/** 测试任务单分页 */
	IPage<TestTask> page(Page page, Long devTaskId, Long testerId, String status);

	/** 测试任务单详情（含执行记录、bug列表） */
	R detail(Long id);

	/**
	 * 从开发任务单创建测试任务单（开发完成提测时自动调用）
	 * PRD TST-02：开发完成提测后自动生成
	 */
	R createFromDevTask(DevTask devTask);

	/**
	 * 执行用例并记录结果
	 * PRD TST-03：按用例逐条执行并记录结果（通过/失败/阻塞）
	 */
	R execute(TestExecutionDTO dto);

	/**
	 * 失败用例一键转 bug
	 * PRD TST-04：失败用例一键转 bug
	 */
	R toBug(Long executionId, BugSaveDTO bugDto);

	/** 测试通过 */
	R pass(Long testTaskId);

	/**
	 * 测试驳回（自动关联测试任务单中的 bug 数量）
	 * PRD TST-05：驳回时自动关联 bug 数量
	 */
	R reject(Long testTaskId, String remark);

	/** 填报测试工作量（考核点5） */
	R saveWorkload(TestWorkloadDTO dto);

}
