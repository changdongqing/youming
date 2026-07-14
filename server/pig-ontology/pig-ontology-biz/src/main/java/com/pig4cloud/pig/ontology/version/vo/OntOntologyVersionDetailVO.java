/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 版本详情 VO。
 *
 * @author youming
 */
@Data
@Schema(description = "版本详情")
public class OntOntologyVersionDetailVO {

	@Schema(description = "主键ID")
	private Long id;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "语义版本号")
	private String versionNumber;

	@Schema(description = "版本IRI")
	private String versionIri;

	@Schema(description = "前序版本ID")
	private Long priorVersionId;

	@Schema(description = "前序版本号")
	private String priorVersionNumber;

	@Schema(description = "恢复来源版本ID")
	private Long restoreSourceVersionId;

	@Schema(description = "恢复来源版本号")
	private String restoreSourceVersionNumber;

	@Schema(description = "兼容性")
	private String compatibility;

	@Schema(description = "发布状态")
	private String releaseStatus;

	@Schema(description = "发布说明")
	private String releaseNotes;

	@Schema(description = "快照格式版本")
	private Integer snapshotFormatVersion;

	@Schema(description = "快照SHA-256哈希")
	private String snapshotHash;

	@Schema(description = "差异摘要JSON")
	private String diffSummary;

	@Schema(description = "迁移计划JSON")
	private String migrationPlan;

	@Schema(description = "关联校验报告ID")
	private Long validationReportId;

	@Schema(description = "工作区修订号")
	private Long workspaceRevision;

	@Schema(description = "发布人")
	private String publishedBy;

	@Schema(description = "发布时间")
	private LocalDateTime publishedAt;

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@Schema(description = "是否为当前版本")
	private Boolean isCurrent;

}
