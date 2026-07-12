/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 序列化审计日志VO。
 *
 * @author youming
 */
@Data
@Schema(description = "序列化审计日志")
public class SerializationLogVO {

	@Schema(description = "日志ID")
	private Long id;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "操作类型: EXPORT/IMPORT")
	private String operationType;

	@Schema(description = "RDF格式")
	private String rdfFormat;

	@Schema(description = "导出范围")
	private String exportScope;

	@Schema(description = "谓词策略")
	private String predicateStrategy;

	@Schema(description = "三元组数")
	private Integer tripleCount;

	@Schema(description = "文件大小（字节）")
	private Long contentSize;

	@Schema(description = "实例数")
	private Integer instanceCount;

	@Schema(description = "数据属性值数")
	private Integer dataValueCount;

	@Schema(description = "对象关系数")
	private Integer objectRelationCount;

	@Schema(description = "跳过数")
	private Integer skippedCount;

	@Schema(description = "失败数")
	private Integer failedCount;

	@Schema(description = "前置校验是否通过")
	private String precheckPassed;

	@Schema(description = "校验报告ID")
	private Long validationReportId;

	@Schema(description = "是否强制导出")
	private String forceFlag;

	@Schema(description = "导入合并模式")
	private String importIriMergeMode;

	@Schema(description = "耗时（毫秒）")
	private Long durationMs;

	@Schema(description = "操作人")
	private String createBy;

	@Schema(description = "操作时间")
	private LocalDateTime createTime;

}
