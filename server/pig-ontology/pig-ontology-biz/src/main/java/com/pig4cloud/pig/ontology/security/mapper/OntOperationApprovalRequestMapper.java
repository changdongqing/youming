/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.security.entity.OntOperationApprovalRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 高风险操作审批请求Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntOperationApprovalRequestMapper extends BaseMapper<OntOperationApprovalRequest> {

	/**
	 * 行锁查询（FOR UPDATE），用于原子消费审批。
	 */
	@Select("SELECT * FROM ont_operation_approval_request WHERE request_no = #{requestNo} FOR UPDATE")
	OntOperationApprovalRequest selectByNoForUpdate(@Param("requestNo") String requestNo);

}
