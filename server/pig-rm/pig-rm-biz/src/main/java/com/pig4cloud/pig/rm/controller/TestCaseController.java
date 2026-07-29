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
import com.pig4cloud.pig.rm.api.entity.TestCase;
import com.pig4cloud.pig.rm.service.TestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 测试用例库
 *
 * @author youming
 * @date 2026-07-29
 */
@RestController
@AllArgsConstructor
@Tag(name = "测试用例库", description = "用例编写/维护/覆盖率")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class TestCaseController {

	private final TestCaseService testCaseService;

	@GetMapping("/rm/test-case/page")
	@Operation(summary = "用例分页")
	@HasPermission("rm_case_view")
	public R<IPage<TestCase>> page(@ParameterObject Page page,
			@RequestParam(required = false) Long requirementId,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String status) {
		return R.ok(testCaseService.page(page, requirementId, keyword, status));
	}

	@SysLog("新增用例")
	@PostMapping("/rm/test-case")
	@Operation(summary = "新增用例（关联需求）")
	@HasPermission("rm_case_add")
	public R save(@Valid @RequestBody TestCase testCase) {
		return testCaseService.saveCase(testCase);
	}

	@SysLog("编辑用例")
	@PutMapping("/rm/test-case")
	@Operation(summary = "编辑用例")
	@HasPermission("rm_case_edit")
	public R update(@Valid @RequestBody TestCase testCase) {
		return testCaseService.updateCase(testCase);
	}

	@SysLog("删除用例")
	@DeleteMapping("/rm/test-case/{id}")
	@Operation(summary = "删除用例")
	@HasPermission("rm_case_del")
	public R remove(@PathVariable Long id) {
		return testCaseService.removeCase(id);
	}

	@GetMapping("/rm/test-case/coverage")
	@Operation(summary = "用例覆盖率统计")
	@HasPermission("rm_report")
	public R coverage() {
		return testCaseService.coverage();
	}

}
