/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.validator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 扩展合法性校验单条结果。
 *
 * @author youming
 */
@Data
@Schema(description = "扩展合法性校验结果")
public class ExtensionValidationResult {

	@Schema(description = "规则编号")
	private String ruleCode;

	@Schema(description = "规则名称")
	private String ruleName;

	@Schema(description = "严重级别: VIOLATION/WARNING/INFO")
	private String severity;

	@Schema(description = "校验是否通过")
	private Boolean passed;

	@Schema(description = "违规资源IRI")
	private String resourceIri;

	@Schema(description = "违规说明")
	private String message;

	@Schema(description = "修复建议")
	private String suggestion;

	@Schema(description = "国标条款依据")
	private String gbClause;

	/**
	 * 构建通过结果。
	 */
	public static ExtensionValidationResult pass(String ruleCode, String ruleName, String gbClause) {
		ExtensionValidationResult r = new ExtensionValidationResult();
		r.setRuleCode(ruleCode);
		r.setRuleName(ruleName);
		r.setGbClause(gbClause);
		r.setPassed(true);
		r.setSeverity("INFO");
		r.setMessage("校验通过");
		return r;
	}

	/**
	 * 构建违规结果。
	 */
	public static ExtensionValidationResult fail(String ruleCode, String ruleName, String gbClause,
			String severity, String resourceIri, String message, String suggestion) {
		ExtensionValidationResult r = new ExtensionValidationResult();
		r.setRuleCode(ruleCode);
		r.setRuleName(ruleName);
		r.setGbClause(gbClause);
		r.setPassed(false);
		r.setSeverity(severity);
		r.setResourceIri(resourceIri);
		r.setMessage(message);
		r.setSuggestion(suggestion);
		return r;
	}

}
