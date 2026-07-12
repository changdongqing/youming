/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.ontology.entity.OntValidationReport;
import com.pig4cloud.pig.ontology.vo.ValidationReportVO;
import com.pig4cloud.pig.ontology.vo.ValidationStatusVO;
import com.pig4cloud.pig.ontology.vo.ValidationSummaryVO;

/**
 * 校验报告服务。
 *
 * @author youming
 */
public interface ValidationReportService extends IService<OntValidationReport> {

	/**
	 * 校验报告分页列表。
	 * @param ontologyId 本体工程ID（可空）
	 * @param status 状态（可空）
	 * @param conforms 是否通过（可空）
	 * @param page 页码
	 * @param size 每页大小
	 * @return 分页结果
	 */
	IPage<ValidationSummaryVO> reportPage(Long ontologyId, String status, Boolean conforms, Integer page, Integer size);

	/**
	 * 获取校验报告详情（含违规明细分页）。
	 * @param reportId 报告ID
	 * @param severity 严重程度过滤（可空）
	 * @param page 页码
	 * @param size 每页大小
	 * @return 报告详情
	 */
	ValidationReportVO getReportDetail(Long reportId, String severity, Integer page, Integer size);

	/**
	 * 获取最近一次全量校验报告。
	 * @param ontologyId 本体工程ID
	 * @return 报告摘要
	 */
	ValidationSummaryVO getLatestReport(Long ontologyId);

	/**
	 * 查询异步校验状态。
	 * @param reportId 报告ID
	 * @return 状态
	 */
	ValidationStatusVO getStatus(Long reportId);

}
