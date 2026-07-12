/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.validator;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionResource;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * R5: 仅允许 subClassOf 核心类型规则。
 *
 * <p>校验扩展实体类型的 subClassOf 至少包含一个核心实体类型。
 *
 * <p>国标依据：第9.2条第5条
 *
 * @author youming
 */
@Component
@AllArgsConstructor
public class SubClassOnlyRule implements ExtensionValidationRule {

	private static final String BUILTIN = "1";

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	@Override
	public String getRuleCode() {
		return "R5";
	}

	@Override
	public String getRuleName() {
		return "仅允许子类扩展";
	}

	@Override
	public String getGbClause() {
		return "GB/T 48000.3—2026 第9.2条第5条";
	}

	@Override
	public List<ExtensionValidationResult> validate(OntExtensionModule module, OntExtensionResource resource) {
		if (resource == null) {
			return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
		}

		// 仅校验实体类型
		if (!"ENTITY_TYPE".equals(resource.getResourceType())) {
			return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
		}

		OntEntityType extType = entityTypeMapper.selectById(resource.getResourceId());
		if (extType == null) {
			return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
		}

		// 查询扩展类型的父类列表
		List<OntEntityTypeHierarchy> hierarchies = hierarchyMapper.selectList(
			Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
				.eq(OntEntityTypeHierarchy::getChildId, extType.getId()));

		if (hierarchies.isEmpty()) {
			return List.of(ExtensionValidationResult.fail(getRuleCode(), getRuleName(),
					getGbClause(), "VIOLATION", resource.getResourceIri(),
					"扩展实体类型未声明任何父类（subClassOf）",
					"请为扩展实体类型至少指定一个核心实体类型作为父类"));
		}

		// 检查至少一个父类是核心类型
		boolean hasCoreParent = false;
		for (OntEntityTypeHierarchy h : hierarchies) {
			OntEntityType parent = entityTypeMapper.selectById(h.getParentId());
			if (parent != null && BUILTIN.equals(parent.getIsBuiltin())) {
				hasCoreParent = true;
				break;
			}
		}

		if (!hasCoreParent) {
			return List.of(ExtensionValidationResult.fail(getRuleCode(), getRuleName(),
					getGbClause(), "VIOLATION", resource.getResourceIri(),
					"扩展实体类型的父类中不包含任何核心实体类型",
					"请确保扩展实体类型至少 subClassOf 一个核心实体类型"));
		}

		return List.of(ExtensionValidationResult.pass(getRuleCode(), getRuleName(), getGbClause()));
	}

}
