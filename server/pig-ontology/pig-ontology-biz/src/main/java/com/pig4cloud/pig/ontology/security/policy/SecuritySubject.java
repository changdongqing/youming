/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.policy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 策略主体：包含用户ID、角色ID列表和部门ID列表。
 * <p>
 * 不得只取第一个角色或第一个部门。
 *
 * @author youming
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecuritySubject {

	/**
	 * 用户ID，SYSTEM 主体为 null。
	 */
	private Long userId;

	/**
	 * 全部角色ID。
	 */
	private List<Long> roleIds;

	/**
	 * 全部部门ID。
	 */
	private List<Long> deptIds;

	/**
	 * 用户名，用于审计。
	 */
	private String username;

	/**
	 * 是否系统内部调用。
	 */
	private boolean system;

}
