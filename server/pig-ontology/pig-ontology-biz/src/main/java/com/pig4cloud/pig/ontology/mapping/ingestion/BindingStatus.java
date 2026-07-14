/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

/**
 * 来源绑定状态。
 *
 * @author youming
 */
public enum BindingStatus {

	/** 活跃绑定 */
	ACTIVE,

	/** 已失活 */
	INACTIVE,

	/** 源记录缺失 */
	MISSING,

	/** 存在冲突 */
	CONFLICT

}
