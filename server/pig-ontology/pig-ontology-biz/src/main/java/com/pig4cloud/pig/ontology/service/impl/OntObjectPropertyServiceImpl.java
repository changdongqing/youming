/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.dto.OntObjectPropertyCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntObjectPropertyQuery;
import com.pig4cloud.pig.ontology.dto.OntObjectPropertyUpdateDTO;
import com.pig4cloud.pig.ontology.entity.OntAxiomRuleTarget;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel;
import com.pig4cloud.pig.ontology.entity.OntInstanceObjectRelation;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyDomain;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyLabel;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyRange;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleTargetMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceObjectRelationMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyDomainMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyRangeMapper;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import com.pig4cloud.pig.ontology.service.OntEntityTypeService;
import com.pig4cloud.pig.ontology.service.OntIriUniquenessService;
import com.pig4cloud.pig.ontology.service.OntObjectPropertyService;
import com.pig4cloud.pig.ontology.vo.OntApplicableObjectPropertyByRangeVO;
import com.pig4cloud.pig.ontology.vo.OntApplicableObjectPropertyVO;
import com.pig4cloud.pig.ontology.vo.OntEntityTypeRefVO;
import com.pig4cloud.pig.ontology.vo.OntObjectPropertyDetailVO;
import com.pig4cloud.pig.ontology.vo.OntObjectPropertySummaryVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 对象属性服务实现。
 *
 * @author youming
 */
@Service
@AllArgsConstructor
public class OntObjectPropertyServiceImpl extends ServiceImpl<OntObjectPropertyMapper, OntObjectProperty>
		implements OntObjectPropertyService {

	private static final String BUILTIN = "1";

	private static final String EXTENSION = "0";

	private static final String ZH = "zh";

	private static final String TABLE_NAME = "ont_object_property";

	private static final int MAX_DEPTH = 100;

	private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z][a-zA-Z0-9]*$");

		private static final Pattern LOCAL_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");

	/** 本期引擎已实现的推理能力子集 */
	private static final Set<String> IMPLEMENTED_CAPABILITIES = Set.of("DISJOINT_CHECK", "FUNCTIONAL_CHECK",
			"SUBCLASS_INFERENCE");

	private final OntObjectPropertyDomainMapper domainMapper;

	private final OntObjectPropertyRangeMapper rangeMapper;

	private final OntObjectPropertyLabelMapper labelMapper;

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntEntityTypeLabelMapper entityTypeLabelMapper;

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	private final OntNamespaceMapper namespaceMapper;

	private final OntOntologyProjectMapper ontologyProjectMapper;

	private final OntIriUniquenessService iriUniquenessService;

	private final OntAxiomRuleTargetMapper axiomRuleTargetMapper;

	private final OntInstanceObjectRelationMapper instanceObjectRelationMapper;

	// ==================== 查询 ====================

	@Override
	public IPage<OntObjectPropertySummaryVO> pageSummary(Page<OntObjectProperty> page,
			OntObjectPropertyQuery query) {
		List<Long> propertyIds = filterPropertyIdsByDomainRange(query);
		Page<OntObjectProperty> rawPage = this.page(page, buildQueryWrapper(query, propertyIds));
		Page<OntObjectPropertySummaryVO> resultPage = new Page<>(rawPage.getCurrent(), rawPage.getSize(),
				rawPage.getTotal());
		resultPage.setRecords(buildSummaryList(rawPage.getRecords()));
		return resultPage;
	}

	@Override
	public List<OntObjectPropertySummaryVO> listSummary(OntObjectPropertyQuery query) {
		List<Long> propertyIds = filterPropertyIdsByDomainRange(query);
		return buildSummaryList(this.list(buildQueryWrapper(query, propertyIds)));
	}

	@Override
	public OntObjectPropertyDetailVO getDetail(Long id) {
		OntObjectProperty prop = this.getById(id);
		if (prop == null) {
			return null;
		}
		OntObjectPropertyDetailVO detail = new OntObjectPropertyDetailVO();
		detail.setObjectProperty(prop);
		detail.setLabels(labelMapper.selectList(Wrappers.<OntObjectPropertyLabel>lambdaQuery()
			.eq(OntObjectPropertyLabel::getObjectPropertyId, id)
			.orderByAsc(OntObjectPropertyLabel::getLocale)));
		detail.setDomains(buildDomainRefs(id));
		detail.setRanges(buildRangeRefs(id));
		detail.setNamespace(namespaceMapper.selectById(prop.getNamespaceId()));
		if (prop.getInverseOfId() != null) {
			OntObjectProperty inverse = this.getById(prop.getInverseOfId());
			if (inverse != null) {
				OntObjectPropertySummaryVO inverseSummary = new OntObjectPropertySummaryVO();
				inverseSummary.setObjectProperty(inverse);
				inverseSummary.setDomains(buildDomainRefs(inverse.getId()));
				inverseSummary.setRanges(buildRangeRefs(inverse.getId()));
				inverseSummary.setLabel(getZhLabel(inverse.getId()));
				detail.setInverseProperty(inverseSummary);
			}
		}
		detail.setSemanticWarnings(computeSemanticWarnings(prop, detail.getDomains(), detail.getRanges()));
		detail.setInferenceSupport(prop.getInferenceSupport());
		return detail;
	}

	@Override
	public List<OntObjectPropertySummaryVO> listByCapability(String capability) {
		if (!IMPLEMENTED_CAPABILITIES.contains(capability)) {
			return List.of();
		}
		List<OntObjectProperty> properties = this.list(Wrappers.<OntObjectProperty>lambdaQuery()
			.apply("inference_support @> ('[\"' || {0} || '\"]')::jsonb", capability)
			.orderByAsc(OntObjectProperty::getSortOrder)
			.orderByAsc(OntObjectProperty::getId));
		return buildSummaryList(properties);
	}

	@Override
	public List<OntApplicableObjectPropertyVO> listApplicableByDomain(Long entityTypeId) {
		OntEntityType entityType = entityTypeMapper.selectById(entityTypeId);
		if (entityType == null) {
			return List.of();
		}

		Map<Long, Integer> distanceMap = collectAncestorsWithDistance(entityTypeId);
		Set<Long> candidateTypeIds = distanceMap.keySet();

		// 查询定义域命中候选类型的对象属性
		List<OntObjectPropertyDomain> domains = domainMapper.selectList(
			Wrappers.<OntObjectPropertyDomain>lambdaQuery()
				.in(OntObjectPropertyDomain::getEntityTypeId, candidateTypeIds));
		if (domains.isEmpty()) {
			return List.of();
		}

		// 按属性ID分组，每条定义域记录计算该属性的命中距离
		Map<Long, Integer> propertyDistance = new HashMap<>();
		Map<Long, Long> propertyMatchedType = new HashMap<>();
		for (OntObjectPropertyDomain d : domains) {
			OntObjectProperty prop = this.getById(d.getObjectPropertyId());
			if (prop == null || !"0".equals(prop.getDelFlag())) {
				continue;
			}
			int distance = distanceMap.getOrDefault(d.getEntityTypeId(), Integer.MAX_VALUE);
			Integer existing = propertyDistance.get(prop.getId());
			if (existing == null || distance < existing) {
				propertyDistance.put(prop.getId(), distance);
				propertyMatchedType.put(prop.getId(), d.getEntityTypeId());
			}
		}
		if (propertyDistance.isEmpty()) {
			return List.of();
		}

		List<OntObjectProperty> properties = this.listByIds(propertyDistance.keySet());
		Map<Long, String> labelMap = batchLoadZhLabels(propertyDistance.keySet());
		Map<Long, OntEntityType> typeMap = batchLoadEntityTypes(propertyMatchedType.values().stream()
			.collect(Collectors.toSet()));
		Map<Long, String> typeLabelMap = batchLoadEntityTypeZhLabels(propertyMatchedType.values().stream()
			.collect(Collectors.toSet()));

		return properties.stream().map(prop -> {
			OntApplicableObjectPropertyVO vo = new OntApplicableObjectPropertyVO();
			vo.setObjectProperty(prop);
			vo.setLabel(labelMap.get(prop.getId()));
			int distance = propertyDistance.get(prop.getId());
			vo.setInherited(distance > 0);
			vo.setInheritanceDistance(distance);
			Long matchedTypeId = propertyMatchedType.get(prop.getId());
			vo.setMatchedDomainEntityTypeId(matchedTypeId);
			OntEntityType matchedType = typeMap.get(matchedTypeId);
			if (matchedType != null) {
				vo.setMatchedDomainName(matchedType.getName());
			}
			vo.setMatchedDomainLabel(typeLabelMap.get(matchedTypeId));
			vo.setDomains(buildDomainRefs(prop.getId()));
			vo.setRanges(buildRangeRefs(prop.getId()));
			return vo;
		}).sorted(Comparator.comparing(OntApplicableObjectPropertyVO::getInheritanceDistance)
			.thenComparing(vo -> vo.getObjectProperty().getSortOrder(), Comparator.nullsLast(Integer::compareTo))
			.thenComparing(vo -> vo.getObjectProperty().getId()))
			.toList();
	}

	@Override
	public List<OntApplicableObjectPropertyByRangeVO> listApplicableByRange(Long entityTypeId) {
		OntEntityType entityType = entityTypeMapper.selectById(entityTypeId);
		if (entityType == null) {
			return List.of();
		}

		Map<Long, Integer> distanceMap = collectAncestorsWithDistance(entityTypeId);
		Set<Long> candidateTypeIds = distanceMap.keySet();

		List<OntObjectPropertyRange> ranges = rangeMapper.selectList(
			Wrappers.<OntObjectPropertyRange>lambdaQuery()
				.in(OntObjectPropertyRange::getEntityTypeId, candidateTypeIds));
		if (ranges.isEmpty()) {
			return List.of();
		}

		Map<Long, Integer> propertyDistance = new HashMap<>();
		Map<Long, Long> propertyMatchedType = new HashMap<>();
		for (OntObjectPropertyRange r : ranges) {
			OntObjectProperty prop = this.getById(r.getObjectPropertyId());
			if (prop == null || !"0".equals(prop.getDelFlag())) {
				continue;
			}
			int distance = distanceMap.getOrDefault(r.getEntityTypeId(), Integer.MAX_VALUE);
			Integer existing = propertyDistance.get(prop.getId());
			if (existing == null || distance < existing) {
				propertyDistance.put(prop.getId(), distance);
				propertyMatchedType.put(prop.getId(), r.getEntityTypeId());
			}
		}
		if (propertyDistance.isEmpty()) {
			return List.of();
		}

		List<OntObjectProperty> properties = this.listByIds(propertyDistance.keySet());
		Map<Long, String> labelMap = batchLoadZhLabels(propertyDistance.keySet());
		Map<Long, OntEntityType> typeMap = batchLoadEntityTypes(propertyMatchedType.values().stream()
			.collect(Collectors.toSet()));
		Map<Long, String> typeLabelMap = batchLoadEntityTypeZhLabels(propertyMatchedType.values().stream()
			.collect(Collectors.toSet()));

		return properties.stream().map(prop -> {
			OntApplicableObjectPropertyByRangeVO vo = new OntApplicableObjectPropertyByRangeVO();
			vo.setObjectProperty(prop);
			vo.setLabel(labelMap.get(prop.getId()));
			int distance = propertyDistance.get(prop.getId());
			vo.setInherited(distance > 0);
			vo.setInheritanceDistance(distance);
			Long matchedTypeId = propertyMatchedType.get(prop.getId());
			vo.setMatchedRangeEntityTypeId(matchedTypeId);
			OntEntityType matchedType = typeMap.get(matchedTypeId);
			if (matchedType != null) {
				vo.setMatchedRangeName(matchedType.getName());
			}
			vo.setMatchedRangeLabel(typeLabelMap.get(matchedTypeId));
			vo.setDomains(buildDomainRefs(prop.getId()));
			vo.setRanges(buildRangeRefs(prop.getId()));
			return vo;
		}).sorted(Comparator.comparing(OntApplicableObjectPropertyByRangeVO::getInheritanceDistance)
			.thenComparing(vo -> vo.getObjectProperty().getSortOrder(), Comparator.nullsLast(Integer::compareTo))
			.thenComparing(vo -> vo.getObjectProperty().getId()))
			.toList();
	}

	// ==================== 新增 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntObjectProperty> saveObjectProperty(OntObjectPropertyCreateDTO request) {
		Long ontologyId = request.getOntologyId() == null
				? OntEntityTypeService.CORE_ONTOLOGY_ID : request.getOntologyId();

		List<Long> domainIds = dedupIds(request.getDomainEntityTypeIds());
		List<Long> rangeIds = dedupIds(request.getRangeEntityTypeIds());

		String semanticError = validateSemanticFields(ontologyId, request.getNamespaceId(), request.getName(),
			request.getIriLocalName(), request.getIri(), domainIds, rangeIds, request.getIsFunctional(),
			request.getIsInverseFunctional(), request.getIsTransitive(), request.getIsSymmetric(),
			request.getInverseOfId(), null);
		if (semanticError != null) {
			return R.failed(semanticError);
		}

		String capabilityError = validateInferenceSupport(request.getInferenceSupport(),
			normalizeFlag(request.getIsFunctional()));
		if (capabilityError != null) {
			return R.failed(capabilityError);
		}

		OntNamespace namespace = namespaceMapper.selectById(request.getNamespaceId());
		String effectiveLocalName = StringUtils.hasText(request.getIriLocalName()) ? request.getIriLocalName()
				: request.getName();
		String expectedIri = namespace.getUri() + effectiveLocalName;

		OntObjectProperty prop = new OntObjectProperty();
		prop.setIri(expectedIri);
		prop.setIriLocalName(effectiveLocalName);
		prop.setName(request.getName());
		prop.setDefinition(request.getDefinition());
		prop.setIsFunctional(normalizeFlag(request.getIsFunctional()));
		prop.setIsInverseFunctional(normalizeFlag(request.getIsInverseFunctional()));
		prop.setIsTransitive(normalizeFlag(request.getIsTransitive()));
		prop.setIsSymmetric(normalizeFlag(request.getIsSymmetric()));
		prop.setSourceType("EXTENSION");
		prop.setIsBuiltin(EXTENSION);
		prop.setOntologyId(ontologyId);
		prop.setNamespaceId(request.getNamespaceId());
		prop.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
		prop.setRemarks(request.getRemarks());
		prop.setInferenceSupport(request.getInferenceSupport() != null ? request.getInferenceSupport() : List.of());
		this.save(prop);

		saveDomainAndRange(prop.getId(), domainIds, rangeIds);
		saveLabel(prop.getId(), ZH, request.getLabel());

		if (request.getInverseOfId() != null) {
			maintainInverseProperty(prop.getId(), request.getInverseOfId(), null);
		}
		return R.ok(prop);
	}

	// ==================== 修改 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntObjectProperty> updateObjectProperty(OntObjectPropertyUpdateDTO request) {
		OntObjectProperty old = this.getById(request.getId());
		if (old == null) {
			return R.failed("对象属性不存在");
		}

		if (BUILTIN.equals(old.getIsBuiltin())) {
			if (hasSemanticChanges(request, old)) {
				return R.failed("内置对象属性语义字段不可修改");
			}
			this.update(Wrappers.<OntObjectProperty>lambdaUpdate()
				.eq(OntObjectProperty::getId, old.getId())
				.set(OntObjectProperty::getSortOrder,
					request.getSortOrder() == null ? old.getSortOrder() : request.getSortOrder())
				.set(OntObjectProperty::getRemarks, request.getRemarks()));
			saveLabel(old.getId(), ZH, request.getLabel());
			return R.ok(this.getById(old.getId()));
		}

		Long ontologyId = old.getOntologyId();
		Long namespaceId = request.getNamespaceId() != null ? request.getNamespaceId() : old.getNamespaceId();
		String name = StringUtils.hasText(request.getName()) ? request.getName() : old.getName();
		String iriLocalName = StringUtils.hasText(request.getIriLocalName()) ? request.getIriLocalName() : name;
		String isFunctional = normalizeFlag(StringUtils.hasText(request.getIsFunctional())
				? request.getIsFunctional() : old.getIsFunctional());
		String isInverseFunctional = normalizeFlag(StringUtils.hasText(request.getIsInverseFunctional())
				? request.getIsInverseFunctional() : old.getIsInverseFunctional());
		String isTransitive = normalizeFlag(StringUtils.hasText(request.getIsTransitive())
				? request.getIsTransitive() : old.getIsTransitive());
		String isSymmetric = normalizeFlag(StringUtils.hasText(request.getIsSymmetric())
				? request.getIsSymmetric() : old.getIsSymmetric());

		List<Long> domainIds = request.getDomainEntityTypeIds() != null
				? dedupIds(request.getDomainEntityTypeIds()) : loadExistingDomainIds(old.getId());
		List<Long> rangeIds = request.getRangeEntityTypeIds() != null
				? dedupIds(request.getRangeEntityTypeIds()) : loadExistingRangeIds(old.getId());

		String semanticError = validateSemanticFields(ontologyId, namespaceId, name, iriLocalName,
			request.getIri(), domainIds, rangeIds, isFunctional, isInverseFunctional, isTransitive, isSymmetric,
			request.getInverseOfId(), old.getId());
		if (semanticError != null) {
			return R.failed(semanticError);
		}

		List<String> inferenceSupport = request.getInferenceSupport() != null ? request.getInferenceSupport()
				: old.getInferenceSupport();
		String capabilityError = validateInferenceSupport(inferenceSupport, isFunctional);
		if (capabilityError != null) {
			return R.failed(capabilityError);
		}

		OntNamespace namespace = namespaceMapper.selectById(namespaceId);
		String expectedIri = namespace.getUri() + iriLocalName;

		this.update(Wrappers.<OntObjectProperty>lambdaUpdate()
			.eq(OntObjectProperty::getId, old.getId())
			.set(OntObjectProperty::getIri, expectedIri)
			.set(OntObjectProperty::getIriLocalName, iriLocalName)
			.set(OntObjectProperty::getName, name)
			.set(OntObjectProperty::getDefinition, request.getDefinition())
			.set(OntObjectProperty::getIsFunctional, isFunctional)
			.set(OntObjectProperty::getIsInverseFunctional, isInverseFunctional)
			.set(OntObjectProperty::getIsTransitive, isTransitive)
			.set(OntObjectProperty::getIsSymmetric, isSymmetric)
			.set(OntObjectProperty::getNamespaceId, namespaceId)
			.set(OntObjectProperty::getSortOrder,
				request.getSortOrder() == null ? old.getSortOrder() : request.getSortOrder())
			.set(OntObjectProperty::getRemarks, request.getRemarks())
			.set(OntObjectProperty::getInferenceSupport, inferenceSupport));

		replaceDomainAndRange(old.getId(), domainIds, rangeIds);
		saveLabel(old.getId(), ZH, request.getLabel());

		if (request.getInverseOfId() != null || hasInverseField(request)) {
			maintainInverseProperty(old.getId(), request.getInverseOfId(), old.getInverseOfId());
		}
		return R.ok(this.getById(old.getId()));
	}

	// ==================== 删除 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> removeObjectProperty(Long id) {
		OntObjectProperty prop = this.getById(id);
		if (prop == null) {
			return R.failed("对象属性不存在");
		}
		if (BUILTIN.equals(prop.getIsBuiltin())) {
			return R.failed("内置对象属性不可删除");
		}
		// 检查公理规则目标引用
		long axiomTargetCount = axiomRuleTargetMapper.selectCount(Wrappers.<OntAxiomRuleTarget>lambdaQuery()
			.eq(OntAxiomRuleTarget::getObjectPropertyId, id));
		if (axiomTargetCount > 0) {
			return R.failed("该对象属性被公理规则引用，不能删除");
		}
		// 检查实例对象断言引用
		long relationCount = instanceObjectRelationMapper
			.selectCount(Wrappers.<OntInstanceObjectRelation>lambdaQuery()
				.eq(OntInstanceObjectRelation::getObjectPropertyId, id));
		if (relationCount > 0) {
			return R.failed("该对象属性被" + relationCount + "条实例断言引用，不能删除");
		}
		// 解除逆属性关系
		if (prop.getInverseOfId() != null) {
			OntObjectProperty inverse = this.getById(prop.getInverseOfId());
			if (inverse != null && id.equals(inverse.getInverseOfId())) {
				this.update(Wrappers.<OntObjectProperty>lambdaUpdate()
					.eq(OntObjectProperty::getId, inverse.getId())
					.set(OntObjectProperty::getInverseOfId, null));
			}
		}
		domainMapper.delete(Wrappers.<OntObjectPropertyDomain>lambdaQuery()
			.eq(OntObjectPropertyDomain::getObjectPropertyId, id));
		rangeMapper.delete(Wrappers.<OntObjectPropertyRange>lambdaQuery()
			.eq(OntObjectPropertyRange::getObjectPropertyId, id));
		labelMapper.delete(Wrappers.<OntObjectPropertyLabel>lambdaQuery()
			.eq(OntObjectPropertyLabel::getObjectPropertyId, id));
		return R.ok(this.removeById(id));
	}

	// ==================== 校验 ====================

	private String validateSemanticFields(Long ontologyId, Long namespaceId, String name, String iriLocalName,
			String requestIri, List<Long> domainIds, List<Long> rangeIds, String isFunctional,
			String isInverseFunctional, String isTransitive, String isSymmetric, Long inverseOfId, Long excludeId) {
		OntOntologyProject ontology = ontologyProjectMapper.selectById(ontologyId);
		if (ontology == null) {
			return "本体工程不存在";
		}
		OntNamespace namespace = namespaceMapper.selectById(namespaceId);
		if (namespace == null) {
			return "命名空间不存在";
		}
		if (BUILTIN.equals(namespace.getIsBuiltin())) {
			return "扩展对象属性必须使用扩展命名空间";
		}
		if (!NAME_PATTERN.matcher(name).matches()) {
			return "英文名称必须以小写字母开头，仅支持英文字母和数字";
		}
		String effectiveLocalName = StringUtils.hasText(iriLocalName) ? iriLocalName : name;
		if (!LOCAL_NAME_PATTERN.matcher(effectiveLocalName).matches()) {
			return "IRI本地名必须以字母开头，仅支持英文字母、数字和下划线";
		}

		// 定义域/值域实体类型存在性和同工程校验
		String domainError = validateEntityTypeRefs(domainIds, ontologyId, "定义域");
		if (domainError != null) {
			return domainError;
		}
		String rangeError = validateEntityTypeRefs(rangeIds, ontologyId, "值域");
		if (rangeError != null) {
			return rangeError;
		}

		// IRI本地名唯一
		long localNameCount = this.count(Wrappers.<OntObjectProperty>lambdaQuery()
			.eq(OntObjectProperty::getOntologyId, ontologyId)
			.eq(OntObjectProperty::getNamespaceId, namespaceId)
			.eq(OntObjectProperty::getIriLocalName, effectiveLocalName)
			.ne(excludeId != null, OntObjectProperty::getId, excludeId));
		if (localNameCount > 0) {
			return "同一命名空间下IRI本地名已存在";
		}

		// 同本体工程name唯一
		long nameCount = this.count(Wrappers.<OntObjectProperty>lambdaQuery()
			.eq(OntObjectProperty::getOntologyId, ontologyId)
			.eq(OntObjectProperty::getName, name)
			.ne(excludeId != null, OntObjectProperty::getId, excludeId));
		if (nameCount > 0) {
			return "同一本体工程下英文名称已存在";
		}

		// 全局IRI冲突
		String expectedIri = namespace.getUri() + effectiveLocalName;
		if (StringUtils.hasText(requestIri) && !expectedIri.equals(requestIri)) {
			return "IRI必须等于命名空间URI与IRI本地名的拼接结果";
		}
		String iriConflict = iriUniquenessService.checkIriConflict(expectedIri, TABLE_NAME, excludeId);
		if (iriConflict != null) {
			return iriConflict;
		}

		// 语义特性组合校验
		String semanticError = validateSemanticCombination(isFunctional, isInverseFunctional, isTransitive,
			isSymmetric);
		if (semanticError != null) {
			return semanticError;
		}

		// 逆属性校验
		if (inverseOfId != null) {
			String inverseError = validateInverseProperty(inverseOfId, excludeId);
			if (inverseError != null) {
				return inverseError;
			}
		}
		return null;
	}

	private String validateEntityTypeRefs(List<Long> typeIds, Long ontologyId, String label) {
		if (typeIds == null || typeIds.isEmpty()) {
			return label + "实体类型不能为空";
		}
		for (Long typeId : typeIds) {
			OntEntityType type = entityTypeMapper.selectById(typeId);
			if (type == null) {
				return label + "实体类型不存在: " + typeId;
			}
			if (!Objects.equals(type.getOntologyId(), ontologyId)) {
				return label + "实体类型必须与当前属性属于同一本体工程";
			}
		}
		return null;
	}

	private String validateSemanticCombination(String isFunctional, String isInverseFunctional, String isTransitive,
			String isSymmetric) {
		String functional = normalizeFlag(isFunctional);
		String inverseFunctional = normalizeFlag(isInverseFunctional);
		String transitive = normalizeFlag(isTransitive);
		if (BUILTIN.equals(transitive) && (BUILTIN.equals(functional) || BUILTIN.equals(inverseFunctional))) {
			return "传递性对象属性不能同时声明功能性或反功能性（违反OWL 2 DL简单对象属性限制）";
		}
		return null;
	}

	/**
	 * 校验推理能力契约的合法性。
	 * <p>
	 * 规则：
	 * 1. 每个元素必须属于 ReasonerCapability 的已实现子集（DISJOINT_CHECK/FUNCTIONAL_CHECK/SUBCLASS_INFERENCE）；
	 * 2. L3 必须是 L2 的子集：声明 FUNCTIONAL_CHECK 要求 isFunctional='1'。
	 * @param inferenceSupport 能力契约列表
	 * @param isFunctional 功能性标记
	 * @return 错误信息，null 表示通过
	 */
	private String validateInferenceSupport(List<String> inferenceSupport, String isFunctional) {
		if (inferenceSupport == null || inferenceSupport.isEmpty()) {
			return null;
		}
		for (String cap : inferenceSupport) {
			if (!IMPLEMENTED_CAPABILITIES.contains(cap)) {
				return "本期未启用的推理能力：" + cap;
			}
		}
		if (inferenceSupport.contains("FUNCTIONAL_CHECK") && !BUILTIN.equals(isFunctional)) {
			return "声明 FUNCTIONAL_CHECK 需先设置 isFunctional='1'";
		}
		return null;
	}

	private String validateInverseProperty(Long inverseOfId, Long currentId) {
		if (inverseOfId.equals(currentId)) {
			return "逆属性不能指向自身";
		}
		OntObjectProperty target = this.getById(inverseOfId);
		if (target == null) {
			return "逆属性目标不存在";
		}
		if (BUILTIN.equals(target.getIsBuiltin())) {
			return "不能与内置对象属性建立逆属性关系";
		}
		if (target.getInverseOfId() != null && !target.getInverseOfId().equals(currentId)) {
			return "目标对象属性已与其他属性建立逆属性关系";
		}
		return null;
	}

	// ==================== 逆属性双向维护 ====================

	/**
	 * 维护逆属性双向关系。
	 * @param currentId 当前属性ID
	 * @param newInverseId 新的逆属性ID（null表示解除）
	 * @param oldInverseId 旧的逆属性ID（用于判断是否需要清理旧目标）
	 */
	private void maintainInverseProperty(Long currentId, Long newInverseId, Long oldInverseId) {
		// 清理旧目标：如果旧逆属性目标当前仍指向当前属性，清空它
		if (oldInverseId != null && !oldInverseId.equals(newInverseId)) {
			OntObjectProperty oldTarget = this.getById(oldInverseId);
			if (oldTarget != null && currentId.equals(oldTarget.getInverseOfId())) {
				this.update(Wrappers.<OntObjectProperty>lambdaUpdate()
					.eq(OntObjectProperty::getId, oldInverseId)
					.set(OntObjectProperty::getInverseOfId, null));
			}
		}
		// 设置当前属性的逆属性
		this.update(Wrappers.<OntObjectProperty>lambdaUpdate()
			.eq(OntObjectProperty::getId, currentId)
			.set(OntObjectProperty::getInverseOfId, newInverseId));
		// 设置目标的逆属性为当前属性
		if (newInverseId != null) {
			this.update(Wrappers.<OntObjectProperty>lambdaUpdate()
				.eq(OntObjectProperty::getId, newInverseId)
				.set(OntObjectProperty::getInverseOfId, currentId));
		}
	}

	// ==================== 辅助方法 ====================

	private List<Long> dedupIds(List<Long> ids) {
		if (ids == null) {
			return List.of();
		}
		return ids.stream().filter(Objects::nonNull).distinct().toList();
	}

	private String normalizeFlag(String value) {
		return BUILTIN.equals(value) ? BUILTIN : EXTENSION;
	}

	private boolean hasSemanticChanges(OntObjectPropertyUpdateDTO request, OntObjectProperty old) {
		return request.getNamespaceId() != null && !Objects.equals(request.getNamespaceId(), old.getNamespaceId())
				|| StringUtils.hasText(request.getName()) && !request.getName().equals(old.getName())
				|| StringUtils.hasText(request.getIriLocalName())
				|| StringUtils.hasText(request.getIri())
				|| request.getDomainEntityTypeIds() != null
				|| request.getRangeEntityTypeIds() != null
				|| request.getIsFunctional() != null && !Objects.equals(request.getIsFunctional(), old.getIsFunctional())
				|| request.getIsInverseFunctional() != null
						&& !Objects.equals(request.getIsInverseFunctional(), old.getIsInverseFunctional())
				|| request.getIsTransitive() != null && !Objects.equals(request.getIsTransitive(), old.getIsTransitive())
				|| request.getIsSymmetric() != null && !Objects.equals(request.getIsSymmetric(), old.getIsSymmetric())
				|| request.getInverseOfId() != null
				|| request.getDefinition() != null
				|| request.getInferenceSupport() != null
						&& !Objects.equals(request.getInferenceSupport(), old.getInferenceSupport());
	}

	private boolean hasInverseField(OntObjectPropertyUpdateDTO request) {
		// update DTO中inverseOfId被显式提供时（包括null值需要通过其他方式判断），
		// 这里通过检查是否曾设置过来决定是否维护逆属性
		// 简化处理：如果inverseOfId不为null则已在validateSemanticFields中校验
		return request.getInverseOfId() != null;
	}

	/**
	 * BFS向上收集自身及全部祖先ID和最短距离。
	 */
	private Map<Long, Integer> collectAncestorsWithDistance(Long entityTypeId) {
		Map<Long, Integer> distanceMap = new HashMap<>();
		distanceMap.put(entityTypeId, 0);
		Deque<Long> queue = new ArrayDeque<>();
		queue.addLast(entityTypeId);
		while (!queue.isEmpty()) {
			Long current = queue.removeFirst();
			int currentDepth = distanceMap.get(current);
			if (currentDepth >= MAX_DEPTH) {
				continue;
			}
			List<Long> parents = hierarchyMapper.selectList(Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
				.eq(OntEntityTypeHierarchy::getChildId, current)).stream()
				.map(OntEntityTypeHierarchy::getParentId)
				.toList();
			for (Long parent : parents) {
				int newDepth = currentDepth + 1;
				if (newDepth > MAX_DEPTH) {
					continue;
				}
				if (!distanceMap.containsKey(parent) || distanceMap.get(parent) > newDepth) {
					distanceMap.put(parent, newDepth);
					queue.addLast(parent);
				}
			}
		}
		return distanceMap;
	}

	private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OntObjectProperty> buildQueryWrapper(
			OntObjectPropertyQuery query, List<Long> propertyIds) {
		return Wrappers.<OntObjectProperty>lambdaQuery()
			.like(StrUtil.isNotBlank(query.getName()), OntObjectProperty::getName, query.getName())
			.eq(query.getOntologyId() != null, OntObjectProperty::getOntologyId, query.getOntologyId())
			.eq(query.getNamespaceId() != null, OntObjectProperty::getNamespaceId, query.getNamespaceId())
			.eq(StrUtil.isNotBlank(query.getSourceType()), OntObjectProperty::getSourceType, query.getSourceType())
			.eq(StrUtil.isNotBlank(query.getIsBuiltin()), OntObjectProperty::getIsBuiltin, query.getIsBuiltin())
			.eq(StrUtil.isNotBlank(query.getIsFunctional()), OntObjectProperty::getIsFunctional,
					query.getIsFunctional())
			.eq(StrUtil.isNotBlank(query.getIsInverseFunctional()), OntObjectProperty::getIsInverseFunctional,
					query.getIsInverseFunctional())
			.eq(StrUtil.isNotBlank(query.getIsTransitive()), OntObjectProperty::getIsTransitive,
					query.getIsTransitive())
			.eq(StrUtil.isNotBlank(query.getIsSymmetric()), OntObjectProperty::getIsSymmetric, query.getIsSymmetric())
			.apply(StrUtil.isNotBlank(query.getInferenceSupport()),
					"inference_support @> ('[\"' || ? || '\"]')::jsonb", query.getInferenceSupport())
			.in(propertyIds != null && !propertyIds.isEmpty(), OntObjectProperty::getId, propertyIds)
			.orderByAsc(OntObjectProperty::getSortOrder)
			.orderByAsc(OntObjectProperty::getId);
	}

	/**
	 * 按domainEntityTypeId/rangeEntityTypeId过滤属性ID集合。
	 * 这两个条件是子表关联过滤，需要先确定主表ID再分页。
	 */
	private List<Long> filterPropertyIdsByDomainRange(OntObjectPropertyQuery query) {
		if (query.getDomainEntityTypeId() == null && query.getRangeEntityTypeId() == null) {
			return null;
		}
		Set<Long> domainMatched = null;
		Set<Long> rangeMatched = null;
		if (query.getDomainEntityTypeId() != null) {
			domainMatched = domainMapper.selectList(Wrappers.<OntObjectPropertyDomain>lambdaQuery()
				.eq(OntObjectPropertyDomain::getEntityTypeId, query.getDomainEntityTypeId()))
				.stream().map(OntObjectPropertyDomain::getObjectPropertyId).collect(Collectors.toSet());
		}
		if (query.getRangeEntityTypeId() != null) {
			rangeMatched = rangeMapper.selectList(Wrappers.<OntObjectPropertyRange>lambdaQuery()
				.eq(OntObjectPropertyRange::getEntityTypeId, query.getRangeEntityTypeId()))
				.stream().map(OntObjectPropertyRange::getObjectPropertyId).collect(Collectors.toSet());
		}
		if (domainMatched != null && rangeMatched != null) {
			return domainMatched.stream().filter(rangeMatched::contains).toList();
		}
		return domainMatched != null ? new ArrayList<>(domainMatched) : new ArrayList<>(rangeMatched);
	}

	/**
	 * 批量组装摘要VO，消除逐行N+1查询。
	 */
	private List<OntObjectPropertySummaryVO> buildSummaryList(List<OntObjectProperty> properties) {
		if (properties == null || properties.isEmpty()) {
			return List.of();
		}
		List<Long> propIds = properties.stream().map(OntObjectProperty::getId).toList();

		Map<Long, String> labelMap = batchLoadZhLabels(propIds);
		Map<Long, List<OntEntityTypeRefVO>> domainMap = batchLoadDomainRefs(propIds);
		Map<Long, List<OntEntityTypeRefVO>> rangeMap = batchLoadRangeRefs(propIds);

		Set<Long> inverseIds = properties.stream().map(OntObjectProperty::getInverseOfId)
			.filter(Objects::nonNull).collect(Collectors.toSet());
		Map<Long, OntObjectProperty> inversePropMap = inverseIds.isEmpty() ? Collections.emptyMap()
				: this.listByIds(inverseIds).stream()
					.collect(Collectors.toMap(OntObjectProperty::getId, Function.identity()));
		Map<Long, String> inverseLabelMap = inverseIds.isEmpty() ? Collections.emptyMap()
				: batchLoadZhLabels(inverseIds);

		return properties.stream().map(prop -> {
			OntObjectPropertySummaryVO vo = new OntObjectPropertySummaryVO();
			vo.setObjectProperty(prop);
			vo.setLabel(labelMap.get(prop.getId()));
			vo.setDomains(domainMap.getOrDefault(prop.getId(), List.of()));
			vo.setRanges(rangeMap.getOrDefault(prop.getId(), List.of()));
			if (prop.getInverseOfId() != null) {
				OntObjectProperty inverse = inversePropMap.get(prop.getInverseOfId());
				if (inverse != null) {
					vo.setInversePropertyName(inverse.getName());
					vo.setInversePropertyLabel(inverseLabelMap.get(prop.getInverseOfId()));
				}
			}
			vo.setInferenceSupport(prop.getInferenceSupport());
			return vo;
		}).toList();
	}

	private Map<Long, String> batchLoadZhLabels(Collection<Long> propIds) {
		return labelMapper.selectList(Wrappers.<OntObjectPropertyLabel>lambdaQuery()
			.eq(OntObjectPropertyLabel::getLocale, ZH)
			.in(OntObjectPropertyLabel::getObjectPropertyId, propIds))
			.stream()
			.collect(Collectors.toMap(OntObjectPropertyLabel::getObjectPropertyId,
				OntObjectPropertyLabel::getLabel, (existing, replacement) -> existing));
	}

	private Map<Long, List<OntEntityTypeRefVO>> batchLoadDomainRefs(Collection<Long> propIds) {
		List<OntObjectPropertyDomain> domains = domainMapper.selectList(
			Wrappers.<OntObjectPropertyDomain>lambdaQuery()
				.in(OntObjectPropertyDomain::getObjectPropertyId, propIds)
				.orderByAsc(OntObjectPropertyDomain::getSortOrder));
		if (domains.isEmpty()) {
			return Collections.emptyMap();
		}
		Set<Long> typeIds = domains.stream().map(OntObjectPropertyDomain::getEntityTypeId)
			.collect(Collectors.toSet());
		Map<Long, OntEntityType> typeMap = batchLoadEntityTypes(typeIds);
		Map<Long, String> typeLabelMap = batchLoadEntityTypeZhLabels(typeIds);
		return domains.stream().collect(Collectors.groupingBy(OntObjectPropertyDomain::getObjectPropertyId,
			Collectors.mapping(d -> toRefVO(d.getEntityTypeId(), typeMap, typeLabelMap), Collectors.toList())));
	}

	private Map<Long, List<OntEntityTypeRefVO>> batchLoadRangeRefs(Collection<Long> propIds) {
		List<OntObjectPropertyRange> ranges = rangeMapper.selectList(
			Wrappers.<OntObjectPropertyRange>lambdaQuery()
				.in(OntObjectPropertyRange::getObjectPropertyId, propIds)
				.orderByAsc(OntObjectPropertyRange::getSortOrder));
		if (ranges.isEmpty()) {
			return Collections.emptyMap();
		}
		Set<Long> typeIds = ranges.stream().map(OntObjectPropertyRange::getEntityTypeId).collect(Collectors.toSet());
		Map<Long, OntEntityType> typeMap = batchLoadEntityTypes(typeIds);
		Map<Long, String> typeLabelMap = batchLoadEntityTypeZhLabels(typeIds);
		return ranges.stream().collect(Collectors.groupingBy(OntObjectPropertyRange::getObjectPropertyId,
			Collectors.mapping(r -> toRefVO(r.getEntityTypeId(), typeMap, typeLabelMap), Collectors.toList())));
	}

	private List<OntEntityTypeRefVO> buildDomainRefs(Long propId) {
		return batchLoadDomainRefs(List.of(propId)).getOrDefault(propId, List.of());
	}

	private List<OntEntityTypeRefVO> buildRangeRefs(Long propId) {
		return batchLoadRangeRefs(List.of(propId)).getOrDefault(propId, List.of());
	}

	private Map<Long, OntEntityType> batchLoadEntityTypes(Set<Long> typeIds) {
		return typeIds.isEmpty() ? Collections.emptyMap()
				: entityTypeMapper.selectBatchIds(typeIds).stream()
					.collect(Collectors.toMap(OntEntityType::getId, Function.identity()));
	}

	private Map<Long, String> batchLoadEntityTypeZhLabels(Set<Long> typeIds) {
		return typeIds.isEmpty() ? Collections.emptyMap()
				: entityTypeLabelMapper.selectList(Wrappers.<OntEntityTypeLabel>lambdaQuery()
					.eq(OntEntityTypeLabel::getLocale, ZH)
					.in(OntEntityTypeLabel::getEntityTypeId, typeIds))
					.stream()
					.collect(Collectors.toMap(OntEntityTypeLabel::getEntityTypeId, OntEntityTypeLabel::getLabel,
						(existing, replacement) -> existing));
	}

	private OntEntityTypeRefVO toRefVO(Long typeId, Map<Long, OntEntityType> typeMap,
			Map<Long, String> typeLabelMap) {
		OntEntityTypeRefVO ref = new OntEntityTypeRefVO();
		ref.setId(typeId);
		OntEntityType type = typeMap.get(typeId);
		if (type != null) {
			ref.setName(type.getName());
			ref.setIri(type.getIri());
		}
		ref.setLabel(typeLabelMap.get(typeId));
		return ref;
	}

	private String getZhLabel(Long propId) {
		return batchLoadZhLabels(List.of(propId)).get(propId);
	}

	private void saveDomainAndRange(Long propId, List<Long> domainIds, List<Long> rangeIds) {
		int order = 1;
		for (Long typeId : domainIds) {
			OntObjectPropertyDomain domain = new OntObjectPropertyDomain();
			domain.setObjectPropertyId(propId);
			domain.setEntityTypeId(typeId);
			domain.setSortOrder(order++);
			domainMapper.insert(domain);
		}
		order = 1;
		for (Long typeId : rangeIds) {
			OntObjectPropertyRange range = new OntObjectPropertyRange();
			range.setObjectPropertyId(propId);
			range.setEntityTypeId(typeId);
			range.setSortOrder(order++);
			rangeMapper.insert(range);
		}
	}

	private void replaceDomainAndRange(Long propId, List<Long> domainIds, List<Long> rangeIds) {
		domainMapper.delete(Wrappers.<OntObjectPropertyDomain>lambdaQuery()
			.eq(OntObjectPropertyDomain::getObjectPropertyId, propId));
		rangeMapper.delete(Wrappers.<OntObjectPropertyRange>lambdaQuery()
			.eq(OntObjectPropertyRange::getObjectPropertyId, propId));
		saveDomainAndRange(propId, domainIds, rangeIds);
	}

	private List<Long> loadExistingDomainIds(Long propId) {
		return domainMapper.selectList(Wrappers.<OntObjectPropertyDomain>lambdaQuery()
			.eq(OntObjectPropertyDomain::getObjectPropertyId, propId)
			.orderByAsc(OntObjectPropertyDomain::getSortOrder))
			.stream().map(OntObjectPropertyDomain::getEntityTypeId).toList();
	}

	private List<Long> loadExistingRangeIds(Long propId) {
		return rangeMapper.selectList(Wrappers.<OntObjectPropertyRange>lambdaQuery()
			.eq(OntObjectPropertyRange::getObjectPropertyId, propId)
			.orderByAsc(OntObjectPropertyRange::getSortOrder))
			.stream().map(OntObjectPropertyRange::getEntityTypeId).toList();
	}

	private void saveLabel(Long propId, String locale, String label) {
		if (!StringUtils.hasText(label)) {
			return;
		}
		int updated = labelMapper.update(null, Wrappers.<OntObjectPropertyLabel>lambdaUpdate()
			.eq(OntObjectPropertyLabel::getObjectPropertyId, propId)
			.eq(OntObjectPropertyLabel::getLocale, locale)
			.set(OntObjectPropertyLabel::getLabel, label));
		if (updated == 0) {
			OntObjectPropertyLabel newLabel = new OntObjectPropertyLabel();
			newLabel.setObjectPropertyId(propId);
			newLabel.setLocale(locale);
			newLabel.setLabel(label);
			labelMapper.insert(newLabel);
		}
	}

	private List<String> computeSemanticWarnings(OntObjectProperty prop, List<OntEntityTypeRefVO> domains,
			List<OntEntityTypeRefVO> ranges) {
		List<String> warnings = new ArrayList<>();
		if (BUILTIN.equals(prop.getIsSymmetric())) {
			Set<Long> domainIds = domains.stream().map(OntEntityTypeRefVO::getId).collect(Collectors.toSet());
			Set<Long> rangeIds = ranges.stream().map(OntEntityTypeRefVO::getId).collect(Collectors.toSet());
			if (!domainIds.equals(rangeIds)) {
				warnings.add("对称属性的定义域和值域集合不同，可能通过对称推理使关系两端获得额外类型");
			}
		}
		return warnings;
	}

}
