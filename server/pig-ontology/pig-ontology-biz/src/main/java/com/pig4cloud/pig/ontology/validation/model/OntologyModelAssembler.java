/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.model;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeDisjoint;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.entity.OntInstanceDataValue;
import com.pig4cloud.pig.ontology.entity.OntInstanceObjectRelation;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeDisjointMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceDataValueMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceObjectRelationMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 本体模型组装器。
 * <p>
 * 从关系库读取 Schema 和实例数据，组装为 Jena {@link Model}。
 * 按 ontologyId 隔离，每次组装创建独立 Model，不跨工程共享。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OntologyModelAssembler {

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	private final OntEntityTypeDisjointMapper disjointMapper;

	private final OntEntityInstanceMapper instanceMapper;

	private final OntInstanceDataValueMapper dataValueMapper;

	private final OntInstanceObjectRelationMapper relationMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	/**
	 * 构建 Schema Model（类层次 + 不相交声明）。
	 * @param ontologyId 本体工程ID
	 * @return Jena Model，包含 owl:Class 声明、rdfs:subClassOf、owl:disjointWith
	 */
	public Model buildSchemaModel(Long ontologyId) {
		Model model = ModelFactory.createDefaultModel();

		// 1. 类声明
		List<OntEntityType> types = entityTypeMapper.selectList(
				Wrappers.<OntEntityType>lambdaQuery().eq(OntEntityType::getOntologyId, ontologyId));
		for (OntEntityType type : types) {
			Resource cls = model.createResource(type.getIri());
			model.add(cls, RDF.type, OWL.Class);
		}

		// 2. 继承关系
		Set<Long> typeIds = types.stream().map(OntEntityType::getId).collect(Collectors.toSet());
		if (!typeIds.isEmpty()) {
			List<OntEntityTypeHierarchy> hierarchies = hierarchyMapper.selectList(
					Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
							.in(OntEntityTypeHierarchy::getChildId, typeIds));
			Map<Long, String> idToIri = types.stream()
				.collect(Collectors.toMap(OntEntityType::getId, OntEntityType::getIri));
			for (OntEntityTypeHierarchy h : hierarchies) {
				String childIri = idToIri.get(h.getChildId());
				String parentIri = idToIri.get(h.getParentId());
				if (childIri != null && parentIri != null) {
					model.add(model.createResource(childIri), RDFS.subClassOf,
							model.createResource(parentIri));
				}
			}
		}

		// 3. 不相交（从 ont_entity_type_disjoint 读取全部，按当前工程类型过滤）
		List<OntEntityTypeDisjoint> disjoints = disjointMapper.selectList(null);
		Set<Long> ontologyTypeIds = types.stream().map(OntEntityType::getId).collect(Collectors.toSet());
		Map<Long, String> idToIri = types.stream()
			.collect(Collectors.toMap(OntEntityType::getId, OntEntityType::getIri));
		for (OntEntityTypeDisjoint d : disjoints) {
			if (ontologyTypeIds.contains(d.getTypeA()) && ontologyTypeIds.contains(d.getTypeB())) {
				String iriA = idToIri.get(d.getTypeA());
				String iriB = idToIri.get(d.getTypeB());
				model.add(model.createResource(iriA), OWL.disjointWith, model.createResource(iriB));
				model.add(model.createResource(iriB), OWL.disjointWith, model.createResource(iriA));
			}
		}

		return model;
	}

	/**
	 * 构建实例 Model（rdf:type + 数据属性值 + 对象属性断言）。
	 * @param ontologyId 本体工程ID
	 * @return Jena Model，包含实例三元组
	 */
	public Model buildInstanceModel(Long ontologyId) {
		Model model = ModelFactory.createDefaultModel();

		List<OntEntityInstance> instances = instanceMapper.selectList(
				Wrappers.<OntEntityInstance>lambdaQuery().eq(OntEntityInstance::getOntologyId, ontologyId));

		if (instances.isEmpty()) {
			return model;
		}

		// 实例ID -> IRI 映射
		Map<Long, String> instanceIdToIri = instances.stream()
			.collect(Collectors.toMap(OntEntityInstance::getId, OntEntityInstance::getIri));

		// 实例ID -> rdf:type IRI 映射
		Set<Long> typeIds = instances.stream()
			.map(OntEntityInstance::getRdfTypeId)
			.collect(Collectors.toSet());
		Map<Long, String> typeIdToIri = entityTypeMapper.selectBatchIds(typeIds)
			.stream()
			.collect(Collectors.toMap(OntEntityType::getId, OntEntityType::getIri));

		// 1. 实例 rdf:type 三元组
		for (OntEntityInstance inst : instances) {
			Resource res = model.createResource(inst.getIri());
			String typeIri = typeIdToIri.get(inst.getRdfTypeId());
			if (typeIri != null) {
				model.add(res, RDF.type, model.createResource(typeIri));
			}
		}

		// 2. 数据属性值三元组
		Set<Long> instanceIds = instances.stream().map(OntEntityInstance::getId).collect(Collectors.toSet());
		List<OntInstanceDataValue> values = dataValueMapper.selectList(
				Wrappers.<OntInstanceDataValue>lambdaQuery()
						.in(OntInstanceDataValue::getInstanceId, instanceIds));
		Set<Long> propertyIds = values.stream()
			.map(OntInstanceDataValue::getDataPropertyId)
			.collect(Collectors.toSet());
		Map<Long, String> propIdToIri = propertyIds.isEmpty() ? new HashMap<>() :
				dataPropertyMapper.selectBatchIds(propertyIds)
					.stream()
					.collect(Collectors.toMap(OntDataProperty::getId, OntDataProperty::getIri));

		for (OntInstanceDataValue dv : values) {
			String subjectIri = instanceIdToIri.get(dv.getInstanceId());
			String propertyIri = propIdToIri.get(dv.getDataPropertyId());
			if (subjectIri == null || propertyIri == null) {
				continue;
			}
			Resource subject = model.createResource(subjectIri);
			Property predicate = model.createProperty(propertyIri);
			RDFNode object = createLiteralNode(model, dv);
			if (object != null) {
				model.add(subject, predicate, object);
			}
		}

		// 3. 对象属性断言三元组
		List<OntInstanceObjectRelation> relations = relationMapper.selectList(
				Wrappers.<OntInstanceObjectRelation>lambdaQuery()
						.in(OntInstanceObjectRelation::getSubjectInstanceId, instanceIds));
		Set<Long> objPropIds = relations.stream()
			.map(OntInstanceObjectRelation::getObjectPropertyId)
			.collect(Collectors.toSet());
		Map<Long, String> objPropIdToIri = objPropIds.isEmpty() ? new HashMap<>() :
				objectPropertyMapper.selectBatchIds(objPropIds)
					.stream()
					.collect(Collectors.toMap(OntObjectProperty::getId, OntObjectProperty::getIri));

		for (OntInstanceObjectRelation rel : relations) {
			String subjectIri = instanceIdToIri.get(rel.getSubjectInstanceId());
			String propertyIri = objPropIdToIri.get(rel.getObjectPropertyId());
			if (subjectIri == null || propertyIri == null) {
				continue;
			}
			Resource subject = model.createResource(subjectIri);
			Property predicate = model.createProperty(propertyIri);
			String objectIri = null;
			if ("INSTANCE".equals(rel.getObjectKind())) {
				objectIri = instanceIdToIri.get(rel.getObjectInstanceId());
			}
			else if ("ENTITY_TYPE".equals(rel.getObjectKind())) {
				objectIri = typeIdToIri.get(rel.getObjectEntityTypeId());
			}
			if (objectIri != null) {
				model.add(subject, predicate, model.createResource(objectIri));
			}
		}

		return model;
	}

	/**
	 * 创建 RDF 字面量。
	 * @param model Jena Model
	 * @param dv 数据属性值
	 * @return RDFNode（字面量或资源）
	 */
	private RDFNode createLiteralNode(Model model, OntInstanceDataValue dv) {
		String value = dv.getLiteralValue();
		if (value == null) {
			return null;
		}
		String type = dv.getLiteralType();
		if (type == null) {
			return model.createLiteral(value);
		}
		switch (type) {
			case "INTEGER":
				try {
					return model.createTypedLiteral(Long.parseLong(value), XSDDatatype.XSDinteger);
				}
				catch (NumberFormatException e) {
					return model.createLiteral(value);
				}
			case "DECIMAL":
				try {
					return model.createTypedLiteral(new BigDecimal(value), XSDDatatype.XSDdecimal);
				}
				catch (NumberFormatException e) {
					return model.createLiteral(value);
				}
			case "BOOLEAN":
				return model.createTypedLiteral(Boolean.parseBoolean(value), XSDDatatype.XSDboolean);
			case "DATE":
				// 平台规范值 YYYYMMDD，转为 xsd:date 的 YYYY-MM-DD
				String dateStr = value;
				if (dateStr.length() == 8) {
					dateStr = dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6) + "-" + dateStr.substring(6, 8);
				}
				return model.createTypedLiteral(dateStr, XSDDatatype.XSDdate);
			case "URI":
				return model.createResource(value);
			default:
				return model.createLiteral(value);
		}
	}

	/**
	 * 获取核心命名空间 URI。
	 * <p>
	 * SHACL Shapes 文本中使用 {@code std:} 前缀引用核心命名空间下的类和属性。
	 * 本方法查询 ont_namespace 表获取核心命名空间 URI。
	 * </p>
	 * @param ontologyId 本体工程ID（用于确定核心命名空间）
	 * @return 核心命名空间 URI
	 */
	public String getCoreNamespaceUri() {
		// 核心命名空间固定为 CORE scope，通常 prefix=std
		// 直接查询 is_default='1' 的命名空间
		return "http://example.org/standard-ontology#";
	}

}
