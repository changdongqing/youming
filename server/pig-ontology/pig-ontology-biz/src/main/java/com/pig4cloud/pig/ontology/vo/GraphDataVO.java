/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 图谱数据 VO（echarts graph series 直接消费格式）。
 *
 * @author youming
 */
@Data
@Schema(description = "图谱数据")
public class GraphDataVO {

	@Schema(description = "节点列表")
	private List<GraphNodeVO> nodes = new ArrayList<>();

	@Schema(description = "边列表")
	private List<GraphEdgeVO> edges = new ArrayList<>();

	@Schema(description = "节点分类（用于着色图例）")
	private List<GraphCategoryVO> categories = new ArrayList<>();

	@Schema(description = "图谱统计摘要")
	private GraphSummaryVO summary;

}
