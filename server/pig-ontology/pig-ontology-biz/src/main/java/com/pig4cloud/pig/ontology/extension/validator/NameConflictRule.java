/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.validator;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitMapper;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionResource;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * R2: 名称不得与核心重复规则。
 *
 * <p>校验扩展资源的IRI或名称不与核心(is_builtin=1)资源重复。
 *
 * <p>国标依据：第9.2条第2条
 *
 * @author youming
 */
@Component
@AllArgsConstructor
public class NameConflictRule implements ExtensionValidationRule {

	private static final String BUILTIN = "1";

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	private final OntAxiomRuleMapper axiomRuleMapper;

	private final OntUnitMapper unitMapper;

	@Override
	public String getRuleCode() {
		return "R2";
	}

	@Override
	public String getRuleName() {
		return "名称不得与核心重复";
	}

	@Override
	public String getGbClause() {
		return "GB/T 48000.3—2026 第9.2条第2条";
	}

	@Override
	public List<ExtensionValidationResult> validate(OntExtensionModule module, OntExtensionResource resource) {
		if (resource == null) {
			return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
		}

		List<ExtensionValidationResult> results = new ArrayList<>();
		String resourceIri = resource.getResourceIri();
		String resourceName = resource.getResourceName();
		boolean conflictFound = false;
		String conflictType = "";

		switch (resource.getResourceType()) {
			case "ENTITY_TYPE":
				if (entityTypeMapper.exists(Wrappers.<OntEntityType>lambdaQuery()
					.eq(OntEntityType::getIsBuiltin, BUILTIN)
					.and(w -> w.eq(OntEntityType::getIri, resourceIri)
						.or().eq(OntEntityType::getName, resourceName)))) {
					conflictFound = true;
					conflictType = "实体类型";
				}
				break;
			case "DATA_PROPERTY":
				if (dataPropertyMapper.exists(Wrappers.<OntDataProperty>lambdaQuery()
					.eq(OntDataProperty::getIsBuiltin, BUILTIN)
					.and(w -> w.eq(OntDataProperty::getIri, resourceIri)
						.or().eq(OntDataProperty::getName, resourceName)))) {
					conflictFound = true;
					conflictType = "数据属性";
				}
				break;
			case "OBJECT_PROPERTY":
				if (objectPropertyMapper.exists(Wrappers.<OntObjectProperty>lambdaQuery()
					.eq(OntObjectProperty::getIsBuiltin, BUILTIN)
					.and(w -> w.eq(OntObjectProperty::getIri, resourceIri)
						.or().eq(OntObjectProperty::getName, resourceName)))) {
					conflictFound = true;
					conflictType = "对象属性";
				}
				break;
			case "AXIOM_RULE":
				if (axiomRuleMapper.exists(Wrappers.<OntAxiomRule>lambdaQuery()
					.eq(OntAxiomRule::getIsBuiltin, BUILTIN)
					.and(w -> w.eq(OntAxiomRule::getRuleCode, resourceName)
						.or().eq(OntAxiomRule::getName, resourceName)))) {
					conflictFound = true;
					conflictType = "公理规则";
				}
				break;
			case "UNIT":
				if (unitMapper.exists(Wrappers.<OntUnit>lambdaQuery()
					.eq(OntUnit::getIsBuiltin, BUILTIN)
					.and(w -> w.eq(OntUnit::getUnitCode, resourceName)))) {
					conflictFound = true;
					conflictType = "单位条目";
				}
				break;
			default:
				break;
		}

		if (conflictFound) {
			results.add(ExtensionValidationResult.fail(getRuleCode(), getRuleName(),
					getGbClause(), "VIOLATION", resourceIri,
					"扩展" + conflictType + "的名称或IRI与核心资源重复",
					"请修改扩展资源的名称或IRI，确保不与核心(is_builtin=1)资源冲突"));
		}
		else {
			results.add(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
		}

		return results;
	}

}
