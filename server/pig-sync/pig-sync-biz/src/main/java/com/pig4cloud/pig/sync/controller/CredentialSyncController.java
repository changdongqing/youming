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

package com.pig4cloud.pig.sync.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.sync.api.vo.CredentialListVO;
import com.pig4cloud.pig.sync.service.CredentialSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业凭据同步（QwenPaw 阶段3，P2）
 *
 * @author youming
 * @date 2026-07-31
 */
@RestController
@AllArgsConstructor
@Tag(name = "企业凭据同步", description = "QwenPaw 企业凭据拉取（secrets 加密存储/返回解密）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class CredentialSyncController {

	private final CredentialSyncService credentialSyncService;

	@GetMapping("/sync/credentials")
	@Operation(summary = "企业凭据列表（含全员可用 + 用户专属，secrets 解密返回）")
	public R<CredentialListVO> list() {
		String userId = String.valueOf(SecurityUtils.getUser().getId());
		return R.ok(credentialSyncService.listCredentials(userId));
	}

}
