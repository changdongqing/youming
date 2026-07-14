/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import com.pig4cloud.pig.ontology.event.model.OntologyDomainEvent;
import com.pig4cloud.pig.ontology.event.model.OntologyEventTypes;
import com.pig4cloud.pig.ontology.event.service.OntDomainEventPublisher;
import com.pig4cloud.pig.ontology.version.diff.OntologyCompatibilityClassifier;
import com.pig4cloud.pig.ontology.version.diff.OntologyVersionDiffService;
import com.pig4cloud.pig.ontology.version.dto.VersionConfigRequest;
import com.pig4cloud.pig.ontology.version.dto.VersionPrepareRequest;
import com.pig4cloud.pig.ontology.version.dto.VersionQuery;
import com.pig4cloud.pig.ontology.version.entity.OntInstanceMigrationJob;
import com.pig4cloud.pig.ontology.version.entity.OntOntologyVersion;
import com.pig4cloud.pig.ontology.version.mapper.OntInstanceMigrationJobMapper;
import com.pig4cloud.pig.ontology.version.mapper.OntOntologyVersionMapper;
import com.pig4cloud.pig.ontology.version.service.OntologyVersionService;
import com.pig4cloud.pig.ontology.version.snapshot.OntologySnapshotBuilder;
import com.pig4cloud.pig.ontology.version.snapshot.OntologySnapshotCanonicalizer;
import com.pig4cloud.pig.ontology.version.support.SemVerValidator;
import com.pig4cloud.pig.ontology.version.vo.OntOntologyVersionDetailVO;
import com.pig4cloud.pig.ontology.version.vo.VersionDiffVO;
import com.pig4cloud.pig.ontology.version.vo.VersionPrepareResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本体版本服务实现。
 * <p>
 * 编排 prepare/activate/cancel/查询流程。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyVersionServiceImpl extends ServiceImpl<OntOntologyVersionMapper, OntOntologyVersion>
		implements OntologyVersionService {

	private final OntOntologyProjectMapper projectMapper;

	private final OntInstanceMigrationJobMapper migrationJobMapper;

	private final OntologySnapshotBuilder snapshotBuilder;

	private final OntologySnapshotCanonicalizer canonicalizer;

	private final OntologyVersionDiffService diffService;

	private final OntologyCompatibilityClassifier classifier;

	private final SemVerValidator semVerValidator;

	private final ObjectMapper objectMapper;

	private final OntDomainEventPublisher eventPublisher;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<VersionPrepareResultVO> prepare(VersionPrepareRequest request) {
		// 1. 获取 advisory lock
		projectMapper.acquireVersionLock(request.getOntologyId());

		OntOntologyProject project = projectMapper.selectById(request.getOntologyId());
		if (project == null) {
			return R.failed("本体工程不存在");
		}

		// 2. 校验工程已配置 IRI
		if (project.getOntologyIri() == null || project.getVersionIriBase() == null) {
			return R.failed("请先配置本体IRI和版本IRI基础路径");
		}

		// 3. 校验版本号格式
		if (!semVerValidator.isValid(request.getVersionNumber())) {
			return R.failed("版本号格式不合法，必须为 MAJOR.MINOR.PATCH");
		}

		// 4. 校验版本号严格递增
		Long currentVersionId = project.getCurrentVersionId();
		if (currentVersionId != null) {
			OntOntologyVersion currentVersion = this.getById(currentVersionId);
			if (currentVersion != null) {
				if (!semVerValidator.isStrictlyGreater(request.getVersionNumber(),
						currentVersion.getVersionNumber())) {
					return R.failed("版本号必须严格大于当前版本 " + currentVersion.getVersionNumber());
				}
			}
		}

		// 5. 校验版本号唯一
		Long existCount = this.baseMapper.selectCount(
			Wrappers.<OntOntologyVersion>lambdaQuery()
				.eq(OntOntologyVersion::getOntologyId, request.getOntologyId())
				.eq(OntOntologyVersion::getVersionNumber, request.getVersionNumber()));
		if (existCount > 0) {
			return R.failed("版本号已存在: " + request.getVersionNumber());
		}

		// 6. 构建版本 IRI
		String versionIri = project.getVersionIriBase() + request.getVersionNumber();

		// 7. 构建快照
		String snapshotJson = snapshotBuilder.buildSnapshot(request.getOntologyId(), project.getOntologyIri(),
				versionIri);

		// 8. 规范化和 hash
		ObjectNode canonicalSnapshot = canonicalizer.canonicalize(snapshotJson);
		String canonicalJson = canonicalizer.toJsonString(canonicalSnapshot);
		String snapshotHash = canonicalizer.computeHash(canonicalSnapshot);

		// 9. 计算差异
		String diffSummary;
		String compatibility;
		List<String> breakingReasons;
		boolean canActivate;

		if (currentVersionId != null) {
			OntOntologyVersion currentVersion = this.getById(currentVersionId);
			ObjectNode oldSnap = canonicalizer.canonicalize(currentVersion.getSchemaSnapshot());
			VersionDiffVO diff = diffService.diffCanonicalized(oldSnap, canonicalSnapshot);

			try {
				diffSummary = objectMapper.writeValueAsString(diff);
			}
			catch (Exception e) {
				diffSummary = "{}";
			}

			OntologyCompatibilityClassifier.CompatibilityResult compatResult = classifier.classify(oldSnap,
					canonicalSnapshot);
			compatibility = compatResult.compatibility();
			breakingReasons = compatResult.breakingReasons();
			canActivate = !"BREAKING".equals(compatibility);
		}
		else {
			// 首次基线发布
			diffSummary = "{\"firstRelease\":true,\"compatibility\":\"BACKWARD_COMPATIBLE\"}";
			compatibility = OntologyCompatibilityClassifier.BACKWARD_COMPATIBLE;
			breakingReasons = List.of();
			canActivate = true;
		}

		// 10. 声明兼容性校验（不能把 BREAKING 降级）
		if (request.getDeclaredCompatibility() != null) {
			if ("BREAKING".equals(compatibility) && !"BREAKING".equals(request.getDeclaredCompatibility())) {
				return R.failed("系统判定为 BREAKING，不能降级为 " + request.getDeclaredCompatibility());
			}
		}

		// 11. 写入 PREPARED 候选版本
		OntOntologyVersion version = new OntOntologyVersion();
		version.setOntologyId(request.getOntologyId());
		version.setVersionNumber(request.getVersionNumber());
		version.setVersionIri(versionIri);
		version.setPriorVersionId(currentVersionId);
		version.setCompatibility(compatibility);
		version.setReleaseStatus("PREPARED");
		version.setReleaseNotes(request.getReleaseNotes());
		version.setSnapshotFormatVersion(1);
		version.setSchemaSnapshot(canonicalJson);
		version.setSnapshotHash(snapshotHash);
		version.setDiffSummary(diffSummary);
		version.setMigrationPlan(request.getMigrationPlan());
		version.setWorkspaceRevision(project.getWorkspaceRevision());
		this.save(version);

		// 12. 构建响应
		VersionPrepareResultVO result = new VersionPrepareResultVO();
		result.setVersionId(version.getId());
		result.setVersionNumber(request.getVersionNumber());
		result.setVersionIri(versionIri);
		result.setCompatibility(compatibility);
		result.setDeclaredCompatibility(request.getDeclaredCompatibility());
		result.setBreakingReasons(breakingReasons);
		result.setDiffSummary(diffSummary);
		result.setSnapshotHash(snapshotHash);
		result.setCanActivate(canActivate);

		eventPublisher.append(OntologyDomainEvent.builder()
			.eventType(OntologyEventTypes.ONTOLOGY_VERSION_PREPARED)
			.ontologyId(version.getOntologyId())
			.aggregateType("ONTOLOGY_VERSION")
			.aggregateId(version.getId().toString())
			.operation("PREPARED")
			.build());
		return R.ok(result);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> activate(Long versionId) {
		projectMapper.acquireVersionLock(null); // 防御性获取全局锁

		OntOntologyVersion version = this.getById(versionId);
		if (version == null) {
			return R.failed("版本不存在");
		}
		if (!"PREPARED".equals(version.getReleaseStatus()) && !"MIGRATING".equals(version.getReleaseStatus())) {
			return R.failed("只有 PREPARED 或 MIGRATING 状态的版本才能激活");
		}

		OntOntologyProject project = projectMapper.selectById(version.getOntologyId());
		if (project == null) {
			return R.failed("本体工程不存在");
		}

		// 校验 workspace_revision 一致
		if (!version.getWorkspaceRevision().equals(project.getWorkspaceRevision())) {
			return R.failed("工作区在准备后已被修改，请重新准备候选版本");
		}

		// 重新计算快照 hash 校验
		ObjectNode currentSnap = canonicalizer.canonicalize(snapshotBuilder.buildSnapshot(version.getOntologyId(),
				project.getOntologyIri(), version.getVersionIri()));
		String currentHash = canonicalizer.computeHash(currentSnap);
		if (!currentHash.equals(version.getSnapshotHash())) {
			return R.failed("工作区快照 hash 与候选版本不一致，工作区在准备后已被修改");
		}

		// BREAKING 版本必须完成迁移
		if ("BREAKING".equals(version.getCompatibility())) {
			Long successJobCount = migrationJobMapper.selectCount(
				Wrappers.<OntInstanceMigrationJob>lambdaQuery()
					.eq(OntInstanceMigrationJob::getCandidateVersionId, versionId)
					.eq(OntInstanceMigrationJob::getStatus, "SUCCEEDED"));
			if (successJobCount == 0) {
				return R.failed("BREAKING 版本必须完成迁移作业后才能激活");
			}
		}

		// 激活
		version.setReleaseStatus("PUBLISHED");
		version.setPublishedBy("admin");
		version.setPublishedAt(LocalDateTime.now());
		this.updateById(version);

		// 更新工程当前版本指针
		project.setCurrentVersionId(versionId);
		projectMapper.updateById(project);

		eventPublisher.append(OntologyDomainEvent.builder()
			.eventType(OntologyEventTypes.ONTOLOGY_VERSION_PUBLISHED)
			.ontologyId(version.getOntologyId())
			.aggregateType("ONTOLOGY_VERSION")
			.aggregateId(version.getId().toString())
			.operation("PUBLISHED")
			.build());
		return R.ok(true);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> cancel(Long versionId) {
		OntOntologyVersion version = this.getById(versionId);
		if (version == null) {
			return R.failed("版本不存在");
		}
		if (!"PREPARED".equals(version.getReleaseStatus()) && !"MIGRATING".equals(version.getReleaseStatus())) {
			return R.failed("只有 PREPARED 或 MIGRATING 状态的版本才能取消");
		}

		version.setReleaseStatus("CANCELLED");
		this.updateById(version);

		// 取消关联的运行中迁移作业
		List<OntInstanceMigrationJob> runningJobs = migrationJobMapper.selectList(
			Wrappers.<OntInstanceMigrationJob>lambdaQuery()
				.eq(OntInstanceMigrationJob::getCandidateVersionId, versionId)
				.in(OntInstanceMigrationJob::getStatus, "PENDING", "RUNNING"));
		for (OntInstanceMigrationJob job : runningJobs) {
			job.setStatus("CANCELLED");
			job.setCompletedAt(LocalDateTime.now());
			migrationJobMapper.updateById(job);
		}

		return R.ok(true);
	}

	@Override
	public R<Page<OntOntologyVersionDetailVO>> page(Page<OntOntologyVersion> page, VersionQuery query) {
		Page<OntOntologyVersion> result = this.page(page,
			Wrappers.<OntOntologyVersion>lambdaQuery()
				.eq(query.getOntologyId() != null, OntOntologyVersion::getOntologyId, query.getOntologyId())
				.like(query.getVersionNumber() != null, OntOntologyVersion::getVersionNumber,
						query.getVersionNumber())
				.eq(query.getCompatibility() != null, OntOntologyVersion::getCompatibility,
						query.getCompatibility())
				.eq(query.getReleaseStatus() != null, OntOntologyVersion::getReleaseStatus,
						query.getReleaseStatus())
				.orderByDesc(OntOntologyVersion::getCreateTime));

		// 转换为 VO
		Page<OntOntologyVersionDetailVO> voPage = new Page<>(result.getCurrent(), result.getSize(),
				result.getTotal());
		List<OntOntologyVersionDetailVO> voList = result.getRecords().stream().map(this::toDetailVO).toList();
		voPage.setRecords(voList);
		return R.ok(voPage);
	}

	@Override
	public R<OntOntologyVersionDetailVO> getDetail(Long id) {
		OntOntologyVersion version = this.getById(id);
		if (version == null) {
			return R.failed("版本不存在");
		}
		return R.ok(toDetailVO(version));
	}

	@Override
	public R<VersionDiffVO> diff(Long id, Long targetId) {
		OntOntologyVersion v1 = this.getById(id);
		OntOntologyVersion v2 = this.getById(targetId);
		if (v1 == null || v2 == null) {
			return R.failed("版本不存在");
		}
		VersionDiffVO diff = diffService.diff(v1.getSchemaSnapshot(), v2.getSchemaSnapshot());
		return R.ok(diff);
	}

	@Override
	public R<String> getSnapshot(Long id) {
		OntOntologyVersion version = this.getById(id);
		if (version == null) {
			return R.failed("版本不存在");
		}
		return R.ok(version.getSchemaSnapshot());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> updateVersionConfig(Long projectId, VersionConfigRequest request) {
		OntOntologyProject project = projectMapper.selectById(projectId);
		if (project == null) {
			return R.failed("本体工程不存在");
		}

		// 已有 PUBLISHED 版本时不可修改 ontology_iri
		if (project.getCurrentVersionId() != null && project.getOntologyIri() != null
				&& !project.getOntologyIri().equals(request.getOntologyIri())) {
			return R.failed("已有已发布版本，不可修改本体IRI");
		}

		// 校验 ontology_iri 唯一
		Long existCount = projectMapper.selectCount(
			Wrappers.<OntOntologyProject>lambdaQuery()
				.ne(OntOntologyProject::getId, projectId)
				.eq(OntOntologyProject::getOntologyIri, request.getOntologyIri()));
		if (existCount > 0) {
			return R.failed("本体IRI已被其他工程使用");
		}

		project.setOntologyIri(request.getOntologyIri());
		project.setVersionIriBase(request.getVersionIriBase());
		projectMapper.updateById(project);

		return R.ok(true);
	}

	private OntOntologyVersionDetailVO toDetailVO(OntOntologyVersion version) {
		OntOntologyVersionDetailVO vo = new OntOntologyVersionDetailVO();
		vo.setId(version.getId());
		vo.setOntologyId(version.getOntologyId());
		vo.setVersionNumber(version.getVersionNumber());
		vo.setVersionIri(version.getVersionIri());
		vo.setPriorVersionId(version.getPriorVersionId());
		vo.setRestoreSourceVersionId(version.getRestoreSourceVersionId());
		vo.setCompatibility(version.getCompatibility());
		vo.setReleaseStatus(version.getReleaseStatus());
		vo.setReleaseNotes(version.getReleaseNotes());
		vo.setSnapshotFormatVersion(version.getSnapshotFormatVersion());
		vo.setSnapshotHash(version.getSnapshotHash());
		vo.setDiffSummary(version.getDiffSummary());
		vo.setMigrationPlan(version.getMigrationPlan());
		vo.setValidationReportId(version.getValidationReportId());
		vo.setWorkspaceRevision(version.getWorkspaceRevision());
		vo.setPublishedBy(version.getPublishedBy());
		vo.setPublishedAt(version.getPublishedAt());
		vo.setCreateTime(version.getCreateTime());

		// 填充前序版本号
		if (version.getPriorVersionId() != null) {
			OntOntologyVersion prior = this.getById(version.getPriorVersionId());
			if (prior != null) {
				vo.setPriorVersionNumber(prior.getVersionNumber());
			}
		}
		// 填充恢复来源版本号
		if (version.getRestoreSourceVersionId() != null) {
			OntOntologyVersion src = this.getById(version.getRestoreSourceVersionId());
			if (src != null) {
				vo.setRestoreSourceVersionNumber(src.getVersionNumber());
			}
		}

		// 判断是否为当前版本
		OntOntologyProject project = projectMapper.selectById(version.getOntologyId());
		if (project != null && version.getId().equals(project.getCurrentVersionId())) {
			vo.setIsCurrent(true);
		}
		else {
			vo.setIsCurrent(false);
		}

		return vo;
	}

}
