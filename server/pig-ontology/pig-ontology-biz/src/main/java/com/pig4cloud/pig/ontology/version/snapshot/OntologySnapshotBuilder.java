/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.snapshot;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntAxiomRuleTarget;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntDataPropertyEnum;
import com.pig4cloud.pig.ontology.entity.OntDataPropertyLabel;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeDisjoint;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeEquivalent;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyDomain;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyLabel;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyRange;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionResource;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleMapper;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleTargetMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyEnumMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeDisjointMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeEquivalentMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyDomainMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyRangeMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitMapper;
import com.pig4cloud.pig.ontology.extension.mapper.OntExtensionModuleMapper;
import com.pig4cloud.pig.ontology.extension.mapper.OntExtensionResourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 读取工作区全部 Schema 元素构建全量快照 JSON。
 * <p>
 * 快照结构按设计文档 §4.4 定义，包含 15 个数组段。
 * 此类只负责读取数据并组装为 Map 结构，不做规范化排序和 hash 计算（由
 * {@link OntologySnapshotCanonicalizer} 负责）。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OntologySnapshotBuilder {

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntEntityTypeLabelMapper entityTypeLabelMapper;

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	private final OntEntityTypeDisjointMapper disjointMapper;

	private final OntEntityTypeEquivalentMapper equivalentMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntDataPropertyLabelMapper dataPropertyLabelMapper;

	private final OntDataPropertyEnumMapper dataPropertyEnumMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	private final OntObjectPropertyLabelMapper objectPropertyLabelMapper;

	private final OntObjectPropertyDomainMapper objectPropertyDomainMapper;

	private final OntObjectPropertyRangeMapper objectPropertyRangeMapper;

	private final OntAxiomRuleMapper axiomRuleMapper;

	private final OntAxiomRuleTargetMapper axiomRuleTargetMapper;

	private final OntNamespaceMapper namespaceMapper;

	private final OntUnitMapper unitMapper;

	private final OntExtensionModuleMapper extensionModuleMapper;

	private final OntExtensionResourceMapper extensionResourceMapper;

	private final ObjectMapper objectMapper;

	/**
	 * 构建全量快照。
	 * @param ontologyId 本体工程ID
	 * @param ontologyIri 本体IRI
	 * @param versionIri 版本IRI
	 * @return 快照 JSON 字符串
	 */
	public String buildSnapshot(Long ontologyId, String ontologyIri, String versionIri) {
		Map<String, Object> snapshot = new HashMap<>();
		snapshot.put("snapshotFormatVersion", 1);

		Map<String, String> ontology = new HashMap<>();
		ontology.put("ontologyIri", ontologyIri);
		ontology.put("versionIri", versionIri);
		snapshot.put("ontology", ontology);

		snapshot.put("namespaces", buildNamespaces());
		snapshot.put("entityTypes", buildEntityTypes(ontologyId));
		snapshot.put("entityTypeHierarchies", buildHierarchies(ontologyId));
		snapshot.put("entityTypeDisjoints", buildDisjoints(ontologyId));
		snapshot.put("entityTypeEquivalents", buildEquivalents(ontologyId));
		snapshot.put("dataProperties", buildDataProperties(ontologyId));
		snapshot.put("dataPropertyEnums", buildDataPropertyEnums(ontologyId));
		snapshot.put("objectProperties", buildObjectProperties(ontologyId));
		snapshot.put("objectPropertyDomains", buildObjectPropertyDomains(ontologyId));
		snapshot.put("objectPropertyRanges", buildObjectPropertyRanges(ontologyId));
		snapshot.put("axiomRules", buildAxiomRules(ontologyId));
		snapshot.put("axiomRuleTargets", buildAxiomRuleTargets(ontologyId));
		snapshot.put("extensionDependencies", buildExtensionDependencies(ontologyId));
		snapshot.put("unitDependencies", buildUnitDependencies(ontologyId));

		try {
			return objectMapper.writeValueAsString(snapshot);
		}
		catch (Exception e) {
			throw new RuntimeException("构建快照JSON失败", e);
		}
	}

	private List<Map<String, Object>> buildNamespaces() {
		List<OntNamespace> namespaces = namespaceMapper
			.selectList(Wrappers.<OntNamespace>lambdaQuery().eq(OntNamespace::getDelFlag, "0"));
		List<Map<String, Object>> result = new ArrayList<>();
		for (OntNamespace ns : namespaces) {
			Map<String, Object> item = new HashMap<>();
			item.put("prefix", ns.getPrefix());
			item.put("uri", ns.getUri());
			item.put("isDefault", ns.getIsDefault());
			result.add(item);
		}
		return result;
	}

	private List<Map<String, Object>> buildEntityTypes(Long ontologyId) {
		List<OntEntityType> types = entityTypeMapper.selectList(
			Wrappers.<OntEntityType>lambdaQuery().eq(OntEntityType::getOntologyId, ontologyId));

		Set<Long> typeIds = types.stream().map(OntEntityType::getId).collect(Collectors.toSet());

		Map<Long, List<OntEntityTypeLabel>> labelMap = new HashMap<>();
		if (!typeIds.isEmpty()) {
			List<OntEntityTypeLabel> labels = entityTypeLabelMapper.selectList(
				Wrappers.<OntEntityTypeLabel>lambdaQuery().in(OntEntityTypeLabel::getEntityTypeId, typeIds));
			labelMap = labels.stream().collect(Collectors.groupingBy(OntEntityTypeLabel::getEntityTypeId));
		}

		List<Map<String, Object>> result = new ArrayList<>();
		for (OntEntityType type : types) {
			Map<String, Object> item = new HashMap<>();
			item.put("id", type.getId());
			item.put("iri", type.getIri());
			item.put("name", type.getName());
			item.put("definition", type.getDefinition());
			item.put("isAbstract", type.getIsAbstract());
			item.put("namespaceId", type.getNamespaceId());

			List<OntEntityTypeLabel> labels = labelMap.get(type.getId());
			if (labels != null) {
				List<Map<String, String>> labelList = new ArrayList<>();
				for (OntEntityTypeLabel label : labels) {
					Map<String, String> l = new HashMap<>();
					l.put("locale", label.getLocale());
					l.put("label", label.getLabel());
					labelList.add(l);
				}
				item.put("labels", labelList);
			}
			else {
				item.put("labels", new ArrayList<>());
			}
			result.add(item);
		}
		return result;
	}

	private List<Map<String, Object>> buildHierarchies(Long ontologyId) {
		List<OntEntityType> types = entityTypeMapper.selectList(
			Wrappers.<OntEntityType>lambdaQuery().eq(OntEntityType::getOntologyId, ontologyId));
		Set<Long> typeIds = types.stream().map(OntEntityType::getId).collect(Collectors.toSet());

		if (typeIds.isEmpty()) {
			return new ArrayList<>();
		}

		List<OntEntityTypeHierarchy> hierarchies = hierarchyMapper.selectList(
			Wrappers.<OntEntityTypeHierarchy>lambdaQuery().in(OntEntityTypeHierarchy::getChildId, typeIds));

		List<Map<String, Object>> result = new ArrayList<>();
		for (OntEntityTypeHierarchy h : hierarchies) {
			if (!typeIds.contains(h.getParentId())) {
				continue;
			}
			Map<String, Object> item = new HashMap<>();
			item.put("parentId", h.getParentId());
			item.put("childId", h.getChildId());
			result.add(item);
		}
		return result;
	}

	private List<Map<String, Object>> buildDisjoints(Long ontologyId) {
		List<OntEntityType> types = entityTypeMapper.selectList(
			Wrappers.<OntEntityType>lambdaQuery().eq(OntEntityType::getOntologyId, ontologyId));
		Set<Long> typeIds = types.stream().map(OntEntityType::getId).collect(Collectors.toSet());

		if (typeIds.isEmpty()) {
			return new ArrayList<>();
		}

		List<OntEntityTypeDisjoint> disjoints = disjointMapper.selectList(null);
		List<Map<String, Object>> result = new ArrayList<>();
		for (OntEntityTypeDisjoint d : disjoints) {
			if (!typeIds.contains(d.getTypeA()) || !typeIds.contains(d.getTypeB())) {
				continue;
			}
			Map<String, Object> item = new HashMap<>();
			item.put("typeA", d.getTypeA());
			item.put("typeB", d.getTypeB());
			result.add(item);
		}
		return result;
	}

	private List<Map<String, Object>> buildEquivalents(Long ontologyId) {
		List<OntEntityType> types = entityTypeMapper.selectList(
			Wrappers.<OntEntityType>lambdaQuery().eq(OntEntityType::getOntologyId, ontologyId));
		Set<Long> typeIds = types.stream().map(OntEntityType::getId).collect(Collectors.toSet());

		if (typeIds.isEmpty()) {
			return new ArrayList<>();
		}

		List<OntEntityTypeEquivalent> equivalents = equivalentMapper.selectList(
			Wrappers.<OntEntityTypeEquivalent>lambdaQuery().in(OntEntityTypeEquivalent::getEntityTypeId, typeIds));

		List<Map<String, Object>> result = new ArrayList<>();
		for (OntEntityTypeEquivalent eq : equivalents) {
			if (!typeIds.contains(eq.getEquivalentId())) {
				continue;
			}
			Map<String, Object> item = new HashMap<>();
			item.put("entityTypeId", eq.getEntityTypeId());
			item.put("equivalentId", eq.getEquivalentId());
			result.add(item);
		}
		return result;
	}

	private List<Map<String, Object>> buildDataProperties(Long ontologyId) {
		List<OntDataProperty> properties = dataPropertyMapper.selectList(
			Wrappers.<OntDataProperty>lambdaQuery().eq(OntDataProperty::getOntologyId, ontologyId));

		Set<Long> propIds = properties.stream().map(OntDataProperty::getId).collect(Collectors.toSet());

		Map<Long, List<OntDataPropertyLabel>> labelMap = new HashMap<>();
		if (!propIds.isEmpty()) {
			List<OntDataPropertyLabel> labels = dataPropertyLabelMapper.selectList(
				Wrappers.<OntDataPropertyLabel>lambdaQuery().in(OntDataPropertyLabel::getDataPropertyId, propIds));
			labelMap = labels.stream().collect(Collectors.groupingBy(OntDataPropertyLabel::getDataPropertyId));
		}

		List<Map<String, Object>> result = new ArrayList<>();
		for (OntDataProperty dp : properties) {
			Map<String, Object> item = new HashMap<>();
			item.put("id", dp.getId());
			item.put("iri", dp.getIri());
			item.put("name", dp.getName());
			item.put("definition", dp.getDefinition());
			item.put("domainEntityTypeId", dp.getDomainEntityTypeId());
			item.put("baseType", dp.getBaseType());
			item.put("valueMode", dp.getValueMode());
			item.put("regexPattern", dp.getRegexPattern());
			item.put("isUnique", dp.getIsUnique());
			item.put("unitCategoryId", dp.getUnitCategoryId());
			item.put("unitRefMode", dp.getUnitRefMode());
			item.put("namespaceId", dp.getNamespaceId());

			List<OntDataPropertyLabel> labels = labelMap.get(dp.getId());
			if (labels != null) {
				List<Map<String, String>> labelList = new ArrayList<>();
				for (OntDataPropertyLabel label : labels) {
					Map<String, String> l = new HashMap<>();
					l.put("locale", label.getLocale());
					l.put("label", label.getLabel());
					labelList.add(l);
				}
				item.put("labels", labelList);
			}
			else {
				item.put("labels", new ArrayList<>());
			}
			result.add(item);
		}
		return result;
	}

	private List<Map<String, Object>> buildDataPropertyEnums(Long ontologyId) {
		List<OntDataProperty> properties = dataPropertyMapper.selectList(
			Wrappers.<OntDataProperty>lambdaQuery().eq(OntDataProperty::getOntologyId, ontologyId));
		Set<Long> propIds = properties.stream().map(OntDataProperty::getId).collect(Collectors.toSet());

		if (propIds.isEmpty()) {
			return new ArrayList<>();
		}

		List<OntDataPropertyEnum> enums = dataPropertyEnumMapper.selectList(
			Wrappers.<OntDataPropertyEnum>lambdaQuery().in(OntDataPropertyEnum::getDataPropertyId, propIds));

		List<Map<String, Object>> result = new ArrayList<>();
		for (OntDataPropertyEnum e : enums) {
			Map<String, Object> item = new HashMap<>();
			item.put("dataPropertyId", e.getDataPropertyId());
			item.put("value", e.getEnumValue());
			item.put("canonicalValue", e.getCanonicalValue());
			result.add(item);
		}
		return result;
	}

	private List<Map<String, Object>> buildObjectProperties(Long ontologyId) {
		List<OntObjectProperty> properties = objectPropertyMapper.selectList(
			Wrappers.<OntObjectProperty>lambdaQuery().eq(OntObjectProperty::getOntologyId, ontologyId));

		Set<Long> propIds = properties.stream().map(OntObjectProperty::getId).collect(Collectors.toSet());

		Map<Long, List<OntObjectPropertyLabel>> labelMap = new HashMap<>();
		if (!propIds.isEmpty()) {
			List<OntObjectPropertyLabel> labels = objectPropertyLabelMapper.selectList(
				Wrappers.<OntObjectPropertyLabel>lambdaQuery().in(OntObjectPropertyLabel::getObjectPropertyId, propIds));
			labelMap = labels.stream().collect(Collectors.groupingBy(OntObjectPropertyLabel::getObjectPropertyId));
		}

		List<Map<String, Object>> result = new ArrayList<>();
		for (OntObjectProperty op : properties) {
			Map<String, Object> item = new HashMap<>();
			item.put("id", op.getId());
			item.put("iri", op.getIri());
			item.put("name", op.getName());
			item.put("definition", op.getDefinition());
			item.put("inverseOfId", op.getInverseOfId());
			item.put("isFunctional", op.getIsFunctional());
			item.put("isInverseFunctional", op.getIsInverseFunctional());
			item.put("isTransitive", op.getIsTransitive());
			item.put("isSymmetric", op.getIsSymmetric());
			item.put("namespaceId", op.getNamespaceId());

			List<OntObjectPropertyLabel> labels = labelMap.get(op.getId());
			if (labels != null) {
				List<Map<String, String>> labelList = new ArrayList<>();
				for (OntObjectPropertyLabel label : labels) {
					Map<String, String> l = new HashMap<>();
					l.put("locale", label.getLocale());
					l.put("label", label.getLabel());
					labelList.add(l);
				}
				item.put("labels", labelList);
			}
			else {
				item.put("labels", new ArrayList<>());
			}
			result.add(item);
		}
		return result;
	}

	private List<Map<String, Object>> buildObjectPropertyDomains(Long ontologyId) {
		List<OntObjectProperty> properties = objectPropertyMapper.selectList(
			Wrappers.<OntObjectProperty>lambdaQuery().eq(OntObjectProperty::getOntologyId, ontologyId));
		Set<Long> propIds = properties.stream().map(OntObjectProperty::getId).collect(Collectors.toSet());

		if (propIds.isEmpty()) {
			return new ArrayList<>();
		}

		List<OntObjectPropertyDomain> domains = objectPropertyDomainMapper.selectList(
			Wrappers.<OntObjectPropertyDomain>lambdaQuery().in(OntObjectPropertyDomain::getObjectPropertyId, propIds));

		List<Map<String, Object>> result = new ArrayList<>();
		for (OntObjectPropertyDomain d : domains) {
			Map<String, Object> item = new HashMap<>();
			item.put("objectPropertyId", d.getObjectPropertyId());
			item.put("entityTypeId", d.getEntityTypeId());
			result.add(item);
		}
		return result;
	}

	private List<Map<String, Object>> buildObjectPropertyRanges(Long ontologyId) {
		List<OntObjectProperty> properties = objectPropertyMapper.selectList(
			Wrappers.<OntObjectProperty>lambdaQuery().eq(OntObjectProperty::getOntologyId, ontologyId));
		Set<Long> propIds = properties.stream().map(OntObjectProperty::getId).collect(Collectors.toSet());

		if (propIds.isEmpty()) {
			return new ArrayList<>();
		}

		List<OntObjectPropertyRange> ranges = objectPropertyRangeMapper.selectList(
			Wrappers.<OntObjectPropertyRange>lambdaQuery().in(OntObjectPropertyRange::getObjectPropertyId, propIds));

		List<Map<String, Object>> result = new ArrayList<>();
		for (OntObjectPropertyRange r : ranges) {
			Map<String, Object> item = new HashMap<>();
			item.put("objectPropertyId", r.getObjectPropertyId());
			item.put("entityTypeId", r.getEntityTypeId());
			result.add(item);
		}
		return result;
	}

	private List<Map<String, Object>> buildAxiomRules(Long ontologyId) {
		List<OntAxiomRule> rules = axiomRuleMapper
			.selectList(Wrappers.<OntAxiomRule>lambdaQuery().eq(OntAxiomRule::getOntologyId, ontologyId));

		List<Map<String, Object>> result = new ArrayList<>();
		for (OntAxiomRule rule : rules) {
			Map<String, Object> item = new HashMap<>();
			item.put("id", rule.getId());
			item.put("ruleCode", rule.getRuleCode());
			item.put("name", rule.getName());
			item.put("category", rule.getCategory());
			item.put("description", rule.getDescription());
			item.put("formalizationMode", rule.getFormalizationMode());
			item.put("owlAxiom", rule.getOwlAxiom());
			item.put("shaclShape", rule.getShaclShape());
			item.put("isEnabled", rule.getIsEnabled());
			item.put("severity", rule.getSeverity());
			result.add(item);
		}
		return result;
	}

	private List<Map<String, Object>> buildAxiomRuleTargets(Long ontologyId) {
		List<OntAxiomRule> rules = axiomRuleMapper
			.selectList(Wrappers.<OntAxiomRule>lambdaQuery().eq(OntAxiomRule::getOntologyId, ontologyId));
		Set<Long> ruleIds = rules.stream().map(OntAxiomRule::getId).collect(Collectors.toSet());

		if (ruleIds.isEmpty()) {
			return new ArrayList<>();
		}

		List<OntAxiomRuleTarget> targets = axiomRuleTargetMapper
			.selectList(Wrappers.<OntAxiomRuleTarget>lambdaQuery().in(OntAxiomRuleTarget::getAxiomRuleId, ruleIds));

		List<Map<String, Object>> result = new ArrayList<>();
		for (OntAxiomRuleTarget t : targets) {
			Map<String, Object> item = new HashMap<>();
			item.put("axiomRuleId", t.getAxiomRuleId());
			item.put("targetType", t.getTargetType());
			item.put("bindingRole", t.getBindingRole());
			item.put("entityTypeId", t.getEntityTypeId());
			item.put("dataPropertyId", t.getDataPropertyId());
			item.put("objectPropertyId", t.getObjectPropertyId());
			item.put("unitCategoryId", t.getUnitCategoryId());
			result.add(item);
		}
		return result;
	}

	private List<Map<String, Object>> buildExtensionDependencies(Long ontologyId) {
		List<OntExtensionModule> modules = extensionModuleMapper.selectList(
			Wrappers.<OntExtensionModule>lambdaQuery().eq(OntExtensionModule::getOntologyId, ontologyId));

		Set<Long> moduleIds = modules.stream().map(OntExtensionModule::getId).collect(Collectors.toSet());

		Map<Long, List<OntExtensionResource>> resourceMap = new HashMap<>();
		if (!moduleIds.isEmpty()) {
			List<OntExtensionResource> resources = extensionResourceMapper.selectList(
				Wrappers.<OntExtensionResource>lambdaQuery().in(OntExtensionResource::getModuleId, moduleIds));
			resourceMap = resources.stream().collect(Collectors.groupingBy(OntExtensionResource::getModuleId));
		}

		List<Map<String, Object>> result = new ArrayList<>();
		for (OntExtensionModule module : modules) {
			Map<String, Object> item = new HashMap<>();
			item.put("moduleId", module.getId());
			item.put("moduleCode", module.getModuleCode());
			item.put("moduleName", module.getModuleName());
			item.put("version", module.getVersion());
			item.put("namespaceId", module.getNamespaceId());

			List<OntExtensionResource> resources = resourceMap.get(module.getId());
			if (resources != null) {
				List<Map<String, Object>> resList = new ArrayList<>();
				for (OntExtensionResource res : resources) {
					Map<String, Object> r = new HashMap<>();
					r.put("resourceType", res.getResourceType());
					r.put("resourceId", res.getResourceId());
					r.put("resourceIri", res.getResourceIri());
					r.put("resourceName", res.getResourceName());
					resList.add(r);
				}
				item.put("resources", resList);
			}
			else {
				item.put("resources", new ArrayList<>());
			}
			result.add(item);
		}
		return result;
	}

	private List<Map<String, Object>> buildUnitDependencies(Long ontologyId) {
		// 收集数据属性引用的单位分类
		List<OntDataProperty> properties = dataPropertyMapper.selectList(
			Wrappers.<OntDataProperty>lambdaQuery().eq(OntDataProperty::getOntologyId, ontologyId));

		Set<Long> categoryIds = properties.stream()
			.map(OntDataProperty::getUnitCategoryId)
			.filter(id -> id != null)
			.collect(Collectors.toSet());

		if (categoryIds.isEmpty()) {
			return new ArrayList<>();
		}

		List<OntUnit> units = unitMapper.selectList(
			Wrappers.<OntUnit>lambdaQuery().in(OntUnit::getCategoryId, categoryIds));

		List<Map<String, Object>> result = new ArrayList<>();
		for (OntUnit unit : units) {
			Map<String, Object> item = new HashMap<>();
			item.put("id", unit.getId());
			item.put("unitCode", unit.getUnitCode());
			item.put("unitSymbol", unit.getUnitSymbol());
			item.put("unitName", unit.getUnitName());
			item.put("categoryId", unit.getCategoryId());
			item.put("isBaseUnit", unit.getIsBaseUnit());
			result.add(item);
		}
		return result;
	}

}
