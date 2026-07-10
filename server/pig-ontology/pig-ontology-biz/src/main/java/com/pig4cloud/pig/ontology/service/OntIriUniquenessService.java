/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service;

/**
 * 全局IRI唯一性校验组件。
 *
 * 统一校验IRI在命名空间URI、实体类型、数据属性和对象属性中是否唯一。
 * 各Schema写入口（实体类型、数据属性、对象属性Service和IRI校验接口）复用该组件。
 *
 * @author youming
 */
public interface OntIriUniquenessService {

	/**
	 * 校验IRI在全部有效Schema记录中是否唯一。
	 * @param iri 完整IRI
	 * @param excludeTableName 排除的表名（当前正在写入的表），可为null
	 * @param excludeId 排除的记录ID（当前更新记录），可为null
	 * @return 冲突类型描述，null表示可用
	 */
	String checkIriConflict(String iri, String excludeTableName, Long excludeId);

	/**
	 * 校验IRI是否可用，返回布尔结果。
	 * @param iri 完整IRI
	 * @param excludeTableName 排除的表名
	 * @param excludeId 排除的记录ID
	 * @return true表示可用，false表示冲突
	 */
	default boolean isIriAvailable(String iri, String excludeTableName, Long excludeId) {
		return checkIriConflict(iri, excludeTableName, excludeId) == null;
	}

}
