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
import com.pig4cloud.pig.rm.api.entity.Bug;
import com.pig4cloud.pig.rm.mapper.BugMapper;
import com.pig4cloud.pig.rm.service.NotifyService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 当日 Bug 提醒定时任务
 * <p>
 * 每日定时扫描未处理的当日 bug，通知处理人。
 * 原则：bug 不隔人天（当日 bug 当日处理）。
 *
 * @author youming
 * @date 2026-07-29
 */
@Slf4j
@Component
@AllArgsConstructor
public class BugDailyReminderJob {

	private final BugMapper bugMapper;

	private final NotifyService notifyService;

	/**
	 * 每日 17:00 执行（下班前提醒当日未处理 bug）
	 */
	@Scheduled(cron = "0 0 17 * * ?")
	public void execute() {
		log.info("开始执行当日bug提醒任务...");

		// 查询当日未关闭的 bug
		List<Bug> todayBugs = bugMapper.selectList(Wrappers.<Bug>lambdaQuery()
			.eq(Bug::getCreateDate, LocalDate.now())
			.notIn(Bug::getStatus, "CLOSED")
			.isNotNull(Bug::getAssigneeId));

		if (todayBugs.isEmpty()) {
			log.info("当日无未处理bug，跳过");
			return;
		}

		// 按处理人分组
		Map<Long, List<Bug>> byAssignee = todayBugs.stream()
			.collect(Collectors.groupingBy(Bug::getAssigneeId));

		// 为每个处理人生成通知
		byAssignee.forEach((userId, bugs) -> {
			String content = String.format("您有 %d 个当日bug未处理（bug不隔人天）：%s",
					bugs.size(),
					bugs.stream()
						.map(b -> b.getBugCode() + ":" + b.getTitle())
						.collect(Collectors.joining(", ")));
			notifyService.notifyUser(userId, "BUG_DAILY",
					"当日bug未处理提醒", content);
		});

		log.info("当日bug提醒任务完成，通知了 {} 位处理人", byAssignee.size());
	}

}
