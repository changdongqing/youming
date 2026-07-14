/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * prepare 响应 VO。
 *
 * @author youming
 */
@Data
@Schema(description = "版本准备结果")
public class VersionPrepareResultVO {

	@Schema(description = "候选版本ID")
	private Long versionId;

	@Schema(description = "版本号")
	private String versionNumber;

	@Schema(description = "版本IRI")
	private String versionIri;

	@Schema(description = "系统判定兼容性")
	private String compatibility;

	@Schema(description = "声明兼容性")
	private String declaredCompatibility;

	@Schema(description = "BREAKING原因列表")
	private List<String> breakingReasons;

	@Schema(description = "差异摘要JSON")
	private String diffSummary;

	@Schema(description = "快照SHA-256哈希")
	private String snapshotHash;

	@Schema(description = "是否可直接激活（非BREAKING时为true）")
	private Boolean canActivate;

}
