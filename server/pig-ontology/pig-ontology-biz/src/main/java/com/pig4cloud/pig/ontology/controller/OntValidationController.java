/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.dto.ValidationRunRequest;
import com.pig4cloud.pig.ontology.validation.model.ValidationResult;
import com.pig4cloud.pig.ontology.validation.service.ValidationExecutorRegistry;
import com.pig4cloud.pig.ontology.validation.service.ValidationOrchestrator;
import com.pig4cloud.pig.ontology.validation.service.ValidationReportService;
import com.pig4cloud.pig.ontology.vo.ExecutorInfoVO;
import com.pig4cloud.pig.ontology.vo.ValidationInstanceResultVO;
import com.pig4cloud.pig.ontology.vo.ValidationReportVO;
import com.pig4cloud.pig.ontology.vo.ValidationStatusVO;
import com.pig4cloud.pig.ontology.vo.ValidationSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 校验引擎管理。
 *
 * @author youming
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ontology/validation")
@Tag(description = "校验引擎管理", name = "校验引擎管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntValidationController {

	private final ValidationOrchestrator orchestrator;

	private final ValidationReportService reportService;

	private final ValidationExecutorRegistry executorRegistry;

	/**
	 * 全量校验（异步）。
	 * @param request 校验请求
	 * @return 校验状态
	 */
	@PostMapping("/run")
	@SysLog("执行全量校验")
	@HasPermission("ontology_validation_run")
	@Operation(summary = "全量校验", description = "对指定本体工程执行全量校验，异步执行")
	public R<ValidationStatusVO> runValidation(@RequestBody @Valid ValidationRunRequest request) {
		R<Long> result = orchestrator.validateOntology(request.getOntologyId());
		if (!result.isOk()) {
			return R.failed(result.getMsg());
		}
		ValidationStatusVO vo = new ValidationStatusVO();
		vo.setReportId(result.getData());
		vo.setStatus("RUNNING");
		vo.setMessage("全量校验已提交，请稍后查看报告");
		return R.ok(vo);
	}

	/**
	 * 单实例校验（同步）。
	 * @param instanceId 实例ID
	 * @return 校验结果
	 */
	@PostMapping("/instance/{instanceId}")
	@SysLog("执行单实例校验")
	@HasPermission("ontology_validation_run")
	@Operation(summary = "单实例校验", description = "对指定实例执行同步校验，返回结果列表")
	public R<ValidationInstanceResultVO> validateInstance(@PathVariable Long instanceId) {
		List<ValidationResult> results = orchestrator.validateInstance(instanceId);

		ValidationInstanceResultVO vo = new ValidationInstanceResultVO();
		vo.setInstanceId(instanceId);
		vo.setResults(new ArrayList<>());

		int violationCount = 0;
		int warningCount = 0;
		int infoCount = 0;

		for (ValidationResult result : results) {
			com.pig4cloud.pig.ontology.vo.ValidationResultVO resultVO = new com.pig4cloud.pig.ontology.vo.ValidationResultVO();
			resultVO.setSeverity(result.getSeverity() != null ? result.getSeverity().name() : "INFO");
			resultVO.setFocusNode(result.getFocusNode());
			resultVO.setResultPath(result.getResultPath());
			resultVO.setRuleName(result.getRuleName());
			resultVO.setRuleCode(result.getRuleCode());
			resultVO.setMessage(result.getMessage());
			resultVO.setExpectedValue(result.getExpectedValue());
			resultVO.setActualValue(result.getActualValue());
			resultVO.setSuggestion(result.getSuggestion());
			vo.getResults().add(resultVO);

			if (result.getSeverity() == ValidationResult.Severity.VIOLATION) {
				violationCount++;
			}
			else if (result.getSeverity() == ValidationResult.Severity.WARNING) {
				warningCount++;
			}
			else {
				infoCount++;
			}
		}

		vo.setConforms(violationCount == 0);
		vo.setViolationCount(violationCount);
		vo.setWarningCount(warningCount);
		vo.setInfoCount(infoCount);

		return R.ok(vo);
	}

	/**
	 * 获取校验报告详情。
	 * @param reportId 报告ID
	 * @param severity 严重程度过滤
	 * @param page 页码
	 * @param size 每页大小
	 * @return 报告详情
	 */
	@GetMapping("/report/{reportId}")
	@HasPermission("ontology_validation_report")
	@Operation(summary = "校验报告详情")
	public R<ValidationReportVO> getReport(@PathVariable Long reportId,
			@RequestParam(required = false) String severity,
			@RequestParam(defaultValue = "1") Integer page,
			@RequestParam(defaultValue = "20") Integer size) {
		return R.ok(reportService.getReportDetail(reportId, severity, page, size));
	}

	/**
	 * 校验报告分页列表。
	 * @param ontologyId 本体工程ID
	 * @param status 状态
	 * @param conforms 是否通过
	 * @param page 页码
	 * @param size 每页大小
	 * @return 分页结果
	 */
	@GetMapping("/reports")
	@HasPermission("ontology_validation_view")
	@Operation(summary = "校验报告列表")
	public R<IPage<ValidationSummaryVO>> reportPage(
			@RequestParam(required = false) Long ontologyId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Boolean conforms,
			@RequestParam(defaultValue = "1") Integer page,
			@RequestParam(defaultValue = "20") Integer size) {
		return R.ok(reportService.reportPage(ontologyId, status, conforms, page, size));
	}

	/**
	 * 获取最近一次全量校验报告。
	 * @param ontologyId 本体工程ID
	 * @return 报告摘要
	 */
	@GetMapping("/latest/{ontologyId}")
	@HasPermission("ontology_validation_view")
	@Operation(summary = "最近一次全量校验报告")
	public R<ValidationSummaryVO> getLatest(@PathVariable Long ontologyId) {
		return R.ok(reportService.getLatestReport(ontologyId));
	}

	/**
	 * 查询异步校验状态。
	 * @param reportId 报告ID
	 * @return 状态
	 */
	@GetMapping("/status/{reportId}")
	@HasPermission("ontology_validation_view")
	@Operation(summary = "查询校验状态")
	public R<ValidationStatusVO> getStatus(@PathVariable Long reportId) {
		return R.ok(reportService.getStatus(reportId));
	}

	/**
	 * 查看已注册的执行器列表。
	 * @return 执行器信息列表
	 */
	@GetMapping("/executors")
	@HasPermission("ontology_validation_view")
	@Operation(summary = "已注册的校验执行器列表")
	public R<List<ExecutorInfoVO>> getExecutors() {
		List<ExecutorInfoVO> list = executorRegistry.getExecutors()
			.stream()
			.map(exec -> {
				ExecutorInfoVO vo = new ExecutorInfoVO();
				vo.setExecutorCode(exec.getExecutorCode());
				vo.setValidationMode(exec.getValidationMode());
				vo.setExecutorClass(exec.getClass().getSimpleName());
				return vo;
			})
			.toList();
		return R.ok(list);
	}

}
