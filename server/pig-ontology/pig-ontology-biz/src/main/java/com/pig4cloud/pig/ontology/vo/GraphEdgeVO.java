/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图谱边 VO（echarts graph series 边格式）。
 *
 * @author youming
 */
@Data
@Schema(description = "图谱边")
public class GraphEdgeVO {

	@Schema(description = "边ID（subject-predicate-object 组合键）")
	private String id;

	@Schema(description = "起点节点ID（IRI）")
	private String source;

	@Schema(description = "终点节点ID（IRI）")
	private String target;

	@Schema(description = "边标签（对象属性中文名或关系类型）")
	private String label;

	@Schema(description = "边类型：SUBCLASS_OF / OBJECT_PROPERTY / EQUIVALENT / DISJOINT / INSTANCE_OF / INSTANCE_RELATION")
	private String edgeType;

	@Schema(description = "对象属性ID（边为对象属性时）")
	private Long objectPropertyId;

	@Schema(description = "边样式：solid / dashed / dotted")
	private String lineStyle;

	@Schema(description = "边颜色（十六进制）")
	private String color;

	@Schema(description = "是否有向箭头")
	private Boolean directed;

	@Schema(description = "边宽度")
	private Integer width;

	@Schema(description = "是否功能属性")
	private Boolean isFunctional;

}
