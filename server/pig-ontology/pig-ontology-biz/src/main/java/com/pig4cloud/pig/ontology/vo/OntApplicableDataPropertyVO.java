/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 适用数据属性（按定义域查询，含继承信息）。
 *
 * @author youming
 */
@Data
@Schema(description = "适用数据属性")
public class OntApplicableDataPropertyVO {

	private OntDataProperty dataProperty;

	@Schema(description = "中文标签")
	private String label;

	@Schema(description = "显示名")
	private String displayName;

	@Schema(description = "是否继承属性，false为直接属性")
	private Boolean inherited;

	@Schema(description = "继承距离，0为直接属性")
	private Integer inheritanceDistance;

	@Schema(description = "声明定义域实体类型ID")
	private Long declaredDomainEntityTypeId;

	@Schema(description = "声明定义域实体类型名称")
	private String declaredDomainName;

	@Schema(description = "声明定义域实体类型中文标签")
	private String declaredDomainLabel;

}
