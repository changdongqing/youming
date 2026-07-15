/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.ontology.mapping.dto.FieldMappingCreateDTO;
import com.pig4cloud.pig.ontology.mapping.dto.FieldMappingUpdateDTO;
import com.pig4cloud.pig.ontology.mapping.entity.OntFieldMapping;
import com.pig4cloud.pig.ontology.mapping.vo.FieldMappingVO;

import java.util.List;

/**
 * 字段映射服务接口（18-04 §12）。
 *
 * @author youming
 */
public interface FieldMappingService extends IService<OntFieldMapping> {

	/**
	 * 列出实体映射下的字段映射。
	 */
	List<FieldMappingVO> listByEntityMapping(Long entityMappingId);

	/**
	 * 新增字段映射（仅 DRAFT 版本可操作）。
	 */
	FieldMappingVO create(Long entityMappingId, FieldMappingCreateDTO dto);

	/**
	 * 修改字段映射。
	 */
	FieldMappingVO update(FieldMappingUpdateDTO dto);

	/**
	 * 删除字段映射。
	 */
	boolean remove(Long id);

}
