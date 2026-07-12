/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 校验执行请求。
 *
 * @author youming
 */
@Data
@Schema(description = "校验执行请求")
public class ValidationRunRequest {

	@NotNull(message = "本体工程ID不能为空")
	@Schema(description = "本体工程ID")
	private Long ontologyId;

}
