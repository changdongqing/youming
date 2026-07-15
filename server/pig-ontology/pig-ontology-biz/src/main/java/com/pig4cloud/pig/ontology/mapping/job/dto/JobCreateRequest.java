/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 创建映射作业请求（18-07 §13）。
 *
 * @author youming
 */
@Data
@Schema(description = "创建映射作业请求")
public class JobCreateRequest {

	@NotBlank(message = "运行类型不能为空")
	@Schema(description = "运行类型: FULL/INCREMENTAL/RETRY/RELATION_RETRY")
	private String runType;

	@Schema(description = "指定执行的实体映射编码列表，空则全部")
	private List<String> entityMappingCodes;

	@Schema(description = "是否试运行（不写实例）")
	private Boolean dryRun;

	@Schema(description = "最大错误率阈值(0-1)，超过快速失败")
	private Double maxErrorRate;

}
