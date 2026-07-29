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
import com.pig4cloud.pig.rm.api.dto.DevTaskDesignDTO;
import com.pig4cloud.pig.rm.api.dto.DevTaskQueryDTO;
import com.pig4cloud.pig.rm.api.dto.DevTaskReviewDTO;
import com.pig4cloud.pig.rm.api.dto.DevTaskSaveDTO;
import com.pig4cloud.pig.rm.api.entity.DevTask;
import com.pig4cloud.pig.rm.service.DevTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 开发任务单
 *
 * @author youming
 * @date 2026-07-29
 */
@RestController
@AllArgsConstructor
@Tag(name = "开发任务单", description = "任务分解/详细设计/评审/开发执行")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class DevTaskController {

	private final DevTaskService devTaskService;

	@GetMapping("/rm/dev-task/page")
	@Operation(summary = "任务分页查询")
	@HasPermission("rm_dev_view")
	public R<IPage<DevTask>> page(@ParameterObject Page page, @ParameterObject DevTaskQueryDTO query) {
		return R.ok(devTaskService.page(page, query));
	}

	@GetMapping("/rm/dev-task/{id}")
	@Operation(summary = "任务详情")
	@HasPermission("rm_dev_view")
	public R detail(@PathVariable Long id) {
		return devTaskService.detail(id);
	}

	@SysLog("任务分解")
	@PostMapping("/rm/dev-task")
	@Operation(summary = "任务分解（批量创建任务单）")
	@HasPermission("rm_dev_add")
	public R createTasks(@RequestParam Long requirementId, @Valid @RequestBody List<DevTaskSaveDTO> tasks) {
		return devTaskService.createTasks(requirementId, tasks);
	}

	@SysLog("编辑任务")
	@PutMapping("/rm/dev-task")
	@Operation(summary = "编辑任务")
	@HasPermission("rm_dev_edit")
	public R update(@Valid @RequestBody DevTask devTask) {
		return devTaskService.updateTask(devTask);
	}

	@SysLog("保存详细设计")
	@PostMapping("/rm/dev-task/detail-design")
	@Operation(summary = "保存详细设计")
	@HasPermission("rm_dev_design")
	public R saveDetailDesign(@Valid @RequestBody DevTaskDesignDTO dto) {
		return devTaskService.saveDetailDesign(dto);
	}

	@SysLog("提交设计评审")
	@PostMapping("/rm/dev-task/design-review/submit/{id}")
	@Operation(summary = "提交详细设计评审")
	@HasPermission("rm_dev_design")
	public R submitDesignReview(@PathVariable Long id) {
		return devTaskService.submitDesignReview(id);
	}

	@SysLog("详细设计评审")
	@PostMapping("/rm/dev-task/design-review")
	@Operation(summary = "详细设计评审（通过/驳回）")
	@HasPermission("rm_dev_review")
	public R designReview(@Valid @RequestBody DevTaskReviewDTO dto) {
		return devTaskService.designReview(dto);
	}

	@SysLog("开始开发")
	@PostMapping("/rm/dev-task/start/{id}")
	@Operation(summary = "开始开发")
	@HasPermission("rm_dev_edit")
	public R startDev(@PathVariable Long id) {
		return devTaskService.startDev(id);
	}

	@SysLog("开发完成提测")
	@PostMapping("/rm/dev-task/submit-test/{id}")
	@Operation(summary = "开发完成提测")
	@HasPermission("rm_dev_edit")
	public R submitTest(@PathVariable Long id) {
		return devTaskService.submitTest(id);
	}

}
