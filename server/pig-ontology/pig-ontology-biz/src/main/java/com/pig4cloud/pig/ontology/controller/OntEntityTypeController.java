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
import com.pig4cloud.pig.ontology.dto.OntEntityTypeCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntEntityTypeQuery;
import com.pig4cloud.pig.ontology.dto.OntEntityTypeUpdateDTO;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.service.OntEntityTypeService;
import com.pig4cloud.pig.ontology.vo.OntEntityTypeDetailVO;
import com.pig4cloud.pig.ontology.vo.OntEntityTypeTreeNode;
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

import java.util.List;

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
	public R<IPage<OntEntityType>> page(@ParameterObject Page<OntEntityType> page,
			@ParameterObject OntEntityTypeQuery query) {
		return R.ok(ontEntityTypeService.page(page, Wrappers.<OntEntityType>lambdaQuery()
			.like(StrUtil.isNotBlank(query.getName()), OntEntityType::getName, query.getName())
			.eq(query.getOntologyId() != null, OntEntityType::getOntologyId, query.getOntologyId())
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
	public R<List<OntEntityType>> list(@ParameterObject OntEntityTypeQuery query) {
		return R.ok(ontEntityTypeService.list(Wrappers.<OntEntityType>lambdaQuery()
			.like(StrUtil.isNotBlank(query.getName()), OntEntityType::getName, query.getName())
			.eq(query.getOntologyId() != null, OntEntityType::getOntologyId, query.getOntologyId())
			.eq(query.getNamespaceId() != null, OntEntityType::getNamespaceId, query.getNamespaceId())
			.eq(StrUtil.isNotBlank(query.getIsBuiltin()), OntEntityType::getIsBuiltin, query.getIsBuiltin())
			.orderByAsc(OntEntityType::getSortOrder)
			.orderByAsc(OntEntityType::getId)));
	}

	/**
	 * 查询实体类型继承树。
	 * @param ontologyId 本体工程ID，首期缺省为核心工程
	 * @return 继承树
	 */
	@GetMapping("/tree")
	@HasPermission("ontology_entity_type_view")
	public R<List<OntEntityTypeTreeNode>> tree(@RequestParam(required = false) Long ontologyId) {
		return R.ok(ontEntityTypeService.tree(ontologyId));
	}

	/**
	 * 查询实体类型详情。
	 * @param id 实体类型ID
	 * @return 详情
	 */
	@GetMapping("/{id}")
	@HasPermission("ontology_entity_type_view")
	public R<OntEntityTypeDetailVO> getById(@PathVariable Long id) {
		OntEntityTypeDetailVO detail = ontEntityTypeService.getDetail(id);
		if (detail == null) {
			return R.failed("实体类型不存在");
		}
		return R.ok(detail);
	}

	/**
	 * 新增实体类型。
	 * @param request 新增请求
	 * @return 新增结果
	 */
	@SysLog("新增实体类型")
	@PostMapping
	@HasPermission("ontology_entity_type_add")
	public R<OntEntityType> save(@Valid @RequestBody OntEntityTypeCreateDTO request) {
		return ontEntityTypeService.saveEntityType(request);
	}

	/**
	 * 修改实体类型。
	 * @param request 修改请求
	 * @return 修改结果
	 */
	@SysLog("修改实体类型")
	@PutMapping
	@HasPermission("ontology_entity_type_edit")
	public R<OntEntityType> update(@Valid @RequestBody OntEntityTypeUpdateDTO request) {
		return ontEntityTypeService.updateEntityType(request);
	}

	/**
	 * 删除实体类型。
	 * @param id 实体类型ID
	 * @return 删除结果
	 */
	@SysLog("删除实体类型")
	@DeleteMapping("/{id}")
	@HasPermission("ontology_entity_type_del")
	public R<Boolean> remove(@PathVariable Long id) {
		return ontEntityTypeService.removeEntityType(id);
	}

	/**
	 * 查询实体类型的数据属性（首期返回空，数据属性模块上线后实现）。
	 * @param id 实体类型ID
	 * @return 数据属性列表
	 */
	@GetMapping("/{id}/properties")
	@HasPermission("ontology_entity_type_view")
	public R<List<Object>> properties(@PathVariable Long id) {
		if (ontEntityTypeService.getById(id) == null) {
			return R.failed("实体类型不存在");
		}
		return R.ok(List.of());
	}

}
