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
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.service.OntEntityTypeService;
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

import java.util.List;
import java.util.Map;

/**
 * 实体类型管理。
 *
 * @author youming
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ontology/entity-types")
@Tag(description = "ontology-entity-type", name = "实体类型管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntEntityTypeController {

	private final OntEntityTypeService ontEntityTypeService;

	/**
	 * 分页查询实体类型列表。
	 * @param page 分页参数
	 * @param query 查询条件
	 * @return 实体类型分页列表
	 */
	@GetMapping
	@HasPermission("ontology_entity_type_view")
	public R<IPage<OntEntityType>> page(@ParameterObject Page<OntEntityType> page, @ParameterObject OntEntityType query) {
		return R.ok(ontEntityTypeService.page(page, Wrappers.<OntEntityType>lambdaQuery()
			.like(StrUtil.isNotBlank(query.getName()), OntEntityType::getName, query.getName())
			.eq(query.getNamespaceId() != null, OntEntityType::getNamespaceId, query.getNamespaceId())
			.eq(StrUtil.isNotBlank(query.getIsBuiltin()), OntEntityType::getIsBuiltin, query.getIsBuiltin())
			.orderByAsc(OntEntityType::getSortOrder)
			.orderByAsc(OntEntityType::getId)));
	}

	/**
	 * 查询实体类型列表（不分页，供下拉引用）。
	 * @param query 查询条件
	 * @return 实体类型列表
	 */
	@GetMapping("/list")
	@HasPermission("ontology_entity_type_view")
	public R list(@ParameterObject OntEntityType query) {
		return R.ok(ontEntityTypeService.list(Wrappers.<OntEntityType>lambdaQuery()
			.like(StrUtil.isNotBlank(query.getName()), OntEntityType::getName, query.getName())
			.eq(query.getNamespaceId() != null, OntEntityType::getNamespaceId, query.getNamespaceId())
			.eq(StrUtil.isNotBlank(query.getIsBuiltin()), OntEntityType::getIsBuiltin, query.getIsBuiltin())
			.orderByAsc(OntEntityType::getSortOrder)
			.orderByAsc(OntEntityType::getId)));
	}

	/**
	 * 查询实体类型继承树。
	 * @return 继承树
	 */
	@GetMapping("/tree")
	@HasPermission("ontology_entity_type_view")
	public R tree() {
		return R.ok(ontEntityTypeService.tree());
	}

	/**
	 * 查询实体类型详情。
	 * @param id 实体类型ID
	 * @return 详情（含标签/父类/子类/等价类/不相交类）
	 */
	@GetMapping("/{id}")
	@HasPermission("ontology_entity_type_view")
	public R getById(@PathVariable Long id) {
		Map<String, Object> detail = ontEntityTypeService.getDetail(id);
		if (detail == null) {
			return R.failed("实体类型不存在");
		}
		return R.ok(detail);
	}

	/**
	 * 新增实体类型。
	 * @param entityType 实体类型
	 * @return 新增结果
	 */
	@SysLog("新增实体类型")
	@PostMapping
	@HasPermission("ontology_entity_type_add")
	public R save(@Valid @RequestBody OntEntityType entityType) {
		return ontEntityTypeService.saveEntityType(entityType);
	}

	/**
	 * 修改实体类型。
	 * @param entityType 实体类型
	 * @return 修改结果
	 */
	@SysLog("修改实体类型")
	@PutMapping
	@HasPermission("ontology_entity_type_edit")
	public R update(@Valid @RequestBody OntEntityType entityType) {
		return ontEntityTypeService.updateEntityType(entityType);
	}

	/**
	 * 删除实体类型。
	 * @param id 实体类型ID
	 * @return 删除结果
	 */
	@SysLog("删除实体类型")
	@DeleteMapping("/{id}")
	@HasPermission("ontology_entity_type_del")
	public R remove(@PathVariable Long id) {
		return ontEntityTypeService.removeEntityType(id);
	}

	/**
	 * 查询实体类型的数据属性（首期返回空，数据属性模块上线后实现）。
	 * @param id 实体类型ID
	 * @return 数据属性列表
	 */
	@GetMapping("/{id}/properties")
	@HasPermission("ontology_entity_type_view")
	public R properties(@PathVariable Long id) {
		return R.ok(List.of());
	}

}
