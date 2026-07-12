/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.export;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * 导出范围解析器。
 * <p>
 * 处理实例子树过滤：给定目标实体类型ID，递归收集所有子类型ID，
 * 然后加载这些类型的实例。
 * </p>
 *
 * @author youming
 */
@Component
@RequiredArgsConstructor
public class ExportScopeResolver {

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	private final OntEntityInstanceMapper instanceMapper;

	/**
	 * 加载实例列表（按导出范围过滤）。
	 * @param ontologyId 本体工程ID
	 * @param scope 导出范围
	 * @param targetTypeFilter 子树过滤的实体类型ID（scope=INSTANCE_SUBTREE 时非空）
	 * @return 实例列表
	 */
	public List<OntEntityInstance> loadInstances(Long ontologyId, ExportScope scope, Long targetTypeFilter) {
		if (scope == ExportScope.SCHEMA_ONLY) {
			return Collections.emptyList();
		}

		if (scope == ExportScope.INSTANCE_SUBTREE && targetTypeFilter != null) {
			Set<Long> typeIds = collectDescendants(targetTypeFilter);
			if (typeIds.isEmpty()) {
				return Collections.emptyList();
			}
			return instanceMapper.selectList(
				Wrappers.<OntEntityInstance>lambdaQuery()
					.eq(OntEntityInstance::getOntologyId, ontologyId)
					.in(OntEntityInstance::getRdfTypeId, typeIds)
					.eq(OntEntityInstance::getDelFlag, "0")
					.orderByAsc(OntEntityInstance::getSortOrder));
		}

		// FULL 或 INSTANCE_ONLY：加载全部实例
		return instanceMapper.selectList(
			Wrappers.<OntEntityInstance>lambdaQuery()
				.eq(OntEntityInstance::getOntologyId, ontologyId)
				.eq(OntEntityInstance::getDelFlag, "0")
				.orderByAsc(OntEntityInstance::getSortOrder));
	}

	/**
	 * 递归收集子类型ID（含自身）。
	 * @param rootTypeId 根类型ID
	 * @return 包含根类型及所有子类型的ID集合
	 */
	public Set<Long> collectDescendants(Long rootTypeId) {
		Set<Long> result = new LinkedHashSet<>();
		Queue<Long> queue = new LinkedList<>();
		queue.add(rootTypeId);
		int depth = 0;
		while (!queue.isEmpty() && depth < 100) {
			Long current = queue.poll();
			if (result.add(current)) {
				List<OntEntityTypeHierarchy> children = hierarchyMapper.selectList(
					Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
						.eq(OntEntityTypeHierarchy::getParentId, current));
				children.forEach(c -> queue.add(c.getChildId()));
			}
			depth++;
		}
		return result;
	}

}
