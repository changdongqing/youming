/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.ontology.mapping.RelationMappingErrorCode;
import com.pig4cloud.pig.ontology.mapping.compiler.RelationKeyCompiler;
import com.pig4cloud.pig.ontology.mapping.dto.RelationKeyPreviewRequestDTO;
import com.pig4cloud.pig.ontology.mapping.dto.RelationMappingCreateDTO;
import com.pig4cloud.pig.ontology.mapping.dto.RelationMappingUpdateDTO;
import com.pig4cloud.pig.ontology.mapping.entity.OntRelationMapping;
import com.pig4cloud.pig.ontology.mapping.mapper.OntRelationMappingMapper;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingVersionMapper;
import com.pig4cloud.pig.ontology.mapping.service.RelationMappingService;
import com.pig4cloud.pig.ontology.mapping.validation.RelationMappingValidator;
import com.pig4cloud.pig.ontology.mapping.vo.RelationKeyPreviewResultVO;
import com.pig4cloud.pig.ontology.mapping.vo.RelationMappingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 关系映射服务实现（18-05 §13）。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelationMappingServiceImpl extends ServiceImpl<OntRelationMappingMapper, OntRelationMapping>
		implements RelationMappingService {

	private final OntMappingVersionMapper versionMapper;

	private final RelationMappingValidator validator;

	private final RelationKeyCompiler relationKeyCompiler;

	// ==================== 查询 ====================

	@Override
	public Page<RelationMappingVO> listByVersion(Long versionId, Page<OntRelationMapping> page, String mappingCode,
			String mappingName, Boolean enabledOnly) {
		Page<OntRelationMapping> result = baseMapper.selectPage(page,
				Wrappers.<OntRelationMapping>lambdaQuery()
						.eq(OntRelationMapping::getMappingVersionId, versionId)
						.eq(OntRelationMapping::getDelFlag, "0")
						.like(StrUtil.isNotBlank(mappingCode), OntRelationMapping::getMappingCode, mappingCode)
						.like(StrUtil.isNotBlank(mappingName), OntRelationMapping::getMappingName, mappingName)
						.eq(enabledOnly != null && enabledOnly, OntRelationMapping::getEnabled, "1")
						.orderByAsc(OntRelationMapping::getSyncOrder)
						.orderByDesc(OntRelationMapping::getCreateTime));

		Page<RelationMappingVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
		voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
		return voPage;
	}

	@Override
	public RelationMappingVO getDetail(Long id) {
		return toVO(findByIdOrThrow(id));
	}

	// ==================== 创建 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public RelationMappingVO create(Long versionId, RelationMappingCreateDTO dto) {
		// 1. 校验版本存在且为 DRAFT
		OntMappingVersion version = findVersionOrThrow(versionId);
		if (!"DRAFT".equals(version.getVersionStatus())) {
			throw new IllegalStateException(RelationMappingErrorCode.ONT_REL_003.getMessage());
		}

		// 2. 校验 mappingCode 唯一
		Long existing = baseMapper.selectCount(Wrappers.<OntRelationMapping>lambdaQuery()
				.eq(OntRelationMapping::getMappingVersionId, versionId)
				.eq(OntRelationMapping::getMappingCode, dto.getMappingCode())
				.eq(OntRelationMapping::getDelFlag, "0"));
		if (existing > 0) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_002.getMessage());
		}

		// 3. 静态校验
		validator.validate(dto.getRelationMode(), dto.getObjectPropertyId(),
				dto.getSubjectEntityMappingId(), dto.getObjectEntityMappingId(), dto.getSourceId(),
				dto.getSubjectKeyMapping(), dto.getObjectKeyMapping(), dto.getRelationKeyColumns(),
				versionId);

		// 4. 创建关系映射
		OntRelationMapping relationMapping = new OntRelationMapping();
		relationMapping.setMappingVersionId(versionId);
		relationMapping.setMappingCode(dto.getMappingCode());
		relationMapping.setMappingName(dto.getMappingName());
		relationMapping.setRelationMode(dto.getRelationMode());
		relationMapping.setObjectPropertyId(dto.getObjectPropertyId());
		relationMapping.setSubjectEntityMappingId(dto.getSubjectEntityMappingId());
		relationMapping.setObjectEntityMappingId(dto.getObjectEntityMappingId());
		relationMapping.setSourceId(dto.getSourceId());
		relationMapping.setSourceSchema(dto.getSourceSchema());
		relationMapping.setSourceObject(dto.getSourceObject());
		relationMapping.setSubjectKeyMapping(dto.getSubjectKeyMapping());
		relationMapping.setObjectKeyMapping(dto.getObjectKeyMapping());
		relationMapping.setRelationKeyColumns(dto.getRelationKeyColumns());
		relationMapping.setFilterDsl(dto.getFilterDsl());
		relationMapping.setMissingTargetPolicy(StrUtil.isBlank(dto.getMissingTargetPolicy()) ? "PENDING"
				: dto.getMissingTargetPolicy());
		relationMapping.setDeleteStrategy(StrUtil.isBlank(dto.getDeleteStrategy()) ? "REMOVE_ASSERTION"
				: dto.getDeleteStrategy());
		relationMapping.setOwnershipPolicy(StrUtil.isBlank(dto.getOwnershipPolicy()) ? "SOURCE_WINS"
				: dto.getOwnershipPolicy());
		relationMapping.setSyncOrder(dto.getSyncOrder() != null ? dto.getSyncOrder() : 1000);
		relationMapping.setEnabled(StrUtil.isBlank(dto.getEnabled()) ? "1" : dto.getEnabled());
		relationMapping.setDescription(dto.getDescription());
		relationMapping.setRevision(dto.getRevision() != null ? dto.getRevision() : 0L);

		baseMapper.insert(relationMapping);

		log.info("Created relation mapping: id={}, versionId={}, code={}", relationMapping.getId(),
				versionId, relationMapping.getMappingCode());
		return toVO(relationMapping);
	}

	// ==================== 更新 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public RelationMappingVO update(RelationMappingUpdateDTO dto) {
		OntRelationMapping relationMapping = findByIdOrThrow(dto.getId());

		// 校验版本为 DRAFT
		OntMappingVersion version = versionMapper.selectById(relationMapping.getMappingVersionId());
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException("映射版本不存在");
		}
		if (!"DRAFT".equals(version.getVersionStatus())) {
			throw new IllegalStateException(RelationMappingErrorCode.ONT_REL_003.getMessage());
		}

		// 乐观锁校验
		if (!relationMapping.getRevision().equals(dto.getRevision())) {
			throw new IllegalStateException(RelationMappingErrorCode.ONT_REL_004.getMessage());
		}

		// 更新字段
		if (StrUtil.isNotBlank(dto.getMappingName())) {
			relationMapping.setMappingName(dto.getMappingName());
		}
		if (dto.getRelationMode() != null) {
			relationMapping.setRelationMode(dto.getRelationMode());
		}
		if (dto.getObjectPropertyId() != null) {
			relationMapping.setObjectPropertyId(dto.getObjectPropertyId());
		}
		if (dto.getSubjectEntityMappingId() != null) {
			relationMapping.setSubjectEntityMappingId(dto.getSubjectEntityMappingId());
		}
		if (dto.getObjectEntityMappingId() != null) {
			relationMapping.setObjectEntityMappingId(dto.getObjectEntityMappingId());
		}
		if (dto.getSubjectKeyMapping() != null) {
			relationMapping.setSubjectKeyMapping(dto.getSubjectKeyMapping());
		}
		if (dto.getObjectKeyMapping() != null) {
			relationMapping.setObjectKeyMapping(dto.getObjectKeyMapping());
		}
		if (dto.getRelationKeyColumns() != null) {
			relationMapping.setRelationKeyColumns(dto.getRelationKeyColumns());
		}
		if (dto.getFilterDsl() != null) {
			relationMapping.setFilterDsl(dto.getFilterDsl());
		}
		if (dto.getMissingTargetPolicy() != null) {
			relationMapping.setMissingTargetPolicy(dto.getMissingTargetPolicy());
		}
		if (dto.getDeleteStrategy() != null) {
			relationMapping.setDeleteStrategy(dto.getDeleteStrategy());
		}
		if (dto.getOwnershipPolicy() != null) {
			relationMapping.setOwnershipPolicy(dto.getOwnershipPolicy());
		}
		if (dto.getSyncOrder() != null) {
			relationMapping.setSyncOrder(dto.getSyncOrder());
		}
		if (dto.getEnabled() != null) {
			relationMapping.setEnabled(dto.getEnabled());
		}
		if (dto.getDescription() != null) {
			relationMapping.setDescription(dto.getDescription());
		}

		// 如果关键配置变化，重新校验
		if (dto.getRelationMode() != null || dto.getObjectPropertyId() != null
				|| dto.getSubjectEntityMappingId() != null || dto.getObjectEntityMappingId() != null
				|| dto.getSubjectKeyMapping() != null || dto.getObjectKeyMapping() != null
				|| dto.getRelationKeyColumns() != null) {
			validator.validate(relationMapping.getRelationMode(), relationMapping.getObjectPropertyId(),
					relationMapping.getSubjectEntityMappingId(), relationMapping.getObjectEntityMappingId(),
					relationMapping.getSourceId(), relationMapping.getSubjectKeyMapping(),
					relationMapping.getObjectKeyMapping(), relationMapping.getRelationKeyColumns(),
					relationMapping.getMappingVersionId());
		}

		// 递增 revision（乐观锁）
		int updated = baseMapper.incrementRevision(relationMapping.getId(), relationMapping.getRevision());
		if (updated == 0) {
			throw new IllegalStateException(RelationMappingErrorCode.ONT_REL_004.getMessage());
		}
		relationMapping.setRevision(relationMapping.getRevision() + 1);

		baseMapper.updateById(relationMapping);

		log.info("Updated relation mapping: id={}, revision={}", relationMapping.getId(),
				relationMapping.getRevision());
		return toVO(relationMapping);
	}

	// ==================== 删除 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long id) {
		OntRelationMapping relationMapping = findByIdOrThrow(id);

		// 校验版本为 DRAFT
		OntMappingVersion version = versionMapper.selectById(relationMapping.getMappingVersionId());
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException("映射版本不存在");
		}
		if (!"DRAFT".equals(version.getVersionStatus())) {
			throw new IllegalStateException(RelationMappingErrorCode.ONT_REL_003.getMessage());
		}

		relationMapping.setDelFlag("1");
		baseMapper.updateById(relationMapping);

		log.info("Removed relation mapping: id={}", id);
		return true;
	}

	// ==================== 键预览 ====================

	@Override
	public RelationKeyPreviewResultVO keyPreview(Long id, RelationKeyPreviewRequestDTO request) {
		OntRelationMapping relationMapping = findByIdOrThrow(id);

		RelationKeyPreviewResultVO result = new RelationKeyPreviewResultVO();
		try {
			String subjectKey = relationKeyCompiler.compileSubjectKey(
					relationMapping.getSubjectKeyMapping(), request.getSampleValues());
			String objectKey = relationKeyCompiler.compileObjectKey(
					relationMapping.getObjectKeyMapping(), request.getSampleValues());
			String relationKey = relationKeyCompiler.compileRelationKey(
					relationMapping.getRelationKeyColumns(), request.getSampleValues());

			result.setSubjectRecordKey(subjectKey);
			result.setSubjectRecordKeyHash(relationKeyCompiler.computeHash(subjectKey));
			result.setObjectRecordKey(objectKey);
			result.setObjectRecordKeyHash(relationKeyCompiler.computeHash(objectKey));
			result.setRelationKey(relationKey);
			result.setRelationKeyHash(relationKeyCompiler.computeHash(relationKey));
			result.setSuccess(true);
		}
		catch (Exception e) {
			result.setSuccess(false);
			result.setErrorMessage(e.getMessage());
		}

		return result;
	}

	// ==================== 内部方法 ====================

	private OntRelationMapping findByIdOrThrow(Long id) {
		OntRelationMapping relationMapping = baseMapper.selectById(id);
		if (relationMapping == null || "1".equals(relationMapping.getDelFlag())) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_001.getMessage());
		}
		return relationMapping;
	}

	private OntMappingVersion findVersionOrThrow(Long versionId) {
		OntMappingVersion version = versionMapper.selectById(versionId);
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException("映射版本不存在");
		}
		return version;
	}

	private RelationMappingVO toVO(OntRelationMapping relationMapping) {
		RelationMappingVO vo = new RelationMappingVO();
		vo.setId(relationMapping.getId());
		vo.setMappingVersionId(relationMapping.getMappingVersionId());
		vo.setMappingCode(relationMapping.getMappingCode());
		vo.setMappingName(relationMapping.getMappingName());
		vo.setRelationMode(relationMapping.getRelationMode());
		vo.setObjectPropertyId(relationMapping.getObjectPropertyId());
		vo.setSubjectEntityMappingId(relationMapping.getSubjectEntityMappingId());
		vo.setObjectEntityMappingId(relationMapping.getObjectEntityMappingId());
		vo.setSourceId(relationMapping.getSourceId());
		vo.setSourceSchema(relationMapping.getSourceSchema());
		vo.setSourceObject(relationMapping.getSourceObject());
		vo.setSubjectKeyMapping(relationMapping.getSubjectKeyMapping());
		vo.setObjectKeyMapping(relationMapping.getObjectKeyMapping());
		vo.setRelationKeyColumns(relationMapping.getRelationKeyColumns());
		vo.setFilterDsl(relationMapping.getFilterDsl());
		vo.setMissingTargetPolicy(relationMapping.getMissingTargetPolicy());
		vo.setDeleteStrategy(relationMapping.getDeleteStrategy());
		vo.setOwnershipPolicy(relationMapping.getOwnershipPolicy());
		vo.setSyncOrder(relationMapping.getSyncOrder());
		vo.setEnabled(relationMapping.getEnabled());
		vo.setDescription(relationMapping.getDescription());
		vo.setRevision(relationMapping.getRevision());
		vo.setCreateTime(relationMapping.getCreateTime());
		vo.setUpdateTime(relationMapping.getUpdateTime());
		return vo;
	}

}
