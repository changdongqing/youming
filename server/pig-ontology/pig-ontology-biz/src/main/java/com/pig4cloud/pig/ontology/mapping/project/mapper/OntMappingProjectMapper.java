/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 映射工程 Mapper（18-03 §3）。
 *
 * @author youming
 */
@Mapper
public interface OntMappingProjectMapper extends BaseMapper<OntMappingProject> {

	/**
	 * 行级悲观锁，用于发布事务中锁定工程行。
	 * @param id 工程ID
	 * @return 工程实体（在同一事务内持有行锁）
	 */
	@Select("SELECT * FROM ont_mapping_project WHERE id = #{id} AND del_flag = '0' FOR UPDATE")
	OntMappingProject selectForUpdate(@Param("id") Long id);

	/**
	 * 乐观锁更新工程修订号。
	 * @param id 工程ID
	 * @param expectedRevision 预期修订号
	 * @return 受影响行数（1成功，0冲突）
	 */
	@Update("UPDATE ont_mapping_project SET revision = revision + 1, update_time = now() "
			+ "WHERE id = #{id} AND revision = #{expectedRevision} AND del_flag = '0'")
	int incrementRevision(@Param("id") Long id, @Param("expectedRevision") Long expectedRevision);

}
