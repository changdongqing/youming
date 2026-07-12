/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.imp;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 实例导入映射器。
 * <p>
 * 将解析后的 Jena Model 映射为内部实例数据结构。
 * 仅处理实例数据三元组，Schema 声明三元组被跳过。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstanceImportMapper {

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	private final OntEntityInstanceMapper instanceMapper;

	private final OntUnitMapper unitMapper;

	/** OWL/RDFS 标准类型IRI集合，用于过滤Schema三元组 */
	private static final Set<String> SCHEMA_TYPES = Set.of(
		OWL.Class.getURI(),
		OWL.DatatypeProperty.getURI(),
		OWL.ObjectProperty.getURI(),
		OWL.FunctionalProperty.getURI(),
		OWL.InverseFunctionalProperty.getURI(),
		OWL.TransitiveProperty.getURI(),
		OWL.SymmetricProperty.getURI(),
		OWL.AnnotationProperty.getURI(),
		OWL.Ontology.getURI()
	);

	/**
	 * 将 Model 映射为导入预检结果。
	 * @param model 解析后的 Model
	 * @param ontologyId 目标本体工程ID
	 * @return 预检结果（不写入数据库）
	 */
	public ImportPreviewResult mapToPreview(Model model, Long ontologyId) {
		ImportPreviewResult preview = new ImportPreviewResult();
		preview.setOntologyId(ontologyId);

		// 构建IRI→实体类型映射
		Map<String, OntEntityType> typeIriMap = buildEntityTypeIriMap(ontologyId);

		// 构建谓词IRI→数据属性/对象属性映射
		Map<String, OntDataProperty> dataPropIriMap = buildDataPropertyIriMap(ontologyId);
		Map<String, OntObjectProperty> objPropIriMap = buildObjectPropertyIriMap(ontologyId);

		// 构建已有实例IRI集合（用于冲突检测）
		Set<String> existingIris = buildExistingInstanceIris(ontologyId);

		// 遍历 Model 三元组
		Map<String, ImportPreviewResult.ImportInstance> instanceMap = new LinkedHashMap<>();

		for (StmtIterator it = model.listStatements(); it.hasNext(); ) {
			Statement stmt = it.next();
			String subject = stmt.getSubject().getURI();
			Property predicate = stmt.getPredicate();
			RDFNode object = stmt.getObject();

			// 跳过 Schema 声明三元组
			if (isSchemaStatement(stmt)) {
				preview.incrementSchemaSkipped();
				continue;
			}

			// rdf:type 三元组 → 实例声明
			if (predicate.equals(RDF.type)) {
				if (!object.isResource()) {
					continue;
				}
				String typeUri = object.asResource().getURI();
				if (SCHEMA_TYPES.contains(typeUri)) {
					preview.incrementSchemaSkipped();
					continue;
				}
				ImportPreviewResult.ImportInstance instance = instanceMap.computeIfAbsent(subject,
					iri -> new ImportPreviewResult.ImportInstance());
				instance.setIri(subject);
				OntEntityType type = typeIriMap.get(typeUri);
				if (type != null) {
					instance.setRdfTypeId(type.getId());
					instance.setRdfTypeIri(typeUri);
				}
				else {
					preview.addError(subject, "rdf:type " + typeUri + " 对应的实体类型不存在");
				}
				continue;
			}

			// rdfs:label 三元组 → 实例标签
			if (predicate.equals(RDFS.label) && object.isLiteral()) {
				ImportPreviewResult.ImportInstance instance = instanceMap.computeIfAbsent(subject,
					iri -> new ImportPreviewResult.ImportInstance());
				instance.setIri(subject);
				instance.setLabel(object.asLiteral().getString());
				continue;
			}

			// rdfs:comment / rdfs:subClassOf / rdfs:domain / rdfs:range 等Schema谓词跳过
			if (predicate.equals(RDFS.subClassOf) || predicate.equals(RDFS.domain)
				|| predicate.equals(RDFS.range) || predicate.equals(OWL.disjointWith)
				|| predicate.equals(OWL.equivalentClass) || predicate.equals(OWL.inverseOf)) {
				preview.incrementSchemaSkipped();
				continue;
			}

			// 数据属性值三元组（object 是 Literal）
			if (object.isLiteral()) {
				ImportPreviewResult.ImportInstance instance = instanceMap.computeIfAbsent(subject,
					iri -> new ImportPreviewResult.ImportInstance());
				instance.setIri(subject);
				ImportPreviewResult.ImportDataValue dv = new ImportPreviewResult.ImportDataValue();
				dv.setPredicateIri(predicate.getURI());
				dv.setLiteralValue(object.asLiteral().getString());
				dv.setLiteralType(inferLiteralType(object.asLiteral()));
				OntDataProperty dp = dataPropIriMap.get(predicate.getURI());
				if (dp != null) {
					dv.setDataPropertyId(dp.getId());
					// UNIT_REF 类型：尝试按符号解析单位
					if ("UNIT_REF".equals(dp.getValueMode())) {
						resolveUnitBySymbol(dv, object.asLiteral().getString(), ontologyId);
					}
				}
				else {
					preview.addWarning(subject, "谓词 " + predicate.getURI()
						+ " 未匹配到已知数据属性");
				}
				instance.addDataValue(dv);
				continue;
			}

			// 对象属性断言三元组（object 是 Resource）
			if (object.isResource()) {
				ImportPreviewResult.ImportInstance instance = instanceMap.computeIfAbsent(subject,
					iri -> new ImportPreviewResult.ImportInstance());
				instance.setIri(subject);
				ImportPreviewResult.ImportObjectRelation rel = new ImportPreviewResult.ImportObjectRelation();
				rel.setPredicateIri(predicate.getURI());
				rel.setObjectIri(object.asResource().getURI());
				rel.setObjectKind("INSTANCE");
				OntObjectProperty op = objPropIriMap.get(predicate.getURI());
				if (op != null) {
					rel.setObjectPropertyId(op.getId());
				}
				else {
					preview.addWarning(subject, "谓词 " + predicate.getURI()
						+ " 未匹配到已知对象属性");
				}
				instance.addObjectRelation(rel);
			}
		}

		// 冲突检测
		for (ImportPreviewResult.ImportInstance instance : instanceMap.values()) {
			if (existingIris.contains(instance.getIri())) {
				instance.setConflictType("IRI_EXISTS");
				preview.incrementConflictCount();
			}
		}

		preview.setInstances(List.copyOf(instanceMap.values()));
		return preview;
	}

	/**
	 * 判断是否为 Schema 声明三元组。
	 */
	private boolean isSchemaStatement(Statement stmt) {
		Property predicate = stmt.getPredicate();
		return predicate.equals(RDFS.subClassOf)
			|| predicate.equals(RDFS.domain)
			|| predicate.equals(RDFS.range)
			|| predicate.equals(OWL.disjointWith)
			|| predicate.equals(OWL.equivalentClass)
			|| predicate.equals(OWL.inverseOf);
	}

	private String inferLiteralType(org.apache.jena.rdf.model.Literal literal) {
		String datatypeUri = literal.getDatatypeURI();
		if (datatypeUri == null) {
			return "STRING";
		}
		if (datatypeUri.contains("integer")) {
			return "INTEGER";
		}
		if (datatypeUri.contains("decimal") || datatypeUri.contains("double") || datatypeUri.contains("float")) {
			return "DECIMAL";
		}
		if (datatypeUri.contains("boolean")) {
			return "BOOLEAN";
		}
		if (datatypeUri.contains("date")) {
			return "DATE";
		}
		if (datatypeUri.contains("anyURI")) {
			return "URI";
		}
		return "STRING";
	}

	private void resolveUnitBySymbol(ImportPreviewResult.ImportDataValue dv, String symbol, Long ontologyId) {
		OntUnit unit = unitMapper.selectOne(
			Wrappers.<OntUnit>lambdaQuery()
				.eq(OntUnit::getUnitSymbol, symbol)
				.last("LIMIT 1"));
		if (unit != null) {
			dv.setUnitId(unit.getId());
			dv.setUnitSymbol(symbol);
		}
	}

	private Map<String, OntEntityType> buildEntityTypeIriMap(Long ontologyId) {
		List<OntEntityType> types = entityTypeMapper.selectList(
			Wrappers.<OntEntityType>lambdaQuery().eq(OntEntityType::getOntologyId, ontologyId));
		return types.stream().collect(Collectors.toMap(OntEntityType::getIri, t -> t, (a, b) -> a));
	}

	private Map<String, OntDataProperty> buildDataPropertyIriMap(Long ontologyId) {
		List<OntDataProperty> props = dataPropertyMapper.selectList(
			Wrappers.<OntDataProperty>lambdaQuery().eq(OntDataProperty::getOntologyId, ontologyId));
		// 同时按 iri 和 standardIri 和 preferredAlias 建索引
		Map<String, OntDataProperty> map = new LinkedHashMap<>();
		for (OntDataProperty dp : props) {
			map.put(dp.getIri(), dp);
			if (dp.getStandardIri() != null && !dp.getStandardIri().isBlank()) {
				map.putIfAbsent(dp.getStandardIri(), dp);
			}
			if (dp.getPreferredAlias() != null && !dp.getPreferredAlias().isBlank()) {
				map.putIfAbsent(dp.getPreferredAlias(), dp);
			}
		}
		return map;
	}

	private Map<String, OntObjectProperty> buildObjectPropertyIriMap(Long ontologyId) {
		List<OntObjectProperty> props = objectPropertyMapper.selectList(
			Wrappers.<OntObjectProperty>lambdaQuery().eq(OntObjectProperty::getOntologyId, ontologyId));
		return props.stream().collect(Collectors.toMap(OntObjectProperty::getIri, p -> p, (a, b) -> a));
	}

	private Set<String> buildExistingInstanceIris(Long ontologyId) {
		List<OntEntityInstance> instances = instanceMapper.selectList(
			Wrappers.<OntEntityInstance>lambdaQuery()
				.eq(OntEntityInstance::getOntologyId, ontologyId)
				.select(OntEntityInstance::getIri));
		return instances.stream().map(OntEntityInstance::getIri).collect(Collectors.toSet());
	}

}
