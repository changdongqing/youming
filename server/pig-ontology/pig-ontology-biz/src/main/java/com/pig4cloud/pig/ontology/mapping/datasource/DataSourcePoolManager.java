/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource;

import com.pig4cloud.pig.ontology.mapping.datasource.connector.postgresql.PostgreSqlDialect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源连接池管理器。
 * <p>
 * 按 {@code sourceId + revision} 缓存独立小连接池，V1 使用简单的连接缓存（不引入 HikariCP/Druid 动态源）。
 * <ul>
 *   <li>每个数据源+版本维护一个小池，默认最大2连接</li>
 *   <li>空闲10分钟自动关闭</li>
 *   <li>配置修改、停用、删除立即关闭旧池</li>
 *   <li>不把动态源注册进全局 DynamicRoutingDataSource</li>
 *   <li>关闭失败记录 WARN，不阻塞配置事务完成</li>
 * </ul>
 *
 * @author youming
 */
@Slf4j
@Component
public class DataSourcePoolManager {

	private static final int MAX_POOL_SIZE = 2;

	private static final long IDLE_TIMEOUT_MS = 10 * 60 * 1000L; // 10分钟

	private final Map<String, PooledEntry> poolMap = new ConcurrentHashMap<>();

	/**
	 * 获取连接。
	 * <p>
	 * 如果池中已有可用连接则复用，否则新建。连接使用后由调用方 close（实际归还池而非真正关闭）。
	 *
	 * @param sourceId    数据源ID
	 * @param revision    配置版本号
	 * @param jdbcUrl     JDBC URL
	 * @param username    用户名
	 * @param password    密码
	 * @param timeoutSeconds 连接超时秒数
	 * @return 只读连接
	 * @throws SQLException 连接失败
	 */
	public Connection getConnection(Long sourceId, Long revision, String jdbcUrl, String username,
			String password, int timeoutSeconds) throws SQLException {
		PostgreSqlDialect.validateJdbcUrl(jdbcUrl);
		String key = buildKey(sourceId, revision);

		synchronized (poolMap) {
			PooledEntry entry = poolMap.get(key);
			if (entry != null && !entry.isFull()) {
				entry.lastUsedAt = System.currentTimeMillis();
				Connection conn = entry.pollConnection();
				if (conn != null && !conn.isClosed()) {
					return wrapConnection(key, conn);
				}
			}
		}

		// 新建连接
		Connection conn = createConnection(jdbcUrl, username, password, timeoutSeconds);
		synchronized (poolMap) {
			PooledEntry entry = poolMap.computeIfAbsent(key, k -> new PooledEntry());
			entry.lastUsedAt = System.currentTimeMillis();
		}
		return wrapConnection(key, conn);
	}

	/**
	 * 按 sourceId 失效连接池（配置变更、停用、删除时调用）。
	 */
	public void invalidateBySourceId(Long sourceId) {
		String prefix = sourceId + ":";
		synchronized (poolMap) {
			poolMap.entrySet().removeIf(entry -> {
				if (entry.getKey().startsWith(prefix)) {
					closePoolQuietly(entry.getKey(), entry.getValue());
					return true;
				}
				return false;
			});
		}
		log.info("Invalidated connection pools for sourceId={}", sourceId);
	}

	/**
	 * 空闲连接池清理（每5分钟执行一次）。
	 */
	@Scheduled(fixedDelay = 5 * 60 * 1000L)
	public void evictIdlePools() {
		long now = System.currentTimeMillis();
		synchronized (poolMap) {
			poolMap.entrySet().removeIf(entry -> {
				if (now - entry.getValue().lastUsedAt > IDLE_TIMEOUT_MS) {
					closePoolQuietly(entry.getKey(), entry.getValue());
					log.debug("Evicted idle pool: {}", entry.getKey());
					return true;
				}
				return false;
			});
		}
	}

	/**
	 * 创建只读连接。
	 */
	private Connection createConnection(String jdbcUrl, String username, String password, int timeoutSeconds)
			throws SQLException {
		Properties props = new Properties();
		props.setProperty("user", username);
		props.setProperty("password", password);
		props.setProperty("readOnly", "true");
		props.setProperty("loginTimeout", String.valueOf(Math.max(1, timeoutSeconds)));
		props.setProperty("socketTimeout", String.valueOf(timeoutSeconds * 1000));
		props.setProperty("statementTimeout", String.valueOf(timeoutSeconds * 1000));

		Connection conn = DriverManager.getConnection(jdbcUrl, props);
		conn.setReadOnly(true);
		return conn;
	}

	/**
	 * 包装连接，使 close 时归还池而非真正关闭。
	 */
	private Connection wrapConnection(String poolKey, Connection delegate) {
		return new PooledConnectionWrapper(poolKey, delegate, this);
	}

	/**
	 * 归还连接到池。
	 */
	void returnConnection(String poolKey, Connection conn) {
		synchronized (poolMap) {
			PooledEntry entry = poolMap.get(poolKey);
			if (entry == null || entry.isFull()) {
				// 池已满或已失效，直接关闭
				closeQuietly(conn);
				return;
			}
			try {
				if (!conn.isClosed()) {
					entry.offerConnection(conn);
					entry.lastUsedAt = System.currentTimeMillis();
					return;
				}
			}
			catch (SQLException e) {
				// 连接已失效
			}
		}
	}

	private void closePoolQuietly(String key, PooledEntry entry) {
		Connection conn;
		while ((conn = entry.pollConnection()) != null) {
			closeQuietly(conn);
		}
	}

	private void closeQuietly(Connection conn) {
		try {
			if (conn != null && !conn.isClosed()) {
				conn.close();
			}
		}
		catch (SQLException e) {
			log.warn("Failed to close connection: {}", e.getMessage());
		}
	}

	private String buildKey(Long sourceId, Long revision) {
		return sourceId + ":" + revision;
	}

	/**
	 * 连接池条目。
	 */
	private static class PooledEntry {

		private final Connection[] connections = new Connection[MAX_POOL_SIZE];

		private volatile long lastUsedAt = System.currentTimeMillis();

		boolean isFull() {
			for (Connection conn : connections) {
				if (conn == null) {
					return false;
				}
			}
			return true;
		}

		Connection pollConnection() {
			for (int i = 0; i < connections.length; i++) {
				if (connections[i] != null) {
					Connection conn = connections[i];
					connections[i] = null;
					return conn;
				}
			}
			return null;
		}

		void offerConnection(Connection conn) {
			for (int i = 0; i < connections.length; i++) {
				if (connections[i] == null) {
					connections[i] = conn;
					return;
				}
			}
		}

	}

}
