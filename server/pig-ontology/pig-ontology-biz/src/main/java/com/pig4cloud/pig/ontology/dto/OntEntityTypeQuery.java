/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实体类型查询条件。
 *
 * @author youming
 */
@Data
@Schema(description = "实体类型查询条件")
public class OntEntityTypeQuery {

	@Schema(description = "英文名称，模糊匹配")
	private String name;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "命名空间ID")
	private Long namespaceId;

	@Schema(description = "是否内置，1是0否")
	private String isBuiltin;

}
