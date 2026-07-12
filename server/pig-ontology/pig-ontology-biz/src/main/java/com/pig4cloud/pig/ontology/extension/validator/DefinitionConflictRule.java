/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.validator;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitMapper;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionResource;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * R3: 定义不得与核心矛盾规则。
 *
 * <p>首期采用文本相似度启发式：Jaccard 相似度低于 0.3 判为潜在矛盾。
 * R3 为 WARNING 级，不阻断操作。
 *
 * <p>国标依据：第9.2条第3条
 *
 * @author youming
 */
@Component
@AllArgsConstructor
public class DefinitionConflictRule implements ExtensionValidationRule {

	private static final String BUILTIN = "1";

	private static final double SIMILARITY_THRESHOLD = 0.3;

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	private final OntUnitMapper unitMapper;

	@Override
	public String getRuleCode() {
		return "R3";
	}

	@Override
	public String getRuleName() {
		return "定义不得与核心矛盾";
	}

	@Override
	public String getGbClause() {
		return "GB/T 48000.3—2026 第9.2条第3条";
	}

	@Override
	public List<ExtensionValidationResult> validate(OntExtensionModule module, OntExtensionResource resource) {
		if (resource == null) {
			return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
		}

		String extensionDefinition = resolveResourceDefinition(resource);
		if (extensionDefinition == null || extensionDefinition.isBlank()) {
			return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
		}

		// 查找同名核心资源的定义
		String coreDefinition = resolveCoreDefinition(resource);
		if (coreDefinition == null || coreDefinition.isBlank()) {
			return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
		}

		double similarity = jaccardSimilarity(extensionDefinition, coreDefinition);
		if (similarity < SIMILARITY_THRESHOLD) {
			return List.of(ExtensionValidationResult.fail(getRuleCode(), getRuleName(),
					getGbClause(), "WARNING", resource.getResourceIri(),
					"扩展资源定义与同名核心资源定义相似度较低（" + String.format("%.2f", similarity) + "），可能存在语义矛盾",
					"请检查扩展资源定义是否与核心资源定义语义一致"));
		}

		return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
	}

	/**
	 * 查询扩展资源的定义文本。
	 */
	private String resolveResourceDefinition(OntExtensionResource resource) {
		switch (resource.getResourceType()) {
			case "ENTITY_TYPE":
				OntEntityType type = entityTypeMapper.selectById(resource.getResourceId());
				return type != null ? type.getDefinition() : null;
			case "DATA_PROPERTY":
				OntDataProperty dp = dataPropertyMapper.selectById(resource.getResourceId());
				return dp != null ? dp.getDefinition() : null;
			case "OBJECT_PROPERTY":
				OntObjectProperty op = objectPropertyMapper.selectById(resource.getResourceId());
				return op != null ? op.getDefinition() : null;
			case "UNIT":
				OntUnit unit = unitMapper.selectById(resource.getResourceId());
				return unit != null ? unit.getUnitName() : null;
			default:
				return null;
		}
	}

	/**
	 * 查找同名核心资源的定义文本。
	 */
	private String resolveCoreDefinition(OntExtensionResource resource) {
		String name = resource.getResourceName();
		if (name == null) {
			return null;
		}
		switch (resource.getResourceType()) {
			case "ENTITY_TYPE":
				OntEntityType type = entityTypeMapper.selectOne(Wrappers.<OntEntityType>lambdaQuery()
					.eq(OntEntityType::getIsBuiltin, BUILTIN)
					.eq(OntEntityType::getName, name).last("LIMIT 1"));
				return type != null ? type.getDefinition() : null;
			case "DATA_PROPERTY":
				OntDataProperty dp = dataPropertyMapper.selectOne(Wrappers.<OntDataProperty>lambdaQuery()
					.eq(OntDataProperty::getIsBuiltin, BUILTIN)
					.eq(OntDataProperty::getName, name).last("LIMIT 1"));
				return dp != null ? dp.getDefinition() : null;
			case "OBJECT_PROPERTY":
				OntObjectProperty op = objectPropertyMapper.selectOne(Wrappers.<OntObjectProperty>lambdaQuery()
					.eq(OntObjectProperty::getIsBuiltin, BUILTIN)
					.eq(OntObjectProperty::getName, name).last("LIMIT 1"));
				return op != null ? op.getDefinition() : null;
			default:
				return null;
		}
	}

	/**
	 * 计算 Jaccard 相似度（基于词集交集/并集）。
	 */
	private double jaccardSimilarity(String a, String b) {
		Set<String> setA = tokenize(a);
		Set<String> setB = tokenize(b);
		if (setA.isEmpty() || setB.isEmpty()) {
			return 0.0;
		}
		Set<String> intersection = new HashSet<>(setA);
		intersection.retainAll(setB);
		Set<String> union = new HashSet<>(setA);
		union.addAll(setB);
		return (double) intersection.size() / union.size();
	}

	/**
	 * 简单中文分词（按字符二元组）。
	 */
	private Set<String> tokenize(String text) {
		if (text == null || text.length() < 2) {
			return Set.of();
		}
		Set<String> tokens = new HashSet<>();
		for (int i = 0; i < text.length() - 1; i++) {
			tokens.add(text.substring(i, i + 2));
		}
		return tokens;
	}

}
