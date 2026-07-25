/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.dto.PropertyTemplatePromoteDTO;
import com.pig4cloud.pig.ontology.api.entity.PropertyTemplate;

/**
 * 属性模板 Service
 *
 * @author pig
 * @date 2026-07-25
 */
public interface PropertyTemplateService extends IService<PropertyTemplate> {

	/**
	 * 分页查询属性模板
	 * @param page 分页对象
	 * @param template 查询条件
	 * @return 分页结果
	 */
	IPage<PropertyTemplate> page(Page page, PropertyTemplate template);

	/**
	 * 新增属性模板（查重 + 设 custom）
	 * @param template 模板信息
	 * @return 操作结果
	 */
	R saveTemplate(PropertyTemplate template);

	/**
	 * 编辑属性模板（builtin 拒绝、templateCode 不可改）
	 * @param template 模板信息
	 * @return 操作结果
	 */
	R updateTemplate(PropertyTemplate template);

	/**
	 * 删除属性模板（builtin 拒绝、软删）
	 * @param id 模板 ID
	 * @return 操作结果
	 */
	R removeTemplate(Long id);

	/**
	 * 弃用/恢复属性模板
	 * @param id 模板 ID
	 * @param deprecated '1'=弃用 '0'=恢复
	 * @return 操作结果
	 */
	R deprecate(Long id, String deprecated);

	/**
	 * 提升属性为模板（FR-6）
	 * @param dto 提升请求
	 * @return 操作结果
	 */
	R promote(PropertyTemplatePromoteDTO dto);

}
