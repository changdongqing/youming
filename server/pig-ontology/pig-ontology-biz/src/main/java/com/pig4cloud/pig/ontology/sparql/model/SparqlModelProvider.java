/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.model;

import com.pig4cloud.pig.ontology.serialization.export.ExportScope;
import com.pig4cloud.pig.ontology.serialization.export.OntologyModelExporter;
import com.pig4cloud.pig.ontology.serialization.export.PredicateStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Component;

/**
 * SPARQL 查询 Model 提供器。
 * <p>
 * 调用完整导出器构建请求级独立 Model（设计文档 §3.1）。
 * 查询端点固定使用 PredicateStrategy.INTERNAL_IRI，因为 ont_data_property.iri
 * 是平台内全局唯一绝对 IRI，适合作为查询图的稳定谓词 IRI。
 * Model 为请求私有对象，查询完成后由调用方 close()，不得放入静态集合。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SparqlModelProvider {

	private final OntologyModelExporter ontologyModelExporter;

	/**
	 * 为指定本体工程构建请求级只读 Model。
	 * @param ontologyId 本体工程 ID
	 * @return Jena Model（调用方负责 close）
	 */
	public Model buildModel(Long ontologyId) {
		Model model = ontologyModelExporter.buildCompleteModel(
				ontologyId,
				ExportScope.FULL,
				PredicateStrategy.INTERNAL_IRI,
				null);
		log.debug("SPARQL Model 构建完成: ontologyId={}, size={}", ontologyId, model.size());
		return model;
	}

}
