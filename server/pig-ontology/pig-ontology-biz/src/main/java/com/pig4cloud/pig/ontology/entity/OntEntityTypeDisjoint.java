/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 实体类型不相交关系。
 *
 * @author youming
 */
@Data
@TableName("ont_entity_type_disjoint")
@Schema(description = "实体类型不相交关系")
public class OntEntityTypeDisjoint {

	@Schema(description = "实体类型A")
	private Long typeA;

	@Schema(description = "实体类型B")
	private Long typeB;

}
