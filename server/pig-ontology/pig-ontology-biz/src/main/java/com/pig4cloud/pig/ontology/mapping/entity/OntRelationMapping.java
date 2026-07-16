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
import com.pig4cloud.pig.common.data.handler.StringToJsonbTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 关系映射表（18-05 §3）。
 * <p>
 * 将源数据库中的外键、自关联和中间表关系转换为 Ontology 对象属性断言的规则。
 * 三种关系模式：FOREIGN_KEY / SELF_REFERENCE / JOIN_TABLE。
 *
 * @author youming
 */
@Data
@TableName(value = "ont_relation_mapping", autoResultMap = true)
@Schema(description = "关系映射")
@EqualsAndHashCode(callSuper = true)
public class OntRelationMapping extends Model<OntRelationMapping> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "关系映射ID")
	private Long id;

	@Schema(description = "映射版本ID")
	private Long mappingVersionId;

	@Schema(description = "映射编码")
	private String mappingCode;

	@Schema(description = "映射名称")
	private String mappingName;

	@Schema(description = "关系模式: FOREIGN_KEY / SELF_REFERENCE / JOIN_TABLE")
	private String relationMode;

	@Schema(description = "对象属性ID")
	private Long objectPropertyId;

	@Schema(description = "主体实体映射ID")
	private Long subjectEntityMappingId;

	@Schema(description = "客体实体映射ID")
	private Long objectEntityMappingId;

	@Schema(description = "数据源ID")
	private Long sourceId;

	@Schema(description = "源Schema名")
	private String sourceSchema;

	@Schema(description = "源对象名")
	private String sourceObject;

	@Schema(description = "主体键映射JSONB")
	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	private String subjectKeyMapping;

	@Schema(description = "客体键映射JSONB")
	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	private String objectKeyMapping;

	@Schema(description = "关系键列JSONB")
	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	private String relationKeyColumns;

	@Schema(description = "过滤条件DSL JSONB")
	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	private String filterDsl;

	@Schema(description = "缺失目标策略: PENDING / SKIP / FAIL_RECORD")
	private String missingTargetPolicy;

	@Schema(description = "删除策略: REMOVE_ASSERTION / KEEP_ASSERTION / BLOCK_AND_REVIEW")
	private String deleteStrategy;

	@Schema(description = "所有权策略: SOURCE_WINS / MANUAL_WINS / REJECT_CONFLICT")
	private String ownershipPolicy;

	@Schema(description = "同步顺序")
	private Integer syncOrder;

	@Schema(description = "是否启用: 0否 1是")
	private String enabled;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "修订号（乐观锁）")
	private Long revision;

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
