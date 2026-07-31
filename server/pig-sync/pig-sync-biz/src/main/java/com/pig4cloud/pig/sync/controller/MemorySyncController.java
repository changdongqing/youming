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

package com.pig4cloud.pig.sync.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.sync.api.dto.MemoryUploadDTO;
import com.pig4cloud.pig.sync.api.vo.MemoryMetaVO;
import com.pig4cloud.pig.sync.api.vo.MemoryPullVO;
import com.pig4cloud.pig.sync.api.vo.MemoryUploadResultVO;
import com.pig4cloud.pig.sync.service.MemorySyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 记忆同步（QwenPaw 阶段2，P1）
 * <p>
 * 所有端点需要 Bearer token 认证，从 SecurityContext 提取 userId 做数据隔离。
 *
 * @author youming
 * @date 2026-07-31
 */
@RestController
@AllArgsConstructor
@Tag(name = "记忆同步", description = "QwenPaw 记忆增量下行/幂等上行/对账")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class MemorySyncController {

	private final MemorySyncService memorySyncService;

	@GetMapping("/sync/memory")
	@Operation(summary = "记忆增量下行（GET /sync/memory?since=&limit=）")
	public R<MemoryPullVO> pull(@RequestParam(defaultValue = "0") long since,
			@RequestParam(defaultValue = "500") int limit) {
		String userId = String.valueOf(SecurityUtils.getUser().getId());
		return R.ok(memorySyncService.pullIncremental(userId, since, limit));
	}

	@PostMapping("/sync/memory")
	@Operation(summary = "记忆批量上行（幂等 ON CONFLICT DO NOTHING）")
	public R<MemoryUploadResultVO> upload(@RequestBody MemoryUploadDTO request) {
		String userId = String.valueOf(SecurityUtils.getUser().getId());
		return R.ok(memorySyncService.upload(userId, request));
	}

	@GetMapping("/sync/memory/meta")
	@Operation(summary = "记忆对账元数据（服务端最新 seq）")
	public R<MemoryMetaVO> meta() {
		String userId = String.valueOf(SecurityUtils.getUser().getId());
		return R.ok(memorySyncService.meta(userId));
	}

}
