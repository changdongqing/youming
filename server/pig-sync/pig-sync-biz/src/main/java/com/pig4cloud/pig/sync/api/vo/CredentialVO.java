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

package com.pig4cloud.pig.sync.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 凭据下行 VO（secrets 已解密）
 *
 * @author youming
 * @date 2026-07-31
 */
@Data
@Schema(description = "企业凭据下行")
public class CredentialVO {

	@Schema(description = "凭据引用名")
	private String ref;

	@Schema(description = "凭据类型")
	private String kind;

	@Schema(description = "公开信息")
	private Map<String, Object> publicInfo;

	@Schema(description = "secrets（已解密，传输走 TLS）")
	private Map<String, Object> secrets;

	@Schema(description = "元数据")
	private Map<String, Object> meta;

}
