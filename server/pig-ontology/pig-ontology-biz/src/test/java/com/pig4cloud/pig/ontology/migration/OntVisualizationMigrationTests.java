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
 * V14 PostgreSQL 迁移集成测试。
 * <p>
 * 可视化模块不新建业务表，仅注册菜单和权限。本测试验证：
 * 1. Flyway V14 迁移成功
 * 2. 可视化菜单（901100）存在
 * 3. 图谱查看权限（901101）存在
 * 4. 角色-菜单映射正确
 * </p>
 *
 * @author youming
 */
@Testcontainers(disabledWithoutDocker = true)
class OntVisualizationMigrationTests {

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
	void shouldApplyV14Migration() throws SQLException {
		assertThat(queryLong("SELECT max(CAST(version AS integer)) FROM flyway_schema_history WHERE success"))
			.isGreaterThanOrEqualTo(14L);
	}

	@Test
	void shouldCreateVisualizationMenu() throws SQLException {
		assertThat(queryLong("SELECT count(*) FROM sys_menu WHERE menu_id = 901100 AND del_flag = '0'"))
			.isEqualTo(1L);
		assertThat(queryString("SELECT name FROM sys_menu WHERE menu_id = 901100"))
			.isEqualTo("可视化");
		assertThat(queryLong("SELECT count(*) FROM sys_menu WHERE menu_id = 901100 AND menu_type = '0'"))
			.isEqualTo(1L);
		assertThat(queryLong("SELECT count(*) FROM sys_menu WHERE menu_id = 901100 AND parent_id = 900000"))
			.isEqualTo(1L);
	}

	@Test
	void shouldCreateViewPermission() throws SQLException {
		assertThat(queryLong("SELECT count(*) FROM sys_menu WHERE menu_id = 901101 AND del_flag = '0'"))
			.isEqualTo(1L);
		assertThat(queryString("SELECT permission FROM sys_menu WHERE menu_id = 901101"))
			.isEqualTo("ontology_visualization_view");
		assertThat(queryLong("SELECT count(*) FROM sys_menu WHERE menu_id = 901101 AND menu_type = '1'"))
			.isEqualTo(1L);
	}

	@Test
	void shouldAssignPermissionsToAdminRole() throws SQLException {
		assertThat(queryLong("SELECT count(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id = 901100"))
			.isEqualTo(1L);
		assertThat(queryLong("SELECT count(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id = 901101"))
			.isEqualTo(1L);
	}

	@Test
	void shouldHaveCorrectMenuCount() throws SQLException {
		assertThat(queryLong("SELECT count(*) FROM sys_menu WHERE menu_id BETWEEN 901100 AND 901101 AND del_flag = '0'"))
			.isEqualTo(2L);
	}

	private long queryLong(String sql) throws SQLException {
		try (Connection conn = POSTGRES.createConnection("");
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql)) {
			rs.next();
			return rs.getLong(1);
		}
	}

	private String queryString(String sql) throws SQLException {
		try (Connection conn = POSTGRES.createConnection("");
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql)) {
			rs.next();
			return rs.getString(1);
		}
	}

}
