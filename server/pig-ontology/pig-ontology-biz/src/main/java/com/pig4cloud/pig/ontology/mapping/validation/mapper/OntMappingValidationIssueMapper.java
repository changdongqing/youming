/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.mapping.validation.entity.OntMappingValidationIssue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 映射校验问题 Mapper（18-06 §5）。
 *
 * @author youming
 */
@Mapper
public interface OntMappingValidationIssueMapper extends BaseMapper<OntMappingValidationIssue> {

	/**
	 * 查询报告下所有未确认的 WARNING 问题。
	 * @param reportId 报告ID
	 * @return 未确认 WARNING 列表
	 */
	@Select("SELECT * FROM ont_mapping_validation_issue "
			+ "WHERE report_id = #{reportId} AND severity = 'WARNING' "
			+ "AND acknowledged = '0' AND del_flag = '0' ORDER BY sort_order")
	List<OntMappingValidationIssue> selectUnacknowledgedWarnings(@Param("reportId") Long reportId);

	/**
	 * 统计报告下未确认 WARNING 数量。
	 * @param reportId 报告ID
	 * @return 数量
	 */
	@Select("SELECT COUNT(*) FROM ont_mapping_validation_issue "
			+ "WHERE report_id = #{reportId} AND severity = 'WARNING' "
			+ "AND acknowledged = '0' AND del_flag = '0'")
	int countUnacknowledgedWarnings(@Param("reportId") Long reportId);

}
