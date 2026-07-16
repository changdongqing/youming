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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import com.pig4cloud.pig.common.data.handler.StringToJsonbTypeHandler;

/**
 * 事件死信表，达到最大重试次数或不可重试错误的死信记录。
 *
 * @author youming
 */
@Data
@TableName("ont_event_dead_letter")
@Schema(description = "事件死信")
@EqualsAndHashCode(callSuper = true)
public class OntEventDeadLetter extends Model<OntEventDeadLetter> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键ID")
	private Long id;

	@Schema(description = "消费者组名称")
	private String consumerGroup;

	@Schema(description = "领域事件唯一ID")
	private String eventId;

	@Schema(description = "回放序号")
	private Integer replayNo;

	@Schema(description = "事件类型")
	private String eventType;

	@Schema(description = "Redis Stream 记录ID")
	private String streamRecordId;

	@Schema(description = "事件负载JSONB")
	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	private String payload;

	@Schema(description = "失败分类：RETRYABLE/NON_RETRYABLE")
	private String failureCategory;

	@Schema(description = "错误码")
	private String errorCode;

	@Schema(description = "错误信息")
	private String errorMessage;

	@Schema(description = "投递尝试总次数")
	private Integer deliveryCount;

	@Schema(description = "死信状态：OPEN/REPLAYED/RESOLVED/IGNORED")
	private String status;

	@Schema(description = "回放后分配的新 replayNo")
	private Integer replayedAsNo;

	@Schema(description = "首次失败时间")
	private LocalDateTime firstFailedAt;

	@Schema(description = "最近失败时间")
	private LocalDateTime lastFailedAt;

	@Schema(description = "人工处理完成时间")
	private LocalDateTime resolvedAt;

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
