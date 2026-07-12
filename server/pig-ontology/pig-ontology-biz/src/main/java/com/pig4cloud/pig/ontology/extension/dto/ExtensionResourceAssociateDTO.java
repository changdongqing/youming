/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 扩展资源批量关联请求。
 *
 * @author youming
 */
@Data
@Schema(description = "扩展资源批量关联请求")
public class ExtensionResourceAssociateDTO {

	@Valid
	@NotEmpty(message = "资源列表不能为空")
	@Schema(description = "资源列表")
	private List<ResourceItem> resources;

	/**
	 * 资源项。
	 */
	@Data
	@Schema(description = "资源项")
	public static class ResourceItem {

		@Schema(description = "资源类型: ENTITY_TYPE/DATA_PROPERTY/OBJECT_PROPERTY/AXIOM_RULE/UNIT")
		private String resourceType;

		@Schema(description = "资源记录ID")
		private Long resourceId;

	}

}
