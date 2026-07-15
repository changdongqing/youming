/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.mapping.datasource.entity.OntDataSourceMetadata;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据源元数据缓存 Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntDataSourceMetadataMapper extends BaseMapper<OntDataSourceMetadata> {

}
