/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图谱统计摘要 VO。
 *
 * @author youming
 */
@Data
@Schema(description = "图谱统计")
public class GraphSummaryVO {

	@Schema(description = "节点总数")
	private Integer nodeCount;

	@Schema(description = "边总数")
	private Integer edgeCount;

	@Schema(description = "实体类型数")
	private Integer entityTypeCount;

	@Schema(description = "实例数")
	private Integer instanceCount;

	@Schema(description = "对象属性数")
	private Integer objectPropertyCount;

	@Schema(description = "继承关系数")
	private Integer subclassCount;

	@Schema(description = "不相交对数")
	private Integer disjointCount;

	@Schema(description = "等价类对数")
	private Integer equivalentCount;

	@Schema(description = "公理规则数")
	private Integer axiomRuleCount;

	@Schema(description = "数据属性数")
	private Integer dataPropertyCount;

}
