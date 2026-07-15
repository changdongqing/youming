/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 作业分页查询参数。
 *
 * @author youming
 */
@Data
@Schema(description = "作业查询参数")
public class JobQuery {

	@Schema(description = "映射工程ID")
	private Long projectId;

	@Schema(description = "运行类型")
	private String runType;

	@Schema(description = "作业状态")
	private String jobStatus;

	@Schema(description = "触发类型")
	private String triggerType;

}
