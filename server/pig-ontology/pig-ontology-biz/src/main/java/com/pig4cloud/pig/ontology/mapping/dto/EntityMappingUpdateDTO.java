/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 实体映射修改 DTO（18-04 §12）。
 * <p>
 * 仅DRAFT版本可修改，带乐观锁revision。
 *
 * @author youming
 */
@Data
@Schema(description = "实体映射修改")
public class EntityMappingUpdateDTO {

	@NotNull(message = "ID不能为空")
	@Schema(description = "实体映射ID")
	private Long id;

	@Schema(description = "映射名称")
	private String mappingName;

	@Schema(description = "源对象类型: TABLE / VIEW")
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

	@Schema(description = "增量类型: TIMESTAMP / NUMERIC")
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

	@Schema(description = "是否启用: 0否 1是")
	private String enabled;

	@Schema(description = "描述")
	private String description;

	@NotNull(message = "乐观锁revision不能为空")
	@Schema(description = "乐观锁revision")
	private Long revision;

}
