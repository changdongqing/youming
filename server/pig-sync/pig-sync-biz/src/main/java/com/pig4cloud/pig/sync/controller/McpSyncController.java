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

package com.pig4cloud.pig.sync.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.sync.api.vo.McpTemplateListVO;
import com.pig4cloud.pig.sync.service.McpSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP 模板同步（QwenPaw 阶段5，P4）
 *
 * @author youming
 * @date 2026-07-31
 */
@RestController
@AllArgsConstructor
@Tag(name = "MCP模板同步", description = "QwenPaw 企业 MCP 模板拉取")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class McpSyncController {

	private final McpSyncService mcpSyncService;

	@GetMapping("/sync/mcp/templates")
	@Operation(summary = "企业 MCP 模板列表（capabilities 解析为 List）")
	public R<McpTemplateListVO> list() {
		return R.ok(mcpSyncService.listTemplates());
	}

}
