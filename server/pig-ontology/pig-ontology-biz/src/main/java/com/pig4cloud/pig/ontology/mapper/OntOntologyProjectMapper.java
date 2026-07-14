/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 本体工程 Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntOntologyProjectMapper extends BaseMapper<OntOntologyProject> {

	/**
	 * 获取版本发布事务级 advisory lock，防止并发发布。
	 * <p>
	 * 事务结束后自动释放，无需手动解锁。
	 * @param ontologyId 本体工程ID
	 */
	@Select("SELECT pg_advisory_xact_lock(hashtext('ont_version_' || #{ontologyId}::text))")
	void acquireVersionLock(@Param("ontologyId") Long ontologyId);

	/**
	 * 乐观锁递增 workspace_revision。
	 * <p>
	 * 返回影响行数：1 表示成功，0 表示 revision 已变化（并发冲突）。
	 * @param ontologyId 本体工程ID
	 * @param expectedRevision 期望的当前 revision 值
	 * @return 影响行数
	 */
	@Update("UPDATE ont_ontology_project SET workspace_revision = workspace_revision + 1, update_time = now() "
			+ "WHERE id = #{ontologyId} AND workspace_revision = #{expectedRevision} AND del_flag = '0'")
	int incrementWorkspaceRevision(@Param("ontologyId") Long ontologyId, @Param("expectedRevision") Long expectedRevision);

}
