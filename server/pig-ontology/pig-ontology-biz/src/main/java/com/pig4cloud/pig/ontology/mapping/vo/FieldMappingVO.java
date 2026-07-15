/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字段映射详情 VO（18-04 §12）。
 *
 * @author youming
 */
@Data
@Schema(description = "字段映射详情")
public class FieldMappingVO {

	@Schema(description = "字段映射ID")
	private Long id;

	@Schema(description = "父实体映射ID")
	private Long entityMappingId;

	@Schema(description = "字段映射编码")
	private String fieldMappingCode;

	@Schema(description = "字段映射名称")
	private String fieldMappingName;

	@Schema(description = "目标数据属性ID")
	private Long targetDataPropertyId;

	@Schema(description = "源列名")
	private String sourceColumn;

	@Schema(description = "来源类型")
	private String sourceKind;

	@Schema(description = "常量值")
	private String constantValue;

	@Schema(description = "常量字面量类型")
	private String constantLiteralType;

	@Schema(description = "常量单位ID")
	private Long constantUnitId;

	@Schema(description = "转换器编码")
	private String transformer;

	@Schema(description = "转换器参数JSONB")
	private String transformerParams;

	@Schema(description = "空值处理")
	private String nullHandling;

	@Schema(description = "默认值")
	private String defaultValue;

	@Schema(description = "默认值字面量类型")
	private String defaultLiteralType;

	@Schema(description = "多值策略")
	private String multiValueStrategy;

	@Schema(description = "固定单位ID")
	private Long unitId;

	@Schema(description = "所有权策略")
	private String ownershipPolicy;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "是否启用")
	private String enabled;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

}
