/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.sync.api.entity;

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
 * QwenPaw 会话记忆（增量同步母版，按 user_id 隔离）
 *
 * @author youming
 * @date 2026-07-31
 */
@Data
@Schema(description = "QwenPaw会话记忆")
@EqualsAndHashCode(callSuper = true)
@TableName("qwenpaw_conversation_history")
public class ConversationHistory extends Model<ConversationHistory> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "服务端全局自增（雪花ID），客户端增量游标")
	private Long seq;

	@Schema(description = "会话ID")
	private String sessionId;

	@Schema(description = "Agent ID")
	private String agentId;

	@Schema(description = "记录类型 model_turn | context_msg | tool_result")
	private String kind;

	@Schema(description = "角色 user | assistant | tool")
	private String role;

	@Schema(description = "名称")
	private String name;

	@Schema(description = "消息文本内容")
	private String content;

	@Schema(description = "工具调用ID")
	private String toolCallId;

	@Schema(description = "工具调用入参（JSON 文本）")
	private String toolInput;

	@Schema(description = "工具状态")
	private String toolState;

	@Schema(description = "标题摘要")
	private String headline;

	@Schema(description = "富文本块（JSON 文本）")
	private String blocks;

	@Schema(description = "扩展元数据（JSON 文本）")
	private String metadata;

	@Schema(description = "记录创建时间（客户端产生时间）")
	private LocalDateTime createdAt;

	@Schema(description = "去重键，与 session_id+user_id 组成唯一约束做幂等上行")
	private String dedupKey;

	@Schema(description = "所属用户ID（来自 pig SecurityUtils.getUser().getId()）")
	private String userId;

	@Schema(description = "客户端原始 seq（可选，便于对账）")
	private Long clientSeq;

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
	@Schema(description = "修改时间")
	private LocalDateTime updateTime;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记")
	private String delFlag;

}
