/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.mapping.dto.EntityMappingCreateDTO;
import com.pig4cloud.pig.ontology.mapping.dto.EntityMappingUpdateDTO;
import com.pig4cloud.pig.ontology.mapping.dto.FieldMappingCreateDTO;
import com.pig4cloud.pig.ontology.mapping.dto.FieldMappingUpdateDTO;
import com.pig4cloud.pig.ontology.mapping.dto.IriPreviewRequestDTO;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.service.EntityMappingService;
import com.pig4cloud.pig.ontology.mapping.service.FieldMappingService;
import com.pig4cloud.pig.ontology.mapping.transform.TransformerInfo;
import com.pig4cloud.pig.ontology.mapping.transform.TransformerRegistry;
import com.pig4cloud.pig.ontology.mapping.vo.EntityMappingVO;
import com.pig4cloud.pig.ontology.mapping.vo.FieldMappingVO;
import com.pig4cloud.pig.ontology.mapping.vo.IriPreviewResultVO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 实体与数据属性映射 API（18-04 §12）。
 *
 * @author youming
 */
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
@Tag(name = "实体与数据属性映射")
public class OntEntityMappingController {

	private final EntityMappingService entityMappingService;

	private final FieldMappingService fieldMappingService;

	private final TransformerRegistry transformerRegistry;

	// ==================== 实体映射 ====================

	@GetMapping("/ontology/data-mapping/versions/{versionId}/entity-mappings")
	@HasPermission("ontology_mapping_view")
	public R<Page<EntityMappingVO>> listEntityMappings(@PathVariable Long versionId,
			@ParameterObject Page<OntEntityMapping> page,
			@RequestParam(required = false) String mappingCode,
			@RequestParam(required = false) String mappingName,
			@RequestParam(required = false) Boolean enabledOnly) {
		return R.ok(entityMappingService.listByVersion(versionId, page, mappingCode, mappingName, enabledOnly));
	}

	@GetMapping("/ontology/data-mapping/entity-mappings/{id}")
	@HasPermission("ontology_mapping_view")
	public R<EntityMappingVO> getEntityMappingDetail(@PathVariable Long id) {
		return R.ok(entityMappingService.getDetail(id));
	}

	@PostMapping("/ontology/data-mapping/versions/{versionId}/entity-mappings")
	@SysLog("新增实体映射")
	@HasPermission("ontology_mapping_edit")
	public R<EntityMappingVO> createEntityMapping(@PathVariable Long versionId,
			@Valid @RequestBody EntityMappingCreateDTO dto) {
		return R.ok(entityMappingService.create(versionId, dto));
	}

	@PutMapping("/ontology/data-mapping/entity-mappings/{id}")
	@SysLog("修改实体映射")
	@HasPermission("ontology_mapping_edit")
	public R<EntityMappingVO> updateEntityMapping(@PathVariable Long id,
			@Valid @RequestBody EntityMappingUpdateDTO dto) {
		dto.setId(id);
		return R.ok(entityMappingService.update(dto));
	}

	@DeleteMapping("/ontology/data-mapping/entity-mappings/{id}")
	@SysLog("删除实体映射")
	@HasPermission("ontology_mapping_edit")
	public R<Boolean> deleteEntityMapping(@PathVariable Long id) {
		return R.ok(entityMappingService.remove(id));
	}

	// ==================== IRI 预览 ====================

	@PostMapping("/ontology/data-mapping/entity-mappings/{id}/iri-preview")
	@HasPermission("ontology_mapping_view")
	public R<IriPreviewResultVO> iriPreview(@PathVariable Long id,
			@Valid @RequestBody IriPreviewRequestDTO request) {
		return R.ok(entityMappingService.iriPreview(id, request));
	}

	// ==================== 字段映射 ====================

	@GetMapping("/ontology/data-mapping/entity-mappings/{id}/fields")
	@HasPermission("ontology_mapping_view")
	public R<List<FieldMappingVO>> listFieldMappings(@PathVariable Long id) {
		return R.ok(fieldMappingService.listByEntityMapping(id));
	}

	@PostMapping("/ontology/data-mapping/entity-mappings/{id}/fields")
	@SysLog("新增字段映射")
	@HasPermission("ontology_mapping_edit")
	public R<FieldMappingVO> createFieldMapping(@PathVariable Long id,
			@Valid @RequestBody FieldMappingCreateDTO dto) {
		return R.ok(fieldMappingService.create(id, dto));
	}

	@PutMapping("/ontology/data-mapping/field-mappings/{id}")
	@SysLog("修改字段映射")
	@HasPermission("ontology_mapping_edit")
	public R<FieldMappingVO> updateFieldMapping(@PathVariable Long id,
			@Valid @RequestBody FieldMappingUpdateDTO dto) {
		dto.setId(id);
		return R.ok(fieldMappingService.update(dto));
	}

	@DeleteMapping("/ontology/data-mapping/field-mappings/{id}")
	@SysLog("删除字段映射")
	@HasPermission("ontology_mapping_edit")
	public R<Boolean> deleteFieldMapping(@PathVariable Long id) {
		return R.ok(fieldMappingService.remove(id));
	}

	// ==================== 转换器注册表 ====================

	@GetMapping("/ontology/data-mapping/transformers")
	@HasPermission("ontology_mapping_view")
	public R<List<TransformerInfo>> listTransformers() {
		return R.ok(transformerRegistry.listAvailable());
	}

}
