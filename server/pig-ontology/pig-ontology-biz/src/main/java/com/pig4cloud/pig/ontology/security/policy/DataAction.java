/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.policy;

/**
 * 数据访问动作。
 *
 * @author youming
 */
public enum DataAction {

	/**
	 * 查看。
	 */
	VIEW,

	/**
	 * 编辑。
	 */
	EDIT,

	/**
	 * 导出。
	 */
	EXPORT,

	/**
	 * SPARQL 查询。
	 */
	SPARQL,

	/**
	 * 映射摄入写入（18-08 §4）。
	 * <p>
	 * INGEST 策略作用于目标实体类型、数据属性和对象属性：
	 * <ul>
	 *   <li>DENY：字段/关系不得发布或执行</li>
	 *   <li>MASK：不适用于写入决策，按 DENY 处理，避免将脱敏值当真实数据写入</li>
	 *   <li>ALLOW：继续执行，并根据目标安全级别决定是否静态加密</li>
	 * </ul>
	 */
	INGEST

}
