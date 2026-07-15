/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.mapping.EntityMappingErrorCode;
import com.pig4cloud.pig.ontology.mapping.compiler.IriTemplateCompiler;
import com.pig4cloud.pig.ontology.mapping.dto.EntityMappingCreateDTO;
import com.pig4cloud.pig.ontology.mapping.dto.EntityMappingUpdateDTO;
import com.pig4cloud.pig.ontology.mapping.dto.IriPreviewRequestDTO;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.entity.OntFieldMapping;
import com.pig4cloud.pig.ontology.mapping.mapper.OntEntityMappingMapper;
import com.pig4cloud.pig.ontology.mapping.mapper.OntFieldMappingMapper;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingVersionMapper;
import com.pig4cloud.pig.ontology.mapping.service.EntityMappingService;
import com.pig4cloud.pig.ontology.mapping.validation.EntityMappingValidator;
import com.pig4cloud.pig.ontology.mapping.vo.EntityMappingVO;
import com.pig4cloud.pig.ontology.mapping.vo.FieldMappingVO;
import com.pig4cloud.pig.ontology.mapping.vo.IriPreviewResultVO;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 实体映射服务实现（18-04 §12）。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityMappingServiceImpl extends ServiceImpl<OntEntityMappingMapper, OntEntityMapping>
		implements EntityMappingService {

	private final OntMappingVersionMapper versionMapper;

	private final OntFieldMappingMapper fieldMappingMapper;

	private final OntNamespaceMapper namespaceMapper;

	private final EntityMappingValidator validator;

	private final IriTemplateCompiler iriTemplateCompiler;

	// ==================== 查询 ====================

	@Override
	public Page<EntityMappingVO> listByVersion(Long versionId, Page<OntEntityMapping> page, String mappingCode,
			String mappingName, Boolean enabledOnly) {
		Page<OntEntityMapping> result = baseMapper.selectPage(page,
				Wrappers.<OntEntityMapping>lambdaQuery()
						.eq(OntEntityMapping::getMappingVersionId, versionId)
						.eq(OntEntityMapping::getDelFlag, "0")
						.like(StrUtil.isNotBlank(mappingCode), OntEntityMapping::getMappingCode, mappingCode)
						.like(StrUtil.isNotBlank(mappingName), OntEntityMapping::getMappingName, mappingName)
						.eq(enabledOnly != null && enabledOnly, OntEntityMapping::getEnabled, "1")
						.orderByAsc(OntEntityMapping::getSyncOrder)
						.orderByDesc(OntEntityMapping::getCreateTime));

		Page<EntityMappingVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
		voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
		return voPage;
	}

	@Override
	public EntityMappingVO getDetail(Long id) {
		OntEntityMapping entityMapping = findByIdOrThrow(id);
		EntityMappingVO vo = toVO(entityMapping);

		// 查询关联的字段映射列表
		List<OntFieldMapping> fields = fieldMappingMapper.selectList(
				Wrappers.<OntFieldMapping>lambdaQuery()
						.eq(OntFieldMapping::getEntityMappingId, id)
						.eq(OntFieldMapping::getDelFlag, "0")
						.orderByAsc(OntFieldMapping::getSortOrder));
		vo.setFieldMappings(fields.stream().map(this::toFieldVO).toList());

		return vo;
	}

	// ==================== 创建 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public EntityMappingVO create(Long versionId, EntityMappingCreateDTO dto) {
		// 1. 校验版本存在且为 DRAFT
		OntMappingVersion version = findVersionOrThrow(versionId);
		if (!"DRAFT".equals(version.getVersionStatus())) {
			throw new IllegalStateException(EntityMappingErrorCode.ONT_MAP_116.getMessage());
		}

		// 2. 校验 mappingCode 唯一
		Long existing = baseMapper.selectCount(Wrappers.<OntEntityMapping>lambdaQuery()
				.eq(OntEntityMapping::getMappingVersionId, versionId)
				.eq(OntEntityMapping::getMappingCode, dto.getMappingCode())
				.eq(OntEntityMapping::getDelFlag, "0"));
		if (existing > 0) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_115.getMessage());
		}

		// 3. 静态校验
		validator.validate(dto.getIriTemplate(), dto.getKeyColumns(), dto.getTargetEntityTypeId(),
				dto.getTargetNamespaceId(), dto.getSourceId(), versionId,
				dto.getDeleteStrategy(), dto.getInactivePropertyId());

		// 4. 创建实体映射
		OntEntityMapping entityMapping = new OntEntityMapping();
		entityMapping.setMappingVersionId(versionId);
		entityMapping.setMappingCode(dto.getMappingCode());
		entityMapping.setMappingName(dto.getMappingName());
		entityMapping.setSourceId(dto.getSourceId());
		entityMapping.setSourceSchema(dto.getSourceSchema());
		entityMapping.setSourceObject(dto.getSourceObject());
		entityMapping.setSourceObjectType(StrUtil.isBlank(dto.getSourceObjectType()) ? "TABLE"
				: dto.getSourceObjectType());
		entityMapping.setTargetEntityTypeId(dto.getTargetEntityTypeId());
		entityMapping.setTargetNamespaceId(dto.getTargetNamespaceId());
		entityMapping.setKeyColumns(dto.getKeyColumns());
		entityMapping.setIriTemplate(dto.getIriTemplate());
		entityMapping.setLabelTemplate(dto.getLabelTemplate());
		entityMapping.setFilterDsl(dto.getFilterDsl());
		entityMapping.setIncrementalColumn(dto.getIncrementalColumn());
		entityMapping.setIncrementalType(dto.getIncrementalType());
		entityMapping.setSourceDeleteFlagColumn(dto.getSourceDeleteFlagColumn());
		entityMapping.setSourceDeleteValues(dto.getSourceDeleteValues());
		entityMapping.setDeleteStrategy(StrUtil.isBlank(dto.getDeleteStrategy()) ? "MARK_INACTIVE"
				: dto.getDeleteStrategy());
		entityMapping.setInactivePropertyId(dto.getInactivePropertyId());
		entityMapping.setInactiveLiteralValue(dto.getInactiveLiteralValue());
		entityMapping.setConflictPolicy(StrUtil.isBlank(dto.getConflictPolicy()) ? "SOURCE_WINS"
				: dto.getConflictPolicy());
		entityMapping.setSyncOrder(dto.getSyncOrder() != null ? dto.getSyncOrder() : 0);
		entityMapping.setEnabled(StrUtil.isBlank(dto.getEnabled()) ? "1" : dto.getEnabled());
		entityMapping.setDescription(dto.getDescription());
		entityMapping.setRevision(dto.getRevision() != null ? dto.getRevision() : 0L);

		baseMapper.insert(entityMapping);

		log.info("Created entity mapping: id={}, versionId={}, code={}", entityMapping.getId(),
				versionId, entityMapping.getMappingCode());
		return toVO(entityMapping);
	}

	// ==================== 更新 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public EntityMappingVO update(EntityMappingUpdateDTO dto) {
		OntEntityMapping entityMapping = findByIdOrThrow(dto.getId());

		// 校验版本为 DRAFT
		OntMappingVersion version = versionMapper.selectById(entityMapping.getMappingVersionId());
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException("映射版本不存在");
		}
		if (!"DRAFT".equals(version.getVersionStatus())) {
			throw new IllegalStateException(EntityMappingErrorCode.ONT_MAP_116.getMessage());
		}

		// 乐观锁校验
		if (!entityMapping.getRevision().equals(dto.getRevision())) {
			throw new IllegalStateException(EntityMappingErrorCode.ONT_MAP_117.getMessage());
		}

		// 更新字段
		if (StrUtil.isNotBlank(dto.getMappingName())) {
			entityMapping.setMappingName(dto.getMappingName());
		}
		if (dto.getTargetEntityTypeId() != null) {
			entityMapping.setTargetEntityTypeId(dto.getTargetEntityTypeId());
		}
		if (dto.getTargetNamespaceId() != null) {
			entityMapping.setTargetNamespaceId(dto.getTargetNamespaceId());
		}
		if (dto.getKeyColumns() != null) {
			entityMapping.setKeyColumns(dto.getKeyColumns());
		}
		if (dto.getIriTemplate() != null) {
			entityMapping.setIriTemplate(dto.getIriTemplate());
		}
		if (dto.getLabelTemplate() != null) {
			entityMapping.setLabelTemplate(dto.getLabelTemplate());
		}
		if (dto.getFilterDsl() != null) {
			entityMapping.setFilterDsl(dto.getFilterDsl());
		}
		if (dto.getIncrementalColumn() != null) {
			entityMapping.setIncrementalColumn(dto.getIncrementalColumn());
		}
		if (dto.getIncrementalType() != null) {
			entityMapping.setIncrementalType(dto.getIncrementalType());
		}
		if (dto.getSourceDeleteFlagColumn() != null) {
			entityMapping.setSourceDeleteFlagColumn(dto.getSourceDeleteFlagColumn());
		}
		if (dto.getSourceDeleteValues() != null) {
			entityMapping.setSourceDeleteValues(dto.getSourceDeleteValues());
		}
		if (dto.getDeleteStrategy() != null) {
			entityMapping.setDeleteStrategy(dto.getDeleteStrategy());
		}
		if (dto.getInactivePropertyId() != null) {
			entityMapping.setInactivePropertyId(dto.getInactivePropertyId());
		}
		if (dto.getInactiveLiteralValue() != null) {
			entityMapping.setInactiveLiteralValue(dto.getInactiveLiteralValue());
		}
		if (dto.getConflictPolicy() != null) {
			entityMapping.setConflictPolicy(dto.getConflictPolicy());
		}
		if (dto.getSyncOrder() != null) {
			entityMapping.setSyncOrder(dto.getSyncOrder());
		}
		if (dto.getEnabled() != null) {
			entityMapping.setEnabled(dto.getEnabled());
		}
		if (dto.getDescription() != null) {
			entityMapping.setDescription(dto.getDescription());
		}
		if (dto.getSourceObjectType() != null) {
			entityMapping.setSourceObjectType(dto.getSourceObjectType());
		}

		// 如果关键配置变化，重新校验
		if (dto.getIriTemplate() != null || dto.getKeyColumns() != null
				|| dto.getTargetEntityTypeId() != null || dto.getTargetNamespaceId() != null
				|| dto.getDeleteStrategy() != null || dto.getInactivePropertyId() != null) {
			validator.validate(entityMapping.getIriTemplate(), entityMapping.getKeyColumns(),
					entityMapping.getTargetEntityTypeId(), entityMapping.getTargetNamespaceId(),
					entityMapping.getSourceId(), entityMapping.getMappingVersionId(),
					entityMapping.getDeleteStrategy(), entityMapping.getInactivePropertyId());
		}

		// 递增 revision（乐观锁）
		int updated = baseMapper.incrementRevision(entityMapping.getId(), entityMapping.getRevision());
		if (updated == 0) {
			throw new IllegalStateException(EntityMappingErrorCode.ONT_MAP_117.getMessage());
		}
		entityMapping.setRevision(entityMapping.getRevision() + 1);

		baseMapper.updateById(entityMapping);

		log.info("Updated entity mapping: id={}, revision={}", entityMapping.getId(),
				entityMapping.getRevision());
		return toVO(entityMapping);
	}

	// ==================== 删除 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long id) {
		OntEntityMapping entityMapping = findByIdOrThrow(id);

		// 校验版本为 DRAFT
		OntMappingVersion version = versionMapper.selectById(entityMapping.getMappingVersionId());
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException("映射版本不存在");
		}
		if (!"DRAFT".equals(version.getVersionStatus())) {
			throw new IllegalStateException(EntityMappingErrorCode.ONT_MAP_116.getMessage());
		}

		// 逻辑删除字段映射（CASCADE 级联）
		fieldMappingMapper.update(null, Wrappers.<OntFieldMapping>lambdaUpdate()
				.eq(OntFieldMapping::getEntityMappingId, id)
				.set(OntFieldMapping::getDelFlag, "1"));

		// 逻辑删除实体映射
		entityMapping.setDelFlag("1");
		baseMapper.updateById(entityMapping);

		log.info("Removed entity mapping: id={}", id);
		return true;
	}

	// ==================== IRI 预览 ====================

	@Override
	public IriPreviewResultVO iriPreview(Long id, IriPreviewRequestDTO request) {
		OntEntityMapping entityMapping = findByIdOrThrow(id);

		// 查询命名空间 URI
		OntNamespace namespace = namespaceMapper.selectById(entityMapping.getTargetNamespaceId());
		if (namespace == null || "1".equals(namespace.getDelFlag())) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_119.getMessage());
		}

		IriPreviewResultVO result = new IriPreviewResultVO();
		result.setNamespaceUri(namespace.getUri());

		try {
			Map<String, String> sampleValues = request.getSampleKeyValues();
			String localName = iriTemplateCompiler.renderTemplate(entityMapping.getIriTemplate(),
					sampleValues);
			String fullIri = iriTemplateCompiler.renderIri(namespace.getUri(),
					entityMapping.getIriTemplate(), sampleValues);
			result.setLocalName(localName);
			result.setFullIri(fullIri);

			// 渲染标签（如配置了标签模板）
			if (StrUtil.isNotBlank(entityMapping.getLabelTemplate())) {
				String label = iriTemplateCompiler.renderTemplate(entityMapping.getLabelTemplate(),
						sampleValues);
				result.setLabel(StrUtil.isBlank(label) ? localName : label);
			}
			else {
				result.setLabel(localName);
			}
			result.setSuccess(true);
		}
		catch (Exception e) {
			result.setSuccess(false);
			result.setErrorMessage(e.getMessage());
		}

		return result;
	}

	// ==================== 内部方法 ====================

	private OntEntityMapping findByIdOrThrow(Long id) {
		OntEntityMapping entityMapping = baseMapper.selectById(id);
		if (entityMapping == null || "1".equals(entityMapping.getDelFlag())) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_113.getMessage());
		}
		return entityMapping;
	}

	private OntMappingVersion findVersionOrThrow(Long versionId) {
		OntMappingVersion version = versionMapper.selectById(versionId);
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException("映射版本不存在");
		}
		return version;
	}

	private EntityMappingVO toVO(OntEntityMapping entityMapping) {
		EntityMappingVO vo = new EntityMappingVO();
		vo.setId(entityMapping.getId());
		vo.setMappingVersionId(entityMapping.getMappingVersionId());
		vo.setMappingCode(entityMapping.getMappingCode());
		vo.setMappingName(entityMapping.getMappingName());
		vo.setSourceId(entityMapping.getSourceId());
		vo.setSourceSchema(entityMapping.getSourceSchema());
		vo.setSourceObject(entityMapping.getSourceObject());
		vo.setSourceObjectType(entityMapping.getSourceObjectType());
		vo.setTargetEntityTypeId(entityMapping.getTargetEntityTypeId());
		vo.setTargetNamespaceId(entityMapping.getTargetNamespaceId());
		vo.setKeyColumns(entityMapping.getKeyColumns());
		vo.setIriTemplate(entityMapping.getIriTemplate());
		vo.setLabelTemplate(entityMapping.getLabelTemplate());
		vo.setFilterDsl(entityMapping.getFilterDsl());
		vo.setIncrementalColumn(entityMapping.getIncrementalColumn());
		vo.setIncrementalType(entityMapping.getIncrementalType());
		vo.setSourceDeleteFlagColumn(entityMapping.getSourceDeleteFlagColumn());
		vo.setSourceDeleteValues(entityMapping.getSourceDeleteValues());
		vo.setDeleteStrategy(entityMapping.getDeleteStrategy());
		vo.setInactivePropertyId(entityMapping.getInactivePropertyId());
		vo.setInactiveLiteralValue(entityMapping.getInactiveLiteralValue());
		vo.setConflictPolicy(entityMapping.getConflictPolicy());
		vo.setSyncOrder(entityMapping.getSyncOrder());
		vo.setEnabled(entityMapping.getEnabled());
		vo.setDescription(entityMapping.getDescription());
		vo.setRevision(entityMapping.getRevision());
		vo.setCreateTime(entityMapping.getCreateTime());
		vo.setUpdateTime(entityMapping.getUpdateTime());
		return vo;
	}

	private FieldMappingVO toFieldVO(OntFieldMapping field) {
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
