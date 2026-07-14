/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 映射版本配置差异 VO（18-03 §12）。
 * <p>
 * 不包含凭证或源样例值。
 *
 * @author youming
 */
@Data
@Schema(description = "映射版本配置差异")
public class MappingVersionDiffVO {

	@Schema(description = "基准版本ID")
	private Long baseVersionId;

	@Schema(description = "基准版本号")
	private String baseVersionNumber;

	@Schema(description = "对比版本ID")
	private Long compareVersionId;

	@Schema(description = "对比版本号")
	private String compareVersionNumber;

	@Schema(description = "变更分类: PATCH / MINOR / MAJOR / MAJOR_HIGH_RISK")
	private String changeClassification;

	@Schema(description = "变更项列表")
	private List<ChangeItem> changes;

	/**
	 * 单个变更项。
	 */
	@Data
	@Schema(description = "变更项")
	public static class ChangeItem {

		@Schema(description = "变更路径，如 project.mappingName / version.ontologyVersionConstraint")
		private String field;

		@Schema(description = "基准值")
		private String oldValue;

		@Schema(description = "对比值")
		private String newValue;

		@Schema(description = "变更分类: PATCH / MINOR / MAJOR / MAJOR_HIGH_RISK")
		private String classification;

	}

}
