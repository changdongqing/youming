/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.executor.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntAxiomRuleTarget;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntInstanceObjectRelation;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyDomain;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyRange;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceObjectRelationMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyDomainMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyRangeMapper;
import com.pig4cloud.pig.ontology.validation.executor.AbstractValidationExecutor;
import com.pig4cloud.pig.ontology.validation.model.ValidationContext;
import com.pig4cloud.pig.ontology.validation.model.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 对象属性域值域校验执行器（APPLICATION 模式）。
 * <p>
 * 对应规则 970010 HIERARCHY_CONTAINMENT 和 970012 REFERENCE_DISTINCTION。
 * 校验对象属性断言的主体类型在定义域内、客体类型在值域内。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
public class ObjectRelationRangeExecutor extends AbstractValidationExecutor {

	private final OntEntityInstanceMapper instanceMapper;

	private final OntInstanceObjectRelationMapper relationMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	private final OntObjectPropertyDomainMapper domainMapper;

	private final OntObjectPropertyRangeMapper rangeMapper;

	public ObjectRelationRangeExecutor(OntEntityTypeMapper entityTypeMapper,
			OntEntityTypeHierarchyMapper hierarchyMapper,
			OntEntityInstanceMapper instanceMapper,
			OntInstanceObjectRelationMapper relationMapper,
			OntObjectPropertyMapper objectPropertyMapper,
			OntObjectPropertyDomainMapper domainMapper,
			OntObjectPropertyRangeMapper rangeMapper) {
		super(entityTypeMapper, hierarchyMapper);
		this.instanceMapper = instanceMapper;
		this.relationMapper = relationMapper;
		this.objectPropertyMapper = objectPropertyMapper;
		this.domainMapper = domainMapper;
		this.rangeMapper = rangeMapper;
	}

	@Override
	public String getExecutorCode() {
		return "OBJECT_RELATION_RANGE";
	}

	@Override
	public String getValidationMode() {
		return "APPLICATION";
	}

	@Override
	public List<ValidationResult> validate(OntAxiomRule rule, List<OntAxiomRuleTarget> targets,
			ValidationContext context) {
		List<ValidationResult> results = new ArrayList<>();
		Long ontologyId = context.getOntologyId();

		// 判断规则子类型：HIERARCHY_CONTAINMENT 或 REFERENCE_DISTINCTION
		String subType = rule.getSubType();
		if ("REFERENCE_DISTINCTION".equals(subType)) {
			return validateReferenceDistinction(rule, targets, ontologyId);
		}
		else {
			return validateHierarchyContainment(rule, targets, ontologyId);
		}
	}

	/**
	 * 层次包含校验：校验对象属性断言的主体在定义域、客体在值域。
	 */
	private List<ValidationResult> validateHierarchyContainment(OntAxiomRule rule,
			List<OntAxiomRuleTarget> targets, Long ontologyId) {
		List<ValidationResult> results = new ArrayList<>();

		OntAxiomRuleTarget relationTarget = findTarget(targets, "RELATION_PROPERTY");
		if (relationTarget == null) {
			return results;
		}

		Long objectPropertyId = relationTarget.getObjectPropertyId();
		OntObjectProperty prop = objectPropertyMapper.selectById(objectPropertyId);
		if (prop == null) {
			return results;
		}

		// 获取对象属性的定义域和值域
		List<OntObjectPropertyDomain> domains = domainMapper.selectList(
				Wrappers.<OntObjectPropertyDomain>lambdaQuery()
						.eq(OntObjectPropertyDomain::getObjectPropertyId, objectPropertyId));
		List<OntObjectPropertyRange> ranges = rangeMapper.selectList(
				Wrappers.<OntObjectPropertyRange>lambdaQuery()
						.eq(OntObjectPropertyRange::getObjectPropertyId, objectPropertyId));

		Set<Long> domainTypeIds = domains.stream()
			.map(OntObjectPropertyDomain::getEntityTypeId).collect(Collectors.toSet());
		Set<Long> rangeTypeIds = ranges.stream()
			.map(OntObjectPropertyRange::getEntityTypeId).collect(Collectors.toSet());

		// 加载所有断言
		List<OntInstanceObjectRelation> relations = relationMapper.selectList(null);
		// 过滤当前工程
		Set<Long> instanceIds = new java.util.HashSet<>();
		List<OntEntityInstance> allInstances = instanceMapper.selectList(
				Wrappers.<OntEntityInstance>lambdaQuery().eq(OntEntityInstance::getOntologyId, ontologyId));
		allInstances.forEach(i -> instanceIds.add(i.getId()));
		Map<Long, OntEntityInstance> instanceMap = allInstances.stream()
			.collect(Collectors.toMap(OntEntityInstance::getId, i -> i));

		relations = relations.stream()
			.filter(r -> instanceIds.contains(r.getSubjectInstanceId()))
			.collect(Collectors.toList());

		for (OntInstanceObjectRelation rel : relations) {
			if (!objectPropertyId.equals(rel.getObjectPropertyId())) {
				continue;
			}
			// 主体校验
			OntEntityInstance subject = instanceMap.get(rel.getSubjectInstanceId());
			if (subject != null) {
				Set<Long> subjectAncestors = collectAncestors(subject.getRdfTypeId());
				if (!domainTypeIds.isEmpty() && subjectAncestors.stream().noneMatch(domainTypeIds::contains)) {
					results.add(violation(rule, subject.getIri(), prop.getIri(),
							"对象属性断言的主体类型不在定义域范围内",
							"定义域类型", "当前类型", "请检查主体实例的 rdf:type 绑定"));
				}
			}

			// 客体校验（仅 INSTANCE 类型客体）
			if ("INSTANCE".equals(rel.getObjectKind()) && rel.getObjectInstanceId() != null) {
				OntEntityInstance object = instanceMap.get(rel.getObjectInstanceId());
				if (object != null) {
					Set<Long> objectAncestors = collectAncestors(object.getRdfTypeId());
					if (!rangeTypeIds.isEmpty() && objectAncestors.stream().noneMatch(rangeTypeIds::contains)) {
						results.add(violation(rule, object.getIri(), prop.getIri(),
								"对象属性断言的客体类型不在值域范围内",
								"值域类型", "当前类型", "请检查客体实例的 rdf:type 绑定"));
					}
				}
			}
		}

		return results;
	}

	/**
	 * 引用区分校验：标准间引用(cites)与条款级引用(citesStandard)的主体类型匹配。
	 */
	private List<ValidationResult> validateReferenceDistinction(OntAxiomRule rule,
			List<OntAxiomRuleTarget> targets, Long ontologyId) {
		List<ValidationResult> results = new ArrayList<>();

		OntAxiomRuleTarget standardClassTarget = findTarget(targets, "STANDARD_CLASS");
		OntAxiomRuleTarget clauseClassTarget = findTarget(targets, "CLAUSE_CLASS");
		OntAxiomRuleTarget standardRelationTarget = findTarget(targets, "STANDARD_RELATION");
		OntAxiomRuleTarget clauseRelationTarget = findTarget(targets, "CLAUSE_RELATION");

		if (standardClassTarget == null || clauseClassTarget == null
				|| standardRelationTarget == null || clauseRelationTarget == null) {
			return results;
		}

		Long standardClassId = standardClassTarget.getEntityTypeId();
		Long clauseClassId = clauseClassTarget.getEntityTypeId();
		Long standardRelationId = standardRelationTarget.getObjectPropertyId();
		Long clauseRelationId = clauseRelationTarget.getObjectPropertyId();

		// 标准关系的主体必须是 Standard 的子类
		Set<Long> standardDescendants = collectDescendants(standardClassId);
		// 条款关系的主体必须是 Clause 的子类
		Set<Long> clauseDescendants = collectDescendants(clauseClassId);

		// 加载当前工程的全部实例
		List<OntEntityInstance> allInstances = instanceMapper.selectList(
				Wrappers.<OntEntityInstance>lambdaQuery().eq(OntEntityInstance::getOntologyId, ontologyId));
		Set<Long> instanceIds = allInstances.stream().map(OntEntityInstance::getId).collect(Collectors.toSet());
		Map<Long, OntEntityInstance> instanceMap = allInstances.stream()
			.collect(Collectors.toMap(OntEntityInstance::getId, i -> i));

		// 加载断言
		List<OntInstanceObjectRelation> relations = relationMapper.selectList(null).stream()
			.filter(r -> instanceIds.contains(r.getSubjectInstanceId()))
			.collect(Collectors.toList());

		OntObjectProperty standardProp = objectPropertyMapper.selectById(standardRelationId);
		OntObjectProperty clauseProp = objectPropertyMapper.selectById(clauseRelationId);

		for (OntInstanceObjectRelation rel : relations) {
			OntEntityInstance subject = instanceMap.get(rel.getSubjectInstanceId());
			if (subject == null) {
				continue;
			}

			if (standardRelationId.equals(rel.getObjectPropertyId())) {
				// 标准引用：主体须是 Standard 的子类
				if (!standardDescendants.contains(subject.getRdfTypeId())) {
					results.add(violation(rule, subject.getIri(),
							standardProp != null ? standardProp.getIri() : null,
							"标准间引用属性的主体类型应为 Standard 的子类",
							"Standard 子类", "当前类型", "请检查主体实例的类型绑定"));
				}
			}
			else if (clauseRelationId.equals(rel.getObjectPropertyId())) {
				// 条款引用：主体须是 Clause 的子类
				if (!clauseDescendants.contains(subject.getRdfTypeId())) {
					results.add(violation(rule, subject.getIri(),
							clauseProp != null ? clauseProp.getIri() : null,
							"条款级引用属性的主体类型应为 Clause 的子类",
							"Clause 子类", "当前类型", "请检查主体实例的类型绑定"));
				}
			}
		}

		return results;
	}

}
