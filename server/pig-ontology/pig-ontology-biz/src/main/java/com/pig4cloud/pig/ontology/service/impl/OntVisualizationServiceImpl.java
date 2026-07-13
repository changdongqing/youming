/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntAxiomRuleTarget;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeDisjoint;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeEquivalent;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel;
import com.pig4cloud.pig.ontology.entity.OntInstanceObjectRelation;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyDomain;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyRange;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleMapper;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleTargetMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeDisjointMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeEquivalentMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceObjectRelationMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyDomainMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyRangeMapper;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import com.pig4cloud.pig.ontology.serialization.log.entity.OntSerializationLog;
import com.pig4cloud.pig.ontology.serialization.log.mapper.OntSerializationLogMapper;
import com.pig4cloud.pig.ontology.entity.OntValidationReport;
import com.pig4cloud.pig.ontology.mapper.OntValidationReportMapper;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.mapper.OntExtensionModuleMapper;
import com.pig4cloud.pig.ontology.service.OntVisualizationService;
import com.pig4cloud.pig.ontology.vo.GraphCategoryVO;
import com.pig4cloud.pig.ontology.vo.GraphDataVO;
import com.pig4cloud.pig.ontology.vo.GraphEdgeVO;
import com.pig4cloud.pig.ontology.vo.GraphNodeVO;
import com.pig4cloud.pig.ontology.vo.GraphSummaryVO;
import com.pig4cloud.pig.ontology.vo.NameValueVO;
import com.pig4cloud.pig.ontology.vo.VisualizationStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 本体可视化服务实现。
 * <p>
 * 从关系库聚合查询各模块数据，组装为 echarts graph series 可消费的
 * { nodes, edges, categories, summary } JSON 结构。
 * 不经过 Jena Model 转换，直接 SQL 查询 + 内存组装。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OntVisualizationServiceImpl implements OntVisualizationService {

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	private final OntEntityTypeDisjointMapper disjointMapper;

	private final OntEntityTypeEquivalentMapper equivalentMapper;

	private final OntEntityTypeLabelMapper labelMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	private final OntObjectPropertyDomainMapper objectPropertyDomainMapper;

	private final OntObjectPropertyRangeMapper objectPropertyRangeMapper;

	private final OntEntityInstanceMapper entityInstanceMapper;

	private final OntInstanceObjectRelationMapper instanceObjectRelationMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntAxiomRuleMapper axiomRuleMapper;

	private final OntAxiomRuleTargetMapper axiomRuleTargetMapper;

	private final OntNamespaceMapper namespaceMapper;

	private final OntOntologyProjectMapper ontologyProjectMapper;

	private final OntValidationReportMapper validationReportMapper;

	private final OntSerializationLogMapper serializationLogMapper;

	private final OntExtensionModuleMapper extensionModuleMapper;

	/**
	 * 节点分类（按顶层祖先着色）。
	 */
	private static final List<GraphCategoryVO> SCHEMA_CATEGORIES = List.of(
			new GraphCategoryVO("标准化对象类", "#5470C6"),
			new GraphCategoryVO("结构层次类", "#91CC75"),
			new GraphCategoryVO("描述对象类", "#FAC858"),
			new GraphCategoryVO("行动类", "#EE6666"),
			new GraphCategoryVO("表示形式类", "#73C0DE"),
			new GraphCategoryVO("术语类", "#3BA272"),
			new GraphCategoryVO("扩展类", "#FC8452"));

	/**
	 * 顶层祖先 IRI → 分类索引映射。
	 */
	private static final Map<String, Integer> ROOT_CATEGORY_MAP = Map.of(
			"http://example.org/standard-ontology#StandardizationObject", 0,
			"http://example.org/standard-ontology#StructuralElement", 1,
			"http://example.org/standard-ontology#Object", 2,
			"http://example.org/standard-ontology#ActionClass", 3,
			"http://example.org/standard-ontology#RepresentationForm", 4,
			"http://example.org/standard-ontology#Term", 5);

	@Override
	public GraphDataVO buildSchemaGraph(Long ontologyId) {
		Long ontId = ontologyId != null ? ontologyId : CORE_ONTOLOGY_ID;

		// 1. 加载所有实体类型
		List<OntEntityType> types = entityTypeMapper.selectList(
				Wrappers.<OntEntityType>lambdaQuery().eq(OntEntityType::getOntologyId, ontId));
		Map<Long, OntEntityType> typeById = types.stream()
				.collect(Collectors.toMap(OntEntityType::getId, t -> t, (a, b) -> a));

		// 2. 加载命名空间
		Map<Long, OntNamespace> nsById = loadNamespaces();

		// 3. 加载中文标签
		Map<Long, String> labelMap = loadLabels(typeById.keySet());

		// 4. 构建节点
		List<GraphNodeVO> nodes = types.stream().map(t -> {
			GraphNodeVO node = new GraphNodeVO();
			node.setId(t.getIri());
			node.setLabel(labelMap.getOrDefault(t.getId(), t.getName()));
			node.setName(t.getName());
			node.setLayer("SCHEMA");
			node.setNodeType("ENTITY_TYPE");
			node.setEntityTypeId(t.getId());
			node.setIsBuiltin("1".equals(t.getIsBuiltin()));
			node.setIsAbstract("1".equals(t.getIsAbstract()));
			node.setDefinition(t.getDefinition());
			OntNamespace ns = nsById.get(t.getNamespaceId());
			node.setNamespacePrefix(ns != null ? ns.getPrefix() : null);
			return node;
		}).collect(Collectors.toList());

		// 5. 构建边
		List<GraphEdgeVO> edges = new ArrayList<>();
		edges.addAll(buildSubclassEdges(typeById));
		edges.addAll(buildObjectPropertyEdges(types, ontId));
		edges.addAll(buildEquivalentEdges(typeById));
		edges.addAll(buildDisjointEdges(typeById));

		// 6. 计算节点 category（按顶层祖先着色）
		Map<Long, Integer> categoryByTypeId = computeCategories(typeById);
		nodes.forEach(n -> n.setCategory(categoryByTypeId.getOrDefault(n.getEntityTypeId(), 6)));

		// 7. 统计每个实体类型的数据属性数和实例数
		Map<Long, Integer> dataPropCountByType = countDataPropertiesByDomain(typeById.keySet());
		Map<Long, Integer> instanceCountByType = countInstancesByType(ontId);
		Map<Long, Integer> axiomCountByType = countAxiomsByTarget(typeById.keySet());
		nodes.forEach(n -> {
			n.setDataPropertyCount(dataPropCountByType.getOrDefault(n.getEntityTypeId(), 0));
			n.setInstanceCount(instanceCountByType.getOrDefault(n.getEntityTypeId(), 0));
			n.setAxiomCount(axiomCountByType.getOrDefault(n.getEntityTypeId(), 0));
		});

		// 8. 计算节点 symbolSize（按度数）
		Map<String, Integer> degreeMap = computeDegree(nodes, edges);
		nodes.forEach(n -> {
			int degree = degreeMap.getOrDefault(n.getId(), 0);
			n.setSymbolSize(Math.min(60, 20 + degree * 3));
		});

		// 9. 组装返回
		GraphDataVO graph = new GraphDataVO();
		graph.setNodes(nodes);
		graph.setEdges(edges);
		graph.setCategories(new ArrayList<>(SCHEMA_CATEGORIES));
		graph.setSummary(buildSummary(types, edges, ontId));
		return graph;
	}

	@Override
	public GraphDataVO buildInstanceGraph(Long ontologyId, Long entityTypeId, Integer maxNodes) {
		Long ontId = ontologyId != null ? ontologyId : CORE_ONTOLOGY_ID;
		int threshold = maxNodes != null ? maxNodes : 500;

		// 1. 确定实例范围
		Set<Long> targetTypeIds = null;
		if (entityTypeId != null) {
			targetTypeIds = collectDescendants(entityTypeId);
		}

		// 2. 查询实例
		List<OntEntityInstance> instances;
		if (targetTypeIds != null) {
			instances = entityInstanceMapper.selectList(
					Wrappers.<OntEntityInstance>lambdaQuery()
							.eq(OntEntityInstance::getOntologyId, ontId)
							.in(OntEntityInstance::getRdfTypeId, targetTypeIds));
		}
		else {
			instances = entityInstanceMapper.selectList(
					Wrappers.<OntEntityInstance>lambdaQuery()
							.eq(OntEntityInstance::getOntologyId, ontId));
		}

		// 3. 摘要模式判断
		if (instances.size() > threshold) {
			return buildInstanceSummaryGraph(instances, ontId);
		}

		// 4. 构建实例节点
		Map<Long, String> instanceIdToIri = instances.stream()
				.collect(Collectors.toMap(OntEntityInstance::getId, OntEntityInstance::getIri, (a, b) -> a));
		Set<Long> typeIds = instances.stream()
				.map(OntEntityInstance::getRdfTypeId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<Long, OntEntityType> typeMap = typeIds.isEmpty() ? Map.of() :
				entityTypeMapper.selectBatchIds(typeIds).stream()
						.collect(Collectors.toMap(OntEntityType::getId, t -> t, (a, b) -> a));

		// 实体类型 IRI → category 映射
		Map<Long, Integer> categoryByTypeId = computeCategories(typeMap);

		List<GraphNodeVO> nodes = instances.stream().map(inst -> {
			GraphNodeVO node = new GraphNodeVO();
			node.setId(inst.getIri());
			node.setLabel(inst.getLabel() != null ? inst.getLabel() : inst.getIri());
			node.setName(inst.getIriLocalName());
			node.setLayer("INSTANCE");
			node.setNodeType("INSTANCE");
			node.setInstanceId(inst.getId());
			node.setIsBuiltin("1".equals(inst.getIsBuiltin()));
			OntEntityType type = typeMap.get(inst.getRdfTypeId());
			if (type != null) {
				node.setRdfTypeIri(type.getIri());
				node.setRdfTypeLabel(type.getName());
				node.setEntityTypeId(type.getId());
				node.setCategory(categoryByTypeId.getOrDefault(type.getId(), 6));
			}
			return node;
		}).collect(Collectors.toList());

		// 5. 构建实例关系边
		Set<Long> instanceIds = instances.stream().map(OntEntityInstance::getId).collect(Collectors.toSet());
		List<OntInstanceObjectRelation> relations = instanceObjectRelationMapper.selectList(
				Wrappers.<OntInstanceObjectRelation>lambdaQuery()
						.in(OntInstanceObjectRelation::getSubjectInstanceId, instanceIds));

		// 批量加载对象属性
		Set<Long> objPropIds = relations.stream()
				.map(OntInstanceObjectRelation::getObjectPropertyId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<Long, OntObjectProperty> objPropMap = objPropIds.isEmpty() ? Map.of() :
				objectPropertyMapper.selectBatchIds(objPropIds).stream()
						.collect(Collectors.toMap(OntObjectProperty::getId, p -> p, (a, b) -> a));

		// 构建边，过滤掉指向不在当前实例集合内的 INSTANCE 类型边
		List<GraphEdgeVO> edges = relations.stream()
				.filter(r -> {
					if ("INSTANCE".equals(r.getObjectKind())) {
						return instanceIds.contains(r.getObjectInstanceId());
					}
					return true; // ENTITY_TYPE 类型对象保留
				})
				.map(r -> {
					String sourceIri = instanceIdToIri.get(r.getSubjectInstanceId());
					String targetIri = null;
					if ("INSTANCE".equals(r.getObjectKind())) {
						targetIri = instanceIdToIri.get(r.getObjectInstanceId());
					}
					else if ("ENTITY_TYPE".equals(r.getObjectKind())) {
						OntEntityType type = typeMap.get(r.getObjectEntityTypeId());
						targetIri = type != null ? type.getIri() : null;
					}
					if (sourceIri == null || targetIri == null) {
						return null;
					}
					OntObjectProperty prop = objPropMap.get(r.getObjectPropertyId());
					String propIri = prop != null ? prop.getIri() : "relation";
					String propLabel = prop != null ? prop.getName() : "关联";

					GraphEdgeVO edge = new GraphEdgeVO();
					edge.setId(sourceIri + "-" + propIri + "-" + targetIri);
					edge.setSource(sourceIri);
					edge.setTarget(targetIri);
					edge.setLabel(propLabel);
					edge.setEdgeType("INSTANCE_RELATION");
					edge.setLineStyle("solid");
					edge.setColor("#909399");
					edge.setDirected(true);
					edge.setWidth(1);
					edge.setIsFunctional(prop != null && "1".equals(prop.getIsFunctional()));
					return edge;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		// 6. 节点 symbolSize
		Map<String, Integer> degreeMap = computeDegree(nodes, edges);
		nodes.forEach(n -> {
			int degree = degreeMap.getOrDefault(n.getId(), 0);
			n.setSymbolSize(Math.min(50, 18 + degree * 3));
		});

		// 7. 组装返回
		GraphDataVO graph = new GraphDataVO();
		graph.setNodes(nodes);
		graph.setEdges(edges);
		graph.setCategories(new ArrayList<>(SCHEMA_CATEGORIES));
		graph.setSummary(buildInstanceSummary(instances, edges));
		return graph;
	}

	@Override
	public GraphDataVO buildInstanceSubgraph(Long instanceId, Integer depth, String direction) {
		int actualDepth = Math.min(Math.max(depth != null ? depth : 1, 1), 3);
		String dir = direction != null ? direction : "BOTH";

		// BFS 遍历收集实例ID和关系
		Set<Long> visitedInstanceIds = new HashSet<>();
		Set<Long> frontier = new HashSet<>();
		frontier.add(instanceId);
		List<OntInstanceObjectRelation> allRelations = new ArrayList<>();

		for (int d = 0; d < actualDepth && !frontier.isEmpty(); d++) {
			Set<Long> nextFrontier = new HashSet<>();
			for (Long currentId : frontier) {
				if (!visitedInstanceIds.add(currentId)) {
					continue;
				}

				// 查询出边
				if ("OUTGOING".equals(dir) || "BOTH".equals(dir)) {
					List<OntInstanceObjectRelation> outgoing = instanceObjectRelationMapper.selectList(
							Wrappers.<OntInstanceObjectRelation>lambdaQuery()
									.eq(OntInstanceObjectRelation::getSubjectInstanceId, currentId));
					allRelations.addAll(outgoing);
					outgoing.stream()
							.filter(r -> "INSTANCE".equals(r.getObjectKind()))
							.map(OntInstanceObjectRelation::getObjectInstanceId)
							.filter(Objects::nonNull)
							.forEach(nextFrontier::add);
				}

				// 查询入边
				if ("INCOMING".equals(dir) || "BOTH".equals(dir)) {
					List<OntInstanceObjectRelation> incoming = instanceObjectRelationMapper.selectList(
							Wrappers.<OntInstanceObjectRelation>lambdaQuery()
									.eq(OntInstanceObjectRelation::getObjectInstanceId, currentId)
									.eq(OntInstanceObjectRelation::getObjectKind, "INSTANCE"));
					allRelations.addAll(incoming);
					incoming.stream()
							.map(OntInstanceObjectRelation::getSubjectInstanceId)
							.forEach(nextFrontier::add);
				}
			}
			frontier = nextFrontier;
			frontier.removeAll(visitedInstanceIds);
		}

		// 收集所有涉及的实例ID
		Set<Long> allInstanceIds = new HashSet<>(visitedInstanceIds);
		allInstanceIds.add(instanceId);
		for (OntInstanceObjectRelation r : allRelations) {
			allInstanceIds.add(r.getSubjectInstanceId());
			if ("INSTANCE".equals(r.getObjectKind()) && r.getObjectInstanceId() != null) {
				allInstanceIds.add(r.getObjectInstanceId());
			}
		}

		// 加载实例数据
		List<OntEntityInstance> instances = entityInstanceMapper.selectBatchIds(allInstanceIds);
		if (instances.isEmpty()) {
			GraphDataVO empty = new GraphDataVO();
			empty.setCategories(new ArrayList<>(SCHEMA_CATEGORIES));
			empty.setSummary(new GraphSummaryVO());
			return empty;
		}

		Map<Long, String> instanceIdToIri = instances.stream()
				.collect(Collectors.toMap(OntEntityInstance::getId, OntEntityInstance::getIri, (a, b) -> a));
		Set<Long> typeIds = instances.stream()
				.map(OntEntityInstance::getRdfTypeId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<Long, OntEntityType> typeMap = typeIds.isEmpty() ? Map.of() :
				entityTypeMapper.selectBatchIds(typeIds).stream()
						.collect(Collectors.toMap(OntEntityType::getId, t -> t, (a, b) -> a));
		Map<Long, Integer> categoryByTypeId = computeCategories(typeMap);

		// 构建节点
		List<GraphNodeVO> nodes = instances.stream().map(inst -> {
			GraphNodeVO node = new GraphNodeVO();
			node.setId(inst.getIri());
			node.setLabel(inst.getLabel() != null ? inst.getLabel() : inst.getIri());
			node.setName(inst.getIriLocalName());
			node.setLayer("INSTANCE");
			node.setNodeType("INSTANCE");
			node.setInstanceId(inst.getId());
			node.setIsBuiltin("1".equals(inst.getIsBuiltin()));
			OntEntityType type = typeMap.get(inst.getRdfTypeId());
			if (type != null) {
				node.setRdfTypeIri(type.getIri());
				node.setRdfTypeLabel(type.getName());
				node.setEntityTypeId(type.getId());
				node.setCategory(categoryByTypeId.getOrDefault(type.getId(), 6));
			}
			return node;
		}).collect(Collectors.toList());

		// 加载对象属性
		Set<Long> objPropIds = allRelations.stream()
				.map(OntInstanceObjectRelation::getObjectPropertyId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<Long, OntObjectProperty> objPropMap = objPropIds.isEmpty() ? Map.of() :
				objectPropertyMapper.selectBatchIds(objPropIds).stream()
						.collect(Collectors.toMap(OntObjectProperty::getId, p -> p, (a, b) -> a));

		// 构建边（去重）
		Map<String, GraphEdgeVO> edgeMap = new LinkedHashMap<>();
		for (OntInstanceObjectRelation r : allRelations) {
			String sourceIri = instanceIdToIri.get(r.getSubjectInstanceId());
			String targetIri = null;
			if ("INSTANCE".equals(r.getObjectKind())) {
				targetIri = instanceIdToIri.get(r.getObjectInstanceId());
			}
			else if ("ENTITY_TYPE".equals(r.getObjectKind())) {
				OntEntityType type = typeMap.get(r.getObjectEntityTypeId());
				targetIri = type != null ? type.getIri() : null;
			}
			if (sourceIri == null || targetIri == null) {
				continue;
			}
			OntObjectProperty prop = objPropMap.get(r.getObjectPropertyId());
			String propIri = prop != null ? prop.getIri() : "relation";
			String propLabel = prop != null ? prop.getName() : "关联";
			String edgeId = sourceIri + "-" + propIri + "-" + targetIri;
			if (edgeMap.containsKey(edgeId)) {
				continue;
			}

			GraphEdgeVO edge = new GraphEdgeVO();
			edge.setId(edgeId);
			edge.setSource(sourceIri);
			edge.setTarget(targetIri);
			edge.setLabel(propLabel);
			edge.setEdgeType("INSTANCE_RELATION");
			edge.setLineStyle("solid");
			edge.setColor("#909399");
			edge.setDirected(true);
			edge.setWidth(1);
			edge.setIsFunctional(prop != null && "1".equals(prop.getIsFunctional()));
			edgeMap.put(edgeId, edge);
		}

		List<GraphEdgeVO> edges = new ArrayList<>(edgeMap.values());

		// 节点 symbolSize
		Map<String, Integer> degreeMap = computeDegree(nodes, edges);
		nodes.forEach(n -> {
			int degree = degreeMap.getOrDefault(n.getId(), 0);
			n.setSymbolSize(Math.min(50, 18 + degree * 3));
		});

		GraphDataVO graph = new GraphDataVO();
		graph.setNodes(nodes);
		graph.setEdges(edges);
		graph.setCategories(new ArrayList<>(SCHEMA_CATEGORIES));
		graph.setSummary(buildInstanceSummary(instances, edges));
		return graph;
	}

	@Override
	public GraphDataVO buildEntityTypeSubgraph(Long entityTypeId, Boolean includeObjectProperties, Boolean includeAxioms) {
		if (entityTypeId == null) {
			GraphDataVO empty = new GraphDataVO();
			empty.setCategories(new ArrayList<>(SCHEMA_CATEGORIES));
			empty.setSummary(new GraphSummaryVO());
			return empty;
		}

		boolean includeProps = includeObjectProperties == null || includeObjectProperties;
		boolean includeAx = includeAxioms == null || includeAxioms;

		// 收集子类型
		Set<Long> typeIds = collectDescendants(entityTypeId);
		if (typeIds.isEmpty()) {
			GraphDataVO empty = new GraphDataVO();
			empty.setCategories(new ArrayList<>(SCHEMA_CATEGORIES));
			empty.setSummary(new GraphSummaryVO());
			return empty;
		}

		List<OntEntityType> types = entityTypeMapper.selectBatchIds(typeIds);
		Map<Long, OntEntityType> typeById = types.stream()
				.collect(Collectors.toMap(OntEntityType::getId, t -> t, (a, b) -> a));
		Map<Long, String> labelMap = loadLabels(typeById.keySet());

		// 构建节点
		List<GraphNodeVO> nodes = types.stream().map(t -> {
			GraphNodeVO node = new GraphNodeVO();
			node.setId(t.getIri());
			node.setLabel(labelMap.getOrDefault(t.getId(), t.getName()));
			node.setName(t.getName());
			node.setLayer("SCHEMA");
			node.setNodeType("ENTITY_TYPE");
			node.setEntityTypeId(t.getId());
			node.setIsBuiltin("1".equals(t.getIsBuiltin()));
			node.setIsAbstract("1".equals(t.getIsAbstract()));
			node.setDefinition(t.getDefinition());
			return node;
		}).collect(Collectors.toList());

		// 构建边
		List<GraphEdgeVO> edges = new ArrayList<>();
		edges.addAll(buildSubclassEdges(typeById));
		if (includeProps) {
			edges.addAll(buildObjectPropertyEdges(types, types.get(0).getOntologyId()));
		}
		if (includeAx) {
			edges.addAll(buildEquivalentEdges(typeById));
			edges.addAll(buildDisjointEdges(typeById));
		}

		// 着色
		Map<Long, Integer> categoryByTypeId = computeCategories(typeById);
		nodes.forEach(n -> n.setCategory(categoryByTypeId.getOrDefault(n.getEntityTypeId(), 6)));

		// 度数
		Map<String, Integer> degreeMap = computeDegree(nodes, edges);
		nodes.forEach(n -> {
			int degree = degreeMap.getOrDefault(n.getId(), 0);
			n.setSymbolSize(Math.min(60, 20 + degree * 3));
		});

		GraphDataVO graph = new GraphDataVO();
		graph.setNodes(nodes);
		graph.setEdges(edges);
		graph.setCategories(new ArrayList<>(SCHEMA_CATEGORIES));
		graph.setSummary(buildSummary(types, edges, types.get(0).getOntologyId()));
		return graph;
	}

	@Override
	public VisualizationStatsVO getStats(Long ontologyId) {
		Long ontId = ontologyId != null ? ontologyId : CORE_ONTOLOGY_ID;

		VisualizationStatsVO stats = new VisualizationStatsVO();
		stats.setOntologyId(ontId);

		// 本体工程名称
		OntOntologyProject project = ontologyProjectMapper.selectById(ontId);
		stats.setOntologyName(project != null ? project.getProjectName() : "未知");

		// 各表 COUNT
		stats.setEntityTypeCount(Math.toIntExact(entityTypeMapper.selectCount(
				Wrappers.<OntEntityType>lambdaQuery().eq(OntEntityType::getOntologyId, ontId))));
		stats.setDataPropertyCount(Math.toIntExact(dataPropertyMapper.selectCount(
				Wrappers.<OntDataProperty>lambdaQuery().eq(OntDataProperty::getOntologyId, ontId))));
		stats.setObjectPropertyCount(Math.toIntExact(objectPropertyMapper.selectCount(
				Wrappers.<OntObjectProperty>lambdaQuery().eq(OntObjectProperty::getOntologyId, ontId))));
		stats.setInstanceCount(Math.toIntExact(entityInstanceMapper.selectCount(
				Wrappers.<OntEntityInstance>lambdaQuery().eq(OntEntityInstance::getOntologyId, ontId))));
		stats.setAxiomRuleCount(Math.toIntExact(axiomRuleMapper.selectCount(
				Wrappers.<OntAxiomRule>lambdaQuery().eq(OntAxiomRule::getOntologyId, ontId))));

		// 最近校验报告
		List<OntValidationReport> reports = validationReportMapper.selectList(
				Wrappers.<OntValidationReport>lambdaQuery()
						.eq(OntValidationReport::getOntologyId, ontId)
						.orderByDesc(OntValidationReport::getTriggeredAt)
						.last("LIMIT 1"));
		if (!reports.isEmpty()) {
			OntValidationReport latest = reports.get(0);
			stats.setLatestValidationStatus(latest.getStatus() != null ? latest.getStatus() : "NONE");
			stats.setLatestValidationTime(latest.getTriggeredAt() != null
					? latest.getTriggeredAt().toString() : null);
			stats.setLatestViolationCount(latest.getViolationCount() != null
					? latest.getViolationCount() : 0);
		}
		else {
			stats.setLatestValidationStatus("NONE");
			stats.setLatestViolationCount(0);
		}

		// 导出次数
		stats.setExportCount(Math.toIntExact(serializationLogMapper.selectCount(
				Wrappers.<OntSerializationLog>lambdaQuery().eq(OntSerializationLog::getOntologyId, ontId))));

		// 扩展模块数
		stats.setExtensionModuleCount(Math.toIntExact(extensionModuleMapper.selectCount(
				Wrappers.<OntExtensionModule>lambdaQuery().eq(OntExtensionModule::getOntologyId, ontId))));

		// 实体类型分布（核心/扩展）
		List<NameValueVO> distribution = new ArrayList<>();
		List<OntEntityType> allTypes = entityTypeMapper.selectList(
				Wrappers.<OntEntityType>lambdaQuery().eq(OntEntityType::getOntologyId, ontId));
		long coreCount = allTypes.stream().filter(t -> "1".equals(t.getIsBuiltin())).count();
		long extCount = allTypes.stream().filter(t -> "0".equals(t.getIsBuiltin())).count();
		distribution.add(new NameValueVO("核心内置", (int) coreCount));
		distribution.add(new NameValueVO("扩展", (int) extCount));
		stats.setEntityTypeDistribution(distribution);

		// 实例分布（按实体类型分组，Top 10）
		List<OntEntityInstance> allInstances = entityInstanceMapper.selectList(
				Wrappers.<OntEntityInstance>lambdaQuery().eq(OntEntityInstance::getOntologyId, ontId));
		Map<Long, Long> instanceByType = allInstances.stream()
				.collect(Collectors.groupingBy(OntEntityInstance::getRdfTypeId, Collectors.counting()));
		List<NameValueVO> instanceDist = instanceByType.entrySet().stream()
				.sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
				.limit(10)
				.map(e -> {
					OntEntityType type = entityTypeMapper.selectById(e.getKey());
					String name = type != null ? type.getName() : "未知类型";
					return new NameValueVO(name, e.getValue().intValue());
				})
				.collect(Collectors.toList());
		stats.setInstanceDistribution(instanceDist);

		return stats;
	}

	// ==================== 私有辅助方法 ====================

	/**
	 * 构建继承边（SUBCLASS_OF）。
	 */
	private List<GraphEdgeVO> buildSubclassEdges(Map<Long, OntEntityType> typeById) {
		Set<Long> typeIds = typeById.keySet();
		if (typeIds.isEmpty()) {
			return List.of();
		}
		List<OntEntityTypeHierarchy> hierarchies = hierarchyMapper.selectList(
				Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
						.in(OntEntityTypeHierarchy::getChildId, typeIds));
		List<GraphEdgeVO> edges = new ArrayList<>();
		for (OntEntityTypeHierarchy h : hierarchies) {
			if (!typeIds.contains(h.getParentId())) {
				continue;
			}
			OntEntityType child = typeById.get(h.getChildId());
			OntEntityType parent = typeById.get(h.getParentId());
			if (child == null || parent == null) {
				continue;
			}
			GraphEdgeVO edge = new GraphEdgeVO();
			edge.setId(child.getIri() + "-subClassOf-" + parent.getIri());
			edge.setSource(child.getIri());
			edge.setTarget(parent.getIri());
			edge.setLabel("继承");
			edge.setEdgeType("SUBCLASS_OF");
			edge.setLineStyle("solid");
			edge.setColor("#409EFF");
			edge.setDirected(true);
			edge.setWidth(2);
			edges.add(edge);
		}
		return edges;
	}

	/**
	 * 构建对象属性边（domain → range）。
	 */
	private List<GraphEdgeVO> buildObjectPropertyEdges(List<OntEntityType> types, Long ontologyId) {
		Set<Long> typeIds = types.stream().map(OntEntityType::getId).collect(Collectors.toSet());
		Map<Long, String> iriById = types.stream()
				.collect(Collectors.toMap(OntEntityType::getId, OntEntityType::getIri, (a, b) -> a));

		List<OntObjectProperty> props = objectPropertyMapper.selectList(
				Wrappers.<OntObjectProperty>lambdaQuery().eq(OntObjectProperty::getOntologyId, ontologyId));

		List<GraphEdgeVO> edges = new ArrayList<>();
		for (OntObjectProperty prop : props) {
			List<OntObjectPropertyDomain> domains = objectPropertyDomainMapper.selectList(
					Wrappers.<OntObjectPropertyDomain>lambdaQuery()
							.eq(OntObjectPropertyDomain::getObjectPropertyId, prop.getId()));
			List<OntObjectPropertyRange> ranges = objectPropertyRangeMapper.selectList(
					Wrappers.<OntObjectPropertyRange>lambdaQuery()
							.eq(OntObjectPropertyRange::getObjectPropertyId, prop.getId()));

			for (OntObjectPropertyDomain domain : domains) {
				for (OntObjectPropertyRange range : ranges) {
					if (!typeIds.contains(domain.getEntityTypeId()) || !typeIds.contains(range.getEntityTypeId())) {
						continue;
					}
					String sourceIri = iriById.get(domain.getEntityTypeId());
					String targetIri = iriById.get(range.getEntityTypeId());
					GraphEdgeVO edge = new GraphEdgeVO();
					edge.setId(sourceIri + "-" + prop.getIri() + "-" + targetIri);
					edge.setSource(sourceIri);
					edge.setTarget(targetIri);
					edge.setLabel(prop.getName());
					edge.setEdgeType("OBJECT_PROPERTY");
					edge.setObjectPropertyId(prop.getId());
					edge.setLineStyle("solid");
					edge.setColor("#909399");
					edge.setDirected(true);
					edge.setWidth(1);
					edge.setIsFunctional("1".equals(prop.getIsFunctional()));
					edges.add(edge);
				}
			}
		}
		return edges;
	}

	/**
	 * 构建等价类边（EQUIVALENT）。
	 */
	private List<GraphEdgeVO> buildEquivalentEdges(Map<Long, OntEntityType> typeById) {
		Set<Long> typeIds = typeById.keySet();
		if (typeIds.isEmpty()) {
			return List.of();
		}
		List<OntEntityTypeEquivalent> equivalents = equivalentMapper.selectList(null);
		List<GraphEdgeVO> edges = new ArrayList<>();
		for (OntEntityTypeEquivalent eq : equivalents) {
			if (!typeIds.contains(eq.getEntityTypeId()) || !typeIds.contains(eq.getEquivalentId())) {
				continue;
			}
			OntEntityType a = typeById.get(eq.getEntityTypeId());
			OntEntityType b = typeById.get(eq.getEquivalentId());
			if (a == null || b == null) {
				continue;
			}
			GraphEdgeVO edge = new GraphEdgeVO();
			edge.setId(a.getIri() + "-equivalent-" + b.getIri());
			edge.setSource(a.getIri());
			edge.setTarget(b.getIri());
			edge.setLabel("等价");
			edge.setEdgeType("EQUIVALENT");
			edge.setLineStyle("solid");
			edge.setColor("#67C23A");
			edge.setDirected(false);
			edge.setWidth(2);
			edges.add(edge);
		}
		return edges;
	}

	/**
	 * 构建不相交边（DISJOINT）。
	 */
	private List<GraphEdgeVO> buildDisjointEdges(Map<Long, OntEntityType> typeById) {
		Set<Long> typeIds = typeById.keySet();
		if (typeIds.isEmpty()) {
			return List.of();
		}
		List<OntEntityTypeDisjoint> disjoints = disjointMapper.selectList(null);
		List<GraphEdgeVO> edges = new ArrayList<>();
		for (OntEntityTypeDisjoint d : disjoints) {
			if (!typeIds.contains(d.getTypeA()) || !typeIds.contains(d.getTypeB())) {
				continue;
			}
			OntEntityType a = typeById.get(d.getTypeA());
			OntEntityType b = typeById.get(d.getTypeB());
			if (a == null || b == null) {
				continue;
			}
			GraphEdgeVO edge = new GraphEdgeVO();
			edge.setId(a.getIri() + "-disjoint-" + b.getIri());
			edge.setSource(a.getIri());
			edge.setTarget(b.getIri());
			edge.setLabel("不相交");
			edge.setEdgeType("DISJOINT");
			edge.setLineStyle("dashed");
			edge.setColor("#F56C6C");
			edge.setDirected(false);
			edge.setWidth(2);
			edges.add(edge);
		}
		return edges;
	}

	/**
	 * 计算每个实体类型的顶层祖先，用于节点着色分类。
	 * BFS 向上追溯继承树，直到找到预设的根类型 IRI。
	 */
	private Map<Long, Integer> computeCategories(Map<Long, OntEntityType> typeById) {
		if (typeById.isEmpty()) {
			return Map.of();
		}
		// 一次性加载所有继承关系
		List<OntEntityTypeHierarchy> allHierarchies = hierarchyMapper.selectList(null);
		Map<Long, List<Long>> parentMap = allHierarchies.stream()
				.collect(Collectors.groupingBy(
						OntEntityTypeHierarchy::getChildId,
						Collectors.mapping(OntEntityTypeHierarchy::getParentId, Collectors.toList())));

		Map<Long, Integer> result = new HashMap<>();
		for (Long typeId : typeById.keySet()) {
			Set<Long> visited = new HashSet<>();
			Deque<Long> queue = new ArrayDeque<>();
			queue.add(typeId);
			Integer category = null;
			while (!queue.isEmpty() && category == null) {
				Long current = queue.poll();
				if (!visited.add(current)) {
					continue;
				}
				OntEntityType type = typeById.get(current);
				if (type == null) {
					// 尝试从数据库加载（子图模式可能不在 typeById 中）
					type = entityTypeMapper.selectById(current);
				}
				if (type == null) {
					continue;
				}
				category = ROOT_CATEGORY_MAP.get(type.getIri());
				if (category == null) {
					List<Long> parents = parentMap.get(current);
					if (parents != null) {
						queue.addAll(parents);
					}
				}
			}
			result.put(typeId, category != null ? category : 6);
		}
		return result;
	}

	/**
	 * 统计每个实体类型的数据属性数。
	 */
	private Map<Long, Integer> countDataPropertiesByDomain(Set<Long> typeIds) {
		if (typeIds.isEmpty()) {
			return Map.of();
		}
		List<OntDataProperty> props = dataPropertyMapper.selectList(
				Wrappers.<OntDataProperty>lambdaQuery()
						.in(OntDataProperty::getDomainEntityTypeId, typeIds));
		return props.stream()
				.collect(Collectors.groupingBy(
						OntDataProperty::getDomainEntityTypeId,
						Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
	}

	/**
	 * 统计每个实体类型的实例数。
	 */
	private Map<Long, Integer> countInstancesByType(Long ontologyId) {
		List<OntEntityInstance> instances = entityInstanceMapper.selectList(
				Wrappers.<OntEntityInstance>lambdaQuery().eq(OntEntityInstance::getOntologyId, ontologyId));
		return instances.stream()
				.filter(i -> i.getRdfTypeId() != null)
				.collect(Collectors.groupingBy(
						OntEntityInstance::getRdfTypeId,
						Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
	}

	/**
	 * 统计每个实体类型的公理约束数。
	 */
	private Map<Long, Integer> countAxiomsByTarget(Set<Long> typeIds) {
		if (typeIds.isEmpty()) {
			return Map.of();
		}
		List<OntAxiomRuleTarget> targets = axiomRuleTargetMapper.selectList(
				Wrappers.<OntAxiomRuleTarget>lambdaQuery()
						.eq(OntAxiomRuleTarget::getTargetType, "ENTITY_TYPE")
						.in(OntAxiomRuleTarget::getEntityTypeId, typeIds));
		return targets.stream()
				.filter(t -> t.getEntityTypeId() != null)
				.collect(Collectors.groupingBy(
						OntAxiomRuleTarget::getEntityTypeId,
						Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
	}

	/**
	 * 计算节点度数（出入边数）。
	 */
	private Map<String, Integer> computeDegree(List<GraphNodeVO> nodes, List<GraphEdgeVO> edges) {
		Map<String, Integer> degreeMap = new HashMap<>();
		for (GraphEdgeVO edge : edges) {
			degreeMap.merge(edge.getSource(), 1, Integer::sum);
			degreeMap.merge(edge.getTarget(), 1, Integer::sum);
		}
		return degreeMap;
	}

	/**
	 * 加载命名空间映射。
	 */
	private Map<Long, OntNamespace> loadNamespaces() {
		return namespaceMapper.selectList(null).stream()
				.collect(Collectors.toMap(OntNamespace::getId, n -> n, (a, b) -> a));
	}

	/**
	 * 加载实体类型的中文标签。
	 */
	private Map<Long, String> loadLabels(Set<Long> typeIds) {
		if (typeIds.isEmpty()) {
			return Map.of();
		}
		List<OntEntityTypeLabel> labels = labelMapper.selectList(
				Wrappers.<OntEntityTypeLabel>lambdaQuery()
						.in(OntEntityTypeLabel::getEntityTypeId, typeIds)
						.eq(OntEntityTypeLabel::getLocale, "zh"));
		return labels.stream()
				.collect(Collectors.toMap(OntEntityTypeLabel::getEntityTypeId, OntEntityTypeLabel::getLabel, (a, b) -> a));
	}

	/**
	 * 收集实体类型及其所有子类型（递归 BFS 向下）。
	 */
	private Set<Long> collectDescendants(Long rootTypeId) {
		Set<Long> result = new HashSet<>();
		Deque<Long> queue = new ArrayDeque<>();
		queue.add(rootTypeId);
		int depth = 0;
		while (!queue.isEmpty() && depth < 100) {
			Long current = queue.poll();
			if (result.add(current)) {
				List<OntEntityTypeHierarchy> children = hierarchyMapper.selectList(
						Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
								.eq(OntEntityTypeHierarchy::getParentId, current));
				for (OntEntityTypeHierarchy h : children) {
					queue.add(h.getChildId());
				}
			}
			depth++;
		}
		return result;
	}

	/**
	 * 构建实例摘要图（节点超阈值时按类型分组折叠）。
	 */
	private GraphDataVO buildInstanceSummaryGraph(List<OntEntityInstance> instances, Long ontologyId) {
		// 按 rdf:type 分组
		Map<Long, List<OntEntityInstance>> byType = instances.stream()
				.filter(i -> i.getRdfTypeId() != null)
				.collect(Collectors.groupingBy(OntEntityInstance::getRdfTypeId));

		Set<Long> typeIds = byType.keySet();
		Map<Long, OntEntityType> typeMap = typeIds.isEmpty() ? Map.of() :
				entityTypeMapper.selectBatchIds(typeIds).stream()
						.collect(Collectors.toMap(OntEntityType::getId, t -> t, (a, b) -> a));
		Map<Long, Integer> categoryByTypeId = computeCategories(typeMap);

		List<GraphNodeVO> nodes = byType.entrySet().stream().map(e -> {
			OntEntityType type = typeMap.get(e.getKey());
			GraphNodeVO node = new GraphNodeVO();
			node.setId(type != null ? type.getIri() : "type-" + e.getKey());
			node.setLabel((type != null ? type.getName() : "未知类型") + " (" + e.getValue().size() + ")");
			node.setName(type != null ? type.getName() : "unknown");
			node.setLayer("INSTANCE");
			node.setNodeType("INSTANCE");
			node.setEntityTypeId(e.getKey());
			node.setInstanceCount(e.getValue().size());
			node.setIsBuiltin(type != null && "1".equals(type.getIsBuiltin()));
			node.setRdfTypeIri(type != null ? type.getIri() : null);
			node.setRdfTypeLabel(type != null ? type.getName() : null);
			node.setCategory(categoryByTypeId.getOrDefault(e.getKey(), 6));
			node.setSymbolSize(Math.min(60, 25 + e.getValue().size() / 10));
			return node;
		}).collect(Collectors.toList());

		GraphSummaryVO summary = new GraphSummaryVO();
		summary.setNodeCount(instances.size());
		summary.setEdgeCount(0);
		summary.setInstanceCount(instances.size());

		GraphDataVO graph = new GraphDataVO();
		graph.setNodes(nodes);
		graph.setEdges(new ArrayList<>());
		graph.setCategories(new ArrayList<>(SCHEMA_CATEGORIES));
		graph.setSummary(summary);
		return graph;
	}

	/**
	 * 构建 Schema 图谱统计摘要。
	 */
	private GraphSummaryVO buildSummary(List<OntEntityType> types, List<GraphEdgeVO> edges, Long ontologyId) {
		GraphSummaryVO summary = new GraphSummaryVO();
		summary.setNodeCount(types.size());
		summary.setEdgeCount(edges.size());
		summary.setEntityTypeCount(types.size());
		summary.setInstanceCount(0);
		summary.setObjectPropertyCount(Math.toIntExact(objectPropertyMapper.selectCount(
				Wrappers.<OntObjectProperty>lambdaQuery().eq(OntObjectProperty::getOntologyId, ontologyId))));
		summary.setSubclassCount((int) edges.stream().filter(e -> "SUBCLASS_OF".equals(e.getEdgeType())).count());
		summary.setDisjointCount((int) edges.stream().filter(e -> "DISJOINT".equals(e.getEdgeType())).count());
		summary.setEquivalentCount((int) edges.stream().filter(e -> "EQUIVALENT".equals(e.getEdgeType())).count());
		summary.setAxiomRuleCount(Math.toIntExact(axiomRuleMapper.selectCount(
				Wrappers.<OntAxiomRule>lambdaQuery().eq(OntAxiomRule::getOntologyId, ontologyId))));
		summary.setDataPropertyCount(Math.toIntExact(dataPropertyMapper.selectCount(
				Wrappers.<OntDataProperty>lambdaQuery().eq(OntDataProperty::getOntologyId, ontologyId))));
		return summary;
	}

	/**
	 * 构建实例图谱统计摘要。
	 */
	private GraphSummaryVO buildInstanceSummary(List<OntEntityInstance> instances, List<GraphEdgeVO> edges) {
		GraphSummaryVO summary = new GraphSummaryVO();
		summary.setNodeCount(instances.size());
		summary.setEdgeCount(edges.size());
		summary.setEntityTypeCount(0);
		summary.setInstanceCount(instances.size());
		summary.setObjectPropertyCount(0);
		summary.setSubclassCount(0);
		summary.setDisjointCount(0);
		summary.setEquivalentCount(0);
		summary.setAxiomRuleCount(0);
		summary.setDataPropertyCount(0);
		return summary;
	}

}
