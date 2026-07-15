/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyDomain;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyRange;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyDomainMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyRangeMapper;
import com.pig4cloud.pig.ontology.mapping.EntityMappingErrorCode;
import com.pig4cloud.pig.ontology.mapping.RelationMappingErrorCode;
import com.pig4cloud.pig.ontology.mapping.compiler.RelationKeyCompiler;
import com.pig4cloud.pig.ontology.mapping.datasource.entity.OntDataSource;
import com.pig4cloud.pig.ontology.mapping.datasource.mapper.OntDataSourceMapper;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.mapper.OntEntityMappingMapper;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingProjectMapper;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 关系映射校验器（18-05 §5）。
 * <p>
 * 静态校验规则：
 * <ul>
 *   <li>主体和客体实体映射存在且属于同一映射版本</li>
 *   <li>SELF_REFERENCE 模式要求主体 == 客体映射</li>
 *   <li>对象属性存在且属于映射工程关联的本体</li>
 *   <li>主体类型满足对象属性 domain（含继承）</li>
 *   <li>客体类型满足对象属性 range（含继承）</li>
 *   <li>数据源存在</li>
 *   <li>键映射 JSON 格式合法</li>
 * </ul>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelationMappingValidator {

	private final OntEntityMappingMapper entityMappingMapper;

	private final OntMappingVersionMapper versionMapper;

	private final OntMappingProjectMapper projectMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	private final OntObjectPropertyDomainMapper domainMapper;

	private final OntObjectPropertyRangeMapper rangeMapper;

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	private final OntDataSourceMapper dataSourceMapper;

	private final RelationKeyCompiler relationKeyCompiler;

	/**
	 * 校验关系映射配置。
	 * @param relationMode 关系模式
	 * @param objectPropertyId 对象属性ID
	 * @param subjectEntityMappingId 主体实体映射ID
	 * @param objectEntityMappingId 客体实体映射ID
	 * @param sourceId 数据源ID
	 * @param subjectKeyMapping 主体键映射JSON
	 * @param objectKeyMapping 客体键映射JSON
	 * @param relationKeyColumns 关系键列JSON
	 * @param mappingVersionId 映射版本ID
	 * @throws IllegalArgumentException 如果校验失败
	 */
	public void validate(String relationMode, Long objectPropertyId, Long subjectEntityMappingId,
			Long objectEntityMappingId, Long sourceId, String subjectKeyMapping, String objectKeyMapping,
			String relationKeyColumns, Long mappingVersionId) {
		// 1. 校验关系模式合法
		if (!"FOREIGN_KEY".equals(relationMode) && !"SELF_REFERENCE".equals(relationMode)
				&& !"JOIN_TABLE".equals(relationMode)) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_005.getMessage());
		}

		// 2. 校验主体实体映射存在且属于同一版本
		OntEntityMapping subjectMapping = entityMappingMapper.selectById(subjectEntityMappingId);
		if (subjectMapping == null || "1".equals(subjectMapping.getDelFlag())) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_006.getMessage());
		}
		if (!subjectMapping.getMappingVersionId().equals(mappingVersionId)) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_008.getMessage());
		}

		// 3. 校验客体实体映射存在且属于同一版本
		OntEntityMapping objectMapping = entityMappingMapper.selectById(objectEntityMappingId);
		if (objectMapping == null || "1".equals(objectMapping.getDelFlag())) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_007.getMessage());
		}
		if (!objectMapping.getMappingVersionId().equals(mappingVersionId)) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_008.getMessage());
		}

		// 4. SELF_REFERENCE 模式要求主体 == 客体
		if ("SELF_REFERENCE".equals(relationMode)
				&& !subjectEntityMappingId.equals(objectEntityMappingId)) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_012.getMessage());
		}

		// 5. 校验对象属性存在且属于映射工程关联的本体
		OntObjectProperty objectProperty = objectPropertyMapper.selectById(objectPropertyId);
		if (objectProperty == null || "1".equals(objectProperty.getDelFlag())) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_009.getMessage());
		}
		OntMappingVersion version = versionMapper.selectById(mappingVersionId);
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException("映射版本不存在");
		}
		OntMappingProject project = projectMapper.selectById(version.getMappingProjectId());
		if (project == null || "1".equals(project.getDelFlag())) {
			throw new IllegalArgumentException("映射工程不存在");
		}
		if (!objectProperty.getOntologyId().equals(project.getOntologyId())) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_009.getMessage()
					+ ": 对象属性不属于当前本体工程");
		}

		// 6. 校验 domain 包含主体实体类型（含继承）
		Long subjectEntityTypeId = subjectMapping.getTargetEntityTypeId();
		if (!isEntityInDomain(objectPropertyId, subjectEntityTypeId)) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_010.getMessage());
		}

		// 7. 校验 range 包含客体实体类型（含继承）
		Long objectEntityTypeId = objectMapping.getTargetEntityTypeId();
		if (!isEntityInRange(objectPropertyId, objectEntityTypeId)) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_011.getMessage());
		}

		// 8. 校验数据源存在
		OntDataSource dataSource = dataSourceMapper.selectById(sourceId);
		if (dataSource == null || "1".equals(dataSource.getDelFlag())) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_014.getMessage());
		}

		// 9. 校验键映射 JSON 格式合法
		if (relationKeyCompiler.extractColumnPairs(subjectKeyMapping).isEmpty()) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_013.getMessage()
					+ ": 主体键映射无法提取列对");
		}
		if (relationKeyCompiler.extractColumnPairs(objectKeyMapping).isEmpty()) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_013.getMessage()
					+ ": 客体键映射无法提取列对");
		}
	}

	/**
	 * 检查实体类型是否在对象属性的 domain 中（含继承）。
	 * @param objectPropertyId 对象属性ID
	 * @param entityTypeId 实体类型ID
	 * @return true 满足
	 */
	private boolean isEntityInDomain(Long objectPropertyId, Long entityTypeId) {
		// 查询 domain 列表
		List<OntObjectPropertyDomain> domains = domainMapper.selectList(
				Wrappers.<OntObjectPropertyDomain>lambdaQuery()
						.eq(OntObjectPropertyDomain::getObjectPropertyId, objectPropertyId));
		if (domains.isEmpty()) {
			// 无 domain 约束则视为满足
			return true;
		}

		Set<Long> domainTypeIds = new HashSet<>();
		for (OntObjectPropertyDomain domain : domains) {
			domainTypeIds.add(domain.getEntityTypeId());
		}

		// 直接匹配
		if (domainTypeIds.contains(entityTypeId)) {
			return true;
		}

		// 检查继承：entityTypeId 是否是某个 domain 类型的子类
		return isSubtypeOfAny(entityTypeId, domainTypeIds);
	}

	/**
	 * 检查实体类型是否在对象属性的 range 中（含继承）。
	 * @param objectPropertyId 对象属性ID
	 * @param entityTypeId 实体类型ID
	 * @return true 满足
	 */
	private boolean isEntityInRange(Long objectPropertyId, Long entityTypeId) {
		List<OntObjectPropertyRange> ranges = rangeMapper.selectList(
				Wrappers.<OntObjectPropertyRange>lambdaQuery()
						.eq(OntObjectPropertyRange::getObjectPropertyId, objectPropertyId));
		if (ranges.isEmpty()) {
			return true;
		}

		Set<Long> rangeTypeIds = new HashSet<>();
		for (OntObjectPropertyRange range : ranges) {
			rangeTypeIds.add(range.getEntityTypeId());
		}

		if (rangeTypeIds.contains(entityTypeId)) {
			return true;
		}

		return isSubtypeOfAny(entityTypeId, rangeTypeIds);
	}

	/**
	 * 检查 entityTypeId 是否是 candidateParentIds 中任意一个的子类（含多级继承）。
	 * @param entityTypeId 待检查的实体类型ID
	 * @param candidateParentIds 候选父类ID集合
	 * @return true 如果是子类
	 */
	private boolean isSubtypeOfAny(Long entityTypeId, Set<Long> candidateParentIds) {
		// BFS 遍历继承层次
		Set<Long> visited = new HashSet<>();
		List<Long> queue = new java.util.ArrayList<>();
		queue.add(entityTypeId);
		visited.add(entityTypeId);

		while (!queue.isEmpty()) {
			Long currentId = queue.remove(0);
			// 查询 currentId 的父类
			List<OntEntityTypeHierarchy> parents = hierarchyMapper.selectList(
					Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
							.eq(OntEntityTypeHierarchy::getChildId, currentId));
			for (OntEntityTypeHierarchy hierarchy : parents) {
				Long parentId = hierarchy.getParentId();
				if (candidateParentIds.contains(parentId)) {
					return true;
				}
				if (!visited.contains(parentId)) {
					visited.add(parentId);
					queue.add(parentId);
				}
			}
		}
		return false;
	}

}
