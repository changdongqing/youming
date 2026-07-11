/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V1-V7 PostgreSQL 迁移集成测试。
 *
 * @author youming
 */
@Testcontainers(disabledWithoutDocker = true)
class OntDataPropertyMigrationTests {

	@Container
	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
		.withDatabaseName("youming_test")
		.withUsername("youming")
		.withPassword("youming");

	@BeforeAll
	static void migrate() {
		Flyway.configure()
			.dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
			.locations("classpath:db/migration")
			.placeholderReplacement(false)
			.load()
			.migrate();
	}

	@Test
	void shouldApplyV7AndSeedDataPropertySchema() throws SQLException {
		assertThat(queryLong("SELECT max(CAST(version AS integer)) FROM flyway_schema_history WHERE success"))
			.isGreaterThanOrEqualTo(7L);
		assertThat(queryLong("SELECT count(*) FROM ont_data_property WHERE source_type = 'APPENDIX_C' AND is_builtin = '1' AND del_flag = '0'"))
			.isEqualTo(47L);
		assertThat(queryLong("SELECT count(*) FROM ont_data_property WHERE is_builtin = '1' AND del_flag = '0'"))
			.isEqualTo(56L);
		assertThat(queryLong("SELECT count(*) FROM ont_data_property_label WHERE locale = 'zh' AND data_property_id IN (SELECT id FROM ont_data_property WHERE is_builtin = '1' AND del_flag = '0')"))
			.isEqualTo(56L);
		assertThat(queryLong("SELECT count(*) FROM ont_data_property_enum"))
			.isGreaterThanOrEqualTo(32L);
	}

	@Test
	void shouldCreateThreeDistinctConstraintTypeIrisWithSameStandardIri() throws SQLException {
		assertThat(queryString("SELECT iri_local_name FROM ont_data_property WHERE id = 950004"))
			.isEqualTo("constraintTypeStandard");
		assertThat(queryString("SELECT iri_local_name FROM ont_data_property WHERE id = 950031"))
			.isEqualTo("constraintTypeClause");
		assertThat(queryString("SELECT iri_local_name FROM ont_data_property WHERE id = 950037"))
			.isEqualTo("constraintTypeConstraint");
		assertThat(queryString("SELECT standard_iri FROM ont_data_property WHERE id = 950004"))
			.isEqualTo("http://example.org/standard-ontology#constraintType");
		assertThat(queryString("SELECT standard_iri FROM ont_data_property WHERE id = 950031"))
			.isEqualTo("http://example.org/standard-ontology#constraintType");
		assertThat(queryString("SELECT standard_iri FROM ont_data_property WHERE id = 950037"))
			.isEqualTo("http://example.org/standard-ontology#constraintType");
	}

	@Test
	void shouldConfigureUnitPropertyWithAliasAndNoCategoryBinding() throws SQLException {
		assertThat(queryString("SELECT name FROM ont_data_property WHERE id = 950041"))
			.isEqualTo("unit");
		assertThat(queryString("SELECT preferred_alias FROM ont_data_property WHERE id = 950041"))
			.isEqualTo("measurementUnit");
		assertThat(queryString("SELECT base_type FROM ont_data_property WHERE id = 950041"))
			.isEqualTo("UNIT_REF");
		assertThat(queryString("SELECT value_mode FROM ont_data_property WHERE id = 950041"))
			.isEqualTo("UNIT_DICTIONARY");
		assertThat(queryString("SELECT unit_ref_mode FROM ont_data_property WHERE id = 950041"))
			.isEqualTo("DICTIONARY_SYMBOL");
		assertThat(queryString("SELECT unit_category_id FROM ont_data_property WHERE id = 950041"))
			.isNull();
	}

	@Test
	void shouldAssignCorrectDomainsForClauseTitleAndUniqueIdentifier() throws SQLException {
		assertThat(queryLong("SELECT domain_entity_type_id FROM ont_data_property WHERE id = 950027"))
			.isEqualTo(940041L);
		assertThat(queryLong("SELECT domain_entity_type_id FROM ont_data_property WHERE id = 950028"))
			.isEqualTo(940048L);
	}

	@Test
	void shouldNotHaveDuplicateUniqueIdentifierForStandardOrClause() throws SQLException {
		assertThat(queryLong("SELECT count(*) FROM ont_data_property WHERE name = 'uniqueIdentifier' AND domain_entity_type_id NOT IN (940048) AND del_flag = '0'"))
			.isZero();
	}

	@Test
	void shouldHaveTextOrNumericForPropertyValue() throws SQLException {
		assertThat(queryString("SELECT base_type FROM ont_data_property WHERE id = 950035"))
			.isEqualTo("TEXT_OR_NUMERIC");
	}

	@Test
	void shouldHaveEffectiveTime() throws SQLException {
		assertThat(queryString("SELECT name FROM ont_data_property WHERE id = 950043"))
			.isEqualTo("effectiveTime");
	}

	@Test
	void shouldVerifyAllIrisEqualNamespaceUriPlusLocalName() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_data_property dp " +
			"JOIN ont_namespace ns ON dp.namespace_id = ns.id " +
			"WHERE dp.del_flag = '0' AND dp.iri <> ns.uri || dp.iri_local_name"))
			.isZero();
	}

	@Test
	void shouldVerifyOpenEnumContainsConstraintTypeAndConformanceAndRelativeValue() throws SQLException {
		assertThat(queryLong("SELECT count(*) FROM ont_data_property_enum WHERE data_property_id = 950036 AND enum_value = '约束型'"))
			.isEqualTo(1L);
		assertThat(queryLong("SELECT count(*) FROM ont_data_property_enum WHERE data_property_id = 950037 AND enum_value = '符合性'"))
			.isEqualTo(1L);
		assertThat(queryLong("SELECT count(*) FROM ont_data_property_enum WHERE data_property_id = 950037 AND enum_value = '相对值'"))
			.isEqualTo(1L);
	}

	@Test
	void shouldCreateForeignKeysAndChecks() throws SQLException {
		assertThat(queryLong("SELECT count(*) FROM pg_constraint WHERE contype = 'f' AND conrelid::regclass::text = 'ont_data_property'"))
			.isGreaterThanOrEqualTo(4L);
		assertThat(queryLong("SELECT count(*) FROM pg_constraint WHERE contype = 'c' AND conrelid::regclass::text = 'ont_data_property'"))
			.isGreaterThanOrEqualTo(10L);
		assertThat(queryLong("SELECT count(*) FROM pg_indexes WHERE indexname IN ('uk_ont_data_property_iri', 'uk_ont_data_property_local_name', 'uk_ont_data_property_domain_name', 'uk_ont_data_property_preferred_alias')"))
			.isEqualTo(4L);
	}

	private static long queryLong(String sql) throws SQLException {
		try (Connection connection = POSTGRES.createConnection("");
				Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
			resultSet.next();
			return resultSet.getLong(1);
		}
	}

	private static String queryString(String sql) throws SQLException {
		try (Connection connection = POSTGRES.createConnection("");
				Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
			resultSet.next();
			return resultSet.getString(1);
		}
	}

}
