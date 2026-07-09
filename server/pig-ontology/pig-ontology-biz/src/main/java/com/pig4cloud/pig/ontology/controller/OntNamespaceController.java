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
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.service.OntNamespaceService;
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
 * 命名空间管理。
 *
 * @author youming
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ontology/namespaces")
@Tag(description = "ontology-namespace", name = "命名空间管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntNamespaceController {

	private final OntNamespaceService ontNamespaceService;

	/**
	 * 分页查询命名空间列表。
	 * @param page 分页参数
	 * @param query 查询条件
	 * @return 命名空间分页列表
	 */
	@GetMapping
	@HasPermission("ontology_namespace_view")
	public R<IPage<OntNamespace>> page(@ParameterObject Page<OntNamespace> page, @ParameterObject OntNamespace query) {
		return R.ok(ontNamespaceService.page(page, Wrappers.<OntNamespace>lambdaQuery()
			.like(StrUtil.isNotBlank(query.getPrefix()), OntNamespace::getPrefix, query.getPrefix())
			.like(StrUtil.isNotBlank(query.getUri()), OntNamespace::getUri, query.getUri())
			.eq(StrUtil.isNotBlank(query.getIsBuiltin()), OntNamespace::getIsBuiltin, query.getIsBuiltin())
			.orderByAsc(OntNamespace::getSortOrder)
			.orderByAsc(OntNamespace::getId)));
	}

	/**
	 * 查询命名空间列表（不分页，供下拉引用）。
	 * @param query 查询条件
	 * @return 命名空间列表
	 */
	@GetMapping("/list")
	@HasPermission("ontology_namespace_view")
	public R list(@ParameterObject OntNamespace query) {
		return R.ok(ontNamespaceService.list(Wrappers.<OntNamespace>lambdaQuery()
			.like(StrUtil.isNotBlank(query.getPrefix()), OntNamespace::getPrefix, query.getPrefix())
			.like(StrUtil.isNotBlank(query.getUri()), OntNamespace::getUri, query.getUri())
			.eq(StrUtil.isNotBlank(query.getIsBuiltin()), OntNamespace::getIsBuiltin, query.getIsBuiltin())
			.orderByAsc(OntNamespace::getSortOrder)
			.orderByAsc(OntNamespace::getId)));
	}

	/**
	 * 查询命名空间详情。
	 * @param id 命名空间ID
	 * @return 命名空间详情
	 */
	@GetMapping("/{id}")
	@HasPermission("ontology_namespace_view")
	public R getById(@PathVariable Long id) {
		return R.ok(ontNamespaceService.getById(id));
	}

	/**
	 * 新增命名空间。
	 * @param namespace 命名空间
	 * @return 新增结果
	 */
	@SysLog("新增命名空间")
	@PostMapping
	@HasPermission("ontology_namespace_add")
	public R save(@Valid @RequestBody OntNamespace namespace) {
		return ontNamespaceService.saveNamespace(namespace);
	}

	/**
	 * 修改命名空间。
	 * @param namespace 命名空间
	 * @return 修改结果
	 */
	@SysLog("修改命名空间")
	@PutMapping
	@HasPermission("ontology_namespace_edit")
	public R update(@Valid @RequestBody OntNamespace namespace) {
		return ontNamespaceService.updateNamespace(namespace);
	}

	/**
	 * 删除命名空间。
	 * @param id 命名空间ID
	 * @return 删除结果
	 */
	@SysLog("删除命名空间")
	@DeleteMapping("/{id}")
	@HasPermission("ontology_namespace_del")
	public R remove(@PathVariable Long id) {
		return ontNamespaceService.removeNamespace(id);
	}

}
