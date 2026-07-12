/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization;

import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.serialization.export.PredicateStrategy;
import com.pig4cloud.pig.ontology.serialization.export.PredicateStrategyResolver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 谓词策略解析器测试。
 *
 * @author youming
 */
class PredicateStrategyResolverTest {

	private final PredicateStrategyResolver resolver = new PredicateStrategyResolver();

	@Test
	void shouldReturnPreferredAliasWhenSet() {
		OntDataProperty dp = new OntDataProperty();
		dp.setIri("http://example.org/standard-ontology#unit");
		dp.setPreferredAlias("http://example.org/standard-ontology#measurementUnit");
		dp.setStandardIri("http://example.org/standard-ontology#unit");

		String result = resolver.resolveDataPropertyPredicate(dp, PredicateStrategy.PREFERRED_ALIAS);
		assertThat(result).isEqualTo("http://example.org/standard-ontology#measurementUnit");
	}

	@Test
	void shouldFallbackToIriWhenPreferredAliasNull() {
		OntDataProperty dp = new OntDataProperty();
		dp.setIri("http://example.org/standard-ontology#unit");

		String result = resolver.resolveDataPropertyPredicate(dp, PredicateStrategy.PREFERRED_ALIAS);
		assertThat(result).isEqualTo("http://example.org/standard-ontology#unit");
	}

	@Test
	void shouldReturnStandardIriWhenSet() {
		OntDataProperty dp = new OntDataProperty();
		dp.setIri("http://example.org/standard-ontology#measurementUnit");
		dp.setStandardIri("http://example.org/standard-ontology#unit");

		String result = resolver.resolveDataPropertyPredicate(dp, PredicateStrategy.STANDARD_IRI);
		assertThat(result).isEqualTo("http://example.org/standard-ontology#unit");
	}

	@Test
	void shouldReturnInternalIri() {
		OntDataProperty dp = new OntDataProperty();
		dp.setIri("http://example.org/standard-ontology#measurementUnit");
		dp.setPreferredAlias("http://example.org/standard-ontology#alias");
		dp.setStandardIri("http://example.org/standard-ontology#unit");

		String result = resolver.resolveDataPropertyPredicate(dp, PredicateStrategy.INTERNAL_IRI);
		assertThat(result).isEqualTo("http://example.org/standard-ontology#measurementUnit");
	}

}
