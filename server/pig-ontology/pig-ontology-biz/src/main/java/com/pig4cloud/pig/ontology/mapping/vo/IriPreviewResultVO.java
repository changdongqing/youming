/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * IRI预览结果 VO（18-04 §12）。
 *
 * @author youming
 */
@Data
@Schema(description = "IRI预览结果")
public class IriPreviewResultVO {

	@Schema(description = "命名空间URI")
	private String namespaceUri;

	@Schema(description = "渲染后的本地名")
	private String localName;

	@Schema(description = "完整IRI")
	private String fullIri;

	@Schema(description = "渲染后的标签（如配置了标签模板）")
	private String label;

	@Schema(description = "是否成功")
	private boolean success;

	@Schema(description = "错误消息（失败时）")
	private String errorMessage;

}
