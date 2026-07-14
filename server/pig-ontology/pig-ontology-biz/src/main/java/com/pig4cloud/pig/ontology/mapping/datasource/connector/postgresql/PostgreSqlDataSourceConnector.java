/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.connector.postgresql;

import com.pig4cloud.pig.ontology.mapping.datasource.DataSourceErrorCode;
import com.pig4cloud.pig.ontology.mapping.datasource.connector.DataSourceConnector;
import com.pig4cloud.pig.ontology.mapping.datasource.connector.DataSourceConnectorFactory;
import com.pig4cloud.pig.ontology.mapping.datasource.vo.SourceObjectMetadataVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PostgreSQL 数据源连接器实现。
 * <p>
 * 通过 JDBC {@link DatabaseMetaData} 实现 schema/表/视图/列/主键/唯一键/外键发现。
 * 所有连接设为只读，系统 schema 默认拒绝，白名单过滤。
 *
 * @author youming
 */
@Slf4j
@Component
public class PostgreSqlDataSourceConnector implements DataSourceConnector, DataSourceConnectorFactory {

	@Override
	public String type() {
		return "JDBC";
	}

	// ==================== DataSourceConnectorFactory ====================

	@Override
	public boolean supports(String sourceType, String databaseType) {
		return "JDBC".equalsIgnoreCase(sourceType) && "POSTGRESQL".equalsIgnoreCase(databaseType);
	}

	@Override
	public DataSourceConnector create() {
		return this;
	}

	// ==================== DataSourceConnector ====================

	@Override
	public ConnectionTestResult test(ConnectionRequest request) {
		long start = System.currentTimeMillis();
		try (Connection conn = openConnection(request.jdbcUrl(), request.username(),
				request.password(), request.timeoutSeconds())) {
			conn.setReadOnly(true);
			// 执行简单验证查询
			try (var stmt = conn.createStatement()) {
				stmt.setQueryTimeout(Math.max(1, request.timeoutSeconds()));
				stmt.execute("SELECT 1");
			}
			long latency = System.currentTimeMillis() - start;
			return ConnectionTestResult.ok(latency);
		}
		catch (SQLException e) {
			long latency = System.currentTimeMillis() - start;
			String errorCode = classifySQLException(e);
			String message = sanitizeExceptionMessage(e);
			log.warn("Connection test failed: code={}, message={}", errorCode, message);
			return new ConnectionTestResult(false, latency, errorCode, message);
		}
		catch (Exception e) {
			long latency = System.currentTimeMillis() - start;
			String message = sanitizeExceptionMessage(e);
			log.warn("Connection test failed: {}", message);
			return new ConnectionTestResult(false, latency, DataSourceErrorCode.ONT_DS_004.getCode(), message);
		}
	}

	@Override
	public List<String> listSchemas(MetadataRequest request) {
		List<String> schemas = new ArrayList<>();
		try {
			ResultSet rs = request.connection().getMetaData().getSchemas();
			while (rs.next()) {
				String schemaName = rs.getString("TABLE_SCHEM");
				if (PostgreSqlDialect.isSystemSchema(schemaName)) {
					continue;
				}
				if (request.allowedSchemas() != null && !request.allowedSchemas().isEmpty()
						&& !request.allowedSchemas().contains(schemaName)) {
					continue;
				}
				schemas.add(schemaName);
			}
		}
		catch (SQLException e) {
			log.error("Failed to list schemas: {}", sanitizeExceptionMessage(e));
			throw new RuntimeException("Failed to list schemas", e);
		}
		return schemas;
	}

	@Override
	public List<SourceObject> listObjects(ObjectQuery query) {
		List<SourceObject> objects = new ArrayList<>();
		try {
			String[] types = query.objectType() != null
					? new String[] { query.objectType() }
					: new String[] { "TABLE", "VIEW" };

			ResultSet rs = query.connection().getMetaData().getTables(
					query.schemaName(), null, "%", types);

			while (rs.next()) {
				String schemaName = rs.getString("TABLE_SCHEM");
				String objectName = rs.getString("TABLE_NAME");
				String objectType = rs.getString("TABLE_TYPE");

				if (PostgreSqlDialect.isSystemSchema(schemaName)
						|| PostgreSqlDialect.isSystemObject(objectName)) {
					continue;
				}

				// 白名单 Schema 过滤
				if (query.allowedSchemas() != null && !query.allowedSchemas().isEmpty()
						&& !query.allowedSchemas().contains(schemaName)) {
					continue;
				}

				// 白名单对象过滤
				if (query.allowedObjects() != null && !query.allowedObjects().isEmpty()
						&& !query.allowedObjects().contains(objectName)) {
					continue;
				}

				// 名称前缀过滤
				if (query.objectNamePrefix() != null && !query.objectNamePrefix().isBlank()) {
					if (!objectName.toLowerCase().startsWith(query.objectNamePrefix().toLowerCase())) {
						continue;
					}
				}

				objects.add(new SourceObject(schemaName, objectName, normalizeTableType(objectType)));
			}
		}
		catch (SQLException e) {
			log.error("Failed to list objects: {}", sanitizeExceptionMessage(e));
			throw new RuntimeException("Failed to list objects", e);
		}
		return objects;
	}

	@Override
	public SourceObjectMetadataVO describeObject(ObjectRef ref) {
		try {
			DatabaseMetaData metaData = ref.connection().getMetaData();

			// 列
			List<SourceObjectMetadataVO.ColumnMetadata> columns = new ArrayList<>();
			ResultSet colRs = metaData.getColumns(ref.schemaName(), null, ref.objectName(), "%");
			while (colRs.next()) {
				SourceObjectMetadataVO.ColumnMetadata col = new SourceObjectMetadataVO.ColumnMetadata();
				col.setName(colRs.getString("COLUMN_NAME"));
				col.setJdbcType(colRs.getString("TYPE_NAME"));
				col.setNullable(colRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
				col.setOrdinal(colRs.getInt("ORDINAL_POSITION"));
				col.setSize(colRs.getInt("COLUMN_SIZE"));
				columns.add(col);
			}
			colRs.close();
			columns.sort(Comparator.comparingInt(SourceObjectMetadataVO.ColumnMetadata::getOrdinal));

			// 主键
			List<String> primaryKey = new ArrayList<>();
			ResultSet pkRs = metaData.getPrimaryKeys(ref.schemaName(), null, ref.objectName());
			while (pkRs.next()) {
				primaryKey.add(pkRs.getString("COLUMN_NAME"));
			}
			pkRs.close();

			// 唯一键
			List<SourceObjectMetadataVO.UniqueKey> uniqueKeys = new ArrayList<>();
			ResultSet ukRs = metaData.getIndexInfo(ref.schemaName(), null, ref.objectName(), true, false);
			Map<String, List<String>> ukMap = new java.util.LinkedHashMap<>();
			while (ukRs.next()) {
				String indexName = ukRs.getString("INDEX_NAME");
				String columnName = ukRs.getString("COLUMN_NAME");
				if (indexName != null && columnName != null) {
					ukMap.computeIfAbsent(indexName, k -> new ArrayList<>()).add(columnName);
				}
			}
			ukRs.close();
			ukMap.forEach((name, cols) -> {
				SourceObjectMetadataVO.UniqueKey uk = new SourceObjectMetadataVO.UniqueKey();
				uk.setName(name);
				uk.setColumns(cols);
				uniqueKeys.add(uk);
			});

			// 外键
			List<SourceObjectMetadataVO.ForeignKey> foreignKeys = new ArrayList<>();
			ResultSet fkRs = metaData.getImportedKeys(ref.schemaName(), null, ref.objectName());
			while (fkRs.next()) {
				SourceObjectMetadataVO.ForeignKey fk = new SourceObjectMetadataVO.ForeignKey();
				fk.setName(fkRs.getString("FK_NAME"));
				fk.setTargetSchema(fkRs.getString("PKTABLE_SCHEM"));
				fk.setTargetObject(fkRs.getString("PKTABLE_NAME"));
				// 外键可能多列，按位置排序后收集
				fk.setColumns(new ArrayList<>());
				fk.setTargetColumns(new ArrayList<>());
				foreignKeys.add(fk);
			}
			fkRs.close();

			// 重新查询外键列（按 KEY_SEQ 排序）
			Map<String, SourceObjectMetadataVO.ForeignKey> fkByName = foreignKeys.stream()
				.collect(Collectors.toMap(SourceObjectMetadataVO.ForeignKey::getName, f -> f, (a, b) -> a));
			if (!fkByName.isEmpty()) {
				ResultSet fkColRs = metaData.getImportedKeys(ref.schemaName(), null, ref.objectName());
				while (fkColRs.next()) {
					String fkName = fkColRs.getString("FK_NAME");
					String fkCol = fkColRs.getString("FKCOLUMN_NAME");
					String pkCol = fkColRs.getString("PKCOLUMN_NAME");
					SourceObjectMetadataVO.ForeignKey fk = fkByName.get(fkName);
					if (fk != null) {
						fk.getColumns().add(fkCol);
						fk.getTargetColumns().add(pkCol);
					}
				}
				fkColRs.close();
			}

			// 组装 VO
			SourceObjectMetadataVO vo = new SourceObjectMetadataVO();
			vo.setSchemaName(ref.schemaName());
			vo.setObjectName(ref.objectName());
			// 对象类型需要额外查询
			ResultSet tableRs = metaData.getTables(ref.schemaName(), null, ref.objectName(), null);
			if (tableRs.next()) {
				vo.setObjectType(normalizeTableType(tableRs.getString("TABLE_TYPE")));
			}
			tableRs.close();

			vo.setColumns(columns);
			vo.setPrimaryKey(primaryKey);
			vo.setUniqueKeys(uniqueKeys);
			vo.setForeignKeys(foreignKeys);
			return vo;

		}
		catch (SQLException e) {
			log.error("Failed to describe object: {}", sanitizeExceptionMessage(e));
			throw new RuntimeException("Failed to describe object", e);
		}
	}

	@Override
	public SourcePage scan(SourceScanPlan plan, SourceCursor cursor) {
		throw new UnsupportedOperationException("scan is implemented in module 18-07");
	}

	@Override
	public Optional<SourceRow> findByKey(SourceLookupPlan plan, SourceRecordKey key) {
		throw new UnsupportedOperationException("findByKey is implemented in module 18-07");
	}

	@Override
	public void close() {
		// PostgreSqlDataSourceConnector 本身不持有连接资源
	}

	// ==================== 内部方法 ====================

	/**
	 * 打开只读连接。
	 */
	private Connection openConnection(String jdbcUrl, String username, String password, int timeoutSeconds)
			throws SQLException {
		PostgreSqlDialect.validateJdbcUrl(jdbcUrl);
		java.util.Properties props = new java.util.Properties();
		props.setProperty("user", username);
		props.setProperty("password", password);
		// 只读
		props.setProperty("readOnly", "true");
		// 连接超时
		props.setProperty("loginTimeout", String.valueOf(Math.max(1, timeoutSeconds)));
		// socket 超时
		props.setProperty("socketTimeout", String.valueOf(timeoutSeconds * 1000));
		// 查询超时
		props.setProperty("statementTimeout", String.valueOf(timeoutSeconds * 1000));

		Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, props);
		conn.setReadOnly(true);
		return conn;
	}

	/**
	 * 将 SQLException 分类为错误码。
	 */
	private String classifySQLException(SQLException e) {
		String sqlState = e.getSQLState();
		// PostgreSQL SQLSTATE: 08001 = connection_fail, 08004 = rejected_connection,
		// 08006 = connection_failure, 28000 = invalid_authorization
		if (sqlState != null) {
			if (sqlState.startsWith("08")) {
				return DataSourceErrorCode.ONT_DS_004.getCode(); // 连接超时/失败
			}
			if ("28000".equals(sqlState) || "28P01".equals(sqlState)) {
				return DataSourceErrorCode.ONT_DS_005.getCode(); // 认证失败
			}
		}
		return DataSourceErrorCode.ONT_DS_004.getCode();
	}

	/**
	 * 清理异常消息，移除敏感信息（密码、完整 URL 参数等）。
	 */
	private String sanitizeExceptionMessage(Exception e) {
		String message = e.getMessage();
		if (message == null) {
			return "Unknown error";
		}
		// 移除 password=xxx 参数
		message = message.replaceAll("(?i)(password|pwd)=[^&\\s]+", "$1=***");
		// 移除 user=xxx 参数
		message = message.replaceAll("(?i)(user|username)=[^&\\s]+", "$1=***");
		// 截断过长的消息
		if (message.length() > 200) {
			message = message.substring(0, 200) + "...";
		}
		return message;
	}

	/**
	 * 规范化 JDBC TABLE_TYPE 到 TABLE/VIEW。
	 */
	private String normalizeTableType(String tableType) {
		if (tableType == null) {
			return "TABLE";
		}
		if (tableType.toUpperCase().startsWith("VIEW")) {
			return "VIEW";
		}
		return "TABLE";
	}

}
