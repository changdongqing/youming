/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * V1 简化：实体/字段/关系子表暂为空数组（18-04/18-05 实现后填充）。
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

			// V1: 子表为空数组，18-04/18-05 后填充
			snapshot.put("entityMappings", List.of());
			snapshot.put("fieldMappings", List.of());
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
