/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 按定义域查询的适用对象属性（含继承信息）。
 *
 * @author youming
 */
@Data
@Schema(description = "适用对象属性（按定义域）")
public class OntApplicableObjectPropertyVO {

	private OntObjectProperty objectProperty;

	@Schema(description = "中文标签")
	private String label;

	@Schema(description = "定义域实体类型集合")
	private List<OntEntityTypeRefVO> domains;

	@Schema(description = "值域实体类型集合")
	private List<OntEntityTypeRefVO> ranges;

	@Schema(description = "是否继承属性，false为直接属性")
	private Boolean inherited;

	@Schema(description = "继承距离，0为直接属性")
	private Integer inheritanceDistance;

	@Schema(description = "命中的声明定义域实体类型ID")
	private Long matchedDomainEntityTypeId;

	@Schema(description = "命中的声明定义域实体类型名称")
	private String matchedDomainName;

	@Schema(description = "命中的声明定义域实体类型中文标签")
	private String matchedDomainLabel;

}
