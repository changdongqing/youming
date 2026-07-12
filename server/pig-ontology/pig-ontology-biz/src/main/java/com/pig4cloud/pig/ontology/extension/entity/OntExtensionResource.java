/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.entity;

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
 * 扩展资源关联。
 *
 * @author youming
 */
@Data
@TableName("ont_extension_resource")
@Schema(description = "扩展资源关联")
@EqualsAndHashCode(callSuper = true)
public class OntExtensionResource extends Model<OntExtensionResource> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "关联记录ID")
	private Long id;

	@Schema(description = "扩展模块ID")
	private Long moduleId;

	@Schema(description = "资源类型: ENTITY_TYPE/DATA_PROPERTY/OBJECT_PROPERTY/AXIOM_RULE/UNIT")
	private String resourceType;

	@Schema(description = "资源记录ID")
	private Long resourceId;

	@Schema(description = "资源IRI（冗余快照）")
	private String resourceIri;

	@Schema(description = "资源名称（冗余快照）")
	private String resourceName;

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
