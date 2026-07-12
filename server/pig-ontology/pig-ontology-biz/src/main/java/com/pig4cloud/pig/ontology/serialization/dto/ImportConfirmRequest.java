/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 导入确认请求。
 *
 * @author youming
 */
@Data
@Schema(description = "导入确认请求")
public class ImportConfirmRequest {

	@NotBlank(message = "previewId不能为空")
	@Schema(description = "预检ID")
	private String previewId;

	@Schema(description = "IRI合并模式: SKIP/MERGE/OVERWRITE，默认SKIP")
	private String iriMergeMode;

	@Schema(description = "本体工程ID，默认935001")
	private Long ontologyId;

}
