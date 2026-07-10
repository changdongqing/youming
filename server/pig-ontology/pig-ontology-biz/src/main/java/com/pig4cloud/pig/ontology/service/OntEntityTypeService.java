/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.dto.OntEntityTypeCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntEntityTypeUpdateDTO;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.vo.OntEntityTypeDetailVO;
import com.pig4cloud.pig.ontology.vo.OntEntityTypeTreeNode;

import java.util.List;

/**
 * 实体类型服务。
 *
 * @author youming
 */
public interface OntEntityTypeService extends IService<OntEntityType> {

	/**
	 * 核心本体工程ID。
	 */
	long CORE_ONTOLOGY_ID = 935001L;

	/**
	 * 新增实体类型。
	 * @param request 新增请求
	 * @return 处理结果
	 */
	R<OntEntityType> saveEntityType(OntEntityTypeCreateDTO request);

	/**
	 * 修改实体类型。
	 * @param request 修改请求
	 * @return 处理结果
	 */
	R<OntEntityType> updateEntityType(OntEntityTypeUpdateDTO request);

	/**
	 * 删除实体类型。
	 * @param id 实体类型ID
	 * @return 处理结果
	 */
	R<Boolean> removeEntityType(Long id);

	/**
	 * 继承树。
	 * @param ontologyId 本体工程ID
	 * @return 树
	 */
	List<OntEntityTypeTreeNode> tree(Long ontologyId);

	/**
	 * 实体类型详情。
	 * @param id 实体类型ID
	 * @return 详情
	 */
	OntEntityTypeDetailVO getDetail(Long id);

}
