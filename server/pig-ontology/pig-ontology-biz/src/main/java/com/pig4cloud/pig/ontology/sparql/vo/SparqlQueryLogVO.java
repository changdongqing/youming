/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SPARQL 查询历史记录展示 VO。
 * <p>
 * 不含 queryText，仅展示 queryPreview（脱敏预览）。
 * </p>
 *
 * @author youming
 */
@Data
@Schema(description = "SPARQL查询历史记录")
public class SparqlQueryLogVO {

	@Schema(description = "主键ID")
	private Long id;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "查询类型：SELECT/ASK")
	private String queryType;

	@Schema(description = "查询预览（脱敏）")
	private String queryPreview;

	@Schema(description = "结果格式")
	private String resultFormat;

	@Schema(description = "结果行数")
	private Integer rowCount;

	@Schema(description = "执行耗时（毫秒）")
	private Long durationMs;

	@Schema(description = "结果是否截断：0/1")
	private String truncated;

	@Schema(description = "执行状态：SUCCESS/REJECTED/TIMEOUT/FAILED")
	private String status;

	@Schema(description = "错误码")
	private String errorCode;

	@Schema(description = "创建人")
	private String createBy;

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

}
