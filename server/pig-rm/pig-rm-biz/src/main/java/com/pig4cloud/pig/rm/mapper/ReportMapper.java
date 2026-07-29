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

package com.pig4cloud.pig.rm.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.rm.api.vo.DevTaskStatReportVO;
import com.pig4cloud.pig.rm.api.vo.RequirementDetailReportVO;
import com.pig4cloud.pig.rm.api.vo.RequirementStatReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 报表专用 Mapper（注解 SQL）
 *
 * @author youming
 * @date 2026-07-29
 */
@Mapper
public interface ReportMapper {

	// ========== RPT-01 需求明细 ==========

	@Select("""
			<script>
			SELECT r.req_code, r.title, r.source, r.customer_project,
			       r.expect_complete_date, r.initiator_id, r.initiator_dept_id,
			       r.status, r.design_workload, r.design_plan_date, r.design_actual_date,
			       r.create_time
			FROM rm_requirement r
			WHERE r.del_flag = '0'
			    <if test="source != null and source != ''">AND r.source = #{source}</if>
			    <if test="customerProject != null and customerProject != ''">
			        AND r.customer_project LIKE CONCAT('%', #{customerProject}, '%')
			    </if>
			    <if test="status != null and status != ''">AND r.status = #{status}</if>
			    <if test="startDate != null">AND r.create_time >= #{startDate}</if>
			    <if test="endDate != null">AND r.create_time &lt;= #{endDate}</if>
			ORDER BY r.create_time DESC
			</script>
			""")
	IPage<RequirementDetailReportVO> selectRequirementDetail(Page page,
			@Param("source") String source,
			@Param("customerProject") String customerProject,
			@Param("status") String status,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	@Select("""
			<script>
			SELECT r.req_code, r.title, r.source, r.customer_project,
			       r.expect_complete_date, r.initiator_id, r.initiator_dept_id,
			       r.status, r.design_workload, r.design_plan_date, r.design_actual_date,
			       r.create_time
			FROM rm_requirement r
			WHERE r.del_flag = '0'
			    <if test="source != null and source != ''">AND r.source = #{source}</if>
			    <if test="customerProject != null and customerProject != ''">
			        AND r.customer_project LIKE CONCAT('%', #{customerProject}, '%')
			    </if>
			    <if test="status != null and status != ''">AND r.status = #{status}</if>
			    <if test="startDate != null">AND r.create_time >= #{startDate}</if>
			    <if test="endDate != null">AND r.create_time &lt;= #{endDate}</if>
			ORDER BY r.create_time DESC
			</script>
			""")
	List<RequirementDetailReportVO> selectRequirementDetailList(
			@Param("source") String source,
			@Param("customerProject") String customerProject,
			@Param("status") String status,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	// ========== RPT-02 需求统计 ==========

	@Select("""
			SELECT r.source AS key, COUNT(*) AS count
			FROM rm_requirement r
			WHERE r.del_flag = '0'
			    AND (#{startDate} IS NULL OR r.create_time >= #{startDate})
			    AND (#{endDate} IS NULL OR r.create_time <= #{endDate})
			GROUP BY r.source ORDER BY count DESC
			""")
	List<RequirementStatReportVO.StatItem> statBySource(
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	@Select("""
			SELECT r.status AS key, COUNT(*) AS count
			FROM rm_requirement r
			WHERE r.del_flag = '0'
			GROUP BY r.status ORDER BY count DESC
			""")
	List<RequirementStatReportVO.StatItem> statByStatus();

	@Select("""
			SELECT TO_CHAR(r.create_time, 'YYYY-MM') AS key, COUNT(*) AS count
			FROM rm_requirement r
			WHERE r.del_flag = '0'
			    AND (#{startDate} IS NULL OR r.create_time >= #{startDate})
			    AND (#{endDate} IS NULL OR r.create_time <= #{endDate})
			GROUP BY TO_CHAR(r.create_time, 'YYYY-MM')
			ORDER BY key
			""")
	List<RequirementStatReportVO.StatItem> statByMonth(
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	@Select("""
			SELECT COUNT(*) FROM rm_requirement
			WHERE del_flag = '0'
			    AND (#{startDate} IS NULL OR create_time >= #{startDate})
			    AND (#{endDate} IS NULL OR create_time <= #{endDate})
			""")
	Long countTotal(@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	@Select("""
			SELECT COALESCE(SUM(design_workload), 0)
			FROM rm_requirement
			WHERE del_flag = '0'
			    AND (#{startDate} IS NULL OR create_time >= #{startDate})
			    AND (#{endDate} IS NULL OR create_time <= #{endDate})
			""")
	BigDecimal sumDesignWorkload(@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	// ========== RPT-03 开发任务统计 ==========

	@Select("""
			SELECT
			    COUNT(*) AS total,
			    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) AS completed,
			    COUNT(CASE WHEN status = 'COMPLETED' AND actual_end_date <= plan_end_date THEN 1 END) AS on_schedule
			FROM rm_dev_task
			WHERE del_flag = '0'
			    AND (#{requirementId} IS NULL OR requirement_id = #{requirementId})
			    AND (#{assigneeId} IS NULL OR assignee_id = #{assigneeId})
			    AND (#{startDate} IS NULL OR create_time >= #{startDate})
			    AND (#{endDate} IS NULL OR create_time <= #{endDate})
			""")
	Map<String, Object> devTaskStatCount(
			@Param("requirementId") Long requirementId,
			@Param("assigneeId") Long assigneeId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	@Select("""
			SELECT status AS key, COUNT(*) AS count
			FROM rm_dev_task
			WHERE del_flag = '0'
			    AND (#{requirementId} IS NULL OR requirement_id = #{requirementId})
			GROUP BY status
			""")
	List<RequirementStatReportVO.StatItem> devTaskStatByStatus(@Param("requirementId") Long requirementId);

}
