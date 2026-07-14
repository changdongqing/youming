/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.guard;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.mapping.project.MappingErrorCode;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingVersionMapper;
import com.pig4cloud.pig.ontology.mapping.project.support.MappingSemVerValidator;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import com.pig4cloud.pig.ontology.version.entity.OntOntologyVersion;
import com.pig4cloud.pig.ontology.version.mapper.OntOntologyVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 映射版本-本体版本兼容守卫（18-03 §9）。
 * <p>
 * 生产执行检查：
 * <ul>
 *   <li>{@code ontology_project.current_version_id} 存在</li>
 *   <li>当前本体版本为 PUBLISHED</li>
 *   <li>当前版本号满足 constraint</li>
 *   <li>当前版本ID等于 {@code validated_ontology_version_id}</li>
 *   <li>当前 {@code workspace_revision} 等于 {@code validated_workspace_revision}</li>
 *   <li>{@code workspace_status=EDITABLE}</li>
 * </ul>
 * 不能仅因版本号仍满足范围就跳过重新校验，因为 Schema 可能在同一兼容范围内改变字段定义。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MappingOntologyVersionGuard {

	private final OntMappingVersionMapper versionMapper;

	private final OntOntologyProjectMapper projectMapper;

	private final OntOntologyVersionMapper ontologyVersionMapper;

	private final MappingSemVerValidator semVerValidator;

	/**
	 * 校验映射版本对预览的兼容性（允许工作区漂移，但报告 SCHEMA_DRIFT）。
	 * @param mappingVersionId 映射版本ID
	 * @return 校验结果
	 */
	public VersionGuardResult validateForPreview(Long mappingVersionId) {
		OntMappingVersion version = versionMapper.selectById(mappingVersionId);
		if (version == null || "1".equals(version.getDelFlag())) {
			return VersionGuardResult.fail(MappingErrorCode.ONT_MAP_002.getMessage());
		}

		OntOntologyProject project = projectMapper.selectById(version.getMappingProjectId());
		if (project == null || "1".equals(project.getDelFlag())) {
			return VersionGuardResult.fail(MappingErrorCode.ONT_MAP_016.getMessage());
		}

		// 预览允许工作区漂移，但需报告
		VersionGuardResult result = checkCoreConstraints(version, project, false);
		if (result.isSuccess() && !isWorkspaceRevisionConsistent(version, project)) {
			return VersionGuardResult.successWithWarning("SCHEMA_DRIFT",
					"工作区修订号已变更，预览结果可能不反映最新Schema");
		}
		return result;
	}

	/**
	 * 断言映射版本可执行生产作业（严格校验，不允许漂移）。
	 * @param mappingVersionId 映射版本ID
	 * @throws IllegalStateException 如果不满足执行条件
	 */
	public void assertExecutable(Long mappingVersionId) {
		OntMappingVersion version = versionMapper.selectById(mappingVersionId);
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_002.getMessage());
		}

		OntOntologyProject project = projectMapper.selectById(version.getMappingProjectId());
		if (project == null || "1".equals(project.getDelFlag())) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_016.getMessage());
		}

		VersionGuardResult result = checkCoreConstraints(version, project, true);
		if (!result.isSuccess()) {
			throw new IllegalStateException(result.getErrorMessage());
		}
	}

	/**
	 * 断言映射版本可发布（校验本体版本一致性和工作区修订一致性）。
	 * @param version 映射版本
	 * @param mappingProject 映射工程
	 * @throws IllegalStateException 如果不满足发布条件
	 */
	public void assertPublishable(OntMappingVersion version, OntMappingProject mappingProject) {
		// 通过映射工程获取本体工程
		OntOntologyProject project = projectMapper.selectById(mappingProject.getOntologyId());
		if (project == null || "1".equals(project.getDelFlag())) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_016.getMessage());
		}

		// 1. 工程必须有已发布本体版本
		if (project.getCurrentVersionId() == null) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_016.getMessage());
		}

		// 2. 工作区状态必须可编辑
		if (project.getWorkspaceStatus() != null && !"EDITABLE".equals(project.getWorkspaceStatus())) {
			throw new IllegalStateException(
					MappingErrorCode.ONT_MAP_009.getMessage() + ": 工作区状态为 " + project.getWorkspaceStatus());
		}

		// 3. 当前本体版本ID必须与校验时一致
		if (version.getValidatedOntologyVersionId() == null
				|| !version.getValidatedOntologyVersionId().equals(project.getCurrentVersionId())) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_008.getMessage()
					+ ": 当前本体版本ID与校验时不一致");
		}

		// 4. 当前本体版本必须为 PUBLISHED
		OntOntologyVersion ontologyVersion = ontologyVersionMapper.selectById(project.getCurrentVersionId());
		if (ontologyVersion == null || !"PUBLISHED".equals(ontologyVersion.getReleaseStatus())) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_008.getMessage()
					+ ": 当前本体版本未发布");
		}

		// 5. 版本号必须满足约束
		if (!semVerValidator.isSatisfied(ontologyVersion.getVersionNumber(),
				version.getOntologyVersionConstraint())) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_008.getMessage()
					+ ": 当前本体版本号 " + ontologyVersion.getVersionNumber()
					+ " 不满足约束 " + version.getOntologyVersionConstraint());
		}

		// 6. 工作区修订号必须与校验时一致
		if (!isWorkspaceRevisionConsistent(version, project)) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_009.getMessage());
		}
	}

	/**
	 * 核心约束检查（共用逻辑）。
	 * @param strict true=生产执行严格模式，false=预览宽松模式
	 */
	private VersionGuardResult checkCoreConstraints(OntMappingVersion version, OntOntologyProject project,
			boolean strict) {
		// 1. 映射版本必须已发布
		if (!"PUBLISHED".equals(version.getVersionStatus())) {
			return VersionGuardResult.fail("映射版本未发布，不可执行生产作业");
		}

		// 2. 工程必须有当前本体版本
		if (project.getCurrentVersionId() == null) {
			return VersionGuardResult.fail(MappingErrorCode.ONT_MAP_016.getMessage());
		}

		// 3. 加载当前本体版本
		OntOntologyVersion ontologyVersion = ontologyVersionMapper.selectById(project.getCurrentVersionId());
		if (ontologyVersion == null) {
			return VersionGuardResult.fail(MappingErrorCode.ONT_MAP_016.getMessage());
		}

		// 4. 本体版本必须已发布
		if (!"PUBLISHED".equals(ontologyVersion.getReleaseStatus())) {
			return VersionGuardResult.fail(MappingErrorCode.ONT_MAP_008.getMessage()
					+ ": 当前本体版本未发布");
		}

		// 5. 版本号必须满足约束
		if (!semVerValidator.isSatisfied(ontologyVersion.getVersionNumber(),
				version.getOntologyVersionConstraint())) {
			return VersionGuardResult.fail(MappingErrorCode.ONT_MAP_008.getMessage()
					+ ": 版本号 " + ontologyVersion.getVersionNumber()
					+ " 不满足约束 " + version.getOntologyVersionConstraint());
		}

		// 6. 生产执行：版本ID必须与校验时一致
		if (strict) {
			if (version.getValidatedOntologyVersionId() == null
					|| !version.getValidatedOntologyVersionId().equals(project.getCurrentVersionId())) {
				return VersionGuardResult.fail(MappingErrorCode.ONT_MAP_008.getMessage()
						+ ": 当前本体版本ID与校验时不一致");
			}
			if (!isWorkspaceRevisionConsistent(version, project)) {
				return VersionGuardResult.fail(MappingErrorCode.ONT_MAP_009.getMessage());
			}
		}

		return VersionGuardResult.success();
	}

	/**
	 * 工作区修订号是否一致。
	 */
	private boolean isWorkspaceRevisionConsistent(OntMappingVersion version, OntOntologyProject project) {
		if (version.getValidatedWorkspaceRevision() == null || project.getWorkspaceRevision() == null) {
			return true;
		}
		return version.getValidatedWorkspaceRevision().equals(project.getWorkspaceRevision());
	}

	/**
	 * 版本守卫校验结果。
	 */
	public static class VersionGuardResult {

		private final boolean success;

		private final String warningCode;

		private final String warningMessage;

		private final String errorMessage;

		private VersionGuardResult(boolean success, String warningCode, String warningMessage, String errorMessage) {
			this.success = success;
			this.warningCode = warningCode;
			this.warningMessage = warningMessage;
			this.errorMessage = errorMessage;
		}

		public static VersionGuardResult success() {
			return new VersionGuardResult(true, null, null, null);
		}

		public static VersionGuardResult successWithWarning(String code, String message) {
			return new VersionGuardResult(true, code, message, null);
		}

		public static VersionGuardResult fail(String errorMessage) {
			return new VersionGuardResult(false, null, null, errorMessage);
		}

		public boolean isSuccess() {
			return success;
		}

		public String getWarningCode() {
			return warningCode;
		}

		public String getWarningMessage() {
			return warningMessage;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

	}

}
