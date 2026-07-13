/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.service;

import cn.hutool.core.util.IdUtil;
import com.pig4cloud.pig.ontology.event.config.OntologyEventConfiguration.EventJsonMapper;
import com.pig4cloud.pig.ontology.event.config.OntologyEventProperties;
import com.pig4cloud.pig.ontology.event.entity.OntEventOutbox;
import com.pig4cloud.pig.ontology.event.mapper.OntEventOutboxMapper;
import com.pig4cloud.pig.ontology.event.model.OntologyDomainEvent;
import com.pig4cloud.pig.ontology.event.model.OntologyEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * 领域事件发布器。
 * <p>
 * 在业务 Service 的现有事务中调用 {@link #append(OntologyDomainEvent)}，保证 Outbox 行与业务写入在同一事务提交或回滚。
 * <p>
 * 使用 {@link Propagation#MANDATORY} 强制要求调用方已有事务，避免业务成功而 Outbox 独立提交。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OntDomainEventPublisher {

	private final OntEventOutboxMapper outboxMapper;

	private final OntologyEventProperties properties;

	private final EventJsonMapper eventJsonMapper;

	/**
	 * 在调用方事务内追加领域事件到 Outbox。
	 * @param event 领域事件
	 * @throws IllegalStateException 事件大小超限或序列化失败
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void append(OntologyDomainEvent event) {
		// 1. 构建完整 Envelope（补充 eventId / replayNo / source）
		OntologyEventEnvelope envelope = OntologyEventEnvelope.builder()
			.eventId(IdUtil.simpleUUID())
			.replayNo(0)
			.eventType(event.getEventType())
			.eventVersion(event.getEventVersion())
			.occurredAt(event.getOccurredAt() != null ? event.getOccurredAt() : Instant.now())
			.source("pig-ontology-biz")
			.ontologyId(event.getOntologyId())
			.aggregateType(event.getAggregateType())
			.aggregateId(event.getAggregateId())
			.aggregateRevision(event.getAggregateRevision())
			.operation(event.getOperation())
			.actorId(event.getActorId())
			.traceId(event.getTraceId())
			.payload(event.getPayload())
			.metadata(event.getMetadata() != null ? event.getMetadata() : Map.of())
			.build();

		// 2. 序列化为 JSON
		String envelopeJson = eventJsonMapper.toJson(envelope);

		// 3. 校验事件大小
		if (envelopeJson.length() > properties.getMaxEventBytes()) {
			throw new IllegalStateException(
					"事件大小 " + envelopeJson.length() + " 超过限制 " + properties.getMaxEventBytes()
							+ "，eventId=" + envelope.getEventId());
		}

		// 4. 写入 Outbox（与业务写入在同一事务）
		OntEventOutbox outbox = new OntEventOutbox();
		outbox.setEventId(envelope.getEventId());
		outbox.setEventType(envelope.getEventType());
		outbox.setEventVersion(envelope.getEventVersion());
		outbox.setOntologyId(envelope.getOntologyId());
		outbox.setAggregateType(envelope.getAggregateType());
		outbox.setAggregateId(envelope.getAggregateId());
		outbox.setOperation(envelope.getOperation());
		outbox.setOccurredAt(LocalDateTime.ofInstant(envelope.getOccurredAt(), ZoneId.systemDefault()));
		outbox.setPayload(eventJsonMapper.toJson(envelope.getPayload()));
		outbox.setMetadata(eventJsonMapper.toJson(envelope.getMetadata()));
		outbox.setTraceId(envelope.getTraceId());
		outbox.setActorId(envelope.getActorId());
		outbox.setStatus("PENDING");
		outbox.setAvailableAt(LocalDateTime.now());
		outbox.setDeliveryAttempt(0);
		outbox.setDelFlag("0");

		outboxMapper.insert(outbox);

		log.debug("事件已追加到 Outbox: eventId={}, type={}, aggregate={}/{}",
				envelope.getEventId(), envelope.getEventType(),
				envelope.getAggregateType(), envelope.getAggregateId());
	}

}
