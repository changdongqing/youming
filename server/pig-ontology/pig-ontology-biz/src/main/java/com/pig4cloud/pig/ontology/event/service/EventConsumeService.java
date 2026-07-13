/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.service;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.pig4cloud.pig.ontology.event.config.OntologyEventConfiguration.EventJsonMapper;
import com.pig4cloud.pig.ontology.event.config.OntologyEventProperties;
import com.pig4cloud.pig.ontology.event.entity.OntEventConsumeRecord;
import com.pig4cloud.pig.ontology.event.entity.OntEventDeadLetter;
import com.pig4cloud.pig.ontology.event.handler.OntologyEventHandler;
import com.pig4cloud.pig.ontology.event.mapper.OntEventConsumeRecordMapper;
import com.pig4cloud.pig.ontology.event.mapper.OntEventDeadLetterMapper;
import com.pig4cloud.pig.ontology.event.model.FailureCategory;
import com.pig4cloud.pig.ontology.event.model.OntologyEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 事件消费服务：Inbox 原子领取 → Handler 分发 → ACK / 死信。
 * <p>
 * 流程： 收到 Stream 消息 → 校验 envelope 和支持版本 → 原子领取 Inbox(group,eventId,replayNo)
 *   ├ 已 SUCCEEDED：直接 XACK
 *   ├ PROCESSING 且租约有效：不重复并发处理
 *   └ 无记录/租约过期：领取并处理 → Handler 成功：Inbox SUCCEEDED + XACK → Handler 失败：Inbox FAILED，不 ACK 等待 reclaim
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventConsumeService {

	private final OntEventConsumeRecordMapper consumeRecordMapper;

	private final OntEventDeadLetterMapper deadLetterMapper;

	private final StringRedisTemplate stringRedisTemplate;

	private final OntologyEventProperties properties;

	private final EventJsonMapper eventJsonMapper;

	private final List<OntologyEventHandler> handlers;

	/**
	 * 处理一条 Stream 消息。
	 * @param message Redis Stream 消息记录
	 * @param handler 匹配到的处理器
	 */
	@Transactional(timeout = 60)
	public void consume(MapRecord<String, String, String> message, OntologyEventHandler handler) {
		Map<String, String> fields = message.getValue();
		String streamKey = properties.getStreamKey();
		String recordId = message.getId().getValue();
		String consumerGroup = handler.consumerGroup();

		// 1. 解析 envelope
		String envelopeJson = fields.get("envelopeJson");
		if (envelopeJson == null) {
			log.error("Stream 消息缺少 envelopeJson 字段，直接 ACK: recordId={}", recordId);
			stringRedisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, recordId);
			return;
		}

		OntologyEventEnvelope envelope;
		try {
			envelope = eventJsonMapper.fromJson(envelopeJson, OntologyEventEnvelope.class);
		}
		catch (Exception e) {
			log.error("Envelope 反序列化失败，直接 ACK 并记录: recordId={}", recordId, e);
			stringRedisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, recordId);
			return;
		}

		Integer replayNo = envelope.getReplayNo() != null ? envelope.getReplayNo() : 0;
		String eventId = envelope.getEventId();
		String eventType = envelope.getEventType();

		// 2. 检查目标 Consumer Group（回放时其他 Group 应跳过）
		Object targetGroup = envelope.getMetadata() != null ? envelope.getMetadata().get("targetConsumerGroup") : null;
		if (targetGroup != null && !targetGroup.toString().equals(consumerGroup)) {
			log.debug("事件目标 Group 不匹配，跳过: eventId={}, target={}, current={}",
					eventId, targetGroup, consumerGroup);
			stringRedisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, recordId);
			return;
		}

		// 3. 校验支持的事件类型和版本
		if (!handler.supportedEventTypes().contains(eventType)) {
			log.debug("Handler 不支持此事件类型，ACK 跳过: group={}, eventType={}", consumerGroup, eventType);
			stringRedisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, recordId);
			return;
		}

		if (!handler.supportedVersions(eventType).contains(envelope.getEventVersion())) {
			log.error("Handler 不支持此事件版本，进入死信: group={}, eventType={}, version={}",
					consumerGroup, eventType, envelope.getEventVersion());
			createDeadLetter(consumerGroup, eventId, replayNo, eventType, recordId, envelopeJson,
					FailureCategory.NON_RETRYABLE, "UNSUPPORTED_VERSION",
					"不支持的事件版本: " + envelope.getEventVersion(), 1);
			stringRedisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, recordId);
			return;
		}

		// 4. Inbox 幂等检查
		OntEventConsumeRecord existing = consumeRecordMapper.findByIdempotencyKey(consumerGroup, eventId, replayNo);
		if (existing != null) {
			if ("SUCCEEDED".equals(existing.getStatus())) {
				log.debug("事件已成功处理，直接 ACK: group={}, eventId={}, replayNo={}",
						consumerGroup, eventId, replayNo);
				stringRedisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, recordId);
				return;
			}
			if ("PROCESSING".equals(existing.getStatus())
					&& existing.getLeaseUntil() != null
					&& existing.getLeaseUntil().isAfter(LocalDateTime.now())) {
				log.debug("事件正在处理中（租约有效），不重复消费: group={}, eventId={}", consumerGroup, eventId);
				return;
			}
		}

		// 5. 创建或更新 Inbox 记录为 PROCESSING
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime leaseUntil = now.plus(properties.getConsumer().getProcessingLease());
		String consumerName = getConsumerName(consumerGroup);

		OntEventConsumeRecord record;
		if (existing != null) {
			// 租约过期，接管处理
			record = existing;
			record.setStatus("PROCESSING");
			record.setLeaseUntil(leaseUntil);
			record.setConsumerName(consumerName);
			record.setProcessAttempt(record.getProcessAttempt() + 1);
			record.setStartedAt(now);
			record.setLastErrorCode(null);
			record.setLastErrorMessage(null);
			consumeRecordMapper.updateById(record);
		}
		else {
			record = new OntEventConsumeRecord();
			record.setConsumerGroup(consumerGroup);
			record.setEventId(eventId);
			record.setReplayNo(replayNo);
			record.setEventType(eventType);
			record.setStatus("PROCESSING");
			record.setLeaseUntil(leaseUntil);
			record.setConsumerName(consumerName);
			record.setProcessAttempt(1);
			record.setStartedAt(now);
			record.setDelFlag("0");
			consumeRecordMapper.insert(record);
		}

		// 6. 调用 Handler
		try {
			handler.handle(envelope);
			consumeRecordMapper.markSucceeded(record.getId());
			stringRedisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, recordId);
			log.debug("事件消费成功: group={}, eventId={}, replayNo={}", consumerGroup, eventId, replayNo);
		}
		catch (Exception e) {
			String errorCode = e.getClass().getSimpleName();
			String errorMsg = truncate(ExceptionUtil.getMessage(e), 500);
			consumeRecordMapper.markFailed(record.getId(), errorCode, errorMsg);

			// 判断是否达到最大尝试次数 → 死信
			if (record.getProcessAttempt() >= properties.getConsumer().getMaxAttempts()) {
				String failureCategory = classifyError(e);
				createDeadLetter(consumerGroup, eventId, replayNo, eventType, recordId, envelopeJson,
						failureCategory, errorCode, errorMsg, record.getProcessAttempt());
				stringRedisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, recordId);
				log.warn("事件达到最大尝试次数，进入死信: group={}, eventId={}, attempts={}",
						consumerGroup, eventId, record.getProcessAttempt());
			}
			else {
				log.warn("事件消费失败，保留 PEL 等待 reclaim: group={}, eventId={}, attempt={}, error={}",
						consumerGroup, eventId, record.getProcessAttempt(), errorMsg);
			}
		}
	}

	/**
	 * 创建死信记录（幂等，唯一键冲突时忽略）。
	 */
	private void createDeadLetter(String consumerGroup, String eventId, Integer replayNo, String eventType,
			String streamRecordId, String payloadJson, String failureCategory, String errorCode,
			String errorMessage, int deliveryCount) {
		try {
			OntEventDeadLetter dlq = new OntEventDeadLetter();
			dlq.setConsumerGroup(consumerGroup);
			dlq.setEventId(eventId);
			dlq.setReplayNo(replayNo);
			dlq.setEventType(eventType);
			dlq.setStreamRecordId(streamRecordId);
			dlq.setPayload(payloadJson);
			dlq.setFailureCategory(failureCategory);
			dlq.setErrorCode(errorCode);
			dlq.setErrorMessage(errorMessage);
			dlq.setDeliveryCount(deliveryCount);
			dlq.setStatus("OPEN");
			dlq.setFirstFailedAt(LocalDateTime.now());
			dlq.setLastFailedAt(LocalDateTime.now());
			dlq.setDelFlag("0");
			deadLetterMapper.insert(dlq);
		}
		catch (org.springframework.dao.DuplicateKeyException e) {
			log.debug("死信记录已存在，跳过: group={}, eventId={}, replayNo={}", consumerGroup, eventId, replayNo);
		}
	}

	/**
	 * 错误分类：判断是否可重试。
	 */
	private String classifyError(Exception e) {
		// 不可重试：NPE/ClassCast/IllegalArgument 等通常表示数据格式问题
		if (e instanceof IllegalArgumentException || e instanceof ClassCastException) {
			return FailureCategory.NON_RETRYABLE;
		}
		return FailureCategory.RETRYABLE;
	}

	private String getConsumerName(String consumerGroup) {
		String hostname = System.getenv("HOSTNAME");
		if (hostname == null || hostname.isBlank()) {
			hostname = "localhost";
		}
		return consumerGroup + "-" + hostname + "-" + ProcessHandle.current().pid();
	}

	private static String truncate(String str, int maxLen) {
		if (str == null) {
			return null;
		}
		return str.length() <= maxLen ? str : str.substring(0, maxLen);
	}

}
