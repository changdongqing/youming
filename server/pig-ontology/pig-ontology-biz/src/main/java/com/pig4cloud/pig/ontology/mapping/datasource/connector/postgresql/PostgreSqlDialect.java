/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.connector.postgresql;

import com.pig4cloud.pig.ontology.mapping.datasource.DataSourceErrorCode;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

/**
 * PostgreSQL 方言工具。
 * <p>
 * 提供标识符转义、JDBC URL 参数校验、系统对象过滤等能力。
 *
 * @author youming
 */
public final class PostgreSqlDialect {

	/**
	 * 默认拒绝的系统 Schema。
	 */
	private static final Set<String> SYSTEM_SCHEMAS = Set.of("pg_catalog", "information_schema");

	/**
	 * 系统对象前缀。
	 */
	private static final String SYSTEM_PREFIX = "pg_";

	/**
	 * JDBC URL 中禁止出现的参数（不区分大小写）。
	 */
	private static final Set<String> FORBIDDEN_URL_PARAMS = Set.of("user", "password", "username", "pwd");

	private PostgreSqlDialect() {
	}

	/**
	 * 转义标识符（PostgreSQL 双引号方式）。
	 * <p>
	 * 内部双引号转义为连续两个双引号。
	 */
	public static String escapeIdentifier(String identifier) {
		if (identifier == null || identifier.isEmpty()) {
			throw new IllegalArgumentException("Identifier must not be empty");
		}
		String escaped = identifier.replace("\"", "\"\"");
		return "\"" + escaped + "\"";
	}

	/**
	 * 判断 Schema 是否为系统 Schema。
	 */
	public static boolean isSystemSchema(String schemaName) {
		if (schemaName == null) {
			return false;
		}
		return SYSTEM_SCHEMAS.contains(schemaName.toLowerCase())
				|| schemaName.toLowerCase().startsWith(SYSTEM_PREFIX);
	}

	/**
	 * 过滤掉系统 Schema。
	 */
	public static List<String> filterSystemSchemas(List<String> schemas) {
		return schemas.stream()
			.filter(s -> !isSystemSchema(s))
			.toList();
	}

	/**
	 * 判断对象名是否为系统对象（以 pg_ 开头）。
	 */
	public static boolean isSystemObject(String objectName) {
		return objectName != null && objectName.toLowerCase().startsWith(SYSTEM_PREFIX);
	}

	/**
	 * 校验 JDBC URL，拒绝包含 user/password 参数的 URL。
	 *
	 * @param jdbcUrl JDBC URL
	 * @throws IllegalArgumentException 如果 URL 含非法参数
	 */
	public static void validateJdbcUrl(String jdbcUrl) {
		if (jdbcUrl == null || jdbcUrl.isBlank()) {
			throw new IllegalArgumentException("JDBC URL must not be empty");
		}

		try {
			// jdbc:postgresql://host:port/db?params -> 取 query 部分
			String normalized = jdbcUrl.trim();
			int queryIndex = normalized.indexOf('?');
			if (queryIndex < 0) {
				return;
			}
			String query = normalized.substring(queryIndex + 1);
			String[] pairs = query.split("&");
			for (String pair : pairs) {
				String[] kv = pair.split("=", 2);
				if (kv.length > 0) {
					String key = kv[0].trim().toLowerCase();
					if (FORBIDDEN_URL_PARAMS.contains(key)) {
						throw new IllegalArgumentException(
							DataSourceErrorCode.ONT_DS_011.getMessage() + ": " + key);
					}
				}
			}
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Invalid JDBC URL format", e);
		}
	}

	/**
	 * 构建 HOST 模式的 JDBC URL。
	 */
	public static String buildJdbcUrl(String host, int port, String database, String sslMode) {
		String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
		if (sslMode != null && !sslMode.isBlank() && !"disable".equalsIgnoreCase(sslMode)) {
			url += "?sslmode=" + sslMode;
		}
		return url;
	}

	/**
	 * 解析 JDBC URL 中的主机和端口。
	 */
	public static String[] parseHostPort(String jdbcUrl) throws URISyntaxException {
		// jdbc:postgresql://host:port/db -> //host:port/db
		String stripped = jdbcUrl.substring("jdbc:postgresql:".length());
		URI uri = new URI(stripped);
		String host = uri.getHost();
		int port = uri.getPort();
		return new String[] { host, String.valueOf(port) };
	}

}
