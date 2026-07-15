/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.validators;

import com.pig4cloud.pig.ontology.mapping.ValidationErrorCode;
import com.pig4cloud.pig.ontology.mapping.validation.IssueCollector;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidationContext;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 映射工程状态校验器（L1 配置结构，18-06 §3）。
 * <p>
 * 校验规则：
 * <ul>
 *   <li>版本状态为 DRAFT（校验入口）</li>
 *   <li>工程状态非 ARCHIVED</li>
 *   <li>至少一个启用的实体映射</li>
 * </ul>
 * 前置失败后标记 {@code context.markPrerequisiteFailed()}，后续校验器跳过。
 *
 * @author youming
 */
@Slf4j
@Component
public class ProjectStateValidator implements MappingValidator {

	@Override
	public String code() {
		return "ProjectStateValidator";
	}

	@Override
	public int order() {
		return 10;
	}

	@Override
	public void validate(MappingValidationContext context, IssueCollector issues) {
		var version = context.getVersion();
		var project = context.getMappingProject();

		// 1. 工程状态非 ARCHIVED
		if ("ARCHIVED".equals(project.getProjectStatus())) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_201.getCode(), "PROJECT",
					project.getMappingCode(), "映射工程已归档，不可校验", "取消归档后重试");
			context.markPrerequisiteFailed();
			return;
		}

		// 2. 至少一个启用的实体映射
		if (context.getEnabledEntityMappings().isEmpty()) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "VERSION",
					version.getVersionNumber(), "映射版本没有启用的实体映射", "至少配置并启用一个实体映射");
			context.markPrerequisiteFailed();
			return;
		}

		// 3. 版本号格式合法
		if (version.getVersionNumber() == null || version.getVersionNumber().isBlank()) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "VERSION",
					null, "版本号为空", "设置合法的 MAJOR.MINOR.PATCH 版本号");
		}

		log.debug("ProjectStateValidator completed: {} issues", issues.getTotalCount());
	}

}
