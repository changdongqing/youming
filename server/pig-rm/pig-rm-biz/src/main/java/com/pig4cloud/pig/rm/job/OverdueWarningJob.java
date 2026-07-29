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

package com.pig4cloud.pig.rm.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.rm.api.entity.DevTask;
import com.pig4cloud.pig.rm.api.entity.Requirement;
import com.pig4cloud.pig.rm.api.entity.Todo;
import com.pig4cloud.pig.rm.mapper.DevTaskMapper;
import com.pig4cloud.pig.rm.mapper.RequirementMapper;
import com.pig4cloud.pig.rm.mapper.TodoMapper;
import com.pig4cloud.pig.rm.service.NotifyService;
import com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 超期预警定时任务
 * <p>
 * 每日 09:00 扫描超期待办、超期需求、超期开发任务，生成通知。
 *
 * @author youming
 * @date 2026-07-29
 */
@Slf4j
@Component
@AllArgsConstructor
public class OverdueWarningJob {

	private final TodoMapper todoMapper;

	private final RequirementMapper requirementMapper;

	private final DevTaskMapper devTaskMapper;

	private final NotifyService notifyService;

	/**
	 * 每日 09:00 执行超期预警
	 */
	@Scheduled(cron = "0 0 9 * * ?")
	public void execute() {
		log.info("开始执行超期预警任务...");
		warnOverdueTodos();
		warnOverdueRequirements();
		warnOverdueDevTasks();
		log.info("超期预警任务完成");
	}

	/**
	 * 1. 待办超期：due_time < now() 且 status=PENDING
	 */
	private void warnOverdueTodos() {
		List<Todo> overdueTodos = todoMapper.selectList(Wrappers.<Todo>lambdaQuery()
			.lt(Todo::getDueTime, LocalDateTime.now())
			.eq(Todo::getStatus, "PENDING"));
		for (Todo todo : overdueTodos) {
			notifyService.notifyUser(todo.getUserId(), "OVERDUE",
					"待办超期：" + todo.getTitle(), "您的待办已超期，请尽快处理");
		}
		if (!overdueTodos.isEmpty()) {
			log.info("超期待办 {} 条", overdueTodos.size());
		}
	}

	/**
	 * 2. 需求超期：expect_complete_date < today 且未完成
	 */
	private void warnOverdueRequirements() {
		List<Requirement> overdueReqs = requirementMapper.selectList(Wrappers.<Requirement>lambdaQuery()
			.lt(Requirement::getExpectCompleteDate, LocalDate.now())
			.notIn(Requirement::getStatus, RequirementStatusEnum.COMPLETED.name(),
					RequirementStatusEnum.RELEASED.name()));
		for (Requirement req : overdueReqs) {
			notifyService.notifyUser(req.getInitiatorId(), "OVERDUE",
					"需求超期：" + req.getReqCode(),
					"您的需求《" + req.getTitle() + "》已超过期望完成时间");
			// 通知产品经理
			List<Long> approverIds = todoMapper
				.selectList(Wrappers.<Todo>lambdaQuery().eq(Todo::getBillType, "REQUIREMENT")
					.eq(Todo::getBillId, req.getId())
					.eq(Todo::getTodoType, "APPROVE"))
				.stream()
				.map(Todo::getUserId)
				.distinct()
				.toList();
			for (Long approverId : approverIds) {
				notifyService.notifyUser(approverId, "OVERDUE",
						"需求超期预警：" + req.getReqCode(),
						"需求《" + req.getTitle() + "》已超期，请跟进");
			}
		}
		if (!overdueReqs.isEmpty()) {
			log.info("超期需求 {} 条", overdueReqs.size());
		}
	}

	/**
	 * 3. 开发任务超期：plan_end_date < today 且未完成
	 */
	private void warnOverdueDevTasks() {
		List<DevTask> overdueTasks = devTaskMapper.selectList(Wrappers.<DevTask>lambdaQuery()
			.lt(DevTask::getPlanEndDate, LocalDate.now())
			.ne(DevTask::getStatus, "COMPLETED"));
		for (DevTask task : overdueTasks) {
			if (task.getAssigneeId() != null) {
				notifyService.notifyUser(task.getAssigneeId(), "OVERDUE",
						"开发任务超期：" + task.getTaskCode(),
						"任务《" + task.getTaskName() + "》已超过计划完成时间");
			}
		}
		if (!overdueTasks.isEmpty()) {
			log.info("超期开发任务 {} 条", overdueTasks.size());
		}
	}

}
