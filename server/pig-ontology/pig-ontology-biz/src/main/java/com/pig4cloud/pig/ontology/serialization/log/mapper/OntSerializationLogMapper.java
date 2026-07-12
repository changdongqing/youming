/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.serialization.log.entity.OntSerializationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 序列化审计日志 Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntSerializationLogMapper extends BaseMapper<OntSerializationLog> {

}
