/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.integration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 映射可观测指标采集（18-08 §13）。
 * <p>
 * 基于 Micrometer，指标命名统一前缀 {@code ontology.mapping.}。
 * <p>
 * <b>高基数控制</b>（§13）：
 * <ul>
 *   <li>不以 jobId、instanceId、sourceRecordKey 作为指标 label</li>
 *   <li>mappingProjectId/mappingCode 数量可控但仍需配置上限</li>
 *   <li>详细 ID 放日志和 trace</li>
 * </ul>
 *
 * @author youming
 */
@Slf4j
@Component
public class MappingMetrics {

	private final MeterRegistry meterRegistry;

	private final ConcurrentMap<String, Counter> counterCache = new ConcurrentHashMap<>();

	/**
	 * 当 Micrometer 不可用时（测试环境或 actuator 未启用）为 null。
	 */
	public MappingMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		if (meterRegistry == null) {
			log.warn("MeterRegistry is null, mapping metrics will be no-ops");
		}
	}

	// ==================== Counter ====================

	/**
	 * 记录数据源连接测试结果。
	 */
	public void recordDataSourceTest(String result, String databaseType) {
		incrementCounter("ontology.mapping.datasource.test.total",
				"result", result, "databaseType", databaseType != null ? databaseType : "unknown");
	}

	/**
	 * 记录作业总数。
	 */
	public void recordJobTotal(String runType, String status) {
		incrementCounter("ontology.mapping.job.total",
				"runType", runType != null ? runType : "unknown",
				"status", status != null ? status : "unknown");
	}

	/**
	 * 记录记录级操作。
	 */
	public void recordRecordTotal(String mappingCode, String action, String status) {
		incrementCounter("ontology.mapping.record.total",
				"mappingCode", mappingCode != null ? mappingCode : "unknown",
				"action", action != null ? action : "unknown",
				"status", status != null ? status : "unknown");
	}

	/**
	 * 记录记录级错误。
	 */
	public void recordRecordError(String errorCode) {
		incrementCounter("ontology.mapping.record.error.total",
				"errorCode", errorCode != null ? errorCode : "unknown");
	}

	/**
	 * 记录安全拒绝。
	 */
	public void recordSecurityDenied(String resourceType) {
		incrementCounter("ontology.mapping.security.denied.total",
				"resourceType", resourceType != null ? resourceType : "unknown");
	}

	// ==================== Timer ====================

	/**
	 * 记录作业耗时。
	 */
	public void recordJobDuration(String runType, Duration duration) {
		if (meterRegistry == null) {
			return;
		}
		Timer.builder("ontology.mapping.job.duration")
			.tag("runType", runType != null ? runType : "unknown")
			.register(meterRegistry)
			.record(duration);
	}

	/**
	 * 记录摄入耗时。
	 */
	public void recordIngestionDuration(String action, Duration duration) {
		if (meterRegistry == null) {
			return;
		}
		Timer.builder("ontology.mapping.ingestion.duration")
			.tag("action", action != null ? action : "unknown")
			.register(meterRegistry)
			.record(duration);
	}

	/**
	 * 记录元数据刷新耗时。
	 */
	public void recordMetadataRefreshDuration(Duration duration) {
		if (meterRegistry == null) {
			return;
		}
		Timer.builder("ontology.mapping.metadata.refresh.duration")
			.register(meterRegistry)
			.record(duration);
	}

	// ==================== 内部方法 ====================

	private void incrementCounter(String name, String... tags) {
		if (meterRegistry == null) {
			return;
		}
		String cacheKey = name + ":" + String.join(":", tags);
		Counter counter = counterCache.computeIfAbsent(cacheKey, k ->
			Counter.builder(name)
				.tags(tags)
				.register(meterRegistry));
		counter.increment();
	}

}
