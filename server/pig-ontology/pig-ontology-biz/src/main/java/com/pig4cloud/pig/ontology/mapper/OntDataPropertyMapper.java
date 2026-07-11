/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 数据属性 Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntDataPropertyMapper extends BaseMapper<OntDataProperty> {

	/**
	 * 按主键加行锁查询（SELECT ... FOR UPDATE），用于 is_unique 数据属性并发写入校验前锁定数据属性行。
	 * @param id 数据属性ID
	 * @return 数据属性（事务内锁定至提交/回滚）
	 */
	@Select("SELECT * FROM ont_data_property WHERE id = #{id} AND del_flag = '0' FOR UPDATE")
	OntDataProperty selectByIdForUpdate(@Param("id") Long id);

}
