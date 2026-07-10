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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 实体类型。
 *
 * @author youming
 */
@Data
@TableName("ont_entity_type")
@Schema(description = "实体类型")
@EqualsAndHashCode(callSuper = true)
public class OntEntityType extends Model<OntEntityType> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "实体类型ID")
	private Long id;

	@NotBlank(message = "IRI不能为空")
	@Schema(description = "全局唯一IRI")
	private String iri;

	@NotBlank(message = "英文名称不能为空")
	@Schema(description = "英文名称（IRI本地标识符）")
	private String name;

	@Schema(description = "定义")
	private String definition;

	@Schema(description = "是否抽象类，1是0否")
	private String isAbstract;

	@Schema(description = "是否内置，1是0否")
	private String isBuiltin;

	@NotNull(message = "命名空间不能为空")
	@Schema(description = "命名空间ID")
	private Long namespaceId;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "备注")
	private String remarks;

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

	/**
	 * 父类ID列表（非持久化，用于新增/修改时传入继承关系）。
	 */
	@TableField(exist = false)
	@Schema(description = "父类ID列表")
	private java.util.List<Long> parentIds;

	/**
	 * 中文标签（非持久化，用于新增/修改时传入标签）。
	 */
	@TableField(exist = false)
	@Schema(description = "中文标签")
	private String label;

}
