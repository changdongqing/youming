/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.executor.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntAxiomRuleTarget;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntInstanceDataValue;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceDataValueMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitMapper;
import com.pig4cloud.pig.ontology.validation.executor.AbstractValidationExecutor;
import com.pig4cloud.pig.ontology.validation.model.ValidationContext;
import com.pig4cloud.pig.ontology.validation.model.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 单位一致性校验执行器（APPLICATION 模式）。
 * <p>
 * 对应规则 970007 PRD_CONSTRAINT_UNIT_CONSISTENCY。
 * 校验 Constraint 实例的 maxValue/minValue/thresholdRange 与 measurementUnit
 * 引用的单位字典条目同属一个物理量分类。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
public class UnitDimensionConsistencyExecutor extends AbstractValidationExecutor {

	private final OntEntityInstanceMapper instanceMapper;

	private final OntInstanceDataValueMapper dataValueMapper;

	private final OntUnitMapper unitMapper;

	public UnitDimensionConsistencyExecutor(OntEntityTypeMapper entityTypeMapper,
			OntEntityTypeHierarchyMapper hierarchyMapper,
			OntEntityInstanceMapper instanceMapper,
			OntInstanceDataValueMapper dataValueMapper,
			OntUnitMapper unitMapper) {
		super(entityTypeMapper, hierarchyMapper);
		this.instanceMapper = instanceMapper;
		this.dataValueMapper = dataValueMapper;
		this.unitMapper = unitMapper;
	}

	@Override
	public String getExecutorCode() {
		return "UNIT_DIMENSION_CONSISTENCY";
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

		// 从目标绑定获取角色
		OntAxiomRuleTarget targetClassTarget = findTarget(targets, "TARGET_CLASS");
		OntAxiomRuleTarget unitTarget = findTarget(targets, "UNIT");
		OntAxiomRuleTarget maxTarget = findTarget(targets, "MAX");
		OntAxiomRuleTarget minTarget = findTarget(targets, "MIN");

		if (targetClassTarget == null || unitTarget == null) {
			return results;
		}

		Long targetClassId = targetClassTarget.getEntityTypeId();
		Long unitPropertyId = unitTarget.getDataPropertyId();
		Long maxPropertyId = maxTarget != null ? maxTarget.getDataPropertyId() : null;
		Long minPropertyId = minTarget != null ? minTarget.getDataPropertyId() : null;

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

		// 批量加载实例的数据值
		Set<Long> instanceIds = instances.stream().map(OntEntityInstance::getId).collect(Collectors.toSet());
		if (instanceIds.isEmpty()) {
			return results;
		}

		// 加载涉及的单位
		Set<Long> allUnitIds = new HashSet<>();
		List<OntInstanceDataValue> allValues = dataValueMapper.selectList(
				Wrappers.<OntInstanceDataValue>lambdaQuery()
						.in(OntInstanceDataValue::getInstanceId, instanceIds));
		for (OntInstanceDataValue dv : allValues) {
			if (dv.getUnitId() != null) {
				allUnitIds.add(dv.getUnitId());
			}
		}
		Map<Long, OntUnit> unitMap = allUnitIds.isEmpty() ? Map.of() :
				unitMapper.selectBatchIds(allUnitIds)
					.stream()
					.collect(Collectors.toMap(OntUnit::getId, u -> u));

		// 加载数据属性 IRI 映射（用于 resultPath）
		Map<Long, String> propIriMap = loadPropertyIris(unitPropertyId, maxPropertyId, minPropertyId);

		// 逐实例校验
		for (OntEntityInstance instance : instances) {
			List<OntInstanceDataValue> instanceValues = allValues.stream()
				.filter(v -> v.getInstanceId().equals(instance.getId()))
				.collect(Collectors.toList());

			// 找到 measurementUnit 值
			OntInstanceDataValue unitValue = instanceValues.stream()
				.filter(v -> unitPropertyId.equals(v.getDataPropertyId()))
				.findFirst().orElse(null);

			if (unitValue == null || unitValue.getUnitId() == null) {
				continue; // 无单位引用，跳过
			}

			OntUnit measurementUnit = unitMap.get(unitValue.getUnitId());
			if (measurementUnit == null) {
				continue;
			}
			Long measurementCategoryId = measurementUnit.getCategoryId();

			// 校验 maxValue
			if (maxPropertyId != null) {
				OntInstanceDataValue maxValue = instanceValues.stream()
					.filter(v -> maxPropertyId.equals(v.getDataPropertyId()))
					.findFirst().orElse(null);
				if (maxValue != null && maxValue.getUnitId() != null) {
					OntUnit maxUnit = unitMap.get(maxValue.getUnitId());
					if (maxUnit != null && !maxUnit.getCategoryId().equals(measurementCategoryId)) {
						results.add(violation(rule, instance.getIri(),
								propIriMap.get(maxPropertyId),
								"maxValue 引用的单位分类(" + getCategoryName(maxUnit.getCategoryId(), unitMap)
										+ ")与 measurementUnit(" + getCategoryName(measurementCategoryId, unitMap)
										+ ")不一致",
								getCategoryName(measurementCategoryId, unitMap),
								getCategoryName(maxUnit.getCategoryId(), unitMap),
								"请统一数值约束与测量单位的物理量分类"));
					}
				}
			}

			// 校验 minValue
			if (minPropertyId != null) {
				OntInstanceDataValue minValue = instanceValues.stream()
					.filter(v -> minPropertyId.equals(v.getDataPropertyId()))
					.findFirst().orElse(null);
				if (minValue != null && minValue.getUnitId() != null) {
					OntUnit minUnit = unitMap.get(minValue.getUnitId());
					if (minUnit != null && !minUnit.getCategoryId().equals(measurementCategoryId)) {
						results.add(violation(rule, instance.getIri(),
								propIriMap.get(minPropertyId),
								"minValue 引用的单位分类(" + getCategoryName(minUnit.getCategoryId(), unitMap)
										+ ")与 measurementUnit(" + getCategoryName(measurementCategoryId, unitMap)
										+ ")不一致",
								getCategoryName(measurementCategoryId, unitMap),
								getCategoryName(minUnit.getCategoryId(), unitMap),
								"请统一数值约束与测量单位的物理量分类"));
					}
				}
			}
		}

		return results;
	}

	private String getCategoryName(Long categoryId, Map<Long, OntUnit> unitMap) {
		// 简化：返回 categoryId 的字符串，实际可查 ont_unit_category 获取名称
		return "分类ID:" + categoryId;
	}

	private Map<Long, String> loadPropertyIris(Long... propertyIds) {
		// 简化：返回属性ID -> "property:" + id 的映射
		// 实际应用中可查询 ont_data_property 表获取 IRI
		Map<Long, String> map = new java.util.HashMap<>();
		for (Long id : propertyIds) {
			if (id != null) {
				map.put(id, "property:" + id);
			}
		}
		return map;
	}

}
