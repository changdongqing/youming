/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation;

/**
 * 映射校验器接口（18-06 §7）。
 * <p>
 * 校验器显式注册，不通过包扫描顺序决定执行次序。
 * 某P0前置失败后可跳过依赖校验，但必须报告 {@code SKIPPED_DUE_TO_PREREQUISITE} 信息项。
 *
 * @author youming
 */
public interface MappingValidator {

	/**
	 * 校验器编码。
	 */
	String code();

	/**
	 * 执行顺序（升序）。
	 */
	int order();

	/**
	 * 执行校验。
	 * @param context 校验上下文
	 * @param issues 问题收集器
	 */
	void validate(MappingValidationContext context, IssueCollector issues);

}
