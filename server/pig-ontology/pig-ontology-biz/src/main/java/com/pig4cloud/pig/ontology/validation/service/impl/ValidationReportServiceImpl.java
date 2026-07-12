/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.ontology.entity.OntValidationReport;
import com.pig4cloud.pig.ontology.entity.OntValidationResult;
import com.pig4cloud.pig.ontology.mapper.OntValidationReportMapper;
import com.pig4cloud.pig.ontology.mapper.OntValidationResultMapper;
import com.pig4cloud.pig.ontology.validation.service.ValidationReportService;
import com.pig4cloud.pig.ontology.vo.ValidationReportVO;
import com.pig4cloud.pig.ontology.vo.ValidationResultVO;
import com.pig4cloud.pig.ontology.vo.ValidationStatusVO;
import com.pig4cloud.pig.ontology.vo.ValidationSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 校验报告服务实现。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationReportServiceImpl extends ServiceImpl<OntValidationReportMapper, OntValidationReport>
		implements ValidationReportService {

	private final OntValidationResultMapper validationResultMapper;

	@Override
	public IPage<ValidationSummaryVO> reportPage(Long ontologyId, String status, Boolean conforms, Integer page,
			Integer size) {
		Page<OntValidationReport> pageParam = new Page<>(page, size);
		Page<OntValidationReport> result = this.page(pageParam,
				Wrappers.<OntValidationReport>lambdaQuery()
						.eq(ontologyId != null, OntValidationReport::getOntologyId, ontologyId)
						.eq(status != null, OntValidationReport::getStatus, status)
						.eq(conforms != null, OntValidationReport::getConforms, conforms)
						.orderByDesc(OntValidationReport::getTriggeredAt));

		Page<ValidationSummaryVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
		voPage.setRecords(result.getRecords().stream().map(this::toSummaryVO).collect(Collectors.toList()));
		return voPage;
	}

	@Override
	public ValidationReportVO getReportDetail(Long reportId, String severity, Integer page, Integer size) {
		ValidationReportVO vo = new ValidationReportVO();

		OntValidationReport report = this.getById(reportId);
		if (report == null) {
			return vo;
		}
		vo.setReport(toSummaryVO(report));

		Page<OntValidationResult> pageParam = new Page<>(page, size);
		Page<OntValidationResult> resultPage = validationResultMapper.selectPage(pageParam,
				Wrappers.<OntValidationResult>lambdaQuery()
						.eq(OntValidationResult::getReportId, reportId)
						.eq(severity != null, OntValidationResult::getSeverity, severity)
						.orderByAsc(OntValidationResult::getSortOrder));

		Page<ValidationResultVO> voResultPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(),
				resultPage.getTotal());
		voResultPage.setRecords(resultPage.getRecords().stream().map(this::toResultVO).collect(Collectors.toList()));
		vo.setResults(voResultPage);

		return vo;
	}

	@Override
	public ValidationSummaryVO getLatestReport(Long ontologyId) {
		OntValidationReport report = this.getOne(
				Wrappers.<OntValidationReport>lambdaQuery()
						.eq(OntValidationReport::getOntologyId, ontologyId)
						.eq(OntValidationReport::getScope, "FULL")
						.orderByDesc(OntValidationReport::getTriggeredAt)
						.last("LIMIT 1"));
		return report != null ? toSummaryVO(report) : null;
	}

	@Override
	public ValidationStatusVO getStatus(Long reportId) {
		OntValidationReport report = this.getById(reportId);
		if (report == null) {
			return null;
		}
		ValidationStatusVO vo = new ValidationStatusVO();
		vo.setReportId(report.getId());
		vo.setStatus(report.getStatus());
		vo.setConforms(report.getConforms());
		vo.setViolationCount(report.getViolationCount());
		vo.setDurationMs(report.getDurationMs());
		return vo;
	}

	/**
	 * 保存校验结果列表。
	 * @param reportId 报告ID
	 * @param results 结果列表
	 */
	public void saveResults(Long reportId, List<com.pig4cloud.pig.ontology.validation.model.ValidationResult> results) {
		int sortOrder = 0;
		for (com.pig4cloud.pig.ontology.validation.model.ValidationResult result : results) {
			OntValidationResult entity = result.toEntity(reportId, sortOrder++);
			validationResultMapper.insert(entity);
		}
	}

	private ValidationSummaryVO toSummaryVO(OntValidationReport report) {
		ValidationSummaryVO vo = new ValidationSummaryVO();
		vo.setId(report.getId());
		vo.setOntologyId(report.getOntologyId());
		vo.setConforms(report.getConforms());
		vo.setTotalCount(report.getTotalCount());
		vo.setViolationCount(report.getViolationCount());
		vo.setWarningCount(report.getWarningCount());
		vo.setInfoCount(report.getInfoCount());
		vo.setRuleCount(report.getRuleCount());
		vo.setInstanceCount(report.getInstanceCount());
		vo.setDurationMs(report.getDurationMs());
		vo.setScope(report.getScope());
		vo.setStatus(report.getStatus());
		vo.setTriggeredBy(report.getTriggeredBy());
		vo.setTriggeredAt(report.getTriggeredAt());
		vo.setCompletedAt(report.getCompletedAt());
		vo.setErrorMessage(report.getErrorMessage());
		return vo;
	}

	private ValidationResultVO toResultVO(OntValidationResult entity) {
		ValidationResultVO vo = new ValidationResultVO();
		vo.setId(entity.getId());
		vo.setSeverity(entity.getSeverity());
		vo.setFocusNode(entity.getFocusNode());
		vo.setResultPath(entity.getResultPath());
		vo.setRuleName(entity.getRuleName());
		vo.setRuleCode(entity.getRuleCode());
		vo.setMessage(entity.getMessage());
		vo.setExpectedValue(entity.getExpectedValue());
		vo.setActualValue(entity.getActualValue());
		vo.setSuggestion(entity.getSuggestion());
		return vo;
	}

}
