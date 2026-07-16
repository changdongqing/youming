/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.template;

import lombok.Data;

import java.util.List;

/**
 * 映射模板完整定义，对应 mapping-templates/*.json 文件结构。
 * <p>
 * 字段名与 JSON 键一一对应，由 Jackson 自动映射。
 *
 * @author youming
 */
@Data
public class MappingTemplateDefinition {

	private String templateCode;

	private String templateName;

	private String description;

	private String version;

	private String namespacePrefix;

	private Long namespaceId;

	private Long extensionModuleId;

	private Long ontologyId;

	/** 工程创建参数，用于调用 OntMappingProjectService.create */
	private TemplateProjectInfo mappingProject;

	/** 实体映射列表 */
	private List<TemplateEntityMapping> entityMappings;

	/** 关系映射列表 */
	private List<TemplateRelationMapping> relationMappings;

	/**
	 * 工程创建参数。
	 */
	@Data
	public static class TemplateProjectInfo {

		private String mappingCode;

		private String mappingName;

		private Long ontologyId;

		private Long defaultNamespaceId;

		private String ontologyVersionConstraint;

		private String securityLevelCode;

		private String description;

		private String releaseNotes;

	}

	/**
	 * 实体映射模板项，字段对应 EntityMappingCreateDTO。
	 */
	@Data
	public static class TemplateEntityMapping {

		private String mappingCode;

		private String mappingName;

		private String sourceSchema;

		private String sourceObject;

		private String sourceObjectType;

		private String targetEntityTypeCode;

		private Long targetEntityTypeId;

		private Long targetNamespaceId;

		/** JSONB 字符串，格式如 [{"column":"dept_id","order":1,"normalizer":"LONG"}] */
		private String keyColumns;

		private String iriTemplate;

		private String labelTemplate;

		private String filter;

		private String incrementalColumn;

		private String incrementalType;

		private String sourceDeleteFlagColumn;

		/** JSONB 字符串，格式如 ["1","-1"] */
		private String sourceDeleteValues;

		private String deleteStrategy;

		private Long inactivePropertyId;

		private String inactiveLiteralValue;

		private String conflictPolicy;

		private Integer syncOrder;

		/** 字段映射列表 */
		private List<TemplateFieldMapping> fieldMappings;

	}

	/**
	 * 字段映射模板项，字段对应 FieldMappingCreateDTO。
	 */
	@Data
	public static class TemplateFieldMapping {

		private String fieldMappingCode;

		private String fieldMappingName;

		private Long targetDataPropertyId;

		private String targetDataPropertyName;

		private String sourceKind;

		private String sourceColumn;

		private String constantValue;

		private String constantLiteralType;

		private String transformer;

		/** JSONB 字符串 */
		private String transformerParams;

		private String nullHandling;

		private String ownershipPolicy;

		private Integer sortOrder;

	}

	/**
	 * 关系映射模板项，字段对应 RelationMappingCreateDTO。
	 */
	@Data
	public static class TemplateRelationMapping {

		private String mappingCode;

		private String mappingName;

		private String relationMode;

		private Long objectPropertyId;

		private String objectPropertyName;

		private String subjectEntityMappingCode;

		private String objectEntityMappingCode;

		private String sourceSchema;

		private String sourceObject;

		/** JSONB 字符串，格式如 {"subject":[{"sourceColumn":"dept_id","bindingKeyColumn":"dept_id"}]} */
		private String subjectKeyMapping;

		/** JSONB 字符串，格式如 {"object":[{"sourceColumn":"parent_id","bindingKeyColumn":"dept_id"}]} */
		private String objectKeyMapping;

		/** JSONB 字符串，格式如 ["dept_id"] */
		private String relationKeyColumns;

		private String filter;

		private String missingTargetPolicy;

		private String deleteStrategy;

		private String ownershipPolicy;

		private Integer syncOrder;

		private String notes;

	}

}
