/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.validator;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntDataPropertyEnum;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyEnumMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionResource;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * R4: 约束一致性规则。
 *
 * <p>校验扩展枚举型数据属性的枚举值集合是否为核心枚举的子集。
 * R4 为 WARNING 级，不阻断操作。
 *
 * <p>国标依据：第9.2条第4条
 *
 * @author youming
 */
@Component
@AllArgsConstructor
public class ConstraintConformanceRule implements ExtensionValidationRule {

	private static final String BUILTIN = "1";

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntDataPropertyEnumMapper dataPropertyEnumMapper;

	@Override
	public String getRuleCode() {
		return "R4";
	}

	@Override
	public String getRuleName() {
		return "约束一致性";
	}

	@Override
	public String getGbClause() {
		return "GB/T 48000.3—2026 第9.2条第4条";
	}

	@Override
	public List<ExtensionValidationResult> validate(OntExtensionModule module, OntExtensionResource resource) {
		if (resource == null) {
			return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
		}

		// 仅校验数据属性
		if (!"DATA_PROPERTY".equals(resource.getResourceType())) {
			return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
		}

		OntDataProperty extProp = dataPropertyMapper.selectById(resource.getResourceId());
		if (extProp == null) {
			return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
		}

		// 查找同名核心数据属性
		OntDataProperty coreProp = dataPropertyMapper.selectOne(Wrappers.<OntDataProperty>lambdaQuery()
			.eq(OntDataProperty::getIsBuiltin, BUILTIN)
			.eq(OntDataProperty::getName, extProp.getName())
			.last("LIMIT 1"));

		if (coreProp == null) {
			// 无同名核心属性，无需子集化校验
			return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
		}

		// 获取核心枚举值集合
		List<OntDataPropertyEnum> coreEnums = dataPropertyEnumMapper.selectList(
			Wrappers.<OntDataPropertyEnum>lambdaQuery()
				.eq(OntDataPropertyEnum::getDataPropertyId, coreProp.getId()));

		if (coreEnums.isEmpty()) {
			return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
		}

		Set<String> coreValues = new HashSet<>();
		coreEnums.forEach(e -> coreValues.add(e.getEnumValue()));

		// 获取扩展枚举值集合
		List<OntDataPropertyEnum> extEnums = dataPropertyEnumMapper.selectList(
			Wrappers.<OntDataPropertyEnum>lambdaQuery()
				.eq(OntDataPropertyEnum::getDataPropertyId, extProp.getId()));

		if (extEnums.isEmpty()) {
			return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
		}

		// 检查扩展枚举是否为核心枚举的子集
		for (OntDataPropertyEnum extEnum : extEnums) {
			if (!coreValues.contains(extEnum.getEnumValue())) {
				return List.of(ExtensionValidationResult.fail(getRuleCode(), getRuleName(),
						getGbClause(), "WARNING", resource.getResourceIri(),
						"扩展枚举值「" + extEnum.getEnumValue() + "」不在核心枚举集合中",
						"请确认扩展枚举值为核心枚举的子集化限定"));
			}
		}

		return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
	}

}
