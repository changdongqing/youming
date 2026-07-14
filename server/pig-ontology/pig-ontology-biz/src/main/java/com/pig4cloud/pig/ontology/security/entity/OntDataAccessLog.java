/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据访问审计日志（不做逻辑删除）。
 *
 * @author youming
 */
@Data
@TableName("ont_data_access_log")
@Schema(description = "数据访问审计日志")
@EqualsAndHashCode(callSuper = true)
public class OntDataAccessLog extends Model<OntDataAccessLog> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "审计日志ID")
	private Long id;

	@Schema(description = "审计事件唯一ID")
	private String auditEventId;

	@Schema(description = "哈希链作用域，格式yyyyMMdd:ontologyId")
	private String chainScope;

	@Schema(description = "链内序号")
	private Long chainSeq;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "用户ID")
	private Long userId;

	@Schema(description = "用户名")
	private String username;

	@Schema(description = "访问类型")
	private String accessType;

	@Schema(description = "资源类型")
	private String resourceType;

	@Schema(description = "资源引用")
	private String resourceRef;

	@Schema(description = "操作摘要")
	private String actionSummary;

	@Schema(description = "结果数量")
	private Long resultCount;

	@Schema(description = "最高安全级别")
	private String maxSecurityLevel;

	@Schema(description = "策略决策：ALLOW/MASK/DENY")
	private String decision;

	@Schema(description = "执行结果：SUCCESS/FAILED")
	private String outcome;

	@Schema(description = "错误码")
	private String errorCode;

	@Schema(description = "追踪ID")
	private String traceId;

	@Schema(description = "远程地址")
	private String remoteAddr;

	@Schema(description = "用户代理哈希")
	private String userAgentHash;

	@Schema(description = "发生时间")
	private LocalDateTime occurredAt;

	@Schema(description = "前一条记录哈希")
	private String prevHash;

	@Schema(description = "当前记录哈希")
	private String currentHash;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

}
