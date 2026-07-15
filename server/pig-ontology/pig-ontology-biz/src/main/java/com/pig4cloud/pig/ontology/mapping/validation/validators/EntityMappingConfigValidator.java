/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.validators;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapping.ValidationErrorCode;
import com.pig4cloud.pig.ontology.mapping.compiler.IriTemplateCompiler;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.validation.IssueCollector;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidationContext;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 实体映射配置校验器（L1+L3 目标Schema，18-06 §3）。
 * <p>
 * 以 IssueCollector 模式复用 {@link com.pig4cloud.pig.ontology.mapping.validation.EntityMappingValidator} 的校验逻辑。
 * 校验规则：
 * <ul>
 *   <li>key_columns 非空且可提取列名</li>
 *   <li>normalizer 合法</li>
 *   <li>IRI 模板变量必须在 key_columns 中</li>
 *   <li>目标实体类型存在且属于映射工程关联的本体</li>
 *   <li>目标命名空间存在</li>
 *   <li>删除策略与失活属性配置一致性</li>
 * </ul>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityMappingConfigValidator implements MappingValidator {

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntNamespaceMapper namespaceMapper;

	private final IriTemplateCompiler iriTemplateCompiler;

	private static final List<String> VALID_NORMALIZERS = List.of(
			"LONG", "TRIM_STRING", "UPPER_STRING", "LOWER_STRING", "URL_ENCODE");

	@Override
	public String code() {
		return "EntityMappingConfigValidator";
	}

	@Override
	public int order() {
		return 40;
	}

	@Override
	public void validate(MappingValidationContext context, IssueCollector issues) {
		if (context.isPrerequisiteFailed()) {
			issues.addSkipped(code());
			return;
		}

		OntMappingProject project = context.getMappingProject();

		for (OntEntityMapping em : context.getEntityMappings()) {
			// 只校验启用的映射
			if (!"1".equals(em.getEnabled())) {
				continue;
			}

			String scopeRef = em.getMappingCode();

			// 1. key_columns 非空
			if (em.getKeyColumns() == null || em.getKeyColumns().isBlank()) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "ENTITY",
						scopeRef, "实体映射 " + scopeRef + " 的键列配置为空", "配置非空的 key_columns");
				continue;
			}

			// 2. 提取键列名并校验 normalizer
			List<String> keyColumnNames = iriTemplateCompiler.extractKeyColumnNames(em.getKeyColumns());
			if (keyColumnNames.isEmpty()) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "ENTITY",
						scopeRef, "实体映射 " + scopeRef + " 的键列配置无法提取列名", "检查 key_columns JSON 格式");
				continue;
			}
			validateNormalizers(em.getKeyColumns(), scopeRef, issues);

			// 3. IRI 模板变量必须是 keyColumns 中的列
			try {
				iriTemplateCompiler.validateTemplate(em.getIriTemplate(), keyColumnNames);
			}
			catch (IllegalArgumentException e) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "ENTITY",
						scopeRef, "实体映射 " + scopeRef + " IRI 模板校验失败: " + e.getMessage(),
						"确保 IRI 模板变量在 key_columns 中");
			}

			// 4. 目标实体类型存在且属于映射工程关联的本体
			if (em.getTargetEntityTypeId() == null) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "ENTITY",
						scopeRef, "实体映射 " + scopeRef + " 未配置目标实体类型", "设置有效的 targetEntityTypeId");
				continue;
			}
			OntEntityType entityType = entityTypeMapper.selectById(em.getTargetEntityTypeId());
			if (entityType == null || "1".equals(entityType.getDelFlag())) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "ENTITY",
						scopeRef, "实体映射 " + scopeRef + " 的目标实体类型不存在", "设置有效的实体类型");
				continue;
			}
			if (!entityType.getOntologyId().equals(project.getOntologyId())) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "ENTITY",
						scopeRef, "实体映射 " + scopeRef + " 的目标实体类型不属于当前本体工程", "选择当前本体工程的实体类型");
			}

			// 5. 目标命名空间存在
			if (em.getTargetNamespaceId() == null) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "ENTITY",
						scopeRef, "实体映射 " + scopeRef + " 未配置目标命名空间", "设置有效的 targetNamespaceId");
			}
			else {
				OntNamespace namespace = namespaceMapper.selectById(em.getTargetNamespaceId());
				if (namespace == null || "1".equals(namespace.getDelFlag())) {
					issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "ENTITY",
							scopeRef, "实体映射 " + scopeRef + " 的目标命名空间不存在", "设置有效的命名空间");
				}
			}

			// 6. 删除策略与失活属性配置一致性
			if ("MARK_INACTIVE".equals(em.getDeleteStrategy()) && em.getInactivePropertyId() == null) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "ENTITY",
						scopeRef, "实体映射 " + scopeRef + " 删除策略为 MARK_INACTIVE 但未配置失活属性ID",
						"设置 inactivePropertyId");
			}
		}

		log.debug("EntityMappingConfigValidator completed: {} issues", issues.getTotalCount());
	}

	private void validateNormalizers(String keyColumnsJson, String scopeRef, IssueCollector issues) {
		java.util.regex.Pattern normalizerPattern = java.util.regex.Pattern.compile(
				"\"normalizer\"\\s*:\\s*\"([^\"]+)\"");
		java.util.regex.Matcher matcher = normalizerPattern.matcher(keyColumnsJson);
		while (matcher.find()) {
			String normalizer = matcher.group(1);
			if (!VALID_NORMALIZERS.contains(normalizer)) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "ENTITY",
						scopeRef, "未知 normalizer: " + normalizer, "使用合法 normalizer: " + VALID_NORMALIZERS);
			}
		}
	}

}
