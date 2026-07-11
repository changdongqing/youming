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
 * 实例对象属性断言。
 *
 * @author youming
 */
@Data
@TableName("ont_instance_object_relation")
@Schema(description = "实例对象属性断言")
@EqualsAndHashCode(callSuper = true)
public class OntInstanceObjectRelation extends Model<OntInstanceObjectRelation> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "断言ID")
	private Long id;

	@Schema(description = "主体实例ID")
	private Long subjectInstanceId;

	@Schema(description = "对象属性ID（谓词）")
	private Long objectPropertyId;

	@Schema(description = "客体类型：INSTANCE/ENTITY_TYPE")
	private String objectKind;

	@Schema(description = "客体实例ID，INSTANCE时必填")
	private Long objectInstanceId;

	@Schema(description = "客体实体类型ID，ENTITY_TYPE时必填")
	private Long objectEntityTypeId;

	@Schema(description = "多值顺序")
	private Integer sortOrder;

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
