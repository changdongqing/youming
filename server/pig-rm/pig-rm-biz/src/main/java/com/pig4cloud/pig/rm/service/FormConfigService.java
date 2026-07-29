/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.rm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.rm.api.entity.FormFieldConfig;
import com.pig4cloud.pig.rm.mapper.FormFieldConfigMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 表单字段配置服务
 *
 * @author youming
 * @date 2026-07-29
 */
@Service
@AllArgsConstructor
public class FormConfigService {

	private final FormFieldConfigMapper formFieldConfigMapper;

	/**
	 * 查询表单字段配置
	 */
	public R<List<FormFieldConfig>> getByFormCode(String formCode) {
		List<FormFieldConfig> configs = formFieldConfigMapper
			.selectList(Wrappers.<FormFieldConfig>lambdaQuery().eq(FormFieldConfig::getFormCode, formCode)
				.orderByAsc(FormFieldConfig::getSortOrder));
		return R.ok(configs);
	}

	/**
	 * 保存表单字段配置（先删后插）
	 */
	@Transactional(rollbackFor = Exception.class)
	public R saveConfigs(List<FormFieldConfig> configs) {
		if (configs == null || configs.isEmpty()) {
			return R.failed("配置不能为空");
		}
		String formCode = configs.get(0).getFormCode();
		// 先删除旧配置
		formFieldConfigMapper.delete(Wrappers.<FormFieldConfig>lambdaQuery().eq(FormFieldConfig::getFormCode, formCode));
		// 批量插入
		for (FormFieldConfig config : configs) {
			formFieldConfigMapper.insert(config);
		}
		return R.ok();
	}

}
