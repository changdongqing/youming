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
import com.pig4cloud.pig.ontology.api.vo.ClassTemplateNodeVO;
import com.pig4cloud.pig.ontology.api.vo.InheritedViewVO;
import com.pig4cloud.pig.ontology.api.vo.PropertyTemplateSupplyVO;
import com.pig4cloud.pig.ontology.api.vo.UnitConvertResultVO;
import com.pig4cloud.pig.ontology.api.vo.UnitSupplyVO;
import com.pig4cloud.pig.ontology.service.ClassTemplateService;
import com.pig4cloud.pig.ontology.service.PropertyTemplateService;
import com.pig4cloud.pig.ontology.service.UnitConversionService;
import com.pig4cloud.pig.ontology.service.UnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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

	private final ClassTemplateService classTemplateService;

	private final UnitService unitService;

	private final UnitConversionService unitConversionService;

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

	@GetMapping("/class-template/tree")
	@Operation(summary = "分类模板树供给", description = "含编码/继承预览/外观（10.5）")
	@HasPermission("ont_supply_view")
	public R<List<ClassTemplateNodeVO>> supplyClassTemplateTree(
			@RequestParam String treeRoot,
			@RequestParam(defaultValue = "false") Boolean includeDeprecated) {
		return R.ok(classTemplateService.tree(treeRoot, includeDeprecated));
	}

	@GetMapping("/class-template/{code}/inherited")
	@Operation(summary = "分类模板继承视图供给", description = "合并父链属性+外观，供建模侧套用（10.5，AC-2.3）")
	@HasPermission("ont_supply_view")
	public R<InheritedViewVO> supplyInherited(@PathVariable String code) {
		com.pig4cloud.pig.ontology.api.entity.ClassTemplate tpl = classTemplateService.getByCode(code);
		if (tpl == null) {
			return R.failed("分类模板不存在: " + code);
		}
		return R.ok(classTemplateService.inheritedView(tpl.getId()));
	}

	@GetMapping("/class-hierarchy/suggest")
	@Operation(summary = "类层级建议供给", description = "据模板父链推荐 subClassOf 父类（FR-9，10.5，AC-9.1）")
	@HasPermission("ont_supply_view")
	public R<List<String>> suggestClassHierarchy(@RequestParam String templateCode) {
		return R.ok(classTemplateService.suggestParentClassIris(templateCode));
	}

	@GetMapping("/units")
	@Operation(summary = "单位供给", description = "按量纲分组返回，含换算系数（10.5，AC-5.4）")
	@HasPermission("ont_supply_view")
	public R<List<UnitSupplyVO>> supplyUnits(
			@RequestParam(required = false) String quantityKindIri,
			@RequestParam(defaultValue = "false") Boolean includeDeprecated) {
		return R.ok(unitService.supplyList(quantityKindIri, includeDeprecated));
	}

	@GetMapping("/units/convert")
	@Operation(summary = "换算供给", description = "同量纲换算，跨量纲返 null（10.5，AC-3.3）")
	@HasPermission("ont_supply_view")
	public R<UnitConvertResultVO> supplyConvert(@RequestParam BigDecimal value, @RequestParam String fromIri,
			@RequestParam String toIri) {
		return R.ok(unitConversionService.convert(value, fromIri, toIri));
	}

}
