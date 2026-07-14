/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.audit;

import com.pig4cloud.pig.ontology.event.model.OntologyDomainEvent;
import com.pig4cloud.pig.ontology.event.model.OntologyEventTypes;
import com.pig4cloud.pig.ontology.event.service.OntDomainEventPublisher;
import com.pig4cloud.pig.ontology.security.entity.OntDataAccessLog;
import com.pig4cloud.pig.ontology.security.mapper.OntDataAccessLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 数据访问审计服务。
 * <p>
 * 采用同步审计写入 + 异步事件通知双写模式：
 * <ol>
 *   <li>同步落库：在业务事务内同步写入 ont_data_access_log，确保 Redis 不可用时不丢失合规记录</li>
 *   <li>异步事件：通过 OntDomainEventPublisher 发布 ONTOLOGY_SECURITY_EVENT，供已存在的 SecurityAuditEventHandler 消费</li>
 * </ol>
 * 秘密不进日志：Token、证书私钥、明文字段、完整受限查询结果不得进入审计日志。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataAccessAuditService {

	private final OntDataAccessLogMapper auditLogMapper;

	private final AuditChainService auditChainService;

	private final OntDomainEventPublisher eventPublisher;

	/**
	 * 同步记录数据访问审计。
	 * <p>
	 * 在调用方事务内执行（Propagation.REQUIRED），确保审计与业务操作同事务提交/回滚。
	 *
	 * @param ontologyId       本体工程ID
	 * @param userId           用户ID
	 * @param username         用户名
	 * @param accessType       访问类型
	 * @param resourceType     资源类型
	 * @param resourceRef      资源引用（非敏感摘要）
	 * @param actionSummary    操作摘要（非敏感）
	 * @param resultCount      结果数量
	 * @param maxSecurityLevel 最高安全级别
	 * @param decision         策略决策
	 * @param outcome          执行结果
	 * @param errorCode        错误码
	 * @param traceId          追踪ID
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void recordAudit(Long ontologyId, Long userId, String username, String accessType,
			String resourceType, String resourceRef, String actionSummary, Long resultCount,
			String maxSecurityLevel, String decision, String outcome, String errorCode, String traceId) {
		try {
			// 1. 获取哈希链上下文
			AuditChainService.ChainContext chainCtx = auditChainService.acquireChainContext(ontologyId);
			String chainScope = auditChainService.buildChainScope(ontologyId);

			// 2. 计算当前记录哈希
			String currentHash = auditChainService.computeHash(chainScope, chainCtx.chainSeq(), username,
				accessType, resourceType, resourceRef, decision, outcome, chainCtx.prevHash());

			// 3. 获取远程地址和 user-agent
			String remoteAddr = getRemoteAddr();
			String userAgentHash = getUserAgentHash();

			// 4. 构建审计记录
			OntDataAccessLog auditLog = new OntDataAccessLog();
			auditLog.setAuditEventId(UUID.randomUUID().toString());
			auditLog.setChainScope(chainScope);
			auditLog.setChainSeq(chainCtx.chainSeq());
			auditLog.setOntologyId(ontologyId);
			auditLog.setUserId(userId);
			auditLog.setUsername(username != null ? username : "unknown");
			auditLog.setAccessType(accessType);
			auditLog.setResourceType(resourceType);
			auditLog.setResourceRef(resourceRef);
			auditLog.setActionSummary(actionSummary);
			auditLog.setResultCount(resultCount != null ? resultCount : 0L);
			auditLog.setMaxSecurityLevel(maxSecurityLevel);
			auditLog.setDecision(decision);
			auditLog.setOutcome(outcome);
			auditLog.setErrorCode(errorCode);
			auditLog.setTraceId(traceId);
			auditLog.setRemoteAddr(remoteAddr);
			auditLog.setUserAgentHash(userAgentHash);
			auditLog.setOccurredAt(java.time.LocalDateTime.now());
			auditLog.setPrevHash(chainCtx.prevHash());
			auditLog.setCurrentHash(currentHash);

			// 5. 同步写入审计表
			auditLogMapper.insert(auditLog);

			// 6. 异步发布 ONTOLOGY_SECURITY_EVENT 事件（payload 不含敏感明文）
			publishSecurityEvent(auditLog);
		}
		catch (Exception e) {
			// 审计写入失败不应阻断业务操作，但必须记录告警
			log.error("Failed to write audit log (audit may be incomplete): {}", e.getMessage(), e);
		}
	}

	/**
	 * 异步发布安全审计事件。
	 * <p>
	 * payload 只含资源 ID/IRI、决策结果、非敏感摘要。
	 */
	@Async
	public void publishSecurityEvent(OntDataAccessLog auditLog) {
		try {
			Map<String, Object> payload = new HashMap<>(8);
			payload.put("auditEventId", auditLog.getAuditEventId());
			payload.put("ontologyId", auditLog.getOntologyId());
			payload.put("decision", auditLog.getDecision());
			payload.put("outcome", auditLog.getOutcome());
			payload.put("maxSecurityLevel", auditLog.getMaxSecurityLevel());
			payload.put("resourceRef", auditLog.getResourceRef());

			Map<String, Object> metadata = new HashMap<>(4);
			metadata.put("accessType", auditLog.getAccessType());
			metadata.put("resourceType", auditLog.getResourceType());
			metadata.put("resultCount", auditLog.getResultCount());

			eventPublisher.append(OntologyDomainEvent.builder()
				.eventType(OntologyEventTypes.ONTOLOGY_SECURITY_EVENT)
				.eventVersion(1)
				.ontologyId(auditLog.getOntologyId())
				.aggregateType("SECURITY_AUDIT")
				.aggregateId(auditLog.getAuditEventId())
				.operation("AUDITED")
				.occurredAt(Instant.now())
				.actorId(auditLog.getUserId())
				.traceId(auditLog.getTraceId())
				.payload(payload)
				.metadata(metadata)
				.build());
		}
		catch (Exception e) {
			// 事件发布失败不影响同步审计结果
			log.warn("Failed to publish security audit event (sync audit already persisted): {}",
				e.getMessage());
		}
	}

	/**
	 * 从 HTTP 请求获取远程地址。
	 */
	private String getRemoteAddr() {
		try {
			RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
			if (attrs instanceof ServletRequestAttributes servletAttrs) {
				return servletAttrs.getRequest().getRemoteAddr();
			}
		}
		catch (Exception ignored) {
		}
		return null;
	}

	/**
	 * 获取 User-Agent 的哈希（不存原始 UA，防隐私泄露）。
	 */
	private String getUserAgentHash() {
		try {
			RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
			if (attrs instanceof ServletRequestAttributes servletAttrs) {
				String ua = servletAttrs.getRequest().getHeader("User-Agent");
				if (ua != null) {
					java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
					byte[] hash = md.digest(ua.getBytes(java.nio.charset.StandardCharsets.UTF_8));
					StringBuilder sb = new StringBuilder(hash.length * 2);
					for (byte b : hash) {
						sb.append(String.format("%02x", b));
					}
					return sb.substring(0, 64);
				}
			}
		}
		catch (Exception ignored) {
		}
		return null;
	}

}
