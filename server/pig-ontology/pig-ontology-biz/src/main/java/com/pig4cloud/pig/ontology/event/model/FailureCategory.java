/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.model;

/**
 * 死信失败分类。
 *
 * @author youming
 */
public final class FailureCategory {

	private FailureCategory() {
	}

	/** 可重试错误：临时故障、超时等 */
	public static final String RETRYABLE = "RETRYABLE";

	/** 不可重试错误：版本不兼容、payload 格式错误、权限拒绝等 */
	public static final String NON_RETRYABLE = "NON_RETRYABLE";

}
