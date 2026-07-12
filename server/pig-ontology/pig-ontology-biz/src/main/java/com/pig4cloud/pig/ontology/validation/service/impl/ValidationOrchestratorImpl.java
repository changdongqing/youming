/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntAxiomRuleTarget;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntValidationReport;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleMapper;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleTargetMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.validation.executor.ValidationExecutor;
import com.pig4cloud.pig.ontology.validation.model.OntologyModelAssembler;
import com.pig4cloud.pig.ontology.validation.model.ValidationContext;
import com.pig4cloud.pig.ontology.validation.model.ValidationResult;
import com.pig4cloud.pig.ontology.validation.model.ValidationScope;
import com.pig4cloud.pig.ontology.validation.service.ValidationExecutorRegistry;
import com.pig4cloud.pig.ontology.validation.service.ValidationOrchestrator;
import com.pig4cloud.pig.ontology.validation.service.impl.ValidationReportServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 校验编排器实现。
 * <p>
 * 统一入口：加载规则 → 分发执行器 → 聚合结果 → 持久化报告。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationOrchestratorImpl implements ValidationOrchestrator {

	private static final String ACTIVE = "ACTIVE";

	private static final String ENABLED = "1";

	private final OntAxiomRuleMapper axiomRuleMapper;

	private final OntAxiomRuleTargetMapper targetMapper;

	private final OntEntityInstanceMapper instanceMapper;

	private final ValidationExecutorRegistry executorRegistry;

	private final ValidationReportServiceImpl reportService;

	private final OntologyModelAssembler modelAssembler;

	@Override
	public R<Long> validateOntology(Long ontologyId) {
		// 1. 创建报告记录（RUNNING 状态）
		OntValidationReport report = new OntValidationReport();
		report.setOntologyId(ontologyId);
		report.setScope("FULL");
		report.setStatus("RUNNING");
		report.setTotalCount(0);
		report.setViolationCount(0);
		report.setWarningCount(0);
		report.setInfoCount(0);
		report.setRuleCount(0);
		report.setInstanceCount(0);
		report.setTriggeredBy(getCurrentUser());
		report.setTriggeredAt(LocalDateTime.now());
		reportService.save(report);

		// 2. 异步执行校验
		executeAsync(report.getId(), ontologyId);

		return R.ok(report.getId());
	}

	@Async("applicationAsyncTaskExecutor")
	public void executeAsync(Long reportId, Long ontologyId) {
		long startTime = System.currentTimeMillis();
		try {
			log.info("开始全量校验: reportId={}, ontologyId={}", reportId, ontologyId);

			// 1. 构建上下文
			ValidationContext context = ValidationContext.builder()
				.ontologyId(ontologyId)
				.scope(ValidationScope.FULL)
				.modelAssembler(modelAssembler)
				.build();

			// 2. 加载 ACTIVE 规则
			List<OntAxiomRule> rules = axiomRuleMapper.selectList(
					Wrappers.<OntAxiomRule>lambdaQuery()
							.eq(OntAxiomRule::getOntologyId, ontologyId)
							.eq(OntAxiomRule::getStatus, ACTIVE)
							.eq(OntAxiomRule::getIsEnabled, ENABLED)
							.orderByAsc(OntAxiomRule::getSortOrder));

			log.info("加载到 {} 条 ACTIVE 规则", rules.size());

			// 3. 批量加载规则目标绑定
			Set<Long> ruleIds = rules.stream().map(OntAxiomRule::getId).collect(Collectors.toSet());
			Map<Long, List<OntAxiomRuleTarget>> targetMap = loadTargets(ruleIds);

			// 4. 逐规则执行
			List<ValidationResult> allResults = new ArrayList<>();
			for (OntAxiomRule rule : rules) {
				if (rule.getExecutorCode() == null || rule.getExecutorCode().isBlank()) {
					log.warn("规则 {} 无 executorCode，跳过", rule.getRuleCode());
					continue;
				}
				List<OntAxiomRuleTarget> targets = targetMap.getOrDefault(rule.getId(), List.of());
				try {
					ValidationExecutor executor = executorRegistry.getExecutor(rule.getExecutorCode());
					List<ValidationResult> results = executor.validate(rule, targets, context);
					allResults.addAll(results);
				}
				catch (Exception e) {
					log.error("执行器 {} 执行失败: ruleCode={}, error={}", rule.getExecutorCode(),
							rule.getRuleCode(), e.getMessage(), e);
					allResults.add(ValidationResult.builder()
						.severity(ValidationResult.Severity.WARNING)
						.ruleName(rule.getName())
						.ruleCode(rule.getRuleCode())
						.message("执行器执行异常: " + e.getMessage())
						.build());
				}
			}

			// 5. 聚合结果（按严重程度排序）
			allResults.sort(Comparator.comparingInt((ValidationResult r) -> r.getSeverity().ordinal()));

			int violationCount = (int) allResults.stream()
				.filter(r -> r.getSeverity() == ValidationResult.Severity.VIOLATION).count();
			int warningCount = (int) allResults.stream()
				.filter(r -> r.getSeverity() == ValidationResult.Severity.WARNING).count();
			int infoCount = (int) allResults.stream()
				.filter(r -> r.getSeverity() == ValidationResult.Severity.INFO).count();

			long duration = System.currentTimeMillis() - startTime;
			Long instanceCount = instanceMapper.selectCount(
					Wrappers.<OntEntityInstance>lambdaQuery().eq(OntEntityInstance::getOntologyId, ontologyId));

			// 6. 更新报告
			OntValidationReport updateReport = new OntValidationReport();
			updateReport.setId(reportId);
			updateReport.setConforms(violationCount == 0);
			updateReport.setTotalCount(allResults.size());
			updateReport.setViolationCount(violationCount);
			updateReport.setWarningCount(warningCount);
			updateReport.setInfoCount(infoCount);
			updateReport.setRuleCount(rules.size());
			updateReport.setInstanceCount(instanceCount != null ? instanceCount.intValue() : 0);
			updateReport.setDurationMs(duration);
			updateReport.setStatus("COMPLETED");
			updateReport.setCompletedAt(LocalDateTime.now());
			reportService.updateById(updateReport);

			// 7. 持久化逐条结果
			reportService.saveResults(reportId, allResults);

			log.info("全量校验完成: reportId={}, conforms={}, violations={}, warnings={}, infos={}, duration={}ms",
					reportId, violationCount == 0, violationCount, warningCount, infoCount, duration);

		}
		catch (Exception e) {
			log.error("全量校验失败: reportId={}", reportId, e);
			OntValidationReport failReport = new OntValidationReport();
			failReport.setId(reportId);
			failReport.setStatus("FAILED");
			failReport.setCompletedAt(LocalDateTime.now());
			failReport.setErrorMessage(e.getMessage());
			failReport.setDurationMs(System.currentTimeMillis() - startTime);
			reportService.updateById(failReport);
		}
	}

	@Override
	public List<ValidationResult> validateInstance(Long instanceId) {
		OntEntityInstance instance = instanceMapper.selectById(instanceId);
		if (instance == null) {
			return List.of();
		}

		// 1. 构建上下文（单实例增量）
		ValidationContext context = ValidationContext.builder()
			.ontologyId(instance.getOntologyId())
			.scope(ValidationScope.INSTANCE)
			.targetInstanceId(instanceId)
			.modelAssembler(modelAssembler)
			.build();

		// 2. 加载 ACTIVE 规则
		List<OntAxiomRule> rules = axiomRuleMapper.selectList(
				Wrappers.<OntAxiomRule>lambdaQuery()
						.eq(OntAxiomRule::getOntologyId, instance.getOntologyId())
						.eq(OntAxiomRule::getStatus, ACTIVE)
						.eq(OntAxiomRule::getIsEnabled, ENABLED));

		// 3. 逐规则执行（单实例场景下不做目标类型过滤，全部执行）
		List<ValidationResult> results = new ArrayList<>();
		for (OntAxiomRule rule : rules) {
			if (rule.getExecutorCode() == null || rule.getExecutorCode().isBlank()) {
				continue;
			}
			List<OntAxiomRuleTarget> targets = targetMapper.selectList(
					Wrappers.<OntAxiomRuleTarget>lambdaQuery()
							.eq(OntAxiomRuleTarget::getAxiomRuleId, rule.getId()));
			try {
				ValidationExecutor executor = executorRegistry.getExecutor(rule.getExecutorCode());
				List<ValidationResult> ruleResults = executor.validate(rule, targets, context);
				results.addAll(ruleResults);
			}
			catch (Exception e) {
				log.error("单实例校验执行器 {} 执行失败: ruleCode={}, error={}", rule.getExecutorCode(),
						rule.getRuleCode(), e.getMessage(), e);
			}
		}

		// 按严重程度排序
		results.sort(Comparator.comparingInt((ValidationResult r) -> r.getSeverity().ordinal()));
		return results;
	}

	/**
	 * 批量加载规则目标绑定，按 axiomRuleId 分组。
	 */
	private Map<Long, List<OntAxiomRuleTarget>> loadTargets(Set<Long> ruleIds) {
		if (ruleIds.isEmpty()) {
			return Map.of();
		}
		List<OntAxiomRuleTarget> allTargets = targetMapper.selectList(
				Wrappers.<OntAxiomRuleTarget>lambdaQuery()
						.in(OntAxiomRuleTarget::getAxiomRuleId, ruleIds));
		return allTargets.stream().collect(Collectors.groupingBy(OntAxiomRuleTarget::getAxiomRuleId));
	}

	/**
	 * 获取当前登录用户名。
	 */
	private String getCurrentUser() {
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null && auth.isAuthenticated()) {
				return auth.getName();
			}
		}
		catch (Exception e) {
			log.debug("获取当前用户失败: {}", e.getMessage());
		}
		return "system";
	}

}
