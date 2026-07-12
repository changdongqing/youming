/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 扩展模块修改请求。
 *
 * @author youming
 */
@Data
@Schema(description = "扩展模块修改请求")
public class ExtensionModuleUpdateDTO {

	@Schema(description = "扩展模块ID")
	private Long id;

	@Schema(description = "模块名称")
	private String moduleName;

	@Schema(description = "绑定的扩展命名空间ID")
	private Long namespaceId;

	@Schema(description = "模块描述")
	private String description;

	@Schema(description = "模块版本号")
	private String version;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "备注")
	private String remarks;

}
