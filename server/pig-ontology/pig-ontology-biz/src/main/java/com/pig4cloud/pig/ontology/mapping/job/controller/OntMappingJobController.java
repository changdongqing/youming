/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.job.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.mapping.job.dto.JobCreateRequest;
import com.pig4cloud.pig.ontology.mapping.job.dto.JobQuery;
import com.pig4cloud.pig.ontology.mapping.job.dto.JobRetryRequest;
import com.pig4cloud.pig.ontology.mapping.job.entity.OntMappingJob;
import com.pig4cloud.pig.ontology.mapping.job.entity.OntMappingJobRecord;
import com.pig4cloud.pig.ontology.mapping.job.service.MappingJobService;
import com.pig4cloud.pig.ontology.mapping.job.vo.MappingJobRecordVO;
import com.pig4cloud.pig.ontology.mapping.job.vo.MappingJobVO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 映射作业管理 API（18-07 §13）。
 * <p>
 * 所有路径前缀为 {@code /ontology/data-mapping}。
 *
 * @author youming
 */
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
@Tag(name = "映射作业管理")
@RequestMapping("/ontology/data-mapping")
public class OntMappingJobController {

	private final MappingJobService jobService;

	// ==================== 作业执行 ====================

	@PostMapping("/versions/{id}/jobs")
	@SysLog("创建映射作业")
	@HasPermission("ontology_mapping_execute")
	public R<MappingJobVO> createJob(@PathVariable("id") Long versionId,
			@Valid @RequestBody JobCreateRequest request) {
		return R.ok(jobService.createJob(versionId, request));
	}

	@GetMapping("/jobs/page")
	@HasPermission("ontology_mapping_job_view")
	public R<Page<MappingJobVO>> jobPage(@ParameterObject Page<OntMappingJob> page,
			@ParameterObject JobQuery query) {
		return R.ok(jobService.page(page, query));
	}

	@GetMapping("/jobs/{id}")
	@HasPermission("ontology_mapping_job_view")
	public R<MappingJobVO> getJobDetail(@PathVariable("id") Long id) {
		return R.ok(jobService.getJobDetail(id));
	}

	@GetMapping("/jobs/{id}/records")
	@HasPermission("ontology_mapping_job_view")
	public R<Page<MappingJobRecordVO>> getJobRecords(@PathVariable("id") Long id,
			@ParameterObject Page<OntMappingJobRecord> page,
			@RequestParam(required = false) String recordStatus) {
		return R.ok(jobService.getJobRecords(id, page, recordStatus));
	}

	@PostMapping("/jobs/{id}/cancel")
	@SysLog("取消映射作业")
	@HasPermission("ontology_mapping_execute")
	public R<MappingJobVO> cancelJob(@PathVariable("id") Long id) {
		return R.ok(jobService.cancelJob(id));
	}

	@PostMapping("/jobs/{id}/retry")
	@SysLog("重试映射作业")
	@HasPermission("ontology_mapping_retry")
	public R<MappingJobVO> retryJob(@PathVariable("id") Long id,
			@Valid @RequestBody(required = false) JobRetryRequest request) {
		if (request == null) {
			request = new JobRetryRequest();
		}
		return R.ok(jobService.retryJob(id, request));
	}

	// ==================== 游标与调度 ====================

	@GetMapping("/projects/{id}/cursor")
	@HasPermission("ontology_mapping_job_view")
	public R<String> getProjectCursor(@PathVariable("id") Long id) {
		return R.ok(jobService.getProjectCursor(id));
	}

	@PutMapping("/projects/{id}/schedule")
	@SysLog("更新映射工程调度")
	@HasPermission("ontology_mapping_admin")
	public R<Void> updateSchedule(@PathVariable("id") Long id,
			@RequestParam boolean scheduleEnabled,
			@RequestParam(required = false) String scheduleCron,
			@RequestParam(required = false) String scheduleRunType) {
		jobService.updateSchedule(id, scheduleEnabled, scheduleCron, scheduleRunType);
		return R.ok();
	}

}
