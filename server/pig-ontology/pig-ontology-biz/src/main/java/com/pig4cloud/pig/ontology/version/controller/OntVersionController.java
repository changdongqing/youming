/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.version.dto.MigrationJobCreateRequest;
import com.pig4cloud.pig.ontology.version.dto.MigrationJobQuery;
import com.pig4cloud.pig.ontology.version.dto.RestorePlanRequest;
import com.pig4cloud.pig.ontology.version.dto.VersionConfigRequest;
import com.pig4cloud.pig.ontology.version.dto.VersionPrepareRequest;
import com.pig4cloud.pig.ontology.version.dto.VersionQuery;
import com.pig4cloud.pig.ontology.version.entity.OntInstanceMigrationJob;
import com.pig4cloud.pig.ontology.version.migration.InstanceMigrationJobService;
import com.pig4cloud.pig.ontology.version.migration.InstanceMigrationPlanner;
import com.pig4cloud.pig.ontology.version.restore.OntologyRestorePlanService;
import com.pig4cloud.pig.ontology.version.service.OntologyVersionService;
import com.pig4cloud.pig.ontology.version.vo.MigrationJobStatusVO;
import com.pig4cloud.pig.ontology.version.vo.MigrationPreviewVO;
import com.pig4cloud.pig.ontology.version.vo.OntOntologyVersionDetailVO;
import com.pig4cloud.pig.ontology.version.vo.RestorePlanVO;
import com.pig4cloud.pig.ontology.version.vo.VersionDiffVO;
import com.pig4cloud.pig.ontology.version.vo.VersionPrepareResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 本体版本演化 API。
 *
 * @author youming
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ontology/versions")
@Tag(description = "ontology-version", name = "本体版本演化")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntVersionController {

	private final OntologyVersionService versionService;

	private final InstanceMigrationJobService migrationJobService;

	private final InstanceMigrationPlanner migrationPlanner;

	private final OntologyRestorePlanService restorePlanService;

	@Operation(summary = "版本分页", description = "分页查询版本列表")
	@GetMapping("/page")
	@HasPermission("ontology_version_view")
	public R<Page<OntOntologyVersionDetailVO>> page(@ParameterObject Page<com.pig4cloud.pig.ontology.version.entity.OntOntologyVersion> page,
			@ParameterObject VersionQuery query) {
		return versionService.page(page, query);
	}

	@Operation(summary = "版本详情", description = "获取版本详情")
	@GetMapping("/{id}")
	@HasPermission("ontology_version_view")
	public R<OntOntologyVersionDetailVO> getById(@PathVariable Long id) {
		return versionService.getDetail(id);
	}

	@Operation(summary = "构建候选版本", description = "构建候选版本、diff和兼容性报告")
	@PostMapping("/prepare")
	@SysLog("版本发布-准备")
	@HasPermission("ontology_version_publish")
	public R<VersionPrepareResultVO> prepare(@Valid @RequestBody VersionPrepareRequest request) {
		return versionService.prepare(request);
	}

	@Operation(summary = "激活候选版本", description = "激活候选版本为当前版本")
	@PostMapping("/{id}/activate")
	@SysLog("版本发布-激活")
	@HasPermission("ontology_version_publish")
	public R<Boolean> activate(@PathVariable Long id) {
		return versionService.activate(id);
	}

	@Operation(summary = "取消候选版本", description = "取消PREPARED或MIGRATING状态的候选版本")
	@PostMapping("/{id}/cancel")
	@SysLog("版本发布-取消")
	@HasPermission("ontology_version_publish")
	public R<Boolean> cancel(@PathVariable Long id) {
		return versionService.cancel(id);
	}

	@Operation(summary = "版本差异", description = "计算两个版本之间的差异")
	@GetMapping("/{id}/diff/{targetId}")
	@HasPermission("ontology_version_view")
	public R<VersionDiffVO> diff(@PathVariable Long id, @PathVariable Long targetId) {
		return versionService.diff(id, targetId);
	}

	@Operation(summary = "查看快照", description = "获取版本Schema快照JSON")
	@GetMapping("/{id}/snapshot")
	@HasPermission("ontology_version_view")
	public R<String> getSnapshot(@PathVariable Long id) {
		return versionService.getSnapshot(id);
	}

	@Operation(summary = "迁移影响预览", description = "预览BREAKING版本的实例迁移影响")
	@PostMapping("/{id}/migration/preview")
	@HasPermission("ontology_version_publish")
	public R<MigrationPreviewVO> migrationPreview(@PathVariable Long id) {
		return R.failed("迁移预览需通过prepare结果中diffSummary分析，暂不支持独立调用");
	}

	@Operation(summary = "创建迁移作业", description = "为BREAKING候选版本创建实例迁移作业")
	@PostMapping("/{id}/migration/jobs")
	@SysLog("版本迁移-创建作业")
	@HasPermission("ontology_version_publish")
	public R<Long> createMigrationJob(@PathVariable Long id,
			@Valid @RequestBody MigrationJobCreateRequest request) {
		return migrationJobService.createJob(request.getCandidateVersionId() != null
				? request.getCandidateVersionId() : id, id);
	}

	@Operation(summary = "迁移作业分页", description = "分页查询迁移作业")
	@GetMapping("/migration/jobs/page")
	@HasPermission("ontology_version_view")
	public R<Page<OntInstanceMigrationJob>> migrationJobPage(
			@ParameterObject Page<OntInstanceMigrationJob> page, @ParameterObject MigrationJobQuery query) {
		return migrationJobService.page(page, query.getOntologyId(), query.getCandidateVersionId(), query.getStatus());
	}

	@Operation(summary = "迁移作业状态", description = "查询迁移作业执行状态")
	@GetMapping("/migration/jobs/{jobId}")
	@HasPermission("ontology_version_view")
	public R<MigrationJobStatusVO> getMigrationJobStatus(@PathVariable Long jobId) {
		return migrationJobService.getJobStatus(jobId);
	}

	@Operation(summary = "取消迁移作业", description = "取消PENDING或RUNNING状态的迁移作业")
	@PostMapping("/migration/jobs/{jobId}/cancel")
	@SysLog("版本迁移-取消作业")
	@HasPermission("ontology_version_publish")
	public R<Boolean> cancelMigrationJob(@PathVariable Long jobId) {
		return migrationJobService.cancelJob(jobId);
	}

	@Operation(summary = "生成恢复计划", description = "从历史版本生成正向恢复计划，不直接改数据")
	@PostMapping("/{id}/restore-plan")
	@SysLog("版本恢复-生成计划")
	@HasPermission("ontology_version_restore")
	public R<RestorePlanVO> restorePlan(@PathVariable Long id,
			@Valid @RequestBody RestorePlanRequest request) {
		return restorePlanService.generateRestorePlan(request.getOntologyId(),
				request.getTargetVersionId() != null ? request.getTargetVersionId() : id);
	}

	@Operation(summary = "配置工程版本IRI", description = "配置本体工程的ontology_iri和version_iri_base")
	@PutMapping("/projects/{projectId}/version-config")
	@SysLog("版本配置")
	@HasPermission("ontology_version_publish")
	public R<Boolean> updateVersionConfig(@PathVariable Long projectId,
			@Valid @RequestBody VersionConfigRequest request) {
		return versionService.updateVersionConfig(projectId, request);
	}

}
