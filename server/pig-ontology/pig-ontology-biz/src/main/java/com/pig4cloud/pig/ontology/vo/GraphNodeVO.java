/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图谱节点 VO（echarts graph series 节点格式）。
 *
 * @author youming
 */
@Data
@Schema(description = "图谱节点")
public class GraphNodeVO {

	@Schema(description = "节点ID（IRI）")
	private String id;

	@Schema(description = "显示名称")
	private String label;

	@Schema(description = "节点名称（英文）")
	private String name;

	@Schema(description = "节点层级：SCHEMA / INSTANCE")
	private String layer;

	@Schema(description = "节点类型：ENTITY_TYPE / INSTANCE")
	private String nodeType;

	@Schema(description = "实体类型ID（节点为实体类型时）")
	private Long entityTypeId;

	@Schema(description = "实例ID（节点为实例时）")
	private Long instanceId;

	@Schema(description = "rdf:type IRI（实例节点的类型IRI）")
	private String rdfTypeIri;

	@Schema(description = "rdf:type 标签（实例节点的类型中文名）")
	private String rdfTypeLabel;

	@Schema(description = "分类索引（对应 categories 数组下标，用于着色）")
	private Integer category;

	@Schema(description = "节点大小（按度数动态计算）")
	private Integer symbolSize;

	@Schema(description = "是否核心内置")
	private Boolean isBuiltin;

	@Schema(description = "是否抽象类")
	private Boolean isAbstract;

	@Schema(description = "定义")
	private String definition;

	@Schema(description = "命名空间前缀")
	private String namespacePrefix;

	@Schema(description = "公理约束数量")
	private Integer axiomCount;

	@Schema(description = "数据属性数量")
	private Integer dataPropertyCount;

	@Schema(description = "实例数量（实体类型节点）")
	private Integer instanceCount;

}
