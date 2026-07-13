/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.handler;

import com.pig4cloud.pig.ontology.event.config.OntologyEventConfiguration.EventJsonMapper;
import com.pig4cloud.pig.ontology.event.config.OntologyEventProperties;
import com.pig4cloud.pig.ontology.event.service.EventConsumeService;
import com.pig4cloud.pig.ontology.event.service.EventStreamAdminService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 事件分发器：按 Consumer Group 创建 StreamMessageListenerContainer 并路由到 Handler。
 * <p>
 * 每个 Handler 对应一个 Consumer Group，Dispatcher 在启动时： 1. 幂等创建 Consumer Group
 * 2. 创建 Listener Container 并注册消费回调 3. Handler 路由：找到 group 匹配的 Handler 执行
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OntologyEventDispatcher {

	private final List<OntologyEventHandler> handlers;

	private final StringRedisTemplate stringRedisTemplate;

	private final OntologyEventProperties properties;

	private final EventStreamAdminService streamAdminService;

	private final EventConsumeService eventConsumeService;

	private final EventJsonMapper eventJsonMapper;

	private final List<Subscription> subscriptions = new ArrayList<>();

	private final Map<String, OntologyEventHandler> handlerByGroup = new HashMap<>();

	@PostConstruct
	public void init() {
		if (!properties.isEnabled()) {
			log.info("事件驱动骨干已禁用，跳过 Dispatcher 初始化");
			return;
		}

		if (handlers.isEmpty()) {
			log.warn("未注册任何 OntologyEventHandler，Dispatcher 不启动消费");
			return;
		}

		for (OntologyEventHandler handler : handlers) {
			String group = handler.consumerGroup();
			handlerByGroup.put(group, handler);

			try {
				// 幂等创建 Consumer Group，从 0-0 开始读取历史事件
				streamAdminService.createConsumerGroupIfAbsent(group, "0-0");

				// 创建 Listener Container
				startListening(handler);
				log.info("已启动事件消费者: group={}, events={}", group, handler.supportedEventTypes());
			}
			catch (Exception e) {
				log.error("启动事件消费者失败: group={}", group, e);
			}
		}
	}

	/**
	 * 为一个 Handler 创建 Listener 并注册消费回调。
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void startListening(OntologyEventHandler handler) {
		String streamKey = properties.getStreamKey();
		String group = handler.consumerGroup();
		String consumerName = group + "-" + getPid();

		StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
				StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
					.pollTimeout(properties.getConsumer().getPollTimeout())
					.batchSize(properties.getConsumer().getBatchSize())
					.errorHandler((throwable) -> log.error("Stream 消费错误: group={}", group, throwable))
					.build();

		StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
				StreamMessageListenerContainer.create(stringRedisTemplate.getConnectionFactory(), options);

		StreamMessageListenerContainer.ConsumerStreamReadRequest<String> request = StreamMessageListenerContainer.StreamReadRequest
			.builder(StreamOffset.create(streamKey, ReadOffset.lastConsumed()))
			.consumer(Consumer.from(group, consumerName))
			.autoAcknowledge(false)
			.build();

		Subscription subscription = container.register(request, message -> {
			try {
				eventConsumeService.consume(message, handler);
			}
			catch (Exception e) {
				log.error("事件消费调度异常: group={}, recordId={}", group, message.getId().getValue(), e);
			}
		});

		container.start();
		subscriptions.add(subscription);
	}

	@PreDestroy
	public void destroy() {
		for (Subscription subscription : subscriptions) {
			try {
				if (subscription != null && subscription.isActive()) {
					subscription.cancel();
				}
			}
			catch (Exception e) {
				log.warn("取消订阅异常", e);
			}
		}
		log.info("事件 Dispatcher 已停止，共关闭 {} 个订阅", subscriptions.size());
	}

	/**
	 * 获取所有已注册的 Handler。
	 * @return Handler 列表
	 */
	public List<OntologyEventHandler> getHandlers() {
		return handlers;
	}

	/**
	 * 按 Group 查找 Handler。
	 * @param group Consumer Group 名称
	 * @return 匹配的 Handler，不存在返回 null
	 */
	public OntologyEventHandler findHandler(String group) {
		return handlerByGroup.get(group);
	}

	private String getPid() {
		String hostname = System.getenv("HOSTNAME");
		if (hostname == null || hostname.isBlank()) {
			hostname = "localhost";
		}
		return hostname + "-" + ProcessHandle.current().pid();
	}

}
