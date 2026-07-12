/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 导出预览结果（限200行）。
 *
 * @author youming
 */
@Data
@Schema(description = "导出预览")
public class ExportPreviewVO {

	@Schema(description = "RDF格式")
	private String format;

	@Schema(description = "RDF文本内容（限200行）")
	private String content;

	@Schema(description = "三元组数")
	private long tripleCount;

	@Schema(description = "导出范围")
	private String scope;

	@Schema(description = "谓词策略")
	private String predicateStrategy;

	@Schema(description = "内容大小（字节）")
	private long contentSize;

	@Schema(description = "前置校验结果")
	private ExportResultVO.PrecheckVO precheck;

}
