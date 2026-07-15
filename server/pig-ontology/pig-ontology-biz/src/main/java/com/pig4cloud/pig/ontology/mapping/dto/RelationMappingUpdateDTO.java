/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 关系映射修改 DTO（18-05 §13）。
 * <p>
 * 仅DRAFT版本可修改，带乐观锁revision。
 *
 * @author youming
 */
@Data
@Schema(description = "关系映射修改")
public class RelationMappingUpdateDTO {

	@NotNull(message = "ID不能为空")
	@Schema(description = "关系映射ID")
	private Long id;

	@Schema(description = "映射名称")
	private String mappingName;

	@Schema(description = "关系模式")
	private String relationMode;

	@Schema(description = "对象属性ID")
	private Long objectPropertyId;

	@Schema(description = "主体实体映射ID")
	private Long subjectEntityMappingId;

	@Schema(description = "客体实体映射ID")
	private Long objectEntityMappingId;

	@Schema(description = "主体键映射JSONB")
	private String subjectKeyMapping;

	@Schema(description = "客体键映射JSONB")
	private String objectKeyMapping;

	@Schema(description = "关系键列JSONB")
	private String relationKeyColumns;

	@Schema(description = "过滤条件DSL JSONB")
	private String filterDsl;

	@Schema(description = "缺失目标策略")
	private String missingTargetPolicy;

	@Schema(description = "删除策略")
	private String deleteStrategy;

	@Schema(description = "所有权策略")
	private String ownershipPolicy;

	@Schema(description = "同步顺序")
	private Integer syncOrder;

	@Schema(description = "是否启用: 0否 1是")
	private String enabled;

	@Schema(description = "描述")
	private String description;

	@NotNull(message = "乐观锁revision不能为空")
	@Schema(description = "乐观锁revision")
	private Long revision;

}
