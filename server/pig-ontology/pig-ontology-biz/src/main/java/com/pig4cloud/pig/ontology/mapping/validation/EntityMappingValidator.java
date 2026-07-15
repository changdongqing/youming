/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapping.EntityMappingErrorCode;
import com.pig4cloud.pig.ontology.mapping.compiler.IriTemplateCompiler;
import com.pig4cloud.pig.ontology.mapping.datasource.entity.OntDataSource;
import com.pig4cloud.pig.ontology.mapping.datasource.mapper.OntDataSourceMapper;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingProjectMapper;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 实体映射校验器（18-04 §3~§5）。
 * <p>
 * 静态校验规则：
 * <ul>
 *   <li>key_columns 非空、normalizer 合法</li>
 *   <li>IRI 模板变量必须是 key_columns 中的列</li>
 *   <li>目标实体类型存在且属于映射工程关联的本体</li>
 *   <li>目标命名空间存在</li>
 *   <li>数据源存在</li>
 *   <li>删除策略与失活属性配置一致性</li>
 * </ul>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityMappingValidator {

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntNamespaceMapper namespaceMapper;

	private final OntDataSourceMapper dataSourceMapper;

	private final OntMappingVersionMapper versionMapper;

	private final OntMappingProjectMapper projectMapper;

	private final IriTemplateCompiler iriTemplateCompiler;

	/**
	 * 合法 normalizer 列表。
	 */
	private static final List<String> VALID_NORMALIZERS = List.of(
			"LONG", "TRIM_STRING", "UPPER_STRING", "LOWER_STRING", "URL_ENCODE");

	/**
	 * 校验实体映射配置。
	 * @param iriTemplate IRI 模板
	 * @param keyColumnsJson key_columns JSON
	 * @param targetEntityTypeId 目标实体类型ID
	 * @param targetNamespaceId 目标命名空间ID
	 * @param sourceId 数据源ID
	 * @param mappingVersionId 映射版本ID
	 * @param deleteStrategy 删除策略
	 * @param inactivePropertyId 失活属性ID
	 * @throws IllegalArgumentException 如果校验失败
	 */
	public void validate(String iriTemplate, String keyColumnsJson, Long targetEntityTypeId,
			Long targetNamespaceId, Long sourceId, Long mappingVersionId,
			String deleteStrategy, Long inactivePropertyId) {
		// 1. 校验 key_columns 非空
		if (keyColumnsJson == null || keyColumnsJson.isBlank()) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_101.getMessage()
					+ ": 键列配置为空");
		}

		// 2. 提取键列名并校验 normalizer
		List<String> keyColumnNames = iriTemplateCompiler.extractKeyColumnNames(keyColumnsJson);
		if (keyColumnNames.isEmpty()) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_101.getMessage()
					+ ": 键列配置无法提取列名");
		}
		validateNormalizers(keyColumnsJson);

		// 3. 校验 IRI 模板变量必须是 keyColumns 中的列
		iriTemplateCompiler.validateTemplate(iriTemplate, keyColumnNames);

		// 4. 校验目标实体类型存在
		OntEntityType entityType = entityTypeMapper.selectById(targetEntityTypeId);
		if (entityType == null || "1".equals(entityType.getDelFlag())) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_104.getMessage());
		}

		// 5. 校验目标实体类型属于映射工程关联的本体
		OntMappingVersion version = versionMapper.selectById(mappingVersionId);
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException("映射版本不存在");
		}
		OntMappingProject project = projectMapper.selectById(version.getMappingProjectId());
		if (project == null || "1".equals(project.getDelFlag())) {
			throw new IllegalArgumentException("映射工程不存在");
		}
		if (!entityType.getOntologyId().equals(project.getOntologyId())) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_104.getMessage()
					+ ": 实体类型的本体工程与映射工程不一致");
		}

		// 6. 校验命名空间存在
		OntNamespace namespace = namespaceMapper.selectById(targetNamespaceId);
		if (namespace == null || "1".equals(namespace.getDelFlag())) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_119.getMessage());
		}

		// 7. 校验数据源存在
		OntDataSource dataSource = dataSourceMapper.selectById(sourceId);
		if (dataSource == null || "1".equals(dataSource.getDelFlag())) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_118.getMessage());
		}

		// 8. 校验删除策略与失活属性配置一致性
		if ("MARK_INACTIVE".equals(deleteStrategy) && inactivePropertyId == null) {
			throw new IllegalArgumentException("删除策略为 MARK_INACTIVE 时必须配置失活属性ID");
		}
	}

	/**
	 * 校验 normalizer 合法性。
	 * @param keyColumnsJson key_columns JSON
	 */
	private void validateNormalizers(String keyColumnsJson) {
		java.util.regex.Pattern normalizerPattern = java.util.regex.Pattern.compile(
				"\"normalizer\"\\s*:\\s*\"([^\"]+)\"");
		java.util.regex.Matcher matcher = normalizerPattern.matcher(keyColumnsJson);
		while (matcher.find()) {
			String normalizer = matcher.group(1);
			if (!VALID_NORMALIZERS.contains(normalizer)) {
				throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_101.getMessage()
						+ ": 未知 normalizer " + normalizer);
			}
		}
	}

}
