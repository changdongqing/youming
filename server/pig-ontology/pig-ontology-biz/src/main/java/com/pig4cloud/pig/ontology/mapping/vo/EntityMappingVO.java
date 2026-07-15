/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 实体映射详情 VO（18-04 §12）。
 *
 * @author youming
 */
@Data
@Schema(description = "实体映射详情")
public class EntityMappingVO {

	@Schema(description = "实体映射ID")
	private Long id;

	@Schema(description = "映射版本ID")
	private Long mappingVersionId;

	@Schema(description = "映射编码")
	private String mappingCode;

	@Schema(description = "映射名称")
	private String mappingName;

	@Schema(description = "数据源ID")
	private Long sourceId;

	@Schema(description = "源Schema名")
	private String sourceSchema;

	@Schema(description = "源对象名")
	private String sourceObject;

	@Schema(description = "源对象类型")
	private String sourceObjectType;

	@Schema(description = "目标实体类型ID")
	private Long targetEntityTypeId;

	@Schema(description = "目标命名空间ID")
	private Long targetNamespaceId;

	@Schema(description = "键列配置JSONB")
	private String keyColumns;

	@Schema(description = "IRI模板")
	private String iriTemplate;

	@Schema(description = "标签模板")
	private String labelTemplate;

	@Schema(description = "过滤条件DSL JSONB")
	private String filterDsl;

	@Schema(description = "增量列名")
	private String incrementalColumn;

	@Schema(description = "增量类型")
	private String incrementalType;

	@Schema(description = "源删除标记列名")
	private String sourceDeleteFlagColumn;

	@Schema(description = "源删除值JSONB")
	private String sourceDeleteValues;

	@Schema(description = "删除策略")
	private String deleteStrategy;

	@Schema(description = "失活属性ID")
	private Long inactivePropertyId;

	@Schema(description = "失活字面值")
	private String inactiveLiteralValue;

	@Schema(description = "冲突策略")
	private String conflictPolicy;

	@Schema(description = "同步顺序")
	private Integer syncOrder;

	@Schema(description = "是否启用")
	private String enabled;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "修订号")
	private Long revision;

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

	@Schema(description = "字段映射列表")
	private List<FieldMappingVO> fieldMappings;

}
