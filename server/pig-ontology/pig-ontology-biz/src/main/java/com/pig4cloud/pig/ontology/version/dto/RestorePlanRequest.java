/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 生成恢复计划请求体。
 *
 * @author youming
 */
@Data
@Schema(description = "恢复计划请求")
public class RestorePlanRequest {

	@NotNull(message = "本体工程ID不能为空")
	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@NotNull(message = "目标历史版本ID不能为空")
	@Schema(description = "要恢复到的历史版本ID")
	private Long targetVersionId;

}
