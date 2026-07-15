/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.validators;

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
import com.pig4cloud.pig.ontology.mapping.ValidationErrorCode;
import com.pig4cloud.pig.ontology.mapping.compiler.RelationKeyCompiler;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.entity.OntRelationMapping;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.validation.IssueCollector;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidationContext;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关系映射配置校验器（L1+L3 目标Schema，18-06 §3）。
 * <p>
 * 以 IssueCollector 模式复用 {@link com.pig4cloud.pig.ontology.mapping.validation.RelationMappingValidator} 的校验逻辑。
 * 校验规则：
 * <ul>
 *   <li>关系模式合法（FOREIGN_KEY / SELF_REFERENCE / JOIN_TABLE）</li>
 *   <li>主体和客体实体映射存在且属于同一版本</li>
 *   <li>SELF_REFERENCE 要求主体 == 客体</li>
 *   <li>对象属性存在且属于映射工程关联的本体</li>
 *   <li>主体类型满足 domain（含继承），客体类型满足 range（含继承）</li>
 *   <li>键映射 JSON 格式合法</li>
 * </ul>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelationMappingConfigValidator implements MappingValidator {

	private final OntObjectPropertyMapper objectPropertyMapper;

	private final OntObjectPropertyDomainMapper domainMapper;

	private final OntObjectPropertyRangeMapper rangeMapper;

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	private final RelationKeyCompiler relationKeyCompiler;

	@Override
	public String code() {
		return "RelationMappingConfigValidator";
	}

	@Override
	public int order() {
		return 60;
	}

	@Override
	public void validate(MappingValidationContext context, IssueCollector issues) {
		if (context.isPrerequisiteFailed()) {
			issues.addSkipped(code());
			return;
		}

		OntMappingProject project = context.getMappingProject();
		Map<Long, OntEntityMapping> entityMappingMap = context.getEntityMappings().stream()
				.collect(Collectors.toMap(OntEntityMapping::getId, em -> em));

		for (OntRelationMapping rm : context.getRelationMappings()) {
			if (!"1".equals(rm.getEnabled())) {
				continue;
			}
			validateRelationMapping(rm, context, entityMappingMap, project, issues);
		}

		log.debug("RelationMappingConfigValidator completed: {} issues", issues.getTotalCount());
	}

	private void validateRelationMapping(OntRelationMapping rm, MappingValidationContext context,
			Map<Long, OntEntityMapping> entityMappingMap, OntMappingProject project, IssueCollector issues) {
		String scopeRef = rm.getMappingCode();

		// 1. 关系模式合法
		if (!"FOREIGN_KEY".equals(rm.getRelationMode()) && !"SELF_REFERENCE".equals(rm.getRelationMode())
				&& !"JOIN_TABLE".equals(rm.getRelationMode())) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "RELATION",
					scopeRef, "非法关系模式: " + rm.getRelationMode(), "使用 FOREIGN_KEY / SELF_REFERENCE / JOIN_TABLE");
		}

		// 2. 主体实体映射存在且属于同一版本
		OntEntityMapping subjectMapping = entityMappingMap.get(rm.getSubjectEntityMappingId());
		if (subjectMapping == null) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "RELATION",
					scopeRef, "主体实体映射不存在: " + rm.getSubjectEntityMappingId(), "设置有效的主体实体映射");
			return;
		}

		// 3. 客体实体映射存在且属于同一版本
		OntEntityMapping objectMapping = entityMappingMap.get(rm.getObjectEntityMappingId());
		if (objectMapping == null) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "RELATION",
					scopeRef, "客体实体映射不存在: " + rm.getObjectEntityMappingId(), "设置有效的客体实体映射");
			return;
		}

		// 4. SELF_REFERENCE 要求主体 == 客体
		if ("SELF_REFERENCE".equals(rm.getRelationMode())
				&& !rm.getSubjectEntityMappingId().equals(rm.getObjectEntityMappingId())) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "RELATION",
					scopeRef, "SELF_REFERENCE 模式要求主体和客体实体映射相同", "设置 subjectEntityMappingId = objectEntityMappingId");
		}

		// 5. 对象属性存在且属于映射工程关联的本体
		if (rm.getObjectPropertyId() == null) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "RELATION",
					scopeRef, "未配置对象属性ID", "设置有效的 objectPropertyId");
			return;
		}
		OntObjectProperty objectProperty = objectPropertyMapper.selectById(rm.getObjectPropertyId());
		if (objectProperty == null || "1".equals(objectProperty.getDelFlag())) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "RELATION",
					scopeRef, "对象属性不存在", "设置有效的对象属性");
			return;
		}
		if (!objectProperty.getOntologyId().equals(project.getOntologyId())) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "RELATION",
					scopeRef, "对象属性不属于当前本体工程", "选择当前本体工程的对象属性");
		}

		// 6. domain 包含主体实体类型（含继承）
		if (!isEntityInDomain(rm.getObjectPropertyId(), subjectMapping.getTargetEntityTypeId())) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "RELATION",
					scopeRef, "主体实体类型不满足对象属性 domain", "调整主体实体映射的目标类型或对象属性");
		}

		// 7. range 包含客体实体类型（含继承）
		if (!isEntityInRange(rm.getObjectPropertyId(), objectMapping.getTargetEntityTypeId())) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "RELATION",
					scopeRef, "客体实体类型不满足对象属性 range", "调整客体实体映射的目标类型或对象属性");
		}

		// 8. 键映射 JSON 格式合法
		if (relationKeyCompiler.extractColumnPairs(rm.getSubjectKeyMapping()).isEmpty()) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "RELATION",
					scopeRef, "主体键映射无法提取列对", "检查 subjectKeyMapping JSON");
		}
		if (relationKeyCompiler.extractColumnPairs(rm.getObjectKeyMapping()).isEmpty()) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "RELATION",
					scopeRef, "客体键映射无法提取列对", "检查 objectKeyMapping JSON");
		}
	}

	private boolean isEntityInDomain(Long objectPropertyId, Long entityTypeId) {
		List<OntObjectPropertyDomain> domains = domainMapper.selectList(
				Wrappers.<OntObjectPropertyDomain>lambdaQuery()
						.eq(OntObjectPropertyDomain::getObjectPropertyId, objectPropertyId));
		if (domains.isEmpty()) {
			return true;
		}
		Set<Long> domainTypeIds = domains.stream()
				.map(OntObjectPropertyDomain::getEntityTypeId)
				.collect(Collectors.toSet());
		if (domainTypeIds.contains(entityTypeId)) {
			return true;
		}
		return isSubtypeOfAny(entityTypeId, domainTypeIds);
	}

	private boolean isEntityInRange(Long objectPropertyId, Long entityTypeId) {
		List<OntObjectPropertyRange> ranges = rangeMapper.selectList(
				Wrappers.<OntObjectPropertyRange>lambdaQuery()
						.eq(OntObjectPropertyRange::getObjectPropertyId, objectPropertyId));
		if (ranges.isEmpty()) {
			return true;
		}
		Set<Long> rangeTypeIds = ranges.stream()
				.map(OntObjectPropertyRange::getEntityTypeId)
				.collect(Collectors.toSet());
		if (rangeTypeIds.contains(entityTypeId)) {
			return true;
		}
		return isSubtypeOfAny(entityTypeId, rangeTypeIds);
	}

	private boolean isSubtypeOfAny(Long entityTypeId, Set<Long> candidateParentIds) {
		Set<Long> visited = new HashSet<>();
		List<Long> queue = new java.util.ArrayList<>();
		queue.add(entityTypeId);
		visited.add(entityTypeId);

		while (!queue.isEmpty()) {
			Long currentId = queue.remove(0);
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
