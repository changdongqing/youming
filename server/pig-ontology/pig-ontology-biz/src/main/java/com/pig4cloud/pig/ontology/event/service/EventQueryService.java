/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.ontology.event.config.OntologyEventConfiguration;
import com.pig4cloud.pig.ontology.event.config.OntologyEventProperties;
import com.pig4cloud.pig.ontology.event.entity.OntEventDeadLetter;
import com.pig4cloud.pig.ontology.event.entity.OntEventOutbox;
import com.pig4cloud.pig.ontology.event.handler.OntologyEventDispatcher;
import com.pig4cloud.pig.ontology.event.handler.OntologyEventHandler;
import com.pig4cloud.pig.ontology.event.mapper.OntEventDeadLetterMapper;
import com.pig4cloud.pig.ontology.event.mapper.OntEventOutboxMapper;
import com.pig4cloud.pig.ontology.event.vo.ConsumerGroupVO;
import com.pig4cloud.pig.ontology.event.vo.DeadLetterVO;
import com.pig4cloud.pig.ontology.event.vo.EventOverviewVO;
import com.pig4cloud.pig.ontology.event.vo.OutboxVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 事件管理查询服务。
 *
 * @author youming
 */
@Service
@RequiredArgsConstructor
public class EventQueryService {

	private final OntEventOutboxMapper outboxMapper;

	private final OntEventDeadLetterMapper deadLetterMapper;

	private final StringRedisTemplate stringRedisTemplate;

	private final OntologyEventProperties properties;

	private final EventStreamAdminService streamAdminService;

	private final OntologyEventDispatcher dispatcher;

	/**
	 * 获取事件中心概览。
	 */
	public EventOverviewVO getOverview() {
		EventOverviewVO vo = new EventOverviewVO();

		// Outbox 统计
		vo.setOutboxPending(countByStatus("PENDING"));
		vo.setOutboxProcessing(countByStatus("PROCESSING"));
		vo.setOutboxPublished(countByStatus("PUBLISHED"));
		vo.setOutboxFailed(countByStatus("FAILED"));

		// Stream 长度
		vo.setStreamLength(streamAdminService.getStreamLength());

		// 死信统计
		LambdaQueryWrapper<OntEventDeadLetter> dlqWrapper = new LambdaQueryWrapper<>();
		dlqWrapper.eq(OntEventDeadLetter::getStatus, "OPEN").eq(OntEventDeadLetter::getDelFlag, "0");
		vo.setDeadLetterOpen(deadLetterMapper.selectCount(dlqWrapper));

		// Redis 可用性
		vo.setRedisAvailable(OntologyEventConfiguration.isRedisAvailable(stringRedisTemplate));

		return vo;
	}

	/**
	 * Outbox 分页查询。
	 */
	public Page<OutboxVO> outboxPage(Page<OntEventOutbox> page, String eventType, String status,
			Long ontologyId, String eventId, String aggregateType, String aggregateId) {
		LambdaQueryWrapper<OntEventOutbox> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(OntEventOutbox::getDelFlag, "0");
		wrapper.eq(StrUtil.isNotBlank(eventType), OntEventOutbox::getEventType, eventType);
		wrapper.eq(StrUtil.isNotBlank(status), OntEventOutbox::getStatus, status);
		wrapper.eq(ontologyId != null, OntEventOutbox::getOntologyId, ontologyId);
		wrapper.eq(StrUtil.isNotBlank(eventId), OntEventOutbox::getEventId, eventId);
		wrapper.eq(StrUtil.isNotBlank(aggregateType), OntEventOutbox::getAggregateType, aggregateType);
		wrapper.eq(StrUtil.isNotBlank(aggregateId), OntEventOutbox::getAggregateId, aggregateId);
		wrapper.orderByDesc(OntEventOutbox::getCreateTime);

		Page<OntEventOutbox> result = outboxMapper.selectPage(page, wrapper);

		// 转换为 VO
		Page<OutboxVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
		List<OutboxVO> voList = result.getRecords().stream().map(this::toOutboxVO).toList();
		voPage.setRecords(voList);
		return voPage;
	}

	/**
	 * 死信分页查询。
	 */
	public Page<DeadLetterVO> deadLetterPage(Page<OntEventDeadLetter> page, String consumerGroup,
			String eventType, String status, String eventId) {
		LambdaQueryWrapper<OntEventDeadLetter> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(OntEventDeadLetter::getDelFlag, "0");
		wrapper.eq(StrUtil.isNotBlank(consumerGroup), OntEventDeadLetter::getConsumerGroup, consumerGroup);
		wrapper.eq(StrUtil.isNotBlank(eventType), OntEventDeadLetter::getEventType, eventType);
		wrapper.eq(StrUtil.isNotBlank(status), OntEventDeadLetter::getStatus, status);
		wrapper.eq(StrUtil.isNotBlank(eventId), OntEventDeadLetter::getEventId, eventId);
		wrapper.orderByDesc(OntEventDeadLetter::getCreateTime);

		Page<OntEventDeadLetter> result = deadLetterMapper.selectPage(page, wrapper);

		Page<DeadLetterVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
		List<DeadLetterVO> voList = result.getRecords().stream().map(this::toDeadLetterVO).toList();
		voPage.setRecords(voList);
		return voPage;
	}

	/**
	 * 获取所有消费者组状态。
	 */
	public List<ConsumerGroupVO> getConsumerGroups() {
		List<ConsumerGroupVO> result = new ArrayList<>();
		List<OntologyEventHandler> handlers = dispatcher.getHandlers();
		String streamKey = properties.getStreamKey();

		for (OntologyEventHandler handler : handlers) {
			ConsumerGroupVO vo = new ConsumerGroupVO();
			vo.setConsumerGroup(handler.consumerGroup());

			try {
				PendingMessagesSummary summary = stringRedisTemplate.opsForStream()
					.pending(streamKey, handler.consumerGroup());
				vo.setPendingCount(summary != null ? summary.getTotalPendingMessages() : 0);
				vo.setOnline(true);
			}
			catch (Exception e) {
				vo.setPendingCount(0);
				vo.setOnline(false);
			}

			result.add(vo);
		}

		return result;
	}

	private long countByStatus(String status) {
		LambdaQueryWrapper<OntEventOutbox> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(OntEventOutbox::getStatus, status).eq(OntEventOutbox::getDelFlag, "0");
		return outboxMapper.selectCount(wrapper);
	}

	private OutboxVO toOutboxVO(OntEventOutbox outbox) {
		OutboxVO vo = new OutboxVO();
		// 复制基本字段
		vo.setId(outbox.getId());
		vo.setEventId(outbox.getEventId());
		vo.setEventType(outbox.getEventType());
		vo.setEventVersion(outbox.getEventVersion());
		vo.setOntologyId(outbox.getOntologyId());
		vo.setAggregateType(outbox.getAggregateType());
		vo.setAggregateId(outbox.getAggregateId());
		vo.setOperation(outbox.getOperation());
		vo.setOccurredAt(outbox.getOccurredAt());
		vo.setStatus(outbox.getStatus());
		vo.setDeliveryAttempt(outbox.getDeliveryAttempt());
		vo.setStreamRecordId(outbox.getStreamRecordId());
		vo.setPublishedAt(outbox.getPublishedAt());
		vo.setLastErrorCode(outbox.getLastErrorCode());
		vo.setLastErrorMessage(outbox.getLastErrorMessage());
		vo.setCreateTime(outbox.getCreateTime());
		// payload 摘要
		String payload = outbox.getPayload();
		vo.setPayloadSummary(payload != null && payload.length() > 200
				? payload.substring(0, 200) + "..." : payload);
		return vo;
	}

	private DeadLetterVO toDeadLetterVO(OntEventDeadLetter dlq) {
		DeadLetterVO vo = new DeadLetterVO();
		vo.setId(dlq.getId());
		vo.setConsumerGroup(dlq.getConsumerGroup());
		vo.setEventId(dlq.getEventId());
		vo.setReplayNo(dlq.getReplayNo());
		vo.setEventType(dlq.getEventType());
		vo.setStreamRecordId(dlq.getStreamRecordId());
		vo.setFailureCategory(dlq.getFailureCategory());
		vo.setErrorCode(dlq.getErrorCode());
		vo.setErrorMessage(dlq.getErrorMessage());
		vo.setDeliveryCount(dlq.getDeliveryCount());
		vo.setStatus(dlq.getStatus());
		vo.setReplayedAsNo(dlq.getReplayedAsNo());
		vo.setFirstFailedAt(dlq.getFirstFailedAt());
		vo.setLastFailedAt(dlq.getLastFailedAt());
		vo.setResolvedAt(dlq.getResolvedAt());
		vo.setCreateTime(dlq.getCreateTime());
		String payload = dlq.getPayload();
		vo.setPayloadSummary(payload != null && payload.length() > 200
				? payload.substring(0, 200) + "..." : payload);
		return vo;
	}

}
