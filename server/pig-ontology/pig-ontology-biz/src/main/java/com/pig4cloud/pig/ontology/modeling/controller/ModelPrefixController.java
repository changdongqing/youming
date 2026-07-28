package com.pig4cloud.pig.ontology.modeling.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.modeling.entity.ModelPrefix;
import com.pig4cloud.pig.ontology.modeling.service.ModelPrefixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * IRI 前缀管理 Controller（FR-10.2）
 * <p>
 * 前缀属于项目的子资源，挂在 /ont/model/project/{projectId}/prefix/** 下。
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/model/project")
@Tag(name = "IRI 前缀管理", description = "项目级前缀 CRUD（FR-10.2）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ModelPrefixController {

	private final ModelPrefixService modelPrefixService;

	@GetMapping("/{projectId}/prefixes")
	@Operation(summary = "项目前缀列表", description = "全量列表（AC-10.2）")
	@HasPermission("ont_project_view")
	public R<List<ModelPrefix>> list(@PathVariable Long projectId) {
		return R.ok(modelPrefixService.listByProject(projectId));
	}

	@SysLog("新增IRI前缀")
	@PostMapping("/{projectId}/prefix")
	@Operation(summary = "新增前缀", description = "NCName 校验 + 同项目查重（AC-10.2）")
	@HasPermission("ont_project_manage")
	public R savePrefix(@PathVariable Long projectId, @Valid @RequestBody ModelPrefix prefix) {
		prefix.setProjectId(projectId);
		return modelPrefixService.savePrefix(prefix);
	}

	@SysLog("编辑IRI前缀")
	@PutMapping("/{projectId}/prefix/{prefixId}")
	@Operation(summary = "编辑前缀")
	@HasPermission("ont_project_manage")
	public R updatePrefix(@PathVariable Long projectId, @PathVariable Long prefixId,
			@Valid @RequestBody ModelPrefix prefix) {
		prefix.setId(prefixId);
		prefix.setProjectId(projectId);
		return modelPrefixService.updatePrefix(prefix);
	}

	@SysLog("删除IRI前缀")
	@DeleteMapping("/{projectId}/prefix/{prefixId}")
	@Operation(summary = "删除前缀")
	@HasPermission("ont_project_manage")
	public R removePrefix(@PathVariable Long projectId, @PathVariable Long prefixId) {
		return modelPrefixService.removePrefix(prefixId);
	}

}
