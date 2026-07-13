/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 可视化首页统计 VO。
 *
 * @author youming
 */
@Data
@Schema(description = "可视化统计")
public class VisualizationStatsVO {

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "本体工程名称")
	private String ontologyName;

	@Schema(description = "实体类型总数")
	private Integer entityTypeCount;

	@Schema(description = "数据属性总数")
	private Integer dataPropertyCount;

	@Schema(description = "对象属性总数")
	private Integer objectPropertyCount;

	@Schema(description = "实例总数")
	private Integer instanceCount;

	@Schema(description = "公理规则总数")
	private Integer axiomRuleCount;

	@Schema(description = "最近校验状态：PASS / FAIL / PENDING / NONE")
	private String latestValidationStatus;

	@Schema(description = "最近校验时间")
	private String latestValidationTime;

	@Schema(description = "最近校验违规数")
	private Integer latestViolationCount;

	@Schema(description = "序列化导出次数")
	private Integer exportCount;

	@Schema(description = "扩展模块数")
	private Integer extensionModuleCount;

	@Schema(description = "实体类型分布（按核心/扩展分组）")
	private List<NameValueVO> entityTypeDistribution = new ArrayList<>();

	@Schema(description = "实例分布（按实体类型分组，Top 10）")
	private List<NameValueVO> instanceDistribution = new ArrayList<>();

}
