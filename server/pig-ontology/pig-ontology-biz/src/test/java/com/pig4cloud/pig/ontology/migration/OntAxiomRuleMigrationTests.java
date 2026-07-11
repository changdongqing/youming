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
 * V1-V9 PostgreSQL 迁移集成测试。
 *
 * @author youming
 */
@Testcontainers(disabledWithoutDocker = true)
class OntAxiomRuleMigrationTests {

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
	void shouldApplyV9AndSeedAxiomRuleSchema() throws SQLException {
		assertThat(queryLong("SELECT max(CAST(version AS integer)) FROM flyway_schema_history WHERE success"))
			.isGreaterThanOrEqualTo(9L);
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule WHERE is_builtin = '1' AND del_flag = '0'"))
				.isEqualTo(12L);
	}

	@Test
	void shouldVerifySourceDistribution() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule WHERE source_type = 'GB_CLAUSE_8' AND is_builtin = '1'"))
				.isEqualTo(11L);
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule WHERE source_type = 'PRD_DERIVED' AND is_builtin = '1'"))
				.isEqualTo(1L);
		assertThat(queryString("SELECT source_type FROM ont_axiom_rule WHERE id = 970007")).isEqualTo("PRD_DERIVED");
	}

	@Test
	void shouldVerifyCategoryDistribution() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule WHERE category = 'ENTITY_TYPE' AND is_builtin = '1'"))
				.isEqualTo(2L);
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule WHERE category = 'PROPERTY' AND is_builtin = '1'"))
				.isEqualTo(5L);
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule WHERE category = 'RELATION' AND is_builtin = '1'"))
				.isEqualTo(5L);
	}

	@Test
	void shouldVerifyActiveAndBlockedStatus() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule WHERE status = 'ACTIVE' AND is_enabled = '1' AND is_builtin = '1'"))
				.isEqualTo(11L);
		assertThat(queryString("SELECT status FROM ont_axiom_rule WHERE id = 970009")).isEqualTo("BLOCKED");
		assertThat(queryString("SELECT is_enabled FROM ont_axiom_rule WHERE id = 970009")).isEqualTo("0");
		assertThat(queryString("SELECT blocked_reason FROM ont_axiom_rule WHERE id = 970009")).isNotNull();
	}

	@Test
	void shouldVerifyAllActiveRulesHaveExecutorAndTargets() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule WHERE status = 'ACTIVE' AND executor_code IS NULL"))
				.isZero();
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule r WHERE r.status = 'ACTIVE' AND r.del_flag = '0' "
			+ "AND NOT EXISTS (SELECT 1 FROM ont_axiom_rule_target t WHERE t.axiom_rule_id = r.id)"))
				.isZero();
	}

	@Test
	void shouldVerifyUnitConsistencyBindings() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule_target WHERE axiom_rule_id = 970007 "
			+ "AND data_property_id IN (950038, 950039, 950040, 950041)"))
				.isEqualTo(4L);
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule_target WHERE axiom_rule_id = 970007 AND data_property_id = 950040"))
				.isEqualTo(1L);
	}

	@Test
	void shouldVerifyFunctionalRuleBinding() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule_target WHERE axiom_rule_id = 970008 AND object_property_id = 960006"))
				.isEqualTo(1L);
		assertThat(queryString(
			"SELECT is_functional FROM ont_object_property WHERE id = 960006")).isEqualTo("1");
	}

	@Test
	void shouldVerifyDisjointTargetConsistency() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule_target WHERE axiom_rule_id = 970001 AND entity_type_id = 940028"))
				.isEqualTo(1L);
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule_target WHERE axiom_rule_id = 970001 AND entity_type_id = 940034"))
				.isEqualTo(1L);
		assertThat(queryLong(
			"SELECT count(*) FROM ont_entity_type_disjoint WHERE type_a = 940028 AND type_b = 940034"))
				.isEqualTo(1L);
	}

	@Test
	void shouldVerifyTargetExactlyOneNonNullable() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule_target WHERE "
			+ "(CASE WHEN entity_type_id IS NOT NULL THEN 1 ELSE 0 END + "
			+ "CASE WHEN data_property_id IS NOT NULL THEN 1 ELSE 0 END + "
			+ "CASE WHEN object_property_id IS NOT NULL THEN 1 ELSE 0 END + "
			+ "CASE WHEN unit_category_id IS NOT NULL THEN 1 ELSE 0 END) <> 1"))
				.isZero();
	}

	@Test
	void shouldVerifyNoDrafterBuiltInRules() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM ont_axiom_rule WHERE is_builtin = '1' AND status = 'DRAFT'"))
				.isZero();
	}

	@Test
	void shouldVerifyMenuAndRoleAuth() throws SQLException {
		assertThat(queryLong(
			"SELECT count(*) FROM sys_menu WHERE menu_id BETWEEN 900600 AND 900604 AND del_flag = '0'"))
				.isEqualTo(5L);
		assertThat(queryLong(
			"SELECT count(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 900600 AND 900604"))
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
