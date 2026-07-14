/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.vo;

import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 映射版本详情 VO（18-03 §11）。
 * <p>
 * configSnapshot 按权限脱敏：管理员可查看完整快照，其他角色不返回。
 *
 * @author youming
 */
@Data
@Schema(description = "映射版本详情")
public class MappingVersionVO {

	@Schema(description = "版本ID")
	private Long id;

	@Schema(description = "映射工程ID")
	private Long mappingProjectId;

	@Schema(description = "版本号")
	private String versionNumber;

	@Schema(description = "版本状态")
	private String versionStatus;

	@Schema(description = "前序版本ID")
	private Long priorVersionId;

	@Schema(description = "本体版本兼容约束")
	private String ontologyVersionConstraint;

	@Schema(description = "校验时的本体版本ID")
	private Long validatedOntologyVersionId;

	@Schema(description = "校验时的工作区修订号")
	private Long validatedWorkspaceRevision;

	@Schema(description = "配置哈希")
	private String configHash;

	@Schema(description = "校验报告ID")
	private Long validationReportId;

	@JsonRawValue
	@Schema(description = "校验摘要JSON（可能为空）")
	private String validationSummary;

	@Schema(description = "发布说明")
	private String releaseNotes;

	@Schema(description = "发布人")
	private String publishedBy;

	@Schema(description = "发布时间")
	private LocalDateTime publishedAt;

	@Schema(description = "停用时间")
	private LocalDateTime retiredAt;

	@Schema(description = "版本修订号")
	private Long revision;

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

	@JsonRawValue
	@Schema(description = "配置快照JSON（管理员可见，不含凭证）")
	private String configSnapshot;

}
