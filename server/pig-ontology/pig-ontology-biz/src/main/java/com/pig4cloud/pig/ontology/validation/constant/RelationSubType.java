/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.constant;

/**
 * 关系规则子类型枚举。
 * <p>
 * 收口 {@code ont_axiom_rule.sub_type} 列中 {@code category='RELATION'} 的硬编码字符串取值。
 * 该列在数据库层无 CHECK 约束（V9），取值合法性由本枚举在 Java 层保证。
 *
 * @author youming
 */
public enum RelationSubType {

	/** 功能性 */
	FUNCTIONAL,

	/** 版本替代 */
	VERSION_REPLACEMENT,

	/** 层次包含 */
	HIERARCHY_CONTAINMENT,

	/** 结构限制 */
	STRUCTURAL_LIMIT,

	/** 引用区分 */
	REFERENCE_DISTINCTION

}
