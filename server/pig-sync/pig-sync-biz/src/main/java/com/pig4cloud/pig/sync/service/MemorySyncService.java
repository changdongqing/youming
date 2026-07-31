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

package com.pig4cloud.pig.sync.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.sync.api.entity.ConversationHistory;
import com.pig4cloud.pig.sync.api.vo.MemoryMetaVO;
import com.pig4cloud.pig.sync.api.vo.MemoryPullVO;
import com.pig4cloud.pig.sync.api.vo.MemoryUploadResultVO;

/**
 * 记忆同步服务接口
 *
 * @author youming
 * @date 2026-07-31
 */
public interface MemorySyncService extends IService<ConversationHistory> {

	/**
	 * 增量下行：查询该用户 seq > since 的记忆，分页返回。
	 *
	 * @param userId 用户ID
	 * @param since 增量游标（> since）
	 * @param limit 每批条数
	 */
	MemoryPullVO pullIncremental(String userId, long since, int limit);

	/**
	 * 幂等上行：逐条 ON CONFLICT DO NOTHING，统计 accepted/deduped。
	 *
	 * @param userId 用户ID（覆盖每条记录的 user_id，防止越权）
	 */
	MemoryUploadResultVO upload(String userId, com.pig4cloud.pig.sync.api.dto.MemoryUploadDTO request);

	/**
	 * 对账元数据：该用户服务端最新 seq。
	 */
	MemoryMetaVO meta(String userId);

}
