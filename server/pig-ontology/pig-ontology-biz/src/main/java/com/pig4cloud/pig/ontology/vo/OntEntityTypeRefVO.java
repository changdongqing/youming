/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实体类型引用（用于定义域/值域集合展示）。
 *
 * @author youming
 */
@Data
@Schema(description = "实体类型引用")
public class OntEntityTypeRefVO {

	@Schema(description = "实体类型ID")
	private Long id;

	@Schema(description = "实体类型名称")
	private String name;

	@Schema(description = "实体类型中文标签")
	private String label;

	@Schema(description = "实体类型IRI")
	private String iri;

}
