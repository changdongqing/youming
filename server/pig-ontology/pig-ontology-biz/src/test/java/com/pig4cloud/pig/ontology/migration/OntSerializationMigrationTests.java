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
 * V12 PostgreSQL 迁移集成测试。
 *
 * @author youming
 */
@Testcontainers(disabledWithoutDocker = true)
class OntSerializationMigrationTests {

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
	void shouldApplyV12AndCreateSerializationLogTable() throws SQLException {
		assertThat(queryLong("SELECT max(CAST(version AS integer)) FROM flyway_schema_history WHERE success"))
			.isGreaterThanOrEqualTo(12L);
		assertThat(queryLong("SELECT count(*) FROM information_schema.tables WHERE table_name = 'ont_serialization_log'"))
			.isEqualTo(1L);
	}

	@Test
	void shouldVerifyCheckConstraints() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM information_schema.check_constraints WHERE constraint_name = 'ck_ont_serial_log_op_type'"))
				.isEqualTo(1L);
		assertThat(queryLong(
			"SELECT count(*) FROM information_schema.check_constraints WHERE constraint_name = 'ck_ont_serial_log_format'"))
				.isEqualTo(1L);
		assertThat(queryLong(
			"SELECT count(*) FROM information_schema.check_constraints WHERE constraint_name = 'ck_ont_serial_log_scope'"))
				.isEqualTo(1L);
		assertThat(queryLong(
			"SELECT count(*) FROM information_schema.check_constraints WHERE constraint_name = 'ck_ont_serial_log_strategy'"))
				.isEqualTo(1L);
	}

	@Test
	void shouldVerifyMenuAndRoleAuth() throws SQLException {
		assertThat(queryLong("SELECT count(*) FROM sys_menu WHERE menu_id BETWEEN 900900 AND 900904 AND del_flag = '0'"))
			.isEqualTo(5L);
		assertThat(queryLong("SELECT count(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 900900 AND 900904"))
			.isEqualTo(5L);
	}

	@Test
	void shouldVerifyMenuPermissions() throws SQLException {
		assertThat(queryString("SELECT permission FROM sys_menu WHERE menu_id = 900901"))
			.isEqualTo("ontology_export_view");
		assertThat(queryString("SELECT permission FROM sys_menu WHERE menu_id = 900902"))
			.isEqualTo("ontology_export_force");
		assertThat(queryString("SELECT permission FROM sys_menu WHERE menu_id = 900903"))
			.isEqualTo("ontology_import_view");
		assertThat(queryString("SELECT permission FROM sys_menu WHERE menu_id = 900904"))
			.isEqualTo("ontology_import_add");
	}

	@Test
	void shouldVerifyOperationTypeValues() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_serialization_log WHERE operation_type NOT IN ('EXPORT', 'IMPORT') AND del_flag = '0'"))
				.isZero();
	}

	@Test
	void shouldVerifyFormatValues() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_serialization_log WHERE rdf_format NOT IN ('TURTLE', 'JSON-LD', 'RDF-XML', 'N-TRIPLES') AND del_flag = '0'"))
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
