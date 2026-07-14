/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 映射版本 Mapper（18-03 §4）。
 *
 * @author youming
 */
@Mapper
public interface OntMappingVersionMapper extends BaseMapper<OntMappingVersion> {

	/**
	 * 行级悲观锁，用于发布事务中锁定版本行。
	 * @param id 版本ID
	 * @return 版本实体（在同一事务内持有行锁）
	 */
	@Select("SELECT * FROM ont_mapping_version WHERE id = #{id} AND del_flag = '0' FOR UPDATE")
	OntMappingVersion selectForUpdate(@Param("id") Long id);

	/**
	 * CAS 状态转换：从期望状态转换到目标状态，同时递增 revision。
	 * <p>
	 * 用于 DRAFT→VALIDATING、VALIDATED→PUBLISHED、VALIDATED→DRAFT(reopen) 等状态机转换。
	 * @param id 版本ID
	 * @param expectedStatus 预期状态
	 * @param targetStatus 目标状态
	 * @param expectedRevision 预期修订号
	 * @return 受影响行数（1成功，0状态或revision不匹配）
	 */
	@Update("UPDATE ont_mapping_version SET version_status = #{targetStatus}, "
			+ "revision = revision + 1, update_time = now() "
			+ "WHERE id = #{id} AND version_status = #{expectedStatus} "
			+ "AND revision = #{expectedRevision} AND del_flag = '0'")
	int casUpdateStatus(@Param("id") Long id, @Param("expectedStatus") String expectedStatus,
			@Param("targetStatus") String targetStatus, @Param("expectedRevision") Long expectedRevision);

	/**
	 * 发布版本：设置 PUBLISHED 状态、快照、哈希、发布人和时间。
	 * @param id 版本ID
	 * @param expectedStatus 预期状态（VALIDATED）
	 * @param expectedRevision 预期修订号
	 * @param configSnapshot 配置快照JSON
	 * @param configHash 配置哈希
	 * @param publishedBy 发布人
	 * @param publishedAt 发布时间
	 * @return 受影响行数
	 */
	@Update("UPDATE ont_mapping_version SET version_status = 'PUBLISHED', "
			+ "config_snapshot = CAST(#{configSnapshot} AS jsonb), "
			+ "config_hash = #{configHash}, published_by = #{publishedBy}, "
			+ "published_at = #{publishedAt}, revision = revision + 1, update_time = now() "
			+ "WHERE id = #{id} AND version_status = #{expectedStatus} "
			+ "AND revision = #{expectedRevision} AND del_flag = '0'")
	int publishVersion(@Param("id") Long id, @Param("expectedStatus") String expectedStatus,
			@Param("expectedRevision") Long expectedRevision, @Param("configSnapshot") String configSnapshot,
			@Param("configHash") String configHash, @Param("publishedBy") String publishedBy,
			@Param("publishedAt") LocalDateTime publishedAt);

	/**
	 * 停用版本：设置 RETIRED 状态和停用时间。
	 * @param id 版本ID
	 * @param retiredAt 停用时间
	 * @return 受影响行数
	 */
	@Update("UPDATE ont_mapping_version SET version_status = 'RETIRED', retired_at = #{retiredAt}, "
			+ "revision = revision + 1, update_time = now() "
			+ "WHERE id = #{id} AND version_status = 'PUBLISHED' AND del_flag = '0'")
	int retireVersion(@Param("id") Long id, @Param("retiredAt") LocalDateTime retiredAt);

}
