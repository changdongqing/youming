/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.entity.OntUnitCategory;

/**
 * 单位分类服务。
 *
 * @author youming
 */
public interface OntUnitCategoryService extends IService<OntUnitCategory> {

	/**
	 * 新增单位分类。
	 * @param category 分类
	 * @return 处理结果
	 */
	R<OntUnitCategory> saveCategory(OntUnitCategory category);

	/**
	 * 修改单位分类。
	 * @param category 分类
	 * @return 处理结果
	 */
	R<OntUnitCategory> updateCategory(OntUnitCategory category);

	/**
	 * 删除单位分类。
	 * @param id 分类ID
	 * @return 处理结果
	 */
	R<Boolean> removeCategory(Long id);

}
