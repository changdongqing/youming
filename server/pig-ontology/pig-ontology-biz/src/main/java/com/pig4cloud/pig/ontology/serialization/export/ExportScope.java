/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.export;

/**
 * 导出范围枚举。
 *
 * @author youming
 */
public enum ExportScope {

	/** 完整导出：Schema + Data */
	FULL("完整导出（Schema+Data）"),

	/** 仅 Schema 层 */
	SCHEMA_ONLY("仅Schema层"),

	/** 仅 Data 层 */
	INSTANCE_ONLY("仅实例数据"),

	/** 按实体类型子树过滤实例（Schema 全量 + 子树实例） */
	INSTANCE_SUBTREE("按实体类型子树过滤实例");

	private final String description;

	ExportScope(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

}
