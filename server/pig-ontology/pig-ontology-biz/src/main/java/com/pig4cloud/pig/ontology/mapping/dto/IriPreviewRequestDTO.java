/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

/**
 * IRI预览请求 DTO（18-04 §12）。
 * <p>
 * 对样例键值渲染IRI，不读源库。
 *
 * @author youming
 */
@Data
@Schema(description = "IRI预览请求")
public class IriPreviewRequestDTO {

	@NotEmpty(message = "样例键值不能为空")
	@Schema(description = "样例键值，key为列名，value为样例值")
	private Map<String, String> sampleKeyValues;

}
