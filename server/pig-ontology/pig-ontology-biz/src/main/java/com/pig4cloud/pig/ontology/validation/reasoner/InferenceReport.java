/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.reasoner;

import lombok.Data;

/**
 * 推理结果报告（扩展用，本期未启用）。
 *
 * @author youming
 */
@Data
public class InferenceReport {

	/**
	 * 推理出的三元组数量
	 */
	private long inferredTripleCount;

	/**
	 * 推理引擎名称
	 */
	private String reasonerName;

}
