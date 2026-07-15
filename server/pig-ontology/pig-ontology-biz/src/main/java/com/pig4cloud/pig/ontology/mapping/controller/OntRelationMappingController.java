/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.mapping.dto.RelationKeyPreviewRequestDTO;
import com.pig4cloud.pig.ontology.mapping.dto.RelationMappingCreateDTO;
import com.pig4cloud.pig.ontology.mapping.dto.RelationMappingUpdateDTO;
import com.pig4cloud.pig.ontology.mapping.entity.OntPendingRelation;
import com.pig4cloud.pig.ontology.mapping.entity.OntRelationMapping;
import com.pig4cloud.pig.ontology.mapping.service.PendingRelationService;
import com.pig4cloud.pig.ontology.mapping.service.RelationMappingService;
import com.pig4cloud.pig.ontology.mapping.vo.PendingRelationVO;
import com.pig4cloud.pig.ontology.mapping.vo.RelationKeyPreviewResultVO;
import com.pig4cloud.pig.ontology.mapping.vo.RelationMappingVO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 对象属性关系映射 API（18-05 §13）。
 *
 * @author youming
 */
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
@Tag(name = "对象属性关系映射")
public class OntRelationMappingController {

	private final RelationMappingService relationMappingService;

	private final PendingRelationService pendingRelationService;

	// ==================== 关系映射 ====================

	@GetMapping("/ontology/data-mapping/versions/{versionId}/relation-mappings")
	@HasPermission("ontology_mapping_view")
	public R<Page<RelationMappingVO>> listRelationMappings(@PathVariable Long versionId,
			@ParameterObject Page<OntRelationMapping> page,
			@RequestParam(required = false) String mappingCode,
			@RequestParam(required = false) String mappingName,
			@RequestParam(required = false) Boolean enabledOnly) {
		return R.ok(relationMappingService.listByVersion(versionId, page, mappingCode, mappingName, enabledOnly));
	}

	@GetMapping("/ontology/data-mapping/relation-mappings/{id}")
	@HasPermission("ontology_mapping_view")
	public R<RelationMappingVO> getRelationMappingDetail(@PathVariable Long id) {
		return R.ok(relationMappingService.getDetail(id));
	}

	@PostMapping("/ontology/data-mapping/versions/{versionId}/relation-mappings")
	@SysLog("新增关系映射")
	@HasPermission("ontology_mapping_edit")
	public R<RelationMappingVO> createRelationMapping(@PathVariable Long versionId,
			@Valid @RequestBody RelationMappingCreateDTO dto) {
		return R.ok(relationMappingService.create(versionId, dto));
	}

	@PutMapping("/ontology/data-mapping/relation-mappings/{id}")
	@SysLog("修改关系映射")
	@HasPermission("ontology_mapping_edit")
	public R<RelationMappingVO> updateRelationMapping(@PathVariable Long id,
			@Valid @RequestBody RelationMappingUpdateDTO dto) {
		dto.setId(id);
		return R.ok(relationMappingService.update(dto));
	}

	@DeleteMapping("/ontology/data-mapping/relation-mappings/{id}")
	@SysLog("删除关系映射")
	@HasPermission("ontology_mapping_edit")
	public R<Boolean> deleteRelationMapping(@PathVariable Long id) {
		return R.ok(relationMappingService.remove(id));
	}

	// ==================== 键预览 ====================

	@PostMapping("/ontology/data-mapping/relation-mappings/{id}/key-preview")
	@HasPermission("ontology_mapping_view")
	public R<RelationKeyPreviewResultVO> keyPreview(@PathVariable Long id,
			@Valid @RequestBody RelationKeyPreviewRequestDTO request) {
		return R.ok(relationMappingService.keyPreview(id, request));
	}

	// ==================== 待解析关系 ====================

	@GetMapping("/ontology/data-mapping/pending-relations/page")
	@HasPermission("ontology_mapping_job_view")
	public R<Page<PendingRelationVO>> listPendingRelations(@ParameterObject Page<OntPendingRelation> page,
			@RequestParam(required = false) Long mappingProjectId,
			@RequestParam(required = false) String pendingStatus,
			@RequestParam(required = false) String relationMappingCode) {
		return R.ok(pendingRelationService.page(page, mappingProjectId, pendingStatus, relationMappingCode));
	}

	@PostMapping("/ontology/data-mapping/pending-relations/{id}/retry")
	@SysLog("人工重试待解析关系")
	@HasPermission("ontology_mapping_retry")
	public R<PendingRelationVO> retryPendingRelation(@PathVariable Long id) {
		return R.ok(pendingRelationService.retry(id));
	}

	@PostMapping("/ontology/data-mapping/pending-relations/{id}/ignore")
	@SysLog("管理员忽略待解析关系")
	@HasPermission("ontology_mapping_admin")
	public R<PendingRelationVO> ignorePendingRelation(@PathVariable Long id) {
		return R.ok(pendingRelationService.ignore(id));
	}

}
