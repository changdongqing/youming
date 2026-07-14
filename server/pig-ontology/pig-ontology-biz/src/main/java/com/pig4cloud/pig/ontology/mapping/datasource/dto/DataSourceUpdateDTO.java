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
 * 数据源更新 DTO。
 * <p>
 * 修改连接配置或凭证时递增 revision 并回退状态为 DRAFT。
 * 凭证字段（username/password）可选，为空时不变更。
 *
 * @author youming
 */
@Data
@Schema(description = "数据源更新")
public class DataSourceUpdateDTO {

	@NotNull(message = "数据源ID不能为空")
	@Schema(description = "数据源ID")
	private Long id;

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

	@Size(max = 128, message = "用户名长度不能超过128")
	@Schema(description = "用户名，为空时不变更凭证")
	private String username;

	@Size(max = 256, message = "密码长度不能超过256")
	@Schema(description = "密码，为空时不变更凭证")
	private String password;

	@Schema(description = "允许的Schema白名单")
	private List<String> allowedSchemas;

	@Schema(description = "允许的对象白名单")
	private List<String> allowedObjects;

	@Size(max = 32, message = "安全级别编码长度不能超过32")
	@Schema(description = "安全级别编码")
	private String securityLevelCode;

	@Size(max = 255, message = "备注长度不能超过255")
	@Schema(description = "备注")
	private String remarks;

}
