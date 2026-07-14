/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.event.model.OntologyDomainEvent;
import com.pig4cloud.pig.ontology.event.model.OntologyEventTypes;
import com.pig4cloud.pig.ontology.event.service.OntDomainEventPublisher;
import com.pig4cloud.pig.ontology.mapping.project.MappingErrorCode;
import com.pig4cloud.pig.ontology.mapping.project.diff.MappingVersionDiffService;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.mapping.project.guard.MappingOntologyVersionGuard;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingProjectMapper;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingVersionMapper;
import com.pig4cloud.pig.ontology.mapping.project.service.OntMappingVersionService;
import com.pig4cloud.pig.ontology.mapping.project.snapshot.MappingSnapshotBuilder;
import com.pig4cloud.pig.ontology.mapping.project.snapshot.MappingSnapshotCanonicalizer;
import com.pig4cloud.pig.ontology.mapping.project.support.MappingSemVerValidator;
import com.pig4cloud.pig.ontology.mapping.project.vo.MappingVersionDiffVO;
import com.pig4cloud.pig.ontology.mapping.project.vo.MappingVersionVO;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import com.pig4cloud.pig.ontology.version.entity.OntOntologyVersion;
import com.pig4cloud.pig.ontology.version.mapper.OntOntologyVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 映射版本服务实现（18-03 §8）。
 * <p>
 * 管理版本状态机：DRAFT → VALIDATING → VALIDATED → PUBLISHED → RETIRED。
 * 发布事务遵循设计 §8.4：锁工程行 → 校验 → 快照 → 旧版本 RETIRED → 新版本 PUBLISHED → 更新工程 → EDA事件。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OntMappingVersionServiceImpl extends ServiceImpl<OntMappingVersionMapper, OntMappingVersion>
		implements OntMappingVersionService {

	private final OntMappingProjectMapper projectMapper;

	private final OntOntologyProjectMapper ontologyProjectMapper;

	private final OntOntologyVersionMapper ontologyVersionMapper;

	private final MappingOntologyVersionGuard versionGuard;

	private final MappingSnapshotBuilder snapshotBuilder;

	private final MappingSnapshotCanonicalizer snapshotCanonicalizer;

	private final MappingSemVerValidator semVerValidator;

	private final MappingVersionDiffService diffService;

	private final OntDomainEventPublisher eventPublisher;

	// ==================== 查询 ====================

	@Override
	public Page<MappingVersionVO> listVersions(Page<OntMappingVersion> page, Long projectId) {
		Page<OntMappingVersion> result = baseMapper.selectPage(page,
				Wrappers.<OntMappingVersion>lambdaQuery()
						.eq(OntMappingVersion::getMappingProjectId, projectId)
						.eq(OntMappingVersion::getDelFlag, "0")
						.orderByDesc(OntMappingVersion::getCreateTime));

		Page<MappingVersionVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
		voPage.setRecords(result.getRecords().stream().map(v -> toVO(v, false)).toList());
		return voPage;
	}

	@Override
	public MappingVersionVO getVersionDetail(Long id) {
		OntMappingVersion version = findVersionOrThrow(id);
		return toVO(version, false);
	}

	// ==================== 创建下一版本 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MappingVersionVO createNextVersion(Long projectId) {
		OntMappingProject project = findProjectOrThrow(projectId);

		// 仅 ACTIVE/DISABLED 工程允许从 active version 复制
		if ("DRAFT".equals(project.getProjectStatus()) || "ARCHIVED".equals(project.getProjectStatus())) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_004.getMessage()
					+ ": 仅 ACTIVE/DISABLED 工程允许创建新版本");
		}

		// 拒绝已存在编辑态版本
		Long existingDraft = baseMapper.selectCount(Wrappers.<OntMappingVersion>lambdaQuery()
				.eq(OntMappingVersion::getMappingProjectId, projectId)
				.eq(OntMappingVersion::getDelFlag, "0")
				.in(OntMappingVersion::getVersionStatus, "DRAFT", "VALIDATING", "VALIDATED"));
		if (existingDraft > 0) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_005.getMessage());
		}

		// 获取 active PUBLISHED 版本
		if (project.getActiveVersionId() == null) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_006.getMessage());
		}
		OntMappingVersion activeVersion = baseMapper.selectById(project.getActiveVersionId());
		if (activeVersion == null || "1".equals(activeVersion.getDelFlag())) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_006.getMessage());
		}

		// 计算下一版本号（patch+1）
		String nextVersionNumber = incrementPatchVersion(activeVersion.getVersionNumber());

		// 创建新 DRAFT 版本
		OntMappingVersion newVersion = new OntMappingVersion();
		newVersion.setMappingProjectId(projectId);
		newVersion.setVersionNumber(nextVersionNumber);
		newVersion.setVersionStatus("DRAFT");
		newVersion.setPriorVersionId(activeVersion.getId());
		newVersion.setOntologyVersionConstraint(activeVersion.getOntologyVersionConstraint());
		newVersion.setMetadataDependencies(activeVersion.getMetadataDependencies());
		newVersion.setRevision(0L);

		baseMapper.insert(newVersion);

		log.info("Created next mapping version: projectId={}, versionId={}, versionNumber={}, priorVersionId={}",
				projectId, newVersion.getId(), nextVersionNumber, activeVersion.getId());
		return toVO(newVersion, false);
	}

	// ==================== reopen ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MappingVersionVO reopen(Long id) {
		OntMappingVersion version = findVersionOrThrow(id);

		// 仅 VALIDATED 可 reopen
		if (!"VALIDATED".equals(version.getVersionStatus())) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_004.getMessage()
					+ ": 仅 VALIDATED 状态可 reopen");
		}

		// CAS: VALIDATED → DRAFT
		int updated = baseMapper.casUpdateStatus(id, "VALIDATED", "DRAFT", version.getRevision());
		if (updated == 0) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_007.getMessage());
		}

		// 清除发布候选信息
		version = baseMapper.selectById(id);
		version.setValidatedOntologyVersionId(null);
		version.setValidatedWorkspaceRevision(null);
		version.setValidationReportId(null);
		version.setValidationSummary(null);
		baseMapper.updateById(version);

		log.info("Reopened mapping version: id={}", id);
		return toVO(version, false);
	}

	// ==================== 发布 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MappingVersionVO publish(Long id) {
		// 1. 锁映射工程行（通过版本查找工程）
		OntMappingVersion version = baseMapper.selectForUpdate(id);
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_002.getMessage());
		}

		// 2. 确认版本状态为 VALIDATED
		if (!"VALIDATED".equals(version.getVersionStatus())) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_004.getMessage()
					+ ": 仅 VALIDATED 版本可发布，当前状态: " + version.getVersionStatus());
		}

		// 3. 锁工程行
		OntMappingProject project = projectMapper.selectForUpdate(version.getMappingProjectId());
		if (project == null || "1".equals(project.getDelFlag())) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_001.getMessage());
		}

		// 4. 本体版本守卫校验
		versionGuard.assertPublishable(version, project);

		// 5. 获取当前本体版本号用于快照
		OntOntologyProject ontologyProject = ontologyProjectMapper.selectById(project.getOntologyId());
		OntOntologyVersion ontologyVersion = ontologyProject != null
				? ontologyVersionMapper.selectById(ontologyProject.getCurrentVersionId()) : null;
		String validatedVersionNumber = ontologyVersion != null ? ontologyVersion.getVersionNumber() : null;

		// 6. 构建规范快照和 hash
		String snapshotJson = snapshotBuilder.buildSnapshot(project, version, validatedVersionNumber);
		String canonicalJson = snapshotCanonicalizer.canonicalize(snapshotJson);
		String configHash = snapshotCanonicalizer.computeHash(canonicalJson);

		// 7. 将旧 PUBLISHED 改 RETIRED（如果存在）
		if (project.getActiveVersionId() != null) {
			int retired = baseMapper.retireVersion(project.getActiveVersionId(), LocalDateTime.now());
			if (retired == 0) {
				log.warn("Old active version {} was not in PUBLISHED state during publish",
						project.getActiveVersionId());
			}
		}

		// 8. 当前版本改 PUBLISHED（CAS: VALIDATED → PUBLISHED）
		int published = baseMapper.publishVersion(id, "VALIDATED", version.getRevision(),
				canonicalJson, configHash, "admin", LocalDateTime.now());
		if (published == 0) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_007.getMessage()
					+ ": 版本状态或修订号已变更");
		}

		// 9. 更新 project.activeVersionId 和 ACTIVE 状态
		project.setActiveVersionId(id);
		if ("DRAFT".equals(project.getProjectStatus())) {
			project.setProjectStatus("ACTIVE");
		}
		projectMapper.updateById(project);

		// 10. 写 Outbox MAPPING_VERSION_PUBLISHED 事件
		eventPublisher.append(OntologyDomainEvent.builder()
				.eventType(OntologyEventTypes.MAPPING_VERSION_PUBLISHED)
				.ontologyId(project.getOntologyId())
				.aggregateType("MAPPING_VERSION")
				.aggregateId(id.toString())
				.operation("PUBLISHED")
				.payload(Map.of(
						"mappingProjectId", project.getId(),
						"versionNumber", version.getVersionNumber(),
						"configHash", configHash))
				.build());

		log.info("Published mapping version: id={}, versionNumber={}, configHash={}",
				id, version.getVersionNumber(), configHash);

		OntMappingVersion publishedVersion = baseMapper.selectById(id);
		return toVO(publishedVersion, false);
	}

	// ==================== retire ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MappingVersionVO retire(Long id) {
		OntMappingVersion version = findVersionOrThrow(id);

		// 仅 PUBLISHED 可 retire
		if (!"PUBLISHED".equals(version.getVersionStatus())) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_004.getMessage()
					+ ": 仅 PUBLISHED 版本可停用");
		}

		// 停用版本
		int retired = baseMapper.retireVersion(id, LocalDateTime.now());
		if (retired == 0) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_007.getMessage());
		}

		// 工程转 DISABLED
		OntMappingProject project = projectMapper.selectById(version.getMappingProjectId());
		if (project != null && "0".equals(project.getDelFlag())) {
			project.setProjectStatus("DISABLED");
			project.setActiveVersionId(null);
			projectMapper.updateById(project);
		}

		log.info("Retired mapping version: id={}", id);
		version = baseMapper.selectById(id);
		return toVO(version, false);
	}

	// ==================== 快照查看 ====================

	@Override
	public String getSnapshot(Long id) {
		OntMappingVersion version = findVersionOrThrow(id);

		if (StrUtil.isBlank(version.getConfigSnapshot())) {
			return null;
		}

		// 返回规范化快照（不含凭证）
		return snapshotCanonicalizer.canonicalize(version.getConfigSnapshot());
	}

	// ==================== 差异对比 ====================

	@Override
	public MappingVersionDiffVO diff(Long baseId, Long compareId) {
		return diffService.diff(baseId, compareId);
	}

	// ==================== 内部方法 ====================

	private OntMappingVersion findVersionOrThrow(Long id) {
		OntMappingVersion version = baseMapper.selectById(id);
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_002.getMessage());
		}
		return version;
	}

	private OntMappingProject findProjectOrThrow(Long id) {
		OntMappingProject project = projectMapper.selectById(id);
		if (project == null || "1".equals(project.getDelFlag())) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_001.getMessage());
		}
		return project;
	}

	/**
	 * 递增 patch 版本号：1.2.3 → 1.2.4。
	 */
	private String incrementPatchVersion(String versionNumber) {
		String[] parts = versionNumber.split("\\.");
		if (parts.length != 3) {
			return versionNumber + "-next";
		}
		int patch = Integer.parseInt(parts[2]);
		return parts[0] + "." + parts[1] + "." + (patch + 1);
	}

	private MappingVersionVO toVO(OntMappingVersion version, boolean includeSnapshot) {
		MappingVersionVO vo = new MappingVersionVO();
		vo.setId(version.getId());
		vo.setMappingProjectId(version.getMappingProjectId());
		vo.setVersionNumber(version.getVersionNumber());
		vo.setVersionStatus(version.getVersionStatus());
		vo.setPriorVersionId(version.getPriorVersionId());
		vo.setOntologyVersionConstraint(version.getOntologyVersionConstraint());
		vo.setValidatedOntologyVersionId(version.getValidatedOntologyVersionId());
		vo.setValidatedWorkspaceRevision(version.getValidatedWorkspaceRevision());
		vo.setConfigHash(version.getConfigHash());
		vo.setValidationReportId(version.getValidationReportId());
		vo.setValidationSummary(version.getValidationSummary());
		vo.setReleaseNotes(version.getReleaseNotes());
		vo.setPublishedBy(version.getPublishedBy());
		vo.setPublishedAt(version.getPublishedAt());
		vo.setRetiredAt(version.getRetiredAt());
		vo.setRevision(version.getRevision());
		vo.setCreateTime(version.getCreateTime());
		vo.setUpdateTime(version.getUpdateTime());

		// 快照仅管理员可查看，由 Controller 层权限控制后传入
		if (includeSnapshot) {
			vo.setConfigSnapshot(version.getConfigSnapshot());
		}

		return vo;
	}

}
