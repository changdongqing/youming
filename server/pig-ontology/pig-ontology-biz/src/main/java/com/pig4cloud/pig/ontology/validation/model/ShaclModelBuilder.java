/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.model;

import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.stereotype.Component;

import java.io.StringReader;

/**
 * SHACL Shapes 图构建器。
 * <p>
 * 优先从规则的 {@code shacl_shape} 字段解析预置 Turtle 文本；
 * 若为空，则返回空 Model（APPLICATION 模式规则无 SHACL 文本）。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
public class ShaclModelBuilder {

	/**
	 * 从规则构建 SHACL Shapes 图。
	 * @param rule 公理规则
	 * @return Jena Model，包含 SHACL Shapes 定义
	 */
	public Model buildShapes(OntAxiomRule rule) {
		Model shapesModel = ModelFactory.createDefaultModel();

		String shaclShape = rule.getShaclShape();
		if (shaclShape == null || shaclShape.isBlank()) {
			return shapesModel;
		}

		try {
			// 注册前缀映射，使 SHACL 文本中的 std: 前缀可被正确解析
			shapesModel.setNsPrefix("std", "http://example.org/standard-ontology#");
			shapesModel.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
			shapesModel.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
			shapesModel.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
			shapesModel.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
			shapesModel.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");

			shapesModel.read(new StringReader(shaclShape), null, "TURTLE");
		}
		catch (Exception e) {
			log.error("解析 SHACL Shapes 失败: ruleCode={}, error={}", rule.getRuleCode(), e.getMessage());
		}

		return shapesModel;
	}

}
