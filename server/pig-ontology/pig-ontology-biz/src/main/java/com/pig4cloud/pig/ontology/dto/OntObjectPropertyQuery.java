/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 对象属性查询条件。
 *
 * @author youming
 */
@Data
@Schema(description = "对象属性查询条件")
public class OntObjectPropertyQuery {

	@Schema(description = "英文名称")
	private String name;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "命名空间ID")
	private Long namespaceId;

	@Schema(description = "定义域实体类型ID（直接声明过滤）")
	private Long domainEntityTypeId;

	@Schema(description = "值域实体类型ID（直接声明过滤）")
	private Long rangeEntityTypeId;

	@Schema(description = "来源类型")
	private String sourceType;

	@Schema(description = "是否内置")
	private String isBuiltin;

	@Schema(description = "是否功能性")
	private String isFunctional;

	@Schema(description = "是否反功能性")
	private String isInverseFunctional;

	@Schema(description = "是否传递性")
	private String isTransitive;

	@Schema(description = "是否对称性")
	private String isSymmetric;

	@Schema(description = "推理能力契约过滤（如FUNCTIONAL_CHECK）")
	private String inferenceSupport;

}
