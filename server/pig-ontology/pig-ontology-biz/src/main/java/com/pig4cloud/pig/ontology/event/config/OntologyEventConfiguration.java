/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 事件驱动骨干自动配置。
 *
 * @author youming
 */
@Configuration
@EnableConfigurationProperties(OntologyEventProperties.class)
@EnableScheduling
@ConditionalOnProperty(prefix = "ontology.event", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OntologyEventConfiguration {

	/**
	 * 事件 JSON 序列化器，复用 Spring 容器中的 ObjectMapper。
	 * <p>
	 * 不创建独立 ObjectMapper 实例，与仓库其他模块（version/snapshot 等）保持一致的序列化行为。
	 * @param objectMapper Spring Boot 自动配置的 ObjectMapper
	 * @return 事件 JSON 序列化器
	 */
	@Bean("ontologyEventJsonMapper")
	public EventJsonMapper eventJsonMapper(ObjectMapper objectMapper) {
		return new EventJsonMapper(objectMapper);
	}

	/**
	 * 事件 JSON 序列化/反序列化工具。
	 * <p>
	 * 使用 Spring Boot 自动配置的 ObjectMapper，确保 JavaTimeModule（Instant 序列化）已注册。
	 */
	public static class EventJsonMapper {

		private final ObjectMapper objectMapper;

		public EventJsonMapper(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		public String toJson(Object obj) {
			try {
				return objectMapper.writeValueAsString(obj);
			}
			catch (Exception e) {
				throw new IllegalStateException("事件 JSON 序列化失败", e);
			}
		}

		public <T> T fromJson(String json, Class<T> type) {
			try {
				return objectMapper.readValue(json, type);
			}
			catch (Exception e) {
				throw new IllegalStateException("事件 JSON 反序列化失败: " + type.getSimpleName(), e);
			}
		}

	}

	/**
	 * 判断 StringRedisTemplate 是否可连接（用于健康检查）。
	 * @param redisTemplate Spring Boot 自动配置的 StringRedisTemplate
	 * @return true 如果 Redis 可连接
	 */
	public static boolean isRedisAvailable(StringRedisTemplate redisTemplate) {
		try {
			String pong = redisTemplate.getConnectionFactory().getConnection().ping();
			return "PONG".equalsIgnoreCase(pong);
		}
		catch (Exception e) {
			return false;
		}
	}

}
