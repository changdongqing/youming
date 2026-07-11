/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 公理规则详情。
 *
 * @author youming
 */
@Data
@Schema(description = "公理规则详情")
public class OntAxiomRuleDetailVO {

	private OntAxiomRule axiomRule;

	@Schema(description = "规则目标绑定集合")
	private List<OntAxiomRuleTargetVO> targets = new ArrayList<>();

	@Schema(description = "生成的OWL预览")
	private String generatedOwlPreview;

	@Schema(description = "生成的SHACL预览")
	private String generatedShaclPreview;

	@Schema(description = "一致性校验警告")
	private List<String> consistencyWarnings = new ArrayList<>();

}
