/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 对象属性摘要（列表/分页）。
 *
 * @author youming
 */
@Data
@Schema(description = "对象属性摘要")
public class OntObjectPropertySummaryVO {

	private OntObjectProperty objectProperty;

	@Schema(description = "中文标签")
	private String label;

	@Schema(description = "定义域实体类型集合")
	private List<OntEntityTypeRefVO> domains;

	@Schema(description = "值域实体类型集合")
	private List<OntEntityTypeRefVO> ranges;

	@Schema(description = "逆属性名称")
	private String inversePropertyName;

	@Schema(description = "逆属性中文标签")
	private String inversePropertyLabel;

	@Schema(description = "推理能力契约")
	private List<String> inferenceSupport;

}
