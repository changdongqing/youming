/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.model;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.entity.OntInstanceDataValue;
import com.pig4cloud.pig.ontology.entity.OntInstanceObjectRelation;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceDataValueMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceObjectRelationMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据图构建器。
 * <p>
 * 根据规则的目标类型过滤实例，只构建规则作用范围内的实例三元组，
 * 避免全量实例加载（性能优化）。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataGraphBuilder {

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	private final OntEntityInstanceMapper instanceMapper;

	private final OntInstanceDataValueMapper dataValueMapper;

	private final OntInstanceObjectRelationMapper relationMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	/**
	 * 为单条规则构建数据图。
	 * <p>
	 * 只包含规则目标类型及其子类型的实例的 rdf:type + 数据属性值 + 对象属性断言三元组。
	 * </p>
	 * @param ontologyId 本体工程ID
	 * @param targetClassId 目标实体类型ID
	 * @return Jena Model，包含过滤后的实例三元组
	 */
	public Model buildForTargetClass(Long ontologyId, Long targetClassId) {
		Model model = ModelFactory.createDefaultModel();
		if (targetClassId == null) {
			return model;
		}

		// 1. 收集目标类型及其所有子类型
		Set<Long> targetTypeIds = collectDescendants(targetClassId);
		if (targetTypeIds.isEmpty()) {
			return model;
		}

		// 2. 加载这些类型的实例
		List<OntEntityInstance> instances = instanceMapper.selectList(
				Wrappers.<OntEntityInstance>lambdaQuery()
						.eq(OntEntityInstance::getOntologyId, ontologyId)
						.in(OntEntityInstance::getRdfTypeId, targetTypeIds));
		if (instances.isEmpty()) {
			return model;
		}

		Set<Long> instanceIds = instances.stream().map(OntEntityInstance::getId).collect(Collectors.toSet());
		Map<Long, String> instanceIdToIri = instances.stream()
			.collect(Collectors.toMap(OntEntityInstance::getId, OntEntityInstance::getIri));

		// 实例ID -> rdf:type IRI 映射
		Set<Long> typeIds = instances.stream()
			.map(OntEntityInstance::getRdfTypeId)
			.collect(Collectors.toSet());
		Map<Long, String> typeIdToIri = typeIds.isEmpty() ? new HashMap<>() :
				entityTypeMapper.selectBatchIds(typeIds)
					.stream()
					.collect(Collectors.toMap(OntEntityType::getId, OntEntityType::getIri));

		// 3. rdf:type 三元组
		for (OntEntityInstance inst : instances) {
			Resource res = model.createResource(inst.getIri());
			String typeIri = typeIdToIri.get(inst.getRdfTypeId());
			if (typeIri != null) {
				model.add(res, RDF.type, model.createResource(typeIri));
			}
		}

		// 4. 数据属性值三元组
		List<OntInstanceDataValue> values = dataValueMapper.selectList(
				Wrappers.<OntInstanceDataValue>lambdaQuery()
						.in(OntInstanceDataValue::getInstanceId, instanceIds));
		Set<Long> propIds = values.stream()
			.map(OntInstanceDataValue::getDataPropertyId)
			.collect(Collectors.toSet());
		Map<Long, String> propIdToIri = propIds.isEmpty() ? new HashMap<>() :
				dataPropertyMapper.selectBatchIds(propIds)
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

		// 5. 对象属性断言三元组（主体在范围内）
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
	 * 收集实体类型及其所有子类型（递归BFS向下）。
	 * @param rootTypeId 根实体类型ID
	 * @return 包含根类型及其所有子类的ID集合
	 */
	private Set<Long> collectDescendants(Long rootTypeId) {
		Set<Long> result = new HashSet<>();
		Deque<Long> queue = new ArrayDeque<>();
		queue.add(rootTypeId);
		int depth = 0;
		while (!queue.isEmpty() && depth < 100) {
			Long current = queue.poll();
			if (result.add(current)) {
				List<OntEntityTypeHierarchy> children = hierarchyMapper.selectList(
						Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
								.eq(OntEntityTypeHierarchy::getParentId, current));
				for (OntEntityTypeHierarchy h : children) {
					queue.add(h.getChildId());
				}
			}
			depth++;
		}
		return result;
	}

	/**
	 * 创建 RDF 字面量。
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

}
