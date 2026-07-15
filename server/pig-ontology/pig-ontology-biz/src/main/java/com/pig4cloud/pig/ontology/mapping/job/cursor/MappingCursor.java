/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.job.cursor;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 映射同步游标模型（18-07 §6）。
 * <p>
 * 按每个实体/关系映射分别保存增量值和最后主键值，用于 keyset 分页恢复。
 * <pre>
 * {
 *   "entityMappings": {
 *     "USER_ACCOUNT": {
 *       "incrementalValue": "2026-07-14T10:00:00Z",
 *       "lastKey": {"user_id": "100"}
 *     }
 *   },
 *   "relationMappings": {}
 * }
 * </pre>
 *
 * @author youming
 */
@Data
public class MappingCursor {

	/** 实体映射游标，key=mappingCode */
	private Map<String, EntityCursor> entityMappings = new LinkedHashMap<>();

	/** 关系映射游标，key=mappingCode */
	private Map<String, EntityCursor> relationMappings = new LinkedHashMap<>();

	/**
	 * 获取或创建实体映射游标。
	 * @param mappingCode 映射编码
	 * @return 游标条目
	 */
	public EntityCursor getOrCreateEntity(String mappingCode) {
		return entityMappings.computeIfAbsent(mappingCode, k -> new EntityCursor());
	}

	/**
	 * 获取或创建关系映射游标。
	 * @param mappingCode 映射编码
	 * @return 游标条目
	 */
	public EntityCursor getOrCreateRelation(String mappingCode) {
		return relationMappings.computeIfAbsent(mappingCode, k -> new EntityCursor());
	}

	/**
	 * 单个映射的游标条目。
	 */
	@Data
	public static class EntityCursor {

		/** 增量列值（时间戳或数值的字符串表示） */
		private String incrementalValue;

		/** 最后一条记录的主键值映射 */
		private Map<String, String> lastKey = new LinkedHashMap<>();

	}

}
