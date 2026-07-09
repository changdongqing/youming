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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 单位条目。
 *
 * @author youming
 */
@Data
@TableName("ont_unit")
@Schema(description = "单位条目")
@EqualsAndHashCode(callSuper = true)
public class OntUnit extends Model<OntUnit> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "单位ID")
	private Long id;

	@NotNull(message = "单位分类不能为空")
	@Schema(description = "分类ID")
	private Long categoryId;

	@NotBlank(message = "单位编码不能为空")
	@Schema(description = "单位编码")
	private String unitCode;

	@NotBlank(message = "单位符号不能为空")
	@Schema(description = "单位符号")
	private String unitSymbol;

	@NotBlank(message = "单位名称不能为空")
	@Schema(description = "单位名称")
	private String unitName;

	@Schema(description = "是否基准单位，1是0否")
	private String isBaseUnit;

	@Schema(description = "是否内置，1是0否")
	private String isBuiltin;

	@Schema(description = "换算乘系数")
	private BigDecimal factor;

	@Schema(description = "换算偏移")
	private BigDecimal offsetValue;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "命名空间")
	private String namespace;

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
