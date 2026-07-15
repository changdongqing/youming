/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关系映射详情 VO（18-05 §13）。
 *
 * @author youming
 */
@Data
@Schema(description = "关系映射详情")
public class RelationMappingVO {

	@Schema(description = "关系映射ID")
	private Long id;

	@Schema(description = "映射版本ID")
	private Long mappingVersionId;

	@Schema(description = "映射编码")
	private String mappingCode;

	@Schema(description = "映射名称")
	private String mappingName;

	@Schema(description = "关系模式")
	private String relationMode;

	@Schema(description = "对象属性ID")
	private Long objectPropertyId;

	@Schema(description = "主体实体映射ID")
	private Long subjectEntityMappingId;

	@Schema(description = "客体实体映射ID")
	private Long objectEntityMappingId;

	@Schema(description = "数据源ID")
	private Long sourceId;

	@Schema(description = "源Schema名")
	private String sourceSchema;

	@Schema(description = "源对象名")
	private String sourceObject;

	@Schema(description = "主体键映射JSONB")
	private String subjectKeyMapping;

	@Schema(description = "客体键映射JSONB")
	private String objectKeyMapping;

	@Schema(description = "关系键列JSONB")
	private String relationKeyColumns;

	@Schema(description = "过滤条件DSL JSONB")
	private String filterDsl;

	@Schema(description = "缺失目标策略")
	private String missingTargetPolicy;

	@Schema(description = "删除策略")
	private String deleteStrategy;

	@Schema(description = "所有权策略")
	private String ownershipPolicy;

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

}
