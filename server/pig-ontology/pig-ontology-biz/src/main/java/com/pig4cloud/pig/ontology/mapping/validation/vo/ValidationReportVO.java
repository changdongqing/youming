/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 校验报告 VO（18-06 §4）。
 *
 * @author youming
 */
@Data
@Schema(description = "校验报告")
public class ValidationReportVO {

	@Schema(description = "报告ID")
	private Long id;

	@Schema(description = "映射版本ID")
	private Long mappingVersionId;

	@Schema(description = "报告状态: RUNNING / PASSED / FAILED / CANCELLED")
	private String reportStatus;

	@Schema(description = "触发类型: MANUAL / PUBLISH_RECHECK / SYSTEM")
	private String triggerType;

	@Schema(description = "配置版本号")
	private Long configRevision;

	@Schema(description = "候选配置哈希")
	private String candidateConfigHash;

	@Schema(description = "本体版本ID")
	private Long ontologyVersionId;

	@Schema(description = "工作区修订号")
	private Long workspaceRevision;

	@Schema(description = "元数据哈希摘要")
	private String metadataHashSummary;

	@Schema(description = "样本数量")
	private Integer sampleSize;

	@Schema(description = "VIOLATION数量")
	private Integer violationCount;

	@Schema(description = "WARNING数量")
	private Integer warningCount;

	@Schema(description = "INFO数量")
	private Integer infoCount;

	@Schema(description = "摘要JSON")
	private String summaryJson;

	@Schema(description = "开始时间")
	private LocalDateTime startedAt;

	@Schema(description = "完成时间")
	private LocalDateTime completedAt;

	@Schema(description = "请求人")
	private String requestedBy;

	@Schema(description = "追踪ID")
	private String traceId;

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

}
