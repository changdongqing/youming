/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节点分类 VO（用于 echarts categories 图例）。
 *
 * @author youming
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "节点分类")
public class GraphCategoryVO {

	@Schema(description = "分类名称")
	private String name;

	@Schema(description = "分类颜色（十六进制）")
	private String color;

}
