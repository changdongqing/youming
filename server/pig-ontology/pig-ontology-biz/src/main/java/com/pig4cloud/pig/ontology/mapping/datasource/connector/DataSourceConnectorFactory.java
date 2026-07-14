/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.connector;

/**
 * 数据源连接器工厂 SPI。
 * <p>
 * 按 {@code sourceType/databaseType} 判断是否支持并创建连接器实例。
 *
 * @author youming
 */
public interface DataSourceConnectorFactory {

	/**
	 * 是否支持指定的源类型和数据库类型。
	 * @param sourceType   源类型（如 "JDBC"）
	 * @param databaseType 数据库类型（如 "POSTGRESQL"）
	 * @return 是否支持
	 */
	boolean supports(String sourceType, String databaseType);

	/**
	 * 创建连接器实例。
	 * @return 连接器
	 */
	DataSourceConnector create();

}
