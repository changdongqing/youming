/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 校验器注册表（18-06 §7）。
 * <p>
 * 校验器通过 Spring 自动注入后显式排序，不通过包扫描顺序决定执行次序。
 * 执行时按 {@link MappingValidator#order()} 升序遍历。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MappingValidatorRegistry {

	private final List<MappingValidator> validators;

	/**
	 * 获取按 order 升序排列的全部校验器。
	 * @return 有序列表
	 */
	public List<MappingValidator> getSortedValidators() {
		return validators.stream()
				.sorted(Comparator.comparingInt(MappingValidator::order))
				.toList();
	}

	/**
	 * 获取校验器编码到顺序的映射（用于日志/调试）。
	 */
	public Map<String, Integer> getValidatorOrderMap() {
		return validators.stream()
				.collect(Collectors.toMap(MappingValidator::code, MappingValidator::order));
	}

}
