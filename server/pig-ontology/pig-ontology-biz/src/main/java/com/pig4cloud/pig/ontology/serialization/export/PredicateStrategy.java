/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.export;

/**
 * 谓词IRI策略枚举。
 *
 * @author youming
 */
public enum PredicateStrategy {

	/** 首选别名（附录D兼容，如 measurementUnit） */
	PREFERRED_ALIAS,

	/** 国标标准IRI（严格口径，如 unit） */
	STANDARD_IRI,

	/** 平台内部IRI */
	INTERNAL_IRI

}
