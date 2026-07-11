/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实例分页查询条件。
 *
 * @author youming
 */
@Data
@Schema(description = "实例查询条件")
public class OntEntityInstanceQuery {

	@Schema(description = "关键词（IRI本地名或标签模糊匹配）")
	private String keyword;

	@Schema(description = "实体类型ID")
	private Long rdfTypeId;

	@Schema(description = "是否包含子类型实例")
	private Boolean includeSubtypes;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "命名空间ID")
	private Long namespaceId;

	@Schema(description = "来源类型")
	private String sourceType;

	@Schema(description = "是否内置")
	private String isBuiltin;

}
