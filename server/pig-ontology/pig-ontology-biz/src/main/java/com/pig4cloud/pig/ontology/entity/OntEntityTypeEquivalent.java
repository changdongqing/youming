/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实体类型等价关系。
 *
 * @author youming
 */
@Data
@TableName("ont_entity_type_equivalent")
@Schema(description = "实体类型等价关系")
public class OntEntityTypeEquivalent {

	@Schema(description = "实体类型ID")
	private Long entityTypeId;

	@Schema(description = "等价实体类型ID")
	private Long equivalentId;

}
