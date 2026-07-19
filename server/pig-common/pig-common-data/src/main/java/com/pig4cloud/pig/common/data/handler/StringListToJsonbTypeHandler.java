/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.common.data.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * PostgreSQL jsonb 列的 List&lt;String&gt; 类型处理器。
 * <p>
 * Java 侧用 {@code List<String>} 持有能力契约列表，数据库侧为 jsonb 列。
 * PostgreSQL 不做 varchar→jsonb 隐式转换，需要通过 PGobject 显式指定类型。
 * 使用反射创建 PGobject，避免 pig-common-data 模块对 postgresql 驱动的编译期依赖。
 *
 * @author youming
 */
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class StringListToJsonbTypeHandler extends BaseTypeHandler<List<String>> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
	};

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
			throws SQLException {
		try {
			String json = OBJECT_MAPPER.writeValueAsString(parameter);
			Class<?> pgObjectClass = Class.forName("org.postgresql.util.PGobject");
			Object pgObject = pgObjectClass.getDeclaredConstructor().newInstance();
			pgObjectClass.getMethod("setType", String.class).invoke(pgObject, "jsonb");
			pgObjectClass.getMethod("setValue", String.class).invoke(pgObject, json);
			ps.setObject(i, pgObject);
		}
		catch (SQLException e) {
			throw e;
		}
		catch (Exception e) {
			// 非PostgreSQL环境降级为普通字符串
			try {
				ps.setString(i, OBJECT_MAPPER.writeValueAsString(parameter));
			}
			catch (JsonProcessingException ex) {
				ps.setString(i, "[]");
			}
		}
	}

	@Override
	public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return parseStringList(rs.getString(columnName));
	}

	@Override
	public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return parseStringList(rs.getString(columnIndex));
	}

	@Override
	public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return parseStringList(cs.getString(columnIndex));
	}

	private List<String> parseStringList(String json) {
		if (json == null || json.isBlank()) {
			return Collections.emptyList();
		}
		try {
			List<String> result = OBJECT_MAPPER.readValue(json, STRING_LIST_TYPE);
			return result != null ? result : Collections.emptyList();
		}
		catch (JsonProcessingException e) {
			return Collections.emptyList();
		}
	}

}
