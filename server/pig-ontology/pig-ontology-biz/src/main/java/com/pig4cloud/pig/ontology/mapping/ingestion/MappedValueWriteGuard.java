/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

import com.pig4cloud.pig.ontology.security.policy.SecuritySubject;

/**
 * 映射来源值写入守卫。
 * <p>
 * 手工实例接口修改带来源的数据值时必须调用此守卫，确保人工编辑不会静默覆盖映射拥有的值，
 * 或被下次同步静默覆盖。
 *
 * @author youming
 */
public interface MappedValueWriteGuard {

	/**
	 * 断言指定数据值可被手工修改。
	 *
	 * @param dataValueId 数据值ID
	 * @param subject 安全主体
	 * @param forceOverride 是否为受控人工覆盖（管理员强制）
	 * @throws IllegalStateException 如果值被映射来源拥有且非受控覆盖
	 */
	void assertWritable(Long dataValueId, SecuritySubject subject, boolean forceOverride);

}
