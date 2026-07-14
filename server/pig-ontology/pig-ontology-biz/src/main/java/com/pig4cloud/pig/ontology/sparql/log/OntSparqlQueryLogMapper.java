/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.log;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.sparql.log.entity.OntSparqlQueryLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * SPARQL 查询日志 Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntSparqlQueryLogMapper extends BaseMapper<OntSparqlQueryLog> {

}
