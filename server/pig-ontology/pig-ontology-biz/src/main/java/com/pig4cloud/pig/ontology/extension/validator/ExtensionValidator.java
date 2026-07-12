/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.validator;

import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 扩展合法性校验编排器。
 *
 * <p>按R1→R2→R3→R4→R5顺序执行5条规则，聚合结果为统一报告。
 * 任一VIOLATION级规则失败时，后续规则仍继续执行（全量校验），
 * 但最终报告的conforms=false会阻断操作。
 *
 * @author youming
 */
@Component
@RequiredArgsConstructor
public class ExtensionValidator {

	private final List<ExtensionValidationRule> rules;

	/**
	 * 执行全量校验（创建模块时，resource=null）。
	 * @param module 扩展模块
	 * @return 校验报告
	 */
	public ExtensionValidationReport validateModule(OntExtensionModule module) {
		return validate(module, null);
	}

	/**
	 * 执行全量校验（注册资源时，resource非空）。
	 * @param module 扩展模块
	 * @param resource 待校验资源
	 * @return 校验报告
	 */
	public ExtensionValidationReport validateResource(OntExtensionModule module, OntExtensionResource resource) {
		return validate(module, resource);
	}

	private ExtensionValidationReport validate(OntExtensionModule module, OntExtensionResource resource) {
		List<ExtensionValidationResult> allResults = new ArrayList<>();
		for (ExtensionValidationRule rule : rules) {
			allResults.addAll(rule.validate(module, resource));
		}

		boolean conforms = allResults.stream()
			.noneMatch(r -> "VIOLATION".equals(r.getSeverity()) && !r.getPassed());

		long violationCount = allResults.stream()
			.filter(r -> "VIOLATION".equals(r.getSeverity()) && !r.getPassed())
			.count();
		long warningCount = allResults.stream()
			.filter(r -> "WARNING".equals(r.getSeverity()) && !r.getPassed())
			.count();

		ExtensionValidationReport report = new ExtensionValidationReport();
		report.setConforms(conforms);
		report.setViolationCount(violationCount);
		report.setWarningCount(warningCount);
		report.setTriggeredAt(LocalDateTime.now());
		report.setResults(allResults);
		return report;
	}

}
