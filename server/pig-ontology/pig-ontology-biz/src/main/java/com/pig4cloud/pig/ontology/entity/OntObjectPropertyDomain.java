/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 对象属性定义域关联。
 *
 * @author youming
 */
@Data
@TableName("ont_object_property_domain")
@Schema(description = "对象属性定义域")
public class OntObjectPropertyDomain {

	@Schema(description = "对象属性ID")
	private Long objectPropertyId;

	@Schema(description = "实体类型ID")
	private Long entityTypeId;

	@Schema(description = "排序")
	private Integer sortOrder;

}
