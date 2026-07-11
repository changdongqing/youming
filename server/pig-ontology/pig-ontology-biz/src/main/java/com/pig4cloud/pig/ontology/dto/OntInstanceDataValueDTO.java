/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 实例数据属性值请求。
 *
 * @author youming
 */
@Data
@Schema(description = "实例数据属性值请求")
public class OntInstanceDataValueDTO {

	@NotNull(message = "数据属性不能为空")
	@Schema(description = "数据属性ID")
	private Long dataPropertyId;

	@NotBlank(message = "字面量值不能为空")
	@Schema(description = "字面量值")
	private String literalValue;

	@NotBlank(message = "字面量类型不能为空")
	@Pattern(regexp = "^(STRING|URI|DATE|INTEGER|DECIMAL|BOOLEAN)$", message = "字面量类型只能为STRING/URI/DATE/INTEGER/DECIMAL/BOOLEAN")
	@Schema(description = "字面量类型")
	private String literalType;

	@Schema(description = "单位ID，UNIT_REF必填")
	private Long unitId;

	@Size(max = 32, message = "单位符号长度不能超过32")
	@Schema(description = "UNIT_REF词法快照")
	private String literalSymbol;

	@Min(value = 0, message = "排序值不能小于0")
	@Schema(description = "同属性多值排序")
	private Integer sortOrder;

}
