/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据属性查询条件。
 *
 * @author youming
 */
@Data
@Schema(description = "数据属性查询条件")
public class OntDataPropertyQuery {

	@Schema(description = "英文名称")
	private String name;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "命名空间ID")
	private Long namespaceId;

	@Schema(description = "定义域实体类型ID")
	private Long domainEntityTypeId;

	@Schema(description = "基本数据类型")
	private String baseType;

	@Schema(description = "值模式")
	private String valueMode;

	@Schema(description = "来源类型")
	private String sourceType;

	@Schema(description = "是否内置")
	private String isBuiltin;

}
