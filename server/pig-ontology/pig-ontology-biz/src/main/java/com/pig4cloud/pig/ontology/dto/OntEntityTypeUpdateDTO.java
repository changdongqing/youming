/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 修改实体类型请求。
 *
 * @author youming
 */
@Data
@Schema(description = "修改实体类型请求")
public class OntEntityTypeUpdateDTO {

	@NotNull(message = "实体类型ID不能为空")
	@Schema(description = "实体类型ID")
	private Long id;

	@Schema(description = "命名空间ID；扩展类型必填，内置类型忽略")
	private Long namespaceId;

	@Size(max = 128, message = "英文名称长度不能超过128")
	@Pattern(regexp = "^[A-Z][a-zA-Z0-9]*$", message = "英文名称必须以大写字母开头，仅支持英文字母和数字")
	@Schema(description = "英文名称；扩展类型必填，内置类型忽略")
	private String name;

	@Size(max = 512, message = "IRI长度不能超过512")
	@Schema(description = "IRI，可省略；扩展类型携带时必须等于命名空间URI加英文名称")
	private String iri;

	@NotBlank(message = "中文标签不能为空")
	@Size(max = 128, message = "中文标签长度不能超过128")
	@Schema(description = "中文标签")
	private String label;

	@Size(max = 512, message = "定义长度不能超过512")
	@Schema(description = "定义")
	private String definition;

	@Pattern(regexp = "[01]", message = "是否抽象类只能为0或1")
	@Schema(description = "是否抽象类，1是0否；内置类型忽略")
	private String isAbstract;

	@Size(max = 50, message = "父类数量不能超过50")
	@Schema(description = "直接父类ID列表；内置类型忽略")
	private List<Long> parentIds;

	@Min(value = 0, message = "排序值不能小于0")
	@Schema(description = "排序")
	private Integer sortOrder;

	@Size(max = 255, message = "备注长度不能超过255")
	@Schema(description = "备注")
	private String remarks;

}
