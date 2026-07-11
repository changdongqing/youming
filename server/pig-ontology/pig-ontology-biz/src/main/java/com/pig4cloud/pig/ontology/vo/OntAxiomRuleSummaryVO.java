/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公理规则摘要（列表/分页）。
 *
 * @author youming
 */
@Data
@Schema(description = "公理规则摘要")
public class OntAxiomRuleSummaryVO {

	private OntAxiomRule axiomRule;

	@Schema(description = "目标数量")
	private Integer targetCount;

	@Schema(description = "校验模式标签")
	private String validationModeLabel;

	@Schema(description = "状态标签")
	private String statusLabel;

}
