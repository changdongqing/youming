/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体类型继承树节点。
 *
 * @author youming
 */
@Data
@Schema(description = "实体类型树节点")
public class OntEntityTypeTreeNode {

	@Schema(description = "树展示路径唯一键")
	private String key;

	@Schema(description = "实体类型ID")
	private Long id;

	@Schema(description = "中文标签")
	private String label;

	@Schema(description = "英文名称")
	private String name;

	@Schema(description = "IRI")
	private String iri;

	@Schema(description = "是否抽象类")
	private String isAbstract;

	@Schema(description = "是否内置")
	private String isBuiltin;

	@Schema(description = "定义")
	private String definition;

	@Schema(description = "子节点")
	private List<OntEntityTypeTreeNode> children = new ArrayList<>();

}
