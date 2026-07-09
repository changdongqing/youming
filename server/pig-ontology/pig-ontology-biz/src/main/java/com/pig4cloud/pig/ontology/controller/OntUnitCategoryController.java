/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.entity.OntUnitCategory;
import com.pig4cloud.pig.ontology.service.OntUnitCategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 单位分类管理。
 *
 * @author youming
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ontology/unit-categories")
@Tag(description = "ontology-unit-category", name = "单位分类管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntUnitCategoryController {

	private final OntUnitCategoryService ontUnitCategoryService;

	/**
	 * 查询单位分类列表。
	 * @param query 查询条件
	 * @return 单位分类列表
	 */
	@GetMapping
	@HasPermission("ontology_unit_view")
	public R list(@ParameterObject OntUnitCategory query) {
		return R.ok(ontUnitCategoryService.list(Wrappers.<OntUnitCategory>lambdaQuery()
			.like(StrUtil.isNotBlank(query.getCategoryCode()), OntUnitCategory::getCategoryCode, query.getCategoryCode())
			.like(StrUtil.isNotBlank(query.getCategoryName()), OntUnitCategory::getCategoryName, query.getCategoryName())
			.orderByAsc(OntUnitCategory::getSortOrder)
			.orderByAsc(OntUnitCategory::getId)));
	}

	/**
	 * 查询单位分类详情。
	 * @param id 分类ID
	 * @return 单位分类
	 */
	@GetMapping("/{id}")
	@HasPermission("ontology_unit_view")
	public R getById(@PathVariable Long id) {
		return R.ok(ontUnitCategoryService.getById(id));
	}

	/**
	 * 新增单位分类。
	 * @param category 单位分类
	 * @return 新增结果
	 */
	@SysLog("新增单位分类")
	@PostMapping
	@HasPermission("ontology_unit_add")
	public R save(@Valid @RequestBody OntUnitCategory category) {
		return ontUnitCategoryService.saveCategory(category);
	}

	/**
	 * 修改单位分类。
	 * @param category 单位分类
	 * @return 修改结果
	 */
	@SysLog("修改单位分类")
	@PutMapping
	@HasPermission("ontology_unit_edit")
	public R update(@Valid @RequestBody OntUnitCategory category) {
		return ontUnitCategoryService.updateCategory(category);
	}

	/**
	 * 删除单位分类。
	 * @param id 分类ID
	 * @return 删除结果
	 */
	@SysLog("删除单位分类")
	@DeleteMapping("/{id}")
	@HasPermission("ontology_unit_del")
	public R remove(@PathVariable Long id) {
		return ontUnitCategoryService.removeCategory(id);
	}

}
