/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.validator;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitMapper;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionResource;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * R1: 扩展命名空间强制规则。
 *
 * <p>校验扩展模块绑定的命名空间为扩展命名空间（is_builtin='0'），
 * 且注册资源的命名空间与模块绑定的命名空间一致。
 *
 * <p>注：{@code OntAxiomRule} 无 {@code namespaceId} 字段，
 * 对 AXIOM_RULE 分支检查 {@code isBuiltin='0'} 即可。
 *
 * <p>国标依据：第9.2条第1条
 *
 * @author youming
 */
@Component
@AllArgsConstructor
public class ExtensionNamespaceRule implements ExtensionValidationRule {

	private static final String BUILTIN = "1";

	private final OntNamespaceMapper namespaceMapper;

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	private final OntAxiomRuleMapper axiomRuleMapper;

	private final OntUnitMapper unitMapper;

	@Override
	public String getRuleCode() {
		return "R1";
	}

	@Override
	public String getRuleName() {
		return "扩展命名空间强制";
	}

	@Override
	public String getGbClause() {
		return "GB/T 48000.3—2026 第9.2条第1条";
	}

	@Override
	public List<ExtensionValidationResult> validate(OntExtensionModule module, OntExtensionResource resource) {
		// 阶段1：创建模块时校验绑定命名空间
		OntNamespace namespace = namespaceMapper.selectById(module.getNamespaceId());
		if (namespace == null) {
			return Collections.singletonList(ExtensionValidationResult.fail(getRuleCode(), getRuleName(),
					getGbClause(), "VIOLATION", null,
					"扩展模块绑定的命名空间不存在", "请选择有效的扩展命名空间"));
		}
		if (BUILTIN.equals(namespace.getIsBuiltin())) {
			return Collections.singletonList(ExtensionValidationResult.fail(getRuleCode(), getRuleName(),
					getGbClause(), "VIOLATION", null,
					"扩展模块必须绑定扩展命名空间，不可绑定核心命名空间(is_builtin=1)",
					"请选择is_builtin=0的扩展命名空间"));
		}

		// 阶段2：注册资源时校验资源命名空间与模块一致
		if (resource == null) {
			return Collections.singletonList(ExtensionValidationResult.pass(
					getRuleCode(), getRuleName(), getGbClause()));
		}

		Long resourceNamespaceId = resolveResourceNamespaceId(module, resource);
		if (resourceNamespaceId == null) {
			return Collections.singletonList(ExtensionValidationResult.fail(getRuleCode(), getRuleName(),
					getGbClause(), "VIOLATION", resource.getResourceIri(),
					"资源未绑定扩展命名空间或为内置资源",
					"请确保资源使用扩展命名空间且非内置"));
		}
		if (!resourceNamespaceId.equals(module.getNamespaceId())) {
			return Collections.singletonList(ExtensionValidationResult.fail(getRuleCode(), getRuleName(),
					getGbClause(), "VIOLATION", resource.getResourceIri(),
					"资源命名空间与扩展模块绑定的命名空间不一致",
					"请确保资源使用模块绑定的扩展命名空间"));
		}

		return Collections.singletonList(ExtensionValidationResult.pass(
				getRuleCode(), getRuleName(), getGbClause()));
	}

	/**
	 * 根据资源类型查询资源所属命名空间ID。
	 *
	 * <p>AXIOM_RULE 无 namespaceId 字段，通过 isBuiltin 判断：
	 * 扩展规则(isBuiltin='0')继承模块命名空间，内置规则返回 null。
	 */
	private Long resolveResourceNamespaceId(OntExtensionModule module, OntExtensionResource resource) {
		switch (resource.getResourceType()) {
			case "ENTITY_TYPE":
				OntEntityType entityType = entityTypeMapper.selectById(resource.getResourceId());
				return entityType != null ? entityType.getNamespaceId() : null;
			case "DATA_PROPERTY":
				OntDataProperty dataProperty = dataPropertyMapper.selectById(resource.getResourceId());
				return dataProperty != null ? dataProperty.getNamespaceId() : null;
			case "OBJECT_PROPERTY":
				OntObjectProperty objectProperty = objectPropertyMapper.selectById(resource.getResourceId());
				return objectProperty != null ? objectProperty.getNamespaceId() : null;
			case "AXIOM_RULE":
				OntAxiomRule rule = axiomRuleMapper.selectById(resource.getResourceId());
				if (rule != null && BUILTIN.equals(rule.getIsBuiltin())) {
					return null;
				}
				return module.getNamespaceId();
			case "UNIT":
				OntUnit unit = unitMapper.selectById(resource.getResourceId());
				return unit != null ? unit.getNamespaceId() : null;
			default:
				return null;
		}
	}

}
