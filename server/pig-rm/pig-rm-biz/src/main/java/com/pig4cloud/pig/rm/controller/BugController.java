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
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.rm.api.dto.BugHandleDTO;
import com.pig4cloud.pig.rm.api.entity.Bug;
import com.pig4cloud.pig.rm.service.BugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Bug 管理
 *
 * @author youming
 * @date 2026-07-29
 */
@RestController
@AllArgsConstructor
@Tag(name = "Bug管理", description = "Bug查询/处理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class BugController {

	private final BugService bugService;

	@GetMapping("/rm/bug/page")
	@Operation(summary = "Bug分页")
	@HasPermission("rm_bug_view")
	public R<IPage<Bug>> page(@ParameterObject Page page,
			@RequestParam(required = false) String source,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long assigneeId,
			@RequestParam(required = false) LocalDate startDate,
			@RequestParam(required = false) LocalDate endDate) {
		return R.ok(bugService.page(page, source, status, assigneeId, startDate, endDate));
	}

	@GetMapping("/rm/bug/{id}")
	@Operation(summary = "Bug详情")
	@HasPermission("rm_bug_view")
	public R detail(@PathVariable Long id) {
		return bugService.detail(id);
	}

	@SysLog("Bug处理")
	@PostMapping("/rm/bug/handle")
	@Operation(summary = "Bug处理（状态流转）")
	@HasPermission("rm_bug_handle")
	public R handle(@Valid @RequestBody BugHandleDTO dto) {
		return bugService.handle(dto);
	}

}
