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
import com.pig4cloud.pig.rm.api.dto.DevTaskDesignDTO;
import com.pig4cloud.pig.rm.api.dto.DevTaskQueryDTO;
import com.pig4cloud.pig.rm.api.dto.DevTaskReviewDTO;
import com.pig4cloud.pig.rm.api.dto.DevTaskSaveDTO;
import com.pig4cloud.pig.rm.api.entity.DevTask;

import java.util.List;

/**
 * 开发任务单服务接口
 *
 * @author youming
 * @date 2026-07-29
 */
public interface DevTaskService extends IService<DevTask> {

	/** 分页查询（支持按需求ID筛选） */
	IPage<DevTask> page(Page page, DevTaskQueryDTO query);

	/** 任务详情（含需求概要、详细设计、评审记录） */
	R detail(Long id);

	/**
	 * 任务分解（批量创建任务单）
	 * PRD DEV-01：开发经理将需求分解为多个任务单
	 */
	R createTasks(Long requirementId, List<DevTaskSaveDTO> tasks);

	/** 编辑任务 */
	R updateTask(DevTask devTask);

	/** 保存详细设计（DEV-02） */
	R saveDetailDesign(DevTaskDesignDTO dto);

	/** 提交详细设计评审 */
	R submitDesignReview(Long taskId);

	/** 详细设计评审（DEV-03，领导通过/驳回） */
	R designReview(DevTaskReviewDTO dto);

	/** 开始开发（PENDING_DEV→IN_DEV） */
	R startDev(Long taskId);

	/**
	 * 开发完成提测（DEV-04）
	 * IN_DEV→PENDING_TEST，通知测试人员（阶段 1.5 自动生成测试任务单）
	 */
	R submitTest(Long taskId);

	/**
	 * 测试驳回联动：任务返工（PENDING_TEST→IN_DEV）
	 * 阶段 1.5 调用
	 */
	R rejectToRework(Long taskId);

	/**
	 * 测试通过联动：任务完成（PENDING_TEST→COMPLETED）
	 * 阶段 1.5 调用
	 */
	R completeTask(Long taskId);

	/**
	 * 检查需求下全部开发任务是否完成
	 * 如果全部完成，触发需求状态 DEVELOPING→TESTING
	 */
	void checkAllTasksCompleted(Long requirementId);

}
