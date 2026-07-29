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
import com.pig4cloud.pig.common.excel.annotation.ResponseExcel;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.rm.api.vo.DevTaskStatReportVO;
import com.pig4cloud.pig.rm.api.vo.RequirementDetailReportVO;
import com.pig4cloud.pig.rm.api.vo.RequirementStatReportVO;
import com.pig4cloud.pig.rm.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 报表统计
 *
 * @author youming
 * @date 2026-07-29
 */
@RestController
@AllArgsConstructor
@Tag(name = "报表统计", description = "需求明细/需求统计/任务统计")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ReportController {

	private final ReportService reportService;

	@GetMapping("/rm/report/requirement-detail")
	@Operation(summary = "RPT-01 需求明细表")
	@HasPermission("rm_report")
	public R<IPage<RequirementDetailReportVO>> requirementDetail(@ParameterObject Page page,
			@RequestParam(required = false) String source,
			@RequestParam(required = false) String customerProject,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) LocalDate startDate,
			@RequestParam(required = false) LocalDate endDate) {
		return R.ok(reportService.requirementDetail(page, source, customerProject, status, startDate, endDate));
	}

	@GetMapping("/rm/report/requirement-stat")
	@Operation(summary = "RPT-02 需求统计表")
	@HasPermission("rm_report")
	public R<RequirementStatReportVO> requirementStat(
			@RequestParam(required = false) LocalDate startDate,
			@RequestParam(required = false) LocalDate endDate) {
		return R.ok(reportService.requirementStat(startDate, endDate));
	}

	@GetMapping("/rm/report/dev-task-stat")
	@Operation(summary = "RPT-03 开发任务统计表")
	@HasPermission("rm_report")
	public R<DevTaskStatReportVO> devTaskStat(
			@RequestParam(required = false) Long requirementId,
			@RequestParam(required = false) Long assigneeId,
			@RequestParam(required = false) LocalDate startDate,
			@RequestParam(required = false) LocalDate endDate) {
		return R.ok(reportService.devTaskStat(requirementId, assigneeId, startDate, endDate));
	}

	@ResponseExcel(name = "需求明细表")
	@GetMapping("/rm/report/requirement-detail/export")
	@Operation(summary = "需求明细表导出Excel")
	@HasPermission("rm_report")
	public List<RequirementDetailReportVO> requirementDetailExport(
			@RequestParam(required = false) String source,
			@RequestParam(required = false) String customerProject,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) LocalDate startDate,
			@RequestParam(required = false) LocalDate endDate) {
		return reportService.requirementDetailList(source, customerProject, status, startDate, endDate);
	}

}
