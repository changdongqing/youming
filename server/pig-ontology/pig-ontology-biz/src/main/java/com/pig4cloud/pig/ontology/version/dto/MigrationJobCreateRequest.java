/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建迁移作业请求体。
 *
 * @author youming
 */
@Data
@Schema(description = "创建迁移作业请求")
public class MigrationJobCreateRequest {

	@NotNull(message = "候选版本ID不能为空")
	@Schema(description = "候选版本ID")
	private Long candidateVersionId;

}
