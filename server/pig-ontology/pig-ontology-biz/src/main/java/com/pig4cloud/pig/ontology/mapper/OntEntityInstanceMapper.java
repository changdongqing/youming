/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 实体对象实例Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntEntityInstanceMapper extends BaseMapper<OntEntityInstance> {

	/**
	 * 按主键加行锁查询（SELECT ... FOR UPDATE），用于关系/唯一性并发校验前锁定实例行。
	 * @param id 实例ID
	 * @return 实例（事务内锁定至提交/回滚）
	 */
	@Select("SELECT * FROM ont_entity_instance WHERE id = #{id} AND del_flag = '0' FOR UPDATE")
	OntEntityInstance selectByIdForUpdate(@Param("id") Long id);

}
