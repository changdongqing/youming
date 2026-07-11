/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实体类型关系。
 *
 * @author youming
 */
@Data
@Schema(description = "实体类型关系")
public class OntEntityTypeRelationVO {

	@Schema(description = "实体类型A ID")
	private Long typeAId;

	@Schema(description = "实体类型A名称")
	private String typeAName;

	@Schema(description = "实体类型A标签")
	private String typeALabel;

	@Schema(description = "实体类型B ID")
	private Long typeBId;

	@Schema(description = "实体类型B名称")
	private String typeBName;

	@Schema(description = "实体类型B标签")
	private String typeBLabel;

}
