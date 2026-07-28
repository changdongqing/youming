package com.pig4cloud.pig.ontology.modeling.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.modeling.entity.ModelProject;
import com.pig4cloud.pig.ontology.modeling.service.ModelProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 本体项目管理 Controller（FR-10）
 * <p>
 * 路径 /ont/model/project/**，对外 /admin/ont/model/project/**
 * （复用 pig-gateway 已有路由 Path=/admin/ont/**，gateway StripPrefix=1 / boot context-path=/admin
 * 剥离后需 /ont 前缀）。
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/model/project")
@Tag(name = "本体项目管理", description = "项目 CRUD + 前缀管理（FR-10）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ModelProjectController {

	private final ModelProjectService modelProjectService;

	@GetMapping("/page")
	@Operation(summary = "分页查询", description = "按名称/状态过滤（AC-10.5）")
	@HasPermission("ont_project_view")
	public R<IPage<ModelProject>> page(@ParameterObject Page page, @ParameterObject ModelProject project) {
		return R.ok(modelProjectService.page(page, project));
	}

	@GetMapping("/{id}")
	@Operation(summary = "项目详情", description = "含前缀列表（AC-10.1）")
	@HasPermission("ont_project_view")
	public R<ModelProject> getById(@PathVariable Long id) {
		return R.ok(modelProjectService.getDetail(id));
	}

	@SysLog("新增本体项目")
	@PostMapping
	@Operation(summary = "新增项目", description = "projectCode 查重 + 策略强制 B（AC-10.1/10.3）")
	@HasPermission("ont_project_manage")
	public R save(@Valid @RequestBody ModelProject project) {
		return modelProjectService.saveProject(project);
	}

	@SysLog("编辑本体项目")
	@PutMapping("/{id}")
	@Operation(summary = "编辑项目", description = "archived 拒绝写（AC-10.4）")
	@HasPermission("ont_project_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody ModelProject project) {
		project.setId(id);
		return modelProjectService.updateProject(project);
	}

	@SysLog("删除本体项目")
	@DeleteMapping("/{id}")
	@Operation(summary = "删除项目", description = "级联软删前缀；类实体校验移交 DD8（AC-10.1）")
	@HasPermission("ont_project_manage")
	public R removeById(@PathVariable Long id) {
		return modelProjectService.removeProject(id);
	}

}
