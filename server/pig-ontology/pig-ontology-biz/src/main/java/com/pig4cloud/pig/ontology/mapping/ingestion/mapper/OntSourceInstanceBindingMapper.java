/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.mapping.ingestion.entity.OntSourceInstanceBinding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 来源实例绑定 Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntSourceInstanceBindingMapper extends BaseMapper<OntSourceInstanceBinding> {

	/**
	 * 按ID悲观锁查询。
	 * @param id 绑定ID
	 * @return 绑定记录
	 */
	@Select("SELECT * FROM ont_source_instance_binding WHERE id = #{id} AND del_flag = '0' FOR UPDATE")
	OntSourceInstanceBinding selectByIdForUpdate(@Param("id") Long id);

	/**
	 * 按来源身份键哈希悲观锁查询。
	 * @param mappingProjectId 映射工程ID
	 * @param entityMappingCode 实体映射编码
	 * @param keyHash 复合键SHA-256哈希
	 * @return 绑定记录
	 */
	@Select("SELECT * FROM ont_source_instance_binding "
		+ "WHERE mapping_project_id = #{mappingProjectId} "
		+ "AND entity_mapping_code = #{entityMappingCode} "
		+ "AND source_record_key_hash = #{keyHash} "
		+ "AND del_flag = '0' FOR UPDATE")
	OntSourceInstanceBinding selectByIdentityForUpdate(@Param("mappingProjectId") Long mappingProjectId,
													   @Param("entityMappingCode") String entityMappingCode,
													   @Param("keyHash") String keyHash);

}
