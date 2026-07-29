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

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.rm.api.dto.BugHandleDTO;
import com.pig4cloud.pig.rm.api.entity.Bug;
import com.pig4cloud.pig.rm.mapper.BugMapper;
import com.pig4cloud.pig.rm.service.BugService;
import com.pig4cloud.pig.rm.service.NotifyService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bug 服务实现
 *
 * @author youming
 * @date 2026-07-29
 */
@Service
@AllArgsConstructor
public class BugServiceImpl extends ServiceImpl<BugMapper, Bug> implements BugService {

	private final NotifyService notifyService;

	@Override
	public IPage<Bug> page(Page page, String source, String status, Long assigneeId, LocalDate startDate,
			LocalDate endDate) {
		return baseMapper.selectPage(page,
				Wrappers.<Bug>lambdaQuery()
					.eq(StrUtil.isNotBlank(source), Bug::getSource, source)
					.eq(StrUtil.isNotBlank(status), Bug::getStatus, status)
					.eq(assigneeId != null, Bug::getAssigneeId, assigneeId)
					.ge(startDate != null, Bug::getCreateDate, startDate)
					.le(endDate != null, Bug::getCreateDate, endDate)
					.orderByDesc(Bug::getCreateDate));
	}

	@Override
	public R detail(Long id) {
		Bug bug = getById(id);
		if (bug == null) {
			return R.failed("bug不存在");
		}
		return R.ok(bug);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R handle(BugHandleDTO dto) {
		Bug bug = getById(dto.getBugId());
		if (bug == null) {
			return R.failed("bug不存在");
		}
		String targetStatus = dto.getTargetStatus();
		bug.setStatus(targetStatus);
		// 如果标记为已解决，记录解决时间
		if ("RESOLVED".equals(targetStatus) || "VERIFIED".equals(targetStatus) || "CLOSED".equals(targetStatus)) {
			bug.setResolveTime(LocalDateTime.now());
		}
		updateById(bug);

		// 通知处理人/提报人 bug 状态变更
		if (bug.getAssigneeId() != null) {
			notifyService.notifyUser(bug.getAssigneeId(), "APPROVAL",
					"Bug状态更新：" + bug.getBugCode(), "状态变更为：" + targetStatus);
		}
		return R.ok("bug状态已更新");
	}

}
