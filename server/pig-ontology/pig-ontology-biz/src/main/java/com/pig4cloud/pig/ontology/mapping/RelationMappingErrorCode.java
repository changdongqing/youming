/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping;

import lombok.Getter;

/**
 * 对象属性关系映射错误码（18-05，ONT-REL-xxx 范围）。
 *
 * @author youming
 */
@Getter
public enum RelationMappingErrorCode {

	/** 关系映射不存在 */
	ONT_REL_001("ONT-REL-001", "关系映射不存在"),

	/** 关系映射编码在版本下已存在 */
	ONT_REL_002("ONT-REL-002", "关系映射编码在该版本下已存在"),

	/** 映射版本非DRAFT状态，不可编辑 */
	ONT_REL_003("ONT-REL-003", "映射版本非DRAFT状态，不可编辑"),

	/** 乐观锁冲突 */
	ONT_REL_004("ONT-REL-004", "关系映射配置已变更，请重新加载后操作"),

	/** 关系模式非法 */
	ONT_REL_005("ONT-REL-005", "关系模式非法，须为 FOREIGN_KEY / SELF_REFERENCE / JOIN_TABLE"),

	/** 主体实体映射不存在 */
	ONT_REL_006("ONT-REL-006", "主体实体映射不存在"),

	/** 客体实体映射不存在 */
	ONT_REL_007("ONT-REL-007", "客体实体映射不存在"),

	/** 主体和客体实体映射不属于同一映射版本 */
	ONT_REL_008("ONT-REL-008", "主体和客体实体映射不属于同一映射版本"),

	/** 对象属性不存在 */
	ONT_REL_009("ONT-REL-009", "对象属性不存在或不属于当前本体工程"),

	/** 对象属性domain不满足主体实体类型 */
	ONT_REL_010("ONT-REL-010", "对象属性定义域不包含主体实体类型"),

	/** 对象属性range不满足客体实体类型 */
	ONT_REL_011("ONT-REL-011", "对象属性值域不包含客体实体类型"),

	/** SELF_REFERENCE模式要求主体等于客体映射 */
	ONT_REL_012("ONT-REL-012", "SELF_REFERENCE 模式要求主体和客体为同一实体映射"),

	/** 键映射JSON格式非法 */
	ONT_REL_013("ONT-REL-013", "键映射JSON格式非法"),

	/** 数据源不存在 */
	ONT_REL_014("ONT-REL-014", "关联的数据源不存在"),

	/** 待解析关系不存在 */
	ONT_REL_015("ONT-REL-015", "待解析关系不存在"),

	/** 待解析关系状态不允许此操作 */
	ONT_REL_016("ONT-REL-016", "待解析关系当前状态不允许此操作");

	private final String code;

	private final String message;

	RelationMappingErrorCode(String code, String message) {
		this.code = code;
		this.message = message;
	}

}
