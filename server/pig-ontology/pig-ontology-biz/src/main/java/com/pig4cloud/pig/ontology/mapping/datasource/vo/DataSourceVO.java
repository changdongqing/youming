/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.vo;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据源脱敏返回 VO。
 * <p>
 * 不返回密文、IV，仅返回凭证配置状态和非敏感 keyId + 脱敏用户名。
 *
 * @author youming
 */
@Data
@Schema(description = "数据源详情（脱敏）")
public class DataSourceVO {

	@Schema(description = "数据源ID")
	private Long id;

	@Schema(description = "数据源编码")
	private String sourceCode;

	@Schema(description = "数据源名称")
	private String sourceName;

	@Schema(description = "源类型")
	private String sourceType;

	@Schema(description = "数据库类型")
	private String databaseType;

	@Schema(description = "连接模式")
	private String connectionMode;

	@JsonRawValue
	@Schema(description = "连接配置JSON")
	private String connectionConfig;

	@Schema(description = "允许的Schema白名单")
	private List<String> allowedSchemas;

	@Schema(description = "允许的对象白名单")
	private List<String> allowedObjects;

	@Schema(description = "状态")
	private String status;

	@Schema(description = "配置版本号")
	private Long revision;

	@Schema(description = "最近测试状态")
	private String lastTestStatus;

	@Schema(description = "最近测试时间")
	private LocalDateTime lastTestAt;

	@Schema(description = "最近测试延迟(ms)")
	private Long lastTestLatencyMs;

	@Schema(description = "最近测试错误码")
	private String lastTestErrorCode;

	@Schema(description = "元数据刷新时间")
	private LocalDateTime metadataRefreshedAt;

	@Schema(description = "安全级别编码")
	private String securityLevelCode;

	@Schema(description = "备注")
	private String remarks;

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

	// === 凭证脱敏信息 ===

	@Schema(description = "是否已配置凭证")
	@JsonProperty("credentialConfigured")
	private Boolean credentialConfigured;

	@Schema(description = "密钥版本ID（非敏感）")
	@JsonProperty("credentialKeyId")
	private String credentialKeyId;

	@Schema(description = "脱敏用户名")
	@JsonProperty("usernameMasked")
	private String usernameMasked;

}
