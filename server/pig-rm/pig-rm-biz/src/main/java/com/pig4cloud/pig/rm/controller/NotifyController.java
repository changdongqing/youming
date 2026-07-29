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

package com.pig4cloud.pig.rm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.rm.api.entity.Notify;
import com.pig4cloud.pig.rm.service.NotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 消息通知
 *
 * @author youming
 * @date 2026-07-29
 */
@RestController
@AllArgsConstructor
@Tag(name = "消息通知", description = "站内信/已读")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class NotifyController {

	private final NotifyService notifyService;

	@GetMapping("/rm/notify/my")
	@Operation(summary = "我的通知（分页）")
	@HasPermission("rm_todo")
	public R<IPage<Notify>> myNotify(@ParameterObject Page page,
			@RequestParam(required = false) String notifyType,
			@RequestParam(required = false) String isRead) {
		return R.ok(notifyService.myNotify(page, notifyType, isRead));
	}

	@PutMapping("/rm/notify/read/{id}")
	@Operation(summary = "标记已读")
	@HasPermission("rm_todo")
	public R markRead(@PathVariable Long id) {
		return notifyService.markRead(id);
	}

	@PutMapping("/rm/notify/read-all")
	@Operation(summary = "全部已读")
	@HasPermission("rm_todo")
	public R markAllRead() {
		return notifyService.markAllRead();
	}

}
