/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.service.OntVisualizationService;
import com.pig4cloud.pig.ontology.vo.GraphDataVO;
import com.pig4cloud.pig.ontology.vo.VisualizationStatsVO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 本体可视化控制器。
 * <p>
 * 提供图谱数据的聚合查询接口，前端 echarts 直接消费。
 * 全部只读 GET，仅需 ontology_visualization_view 权限。
 * </p>
 *
 * @author youming
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ontology/visualization")
@Tag(description = "ontology-visualization", name = "本体可视化")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntVisualizationController {

	private final OntVisualizationService ontVisualizationService;

	/**
	 * 获取 Schema 层完整图谱。
	 * @param ontologyId 本体工程ID
	 * @return 图谱数据
	 */
	@GetMapping("/schema-graph")
	@HasPermission("ontology_visualization_view")
	public R<GraphDataVO> getSchemaGraph(@RequestParam Long ontologyId) {
		return R.ok(ontVisualizationService.buildSchemaGraph(ontologyId));
	}

	/**
	 * 获取实例层完整图谱。
	 * @param ontologyId 本体工程ID
	 * @param entityTypeId 可选，按实体类型过滤
	 * @param maxNodes 最大节点数阈值
	 * @return 图谱数据
	 */
	@GetMapping("/instance-graph")
	@HasPermission("ontology_visualization_view")
	public R<GraphDataVO> getInstanceGraph(
			@RequestParam Long ontologyId,
			@RequestParam(required = false) Long entityTypeId,
			@RequestParam(required = false, defaultValue = "500") Integer maxNodes) {
		return R.ok(ontVisualizationService.buildInstanceGraph(ontologyId, entityTypeId, maxNodes));
	}

	/**
	 * 获取某实例的 N 跳邻居子图。
	 * @param instanceId 中心实例ID
	 * @param depth 展开深度（1-3）
	 * @param direction 方向：OUTGOING / INCOMING / BOTH
	 * @return 子图数据
	 */
	@GetMapping("/instance-subgraph")
	@HasPermission("ontology_visualization_view")
	public R<GraphDataVO> getInstanceSubgraph(
			@RequestParam Long instanceId,
			@RequestParam(required = false, defaultValue = "1") Integer depth,
			@RequestParam(required = false, defaultValue = "BOTH") String direction) {
		return R.ok(ontVisualizationService.buildInstanceSubgraph(instanceId, depth, direction));
	}

	/**
	 * 获取某实体类型及其子类型的 Schema 子图。
	 * @param entityTypeId 根实体类型ID
	 * @param includeObjectProperties 是否包含对象属性边
	 * @param includeAxioms 是否包含等价/不相交边
	 * @return 子图数据
	 */
	@GetMapping("/entity-type-subgraph")
	@HasPermission("ontology_visualization_view")
	public R<GraphDataVO> getEntityTypeSubgraph(
			@RequestParam Long entityTypeId,
			@RequestParam(required = false, defaultValue = "true") Boolean includeObjectProperties,
			@RequestParam(required = false, defaultValue = "true") Boolean includeAxioms) {
		return R.ok(ontVisualizationService.buildEntityTypeSubgraph(
				entityTypeId, includeObjectProperties, includeAxioms));
	}

	/**
	 * 获取可视化首页统计摘要。
	 * @param ontologyId 本体工程ID
	 * @return 统计数据
	 */
	@GetMapping("/stats")
	@HasPermission("ontology_visualization_view")
	public R<VisualizationStatsVO> getStats(@RequestParam Long ontologyId) {
		return R.ok(ontVisualizationService.getStats(ontologyId));
	}

}
