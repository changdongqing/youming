/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据属性多语言标签。
 *
 * @author youming
 */
@Data
@TableName("ont_data_property_label")
@Schema(description = "数据属性标签")
public class OntDataPropertyLabel {

	@Schema(description = "数据属性ID")
	private Long dataPropertyId;

	@Schema(description = "语言区域")
	private String locale;

	@Schema(description = "标签文本")
	private String label;

}
