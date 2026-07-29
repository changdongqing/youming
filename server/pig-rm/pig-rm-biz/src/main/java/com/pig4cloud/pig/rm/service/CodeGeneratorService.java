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

package com.pig4cloud.pig.rm.service;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 统一编号生成器（基于 Redis 原子递增）
 * <p>
 * 规则：PREFIX-YYYYMMDD-XXX（XXX 为当日递增序号，3位补零）
 *
 * @author youming
 * @date 2026-07-29
 */
@Service
@AllArgsConstructor
public class CodeGeneratorService {

	private final StringRedisTemplate redisTemplate;

	/**
	 * 生成编号
	 * @param prefix 前缀，如 REQ / DEV / TST / BUG / CASE
	 * @return 编号字符串
	 */
	public String next(String prefix) {
		String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String key = "rm:seq:" + prefix + ":" + date;
		Long seq = redisTemplate.opsForValue().increment(key);
		redisTemplate.expire(key, 2, TimeUnit.DAYS);
		return prefix + "-" + date + "-" + String.format("%03d", seq);
	}

}
