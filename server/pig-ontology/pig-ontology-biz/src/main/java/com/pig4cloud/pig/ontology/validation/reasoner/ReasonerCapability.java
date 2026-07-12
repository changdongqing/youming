/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.reasoner;

/**
 * 推理引擎能力声明枚举。
 *
 * @author youming
 */
public enum ReasonerCapability {

	/** 不相交冲突检测 */
	DISJOINT_CHECK,

	/** 功能性属性唯一性检测 */
	FUNCTIONAL_CHECK,

	/** 传递性推理 */
	TRANSITIVE_INFERENCE,

	/** 对称性推理 */
	SYMMETRIC_INFERENCE,

	/** 子类层次推理 */
	SUBCLASS_INFERENCE,

	/** 自定义规则推理 */
	CUSTOM_RULE_INFERENCE

}
