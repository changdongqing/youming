/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.validators;

import com.pig4cloud.pig.ontology.mapping.ValidationErrorCode;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.validation.IssueCollector;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidationContext;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 增量与删除能力校验器（L8 增量能力，18-06 §11）。
 * <p>
 * 校验规则：
 * <ul>
 *   <li>incrementalColumn 存在时类型为 TIMESTAMP 或 NUMERIC</li>
 *   <li>无更新时间列则不可发布为 INCREMENTAL 计划运行类型</li>
 *   <li>有软删除列时可按值检测，Filter DSL 不得提前排除全部删除值</li>
 *   <li>INCREMENTAL 无 CDC 且无软删除列：物理删除不可检测，产生 WARNING</li>
 *   <li>deleteStrategy=SOFT_DELETE 且不可检测时至少 WARNING</li>
 * </ul>
 *
 * @author youming
 */
@Slf4j
@Component
public class IncrementalCapabilityValidator implements MappingValidator {

	@Override
	public String code() {
		return "IncrementalCapabilityValidator";
	}

	@Override
	public int order() {
		return 80;
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

			String scopeRef = em.getMappingCode();

			// 1. 增量列类型校验
			if (em.getIncrementalColumn() != null && !em.getIncrementalColumn().isBlank()) {
				String incrementalType = em.getIncrementalType();
				if (incrementalType == null || incrementalType.isBlank()) {
					issues.addWarning("INCREMENTAL_TYPE_MISSING", "ENTITY",
							scopeRef, "增量列 " + em.getIncrementalColumn() + " 未指定类型",
							"设置为 TIMESTAMP 或 NUMERIC");
				}
				else if (!"TIMESTAMP".equals(incrementalType) && !"NUMERIC".equals(incrementalType)) {
					issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "ENTITY",
							scopeRef, "增量列类型非法: " + incrementalType, "使用 TIMESTAMP 或 NUMERIC");
				}
			}

			// 2. 删除检测能力校验
			validateDeleteDetection(em, scopeRef, issues);
		}

		log.debug("IncrementalCapabilityValidator completed: {} issues", issues.getTotalCount());
	}

	private void validateDeleteDetection(OntEntityMapping em, String scopeRef, IssueCollector issues) {
		String deleteStrategy = em.getDeleteStrategy();
		if (deleteStrategy == null) {
			return;
		}

		boolean hasSoftDeleteColumn = em.getSourceDeleteFlagColumn() != null
				&& !em.getSourceDeleteFlagColumn().isBlank();

		switch (deleteStrategy) {
			case "SOFT_DELETE" -> {
				if (!hasSoftDeleteColumn) {
					issues.addWarning("DELETE_UNDETECTABLE", "ENTITY",
							scopeRef, "删除策略为 SOFT_DELETE 但未配置源删除标记列",
							"配置 sourceDeleteFlagColumn");
				}
				else if (em.getSourceDeleteValues() == null || em.getSourceDeleteValues().isBlank()) {
					issues.addWarning("DELETE_VALUES_MISSING", "ENTITY",
							scopeRef, "已配置删除标记列但未指定删除值",
							"配置 sourceDeleteValues");
				}
			}
			case "MARK_INACTIVE" -> {
				if (!hasSoftDeleteColumn) {
					issues.addWarning("DELETE_UNDETECTABLE", "ENTITY",
							scopeRef, "删除策略 MARK_INACTIVE 无软删除列，物理删除不可检测",
							"配置 sourceDeleteFlagColumn 或接受无法检测物理删除");
				}
			}
			case "IGNORE" -> {
				// IGNORE 不要求删除检测
			}
			case "BLOCK_AND_REVIEW" -> {
				// BLOCK_AND_REVIEW 不降低检测要求
				if (!hasSoftDeleteColumn) {
					issues.addWarning("DELETE_UNDETECTABLE", "ENTITY",
							scopeRef, "删除策略 BLOCK_AND_REVIEW 无软删除列，物理删除不可检测",
							"配置 sourceDeleteFlagColumn");
				}
			}
			default -> {
				// 其他策略不校验
			}
		}

		// INCREMENTAL 无 CDC 且无软删除列 → 物理删除不可检测
		if (hasSoftDeleteColumn && em.getFilterDsl() != null
				&& em.getFilterDsl().toLowerCase().contains("delete")) {
			issues.addWarning("DELETE_FILTER_RISK", "ENTITY",
					scopeRef, "Filter DSL 包含 delete 相关条件，可能提前排除删除值",
					"确保 Filter DSL 不排除所有删除标记值");
		}
	}

}
