/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实例入向引用视图。
 *
 * @author youming
 */
@Data
@Schema(description = "实例入向引用")
public class OntInstanceInboundRelationVO {

	@Schema(description = "断言ID")
	private Long id;

	@Schema(description = "主体实例ID")
	private Long subjectInstanceId;

	@Schema(description = "主体实例IRI")
	private String subjectIri;

	@Schema(description = "主体实例标签")
	private String subjectLabel;

	@Schema(description = "对象属性ID（谓词）")
	private Long objectPropertyId;

	@Schema(description = "对象属性名称")
	private String objectPropertyName;

	@Schema(description = "对象属性中文标签")
	private String objectPropertyLabel;

}
