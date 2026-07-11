/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实例选择器选项（分页远程搜索）。
 *
 * @author youming
 */
@Data
@Schema(description = "实例选择器选项")
public class OntEntityInstanceOptionVO {

	@Schema(description = "实例ID")
	private Long id;

	@Schema(description = "完整IRI")
	private String iri;

	@Schema(description = "IRI本地标识符")
	private String iriLocalName;

	@Schema(description = "UI显示标签")
	private String label;

	@Schema(description = "实体类型ID")
	private Long rdfTypeId;

	@Schema(description = "实体类型名称")
	private String rdfTypeName;

}
