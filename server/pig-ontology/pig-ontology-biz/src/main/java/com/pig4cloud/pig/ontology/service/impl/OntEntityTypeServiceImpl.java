/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeDisjoint;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeEquivalent;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeDisjointMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeEquivalentMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.service.OntEntityTypeService;
import com.pig4cloud.pig.ontology.vo.OntEntityTypeTreeNode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
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

	private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");

	private static final int MAX_DEPTH = 100;

	private final OntEntityTypeLabelMapper labelMapper;

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	private final OntEntityTypeDisjointMapper disjointMapper;

	private final OntEntityTypeEquivalentMapper equivalentMapper;

	private final OntNamespaceMapper namespaceMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntEntityType> saveEntityType(OntEntityType entityType) {
		R<OntEntityType> validation = validateEntityType(entityType, false);
		if (validation.getCode() != 0) {
			return validation;
		}
		entityType.setId(null);
		entityType.setIsBuiltin(EXTENSION);
		if (!StringUtils.hasText(entityType.getIsAbstract())) {
			entityType.setIsAbstract(EXTENSION);
		}
		if (entityType.getSortOrder() == null) {
			entityType.setSortOrder(0);
		}
		this.save(entityType);
		saveRelations(entityType);
		return R.ok(entityType);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntEntityType> updateEntityType(OntEntityType entityType) {
		if (entityType.getId() == null) {
			return R.failed("实体类型ID不能为空");
		}
		OntEntityType old = this.getById(entityType.getId());
		if (old == null) {
			return R.failed("实体类型不存在");
		}

		if (BUILTIN.equals(old.getIsBuiltin())) {
			OntEntityType update = new OntEntityType();
			update.setId(old.getId());
			update.setDefinition(entityType.getDefinition());
			update.setSortOrder(entityType.getSortOrder());
			update.setRemarks(entityType.getRemarks());
			this.updateById(update);
			if (StringUtils.hasText(entityType.getLabel())) {
				saveLabel(entityType.getId(), "zh", entityType.getLabel());
			}
			return R.ok(this.getById(old.getId()));
		}

		R<OntEntityType> validation = validateEntityType(entityType, true);
		if (validation.getCode() != 0) {
			return validation;
		}
		entityType.setIsBuiltin(EXTENSION);
		this.updateById(entityType);
		saveRelations(entityType);
		if (StringUtils.hasText(entityType.getLabel())) {
			saveLabel(entityType.getId(), "zh", entityType.getLabel());
		}
		return R.ok(this.getById(entityType.getId()));
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
		long equivCount = equivalentMapper.selectCount(Wrappers.<OntEntityTypeEquivalent>lambdaQuery()
			.eq(OntEntityTypeEquivalent::getEntityTypeId, id)
			.or()
			.eq(OntEntityTypeEquivalent::getEquivalentId, id));
		if (equivCount > 0) {
			return R.failed("该实体类型被等价关系引用，不能删除");
		}
		long disjointCount = disjointMapper.selectCount(Wrappers.<OntEntityTypeDisjoint>lambdaQuery()
			.eq(OntEntityTypeDisjoint::getTypeA, id)
			.or()
			.eq(OntEntityTypeDisjoint::getTypeB, id));
		if (disjointCount > 0) {
			return R.failed("该实体类型被不相交关系引用，不能删除");
		}
		hierarchyMapper.delete(Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
			.eq(OntEntityTypeHierarchy::getChildId, id));
		labelMapper.delete(Wrappers.<OntEntityTypeLabel>lambdaQuery()
			.eq(OntEntityTypeLabel::getEntityTypeId, id));
		return R.ok(this.removeById(id));
	}

	@Override
	public List<OntEntityTypeTreeNode> tree() {
		List<OntEntityType> types = this.list(Wrappers.<OntEntityType>lambdaQuery()
			.orderByAsc(OntEntityType::getSortOrder)
			.orderByAsc(OntEntityType::getId));
		List<OntEntityTypeLabel> labels = labelMapper.selectList(Wrappers.<OntEntityTypeLabel>lambdaQuery()
			.eq(OntEntityTypeLabel::getLocale, "zh"));
		List<OntEntityTypeHierarchy> hierarchies = hierarchyMapper.selectList(null);

		Map<Long, String> labelMap = labels.stream()
			.collect(Collectors.toMap(OntEntityTypeLabel::getEntityTypeId, OntEntityTypeLabel::getLabel, (a, b) -> a));
		Map<Long, List<Long>> childrenMap = hierarchies.stream()
			.collect(Collectors.groupingBy(OntEntityTypeHierarchy::getParentId,
				Collectors.mapping(OntEntityTypeHierarchy::getChildId, Collectors.toList())));
		Set<Long> hasParent = hierarchies.stream()
			.map(OntEntityTypeHierarchy::getChildId)
			.collect(Collectors.toSet());

		Map<Long, OntEntityTypeTreeNode> nodeMap = new HashMap<>();
		for (OntEntityType type : types) {
			OntEntityTypeTreeNode node = new OntEntityTypeTreeNode();
			node.setId(type.getId());
			node.setLabel(labelMap.getOrDefault(type.getId(), type.getName()));
			node.setName(type.getName());
			node.setIri(type.getIri());
			node.setIsAbstract(type.getIsAbstract());
			node.setIsBuiltin(type.getIsBuiltin());
			node.setDefinition(type.getDefinition());
			nodeMap.put(type.getId(), node);
		}

		for (OntEntityTypeHierarchy h : hierarchies) {
			OntEntityTypeTreeNode parent = nodeMap.get(h.getParentId());
			OntEntityTypeTreeNode child = nodeMap.get(h.getChildId());
			if (parent != null && child != null) {
				parent.getChildren().add(child);
			}
		}

		return types.stream()
			.filter(t -> !hasParent.contains(t.getId()))
			.map(t -> nodeMap.get(t.getId()))
			.filter(java.util.Objects::nonNull)
			.collect(Collectors.toList());
	}

	@Override
	public Map<String, Object> getDetail(Long id) {
		OntEntityType entityType = this.getById(id);
		if (entityType == null) {
			return null;
		}
		Map<String, Object> detail = new HashMap<>();
		detail.put("entityType", entityType);

		List<OntEntityTypeLabel> labels = labelMapper.selectList(Wrappers.<OntEntityTypeLabel>lambdaQuery()
			.eq(OntEntityTypeLabel::getEntityTypeId, id));
		detail.put("labels", labels);

		List<OntEntityTypeHierarchy> parents = hierarchyMapper.selectList(Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
			.eq(OntEntityTypeHierarchy::getChildId, id));
		List<Long> parentIds = parents.stream().map(OntEntityTypeHierarchy::getParentId).collect(Collectors.toList());
		detail.put("parentIds", parentIds);
		if (!parentIds.isEmpty()) {
			detail.put("parents", this.listByIds(parentIds));
		} else {
			detail.put("parents", List.of());
		}

		List<OntEntityTypeHierarchy> childrenRel = hierarchyMapper.selectList(Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
			.eq(OntEntityTypeHierarchy::getParentId, id));
		List<Long> childIds = childrenRel.stream().map(OntEntityTypeHierarchy::getChildId).collect(Collectors.toList());
		detail.put("childIds", childIds);
		if (!childIds.isEmpty()) {
			detail.put("children", this.listByIds(childIds));
		} else {
			detail.put("children", List.of());
		}

		List<OntEntityTypeEquivalent> equivs = equivalentMapper.selectList(Wrappers.<OntEntityTypeEquivalent>lambdaQuery()
			.eq(OntEntityTypeEquivalent::getEntityTypeId, id));
		List<Long> equivIds = equivs.stream().map(OntEntityTypeEquivalent::getEquivalentId).collect(Collectors.toList());
		if (!equivIds.isEmpty()) {
			detail.put("equivalents", this.listByIds(equivIds));
		} else {
			detail.put("equivalents", List.of());
		}

		List<OntEntityTypeDisjoint> disjoints = disjointMapper.selectList(Wrappers.<OntEntityTypeDisjoint>lambdaQuery()
			.eq(OntEntityTypeDisjoint::getTypeA, id)
			.or()
			.eq(OntEntityTypeDisjoint::getTypeB, id));
		List<Long> disjointIds = disjoints.stream()
			.flatMap(d -> {
				List<Long> ids = new ArrayList<>();
				if (d.getTypeA().equals(id)) {
					ids.add(d.getTypeB());
				} else {
					ids.add(d.getTypeA());
				}
				return ids.stream();
			})
			.collect(Collectors.toList());
		if (!disjointIds.isEmpty()) {
			detail.put("disjoints", this.listByIds(disjointIds));
		} else {
			detail.put("disjoints", List.of());
		}

		OntNamespace namespace = namespaceMapper.selectById(entityType.getNamespaceId());
		detail.put("namespace", namespace);

		return detail;
	}

	private R<OntEntityType> validateEntityType(OntEntityType entityType, boolean edit) {
		if (!StringUtils.hasText(entityType.getName())) {
			return R.failed("英文名称不能为空");
		}
		if (!NAME_PATTERN.matcher(entityType.getName()).matches()) {
			return R.failed("英文名称必须以大写字母开头，仅支持英文字母和数字");
		}
		if (entityType.getNamespaceId() == null) {
			return R.failed("命名空间不能为空");
		}
		OntNamespace namespace = namespaceMapper.selectById(entityType.getNamespaceId());
		if (namespace == null) {
			return R.failed("命名空间不存在");
		}

		if (!StringUtils.hasText(entityType.getIri())) {
			entityType.setIri(namespace.getUri() + entityType.getName());
		}

		long iriCount = this.count(Wrappers.<OntEntityType>lambdaQuery()
			.eq(OntEntityType::getIri, entityType.getIri())
			.ne(edit && entityType.getId() != null, OntEntityType::getId, entityType.getId()));
		if (iriCount > 0) {
			return R.failed("IRI已存在");
		}

		long nameCount = this.count(Wrappers.<OntEntityType>lambdaQuery()
			.eq(OntEntityType::getName, entityType.getName())
			.ne(edit && entityType.getId() != null, OntEntityType::getId, entityType.getId()));
		if (nameCount > 0) {
			return R.failed("英文名称已存在");
		}

		if (entityType.getParentIds() != null && !entityType.getParentIds().isEmpty()) {
			for (Long parentId : entityType.getParentIds()) {
				OntEntityType parent = this.getById(parentId);
				if (parent == null) {
					return R.failed("父类实体类型不存在: " + parentId);
				}
			}
			if (hasCycle(entityType.getId(), entityType.getParentIds())) {
				return R.failed("继承关系存在环，不允许创建循环继承");
			}
		}
		return R.ok(entityType);
	}

	private boolean hasCycle(Long childId, List<Long> parentIds) {
		Set<Long> visited = new HashSet<>();
		List<Long> queue = new ArrayList<>(parentIds);
		int depth = 0;
		while (!queue.isEmpty() && depth < MAX_DEPTH) {
			Long current = queue.remove(0);
			if (current.equals(childId)) {
				return true;
			}
			if (visited.contains(current)) {
				continue;
			}
			visited.add(current);
			List<OntEntityTypeHierarchy> parents = hierarchyMapper.selectList(Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
				.eq(OntEntityTypeHierarchy::getChildId, current));
			for (OntEntityTypeHierarchy h : parents) {
				queue.add(h.getParentId());
			}
			depth++;
		}
		return false;
	}

	private void saveRelations(OntEntityType entityType) {
		hierarchyMapper.delete(Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
			.eq(OntEntityTypeHierarchy::getChildId, entityType.getId()));
		if (entityType.getParentIds() != null) {
			for (Long parentId : entityType.getParentIds()) {
				OntEntityTypeHierarchy h = new OntEntityTypeHierarchy();
				h.setParentId(parentId);
				h.setChildId(entityType.getId());
				hierarchyMapper.insert(h);
			}
		}
	}

	private void saveLabel(Long entityTypeId, String locale, String label) {
		OntEntityTypeLabel existing = labelMapper.selectOne(Wrappers.<OntEntityTypeLabel>lambdaQuery()
			.eq(OntEntityTypeLabel::getEntityTypeId, entityTypeId)
			.eq(OntEntityTypeLabel::getLocale, locale));
		if (existing != null) {
			existing.setLabel(label);
			labelMapper.updateById(existing);
		} else {
			OntEntityTypeLabel newLabel = new OntEntityTypeLabel();
			newLabel.setEntityTypeId(entityTypeId);
			newLabel.setLocale(locale);
			newLabel.setLabel(label);
			labelMapper.insert(newLabel);
		}
	}

}
