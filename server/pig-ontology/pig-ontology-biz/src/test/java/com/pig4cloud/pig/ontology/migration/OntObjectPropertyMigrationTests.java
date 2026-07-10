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
 * V1-V8 PostgreSQL 迁移集成测试。
 *
 * @author youming
 */
@Testcontainers(disabledWithoutDocker = true)
class OntObjectPropertyMigrationTests {

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
	void shouldApplyV8AndSeedObjectPropertySchema() throws SQLException {
		assertThat(queryLong("SELECT max(CAST(version AS integer)) FROM flyway_schema_history WHERE success"))
			.isGreaterThanOrEqualTo(8L);
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property WHERE source_type = 'GB_TABLE1' AND is_builtin = '1' AND del_flag = '0'"))
				.isEqualTo(34L);
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property WHERE is_builtin = '1' AND del_flag = '0'"))
				.isEqualTo(35L);
		assertThat(queryLong("SELECT count(*) FROM ont_object_property_label WHERE locale = 'zh' "
			+ "AND object_property_id IN (SELECT id FROM ont_object_property WHERE is_builtin = '1' AND del_flag = '0')"))
				.isEqualTo(35L);
	}

	@Test
	void shouldVerifyDerivedInverseProperty() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property WHERE source_type = 'GB_TABLE1_DERIVED' AND del_flag = '0'"))
				.isEqualTo(1L);
		assertThat(queryString("SELECT name FROM ont_object_property WHERE id = 960035")).isEqualTo("isReplacedBy");
	}

	@Test
	void shouldVerifyBidirectionalInverseRelations() throws SQLException {
		assertThat(queryLong("SELECT inverse_of_id FROM ont_object_property WHERE id = 960002")).isEqualTo(960035L);
		assertThat(queryLong("SELECT inverse_of_id FROM ont_object_property WHERE id = 960035")).isEqualTo(960002L);
		assertThat(queryLong("SELECT inverse_of_id FROM ont_object_property WHERE id = 960033")).isEqualTo(960034L);
		assertThat(queryLong("SELECT inverse_of_id FROM ont_object_property WHERE id = 960034")).isEqualTo(960033L);
	}

	@Test
	void shouldOnlyMarkIssuedByAsFunctional() throws SQLException {
		assertThat(queryString("SELECT is_functional FROM ont_object_property WHERE id = 960006")).isEqualTo("1");
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property WHERE is_builtin = '1' AND is_functional = '1' AND del_flag = '0'"))
				.isEqualTo(1L);
	}

	@Test
	void shouldNotPresetTransitiveOrSymmetric() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property WHERE is_builtin = '1' "
			+ "AND (is_transitive = '1' OR is_symmetric = '1') AND del_flag = '0'"))
				.isZero();
	}

	@Test
	void shouldNotPresetFunctionalForStageProperties() throws SQLException {
		assertThat(queryString("SELECT is_functional FROM ont_object_property WHERE id = 960033")).isEqualTo("0");
		assertThat(queryString("SELECT is_inverse_functional FROM ont_object_property WHERE id = 960034"))
			.isEqualTo("0");
	}

	@Test
	void shouldVerifyUnionDomainsAndRanges() throws SQLException {
		// 960009 draftedBy 双值域：Organization(940012) 或 Individual(940021)
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property_range WHERE object_property_id = 960009")).isEqualTo(2L);
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property_range WHERE object_property_id = 960009 AND entity_type_id = 940021"))
				.isEqualTo(1L);
		// 960014 hasStructuralElement 双定义域
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property_domain WHERE object_property_id = 960014")).isEqualTo(2L);
		// 960015 hasClause 双定义域
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property_domain WHERE object_property_id = 960015")).isEqualTo(2L);
		// 960017 defines 双定义域
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property_domain WHERE object_property_id = 960017")).isEqualTo(2L);
		// 960027 imposesConstraint 双定义域
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property_domain WHERE object_property_id = 960027")).isEqualTo(2L);
		// 960031 referencesExternalResource 双定义域
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property_domain WHERE object_property_id = 960031")).isEqualTo(2L);
	}

	@Test
	void shouldVerifyAllPropertiesHaveDomainAndRange() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property op WHERE op.is_builtin = '1' AND op.del_flag = '0' "
			+ "AND NOT EXISTS (SELECT 1 FROM ont_object_property_domain d WHERE d.object_property_id = op.id)"))
				.isZero();
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property op WHERE op.is_builtin = '1' AND op.del_flag = '0' "
			+ "AND NOT EXISTS (SELECT 1 FROM ont_object_property_range r WHERE r.object_property_id = op.id)"))
				.isZero();
	}

	@Test
	void shouldVerifyAllIrisEqualNamespaceUriPlusLocalName() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property op "
			+ "JOIN ont_namespace ns ON op.namespace_id = ns.id "
			+ "WHERE op.del_flag = '0' AND op.iri <> ns.uri || op.iri_local_name"))
				.isZero();
	}

	@Test
	void shouldVerifyNoIriConflictWithEntityAndDataProperties() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property op "
			+ "JOIN ont_entity_type et ON op.iri = et.iri "
			+ "WHERE op.del_flag = '0' AND et.del_flag = '0'"))
				.isZero();
		assertThat(queryLong(
			"SELECT count(*) FROM ont_object_property op "
			+ "JOIN ont_data_property dp ON op.iri = dp.iri "
			+ "WHERE op.del_flag = '0' AND dp.del_flag = '0'"))
				.isZero();
	}

	@Test
	void shouldVerifyHasSubClauseIsNotInverse() throws SQLException {
		assertThat(queryString("SELECT inverse_of_id FROM ont_object_property WHERE id = 960016")).isNull();
	}

	@Test
	void shouldCreateConstraintsAndIndexes() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM pg_constraint WHERE contype = 'c' AND conrelid::regclass::text = 'ont_object_property'"))
				.isGreaterThanOrEqualTo(8L);
		assertThat(queryLong("SELECT count(*) FROM pg_indexes WHERE indexname IN ("
			+ "'uk_ont_object_property_iri', 'uk_ont_object_property_local_name', "
			+ "'uk_ont_object_property_name', 'uk_ont_object_property_inverse')"))
				.isEqualTo(4L);
	}

	@Test
	void shouldVerifyMenuAndRoleAuth() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM sys_menu WHERE menu_id BETWEEN 900500 AND 900504 AND del_flag = '0'"))
				.isEqualTo(5L);
		assertThat(queryLong(
			"SELECT count(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 900500 AND 900504"))
				.isEqualTo(5L);
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
