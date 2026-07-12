/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.executor;

import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntAxiomRuleTarget;
import com.pig4cloud.pig.ontology.validation.model.ValidationContext;
import com.pig4cloud.pig.ontology.validation.model.ValidationResult;

import java.util.List;

/**
 * 校验执行器 SPI 接口。
 * <p>
 * 每个执行器对应一个 {@code executorCode}，负责执行特定类型的公理规则校验。
 * 执行器是只读消费者，不修改任何数据。
 * </p>
 *
 * @author youming
 */
public interface ValidationExecutor {

	/**
	 * 执行器编码，对应 {@code ont_axiom_rule.executor_code}。
	 * @return 执行器编码
	 */
	String getExecutorCode();

	/**
	 * 校验模式，对应 {@code ont_axiom_rule.validation_mode}。
	 * @return 校验模式
	 */
	String getValidationMode();

	/**
	 * 执行单条规则的校验。
	 * @param rule 规则定义（含 configJson、owlAxiom、shaclShape）
	 * @param targets 规则目标绑定（按 bindingRole 分组前的完整列表）
	 * @param context 校验上下文（ontologyId、Model 缓存、数据访问器）
	 * @return 校验结果列表（空列表表示通过）
	 */
	List<ValidationResult> validate(OntAxiomRule rule, List<OntAxiomRuleTarget> targets, ValidationContext context);

}
