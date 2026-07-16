/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.template;

import com.pig4cloud.pig.ontology.mapping.datasource.entity.OntDataSource;
import com.pig4cloud.pig.ontology.mapping.datasource.service.OntDataSourceService;
import com.pig4cloud.pig.ontology.mapping.dto.EntityMappingCreateDTO;
import com.pig4cloud.pig.ontology.mapping.dto.FieldMappingCreateDTO;
import com.pig4cloud.pig.ontology.mapping.dto.RelationMappingCreateDTO;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.project.dto.MappingProjectCreateDTO;
import com.pig4cloud.pig.ontology.mapping.project.service.OntMappingProjectService;
import com.pig4cloud.pig.ontology.mapping.project.vo.MappingProjectVO;
import com.pig4cloud.pig.ontology.mapping.service.EntityMappingService;
import com.pig4cloud.pig.ontology.mapping.service.FieldMappingService;
import com.pig4cloud.pig.ontology.mapping.service.RelationMappingService;
import com.pig4cloud.pig.ontology.mapping.vo.EntityMappingVO;
import com.pig4cloud.pig.ontology.mapping.vo.RelationMappingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 映射模板导入服务实现。
 * <p>
 * 读取 JSON 模板 → 创建工程+初始版本 → 批量创建实体/字段/关系映射。
 * 全流程单一事务，全部成功或全部回滚。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateImportServiceImpl implements TemplateImportService {

	private final MappingTemplateLoader templateLoader;

	private final OntMappingProjectService mappingProjectService;

	private final OntDataSourceService dataSourceService;

	private final EntityMappingService entityMappingService;

	private final FieldMappingService fieldMappingService;

	private final RelationMappingService relationMappingService;

	@Override
	public List<MappingTemplateSummary> listTemplates() {
		return templateLoader.listTemplates();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MappingProjectVO importTemplate(TemplateImportRequest request) {
		// 1. 加载模板定义
		MappingTemplateDefinition template = templateLoader.loadTemplate(request.getTemplateCode());
		MappingTemplateDefinition.TemplateProjectInfo projectInfo = template.getMappingProject();
		if (projectInfo == null) {
			throw new IllegalArgumentException("模板缺少工程创建参数(mappingProject节点): " + request.getTemplateCode());
		}

		// 2. 校验数据源存在且可用
		OntDataSource dataSource = dataSourceService.getById(request.getDataSourceId());
		if (dataSource == null) {
			throw new IllegalArgumentException("数据源不存在: id=" + request.getDataSourceId());
		}
		if (!"ACTIVE".equals(dataSource.getStatus())) {
			throw new IllegalArgumentException("数据源状态不可用(当前=" + dataSource.getStatus() + ")，需为ACTIVE");
		}

		// 3. 创建映射工程 + 初始 DRAFT 版本 0.1.0
		MappingProjectCreateDTO projectDTO = buildProjectDTO(projectInfo, request);
		MappingProjectVO project = mappingProjectService.create(projectDTO);
		log.info("模板导入: 已创建映射工程 id={}, code={}", project.getId(), project.getMappingCode());

		// 4. 获取 draft 版本 ID
		Long versionId = extractDraftVersionId(project);

		// 5. 创建实体映射 + 字段映射，建立 mappingCode→entityMappingId 字典
		Map<String, Long> entityMappingIdMap = new HashMap<>();
		for (MappingTemplateDefinition.TemplateEntityMapping em : template.getEntityMappings()) {
			EntityMappingCreateDTO emDTO = buildEntityMappingDTO(em, request.getDataSourceId());
			EntityMappingVO created = entityMappingService.create(versionId, emDTO);
			entityMappingIdMap.put(em.getMappingCode(), created.getId());
			log.info("模板导入: 已创建实体映射 code={}, id={}", em.getMappingCode(), created.getId());

			// 创建字段映射
			if (em.getFieldMappings() != null) {
				for (MappingTemplateDefinition.TemplateFieldMapping fm : em.getFieldMappings()) {
					FieldMappingCreateDTO fmDTO = buildFieldMappingDTO(fm);
					fieldMappingService.create(created.getId(), fmDTO);
				}
				log.info("模板导入: 已创建字段映射 {} 条, entityMappingCode={}",
						em.getFieldMappings().size(), em.getMappingCode());
			}
		}

		// 6. 创建关系映射，用 mappingCode 字典解析 subject/object 实体映射 ID
		if (template.getRelationMappings() != null) {
			for (MappingTemplateDefinition.TemplateRelationMapping rm : template.getRelationMappings()) {
				Long subjectId = entityMappingIdMap.get(rm.getSubjectEntityMappingCode());
				Long objectId = entityMappingIdMap.get(rm.getObjectEntityMappingCode());
				if (subjectId == null || objectId == null) {
					throw new IllegalArgumentException(
							"关系映射 " + rm.getMappingCode() + " 引用的实体映射编码不存在: subject="
									+ rm.getSubjectEntityMappingCode() + ", object=" + rm.getObjectEntityMappingCode());
				}
				RelationMappingCreateDTO rmDTO = buildRelationMappingDTO(rm, request.getDataSourceId(),
						subjectId, objectId);
				RelationMappingVO created = relationMappingService.create(versionId, rmDTO);
				log.info("模板导入: 已创建关系映射 code={}, id={}", rm.getMappingCode(), created.getId());
			}
		}

		log.info("模板导入完成: templateCode={}, projectId={}, versionId={}",
				request.getTemplateCode(), project.getId(), versionId);
		return project;
	}

	/**
	 * 从 MappingProjectVO 中提取 draft 版本 ID。
	 */
	private Long extractDraftVersionId(MappingProjectVO project) {
		if (project.getDraftVersion() != null && project.getDraftVersion().getId() != null) {
			return project.getDraftVersion().getId();
		}
		// 如果 create 返回的 VO 不含 draft 版本，通过 getDetail 重新获取
		MappingProjectVO detail = mappingProjectService.getDetail(project.getId());
		if (detail.getDraftVersion() != null && detail.getDraftVersion().getId() != null) {
			return detail.getDraftVersion().getId();
		}
		throw new IllegalStateException("映射工程创建后未生成草稿版本: projectId=" + project.getId());
	}

	private MappingProjectCreateDTO buildProjectDTO(MappingTemplateDefinition.TemplateProjectInfo info,
													TemplateImportRequest request) {
		MappingProjectCreateDTO dto = new MappingProjectCreateDTO();
		dto.setMappingCode(request.getMappingCodeOverride() != null && !request.getMappingCodeOverride()
				.isBlank() ? request.getMappingCodeOverride() : info.getMappingCode());
		dto.setMappingName(info.getMappingName());
		dto.setOntologyId(info.getOntologyId());
		dto.setDefaultNamespaceId(info.getDefaultNamespaceId());
		dto.setOntologyVersionConstraint(info.getOntologyVersionConstraint());
		dto.setSecurityLevelCode(info.getSecurityLevelCode());
		dto.setDescription(info.getDescription());
		dto.setReleaseNotes(info.getReleaseNotes());
		return dto;
	}

	private EntityMappingCreateDTO buildEntityMappingDTO(
			MappingTemplateDefinition.TemplateEntityMapping em, Long dataSourceId) {
		EntityMappingCreateDTO dto = new EntityMappingCreateDTO();
		dto.setMappingCode(em.getMappingCode());
		dto.setMappingName(em.getMappingName());
		dto.setSourceId(dataSourceId);
		dto.setSourceSchema(em.getSourceSchema());
		dto.setSourceObject(em.getSourceObject());
		dto.setSourceObjectType(em.getSourceObjectType());
		dto.setTargetEntityTypeId(em.getTargetEntityTypeId());
		dto.setTargetNamespaceId(em.getTargetNamespaceId());
		dto.setKeyColumns(em.getKeyColumns());
		dto.setIriTemplate(em.getIriTemplate());
		dto.setLabelTemplate(em.getLabelTemplate());
		dto.setFilterDsl(em.getFilter());
		dto.setIncrementalColumn(em.getIncrementalColumn());
		dto.setIncrementalType(em.getIncrementalType());
		dto.setSourceDeleteFlagColumn(em.getSourceDeleteFlagColumn());
		dto.setSourceDeleteValues(em.getSourceDeleteValues());
		dto.setDeleteStrategy(em.getDeleteStrategy());
		dto.setInactivePropertyId(em.getInactivePropertyId());
		dto.setInactiveLiteralValue(em.getInactiveLiteralValue());
		dto.setConflictPolicy(em.getConflictPolicy());
		dto.setSyncOrder(em.getSyncOrder());
		dto.setRevision(0L);
		return dto;
	}

	private FieldMappingCreateDTO buildFieldMappingDTO(
			MappingTemplateDefinition.TemplateFieldMapping fm) {
		FieldMappingCreateDTO dto = new FieldMappingCreateDTO();
		dto.setFieldMappingCode(fm.getFieldMappingCode());
		dto.setFieldMappingName(fm.getFieldMappingName());
		dto.setTargetDataPropertyId(fm.getTargetDataPropertyId());
		dto.setSourceColumn(fm.getSourceColumn());
		dto.setSourceKind(fm.getSourceKind());
		dto.setConstantValue(fm.getConstantValue());
		dto.setConstantLiteralType(fm.getConstantLiteralType());
		dto.setTransformer(fm.getTransformer());
		dto.setTransformerParams(fm.getTransformerParams());
		dto.setNullHandling(fm.getNullHandling());
		dto.setOwnershipPolicy(fm.getOwnershipPolicy());
		dto.setSortOrder(fm.getSortOrder());
		return dto;
	}

	private RelationMappingCreateDTO buildRelationMappingDTO(
			MappingTemplateDefinition.TemplateRelationMapping rm, Long dataSourceId,
			Long subjectEntityMappingId, Long objectEntityMappingId) {
		RelationMappingCreateDTO dto = new RelationMappingCreateDTO();
		dto.setMappingCode(rm.getMappingCode());
		dto.setMappingName(rm.getMappingName());
		dto.setRelationMode(rm.getRelationMode());
		dto.setObjectPropertyId(rm.getObjectPropertyId());
		dto.setSubjectEntityMappingId(subjectEntityMappingId);
		dto.setObjectEntityMappingId(objectEntityMappingId);
		dto.setSourceId(dataSourceId);
		dto.setSourceSchema(rm.getSourceSchema());
		dto.setSourceObject(rm.getSourceObject());
		dto.setSubjectKeyMapping(rm.getSubjectKeyMapping());
		dto.setObjectKeyMapping(rm.getObjectKeyMapping());
		dto.setRelationKeyColumns(rm.getRelationKeyColumns());
		dto.setFilterDsl(rm.getFilter());
		dto.setMissingTargetPolicy(rm.getMissingTargetPolicy());
		dto.setDeleteStrategy(rm.getDeleteStrategy());
		dto.setOwnershipPolicy(rm.getOwnershipPolicy());
		dto.setSyncOrder(rm.getSyncOrder());
		dto.setRevision(0L);
		return dto;
	}

}
