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
 * 事件回放日志，记录每次受控回放的来源和目标。
 *
 * @author youming
 */
@Data
@TableName("ont_event_replay_log")
@Schema(description = "事件回放日志")
@EqualsAndHashCode(callSuper = true)
public class OntEventReplayLog extends Model<OntEventReplayLog> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键ID")
	private Long id;

	@Schema(description = "被回放的领域事件ID")
	private String eventId;

	@Schema(description = "源回放序号")
	private Integer sourceReplayNo;

	@Schema(description = "目标回放序号（新分配）")
	private Integer targetReplayNo;

	@Schema(description = "目标消费者组")
	private String targetConsumerGroup;

	@Schema(description = "回放原因")
	private String reason;

	@Schema(description = "模块36审批号（批量或高风险回放）")
	private String approvalRequestNo;

	@Schema(description = "执行回放的用户ID")
	private Long replayedBy;

	@Schema(description = "回放执行时间")
	private LocalDateTime replayedAt;

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
