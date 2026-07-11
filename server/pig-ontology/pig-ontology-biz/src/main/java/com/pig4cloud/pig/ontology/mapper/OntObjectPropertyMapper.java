/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 对象属性 Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntObjectPropertyMapper extends BaseMapper<OntObjectProperty> {

	/**
	 * 按主键加行锁查询（SELECT ... FOR UPDATE），用于功能性断言并发校验前锁定对象属性行。
	 * @param id 对象属性ID
	 * @return 对象属性（事务内锁定至提交/回滚）
	 */
	@Select("SELECT * FROM ont_object_property WHERE id = #{id} AND del_flag = '0' FOR UPDATE")
	OntObjectProperty selectByIdForUpdate(@Param("id") Long id);

}
