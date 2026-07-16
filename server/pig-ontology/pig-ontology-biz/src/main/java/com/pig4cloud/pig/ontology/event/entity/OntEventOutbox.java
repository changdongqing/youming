/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.pig4cloud.pig.common.data.handler.StringToJsonbTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 事件 Outbox 表，DB 事务内写入的待投递领域事件。
 *
 * @author youming
 */
@Data
@TableName("ont_event_outbox")
@Schema(description = "事件 Outbox")
@EqualsAndHashCode(callSuper = true)
public class OntEventOutbox extends Model<OntEventOutbox> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键ID")
	private Long id;

	@Schema(description = "领域事件唯一ID（UUID），重复投递保持不变")
	private String eventId;

	@Schema(description = "事件类型，如 ONTOLOGY_INSTANCE_CHANGED")
	private String eventType;

	@Schema(description = "事件契约版本号，默认1")
	private Integer eventVersion;

	@Schema(description = "所属本体工程ID")
	private Long ontologyId;

	@Schema(description = "聚合类型，如 ENTITY_INSTANCE")
	private String aggregateType;

	@Schema(description = "聚合ID，字符串兼容雪花ID/IRI")
	private String aggregateId;

	@Schema(description = "操作类型，如 CREATED/UPDATED/DELETED")
	private String operation;

	@Schema(description = "事件发生时间（UTC Instant）")
	private LocalDateTime occurredAt;

	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	@Schema(description = "事件负载JSONB，仅放消费所需最小信息")
	private String payload;

	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	@Schema(description = "事件元数据JSONB，如 targetConsumerGroup")
	private String metadata;

	@Schema(description = "链路追踪ID")
	private String traceId;

	@Schema(description = "触发操作的用户ID")
	private Long actorId;

	@Schema(description = "投递状态：PENDING/PROCESSING/PUBLISHED/FAILED")
	private String status;

	@Schema(description = "可投递时间，用于延迟投递")
	private LocalDateTime availableAt;

	@Schema(description = "领取租约到期时间")
	private LocalDateTime leaseUntil;

	@Schema(description = "当前领取实例标识")
	private String lockedBy;

	@Schema(description = "投递尝试次数")
	private Integer deliveryAttempt;

	@Schema(description = "Redis Stream 返回的记录ID")
	private String streamRecordId;

	@Schema(description = "确认 XADD 成功的时间")
	private LocalDateTime publishedAt;

	@Schema(description = "最近一次投递错误码")
	private String lastErrorCode;

	@Schema(description = "最近一次投递错误信息")
	private String lastErrorMessage;

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
