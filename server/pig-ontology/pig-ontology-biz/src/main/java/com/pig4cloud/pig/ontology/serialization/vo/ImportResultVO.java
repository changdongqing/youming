/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 导入确认结果VO。
 *
 * @author youming
 */
@Data
@Schema(description = "导入结果")
public class ImportResultVO {

	@Schema(description = "总实例数")
	private int totalInstances;

	@Schema(description = "导入成功数")
	private int importedCount;

	@Schema(description = "跳过数")
	private int skippedCount;

	@Schema(description = "失败数")
	private int failedCount;

	@Schema(description = "数据值数")
	private int dataValueCount;

	@Schema(description = "对象关系数")
	private int objectRelationCount;

	@Schema(description = "耗时（毫秒）")
	private long durationMs;

	@Schema(description = "审计日志ID")
	private Long logId;

}
