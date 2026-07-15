/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 发布风险摘要 VO（18-06 §12, §14 publish/prepare）。
 * <p>
 * 在正式发布前向用户展示风险摘要，包括高风险变化、未确认 WARNING 数等。
 *
 * @author youming
 */
@Data
@Schema(description = "发布风险摘要")
public class PublishPrepareResultVO {

	@Schema(description = "映射版本ID")
	private Long versionId;

	@Schema(description = "版本号")
	private String versionNumber;

	@Schema(description = "版本状态")
	private String versionStatus;

	@Schema(description = "是否可发布")
	private Boolean publishable;

	@Schema(description = "校验报告ID")
	private Long validationReportId;

	@Schema(description = "校验报告状态")
	private String reportStatus;

	@Schema(description = "VIOLATION数量")
	private Integer violationCount;

	@Schema(description = "WARNING数量")
	private Integer warningCount;

	@Schema(description = "未确认WARNING数量")
	private Integer unacknowledgedWarningCount;

	@Schema(description = "配置哈希")
	private String configHash;

	@Schema(description = "本体版本ID")
	private Long ontologyVersionId;

	@Schema(description = "工作区修订号")
	private Long workspaceRevision;

	@Schema(description = "高风险变化列表")
	private List<RiskItem> highRiskChanges;

	@Schema(description = "发布门禁检查结果列表")
	private List<GateCheckItem> gateChecks;

	/**
	 * 风险项。
	 */
	@Data
	@Schema(description = "风险项")
	public static class RiskItem {

		@Schema(description = "风险类型: IRI_CHANGE / PRIMARY_KEY_CHANGE / DELETE_STRATEGY_CHANGE / SCHEMA_DRIFT")
		private String riskType;

		@Schema(description = "映射编码")
		private String mappingCode;

		@Schema(description = "风险描述")
		private String description;

	}

	/**
	 * 门禁检查项。
	 */
	@Data
	@Schema(description = "门禁检查项")
	public static class GateCheckItem {

		@Schema(description = "检查项名称")
		private String checkName;

		@Schema(description = "是否通过")
		private Boolean passed;

		@Schema(description = "说明")
		private String message;

	}

}
