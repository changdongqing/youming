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

package com.pig4cloud.pig.sync.api.vo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MCP 模板条目（capabilities 解析为 List）
 *
 * @author youming
 * @date 2026-07-31
 */
@Slf4j
@Data
@Schema(description = "MCP 模板条目")
public class McpTemplateItemVO {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Schema(description = "模板名")
	private String name;

	@Schema(description = "MCP 服务接入地址")
	private String endpoint;

	@Schema(description = "模板描述")
	private String description;

	@Schema(description = "模板版本")
	private String version;

	@Schema(description = "协议")
	private String protocol;

	@Schema(description = "传输方式")
	private String transport;

	@Schema(description = "引用的企业凭据 ref")
	private String credentialRef;

	@Schema(description = "默认策略")
	private String defaultPolicy;

	@Schema(description = "能力清单")
	private List<Map<String, Object>> capabilities;

	/**
	 * 从 JSON 文本解析 capabilities，失败返回空列表。
	 */
	public static List<Map<String, Object>> parseCapabilities(String json) {
		if (StrUtil.isBlank(json)) {
			return Collections.emptyList();
		}
		try {
			return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {
			});
		}
		catch (Exception ex) {
			log.warn("capabilities parse failed: {}", json, ex);
			return Collections.emptyList();
		}
	}

}
