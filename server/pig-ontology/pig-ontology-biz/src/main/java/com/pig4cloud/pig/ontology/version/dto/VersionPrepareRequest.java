/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 版本 prepare 请求体。
 *
 * @author youming
 */
@Data
@Schema(description = "版本发布请求")
public class VersionPrepareRequest {

	@NotNull(message = "本体工程ID不能为空")
	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@NotBlank(message = "版本号不能为空")
	@Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "版本号格式必须为 MAJOR.MINOR.PATCH")
	@Schema(description = "语义版本号")
	private String versionNumber;

	@Schema(description = "发布说明")
	private String releaseNotes;

	@Schema(description = "声明兼容性：PATCH_ONLY/BACKWARD_COMPATIBLE/BREAKING")
	private String declaredCompatibility;

	@Schema(description = "实例迁移计划JSON，BREAKING时必填")
	private String migrationPlan;

}
