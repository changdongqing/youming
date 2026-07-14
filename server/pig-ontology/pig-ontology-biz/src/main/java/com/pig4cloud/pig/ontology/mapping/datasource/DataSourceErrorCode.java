/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource;

import lombok.Getter;

/**
 * 数据源映射错误码（18-02 §14）。
 *
 * @author youming
 */
@Getter
public enum DataSourceErrorCode {

	/** 数据源不存在 */
	ONT_DS_001("ONT-DS-001", "数据源不存在"),

	/** 数据源未启用 */
	ONT_DS_002("ONT-DS-002", "数据源未启用"),

	/** 凭证解密失败 */
	ONT_DS_003("ONT-DS-003", "凭证解密失败"),

	/** 连接超时 */
	ONT_DS_004("ONT-DS-004", "连接超时"),

	/** 认证失败 */
	ONT_DS_005("ONT-DS-005", "认证失败"),

	/** 数据库类型不支持 */
	ONT_DS_006("ONT-DS-006", "数据库类型不支持"),

	/** Schema不在白名单 */
	ONT_DS_007("ONT-DS-007", "Schema不在白名单"),

	/** 对象不在白名单 */
	ONT_DS_008("ONT-DS-008", "对象不在白名单"),

	/** 元数据不存在或已过期 */
	ONT_DS_009("ONT-DS-009", "元数据不存在或已过期"),

	/** 元数据发生漂移 */
	ONT_DS_010("ONT-DS-010", "元数据发生漂移"),

	/** 非法JDBC URL参数 */
	ONT_DS_011("ONT-DS-011", "非法JDBC URL参数"),

	/** 元数据对象数超过上限 */
	ONT_DS_012("ONT-DS-012", "元数据对象数超过上限"),

	/** 作业期间数据源被停用 */
	ONT_DS_021("ONT-DS-021", "作业期间数据源被停用"),

	/** 数据源状态不允许操作 */
	ONT_DS_013("ONT-DS-013", "当前状态不允许此操作"),

	/** 测试状态不满足启用条件 */
	ONT_DS_014("ONT-DS-014", "启用要求最近测试成功且测试版本与当前版本一致"),

	/** 数据源被引用不可删除 */
	ONT_DS_015("ONT-DS-015", "数据源被发布映射版本或运行中作业引用，不可删除"),

	/** connection_config非法 */
	ONT_DS_016("ONT-DS-016", "连接配置非法或缺少必填字段"),

	/** 源编码已存在 */
	ONT_DS_017("ONT-DS-017", "数据源编码已存在"),

	/** 元数据刷新对象超过上限 */
	ONT_DS_018("ONT-DS-018", "元数据刷新对象数超过上限，请按Schema或对象名过滤");

	private final String code;

	private final String message;

	DataSourceErrorCode(String code, String message) {
		this.code = code;
		this.message = message;
	}

}
