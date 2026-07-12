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
 * 枚举成员校验执行器（SHACL_CORE 模式）。
 * <p>
 * 对应规则 970005 GB8_STANDARD_STATUS_ENUM 和 970006 GB8_STANDARD_CONSTRAINT_TYPE。
 * 通过 SHACL sh:in 约束校验数据属性值是否在枚举集合内。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
public class EnumMembershipExecutor extends AbstractShaclExecutor {

	public EnumMembershipExecutor(OntEntityTypeMapper entityTypeMapper,
			OntEntityTypeHierarchyMapper hierarchyMapper,
			ShaclModelBuilder shaclBuilder,
			DataGraphBuilder dataGraphBuilder) {
		super(entityTypeMapper, hierarchyMapper, shaclBuilder, dataGraphBuilder);
	}

	@Override
	public String getExecutorCode() {
		return "ENUM_MEMBERSHIP";
	}

	@Override
	public String getValidationMode() {
		return "SHACL_CORE";
	}

}
