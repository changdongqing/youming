/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.integration;

import com.pig4cloud.pig.ontology.security.audit.DataAccessAuditService;
import com.pig4cloud.pig.ontology.security.policy.SecurityConstants;
import com.pig4cloud.pig.ontology.security.policy.SecuritySubject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 映射审计门面（18-08 §9 数据访问审计）。
 * <p>
 * 封装模块36 {@link DataAccessAuditService}，提供映射场景的语义化审计方法。
 * 审计链使用模块36 {@code DataAccessAuditService}，resourceRef 使用 ID 组合，不写源记录明文：
 * <pre>
 * mappingProject:100/version:5/job:900
 * </pre>
 * <p>
 * 必须审计的操作（§9）：
 * <ul>
 *   <li>数据源新增、修改、测试、启停、元数据刷新</li>
 *   <li>映射工程/版本新增、编辑、校验、WARNING确认、发布、停用</li>
 *   <li>FULL/INCREMENTAL/RETRY作业创建、取消、恢复、游标重置</li>
 *   <li>强制人工覆盖来源值</li>
 *   <li>高风险删除策略执行</li>
 *   <li>作业错误详情查看</li>
 *   <li>凭证解密失败和策略拒绝</li>
 * </ul>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MappingAuditFacade {

	private final DataAccessAuditService auditService;

	/**
	 * 审计数据源操作。
	 *
	 * @param ontologyId   本体工程ID
	 * @param subject      安全主体
	 * @param sourceId     数据源ID
	 * @param operation    操作：CREATED/UPDATED/TESTED/ENABLED/DISABLED/METADATA_REFRESHED
	 * @param outcome      结果：SUCCESS/FAILED
	 * @param errorCode    错误码（失败时）
	 * @param traceId      追踪ID
	 */
	public void auditDataSourceOperation(Long ontologyId, SecuritySubject subject, Long sourceId,
			String operation, String outcome, String errorCode, String traceId) {
		String resourceRef = buildResourceRef(SecurityConstants.RESOURCE_DATA_SOURCE, sourceId);
		auditService.recordAudit(ontologyId, getUserId(subject), getUsername(subject),
				"DATA_SOURCE_" + operation, SecurityConstants.RESOURCE_DATA_SOURCE, resourceRef,
				operation, 1L, null, "ALLOW", outcome, errorCode, traceId);
	}

	/**
	 * 审计映射工程/版本操作。
	 *
	 * @param ontologyId   本体工程ID
	 * @param subject      安全主体
	 * @param projectId    映射工程ID
	 * @param versionId    映射版本ID（可为null）
	 * @param operation    操作：CREATED/EDITED/VALIDATED/WARNING_CONFIRMED/PUBLISHED/RETIRED
	 * @param outcome      结果：SUCCESS/FAILED
	 * @param errorCode    错误码（失败时）
	 * @param traceId      追踪ID
	 */
	public void auditMappingProject(Long ontologyId, SecuritySubject subject, Long projectId,
			Long versionId, String operation, String outcome, String errorCode, String traceId) {
		String resourceRef = buildProjectRef(projectId, versionId, null);
		auditService.recordAudit(ontologyId, getUserId(subject), getUsername(subject),
				"MAPPING_PROJECT_" + operation, SecurityConstants.RESOURCE_MAPPING_PROJECT,
				resourceRef, operation, 1L, null, "ALLOW", outcome, errorCode, traceId);
	}

	/**
	 * 审计映射作业生命周期。
	 *
	 * @param ontologyId   本体工程ID
	 * @param subject      安全主体
	 * @param projectId    映射工程ID
	 * @param versionId    映射版本ID
	 * @param jobId        作业ID
	 * @param operation    操作：CREATED/STARTED/COMPLETED/FAILED/CANCELLED/RECOVERED/CURSOR_RESET
	 * @param outcome      结果：SUCCESS/FAILED
	 * @param errorCode    错误码（失败时）
	 * @param traceId      追踪ID
	 */
	public void auditMappingJob(Long ontologyId, SecuritySubject subject, Long projectId,
			Long versionId, Long jobId, String operation, String outcome, String errorCode,
			String traceId) {
		String resourceRef = buildProjectRef(projectId, versionId, jobId);
		auditService.recordAudit(ontologyId, getUserId(subject), getUsername(subject),
				"MAPPING_JOB_" + operation, SecurityConstants.RESOURCE_MAPPING_JOB,
				resourceRef, operation, 1L, null, "ALLOW", outcome, errorCode, traceId);
	}

	/**
	 * 审计强制人工覆盖来源值。
	 *
	 * @param ontologyId    本体工程ID
	 * @param subject       安全主体
	 * @param dataValueId   数据值ID
	 * @param fieldMappingCode 字段映射编码
	 * @param traceId       追踪ID
	 */
	public void auditValueOverride(Long ontologyId, SecuritySubject subject, Long dataValueId,
			String fieldMappingCode, String traceId) {
		String resourceRef = SecurityConstants.RESOURCE_ENTITY_INSTANCE + ":" + dataValueId;
		auditService.recordAudit(ontologyId, getUserId(subject), getUsername(subject),
				"MAPPING_VALUE_OVERRIDE", SecurityConstants.RESOURCE_ENTITY_INSTANCE,
				resourceRef, "FORCE_OVERRIDE:" + fieldMappingCode, 1L, null,
				"ALLOW", "SUCCESS", null, traceId);
	}

	/**
	 * 审计安全拒绝。
	 *
	 * @param ontologyId   本体工程ID
	 * @param subject      安全主体
	 * @param resourceType 资源类型
	 * @param resourceRef  资源引用
	 * @param reasonCode   拒绝原因码
	 * @param traceId      追踪ID
	 */
	public void auditSecurityDenial(Long ontologyId, SecuritySubject subject, String resourceType,
			String resourceRef, String reasonCode, String traceId) {
		auditService.recordAudit(ontologyId, getUserId(subject), getUsername(subject),
				"MAPPING_SECURITY_DENIED", resourceType, resourceRef,
				"DENIED:" + reasonCode, 0L, null, "DENY", "DENIED", reasonCode, traceId);
	}

	/**
	 * 审计凭证解密失败。
	 *
	 * @param ontologyId  本体工程ID
	 * @param subject     安全主体
	 * @param sourceId    数据源ID
	 * @param errorCode   错误码
	 * @param traceId     追踪ID
	 */
	public void auditCredentialFailure(Long ontologyId, SecuritySubject subject, Long sourceId,
			String errorCode, String traceId) {
		String resourceRef = buildResourceRef(SecurityConstants.RESOURCE_DATA_SOURCE, sourceId);
		auditService.recordAudit(ontologyId, getUserId(subject), getUsername(subject),
				"CREDENTIAL_DECRYPT_FAILED", SecurityConstants.RESOURCE_DATA_SOURCE,
				resourceRef, "DECRYPT_FAILED", 0L, null, "DENY", "FAILED", errorCode, traceId);
	}

	/**
	 * 审计高风险操作执行（审批消费后）。
	 *
	 * @param ontologyId    本体工程ID
	 * @param subject       安全主体
	 * @param operationType 操作类型（如 MAPPING_RESET_CURSOR）
	 * @param targetRef     目标引用
	 * @param outcome       结果
	 * @param errorCode     错误码
	 * @param traceId       追踪ID
	 */
	public void auditHighRiskOperation(Long ontologyId, SecuritySubject subject,
			String operationType, String targetRef, String outcome, String errorCode, String traceId) {
		auditService.recordAudit(ontologyId, getUserId(subject), getUsername(subject),
				"MAPPING_HIGH_RISK:" + operationType, SecurityConstants.RESOURCE_MAPPING_PROJECT,
				targetRef, operationType, 1L, null, "ALLOW", outcome, errorCode, traceId);
	}

	// ==================== 内部方法 ====================

	private Long getUserId(SecuritySubject subject) {
		return subject != null ? subject.getUserId() : null;
	}

	private String getUsername(SecuritySubject subject) {
		return subject != null && subject.getUsername() != null ? subject.getUsername() : "system";
	}

	/**
	 * 构建资源引用：resourceType:resourceId
	 */
	private String buildResourceRef(String resourceType, Long resourceId) {
		return resourceType + ":" + resourceId;
	}

	/**
	 * 构建工程级资源引用：mappingProject:projectId/version:versionId/job:jobId
	 * <p>
	 * 遵循 18-08 §9 格式，仅包含 ID 组合，不写源记录明文。
	 */
	private String buildProjectRef(Long projectId, Long versionId, Long jobId) {
		StringBuilder sb = new StringBuilder();
		sb.append("mappingProject:").append(projectId);
		if (versionId != null) {
			sb.append("/version:").append(versionId);
		}
		if (jobId != null) {
			sb.append("/job:").append(jobId);
		}
		return sb.toString();
	}

}
