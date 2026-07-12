/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation;

import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntAxiomRuleTarget;
import com.pig4cloud.pig.ontology.validation.executor.ValidationExecutor;
import com.pig4cloud.pig.ontology.validation.model.ValidationContext;
import com.pig4cloud.pig.ontology.validation.model.ValidationResult;
import com.pig4cloud.pig.ontology.validation.service.ValidationExecutorRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 执行器注册表单元测试。
 *
 * @author youming
 */
class ValidationExecutorRegistryTest {

	@Test
	void shouldRegisterExecutorsByCode() {
		ValidationExecutor executor1 = new TestExecutor("TEST_CODE_1", "APPLICATION");
		ValidationExecutor executor2 = new TestExecutor("TEST_CODE_2", "SHACL_CORE");

		ValidationExecutorRegistry registry = new ValidationExecutorRegistry(List.of(executor1, executor2));

		assertThat(registry.getRegisteredCodes()).containsExactlyInAnyOrder("TEST_CODE_1", "TEST_CODE_2");
		assertThat(registry.getExecutor("TEST_CODE_1")).isSameAs(executor1);
		assertThat(registry.getExecutor("TEST_CODE_2")).isSameAs(executor2);
	}

	@Test
	void shouldThrowOnDuplicateCode() {
		ValidationExecutor executor1 = new TestExecutor("DUPLICATE_CODE", "APPLICATION");
		ValidationExecutor executor2 = new TestExecutor("DUPLICATE_CODE", "SHACL_CORE");

		assertThatThrownBy(() -> new ValidationExecutorRegistry(List.of(executor1, executor2)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("重复的执行器编码");
	}

	@Test
	void shouldThrowOnUnregisteredCode() {
		ValidationExecutorRegistry registry = new ValidationExecutorRegistry(List.of());

		assertThatThrownBy(() -> registry.getExecutor("NON_EXISTENT"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("执行器未注册");
	}

	@Test
	void shouldReturnAllExecutors() {
		ValidationExecutor executor1 = new TestExecutor("CODE_A", "APPLICATION");
		ValidationExecutor executor2 = new TestExecutor("CODE_B", "OWL_CONSISTENCY");

		ValidationExecutorRegistry registry = new ValidationExecutorRegistry(List.of(executor1, executor2));

		assertThat(registry.getExecutors()).hasSize(2);
	}

	/**
	 * 测试用执行器实现。
	 */
	private static class TestExecutor implements ValidationExecutor {

		private final String code;

		private final String mode;

		TestExecutor(String code, String mode) {
			this.code = code;
			this.mode = mode;
		}

		@Override
		public String getExecutorCode() {
			return code;
		}

		@Override
		public String getValidationMode() {
			return mode;
		}

		@Override
		public List<ValidationResult> validate(OntAxiomRule rule, List<OntAxiomRuleTarget> targets,
				ValidationContext context) {
			return List.of();
		}

	}

}
