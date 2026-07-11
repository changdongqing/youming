/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 实例动态表单Schema。
 *
 * @author youming
 */
@Data
@Schema(description = "实例动态表单Schema")
public class OntInstanceFormMetaVO {

	@Schema(description = "实体类型ID")
	private Long rdfTypeId;

	@Schema(description = "实体类型名称")
	private String rdfTypeName;

	@Schema(description = "实体类型中文标签")
	private String rdfTypeLabel;

	@Schema(description = "是否抽象类")
	private String isAbstract;

	@Schema(description = "适用数据属性集合（含继承属性）")
	private List<DataPropertyMeta> applicableDataProperties = new ArrayList<>();

	@Schema(description = "适用对象属性集合（含继承属性）")
	private List<ObjectPropertyMeta> applicableObjectProperties = new ArrayList<>();

	/**
	 * 数据属性表单元数据。
	 */
	@Data
	@Schema(description = "数据属性表单元数据")
	public static class DataPropertyMeta {

		@Schema(description = "数据属性ID")
		private Long dataPropertyId;

		@Schema(description = "数据属性名称")
		private String dataPropertyName;

		@Schema(description = "数据属性中文标签")
		private String dataPropertyLabel;

		@Schema(description = "基础类型")
		private String baseType;

		@Schema(description = "值模式")
		private String valueMode;

		@Schema(description = "是否唯一")
		private String isUnique;

		@Schema(description = "单位分类ID")
		private Long unitCategoryId;

		@Schema(description = "正则约束")
		private String regexPattern;

		@Schema(description = "格式提示")
		private String formatHint;

		@Schema(description = "是否继承")
		private Boolean inherited;

		@Schema(description = "继承距离")
		private Integer inheritanceDistance;

	}

	/**
	 * 对象属性表单元数据。
	 */
	@Data
	@Schema(description = "对象属性表单元数据")
	public static class ObjectPropertyMeta {

		@Schema(description = "对象属性ID")
		private Long objectPropertyId;

		@Schema(description = "对象属性名称")
		private String objectPropertyName;

		@Schema(description = "对象属性中文标签")
		private String objectPropertyLabel;

		@Schema(description = "是否功能性")
		private String isFunctional;

		@Schema(description = "值域实体类型集合")
		private List<OntEntityTypeRefVO> rangeEntityTypes;

		@Schema(description = "是否继承")
		private Boolean inherited;

		@Schema(description = "继承距离")
		private Integer inheritanceDistance;

	}

}
