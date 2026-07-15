/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.ontology.mapping.EntityMappingErrorCode;
import com.pig4cloud.pig.ontology.mapping.dto.FieldMappingCreateDTO;
import com.pig4cloud.pig.ontology.mapping.dto.FieldMappingUpdateDTO;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.entity.OntFieldMapping;
import com.pig4cloud.pig.ontology.mapping.mapper.OntEntityMappingMapper;
import com.pig4cloud.pig.ontology.mapping.mapper.OntFieldMappingMapper;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingVersionMapper;
import com.pig4cloud.pig.ontology.mapping.service.FieldMappingService;
import com.pig4cloud.pig.ontology.mapping.validation.FieldMappingValidator;
import com.pig4cloud.pig.ontology.mapping.vo.FieldMappingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字段映射服务实现（18-04 §12）。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FieldMappingServiceImpl extends ServiceImpl<OntFieldMappingMapper, OntFieldMapping>
		implements FieldMappingService {

	private final OntEntityMappingMapper entityMappingMapper;

	private final OntMappingVersionMapper versionMapper;

	private final FieldMappingValidator validator;

	// ==================== 查询 ====================

	@Override
	public List<FieldMappingVO> listByEntityMapping(Long entityMappingId) {
		List<OntFieldMapping> fields = baseMapper.selectList(
				Wrappers.<OntFieldMapping>lambdaQuery()
						.eq(OntFieldMapping::getEntityMappingId, entityMappingId)
						.eq(OntFieldMapping::getDelFlag, "0")
						.orderByAsc(OntFieldMapping::getSortOrder));
		return fields.stream().map(this::toVO).toList();
	}

	// ==================== 创建 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public FieldMappingVO create(Long entityMappingId, FieldMappingCreateDTO dto) {
		// 1. 校验父实体映射存在
		OntEntityMapping entityMapping = findEntityMappingOrThrow(entityMappingId);

		// 2. 校验版本为 DRAFT
		OntMappingVersion version = versionMapper.selectById(entityMapping.getMappingVersionId());
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException("映射版本不存在");
		}
		if (!"DRAFT".equals(version.getVersionStatus())) {
			throw new IllegalStateException(EntityMappingErrorCode.ONT_MAP_116.getMessage());
		}

		// 3. 校验 fieldMappingCode 唯一
		Long existing = baseMapper.selectCount(Wrappers.<OntFieldMapping>lambdaQuery()
				.eq(OntFieldMapping::getEntityMappingId, entityMappingId)
				.eq(OntFieldMapping::getFieldMappingCode, dto.getFieldMappingCode())
				.eq(OntFieldMapping::getDelFlag, "0"));
		if (existing > 0) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_115.getMessage());
		}

		// 4. 静态校验
		String sourceKind = StrUtil.isBlank(dto.getSourceKind()) ? "COLUMN" : dto.getSourceKind();
		String transformer = StrUtil.isBlank(dto.getTransformer()) ? "IDENTITY" : dto.getTransformer();
		validator.validate(sourceKind, dto.getSourceColumn(), dto.getConstantValue(),
				dto.getTargetDataPropertyId(), transformer, dto.getUnitId(), entityMapping);

		// 5. 创建字段映射
		OntFieldMapping field = new OntFieldMapping();
		field.setEntityMappingId(entityMappingId);
		field.setFieldMappingCode(dto.getFieldMappingCode());
		field.setFieldMappingName(dto.getFieldMappingName());
		field.setTargetDataPropertyId(dto.getTargetDataPropertyId());
		field.setSourceColumn(dto.getSourceColumn());
		field.setSourceKind(sourceKind);
		field.setConstantValue(dto.getConstantValue());
		field.setConstantLiteralType(dto.getConstantLiteralType());
		field.setConstantUnitId(dto.getConstantUnitId());
		field.setTransformer(transformer);
		field.setTransformerParams(dto.getTransformerParams());
		field.setNullHandling(StrUtil.isBlank(dto.getNullHandling()) ? "SKIP_NULL" : dto.getNullHandling());
		field.setDefaultValue(dto.getDefaultValue());
		field.setDefaultLiteralType(dto.getDefaultLiteralType());
		field.setMultiValueStrategy(StrUtil.isBlank(dto.getMultiValueStrategy()) ? "SINGLE"
				: dto.getMultiValueStrategy());
		field.setUnitId(dto.getUnitId());
		field.setOwnershipPolicy(StrUtil.isBlank(dto.getOwnershipPolicy()) ? "SOURCE_WINS"
				: dto.getOwnershipPolicy());
		field.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
		field.setEnabled(StrUtil.isBlank(dto.getEnabled()) ? "1" : dto.getEnabled());
		field.setDescription(dto.getDescription());

		baseMapper.insert(field);

		log.info("Created field mapping: id={}, entityMappingId={}, code={}", field.getId(),
				entityMappingId, field.getFieldMappingCode());
		return toVO(field);
	}

	// ==================== 更新 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public FieldMappingVO update(FieldMappingUpdateDTO dto) {
		OntFieldMapping field = findByIdOrThrow(dto.getId());

		// 校验版本为 DRAFT
		OntEntityMapping entityMapping = entityMappingMapper.selectById(field.getEntityMappingId());
		if (entityMapping == null || "1".equals(entityMapping.getDelFlag())) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_113.getMessage());
		}
		OntMappingVersion version = versionMapper.selectById(entityMapping.getMappingVersionId());
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException("映射版本不存在");
		}
		if (!"DRAFT".equals(version.getVersionStatus())) {
			throw new IllegalStateException(EntityMappingErrorCode.ONT_MAP_116.getMessage());
		}

		// 更新字段
		if (dto.getFieldMappingName() != null) {
			field.setFieldMappingName(dto.getFieldMappingName());
		}
		if (dto.getTargetDataPropertyId() != null) {
			field.setTargetDataPropertyId(dto.getTargetDataPropertyId());
		}
		if (dto.getSourceColumn() != null) {
			field.setSourceColumn(dto.getSourceColumn());
		}
		if (dto.getSourceKind() != null) {
			field.setSourceKind(dto.getSourceKind());
		}
		if (dto.getConstantValue() != null) {
			field.setConstantValue(dto.getConstantValue());
		}
		if (dto.getConstantLiteralType() != null) {
			field.setConstantLiteralType(dto.getConstantLiteralType());
		}
		if (dto.getConstantUnitId() != null) {
			field.setConstantUnitId(dto.getConstantUnitId());
		}
		if (dto.getTransformer() != null) {
			field.setTransformer(dto.getTransformer());
		}
		if (dto.getTransformerParams() != null) {
			field.setTransformerParams(dto.getTransformerParams());
		}
		if (dto.getNullHandling() != null) {
			field.setNullHandling(dto.getNullHandling());
		}
		if (dto.getDefaultValue() != null) {
			field.setDefaultValue(dto.getDefaultValue());
		}
		if (dto.getDefaultLiteralType() != null) {
			field.setDefaultLiteralType(dto.getDefaultLiteralType());
		}
		if (dto.getMultiValueStrategy() != null) {
			field.setMultiValueStrategy(dto.getMultiValueStrategy());
		}
		if (dto.getUnitId() != null) {
			field.setUnitId(dto.getUnitId());
		}
		if (dto.getOwnershipPolicy() != null) {
			field.setOwnershipPolicy(dto.getOwnershipPolicy());
		}
		if (dto.getSortOrder() != null) {
			field.setSortOrder(dto.getSortOrder());
		}
		if (dto.getEnabled() != null) {
			field.setEnabled(dto.getEnabled());
		}
		if (dto.getDescription() != null) {
			field.setDescription(dto.getDescription());
		}

		// 如果关键配置变化，重新校验
		if (dto.getTargetDataPropertyId() != null || dto.getSourceKind() != null
				|| dto.getSourceColumn() != null || dto.getTransformer() != null
				|| dto.getUnitId() != null) {
			validator.validate(field.getSourceKind(), field.getSourceColumn(),
					field.getConstantValue(), field.getTargetDataPropertyId(),
					field.getTransformer(), field.getUnitId(), entityMapping);
		}

		baseMapper.updateById(field);

		log.info("Updated field mapping: id={}", field.getId());
		return toVO(field);
	}

	// ==================== 删除 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long id) {
		OntFieldMapping field = findByIdOrThrow(id);

		// 校验版本为 DRAFT
		OntEntityMapping entityMapping = entityMappingMapper.selectById(field.getEntityMappingId());
		if (entityMapping == null || "1".equals(entityMapping.getDelFlag())) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_113.getMessage());
		}
		OntMappingVersion version = versionMapper.selectById(entityMapping.getMappingVersionId());
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException("映射版本不存在");
		}
		if (!"DRAFT".equals(version.getVersionStatus())) {
			throw new IllegalStateException(EntityMappingErrorCode.ONT_MAP_116.getMessage());
		}

		field.setDelFlag("1");
		baseMapper.updateById(field);

		log.info("Removed field mapping: id={}", id);
		return true;
	}

	// ==================== 内部方法 ====================

	private OntFieldMapping findByIdOrThrow(Long id) {
		OntFieldMapping field = baseMapper.selectById(id);
		if (field == null || "1".equals(field.getDelFlag())) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_114.getMessage());
		}
		return field;
	}

	private OntEntityMapping findEntityMappingOrThrow(Long id) {
		OntEntityMapping entityMapping = entityMappingMapper.selectById(id);
		if (entityMapping == null || "1".equals(entityMapping.getDelFlag())) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_113.getMessage());
		}
		return entityMapping;
	}

	private FieldMappingVO toVO(OntFieldMapping field) {
		FieldMappingVO vo = new FieldMappingVO();
		vo.setId(field.getId());
		vo.setEntityMappingId(field.getEntityMappingId());
		vo.setFieldMappingCode(field.getFieldMappingCode());
		vo.setFieldMappingName(field.getFieldMappingName());
		vo.setTargetDataPropertyId(field.getTargetDataPropertyId());
		vo.setSourceColumn(field.getSourceColumn());
		vo.setSourceKind(field.getSourceKind());
		vo.setConstantValue(field.getConstantValue());
		vo.setConstantLiteralType(field.getConstantLiteralType());
		vo.setConstantUnitId(field.getConstantUnitId());
		vo.setTransformer(field.getTransformer());
		vo.setTransformerParams(field.getTransformerParams());
		vo.setNullHandling(field.getNullHandling());
		vo.setDefaultValue(field.getDefaultValue());
		vo.setDefaultLiteralType(field.getDefaultLiteralType());
		vo.setMultiValueStrategy(field.getMultiValueStrategy());
		vo.setUnitId(field.getUnitId());
		vo.setOwnershipPolicy(field.getOwnershipPolicy());
		vo.setSortOrder(field.getSortOrder());
		vo.setEnabled(field.getEnabled());
		vo.setDescription(field.getDescription());
		vo.setCreateTime(field.getCreateTime());
		vo.setUpdateTime(field.getUpdateTime());
		return vo;
	}

}
