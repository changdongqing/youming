/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.constant;

/**
 * 关系客体类型枚举。
 * <p>
 * 收口 {@code ont_instance_object_relation.object_kind} 列的硬编码字符串取值。
 * 对应 V10:144 的 CHECK 约束 {@code ck_ont_relation_object_kind}。
 *
 * @author youming
 */
public enum ObjectKind {

	/** 客体为实例 */
	INSTANCE,

	/** 客体为实体类型 */
	ENTITY_TYPE

}
