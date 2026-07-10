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
import com.pig4cloud.pig.ontology.dto.OntDataPropertyCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntDataPropertyQuery;
import com.pig4cloud.pig.ontology.dto.OntDataPropertyUpdateDTO;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.service.OntDataPropertyService;
import com.pig4cloud.pig.ontology.vo.OntApplicableDataPropertyVO;
import com.pig4cloud.pig.ontology.vo.OntDataPropertyDetailVO;
import com.pig4cloud.pig.ontology.vo.OntDataPropertySummaryVO;
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
 * 数据属性管理。
 *
 * @author youming
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ontology/data-properties")
@Tag(description = "ontology-data-property", name = "数据属性管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntDataPropertyController {

	private final OntDataPropertyService ontDataPropertyService;

	/**
	 * 分页查询数据属性。
	 * @param page 分页参数
	 * @param query 查询条件
	 * @return 数据属性分页列表
	 */
	@GetMapping
	@HasPermission("ontology_data_property_view")
	public R<IPage<OntDataPropertySummaryVO>> page(@ParameterObject Page<OntDataProperty> page,
			@ParameterObject OntDataPropertyQuery query) {
		return R.ok(ontDataPropertyService.pageSummary(page, query));
	}

	/**
	 * 查询数据属性列表（不分页，供下拉引用）。
	 * @param query 查询条件
	 * @return 数据属性摘要列表
	 */
	@GetMapping("/list")
	@HasPermission("ontology_data_property_view")
	public R<List<OntDataPropertySummaryVO>> list(@ParameterObject OntDataPropertyQuery query) {
		return R.ok(ontDataPropertyService.listSummary(query));
	}

	/**
	 * 查询数据属性详情。
	 * @param id 数据属性ID
	 * @return 详情
	 */
	@GetMapping("/{id}")
	@HasPermission("ontology_data_property_view")
	public R<OntDataPropertyDetailVO> getById(@PathVariable Long id) {
		OntDataPropertyDetailVO detail = ontDataPropertyService.getDetail(id);
		if (detail == null) {
			return R.failed("数据属性不存在");
		}
		return R.ok(detail);
	}

	/**
	 * 按定义域查询适用数据属性（含继承）。
	 * @param entityTypeId 实体类型ID
	 * @return 适用数据属性列表
	 */
	@GetMapping("/by-domain/{entityTypeId}")
	@HasPermission("ontology_data_property_view")
	public R<List<OntApplicableDataPropertyVO>> byDomain(@PathVariable Long entityTypeId) {
		return R.ok(ontDataPropertyService.listApplicableByDomain(entityTypeId));
	}

	/**
	 * 新增数据属性。
	 * @param request 新增请求
	 * @return 新增结果
	 */
	@SysLog("新增数据属性")
	@PostMapping
	@HasPermission("ontology_data_property_add")
	public R<OntDataProperty> save(@Valid @RequestBody OntDataPropertyCreateDTO request) {
		return ontDataPropertyService.saveDataProperty(request);
	}

	/**
	 * 修改数据属性。
	 * @param request 修改请求
	 * @return 修改结果
	 */
	@SysLog("修改数据属性")
	@PutMapping
	@HasPermission("ontology_data_property_edit")
	public R<OntDataProperty> update(@Valid @RequestBody OntDataPropertyUpdateDTO request) {
		return ontDataPropertyService.updateDataProperty(request);
	}

	/**
	 * 删除数据属性。
	 * @param id 数据属性ID
	 * @return 删除结果
	 */
	@SysLog("删除数据属性")
	@DeleteMapping("/{id}")
	@HasPermission("ontology_data_property_del")
	public R<Boolean> remove(@PathVariable Long id) {
		return ontDataPropertyService.removeDataProperty(id);
	}

}
