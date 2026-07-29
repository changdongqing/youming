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
import com.pig4cloud.pig.rm.api.dto.BugSaveDTO;
import com.pig4cloud.pig.rm.api.dto.TestExecutionDTO;
import com.pig4cloud.pig.rm.api.dto.TestWorkloadDTO;
import com.pig4cloud.pig.rm.api.entity.Bug;
import com.pig4cloud.pig.rm.api.entity.DevTask;
import com.pig4cloud.pig.rm.api.entity.Requirement;
import com.pig4cloud.pig.rm.api.entity.TestExecution;
import com.pig4cloud.pig.rm.api.entity.TestTask;
import com.pig4cloud.pig.rm.api.vo.TestTaskDetailVO;
import com.pig4cloud.pig.rm.mapper.BugMapper;
import com.pig4cloud.pig.rm.mapper.SysUserMapper;
import com.pig4cloud.pig.rm.mapper.TestExecutionMapper;
import com.pig4cloud.pig.rm.mapper.TestTaskMapper;
import com.pig4cloud.pig.rm.service.CodeGeneratorService;
import com.pig4cloud.pig.rm.service.DevTaskService;
import com.pig4cloud.pig.rm.service.NotifyService;
import com.pig4cloud.pig.rm.service.RequirementService;
import com.pig4cloud.pig.rm.service.TestTaskService;
import com.pig4cloud.pig.rm.service.TodoService;
import com.pig4cloud.pig.rm.statemachine.DevTaskStatusEnum;
import com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 测试任务单服务实现（全链路状态联动枢纽）
 *
 * @author youming
 * @date 2026-07-29
 */
@Slf4j
@Service
@AllArgsConstructor
public class TestTaskServiceImpl extends ServiceImpl<TestTaskMapper, TestTask> implements TestTaskService {

	private final CodeGeneratorService codeGenerator;

	private final BugMapper bugMapper;

	private final TestExecutionMapper executionMapper;

	private final SysUserMapper sysUserMapper;

	private final DevTaskService devTaskService;

	private final RequirementService requirementService;

	private final TodoService todoService;

	private final NotifyService notifyService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R createFromDevTask(DevTask devTask) {
		TestTask task = new TestTask();
		task.setTaskCode(codeGenerator.next("TST"));
		task.setDevTaskId(devTask.getId());
		task.setRequirementId(devTask.getRequirementId());
		task.setStatus("PENDING_TEST");
		save(task);

		// 为测试人员生成待办
		List<Long> testerIds = sysUserMapper.getUserIdsByRoleCode("rm_tester");
		for (Long testerId : testerIds) {
			todoService.createTodo("TEST", "TEST_TASK", task.getId(),
					"待测试：" + task.getTaskCode() + "（来自 " + devTask.getTaskCode() + "）",
					"/admin/rm/test/task/detail?id=" + task.getId(), testerId);
		}
		return R.ok(task);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R execute(TestExecutionDTO dto) {
		TestTask task = getById(dto.getTestTaskId());
		if (task == null) {
			return R.failed("测试任务单不存在");
		}

		// 如果任务还是待测试状态，自动流转为测试中
		if ("PENDING_TEST".equals(task.getStatus())) {
			task.setStatus("IN_TEST");
			task.setTestStartTime(LocalDateTime.now());
			updateById(task);
		}

		// 保存执行记录
		TestExecution exec = new TestExecution();
		exec.setTestTaskId(dto.getTestTaskId());
		exec.setCaseId(dto.getCaseId());
		exec.setResult(dto.getResult());
		exec.setRemark(dto.getRemark());
		exec.setExecuteTime(LocalDateTime.now());
		exec.setExecutorId(SecurityUtils.getUser().getId());
		executionMapper.insert(exec);
		return R.ok(exec);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R toBug(Long executionId, BugSaveDTO bugDto) {
		TestExecution exec = executionMapper.selectById(executionId);
		if (exec == null) {
			return R.failed("执行记录不存在");
		}
		if (!"FAIL".equals(exec.getResult())) {
			return R.failed("仅失败用例可转bug");
		}

		// 创建 bug
		Bug bug = new Bug();
		bug.setBugCode(codeGenerator.next("BUG"));
		bug.setTitle(bugDto.getTitle());
		bug.setTestTaskId(exec.getTestTaskId());
		bug.setExecutionId(executionId);
		// 冗余需求ID，便于统计
		TestTask task = getById(exec.getTestTaskId());
		if (task != null) {
			bug.setRequirementId(task.getRequirementId());
		}
		bug.setSource("TEST"); // 测试发现的bug
		bug.setSeverity(StrUtil.isBlank(bugDto.getSeverity()) ? "MAJOR" : bugDto.getSeverity());
		bug.setAssigneeId(bugDto.getAssigneeId());
		bug.setStatus("NEW");
		bug.setCreateDate(LocalDate.now());
		bugMapper.insert(bug);

		// 回填执行记录的 bug_id
		exec.setBugId(bug.getId());
		executionMapper.updateById(exec);

		return R.ok(bug);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R pass(Long testTaskId) {
		TestTask task = getById(testTaskId);
		if (task == null) {
			return R.failed("测试任务单不存在");
		}

		// 校验：是否有未解决的 bug
		long unresolvedBugs = bugMapper.selectCount(Wrappers.<Bug>lambdaQuery()
			.eq(Bug::getTestTaskId, testTaskId)
			.notIn(Bug::getStatus, "CLOSED", "VERIFIED"));
		if (unresolvedBugs > 0) {
			return R.failed("存在 " + unresolvedBugs + " 个未解决bug，不可通过测试");
		}

		task.setStatus("PASS");
		task.setConclusion("PASS");
		task.setTestEndTime(LocalDateTime.now());
		updateById(task);
		todoService.closeTodo("TEST_TASK", task.getId(), "TEST");

		// 联动开发任务状态→COMPLETED
		devTaskService.completeTask(task.getDevTaskId());

		return R.ok("测试通过");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R reject(Long testTaskId, String remark) {
		TestTask task = getById(testTaskId);
		if (task == null) {
			return R.failed("测试任务单不存在");
		}

		// 统计关联 bug 数量
		long bugCount = bugMapper.selectCount(Wrappers.<Bug>lambdaQuery().eq(Bug::getTestTaskId, testTaskId));

		task.setStatus("FAIL");
		task.setConclusion("FAIL");
		task.setTestEndTime(LocalDateTime.now());
		updateById(task);
		todoService.closeTodo("TEST_TASK", task.getId(), "TEST");

		// 联动开发任务状态→返工（IN_DEV）
		devTaskService.rejectToRework(task.getDevTaskId());

		// 通知开发人员（含 bug 数量）
		DevTask devTask = devTaskService.getById(task.getDevTaskId());
		if (devTask != null && devTask.getAssigneeId() != null) {
			String msg = String.format("测试未通过，关联bug %d 个：%s", bugCount, remark != null ? remark : "");
			notifyService.notifyUser(devTask.getAssigneeId(), "REJECT",
					"测试驳回：" + task.getTaskCode(), msg);
		}

		return R.ok(Map.of("bugCount", bugCount, "message", "测试已驳回，关联bug " + bugCount + " 个"));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveWorkload(TestWorkloadDTO dto) {
		TestTask task = getById(dto.getTestTaskId());
		if (task == null) {
			return R.failed("测试任务单不存在");
		}
		task.setWorkload(dto.getWorkload());
		updateById(task);
		return R.ok("工作量已保存");
	}

	@Override
	public IPage<TestTask> page(Page page, Long devTaskId, Long testerId, String status) {
		return baseMapper.selectPage(page,
				Wrappers.<TestTask>lambdaQuery()
					.eq(devTaskId != null, TestTask::getDevTaskId, devTaskId)
					.eq(testerId != null, TestTask::getTesterId, testerId)
					.eq(StrUtil.isNotBlank(status), TestTask::getStatus, status)
					.orderByDesc(TestTask::getCreateTime));
	}

	@Override
	public R detail(Long id) {
		TestTask task = getById(id);
		if (task == null) {
			return R.failed("测试任务单不存在");
		}
		TestTaskDetailVO vo = new TestTaskDetailVO();
		BeanUtil.copyProperties(task, vo);

		// 填充执行记录列表
		List<TestExecution> executions = executionMapper.selectList(Wrappers.<TestExecution>lambdaQuery()
			.eq(TestExecution::getTestTaskId, id)
			.orderByAsc(TestExecution::getExecuteTime));
		vo.setExecutions(executions);

		// 填充 bug 列表
		List<Bug> bugs = bugMapper.selectList(Wrappers.<Bug>lambdaQuery()
			.eq(Bug::getTestTaskId, id)
			.orderByAsc(Bug::getCreateDate));
		vo.setBugs(bugs);

		// 统计 bug 数量
		vo.setBugCount((long) bugs.size());
		vo.setUnresolvedBugCount(bugs.stream()
			.filter(b -> !"CLOSED".equals(b.getStatus()) && !"VERIFIED".equals(b.getStatus()))
			.count());

		// 填充需求编号、开发任务编号
		if (task.getRequirementId() != null) {
			Requirement req = requirementService.getById(task.getRequirementId());
			if (req != null) {
				vo.setRequirementCode(req.getReqCode());
			}
		}
		if (task.getDevTaskId() != null) {
			DevTask devTask = devTaskService.getById(task.getDevTaskId());
			if (devTask != null) {
				vo.setDevTaskCode(devTask.getTaskCode());
			}
		}

		return R.ok(vo);
	}

}
