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
 * 实体映射创建 DTO（18-04 §12）。
 *
 * @author youming
 */
@Data
@Schema(description = "实体映射创建")
public class EntityMappingCreateDTO {

	@NotBlank(message = "映射编码不能为空")
	@Size(max = 64, message = "映射编码长度不能超过64")
	@Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "映射编码须以字母开头，仅含字母、数字、下划线")
	@Schema(description = "映射编码")
	private String mappingCode;

	@NotBlank(message = "映射名称不能为空")
	@Size(max = 128, message = "映射名称长度不能超过128")
	@Schema(description = "映射名称")
	private String mappingName;

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

	@Schema(description = "源对象类型: TABLE / VIEW，默认TABLE")
	@Pattern(regexp = "^(TABLE|VIEW)$", message = "源对象类型必须为TABLE或VIEW")
	private String sourceObjectType;

	@NotNull(message = "目标实体类型ID不能为空")
	@Schema(description = "目标实体类型ID")
	private Long targetEntityTypeId;

	@NotNull(message = "目标命名空间ID不能为空")
	@Schema(description = "目标命名空间ID")
	private Long targetNamespaceId;

	@NotBlank(message = "键列配置不能为空")
	@Schema(description = "键列配置JSONB，如 [{\"column\":\"user_id\",\"order\":1,\"normalizer\":\"LONG\"}]")
	private String keyColumns;

	@NotBlank(message = "IRI模板不能为空")
	@Size(max = 512, message = "IRI模板长度不能超过512")
	@Schema(description = "IRI模板，如 user-account/{user_id|url}")
	private String iriTemplate;

	@Size(max = 512, message = "标签模板长度不能超过512")
	@Schema(description = "标签模板，如 {name}（{username}）")
	private String labelTemplate;

	@Schema(description = "过滤条件DSL JSONB")
	private String filterDsl;

	@Size(max = 128, message = "增量列名长度不能超过128")
	@Schema(description = "增量列名")
	private String incrementalColumn;

	@Schema(description = "增量类型: TIMESTAMP / NUMERIC")
	@Pattern(regexp = "^(TIMESTAMP|NUMERIC)$", message = "增量类型必须为TIMESTAMP或NUMERIC")
	private String incrementalType;

	@Size(max = 128, message = "源删除标记列名长度不能超过128")
	@Schema(description = "源删除标记列名")
	private String sourceDeleteFlagColumn;

	@Schema(description = "源删除值JSONB，如 [\"1\",\"true\"]")
	private String sourceDeleteValues;

	@Schema(description = "删除策略，默认MARK_INACTIVE")
	@Pattern(regexp = "^(IGNORE|MARK_INACTIVE|SOFT_DELETE|BLOCK_AND_REVIEW)$",
			message = "删除策略必须为IGNORE/MARK_INACTIVE/SOFT_DELETE/BLOCK_AND_REVIEW")
	private String deleteStrategy;

	@Schema(description = "失活属性ID")
	private Long inactivePropertyId;

	@Size(max = 255, message = "失活字面值长度不能超过255")
	@Schema(description = "失活字面值")
	private String inactiveLiteralValue;

	@Schema(description = "冲突策略，默认SOURCE_WINS")
	@Pattern(regexp = "^(SOURCE_WINS|MANUAL_WINS|REJECT_CONFLICT)$",
			message = "冲突策略必须为SOURCE_WINS/MANUAL_WINS/REJECT_CONFLICT")
	private String conflictPolicy;

	@Schema(description = "同步顺序，默认0")
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
