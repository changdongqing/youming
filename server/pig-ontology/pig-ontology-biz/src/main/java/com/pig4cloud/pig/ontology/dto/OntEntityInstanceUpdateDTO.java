/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改实体对象实例请求。
 *
 * @author youming
 */
@Data
@Schema(description = "修改实例请求")
public class OntEntityInstanceUpdateDTO {

	@NotNull(message = "实例ID不能为空")
	@Schema(description = "实例ID")
	private Long id;

	@Schema(description = "命名空间ID；扩展实例可修改，内置实例忽略")
	private Long namespaceId;

	@Size(max = 128, message = "IRI本地名长度不能超过128")
	@Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]{0,127}$", message = "IRI本地名必须以字母开头，仅支持英文字母、数字、下划线和短横线")
	@Schema(description = "IRI本地标识符；扩展实例可修改")
	private String iriLocalName;

	@Schema(description = "rdf:type实体类型ID；扩展实例可修改，修改后全量重校验数据值和断言")
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

}
