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
 * 日期顺序校验执行器（SHACL_SPARQL 模式）。
 * <p>
 * 对应规则 970004 GB8_EFFECTIVE_DATE_ORDER。
 * 通过 SHACL SPARQL 约束校验实施日期不早于发布日期。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
public class DateOrderExecutor extends AbstractShaclExecutor {

	public DateOrderExecutor(OntEntityTypeMapper entityTypeMapper,
			OntEntityTypeHierarchyMapper hierarchyMapper,
			ShaclModelBuilder shaclBuilder,
			DataGraphBuilder dataGraphBuilder) {
		super(entityTypeMapper, hierarchyMapper, shaclBuilder, dataGraphBuilder);
	}

	@Override
	public String getExecutorCode() {
		return "DATE_ORDER";
	}

	@Override
	public String getValidationMode() {
		return "SHACL_SPARQL";
	}

}
