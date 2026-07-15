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
 * 实体映射表（18-04 §3）。
 * <p>
 * 配置"源对象记录 → 本体实体实例"的确定性映射规则，包括稳定身份、IRI模板、标签模板、增量游标和删除策略。
 *
 * @author youming
 */
@Data
@TableName("ont_entity_mapping")
@Schema(description = "实体映射")
@EqualsAndHashCode(callSuper = true)
public class OntEntityMapping extends Model<OntEntityMapping> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "实体映射ID")
	private Long id;

	@Schema(description = "映射版本ID")
	private Long mappingVersionId;

	@Schema(description = "映射编码")
	private String mappingCode;

	@Schema(description = "映射名称")
	private String mappingName;

	@Schema(description = "数据源ID")
	private Long sourceId;

	@Schema(description = "源Schema名")
	private String sourceSchema;

	@Schema(description = "源对象名")
	private String sourceObject;

	@Schema(description = "源对象类型: TABLE / VIEW")
	private String sourceObjectType;

	@Schema(description = "目标实体类型ID")
	private Long targetEntityTypeId;

	@Schema(description = "目标命名空间ID")
	private Long targetNamespaceId;

	@Schema(description = "键列配置JSONB")
	private String keyColumns;

	@Schema(description = "IRI模板")
	private String iriTemplate;

	@Schema(description = "标签模板")
	private String labelTemplate;

	@Schema(description = "过滤条件DSL JSONB")
	private String filterDsl;

	@Schema(description = "增量列名")
	private String incrementalColumn;

	@Schema(description = "增量类型: TIMESTAMP / NUMERIC")
	private String incrementalType;

	@Schema(description = "源删除标记列名")
	private String sourceDeleteFlagColumn;

	@Schema(description = "源删除值JSONB")
	private String sourceDeleteValues;

	@Schema(description = "删除策略: IGNORE / MARK_INACTIVE / SOFT_DELETE / BLOCK_AND_REVIEW")
	private String deleteStrategy;

	@Schema(description = "失活属性ID")
	private Long inactivePropertyId;

	@Schema(description = "失活字面值")
	private String inactiveLiteralValue;

	@Schema(description = "冲突策略: SOURCE_WINS / MANUAL_WINS / REJECT_CONFLICT")
	private String conflictPolicy;

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
