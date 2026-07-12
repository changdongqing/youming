/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 导出请求参数。
 *
 * @author youming
 */
@Data
@Schema(description = "导出请求")
public class ExportRequest {

	@Schema(description = "本体工程ID，默认935001")
	private Long ontologyId;

	@Schema(description = "导出格式：TURTLE/JSON-LD/RDF-XML/N-TRIPLES，默认TURTLE")
	private String format;

	@Schema(description = "导出范围：FULL/SCHEMA_ONLY/INSTANCE_ONLY/INSTANCE_SUBTREE，默认FULL")
	private String scope;

	@Schema(description = "谓词策略：PREFERRED_ALIAS/STANDARD_IRI/INTERNAL_IRI，默认PREFERRED_ALIAS")
	private String predicateStrategy;

	@Schema(description = "子树过滤的实体类型ID（scope=INSTANCE_SUBTREE 时必填）")
	private Long targetTypeId;

	@Schema(description = "是否强制导出（跳过校验拦截），默认false")
	private Boolean force;

	@Schema(description = "true=返回文件下载流，false=返回文本内容")
	private Boolean download;

}
