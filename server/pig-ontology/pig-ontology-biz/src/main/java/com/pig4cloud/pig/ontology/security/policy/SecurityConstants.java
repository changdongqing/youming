/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.policy;

/**
 * 安全模块常量定义。
 *
 * @author youming
 */
public final class SecurityConstants {

	private SecurityConstants() {
	}

	/**
	 * 主体类型。
	 */
	public static final String SUBJECT_USER = "USER";

	public static final String SUBJECT_ROLE = "ROLE";

	public static final String SUBJECT_DEPT = "DEPT";

	/**
	 * 资源类型。
	 */
	public static final String RESOURCE_ENTITY_TYPE = "ENTITY_TYPE";

	public static final String RESOURCE_DATA_PROPERTY = "DATA_PROPERTY";

	public static final String RESOURCE_OBJECT_PROPERTY = "OBJECT_PROPERTY";

	/**
	 * 工程 ACL 访问级别。
	 */
	public static final String ACL_VIEW = "VIEW";

	public static final String ACL_EDIT = "EDIT";

	public static final String ACL_PUBLISH = "PUBLISH";

	public static final String ACL_ADMIN = "ADMIN";

	/**
	 * 默认安全级别。
	 */
	public static final String LEVEL_INTERNAL = "INTERNAL";

	/**
	 * Redis 缓存 key 前缀。
	 */
	public static final String CACHE_POLICY_PREFIX = "ont:policy:";

	public static final String CACHE_POLICY_REVISION_PREFIX = "ont:policy:revision:";

	/**
	 * 缓存 TTL（秒）。
	 */
	public static final long CACHE_POLICY_TTL_SECONDS = 300;

}
