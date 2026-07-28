package com.pig4cloud.pig.ontology.modeling.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据属性 Mapper（表前移自 DD9，DD9 复用）
 *
 * @author pig
 * @date 2026-07-28
 */
@Mapper
public interface ModelDatatypePropertyMapper extends MPJBaseMapper<ModelDatatypeProperty> {

}
