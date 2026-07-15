/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据源注册表。
 * <p>
 * 记录外部数据源的连接配置、加密凭证、白名单和安全级别，为映射设计器和同步作业提供统一的数据源注册。
 *
 * @author youming
 */
@Data
@TableName("ont_data_source")
@Schema(description = "数据源注册")
@EqualsAndHashCode(callSuper = true)
public class OntDataSource extends Model<OntDataSource> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "数据源ID")
	private Long id;

	@Schema(description = "数据源编码")
	private String sourceCode;

	@Schema(description = "数据源名称")
	private String sourceName;

	@Schema(description = "源类型: JDBC")
	private String sourceType;

	@Schema(description = "数据库类型: POSTGRESQL")
	private String databaseType;

	@Schema(description = "连接模式: HOST / JDBC_URL")
	private String connectionMode;

	@Schema(description = "连接配置JSON（不含凭证）")
	private String connectionConfig;

	@Schema(description = "凭证密文")
	private byte[] credentialCiphertext;

	@Schema(description = "加密IV")
	private byte[] credentialIv;

	@Schema(description = "密钥版本ID")
	private String credentialKeyId;

	@Schema(description = "允许的Schema白名单JSON")
	private String allowedSchemas;

	@Schema(description = "允许的对象白名单JSON")
	private String allowedObjects;

	@Schema(description = "状态: DRAFT / ACTIVE / DISABLED")
	private String status;

	@Schema(description = "配置版本号")
	private Long revision;

	@Schema(description = "最近测试状态: SUCCESS / FAILED")
	private String lastTestStatus;

	@Schema(description = "最近测试时的版本号")
	private Long lastTestRevision;

	@Schema(description = "最近测试时间")
	private LocalDateTime lastTestAt;

	@Schema(description = "最近测试延迟(ms)")
	private Long lastTestLatencyMs;

	@Schema(description = "最近测试错误码")
	private String lastTestErrorCode;

	@Schema(description = "元数据刷新时间")
	private LocalDateTime metadataRefreshedAt;

	@Schema(description = "凭证最近轮换时间")
	private LocalDateTime credentialRotatedAt;

	@Schema(description = "安全级别编码")
	private String securityLevelCode;

	@Schema(description = "备注")
	private String remarks;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改人")
	private String updateBy;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;

}
