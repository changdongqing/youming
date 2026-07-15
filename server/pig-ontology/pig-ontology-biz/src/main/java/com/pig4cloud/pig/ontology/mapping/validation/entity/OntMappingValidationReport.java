/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.entity;

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
 * 映射校验报告表（18-06 §4）。
 * <p>
 * 记录一次映射版本校验的报告状态、触发类型、配置快照指纹、本体版本绑定和计数统计。
 * 校验结果必须绑定配置 revision、configHash、本体版本和元数据 hash。
 *
 * @author youming
 */
@Data
@TableName("ont_mapping_validation_report")
@Schema(description = "映射校验报告")
@EqualsAndHashCode(callSuper = true)
public class OntMappingValidationReport extends Model<OntMappingValidationReport> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "报告ID")
	private Long id;

	@Schema(description = "映射版本ID")
	private Long mappingVersionId;

	@Schema(description = "报告状态: RUNNING / PASSED / FAILED / CANCELLED")
	private String reportStatus;

	@Schema(description = "触发类型: MANUAL / PUBLISH_RECHECK / SYSTEM")
	private String triggerType;

	@Schema(description = "配置版本号")
	private Long configRevision;

	@Schema(description = "候选配置哈希")
	private String candidateConfigHash;

	@Schema(description = "本体版本ID")
	private Long ontologyVersionId;

	@Schema(description = "工作区修订号")
	private Long workspaceRevision;

	@Schema(description = "元数据哈希摘要")
	private String metadataHashSummary;

	@Schema(description = "样本数量")
	private Integer sampleSize;

	@Schema(description = "VIOLATION数量")
	private Integer violationCount;

	@Schema(description = "WARNING数量")
	private Integer warningCount;

	@Schema(description = "INFO数量")
	private Integer infoCount;

	@Schema(description = "摘要JSON")
	private String summaryJson;

	@Schema(description = "开始时间")
	private LocalDateTime startedAt;

	@Schema(description = "完成时间")
	private LocalDateTime completedAt;

	@Schema(description = "请求人")
	private String requestedBy;

	@Schema(description = "追踪ID")
	private String traceId;

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
