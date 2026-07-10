/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.service.OntIriUniquenessService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 全局IRI唯一性校验实现。
 *
 * @author youming
 */
@Service
@AllArgsConstructor
public class OntIriUniquenessServiceImpl implements OntIriUniquenessService {

	private final OntNamespaceMapper namespaceMapper;

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	@Override
	public String checkIriConflict(String iri, String excludeTableName, Long excludeId) {
		// 1. 命名空间URI冲突
		long nsCount = namespaceMapper.selectCount(Wrappers.<OntNamespace>lambdaQuery()
			.eq(OntNamespace::getUri, iri));
		if (nsCount > 0) {
			return "IRI与已有命名空间URI冲突";
		}

		// 2. 实体类型IRI冲突
		long etCount = entityTypeMapper.selectCount(Wrappers.<OntEntityType>lambdaQuery()
			.eq(OntEntityType::getIri, iri)
			.ne(isSameTable(excludeTableName, "ont_entity_type"), OntEntityType::getId, excludeId));
		if (etCount > 0) {
			return "IRI与已有实体类型IRI冲突";
		}

		// 3. 数据属性IRI冲突
		long dpCount = dataPropertyMapper.selectCount(Wrappers.<OntDataProperty>lambdaQuery()
			.eq(OntDataProperty::getIri, iri)
			.ne(isSameTable(excludeTableName, "ont_data_property"), OntDataProperty::getId, excludeId));
		if (dpCount > 0) {
			return "IRI与已有数据属性IRI冲突";
		}

		// 4. 对象属性IRI冲突
		long opCount = objectPropertyMapper.selectCount(Wrappers.<OntObjectProperty>lambdaQuery()
			.eq(OntObjectProperty::getIri, iri)
			.ne(isSameTable(excludeTableName, "ont_object_property"), OntObjectProperty::getId, excludeId));
		if (opCount > 0) {
			return "IRI与已有对象属性IRI冲突";
		}

		return null;
	}

	private boolean isSameTable(String excludeTableName, String tableName) {
		return tableName.equals(excludeTableName);
	}

}
