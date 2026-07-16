/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.entity;

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
 * 实例迁移作业表，记录 BREAKING 版本发布时的实例迁移执行。
 *
 * @author youming
 */
@Data
@TableName("ont_instance_migration_job")
@Schema(description = "实例迁移作业")
@EqualsAndHashCode(callSuper = true)
public class OntInstanceMigrationJob extends Model<OntInstanceMigrationJob> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键ID")
	private Long id;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "候选版本ID")
	private Long candidateVersionId;

	@Schema(description = "作业状态：PENDING/RUNNING/SUCCEEDED/PARTIAL_FAILED/FAILED/CANCELLED")
	private String status;

	@Schema(description = "迁移计划快照JSONB")
	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	private String planSnapshot;

	@Schema(description = "游标数据JSONB，记录上次处理位置")
	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	private String cursorData;

	@Schema(description = "待迁移实例总数")
	private Long totalCount;

	@Schema(description = "已处理实例数")
	private Long processedCount;

	@Schema(description = "成功实例数")
	private Long successCount;

	@Schema(description = "失败实例数")
	private Long failedCount;

	@Schema(description = "错误摘要文本")
	private String errorSummary;

	@Schema(description = "开始执行时间")
	private LocalDateTime startedAt;

	@Schema(description = "完成时间")
	private LocalDateTime completedAt;

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
