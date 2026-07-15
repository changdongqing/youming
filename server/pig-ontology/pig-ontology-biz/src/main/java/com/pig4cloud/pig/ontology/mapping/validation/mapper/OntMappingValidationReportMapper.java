/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.mapping.validation.entity.OntMappingValidationReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 映射校验报告 Mapper（18-06 §4）。
 *
 * @author youming
 */
@Mapper
public interface OntMappingValidationReportMapper extends BaseMapper<OntMappingValidationReport> {

	/**
	 * 行级悲观锁，用于发布门禁检查时锁定报告行。
	 * @param id 报告ID
	 * @return 报告实体（在同一事务内持有行锁）
	 */
	@Select("SELECT * FROM ont_mapping_validation_report WHERE id = #{id} AND del_flag = '0' FOR UPDATE")
	OntMappingValidationReport selectForUpdate(@Param("id") Long id);

}
