/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 校验问题 VO（18-06 §5）。
 *
 * @author youming
 */
@Data
@Schema(description = "校验问题")
public class ValidationIssueVO {

	@Schema(description = "问题ID")
	private Long id;

	@Schema(description = "报告ID")
	private Long reportId;

	@Schema(description = "严重级别: VIOLATION / WARNING / INFO")
	private String severity;

	@Schema(description = "问题编码")
	private String issueCode;

	@Schema(description = "范围类型: PROJECT / VERSION / SOURCE / ENTITY / FIELD / RELATION / RECORD")
	private String scopeType;

	@Schema(description = "范围引用")
	private String scopeRef;

	@Schema(description = "问题描述")
	private String message;

	@Schema(description = "修复建议")
	private String suggestion;

	@Schema(description = "源记录键哈希")
	private String sourceRecordKeyHash;

	@Schema(description = "是否已确认: 0否 1是")
	private String acknowledged;

	@Schema(description = "确认人")
	private String acknowledgedBy;

	@Schema(description = "确认时间")
	private LocalDateTime acknowledgedAt;

	@Schema(description = "排序")
	private Integer sortOrder;

}
