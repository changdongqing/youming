/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * SPARQL 查询请求。
 *
 * @author youming
 */
@Data
@Schema(description = "SPARQL查询请求")
public class SparqlQueryRequest {

	@NotNull
	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@NotBlank
	@Size(max = 65536)
	@Schema(description = "SPARQL查询文本")
	private String query;

	@Schema(description = "结果格式：JSON（CSV走独立导出接口）", defaultValue = "JSON")
	private String format = "JSON";

}
