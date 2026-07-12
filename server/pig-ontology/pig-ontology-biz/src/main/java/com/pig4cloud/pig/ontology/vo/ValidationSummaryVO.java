/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 校验报告摘要VO（列表展示用）。
 *
 * @author youming
 */
@Data
@Schema(description = "校验报告摘要")
public class ValidationSummaryVO {

	@Schema(description = "报告ID")
	private Long id;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "是否全部通过")
	private Boolean conforms;

	@Schema(description = "总结果数")
	private Integer totalCount;

	@Schema(description = "违规数")
	private Integer violationCount;

	@Schema(description = "警告数")
	private Integer warningCount;

	@Schema(description = "信息数")
	private Integer infoCount;

	@Schema(description = "执行规则数")
	private Integer ruleCount;

	@Schema(description = "校验实例数")
	private Integer instanceCount;

	@Schema(description = "耗时（毫秒）")
	private Long durationMs;

	@Schema(description = "校验范围")
	private String scope;

	@Schema(description = "状态")
	private String status;

	@Schema(description = "触发人")
	private String triggeredBy;

	@Schema(description = "触发时间")
	private LocalDateTime triggeredAt;

	@Schema(description = "完成时间")
	private LocalDateTime completedAt;

	@Schema(description = "错误信息")
	private String errorMessage;

}
