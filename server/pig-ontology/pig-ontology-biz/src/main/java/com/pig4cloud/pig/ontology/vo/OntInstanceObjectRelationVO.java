/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实例对象属性断言视图。
 *
 * @author youming
 */
@Data
@Schema(description = "实例对象属性断言")
public class OntInstanceObjectRelationVO {

	@Schema(description = "断言ID")
	private Long id;

	@Schema(description = "对象属性ID（谓词）")
	private Long objectPropertyId;

	@Schema(description = "对象属性名称")
	private String objectPropertyName;

	@Schema(description = "对象属性中文标签")
	private String objectPropertyLabel;

	@Schema(description = "客体类型：INSTANCE/ENTITY_TYPE")
	private String objectKind;

	@Schema(description = "客体资源ID（实例ID或实体类型ID）")
	private Long objectResourceId;

	@Schema(description = "客体IRI")
	private String objectIri;

	@Schema(description = "客体标签")
	private String objectLabel;

	@Schema(description = "客体实体类型ID（仅INSTANCE客体返回）")
	private Long objectRdfTypeId;

	@Schema(description = "是否显式断言，固定true")
	private Boolean asserted;

	@Schema(description = "多值顺序")
	private Integer sortOrder;

}
