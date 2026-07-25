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

package com.pig4cloud.pig.ontology.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.entity.PropertyTemplate;
import com.pig4cloud.pig.ontology.api.vo.PropertyTemplateSupplyVO;
import com.pig4cloud.pig.ontology.service.PropertyTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 治理资产供给接口 v1
 * <p>
 * 供建模侧拉取模板/单位/注册表。路径 /ont/supply/v1/**，对外
 * /admin/ont/supply/v1/**（对齐 PRD 10.5）。后续 DD 在此 Controller 追加端点。
 * <p>
 * 注意：pig-gateway 的 PigRequestGlobalFilter 会全局重写 StripPrefix=1（剥掉首段），
 * pig-boot 的 context-path=/admin 也只剥 /admin，因此控制器必须带 /ont 前缀，否则 404。
 *
 * @author pig
 * @date 2026-07-25
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/supply/v1")
@Tag(name = "治理资产供给接口", description = "供建模侧拉取模板/单位/注册表")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class SupplyController {

	private final PropertyTemplateService propertyTemplateService;

	@GetMapping("/property-templates")
	@Operation(summary = "属性模板供给", description = "按 kind/category 拉取，默认排除弃用")
	@HasPermission("ont_supply_view")
	public R<List<PropertyTemplateSupplyVO>> propertyTemplates(
			@RequestParam(required = false) String kind,
			@RequestParam(required = false) String category,
			@RequestParam(defaultValue = "false") Boolean includeDeprecated) {
		List<PropertyTemplate> list = propertyTemplateService.list(
				Wrappers.<PropertyTemplate>lambdaQuery()
					.eq(StrUtil.isNotBlank(kind), PropertyTemplate::getKind, kind)
					.eq(StrUtil.isNotBlank(category), PropertyTemplate::getCategory, category)
					.eq(!includeDeprecated, PropertyTemplate::getDeprecated, "0")
					.orderByAsc(PropertyTemplate::getId));
		// 转为稳定化 VO，屏蔽审计/逻辑删除字段，保证建模侧契约稳定（AC-5.7）
		List<PropertyTemplateSupplyVO> voList = BeanUtil.copyToList(list, PropertyTemplateSupplyVO.class);
		return R.ok(voList);
	}

}
