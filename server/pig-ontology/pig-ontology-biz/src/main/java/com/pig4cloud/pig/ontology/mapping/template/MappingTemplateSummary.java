/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 模板摘要信息，用于前端展示可选模板列表。
 *
 * @author youming
 */
@Data
@Schema(description = "映射模板摘要")
public class MappingTemplateSummary {

	@Schema(description = "模板编码")
	private String templateCode;

	@Schema(description = "模板名称")
	private String templateName;

	@Schema(description = "模板描述")
	private String description;

	@Schema(description = "模板版本")
	private String version;

	@Schema(description = "命名空间前缀")
	private String namespacePrefix;

	@Schema(description = "实体映射数量")
	private Integer entityMappingCount;

	@Schema(description = "关系映射数量")
	private Integer relationMappingCount;

}
