/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 扩展模块查询条件。
 *
 * @author youming
 */
@Data
@Schema(description = "扩展模块查询条件")
public class ExtensionModuleQuery {

	@Schema(description = "模块代码（模糊）")
	private String moduleCode;

	@Schema(description = "模块名称（模糊）")
	private String moduleName;

	@Schema(description = "命名空间ID")
	private Long namespaceId;

}
