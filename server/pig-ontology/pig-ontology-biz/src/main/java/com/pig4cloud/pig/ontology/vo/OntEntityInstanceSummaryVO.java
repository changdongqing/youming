/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实例摘要（列表/分页）。
 *
 * @author youming
 */
@Data
@Schema(description = "实例摘要")
public class OntEntityInstanceSummaryVO {

	@Schema(description = "实例ID")
	private Long id;

	@Schema(description = "完整IRI")
	private String iri;

	@Schema(description = "IRI本地标识符")
	private String iriLocalName;

	@Schema(description = "UI显示标签")
	private String label;

	@Schema(description = "实体类型ID")
	private Long rdfTypeId;

	@Schema(description = "实体类型名称")
	private String rdfTypeName;

	@Schema(description = "实体类型中文标签")
	private String rdfTypeLabel;

	@Schema(description = "命名空间ID")
	private Long namespaceId;

	@Schema(description = "命名空间前缀")
	private String namespacePrefix;

	@Schema(description = "来源类型")
	private String sourceType;

	@Schema(description = "是否内置")
	private String isBuiltin;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "数据值数量")
	private Integer dataValueCount;

	@Schema(description = "出向断言数量")
	private Integer outgoingRelationCount;

	@Schema(description = "入向断言数量")
	private Integer incomingRelationCount;

}
