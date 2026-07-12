/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.precheck;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导出前置校验结果。
 *
 * @author youming
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportPrecheckResult {

	private boolean passed;

	private String blockedReason;

	private Long validationReportId;

	public static ExportPrecheckResult passed() {
		return ExportPrecheckResult.builder().passed(true).build();
	}

	public static ExportPrecheckResult blocked(String reason, Long reportId) {
		return ExportPrecheckResult.builder()
			.passed(false)
			.blockedReason(reason)
			.validationReportId(reportId)
			.build();
	}

}
