/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.precheck;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntValidationReport;
import com.pig4cloud.pig.ontology.mapper.OntValidationReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 导出前置校验服务。
 * <p>
 * PRD §3.3/§6.3 要求"导出前强制全量校验；Error未清零时禁用发布导出"。
 * 本服务检查最近一次全量校验报告，决定是否放行导出。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportPrecheckService {

	private final OntValidationReportMapper reportMapper;

	/**
	 * 导出前置校验。
	 * @param ontologyId 本体工程ID
	 * @param force 是否强制导出（跳过校验拦截）
	 * @return 校验通过返回 passed；校验未通过返回 blocked
	 */
	public ExportPrecheckResult check(Long ontologyId, boolean force) {
		if (force) {
			log.info("导出强制跳过前置校验, ontologyId={}", ontologyId);
			return ExportPrecheckResult.passed();
		}

		// 查询最近一次全量校验报告
		OntValidationReport latestReport = reportMapper.selectOne(
			Wrappers.<OntValidationReport>lambdaQuery()
				.eq(OntValidationReport::getOntologyId, ontologyId)
				.eq(OntValidationReport::getScope, "FULL")
				.eq(OntValidationReport::getStatus, "COMPLETED")
				.orderByDesc(OntValidationReport::getTriggeredAt)
				.last("LIMIT 1"));

		if (latestReport == null) {
			return ExportPrecheckResult.blocked(
				"尚未执行全量校验，请先在校验中心执行全量校验后再导出", null);
		}

		if (!Boolean.TRUE.equals(latestReport.getConforms())) {
			int violations = latestReport.getViolationCount() != null
				? latestReport.getViolationCount() : 0;
			return ExportPrecheckResult.blocked(
				"最近一次全量校验存在 " + violations + " 条违规，请修复后导出或使用强制导出",
				latestReport.getId());
		}

		return ExportPrecheckResult.passed();
	}

}
