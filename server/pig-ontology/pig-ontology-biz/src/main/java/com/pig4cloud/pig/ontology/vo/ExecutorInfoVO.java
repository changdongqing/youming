/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 执行器信息VO。
 *
 * @author youming
 */
@Data
@Schema(description = "执行器信息")
public class ExecutorInfoVO {

	@Schema(description = "执行器编码")
	private String executorCode;

	@Schema(description = "校验模式")
	private String validationMode;

	@Schema(description = "执行器类名")
	private String executorClass;

}
