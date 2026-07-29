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

package com.pig4cloud.pig.rm.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.rm.api.entity.FormFieldConfig;
import com.pig4cloud.pig.rm.service.FlowConfigService;
import com.pig4cloud.pig.rm.service.FormConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 表单/流程配置
 *
 * @author youming
 * @date 2026-07-29
 */
@RestController
@AllArgsConstructor
@Tag(name = "表单/流程配置", description = "表单字段配置 + 流程节点配置")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class FormConfigController {

	private final FormConfigService formConfigService;

	private final FlowConfigService flowConfigService;

	@GetMapping("/rm/form-config/{formCode}")
	@Operation(summary = "查询表单字段配置")
	@HasPermission("rm_req_view")
	public R getByFormCode(@PathVariable String formCode) {
		return formConfigService.getByFormCode(formCode);
	}

	@SysLog("保存表单配置")
	@PutMapping("/rm/form-config")
	@Operation(summary = "保存表单字段配置")
	@HasPermission("rm_form_config")
	public R saveFormConfig(@RequestBody List<FormFieldConfig> configs) {
		return formConfigService.saveConfigs(configs);
	}

	@GetMapping("/rm/flow-config/{flowCode}")
	@Operation(summary = "查询流程节点配置")
	@HasPermission("rm_req_view")
	public R getByFlowCode(@PathVariable String flowCode) {
		return flowConfigService.getByFlowCode(flowCode);
	}

}
