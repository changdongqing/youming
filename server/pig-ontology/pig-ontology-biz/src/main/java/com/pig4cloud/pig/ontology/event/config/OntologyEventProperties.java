/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 事件驱动骨干配置属性。
 *
 * @author youming
 */
@Data
@ConfigurationProperties(prefix = "ontology.event")
public class OntologyEventProperties {

	/**
	 * 是否启用事件驱动骨干。
	 */
	private boolean enabled = true;

	/**
	 * Redis Stream Key。
	 */
	private String streamKey = "youming:ontology:domain-events";

	/**
	 * 单个事件最大字节数，默认 64KiB。
	 */
	private int maxEventBytes = 65536;

	/**
	 * Redis Stream 最大保留长度（近似 MAXLEN）。
	 */
	private long streamMaxLength = 100000;

	/**
	 * Outbox 相关配置。
	 */
	private Outbox outbox = new Outbox();

	/**
	 * 消费者相关配置。
	 */
	private Consumer consumer = new Consumer();

	/**
	 * 回放相关配置。
	 */
	private Replay replay = new Replay();

	@Data
	public static class Outbox {

		/**
		 * 轮询间隔。
		 */
		private Duration pollDelay = Duration.ofSeconds(2);

		/**
		 * 单次领取批次大小。
		 */
		private int batchSize = 100;

		/**
		 * 领取租约时长。
		 */
		private Duration leaseDuration = Duration.ofSeconds(30);

		/**
		 * 最大投递尝试次数。
		 */
		private int maxAttempts = 10;

		/**
		 * PUBLISHED 行保留天数。
		 */
		private int publishedRetentionDays = 30;

	}

	@Data
	public static class Consumer {

		/**
		 * 单次读取批次大小。
		 */
		private int batchSize = 20;

		/**
		 * 轮询超时。
		 */
		private Duration pollTimeout = Duration.ofSeconds(2);

		/**
		 * PEL 中消息空闲超过此时间后 reclaim。
		 */
		private Duration claimIdle = Duration.ofSeconds(60);

		/**
		 * 消费处理租约时长。
		 */
		private Duration processingLease = Duration.ofSeconds(120);

		/**
		 * 最大消费尝试次数，超过后入死信。
		 */
		private int maxAttempts = 5;

	}

	@Data
	public static class Replay {

		/**
		 * 单次回放请求最大事件数。
		 */
		private int maxEventsPerRequest = 1000;

	}

}
