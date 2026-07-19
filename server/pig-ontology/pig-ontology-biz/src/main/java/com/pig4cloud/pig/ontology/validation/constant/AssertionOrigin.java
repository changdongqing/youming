/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.constant;

/**
 * 断言来源枚举。
 * <p>
 * 收口 {@code ont_instance_object_relation.assertion_origin} 列的硬编码字符串取值。
 * 对应 V22:1075 的 CHECK 约束 {@code ck_ont_relation_assertion_origin}。
 *
 * @author youming
 */
public enum AssertionOrigin {

	/** 种子数据 */
	SEED,

	/** 人工录入 */
	MANUAL,

	/** 数据源映射 */
	DATA_MAPPING,

	/** 物模型接入 */
	IOT,

	/** 规则推理 */
	RULE,

	/** 批量导入 */
	IMPORT,

	/** 外部API */
	API

}
