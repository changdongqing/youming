/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.model;

import org.apache.jena.rdf.model.Model;

/**
 * SPARQL Model 授权投影策略。
 * <p>
 * 模块 36 接入点。一期为空实现（直接返回原始 Model）。
 * 模块 36 落地后：在查询前移除当前用户不可见的数据属性三元组和实例；
 * 不能仅在查询结果返回后脱敏，否则 ASK、COUNT、FILTER、聚合已泄漏受限信息。
 * </p>
 *
 * @author youming
 */
public interface SparqlModelProjectionPolicy {

	/**
	 * 对 Model 执行授权投影。
	 * @param model 请求级 Model（可变）
	 * @param ontologyId 本体工程 ID
	 * @param currentUser 当前用户
	 */
	void applyProjection(Model model, Long ontologyId, String currentUser);

}
