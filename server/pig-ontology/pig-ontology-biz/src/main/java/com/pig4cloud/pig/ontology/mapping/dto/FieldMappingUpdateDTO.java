/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 字段映射修改 DTO（18-04 §12）。
 *
 * @author youming
 */
@Data
@Schema(description = "字段映射修改")
public class FieldMappingUpdateDTO {

	@NotNull(message = "ID不能为空")
	@Schema(description = "字段映射ID")
	private Long id;

	@Schema(description = "字段映射名称")
	private String fieldMappingName;

	@Schema(description = "目标数据属性ID")
	private Long targetDataPropertyId;

	@Schema(description = "源列名")
	private String sourceColumn;

	@Schema(description = "来源类型: COLUMN / CONSTANT")
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

	@Schema(description = "空值处理: SKIP_NULL / USE_DEFAULT / REJECT_NULL")
	private String nullHandling;

	@Schema(description = "默认值")
	private String defaultValue;

	@Schema(description = "默认值字面量类型")
	private String defaultLiteralType;

	@Schema(description = "多值策略: SINGLE / FIRST / LAST / ALL")
	private String multiValueStrategy;

	@Schema(description = "固定单位ID")
	private Long unitId;

	@Schema(description = "所有权策略")
	private String ownershipPolicy;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "是否启用: 0否 1是")
	private String enabled;

	@Schema(description = "描述")
	private String description;

}
