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

/**
 * 本体版本表，存储不可变 Schema 快照及发布元数据。
 *
 * @author youming
 */
@Data
@TableName("ont_ontology_version")
@Schema(description = "本体版本")
@EqualsAndHashCode(callSuper = true)
public class OntOntologyVersion extends Model<OntOntologyVersion> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键ID")
	private Long id;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "语义版本号 MAJOR.MINOR.PATCH")
	private String versionNumber;

	@Schema(description = "版本IRI")
	private String versionIri;

	@Schema(description = "前序版本ID")
	private Long priorVersionId;

	@Schema(description = "恢复来源版本ID")
	private Long restoreSourceVersionId;

	@Schema(description = "兼容性：PATCH_ONLY/BACKWARD_COMPATIBLE/BREAKING")
	private String compatibility;

	@Schema(description = "发布状态：PREPARED/MIGRATING/PUBLISHED/FAILED/CANCELLED")
	private String releaseStatus;

	@Schema(description = "发布说明")
	private String releaseNotes;

	@Schema(description = "快照格式版本")
	private Integer snapshotFormatVersion;

	@Schema(description = "规范化全量Schema快照JSONB")
	private String schemaSnapshot;

	@Schema(description = "快照SHA-256哈希")
	private String snapshotHash;

	@Schema(description = "与priorVersion的差异摘要JSONB")
	private String diffSummary;

	@Schema(description = "实例迁移计划JSONB，BREAKING时必填")
	private String migrationPlan;

	@Schema(description = "关联校验报告ID")
	private Long validationReportId;

	@Schema(description = "prepare时记录的工作区修订号")
	private Long workspaceRevision;

	@Schema(description = "发布人")
	private String publishedBy;

	@Schema(description = "发布时间")
	private LocalDateTime publishedAt;

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
