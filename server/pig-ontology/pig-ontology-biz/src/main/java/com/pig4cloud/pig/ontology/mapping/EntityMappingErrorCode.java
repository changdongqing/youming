/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping;

import lombok.Getter;

/**
 * 实体与数据属性映射错误码（18-04 §13，ONT-MAP-1xx 范围）。
 *
 * @author youming
 */
@Getter
public enum EntityMappingErrorCode {

	/** 主键列不存在或不可唯一 */
	ONT_MAP_101("ONT-MAP-101", "主键列不存在或不可唯一"),

	/** IRI模板引用非主键列 */
	ONT_MAP_102("ONT-MAP-102", "IRI模板引用非主键列"),

	/** IRI渲染非法或超长 */
	ONT_MAP_103("ONT-MAP-103", "IRI渲染非法或超长"),

	/** 目标实体类型与工程不一致 */
	ONT_MAP_104("ONT-MAP-104", "目标实体类型与本映射工程不一致"),

	/** 数据属性不适用于目标类型 */
	ONT_MAP_105("ONT-MAP-105", "数据属性不适用于目标实体类型"),

	/** 源列与目标类型不兼容 */
	ONT_MAP_106("ONT-MAP-106", "源列与目标类型不兼容"),

	/** 转换器配置非法 */
	ONT_MAP_107("ONT-MAP-107", "转换器配置非法"),

	/** 单位分类不兼容 */
	ONT_MAP_108("ONT-MAP-108", "单位分类不兼容"),

	/** 多值超过上限 */
	ONT_MAP_109("ONT-MAP-109", "多值超过上限"),

	/** CLOSED_ENUM不存在转换值 */
	ONT_MAP_110("ONT-MAP-110", "CLOSED_ENUM不存在转换值"),

	/** 必填属性空值 */
	ONT_MAP_111("ONT-MAP-111", "必填属性不允许空值"),

	/** IRI模板变化需要迁移 */
	ONT_MAP_112("ONT-MAP-112", "IRI模板变化需要迁移计划"),

	/** 实体映射不存在 */
	ONT_MAP_113("ONT-MAP-113", "实体映射不存在"),

	/** 字段映射不存在 */
	ONT_MAP_114("ONT-MAP-114", "字段映射不存在"),

	/** 映射编码在版本下已存在 */
	ONT_MAP_115("ONT-MAP-115", "映射编码在该版本下已存在"),

	/** 映射版本非DRAFT状态，不可编辑 */
	ONT_MAP_116("ONT-MAP-116", "映射版本非DRAFT状态，不可编辑"),

	/** 乐观锁冲突 */
	ONT_MAP_117("ONT-MAP-117", "实体映射配置已变更，请重新加载后操作"),

	/** 数据源不存在 */
	ONT_MAP_118("ONT-MAP-118", "关联的数据源不存在或未启用"),

	/** 命名空间不存在 */
	ONT_MAP_119("ONT-MAP-119", "目标命名空间不存在"),

	/** 模板语法错误 */
	ONT_MAP_120("ONT-MAP-120", "IRI或标签模板语法错误");

	private final String code;

	private final String message;

	EntityMappingErrorCode(String code, String message) {
		this.code = code;
		this.message = message;
	}

}
