/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.mapping.entity.OntPendingRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 待解析关系 Mapper（18-05 §6）。
 *
 * @author youming
 */
@Mapper
public interface OntPendingRelationMapper extends BaseMapper<OntPendingRelation> {

}
