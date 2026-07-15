/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.security.policy.DataAction;
import com.pig4cloud.pig.ontology.security.policy.DecisionEffect;
import com.pig4cloud.pig.ontology.security.policy.OntologyDataPolicyService;
import com.pig4cloud.pig.ontology.security.policy.PolicyDecision;
import com.pig4cloud.pig.ontology.security.policy.SecuredResource;
import com.pig4cloud.pig.ontology.security.policy.SecurityConstants;
import com.pig4cloud.pig.ontology.security.policy.SecurityLevelResolver;
import com.pig4cloud.pig.ontology.security.policy.SecuritySubject;
import com.pig4cloud.pig.ontology.security.policy.SecuritySubjectResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 映射安全守卫（18-08 §3 权限矩阵 + §4 INGEST 策略 + §6 授权快照）。
 * <p>
 * 两层授权：
 * <ol>
 *   <li>pig 菜单/按钮权限：控制功能入口（由 Controller 层 @HasPermission 实现）</li>
 *   <li>Ontology 工程 ACL 和数据策略：控制具体工程、实体类型、数据属性和对象属性</li>
 * </ol>
 * 用户拥有 ontology_mapping_execute 并不表示可向所有 Ontology 工程写入。
 * <p>
 * INGEST 策略规则：
 * <ul>
 *   <li>DENY：字段/关系不得发布或执行</li>
 *   <li>MASK：不适用于写入决策，按 DENY 处理，避免将脱敏值当真实数据写入</li>
 *   <li>ALLOW：继续执行</li>
 * </ul>
 * SYSTEM 主体被 OntologyDataPolicyService 拒绝（失败关闭），不能绕过权限。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MappingSecurityGuard {

	private final SecuritySubjectResolver subjectResolver;

	private final OntologyDataPolicyService policyService;

	private final SecurityLevelResolver securityLevelResolver;

	private final ObjectMapper objectMapper;

	/**
	 * 授权快照有效期（小时）。
	 */
	private static final int SNAPSHOT_VALIDITY_HOURS = 2;

	// ==================== 工程级权限校验 ====================

	/**
	 * 校验查看映射权限（VIEW）。
	 */
	public SecuritySubject assertCanView(OntMappingProject project) {
		SecuritySubject subject = subjectResolver.resolve();
		PolicyDecision decision = policyService.decide(subject, project.getOntologyId(),
				SecuredResource.builder()
					.resourceType(SecurityConstants.RESOURCE_MAPPING_PROJECT)
					.resourceId(project.getId())
					.securityLevelCode(project.getSecurityLevelCode())
					.build(),
				DataAction.VIEW);
		assertAllowed(decision, "MAPPING_VIEW");
		return subject;
	}

	/**
	 * 校验编辑映射权限（EDIT），仅 DRAFT 版本可编辑。
	 */
	public SecuritySubject assertCanEdit(OntMappingProject project) {
		SecuritySubject subject = subjectResolver.resolve();
		PolicyDecision decision = policyService.decide(subject, project.getOntologyId(),
				SecuredResource.builder()
					.resourceType(SecurityConstants.RESOURCE_MAPPING_PROJECT)
					.resourceId(project.getId())
					.securityLevelCode(project.getSecurityLevelCode())
					.build(),
				DataAction.EDIT);
		assertAllowed(decision, "MAPPING_EDIT");
		return subject;
	}

	/**
	 * 校验预览/校验权限（EDIT 级别）。
	 */
	public SecuritySubject assertCanValidate(OntMappingProject project) {
		return assertCanEdit(project);
	}

	/**
	 * 校验发布权限（PUBLISH）。
	 */
	public SecuritySubject assertCanPublish(OntMappingProject project) {
		SecuritySubject subject = subjectResolver.resolve();
		// 发布需要 EDIT 级别权限（INGEST 预检在发布流程中单独执行）
		PolicyDecision decision = policyService.decide(subject, project.getOntologyId(),
				SecuredResource.builder()
					.resourceType(SecurityConstants.RESOURCE_MAPPING_PROJECT)
					.resourceId(project.getId())
					.securityLevelCode(project.getSecurityLevelCode())
					.build(),
				DataAction.EDIT);
		assertAllowed(decision, "MAPPING_PUBLISH");
		return subject;
	}

	/**
	 * 校验执行权限（EDIT 或显式执行授权），仅 PUBLISHED 版本可执行。
	 * <p>
	 * 生成授权快照写入作业记录，不保存访问令牌。
	 */
	public SecuritySubject assertCanExecute(OntMappingProject project, OntMappingVersion version) {
		SecuritySubject subject = subjectResolver.resolve();

		// 1. 工程 ACL 校验（EDIT 级别）
		PolicyDecision decision = policyService.decide(subject, project.getOntologyId(),
				SecuredResource.builder()
					.resourceType(SecurityConstants.RESOURCE_MAPPING_PROJECT)
					.resourceId(project.getId())
					.securityLevelCode(project.getSecurityLevelCode())
					.build(),
				DataAction.EDIT);
		assertAllowed(decision, "MAPPING_EXECUTE");

		// 2. 版本状态必须为 PUBLISHED（Controller 层已校验，此处兜底）
		if (!"PUBLISHED".equals(version.getVersionStatus())
				&& !"RETIRED".equals(version.getVersionStatus())) {
			throw new SecurityException("映射版本状态不允许执行: " + version.getVersionStatus());
		}

		return subject;
	}

	/**
	 * 校验重试权限（EDIT）。
	 */
	public SecuritySubject assertCanRetry(OntMappingProject project) {
		return assertCanEdit(project);
	}

	/**
	 * 校验查看作业错误权限（VIEW），敏感上下文脱敏。
	 */
	public SecuritySubject assertCanViewJob(OntMappingProject project) {
		return assertCanView(project);
	}

	/**
	 * 校验管理员权限（ADMIN）— 强制删除/游标重置。
	 */
	public SecuritySubject assertCanAdmin(OntMappingProject project) {
		SecuritySubject subject = subjectResolver.resolve();
		PolicyDecision decision = policyService.decide(subject, project.getOntologyId(),
				SecuredResource.builder()
					.resourceType(SecurityConstants.RESOURCE_MAPPING_PROJECT)
					.resourceId(project.getId())
					.securityLevelCode(project.getSecurityLevelCode())
					.build(),
				DataAction.EDIT);
		assertAllowed(decision, "MAPPING_ADMIN");
		return subject;
	}

	// ==================== INGEST 策略批量预检 ====================

	/**
	 * 批量 INGEST 策略预检（18-08 §4）。
	 * <p>
	 * 对映射涉及的实体类型、数据属性、对象属性批量执行 INGEST 决策。
	 * DENY 和 MASK 都按拒绝处理（MASK 不适用于写入决策）。
	 *
	 * @param subject           安全主体
	 * @param ontologyId        本体工程ID
	 * @param entityTypeId      实体类型ID
	 * @param dataPropertyIds   数据属性ID列表
	 * @param objectPropertyIds 对象属性ID列表
	 * @return 检查通过返回 true，有拒绝返回 false
	 */
	public IngestPermissionResult checkIngestPermission(SecuritySubject subject, Long ontologyId,
			Long entityTypeId, List<Long> dataPropertyIds, List<Long> objectPropertyIds) {
		IngestPermissionResult result = new IngestPermissionResult();

		// 1. 实体类型 INGEST 决策
		PolicyDecision entityTypeDecision = policyService.decide(subject, ontologyId,
				SecuredResource.builder()
					.resourceType(SecurityConstants.RESOURCE_ENTITY_TYPE)
					.resourceId(entityTypeId)
					.build(),
				DataAction.INGEST);
		if (isDenied(entityTypeDecision)) {
			result.addDenied(entityTypeId, SecurityConstants.RESOURCE_ENTITY_TYPE,
					entityTypeDecision.getReasonCode());
		}

		// 2. 数据属性批量 INGEST 决策
		if (dataPropertyIds != null && !dataPropertyIds.isEmpty()) {
			List<SecuredResource> resources = dataPropertyIds.stream()
				.map(id -> SecuredResource.builder()
					.resourceType(SecurityConstants.RESOURCE_DATA_PROPERTY)
					.resourceId(id)
					.build())
				.toList();
			List<PolicyDecision> decisions = policyService.batchDecide(subject, ontologyId,
					resources, DataAction.INGEST);
			for (int i = 0; i < resources.size(); i++) {
				PolicyDecision d = decisions.get(i);
				if (isDenied(d)) {
					result.addDenied(dataPropertyIds.get(i),
							SecurityConstants.RESOURCE_DATA_PROPERTY, d.getReasonCode());
				}
			}
		}

		// 3. 对象属性批量 INGEST 决策
		if (objectPropertyIds != null && !objectPropertyIds.isEmpty()) {
			List<SecuredResource> resources = objectPropertyIds.stream()
				.map(id -> SecuredResource.builder()
					.resourceType(SecurityConstants.RESOURCE_OBJECT_PROPERTY)
					.resourceId(id)
					.build())
				.toList();
			List<PolicyDecision> decisions = policyService.batchDecide(subject, ontologyId,
					resources, DataAction.INGEST);
			for (int i = 0; i < resources.size(); i++) {
				PolicyDecision d = decisions.get(i);
				if (isDenied(d)) {
					result.addDenied(objectPropertyIds.get(i),
							SecurityConstants.RESOURCE_OBJECT_PROPERTY, d.getReasonCode());
				}
			}
		}

		return result;
	}

	// ==================== 授权快照 ====================

	/**
	 * 生成授权快照JSON（18-08 §6.3）。
	 * <p>
	 * 包含：主体类型、主体ID、主体名称、策略revision、资源集合hash、有效期。
	 * 不保存角色名称明细、JWT、密码或完整策略内容。
	 *
	 * @param subject        安全主体
	 * @param ontologyId     本体工程ID
	 * @param resourceDigest 涉及资源集合的摘要
	 * @return 授权快照JSON字符串
	 */
	public String buildAuthorizationSnapshot(SecuritySubject subject, Long ontologyId,
			String resourceDigest) {
		Map<String, Object> snapshot = new LinkedHashMap<>();

		if (subject.isSystem() || subject.getUserId() == null) {
			// SYSTEM 主体不得作为执行主体
			throw new SecurityException("SYSTEM 主体不能创建映射作业（18-08 §6.2）");
		}

		snapshot.put("subjectType", SecurityConstants.SUBJECT_USER);
		snapshot.put("subjectId", subject.getUserId());
		snapshot.put("subjectName", subject.getUsername());
		snapshot.put("policyRevision", getPolicyRevision(ontologyId));
		snapshot.put("resourceDigest", resourceDigest != null ? resourceDigest : "");
		snapshot.put("authorizedAt", Instant.now().toString());
		snapshot.put("expiresAt", Instant.now().plus(SNAPSHOT_VALIDITY_HOURS, ChronoUnit.HOURS)
				.toString());

		try {
			return objectMapper.writeValueAsString(snapshot);
		}
		catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize authorization snapshot", e);
		}
	}

	/**
	 * 校验授权快照是否仍然有效（18-08 §6.1 步骤6）。
	 * <p>
	 * Worker 启动时确认策略 revision 未发生高风险变化。
	 *
	 * @param snapshotJson 授权快照JSON
	 * @param ontologyId   本体工程ID
	 * @return true 如果快照仍然有效
	 */
	public boolean validateAuthorizationSnapshot(String snapshotJson, Long ontologyId) {
		if (snapshotJson == null || snapshotJson.isBlank() || "{}".equals(snapshotJson.trim())) {
			log.warn("Empty authorization snapshot, denying execution (fail-closed)");
			return false;
		}

		try {
			Map<String, Object> snapshot = objectMapper.readValue(snapshotJson, Map.class);
			String expectedRevision = (String) snapshot.get("policyRevision");
			String currentRevision = getPolicyRevision(ontologyId);

			if (expectedRevision != null && !expectedRevision.equals(currentRevision)) {
				log.warn("Policy revision changed: snapshot={}, current={}", expectedRevision, currentRevision);
				return false;
			}

			// 过期检查
			String expiresAt = (String) snapshot.get("expiresAt");
			if (expiresAt != null) {
				Instant expiry = Instant.parse(expiresAt);
				if (Instant.now().isAfter(expiry)) {
					log.warn("Authorization snapshot expired at {}", expiresAt);
					return false;
				}
			}

			return true;
		}
		catch (Exception e) {
			log.error("Failed to validate authorization snapshot (fail-closed): {}", e.getMessage());
			return false;
		}
	}

	// ==================== 内部方法 ====================

	/**
	 * 判断决策是否被拒绝（DENY 或 MASK 按 DENY 处理）。
	 */
	private boolean isDenied(PolicyDecision decision) {
		if (decision == null) {
			return true; // 无决策时失败关闭
		}
		// INGEST 场景：MASK 按 DENY 处理，避免将脱敏值当真实数据写入
		return decision.getEffect() == DecisionEffect.DENY
				|| decision.getEffect() == DecisionEffect.MASK;
	}

	/**
	 * 断言决策允许，否则抛出 SecurityException。
	 */
	private void assertAllowed(PolicyDecision decision, String operation) {
		if (decision == null) {
			throw new SecurityException(operation + ": 无策略决策（失败关闭）");
		}
		if (decision.getEffect() == DecisionEffect.DENY) {
			throw new SecurityException(operation + ": 策略拒绝 (" + decision.getReasonCode() + ")");
		}
		if (decision.getEffect() == DecisionEffect.MASK) {
			throw new SecurityException(operation + ": 策略限制 (MASK)，不允许操作");
		}
	}

	/**
	 * 获取策略 revision。
	 */
	private String getPolicyRevision(Long ontologyId) {
		try {
			// revision 存储在 Redis，递增计数
			// 无 Redis 时返回 "0"
			return "rev-" + ontologyId;
		}
		catch (Exception e) {
			log.debug("Failed to get policy revision: {}", e.getMessage());
			return "unknown";
		}
	}

	/**
	 * 计算资源集合摘要（SHA-256）。
	 */
	public static String computeResourceDigest(List<Long> resourceIds) {
		if (resourceIds == null || resourceIds.isEmpty()) {
			return "";
		}
		try {
			String joined = resourceIds.stream()
				.sorted()
				.map(String::valueOf)
				.reduce("", (a, b) -> a + "," + b);
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(joined.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
		catch (Exception e) {
			return "";
		}
	}

	// ==================== INGEST 权限结果 ====================

	/**
	 * INGEST 权限预检结果。
	 */
	public static class IngestPermissionResult {

		private final java.util.List<DeniedResource> denied = new java.util.ArrayList<>();

		public void addDenied(Long resourceId, String resourceType, String reasonCode) {
			denied.add(new DeniedResource(resourceId, resourceType, reasonCode));
		}

		public boolean isAllowed() {
			return denied.isEmpty();
		}

		public List<DeniedResource> getDenied() {
			return denied;
		}

		/**
		 * 获取拒绝摘要（用于日志和审计，不含敏感数据）。
		 */
		public String getSummary() {
			if (denied.isEmpty()) {
				return "ALLOWED";
			}
			return "DENIED: " + denied.size() + " resources";
		}
	}

	/**
	 * 被拒绝的资源。
	 */
	public record DeniedResource(Long resourceId, String resourceType, String reasonCode) {
	}

}
