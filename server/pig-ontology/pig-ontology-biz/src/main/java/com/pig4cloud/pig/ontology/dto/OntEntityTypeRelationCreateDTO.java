/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 实体类型关系请求。
 *
 * @author youming
 */
@Data
@Schema(description = "实体类型关系请求")
public class OntEntityTypeRelationCreateDTO {

	@NotNull(message = "实体类型A不能为空")
	@Schema(description = "实体类型A ID")
	private Long entityTypeAId;

	@NotNull(message = "实体类型B不能为空")
	@Schema(description = "实体类型B ID")
	private Long entityTypeBId;

}
