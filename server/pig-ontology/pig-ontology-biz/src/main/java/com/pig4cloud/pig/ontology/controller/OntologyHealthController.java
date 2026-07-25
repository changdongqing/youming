/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.ontology.controller;

import com.pig4cloud.pig.common.core.util.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 本体治理模块健康检查（M0 验证用）。
 * <p>
 * 后续里程碑的业务 Controller 随各 DD 实现。
 *
 * @author pig
 * @date 2026-07-25
 */
@RestController
@RequestMapping("/ont")
@Tag(name = "本体治理-健康检查", description = "模块可用性验证")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntologyHealthController {

	@GetMapping("/health")
	@Operation(summary = "健康检查", description = "验证 pig-ontology 模块在当前形态下可用")
	public R<String> health() {
		return R.ok("pig-ontology is up");
	}

}
