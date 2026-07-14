/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

/**
 * 冲突/所有权策略。
 * <p>
 * 决定同步写入与人工编辑之间的冲突处理方式。
 *
 * @author youming
 */
public enum OwnershipPolicy {

	/** 源值覆盖映射拥有的旧值 */
	SOURCE_WINS,

	/** 人工覆盖优先，无覆盖时同步更新 */
	MANUAL_WINS,

	/** 检测到值与现值不同则记录失败，不覆盖 */
	REJECT_CONFLICT

}
