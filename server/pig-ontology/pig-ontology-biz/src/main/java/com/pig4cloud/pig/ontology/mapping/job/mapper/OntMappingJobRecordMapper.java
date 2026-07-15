/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.mapping.job.entity.OntMappingJobRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 映射作业记录 Mapper（18-07 §4）。
 *
 * @author youming
 */
@Mapper
public interface OntMappingJobRecordMapper extends BaseMapper<OntMappingJobRecord> {

	/**
	 * 查询指定作业的失败记录。
	 * @param jobId 作业ID
	 * @param limit 最大返回数
	 * @return 失败记录列表
	 */
	@Select("SELECT * FROM ont_mapping_job_record "
			+ "WHERE job_id = #{jobId} AND record_status = 'FAILED' AND del_flag = '0' "
			+ "ORDER BY create_time LIMIT #{limit}")
	List<OntMappingJobRecord> selectFailedRecords(@Param("jobId") Long jobId, @Param("limit") int limit);

	/**
	 * 查询指定作业的待解析关系记录。
	 * @param jobId 作业ID
	 * @return 待解析记录列表
	 */
	@Select("SELECT * FROM ont_mapping_job_record "
			+ "WHERE job_id = #{jobId} AND record_status = 'PENDING' AND del_flag = '0' "
			+ "ORDER BY create_time")
	List<OntMappingJobRecord> selectPendingRecords(@Param("jobId") Long jobId);

	/**
	 * 按记录ID列表查询失败记录（重试用）。
	 * @param recordIds 记录ID列表
	 * @return 失败记录列表
	 */
	default List<OntMappingJobRecord> selectByIds(List<Long> recordIds) {
		if (recordIds == null || recordIds.isEmpty()) {
			return List.of();
		}
		return selectList(Wrappers.<OntMappingJobRecord>lambdaQuery()
				.in(OntMappingJobRecord::getId, recordIds)
				.eq(OntMappingJobRecord::getRecordStatus, "FAILED")
				.eq(OntMappingJobRecord::getDelFlag, "0"));
	}

}
