/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Schema 预览请求 DTO（新建数据源向导第4步使用，不落库直连发现）。
 *
 * @author youming
 */
@Data
@Schema(description = "Schema预览请求")
public class SchemaPreviewRequest {

	@NotBlank(message = "连接模式不能为空")
	@Schema(description = "连接模式: HOST / JDBC_URL")
	private String connectionMode;

	@NotBlank(message = "连接配置不能为空")
	@Schema(description = "连接配置JSON，HOST模式{host,port,database,sslMode}，JDBC_URL模式{jdbcUrl}")
	private String connectionConfig;

	@NotBlank(message = "用户名不能为空")
	@Schema(description = "数据库用户名")
	private String username;

	@NotBlank(message = "密码不能为空")
	@Schema(description = "数据库密码")
	private String password;

	@NotNull(message = "源类型不能为空")
	@Schema(description = "数据源类型，如 POSTGRESQL")
	private String sourceType;

}
