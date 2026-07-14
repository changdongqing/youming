/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.security.entity.OntOperationApprovalRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 高风险操作审批请求Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntOperationApprovalRequestMapper extends BaseMapper<OntOperationApprovalRequest> {

}
