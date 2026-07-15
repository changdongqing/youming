/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.job.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 映射作业记录明细 VO（18-07 §4）。
 * <p>
 * sourceRecordKeyMasked 为脱敏后的记录键摘要，不暴露源整行。
 *
 * @author youming
 */
@Data
@Schema(description = "映射作业记录")
public class MappingJobRecordVO {

	@Schema(description = "记录ID")
	private Long id;

	@Schema(description = "作业ID")
	private Long jobId;

	@Schema(description = "重试原记录ID")
	private Long retryOfRecordId;

	@Schema(description = "阶段")
	private String phase;

	@Schema(description = "映射编码")
	private String mappingCode;

	@Schema(description = "源对象名")
	private String sourceObject;

	@Schema(description = "脱敏后的记录键摘要")
	private String sourceRecordKeyMasked;

	@Schema(description = "记录动作")
	private String recordAction;

	@Schema(description = "记录状态")
	private String recordStatus;

	@Schema(description = "实例ID")
	private Long instanceId;

	@Schema(description = "错误码")
	private String errorCode;

	@Schema(description = "错误消息")
	private String errorMessage;

	@Schema(description = "重试次数")
	private Integer retryCount;

	@Schema(description = "处理耗时(ms)")
	private Long durationMs;

	@Schema(description = "源记录更新时间")
	private LocalDateTime sourceUpdatedAt;

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

}
