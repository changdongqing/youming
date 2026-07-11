/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.dto.OntEntityInstanceCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntEntityInstanceQuery;
import com.pig4cloud.pig.ontology.dto.OntEntityInstanceUpdateDTO;
import com.pig4cloud.pig.ontology.dto.OntInstanceDataValueDTO;
import com.pig4cloud.pig.ontology.dto.OntInstanceObjectRelationDTO;
import com.pig4cloud.pig.ontology.dto.OntInstanceOptionQuery;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.service.OntEntityInstanceService;
import com.pig4cloud.pig.ontology.vo.OntEntityInstanceDetailVO;
import com.pig4cloud.pig.ontology.vo.OntEntityInstanceOptionVO;
import com.pig4cloud.pig.ontology.vo.OntEntityInstanceSummaryVO;
import com.pig4cloud.pig.ontology.vo.OntInstanceDataValueVO;
import com.pig4cloud.pig.ontology.vo.OntInstanceFormMetaVO;
import com.pig4cloud.pig.ontology.vo.OntInstanceObjectRelationVO;
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
 * 实体对象实例管理。
 *
 * @author youming
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ontology/instances")
@Tag(description = "ontology-instance", name = "实体对象实例管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntEntityInstanceController {

	private final OntEntityInstanceService ontEntityInstanceService;

	// ==================== 实例接口 ====================

	@GetMapping
	@HasPermission("ontology_instance_view")
	public R<IPage<OntEntityInstanceSummaryVO>> page(@ParameterObject Page<OntEntityInstance> page,
			@ParameterObject OntEntityInstanceQuery query) {
		return R.ok(ontEntityInstanceService.pageSummary(page, query));
	}

	@GetMapping("/{id}")
	@HasPermission("ontology_instance_view")
	public R<OntEntityInstanceDetailVO> getById(@PathVariable Long id) {
		OntEntityInstanceDetailVO detail = ontEntityInstanceService.getDetail(id);
		if (detail == null) {
			return R.failed("实例不存在");
		}
		return R.ok(detail);
	}

	@GetMapping("/form-meta/{entityTypeId}")
	@HasPermission("ontology_instance_view")
	public R<OntInstanceFormMetaVO> getFormMeta(@PathVariable Long entityTypeId) {
		OntInstanceFormMetaVO meta = ontEntityInstanceService.getFormMeta(entityTypeId);
		if (meta == null) {
			return R.failed("实体类型不存在");
		}
		return R.ok(meta);
	}

	@GetMapping("/options")
	@HasPermission("ontology_instance_view")
	public R<IPage<OntEntityInstanceOptionVO>> options(@ParameterObject Page<OntEntityInstance> page,
			@ParameterObject OntInstanceOptionQuery query) {
		return R.ok(ontEntityInstanceService.pageOptions(page, query));
	}

	@SysLog("新增实例")
	@PostMapping
	@HasPermission("ontology_instance_add")
	public R<OntEntityInstance> save(@Valid @RequestBody OntEntityInstanceCreateDTO request) {
		return ontEntityInstanceService.createInstance(request);
	}

	@SysLog("修改实例")
	@PutMapping
	@HasPermission("ontology_instance_edit")
	public R<OntEntityInstance> update(@Valid @RequestBody OntEntityInstanceUpdateDTO request) {
		return ontEntityInstanceService.updateInstance(request);
	}

	@SysLog("删除实例")
	@DeleteMapping("/{id}")
	@HasPermission("ontology_instance_del")
	public R<Boolean> remove(@PathVariable Long id) {
		return ontEntityInstanceService.deleteInstance(id);
	}

	// ==================== 数据值接口 ====================

	@GetMapping("/{id}/data-values")
	@HasPermission("ontology_instance_view")
	public R<List<OntInstanceDataValueVO>> getDataValues(@PathVariable Long id) {
		return R.ok(ontEntityInstanceService.getDataValues(id));
	}

	@SysLog("替换实例数据值")
	@PutMapping("/{id}/data-values")
	@HasPermission("ontology_instance_edit")
	public R<Boolean> replaceDataValues(@PathVariable Long id,
			@Valid @RequestBody List<OntInstanceDataValueDTO> dataValues) {
		return ontEntityInstanceService.replaceDataValues(id, dataValues);
	}

	@SysLog("删除实例数据值")
	@DeleteMapping("/{id}/data-values/{dataPropertyId}")
	@HasPermission("ontology_instance_edit")
	public R<Boolean> deleteDataValues(@PathVariable Long id, @PathVariable Long dataPropertyId) {
		return ontEntityInstanceService.deleteDataValues(id, dataPropertyId);
	}

	// ==================== 对象断言接口 ====================

	@GetMapping("/{id}/relations")
	@HasPermission("ontology_instance_view")
	public R<List<OntInstanceObjectRelationVO>> getRelations(@PathVariable Long id,
			@RequestParam(defaultValue = "OUTGOING") String direction) {
		return R.ok(ontEntityInstanceService.getRelations(id, direction));
	}

	@SysLog("新增实例对象断言")
	@PostMapping("/{id}/relations")
	@HasPermission("ontology_instance_edit")
	public R<OntInstanceObjectRelationVO> addRelation(@PathVariable Long id,
			@Valid @RequestBody OntInstanceObjectRelationDTO request) {
		return ontEntityInstanceService.addRelation(id, request);
	}

	@SysLog("删除实例对象断言")
	@DeleteMapping("/{id}/relations/{relationId}")
	@HasPermission("ontology_instance_edit")
	public R<Boolean> removeRelation(@PathVariable Long id, @PathVariable Long relationId) {
		return ontEntityInstanceService.removeRelation(id, relationId);
	}

}
