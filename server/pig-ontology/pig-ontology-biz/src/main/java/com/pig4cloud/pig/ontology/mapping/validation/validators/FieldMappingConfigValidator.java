/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.validators;

import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitMapper;
import com.pig4cloud.pig.ontology.mapping.ValidationErrorCode;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.entity.OntFieldMapping;
import com.pig4cloud.pig.ontology.mapping.transform.TransformerRegistry;
import com.pig4cloud.pig.ontology.mapping.validation.IssueCollector;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidationContext;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 字段映射配置校验器（L1+L3 目标Schema，18-06 §3）。
 * <p>
 * 以 IssueCollector 模式复用 {@link com.pig4cloud.pig.ontology.mapping.validation.FieldMappingValidator} 的校验逻辑。
 * 校验规则：
 * <ul>
 *   <li>source_kind=COLUMN 时 source_column 非空；CONSTANT 时 constant_value 非空</li>
 *   <li>目标数据属性的 domainEntityTypeId 与实体映射的 targetEntityTypeId 一致</li>
 *   <li>转换器 code 在注册表中存在</li>
 *   <li>单位分类兼容</li>
 * </ul>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FieldMappingConfigValidator implements MappingValidator {

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntUnitMapper unitMapper;

	private final TransformerRegistry transformerRegistry;

	@Override
	public String code() {
		return "FieldMappingConfigValidator";
	}

	@Override
	public int order() {
		return 50;
	}

	@Override
	public void validate(MappingValidationContext context, IssueCollector issues) {
		if (context.isPrerequisiteFailed()) {
			issues.addSkipped(code());
			return;
		}

		for (OntEntityMapping em : context.getEntityMappings()) {
			if (!"1".equals(em.getEnabled())) {
				continue;
			}

			List<OntFieldMapping> fieldMappings = context.getFieldMappings(em.getId());
			for (OntFieldMapping fm : fieldMappings) {
				validateFieldMapping(fm, em, issues);
			}
		}

		log.debug("FieldMappingConfigValidator completed: {} issues", issues.getTotalCount());
	}

	private void validateFieldMapping(OntFieldMapping fm, OntEntityMapping em, IssueCollector issues) {
		String scopeRef = em.getMappingCode() + "." + fm.getFieldMappingCode();

		// 1. source_kind 一致性
		if ("COLUMN".equals(fm.getSourceKind())) {
			if (fm.getSourceColumn() == null || fm.getSourceColumn().isBlank()) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "FIELD",
						scopeRef, "source_kind=COLUMN 时 source_column 为空", "设置 source_column");
			}
		}
		else if ("CONSTANT".equals(fm.getSourceKind())) {
			if (fm.getConstantValue() == null || fm.getConstantValue().isBlank()) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "FIELD",
						scopeRef, "source_kind=CONSTANT 时 constant_value 为空", "设置 constant_value");
			}
		}
		else {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "FIELD",
					scopeRef, "非法 source_kind: " + fm.getSourceKind(), "使用 COLUMN 或 CONSTANT");
		}

		// 2. 目标数据属性存在
		if (fm.getTargetDataPropertyId() == null) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "FIELD",
					scopeRef, "未配置目标数据属性ID", "设置有效的 targetDataPropertyId");
			return;
		}
		OntDataProperty dataProperty = dataPropertyMapper.selectById(fm.getTargetDataPropertyId());
		if (dataProperty == null || "1".equals(dataProperty.getDelFlag())) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "FIELD",
					scopeRef, "目标数据属性不存在", "设置有效的数据属性");
			return;
		}

		// 3. 数据属性 domain 与实体映射的 targetEntityTypeId 一致
		if (dataProperty.getDomainEntityTypeId() == null
				|| !dataProperty.getDomainEntityTypeId().equals(em.getTargetEntityTypeId())) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "FIELD",
					scopeRef, "数据属性定义域与目标实体类型不一致", "选择 domain 匹配的数据属性");
		}

		// 4. 转换器已注册
		String transformerCode = (fm.getTransformer() != null && !fm.getTransformer().isBlank())
				? fm.getTransformer() : "IDENTITY";
		if (!transformerRegistry.isRegistered(transformerCode)) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "FIELD",
					scopeRef, "未知转换器: " + transformerCode, "使用已注册的转换器");
		}

		// 5. 单位分类兼容
		if (fm.getUnitId() != null) {
			OntUnit unit = unitMapper.selectById(fm.getUnitId());
			if (unit == null || "1".equals(unit.getDelFlag())) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "FIELD",
						scopeRef, "单位不存在", "设置有效的单位");
			}
			else if (dataProperty.getUnitCategoryId() != null
					&& !dataProperty.getUnitCategoryId().equals(unit.getCategoryId())) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "FIELD",
						scopeRef, "单位分类与属性要求不一致", "选择正确分类的单位");
			}
		}
		else if (dataProperty.getUnitRefMode() != null
				&& ("REQUIRED".equals(dataProperty.getUnitRefMode())
						|| "FIXED".equals(dataProperty.getUnitRefMode()))) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "FIELD",
					scopeRef, "属性要求单位但未配置 unit_id", "配置单位");
		}
	}

}
