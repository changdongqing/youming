/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 映射工程创建 DTO（18-03 §8.1）。
 * <p>
 * 新建工程同时创建初始 DRAFT 版本 0.1.0。
 *
 * @author youming
 */
@Data
@Schema(description = "映射工程创建")
public class MappingProjectCreateDTO {

	@NotBlank(message = "映射编码不能为空")
	@Size(max = 64, message = "映射编码长度不能超过64")
	@Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "映射编码须以字母开头，仅含字母、数字、下划线")
	@Schema(description = "映射编码")
	private String mappingCode;

	@NotBlank(message = "映射名称不能为空")
	@Size(max = 128, message = "映射名称长度不能超过128")
	@Schema(description = "映射名称")
	private String mappingName;

	@NotNull(message = "本体工程ID不能为空")
	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@NotNull(message = "默认命名空间ID不能为空")
	@Schema(description = "默认命名空间ID")
	private Long defaultNamespaceId;

	@Schema(description = "描述")
	private String description;

	@NotBlank(message = "本体版本约束不能为空")
	@Size(max = 128, message = "本体版本约束长度不能超过128")
	@Schema(description = "本体版本兼容约束，如 >=1.0.0 <2.0.0")
	private String ontologyVersionConstraint;

	@Size(max = 32, message = "安全级别编码长度不能超过32")
	@Schema(description = "安全级别编码，默认INTERNAL")
	private String securityLevelCode;

	@Schema(description = "调度Cron表达式（可选）")
	@Size(max = 128, message = "Cron表达式长度不能超过128")
	private String scheduleCron;

	@Schema(description = "调度运行类型: FULL / INCREMENTAL（可选）")
	@Pattern(regexp = "^(FULL|INCREMENTAL)$", message = "调度运行类型必须为FULL或INCREMENTAL")
	private String scheduleRunType;

	@Schema(description = "执行主体类型: USER / ROLE（可选）")
	@Pattern(regexp = "^(USER|ROLE)$", message = "执行主体类型必须为USER或ROLE")
	private String executionSubjectType;

	@Schema(description = "执行主体ID（可选）")
	private Long executionSubjectId;

	@Schema(description = "备注")
	@Size(max = 255, message = "备注长度不能超过255")
	private String remarks;

	@Schema(description = "初始版本发布说明")
	private String releaseNotes;

}
