/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 实例对象属性断言请求（普通接口仅接受INSTANCE客体）。
 *
 * @author youming
 */
@Data
@Schema(description = "实例对象属性断言请求")
public class OntInstanceObjectRelationDTO {

	@NotNull(message = "对象属性不能为空")
	@Schema(description = "对象属性ID（谓词）")
	private Long objectPropertyId;

	@NotNull(message = "客体实例不能为空")
	@Schema(description = "客体实例ID")
	private Long objectInstanceId;

	@Min(value = 0, message = "排序值不能小于0")
	@Schema(description = "多值顺序")
	private Integer sortOrder;

}
