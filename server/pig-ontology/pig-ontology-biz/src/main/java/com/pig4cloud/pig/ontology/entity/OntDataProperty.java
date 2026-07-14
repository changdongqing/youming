/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据属性。
 *
 * @author youming
 */
@Data
@TableName("ont_data_property")
@Schema(description = "数据属性")
@EqualsAndHashCode(callSuper = true)
public class OntDataProperty extends Model<OntDataProperty> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "数据属性ID")
	private Long id;

	@Schema(description = "平台内部全局唯一IRI")
	private String iri;

	@Schema(description = "IRI本地标识符，用于拼接iri")
	private String iriLocalName;

	@Schema(description = "国标原始IRI，允许因国标重名而重复")
	private String standardIri;

	@Schema(description = "附录A.2 Name，核心值保持国标原名")
	private String name;

	@Schema(description = "UI/兼容导入导出的首选别名")
	private String preferredAlias;

	@Schema(description = "定义")
	private String definition;

	@Schema(description = "定义域实体类型ID")
	private Long domainEntityTypeId;

	@Schema(description = "基本数据类型")
	private String baseType;

	@Schema(description = "值模式")
	private String valueMode;

	@Schema(description = "外部值源代码，如ICS、CCS")
	private String valueSourceRef;

	@Schema(description = "正则约束")
	private String regexPattern;

	@Schema(description = "人类可读格式提示")
	private String formatHint;

	@Schema(description = "实例值是否在本体工程内唯一，1是0否")
	private String isUnique;

	@Schema(description = "单位分类ID，NULL表示不限分类")
	private Long unitCategoryId;

	@Schema(description = "单位引用模式，仅UNIT_REF使用")
	private String unitRefMode;

	@Schema(description = "来源类型")
	private String sourceType;

	@Schema(description = "来源引用")
	private String sourceReference;

	@Schema(description = "是否内置，1是0否")
	private String isBuiltin;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "命名空间ID")
	private Long namespaceId;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "平台治理备注")
	private String remarks;

	@Schema(description = "安全级别编码，默认INTERNAL")
	private String securityLevelCode;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改人")
	private String updateBy;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;

}
