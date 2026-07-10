/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntDataPropertyEnum;
import com.pig4cloud.pig.ontology.entity.OntDataPropertyLabel;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntUnitCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据属性详情。
 *
 * @author youming
 */
@Data
@Schema(description = "数据属性详情")
public class OntDataPropertyDetailVO {

	private OntDataProperty dataProperty;

	private List<OntDataPropertyLabel> labels = new ArrayList<>();

	private List<OntDataPropertyEnum> enums = new ArrayList<>();

	private OntEntityType domainEntityType;

	private OntNamespace namespace;

	private OntUnitCategory unitCategory;

	@Schema(description = "显示名（优先取别名，其次取name）")
	private String displayName;

}
