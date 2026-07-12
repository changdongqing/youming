/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.service;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.validation.model.ValidationResult;

import java.util.List;

/**
 * 校验编排服务。
 * <p>
 * 统一入口：加载规则 → 分发执行器 → 聚合结果 → 持久化报告。
 * 支持全量校验（异步）和单实例校验（同步）。
 * </p>
 *
 * @author youming
 */
public interface ValidationOrchestrator {

	/**
	 * 全量校验（异步）。
	 * <p>
	 * 对指定本体工程下所有 ACTIVE 规则执行校验，返回报告 ID。
	 * 校验在后台异步执行，前端通过 GET /validation/status 轮询状态。
	 * </p>
	 * @param ontologyId 本体工程ID
	 * @return 报告ID
	 */
	R<Long> validateOntology(Long ontologyId);

	/**
	 * 单实例校验（同步）。
	 * <p>
	 * 对指定实例执行所有适用的 ACTIVE 规则校验，返回结果列表。
	 * 用于实例编辑器实时校验面板。
	 * </p>
	 * @param instanceId 实例ID
	 * @return 校验结果列表
	 */
	List<ValidationResult> validateInstance(Long instanceId);

}
