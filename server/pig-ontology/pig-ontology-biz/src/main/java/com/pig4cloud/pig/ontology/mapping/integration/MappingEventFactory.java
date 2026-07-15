/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.integration;

import com.pig4cloud.pig.ontology.event.model.OntologyDomainEvent;
import com.pig4cloud.pig.ontology.event.model.OntologyEventTypes;
import com.pig4cloud.pig.ontology.mapping.ingestion.IngestionContext;
import com.pig4cloud.pig.ontology.mapping.ingestion.SourceIdentity;
import com.pig4cloud.pig.ontology.mapping.job.entity.OntMappingJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 映射事件工厂（18-08 §11 EDA 事件契约）。
 * <p>
 * 统一构造 EDA 事件 payload，确保载荷最小化。
 * <p>
 * <b>Payload 允许字段</b>（§11.2）：
 * <ul>
 *   <li>ontologyId、mappingProjectId、mappingVersionId、jobId</li>
 *   <li>instanceId/entityTypeId/propertyIds</li>
 *   <li>sourceId 但不含连接信息</li>
 *   <li>计数、耗时、状态、错误码</li>
 *   <li>sourceRecordKeyHash，不含完整 key</li>
 *   <li>traceId</li>
 * </ul>
 * <p>
 * <b>禁止字段</b>（§11.2）：
 * <ul>
 *   <li>原始行 JSON、字面量明文</li>
 *   <li>手机、邮箱、OpenID</li>
 *   <li>JDBC URL、用户名、密码</li>
 *   <li>SQL 和 PreparedStatement 参数</li>
 *   <li>授权快照全文</li>
 * </ul>
 *
 * @author youming
 */
@Slf4j
@Component
public class MappingEventFactory {

	/**
	 * 构建映射作业生命周期事件。
	 */
	public OntologyDomainEvent jobLifecycleEvent(OntMappingJob job, String eventType, String operation) {
		return OntologyDomainEvent.builder()
			.eventType(eventType)
			.aggregateType("MAPPING_JOB")
			.aggregateId(job.getId().toString())
			.operation(operation)
			.traceId(job.getTraceId())
			.actorId(job.getRequestedUserId())
			.payload(buildJobPayload(job))
			.build();
	}

	/**
	 * 构建映射版本发布事件。
	 */
	public OntologyDomainEvent versionPublishedEvent(Long ontologyId, Long projectId, Long versionId,
			String configHash, Long actorId, String traceId) {
		Map<String, Object> payload = new LinkedHashMap<>(4);
		payload.put("mappingProjectId", projectId);
		payload.put("mappingVersionId", versionId);
		payload.put("configHash", configHash != null ? configHash : "");

		return OntologyDomainEvent.builder()
			.eventType(OntologyEventTypes.MAPPING_VERSION_PUBLISHED)
			.ontologyId(ontologyId)
			.aggregateType("MAPPING_VERSION")
			.aggregateId(versionId.toString())
			.operation("PUBLISHED")
			.actorId(actorId)
			.traceId(traceId)
			.payload(payload)
			.build();
	}

	/**
	 * 构建实例摄入事件（18-08 §11.1 INSTANCE_INGESTED / INSTANCE_UPDATED_FROM_SOURCE）。
	 * <p>
	 * SKIPPED_UNCHANGED 不发布。
	 */
	public OntologyDomainEvent instanceIngestedEvent(Long ontologyId, Long instanceId, Long entityTypeId,
			IngestionContext ctx, SourceIdentity sourceIdentity, Set<String> changedFields,
			String operation) {
		// SKIPPED 默认不发布
		if ("SKIPPED".equals(operation)) {
			return null;
		}

		String eventType = switch (operation) {
			case "CREATED" -> OntologyEventTypes.INSTANCE_INGESTED;
			case "UPDATED" -> OntologyEventTypes.INSTANCE_UPDATED_FROM_SOURCE;
			case "DEACTIVATED" -> OntologyEventTypes.INSTANCE_DEACTIVATED_FROM_SOURCE;
			default -> OntologyEventTypes.INSTANCE_INGESTED;
		};

		Map<String, Object> payload = new LinkedHashMap<>(8);
		payload.put("entityTypeId", entityTypeId);
		payload.put("mappingProjectId", sourceIdentity != null && sourceIdentity.mappingProjectId() != null
				? sourceIdentity.mappingProjectId() : 0);
		payload.put("mappingVersionId", sourceIdentity != null && sourceIdentity.mappingVersionId() != null
				? sourceIdentity.mappingVersionId() : 0);
		payload.put("jobId", ctx != null && ctx.getJobId() != null ? ctx.getJobId() : 0);
		payload.put("sourceId", sourceIdentity != null && sourceIdentity.sourceId() != null
				? sourceIdentity.sourceId() : 0);
		payload.put("sourceRecordKeyHash", sourceIdentity != null ? sourceIdentity.keyHash() : "");
		payload.put("changedFields", changedFields != null ? changedFields : Set.of());

		return OntologyDomainEvent.builder()
			.eventType(eventType)
			.ontologyId(ontologyId)
			.aggregateType("ENTITY_INSTANCE")
			.aggregateId(instanceId.toString())
			.operation(operation)
			.actorId(ctx != null ? ctx.getRequestedUserId() : null)
			.traceId(ctx != null ? ctx.getTraceId() : null)
			.payload(payload)
			.build();
	}

	/**
	 * 构建关系摄入事件（18-08 §11.1 RELATION_INGESTED / RELATION_UPDATED_FROM_SOURCE）。
	 */
	public OntologyDomainEvent relationIngestedEvent(Long ontologyId, Long relationId, Long objectPropertyId,
			IngestionContext ctx, SourceIdentity sourceIdentity, String operation) {
		String eventType = switch (operation) {
			case "CREATED" -> OntologyEventTypes.RELATION_INGESTED;
			case "UPDATED" -> OntologyEventTypes.RELATION_UPDATED_FROM_SOURCE;
			case "REMOVED" -> OntologyEventTypes.RELATION_SOURCE_REMOVED;
			default -> OntologyEventTypes.RELATION_INGESTED;
		};

		Map<String, Object> payload = new LinkedHashMap<>(6);
		payload.put("objectPropertyId", objectPropertyId);
		payload.put("mappingProjectId", sourceIdentity != null && sourceIdentity.mappingProjectId() != null
				? sourceIdentity.mappingProjectId() : 0);
		payload.put("mappingVersionId", sourceIdentity != null && sourceIdentity.mappingVersionId() != null
				? sourceIdentity.mappingVersionId() : 0);
		payload.put("jobId", ctx != null && ctx.getJobId() != null ? ctx.getJobId() : 0);
		payload.put("sourceId", sourceIdentity != null && sourceIdentity.sourceId() != null
				? sourceIdentity.sourceId() : 0);
		payload.put("sourceRecordKeyHash", sourceIdentity != null ? sourceIdentity.keyHash() : "");

		return OntologyDomainEvent.builder()
			.eventType(eventType)
			.ontologyId(ontologyId)
			.aggregateType("OBJECT_RELATION")
			.aggregateId(relationId.toString())
			.operation(operation)
			.actorId(ctx != null ? ctx.getRequestedUserId() : null)
			.traceId(ctx != null ? ctx.getTraceId() : null)
			.payload(payload)
			.build();
	}

	/**
	 * 构建安全拒绝事件（18-08 §11.1 MAPPING_SECURITY_DENIED）。
	 */
	public OntologyDomainEvent securityDeniedEvent(Long ontologyId, String resourceType,
			String resourceRef, String reasonCode, String traceId, Long actorId) {
		Map<String, Object> payload = new LinkedHashMap<>(4);
		payload.put("resourceType", resourceType);
		payload.put("resourceRef", resourceRef);
		payload.put("reasonCode", reasonCode);

		return OntologyDomainEvent.builder()
			.eventType(OntologyEventTypes.MAPPING_SECURITY_DENIED)
			.ontologyId(ontologyId)
			.aggregateType("SECURITY_DENIAL")
			.aggregateId(resourceRef)
			.operation("DENIED")
			.actorId(actorId)
			.traceId(traceId)
			.payload(payload)
			.build();
	}

	/**
	 * 构建记录拒绝事件（18-08 §11.1 MAPPING_RECORD_REJECTED）。
	 * <p>
	 * 记录失败默认只聚合作业错误码计数，不逐条发事件。
	 * 此方法仅在需要逐条发布时使用。
	 */
	public OntologyDomainEvent recordRejectedEvent(Long ontologyId, IngestionContext ctx,
			SourceIdentity sourceIdentity, String errorCode, String mappingCode) {
		Map<String, Object> payload = new LinkedHashMap<>(6);
		payload.put("mappingProjectId", sourceIdentity != null && sourceIdentity.mappingProjectId() != null
				? sourceIdentity.mappingProjectId() : 0);
		payload.put("mappingVersionId", sourceIdentity != null && sourceIdentity.mappingVersionId() != null
				? sourceIdentity.mappingVersionId() : 0);
		payload.put("jobId", ctx != null && ctx.getJobId() != null ? ctx.getJobId() : 0);
		payload.put("sourceId", sourceIdentity != null && sourceIdentity.sourceId() != null
				? sourceIdentity.sourceId() : 0);
		payload.put("sourceRecordKeyHash", sourceIdentity != null ? sourceIdentity.keyHash() : "");
		payload.put("errorCode", errorCode);
		payload.put("mappingCode", mappingCode != null ? mappingCode : "");

		return OntologyDomainEvent.builder()
			.eventType(OntologyEventTypes.MAPPING_RECORD_REJECTED)
			.ontologyId(ontologyId)
			.aggregateType("MAPPING_RECORD")
			.aggregateId(sourceIdentity != null ? sourceIdentity.keyHash() : "unknown")
			.operation("REJECTED")
			.actorId(ctx != null ? ctx.getRequestedUserId() : null)
			.traceId(ctx != null ? ctx.getTraceId() : null)
			.payload(payload)
			.build();
	}

	/**
	 * 构建值覆盖事件（18-08 §11.1 MAPPING_VALUE_OVERRIDDEN）。
	 */
	public OntologyDomainEvent valueOverriddenEvent(Long ontologyId, Long dataValueId,
			String fieldMappingCode, Long actorId, String traceId) {
		Map<String, Object> payload = new LinkedHashMap<>(2);
		payload.put("dataValueId", dataValueId);
		payload.put("fieldMappingCode", fieldMappingCode != null ? fieldMappingCode : "");

		return OntologyDomainEvent.builder()
			.eventType(OntologyEventTypes.MAPPING_VALUE_OVERRIDDEN)
			.ontologyId(ontologyId)
			.aggregateType("DATA_VALUE")
			.aggregateId(dataValueId.toString())
			.operation("OVERRIDDEN")
			.actorId(actorId)
			.traceId(traceId)
			.payload(payload)
			.build();
	}

	// ==================== 内部方法 ====================

	/**
	 * 构建作业 payload（仅含允许的非敏感字段）。
	 */
	private Map<String, Object> buildJobPayload(OntMappingJob job) {
		Map<String, Object> payload = new LinkedHashMap<>(8);
		payload.put("jobId", job.getId());
		payload.put("projectId", job.getMappingProjectId());
		payload.put("versionId", job.getMappingVersionId());
		payload.put("runType", job.getRunType());
		payload.put("status", job.getJobStatus());
		// 计数摘要
		payload.put("totalRead", job.getTotalRead() != null ? job.getTotalRead() : 0);
		payload.put("totalCreated", job.getTotalCreated() != null ? job.getTotalCreated() : 0);
		payload.put("totalUpdated", job.getTotalUpdated() != null ? job.getTotalUpdated() : 0);
		payload.put("totalFailed", job.getTotalFailed() != null ? job.getTotalFailed() : 0);
		if (job.getErrorCode() != null) {
			payload.put("errorCode", job.getErrorCode());
		}
		return payload;
	}

}
