/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.service;

import com.pig4cloud.pig.ontology.validation.executor.ValidationExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行器注册表。
 * <p>
 * 启动时扫描所有 {@link ValidationExecutor} Bean，按 {@code executorCode} 注册。
 * 校验编排器通过 {@code executorCode} 查找对应执行器。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
public class ValidationExecutorRegistry {

	private final Map<String, ValidationExecutor> executors = new ConcurrentHashMap<>();

	public ValidationExecutorRegistry(List<ValidationExecutor> executorList) {
		for (ValidationExecutor executor : executorList) {
			String code = executor.getExecutorCode();
			if (executors.containsKey(code)) {
				throw new IllegalStateException("重复的执行器编码: " + code);
			}
			executors.put(code, executor);
			log.info("注册校验执行器: {} -> {} ({})", code, executor.getClass().getSimpleName(),
					executor.getValidationMode());
		}
		log.info("校验执行器注册完成，共 {} 个", executors.size());
	}

	/**
	 * 按执行器编码查找执行器。
	 * @param executorCode ont_axiom_rule.executor_code
	 * @return 执行器实例
	 * @throws IllegalStateException 执行器未注册
	 */
	public ValidationExecutor getExecutor(String executorCode) {
		ValidationExecutor executor = executors.get(executorCode);
		if (executor == null) {
			throw new IllegalStateException(
					"执行器未注册: " + executorCode + "，请检查该规则对应的执行器是否已实现");
		}
		return executor;
	}

	/**
	 * 获取所有已注册的执行器编码。
	 * @return 不可变编码集合
	 */
	public Set<String> getRegisteredCodes() {
		return Collections.unmodifiableSet(executors.keySet());
	}

	/**
	 * 获取所有已注册的执行器。
	 * @return 不可变执行器列表
	 */
	public List<ValidationExecutor> getExecutors() {
		return List.copyOf(executors.values());
	}

}
