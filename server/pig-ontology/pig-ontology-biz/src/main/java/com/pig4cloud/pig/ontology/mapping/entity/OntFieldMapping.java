/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.entity;

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
 * 字段映射表（18-04 §6）。
 * <p>
 * 配置"源列/常量 → 本体数据属性值"的确定性映射规则，包括转换器、空值处理、多值和所有权策略。
 * 字段映射无乐观锁（无 revision 列），生命周期完全依附于父实体映射（ON DELETE CASCADE）。
 *
 * @author youming
 */
@Data
@TableName("ont_field_mapping")
@Schema(description = "字段映射")
@EqualsAndHashCode(callSuper = true)
public class OntFieldMapping extends Model<OntFieldMapping> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "字段映射ID")
	private Long id;

	@Schema(description = "父实体映射ID")
	private Long entityMappingId;

	@Schema(description = "字段映射编码")
	private String fieldMappingCode;

	@Schema(description = "字段映射名称")
	private String fieldMappingName;

	@Schema(description = "目标数据属性ID")
	private Long targetDataPropertyId;

	@Schema(description = "源列名（source_kind=COLUMN时必填）")
	private String sourceColumn;

	@Schema(description = "来源类型: COLUMN / CONSTANT")
	private String sourceKind;

	@Schema(description = "常量值（source_kind=CONSTANT时必填）")
	private String constantValue;

	@Schema(description = "常量字面量类型")
	private String constantLiteralType;

	@Schema(description = "常量单位ID")
	private Long constantUnitId;

	@Schema(description = "转换器编码，默认IDENTITY")
	private String transformer;

	@Schema(description = "转换器参数JSONB")
	private String transformerParams;

	@Schema(description = "空值处理: SKIP_NULL / USE_DEFAULT / REJECT_NULL")
	private String nullHandling;

	@Schema(description = "默认值")
	private String defaultValue;

	@Schema(description = "默认值字面量类型")
	private String defaultLiteralType;

	@Schema(description = "多值策略: SINGLE / FIRST / LAST / ALL")
	private String multiValueStrategy;

	@Schema(description = "固定单位ID")
	private Long unitId;

	@Schema(description = "所有权策略: SOURCE_WINS / MANUAL_WINS / REJECT_CONFLICT")
	private String ownershipPolicy;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "是否启用: 0否 1是")
	private String enabled;

	@Schema(description = "描述")
	private String description;

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
