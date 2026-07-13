/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.service.impl;

import com.pig4cloud.pig.ontology.event.config.OntologyEventConfiguration.EventJsonMapper;
import com.pig4cloud.pig.ontology.event.config.OntologyEventProperties;
import com.pig4cloud.pig.ontology.event.entity.OntEventOutbox;
import com.pig4cloud.pig.ontology.event.mapper.OntEventOutboxMapper;
import com.pig4cloud.pig.ontology.event.model.OntologyEventEnvelope;
import com.pig4cloud.pig.ontology.event.service.EventStreamAdminService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Outbox 投递中继：轮询领取 → XADD → 更新状态。
 * <p>
 * 领取使用 FOR UPDATE SKIP LOCKED + PROCESSING 租约，多实例不重复领取。 XADD 成功后标记 PUBLISHED；
 * 超过最大尝试次数后标记 FAILED。XADD 成功、DB 更新前宕机会导致重复 XADD，消费者 Inbox 必须去重。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayService {

	private final OntEventOutboxMapper outboxMapper;

	private final StringRedisTemplate stringRedisTemplate;

	private final OntologyEventProperties properties;

	private final EventJsonMapper eventJsonMapper;

	private final EventStreamAdminService streamAdminService;

	/**
	 * 应用启动后确保 Stream 存在。
	 */
	@PostConstruct
	public void init() {
		if (!properties.isEnabled()) {
			log.info("事件驱动骨干已禁用，跳过 Stream 初始化");
			return;
		}
		try {
			streamAdminService.ensureStreamExists();
			log.info("Outbox Relay 初始化完成，stream={}", properties.getStreamKey());
		}
		catch (Exception e) {
			log.error("Outbox Relay 初始化失败，Relay 将在 Redis 恢复后自动重试", e);
		}
	}

	/**
	 * 定时轮询投递。固定延迟，避免与上一轮重叠。
	 */
	@Scheduled(fixedDelayString = "${ontology.event.outbox.poll-delay:2s}",
			initialDelayString = "${ontology.event.outbox.poll-delay:2s}")
	public void relay() {
		if (!properties.isEnabled()) {
			return;
		}
		try {
			relayBatch();
		}
		catch (Exception e) {
			log.error("Outbox 投递轮询异常", e);
		}
	}

	/**
	 * 执行一轮领取和投递。
	 */
	@Transactional(timeout = 30)
	public void relayBatch() {
		int batchSize = properties.getOutbox().getBatchSize();
		List<Long> ids = outboxMapper.selectPollingBatch(batchSize);
		if (ids.isEmpty()) {
			return;
		}

		String instanceId = getInstanceId();
		Duration leaseDuration = properties.getOutbox().getLeaseDuration();
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime leaseUntil = now.plus(leaseDuration);

		int published = 0;
		int failed = 0;

		for (Long id : ids) {
			try {
				// 领取并设置租约
				int locked = outboxMapper.lockForProcessing(id, instanceId, leaseUntil);
				if (locked == 0) {
					continue;
				}

				OntEventOutbox outbox = outboxMapper.selectById(id);
				if (outbox == null) {
					continue;
				}

				// 检查是否超过最大尝试次数
				if (outbox.getDeliveryAttempt() > properties.getOutbox().getMaxAttempts()) {
					outboxMapper.markFailed(id, "MAX_ATTEMPTS_EXCEEDED",
							"投递尝试次数超过上限 " + properties.getOutbox().getMaxAttempts());
					failed++;
					log.warn("Outbox 事件投递达到上限，标记 FAILED: id={}, eventId={}, attempts={}",
							id, outbox.getEventId(), outbox.getDeliveryAttempt());
					continue;
				}

				// 重建完整 Envelope JSON 用于 XADD
				String envelopeJson = buildEnvelopeJson(outbox);

				// XADD 到 Redis Stream
				Map<String, String> fields = new HashMap<>();
				fields.put("eventId", outbox.getEventId());
				fields.put("replayNo", "0");
				fields.put("eventType", outbox.getEventType());
				fields.put("eventVersion", String.valueOf(outbox.getEventVersion()));
				fields.put("envelopeJson", envelopeJson);

				RecordId recordId = stringRedisTemplate.opsForStream().add(
						StreamRecords.mapBacked(fields).withStreamKey(properties.getStreamKey()));

				// 标记 PUBLISHED
				outboxMapper.markPublished(id, recordId.getValue(), LocalDateTime.now());
				published++;

				log.debug("事件已投递到 Stream: eventId={}, streamRecordId={}",
						outbox.getEventId(), recordId.getValue());
			}
			catch (Exception e) {
				log.error("Outbox 事件投递失败: id={}", id, e);
				outboxMapper.markFailed(id, "XADD_ERROR", truncate(e.getMessage(), 500));
				failed++;
			}
		}

		if (published > 0 || failed > 0) {
			log.info("Outbox 投递轮询完成: 领取={}, 已发布={}, 失败={}", ids.size(), published, failed);
		}

		// 投递后修剪 Stream
		streamAdminService.trimStream();
	}

	/**
	 * 从 Outbox 行重建完整 Envelope JSON。
	 * <p>
	 * Outbox 的 payload 和 metadata 是分开存储的，这里组合为完整 Envelope。
	 */
	private String buildEnvelopeJson(OntEventOutbox outbox) {
		OntologyEventEnvelope envelope = OntologyEventEnvelope.builder()
			.eventId(outbox.getEventId())
			.replayNo(0)
			.eventType(outbox.getEventType())
			.eventVersion(outbox.getEventVersion())
			.occurredAt(outbox.getOccurredAt()
					.atZone(java.time.ZoneId.systemDefault())
					.toInstant())
			.source("pig-ontology-biz")
			.ontologyId(outbox.getOntologyId())
			.aggregateType(outbox.getAggregateType())
			.aggregateId(outbox.getAggregateId())
			.operation(outbox.getOperation())
			.actorId(outbox.getActorId())
			.traceId(outbox.getTraceId())
			.payload(eventJsonMapper.fromJson(outbox.getPayload(), Map.class))
			.metadata(outbox.getMetadata() != null
					? eventJsonMapper.fromJson(outbox.getMetadata(), Map.class)
					: Map.of())
			.build();
		return eventJsonMapper.toJson(envelope);
	}

	private String getInstanceId() {
		String hostname = System.getenv("HOSTNAME");
		if (hostname == null || hostname.isBlank()) {
			hostname = "localhost";
		}
		return hostname + "-" + ProcessHandle.current().pid();
	}

	private static String truncate(String str, int maxLen) {
		if (str == null) {
			return null;
		}
		return str.length() <= maxLen ? str : str.substring(0, maxLen);
	}

}
