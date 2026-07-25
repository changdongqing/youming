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

package com.pig4cloud.pig.ontology.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.dto.PropertyTemplatePromoteDTO;
import com.pig4cloud.pig.ontology.api.entity.PropertyTemplate;
import com.pig4cloud.pig.ontology.mapper.PropertyTemplateMapper;
import com.pig4cloud.pig.ontology.service.PropertyTemplateService;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 属性模板 Service 实现
 *
 * @author pig
 * @date 2026-07-25
 */
@AllArgsConstructor
@Service
public class PropertyTemplateServiceImpl extends ServiceImpl<PropertyTemplateMapper, PropertyTemplate>
		implements PropertyTemplateService {

	@Override
	public IPage<PropertyTemplate> page(Page page, PropertyTemplate template) {
		return baseMapper.selectPage(page,
				Wrappers.<PropertyTemplate>lambdaQuery()
					.eq(StrUtil.isNotBlank(template.getKind()), PropertyTemplate::getKind, template.getKind())
					.eq(StrUtil.isNotBlank(template.getCategory()), PropertyTemplate::getCategory,
							template.getCategory())
					.and(StrUtil.isNotBlank(template.getTemplateCode()),
							w -> w.like(PropertyTemplate::getTemplateCode, template.getTemplateCode())
								.or()
								.like(PropertyTemplate::getLabel, template.getTemplateCode()))
					.eq(StrUtil.isNotBlank(template.getDeprecated()), PropertyTemplate::getDeprecated,
							template.getDeprecated())
					.orderByDesc(PropertyTemplate::getCreateTime));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveTemplate(PropertyTemplate template) {
		// kind 与专属字段一致性校验（datatype 必填 type；object 必填 defaultCardinality）
		R consistency = validateKindConsistency(template);
		if (consistency != null) {
			return consistency;
		}
		// 预查重：templateCode 唯一（命中有效记录时快速返回友好提示）
		long count = count(Wrappers.<PropertyTemplate>lambdaQuery()
			.eq(PropertyTemplate::getTemplateCode, template.getTemplateCode()));
		if (count > 0) {
			return R.failed("模板标识 '" + template.getTemplateCode() + "' 已存在");
		}
		template.setSource("custom");
		template.setDeprecated("0");
		try {
			// DB 唯一约束 uk_ont_prop_tpl_code 不受逻辑删除过滤，是权威兜底：
			// 若 templateCode 曾被软删，预查重查不到，由约束抛 DuplicateKeyException，转友好提示
			return R.ok(save(template));
		}
		catch (DuplicateKeyException e) {
			return R.failed("模板标识 '" + template.getTemplateCode() + "' 已存在");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateTemplate(PropertyTemplate template) {
		PropertyTemplate existing = getById(template.getId());
		if (existing == null) {
			return R.failed("模板不存在");
		}
		if ("builtin".equals(existing.getSource())) {
			return R.failed("内置模板不可编辑");
		}
		// kind 与专属字段一致性校验
		template.setTemplateCode(existing.getTemplateCode());
		R consistency = validateKindConsistency(template);
		if (consistency != null) {
			return consistency;
		}
		return R.ok(updateById(template));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeTemplate(Long id) {
		PropertyTemplate existing = getById(id);
		if (existing == null) {
			return R.failed("模板不存在");
		}
		if ("builtin".equals(existing.getSource())) {
			return R.failed("内置模板不可删除");
		}
		// TODO: 后期接入建模侧引用计数，有引用时拒绝删除
		return R.ok(removeById(id));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R deprecate(Long id, String deprecated) {
		return R.ok(update(Wrappers.<PropertyTemplate>lambdaUpdate()
			.eq(PropertyTemplate::getId, id)
			.set(PropertyTemplate::getDeprecated, deprecated)));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R promote(PropertyTemplatePromoteDTO dto) {
		PropertyTemplate template = new PropertyTemplate();
		template.setTemplateCode(dto.getTemplateCode());
		template.setKind(dto.getKind());
		template.setLabel(dto.getLabel());
		template.setDescription(dto.getDescription());
		template.setCategory(dto.getCategory());
		template.setType(dto.getType());
		template.setIsIdentifier(dto.getIsIdentifier());
		template.setUnitRef(dto.getUnitRef());
		template.setEnumValues(dto.getEnumValues());
		template.setDefaultCardinality(dto.getDefaultCardinality());
		template.setSource("custom");
		template.setDeprecated("0");
		// kind 与专属字段一致性校验（与 saveTemplate 同口径）
		R consistency = validateKindConsistency(template);
		if (consistency != null) {
			return consistency;
		}
		// 预查重 + DB 唯一约束兜底（覆盖软删后复用 templateCode 的场景）
		long count = count(Wrappers.<PropertyTemplate>lambdaQuery()
			.eq(PropertyTemplate::getTemplateCode, dto.getTemplateCode()));
		if (count > 0) {
			return R.failed("模板标识 '" + dto.getTemplateCode() + "' 已存在，请勿重复提升");
		}
		try {
			save(template);
		}
		catch (DuplicateKeyException e) {
			return R.failed("模板标识 '" + dto.getTemplateCode() + "' 已存在，请勿重复提升");
		}
		return R.ok(template);
	}

	/**
	 * kind 与专属字段一致性校验（DD2 §6.1 兜底）。
	 * @param template 待校验模板
	 * @return 不合法时返回 R.failed；合法返回 null
	 */
	private R validateKindConsistency(PropertyTemplate template) {
		if ("datatype".equals(template.getKind())) {
			if (StrUtil.isBlank(template.getType())) {
				return R.failed("数据属性模板必须填写数据类型(type)");
			}
		}
		else if ("object".equals(template.getKind())) {
			if (StrUtil.isBlank(template.getDefaultCardinality())) {
				return R.failed("对象属性模板必须填写基数(defaultCardinality)");
			}
		}
		return null;
	}

}
