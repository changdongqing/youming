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

package com.pig4cloud.pig.rm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.rm.api.vo.DevTaskStatReportVO;
import com.pig4cloud.pig.rm.api.vo.RequirementDetailReportVO;
import com.pig4cloud.pig.rm.api.vo.RequirementStatReportVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 报表服务接口
 *
 * @author youming
 * @date 2026-07-29
 */
public interface ReportService {

	/** RPT-01 需求明细表 */
	IPage<RequirementDetailReportVO> requirementDetail(Page page, String source, String customerProject,
			String status, LocalDate startDate, LocalDate endDate);

	/** RPT-02 需求统计表 */
	RequirementStatReportVO requirementStat(LocalDate startDate, LocalDate endDate);

	/** RPT-03 开发任务统计表 */
	DevTaskStatReportVO devTaskStat(Long requirementId, Long assigneeId, LocalDate startDate, LocalDate endDate);

	/** 需求明细导出数据（配合 @ResponseExcel 注解使用） */
	List<RequirementDetailReportVO> requirementDetailList(String source, String customerProject, String status,
			LocalDate startDate, LocalDate endDate);

}
