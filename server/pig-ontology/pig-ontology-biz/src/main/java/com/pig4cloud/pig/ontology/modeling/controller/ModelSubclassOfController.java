package com.pig4cloud.pig.ontology.modeling.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.modeling.dto.SubclassOfSaveDTO;
import com.pig4cloud.pig.ontology.modeling.service.ModelSubclassOfService;
import com.pig4cloud.pig.ontology.modeling.vo.ModelSubclassOfTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 类层级 Controller（FR-14）
 * <p>
 * subClassOf 是类的子操作，复用类建模权限 ont_class_model_manage。
 * 路径 /ont/model/subclassof/**，对外 /admin/ont/model/subclassof/**
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/model/subclassof")
@Tag(name = "类层级建模", description = "subClassOf CRUD + 类树 + 镜像回推（FR-14）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ModelSubclassOfController {

	private final ModelSubclassOfService subclassOfService;

	@GetMapping("/tree")
	@Operation(summary = "类树视图", description = "按 projectId 返回类层级树（AC-14.1）")
	@HasPermission("ont_class_model_view")
	public R<List<ModelSubclassOfTreeVO>> tree(@RequestParam Long projectId) {
		return R.ok(subclassOfService.tree(projectId));
	}

	@GetMapping("/suggest-parent")
	@Operation(summary = "父类建议", description = "消费治理域 suggest 端点（AC-14.2）")
	@HasPermission("ont_class_model_view")
	public R<List<String>> suggestParent(@RequestParam Long classId) {
		return R.ok(subclassOfService.suggestParentIris(classId));
	}

	@SysLog("建立类层级")
	@PostMapping
	@Operation(summary = "建立 subClassOf", description = "环路检测 + 镜像回推（AC-14.1/14.3）")
	@HasPermission("ont_class_model_manage")
	public R save(@Valid @RequestBody SubclassOfSaveDTO dto) {
		return subclassOfService.saveEdge(dto);
	}

	@SysLog("删除类层级")
	@DeleteMapping
	@Operation(summary = "删除 subClassOf", description = "镜像失效（AC-14.4）")
	@HasPermission("ont_class_model_manage")
	public R remove(@RequestParam Long childClassId, @RequestParam Long parentClassId) {
		return subclassOfService.removeEdge(childClassId, parentClassId);
	}

}
