/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 本体工程版本配置请求体。
 *
 * @author youming
 */
@Data
@Schema(description = "版本配置请求")
public class VersionConfigRequest {

	@NotBlank(message = "本体IRI不能为空")
	@Pattern(regexp = "^https?://.+", message = "本体IRI必须为绝对IRI（http://或https://开头）")
	@Schema(description = "本体标识IRI")
	private String ontologyIri;

	@NotBlank(message = "版本IRI基础路径不能为空")
	@Pattern(regexp = "^https?://.*/$", message = "版本IRI基础路径必须以/结尾")
	@Schema(description = "版本IRI基础路径")
	private String versionIriBase;

}
