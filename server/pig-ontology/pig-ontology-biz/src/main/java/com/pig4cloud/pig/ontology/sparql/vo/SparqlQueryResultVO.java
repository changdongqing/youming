/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * SPARQL 查询结果。
 *
 * @author youming
 */
@Data
@Schema(description = "SPARQL查询结果")
public class SparqlQueryResultVO {

	@Schema(description = "查询类型：SELECT/ASK")
	private String queryType;

	@Schema(description = "结果变量列表（SELECT）")
	private List<String> variables;

	@Schema(description = "结果行（SELECT）")
	private List<Map<String, SparqlBindingVO>> rows;

	@Schema(description = "布尔结果（ASK）")
	private Boolean booleanResult;

	@Schema(description = "结果行数")
	private Integer rowCount;

	@Schema(description = "执行耗时（毫秒）")
	private Long durationMs;

	@Schema(description = "结果是否截断")
	private Boolean truncated;

}
