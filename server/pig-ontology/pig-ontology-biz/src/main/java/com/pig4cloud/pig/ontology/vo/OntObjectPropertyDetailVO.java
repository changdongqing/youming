/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyLabel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 对象属性详情。
 *
 * @author youming
 */
@Data
@Schema(description = "对象属性详情")
public class OntObjectPropertyDetailVO {

	private OntObjectProperty objectProperty;

	private List<OntObjectPropertyLabel> labels = new ArrayList<>();

	@Schema(description = "定义域实体类型集合")
	private List<OntEntityTypeRefVO> domains;

	@Schema(description = "值域实体类型集合")
	private List<OntEntityTypeRefVO> ranges;

	private OntNamespace namespace;

	@Schema(description = "逆属性摘要")
	private OntObjectPropertySummaryVO inverseProperty;

	@Schema(description = "语义警告提示")
	private List<String> semanticWarnings;

	@Schema(description = "推理能力契约")
	private List<String> inferenceSupport;

}
