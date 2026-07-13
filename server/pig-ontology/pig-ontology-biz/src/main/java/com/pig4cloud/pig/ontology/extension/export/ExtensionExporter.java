/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.export;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeDisjoint;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeEquivalent;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel;
import com.pig4cloud.pig.ontology.entity.OntInstanceDataValue;
import com.pig4cloud.pig.ontology.entity.OntInstanceObjectRelation;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionResource;
import com.pig4cloud.pig.ontology.extension.mapper.OntExtensionResourceMapper;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeDisjointMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeEquivalentMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceDataValueMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceObjectRelationMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.serialization.export.NamespacePrefixResolver;
import com.pig4cloud.pig.ontology.serialization.export.PredicateStrategy;
import com.pig4cloud.pig.ontology.serialization.export.PredicateStrategyResolver;
import com.pig4cloud.pig.ontology.serialization.format.RdfFormat;
import com.pig4cloud.pig.ontology.serialization.format.RdfSerializer;
import com.pig4cloud.pig.ontology.serialization.format.RdfSerializerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 扩展模块导出器。
 *
 * <p>按模块关联的资源 ID 集合自行组装 Jena Model 并序列化，
 * 不侵入序列化模块（10）的 SerializationService 接口。
 * 复用 OntologyModelExporter 的底层依赖（Mapper、NamespacePrefixResolver、
 * PredicateStrategyResolver、RdfSerializerRegistry）但不调用其 buildCompleteModel 方法，
 * 避免全量加载核心 Schema。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExtensionExporter {

	private final OntExtensionResourceMapper resourceMapper;

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntEntityTypeLabelMapper entityTypeLabelMapper;

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	private final OntEntityTypeDisjointMapper disjointMapper;

	private final OntEntityTypeEquivalentMapper equivalentMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	private final OntAxiomRuleMapper axiomRuleMapper;

	private final OntEntityInstanceMapper instanceMapper;

	private final OntInstanceDataValueMapper dataValueMapper;

	private final OntInstanceObjectRelationMapper relationMapper;

	private final NamespacePrefixResolver prefixResolver;

	private final PredicateStrategyResolver predicateResolver;

	private final RdfSerializerRegistry serializerRegistry;

	/**
	 * 导出扩展模块为指定RDF格式。
	 * @param module 扩展模块
	 * @param format RDF格式（TURTLE / JSON-LD / RDF-XML / N-TRIPLES）
	 * @return 序列化文本
	 */
	public String exportModule(OntExtensionModule module, String format) {
		// 1. 查询模块关联的所有资源
		List<OntExtensionResource> resources = resourceMapper.selectList(
			Wrappers.<OntExtensionResource>lambdaQuery()
				.eq(OntExtensionResource::getModuleId, module.getId())
				.eq(OntExtensionResource::getDelFlag, "0"));

		Map<String, List<Long>> resourceIdsByType = resources.stream()
			.collect(Collectors.groupingBy(
				OntExtensionResource::getResourceType,
				Collectors.mapping(OntExtensionResource::getResourceId, Collectors.toList())));

		List<Long> entityTypeIds = resourceIdsByType.getOrDefault("ENTITY_TYPE", List.of());
		List<Long> dataPropertyIds = resourceIdsByType.getOrDefault("DATA_PROPERTY", List.of());
		List<Long> objectPropertyIds = resourceIdsByType.getOrDefault("OBJECT_PROPERTY", List.of());
		List<Long> axiomRuleIds = resourceIdsByType.getOrDefault("AXIOM_RULE", List.of());

		// 2. 组装 Model
		Model model = ModelFactory.createDefaultModel();
		prefixResolver.registerPrefixes(model);

		// Schema 层：仅组装扩展资源
		if (!entityTypeIds.isEmpty()) {
			buildClassDeclarations(model, entityTypeIds);
		}
		if (!dataPropertyIds.isEmpty()) {
			buildDataPropertyDeclarations(model, dataPropertyIds);
		}
		if (!objectPropertyIds.isEmpty()) {
			buildObjectPropertyDeclarations(model, objectPropertyIds);
		}
		if (!axiomRuleIds.isEmpty()) {
			buildAxiomDeclarations(model, axiomRuleIds);
		}

		// Data 层：查询扩展实体类型下的实例
		if (!entityTypeIds.isEmpty()) {
			buildDataLayer(model, module.getOntologyId(), entityTypeIds);
		}

		// 3. 序列化
		RdfFormat rdfFormat = parseFormat(format);
		RdfSerializer serializer = serializerRegistry.getSerializer(rdfFormat);
		return serializer.serialize(model);
	}

	private void buildClassDeclarations(Model model, List<Long> typeIds) {
		List<OntEntityType> types = entityTypeMapper.selectBatchIds(typeIds);
		Map<Long, String> idToIri = types.stream()
			.collect(Collectors.toMap(OntEntityType::getId, OntEntityType::getIri));
		Set<Long> typeIdSet = Set.copyOf(typeIds);

		for (OntEntityType type : types) {
			Resource cls = model.createResource(type.getIri());
			model.add(cls, RDF.type, OWL.Class);
			if (type.getDefinition() != null && !type.getDefinition().isBlank()) {
				model.add(cls, RDFS.comment, type.getDefinition());
			}
		}

		// 标签
		List<OntEntityTypeLabel> labels = entityTypeLabelMapper.selectList(
			Wrappers.<OntEntityTypeLabel>lambdaQuery()
				.in(OntEntityTypeLabel::getEntityTypeId, typeIds));
		for (OntEntityTypeLabel label : labels) {
			String iri = idToIri.get(label.getEntityTypeId());
			if (iri != null && label.getLabel() != null) {
				model.add(model.createResource(iri), RDFS.label, label.getLabel(), label.getLocale());
			}
		}

		// 继承关系（仅包含子类在扩展集合中的）；批量查询父类避免 N+1
		List<OntEntityTypeHierarchy> hierarchies = hierarchyMapper.selectList(
			Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
				.in(OntEntityTypeHierarchy::getChildId, typeIds));
		Set<Long> parentIds = hierarchies.stream()
			.map(OntEntityTypeHierarchy::getParentId)
			.filter(pid -> !idToIri.containsKey(pid))
			.collect(Collectors.toSet());
		Map<Long, String> parentIdToIri = parentIds.isEmpty() ? Collections.emptyMap()
				: entityTypeMapper.selectBatchIds(parentIds).stream()
					.collect(Collectors.toMap(OntEntityType::getId, OntEntityType::getIri));
		// 合并扩展集合自身的 IRI 映射，统一查找
		Map<Long, String> allTypeIdToIri = new java.util.HashMap<>(idToIri);
		allTypeIdToIri.putAll(parentIdToIri);
		for (OntEntityTypeHierarchy h : hierarchies) {
			String childIri = idToIri.get(h.getChildId());
			String parentIri = allTypeIdToIri.get(h.getParentId());
			if (childIri != null && parentIri != null) {
				model.add(model.createResource(childIri), RDFS.subClassOf,
					model.createResource(parentIri));
			}
		}

		// 不相交声明（仅查询至少一端在扩展集合中的，避免全表扫描）
		List<OntEntityTypeDisjoint> disjoints = disjointMapper.selectList(
			Wrappers.<OntEntityTypeDisjoint>lambdaQuery()
				.in(OntEntityTypeDisjoint::getTypeA, typeIds)
				.or()
				.in(OntEntityTypeDisjoint::getTypeB, typeIds));
		for (OntEntityTypeDisjoint d : disjoints) {
			if (typeIdSet.contains(d.getTypeA()) && typeIdSet.contains(d.getTypeB())) {
				String iriA = idToIri.get(d.getTypeA());
				String iriB = idToIri.get(d.getTypeB());
				if (iriA != null && iriB != null) {
					model.add(model.createResource(iriA), OWL.disjointWith,
						model.createResource(iriB));
				}
			}
		}

		// 等价类声明；批量查询等价目标类型避免 N+1
		List<OntEntityTypeEquivalent> equivalents = equivalentMapper.selectList(
			Wrappers.<OntEntityTypeEquivalent>lambdaQuery()
				.in(OntEntityTypeEquivalent::getEntityTypeId, typeIds));
		Set<Long> equivalentIds = equivalents.stream()
			.map(OntEntityTypeEquivalent::getEquivalentId)
			.filter(eid -> !idToIri.containsKey(eid))
			.collect(Collectors.toSet());
		Map<Long, String> equivalentIdToIri = equivalentIds.isEmpty() ? Collections.emptyMap()
				: entityTypeMapper.selectBatchIds(equivalentIds).stream()
					.collect(Collectors.toMap(OntEntityType::getId, OntEntityType::getIri));
		Map<Long, String> allEqTypeIdToIri = new java.util.HashMap<>(idToIri);
		allEqTypeIdToIri.putAll(equivalentIdToIri);
		for (OntEntityTypeEquivalent eq : equivalents) {
			String iriA = idToIri.get(eq.getEntityTypeId());
			String eqIri = allEqTypeIdToIri.get(eq.getEquivalentId());
			if (iriA != null && eqIri != null) {
				model.add(model.createResource(iriA), OWL.equivalentClass,
					model.createResource(eqIri));
			}
		}
	}

	private void buildDataPropertyDeclarations(Model model, List<Long> propIds) {
		List<OntDataProperty> properties = dataPropertyMapper.selectBatchIds(propIds);

		for (OntDataProperty dp : properties) {
			String predicateIri = predicateResolver.resolveDataPropertyPredicate(dp,
					PredicateStrategy.PREFERRED_ALIAS);
			Property prop = model.createProperty(predicateIri);
			model.add(prop, RDF.type, OWL.DatatypeProperty);

			if (dp.getDefinition() != null && !dp.getDefinition().isBlank()) {
				model.add(prop, RDFS.comment, dp.getDefinition());
			}

			if (dp.getDomainEntityTypeId() != null) {
				OntEntityType domainType = entityTypeMapper.selectById(dp.getDomainEntityTypeId());
				if (domainType != null) {
					model.add(prop, RDFS.domain, model.createResource(domainType.getIri()));
				}
			}

			if ("1".equals(dp.getIsUnique())) {
				model.add(prop, RDF.type, OWL.FunctionalProperty);
			}
		}
	}

	private void buildObjectPropertyDeclarations(Model model, List<Long> propIds) {
		List<OntObjectProperty> properties = objectPropertyMapper.selectBatchIds(propIds);

		for (OntObjectProperty op : properties) {
			Property prop = model.createProperty(op.getIri());
			model.add(prop, RDF.type, OWL.ObjectProperty);

			if (op.getDefinition() != null && !op.getDefinition().isBlank()) {
				model.add(prop, RDFS.comment, op.getDefinition());
			}

			if ("1".equals(op.getIsFunctional())) {
				model.add(prop, RDF.type, OWL.FunctionalProperty);
			}
			if ("1".equals(op.getIsInverseFunctional())) {
				model.add(prop, RDF.type, OWL.InverseFunctionalProperty);
			}
			if ("1".equals(op.getIsTransitive())) {
				model.add(prop, RDF.type, OWL.TransitiveProperty);
			}
			if ("1".equals(op.getIsSymmetric())) {
				model.add(prop, RDF.type, OWL.SymmetricProperty);
			}
		}
	}

	private void buildAxiomDeclarations(Model model, List<Long> ruleIds) {
		List<OntAxiomRule> rules = axiomRuleMapper.selectBatchIds(ruleIds);

		for (OntAxiomRule rule : rules) {
			String owlAxiom = rule.getOwlAxiom();
			if (owlAxiom == null || owlAxiom.isBlank()) {
				continue;
			}
			try {
				model.read(new java.io.StringReader(owlAxiom), null, "TURTLE");
			}
			catch (Exception e) {
				log.warn("解析规则 {} 的 owlAxiom 失败: {}", rule.getRuleCode(), e.getMessage());
			}
		}
	}

	private void buildDataLayer(Model model, Long ontologyId, List<Long> entityTypeIds) {
		List<OntEntityInstance> instances = instanceMapper.selectList(
			Wrappers.<OntEntityInstance>lambdaQuery()
				.eq(OntEntityInstance::getOntologyId, ontologyId)
				.in(OntEntityInstance::getRdfTypeId, entityTypeIds)
				.eq(OntEntityInstance::getDelFlag, "0"));

		if (instances.isEmpty()) {
			return;
		}

		Map<Long, String> instanceIdToIri = instances.stream()
			.collect(Collectors.toMap(OntEntityInstance::getId, OntEntityInstance::getIri));

		// 实体类型IRI映射
		Set<Long> typeIds = instances.stream()
			.map(OntEntityInstance::getRdfTypeId)
			.collect(Collectors.toSet());
		Map<Long, String> typeIdToIri = typeIds.isEmpty() ? Collections.emptyMap()
				: entityTypeMapper.selectBatchIds(typeIds).stream()
					.collect(Collectors.toMap(OntEntityType::getId, OntEntityType::getIri));

		// rdf:type 三元组
		for (OntEntityInstance inst : instances) {
			if ("REFERENCE_ONLY".equals(inst.getDeclarationMode())) {
				continue;
			}
			String instanceIri = instanceIdToIri.get(inst.getId());
			String typeIri = typeIdToIri.get(inst.getRdfTypeId());
			if (instanceIri != null && typeIri != null) {
				Resource res = model.createResource(instanceIri);
				model.add(res, RDF.type, model.createResource(typeIri));
				if (inst.getLabel() != null && !inst.getLabel().isBlank()) {
					model.add(res, RDFS.label, inst.getLabel());
				}
			}
		}

		// 数据属性值
		Set<Long> instanceIds = instances.stream()
			.map(OntEntityInstance::getId).collect(Collectors.toSet());
		if (!instanceIds.isEmpty()) {
			List<OntInstanceDataValue> values = dataValueMapper.selectList(
				Wrappers.<OntInstanceDataValue>lambdaQuery()
					.in(OntInstanceDataValue::getInstanceId, instanceIds));
			// 批量查询数据属性 IRI 映射，避免逐条 N+1 查询
			Set<Long> dataPropertyIds = values.stream()
				.map(OntInstanceDataValue::getDataPropertyId)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());
			Map<Long, String> dataPropIdToIri = dataPropertyIds.isEmpty()
					? Collections.emptyMap()
					: dataPropertyMapper.selectBatchIds(dataPropertyIds).stream()
						.collect(Collectors.toMap(OntDataProperty::getId,
							dp -> predicateResolver.resolveDataPropertyPredicate(dp,
								PredicateStrategy.PREFERRED_ALIAS)));
			for (OntInstanceDataValue dv : values) {
				String subjectIri = instanceIdToIri.get(dv.getInstanceId());
				String predicateIri = dataPropIdToIri.get(dv.getDataPropertyId());
				if (subjectIri == null || predicateIri == null || dv.getLiteralValue() == null) {
					continue;
				}
				model.add(model.createResource(subjectIri),
					model.createProperty(predicateIri),
					model.createLiteral(dv.getLiteralValue()));
			}
		}

		// 对象属性关系
		if (!instanceIds.isEmpty()) {
			List<OntInstanceObjectRelation> relations = relationMapper.selectList(
				Wrappers.<OntInstanceObjectRelation>lambdaQuery()
					.in(OntInstanceObjectRelation::getSubjectInstanceId, instanceIds));
			for (OntInstanceObjectRelation rel : relations) {
				String subjectIri = instanceIdToIri.get(rel.getSubjectInstanceId());
				if (subjectIri == null) {
					continue;
				}
				OntObjectProperty prop = objectPropertyMapper.selectById(rel.getObjectPropertyId());
				if (prop == null) {
					continue;
				}
				String objectIri = null;
				if ("INSTANCE".equals(rel.getObjectKind())) {
					objectIri = instanceIdToIri.get(rel.getObjectInstanceId());
				}
				else if ("ENTITY_TYPE".equals(rel.getObjectKind())) {
					objectIri = typeIdToIri.get(rel.getObjectEntityTypeId());
				}
				if (objectIri != null) {
					model.add(model.createResource(subjectIri),
						model.createProperty(prop.getIri()),
						model.createResource(objectIri));
				}
			}
		}
	}

	private RdfFormat parseFormat(String format) {
		if (format == null || format.isBlank()) {
			return RdfFormat.TURTLE;
		}
		try {
			return RdfFormat.valueOf(format.replace("-", "_").toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("不支持的RDF格式: " + format);
		}
	}

}
