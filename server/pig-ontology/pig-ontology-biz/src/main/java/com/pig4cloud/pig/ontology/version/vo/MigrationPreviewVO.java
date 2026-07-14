/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 迁移影响预览 VO。
 *
 * @author youming
 */
@Data
@Schema(description = "迁移影响预览")
public class MigrationPreviewVO {

	@Schema(description = "候选版本ID")
	private Long candidateVersionId;

	@Schema(description = "受影响实例总数")
	private Long affectedInstanceCount;

	@Schema(description = "迁移规则列表")
	private List<MigrationRulePreview> rules;

	@Data
	@Schema(description = "迁移规则预览")
	public static class MigrationRulePreview {

		@Schema(description = "规则类型")
		private String ruleType;

		@Schema(description = "规则描述")
		private String description;

		@Schema(description = "受影响实例数")
		private Long affectedCount;

	}

}
