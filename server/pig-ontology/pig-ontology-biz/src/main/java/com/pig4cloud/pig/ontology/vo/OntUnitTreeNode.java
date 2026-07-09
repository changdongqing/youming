/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单位字典树节点。
 *
 * @author youming
 */
@Data
@Schema(description = "单位字典树节点")
public class OntUnitTreeNode {

	@Schema(description = "节点ID")
	private Long id;

	@Schema(description = "父节点ID")
	private Long parentId;

	@Schema(description = "节点类型：category/unit")
	private String type;

	@Schema(description = "节点标签")
	private String label;

	@Schema(description = "编码")
	private String code;

	@Schema(description = "单位符号")
	private String symbol;

	@Schema(description = "是否内置")
	private String isBuiltin;

	@Schema(description = "子节点")
	private List<OntUnitTreeNode> children = new ArrayList<>();

}
