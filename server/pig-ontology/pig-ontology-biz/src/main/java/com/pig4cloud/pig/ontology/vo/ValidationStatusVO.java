/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 校验状态。
 *
 * @author youming
 */
@Data
@Schema(description = "校验状态")
public class ValidationStatusVO {

	@Schema(description = "报告ID")
	private Long reportId;

	@Schema(description = "状态：RUNNING/COMPLETED/FAILED")
	private String status;

	@Schema(description = "是否全部通过")
	private Boolean conforms;

	@Schema(description = "违规数")
	private Integer violationCount;

	@Schema(description = "耗时（毫秒）")
	private Long durationMs;

	@Schema(description = "提示消息")
	private String message;

}
