/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实体类型继承关系。
 *
 * @author youming
 */
@Data
@TableName("ont_entity_type_hierarchy")
@Schema(description = "实体类型继承关系")
public class OntEntityTypeHierarchy {

	@Schema(description = "父类实体类型ID")
	private Long parentId;

	@Schema(description = "子类实体类型ID")
	private Long childId;

}
