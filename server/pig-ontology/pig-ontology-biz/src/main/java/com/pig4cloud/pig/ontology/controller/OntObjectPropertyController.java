/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.dto.OntObjectPropertyCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntObjectPropertyQuery;
import com.pig4cloud.pig.ontology.dto.OntObjectPropertyUpdateDTO;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.service.OntObjectPropertyService;
import com.pig4cloud.pig.ontology.vo.OntApplicableObjectPropertyByRangeVO;
import com.pig4cloud.pig.ontology.vo.OntApplicableObjectPropertyVO;
import com.pig4cloud.pig.ontology.vo.OntObjectPropertyDetailVO;
import com.pig4cloud.pig.ontology.vo.OntObjectPropertySummaryVO;
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

/**
 * 对象属性管理。
 *
 * @author youming
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ontology/object-properties")
@Tag(description = "ontology-object-property", name = "对象属性管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntObjectPropertyController {

	private final OntObjectPropertyService ontObjectPropertyService;

	/**
	 * 分页查询对象属性。
	 * @param page 分页参数
	 * @param query 查询条件
	 * @return 对象属性分页列表
	 */
	@GetMapping
	@HasPermission("ontology_object_property_view")
	public R<IPage<OntObjectPropertySummaryVO>> page(@ParameterObject Page<OntObjectProperty> page,
			@ParameterObject OntObjectPropertyQuery query) {
		return R.ok(ontObjectPropertyService.pageSummary(page, query));
	}

	/**
	 * 查询对象属性列表（不分页，供下拉引用）。
	 * @param query 查询条件
	 * @return 对象属性摘要列表
	 */
	@GetMapping("/list")
	@HasPermission("ontology_object_property_view")
	public R<List<OntObjectPropertySummaryVO>> list(@ParameterObject OntObjectPropertyQuery query) {
		return R.ok(ontObjectPropertyService.listSummary(query));
	}

	/**
	 * 查询对象属性详情。
	 * @param id 对象属性ID
	 * @return 详情
	 */
	@GetMapping("/{id}")
	@HasPermission("ontology_object_property_view")
	public R<OntObjectPropertyDetailVO> getById(@PathVariable Long id) {
		OntObjectPropertyDetailVO detail = ontObjectPropertyService.getDetail(id);
		if (detail == null) {
			return R.failed("对象属性不存在");
		}
		return R.ok(detail);
	}

	/**
	 * 按定义域查询适用对象属性（含继承）。
	 * @param entityTypeId 实体类型ID
	 * @return 适用对象属性列表
	 */
	@GetMapping("/by-domain/{entityTypeId}")
	@HasPermission("ontology_object_property_view")
	public R<List<OntApplicableObjectPropertyVO>> byDomain(@PathVariable Long entityTypeId) {
		return R.ok(ontObjectPropertyService.listApplicableByDomain(entityTypeId));
	}

	/**
	 * 按值域查询适用对象属性（含继承）。
	 * @param entityTypeId 实体类型ID
	 * @return 适用对象属性列表
	 */
	@GetMapping("/by-range/{entityTypeId}")
	@HasPermission("ontology_object_property_view")
	public R<List<OntApplicableObjectPropertyByRangeVO>> byRange(@PathVariable Long entityTypeId) {
		return R.ok(ontObjectPropertyService.listApplicableByRange(entityTypeId));
	}

	/**
	 * 新增对象属性。
	 * @param request 新增请求
	 * @return 新增结果
	 */
	@SysLog("新增对象属性")
	@PostMapping
	@HasPermission("ontology_object_property_add")
	public R<OntObjectProperty> save(@Valid @RequestBody OntObjectPropertyCreateDTO request) {
		return ontObjectPropertyService.saveObjectProperty(request);
	}

	/**
	 * 修改对象属性。
	 * @param request 修改请求
	 * @return 修改结果
	 */
	@SysLog("修改对象属性")
	@PutMapping
	@HasPermission("ontology_object_property_edit")
	public R<OntObjectProperty> update(@Valid @RequestBody OntObjectPropertyUpdateDTO request) {
		return ontObjectPropertyService.updateObjectProperty(request);
	}

	/**
	 * 删除对象属性。
	 * @param id 对象属性ID
	 * @return 删除结果
	 */
	@SysLog("删除对象属性")
	@DeleteMapping("/{id}")
	@HasPermission("ontology_object_property_del")
	public R<Boolean> remove(@PathVariable Long id) {
		return ontObjectPropertyService.removeObjectProperty(id);
	}

}
