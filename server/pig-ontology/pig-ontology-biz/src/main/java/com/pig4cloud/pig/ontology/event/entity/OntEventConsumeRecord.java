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

/**
 * 事件消费记录（Inbox），保证重复投递时的业务幂等。
 *
 * @author youming
 */
@Data
@TableName("ont_event_consume_record")
@Schema(description = "事件消费记录")
@EqualsAndHashCode(callSuper = true)
public class OntEventConsumeRecord extends Model<OntEventConsumeRecord> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键ID")
	private Long id;

	@Schema(description = "消费者组名称")
	private String consumerGroup;

	@Schema(description = "领域事件唯一ID")
	private String eventId;

	@Schema(description = "回放序号，0为原始投递，人工回放递增")
	private Integer replayNo;

	@Schema(description = "事件类型")
	private String eventType;

	@Schema(description = "消费状态：PROCESSING/SUCCEEDED/FAILED")
	private String status;

	@Schema(description = "消费处理租约到期时间")
	private LocalDateTime leaseUntil;

	@Schema(description = "当前处理消费者实例名称")
	private String consumerName;

	@Schema(description = "处理尝试次数")
	private Integer processAttempt;

	@Schema(description = "开始处理时间")
	private LocalDateTime startedAt;

	@Schema(description = "完成处理时间")
	private LocalDateTime completedAt;

	@Schema(description = "最近一次处理错误码")
	private String lastErrorCode;

	@Schema(description = "最近一次处理错误信息")
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
