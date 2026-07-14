/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

/**
 * 删除与失活策略。
 * <p>
 * 当源记录在增量同步中消失时的处理方式。
 *
 * @author youming
 */
public enum DeleteStrategy {

	/** 仅更新绑定为MISSING */
	IGNORE,

	/** 写目标本体配置的状态数据属性 */
	MARK_INACTIVE,

	/** 软删除实例（仅当无其他来源引用时） */
	SOFT_DELETE,

	/** 绑定进入CONFLICT并创建作业错误 */
	BLOCK_AND_REVIEW

}
