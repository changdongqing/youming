/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增对象属性请求。
 *
 * @author youming
 */
@Data
@Schema(description = "新增对象属性请求")
public class OntObjectPropertyCreateDTO {

	@Schema(description = "本体工程ID，首期缺省为核心工程")
	private Long ontologyId;

	@NotNull(message = "命名空间不能为空")
	@Schema(description = "命名空间ID")
	private Long namespaceId;

	@NotBlank(message = "英文名称不能为空")
	@Size(max = 128, message = "英文名称长度不能超过128")
	@Pattern(regexp = "^[a-z][a-zA-Z0-9]*$", message = "英文名称必须以小写字母开头，仅支持英文字母和数字")
	@Schema(description = "英文名称")
	private String name;

	@Size(max = 128, message = "IRI本地名长度不能超过128")
	@Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "IRI本地名必须以字母开头，仅支持英文字母、数字和下划线")
	@Schema(description = "IRI本地标识符，省略时等于name")
	private String iriLocalName;

	@Size(max = 512, message = "IRI长度不能超过512")
	@Schema(description = "IRI，可省略；携带时必须等于后端生成值")
	private String iri;

	@NotBlank(message = "中文标签不能为空")
	@Size(max = 128, message = "中文标签长度不能超过128")
	@Schema(description = "中文标签")
	private String label;

	@Size(max = 512, message = "定义长度不能超过512")
	@Schema(description = "定义")
	private String definition;

	@NotEmpty(message = "定义域实体类型不能为空")
	@Schema(description = "定义域实体类型ID集合（并集）")
	private List<Long> domainEntityTypeIds;

	@NotEmpty(message = "值域实体类型不能为空")
	@Schema(description = "值域实体类型ID集合（并集）")
	private List<Long> rangeEntityTypeIds;

	@Pattern(regexp = "[01]", message = "功能性约束只能为0或1")
	@Schema(description = "是否功能性属性")
	private String isFunctional;

	@Pattern(regexp = "[01]", message = "反功能性约束只能为0或1")
	@Schema(description = "是否反功能性属性")
	private String isInverseFunctional;

	@Pattern(regexp = "[01]", message = "传递性约束只能为0或1")
	@Schema(description = "是否传递性属性")
	private String isTransitive;

	@Pattern(regexp = "[01]", message = "对称性约束只能为0或1")
	@Schema(description = "是否对称性属性")
	private String isSymmetric;

	@Schema(description = "逆属性ID")
	private Long inverseOfId;

	@Min(value = 0, message = "排序值不能小于0")
	@Schema(description = "排序")
	private Integer sortOrder;

	@Size(max = 255, message = "备注长度不能超过255")
	@Schema(description = "平台治理备注")
	private String remarks;

	@Schema(description = "推理能力契约，可选，取值受ReasonerCapability枚举约束")
	private List<String> inferenceSupport;

}
