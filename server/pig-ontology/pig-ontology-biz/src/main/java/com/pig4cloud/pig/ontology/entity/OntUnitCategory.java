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
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 单位分类。
 *
 * @author youming
 */
@Data
@TableName("ont_unit_category")
@Schema(description = "单位分类")
@EqualsAndHashCode(callSuper = true)
public class OntUnitCategory extends Model<OntUnitCategory> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "分类ID")
	private Long id;

	@NotBlank(message = "分类编码不能为空")
	@Schema(description = "分类编码")
	private String categoryCode;

	@NotBlank(message = "分类名称不能为空")
	@Schema(description = "分类名称")
	private String categoryName;

	@NotBlank(message = "基准单位符号不能为空")
	@Schema(description = "基准单位符号")
	private String baseUnitSymbol;

	@Schema(description = "是否内置，1是0否")
	private String isBuiltin;

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

}
