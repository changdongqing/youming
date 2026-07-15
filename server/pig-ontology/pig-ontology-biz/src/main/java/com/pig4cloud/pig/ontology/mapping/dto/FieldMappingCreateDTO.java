/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 字段映射创建 DTO（18-04 §12）。
 *
 * @author youming
 */
@Data
@Schema(description = "字段映射创建")
public class FieldMappingCreateDTO {

	@NotBlank(message = "字段映射编码不能为空")
	@Size(max = 64, message = "字段映射编码长度不能超过64")
	@Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "字段映射编码须以字母开头，仅含字母、数字、下划线")
	@Schema(description = "字段映射编码")
	private String fieldMappingCode;

	@Size(max = 128, message = "字段映射名称长度不能超过128")
	@Schema(description = "字段映射名称")
	private String fieldMappingName;

	@NotNull(message = "目标数据属性ID不能为空")
	@Schema(description = "目标数据属性ID")
	private Long targetDataPropertyId;

	@Size(max = 128, message = "源列名长度不能超过128")
	@Schema(description = "源列名（source_kind=COLUMN时必填）")
	private String sourceColumn;

	@Schema(description = "来源类型: COLUMN / CONSTANT，默认COLUMN")
	@Pattern(regexp = "^(COLUMN|CONSTANT)$", message = "来源类型必须为COLUMN或CONSTANT")
	private String sourceKind;

	@Schema(description = "常量值（source_kind=CONSTANT时必填）")
	private String constantValue;

	@Schema(description = "常量字面量类型: STRING / INTEGER / DECIMAL / BOOLEAN / DATE / URI")
	private String constantLiteralType;

	@Schema(description = "常量单位ID")
	private Long constantUnitId;

	@Schema(description = "转换器编码，默认IDENTITY")
	@Size(max = 64, message = "转换器编码长度不能超过64")
	private String transformer;

	@Schema(description = "转换器参数JSONB")
	private String transformerParams;

	@Schema(description = "空值处理: SKIP_NULL / USE_DEFAULT / REJECT_NULL，默认SKIP_NULL")
	@Pattern(regexp = "^(SKIP_NULL|USE_DEFAULT|REJECT_NULL)$",
			message = "空值处理必须为SKIP_NULL/USE_DEFAULT/REJECT_NULL")
	private String nullHandling;

	@Schema(description = "默认值")
	private String defaultValue;

	@Schema(description = "默认值字面量类型")
	private String defaultLiteralType;

	@Schema(description = "多值策略: SINGLE / FIRST / LAST / ALL，默认SINGLE")
	@Pattern(regexp = "^(SINGLE|FIRST|LAST|ALL)$", message = "多值策略必须为SINGLE/FIRST/LAST/ALL")
	private String multiValueStrategy;

	@Schema(description = "固定单位ID")
	private Long unitId;

	@Schema(description = "所有权策略，默认SOURCE_WINS")
	@Pattern(regexp = "^(SOURCE_WINS|MANUAL_WINS|REJECT_CONFLICT)$",
			message = "所有权策略必须为SOURCE_WINS/MANUAL_WINS/REJECT_CONFLICT")
	private String ownershipPolicy;

	@Schema(description = "排序，默认0")
	private Integer sortOrder;

	@Schema(description = "是否启用: 0否 1是，默认1")
	@Pattern(regexp = "^[01]$", message = "启用标记必须为0或1")
	private String enabled;

	@Schema(description = "描述")
	@Size(max = 255, message = "描述长度不能超过255")
	private String description;

}
