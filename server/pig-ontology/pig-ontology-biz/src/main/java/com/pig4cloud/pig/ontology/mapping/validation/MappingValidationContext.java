/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation;

import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.mapping.datasource.entity.OntDataSource;
import com.pig4cloud.pig.ontology.mapping.datasource.entity.OntDataSourceMetadata;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.entity.OntFieldMapping;
import com.pig4cloud.pig.ontology.mapping.entity.OntRelationMapping;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.version.entity.OntOntologyVersion;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 映射校验上下文（18-06 §7）。
 * <p>
 * 在校验开始时一次性加载全部相关数据，供所有校验器共享。
 * 当某 P0 前置校验失败时，调用 {@link #markPrerequisiteFailed()} 标记，
 * 后续依赖校验器应检查 {@link #isPrerequisiteFailed()} 并跳过。
 *
 * @author youming
 */
@Getter
public class MappingValidationContext {

	private final OntMappingVersion version;

	private final OntMappingProject mappingProject;

	private final OntOntologyProject ontologyProject;

	private final OntOntologyVersion ontologyVersion;

	private final List<OntEntityMapping> entityMappings;

	private final List<OntFieldMapping> fieldMappings;

	private final List<OntRelationMapping> relationMappings;

	private final Map<Long, OntDataSource> dataSourceMap;

	private final Map<Long, List<OntDataSourceMetadata>> metadataMap;

	/** 候选配置哈希（由快照构建器计算） */
	private final String candidateConfigHash;

	/** 元数据依赖哈希摘要 */
	private final String metadataHashSummary;

	@Getter
	private boolean prerequisiteFailed = false;

	public MappingValidationContext(OntMappingVersion version, OntMappingProject mappingProject,
			OntOntologyProject ontologyProject, OntOntologyVersion ontologyVersion,
			List<OntEntityMapping> entityMappings, List<OntFieldMapping> fieldMappings,
			List<OntRelationMapping> relationMappings, Map<Long, OntDataSource> dataSourceMap,
			Map<Long, List<OntDataSourceMetadata>> metadataMap, String candidateConfigHash,
			String metadataHashSummary) {
		this.version = version;
		this.mappingProject = mappingProject;
		this.ontologyProject = ontologyProject;
		this.ontologyVersion = ontologyVersion;
		this.entityMappings = entityMappings;
		this.fieldMappings = fieldMappings;
		this.relationMappings = relationMappings;
		this.dataSourceMap = dataSourceMap;
		this.metadataMap = metadataMap;
		this.candidateConfigHash = candidateConfigHash;
		this.metadataHashSummary = metadataHashSummary;
	}

	/**
	 * 标记前置条件已失败。
	 */
	public void markPrerequisiteFailed() {
		this.prerequisiteFailed = true;
	}

	/**
	 * 获取启用的实体映射列表。
	 */
	public List<OntEntityMapping> getEnabledEntityMappings() {
		return entityMappings.stream()
				.filter(em -> "1".equals(em.getEnabled()))
				.toList();
	}

	/**
	 * 获取指定实体映射下的字段映射。
	 */
	public List<OntFieldMapping> getFieldMappings(Long entityMappingId) {
		return fieldMappings.stream()
				.filter(fm -> entityMappingId.equals(fm.getEntityMappingId()))
				.toList();
	}

}
