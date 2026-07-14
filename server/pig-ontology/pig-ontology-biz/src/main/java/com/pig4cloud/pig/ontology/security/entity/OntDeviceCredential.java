/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.entity;

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
 * 设备凭证元数据。
 *
 * @author youming
 */
@Data
@TableName("ont_device_credential")
@Schema(description = "设备凭证元数据")
@EqualsAndHashCode(callSuper = true)
public class OntDeviceCredential extends Model<OntDeviceCredential> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "凭证ID")
	private Long id;

	@Schema(description = "设备编码")
	private String deviceCode;

	@Schema(description = "认证类型：TOKEN_HASH/MTLS")
	private String authType;

	@Schema(description = "Token的HMAC-SHA-256摘要，不存可逆Token")
	private String tokenDigest;

	@Schema(description = "摘要密钥版本")
	private String digestKeyVersion;

	@Schema(description = "证书指纹（mTLS）")
	private String certificateFingerprint;

	@Schema(description = "证书主题（mTLS）")
	private String certificateSubject;

	@Schema(description = "状态：ACTIVE/LOCKED/REVOKED/EXPIRED")
	private String status;

	@Schema(description = "过期时间")
	private LocalDateTime expiresAt;

	@Schema(description = "最后认证时间")
	private LocalDateTime lastAuthenticatedAt;

	@Schema(description = "最后认证IP")
	private String lastAuthenticatedIp;

	@Schema(description = "失败计数")
	private Integer failedCount;

	@Schema(description = "锁定截止时间")
	private LocalDateTime lockedUntil;

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
