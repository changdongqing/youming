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

package com.pig4cloud.pig.sync.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记忆条目 DTO（上行/下行统一格式）
 *
 * @author youming
 * @date 2026-07-31
 */
@Data
@Schema(description = "记忆条目")
public class MemoryEntryDTO {

	@Schema(description = "客户端本地 seq（上行时携带；下行返回服务端 seq）")
	private Long seq;

	@Schema(description = "会话ID")
	private String sessionId;

	@Schema(description = "Agent ID")
	private String agentId;

	@Schema(description = "记录类型 model_turn | context_msg | tool_result")
	private String kind;

	@Schema(description = "角色")
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

	@Schema(description = "记录创建时间")
	private LocalDateTime createdAt;

	@Schema(description = "去重键")
	private String dedupKey;

}
