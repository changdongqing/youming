/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 实例详情。
 *
 * @author youming
 */
@Data
@Schema(description = "实例详情")
public class OntEntityInstanceDetailVO {

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

	@Schema(description = "命名空间URI")
	private String namespaceUri;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "来源类型")
	private String sourceType;

	@Schema(description = "来源引用")
	private String sourceReference;

	@Schema(description = "声明模式")
	private String declarationMode;

	@Schema(description = "是否内置")
	private String isBuiltin;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "治理说明")
	private String remarks;

	@Schema(description = "数据属性值集合")
	private List<OntInstanceDataValueVO> dataValues = new ArrayList<>();

	@Schema(description = "出向断言集合")
	private List<OntInstanceObjectRelationVO> outgoingRelations = new ArrayList<>();

	@Schema(description = "入向引用集合")
	private List<OntInstanceInboundRelationVO> inboundRelations = new ArrayList<>();

}
