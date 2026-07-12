/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation;

import com.pig4cloud.pig.ontology.entity.OntValidationResult;
import com.pig4cloud.pig.ontology.validation.model.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ValidationResult 模型单元测试。
 *
 * @author youming
 */
class ValidationResultTest {

	@Test
	void shouldConvertToEntityWithCorrectFields() {
		ValidationResult result = ValidationResult.builder()
			.severity(ValidationResult.Severity.VIOLATION)
			.focusNode("http://example.org/standard-ontology#TestInstance")
			.resultPath("http://example.org/standard-ontology#status")
			.ruleName("标准状态枚举约束")
			.ruleCode("GB8_STANDARD_STATUS_ENUM")
			.message("标准状态取值不合法")
			.expectedValue("草案/现行/废止/修订中")
			.actualValue("未知状态")
			.suggestion("请选择合法的枚举值")
			.build();

		OntValidationResult entity = result.toEntity(990001L, 5);

		assertThat(entity.getReportId()).isEqualTo(990001L);
		assertThat(entity.getSortOrder()).isEqualTo(5);
		assertThat(entity.getSeverity()).isEqualTo("VIOLATION");
		assertThat(entity.getFocusNode()).isEqualTo("http://example.org/standard-ontology#TestInstance");
		assertThat(entity.getResultPath()).isEqualTo("http://example.org/standard-ontology#status");
		assertThat(entity.getRuleName()).isEqualTo("标准状态枚举约束");
		assertThat(entity.getRuleCode()).isEqualTo("GB8_STANDARD_STATUS_ENUM");
		assertThat(entity.getMessage()).isEqualTo("标准状态取值不合法");
		assertThat(entity.getExpectedValue()).isEqualTo("草案/现行/废止/修订中");
		assertThat(entity.getActualValue()).isEqualTo("未知状态");
		assertThat(entity.getSuggestion()).isEqualTo("请选择合法的枚举值");
	}

	@Test
	void shouldDefaultToInfoWhenSeverityIsNull() {
		ValidationResult result = ValidationResult.builder()
			.severity(null)
			.ruleName("测试规则")
			.message("测试消息")
			.build();

		OntValidationResult entity = result.toEntity(1L, 0);

		assertThat(entity.getSeverity()).isEqualTo("INFO");
	}

	@Test
	void shouldHandleNullOptionalFields() {
		ValidationResult result = ValidationResult.builder()
			.severity(ValidationResult.Severity.WARNING)
			.ruleName("规则")
			.message("消息")
			.build();

		OntValidationResult entity = result.toEntity(1L, 0);

		assertThat(entity.getFocusNode()).isNull();
		assertThat(entity.getResultPath()).isNull();
		assertThat(entity.getExpectedValue()).isNull();
		assertThat(entity.getActualValue()).isNull();
		assertThat(entity.getSuggestion()).isNull();
	}

}
