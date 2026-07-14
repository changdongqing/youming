/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project;

import lombok.Getter;

/**
 * 映射工程与版本管理错误码（18-03 §11，ONT-MAP-0xx 范围）。
 *
 * @author youming
 */
@Getter
public enum MappingErrorCode {

	/** 映射工程不存在 */
	ONT_MAP_001("ONT-MAP-001", "映射工程不存在"),

	/** 映射版本不存在 */
	ONT_MAP_002("ONT-MAP-002", "映射版本不存在"),

	/** 映射工程编码已存在 */
	ONT_MAP_003("ONT-MAP-003", "映射工程编码已存在"),

	/** 当前工程状态不允许此操作 */
	ONT_MAP_004("ONT-MAP-004", "当前工程状态不允许此操作"),

	/** 已存在编辑态版本，不能再次创建 */
	ONT_MAP_005("ONT-MAP-005", "已存在编辑态版本（DRAFT/VALIDATING/VALIDATED），不能再次创建"),

	/** 工程无活跃发布版本 */
	ONT_MAP_006("ONT-MAP-006", "工程无活跃发布版本"),

	/** 版本乐观锁冲突 */
	ONT_MAP_007("ONT-MAP-007", "版本配置已变更，请重新加载后操作"),

	/** 本体版本不满足映射约束 */
	ONT_MAP_008("ONT-MAP-008", "当前本体版本不满足映射版本约束"),

	/** 工作区修订号漂移 */
	ONT_MAP_009("ONT-MAP-009", "工作区修订号已变更，需重新校验映射版本"),

	/** 元数据依赖漂移 */
	ONT_MAP_010("ONT-MAP-010", "元数据依赖已漂移，需重新校验"),

	/** 版本号格式非法 */
	ONT_MAP_011("ONT-MAP-011", "版本号格式非法，须为 MAJOR.MINOR.PATCH"),

	/** 版本号已存在 */
	ONT_MAP_012("ONT-MAP-012", "版本号在该工程下已存在"),

	/** 已发布版本不可修改 */
	ONT_MAP_013("ONT-MAP-013", "已发布版本不可修改，需复制为新DRAFT"),

	/** 工程已归档，不可操作 */
	ONT_MAP_014("ONT-MAP-014", "工程已归档，不可重新启用或创建新版本"),

	/** 存在运行中作业，不可归档 */
	ONT_MAP_015("ONT-MAP-015", "存在运行中作业，不可归档"),

	/** 本体工程不存在 */
	ONT_MAP_016("ONT-MAP-016", "关联的本体工程不存在或无已发布版本"),

	/** 命名空间不存在 */
	ONT_MAP_017("ONT-MAP-017", "默认命名空间不存在"),

	/** 版本约束表达式非法 */
	ONT_MAP_018("ONT-MAP-018", "本体版本约束表达式非法"),

	/** 调度启用要求存在PUBLISHED版本 */
	ONT_MAP_019("ONT-MAP-019", "启用调度要求工程存在PUBLISHED活跃版本"),

	/** 工程已发布不可删除 */
	ONT_MAP_020("ONT-MAP-020", "工程已发布或存在来源绑定，不可删除");

	private final String code;

	private final String message;

	MappingErrorCode(String code, String message) {
		this.code = code;
		this.message = message;
	}

}
