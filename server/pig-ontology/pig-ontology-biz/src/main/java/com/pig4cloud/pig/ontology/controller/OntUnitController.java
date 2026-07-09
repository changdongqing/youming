/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.service.OntUnitService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 单位字典管理。
 *
 * @author youming
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ontology/units")
@Tag(description = "ontology-unit", name = "单位字典管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntUnitController {

	private final OntUnitService ontUnitService;

	/**
	 * 分页查询单位列表。
	 * @param page 分页参数
	 * @param query 查询条件
	 * @return 单位分页列表
	 */
	@GetMapping
	@HasPermission("ontology_unit_view")
	public R<IPage<OntUnit>> page(@ParameterObject Page<OntUnit> page, @ParameterObject OntUnit query) {
		return R.ok(ontUnitService.page(page, Wrappers.<OntUnit>lambdaQuery()
			.eq(query.getCategoryId() != null, OntUnit::getCategoryId, query.getCategoryId())
			.like(StrUtil.isNotBlank(query.getUnitCode()), OntUnit::getUnitCode, query.getUnitCode())
			.like(StrUtil.isNotBlank(query.getUnitSymbol()), OntUnit::getUnitSymbol, query.getUnitSymbol())
			.like(StrUtil.isNotBlank(query.getUnitName()), OntUnit::getUnitName, query.getUnitName())
			.eq(StrUtil.isNotBlank(query.getIsBuiltin()), OntUnit::getIsBuiltin, query.getIsBuiltin())
			.orderByAsc(OntUnit::getSortOrder)
			.orderByAsc(OntUnit::getId)));
	}

	/**
	 * 查询单位列表。
	 * @param query 查询条件
	 * @return 单位列表
	 */
	@GetMapping("/list")
	@HasPermission("ontology_unit_view")
	public R list(@ParameterObject OntUnit query) {
		return R.ok(ontUnitService.list(Wrappers.<OntUnit>lambdaQuery()
			.eq(query.getCategoryId() != null, OntUnit::getCategoryId, query.getCategoryId())
			.like(StrUtil.isNotBlank(query.getUnitCode()), OntUnit::getUnitCode, query.getUnitCode())
			.like(StrUtil.isNotBlank(query.getUnitSymbol()), OntUnit::getUnitSymbol, query.getUnitSymbol())
			.like(StrUtil.isNotBlank(query.getUnitName()), OntUnit::getUnitName, query.getUnitName())
			.eq(StrUtil.isNotBlank(query.getIsBuiltin()), OntUnit::getIsBuiltin, query.getIsBuiltin())
			.orderByAsc(OntUnit::getSortOrder)
			.orderByAsc(OntUnit::getId)));
	}

	/**
	 * 查询单位详情。
	 * @param id 单位ID
	 * @return 单位详情
	 */
	@GetMapping("/{id}")
	@HasPermission("ontology_unit_view")
	public R getById(@PathVariable Long id) {
		return R.ok(ontUnitService.getById(id));
	}

	/**
	 * 查询单位树。
	 * @return 单位分类和单位树
	 */
	@GetMapping("/tree")
	@HasPermission("ontology_unit_view")
	public R tree() {
		return R.ok(ontUnitService.tree());
	}

	/**
	 * 按单位符号查询默认单位。
	 * @param symbol 单位符号
	 * @return 单位
	 */
	@GetMapping("/by-symbol/{symbol}")
	@HasPermission("ontology_unit_view")
	public R getBySymbol(@PathVariable String symbol) {
		OntUnit unit = ontUnitService.getBySymbol(symbol);
		if (unit == null) {
			return R.failed("未找到符号对应的单位: " + symbol);
		}
		return R.ok(unit);
	}

	/**
	 * 新增单位。
	 * @param unit 单位
	 * @return 新增结果
	 */
	@SysLog("新增单位")
	@PostMapping
	@HasPermission("ontology_unit_add")
	public R save(@Valid @RequestBody OntUnit unit) {
		return ontUnitService.saveUnit(unit);
	}

	/**
	 * 修改单位。
	 * @param unit 单位
	 * @return 修改结果
	 */
	@SysLog("修改单位")
	@PutMapping
	@HasPermission("ontology_unit_edit")
	public R update(@Valid @RequestBody OntUnit unit) {
		return ontUnitService.updateUnit(unit);
	}

	/**
	 * 删除单位。
	 * @param id 单位ID
	 * @return 删除结果
	 */
	@SysLog("删除单位")
	@DeleteMapping("/{id}")
	@HasPermission("ontology_unit_del")
	public R remove(@PathVariable Long id) {
		return ontUnitService.removeUnit(id);
	}

	/**
	 * 单位换算。
	 * @param from 来源单位ID
	 * @param to 目标单位ID
	 * @param value 数值
	 * @return 换算结果
	 */
	@GetMapping("/convert")
	@HasPermission("ontology_unit_view")
	public R<BigDecimal> convert(@RequestParam Long from, @RequestParam Long to, @RequestParam BigDecimal value) {
		return ontUnitService.convert(from, to, value);
	}

}
