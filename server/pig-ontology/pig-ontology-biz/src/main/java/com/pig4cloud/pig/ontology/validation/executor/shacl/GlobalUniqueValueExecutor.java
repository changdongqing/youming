/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.executor.shacl;

import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.validation.model.DataGraphBuilder;
import com.pig4cloud.pig.ontology.validation.model.ShaclModelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 全局唯一值校验执行器（SHACL_SPARQL 模式）。
 * <p>
 * 对应规则 970002 GB8_INFO_UNIT_IDENTIFIER_UNIQUE 和 970003 GB8_STANDARD_NUMBER_UNIQUE。
 * 通过 SHACL SPARQL 约束校验数据属性值在本体工程内全局唯一。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
public class GlobalUniqueValueExecutor extends AbstractShaclExecutor {

	public GlobalUniqueValueExecutor(OntEntityTypeMapper entityTypeMapper,
			OntEntityTypeHierarchyMapper hierarchyMapper,
			ShaclModelBuilder shaclBuilder,
			DataGraphBuilder dataGraphBuilder) {
		super(entityTypeMapper, hierarchyMapper, shaclBuilder, dataGraphBuilder);
	}

	@Override
	public String getExecutorCode() {
		return "GLOBAL_UNIQUE_VALUE";
	}

	@Override
	public String getValidationMode() {
		return "SHACL_SPARQL";
	}

}
