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
 * V1-V6 PostgreSQL 迁移集成测试。
 *
 * @author youming
 */
@Testcontainers(disabledWithoutDocker = true)
class OntologyEntityTypeMigrationTests {

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
	void shouldApplyV1ToV6AndSeedEntityTypeSchema() throws SQLException {
		assertThat(queryLong("SELECT max(CAST(version AS integer)) FROM flyway_schema_history WHERE success"))
			.isGreaterThanOrEqualTo(6L);
		assertThat(queryLong("SELECT count(*) FROM ont_ontology_project WHERE id = 935001 AND del_flag = '0'"))
			.isEqualTo(1L);
		assertThat(queryLong("SELECT count(*) FROM ont_entity_type WHERE id BETWEEN 940001 AND 940086 AND del_flag = '0'"))
			.isEqualTo(86L);
		assertThat(queryLong("SELECT count(*) FROM ont_entity_type_label WHERE locale = 'zh' AND entity_type_id BETWEEN 940001 AND 940086"))
			.isEqualTo(86L);
		assertThat(queryLong("SELECT count(*) FROM ont_entity_type_hierarchy WHERE parent_id BETWEEN 940001 AND 940086 AND child_id BETWEEN 940001 AND 940086"))
			.isEqualTo(72L);
		assertThat(queryLong("SELECT count(*) FROM ont_entity_type_equivalent"))
			.isZero();
	}

	@Test
	void shouldModelClauseAsMultipleInheritanceWithoutEquivalence() throws SQLException {
		assertThat(queryLong("SELECT count(*) FROM ont_entity_type_hierarchy WHERE child_id = 940040 AND parent_id IN (940038, 940048)"))
			.isEqualTo(2L);
		assertThat(queryLong("SELECT count(*) FROM ont_entity_type_equivalent WHERE (entity_type_id = 940040 AND equivalent_id = 940048) OR (entity_type_id = 940048 AND equivalent_id = 940040)"))
			.isZero();
	}

	@Test
	void shouldKeepListItemAndListIrisDistinct() throws SQLException {
		assertThat(queryString("SELECT iri FROM ont_entity_type WHERE id = 940044"))
			.endsWith("#ListItem");
		assertThat(queryString("SELECT iri FROM ont_entity_type WHERE id = 940054"))
			.endsWith("#List");
		assertThat(queryLong("SELECT count(*) FROM (SELECT iri FROM ont_entity_type WHERE del_flag = '0' GROUP BY iri HAVING count(*) > 1) duplicate"))
			.isZero();
	}

	@Test
	void shouldCreateForeignKeysChecksAndReverseIndexes() throws SQLException {
		assertThat(queryLong("SELECT count(*) FROM pg_constraint WHERE contype = 'f' AND conrelid::regclass::text LIKE 'ont_%'"))
			.isGreaterThanOrEqualTo(9L);
		assertThat(queryLong("SELECT count(*) FROM pg_constraint WHERE contype = 'c' AND conrelid::regclass::text LIKE 'ont_%'"))
			.isGreaterThanOrEqualTo(11L);
		assertThat(queryLong("SELECT count(*) FROM pg_indexes WHERE indexname IN ('idx_ont_entity_type_hierarchy_child', 'idx_ont_entity_type_equivalent_reverse', 'idx_ont_entity_type_disjoint_reverse')"))
			.isEqualTo(3L);
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
