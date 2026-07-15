/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.connector;

import com.pig4cloud.pig.ontology.mapping.datasource.vo.SourceObjectMetadataVO;

import java.util.List;

/**
 * 数据源连接器 SPI。
 * <p>
 * 为映射设计器和同步作业提供统一的只读数据访问接口。
 * 实现必须是线程安全的，且不得暴露任何写入方法。
 *
 * @author youming
 */
public interface DataSourceConnector extends AutoCloseable {

	/**
	 * 连接器类型标识（如 "JDBC"）。
	 */
	String type();

	/**
	 * 测试连接。
	 *
	 * @param request 连接请求
	 * @return 测试结果
	 */
	ConnectionTestResult test(ConnectionRequest request);

	/**
	 * 列出所有 Schema（已通过白名单过滤）。
	 *
	 * @param request 元数据请求
	 * @return Schema列表
	 */
	List<String> listSchemas(MetadataRequest request);

	/**
	 * 列出指定 Schema 下的表和视图（已通过白名单过滤）。
	 *
	 * @param query 对象查询参数
	 * @return 对象列表
	 */
	List<SourceObject> listObjects(ObjectQuery query);

	/**
	 * 描述指定对象的完整元数据（列、主键、唯一键、外键）。
	 *
	 * @param ref 对象引用
	 * @return 对象元数据
	 */
	SourceObjectMetadataVO describeObject(ObjectRef ref);

	/**
	 * 扫描数据（分页）。
	 * <p>
	 * V1 仅留接口签名，实际实现在 18-07 同步作业模块。
	 *
	 * @param plan   扫描计划
	 * @param cursor 游标
	 * @return 分页数据
	 */
	SourcePage scan(SourceScanPlan plan, SourceCursor cursor);

	/**
	 * 扫描数据（分页，使用外部传入连接）。
	 * <p>
	 * 由 18-07 同步作业模块通过 DataSourcePoolManager 获取只读连接后调用。
	 *
	 * @param connection 数据库连接（只读）
	 * @param plan   扫描计划
	 * @param cursor 游标
	 * @return 分页数据
	 */
	default SourcePage scanWithConnection(java.sql.Connection connection, SourceScanPlan plan, SourceCursor cursor) {
		throw new UnsupportedOperationException("scanWithConnection not implemented");
	}

	/**
	 * 按主键查找单行。
	 * <p>
	 * V1 仅留接口签名，实际实现在 18-07 同步作业模块。
	 *
	 * @param plan 查找计划
	 * @param key  主键
	 * @return 源行
	 */
	java.util.Optional<SourceRow> findByKey(SourceLookupPlan plan, SourceRecordKey key);

	/**
	 * 按主键查找单行（使用外部传入连接）。
	 * <p>
	 * 由 18-07 同步作业模块通过 DataSourcePoolManager 获取只读连接后调用。
	 *
	 * @param connection 数据库连接（只读）
	 * @param plan 查找计划
	 * @param key  主键
	 * @return 源行
	 */
	default java.util.Optional<SourceRow> findByKeyWithConnection(java.sql.Connection connection,
			SourceLookupPlan plan, SourceRecordKey key) {
		throw new UnsupportedOperationException("findByKeyWithConnection not implemented");
	}

	// ==================== 值对象 ====================

	/**
	 * 连接请求。
	 *
	 * @param jdbcUrl       JDBC URL
	 * @param username      用户名
	 * @param password      密码
	 * @param timeoutSeconds 超时秒数
	 */
	record ConnectionRequest(String jdbcUrl, String username, String password, int timeoutSeconds) {
	}

	/**
	 * 连接测试结果。
	 *
	 * @param success   是否成功
	 * @param latencyMs 延迟毫秒
	 * @param errorCode 错误码（失败时）
	 * @param errorMessage 错误消息（失败时，不含敏感信息）
	 */
	record ConnectionTestResult(boolean success, long latencyMs, String errorCode, String errorMessage) {

		public static ConnectionTestResult ok(long latencyMs) {
			return new ConnectionTestResult(true, latencyMs, null, null);
		}

		public static ConnectionTestResult fail(String errorCode, String errorMessage) {
			return new ConnectionTestResult(false, 0, errorCode, errorMessage);
		}

	}

	/**
	 * 元数据请求。
	 *
	 * @param connection  数据库连接
	 * @param allowedSchemas 允许的Schema白名单
	 */
	record MetadataRequest(java.sql.Connection connection, List<String> allowedSchemas) {
	}

	/**
	 * 对象查询参数。
	 *
	 * @param connection     数据库连接
	 * @param schemaName     Schema名
	 * @param allowedSchemas 允许的Schema白名单
	 * @param allowedObjects 允许的对象白名单
	 * @param objectType     对象类型过滤（可选）
	 * @param objectNamePrefix 对象名前缀过滤（可选）
	 */
	record ObjectQuery(java.sql.Connection connection, String schemaName, List<String> allowedSchemas,
			List<String> allowedObjects, String objectType, String objectNamePrefix) {
	}

	/**
	 * 对象引用。
	 *
	 * @param connection 数据库连接
	 * @param schemaName Schema名
	 * @param objectName 对象名
	 */
	record ObjectRef(java.sql.Connection connection, String schemaName, String objectName) {
	}

	/**
	 * 源对象（表或视图）。
	 *
	 * @param schemaName Schema名
	 * @param objectName 对象名
	 * @param objectType 对象类型: TABLE / VIEW
	 */
	record SourceObject(String schemaName, String objectName, String objectType) {
	}

	/**
	 * 扫描计划（18-07实现）。
	 *
	 * @param schemaName  Schema名
	 * @param objectName  对象名
	 * @param columns     列名列表（SELECT 投影）
	 * @param pageSize    页大小
	 * @param queryTimeoutSeconds 查询超时
	 * @param keyColumns  主键列名列表（用于 keyset 分页排序）
	 * @param incrementalColumn 增量列名（可为 null，全量扫描时）
	 * @param filterClause 过滤 WHERE 子句（可为 null）
	 */
	record SourceScanPlan(String schemaName, String objectName, List<String> columns,
			int pageSize, int queryTimeoutSeconds,
			List<String> keyColumns, String incrementalColumn, String filterClause) {

		/**
		 * 兼容旧签名的工厂方法（无 keyset 信息）。
		 */
		public static SourceScanPlan of(String schemaName, String objectName, List<String> columns,
				int pageSize, int queryTimeoutSeconds) {
			return new SourceScanPlan(schemaName, objectName, columns, pageSize, queryTimeoutSeconds,
					null, null, null);
		}

	}

	/**
	 * 游标（18-07实现）。
	 * <p>
	 * 全量扫描使用 lastKeyValues 做 keyset 分页；
	 * 增量扫描使用 incrementalValue + lastKeyValues 做 (updated_at, pk) 复合 keyset。
	 *
	 * @param lastKeyValues  最后一条记录的主键值（按 keyColumns 顺序）
	 * @param incrementalValue 增量列值（时间戳或数值字符串）
	 */
	record SourceCursor(List<Object> lastKeyValues, String incrementalValue) {

		/**
		 * 空游标（从头开始）。
		 */
		public static SourceCursor initial() {
			return new SourceCursor(null, null);
		}

		/**
		 * 是否有 keyset 定位点。
		 */
		public boolean hasKeyset() {
			return lastKeyValues != null && !lastKeyValues.isEmpty();
		}

	}

	/**
	 * 分页数据（18-07实现）。
	 *
	 * @param rows 数据行
	 * @param nextCursor 下一页游标
	 * @param hasMore 是否还有更多
	 */
	record SourcePage(List<SourceRow> rows, SourceCursor nextCursor, boolean hasMore) {
	}

	/**
	 * 源行（18-07实现）。
	 *
	 * @param values 列值映射
	 */
	record SourceRow(java.util.Map<String, Object> values) {
	}

	/**
	 * 查找计划（18-07实现）。
	 *
	 * @param schemaName Schema名
	 * @param objectName 对象名
	 * @param columns    列名列表
	 * @param keyColumns 主键列名列表
	 */
	record SourceLookupPlan(String schemaName, String objectName, List<String> columns,
			List<String> keyColumns) {
	}

	/**
	 * 源记录主键（18-07实现）。
	 *
	 * @param values 主键值映射
	 */
	record SourceRecordKey(java.util.Map<String, Object> values) {
	}

}
