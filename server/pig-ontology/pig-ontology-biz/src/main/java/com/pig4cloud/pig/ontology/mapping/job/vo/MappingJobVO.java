/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.job.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 映射作业详情 VO（18-07 §13）。
 *
 * @author youming
 */
@Data
@Schema(description = "映射作业详情")
public class MappingJobVO {

	@Schema(description = "作业ID")
	private Long id;

	@Schema(description = "映射工程ID")
	private Long mappingProjectId;

	@Schema(description = "映射版本ID")
	private Long mappingVersionId;

	@Schema(description = "运行类型")
	private String runType;

	@Schema(description = "触发类型")
	private String triggerType;

	@Schema(description = "作业状态")
	private String jobStatus;

	@Schema(description = "请求人")
	private String requestedBy;

	@Schema(description = "配置哈希")
	private String configHash;

	@Schema(description = "本体版本ID")
	private Long ontologyVersionId;

	@Schema(description = "工作区修订号")
	private Long workspaceRevision;

	@Schema(description = "当前阶段")
	private String currentPhase;

	@Schema(description = "当前映射编码")
	private String currentMappingCode;

	@Schema(description = "当前页号")
	private Long currentPageNo;

	@Schema(description = "页大小")
	private Integer pageSize;

	@Schema(description = "总读取数")
	private Long totalRead;

	@Schema(description = "新建数")
	private Long totalCreated;

	@Schema(description = "更新数")
	private Long totalUpdated;

	@Schema(description = "未变化数")
	private Long totalUnchanged;

	@Schema(description = "跳过数")
	private Long totalSkipped;

	@Schema(description = "失败数")
	private Long totalFailed;

	@Schema(description = "关系数")
	private Long totalRelations;

	@Schema(description = "是否请求取消")
	private String cancelRequested;

	@Schema(description = "开始时间")
	private LocalDateTime startedAt;

	@Schema(description = "完成时间")
	private LocalDateTime finishedAt;

	@Schema(description = "错误码")
	private String errorCode;

	@Schema(description = "错误消息")
	private String errorMessage;

	@Schema(description = "追踪ID")
	private String traceId;

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

}
