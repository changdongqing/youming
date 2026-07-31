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

package com.pig4cloud.pig.sync;

import com.pig4cloud.pig.common.security.annotation.EnablePigResourceServer;
import com.pig4cloud.pig.common.swagger.annotation.EnableOpenApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * QwenPaw 同步业务模块启动类（微服务形态）。
 * <p>
 * 单体形态下本类不启用，由 {@code PigBootApplication} 聚合本模块 jar，
 * 靠默认 {@code com.pig4cloud.pig} 基包扫描装载本模块组件。
 * <p>
 * 提供 /sync/** 端点供 QwenPaw 客户端同步记忆、凭据、技能、MCP 模板。
 *
 * @author youming
 * @date 2026-07-31
 */
@EnableOpenApi("sync")
@EnablePigResourceServer
@EnableDiscoveryClient
@SpringBootApplication
public class PigSyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(PigSyncApplication.class, args);
	}

}
