/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

/**
 * 摄入错误码 (ONT-ING-xxx)。
 * <p>
 * 定义见 18-01 §11 和 18-00 §11。
 *
 * @author youming
 */
public enum IngestionErrorCode {

	// 001~099 摄入、幂等和来源绑定（18-01定义）

	/** 映射版本未发布或不兼容 */
	ONT_ING_001("映射版本未发布或不兼容"),

	/** 工作区不可写 */
	ONT_ING_002("工作区不可写"),

	/** 本体版本漂移 */
	ONT_ING_003("本体版本漂移，生产写入已关闭"),

	/** 来源身份不完整 */
	ONT_ING_004("来源身份不完整"),

	/** 命名空间或实体类型不属于同一工程 */
	ONT_ING_005("命名空间或实体类型不属于同一工程"),

	/** IRI格式不合法或冲突 */
	ONT_ING_006("IRI格式不合法或冲突"),

	/** 数据属性校验失败 */
	ONT_ING_007("数据属性校验失败"),

	/** 安全策略拒绝写入 */
	ONT_ING_008("安全策略拒绝写入"),

	/** 哈希碰撞，完整键不相等 */
	ONT_ING_009("哈希碰撞，完整键不相等，失败关闭"),

	/** 实例类型不一致 */
	ONT_ING_010("实例类型不一致"),

	/** 值冲突（REJECT_CONFLICT策略） */
	ONT_ING_011("值冲突，REJECT_CONFLICT策略拒绝覆盖"),

	/** 敏感值加密失败 */
	ONT_ING_012("敏感值加密失败，不回退明文"),

	/** 类型变更需要迁移 */
	ONT_ING_013("类型变更需要迁移，不在普通同步中静默改类型");

	private final String message;

	IngestionErrorCode(String message) {
		this.message = message;
	}

	public String code() {
		return name().replace('_', '-');
	}

	public String message() {
		return message;
	}

}
