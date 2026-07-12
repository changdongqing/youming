/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 扩展组件（推理引擎）视图。
 *
 * @author youming
 */
@Data
@Schema(description = "扩展组件视图")
public class ExtensionComponentVO {

	@Schema(description = "Spring Bean名称")
	private String beanName;

	@Schema(description = "推理引擎名称")
	private String reasonerName;

	@Schema(description = "能力声明列表")
	private String[] capabilities;

	@Schema(description = "是否支持公理一致性检测")
	private Boolean checkConsistencySupported;

	@Schema(description = "是否支持推理闭包")
	private Boolean inferEntailmentsSupported;

}
