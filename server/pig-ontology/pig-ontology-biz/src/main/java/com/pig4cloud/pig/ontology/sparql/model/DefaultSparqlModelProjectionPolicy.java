/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.model;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Component;

/**
 * SPARQL Model 授权投影策略默认实现。
 * <p>
 * 一期为空实现（不做投影）。模块 36 落地后替换为授权投影实现。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
public class DefaultSparqlModelProjectionPolicy implements SparqlModelProjectionPolicy {

	@Override
	public void applyProjection(Model model, Long ontologyId, String currentUser) {
		// 一期：不做授权投影，直接返回原始 Model。
		// 模块 36 接入后：按用户授权视图移除受限数据属性三元组和实例。
		log.debug("授权投影（一期空实现）: ontologyId={}, user={}", ontologyId, currentUser);
	}

}
