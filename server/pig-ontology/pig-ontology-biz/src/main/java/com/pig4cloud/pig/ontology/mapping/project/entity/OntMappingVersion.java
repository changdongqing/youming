/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.entity;

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
 * 映射版本表（18-03 §4）。
 * <p>
 * 映射配置的不可变发布快照，管理版本状态机、配置快照、SHA-256哈希和本体版本兼容绑定。
 *
 * @author youming
 */
@Data
@TableName(value = "ont_mapping_version", autoResultMap = true)
@Schema(description = "映射版本")
@EqualsAndHashCode(callSuper = true)
public class OntMappingVersion extends Model<OntMappingVersion> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "版本ID")
	private Long id;

	@Schema(description = "映射工程ID")
	private Long mappingProjectId;

	@Schema(description = "版本号 MAJOR.MINOR.PATCH")
	private String versionNumber;

	@Schema(description = "版本状态: DRAFT / VALIDATING / VALIDATED / PUBLISHED / RETIRED")
	private String versionStatus;

	@Schema(description = "前序版本ID")
	private Long priorVersionId;

	@Schema(description = "本体版本兼容约束表达式")
	private String ontologyVersionConstraint;

	@Schema(description = "校验时的本体版本ID")
	private Long validatedOntologyVersionId;

	@Schema(description = "校验时的工作区修订号")
	private Long validatedWorkspaceRevision;

	@Schema(description = "配置快照JSONB")
	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	private String configSnapshot;

	@Schema(description = "配置SHA-256哈希")
	private String configHash;

	@Schema(description = "元数据依赖JSONB")
	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	private String metadataDependencies;

	@Schema(description = "校验报告ID")
	private Long validationReportId;

	@Schema(description = "校验摘要JSONB")
	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	private String validationSummary;

	@Schema(description = "发布说明")
	private String releaseNotes;

	@Schema(description = "发布人")
	private String publishedBy;

	@Schema(description = "发布时间")
	private LocalDateTime publishedAt;

	@Schema(description = "停用时间")
	private LocalDateTime retiredAt;

	@Schema(description = "版本修订号（乐观锁）")
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
