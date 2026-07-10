/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据属性枚举值。
 *
 * @author youming
 */
@Data
@TableName("ont_data_property_enum")
@Schema(description = "数据属性枚举值")
public class OntDataPropertyEnum {

	@Schema(description = "数据属性ID")
	private Long dataPropertyId;

	@Schema(description = "可接受的枚举词法值")
	private String enumValue;

	@Schema(description = "归一后的标准值，标准值本身为NULL")
	private String canonicalValue;

	@Schema(description = "1来自附录C，0为兼容别名或平台推荐")
	private String isStandard;

	@Schema(description = "来源引用")
	private String sourceReference;

	@Schema(description = "排序")
	private Integer sortOrder;

}
