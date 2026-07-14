/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.connector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连接器注册表。
 * <p>
 * 按 {@code sourceType/databaseType} 选择工厂实现，V1 唯一映射：
 * <pre>JDBC + POSTGRESQL → PostgreSqlDataSourceConnector</pre>
 *
 * @author youming
 */
@Slf4j
@Component
public class ConnectorRegistry {

	private final Map<String, DataSourceConnectorFactory> factoryMap = new ConcurrentHashMap<>();

	/**
	 * 注册工厂（由 Spring 自动调用所有 DataSourceConnectorFactory bean）。
	 */
	@org.springframework.beans.factory.annotation.Autowired
	public void registerFactories(List<DataSourceConnectorFactory> factories) {
		for (DataSourceConnectorFactory factory : factories) {
			String key = buildKey(factory);
			factoryMap.put(key, factory);
			log.info("Registered DataSourceConnectorFactory: {}", key);
		}
	}

	/**
	 * 获取连接器。
	 * @param sourceType   源类型
	 * @param databaseType 数据库类型
	 * @return 连接器
	 * @throws IllegalArgumentException 不支持的类型组合
	 */
	public DataSourceConnector getConnector(String sourceType, String databaseType) {
		String key = buildKey(sourceType, databaseType);
		DataSourceConnectorFactory factory = factoryMap.get(key);
		if (factory == null) {
			throw new IllegalArgumentException(
				"Unsupported source type + database type: " + sourceType + "/" + databaseType);
		}
		return factory.create();
	}

	/**
	 * 是否支持指定的类型组合。
	 */
	public boolean supports(String sourceType, String databaseType) {
		return factoryMap.containsKey(buildKey(sourceType, databaseType));
	}

	/**
	 * 尝试获取连接器，返回 Optional。
	 */
	public Optional<DataSourceConnector> tryGetConnector(String sourceType, String databaseType) {
		return Optional.ofNullable(factoryMap.get(buildKey(sourceType, databaseType)))
			.map(DataSourceConnectorFactory::create);
	}

	private String buildKey(DataSourceConnectorFactory factory) {
		// 通过工厂自身的 supports 探测其键 — V1 工厂唯一支持 JDBC+POSTGRESQL
		if (factory.supports("JDBC", "POSTGRESQL")) {
			return buildKey("JDBC", "POSTGRESQL");
		}
		return "UNKNOWN";
	}

	private String buildKey(String sourceType, String databaseType) {
		return sourceType + ":" + databaseType;
	}

}
