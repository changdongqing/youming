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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.dto.PropertyTemplatePromoteDTO;
import com.pig4cloud.pig.ontology.api.entity.PropertyTemplate;
import com.pig4cloud.pig.ontology.service.PropertyTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 属性模板管理 Controller
 * <p>
 * 路径 /ont/property-template/**，经 context-path /admin 或网关路由后对外为
 * /admin/ont/property-template/**（对齐 PRD 10.1）。
 * <p>
 * 注意：pig-gateway 的 PigRequestGlobalFilter 会全局重写 StripPrefix=1（剥掉首段），
 * pig-boot 的 context-path=/admin 也只剥 /admin，因此前端调 /admin/ont/xxx 后，
 * 后端实际收到的是 /ont/xxx —— 控制器必须带 /ont 前缀，否则 404。
 *
 * @author pig
 * @date 2026-07-25
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/property-template")
@Tag(name = "属性模板管理", description = "属性模板 CRUD + 弃用 + 提升")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class PropertyTemplateController {

	private final PropertyTemplateService propertyTemplateService;

	@GetMapping("/page")
	@Operation(summary = "分页查询", description = "按 kind/category/keyword 过滤")
	@HasPermission("ont_prop_tpl_view")
	public R<IPage<PropertyTemplate>> page(@ParameterObject Page page, @ParameterObject PropertyTemplate template) {
		return R.ok(propertyTemplateService.page(page, template));
	}

	@GetMapping("/{id}")
	@Operation(summary = "详情")
	@HasPermission("ont_prop_tpl_view")
	public R<PropertyTemplate> getById(@PathVariable Long id) {
		return R.ok(propertyTemplateService.getById(id));
	}

	@SysLog("新增属性模板")
	@PostMapping
	@Operation(summary = "新增 custom 模板")
	@HasPermission("ont_prop_tpl_manage")
	public R save(@Valid @RequestBody PropertyTemplate template) {
		return propertyTemplateService.saveTemplate(template);
	}

	@SysLog("编辑属性模板")
	@PutMapping("/{id}")
	@Operation(summary = "编辑（builtin 拒绝）")
	@HasPermission("ont_prop_tpl_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody PropertyTemplate template) {
		template.setId(id);
		return propertyTemplateService.updateTemplate(template);
	}

	@SysLog("删除属性模板")
	@DeleteMapping("/{id}")
	@Operation(summary = "删除（custom，软删）")
	@HasPermission("ont_prop_tpl_manage")
	public R removeById(@PathVariable Long id) {
		return propertyTemplateService.removeTemplate(id);
	}

	@SysLog("弃用属性模板")
	@PutMapping("/{id}/deprecate")
	@Operation(summary = "弃用/取消弃用")
	@HasPermission("ont_prop_tpl_manage")
	public R deprecate(@PathVariable Long id, @RequestParam(defaultValue = "1") String deprecated) {
		return propertyTemplateService.deprecate(id, deprecated);
	}

	@SysLog("提升属性为模板")
	@PostMapping("/promote")
	@Operation(summary = "提升属性为模板（FR-6）")
	@HasPermission("ont_prop_tpl_manage")
	public R promote(@Valid @RequestBody PropertyTemplatePromoteDTO dto) {
		return propertyTemplateService.promote(dto);
	}

}
