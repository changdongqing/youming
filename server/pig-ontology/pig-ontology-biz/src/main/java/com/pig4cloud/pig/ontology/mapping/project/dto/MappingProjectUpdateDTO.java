/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 映射工程治理字段更新 DTO（18-03 §8.5）。
 * <p>
 * 仅允许更新治理字段（名称、描述、调度、安全级别、备注），不能修改映射编码和本体绑定。
 *
 * @author youming
 */
@Data
@Schema(description = "映射工程更新")
public class MappingProjectUpdateDTO {

	@NotNull(message = "工程ID不能为空")
	@Schema(description = "工程ID")
	private Long id;

	@Size(max = 128, message = "映射名称长度不能超过128")
	@Schema(description = "映射名称")
	private String mappingName;

	@Schema(description = "描述")
	private String description;

	@Size(max = 32, message = "安全级别编码长度不能超过32")
	@Schema(description = "安全级别编码")
	private String securityLevelCode;

	@Schema(description = "是否启用调度: 0否 1是")
	@Pattern(regexp = "^[01]$", message = "调度启用标记必须为0或1")
	private String scheduleEnabled;

	@Size(max = 128, message = "Cron表达式长度不能超过128")
	@Schema(description = "调度Cron表达式")
	private String scheduleCron;

	@Schema(description = "调度运行类型: FULL / INCREMENTAL")
	@Pattern(regexp = "^(FULL|INCREMENTAL)$", message = "调度运行类型必须为FULL或INCREMENTAL")
	private String scheduleRunType;

	@Schema(description = "执行主体类型: USER / ROLE")
	@Pattern(regexp = "^(USER|ROLE)$", message = "执行主体类型必须为USER或ROLE")
	private String executionSubjectType;

	@Schema(description = "执行主体ID")
	private Long executionSubjectId;

	@Size(max = 255, message = "备注长度不能超过255")
	@Schema(description = "备注")
	private String remarks;

	@Schema(description = "工程修订号（乐观锁，必填）")
	@NotNull(message = "工程修订号不能为空")
	private Long revision;

}
