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
 * 修改数据属性请求。
 *
 * @author youming
 */
@Data
@Schema(description = "修改数据属性请求")
public class OntDataPropertyUpdateDTO {

	@NotNull(message = "数据属性ID不能为空")
	@Schema(description = "数据属性ID")
	private Long id;

	@Schema(description = "命名空间ID；扩展类型必填，内置类型忽略")
	private Long namespaceId;

	@Size(max = 128, message = "英文名称长度不能超过128")
	@Pattern(regexp = "^[a-z][a-zA-Z0-9]*$", message = "英文名称必须以小写字母开头，仅支持英文字母和数字")
	@Schema(description = "英文名称；扩展类型必填，内置类型忽略")
	private String name;

	@Size(max = 128, message = "IRI本地名长度不能超过128")
	@Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "IRI本地名必须以字母开头，仅支持英文字母、数字和下划线")
	@Schema(description = "IRI本地标识符；扩展类型可修改，内置类型忽略")
	private String iriLocalName;

	@Size(max = 512, message = "IRI长度不能超过512")
	@Schema(description = "IRI，可省略；扩展类型携带时必须等于后端生成值")
	private String iri;

	@NotBlank(message = "中文标签不能为空")
	@Size(max = 128, message = "中文标签长度不能超过128")
	@Schema(description = "中文标签")
	private String label;

	@Size(max = 512, message = "定义长度不能超过512")
	@Schema(description = "定义")
	private String definition;

	@Schema(description = "定义域实体类型ID；扩展类型必填，内置类型忽略")
	private Long domainEntityTypeId;

	@Pattern(regexp = "BOOLEAN|DATE|NUMERIC|TEXT|URI|UNIT_REF|TEXT_OR_NUMERIC", message = "值域类型只能为BOOLEAN/DATE/NUMERIC/TEXT/URI/UNIT_REF/TEXT_OR_NUMERIC")
	@Schema(description = "基本数据类型；扩展类型必填，内置类型忽略")
	private String baseType;

	@Pattern(regexp = "FREE|CLOSED_ENUM|OPEN_ENUM|EXTERNAL_DICTIONARY|UNIT_DICTIONARY", message = "值模式只能为FREE/CLOSED_ENUM/OPEN_ENUM/EXTERNAL_DICTIONARY/UNIT_DICTIONARY")
	@Schema(description = "值模式；扩展类型必填，内置类型忽略")
	private String valueMode;

	@Size(max = 64, message = "外部值源代码长度不能超过64")
	@Schema(description = "外部值源代码")
	private String valueSourceRef;

	@Size(max = 512, message = "正则约束长度不能超过512")
	@Schema(description = "正则约束")
	private String regexPattern;

	@Size(max = 255, message = "格式提示长度不能超过255")
	@Schema(description = "格式提示")
	private String formatHint;

	@Pattern(regexp = "[01]", message = "唯一性约束只能为0或1")
	@Schema(description = "实例值是否在本体工程内唯一")
	private String isUnique;

	@Schema(description = "单位分类ID")
	private Long unitCategoryId;

	@Schema(description = "单位引用模式，仅UNIT_REF使用DICTIONARY_SYMBOL")
	private String unitRefMode;

	@Size(max = 128, message = "首选别名长度不能超过128")
	@Schema(description = "UI/兼容导入导出的首选别名")
	private String preferredAlias;

	@Schema(description = "枚举值集合")
	private List<String> enumValues;

	@Min(value = 0, message = "排序值不能小于0")
	@Schema(description = "排序")
	private Integer sortOrder;

	@Size(max = 255, message = "备注长度不能超过255")
	@Schema(description = "平台治理备注")
	private String remarks;

}
