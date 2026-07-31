/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary, with or without
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
 * QwenPaw 企业 MCP 模板（只读拉取，含接入地址/默认策略）
 *
 * @author youming
 * @date 2026-07-31
 */
@Data
@Schema(description = "QwenPaw企业MCP模板")
@EqualsAndHashCode(callSuper = true)
@TableName("qwenpaw_mcp_templates")
public class McpTemplate extends Model<McpTemplate> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "模板名（唯一）")
	private String name;

	@Schema(description = "MCP 服务接入地址")
	private String endpoint;

	@Schema(description = "模板描述")
	private String description;

	@Schema(description = "模板版本")
	private String version;

	@Schema(description = "协议 mcp")
	private String protocol;

	@Schema(description = "传输方式 sse | streamable")
	private String transport;

	@Schema(description = "引用的企业凭据 ref（-> qwenpaw_enterprise_credentials.ref）")
	private String credentialRef;

	@Schema(description = "默认策略 ask | allow | deny")
	private String defaultPolicy;

	@Schema(description = "能力清单（JSON 文本）")
	private String capabilities;

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
