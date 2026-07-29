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

package com.pig4cloud.pig.rm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.rm.api.dto.DevTaskDesignDTO;
import com.pig4cloud.pig.rm.api.dto.DevTaskQueryDTO;
import com.pig4cloud.pig.rm.api.dto.DevTaskReviewDTO;
import com.pig4cloud.pig.rm.api.dto.DevTaskSaveDTO;
import com.pig4cloud.pig.rm.api.entity.ApprovalRecord;
import com.pig4cloud.pig.rm.api.entity.DevTask;
import com.pig4cloud.pig.rm.api.entity.Requirement;
import com.pig4cloud.pig.rm.api.vo.DevTaskDetailVO;
import com.pig4cloud.pig.rm.mapper.ApprovalRecordMapper;
import com.pig4cloud.pig.rm.mapper.DevTaskMapper;
import com.pig4cloud.pig.rm.mapper.SysUserMapper;
import com.pig4cloud.pig.rm.service.CodeGeneratorService;
import com.pig4cloud.pig.rm.service.DevTaskService;
import com.pig4cloud.pig.rm.service.NotifyService;
import com.pig4cloud.pig.rm.service.RequirementService;
import com.pig4cloud.pig.rm.service.TodoService;
import com.pig4cloud.pig.rm.statemachine.DevTaskStateMachine;
import com.pig4cloud.pig.rm.statemachine.DevTaskStatusEnum;
import com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 开发任务单服务实现
 *
 * @author youming
 * @date 2026-07-29
 */
@Slf4j
@Service
@AllArgsConstructor
public class DevTaskServiceImpl extends ServiceImpl<DevTaskMapper, DevTask> implements DevTaskService {

	private final CodeGeneratorService codeGenerator;

	private final DevTaskStateMachine stateMachine;

	private final RequirementService requirementService;

	private final ApprovalRecordMapper approvalRecordMapper;

	private final SysUserMapper sysUserMapper;

	private final TodoService todoService;

	private final NotifyService notifyService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R createTasks(Long requirementId, List<DevTaskSaveDTO> tasks) {
		// 1. 校验需求存在且处于可分解状态
		Requirement req = requirementService.getById(requirementId);
		if (req == null) {
			return R.failed("需求不存在");
		}
		String reqStatus = req.getStatus();
		if (!RequirementStatusEnum.SCHEDULING.name().equals(reqStatus)
				&& !RequirementStatusEnum.DEVELOPING.name().equals(reqStatus)) {
			return R.failed("需求当前状态不可分解任务");
		}

		// 2. 批量创建任务
		List<DevTask> entities = new ArrayList<>();
		for (DevTaskSaveDTO dto : tasks) {
			DevTask task = new DevTask();
			task.setTaskCode(codeGenerator.next("DEV"));
			task.setRequirementId(requirementId);
			task.setTaskName(dto.getTaskName());
			task.setTaskDesc(dto.getTaskDesc());
			task.setAssigneeId(dto.getAssigneeId());
			task.setPlanStartDate(dto.getPlanStartDate());
			task.setPlanEndDate(dto.getPlanEndDate());
			task.setReviewConclusion("PENDING");
			task.setStatus(DevTaskStatusEnum.PENDING_DEV.name());
			entities.add(task);
		}
		saveBatch(entities);

		// 3. 如果需求还处于排期中，自动流转到开发中
		if (RequirementStatusEnum.SCHEDULING.name().equals(reqStatus)) {
			req.setStatus(RequirementStatusEnum.DEVELOPING.name());
			requirementService.updateById(req);
		}

		// 4. 为每个任务负责人创建开发待办
		for (DevTask task : entities) {
			if (task.getAssigneeId() != null) {
				todoService.createTodo("DEV", "DEV_TASK", task.getId(),
						"待开发：" + task.getTaskCode() + " " + task.getTaskName(),
						"/admin/rm/dev-task/detail?id=" + task.getId(), task.getAssigneeId());
			}
		}

		return R.ok(entities);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateTask(DevTask devTask) {
		DevTask existing = getById(devTask.getId());
		if (existing == null) {
			return R.failed("任务不存在");
		}
		if (!DevTaskStatusEnum.PENDING_DEV.name().equals(existing.getStatus())) {
			return R.failed("仅待开发状态可编辑");
		}
		updateById(devTask);
		return R.ok();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveDetailDesign(DevTaskDesignDTO dto) {
		DevTask task = getById(dto.getTaskId());
		if (task == null) {
			return R.failed("任务不存在");
		}
		// 详细设计可在待开发/开发中状态编制
		if (!DevTaskStatusEnum.PENDING_DEV.name().equals(task.getStatus())
				&& !DevTaskStatusEnum.IN_DEV.name().equals(task.getStatus())) {
			return R.failed("当前状态不可编制详细设计");
		}
		task.setDetailDesign(dto.getDetailDesign());
		task.setReviewConclusion("PENDING");
		updateById(task);
		return R.ok("详细设计已保存");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R submitDesignReview(Long taskId) {
		DevTask task = getById(taskId);
		if (task == null) {
			return R.failed("任务不存在");
		}
		if (StrUtil.isBlank(task.getDetailDesign())) {
			return R.failed("请先编制详细设计");
		}
		task.setReviewConclusion("PENDING");
		updateById(task);

		// 生成设计评审待办给领导
		List<Long> leaderIds = sysUserMapper.getUserIdsByRoleCode("rm_leader");
		for (Long leaderId : leaderIds) {
			todoService.createTodo("REVIEW", "DEV_TASK", taskId,
					"待详细设计评审：" + task.getTaskCode(),
					"/admin/rm/dev-task/detail?id=" + taskId, leaderId);
		}
		return R.ok("已提交详细设计评审");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R designReview(DevTaskReviewDTO dto) {
		DevTask task = getById(dto.getTaskId());
		if (task == null) {
			return R.failed("任务不存在");
		}
		if (!"PENDING".equals(task.getReviewConclusion())) {
			return R.failed("该任务不在待评审状态");
		}

		// 记录评审记录
		ApprovalRecord record = new ApprovalRecord();
		record.setBillType("DEV_TASK");
		record.setBillId(task.getId());
		record.setNodeCode("DESIGN_REVIEW");
		record.setApproverId(SecurityUtils.getUser().getId());
		record.setConclusion(dto.getConclusion());
		record.setOpinion(dto.getOpinion());
		record.setApproveTime(LocalDateTime.now());
		approvalRecordMapper.insert(record);
		todoService.closeTodo("DEV_TASK", task.getId(), "REVIEW");

		if ("PASS".equals(dto.getConclusion())) {
			task.setReviewConclusion("PASS");
			task.setReviewRemark(dto.getOpinion());
			// 评审通过后可开始开发
			if (DevTaskStatusEnum.PENDING_DEV.name().equals(task.getStatus())) {
				task.setStatus(DevTaskStatusEnum.IN_DEV.name());
				task.setActualStartDate(LocalDate.now());
			}
			updateById(task);
			// 通知开发人员
			if (task.getAssigneeId() != null) {
				notifyService.notifyUser(task.getAssigneeId(), "APPROVAL",
						"详细设计评审通过：" + task.getTaskCode(), "可开始开发");
			}
		}
		else {
			task.setReviewConclusion("FAIL");
			task.setReviewRemark(dto.getOpinion());
			updateById(task);
			// 通知开发人员重新设计
			if (task.getAssigneeId() != null) {
				notifyService.notifyUser(task.getAssigneeId(), "REJECT",
						"详细设计评审未通过：" + task.getTaskCode(), dto.getOpinion());
			}
		}
		return R.ok("评审完成");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R startDev(Long taskId) {
		DevTask task = getById(taskId);
		DevTaskStatusEnum next = stateMachine.transit(DevTaskStatusEnum.valueOf(task.getStatus()),
				com.pig4cloud.pig.rm.statemachine.DevTaskEventEnum.START_DEV);
		task.setStatus(next.name());
		task.setActualStartDate(LocalDate.now());
		updateById(task);
		return R.ok("已开始开发");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R submitTest(Long taskId) {
		DevTask task = getById(taskId);
		if (task == null) {
			return R.failed("任务不存在");
		}
		if (!"PASS".equals(task.getReviewConclusion())) {
			return R.failed("详细设计评审未通过，不可提测");
		}
		DevTaskStatusEnum next = stateMachine.transit(DevTaskStatusEnum.valueOf(task.getStatus()),
				com.pig4cloud.pig.rm.statemachine.DevTaskEventEnum.SUBMIT_TEST);
		task.setStatus(next.name());
		task.setActualEndDate(LocalDate.now());
		updateById(task);
		todoService.closeTodo("DEV_TASK", task.getId(), "DEV");

		// 通知测试人员（阶段 1.5 自动生成测试任务单，此处仅通知）
		List<Long> testerIds = sysUserMapper.getUserIdsByRoleCode("rm_tester");
		for (Long testerId : testerIds) {
			notifyService.notifyUser(testerId, "TEST",
					"开发任务已提测：" + task.getTaskCode(), "任务：" + task.getTaskName());
		}
		return R.ok("已提测，已通知测试人员");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R rejectToRework(Long taskId) {
		DevTask task = getById(taskId);
		if (task == null) {
			return R.failed("任务不存在");
		}
		DevTaskStatusEnum next = stateMachine.transit(DevTaskStatusEnum.valueOf(task.getStatus()),
				com.pig4cloud.pig.rm.statemachine.DevTaskEventEnum.TEST_REJECT);
		task.setStatus(next.name());
		updateById(task);
		// 为开发人员重新生成开发待办
		if (task.getAssigneeId() != null) {
			todoService.createTodo("DEV", "DEV_TASK", task.getId(),
					"测试驳回返工：" + task.getTaskCode(),
					"/admin/rm/dev-task/detail?id=" + task.getId(), task.getAssigneeId());
		}
		return R.ok("任务已返工");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R completeTask(Long taskId) {
		DevTask task = getById(taskId);
		if (task == null) {
			return R.failed("任务不存在");
		}
		DevTaskStatusEnum next = stateMachine.transit(DevTaskStatusEnum.valueOf(task.getStatus()),
				com.pig4cloud.pig.rm.statemachine.DevTaskEventEnum.TEST_PASS);
		task.setStatus(next.name());
		updateById(task);
		// 检查需求下全部任务是否完成
		checkAllTasksCompleted(task.getRequirementId());
		return R.ok("任务已完成");
	}

	@Override
	public void checkAllTasksCompleted(Long requirementId) {
		long pending = count(Wrappers.<DevTask>lambdaQuery()
			.eq(DevTask::getRequirementId, requirementId)
			.ne(DevTask::getStatus, DevTaskStatusEnum.COMPLETED.name()));
		if (pending == 0) {
			// 所有任务完成，需求状态变为 TESTING
			Requirement req = requirementService.getById(requirementId);
			if (req != null && RequirementStatusEnum.DEVELOPING.name().equals(req.getStatus())) {
				req.setStatus(RequirementStatusEnum.TESTING.name());
				requirementService.updateById(req);
				// 通知产品经理质量确认
				List<Long> approverIds = sysUserMapper.getUserIdsByRoleCode("rm_product_approver");
				for (Long approverId : approverIds) {
					notifyService.notifyUser(approverId, "APPROVAL",
							"需求待质量确认：" + req.getReqCode(),
							"所有开发任务测试通过，请进行质量确认");
				}
			}
		}
	}

	@Override
	public IPage<DevTask> page(Page page, DevTaskQueryDTO query) {
		return baseMapper.selectPage(page,
				Wrappers.<DevTask>lambdaQuery()
					.eq(query.getRequirementId() != null, DevTask::getRequirementId, query.getRequirementId())
					.eq(query.getAssigneeId() != null, DevTask::getAssigneeId, query.getAssigneeId())
					.eq(StrUtil.isNotBlank(query.getStatus()), DevTask::getStatus, query.getStatus())
					.like(StrUtil.isNotBlank(query.getKeyword()), DevTask::getTaskName, query.getKeyword())
					.orderByDesc(DevTask::getCreateTime));
	}

	@Override
	public R detail(Long id) {
		DevTask task = getById(id);
		if (task == null) {
			return R.failed("任务不存在");
		}
		DevTaskDetailVO vo = new DevTaskDetailVO();
		BeanUtil.copyProperties(task, vo);

		// 填充需求概要
		Requirement req = requirementService.getById(task.getRequirementId());
		if (req != null) {
			vo.setRequirementCode(req.getReqCode());
			vo.setRequirementTitle(req.getTitle());
		}

		// 填充负责人名称
		if (task.getAssigneeId() != null) {
			vo.setAssigneeName(sysUserMapper.getUserName(task.getAssigneeId()));
		}

		// 填充评审记录
		List<ApprovalRecord> records = approvalRecordMapper.selectList(Wrappers.<ApprovalRecord>lambdaQuery()
			.eq(ApprovalRecord::getBillType, "DEV_TASK")
			.eq(ApprovalRecord::getBillId, id)
			.orderByAsc(ApprovalRecord::getApproveTime));
		vo.setReviewRecords(records);

		return R.ok(vo);
	}

}
