/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.common.data.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreSQL jsonb 列的 String 类型处理器。
 * <p>
 * Java 侧用 String 持有 JSON 文本，数据库侧为 jsonb 列。
 * PostgreSQL 不做 varchar→jsonb 隐式转换，需要通过 PGobject 显式指定类型。
 * 使用反射创建 PGobject，避免 pig-common-data 模块对 postgresql 驱动的编译期依赖。
 *
 * @author youming
 */
@MappedTypes(value = { String.class })
@MappedJdbcTypes(value = JdbcType.OTHER)
public class StringToJsonbTypeHandler extends BaseTypeHandler<String> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
			throws SQLException {
		try {
			Class<?> pgObjectClass = Class.forName("org.postgresql.util.PGobject");
			Object pgObject = pgObjectClass.getDeclaredConstructor().newInstance();
			pgObjectClass.getMethod("setType", String.class).invoke(pgObject, "jsonb");
			pgObjectClass.getMethod("setValue", String.class).invoke(pgObject, parameter);
			ps.setObject(i, pgObject);
		}
		catch (SQLException e) {
			throw e;
		}
		catch (Exception e) {
			// 非PostgreSQL环境降级为普通字符串
			ps.setString(i, parameter);
		}
	}

	@Override
	public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return rs.getString(columnName);
	}

	@Override
	public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return rs.getString(columnIndex);
	}

	@Override
	public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return cs.getString(columnIndex);
	}

}
