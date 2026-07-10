/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.vo.OntEntityTypeTreeNode;

import java.util.List;
import java.util.Map;

/**
 * 实体类型服务。
 *
 * @author youming
 */
public interface OntEntityTypeService extends IService<OntEntityType> {

	/**
	 * 新增实体类型。
	 * @param entityType 实体类型
	 * @return 处理结果
	 */
	R<OntEntityType> saveEntityType(OntEntityType entityType);

	/**
	 * 修改实体类型。
	 * @param entityType 实体类型
	 * @return 处理结果
	 */
	R<OntEntityType> updateEntityType(OntEntityType entityType);

	/**
	 * 删除实体类型。
	 * @param id 实体类型ID
	 * @return 处理结果
	 */
	R<Boolean> removeEntityType(Long id);

	/**
	 * 继承树。
	 * @return 树
	 */
	List<OntEntityTypeTreeNode> tree();

	/**
	 * 实体类型详情（含标签、父类、子类、等价类、不相交类）。
	 * @param id 实体类型ID
	 * @return 详情
	 */
	Map<String, Object> getDetail(Long id);

}
