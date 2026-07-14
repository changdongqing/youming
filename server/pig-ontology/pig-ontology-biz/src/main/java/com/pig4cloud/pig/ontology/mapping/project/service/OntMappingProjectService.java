/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.ontology.mapping.project.dto.MappingProjectCreateDTO;
import com.pig4cloud.pig.ontology.mapping.project.dto.MappingProjectUpdateDTO;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.project.vo.MappingProjectVO;

/**
 * 映射工程服务接口（18-03 §8）。
 *
 * @author youming
 */
public interface OntMappingProjectService extends IService<OntMappingProject> {

	/**
	 * 分页查询映射工程。
	 */
	Page<MappingProjectVO> page(Page<OntMappingProject> page, String mappingCode, String mappingName,
			String projectStatus, Long ontologyId);

	/**
	 * 获取工程详情（含 active/draft 版本摘要）。
	 */
	MappingProjectVO getDetail(Long id);

	/**
	 * 新建映射工程并创建初始 DRAFT 版本 0.1.0。
	 */
	MappingProjectVO create(MappingProjectCreateDTO dto);

	/**
	 * 修改工程治理字段（乐观锁）。
	 */
	MappingProjectVO update(MappingProjectUpdateDTO dto);

	/**
	 * 受限逻辑删除工程（仅未发布、无作业、无绑定的 DRAFT 工程）。
	 */
	boolean remove(Long id);

	/**
	 * 更新工程状态（启用/停用/归档）。
	 */
	MappingProjectVO updateStatus(Long id, String status);

}
