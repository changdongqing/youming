/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据属性摘要（列表/分页）。
 *
 * @author youming
 */
@Data
@Schema(description = "数据属性摘要")
public class OntDataPropertySummaryVO {

	private OntDataProperty dataProperty;

	@Schema(description = "中文标签")
	private String label;

	@Schema(description = "显示名（优先取别名，其次取name）")
	private String displayName;

	@Schema(description = "定义域实体类型名称")
	private String domainEntityTypeName;

	@Schema(description = "定义域实体类型中文标签")
	private String domainEntityTypeLabel;

	@Schema(description = "枚举值数量")
	private Integer enumCount;

	@Schema(description = "单位分类名称")
	private String unitCategoryName;

}
