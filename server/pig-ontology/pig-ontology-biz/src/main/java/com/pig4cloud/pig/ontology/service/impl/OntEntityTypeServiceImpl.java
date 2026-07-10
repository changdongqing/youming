/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.exception.CheckedException;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.dto.OntEntityTypeCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntEntityTypeUpdateDTO;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeDisjoint;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeEquivalent;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeDisjointMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeEquivalentMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import com.pig4cloud.pig.ontology.service.OntEntityTypeService;
import com.pig4cloud.pig.ontology.vo.OntEntityTypeDetailVO;
import com.pig4cloud.pig.ontology.vo.OntEntityTypeTreeNode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 实体类型服务实现。
 *
 * @author youming
 */
@Service
@AllArgsConstructor
public class OntEntityTypeServiceImpl extends ServiceImpl<OntEntityTypeMapper, OntEntityType>
		implements OntEntityTypeService {

	private static final String BUILTIN = "1";

	private static final String EXTENSION = "0";

	private static final String ZH = "zh";

	private static final int MAX_DEPTH = 100;

	private static final Comparator<OntEntityType> TYPE_COMPARATOR = Comparator
		.comparing(OntEntityType::getSortOrder, Comparator.nullsLast(Integer::compareTo))
		.thenComparing(OntEntityType::getId);

	private final OntEntityTypeLabelMapper labelMapper;

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	private final OntEntityTypeDisjointMapper disjointMapper;

	private final OntEntityTypeEquivalentMapper equivalentMapper;

	private final OntNamespaceMapper namespaceMapper;

	private final OntOntologyProjectMapper ontologyProjectMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntEntityType> saveEntityType(OntEntityTypeCreateDTO request) {
		Long ontologyId = request.getOntologyId() == null ? CORE_ONTOLOGY_ID : request.getOntologyId();
		ValidationContext context = validateSemanticFields(ontologyId, request.getNamespaceId(), request.getName(),
			request.getIri(), null, true);
		if (context.error() != null) {
			return R.failed(context.error());
		}

		List<Long> parentIds = normalizeParentIds(request.getParentIds());
		String parentError = validateParents(null, ontologyId, parentIds);
		if (parentError != null) {
			return R.failed(parentError);
		}

		OntEntityType entityType = new OntEntityType();
		entityType.setIri(context.expectedIri());
		entityType.setName(request.getName());
		entityType.setDefinition(request.getDefinition());
		entityType.setIsAbstract(StringUtils.hasText(request.getIsAbstract()) ? request.getIsAbstract() : EXTENSION);
		entityType.setIsBuiltin(EXTENSION);
		entityType.setOntologyId(ontologyId);
		entityType.setNamespaceId(request.getNamespaceId());
		entityType.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
		entityType.setRemarks(request.getRemarks());
		this.save(entityType);
		saveLabel(entityType.getId(), ZH, request.getLabel());
		replaceParents(entityType.getId(), parentIds);
		return R.ok(entityType);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntEntityType> updateEntityType(OntEntityTypeUpdateDTO request) {
		OntEntityType old = this.getById(request.getId());
		if (old == null) {
			return R.failed("实体类型不存在");
		}

		if (BUILTIN.equals(old.getIsBuiltin())) {
			this.update(Wrappers.<OntEntityType>lambdaUpdate()
				.eq(OntEntityType::getId, old.getId())
				.set(OntEntityType::getDefinition, request.getDefinition())
				.set(OntEntityType::getSortOrder,
					request.getSortOrder() == null ? old.getSortOrder() : request.getSortOrder())
				.set(OntEntityType::getRemarks, request.getRemarks()));
			saveLabel(old.getId(), ZH, request.getLabel());
			return R.ok(this.getById(old.getId()));
		}

		if (!StringUtils.hasText(request.getName())) {
			return R.failed("英文名称不能为空");
		}
		if (request.getNamespaceId() == null) {
			return R.failed("命名空间不能为空");
		}
		ValidationContext context = validateSemanticFields(old.getOntologyId(), request.getNamespaceId(),
			request.getName(), request.getIri(), old.getId(), true);
		if (context.error() != null) {
			return R.failed(context.error());
		}

		List<Long> parentIds = normalizeParentIds(request.getParentIds());
		String parentError = validateParents(old.getId(), old.getOntologyId(), parentIds);
		if (parentError != null) {
			return R.failed(parentError);
		}

		this.update(Wrappers.<OntEntityType>lambdaUpdate()
			.eq(OntEntityType::getId, old.getId())
			.set(OntEntityType::getIri, context.expectedIri())
			.set(OntEntityType::getName, request.getName())
			.set(OntEntityType::getDefinition, request.getDefinition())
			.set(OntEntityType::getIsAbstract,
				StringUtils.hasText(request.getIsAbstract()) ? request.getIsAbstract() : EXTENSION)
			.set(OntEntityType::getNamespaceId, request.getNamespaceId())
			.set(OntEntityType::getSortOrder,
				request.getSortOrder() == null ? old.getSortOrder() : request.getSortOrder())
			.set(OntEntityType::getRemarks, request.getRemarks())
			.set(OntEntityType::getIsBuiltin, EXTENSION));
		saveLabel(old.getId(), ZH, request.getLabel());
		replaceParents(old.getId(), parentIds);
		return R.ok(this.getById(old.getId()));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> removeEntityType(Long id) {
		OntEntityType entityType = this.getById(id);
		if (entityType == null) {
			return R.failed("实体类型不存在");
		}
		if (BUILTIN.equals(entityType.getIsBuiltin())) {
			return R.failed("内置实体类型不可删除");
		}
		long childCount = hierarchyMapper.selectCount(Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
			.eq(OntEntityTypeHierarchy::getParentId, id));
		if (childCount > 0) {
			return R.failed("该实体类型存在子类，不能删除");
		}
		long equivalentCount = equivalentMapper.selectCount(Wrappers.<OntEntityTypeEquivalent>lambdaQuery()
			.eq(OntEntityTypeEquivalent::getEntityTypeId, id)
			.or()
			.eq(OntEntityTypeEquivalent::getEquivalentId, id));
		if (equivalentCount > 0) {
			return R.failed("该实体类型被等价关系引用，不能删除");
		}
		long disjointCount = disjointMapper.selectCount(Wrappers.<OntEntityTypeDisjoint>lambdaQuery()
			.eq(OntEntityTypeDisjoint::getTypeA, id)
			.or()
			.eq(OntEntityTypeDisjoint::getTypeB, id));
		if (disjointCount > 0) {
			return R.failed("该实体类型被不相交关系引用，不能删除");
		}
		long dataPropertyCount = dataPropertyMapper.selectCount(Wrappers.<OntDataProperty>lambdaQuery()
			.eq(OntDataProperty::getDomainEntityTypeId, id));
		if (dataPropertyCount > 0) {
			return R.failed("该实体类型被数据属性引用为定义域，不能删除");
		}
		hierarchyMapper.delete(Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
			.eq(OntEntityTypeHierarchy::getChildId, id));
		labelMapper.delete(Wrappers.<OntEntityTypeLabel>lambdaQuery()
			.eq(OntEntityTypeLabel::getEntityTypeId, id));
		return R.ok(this.removeById(id));
	}

	@Override
	public List<OntEntityTypeTreeNode> tree(Long ontologyId) {
		Long scopeId = ontologyId == null ? CORE_ONTOLOGY_ID : ontologyId;
		List<OntEntityType> types = this.list(Wrappers.<OntEntityType>lambdaQuery()
			.eq(OntEntityType::getOntologyId, scopeId)
			.orderByAsc(OntEntityType::getSortOrder)
			.orderByAsc(OntEntityType::getId));
		if (types.isEmpty()) {
			return List.of();
		}

		Set<Long> typeIds = types.stream().map(OntEntityType::getId).collect(Collectors.toSet());
		List<OntEntityTypeHierarchy> hierarchies = hierarchyMapper.selectList(
			Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
				.in(OntEntityTypeHierarchy::getParentId, typeIds)
				.in(OntEntityTypeHierarchy::getChildId, typeIds));
		Map<Long, List<Long>> childrenByParent = hierarchies.stream()
			.collect(Collectors.groupingBy(OntEntityTypeHierarchy::getParentId,
				Collectors.mapping(OntEntityTypeHierarchy::getChildId, Collectors.toList())));
		validateAcyclic(typeIds, childrenByParent);

		Map<Long, OntEntityType> typeMap = types.stream()
			.collect(Collectors.toMap(OntEntityType::getId, Function.identity()));
		for (List<Long> childIds : childrenByParent.values()) {
			childIds.sort(Comparator.comparing(typeMap::get, TYPE_COMPARATOR));
		}
		Map<Long, String> labelMap = labelMapper.selectList(Wrappers.<OntEntityTypeLabel>lambdaQuery()
			.eq(OntEntityTypeLabel::getLocale, ZH)
			.in(OntEntityTypeLabel::getEntityTypeId, typeIds))
			.stream()
			.collect(Collectors.toMap(OntEntityTypeLabel::getEntityTypeId, OntEntityTypeLabel::getLabel));
		Set<Long> hasParent = hierarchies.stream().map(OntEntityTypeHierarchy::getChildId).collect(Collectors.toSet());
		List<Long> roots = types.stream().map(OntEntityType::getId).filter(id -> !hasParent.contains(id)).toList();
		if (roots.isEmpty()) {
			throw new CheckedException("实体类型继承图不存在根节点，请检查循环关系");
		}

		List<OntEntityTypeTreeNode> result = new ArrayList<>();
		Set<Long> reachable = new HashSet<>();
		for (Long rootId : roots) {
			result.add(buildTreeNode(rootId, String.valueOf(rootId), 0, typeMap, labelMap, childrenByParent,
				new HashSet<>(), reachable));
		}
		if (reachable.size() != typeIds.size()) {
			throw new CheckedException("实体类型继承图存在不可达节点，请检查循环或跨工程关系");
		}
		return result;
	}

	@Override
	public OntEntityTypeDetailVO getDetail(Long id) {
		OntEntityType entityType = this.getById(id);
		if (entityType == null) {
			return null;
		}
		OntEntityTypeDetailVO detail = new OntEntityTypeDetailVO();
		detail.setEntityType(entityType);
		detail.setLabels(labelMapper.selectList(Wrappers.<OntEntityTypeLabel>lambdaQuery()
			.eq(OntEntityTypeLabel::getEntityTypeId, id)
			.orderByAsc(OntEntityTypeLabel::getLocale)));

		List<Long> parentIds = hierarchyMapper.selectList(Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
			.eq(OntEntityTypeHierarchy::getChildId, id)).stream()
			.map(OntEntityTypeHierarchy::getParentId)
			.toList();
		detail.setParentIds(parentIds);
		detail.setParents(orderedTypes(parentIds));

		List<Long> childIds = hierarchyMapper.selectList(Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
			.eq(OntEntityTypeHierarchy::getParentId, id)).stream()
			.map(OntEntityTypeHierarchy::getChildId)
			.toList();
		detail.setChildIds(childIds);
		detail.setChildren(orderedTypes(childIds));

		List<Long> equivalentIds = equivalentMapper.selectList(Wrappers.<OntEntityTypeEquivalent>lambdaQuery()
			.eq(OntEntityTypeEquivalent::getEntityTypeId, id)
			.or()
			.eq(OntEntityTypeEquivalent::getEquivalentId, id)).stream()
			.map(relation -> Objects.equals(relation.getEntityTypeId(), id)
				? relation.getEquivalentId() : relation.getEntityTypeId())
			.distinct()
			.toList();
		detail.setEquivalents(orderedTypes(equivalentIds));

		List<Long> disjointIds = disjointMapper.selectList(Wrappers.<OntEntityTypeDisjoint>lambdaQuery()
			.eq(OntEntityTypeDisjoint::getTypeA, id)
			.or()
			.eq(OntEntityTypeDisjoint::getTypeB, id)).stream()
			.map(relation -> Objects.equals(relation.getTypeA(), id) ? relation.getTypeB() : relation.getTypeA())
			.distinct()
			.toList();
		detail.setDisjoints(orderedTypes(disjointIds));
		detail.setNamespace(namespaceMapper.selectById(entityType.getNamespaceId()));
		return detail;
	}

	private ValidationContext validateSemanticFields(Long ontologyId, Long namespaceId, String name, String requestIri,
			Long excludeId, boolean requireExtensionNamespace) {
		OntOntologyProject ontology = ontologyProjectMapper.selectById(ontologyId);
		if (ontology == null) {
			return ValidationContext.failed("本体工程不存在");
		}
		OntNamespace namespace = namespaceMapper.selectById(namespaceId);
		if (namespace == null) {
			return ValidationContext.failed("命名空间不存在");
		}
		if (requireExtensionNamespace && BUILTIN.equals(namespace.getIsBuiltin())) {
			return ValidationContext.failed("扩展实体类型必须使用扩展命名空间");
		}
		String expectedIri = namespace.getUri() + name;
		if (StringUtils.hasText(requestIri) && !expectedIri.equals(requestIri)) {
			return ValidationContext.failed("IRI必须等于命名空间URI与英文名称的拼接结果");
		}
		long iriCount = this.count(Wrappers.<OntEntityType>lambdaQuery()
			.eq(OntEntityType::getIri, expectedIri)
			.ne(excludeId != null, OntEntityType::getId, excludeId));
		if (iriCount > 0) {
			return ValidationContext.failed("IRI已存在");
		}
		long nameCount = this.count(Wrappers.<OntEntityType>lambdaQuery()
			.eq(OntEntityType::getOntologyId, ontologyId)
			.eq(OntEntityType::getNamespaceId, namespaceId)
			.eq(OntEntityType::getName, name)
			.ne(excludeId != null, OntEntityType::getId, excludeId));
		if (nameCount > 0) {
			return ValidationContext.failed("同一命名空间下英文名称已存在");
		}
		return new ValidationContext(expectedIri, null);
	}

	private List<Long> normalizeParentIds(List<Long> parentIds) {
		if (parentIds == null || parentIds.isEmpty()) {
			return List.of();
		}
		return new ArrayList<>(new LinkedHashSet<>(parentIds));
	}

	private String validateParents(Long childId, Long ontologyId, List<Long> parentIds) {
		if (parentIds.isEmpty()) {
			return null;
		}
		List<OntEntityType> parents = this.listByIds(parentIds);
		if (parents.size() != parentIds.size()) {
			return "父类实体类型不存在或已删除";
		}
		for (OntEntityType parent : parents) {
			if (!Objects.equals(parent.getOntologyId(), ontologyId)) {
				return "父类必须与当前实体类型属于同一本体工程";
			}
			if (Objects.equals(parent.getId(), childId)) {
				return "实体类型不能继承自身";
			}
		}
		if (childId == null) {
			return null;
		}

		Map<Long, List<Long>> parentsByChild = hierarchyMapper.selectList(null).stream()
			.collect(Collectors.groupingBy(OntEntityTypeHierarchy::getChildId,
				Collectors.mapping(OntEntityTypeHierarchy::getParentId, Collectors.toList())));
		for (Long parentId : parentIds) {
			String error = checkParentReachability(childId, parentId, parentsByChild);
			if (error != null) {
				return error;
			}
		}
		return null;
	}

	private String checkParentReachability(Long childId, Long parentId, Map<Long, List<Long>> parentsByChild) {
		Deque<NodeDepth> queue = new ArrayDeque<>();
		queue.add(new NodeDepth(parentId, 0));
		Set<Long> visited = new HashSet<>();
		while (!queue.isEmpty()) {
			NodeDepth current = queue.removeFirst();
			if (current.depth() > MAX_DEPTH) {
				return "继承层次超过100层，拒绝保存";
			}
			if (Objects.equals(current.id(), childId)) {
				return "继承关系存在环，不允许创建循环继承";
			}
			if (!visited.add(current.id())) {
				continue;
			}
			for (Long ancestorId : parentsByChild.getOrDefault(current.id(), List.of())) {
				queue.addLast(new NodeDepth(ancestorId, current.depth() + 1));
			}
		}
		return null;
	}

	private void replaceParents(Long entityTypeId, List<Long> parentIds) {
		hierarchyMapper.delete(Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
			.eq(OntEntityTypeHierarchy::getChildId, entityTypeId));
		for (Long parentId : parentIds) {
			OntEntityTypeHierarchy hierarchy = new OntEntityTypeHierarchy();
			hierarchy.setParentId(parentId);
			hierarchy.setChildId(entityTypeId);
			hierarchyMapper.insert(hierarchy);
		}
	}

	private void saveLabel(Long entityTypeId, String locale, String label) {
		int updated = labelMapper.update(null, Wrappers.<OntEntityTypeLabel>lambdaUpdate()
			.eq(OntEntityTypeLabel::getEntityTypeId, entityTypeId)
			.eq(OntEntityTypeLabel::getLocale, locale)
			.set(OntEntityTypeLabel::getLabel, label));
		if (updated == 0) {
			OntEntityTypeLabel newLabel = new OntEntityTypeLabel();
			newLabel.setEntityTypeId(entityTypeId);
			newLabel.setLocale(locale);
			newLabel.setLabel(label);
			labelMapper.insert(newLabel);
		}
	}

	private List<OntEntityType> orderedTypes(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		return this.listByIds(ids).stream().sorted(TYPE_COMPARATOR).toList();
	}

	private void validateAcyclic(Set<Long> typeIds, Map<Long, List<Long>> childrenByParent) {
		Map<Long, Integer> colors = new HashMap<>();
		for (Long typeId : typeIds) {
			visitForCycle(typeId, 0, childrenByParent, colors);
		}
	}

	private void visitForCycle(Long typeId, int depth, Map<Long, List<Long>> childrenByParent,
			Map<Long, Integer> colors) {
		if (depth > MAX_DEPTH) {
			throw new CheckedException("实体类型继承层次超过100层");
		}
		Integer color = colors.getOrDefault(typeId, 0);
		if (color == 1) {
			throw new CheckedException("实体类型继承图存在循环关系");
		}
		if (color == 2) {
			return;
		}
		colors.put(typeId, 1);
		for (Long childId : childrenByParent.getOrDefault(typeId, List.of())) {
			visitForCycle(childId, depth + 1, childrenByParent, colors);
		}
		colors.put(typeId, 2);
	}

	private OntEntityTypeTreeNode buildTreeNode(Long id, String pathKey, int depth,
			Map<Long, OntEntityType> typeMap, Map<Long, String> labelMap, Map<Long, List<Long>> childrenByParent,
			Set<Long> currentPath, Set<Long> reachable) {
		if (depth > MAX_DEPTH) {
			throw new CheckedException("实体类型继承层次超过100层");
		}
		if (!currentPath.add(id)) {
			throw new CheckedException("实体类型继承图存在循环关系");
		}
		OntEntityType type = typeMap.get(id);
		if (type == null) {
			throw new CheckedException("实体类型继承关系引用了不存在的节点: " + id);
		}
		reachable.add(id);
		OntEntityTypeTreeNode node = new OntEntityTypeTreeNode();
		node.setKey(pathKey);
		node.setId(type.getId());
		node.setLabel(labelMap.getOrDefault(type.getId(), type.getName()));
		node.setName(type.getName());
		node.setIri(type.getIri());
		node.setIsAbstract(type.getIsAbstract());
		node.setIsBuiltin(type.getIsBuiltin());
		node.setDefinition(type.getDefinition());
		for (Long childId : childrenByParent.getOrDefault(id, List.of())) {
			node.getChildren().add(buildTreeNode(childId, pathKey + "/" + childId, depth + 1, typeMap, labelMap,
				childrenByParent, new HashSet<>(currentPath), reachable));
		}
		return node;
	}

	private record ValidationContext(String expectedIri, String error) {

		private static ValidationContext failed(String error) {
			return new ValidationContext(null, error);
		}

	}

	private record NodeDepth(Long id, int depth) {
	}

}
