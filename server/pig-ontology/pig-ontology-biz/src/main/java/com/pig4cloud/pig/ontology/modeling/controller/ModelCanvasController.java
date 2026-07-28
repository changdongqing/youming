package com.pig4cloud.pig.ontology.modeling.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.modeling.entity.ModelCanvasState;
import com.pig4cloud.pig.ontology.modeling.service.ModelCanvasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 可视化画布 Controller（FR-17）
 * <p>
 * 路径 /ont/model/canvas/**，对外 /admin/ont/model/canvas/**
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/model/canvas")
@Tag(name = "可视化画布", description = "画布状态持久化 + 图数据查询（FR-17）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ModelCanvasController {

	private final ModelCanvasService modelCanvasService;

	@GetMapping("/{projectId}/state")
	@Operation(summary = "获取画布状态", description = "当前用户的画布状态（AC-17.8）")
	@HasPermission("ont_canvas_view")
	public R<ModelCanvasState> getState(@PathVariable Long projectId) {
		return R.ok(modelCanvasService.getState(projectId));
	}

	@SysLog("保存画布状态")
	@PutMapping("/{projectId}/state")
	@Operation(summary = "保存画布状态", description = "graph.toJSON() 持久化（AC-17.8）")
	@HasPermission("ont_canvas_manage")
	public R saveState(@PathVariable Long projectId, @RequestBody ModelCanvasState state) {
		state.setProjectId(projectId);
		return modelCanvasService.saveState(state);
	}

	@GetMapping("/{projectId}/graph")
	@Operation(summary = "获取画布图数据", description = "类节点 + subClassOf 边 + 对象属性边（AC-17.1）")
	@HasPermission("ont_canvas_view")
	public R<Map<String, Object>> getGraph(@PathVariable Long projectId) {
		return R.ok(modelCanvasService.buildGraphData(projectId));
	}

}
