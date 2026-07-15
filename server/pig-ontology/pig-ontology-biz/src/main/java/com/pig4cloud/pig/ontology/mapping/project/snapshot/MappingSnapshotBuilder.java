/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.snapshot;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.entity.OntFieldMapping;
import com.pig4cloud.pig.ontology.mapping.mapper.OntEntityMappingMapper;
import com.pig4cloud.pig.ontology.mapping.mapper.OntFieldMappingMapper;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 映射配置快照构建器（18-03 §6）。
 * <p>
 * 发布时将工程信息、本体绑定和映射子表规范化写入 {@code config_snapshot}。
 * 18-04 起填充实体映射和字段映射子表；关系映射子表待 18-05 后填充。
 * <p>
 * 不包含审计字段、凭证、最近作业和 UI 临时坐标。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MappingSnapshotBuilder {

	private final ObjectMapper objectMapper;

	private final OntEntityMappingMapper entityMappingMapper;

	private final OntFieldMappingMapper fieldMappingMapper;

	private static final int SNAPSHOT_FORMAT_VERSION = 1;

	/**
	 * 构建配置快照 JSON 字符串。
	 * @param project 映射工程
	 * @param version 映射版本
	 * @param validatedVersionNumber 校验时的本体版本号
	 * @return 规范化的 JSON 字符串
	 */
	public String buildSnapshot(OntMappingProject project, OntMappingVersion version,
			String validatedVersionNumber) {
		try {
			// 使用 TreeMap 保证键按字典序排列
			Map<String, Object> snapshot = new TreeMap<>();

			snapshot.put("snapshotFormatVersion", SNAPSHOT_FORMAT_VERSION);

			// mappingProject（不含审计字段）
			Map<String, Object> mappingProject = new TreeMap<>();
			mappingProject.put("id", project.getId());
			mappingProject.put("code", project.getMappingCode());
			mappingProject.put("ontologyId", project.getOntologyId());
			mappingProject.put("defaultNamespaceId", project.getDefaultNamespaceId());
			snapshot.put("mappingProject", mappingProject);

			// ontologyBinding
			Map<String, Object> ontologyBinding = new TreeMap<>();
			ontologyBinding.put("constraint", version.getOntologyVersionConstraint());
			ontologyBinding.put("validatedVersionId", version.getValidatedOntologyVersionId());
			ontologyBinding.put("validatedVersionNumber", validatedVersionNumber);
			ontologyBinding.put("workspaceRevision", version.getValidatedWorkspaceRevision());
			snapshot.put("ontologyBinding", ontologyBinding);

			// 18-04: 填充实体映射和字段映射子表
			List<OntEntityMapping> entityMappings = entityMappingMapper.selectList(
					Wrappers.<OntEntityMapping>lambdaQuery()
							.eq(OntEntityMapping::getMappingVersionId, version.getId())
							.eq(OntEntityMapping::getDelFlag, "0")
							.orderByAsc(OntEntityMapping::getSyncOrder));

			List<Map<String, Object>> entityMappingSnapshots = new java.util.ArrayList<>();
			List<Map<String, Object>> fieldMappingSnapshots = new java.util.ArrayList<>();

			for (OntEntityMapping em : entityMappings) {
				Map<String, Object> emSnapshot = new TreeMap<>();
				emSnapshot.put("code", em.getMappingCode());
				emSnapshot.put("conflictPolicy", em.getConflictPolicy());
				emSnapshot.put("deleteStrategy", em.getDeleteStrategy());
				emSnapshot.put("enabled", em.getEnabled());
				emSnapshot.put("id", em.getId());
				emSnapshot.put("iriTemplate", em.getIriTemplate());
				emSnapshot.put("keyColumns", em.getKeyColumns());
				emSnapshot.put("labelTemplate", em.getLabelTemplate());
				emSnapshot.put("mappingName", em.getMappingName());
				emSnapshot.put("sourceId", em.getSourceId());
				emSnapshot.put("sourceObject", em.getSourceObject());
				emSnapshot.put("sourceObjectType", em.getSourceObjectType());
				emSnapshot.put("sourceSchema", em.getSourceSchema());
				emSnapshot.put("syncOrder", em.getSyncOrder());
				emSnapshot.put("targetEntityTypeId", em.getTargetEntityTypeId());
				emSnapshot.put("targetNamespaceId", em.getTargetNamespaceId());
				entityMappingSnapshots.add(emSnapshot);

				// 查询该实体映射下的字段映射
				List<OntFieldMapping> fieldMappings = fieldMappingMapper.selectList(
						Wrappers.<OntFieldMapping>lambdaQuery()
								.eq(OntFieldMapping::getEntityMappingId, em.getId())
								.eq(OntFieldMapping::getDelFlag, "0")
								.orderByAsc(OntFieldMapping::getSortOrder));

				for (OntFieldMapping fm : fieldMappings) {
					Map<String, Object> fmSnapshot = new TreeMap<>();
					fmSnapshot.put("constantValue", fm.getConstantValue());
					fmSnapshot.put("entityMappingId", fm.getEntityMappingId());
					fmSnapshot.put("fieldMappingCode", fm.getFieldMappingCode());
					fmSnapshot.put("fieldMappingName", fm.getFieldMappingName());
					fmSnapshot.put("id", fm.getId());
					fmSnapshot.put("multiValueStrategy", fm.getMultiValueStrategy());
					fmSnapshot.put("nullHandling", fm.getNullHandling());
					fmSnapshot.put("ownershipPolicy", fm.getOwnershipPolicy());
					fmSnapshot.put("sourceColumn", fm.getSourceColumn());
					fmSnapshot.put("sourceKind", fm.getSourceKind());
					fmSnapshot.put("sortOrder", fm.getSortOrder());
					fmSnapshot.put("targetDataPropertyId", fm.getTargetDataPropertyId());
					fmSnapshot.put("transformer", fm.getTransformer());
					fmSnapshot.put("transformerParams", fm.getTransformerParams());
					fmSnapshot.put("unitId", fm.getUnitId());
					fieldMappingSnapshots.add(fmSnapshot);
				}
			}

			snapshot.put("entityMappings", entityMappingSnapshots);
			snapshot.put("fieldMappings", fieldMappingSnapshots);
			// 18-05: 关系映射子表待实现后填充
			snapshot.put("relationMappings", List.of());

			// 元数据依赖
			snapshot.put("metadataDependencies", parseMetadataDependencies(version.getMetadataDependencies()));

			return objectMapper.writeValueAsString(snapshot);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to build mapping snapshot", e);
		}
	}

	/**
	 * 解析元数据依赖 JSON。
	 */
	private List<Object> parseMetadataDependencies(String json) {
		if (json == null || json.isBlank() || "[]".equals(json.trim())) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json,
					objectMapper.getTypeFactory().constructCollectionType(List.class, Object.class));
		}
		catch (Exception e) {
			log.warn("Failed to parse metadataDependencies, using empty list: {}", e.getMessage());
			return List.of();
		}
	}

}
