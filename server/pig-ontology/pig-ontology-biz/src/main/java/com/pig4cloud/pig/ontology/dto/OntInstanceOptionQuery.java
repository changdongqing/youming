/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实例分页选择器查询条件。
 *
 * @author youming
 */
@Data
@Schema(description = "实例选择器查询条件")
public class OntInstanceOptionQuery {

	@Schema(description = "对象属性ID，按值域类型过滤可选实例")
	private Long objectPropertyId;

	@Schema(description = "关键词（IRI本地名或标签模糊匹配）")
	private String keyword;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

}
