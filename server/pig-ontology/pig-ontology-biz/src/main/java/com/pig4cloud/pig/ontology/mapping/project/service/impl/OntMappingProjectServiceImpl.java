/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.mapping.project.MappingErrorCode;
import com.pig4cloud.pig.ontology.mapping.project.dto.MappingProjectCreateDTO;
import com.pig4cloud.pig.ontology.mapping.project.dto.MappingProjectUpdateDTO;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingProjectMapper;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingVersionMapper;
import com.pig4cloud.pig.ontology.mapping.project.service.OntMappingProjectService;
import com.pig4cloud.pig.ontology.mapping.project.support.MappingSemVerValidator;
import com.pig4cloud.pig.ontology.mapping.project.vo.MappingProjectVO;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 映射工程服务实现（18-03 §8）。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OntMappingProjectServiceImpl extends ServiceImpl<OntMappingProjectMapper, OntMappingProject>
		implements OntMappingProjectService {

	private final OntMappingVersionMapper versionMapper;

	private final OntOntologyProjectMapper ontologyProjectMapper;

	private final OntNamespaceMapper namespaceMapper;

	private final MappingSemVerValidator semVerValidator;

	private static final String INITIAL_VERSION_NUMBER = "0.1.0";

	// ==================== 查询 ====================

	@Override
	public Page<MappingProjectVO> page(Page<OntMappingProject> page, String mappingCode, String mappingName,
			String projectStatus, Long ontologyId) {
		Page<OntMappingProject> result = baseMapper.selectPage(page,
				Wrappers.<OntMappingProject>lambdaQuery()
						.like(StrUtil.isNotBlank(mappingCode), OntMappingProject::getMappingCode, mappingCode)
						.like(StrUtil.isNotBlank(mappingName), OntMappingProject::getMappingName, mappingName)
						.eq(StrUtil.isNotBlank(projectStatus), OntMappingProject::getProjectStatus, projectStatus)
						.eq(ontologyId != null, OntMappingProject::getOntologyId, ontologyId)
						.eq(OntMappingProject::getDelFlag, "0")
						.orderByDesc(OntMappingProject::getCreateTime));

		Page<MappingProjectVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
		voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
		return voPage;
	}

	@Override
	public MappingProjectVO getDetail(Long id) {
		OntMappingProject project = findByIdOrThrow(id);
		MappingProjectVO vo = toVO(project);

		// 查询活跃版本摘要
		if (project.getActiveVersionId() != null) {
			OntMappingVersion activeVersion = versionMapper.selectById(project.getActiveVersionId());
			if (activeVersion != null && "0".equals(activeVersion.getDelFlag())) {
				vo.setActiveVersion(toVersionSummary(activeVersion));
			}
		}

		// 查询草稿版本摘要
		OntMappingVersion draftVersion = versionMapper.selectOne(
				Wrappers.<OntMappingVersion>lambdaQuery()
						.eq(OntMappingVersion::getMappingProjectId, id)
						.eq(OntMappingVersion::getDelFlag, "0")
						.in(OntMappingVersion::getVersionStatus, "DRAFT", "VALIDATING", "VALIDATED")
						.orderByDesc(OntMappingVersion::getCreateTime)
						.last("LIMIT 1"));
		if (draftVersion != null) {
			vo.setDraftVersion(toVersionSummary(draftVersion));
		}

		return vo;
	}

	// ==================== 创建 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MappingProjectVO create(MappingProjectCreateDTO dto) {
		// 1. 校验 mappingCode 唯一
		Long existing = baseMapper.selectCount(Wrappers.<OntMappingProject>lambdaQuery()
				.eq(OntMappingProject::getMappingCode, dto.getMappingCode())
				.eq(OntMappingProject::getDelFlag, "0"));
		if (existing > 0) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_003.getMessage());
		}

		// 2. 校验本体工程存在且有已发布版本
		OntOntologyProject ontologyProject = ontologyProjectMapper.selectById(dto.getOntologyId());
		if (ontologyProject == null || "1".equals(ontologyProject.getDelFlag())) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_016.getMessage());
		}
		if (ontologyProject.getCurrentVersionId() == null) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_016.getMessage());
		}

		// 3. 校验命名空间存在
		OntNamespace namespace = namespaceMapper.selectById(dto.getDefaultNamespaceId());
		if (namespace == null || "1".equals(namespace.getDelFlag())) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_017.getMessage());
		}

		// 4. 校验本体版本约束表达式合法
		if (!semVerValidator.isValidConstraint(dto.getOntologyVersionConstraint())) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_018.getMessage());
		}

		// 5. 创建工程
		OntMappingProject project = new OntMappingProject();
		project.setMappingCode(dto.getMappingCode());
		project.setMappingName(dto.getMappingName());
		project.setOntologyId(dto.getOntologyId());
		project.setDefaultNamespaceId(dto.getDefaultNamespaceId());
		project.setProjectStatus("DRAFT");
		project.setDescription(dto.getDescription());
		project.setScheduleEnabled("0");
		project.setSecurityLevelCode(
				StrUtil.isBlank(dto.getSecurityLevelCode()) ? "INTERNAL" : dto.getSecurityLevelCode());
		project.setRevision(0L);
		project.setRemarks(dto.getRemarks());

		// 调度字段（初始工程默认不启用）
		if (StrUtil.isNotBlank(dto.getScheduleCron())) {
			project.setScheduleEnabled("0"); // DRAFT 工程不允许启用调度
			project.setScheduleCron(dto.getScheduleCron());
			project.setScheduleRunType(dto.getScheduleRunType());
			project.setExecutionSubjectType(dto.getExecutionSubjectType());
			project.setExecutionSubjectId(dto.getExecutionSubjectId());
		}

		baseMapper.insert(project);

		// 6. 创建初始 DRAFT 版本 0.1.0
		OntMappingVersion version = new OntMappingVersion();
		version.setMappingProjectId(project.getId());
		version.setVersionNumber(INITIAL_VERSION_NUMBER);
		version.setVersionStatus("DRAFT");
		version.setOntologyVersionConstraint(dto.getOntologyVersionConstraint());
		version.setMetadataDependencies("[]");
		version.setReleaseNotes(dto.getReleaseNotes());
		version.setRevision(0L);
		versionMapper.insert(version);

		log.info("Created mapping project: id={}, code={}, initial version={}", project.getId(),
				project.getMappingCode(), INITIAL_VERSION_NUMBER);
		return toVO(project);
	}

	// ==================== 更新 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MappingProjectVO update(MappingProjectUpdateDTO dto) {
		OntMappingProject project = findByIdOrThrow(dto.getId());

		// 已归档工程不可修改
		if ("ARCHIVED".equals(project.getProjectStatus())) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_014.getMessage());
		}

		// 乐观锁校验
		if (!project.getRevision().equals(dto.getRevision())) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_007.getMessage());
		}

		// 更新治理字段
		if (StrUtil.isNotBlank(dto.getMappingName())) {
			project.setMappingName(dto.getMappingName());
		}
		if (dto.getDescription() != null) {
			project.setDescription(dto.getDescription());
		}
		if (StrUtil.isNotBlank(dto.getSecurityLevelCode())) {
			project.setSecurityLevelCode(dto.getSecurityLevelCode());
		}
		if (dto.getScheduleEnabled() != null) {
			// 启用调度要求存在 PUBLISHED 版本
			if ("1".equals(dto.getScheduleEnabled()) && project.getActiveVersionId() == null) {
				throw new IllegalStateException(MappingErrorCode.ONT_MAP_019.getMessage());
			}
			project.setScheduleEnabled(dto.getScheduleEnabled());
		}
		if (dto.getScheduleCron() != null) {
			project.setScheduleCron(dto.getScheduleCron());
		}
		if (dto.getScheduleRunType() != null) {
			project.setScheduleRunType(dto.getScheduleRunType());
		}
		if (dto.getExecutionSubjectType() != null) {
			project.setExecutionSubjectType(dto.getExecutionSubjectType());
		}
		if (dto.getExecutionSubjectId() != null) {
			project.setExecutionSubjectId(dto.getExecutionSubjectId());
		}
		if (dto.getRemarks() != null) {
			project.setRemarks(dto.getRemarks());
		}

		// 递增 revision（乐观锁）
		int updated = baseMapper.incrementRevision(project.getId(), project.getRevision());
		if (updated == 0) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_007.getMessage());
		}
		project.setRevision(project.getRevision() + 1);

		// 重新查询确保数据一致
		project = baseMapper.selectById(dto.getId());

		log.info("Updated mapping project: id={}, revision={}", project.getId(), project.getRevision());
		return toVO(project);
	}

	// ==================== 删除 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long id) {
		OntMappingProject project = findByIdOrThrow(id);

		// 已发布工程不能删除
		if (project.getActiveVersionId() != null) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_020.getMessage());
		}

		// 只允许 DRAFT 状态删除
		if (!"DRAFT".equals(project.getProjectStatus())) {
			throw new IllegalStateException(
					MappingErrorCode.ONT_MAP_004.getMessage() + ": 仅 DRAFT 状态工程可删除");
		}

		// 逻辑删除工程的版本
		versionMapper.update(null, Wrappers.<OntMappingVersion>lambdaUpdate()
				.eq(OntMappingVersion::getMappingProjectId, id)
				.set(OntMappingVersion::getDelFlag, "1"));

		// 逻辑删除工程
		project.setDelFlag("1");
		baseMapper.updateById(project);

		log.info("Removed mapping project: id={}", id);
		return true;
	}

	// ==================== 状态管理 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MappingProjectVO updateStatus(Long id, String status) {
		OntMappingProject project = findByIdOrThrow(id);

		if (!"DRAFT".equals(status) && !"ACTIVE".equals(status)
				&& !"DISABLED".equals(status) && !"ARCHIVED".equals(status)) {
			throw new IllegalArgumentException("非法状态: " + status);
		}

		// 已归档不可重新启用
		if ("ARCHIVED".equals(project.getProjectStatus())) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_014.getMessage());
		}

		switch (status) {
			case "ACTIVE" -> {
				// ACTIVE 需存在 PUBLISHED 版本
				if (project.getActiveVersionId() == null) {
					throw new IllegalStateException(MappingErrorCode.ONT_MAP_006.getMessage());
				}
			}
			case "ARCHIVED" -> {
				// TODO: 校验存在运行中作业不可归档，待 18-07 实现后补充
			}
			case "DISABLED" -> {
				// 允许从 ACTIVE/DRAFT 停用
			}
		}

		project.setProjectStatus(status);
		baseMapper.updateById(project);

		log.info("Updated mapping project status: id={}, status={}", id, status);
		return toVO(project);
	}

	// ==================== 内部方法 ====================

	private OntMappingProject findByIdOrThrow(Long id) {
		OntMappingProject project = baseMapper.selectById(id);
		if (project == null || "1".equals(project.getDelFlag())) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_001.getMessage());
		}
		return project;
	}

	private MappingProjectVO toVO(OntMappingProject project) {
		MappingProjectVO vo = new MappingProjectVO();
		vo.setId(project.getId());
		vo.setMappingCode(project.getMappingCode());
		vo.setMappingName(project.getMappingName());
		vo.setOntologyId(project.getOntologyId());
		vo.setDefaultNamespaceId(project.getDefaultNamespaceId());
		vo.setActiveVersionId(project.getActiveVersionId());
		vo.setProjectStatus(project.getProjectStatus());
		vo.setDescription(project.getDescription());
		vo.setScheduleEnabled(project.getScheduleEnabled());
		vo.setScheduleCron(project.getScheduleCron());
		vo.setScheduleRunType(project.getScheduleRunType());
		vo.setExecutionSubjectType(project.getExecutionSubjectType());
		vo.setExecutionSubjectId(project.getExecutionSubjectId());
		vo.setSecurityLevelCode(project.getSecurityLevelCode());
		vo.setRevision(project.getRevision());
		vo.setLastJobId(project.getLastJobId());
		vo.setRemarks(project.getRemarks());
		vo.setCreateTime(project.getCreateTime());
		vo.setUpdateTime(project.getUpdateTime());
		return vo;
	}

	private MappingProjectVO.VersionSummary toVersionSummary(OntMappingVersion version) {
		MappingProjectVO.VersionSummary summary = new MappingProjectVO.VersionSummary();
		summary.setId(version.getId());
		summary.setVersionNumber(version.getVersionNumber());
		summary.setVersionStatus(version.getVersionStatus());
		summary.setConfigHash(version.getConfigHash());
		summary.setPublishedAt(version.getPublishedAt());
		summary.setCreateTime(version.getCreateTime());
		return summary;
	}

}
