/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.executor.composite;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntAxiomRuleTarget;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntInstanceObjectRelation;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceObjectRelationMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.validation.executor.AbstractValidationExecutor;
import com.pig4cloud.pig.ontology.validation.model.ValidationContext;
import com.pig4cloud.pig.ontology.validation.model.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 功能性对象属性校验执行器（COMPOSITE 模式）。
 * <p>
 * 对应规则 970008 GB8_ISSUED_BY_FUNCTIONAL。
 * 校验功能性对象属性：每个主体实例至多有一个该属性的断言。
 * </p>
 * <p>
 * 本期以 Java 计数为主轨（等同于 SHACL maxCount 1），OWL FunctionalProperty 声明
 * 已在 Schema 中预置但不主动执行推理，以避免不必要的 Model 组装开销。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
public class FunctionalObjectPropertyExecutor extends AbstractValidationExecutor {

	private final OntEntityInstanceMapper instanceMapper;

	private final OntInstanceObjectRelationMapper relationMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	public FunctionalObjectPropertyExecutor(OntEntityTypeMapper entityTypeMapper,
			OntEntityTypeHierarchyMapper hierarchyMapper,
			OntEntityInstanceMapper instanceMapper,
			OntInstanceObjectRelationMapper relationMapper,
			OntObjectPropertyMapper objectPropertyMapper) {
		super(entityTypeMapper, hierarchyMapper);
		this.instanceMapper = instanceMapper;
		this.relationMapper = relationMapper;
		this.objectPropertyMapper = objectPropertyMapper;
	}

	@Override
	public String getExecutorCode() {
		return "FUNCTIONAL_OBJECT_PROPERTY";
	}

	@Override
	public String getValidationMode() {
		return "COMPOSITE";
	}

	@Override
	public List<ValidationResult> validate(OntAxiomRule rule, List<OntAxiomRuleTarget> targets,
			ValidationContext context) {
		List<ValidationResult> results = new ArrayList<>();
		Long ontologyId = context.getOntologyId();

		OntAxiomRuleTarget targetClassTarget = findTarget(targets, "TARGET_CLASS");
		OntAxiomRuleTarget relationTarget = findTarget(targets, "RELATION_PROPERTY");

		if (targetClassTarget == null || relationTarget == null) {
			return results;
		}

		Long targetClassId = targetClassTarget.getEntityTypeId();
		Long objectPropertyId = relationTarget.getObjectPropertyId();

		OntObjectProperty prop = objectPropertyMapper.selectById(objectPropertyId);
		String propIri = prop != null ? prop.getIri() : null;

		// 收集目标类型及其子类型
		Set<Long> targetTypeIds = collectDescendants(targetClassId);
		if (targetTypeIds.isEmpty()) {
			return results;
		}

		// 加载这些类型的实例
		List<OntEntityInstance> instances = instanceMapper.selectList(
				Wrappers.<OntEntityInstance>lambdaQuery()
						.eq(OntEntityInstance::getOntologyId, ontologyId)
						.in(OntEntityInstance::getRdfTypeId, targetTypeIds));

		// 逐实例校验功能性约束
		for (OntEntityInstance instance : instances) {
			Long count = relationMapper.selectCount(
					Wrappers.<OntInstanceObjectRelation>lambdaQuery()
							.eq(OntInstanceObjectRelation::getSubjectInstanceId, instance.getId())
							.eq(OntInstanceObjectRelation::getObjectPropertyId, objectPropertyId));

			if (count > 1) {
				results.add(violation(rule, instance.getIri(), propIri,
						"功能性属性断言数 " + count + " 超过最大限制 1",
						"1", String.valueOf(count),
						"每个实例只能有一个该对象属性的断言，请移除多余的断言"));
			}
		}

		return results;
	}

}
