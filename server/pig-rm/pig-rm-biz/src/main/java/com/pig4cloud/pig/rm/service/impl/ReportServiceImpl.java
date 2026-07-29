/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.rm.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.rm.api.vo.DevTaskStatReportVO;
import com.pig4cloud.pig.rm.api.vo.RequirementDetailReportVO;
import com.pig4cloud.pig.rm.api.vo.RequirementStatReportVO;
import com.pig4cloud.pig.rm.mapper.ReportMapper;
import com.pig4cloud.pig.rm.service.ReportService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 报表服务实现
 *
 * @author youming
 * @date 2026-07-29
 */
@Slf4j
@Service
@AllArgsConstructor
public class ReportServiceImpl implements ReportService {

	private final ReportMapper reportMapper;

	@Override
	public IPage<RequirementDetailReportVO> requirementDetail(Page page, String source, String customerProject,
			String status, LocalDate startDate, LocalDate endDate) {
		return reportMapper.selectRequirementDetail(page, source, customerProject, status, startDate, endDate);
	}

	@Override
	public RequirementStatReportVO requirementStat(LocalDate startDate, LocalDate endDate) {
		RequirementStatReportVO vo = new RequirementStatReportVO();
		vo.setBySource(reportMapper.statBySource(startDate, endDate));
		vo.setByStatus(reportMapper.statByStatus());
		vo.setByMonth(reportMapper.statByMonth(startDate, endDate));
		vo.setTotalCount(reportMapper.countTotal(startDate, endDate));
		vo.setTotalDesignWorkload(reportMapper.sumDesignWorkload(startDate, endDate));
		return vo;
	}

	@Override
	public DevTaskStatReportVO devTaskStat(Long requirementId, Long assigneeId, LocalDate startDate,
			LocalDate endDate) {
		DevTaskStatReportVO vo = new DevTaskStatReportVO();

		Map<String, Object> stat = reportMapper.devTaskStatCount(requirementId, assigneeId, startDate, endDate);
		Long total = toLong(stat.get("total"));
		Long completed = toLong(stat.get("completed"));
		Long onSchedule = toLong(stat.get("on_schedule"));

		vo.setTotalTasks(total);
		vo.setCompletedTasks(completed);
		vo.setOnScheduleTasks(onSchedule);
		vo.setCompletionRate(calcRate(completed, total));
		vo.setScheduleAchievementRate(calcRate(onSchedule, total));
		vo.setByStatus(reportMapper.devTaskStatByStatus(requirementId));
		return vo;
	}

	@Override
	public List<RequirementDetailReportVO> requirementDetailList(String source, String customerProject, String status,
			LocalDate startDate, LocalDate endDate) {
		return reportMapper.selectRequirementDetailList(source, customerProject, status, startDate, endDate);
	}

	private BigDecimal calcRate(Long numerator, Long denominator) {
		if (denominator == null || denominator == 0) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(numerator)
			.multiply(BigDecimal.valueOf(100))
			.divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
	}

	private Long toLong(Object value) {
		if (value == null) {
			return 0L;
		}
		if (value instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(value.toString());
	}

}
