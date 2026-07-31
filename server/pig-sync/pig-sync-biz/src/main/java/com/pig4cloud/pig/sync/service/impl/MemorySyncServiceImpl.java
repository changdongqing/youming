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

package com.pig4cloud.pig.sync.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.sync.api.dto.MemoryEntryDTO;
import com.pig4cloud.pig.sync.api.dto.MemoryUploadDTO;
import com.pig4cloud.pig.sync.api.entity.ConversationHistory;
import com.pig4cloud.pig.sync.api.vo.MemoryMetaVO;
import com.pig4cloud.pig.sync.api.vo.MemoryPullItemVO;
import com.pig4cloud.pig.sync.api.vo.MemoryPullVO;
import com.pig4cloud.pig.sync.api.vo.MemoryUploadResultVO;
import com.pig4cloud.pig.sync.mapper.ConversationHistoryMapper;
import com.pig4cloud.pig.sync.service.MemorySyncService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 记忆同步服务实现
 *
 * @author youming
 * @date 2026-07-31
 */
@Slf4j
@Service
@AllArgsConstructor
public class MemorySyncServiceImpl extends ServiceImpl<ConversationHistoryMapper, ConversationHistory>
		implements MemorySyncService {

	@Override
	public MemoryPullVO pullIncremental(String userId, long since, int limit) {
		// 增量查询：seq > since，按 seq 升序，limit 条
		List<ConversationHistory> records = list(Wrappers.<ConversationHistory>lambdaQuery()
			.eq(ConversationHistory::getUserId, userId)
			.gt(ConversationHistory::getSeq, since)
			.orderByAsc(ConversationHistory::getSeq)
			.last("limit " + Math.max(1, Math.min(limit, 5000))));

		List<MemoryPullItemVO> items = records.stream().map(this::toPullItem).toList();

		MemoryPullVO vo = new MemoryPullVO();
		vo.setEntries(items);
		vo.setNextSeq(items.isEmpty() ? since : items.get(items.size() - 1).getSeq());
		// has_more：除本批外是否还有更多
		long remaining = baseMapper.countAfter(userId, vo.getNextSeq());
		vo.setHasMore(remaining > 0);
		return vo;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MemoryUploadResultVO upload(String userId, MemoryUploadDTO request) {
		int accepted = 0;
		int deduped = 0;
		if (request == null || request.getEntries() == null) {
			return buildResult(0, 0);
		}
		for (MemoryEntryDTO entry : request.getEntries()) {
			// dedup_key 为空时跳过（无法幂等）
			if (entry.getDedupKey() == null || entry.getDedupKey().isBlank()) {
				continue;
			}
			ConversationHistory record = toEntity(entry, userId);
			int rows = baseMapper.insertIfAbsent(record);
			if (rows > 0) {
				accepted++;
			}
			else {
				deduped++;
			}
		}
		log.debug("memory upload: userId={}, accepted={}, deduped={}", userId, accepted, deduped);
		return buildResult(accepted, deduped);
	}

	@Override
	public MemoryMetaVO meta(String userId) {
		MemoryMetaVO vo = new MemoryMetaVO();
		vo.setMaxSeq(baseMapper.maxSeq(userId));
		return vo;
	}

	// ================================================================
	// 转换辅助
	// ================================================================

	private MemoryPullItemVO toPullItem(ConversationHistory record) {
		MemoryPullItemVO item = new MemoryPullItemVO();
		BeanUtils.copyProperties(record, item);
		return item;
	}

	private ConversationHistory toEntity(MemoryEntryDTO entry, String userId) {
		ConversationHistory record = new ConversationHistory();
		BeanUtils.copyProperties(entry, record);
		// seq 由服务端分配（雪花），客户端的 seq 存入 clientSeq
		record.setClientSeq(entry.getSeq());
		record.setSeq(null);
		// 强制覆盖 userId，防止越权写入他人数据
		record.setUserId(userId);
		return record;
	}

	private MemoryUploadResultVO buildResult(int accepted, int deduped) {
		MemoryUploadResultVO vo = new MemoryUploadResultVO();
		vo.setAccepted(accepted);
		vo.setDeduped(deduped);
		return vo;
	}

}
