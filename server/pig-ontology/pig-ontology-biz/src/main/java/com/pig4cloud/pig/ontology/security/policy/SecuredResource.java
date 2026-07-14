/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.policy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 受保护的资源描述。
 *
 * @author youming
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecuredResource {

	/**
	 * 资源类型：ENTITY_TYPE / DATA_PROPERTY / OBJECT_PROPERTY / ENTITY_INSTANCE。
	 */
	private String resourceType;

	/**
	 * 资源ID。
	 */
	private Long resourceId;

	/**
	 * 安全级别编码（已由 SecurityLevelResolver 计算的有效级别）。
	 */
	private String securityLevelCode;

	/**
	 * 实体类型ID（实例级资源时用于关联）。
	 */
	private Long entityTypeId;

}
