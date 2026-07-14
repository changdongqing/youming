/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.snapshot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.ontology.serialization.export.NamespacePrefixResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Component;

/**
 * 从版本快照 JSON 构建 Jena {@link Model}。
 * <p>
 * 用于历史版本导出（设计文档 §6）：读取 {@code ont_ontology_version.schema_snapshot}
 * JSONB，按快照中的 IRI 直接创建 Jena 三元组，不经过数据库关系表查询。
 * <p>
 * 注意：此构建器只组装 Schema 层（不含实例 ABox），因为版本快照本身不含实例。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotModelBuilder {

	private final ObjectMapper objectMapper;

	private final NamespacePrefixResolver prefixResolver;

	/**
	 * 从快照 JSON 构建 Schema Model。
	 * @param snapshotJson 快照 JSON 字符串
	 * @return Jena Model（含前缀映射和 Schema 三元组）
	 */
	public Model buildFromSnapshot(String snapshotJson) {
		try {
			JsonNode root = objectMapper.readTree(snapshotJson);
			Model model = ModelFactory.createDefaultModel();

			// 1. 注册前缀映射
			prefixResolver.registerPrefixes(model);

			// 2. 构建 Schema 层
			buildClassDeclarations(model, root);
			buildDataPropertyDeclarations(model, root);
			buildObjectPropertyDeclarations(model, root);
			buildAxiomDeclarations(model, root);

			return model;
		}
		catch (Exception e) {
			throw new RuntimeException("从快照构建Model失败", e);
		}
	}

	private void buildClassDeclarations(Model model, JsonNode root) {
		JsonNode entityTypes = root.get("entityTypes");
		if (entityTypes == null || !entityTypes.isArray()) {
			return;
		}

		for (JsonNode type : entityTypes) {
			String iri = textOrNull(type, "iri");
			if (iri == null) {
				continue;
			}
			Resource cls = model.createResource(iri);
			model.add(cls, RDF.type, OWL.Class);

			String definition = textOrNull(type, "definition");
			if (definition != null && !definition.isBlank()) {
				model.add(cls, RDFS.comment, definition);
			}

			// 标签
			JsonNode labels = type.get("labels");
			if (labels != null && labels.isArray()) {
				for (JsonNode label : labels) {
					String locale = textOrNull(label, "locale");
					String labelText = textOrNull(label, "label");
					if (labelText != null) {
						if (locale != null) {
							model.add(cls, RDFS.label, labelText, locale);
						}
						else {
							model.add(cls, RDFS.label, labelText);
						}
					}
				}
			}
		}

		// 继承关系
		JsonNode hierarchies = root.get("entityTypeHierarchies");
		if (hierarchies != null && hierarchies.isArray()) {
			for (JsonNode h : hierarchies) {
				String parentIri = textOrNull(h, "parentIri");
				String childIri = textOrNull(h, "childIri");
				if (parentIri != null && childIri != null) {
					model.add(model.createResource(childIri), RDFS.subClassOf, model.createResource(parentIri));
				}
			}
		}

		// 不相交
		JsonNode disjoints = root.get("entityTypeDisjoints");
		if (disjoints != null && disjoints.isArray()) {
			for (JsonNode d : disjoints) {
				String iriA = textOrNull(d, "iriA");
				String iriB = textOrNull(d, "iriB");
				if (iriA != null && iriB != null) {
					model.add(model.createResource(iriA), OWL.disjointWith, model.createResource(iriB));
				}
			}
		}

		// 等价类
		JsonNode equivalents = root.get("entityTypeEquivalents");
		if (equivalents != null && equivalents.isArray()) {
			for (JsonNode eq : equivalents) {
				String iriA = textOrNull(eq, "iriA");
				String iriB = textOrNull(eq, "iriB");
				if (iriA != null && iriB != null) {
					model.add(model.createResource(iriA), OWL.equivalentClass, model.createResource(iriB));
				}
			}
		}
	}

	private void buildDataPropertyDeclarations(Model model, JsonNode root) {
		JsonNode properties = root.get("dataProperties");
		if (properties == null || !properties.isArray()) {
			return;
		}

		for (JsonNode dp : properties) {
			String iri = textOrNull(dp, "iri");
			if (iri == null) {
				continue;
			}
			Property prop = model.createProperty(iri);
			model.add(prop, RDF.type, OWL.DatatypeProperty);

			String definition = textOrNull(dp, "definition");
			if (definition != null && !definition.isBlank()) {
				model.add(prop, RDFS.comment, definition);
			}

			String domainIri = textOrNull(dp, "domainEntityIri");
			if (domainIri != null) {
				model.add(prop, RDFS.domain, model.createResource(domainIri));
			}

			// 标签
			JsonNode labels = dp.get("labels");
			if (labels != null && labels.isArray()) {
				for (JsonNode label : labels) {
					String locale = textOrNull(label, "locale");
					String labelText = textOrNull(label, "label");
					if (labelText != null) {
						if (locale != null) {
							model.add(prop, RDFS.label, labelText, locale);
						}
						else {
							model.add(prop, RDFS.label, labelText);
						}
					}
				}
			}

			// FunctionalProperty
			if ("1".equals(textOrNull(dp, "isUnique"))) {
				model.add(prop, RDF.type, OWL.FunctionalProperty);
			}
		}
	}

	private void buildObjectPropertyDeclarations(Model model, JsonNode root) {
		JsonNode properties = root.get("objectProperties");
		if (properties == null || !properties.isArray()) {
			return;
		}

		for (JsonNode op : properties) {
			String iri = textOrNull(op, "iri");
			if (iri == null) {
				continue;
			}
			Property prop = model.createProperty(iri);
			model.add(prop, RDF.type, OWL.ObjectProperty);

			String definition = textOrNull(op, "definition");
			if (definition != null && !definition.isBlank()) {
				model.add(prop, RDFS.comment, definition);
			}

			// 标签
			JsonNode labels = op.get("labels");
			if (labels != null && labels.isArray()) {
				for (JsonNode label : labels) {
					String locale = textOrNull(label, "locale");
					String labelText = textOrNull(label, "label");
					if (labelText != null) {
						if (locale != null) {
							model.add(prop, RDFS.label, labelText, locale);
						}
						else {
							model.add(prop, RDFS.label, labelText);
						}
					}
				}
			}

			// 语义标记
			if ("1".equals(textOrNull(op, "isFunctional"))) {
				model.add(prop, RDF.type, OWL.FunctionalProperty);
			}
			if ("1".equals(textOrNull(op, "isInverseFunctional"))) {
				model.add(prop, RDF.type, OWL.InverseFunctionalProperty);
			}
			if ("1".equals(textOrNull(op, "isTransitive"))) {
				model.add(prop, RDF.type, OWL.TransitiveProperty);
			}
			if ("1".equals(textOrNull(op, "isSymmetric"))) {
				model.add(prop, RDF.type, OWL.SymmetricProperty);
			}

			// inverseOf
			String inverseIri = textOrNull(op, "inverseOfIri");
			if (inverseIri != null) {
				model.add(prop, OWL.inverseOf, model.createProperty(inverseIri));
			}
		}

		// domain
		JsonNode domains = root.get("objectPropertyDomains");
		if (domains != null && domains.isArray()) {
			for (JsonNode d : domains) {
				String propIri = textOrNull(d, "objectPropertyIri");
				String typeIri = textOrNull(d, "entityTypeIri");
				if (propIri != null && typeIri != null) {
					model.add(model.createProperty(propIri), RDFS.domain, model.createResource(typeIri));
				}
			}
		}

		// range
		JsonNode ranges = root.get("objectPropertyRanges");
		if (ranges != null && ranges.isArray()) {
			for (JsonNode r : ranges) {
				String propIri = textOrNull(r, "objectPropertyIri");
				String typeIri = textOrNull(r, "entityTypeIri");
				if (propIri != null && typeIri != null) {
					model.add(model.createProperty(propIri), RDFS.range, model.createResource(typeIri));
				}
			}
		}
	}

	private void buildAxiomDeclarations(Model model, JsonNode root) {
		JsonNode rules = root.get("axiomRules");
		if (rules == null || !rules.isArray()) {
			return;
		}

		for (JsonNode rule : rules) {
			String owlAxiom = textOrNull(rule, "owlAxiom");
			if (owlAxiom == null || owlAxiom.isBlank()) {
				continue;
			}
			try {
				model.read(new java.io.StringReader(owlAxiom), null, "TURTLE");
			}
			catch (Exception e) {
				log.warn("解析快照规则 {} 的 owlAxiom 失败: {}", textOrNull(rule, "ruleCode"), e.getMessage());
			}
		}
	}

	private String textOrNull(JsonNode node, String field) {
		JsonNode val = node.get(field);
		return val != null && !val.isNull() ? val.asText() : null;
	}

}
