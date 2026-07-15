/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.ontology.mapping.dto.RelationKeyPreviewRequestDTO;
import com.pig4cloud.pig.ontology.mapping.dto.RelationMappingCreateDTO;
import com.pig4cloud.pig.ontology.mapping.dto.RelationMappingUpdateDTO;
import com.pig4cloud.pig.ontology.mapping.entity.OntRelationMapping;
import com.pig4cloud.pig.ontology.mapping.vo.RelationKeyPreviewResultVO;
import com.pig4cloud.pig.ontology.mapping.vo.RelationMappingVO;

/**
 * 关系映射服务接口（18-05 §13）。
 *
 * @author youming
 */
public interface RelationMappingService extends IService<OntRelationMapping> {

	/**
	 * 分页查询版本下的关系映射。
	 */
	Page<RelationMappingVO> listByVersion(Long versionId, Page<OntRelationMapping> page, String mappingCode,
			String mappingName, Boolean enabledOnly);

	/**
	 * 获取关系映射详情。
	 */
	RelationMappingVO getDetail(Long id);

	/**
	 * 新增关系映射（仅 DRAFT 版本可操作）。
	 */
	RelationMappingVO create(Long versionId, RelationMappingCreateDTO dto);

	/**
	 * 修改 DRAFT 关系映射（乐观锁）。
	 */
	RelationMappingVO update(RelationMappingUpdateDTO dto);

	/**
	 * 删除 DRAFT 关系映射。
	 */
	boolean remove(Long id);

	/**
	 * 样例键解析预览（不读源库）。
	 */
	RelationKeyPreviewResultVO keyPreview(Long id, RelationKeyPreviewRequestDTO request);

}
