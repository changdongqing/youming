/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.common.security.service.PigUser;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.mapping.ValidationErrorCode;
import com.pig4cloud.pig.ontology.mapping.datasource.entity.OntDataSource;
import com.pig4cloud.pig.ontology.mapping.datasource.entity.OntDataSourceMetadata;
import com.pig4cloud.pig.ontology.mapping.datasource.mapper.OntDataSourceMapper;
import com.pig4cloud.pig.ontology.mapping.datasource.mapper.OntDataSourceMetadataMapper;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.entity.OntFieldMapping;
import com.pig4cloud.pig.ontology.mapping.entity.OntRelationMapping;
import com.pig4cloud.pig.ontology.mapping.mapper.OntEntityMappingMapper;
import com.pig4cloud.pig.ontology.mapping.mapper.OntFieldMappingMapper;
import com.pig4cloud.pig.ontology.mapping.mapper.OntRelationMappingMapper;
import com.pig4cloud.pig.ontology.mapping.project.MappingErrorCode;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingProjectMapper;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingVersionMapper;
import com.pig4cloud.pig.ontology.mapping.project.snapshot.MappingSnapshotBuilder;
import com.pig4cloud.pig.ontology.mapping.project.snapshot.MappingSnapshotCanonicalizer;
import com.pig4cloud.pig.ontology.mapping.validation.IssueCollector;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidationContext;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidationService;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidatorRegistry;
import com.pig4cloud.pig.ontology.mapping.validation.entity.OntMappingValidationIssue;
import com.pig4cloud.pig.ontology.mapping.validation.entity.OntMappingValidationReport;
import com.pig4cloud.pig.ontology.mapping.validation.mapper.OntMappingValidationIssueMapper;
import com.pig4cloud.pig.ontology.mapping.validation.mapper.OntMappingValidationReportMapper;
import com.pig4cloud.pig.ontology.mapping.validation.vo.ValidationIssueVO;
import com.pig4cloud.pig.ontology.mapping.validation.vo.ValidationReportVO;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import com.pig4cloud.pig.ontology.version.entity.OntOntologyVersion;
import com.pig4cloud.pig.ontology.version.mapper.OntOntologyVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 映射校验服务实现（18-06 §7, §12, §13）。
 * <p>
 * 编排校验器执行、管理版本状态机转换（DRAFT→VALIDATING→VALIDATED）、
 * 写校验报告和问题、执行发布门禁检查和 WARNING 确认。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MappingValidationServiceImpl
		extends ServiceImpl<OntMappingValidationReportMapper, OntMappingValidationReport>
		implements MappingValidationService {

	private final OntMappingVersionMapper versionMapper;

	private final OntMappingProjectMapper projectMapper;

	private final OntOntologyProjectMapper ontologyProjectMapper;

	private final OntOntologyVersionMapper ontologyVersionMapper;

	private final OntEntityMappingMapper entityMappingMapper;

	private final OntFieldMappingMapper fieldMappingMapper;

	private final OntRelationMappingMapper relationMappingMapper;

	private final OntDataSourceMapper dataSourceMapper;

	private final OntDataSourceMetadataMapper metadataMapper;

	private final OntMappingValidationIssueMapper issueMapper;

	private final MappingValidatorRegistry validatorRegistry;

	private final MappingSnapshotBuilder snapshotBuilder;

	private final MappingSnapshotCanonicalizer snapshotCanonicalizer;

	private final ObjectMapper objectMapper;

	// ==================== validate ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ValidationReportVO validate(Long versionId, String triggerType) {
		OntMappingVersion version = versionMapper.selectById(versionId);
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_002.getMessage());
		}

		// 1. CAS: DRAFT → VALIDATING
		int updated = versionMapper.casUpdateStatus(versionId, "DRAFT", "VALIDATING", version.getRevision());
		if (updated == 0) {
			throw new IllegalStateException(ValidationErrorCode.ONT_MAP_201.getMessage()
					+ ": 仅 DRAFT 状态可校验，当前状态: " + version.getVersionStatus());
		}

		// 2. 重新加载版本和工程
		version = versionMapper.selectById(versionId);
		OntMappingProject project = projectMapper.selectById(version.getMappingProjectId());
		OntOntologyProject ontologyProject = ontologyProjectMapper.selectById(project.getOntologyId());

		// 3. 创建报告（RUNNING）
		OntMappingValidationReport report = createRunningReport(version, project, ontologyProject, triggerType);
		baseMapper.insert(report);

		try {
			// 4. 构建校验上下文
			MappingValidationContext context = buildContext(version, project, ontologyProject);

			// 5. 执行校验器
			IssueCollector issues = new IssueCollector();
			for (var validator : validatorRegistry.getSortedValidators()) {
				try {
					validator.validate(context, issues);
				}
				catch (Exception e) {
					log.error("Validator {} failed: {}", validator.code(), e.getMessage(), e);
					issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "VERSION", null,
							"校验器 " + validator.code() + " 执行异常: " + e.getMessage(), null);
				}
			}

			// 6. 更新报告状态和计数
			boolean passed = !issues.hasViolations();
			updateReportCompletion(report, issues, passed);

			// 7. 批量写入问题
			saveIssues(report.getId(), issues.getIssues());

			if (passed) {
				// 8a. PASSED: CAS VALIDATING → VALIDATED
				int validated = versionMapper.casUpdateStatus(versionId, "VALIDATING", "VALIDATED",
						version.getRevision());
				if (validated == 0) {
					throw new IllegalStateException(MappingErrorCode.ONT_MAP_007.getMessage());
				}

				// 绑定校验信息到版本
				bindValidationToVersion(versionId, report, context);
			}
			else {
				// 8b. FAILED: CAS VALIDATING → DRAFT
				versionMapper.casUpdateStatus(versionId, "VALIDATING", "DRAFT", version.getRevision());
			}

			log.info("Validation completed: versionId={}, reportId={}, passed={}, violations={}, warnings={}",
					versionId, report.getId(), passed, issues.getViolationCount(), issues.getWarningCount());

		}
		catch (Exception e) {
			// 异常时回退状态
			log.error("Validation failed unexpectedly, rolling back version status: versionId={}", versionId, e);
			versionMapper.casUpdateStatus(versionId, "VALIDATING", "DRAFT", version.getRevision());
			report.setReportStatus("FAILED");
			report.setCompletedAt(LocalDateTime.now());
			baseMapper.updateById(report);
			throw e;
		}

		return toReportVO(baseMapper.selectById(report.getId()));
	}

	// ==================== getReport ====================

	@Override
	public ValidationReportVO getReport(Long reportId) {
		OntMappingValidationReport report = baseMapper.selectById(reportId);
		if (report == null || "1".equals(report.getDelFlag())) {
			throw new IllegalArgumentException("校验报告不存在: " + reportId);
		}
		return toReportVO(report);
	}

	// ==================== getIssues ====================

	@Override
	public Page<ValidationIssueVO> getIssues(Long reportId, Page<OntMappingValidationIssue> page, String severity) {
		Page<OntMappingValidationIssue> result = issueMapper.selectPage(page,
				Wrappers.<OntMappingValidationIssue>lambdaQuery()
						.eq(OntMappingValidationIssue::getReportId, reportId)
						.eq(OntMappingValidationIssue::getDelFlag, "0")
						.eq(StrUtil.isNotBlank(severity), OntMappingValidationIssue::getSeverity, severity)
						.orderByAsc(OntMappingValidationIssue::getSortOrder));

		Page<ValidationIssueVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
		voPage.setRecords(result.getRecords().stream().map(this::toIssueVO).toList());
		return voPage;
	}

	// ==================== acknowledgeIssue ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ValidationIssueVO acknowledgeIssue(Long issueId) {
		OntMappingValidationIssue issue = issueMapper.selectById(issueId);
		if (issue == null || "1".equals(issue.getDelFlag())) {
			throw new IllegalArgumentException("校验问题不存在: " + issueId);
		}

		// VIOLATION 没有确认接口
		if ("VIOLATION".equals(issue.getSeverity())) {
			throw new IllegalStateException(ValidationErrorCode.ONT_MAP_209.getMessage()
					+ ": VIOLATION 不可确认");
		}

		// 仅 WARNING 可确认
		if (!"WARNING".equals(issue.getSeverity())) {
			throw new IllegalStateException("仅 WARNING 可确认");
		}

		if ("1".equals(issue.getAcknowledged())) {
			return toIssueVO(issue); // 已确认，幂等返回
		}

		// 写 ack 字段和审计
		issue.setAcknowledged("1");
		issue.setAcknowledgedBy(getCurrentUser());
		issue.setAcknowledgedAt(LocalDateTime.now());
		issueMapper.updateById(issue);

		log.info("Issue acknowledged: issueId={}, by={}", issueId, issue.getAcknowledgedBy());
		return toIssueVO(issue);
	}

	// ==================== assertPublishGate ====================

	@Override
	public void assertPublishGate(Long versionId) {
		OntMappingVersion version = versionMapper.selectById(versionId);
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_002.getMessage());
		}

		// 1. 版本状态必须 VALIDATED
		if (!"VALIDATED".equals(version.getVersionStatus())) {
			throw new IllegalStateException(ValidationErrorCode.ONT_MAP_211.getMessage()
					+ ": 版本状态非 VALIDATED: " + version.getVersionStatus());
		}

		// 2. 必须有校验报告
		if (version.getValidationReportId() == null) {
			throw new IllegalStateException(ValidationErrorCode.ONT_MAP_211.getMessage()
					+ ": 版本无校验报告");
		}

		// 锁定报告行
		OntMappingValidationReport report = baseMapper.selectForUpdate(version.getValidationReportId());
		if (report == null || "1".equals(report.getDelFlag())) {
			throw new IllegalStateException(ValidationErrorCode.ONT_MAP_210.getMessage()
					+ ": 校验报告不存在");
		}

		// 3. 报告必须 PASSED
		if (!"PASSED".equals(report.getReportStatus())) {
			throw new IllegalStateException(ValidationErrorCode.ONT_MAP_211.getMessage()
					+ ": 校验报告状态非 PASSED: " + report.getReportStatus());
		}

		// 4. violationCount 必须为 0
		if (report.getViolationCount() != null && report.getViolationCount() > 0) {
			throw new IllegalStateException(ValidationErrorCode.ONT_MAP_211.getMessage()
					+ ": 存在 " + report.getViolationCount() + " 个 VIOLATION");
		}

		// 5. 所有 WARNING 必须已确认
		int unacknowledgedWarnings = issueMapper.countUnacknowledgedWarnings(report.getId());
		if (unacknowledgedWarnings > 0) {
			throw new IllegalStateException(ValidationErrorCode.ONT_MAP_209.getMessage()
					+ ": 存在 " + unacknowledgedWarnings + " 个未确认 WARNING");
		}

		// 6. config revision 必须与报告一致
		if (version.getRevision() != null && report.getConfigRevision() != null
				&& !version.getRevision().equals(report.getConfigRevision())) {
			throw new IllegalStateException(ValidationErrorCode.ONT_MAP_210.getMessage()
					+ ": 配置 revision 已变更（版本=" + version.getRevision()
					+ ", 报告=" + report.getConfigRevision() + "）");
		}

		log.debug("Publish gate passed: versionId={}, reportId={}", versionId, report.getId());
	}

	// ==================== 内部方法 ====================

	private OntMappingValidationReport createRunningReport(OntMappingVersion version,
			OntMappingProject project, OntOntologyProject ontologyProject, String triggerType) {
		OntMappingValidationReport report = new OntMappingValidationReport();
		report.setMappingVersionId(version.getId());
		report.setReportStatus("RUNNING");
		report.setTriggerType(triggerType != null ? triggerType : "MANUAL");
		report.setConfigRevision(version.getRevision());
		report.setOntologyVersionId(ontologyProject != null ? ontologyProject.getCurrentVersionId() : 0L);
		report.setWorkspaceRevision(ontologyProject != null ? ontologyProject.getWorkspaceRevision() : 0L);
		report.setMetadataHashSummary("pending");
		report.setSampleSize(0);
		report.setViolationCount(0);
		report.setWarningCount(0);
		report.setInfoCount(0);
		report.setStartedAt(LocalDateTime.now());
		report.setRequestedBy(getCurrentUser());
		return report;
	}

	private MappingValidationContext buildContext(OntMappingVersion version, OntMappingProject project,
			OntOntologyProject ontologyProject) {
		// 加载映射子表
		List<OntEntityMapping> entityMappings = entityMappingMapper.selectList(
				Wrappers.<OntEntityMapping>lambdaQuery()
						.eq(OntEntityMapping::getMappingVersionId, version.getId())
						.eq(OntEntityMapping::getDelFlag, "0")
						.orderByAsc(OntEntityMapping::getSyncOrder));

		List<Long> entityMappingIds = entityMappings.stream()
				.map(OntEntityMapping::getId).toList();

		List<OntFieldMapping> fieldMappings = entityMappingIds.isEmpty()
				? List.of()
				: fieldMappingMapper.selectList(
						Wrappers.<OntFieldMapping>lambdaQuery()
								.in(OntFieldMapping::getEntityMappingId, entityMappingIds)
								.eq(OntFieldMapping::getDelFlag, "0")
								.orderByAsc(OntFieldMapping::getSortOrder));

		List<OntRelationMapping> relationMappings = relationMappingMapper.selectList(
				Wrappers.<OntRelationMapping>lambdaQuery()
						.eq(OntRelationMapping::getMappingVersionId, version.getId())
						.eq(OntRelationMapping::getDelFlag, "0")
						.orderByAsc(OntRelationMapping::getSyncOrder));

		// 加载数据源
		Set<Long> sourceIds = new java.util.HashSet<>();
		entityMappings.forEach(em -> { if (em.getSourceId() != null) sourceIds.add(em.getSourceId()); });
		relationMappings.forEach(rm -> { if (rm.getSourceId() != null) sourceIds.add(rm.getSourceId()); });

		Map<Long, OntDataSource> dataSourceMap = sourceIds.isEmpty()
				? Map.of()
				: dataSourceMapper.selectBatchIds(sourceIds).stream()
						.filter(ds -> !"1".equals(ds.getDelFlag()))
						.collect(Collectors.toMap(OntDataSource::getId, ds -> ds));

		// 加载元数据
		Map<Long, List<OntDataSourceMetadata>> metadataMap = new HashMap<>();
		for (Long sourceId : sourceIds) {
			List<OntDataSourceMetadata> metadatas = metadataMapper.selectList(
					Wrappers.<OntDataSourceMetadata>lambdaQuery()
							.eq(OntDataSourceMetadata::getSourceId, sourceId)
							.eq(OntDataSourceMetadata::getDelFlag, "0"));
			metadataMap.put(sourceId, metadatas);
		}

		// 加载本体版本
		OntOntologyVersion ontologyVersion = null;
		if (ontologyProject != null && ontologyProject.getCurrentVersionId() != null) {
			ontologyVersion = ontologyVersionMapper.selectById(ontologyProject.getCurrentVersionId());
		}

		// 计算候选配置哈希
		String validatedVersionNumber = ontologyVersion != null ? ontologyVersion.getVersionNumber() : null;
		String snapshotJson = snapshotBuilder.buildSnapshot(project, version, validatedVersionNumber);
		String canonicalJson = snapshotCanonicalizer.canonicalize(snapshotJson);
		String candidateConfigHash = snapshotCanonicalizer.computeHash(canonicalJson);

		// 元数据哈希摘要
		String metadataHashSummary = computeMetadataHashSummary(metadataMap);

		return new MappingValidationContext(version, project, ontologyProject, ontologyVersion,
				entityMappings, fieldMappings, relationMappings, dataSourceMap, metadataMap,
				candidateConfigHash, metadataHashSummary);
	}

	private void updateReportCompletion(OntMappingValidationReport report, IssueCollector issues, boolean passed) {
		report.setReportStatus(passed ? "PASSED" : "FAILED");
		report.setViolationCount(issues.getViolationCount());
		report.setWarningCount(issues.getWarningCount());
		report.setInfoCount(issues.getInfoCount());
		report.setCompletedAt(LocalDateTime.now());

		// 摘要 JSON
		Map<String, Object> summary = new HashMap<>();
		summary.put("passed", passed);
		summary.put("totalIssues", issues.getTotalCount());
		summary.put("violations", issues.getViolationCount());
		summary.put("warnings", issues.getWarningCount());
		summary.put("infos", issues.getInfoCount());
		try {
			report.setSummaryJson(objectMapper.writeValueAsString(summary));
		}
		catch (Exception e) {
			report.setSummaryJson("{}");
		}

		baseMapper.updateById(report);
	}

	private void saveIssues(Long reportId, List<OntMappingValidationIssue> issues) {
		for (OntMappingValidationIssue issue : issues) {
			issue.setReportId(reportId);
			issueMapper.insert(issue);
		}
	}

	private void bindValidationToVersion(Long versionId, OntMappingValidationReport report,
			MappingValidationContext context) {
		OntMappingVersion version = versionMapper.selectById(versionId);

		version.setValidatedOntologyVersionId(context.getOntologyProject() != null
				? context.getOntologyProject().getCurrentVersionId() : null);
		version.setValidatedWorkspaceRevision(context.getOntologyProject() != null
				? context.getOntologyProject().getWorkspaceRevision() : null);
		version.setValidationReportId(report.getId());
		version.setValidationSummary(report.getSummaryJson());
		versionMapper.updateById(version);
	}

	private String computeMetadataHashSummary(Map<Long, List<OntDataSourceMetadata>> metadataMap) {
		List<String> hashes = new ArrayList<>();
		for (var entry : metadataMap.entrySet()) {
			for (var meta : entry.getValue()) {
				if (meta.getMetadataHash() != null) {
					hashes.add(meta.getMetadataHash());
				}
			}
		}
		if (hashes.isEmpty()) {
			return "none";
		}
		// 简单拼接后 SHA-256
		return snapshotCanonicalizer.computeHash(String.join("|", hashes));
	}

	private String getCurrentUser() {
		try {
			PigUser user = SecurityUtils.getUser();
			if (user != null) {
				return user.getUsername();
			}
		}
		catch (Exception e) {
			// 安全上下文不可用时回退
		}
		return "system";
	}

	private ValidationReportVO toReportVO(OntMappingValidationReport report) {
		ValidationReportVO vo = new ValidationReportVO();
		vo.setId(report.getId());
		vo.setMappingVersionId(report.getMappingVersionId());
		vo.setReportStatus(report.getReportStatus());
		vo.setTriggerType(report.getTriggerType());
		vo.setConfigRevision(report.getConfigRevision());
		vo.setCandidateConfigHash(report.getCandidateConfigHash());
		vo.setOntologyVersionId(report.getOntologyVersionId());
		vo.setWorkspaceRevision(report.getWorkspaceRevision());
		vo.setMetadataHashSummary(report.getMetadataHashSummary());
		vo.setSampleSize(report.getSampleSize());
		vo.setViolationCount(report.getViolationCount());
		vo.setWarningCount(report.getWarningCount());
		vo.setInfoCount(report.getInfoCount());
		vo.setSummaryJson(report.getSummaryJson());
		vo.setStartedAt(report.getStartedAt());
		vo.setCompletedAt(report.getCompletedAt());
		vo.setRequestedBy(report.getRequestedBy());
		vo.setTraceId(report.getTraceId());
		vo.setCreateTime(report.getCreateTime());
		return vo;
	}

	private ValidationIssueVO toIssueVO(OntMappingValidationIssue issue) {
		ValidationIssueVO vo = new ValidationIssueVO();
		vo.setId(issue.getId());
		vo.setReportId(issue.getReportId());
		vo.setSeverity(issue.getSeverity());
		vo.setIssueCode(issue.getIssueCode());
		vo.setScopeType(issue.getScopeType());
		vo.setScopeRef(issue.getScopeRef());
		vo.setMessage(issue.getMessage());
		vo.setSuggestion(issue.getSuggestion());
		vo.setSourceRecordKeyHash(issue.getSourceRecordKeyHash());
		vo.setAcknowledged(issue.getAcknowledged());
		vo.setAcknowledgedBy(issue.getAcknowledgedBy());
		vo.setAcknowledgedAt(issue.getAcknowledgedAt());
		vo.setSortOrder(issue.getSortOrder());
		return vo;
	}

}
