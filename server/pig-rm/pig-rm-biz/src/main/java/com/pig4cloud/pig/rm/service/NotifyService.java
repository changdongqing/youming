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

import com.pig4cloud.pig.rm.api.entity.Notify;
import com.pig4cloud.pig.rm.mapper.NotifyMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 消息通知服务（阶段 1.3 占位实现，阶段 1.6 补全全部通知渠道与超期预警）
 *
 * @author youming
 * @date 2026-07-29
 */
@Slf4j
@Service
@AllArgsConstructor
public class NotifyService {

	private final NotifyMapper notifyMapper;

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
		notifyMapper.insert(notify);
		log.debug("通知已发送: userId={}, type={}, title={}", userId, notifyType, title);
	}

}
