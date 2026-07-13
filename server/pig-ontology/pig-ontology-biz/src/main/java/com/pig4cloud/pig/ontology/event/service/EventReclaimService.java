/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.service;

import com.pig4cloud.pig.ontology.event.config.OntologyEventProperties;
import com.pig4cloud.pig.ontology.event.handler.OntologyEventDispatcher;
import com.pig4cloud.pig.ontology.event.handler.OntologyEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * PEL (Pending Entry List) reclaim 服务。
 * <p>
 * 定时扫描各 Group 的 Pending 消息，对空闲超过 claim-idle 的消息使用 claim 回收。
 * reclaim 后由 Listener Container 重新消费，Inbox 幂等保证不重复处理。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventReclaimService {

	private final StringRedisTemplate stringRedisTemplate;

	private final OntologyEventProperties properties;

	private final OntologyEventDispatcher dispatcher;

	/**
	 * 定时 reclaim PEL 中空闲过久的消息。
	 */
	@Scheduled(fixedDelayString = "${ontology.event.consumer.claim-idle:60s}",
			initialDelayString = "${ontology.event.consumer.claim-idle:60s}")
	public void reclaim() {
		if (!properties.isEnabled()) {
			return;
		}

		List<OntologyEventHandler> handlers = dispatcher.getHandlers();
		if (handlers == null || handlers.isEmpty()) {
			return;
		}

		String streamKey = properties.getStreamKey();
		Duration minIdleTime = properties.getConsumer().getClaimIdle();
		String newConsumer = "reclaimer-" + getPid();
		long batchSize = properties.getConsumer().getBatchSize();

		for (OntologyEventHandler handler : handlers) {
			String group = handler.consumerGroup();
			try {
				reclaimGroup(streamKey, group, newConsumer, minIdleTime, batchSize);
			}
			catch (Exception e) {
				log.error("PEL reclaim 失败: group={}", group, e);
			}
		}
	}

	/**
	 * 回收一个 Group 的 PEL 消息。
	 * <p>
	 * 1. 查询 Pending Summary 获取待处理消息总数 2. 查询 Pending 详情，筛选空闲超过 minIdleTime 的消息
	 * 3. 使用 claim 将这些消息转移给 newConsumer
	 */
	private void reclaimGroup(String streamKey, String group, String newConsumer,
			Duration minIdleTime, long batchSize) {
		// 1. 查询 Pending Summary
		var summary = stringRedisTemplate.opsForStream().pending(streamKey, group);
		if (summary == null || summary.getTotalPendingMessages() == 0) {
			return;
		}

		log.debug("Group 有待处理消息: group={}, pending={}", group, summary.getTotalPendingMessages());

		// 2. 查询 Pending 详情（全部范围，限制 batchSize 条）
		PendingMessages pendingMessages = stringRedisTemplate.opsForStream()
			.pending(streamKey, group, Range.unbounded(), batchSize);

		if (pendingMessages == null || pendingMessages.isEmpty()) {
			return;
		}

		// 3. 筛选空闲超过 minIdleTime 的消息 ID
		List<RecordId> toClaim = new ArrayList<>();
		for (PendingMessage pm : pendingMessages) {
			Duration idleTime = pm.getElapsedTimeSinceLastDelivery();
			if (idleTime != null && idleTime.compareTo(minIdleTime) >= 0) {
				toClaim.add(pm.getId());
			}
		}

		if (toClaim.isEmpty()) {
			return;
		}

		// 4. Claim 这些消息转移给 newConsumer
		RecordId[] recordIds = toClaim.toArray(new RecordId[0]);
		var claimedMessages = stringRedisTemplate.opsForStream()
			.claim(streamKey, group, newConsumer, minIdleTime, recordIds);

		if (claimedMessages != null && !claimedMessages.isEmpty()) {
			log.info("PEL reclaim 完成: group={}, reclaimed={}", group, claimedMessages.size());
		}
	}

	private String getPid() {
		String hostname = System.getenv("HOSTNAME");
		if (hostname == null || hostname.isBlank()) {
			hostname = "localhost";
		}
		return hostname + "-" + ProcessHandle.current().pid();
	}

}
