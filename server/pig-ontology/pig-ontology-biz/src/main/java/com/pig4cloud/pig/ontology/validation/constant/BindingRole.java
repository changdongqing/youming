/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.constant;

/**
 * 公理规则目标绑定角色枚举。
 * <p>
 * 收口 {@code ont_axiom_rule_target.binding_role} 列的硬编码字符串取值。
 * 该列在数据库层无 CHECK 约束（V9），取值合法性由本枚举在 Java 层保证。
 *
 * @author youming
 */
public enum BindingRole {

	/** 不相交规则类型A */
	TYPE_A,

	/** 不相交规则类型B */
	TYPE_B,

	/** 目标类 */
	TARGET_CLASS,

	/** 键属性 */
	KEY_PROPERTY,

	/** 日期顺序-较早属性 */
	EARLIER_PROPERTY,

	/** 日期顺序-较晚属性 */
	LATER_PROPERTY,

	/** 枚举属性 */
	ENUM_PROPERTY,

	/** 值属性 */
	VALUE_PROPERTY,

	/** 单位一致性-最大值 */
	MAX,

	/** 单位一致性-最小值 */
	MIN,

	/** 单位一致性-范围 */
	RANGE,

	/** 单位一致性-单位 */
	UNIT,

	/** 关系属性 */
	RELATION_PROPERTY,

	/** 状态属性 */
	STATUS,

	/** 替代关系 */
	REPLACES,

	/** 被替代关系 */
	REPLACED_BY,

	/** 父类 */
	PARENT_CLASS,

	/** 子类 */
	CHILD_CLASS,

	/** 标准类 */
	STANDARD_CLASS,

	/** 条款类 */
	CLAUSE_CLASS,

	/** 标准间关系 */
	STANDARD_RELATION,

	/** 条款级关系 */
	CLAUSE_RELATION

}
