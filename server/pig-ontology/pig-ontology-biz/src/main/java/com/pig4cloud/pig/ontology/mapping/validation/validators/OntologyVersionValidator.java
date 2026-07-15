/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.validators;

import com.pig4cloud.pig.ontology.mapping.ValidationErrorCode;
import com.pig4cloud.pig.ontology.mapping.project.support.MappingSemVerValidator;
import com.pig4cloud.pig.ontology.mapping.validation.IssueCollector;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidationContext;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 本体版本校验器（L4 本体版本，18-06 §3）。
 * <p>
 * 校验规则：
 * <ul>
 *   <li>本体工程有 current version</li>
 *   <li>当前本体版本为 PUBLISHED</li>
 *   <li>当前版本号满足映射版本约束</li>
 *   <li>工作区状态 EDITABLE</li>
 *   <li>工作区修订号一致（否则 SCHEMA_DRIFT WARNING）</li>
 * </ul>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OntologyVersionValidator implements MappingValidator {

	private final MappingSemVerValidator semVerValidator;

	@Override
	public String code() {
		return "OntologyVersionValidator";
	}

	@Override
	public int order() {
		return 30;
	}

	@Override
	public void validate(MappingValidationContext context, IssueCollector issues) {
		if (context.isPrerequisiteFailed()) {
			issues.addSkipped(code());
			return;
		}

		var ontologyProject = context.getOntologyProject();
		var version = context.getVersion();

		// 1. 本体工程必须有 current version
		if (ontologyProject == null || ontologyProject.getCurrentVersionId() == null) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_203.getCode(), "VERSION",
					version.getVersionNumber(), "关联的本体工程无已发布版本", "发布本体版本后重新校验");
			context.markPrerequisiteFailed();
			return;
		}

		var ontologyVersion = context.getOntologyVersion();
		if (ontologyVersion == null) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_203.getCode(), "VERSION",
					version.getVersionNumber(), "当前本体版本不存在", "检查本体工程状态");
			context.markPrerequisiteFailed();
			return;
		}

		// 2. 当前本体版本必须 PUBLISHED
		if (!"PUBLISHED".equals(ontologyVersion.getReleaseStatus())) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_203.getCode(), "VERSION",
					ontologyVersion.getVersionNumber(),
					"当前本体版本未发布: " + ontologyVersion.getReleaseStatus(),
					"发布本体版本后重新校验");
			context.markPrerequisiteFailed();
			return;
		}

		// 3. 版本号必须满足约束
		if (version.getOntologyVersionConstraint() != null && !version.getOntologyVersionConstraint().isBlank()) {
			if (!semVerValidator.isSatisfied(ontologyVersion.getVersionNumber(),
					version.getOntologyVersionConstraint())) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_203.getCode(), "VERSION",
						version.getVersionNumber(),
						"本体版本号 " + ontologyVersion.getVersionNumber()
								+ " 不满足约束 " + version.getOntologyVersionConstraint(),
						"调整映射版本的本体版本约束");
			}
		}

		// 4. 工作区状态必须 EDITABLE
		if (ontologyProject.getWorkspaceStatus() != null
				&& !"EDITABLE".equals(ontologyProject.getWorkspaceStatus())) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_203.getCode(), "VERSION",
					version.getVersionNumber(),
					"本体工作区状态非 EDITABLE: " + ontologyProject.getWorkspaceStatus(),
					"发布或回退本体工作区变更");
		}

		// 5. 工作区修订号漂移检查（WARNING 而非 VIOLATION）
		if (ontologyProject.getWorkspaceRevision() != null
				&& version.getValidatedWorkspaceRevision() != null
				&& !ontologyProject.getWorkspaceRevision().equals(version.getValidatedWorkspaceRevision())) {
			issues.addWarning("SCHEMA_DRIFT", "VERSION", version.getVersionNumber(),
					"工作区修订号已变更，预览结果可能不反映最新Schema",
					"发布新的本体版本并重新校验");
		}

		log.debug("OntologyVersionValidator completed: {} issues", issues.getTotalCount());
	}

}
