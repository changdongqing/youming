/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.executor;

import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntAxiomRuleTarget;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.validation.model.ValidationResult;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 执行器公共基类，提供通用辅助方法。
 *
 * @author youming
 */
@Slf4j
public abstract class AbstractValidationExecutor implements ValidationExecutor {

	private static final int MAX_DEPTH = 100;

	protected final OntEntityTypeMapper entityTypeMapper;

	protected final OntEntityTypeHierarchyMapper hierarchyMapper;

	protected AbstractValidationExecutor(OntEntityTypeMapper entityTypeMapper,
			OntEntityTypeHierarchyMapper hierarchyMapper) {
		this.entityTypeMapper = entityTypeMapper;
		this.hierarchyMapper = hierarchyMapper;
	}

	/**
	 * 从规则严重程度字段构建 Severity。
	 * @param rule 规则
	 * @return 严重程度
	 */
	protected ValidationResult.Severity severityOf(OntAxiomRule rule) {
		String severity = rule.getSeverity();
		if (severity == null) {
			return ValidationResult.Severity.VIOLATION;
		}
		try {
			return ValidationResult.Severity.valueOf(severity);
		}
		catch (IllegalArgumentException e) {
			return ValidationResult.Severity.VIOLATION;
		}
	}

	/**
	 * 按绑定角色查找目标绑定。
	 * @param targets 目标绑定列表
	 * @param role 角色名
	 * @return 匹配的第一条目标绑定，不存在返回 null
	 */
	protected OntAxiomRuleTarget findTarget(List<OntAxiomRuleTarget> targets, String role) {
		return targets.stream()
			.filter(t -> role.equals(t.getBindingRole()))
			.findFirst()
			.orElse(null);
	}

	/**
	 * 按绑定角色查找全部目标绑定。
	 * @param targets 目标绑定列表
	 * @param role 角色名
	 * @return 匹配的目标绑定列表
	 */
	protected List<OntAxiomRuleTarget> findTargets(List<OntAxiomRuleTarget> targets, String role) {
		return targets.stream().filter(t -> role.equals(t.getBindingRole())).collect(Collectors.toList());
	}

	/**
	 * 收集实体类型及其所有子类型（递归BFS向下）。
	 * @param rootTypeId 根实体类型ID
	 * @return 包含根类型及其所有子类的ID集合
	 */
	protected Set<Long> collectDescendants(Long rootTypeId) {
		Set<Long> result = new HashSet<>();
		Deque<Long> queue = new ArrayDeque<>();
		queue.add(rootTypeId);
		int depth = 0;
		while (!queue.isEmpty() && depth < MAX_DEPTH) {
			Long current = queue.poll();
			if (result.add(current)) {
				List<OntEntityTypeHierarchy> children = hierarchyMapper.selectList(
						Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
								.eq(OntEntityTypeHierarchy::getParentId, current));
				for (OntEntityTypeHierarchy h : children) {
					queue.add(h.getChildId());
				}
			}
			depth++;
		}
		return result;
	}

	/**
	 * 收集实体类型及其所有祖先（递归BFS向上，含自身）。
	 * @param typeId 实体类型ID
	 * @return 包含自身及其所有祖先的ID集合
	 */
	protected Set<Long> collectAncestors(Long typeId) {
		Set<Long> result = new HashSet<>();
		Deque<Long> queue = new ArrayDeque<>();
		queue.add(typeId);
		int depth = 0;
		while (!queue.isEmpty() && depth < MAX_DEPTH) {
			Long current = queue.poll();
			if (result.add(current)) {
				List<OntEntityTypeHierarchy> parents = hierarchyMapper.selectList(
						Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
								.eq(OntEntityTypeHierarchy::getChildId, current));
				for (OntEntityTypeHierarchy h : parents) {
					queue.add(h.getParentId());
				}
			}
			depth++;
		}
		return result;
	}

	/**
	 * 批量加载实体类型。
	 * @param ids 实体类型ID集合
	 * @return ID -> 实体类型映射
	 */
	protected Map<Long, OntEntityType> batchLoadEntityTypes(Set<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyMap();
		}
		return entityTypeMapper.selectBatchIds(ids)
			.stream()
			.collect(Collectors.toMap(OntEntityType::getId, Function.identity()));
	}

	/**
	 * 构建一个违规结果。
	 * @param rule 规则
	 * @param focusNode 违规实例IRI
	 * @param resultPath 违规属性IRI
	 * @param message 说明
	 * @param expectedValue 期望值
	 * @param actualValue 实际值
	 * @param suggestion 修复建议
	 * @return 校验结果
	 */
	protected ValidationResult violation(OntAxiomRule rule, String focusNode, String resultPath,
			String message, String expectedValue, String actualValue, String suggestion) {
		return ValidationResult.builder()
			.severity(severityOf(rule))
			.focusNode(focusNode)
			.resultPath(resultPath)
			.ruleName(rule.getName())
			.ruleCode(rule.getRuleCode())
			.message(message)
			.expectedValue(expectedValue)
			.actualValue(actualValue)
			.suggestion(suggestion)
			.build();
	}

}
