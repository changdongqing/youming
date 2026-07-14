/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 迁移作业状态 VO。
 *
 * @author youming
 */
@Data
@Schema(description = "迁移作业状态")
public class MigrationJobStatusVO {

	@Schema(description = "作业ID")
	private Long id;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "候选版本ID")
	private Long candidateVersionId;

	@Schema(description = "作业状态")
	private String status;

	@Schema(description = "待迁移实例总数")
	private Long totalCount;

	@Schema(description = "已处理实例数")
	private Long processedCount;

	@Schema(description = "成功实例数")
	private Long successCount;

	@Schema(description = "失败实例数")
	private Long failedCount;

	@Schema(description = "错误摘要")
	private String errorSummary;

	@Schema(description = "开始执行时间")
	private LocalDateTime startedAt;

	@Schema(description = "完成时间")
	private LocalDateTime completedAt;

	@Schema(description = "进度百分比")
	private Integer progressPercent;

}
