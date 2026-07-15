/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.job;

import lombok.Getter;

/**
 * 映射作业错误码（18-07 §9/§10，ONT-MAP-02x 范围）。
 *
 * @author youming
 */
@Getter
public enum MappingJobErrorCode {

	/** 映射版本未发布，不可执行作业 */
	ONT_MAP_021("ONT-MAP-021", "映射版本未发布，不可执行作业"),

	/** 已存在运行中作业 */
	ONT_MAP_022("ONT-MAP-022", "该映射工程已存在运行中作业"),

	/** 作业不存在 */
	ONT_MAP_023("ONT-MAP-023", "作业不存在"),

	/** 作业状态不允许此操作 */
	ONT_MAP_024("ONT-MAP-024", "当前作业状态不允许此操作"),

	/** 数据源不可用 */
	ONT_MAP_025("ONT-MAP-025", "数据源不可用或未激活"),

	/** 数据源凭证解密失败 */
	ONT_MAP_026("ONT-MAP-026", "数据源凭证解密失败"),

	/** 错误率超过阈值 */
	ONT_MAP_027("ONT-MAP-027", "作业错误率超过阈值，快速失败"),

	/** 作业取消 */
	ONT_MAP_028("ONT-MAP-028", "作业已被取消"),

	/** 源读取失败 */
	ONT_MAP_029("ONT-MAP-029", "源数据读取失败"),

	/** 重试记录不存在或不可重试 */
	ONT_MAP_030("ONT-MAP-030", "重试记录不存在或不可重试");

	private final String code;

	private final String message;

	MappingJobErrorCode(String code, String message) {
		this.code = code;
		this.message = message;
	}

}
