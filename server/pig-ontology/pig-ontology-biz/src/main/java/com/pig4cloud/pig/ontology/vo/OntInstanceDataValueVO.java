/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实例数据属性值视图。
 *
 * @author youming
 */
@Data
@Schema(description = "实例数据属性值")
public class OntInstanceDataValueVO {

	@Schema(description = "数据值ID")
	private Long id;

	@Schema(description = "数据属性ID")
	private Long dataPropertyId;

	@Schema(description = "数据属性名称")
	private String dataPropertyName;

	@Schema(description = "数据属性中文标签")
	private String dataPropertyLabel;

	@Schema(description = "规范化词法值")
	private String literalValue;

	@Schema(description = "字面量类型")
	private String literalType;

	@Schema(description = "单位ID")
	private Long unitId;

	@Schema(description = "单位符号")
	private String unitSymbol;

	@Schema(description = "排序")
	private Integer sortOrder;

}
