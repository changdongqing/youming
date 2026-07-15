/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 关系映射创建 DTO（18-05 §13）。
 *
 * @author youming
 */
@Data
@Schema(description = "关系映射创建")
public class RelationMappingCreateDTO {

	@NotBlank(message = "映射编码不能为空")
	@Size(max = 64, message = "映射编码长度不能超过64")
	@Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "映射编码须以字母开头，仅含字母、数字、下划线")
	@Schema(description = "映射编码")
	private String mappingCode;

	@NotBlank(message = "映射名称不能为空")
	@Size(max = 128, message = "映射名称长度不能超过128")
	@Schema(description = "映射名称")
	private String mappingName;

	@NotBlank(message = "关系模式不能为空")
	@Pattern(regexp = "^(FOREIGN_KEY|SELF_REFERENCE|JOIN_TABLE)$",
			message = "关系模式必须为FOREIGN_KEY/SELF_REFERENCE/JOIN_TABLE")
	@Schema(description = "关系模式: FOREIGN_KEY / SELF_REFERENCE / JOIN_TABLE")
	private String relationMode;

	@NotNull(message = "对象属性ID不能为空")
	@Schema(description = "对象属性ID")
	private Long objectPropertyId;

	@NotNull(message = "主体实体映射ID不能为空")
	@Schema(description = "主体实体映射ID")
	private Long subjectEntityMappingId;

	@NotNull(message = "客体实体映射ID不能为空")
	@Schema(description = "客体实体映射ID")
	private Long objectEntityMappingId;

	@NotNull(message = "数据源ID不能为空")
	@Schema(description = "数据源ID")
	private Long sourceId;

	@NotBlank(message = "源Schema名不能为空")
	@Size(max = 128, message = "源Schema名长度不能超过128")
	@Schema(description = "源Schema名")
	private String sourceSchema;

	@NotBlank(message = "源对象名不能为空")
	@Size(max = 128, message = "源对象名长度不能超过128")
	@Schema(description = "源对象名")
	private String sourceObject;

	@NotBlank(message = "主体键映射不能为空")
	@Schema(description = "主体键映射JSONB")
	private String subjectKeyMapping;

	@NotBlank(message = "客体键映射不能为空")
	@Schema(description = "客体键映射JSONB")
	private String objectKeyMapping;

	@NotBlank(message = "关系键列不能为空")
	@Schema(description = "关系键列JSONB")
	private String relationKeyColumns;

	@Schema(description = "过滤条件DSL JSONB")
	private String filterDsl;

	@Schema(description = "缺失目标策略，默认PENDING")
	@Pattern(regexp = "^(PENDING|SKIP|FAIL_RECORD)$",
			message = "缺失目标策略必须为PENDING/SKIP/FAIL_RECORD")
	private String missingTargetPolicy;

	@Schema(description = "删除策略，默认REMOVE_ASSERTION")
	@Pattern(regexp = "^(REMOVE_ASSERTION|KEEP_ASSERTION|BLOCK_AND_REVIEW)$",
			message = "删除策略必须为REMOVE_ASSERTION/KEEP_ASSERTION/BLOCK_AND_REVIEW")
	private String deleteStrategy;

	@Schema(description = "所有权策略，默认SOURCE_WINS")
	@Pattern(regexp = "^(SOURCE_WINS|MANUAL_WINS|REJECT_CONFLICT)$",
			message = "所有权策略必须为SOURCE_WINS/MANUAL_WINS/REJECT_CONFLICT")
	private String ownershipPolicy;

	@Schema(description = "同步顺序，默认1000")
	private Integer syncOrder;

	@Schema(description = "是否启用: 0否 1是，默认1")
	@Pattern(regexp = "^[01]$", message = "启用标记必须为0或1")
	private String enabled;

	@Schema(description = "描述")
	private String description;

	@NotNull(message = "乐观锁revision不能为空")
	@Schema(description = "乐观锁revision（默认0）")
	private Long revision;

}
