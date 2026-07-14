/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 恢复计划 VO。
 *
 * @author youming
 */
@Data
@Schema(description = "恢复计划")
public class RestorePlanVO {

	@Schema(description = "目标历史版本ID")
	private Long targetVersionId;

	@Schema(description = "目标历史版本号")
	private String targetVersionNumber;

	@Schema(description = "兼容性")
	private String compatibility;

	@Schema(description = "BREAKING原因列表")
	private List<String> breakingReasons;

	@Schema(description = "需要恢复的Schema变更列表")
	private List<VersionDiffVO.DiffResource> schemaToRestore;

	@Schema(description = "需要移除的Schema变更列表")
	private List<VersionDiffVO.DiffResource> schemaToRemove;

	@Schema(description = "需要的迁移规则列表")
	private List<String> migrationRules;

	@Schema(description = "风险提示")
	private List<String> riskWarnings;

}
