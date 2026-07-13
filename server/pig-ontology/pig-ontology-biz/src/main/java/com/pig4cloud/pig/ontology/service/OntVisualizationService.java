/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service;

import com.pig4cloud.pig.ontology.vo.GraphDataVO;
import com.pig4cloud.pig.ontology.vo.VisualizationStatsVO;

/**
 * 本体可视化服务。
 * <p>
 * 从关系库聚合查询各模块数据，组装为 echarts graph series 可消费的
 * { nodes, edges, categories, summary } JSON 结构。
 * 不经过 Jena Model 转换，直接 SQL 查询 + 内存组装。
 * </p>
 *
 * @author youming
 */
public interface OntVisualizationService {

	/**
	 * 核心本体工程ID。
	 */
	long CORE_ONTOLOGY_ID = 935001L;

	/**
	 * 构建 Schema 层完整图谱。
	 * <p>
	 * 节点 = 实体类型（owl:Class）； 边 = 继承（subClassOf）+ 对象属性（domain→range）+ 等价类 + 不相交类。
	 * </p>
	 * @param ontologyId 本体工程ID
	 * @return 图谱数据
	 */
	GraphDataVO buildSchemaGraph(Long ontologyId);

	/**
	 * 构建实例层完整图谱。
	 * <p>
	 * 节点 = 实例（individual）； 边 = 对象属性断言（subject→object）。
	 * 节点数超过 maxNodes 时切换为摘要模式（按类型分组折叠）。
	 * </p>
	 * @param ontologyId 本体工程ID
	 * @param entityTypeId 可选，按实体类型过滤（含子类型）
	 * @param maxNodes 最大节点数阈值
	 * @return 图谱数据
	 */
	GraphDataVO buildInstanceGraph(Long ontologyId, Long entityTypeId, Integer maxNodes);

	/**
	 * 构建某实例的 N 跳邻居子图。
	 * @param instanceId 中心实例ID
	 * @param depth 展开深度（1-3）
	 * @param direction 方向：OUTGOING / INCOMING / BOTH
	 * @return 子图数据
	 */
	GraphDataVO buildInstanceSubgraph(Long instanceId, Integer depth, String direction);

	/**
	 * 构建某实体类型及其子类型的 Schema 子图。
	 * @param entityTypeId 根实体类型ID
	 * @param includeObjectProperties 是否包含对象属性边
	 * @param includeAxioms 是否包含等价/不相交边
	 * @return 子图数据
	 */
	GraphDataVO buildEntityTypeSubgraph(Long entityTypeId, Boolean includeObjectProperties, Boolean includeAxioms);

	/**
	 * 获取可视化首页统计摘要。
	 * @param ontologyId 本体工程ID
	 * @return 统计数据
	 */
	VisualizationStatsVO getStats(Long ontologyId);

}
