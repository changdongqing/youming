/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 数据源创建 DTO。
 *
 * @author youming
 */
@Data
@Schema(description = "数据源创建")
public class DataSourceCreateDTO {

	@NotBlank(message = "数据源编码不能为空")
	@Size(max = 64, message = "数据源编码长度不能超过64")
	@Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "数据源编码须以字母开头，仅含字母、数字、下划线")
	@Schema(description = "数据源编码")
	private String sourceCode;

	@NotBlank(message = "数据源名称不能为空")
	@Size(max = 128, message = "数据源名称长度不能超过128")
	@Schema(description = "数据源名称")
	private String sourceName;

	@NotBlank(message = "连接模式不能为空")
	@Pattern(regexp = "^(HOST|JDBC_URL)$", message = "连接模式必须为HOST或JDBC_URL")
	@Schema(description = "连接模式: HOST / JDBC_URL")
	private String connectionMode;

	@NotBlank(message = "连接配置不能为空")
	@Schema(description = "连接配置JSON（不含凭证）")
	private String connectionConfig;

	@NotBlank(message = "用户名不能为空")
	@Size(max = 128, message = "用户名长度不能超过128")
	@Schema(description = "用户名")
	private String username;

	@NotBlank(message = "密码不能为空")
	@Size(max = 256, message = "密码长度不能超过256")
	@Schema(description = "密码")
	private String password;

	@Schema(description = "允许的Schema白名单")
	private List<String> allowedSchemas;

	@Schema(description = "允许的对象白名单")
	private List<String> allowedObjects;

	@Size(max = 32, message = "安全级别编码长度不能超过32")
	@Schema(description = "安全级别编码，默认RESTRICTED")
	private String securityLevelCode;

	@Size(max = 255, message = "备注长度不能超过255")
	@Schema(description = "备注")
	private String remarks;

}
