/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 单实例校验结果VO。
 *
 * @author youming
 */
@Data
@Schema(description = "单实例校验结果")
public class ValidationInstanceResultVO {

	@Schema(description = "实例ID")
	private Long instanceId;

	@Schema(description = "实例IRI")
	private String instanceIri;

	@Schema(description = "是否全部通过")
	private Boolean conforms;

	@Schema(description = "校验结果列表")
	private List<ValidationResultVO> results;

	@Schema(description = "违规数")
	private Integer violationCount;

	@Schema(description = "警告数")
	private Integer warningCount;

	@Schema(description = "信息数")
	private Integer infoCount;

}
