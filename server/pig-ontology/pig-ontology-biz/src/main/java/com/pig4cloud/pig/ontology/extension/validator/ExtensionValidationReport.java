/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.validator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 扩展合法性校验报告。
 *
 * @author youming
 */
@Data
@Schema(description = "扩展合法性校验报告")
public class ExtensionValidationReport {

	@Schema(description = "是否全部通过（无VIOLATION）")
	private Boolean conforms;

	@Schema(description = "VIOLATION级违规数")
	private long violationCount;

	@Schema(description = "WARNING级警告数")
	private long warningCount;

	@Schema(description = "校验时间")
	private LocalDateTime triggeredAt;

	@Schema(description = "校验结果明细")
	private List<ExtensionValidationResult> results;

}
