/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.export;

import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import org.springframework.stereotype.Component;

/**
 * 谓词IRI策略解析器。
 * <p>
 * 根据导出策略解析数据属性的谓词IRI：
 * - PREFERRED_ALIAS: 输出 preferredAlias（若非空）否则 iri
 * - STANDARD_IRI: 输出 standardIri（若非空）否则 iri
 * - INTERNAL_IRI: 始终输出 iri
 * </p>
 *
 * @author youming
 */
@Component
public class PredicateStrategyResolver {

	/**
	 * 解析数据属性谓词IRI。
	 * @param property 数据属性
	 * @param strategy 谓词策略
	 * @return 谓词IRI
	 */
	public String resolveDataPropertyPredicate(OntDataProperty property, PredicateStrategy strategy) {
		switch (strategy) {
			case PREFERRED_ALIAS:
				return isNotBlank(property.getPreferredAlias())
					? property.getPreferredAlias()
					: property.getIri();
			case STANDARD_IRI:
				return isNotBlank(property.getStandardIri())
					? property.getStandardIri()
					: property.getIri();
			case INTERNAL_IRI:
			default:
				return property.getIri();
		}
	}

	/**
	 * 解析对象属性谓词IRI。
	 * <p>
	 * 对象属性无 preferredAlias/standardIri 字段，始终使用 iri。
	 * </p>
	 * @param property 对象属性
	 * @param strategy 谓词策略（对象属性忽略此参数，始终使用内部IRI）
	 * @return 谓词IRI
	 */
	public String resolveObjectPropertyPredicate(OntObjectProperty property, PredicateStrategy strategy) {
		return property.getIri();
	}

	private static boolean isNotBlank(String s) {
		return s != null && !s.trim().isEmpty();
	}

}
