/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.version.entity.OntInstanceMigrationJob;
import org.apache.ibatis.annotations.Mapper;

/**
 * 实例迁移作业 Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntInstanceMigrationJobMapper extends BaseMapper<OntInstanceMigrationJob> {

}
