/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 导出结果。
 *
 * @author youming
 */
@Data
@Schema(description = "导出结果")
public class ExportResultVO {

	@Schema(description = "RDF格式")
	private String format;

	@Schema(description = "RDF文本内容")
	private String content;

	@Schema(description = "三元组数")
	private long tripleCount;

	@Schema(description = "导出范围")
	private String scope;

	@Schema(description = "谓词策略")
	private String predicateStrategy;

	@Schema(description = "前置校验结果")
	private PrecheckVO precheck;

	@Schema(description = "审计日志ID")
	private Long logId;

	/**
	 * 前置校验摘要。
	 */
	@Data
	@Schema(description = "前置校验结果")
	public static class PrecheckVO {

		@Schema(description = "是否通过")
		private boolean passed;

		@Schema(description = "拦截原因")
		private String blockedReason;

		@Schema(description = "校验报告ID")
		private Long validationReportId;

	}

}
