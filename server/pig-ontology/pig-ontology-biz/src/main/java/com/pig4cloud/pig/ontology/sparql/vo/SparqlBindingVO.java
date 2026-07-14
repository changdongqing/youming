/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SPARQL 结果绑定值对象。
 * <p>
 * nodeType 取值固定为 IRI、BNODE、LITERAL；字面量可带 datatypeIri 或 language。
 * 空绑定不生成伪字符串 null。
 * </p>
 *
 * @author youming
 */
@Data
@Schema(description = "SPARQL结果绑定")
public class SparqlBindingVO {

	@Schema(description = "值（IRI的URI或字面量的词法形式）")
	private String value;

	@Schema(description = "节点类型：IRI/BNODE/LITERAL")
	private String nodeType;

	@Schema(description = "数据类型IRI（仅LITERAL）")
	private String datatypeIri;

	@Schema(description = "语言标签（仅LITERAL）")
	private String language;

}
