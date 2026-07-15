/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.export;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntDataPropertyEnum;
import com.pig4cloud.pig.ontology.entity.OntDataPropertyLabel;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeDisjoint;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeEquivalent;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel;
import com.pig4cloud.pig.ontology.entity.OntInstanceDataValue;
import com.pig4cloud.pig.ontology.entity.OntInstanceObjectRelation;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyDomain;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyLabel;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyRange;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyEnumMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeDisjointMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeEquivalentMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceDataValueMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceObjectRelationMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyDomainMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyRangeMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitMapper;
import com.pig4cloud.pig.ontology.version.mapper.OntOntologyVersionMapper;
import com.pig4cloud.pig.ontology.version.serialization.OntologyVersionDeclarationContributor;
import com.pig4cloud.pig.ontology.version.snapshot.SnapshotModelBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 本体模型导出组装器。
 * <p>
 * 从关系库读取 Schema 和实例数据，组装为完整的 Jena {@link Model}。
 * 与校验引擎的 {@code OntologyModelAssembler} 职责分离：
 * 本组装器输出完整 OWL 本体定义（类/属性/公理/实例），用于序列化导出。
 * </p>
 * <p>
 * 每次调用创建独立的 Model，不跨本体工程共享，避免并发污染。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OntologyModelExporter {

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	private final OntEntityTypeDisjointMapper disjointMapper;

	private final OntEntityTypeEquivalentMapper equivalentMapper;

	private final OntEntityTypeLabelMapper entityTypeLabelMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntDataPropertyLabelMapper dataPropertyLabelMapper;

	private final OntDataPropertyEnumMapper dataPropertyEnumMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	private final OntObjectPropertyLabelMapper objectPropertyLabelMapper;

	private final OntObjectPropertyDomainMapper objectPropertyDomainMapper;

	private final OntObjectPropertyRangeMapper objectPropertyRangeMapper;

	private final OntAxiomRuleMapper axiomRuleMapper;

	private final OntEntityInstanceMapper instanceMapper;

	private final OntInstanceDataValueMapper dataValueMapper;

	private final OntInstanceObjectRelationMapper relationMapper;

	private final OntUnitMapper unitMapper;

	private final NamespacePrefixResolver prefixResolver;

	private final PredicateStrategyResolver predicateResolver;

	private final ExportScopeResolver scopeResolver;

	private final OntologyVersionDeclarationContributor versionDeclarationContributor;

	private final SnapshotModelBuilder snapshotModelBuilder;

	private final OntOntologyVersionMapper versionMapper;

	/**
	 * 组装完整本体 Model（Schema + Data）。
	 * @param ontologyId 本体工程ID
	 * @param scope 导出范围
	 * @param predicateStrategy 谓词IRI策略
	 * @param targetTypeFilter 实例子树过滤的实体类型ID（scope=INSTANCE_SUBTREE 时非空）
	 * @return 完整 Jena Model（含前缀映射）
	 */
	public Model buildCompleteModel(Long ontologyId, ExportScope scope,
			PredicateStrategy predicateStrategy, Long targetTypeFilter) {
		return buildCompleteModel(ontologyId, scope, predicateStrategy, targetTypeFilter, null);
	}

	/**
	 * 组装完整本体 Model（Schema + Data），支持版本声明。
	 * @param ontologyId 本体工程ID
	 * @param scope 导出范围
	 * @param predicateStrategy 谓词IRI策略
	 * @param targetTypeFilter 实例子树过滤的实体类型ID（scope=INSTANCE_SUBTREE 时非空）
	 * @param versionId 版本ID（非空时从快照构建历史版本 Model，null 时从当前工作区读取）
	 * @return 完整 Jena Model（含前缀映射和版本声明）
	 */
	public Model buildCompleteModel(Long ontologyId, ExportScope scope,
			PredicateStrategy predicateStrategy, Long targetTypeFilter, Long versionId) {
		Model model = ModelFactory.createDefaultModel();

		// 1. 注册前缀映射
		prefixResolver.registerPrefixes(model);

		if (versionId != null) {
			// 从版本快照构建 Schema 层（历史版本导出）
			buildSchemaLayerFromSnapshot(model, versionId);
		}
		else {
			// 2. 组装 Schema 层（当前工作区）
			if (scope != ExportScope.INSTANCE_ONLY) {
				buildSchemaLayer(model, ontologyId, predicateStrategy);
			}

			// 3. 组装 Data 层（当前工作区）
			if (scope != ExportScope.SCHEMA_ONLY) {
				buildDataLayer(model, ontologyId, predicateStrategy, scope, targetTypeFilter);
			}
		}

		// 4. 添加版本声明（owl:versionIRI 等）
		versionDeclarationContributor.contribute(model, ontologyId, versionId);

		return model;
	}

	/**
	 * 从版本快照构建 Schema 层。
	 */
	private void buildSchemaLayerFromSnapshot(Model model, Long versionId) {
		com.pig4cloud.pig.ontology.version.entity.OntOntologyVersion version = versionMapper
			.selectById(versionId);
		if (version == null || version.getSchemaSnapshot() == null) {
			return;
		}
		// 委托 SnapshotModelBuilder 从快照 JSON 构建
		Model snapshotModel = snapshotModelBuilder.buildFromSnapshot(version.getSchemaSnapshot());
		model.add(snapshotModel);
	}

	// ==================== Schema 层 ====================

	private void buildSchemaLayer(Model model, Long ontologyId, PredicateStrategy predicateStrategy) {
		buildClassDeclarations(model, ontologyId);
		buildDataPropertyDeclarations(model, ontologyId, predicateStrategy);
		buildObjectPropertyDeclarations(model, ontologyId);
		buildAxiomDeclarations(model, ontologyId);
	}

	/**
	 * 组装类声明：owl:Class + rdfs:label + rdfs:comment + subClassOf + disjointWith + equivalentClass。
	 */
	private void buildClassDeclarations(Model model, Long ontologyId) {
		List<OntEntityType> types = entityTypeMapper.selectList(
			Wrappers.<OntEntityType>lambdaQuery().eq(OntEntityType::getOntologyId, ontologyId));

		Set<Long> typeIds = types.stream().map(OntEntityType::getId).collect(Collectors.toSet());
		Map<Long, String> idToIri = types.stream()
			.collect(Collectors.toMap(OntEntityType::getId, OntEntityType::getIri));

		// 1. 类声明 + 定义
		for (OntEntityType type : types) {
			Resource cls = model.createResource(type.getIri());
			model.add(cls, RDF.type, OWL.Class);
			if (type.getDefinition() != null && !type.getDefinition().isBlank()) {
				model.add(cls, RDFS.comment, type.getDefinition());
			}
		}

		// 2. 多语言标签
		if (!typeIds.isEmpty()) {
			List<OntEntityTypeLabel> labels = entityTypeLabelMapper.selectList(
				Wrappers.<OntEntityTypeLabel>lambdaQuery()
					.in(OntEntityTypeLabel::getEntityTypeId, typeIds));
			for (OntEntityTypeLabel label : labels) {
				String iri = idToIri.get(label.getEntityTypeId());
				if (iri != null && label.getLabel() != null) {
					Resource cls = model.createResource(iri);
					model.add(cls, RDFS.label, label.getLabel(), label.getLocale());
				}
			}
		}

		// 3. 继承关系
		if (!typeIds.isEmpty()) {
			List<OntEntityTypeHierarchy> hierarchies = hierarchyMapper.selectList(
				Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
					.in(OntEntityTypeHierarchy::getChildId, typeIds));
			for (OntEntityTypeHierarchy h : hierarchies) {
				String childIri = idToIri.get(h.getChildId());
				String parentIri = idToIri.get(h.getParentId());
				if (childIri != null && parentIri != null) {
					model.add(model.createResource(childIri), RDFS.subClassOf,
						model.createResource(parentIri));
				}
			}
		}

		// 4. 不相交声明
		List<OntEntityTypeDisjoint> disjoints = disjointMapper.selectList(null);
		for (OntEntityTypeDisjoint d : disjoints) {
			if (typeIds.contains(d.getTypeA()) && typeIds.contains(d.getTypeB())) {
				String iriA = idToIri.get(d.getTypeA());
				String iriB = idToIri.get(d.getTypeB());
				model.add(model.createResource(iriA), OWL.disjointWith, model.createResource(iriB));
			}
		}

		// 5. 等价类声明
		if (!typeIds.isEmpty()) {
			List<OntEntityTypeEquivalent> equivalents = equivalentMapper.selectList(
				Wrappers.<OntEntityTypeEquivalent>lambdaQuery()
					.in(OntEntityTypeEquivalent::getEntityTypeId, typeIds));
			for (OntEntityTypeEquivalent eq : equivalents) {
				String iriA = idToIri.get(eq.getEntityTypeId());
				String iriB = idToIri.get(eq.getEquivalentId());
				if (iriA != null && iriB != null) {
					model.add(model.createResource(iriA), OWL.equivalentClass,
						model.createResource(iriB));
				}
			}
		}
	}

	/**
	 * 组装数据属性声明：owl:DatatypeProperty + domain + range + label + FunctionalProperty(若unique)。
	 */
	private void buildDataPropertyDeclarations(Model model, Long ontologyId,
			PredicateStrategy predicateStrategy) {
		List<OntDataProperty> properties = dataPropertyMapper.selectList(
			Wrappers.<OntDataProperty>lambdaQuery().eq(OntDataProperty::getOntologyId, ontologyId));

		Set<Long> propIds = properties.stream().map(OntDataProperty::getId).collect(Collectors.toSet());
		Map<Long, OntEntityType> typeMap = loadEntityTypes(ontologyId);

		// 标签批量加载
		Map<Long, List<OntDataPropertyLabel>> labelMap = new HashMap<>();
		if (!propIds.isEmpty()) {
			List<OntDataPropertyLabel> labels = dataPropertyLabelMapper.selectList(
				Wrappers.<OntDataPropertyLabel>lambdaQuery()
					.in(OntDataPropertyLabel::getDataPropertyId, propIds));
			labelMap = labels.stream().collect(Collectors.groupingBy(OntDataPropertyLabel::getDataPropertyId));
		}

		for (OntDataProperty dp : properties) {
			String predicateIri = predicateResolver.resolveDataPropertyPredicate(dp, predicateStrategy);
			Property prop = model.createProperty(predicateIri);

			// owl:DatatypeProperty
			model.add(prop, RDF.type, OWL.DatatypeProperty);

			// rdfs:label
			List<OntDataPropertyLabel> labels = labelMap.get(dp.getId());
			if (labels != null) {
				for (OntDataPropertyLabel label : labels) {
					if (label.getLabel() != null) {
						model.add(prop, RDFS.label, label.getLabel(), label.getLocale());
					}
				}
			}

			// rdfs:comment (definition)
			if (dp.getDefinition() != null && !dp.getDefinition().isBlank()) {
				model.add(prop, RDFS.comment, dp.getDefinition());
			}

			// rdfs:domain
			if (dp.getDomainEntityTypeId() != null) {
				OntEntityType domainType = typeMap.get(dp.getDomainEntityTypeId());
				if (domainType != null) {
					model.add(prop, RDFS.domain, model.createResource(domainType.getIri()));
				}
			}

			// rdfs:range (baseType -> XSD mapping)
			String xsdType = mapBaseTypeToXsd(dp.getBaseType());
			if (xsdType != null) {
				model.add(prop, RDFS.range, model.createResource(xsdType));
			}

			// FunctionalProperty (unique)
			if ("1".equals(dp.getIsUnique())) {
				model.add(prop, RDF.type, OWL.FunctionalProperty);
			}
		}
	}

	/**
	 * 组装对象属性声明：owl:ObjectProperty + domain/range + 语义标记 + inverseOf。
	 */
	private void buildObjectPropertyDeclarations(Model model, Long ontologyId) {
		List<OntObjectProperty> properties = objectPropertyMapper.selectList(
			Wrappers.<OntObjectProperty>lambdaQuery().eq(OntObjectProperty::getOntologyId, ontologyId));

		Set<Long> propIds = properties.stream().map(OntObjectProperty::getId).collect(Collectors.toSet());
		Map<Long, OntEntityType> typeMap = loadEntityTypes(ontologyId);
		Map<Long, OntObjectProperty> propIdMap = properties.stream()
			.collect(Collectors.toMap(OntObjectProperty::getId, p -> p));

		// domain/range 批量加载
		List<OntObjectPropertyDomain> domains = Collections.emptyList();
		List<OntObjectPropertyRange> ranges = Collections.emptyList();
		if (!propIds.isEmpty()) {
			domains = objectPropertyDomainMapper.selectList(
				Wrappers.<OntObjectPropertyDomain>lambdaQuery()
					.in(OntObjectPropertyDomain::getObjectPropertyId, propIds));
			ranges = objectPropertyRangeMapper.selectList(
				Wrappers.<OntObjectPropertyRange>lambdaQuery()
					.in(OntObjectPropertyRange::getObjectPropertyId, propIds));
		}
		Map<Long, List<OntObjectPropertyDomain>> domainMap = domains.stream()
			.collect(Collectors.groupingBy(OntObjectPropertyDomain::getObjectPropertyId));
		Map<Long, List<OntObjectPropertyRange>> rangeMap = ranges.stream()
			.collect(Collectors.groupingBy(OntObjectPropertyRange::getObjectPropertyId));

		// 标签批量加载
		Map<Long, List<OntObjectPropertyLabel>> labelMap = new HashMap<>();
		if (!propIds.isEmpty()) {
			List<OntObjectPropertyLabel> labels = objectPropertyLabelMapper.selectList(
				Wrappers.<OntObjectPropertyLabel>lambdaQuery()
					.in(OntObjectPropertyLabel::getObjectPropertyId, propIds));
			labelMap = labels.stream().collect(Collectors.groupingBy(OntObjectPropertyLabel::getObjectPropertyId));
		}

		for (OntObjectProperty op : properties) {
			Property prop = model.createProperty(op.getIri());

			// owl:ObjectProperty
			model.add(prop, RDF.type, OWL.ObjectProperty);

			// rdfs:label
			List<OntObjectPropertyLabel> labels = labelMap.get(op.getId());
			if (labels != null) {
				for (OntObjectPropertyLabel label : labels) {
					if (label.getLabel() != null) {
						model.add(prop, RDFS.label, label.getLabel(), label.getLocale());
					}
				}
			}

			// rdfs:comment
			if (op.getDefinition() != null && !op.getDefinition().isBlank()) {
				model.add(prop, RDFS.comment, op.getDefinition());
			}

			// rdfs:domain (单值直接声明)
			List<OntObjectPropertyDomain> dlist = domainMap.get(op.getId());
			if (dlist != null) {
				for (OntObjectPropertyDomain d : dlist) {
					OntEntityType type = typeMap.get(d.getEntityTypeId());
					if (type != null) {
						model.add(prop, RDFS.domain, model.createResource(type.getIri()));
					}
				}
			}

			// rdfs:range (单值直接声明)
			List<OntObjectPropertyRange> rlist = rangeMap.get(op.getId());
			if (rlist != null) {
				for (OntObjectPropertyRange r : rlist) {
					OntEntityType type = typeMap.get(r.getEntityTypeId());
					if (type != null) {
						model.add(prop, RDFS.range, model.createResource(type.getIri()));
					}
				}
			}

			// 语义标记
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

			// inverseOf
			if (op.getInverseOfId() != null) {
				OntObjectProperty inverse = propIdMap.get(op.getInverseOfId());
				if (inverse != null) {
					model.add(prop, OWL.inverseOf, model.createProperty(inverse.getIri()));
				}
			}
		}
	}

	/**
	 * 组装公理声明：解析 owlAxiom Turtle 片段合并到 Model。
	 */
	private void buildAxiomDeclarations(Model model, Long ontologyId) {
		List<OntAxiomRule> rules = axiomRuleMapper.selectList(
			Wrappers.<OntAxiomRule>lambdaQuery().eq(OntAxiomRule::getOntologyId, ontologyId));

		for (OntAxiomRule rule : rules) {
			String owlAxiom = rule.getOwlAxiom();
			if (owlAxiom == null || owlAxiom.isBlank()) {
				continue;
			}
			try {
				model.read(new StringReader(owlAxiom), null, "TURTLE");
			}
			catch (Exception e) {
				log.warn("解析规则 {} 的 owlAxiom 失败: {}", rule.getRuleCode(), e.getMessage());
			}
		}
	}

	// ==================== Data 层 ====================

	private void buildDataLayer(Model model, Long ontologyId, PredicateStrategy predicateStrategy,
			ExportScope scope, Long targetTypeFilter) {
		List<OntEntityInstance> instances = scopeResolver.loadInstances(ontologyId, scope, targetTypeFilter);
		if (instances.isEmpty()) {
			return;
		}

		Map<Long, String> instanceIdToIri = instances.stream()
			.collect(Collectors.toMap(OntEntityInstance::getId, OntEntityInstance::getIri));

		// 加载实体类型IRI映射
		Set<Long> typeIds = instances.stream()
			.map(OntEntityInstance::getRdfTypeId)
			.collect(Collectors.toSet());
		Map<Long, String> typeIdToIri = typeIds.isEmpty() ? new HashMap<>() :
			entityTypeMapper.selectBatchIds(typeIds).stream()
				.collect(Collectors.toMap(OntEntityType::getId, OntEntityType::getIri));

		buildInstanceTypeTriples(model, instances, instanceIdToIri, typeIdToIri);

		// 数据属性IRI映射（按谓词策略解析）
		Map<Long, String> dataPropIdToIri = loadDataPropertyIriMap(ontologyId, predicateStrategy);

		buildDataValueTriples(model, instances, instanceIdToIri, dataPropIdToIri);

		// 对象属性IRI映射（对象属性始终用内部IRI）
		Map<Long, String> objPropIdToIri = loadObjectPropertyIriMap(ontologyId);

		buildObjectRelationTriples(model, instances, instanceIdToIri, objPropIdToIri, typeIdToIri);
	}

	private void buildInstanceTypeTriples(Model model, List<OntEntityInstance> instances,
			Map<Long, String> instanceIdToIri, Map<Long, String> typeIdToIri) {
		for (OntEntityInstance inst : instances) {
			// REFERENCE_ONLY 实例不输出 rdf:type 三元组
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
	}

	private void buildDataValueTriples(Model model, List<OntEntityInstance> instances,
			Map<Long, String> instanceIdToIri, Map<Long, String> propIdToIri) {
		Set<Long> instanceIds = instances.stream().map(OntEntityInstance::getId).collect(Collectors.toSet());
		if (instanceIds.isEmpty()) {
			return;
		}
		List<OntInstanceDataValue> values = dataValueMapper.selectList(
			Wrappers.<OntInstanceDataValue>lambdaQuery()
				.in(OntInstanceDataValue::getInstanceId, instanceIds));

		// 加载单位映射（UNIT_REF 值回退查 ont_unit.unit_symbol）
		Set<Long> unitIds = values.stream()
			.map(OntInstanceDataValue::getUnitId)
			.filter(id -> id != null)
			.collect(Collectors.toSet());
		Map<Long, String> unitIdToSymbol = unitIds.isEmpty() ? new HashMap<>() :
			unitMapper.selectBatchIds(unitIds).stream()
				.collect(Collectors.toMap(OntUnit::getId, OntUnit::getUnitSymbol));

		for (OntInstanceDataValue dv : values) {
			String subjectIri = instanceIdToIri.get(dv.getInstanceId());
			String propertyIri = propIdToIri.get(dv.getDataPropertyId());
			if (subjectIri == null || propertyIri == null) {
				continue;
			}
			RDFNode object = createLiteralNode(model, dv, unitIdToSymbol);
			if (object != null) {
				Resource subject = model.createResource(subjectIri);
				Property predicate = model.createProperty(propertyIri);
				model.add(subject, predicate, object);
			}
		}
	}

	private void buildObjectRelationTriples(Model model, List<OntEntityInstance> instances,
			Map<Long, String> instanceIdToIri, Map<Long, String> propIdToIri,
			Map<Long, String> typeIdToIri) {
		Set<Long> instanceIds = instances.stream().map(OntEntityInstance::getId).collect(Collectors.toSet());
		if (instanceIds.isEmpty()) {
			return;
		}
		List<OntInstanceObjectRelation> relations = relationMapper.selectList(
			Wrappers.<OntInstanceObjectRelation>lambdaQuery()
				.in(OntInstanceObjectRelation::getSubjectInstanceId, instanceIds));

		for (OntInstanceObjectRelation rel : relations) {
			String subjectIri = instanceIdToIri.get(rel.getSubjectInstanceId());
			String propertyIri = propIdToIri.get(rel.getObjectPropertyId());
			if (subjectIri == null || propertyIri == null) {
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
				Resource subject = model.createResource(subjectIri);
				Property predicate = model.createProperty(propertyIri);
				model.add(subject, predicate, model.createResource(objectIri));
			}
		}
	}

	// ==================== 辅助方法 ====================

	/**
	 * 创建 RDF 字面量（与 OntologyModelAssembler.createLiteralNode 逻辑一致）。
	 * UNIT_REF 模式使用 literal_symbol（优先）或 ont_unit.unit_symbol（回退）。
	 */
	private RDFNode createLiteralNode(Model model, OntInstanceDataValue dv,
			Map<Long, String> unitIdToSymbol) {
		String value = dv.getLiteralValue();
		if (value == null) {
			// UNIT_REF 模式：值来自 literal_symbol 或 ont_unit.unit_symbol
			if (dv.getLiteralSymbol() != null) {
				return model.createLiteral(dv.getLiteralSymbol());
			}
			if (dv.getUnitId() != null) {
				String symbol = unitIdToSymbol.get(dv.getUnitId());
				if (symbol != null) {
					return model.createLiteral(symbol);
				}
			}
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
			case "DATETIME":
				// ISO-8601 格式值，直接映射为 xsd:dateTime
				return model.createTypedLiteral(value, XSDDatatype.XSDdateTime);
			case "URI":
				return model.createResource(value);
			default:
				return model.createLiteral(value);
		}
	}

	/**
	 * baseType -> XSD 类型映射。
	 */
	private String mapBaseTypeToXsd(String baseType) {
		if (baseType == null) {
			return null;
		}
		switch (baseType) {
			case "BOOLEAN":
				return XSDDatatype.XSDboolean.getURI();
			case "DATE":
				return XSDDatatype.XSDdate.getURI();
			case "DATETIME":
				return XSDDatatype.XSDdateTime.getURI();
			case "INTEGER":
				return XSDDatatype.XSDinteger.getURI();
			case "DECIMAL":
			case "NUMERIC":
				return XSDDatatype.XSDdecimal.getURI();
			case "TEXT":
			case "STRING":
				return XSDDatatype.XSDstring.getURI();
			case "URI":
				return "http://www.w3.org/2001/XMLSchema#anyURI";
			default:
				return XSDDatatype.XSDstring.getURI();
		}
	}

	private Map<Long, OntEntityType> loadEntityTypes(Long ontologyId) {
		List<OntEntityType> types = entityTypeMapper.selectList(
			Wrappers.<OntEntityType>lambdaQuery().eq(OntEntityType::getOntologyId, ontologyId));
		return types.stream().collect(Collectors.toMap(OntEntityType::getId, t -> t));
	}

	private Map<Long, String> loadDataPropertyIriMap(Long ontologyId, PredicateStrategy predicateStrategy) {
		List<OntDataProperty> properties = dataPropertyMapper.selectList(
			Wrappers.<OntDataProperty>lambdaQuery().eq(OntDataProperty::getOntologyId, ontologyId));
		Map<Long, String> result = new HashMap<>();
		for (OntDataProperty dp : properties) {
			result.put(dp.getId(), predicateResolver.resolveDataPropertyPredicate(dp, predicateStrategy));
		}
		return result;
	}

	private Map<Long, String> loadObjectPropertyIriMap(Long ontologyId) {
		List<OntObjectProperty> properties = objectPropertyMapper.selectList(
			Wrappers.<OntObjectProperty>lambdaQuery().eq(OntObjectProperty::getOntologyId, ontologyId));
		return properties.stream()
			.collect(Collectors.toMap(OntObjectProperty::getId, OntObjectProperty::getIri));
	}

}
