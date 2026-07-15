/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.mapping.preview.MappingPreviewRequest;
import com.pig4cloud.pig.ontology.mapping.preview.MappingPreviewResult;
import com.pig4cloud.pig.ontology.mapping.preview.MappingPreviewService;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidationService;
import com.pig4cloud.pig.ontology.mapping.validation.entity.OntMappingValidationIssue;
import com.pig4cloud.pig.ontology.mapping.validation.vo.ValidationIssueVO;
import com.pig4cloud.pig.ontology.mapping.validation.vo.ValidationReportVO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 映射校验与预览 API（18-06 §14）。
 * <p>
 * 所有路径前缀为 /ontology/data-mapping。
 *
 * @author youming
 */
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
@Tag(name = "映射校验与预览")
public class OntMappingValidationController {

	private final MappingValidationService validationService;

	private final MappingPreviewService previewService;

	// ==================== 预览 ====================

	@PostMapping("/ontology/data-mapping/versions/{id}/preview")
	@SysLog("映射版本预览")
	@HasPermission("ontology_mapping_validate")
	public R<MappingPreviewResult> preview(@PathVariable Long id,
			@RequestBody(required = false) MappingPreviewRequest request) {
		if (request == null) {
			request = new MappingPreviewRequest();
		}
		request.setMappingVersionId(id);
		return R.ok(previewService.preview(request));
	}

	// ==================== 校验 ====================

	@PostMapping("/ontology/data-mapping/versions/{id}/validate")
	@SysLog("映射版本校验")
	@HasPermission("ontology_mapping_validate")
	public R<ValidationReportVO> validate(@PathVariable Long id,
			@RequestParam(defaultValue = "MANUAL") String triggerType) {
		return R.ok(validationService.validate(id, triggerType));
	}

	// ==================== 校验报告 ====================

	@GetMapping("/ontology/data-mapping/validation-reports/{id}")
	@HasPermission("ontology_mapping_view")
	public R<ValidationReportVO> getReport(@PathVariable Long id) {
		return R.ok(validationService.getReport(id));
	}

	@GetMapping("/ontology/data-mapping/validation-reports/{id}/issues")
	@HasPermission("ontology_mapping_view")
	public R<Page<ValidationIssueVO>> getIssues(@PathVariable Long id,
			@org.springdoc.core.annotations.ParameterObject Page<OntMappingValidationIssue> page,
			@RequestParam(required = false) String severity) {
		return R.ok(validationService.getIssues(id, page, severity));
	}

	// ==================== WARNING 确认 ====================

	@PostMapping("/ontology/data-mapping/validation-issues/{id}/acknowledge")
	@SysLog("确认校验WARNING")
	@HasPermission("ontology_mapping_publish")
	public R<ValidationIssueVO> acknowledgeIssue(@PathVariable Long id) {
		return R.ok(validationService.acknowledgeIssue(id));
	}

}
