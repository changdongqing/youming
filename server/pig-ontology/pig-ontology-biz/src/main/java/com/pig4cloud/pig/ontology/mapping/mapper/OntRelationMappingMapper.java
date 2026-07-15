/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.mapping.entity.OntRelationMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 关系映射 Mapper（18-05 §3）。
 *
 * @author youming
 */
@Mapper
public interface OntRelationMappingMapper extends BaseMapper<OntRelationMapping> {

	/**
	 * 行级悲观锁。
	 * @param id 关系映射ID
	 * @return 关系映射实体（在同一事务内持有行锁）
	 */
	@Select("SELECT * FROM ont_relation_mapping WHERE id = #{id} AND del_flag = '0' FOR UPDATE")
	OntRelationMapping selectForUpdate(@Param("id") Long id);

	/**
	 * 乐观锁更新关系映射修订号。
	 * @param id 关系映射ID
	 * @param expectedRevision 预期修订号
	 * @return 受影响行数（1成功，0冲突）
	 */
	@Update("UPDATE ont_relation_mapping SET revision = revision + 1, update_time = now() "
			+ "WHERE id = #{id} AND revision = #{expectedRevision} AND del_flag = '0'")
	int incrementRevision(@Param("id") Long id, @Param("expectedRevision") Long expectedRevision);

}
