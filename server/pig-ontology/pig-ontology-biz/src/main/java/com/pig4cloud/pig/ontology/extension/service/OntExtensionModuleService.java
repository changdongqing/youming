/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.extension.dto.ExtensionModuleCreateDTO;
import com.pig4cloud.pig.ontology.extension.dto.ExtensionModuleUpdateDTO;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.validator.ExtensionValidationReport;
import com.pig4cloud.pig.ontology.extension.vo.ExtensionModuleDetailVO;

/**
 * 扩展模块服务接口。
 *
 * @author youming
 */
public interface OntExtensionModuleService extends IService<OntExtensionModule> {

	/**
	 * 新建扩展模块。
	 * @param request 创建请求
	 * @return 新建结果
	 */
	R<OntExtensionModule> saveModule(ExtensionModuleCreateDTO request);

	/**
	 * 修改扩展模块。
	 * @param request 修改请求
	 * @return 修改结果
	 */
	R<OntExtensionModule> updateModule(ExtensionModuleUpdateDTO request);

	/**
	 * 删除扩展模块（逻辑删除+解除关联）。
	 * @param id 模块ID
	 * @return 删除结果
	 */
	R<Boolean> removeModule(Long id);

	/**
	 * 查询模块详情。
	 * @param id 模块ID
	 * @return 详情
	 */
	ExtensionModuleDetailVO getDetail(Long id);

	/**
	 * 执行扩展合法性校验。
	 * @param id 模块ID
	 * @return 校验报告
	 */
	ExtensionValidationReport validateModule(Long id);

	/**
	 * 导出扩展模块为指定RDF格式。
	 * @param id 模块ID
	 * @param format RDF格式
	 * @return 序列化文本
	 */
	String exportModule(Long id, String format);

	/**
	 * 获取导出文件名。
	 * @param id 模块ID
	 * @param format RDF格式
	 * @return 文件名
	 */
	String getExportFilename(Long id, String format);

}
