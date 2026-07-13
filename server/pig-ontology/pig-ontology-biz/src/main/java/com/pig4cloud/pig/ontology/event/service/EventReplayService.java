/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.service;

import com.pig4cloud.pig.ontology.event.config.OntologyEventConfiguration.EventJsonMapper;
import com.pig4cloud.pig.ontology.event.config.OntologyEventProperties;
import com.pig4cloud.pig.ontology.event.entity.OntEventDeadLetter;
import com.pig4cloud.pig.ontology.event.entity.OntEventOutbox;
import com.pig4cloud.pig.ontology.event.entity.OntEventReplayLog;
import com.pig4cloud.pig.ontology.event.mapper.OntEventDeadLetterMapper;
import com.pig4cloud.pig.ontology.event.mapper.OntEventOutboxMapper;
import com.pig4cloud.pig.ontology.event.mapper.OntEventReplayLogMapper;
import com.pig4cloud.pig.ontology.event.model.OntologyEventEnvelope;
import com.pig4cloud.pig.ontology.event.dto.ReplayRequest;
import com.pig4cloud.pig.common.core.util.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 事件受控回放服务。
 * <p>
 * 回放流程： 1. 从 Outbox 读取原始已发布事件 2. 计算面向目标 Group 的下一 replayNo 3. 写
 * ont_event_replay_log 4. 将新 replayNo 的 Envelope 重新 XADD（metadata 标记 targetConsumerGroup）
 * 5. 仅目标 Group 的 Dispatcher 处理该 replay；其他 Group 检测后 ACK 跳过
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventReplayService {

	private final OntEventOutboxMapper outboxMapper;

	private final OntEventReplayLogMapper replayLogMapper;

	private final OntEventDeadLetterMapper deadLetterMapper;

	private final StringRedisTemplate stringRedisTemplate;

	private final OntologyEventProperties properties;

	private final EventJsonMapper eventJsonMapper;

	/**
	 * 从 Outbox 回放指定事件到目标消费者组。
	 * @param request 回放请求
	 * @return 操作结果
	 */
	@Transactional(timeout = 30)
	public R<Boolean> replayFromOutbox(ReplayRequest request) {
		// 1. 从 Outbox 读取原始事件
		com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OntEventOutbox> outboxWrapper =
				new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
		outboxWrapper.eq(OntEventOutbox::getEventId, request.getEventId())
			.eq(OntEventOutbox::getDelFlag, "0");
		OntEventOutbox outbox = outboxMapper.selectOne(outboxWrapper);

		if (outbox == null) {
			return R.failed("事件不存在: " + request.getEventId());
		}

		// 2. 计算目标 Group 的下一 replayNo
		Integer maxReplayNo = replayLogMapper.selectMaxReplayNo(
				request.getEventId(), request.getTargetConsumerGroup());
		int targetReplayNo = (maxReplayNo != null ? maxReplayNo : 0) + 1;

		// 3. 写回放日志
		OntEventReplayLog replayLog = new OntEventReplayLog();
		replayLog.setEventId(request.getEventId());
		replayLog.setSourceReplayNo(request.getSourceReplayNo());
		replayLog.setTargetReplayNo(targetReplayNo);
		replayLog.setTargetConsumerGroup(request.getTargetConsumerGroup());
		replayLog.setReason(request.getReason());
		replayLog.setApprovalRequestNo(request.getApprovalRequestNo());
		replayLog.setReplayedBy(getCurrentUserId());
		replayLog.setReplayedAt(LocalDateTime.now());
		replayLog.setDelFlag("0");
		replayLogMapper.insert(replayLog);

		// 4. 构建新 Envelope（replayNo 递增，metadata 标记目标 Group）
		Map<String, Object> metadata = eventJsonMapper.fromJson(outbox.getMetadata(), Map.class);
		if (metadata == null) {
			metadata = new HashMap<>();
		}
		else {
			metadata = new HashMap<>(metadata);
		}
		metadata.put("targetConsumerGroup", request.getTargetConsumerGroup());
		metadata.put("replayReason", request.getReason());

		OntologyEventEnvelope envelope = OntologyEventEnvelope.builder()
			.eventId(outbox.getEventId())
			.replayNo(targetReplayNo)
			.eventType(outbox.getEventType())
			.eventVersion(outbox.getEventVersion())
			.occurredAt(outbox.getOccurredAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
			.source("pig-ontology-biz")
			.ontologyId(outbox.getOntologyId())
			.aggregateType(outbox.getAggregateType())
			.aggregateId(outbox.getAggregateId())
			.operation(outbox.getOperation())
			.actorId(outbox.getActorId())
			.traceId(outbox.getTraceId())
			.payload(eventJsonMapper.fromJson(outbox.getPayload(), Map.class))
			.metadata(metadata)
			.build();

		String envelopeJson = eventJsonMapper.toJson(envelope);

		// 5. XADD 到 Redis Stream
		Map<String, String> fields = new HashMap<>();
		fields.put("eventId", outbox.getEventId());
		fields.put("replayNo", String.valueOf(targetReplayNo));
		fields.put("eventType", outbox.getEventType());
		fields.put("eventVersion", String.valueOf(outbox.getEventVersion()));
		fields.put("envelopeJson", envelopeJson);

		RecordId recordId = stringRedisTemplate.opsForStream().add(
				StreamRecords.mapBacked(fields).withStreamKey(properties.getStreamKey()));

		log.info("事件已回放: eventId={}, targetGroup={}, replayNo={}, streamRecordId={}",
				outbox.getEventId(), request.getTargetConsumerGroup(), targetReplayNo, recordId.getValue());

		return R.ok(true);
	}

	/**
	 * 从死信回放：将死信标记为 REPLAYED，然后从 Outbox 回放。
	 * @param deadLetterId 死信 ID
	 * @param reason 回放原因
	 * @return 操作结果
	 */
	@Transactional(timeout = 30)
	public R<Integer> replayDeadLetter(Long deadLetterId, String reason) {
		OntEventDeadLetter dlq = deadLetterMapper.selectById(deadLetterId);
		if (dlq == null) {
			return R.failed("死信不存在: " + deadLetterId);
		}
		if (!"OPEN".equals(dlq.getStatus())) {
			return R.failed("死信状态不允许回放: " + dlq.getStatus());
		}

		// 构建回放请求
		ReplayRequest request = new ReplayRequest();
		request.setEventId(dlq.getEventId());
		request.setTargetConsumerGroup(dlq.getConsumerGroup());
		request.setReason(reason);
		request.setSourceReplayNo(dlq.getReplayNo());

		// 执行回放
		R<Boolean> result = replayFromOutbox(request);
		if (result.isOk()) {
			// 标记死信为 REPLAYED
			// 需要知道新的 replayNo
			Integer maxReplayNo = replayLogMapper.selectMaxReplayNo(
					dlq.getEventId(), dlq.getConsumerGroup());
			deadLetterMapper.markReplayed(deadLetterId, maxReplayNo);
		}

		return R.ok(maxReplayNo(dlq));
	}

	/**
	 * 人工标记死信为已处理/忽略。
	 * @param deadLetterId 死信 ID
	 * @param action RESOLVED 或 IGNORED
	 */
	public R<Boolean> resolveDeadLetter(Long deadLetterId, String action) {
		OntEventDeadLetter dlq = deadLetterMapper.selectById(deadLetterId);
		if (dlq == null) {
			return R.failed("死信不存在: " + deadLetterId);
		}

		if ("RESOLVED".equals(action)) {
			deadLetterMapper.markResolved(deadLetterId);
		}
		else if ("IGNORED".equals(action)) {
			deadLetterMapper.markIgnored(deadLetterId);
		}
		else {
			return R.failed("不支持的处理方式: " + action);
		}

		log.info("死信已处理: id={}, action={}", deadLetterId, action);
		return R.ok(true);
	}

	/**
	 * 重试 FAILED 的 Outbox 事件（重置为 PENDING）。
	 * @param outboxId Outbox 行 ID
	 */
	public R<Boolean> retryOutbox(Long outboxId) {
		int updated = outboxMapper.resetForRetry(outboxId);
		if (updated == 0) {
			return R.failed("事件不存在或状态不允许重试");
		}
		log.info("Outbox 事件已重置为 PENDING 以便重试: id={}", outboxId);
		return R.ok(true);
	}

	private Integer maxReplayNo(OntEventDeadLetter dlq) {
		return replayLogMapper.selectMaxReplayNo(dlq.getEventId(), dlq.getConsumerGroup());
	}

	private Long getCurrentUserId() {
		// 首期使用 SecurityContext，后续可从 pig-common-security 获取
		return 1L;
	}

}
