/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.executor.application;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 结构限制校验执行器（SHACL_CORE 模式，Java 计数实现）。
 * <p>
 * 对应规则 970011 GB8_UNTITLED_NO_SUBCLAUSE。
 * 校验特定类型实例的对象属性断言数不超过 maxCount。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
public class MaxRelationCountExecutor extends AbstractValidationExecutor {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final OntEntityInstanceMapper instanceMapper;

	private final OntInstanceObjectRelationMapper relationMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	public MaxRelationCountExecutor(OntEntityTypeMapper entityTypeMapper,
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
		return "MAX_RELATION_COUNT";
	}

	@Override
	public String getValidationMode() {
		return "SHACL_CORE";
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

		// 从 configJson 获取 maxCount
		int maxCount = 0; // 默认0
		try {
			JsonNode config = OBJECT_MAPPER.readTree(rule.getConfigJson() != null ? rule.getConfigJson() : "{}");
			if (config.has("maxCount")) {
				maxCount = config.get("maxCount").asInt(0);
			}
		}
		catch (Exception e) {
			log.warn("解析 configJson 失败: ruleCode={}", rule.getRuleCode());
		}

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

		OntObjectProperty prop = objectPropertyMapper.selectById(objectPropertyId);
		String propIri = prop != null ? prop.getIri() : null;

		// 逐实例计数
		for (OntEntityInstance instance : instances) {
			Long count = relationMapper.selectCount(
					Wrappers.<OntInstanceObjectRelation>lambdaQuery()
							.eq(OntInstanceObjectRelation::getSubjectInstanceId, instance.getId())
							.eq(OntInstanceObjectRelation::getObjectPropertyId, objectPropertyId));

			if (count > maxCount) {
				results.add(violation(rule, instance.getIri(), propIri,
						"对象属性断言数 " + count + " 超过最大限制 " + maxCount,
						String.valueOf(maxCount), String.valueOf(count),
						maxCount == 0 ? "该类型实例不应包含此对象属性断言" : "请减少断言数量至不超过 " + maxCount));
			}
		}

		return results;
	}

}
