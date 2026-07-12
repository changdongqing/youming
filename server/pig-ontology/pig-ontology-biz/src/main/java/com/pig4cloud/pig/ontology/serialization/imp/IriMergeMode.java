/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.imp;

/**
 * IRI 合并模式（导入冲突处理策略）。
 *
 * @author youming
 */
public enum IriMergeMode {

	/** 跳过已存在的实例 */
	SKIP,

	/** 保留已有实例，追加新数据值和关系 */
	MERGE,

	/** 删除已有实例的全部数据，重新写入 */
	OVERWRITE

}
