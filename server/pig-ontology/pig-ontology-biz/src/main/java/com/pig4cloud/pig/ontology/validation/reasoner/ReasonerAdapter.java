/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.reasoner;

import org.apache.jena.rdf.model.Model;

import java.util.Set;

/**
 * 推理引擎适配器 SPI（对应 PRD §4.7.4）。
 * <p>
 * 本期仅注册 {@link JenaReasonerAdapter}，承接国标 §5.3 OWL 公理一致性检测。
 * 行业扩展可通过 Spring {@code @Component} 注册新实现，接入 GraphDB 推理机等。
 * </p>
 *
 * @author youming
 */
public interface ReasonerAdapter {

	/**
	 * 推理引擎标识。
	 * @return 引擎名称
	 */
	String getReasonerName();

	/**
	 * 能力声明。
	 * @return 支持的推理能力集合
	 */
	Set<ReasonerCapability> getReasonerCapabilities();

	/**
	 * 公理一致性检测（国标 §5.3，本期必做）。
	 * @param schemaModel 本体 Schema 模型（类层次 + 公理声明）
	 * @param instanceModel 实例模型（rdf:type 三元组 + 数据/对象属性断言）
	 * @return 一致性报告
	 */
	ConsistencyReport checkConsistency(Model schemaModel, Model instanceModel);

	/**
	 * 推理闭包（扩展，本期返回未启用）。
	 * @param schemaModel 本体 Schema 模型
	 * @param instanceModel 实例模型
	 * @return 推理结果
	 * @throws UnsupportedOperationException 本期未启用大规模推理
	 */
	default InferenceReport inferEntailments(Model schemaModel, Model instanceModel) {
		throw new UnsupportedOperationException("大规模推理本期未启用");
	}

}
