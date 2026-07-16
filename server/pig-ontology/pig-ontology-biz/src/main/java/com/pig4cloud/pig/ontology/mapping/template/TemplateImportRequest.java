/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.template;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 从模板创建映射工程的请求。
 *
 * @author youming
 */
@Data
@Schema(description = "从模板创建映射工程")
public class TemplateImportRequest {

	@NotBlank(message = "模板编码不能为空")
	@Schema(description = "模板编码，如 upms-organization-v1")
	private String templateCode;

	@NotNull(message = "数据源ID不能为空")
	@Schema(description = "数据源ID（模板不包含数据源，需用户选择已创建的数据源）")
	private Long dataSourceId;

	@Schema(description = "映射编码覆盖（可选，为空则使用模板中的默认编码）")
	private String mappingCodeOverride;

}
