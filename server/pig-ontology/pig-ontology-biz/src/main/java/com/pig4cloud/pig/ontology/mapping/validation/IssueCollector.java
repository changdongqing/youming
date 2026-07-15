/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation;

import com.pig4cloud.pig.ontology.mapping.validation.entity.OntMappingValidationIssue;

import java.util.ArrayList;
import java.util.List;

/**
 * 校验问题收集器（18-06 §7）。
 * <p>
 * 校验器不抛异常，而是通过收集器记录问题。
 * 严重级别：VIOLATION 阻断发布，WARNING 可由有权限用户确认，INFO 仅记录。
 *
 * @author youming
 */
public class IssueCollector {

	private final List<OntMappingValidationIssue> issues = new ArrayList<>();

	private int violationCount = 0;

	private int warningCount = 0;

	private int infoCount = 0;

	private int sortOrder = 0;

	/**
	 * 记录 VIOLATION 问题。
	 * @param code 问题编码
	 * @param scopeType 范围类型
	 * @param scopeRef 范围引用
	 * @param message 问题描述
	 * @param suggestion 修复建议
	 */
	public void addViolation(String code, String scopeType, String scopeRef, String message, String suggestion) {
		OntMappingValidationIssue issue = createIssue("VIOLATION", code, scopeType, scopeRef, message, suggestion);
		issues.add(issue);
		violationCount++;
	}

	/**
	 * 记录 WARNING 问题。
	 */
	public void addWarning(String code, String scopeType, String scopeRef, String message, String suggestion) {
		OntMappingValidationIssue issue = createIssue("WARNING", code, scopeType, scopeRef, message, suggestion);
		issues.add(issue);
		warningCount++;
	}

	/**
	 * 记录 INFO 信息项。
	 */
	public void addInfo(String code, String scopeType, String scopeRef, String message, String suggestion) {
		OntMappingValidationIssue issue = createIssue("INFO", code, scopeType, scopeRef, message, suggestion);
		issues.add(issue);
		infoCount++;
	}

	/**
	 * 记录因前置条件失败而跳过的 INFO 项。
	 * @param validatorCode 被跳过的校验器编码
	 */
	public void addSkipped(String validatorCode) {
		addInfo("SKIPPED_DUE_TO_PREREQUISITE", "VERSION", null,
				"校验器 " + validatorCode + " 因前置条件失败而跳过", null);
	}

	/**
	 * 是否存在 VIOLATION。
	 */
	public boolean hasViolations() {
		return violationCount > 0;
	}

	public List<OntMappingValidationIssue> getIssues() {
		return List.copyOf(issues);
	}

	public int getViolationCount() {
		return violationCount;
	}

	public int getWarningCount() {
		return warningCount;
	}

	public int getInfoCount() {
		return infoCount;
	}

	public int getTotalCount() {
		return violationCount + warningCount + infoCount;
	}

	private OntMappingValidationIssue createIssue(String severity, String code, String scopeType,
			String scopeRef, String message, String suggestion) {
		OntMappingValidationIssue issue = new OntMappingValidationIssue();
		issue.setSeverity(severity);
		issue.setIssueCode(code);
		issue.setScopeType(scopeType);
		issue.setScopeRef(scopeRef);
		issue.setMessage(message);
		issue.setSuggestion(suggestion);
		issue.setAcknowledged("0");
		issue.setSortOrder(sortOrder++);
		issue.setCreateBy("system");
		issue.setUpdateBy("system");
		return issue;
	}

}
