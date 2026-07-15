/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

/**
 * 关系键预览请求 DTO（18-05 §13）。
 * <p>
 * 对样例键值解析为 subject/object recordKey 和 relationKey，不读源库。
 *
 * @author youming
 */
@Data
@Schema(description = "关系键预览请求")
public class RelationKeyPreviewRequestDTO {

	@NotEmpty(message = "样例键值不能为空")
	@Schema(description = "样例键值，key为源列名，value为样例值")
	private Map<String, String> sampleValues;

}
