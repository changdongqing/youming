/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.ontology.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 属性提升为模板请求 DTO（FR-6）
 *
 * @author pig
 * @date 2026-07-25
 */
@Data
@Schema(description = "属性提升为模板请求")
public class PropertyTemplatePromoteDTO {

	@NotBlank(message = "模板标识不能为空")
	@Schema(description = "唯一标识")
	private String templateCode;

	@NotBlank(message = "类型不能为空")
	@Schema(description = "datatype / object")
	private String kind;

	@NotBlank(message = "显示名不能为空")
	@Schema(description = "显示名")
	private String label;

	@Schema(description = "业务说明")
	private String description;

	@Schema(description = "分组")
	private String category;

	@Schema(description = "datatype 专属类型")
	private String type;

	@Schema(description = "0/1 标识符")
	private String isIdentifier;

	@Schema(description = "预设单位 IRI")
	private String unitRef;

	@Schema(description = "枚举值")
	private String enumValues;

	@Schema(description = "object 专属基数")
	private String defaultCardinality;

}
