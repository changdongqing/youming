/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 扩展模块创建请求。
 *
 * @author youming
 */
@Data
@Schema(description = "扩展模块创建请求")
public class ExtensionModuleCreateDTO {

	@NotBlank(message = "模块代码不能为空")
	@Schema(description = "模块代码（全局唯一，如 medical）")
	private String moduleCode;

	@NotBlank(message = "模块名称不能为空")
	@Schema(description = "模块名称")
	private String moduleName;

	@Schema(description = "绑定的扩展命名空间ID")
	private Long namespaceId;

	@Schema(description = "归属本体工程ID（默认935001）")
	private Long ontologyId;

	@Schema(description = "模块描述")
	private String description;

	@Schema(description = "模块版本号")
	private String version;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "备注")
	private String remarks;

}
