/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 实体映射 Mapper（18-04 §3）。
 *
 * @author youming
 */
@Mapper
public interface OntEntityMappingMapper extends BaseMapper<OntEntityMapping> {

	/**
	 * 行级悲观锁，用于发布事务中锁定实体映射行。
	 * @param id 实体映射ID
	 * @return 实体映射实体（在同一事务内持有行锁）
	 */
	@Select("SELECT * FROM ont_entity_mapping WHERE id = #{id} AND del_flag = '0' FOR UPDATE")
	OntEntityMapping selectForUpdate(@Param("id") Long id);

	/**
	 * 乐观锁更新实体映射修订号。
	 * @param id 实体映射ID
	 * @param expectedRevision 预期修订号
	 * @return 受影响行数（1成功，0冲突）
	 */
	@Update("UPDATE ont_entity_mapping SET revision = revision + 1, update_time = now() "
			+ "WHERE id = #{id} AND revision = #{expectedRevision} AND del_flag = '0'")
	int incrementRevision(@Param("id") Long id, @Param("expectedRevision") Long expectedRevision);

}
