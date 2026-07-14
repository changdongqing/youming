/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 元数据对象查询参数。
 *
 * @author youming
 */
@Data
@Schema(description = "元数据对象查询")
public class MetadataObjectQuery {

	@Schema(description = "Schema名（可选，为空则查全部白名单内Schema）")
	private String schemaName;

	@Schema(description = "对象名前缀过滤（可选）")
	private String objectNamePrefix;

	@Schema(description = "对象类型过滤: TABLE / VIEW（可选，为空则全部）")
	private String objectType;

}
