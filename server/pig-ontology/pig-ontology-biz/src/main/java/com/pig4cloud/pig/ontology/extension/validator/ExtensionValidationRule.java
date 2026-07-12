/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.validator;

import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionResource;

import java.util.List;

/**
 * 扩展合法性校验规则接口。
 *
 * <p>每条规则对应国标第9.2条的一条要求，校验结果按严重级别分级：
 * <ul>
 *   <li>VIOLATION — 违反国标硬性要求，阻断操作</li>
 *   <li>WARNING   — 潜在风险，允许操作但需用户确认</li>
 *   <li>INFO      — 提示信息，不阻断</li>
 * </ul>
 *
 * @author youming
 */
public interface ExtensionValidationRule {

	/**
	 * 规则编号（R1~R5）。
	 * @return 规则编号
	 */
	String getRuleCode();

	/**
	 * 规则名称。
	 * @return 规则名称
	 */
	String getRuleName();

	/**
	 * 国标条款依据。
	 * @return 国标条款
	 */
	String getGbClause();

	/**
	 * 执行校验。
	 * @param module 扩展模块
	 * @param resource 待校验的扩展资源（注册时非空，创建模块时为null）
	 * @return 校验结果列表（一条规则可产出多条结果）
	 */
	List<ExtensionValidationResult> validate(OntExtensionModule module, OntExtensionResource resource);

}
