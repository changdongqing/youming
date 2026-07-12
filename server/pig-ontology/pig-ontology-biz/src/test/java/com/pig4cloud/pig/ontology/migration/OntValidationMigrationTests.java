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
 * V11 PostgreSQL 迁移集成测试。
 *
 * @author youming
 */
@Testcontainers(disabledWithoutDocker = true)
class OntValidationMigrationTests {

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
	void shouldApplyV11AndCreateValidationTables() throws SQLException {
		assertThat(queryLong("SELECT max(CAST(version AS integer)) FROM flyway_schema_history WHERE success"))
			.isGreaterThanOrEqualTo(11L);
		assertThat(queryLong("SELECT count(*) FROM information_schema.tables WHERE table_name = 'ont_validation_report'"))
			.isEqualTo(1L);
		assertThat(queryLong("SELECT count(*) FROM information_schema.tables WHERE table_name = 'ont_validation_result'"))
			.isEqualTo(1L);
	}

	@Test
	void shouldVerifyCheckConstraints() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM information_schema.check_constraints WHERE constraint_name = 'ck_ont_val_report_status'"))
				.isEqualTo(1L);
		assertThat(queryLong(
			"SELECT count(*) FROM information_schema.check_constraints WHERE constraint_name = 'ck_ont_val_result_severity'"))
				.isEqualTo(1L);
	}

	@Test
	void shouldVerifyAppendixDSeedReport() throws SQLException {
		assertThat(queryLong("SELECT count(*) FROM ont_validation_report WHERE id = 990001 AND conforms = TRUE AND del_flag = '0'"))
			.isEqualTo(1L);
		assertThat(queryLong("SELECT count(*) FROM ont_validation_result WHERE report_id = 990001 AND del_flag = '0'"))
			.isZero();
		assertThat(queryString("SELECT scope FROM ont_validation_report WHERE id = 990001")).isEqualTo("FULL");
		assertThat(queryString("SELECT status FROM ont_validation_report WHERE id = 990001")).isEqualTo("COMPLETED");
	}

	@Test
	void shouldVerifyMenuAndRoleAuth() throws SQLException {
		assertThat(queryLong("SELECT count(*) FROM sys_menu WHERE menu_id BETWEEN 900800 AND 900803 AND del_flag = '0'"))
			.isEqualTo(4L);
		assertThat(queryLong("SELECT count(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 900800 AND 900803"))
			.isEqualTo(4L);
	}

	@Test
	void shouldVerifyReportStatusValues() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_validation_report WHERE status NOT IN ('RUNNING', 'COMPLETED', 'FAILED') AND del_flag = '0'"))
				.isZero();
	}

	@Test
	void shouldVerifyResultSeverityValues() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_validation_result WHERE severity NOT IN ('VIOLATION', 'WARNING', 'INFO') AND del_flag = '0'"))
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
