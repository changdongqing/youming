/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.service;

import com.pig4cloud.pig.ontology.event.config.OntologyEventProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis Stream 和 Consumer Group 初始化服务。
 * <p>
 * 应用启动时幂等创建 Stream Key 和各 Consumer Group，以便不同环境使用配置化 Stream Key。
 * 迁移不创建 Redis Consumer Group（Flyway 只管 DB），Group 在此初始化。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventStreamAdminService {

	private final StringRedisTemplate stringRedisTemplate;

	private final OntologyEventProperties properties;

	/**
	 * 幂等创建 Consumer Group。
	 * <p>
	 * 如果 Stream 不存在，先创建一个空 Stream（XADD + DEL 占位），再创建 Group 从 0-0 开始。
	 * 如果 Group 已存在，忽略 BUSYGROUP 错误。
	 * @param groupName 消费者组名称
	 * @param startId 起始 ID：全新部署用 "0-0"，后加消费者用 "$"
	 */
	public void createConsumerGroupIfAbsent(String groupName, String startId) {
		String streamKey = properties.getStreamKey();
		try {
			stringRedisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from(startId), groupName);
			log.info("已创建 Consumer Group: stream={}, group={}, startId={}", streamKey, groupName, startId);
		}
		catch (RedisSystemException e) {
			String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
			if (message != null && message.contains("BUSYGROUP")) {
				log.debug("Consumer Group 已存在，跳过: stream={}, group={}", streamKey, groupName);
			}
			else if (message != null && message.contains("no such key")) {
				// Stream 不存在，先创建空 Stream 再创建 Group
				log.info("Stream 不存在，先创建再建 Group: stream={}", streamKey);
				ensureStreamExists();
				stringRedisTemplate.opsForStream()
					.createGroup(streamKey, ReadOffset.from(startId), groupName);
				log.info("已创建 Consumer Group: stream={}, group={}", streamKey, groupName);
			}
			else {
				throw e;
			}
		}
	}

	/**
	 * 确保 Stream Key 存在（通过 XADD 一条占位记录再 XDEL）。
	 */
	public void ensureStreamExists() {
		String streamKey = properties.getStreamKey();
		Boolean hasKey = stringRedisTemplate.hasKey(streamKey);
		if (Boolean.TRUE.equals(hasKey)) {
			return;
		}
		// XADD 一条占位记录来创建 Stream，然后 XDEL 删除
		var recordId = stringRedisTemplate.opsForStream()
			.add(streamKey, java.util.Map.of("_init", "1"));
		stringRedisTemplate.opsForStream().delete(streamKey, recordId);
		log.info("Stream 已初始化: key={}", streamKey);
	}

	/**
	 * 获取 Stream 长度。
	 * @return Stream 中的记录数
	 */
	public long getStreamLength() {
		Long len = stringRedisTemplate.opsForStream().size(properties.getStreamKey());
		return len != null ? len : 0L;
	}

	/**
	 * 修剪 Stream 到最大长度（近似 MAXLEN）。
	 */
	public void trimStream() {
		Long trimmed = stringRedisTemplate.opsForStream()
			.trim(properties.getStreamKey(), properties.getStreamMaxLength());
		if (trimmed != null && trimmed > 0) {
			log.debug("Stream 修剪完成: key={}, trimmed={}", properties.getStreamKey(), trimmed);
		}
	}

}
