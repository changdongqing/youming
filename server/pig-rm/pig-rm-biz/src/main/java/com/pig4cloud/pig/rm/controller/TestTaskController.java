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
import com.pig4cloud.pig.rm.api.dto.BugSaveDTO;
import com.pig4cloud.pig.rm.api.dto.TestExecutionDTO;
import com.pig4cloud.pig.rm.api.dto.TestWorkloadDTO;
import com.pig4cloud.pig.rm.api.entity.TestTask;
import com.pig4cloud.pig.rm.service.TestTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 测试任务单
 *
 * @author youming
 * @date 2026-07-29
 */
@RestController
@AllArgsConstructor
@Tag(name = "测试任务单", description = "测试执行/结果记录/通过驳回")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class TestTaskController {

	private final TestTaskService testTaskService;

	@GetMapping("/rm/test-task/page")
	@Operation(summary = "测试任务单分页")
	@HasPermission("rm_case_view")
	public R<IPage<TestTask>> page(@ParameterObject Page page,
			@RequestParam(required = false) Long devTaskId,
			@RequestParam(required = false) Long testerId,
			@RequestParam(required = false) String status) {
		return R.ok(testTaskService.page(page, devTaskId, testerId, status));
	}

	@GetMapping("/rm/test-task/{id}")
	@Operation(summary = "测试任务单详情")
	@HasPermission("rm_case_view")
	public R detail(@PathVariable Long id) {
		return testTaskService.detail(id);
	}

	@SysLog("测试执行")
	@PostMapping("/rm/test-task/execute")
	@Operation(summary = "执行用例并记录结果")
	@HasPermission("rm_tst_execute")
	public R execute(@Valid @RequestBody TestExecutionDTO dto) {
		return testTaskService.execute(dto);
	}

	@SysLog("转Bug")
	@PostMapping("/rm/test-task/to-bug")
	@Operation(summary = "失败用例一键转bug")
	@HasPermission("rm_tst_bug")
	public R toBug(@RequestParam Long executionId, @Valid @RequestBody BugSaveDTO bugDto) {
		return testTaskService.toBug(executionId, bugDto);
	}

	@SysLog("测试通过")
	@PostMapping("/rm/test-task/pass/{id}")
	@Operation(summary = "测试通过")
	@HasPermission("rm_tst_execute")
	public R pass(@PathVariable Long id) {
		return testTaskService.pass(id);
	}

	@SysLog("测试驳回")
	@PostMapping("/rm/test-task/reject/{id}")
	@Operation(summary = "测试驳回（自动关联bug数量）")
	@HasPermission("rm_tst_execute")
	public R reject(@PathVariable Long id, @RequestParam(required = false) String remark) {
		return testTaskService.reject(id, remark);
	}

	@SysLog("填报测试工作量")
	@PostMapping("/rm/test-task/workload")
	@Operation(summary = "填报测试工作量")
	@HasPermission("rm_tst_execute")
	public R workload(@Valid @RequestBody TestWorkloadDTO dto) {
		return testTaskService.saveWorkload(dto);
	}

}
