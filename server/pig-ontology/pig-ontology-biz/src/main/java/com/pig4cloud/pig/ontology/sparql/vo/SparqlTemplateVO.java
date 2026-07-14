/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SPARQL 预置查询模板 VO。
 *
 * @author youming
 */
@Data
@Schema(description = "SPARQL预置查询模板")
public class SparqlTemplateVO {

	@Schema(description = "模板ID")
	private String id;

	@Schema(description = "模板名称")
	private String name;

	@Schema(description = "查询类型：SELECT/ASK")
	private String queryType;

	@Schema(description = "模板说明")
	private String description;

	@Schema(description = "SPARQL查询文本（含占位符）")
	private String query;

	@Schema(description = "占位符列表")
	private java.util.List<String> placeholders;

}
