/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.ontology.mapping.dto.EntityMappingCreateDTO;
import com.pig4cloud.pig.ontology.mapping.dto.EntityMappingUpdateDTO;
import com.pig4cloud.pig.ontology.mapping.dto.IriPreviewRequestDTO;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.vo.EntityMappingVO;
import com.pig4cloud.pig.ontology.mapping.vo.IriPreviewResultVO;

/**
 * 实体映射服务接口（18-04 §12）。
 *
 * @author youming
 */
public interface EntityMappingService extends IService<OntEntityMapping> {

	/**
	 * 分页查询版本下的实体映射。
	 */
	Page<EntityMappingVO> listByVersion(Long versionId, Page<OntEntityMapping> page, String mappingCode,
			String mappingName, Boolean enabledOnly);

	/**
	 * 获取实体映射详情（含字段映射列表）。
	 */
	EntityMappingVO getDetail(Long id);

	/**
	 * 新增实体映射（仅 DRAFT 版本可操作）。
	 */
	EntityMappingVO create(Long versionId, EntityMappingCreateDTO dto);

	/**
	 * 修改 DRAFT 实体映射（乐观锁）。
	 */
	EntityMappingVO update(EntityMappingUpdateDTO dto);

	/**
	 * 删除 DRAFT 实体映射（级联删除字段映射）。
	 */
	boolean remove(Long id);

	/**
	 * IRI 样例预览（不读源库）。
	 */
	IriPreviewResultVO iriPreview(Long id, IriPreviewRequestDTO request);

}
