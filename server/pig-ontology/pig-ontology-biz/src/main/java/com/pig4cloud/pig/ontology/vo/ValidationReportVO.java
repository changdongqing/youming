/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 校验报告详情VO。
 *
 * @author youming
 */
@Data
@Schema(description = "校验报告详情")
public class ValidationReportVO {

	@Schema(description = "报告摘要")
	private ValidationSummaryVO report;

	@Schema(description = "违规结果分页")
	private IPage<ValidationResultVO> results;

}
