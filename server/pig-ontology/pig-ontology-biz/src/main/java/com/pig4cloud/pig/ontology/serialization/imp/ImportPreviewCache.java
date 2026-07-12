/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.imp;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 导入预检结果缓存（30分钟TTL）。
 *
 * @author youming
 */
@Component
public class ImportPreviewCache {

	private static final long TTL_MS = 30 * 60 * 1000L;

	private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

	/**
	 * 存入预检结果。
	 * @return 预检ID
	 */
	public String put(ImportPreviewResult result) {
		String previewId = "imp-" + System.currentTimeMillis() + "-" + result.hashCode();
		cache.put(previewId, new CacheEntry(result, System.currentTimeMillis()));
		return previewId;
	}

	/**
	 * 获取预检结果。
	 * @return 预检结果，过期或不存在返回 null
	 */
	public ImportPreviewResult get(String previewId) {
		CacheEntry entry = cache.get(previewId);
		if (entry == null) {
			return null;
		}
		if (System.currentTimeMillis() - entry.timestamp > TTL_MS) {
			cache.remove(previewId);
			return null;
		}
		return entry.result;
	}

	/**
	 * 移除预检结果。
	 */
	public void remove(String previewId) {
		cache.remove(previewId);
	}

	private record CacheEntry(ImportPreviewResult result, long timestamp) {
	}

}
