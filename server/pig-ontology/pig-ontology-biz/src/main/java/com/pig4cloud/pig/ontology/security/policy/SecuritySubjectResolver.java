/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.policy;

import com.pig4cloud.pig.common.security.service.PigUser;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 从 pig 安全上下文解析 SecuritySubject。
 * <p>
 * 优先从 {@link SecurityUtils#getUser()} 获取 {@link PigUser}，
 * 无安全上下文时返回 SYSTEM 主体（策略服务按失败关闭处理）。
 *
 * @author youming
 */
@Slf4j
@Component
public class SecuritySubjectResolver {

	/**
	 * 解析当前安全主体。
	 *
	 * @return 安全主体，无上下文时返回 SYSTEM 主体
	 */
	public SecuritySubject resolve() {
		try {
			PigUser user = SecurityUtils.getUser();
			if (user == null) {
				log.debug("No security context, resolving SYSTEM subject");
				return systemSubject();
			}
			return SecuritySubject.builder()
				.userId(user.getId())
				.roleIds(user.getRoleIds() != null ? user.getRoleIds() : Collections.emptyList())
				.deptIds(user.getDeptIds() != null ? user.getDeptIds() : Collections.emptyList())
				.username(user.getUsername())
				.system(false)
				.build();
		}
		catch (Exception e) {
			log.debug("Security context unavailable, resolving SYSTEM subject: {}", e.getMessage());
			return systemSubject();
		}
	}

	/**
	 * 创建系统内部调用主体。
	 * <p>
	 * 系统主体不代表管理员权限，策略服务应按失败关闭处理。
	 */
	private SecuritySubject systemSubject() {
		return SecuritySubject.builder()
			.userId(null)
			.roleIds(Collections.emptyList())
			.deptIds(Collections.emptyList())
			.username("system")
			.system(true)
			.build();
	}

}
