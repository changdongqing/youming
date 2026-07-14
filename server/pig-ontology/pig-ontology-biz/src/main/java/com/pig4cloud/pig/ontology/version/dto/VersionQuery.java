/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 版本分页查询参数。
 *
 * @author youming
 */
@Data
@Schema(description = "版本查询参数")
public class VersionQuery {

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "版本号（模糊）")
	private String versionNumber;

	@Schema(description = "兼容性")
	private String compatibility;

	@Schema(description = "发布状态")
	private String releaseStatus;

}
