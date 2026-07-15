/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.mapping.entity.OntFieldMapping;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字段映射 Mapper（18-04 §6）。
 * <p>
 * 字段映射无乐观锁和行锁需求，随父实体映射级联删除。
 *
 * @author youming
 */
@Mapper
public interface OntFieldMappingMapper extends BaseMapper<OntFieldMapping> {

}
