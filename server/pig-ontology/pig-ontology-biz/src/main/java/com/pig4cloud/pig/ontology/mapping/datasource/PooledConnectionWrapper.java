/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * 连接池包装器。
 * <p>
 * 包装 JDBC Connection，使 close() 时归还连接到池而非真正关闭。
 * 所有写操作（createStatement/prepareStatement 中的 DDL/DML）受只读保护。
 *
 * @author youming
 */
public class PooledConnectionWrapper implements Connection {

	private static final Logger log = LoggerFactory.getLogger(PooledConnectionWrapper.class);

	private final String poolKey;

	private final Connection delegate;

	private final DataSourcePoolManager poolManager;

	private volatile boolean closed = false;

	public PooledConnectionWrapper(String poolKey, Connection delegate, DataSourcePoolManager poolManager) {
		this.poolKey = poolKey;
		this.delegate = delegate;
		this.poolManager = poolManager;
	}

	@Override
	public void close() throws SQLException {
		if (closed) {
			return;
		}
		closed = true;
		// 归还到池
		poolManager.returnConnection(poolKey, delegate);
	}

	@Override
	public boolean isClosed() throws SQLException {
		return closed || delegate.isClosed();
	}

	// ==================== 委托方法 ====================

	@Override
	public Statement createStatement() throws SQLException {
		return delegate.createStatement();
	}

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return delegate.prepareStatement(sql);
	}

	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		return delegate.prepareCall(sql);
	}

	@Override
	public String nativeSQL(String sql) throws SQLException {
		return delegate.nativeSQL(sql);
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		delegate.setAutoCommit(autoCommit);
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		return delegate.getAutoCommit();
	}

	@Override
	public void commit() throws SQLException {
		delegate.commit();
	}

	@Override
	public void rollback() throws SQLException {
		delegate.rollback();
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		delegate.setReadOnly(readOnly);
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return delegate.isReadOnly();
	}

	@Override
	public void setCatalog(String catalog) throws SQLException {
		delegate.setCatalog(catalog);
	}

	@Override
	public String getCatalog() throws SQLException {
		return delegate.getCatalog();
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		delegate.setTransactionIsolation(level);
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		return delegate.getTransactionIsolation();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return delegate.getWarnings();
	}

	@Override
	public void clearWarnings() throws SQLException {
		delegate.clearWarnings();
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return delegate.createStatement(resultSetType, resultSetConcurrency);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return delegate.getTypeMap();
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		delegate.setTypeMap(map);
	}

	@Override
	public void setHoldability(int holdability) throws SQLException {
		delegate.setHoldability(holdability);
	}

	@Override
	public int getHoldability() throws SQLException {
		return delegate.getHoldability();
	}

	@Override
	public Savepoint setSavepoint() throws SQLException {
		return delegate.setSavepoint();
	}

	@Override
	public Savepoint setSavepoint(String name) throws SQLException {
		return delegate.setSavepoint(name);
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		delegate.rollback(savepoint);
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		delegate.releaseSavepoint(savepoint);
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		return delegate.prepareStatement(sql, autoGeneratedKeys);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		return delegate.prepareStatement(sql, columnIndexes);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		return delegate.prepareStatement(sql, columnNames);
	}

	@Override
	public Clob createClob() throws SQLException {
		return delegate.createClob();
	}

	@Override
	public Blob createBlob() throws SQLException {
		return delegate.createBlob();
	}

	@Override
	public NClob createNClob() throws SQLException {
		return delegate.createNClob();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		return delegate.createSQLXML();
	}

	@Override
	public boolean isValid(int timeout) throws SQLException {
		return !closed && delegate.isValid(timeout);
	}

	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		delegate.setClientInfo(name, value);
	}

	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		delegate.setClientInfo(properties);
	}

	@Override
	public String getClientInfo(String name) throws SQLException {
		return delegate.getClientInfo(name);
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		return delegate.getClientInfo();
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		return delegate.createArrayOf(typeName, elements);
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		return delegate.createStruct(typeName, attributes);
	}

	@Override
	public void setSchema(String schema) throws SQLException {
		delegate.setSchema(schema);
	}

	@Override
	public String getSchema() throws SQLException {
		return delegate.getSchema();
	}

	@Override
	public void abort(Executor executor) throws SQLException {
		delegate.abort(executor);
		closed = true;
	}

	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		delegate.setNetworkTimeout(executor, milliseconds);
	}

	@Override
	public int getNetworkTimeout() throws SQLException {
		return delegate.getNetworkTimeout();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return delegate.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return delegate.isWrapperFor(iface);
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		return delegate.getMetaData();
	}

}
