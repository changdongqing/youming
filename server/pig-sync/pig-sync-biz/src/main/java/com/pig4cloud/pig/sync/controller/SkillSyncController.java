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
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.sync.api.vo.SkillManifestVO;
import com.pig4cloud.pig.sync.api.vo.SkillUploadResultVO;
import com.pig4cloud.pig.sync.service.SkillSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 企业技能同步（QwenPaw 阶段4，P3）
 *
 * @author youming
 * @date 2026-07-31
 */
@RestController
@AllArgsConstructor
@Tag(name = "企业技能同步", description = "QwenPaw 技能 manifest/下载/上传")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class SkillSyncController {

	private final SkillSyncService skillSyncService;

	@GetMapping("/sync/skills/manifest")
	@Operation(summary = "技能 manifest（含 version + skills 列表，供客户端增量比对）")
	public R<SkillManifestVO> manifest() {
		return R.ok(skillSyncService.manifest());
	}

	@GetMapping("/sync/skills/{name}/download")
	@Operation(summary = "技能包下载（流式返回 zip）")
	public void download(@PathVariable String name, HttpServletResponse response) {
		skillSyncService.download(name, response);
	}

	@PostMapping("/sync/skills/upload")
	@Operation(summary = "个人技能上传（云备份，multipart zip）")
	public R<SkillUploadResultVO> upload(@RequestPart("file") MultipartFile file) {
		String userId = String.valueOf(SecurityUtils.getUser().getId());
		return R.ok(skillSyncService.uploadPersonal(userId, file));
	}

}
