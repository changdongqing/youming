/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 显式数据访问审计注解。
 * <p>
 * 仅通过显式注解或服务门面记录，避免按方法名拦截的遗漏或误拦截。
 * 记录成功和失败两种情况。
 *
 * @author youming
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataAccessAudit {

	/**
	 * 访问类型（如 SPARQL、INSTANCE_VIEW、EXPORT 等）。
	 */
	String accessType();

	/**
	 * 资源类型（如 ONTOLOGY、ENTITY_INSTANCE 等）。
	 */
	String resourceType() default "ONTOLOGY";

}
