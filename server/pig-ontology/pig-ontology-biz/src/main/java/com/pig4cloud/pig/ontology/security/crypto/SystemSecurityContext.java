/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.crypto;

import lombok.Getter;

/**
 * 显式系统内部调用上下文。
 * <p>
 * 由内部定时任务、迁移作业等受信入口显式创建，
 * 不等同于管理员权限：系统上下文只能执行指定操作，仍受策略审计。
 * 不允许通过"user==null"自动获得。
 *
 * @author youming
 */
@Getter
public class SystemSecurityContext {

	/**
	 * 调用来源标记，用于审计追踪。
	 */
	private final String source;

	private SystemSecurityContext(String source) {
		this.source = source;
	}

	/**
	 * 为内部操作创建系统安全上下文。
	 *
	 * @param source 调用来源（如 "key-rotation-job"）
	 * @return 系统安全上下文
	 */
	public static SystemSecurityContext forInternalOperation(String source) {
		return new SystemSecurityContext(source);
	}

}
