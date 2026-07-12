/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.reasoner;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 一致性报告。
 *
 * @author youming
 */
@Data
public class ConsistencyReport {

	/**
	 * 是否一致
	 */
	private boolean consistent;

	/**
	 * 违规列表
	 */
	private List<ConsistencyViolation> violations;

	/**
	 * 一致性违规。
	 */
	@Data
	public static class ConsistencyViolation {

		/**
		 * 违规实例IRI
		 */
		private String instanceIRI;

		/**
		 * 违规类型（DISJOINT / FUNCTIONAL / ...）
		 */
		private String violationType;

		/**
		 * 描述
		 */
		private String description;

		/**
		 * 冲突类型集合
		 */
		private Set<String> conflictingTypes;

	}

}
