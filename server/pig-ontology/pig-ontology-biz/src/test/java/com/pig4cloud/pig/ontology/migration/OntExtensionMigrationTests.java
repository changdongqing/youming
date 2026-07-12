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
 * V13 PostgreSQL 迁移集成测试。
 *
 * @author youming
 */
@Testcontainers(disabledWithoutDocker = true)
class OntExtensionMigrationTests {

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
	void shouldApplyV13AndCreateExtensionTables() throws SQLException {
		assertThat(queryLong("SELECT max(CAST(version AS integer)) FROM flyway_schema_history WHERE success"))
			.isGreaterThanOrEqualTo(13L);
		assertThat(queryLong("SELECT count(*) FROM information_schema.tables WHERE table_name = 'ont_extension_module'"))
			.isEqualTo(1L);
		assertThat(queryLong("SELECT count(*) FROM information_schema.tables WHERE table_name = 'ont_extension_resource'"))
			.isEqualTo(1L);
	}

	@Test
	void shouldVerifyCheckConstraints() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM information_schema.check_constraints WHERE constraint_name = 'ck_ont_ext_resource_type'"))
				.isEqualTo(1L);
		assertThat(queryLong(
			"SELECT count(*) FROM information_schema.check_constraints WHERE constraint_name = 'ck_ont_ext_module_builtin'"))
				.isEqualTo(1L);
	}

	@Test
	void shouldVerifyUniqueIndexes() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM pg_indexes WHERE indexname = 'uk_ont_ext_module_code'"))
				.isEqualTo(1L);
		assertThat(queryLong(
			"SELECT count(*) FROM pg_indexes WHERE indexname = 'uk_ont_ext_resource'"))
				.isEqualTo(1L);
	}

	@Test
	void shouldVerifyMenuAndRoleAuth() throws SQLException {
		assertThat(queryLong("SELECT count(*) FROM sys_menu WHERE menu_id BETWEEN 901000 AND 901006 AND del_flag = '0'"))
			.isEqualTo(7L);
		assertThat(queryLong("SELECT count(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 901000 AND 901006"))
			.isEqualTo(7L);
	}

	@Test
	void shouldVerifyMenuPermissions() throws SQLException {
		assertThat(queryString("SELECT permission FROM sys_menu WHERE menu_id = 901001"))
			.isEqualTo("ontology_extension_view");
		assertThat(queryString("SELECT permission FROM sys_menu WHERE menu_id = 901002"))
			.isEqualTo("ontology_extension_add");
		assertThat(queryString("SELECT permission FROM sys_menu WHERE menu_id = 901005"))
			.isEqualTo("ontology_extension_validate");
		assertThat(queryString("SELECT permission FROM sys_menu WHERE menu_id = 901006"))
			.isEqualTo("ontology_extension_export");
	}

	@Test
	void shouldVerifyResourceTypeValues() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_extension_resource WHERE resource_type NOT IN ('ENTITY_TYPE', 'DATA_PROPERTY', 'OBJECT_PROPERTY', 'AXIOM_RULE', 'UNIT') AND del_flag = '0'"))
				.isZero();
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
