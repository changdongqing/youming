/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增实体对象实例请求。
 *
 * @author youming
 */
@Data
@Schema(description = "新增实例请求")
public class OntEntityInstanceCreateDTO {

	@Schema(description = "本体工程ID，首期缺省为核心工程")
	private Long ontologyId;

	@NotNull(message = "命名空间不能为空")
	@Schema(description = "命名空间ID")
	private Long namespaceId;

	@Size(max = 128, message = "IRI本地名长度不能超过128")
	@Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]{0,127}$", message = "IRI本地名必须以字母开头，仅支持英文字母、数字、下划线和短横线")
	@Schema(description = "IRI本地标识符，省略时由后端按类型本地名_ID自动生成")
	private String iriLocalName;

	@NotNull(message = "实体类型不能为空")
	@Schema(description = "rdf:type实体类型ID")
	private Long rdfTypeId;

	@Size(max = 255, message = "标签长度不能超过255")
	@Schema(description = "UI显示标签")
	private String label;

	@Min(value = 0, message = "排序值不能小于0")
	@Schema(description = "排序")
	private Integer sortOrder;

	@Size(max = 255, message = "备注长度不能超过255")
	@Schema(description = "治理备注")
	private String remarks;

	@Valid
	@Size(max = 200, message = "单次提交数据属性值不能超过200条")
	@Schema(description = "数据属性值集合")
	private List<OntInstanceDataValueDTO> dataValues;

	@Valid
	@Size(max = 200, message = "单次提交对象属性断言不能超过200条")
	@Schema(description = "对象属性断言集合（仅指向已存在实例）")
	private List<OntInstanceObjectRelationDTO> objectRelations;

}
