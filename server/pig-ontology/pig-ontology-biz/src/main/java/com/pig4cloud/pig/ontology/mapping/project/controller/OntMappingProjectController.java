/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.mapping.project.dto.MappingProjectCreateDTO;
import com.pig4cloud.pig.ontology.mapping.project.dto.MappingProjectUpdateDTO;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.mapping.project.service.OntMappingProjectService;
import com.pig4cloud.pig.ontology.mapping.project.service.OntMappingVersionService;
import com.pig4cloud.pig.ontology.mapping.project.vo.MappingProjectVO;
import com.pig4cloud.pig.ontology.mapping.project.vo.MappingVersionDiffVO;
import com.pig4cloud.pig.ontology.mapping.project.vo.MappingVersionVO;
import com.pig4cloud.pig.ontology.mapping.vo.PublishPrepareResultVO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 映射工程与版本管理 API（18-03 §11）。
 *
 * @author youming
 */
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
@Tag(name = "映射工程与版本管理")
public class OntMappingProjectController {

	private final OntMappingProjectService projectService;

	private final OntMappingVersionService versionService;

	// ==================== 工程管理 ====================

	@GetMapping("/ontology/data-mapping/projects/page")
	@HasPermission("ontology_mapping_view")
	public R<Page<MappingProjectVO>> projectPage(@ParameterObject Page<OntMappingProject> page,
			@RequestParam(required = false) String mappingCode,
			@RequestParam(required = false) String mappingName,
			@RequestParam(required = false) String projectStatus,
			@RequestParam(required = false) Long ontologyId) {
		return R.ok(projectService.page(page, mappingCode, mappingName, projectStatus, ontologyId));
	}

	@GetMapping("/ontology/data-mapping/projects/{id}")
	@HasPermission("ontology_mapping_view")
	public R<MappingProjectVO> getProjectDetail(@PathVariable Long id) {
		return R.ok(projectService.getDetail(id));
	}

	@PostMapping("/ontology/data-mapping/projects")
	@SysLog("新增映射工程")
	@HasPermission("ontology_mapping_edit")
	public R<MappingProjectVO> createProject(@Valid @RequestBody MappingProjectCreateDTO dto) {
		return R.ok(projectService.create(dto));
	}

	@PutMapping("/ontology/data-mapping/projects/{id}")
	@SysLog("修改映射工程")
	@HasPermission("ontology_mapping_edit")
	public R<MappingProjectVO> updateProject(@PathVariable Long id,
			@Valid @RequestBody MappingProjectUpdateDTO dto) {
		dto.setId(id);
		return R.ok(projectService.update(dto));
	}

	@DeleteMapping("/ontology/data-mapping/projects/{id}")
	@SysLog("删除映射工程")
	@HasPermission("ontology_mapping_admin")
	public R<Boolean> deleteProject(@PathVariable Long id) {
		return R.ok(projectService.remove(id));
	}

	@PutMapping("/ontology/data-mapping/projects/{id}/status")
	@SysLog("更新映射工程状态")
	@HasPermission("ontology_mapping_edit")
	public R<MappingProjectVO> updateProjectStatus(@PathVariable Long id,
			@RequestParam String status) {
		return R.ok(projectService.updateStatus(id, status));
	}

	// ==================== 版本管理 ====================

	@GetMapping("/ontology/data-mapping/projects/{id}/versions")
	@HasPermission("ontology_mapping_view")
	public R<Page<MappingVersionVO>> listVersions(@PathVariable Long id,
			@ParameterObject Page<OntMappingVersion> page) {
		return R.ok(versionService.listVersions(page, id));
	}

	@PostMapping("/ontology/data-mapping/projects/{id}/versions")
	@SysLog("创建映射版本")
	@HasPermission("ontology_mapping_edit")
	public R<MappingVersionVO> createNextVersion(@PathVariable Long id) {
		return R.ok(versionService.createNextVersion(id));
	}

	@GetMapping("/ontology/data-mapping/versions/{id}")
	@HasPermission("ontology_mapping_view")
	public R<MappingVersionVO> getVersionDetail(@PathVariable Long id) {
		return R.ok(versionService.getVersionDetail(id));
	}

	@PostMapping("/ontology/data-mapping/versions/{id}/reopen")
	@SysLog("重开映射版本")
	@HasPermission("ontology_mapping_edit")
	public R<MappingVersionVO> reopenVersion(@PathVariable Long id) {
		return R.ok(versionService.reopen(id));
	}

	@PostMapping("/ontology/data-mapping/versions/{id}/publish")
	@SysLog("发布映射版本")
	@HasPermission("ontology_mapping_publish")
	public R<MappingVersionVO> publishVersion(@PathVariable Long id) {
		return R.ok(versionService.publish(id));
	}

	@PostMapping("/ontology/data-mapping/versions/{id}/retire")
	@SysLog("停用映射版本")
	@HasPermission("ontology_mapping_publish")
	public R<MappingVersionVO> retireVersion(@PathVariable Long id) {
		return R.ok(versionService.retire(id));
	}

	@GetMapping("/ontology/data-mapping/versions/{id}/snapshot")
	@HasPermission("ontology_mapping_admin")
	public R<String> getSnapshot(@PathVariable Long id) {
		return R.ok(versionService.getSnapshot(id));
	}

	@GetMapping("/ontology/data-mapping/versions/{id}/diff/{otherId}")
	@HasPermission("ontology_mapping_view")
	public R<MappingVersionDiffVO> diffVersions(@PathVariable Long id, @PathVariable Long otherId) {
		return R.ok(versionService.diff(id, otherId));
	}

	@PostMapping("/ontology/data-mapping/versions/{id}/publish/prepare")
	@HasPermission("ontology_mapping_publish")
	public R<PublishPrepareResultVO> preparePublish(@PathVariable Long id) {
		return R.ok(versionService.preparePublish(id));
	}

}
