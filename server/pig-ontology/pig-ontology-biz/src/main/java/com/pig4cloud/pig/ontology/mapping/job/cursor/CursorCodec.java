/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.job.cursor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 游标 JSON 编解码器（18-07 §6）。
 * <p>
 * 复用全局 ObjectMapper 将 {@link MappingCursor} 序列化为 JSON 存入 jsonb 列，
 * 或从 jsonb 列反序列化恢复游标。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CursorCodec {

	private final ObjectMapper objectMapper;

	/**
	 * 序列化游标为 JSON 字符串。
	 * @param cursor 游标模型
	 * @return JSON 字符串，null 输入返回 null
	 */
	public String encode(MappingCursor cursor) {
		if (cursor == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(cursor);
		}
		catch (Exception e) {
			log.error("Failed to encode mapping cursor", e);
			throw new RuntimeException("Failed to encode mapping cursor", e);
		}
	}

	/**
	 * 反序列化游标。
	 * @param json JSON 字符串
	 * @return 游标模型，null 或空串返回空游标
	 */
	public MappingCursor decode(String json) {
		if (json == null || json.isBlank()) {
			return new MappingCursor();
		}
		try {
			MappingCursor cursor = objectMapper.readValue(json, MappingCursor.class);
			if (cursor.getEntityMappings() == null) {
				cursor.setEntityMappings(new java.util.LinkedHashMap<>());
			}
			if (cursor.getRelationMappings() == null) {
				cursor.setRelationMappings(new java.util.LinkedHashMap<>());
			}
			return cursor;
		}
		catch (Exception e) {
			log.error("Failed to decode mapping cursor: {}", e.getMessage());
			return new MappingCursor();
		}
	}

}
