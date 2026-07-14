/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 迁移作业分页查询参数。
 *
 * @author youming
 */
@Data
@Schema(description = "迁移作业查询参数")
public class MigrationJobQuery {

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "候选版本ID")
	private Long candidateVersionId;

	@Schema(description = "作业状态")
	private String status;

}
