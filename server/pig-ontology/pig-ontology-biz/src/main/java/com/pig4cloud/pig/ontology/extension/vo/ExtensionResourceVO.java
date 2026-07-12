/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扩展资源关联视图。
 *
 * @author youming
 */
@Data
@Schema(description = "扩展资源关联视图")
public class ExtensionResourceVO {

	@Schema(description = "关联记录ID")
	private Long id;

	@Schema(description = "扩展模块ID")
	private Long moduleId;

	@Schema(description = "资源类型: ENTITY_TYPE/DATA_PROPERTY/OBJECT_PROPERTY/AXIOM_RULE/UNIT")
	private String resourceType;

	@Schema(description = "资源记录ID")
	private Long resourceId;

	@Schema(description = "资源IRI")
	private String resourceIri;

	@Schema(description = "资源名称")
	private String resourceName;

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

}
