/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.security.entity.OntDataAccessLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据访问审计日志Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntDataAccessLogMapper extends BaseMapper<OntDataAccessLog> {

}
