/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.vo;

import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 映射工程详情 VO（18-03 §11）。
 * <p>
 * 包含工程基本信息、活跃版本摘要和草稿版本摘要，不暴露凭证。
 *
 * @author youming
 */
@Data
@Schema(description = "映射工程详情")
public class MappingProjectVO {

	@Schema(description = "工程ID")
	private Long id;

	@Schema(description = "映射编码")
	private String mappingCode;

	@Schema(description = "映射名称")
	private String mappingName;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "默认命名空间ID")
	private Long defaultNamespaceId;

	@Schema(description = "活跃版本ID")
	private Long activeVersionId;

	@Schema(description = "工程状态")
	private String projectStatus;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "是否启用调度")
	private String scheduleEnabled;

	@Schema(description = "调度Cron表达式")
	private String scheduleCron;

	@Schema(description = "调度运行类型")
	private String scheduleRunType;

	@Schema(description = "执行主体类型")
	private String executionSubjectType;

	@Schema(description = "执行主体ID")
	private Long executionSubjectId;

	@Schema(description = "安全级别编码")
	private String securityLevelCode;

	@Schema(description = "工程修订号")
	private Long revision;

	@Schema(description = "最近作业ID")
	private Long lastJobId;

	@Schema(description = "备注")
	private String remarks;

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

	@Schema(description = "活跃版本摘要")
	private VersionSummary activeVersion;

	@Schema(description = "草稿版本摘要")
	private VersionSummary draftVersion;

	/**
	 * 版本摘要，用于工程详情中展示活跃版本和草稿版本概况。
	 */
	@Data
	@Schema(description = "版本摘要")
	public static class VersionSummary {

		@Schema(description = "版本ID")
		private Long id;

		@Schema(description = "版本号")
		private String versionNumber;

		@Schema(description = "版本状态")
		private String versionStatus;

		@Schema(description = "配置哈希")
		private String configHash;

		@Schema(description = "发布时间")
		private LocalDateTime publishedAt;

		@Schema(description = "创建时间")
		private LocalDateTime createTime;

	}

}
