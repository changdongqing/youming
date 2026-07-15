/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping;

import lombok.Getter;

/**
 * 映射预览校验与发布错误码（18-06 §15，ONT-MAP-2xx 范围）。
 *
 * @author youming
 */
@Getter
public enum ValidationErrorCode {

	/** 校验前置条件失败 */
	ONT_MAP_201("ONT-MAP-201", "校验前置条件失败"),

	/** 配置结构非法 */
	ONT_MAP_202("ONT-MAP-202", "配置结构非法"),

	/** 本体版本漂移 */
	ONT_MAP_203("ONT-MAP-203", "本体版本漂移"),

	/** 元数据漂移 */
	ONT_MAP_204("ONT-MAP-204", "元数据漂移"),

	/** 样本键重复 */
	ONT_MAP_205("ONT-MAP-205", "样本键重复"),

	/** 禁止字段映射 */
	ONT_MAP_206("ONT-MAP-206", "禁止字段映射"),

	/** 转换样本失败 */
	ONT_MAP_207("ONT-MAP-207", "转换样本失败"),

	/** 关系样本缺失 */
	ONT_MAP_208("ONT-MAP-208", "关系样本缺失"),

	/** 存在未确认WARNING */
	ONT_MAP_209("ONT-MAP-209", "存在未确认的WARNING"),

	/** 报告已过期 */
	ONT_MAP_210("ONT-MAP-210", "校验报告已过期"),

	/** 发布门禁失败 */
	ONT_MAP_211("ONT-MAP-211", "发布门禁失败");

	private final String code;

	private final String message;

	ValidationErrorCode(String code, String message) {
		this.code = code;
		this.message = message;
	}

}
