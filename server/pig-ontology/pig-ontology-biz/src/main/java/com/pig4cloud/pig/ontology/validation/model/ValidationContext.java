/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.model;

import com.pig4cloud.pig.ontology.validation.model.OntologyModelAssembler;
import lombok.Builder;
import lombok.Getter;
import org.apache.jena.rdf.model.Model;

import java.util.HashMap;
import java.util.Map;

/**
 * 校验上下文，在单次校验会话内共享。
 * <p>
 * 持有 ontologyId、校验范围、目标实例、Model 缓存和实体类型祖先缓存，
 * 避免执行器重复查询和组装。
 * </p>
 *
 * @author youming
 */
@Getter
@Builder
public class ValidationContext {

	/**
	 * 本体工程ID
	 */
	private final Long ontologyId;

	/**
	 * 校验范围
	 */
	private final ValidationScope scope;

	/**
	 * 增量校验时的目标实例ID（scope=INSTANCE 时非空）
	 */
	private final Long targetInstanceId;

	/**
	 * Model 组装器
	 */
	private final OntologyModelAssembler modelAssembler;

	/**
	 * Schema Model 缓存（类层次 + 不相交声明）
	 */
	private Model cachedSchemaModel;

	/**
	 * 实体类型祖先缓存（entityTypeId -> 祖先ID集合含自身）
	 */
	private final Map<Long, java.util.Set<Long>> ancestorCache = new HashMap<>();

	/**
	 * 实体类型子类缓存（entityTypeId -> 子类ID集合含自身）
	 */
	private final Map<Long, java.util.Set<Long>> descendantCache = new HashMap<>();

	/**
	 * 获取或组装 Schema Model。
	 * @return Schema Model
	 */
	public Model getOrCreateSchemaModel() {
		if (cachedSchemaModel == null) {
			cachedSchemaModel = modelAssembler.buildSchemaModel(ontologyId);
		}
		return cachedSchemaModel;
	}

}
