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

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.rm.api.entity.Notify;
import com.pig4cloud.pig.rm.mapper.NotifyMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 消息通知服务
 *
 * @author youming
 * @date 2026-07-29
 */
@Slf4j
@Service
@AllArgsConstructor
public class NotifyService extends ServiceImpl<NotifyMapper, Notify> {

	/**
	 * 通知指定用户
	 */
	@Transactional(rollbackFor = Exception.class)
	public void notifyUser(Long userId, String notifyType, String title, String content) {
		Notify notify = new Notify();
		notify.setUserId(userId);
		notify.setNotifyType(notifyType);
		notify.setTitle(title);
		notify.setContent(content);
		notify.setIsRead("0");
		save(notify);
		log.debug("通知已发送: userId={}, type={}, title={}", userId, notifyType, title);
	}

	/**
	 * 我的通知（分页）
	 */
	public IPage<Notify> myNotify(Page page, String notifyType, String isRead) {
		Long userId = SecurityUtils.getUser().getId();
		return page(page,
				Wrappers.<Notify>lambdaQuery()
					.eq(Notify::getUserId, userId)
					.eq(StrUtil.isNotBlank(notifyType), Notify::getNotifyType, notifyType)
					.eq(StrUtil.isNotBlank(isRead), Notify::getIsRead, isRead)
					.orderByDesc(Notify::getCreateTime));
	}

	/**
	 * 标记已读
	 */
	public R markRead(Long id) {
		Notify notify = getById(id);
		if (notify == null) {
			return R.failed("通知不存在");
		}
		notify.setIsRead("1");
		updateById(notify);
		return R.ok();
	}

	/**
	 * 全部已读
	 */
	@Transactional(rollbackFor = Exception.class)
	public R markAllRead() {
		Long userId = SecurityUtils.getUser().getId();
		update(Wrappers.<Notify>lambdaUpdate()
			.eq(Notify::getUserId, userId)
			.eq(Notify::getIsRead, "0")
			.set(Notify::getIsRead, "1"));
		return R.ok();
	}

}
