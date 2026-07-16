/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.entity;

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
import com.pig4cloud.pig.common.data.handler.StringToJsonbTypeHandler;

/**
 * 数据策略例外规则。
 *
 * @author youming
 */
@Data
@TableName("ont_data_policy_rule")
@Schema(description = "数据策略例外规则")
@EqualsAndHashCode(callSuper = true)
public class OntDataPolicyRule extends Model<OntDataPolicyRule> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "规则ID")
	private Long id;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "主体类型：USER/ROLE/DEPT")
	private String subjectType;

	@Schema(description = "主体ID")
	private Long subjectId;

	@Schema(description = "资源类型：ENTITY_TYPE/DATA_PROPERTY/OBJECT_PROPERTY")
	private String resourceType;

	@Schema(description = "资源ID")
	private Long resourceId;

	@Schema(description = "动作：VIEW/EDIT/EXPORT/SPARQL")
	private String action;

	@Schema(description = "效果：ALLOW/MASK/DENY")
	private String effect;

	@Schema(description = "脱敏类型，effect=MASK时必填")
	private String maskType;

	@Schema(description = "脱敏参数JSON")
	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	private String maskParameter;

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
