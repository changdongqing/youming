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
import com.pig4cloud.pig.rm.api.dto.RequirementApproveDTO;
import com.pig4cloud.pig.rm.api.dto.RequirementDesignDTO;
import com.pig4cloud.pig.rm.api.dto.RequirementQueryDTO;
import com.pig4cloud.pig.rm.api.dto.RequirementScheduleDTO;
import com.pig4cloud.pig.rm.api.entity.Requirement;
import com.pig4cloud.pig.rm.service.RequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 软件需求申请单
 *
 * @author youming
 * @date 2026-07-29
 */
@RestController
@AllArgsConstructor
@Tag(name = "软件需求申请单", description = "需求全流程管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class RequirementController {

	private final RequirementService requirementService;

	@GetMapping("/rm/requirement/page")
	@Operation(summary = "需求分页查询")
	@HasPermission("rm_req_view")
	public R<IPage<Requirement>> page(@ParameterObject Page page, @ParameterObject RequirementQueryDTO query) {
		return R.ok(requirementService.page(page, query));
	}

	@GetMapping("/rm/requirement/{id}")
	@Operation(summary = "需求详情")
	@HasPermission("rm_req_view")
	public R detail(@PathVariable Long id) {
		return requirementService.detail(id);
	}

	@SysLog("新增需求")
	@PostMapping("/rm/requirement")
	@Operation(summary = "创建需求")
	@HasPermission("rm_req_add")
	public R save(@Valid @RequestBody Requirement requirement) {
		return requirementService.saveRequirement(requirement);
	}

	@SysLog("编辑需求")
	@PutMapping("/rm/requirement")
	@Operation(summary = "编辑需求")
	@HasPermission("rm_req_edit")
	public R update(@Valid @RequestBody Requirement requirement) {
		return requirementService.updateRequirement(requirement);
	}

	@SysLog("删除需求")
	@DeleteMapping("/rm/requirement/{id}")
	@Operation(summary = "删除需求")
	@HasPermission("rm_req_del")
	public R remove(@PathVariable Long id) {
		return requirementService.removeRequirement(id);
	}

	@SysLog("提交需求")
	@PostMapping("/rm/requirement/submit/{id}")
	@Operation(summary = "提交需求（草稿→待审批）")
	@HasPermission("rm_req_add")
	public R submit(@PathVariable Long id) {
		return requirementService.submit(id);
	}

	@SysLog("需求审批")
	@PostMapping("/rm/requirement/approve")
	@Operation(summary = "需求审批（通过/驳回）")
	@HasPermission("rm_req_approve")
	public R approve(@Valid @RequestBody RequirementApproveDTO dto) {
		return requirementService.approve(dto);
	}

	@SysLog("讨论会评审")
	@PostMapping("/rm/requirement/review")
	@Operation(summary = "讨论会评审结论")
	@HasPermission("rm_req_approve")
	public R review(@Valid @RequestBody RequirementApproveDTO dto) {
		return requirementService.review(dto);
	}

	@SysLog("保存需求设计")
	@PostMapping("/rm/requirement/design")
	@Operation(summary = "保存需求设计")
	@HasPermission("rm_req_design")
	public R saveDesign(@Valid @RequestBody RequirementDesignDTO dto) {
		return requirementService.saveDesign(dto);
	}

	@SysLog("提交设计评审")
	@PostMapping("/rm/requirement/design-review/submit/{id}")
	@Operation(summary = "提交设计评审")
	@HasPermission("rm_req_design")
	public R submitDesignReview(@PathVariable Long id) {
		return requirementService.submitDesignReview(id);
	}

	@SysLog("需求设计评审")
	@PostMapping("/rm/requirement/design-review")
	@Operation(summary = "需求设计评审（通过/退回）")
	@HasPermission("rm_req_approve")
	public R designReview(@Valid @RequestBody RequirementApproveDTO dto) {
		return requirementService.designReview(dto);
	}

	@SysLog("开发排期")
	@PostMapping("/rm/requirement/schedule")
	@Operation(summary = "开发排期")
	@HasPermission("rm_req_schedule")
	public R schedule(@Valid @RequestBody RequirementScheduleDTO dto) {
		return requirementService.schedule(dto);
	}

	@SysLog("质量确认")
	@PostMapping("/rm/requirement/quality-confirm/{id}")
	@Operation(summary = "产品经理质量确认")
	@HasPermission("rm_req_approve")
	public R qualityConfirm(@PathVariable Long id) {
		return requirementService.qualityConfirm(id);
	}

	@SysLog("需求验收")
	@PostMapping("/rm/requirement/accept/{id}")
	@Operation(summary = "发起人验收")
	@HasPermission("rm_req_view")
	public R accept(@PathVariable Long id, @RequestParam String conclusion,
			@RequestParam(required = false) String remark) {
		return requirementService.accept(id, conclusion, remark);
	}

	@GetMapping("/rm/requirement/statistics")
	@Operation(summary = "需求统计")
	@HasPermission("rm_report")
	public R statistics() {
		return requirementService.statistics();
	}

}
