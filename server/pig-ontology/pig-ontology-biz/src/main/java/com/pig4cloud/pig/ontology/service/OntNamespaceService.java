/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.entity.OntNamespace;

/**
 * 命名空间服务。
 *
 * @author youming
 */
public interface OntNamespaceService extends IService<OntNamespace> {

	/**
	 * 新增命名空间。
	 * @param namespace 命名空间
	 * @return 处理结果
	 */
	R<OntNamespace> saveNamespace(OntNamespace namespace);

	/**
	 * 修改命名空间。
	 * @param namespace 命名空间
	 * @return 处理结果
	 */
	R<OntNamespace> updateNamespace(OntNamespace namespace);

	/**
	 * 删除命名空间。
	 * @param id 命名空间ID
	 * @return 处理结果
	 */
	R<Boolean> removeNamespace(Long id);

}
