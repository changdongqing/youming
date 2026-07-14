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
	SPARQL

}
