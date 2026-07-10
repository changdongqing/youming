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
import com.pig4cloud.pig.ontology.dto.OntDataPropertyCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntDataPropertyQuery;
import com.pig4cloud.pig.ontology.dto.OntDataPropertyUpdateDTO;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntDataPropertyEnum;
import com.pig4cloud.pig.ontology.entity.OntDataPropertyLabel;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.entity.OntUnitCategory;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyEnumMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitCategoryMapper;
import com.pig4cloud.pig.ontology.service.OntDataPropertyService;
import com.pig4cloud.pig.ontology.service.OntEntityTypeService;
import com.pig4cloud.pig.ontology.vo.OntApplicableDataPropertyVO;
import com.pig4cloud.pig.ontology.vo.OntDataPropertyDetailVO;
import com.pig4cloud.pig.ontology.vo.OntDataPropertySummaryVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
 * 数据属性服务实现。
 *
 * @author youming
 */
@Service
@AllArgsConstructor
public class OntDataPropertyServiceImpl extends ServiceImpl<OntDataPropertyMapper, OntDataProperty>
		implements OntDataPropertyService {

	private static final String BUILTIN = "1";

	private static final String EXTENSION = "0";

	private static final String ZH = "zh";

	private static final int MAX_DEPTH = 100;

	private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z][a-zA-Z0-9]*$");

	private static final Pattern LOCAL_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");

	private static final Comparator<OntDataProperty> PROPERTY_COMPARATOR = Comparator
		.comparing(OntDataProperty::getSortOrder, Comparator.nullsLast(Integer::compareTo))
		.thenComparing(OntDataProperty::getId);

	private final OntDataPropertyLabelMapper labelMapper;

	private final OntDataPropertyEnumMapper enumMapper;

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntEntityTypeLabelMapper entityTypeLabelMapper;

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	private final OntNamespaceMapper namespaceMapper;

	private final OntOntologyProjectMapper ontologyProjectMapper;

	private final OntUnitCategoryMapper unitCategoryMapper;

	@Override
	public IPage<OntDataPropertySummaryVO> pageSummary(Page<OntDataProperty> page, OntDataPropertyQuery query) {
		Page<OntDataProperty> rawPage = this.page(page, buildQueryWrapper(query));
		return rawPage.convert(this::toSummary);
	}

	@Override
	public List<OntDataPropertySummaryVO> listSummary(OntDataPropertyQuery query) {
		List<OntDataProperty> list = this.list(buildQueryWrapper(query));
		return list.stream().map(this::toSummary).toList();
	}

	@Override
	public OntDataPropertyDetailVO getDetail(Long id) {
		OntDataProperty dataProperty = this.getById(id);
		if (dataProperty == null) {
			return null;
		}
		OntDataPropertyDetailVO detail = new OntDataPropertyDetailVO();
		detail.setDataProperty(dataProperty);
		detail.setLabels(labelMapper.selectList(Wrappers.<OntDataPropertyLabel>lambdaQuery()
			.eq(OntDataPropertyLabel::getDataPropertyId, id)
			.orderByAsc(OntDataPropertyLabel::getLocale)));
		detail.setEnums(enumMapper.selectList(Wrappers.<OntDataPropertyEnum>lambdaQuery()
			.eq(OntDataPropertyEnum::getDataPropertyId, id)
			.orderByAsc(OntDataPropertyEnum::getSortOrder)));
		detail.setDomainEntityType(entityTypeMapper.selectById(dataProperty.getDomainEntityTypeId()));
		detail.setNamespace(namespaceMapper.selectById(dataProperty.getNamespaceId()));
		if (dataProperty.getUnitCategoryId() != null) {
			detail.setUnitCategory(unitCategoryMapper.selectById(dataProperty.getUnitCategoryId()));
		}
		detail.setDisplayName(resolveDisplayName(dataProperty));
		return detail;
	}

	@Override
	public List<OntApplicableDataPropertyVO> listApplicableByDomain(Long entityTypeId) {
		OntEntityType entityType = entityTypeMapper.selectById(entityTypeId);
		if (entityType == null) {
			return List.of();
		}

		// BFS向上收集自身及全部祖先ID和最短距离
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

		// 查询所有定义域上的有效属性
		List<OntDataProperty> properties = this.list(Wrappers.<OntDataProperty>lambdaQuery()
			.in(OntDataProperty::getDomainEntityTypeId, distanceMap.keySet())
			.orderByAsc(OntDataProperty::getSortOrder)
			.orderByAsc(OntDataProperty::getId));
		if (properties.isEmpty()) {
			return List.of();
		}

		// 以属性ID去重，保留最短继承距离
		Map<Long, OntApplicableDataPropertyVO> resultMap = new HashMap<>();
		Map<Long, Integer> propertyDistance = new HashMap<>();
		for (OntDataProperty prop : properties) {
			int distance = distanceMap.getOrDefault(prop.getDomainEntityTypeId(), 0);
			Integer existing = propertyDistance.get(prop.getId());
			if (existing == null || distance < existing) {
				propertyDistance.put(prop.getId(), distance);
				OntApplicableDataPropertyVO vo = new OntApplicableDataPropertyVO();
				vo.setDataProperty(prop);
				vo.setInherited(distance > 0);
				vo.setInheritanceDistance(distance);
				vo.setDeclaredDomainEntityTypeId(prop.getDomainEntityTypeId());
				vo.setDisplayName(resolveDisplayName(prop));
				resultMap.put(prop.getId(), vo);
			}
		}

		// 批量查询中文标签
		Set<Long> propIds = resultMap.keySet();
		Map<Long, String> labelMap = labelMapper.selectList(Wrappers.<OntDataPropertyLabel>lambdaQuery()
			.eq(OntDataPropertyLabel::getLocale, ZH)
			.in(OntDataPropertyLabel::getDataPropertyId, propIds))
			.stream()
			.collect(Collectors.toMap(OntDataPropertyLabel::getDataPropertyId, OntDataPropertyLabel::getLabel));

		// 批量查询定义域实体类型名称和标签
		Set<Long> domainTypeIds = resultMap.values().stream()
			.map(v -> v.getDeclaredDomainEntityTypeId())
			.collect(Collectors.toSet());
		Map<Long, OntEntityType> domainTypeMap = entityTypeMapper.selectBatchIds(domainTypeIds).stream()
			.collect(Collectors.toMap(OntEntityType::getId, Function.identity()));
		Map<Long, String> domainLabelMap = entityTypeLabelMapper.selectList(Wrappers.<OntEntityTypeLabel>lambdaQuery()
			.eq(OntEntityTypeLabel::getLocale, ZH)
			.in(OntEntityTypeLabel::getEntityTypeId, domainTypeIds))
			.stream()
			.collect(Collectors.toMap(OntEntityTypeLabel::getEntityTypeId, OntEntityTypeLabel::getLabel));

		for (OntApplicableDataPropertyVO vo : resultMap.values()) {
			vo.setLabel(labelMap.get(vo.getDataProperty().getId()));
			OntEntityType domain = domainTypeMap.get(vo.getDeclaredDomainEntityTypeId());
			if (domain != null) {
				vo.setDeclaredDomainName(domain.getName());
			}
			vo.setDeclaredDomainLabel(domainLabelMap.get(vo.getDeclaredDomainEntityTypeId()));
		}

		// 排序：直接属性优先，其次继承距离升序，再按sort_order/id
		return resultMap.values().stream()
			.sorted(Comparator.comparing(OntApplicableDataPropertyVO::getInheritanceDistance)
				.thenComparing(vo -> vo.getDataProperty().getSortOrder(), Comparator.nullsLast(Integer::compareTo))
				.thenComparing(vo -> vo.getDataProperty().getId()))
			.toList();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntDataProperty> saveDataProperty(OntDataPropertyCreateDTO request) {
		Long ontologyId = request.getOntologyId() == null
				? OntEntityTypeService.CORE_ONTOLOGY_ID : request.getOntologyId();

		ValidationContext context = validateSemanticFields(ontologyId, request.getNamespaceId(), request.getName(),
			request.getIriLocalName(), request.getIri(), request.getDomainEntityTypeId(), null);
		if (context.error() != null) {
			return R.failed(context.error());
		}

		String valueModeError = validateValueMode(request.getBaseType(), request.getValueMode(),
			request.getValueSourceRef(), request.getUnitCategoryId(), request.getUnitRefMode(),
			request.getRegexPattern(), request.getIsUnique(), request.getEnumValues());
		if (valueModeError != null) {
			return R.failed(valueModeError);
		}

		OntDataProperty dataProperty = new OntDataProperty();
		dataProperty.setIri(context.expectedIri());
		dataProperty.setIriLocalName(context.iriLocalName());
		dataProperty.setName(request.getName());
		dataProperty.setPreferredAlias(request.getPreferredAlias());
		dataProperty.setDefinition(request.getDefinition());
		dataProperty.setDomainEntityTypeId(request.getDomainEntityTypeId());
		dataProperty.setBaseType(request.getBaseType());
		dataProperty.setValueMode(request.getValueMode());
		dataProperty.setValueSourceRef(request.getValueSourceRef());
		dataProperty.setRegexPattern(request.getRegexPattern());
		dataProperty.setFormatHint(request.getFormatHint());
		dataProperty.setIsUnique(StringUtils.hasText(request.getIsUnique()) ? request.getIsUnique() : EXTENSION);
		dataProperty.setUnitCategoryId(request.getUnitCategoryId());
		dataProperty.setUnitRefMode(request.getUnitRefMode());
		dataProperty.setSourceType("EXTENSION");
		dataProperty.setIsBuiltin(EXTENSION);
		dataProperty.setOntologyId(ontologyId);
		dataProperty.setNamespaceId(request.getNamespaceId());
		dataProperty.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
		dataProperty.setRemarks(request.getRemarks());
		this.save(dataProperty);

		saveLabel(dataProperty.getId(), ZH, request.getLabel());
		replaceEnums(dataProperty.getId(), request.getEnumValues());
		return R.ok(dataProperty);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntDataProperty> updateDataProperty(OntDataPropertyUpdateDTO request) {
		OntDataProperty old = this.getById(request.getId());
		if (old == null) {
			return R.failed("数据属性不存在");
		}

		if (BUILTIN.equals(old.getIsBuiltin())) {
			if (hasSemanticChanges(request, old)) {
				return R.failed("内置数据属性语义字段不可修改");
			}
			this.update(Wrappers.<OntDataProperty>lambdaUpdate()
				.eq(OntDataProperty::getId, old.getId())
				.set(OntDataProperty::getSortOrder,
					request.getSortOrder() == null ? old.getSortOrder() : request.getSortOrder())
				.set(OntDataProperty::getRemarks, request.getRemarks()));
			saveLabel(old.getId(), ZH, request.getLabel());
			return R.ok(this.getById(old.getId()));
		}

		Long ontologyId = old.getOntologyId();
		Long namespaceId = request.getNamespaceId() != null ? request.getNamespaceId() : old.getNamespaceId();
		String name = StringUtils.hasText(request.getName()) ? request.getName() : old.getName();
		String iriLocalName = StringUtils.hasText(request.getIriLocalName()) ? request.getIriLocalName() : name;
		Long domainEntityTypeId = request.getDomainEntityTypeId() != null
				? request.getDomainEntityTypeId() : old.getDomainEntityTypeId();
		String baseType = StringUtils.hasText(request.getBaseType()) ? request.getBaseType() : old.getBaseType();
		String valueMode = StringUtils.hasText(request.getValueMode()) ? request.getValueMode() : old.getValueMode();
		String valueSourceRef = request.getValueSourceRef() != null
				? request.getValueSourceRef() : old.getValueSourceRef();
		Long unitCategoryId = request.getUnitCategoryId();
		String unitRefMode = request.getUnitRefMode() != null ? request.getUnitRefMode() : old.getUnitRefMode();
		String regexPattern = request.getRegexPattern() != null ? request.getRegexPattern() : old.getRegexPattern();
		String isUnique = StringUtils.hasText(request.getIsUnique()) ? request.getIsUnique() : old.getIsUnique();
		List<String> enumValues = request.getEnumValues();

		ValidationContext context = validateSemanticFields(ontologyId, namespaceId, name, iriLocalName,
			request.getIri(), domainEntityTypeId, old.getId());
		if (context.error() != null) {
			return R.failed(context.error());
		}

		String valueModeError = validateValueMode(baseType, valueMode, valueSourceRef, unitCategoryId, unitRefMode,
			regexPattern, isUnique, enumValues);
		if (valueModeError != null) {
			return R.failed(valueModeError);
		}

		this.update(Wrappers.<OntDataProperty>lambdaUpdate()
			.eq(OntDataProperty::getId, old.getId())
			.set(OntDataProperty::getIri, context.expectedIri())
			.set(OntDataProperty::getIriLocalName, context.iriLocalName())
			.set(OntDataProperty::getName, name)
			.set(OntDataProperty::getPreferredAlias, request.getPreferredAlias())
			.set(OntDataProperty::getDefinition, request.getDefinition())
			.set(OntDataProperty::getDomainEntityTypeId, domainEntityTypeId)
			.set(OntDataProperty::getBaseType, baseType)
			.set(OntDataProperty::getValueMode, valueMode)
			.set(OntDataProperty::getValueSourceRef, valueSourceRef)
			.set(OntDataProperty::getRegexPattern, regexPattern)
			.set(OntDataProperty::getFormatHint, request.getFormatHint())
			.set(OntDataProperty::getIsUnique, isUnique)
			.set(OntDataProperty::getUnitCategoryId, unitCategoryId)
			.set(OntDataProperty::getUnitRefMode, unitRefMode)
			.set(OntDataProperty::getSortOrder,
				request.getSortOrder() == null ? old.getSortOrder() : request.getSortOrder())
			.set(OntDataProperty::getRemarks, request.getRemarks()));
		saveLabel(old.getId(), ZH, request.getLabel());
		replaceEnums(old.getId(), enumValues);
		return R.ok(this.getById(old.getId()));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> removeDataProperty(Long id) {
		OntDataProperty dataProperty = this.getById(id);
		if (dataProperty == null) {
			return R.failed("数据属性不存在");
		}
		if (BUILTIN.equals(dataProperty.getIsBuiltin())) {
			return R.failed("内置数据属性不可删除");
		}
		// 后续公理/实例模块上线后追加引用检查
		enumMapper.delete(Wrappers.<OntDataPropertyEnum>lambdaQuery()
			.eq(OntDataPropertyEnum::getDataPropertyId, id));
		labelMapper.delete(Wrappers.<OntDataPropertyLabel>lambdaQuery()
			.eq(OntDataPropertyLabel::getDataPropertyId, id));
		return R.ok(this.removeById(id));
	}

	// ==================== 校验 ====================

	private ValidationContext validateSemanticFields(Long ontologyId, Long namespaceId, String name,
			String iriLocalName, String requestIri, Long domainEntityTypeId, Long excludeId) {
		OntOntologyProject ontology = ontologyProjectMapper.selectById(ontologyId);
		if (ontology == null) {
			return ValidationContext.failed("本体工程不存在");
		}
		OntNamespace namespace = namespaceMapper.selectById(namespaceId);
		if (namespace == null) {
			return ValidationContext.failed("命名空间不存在");
		}
		if (BUILTIN.equals(namespace.getIsBuiltin())) {
			return ValidationContext.failed("扩展数据属性必须使用扩展命名空间");
		}
		if (!NAME_PATTERN.matcher(name).matches()) {
			return ValidationContext.failed("英文名称必须以小写字母开头，仅支持英文字母和数字");
		}
		String effectiveLocalName = StringUtils.hasText(iriLocalName) ? iriLocalName : name;
		if (!LOCAL_NAME_PATTERN.matcher(effectiveLocalName).matches()) {
			return ValidationContext.failed("IRI本地名必须以字母开头，仅支持英文字母、数字和下划线");
		}
		OntEntityType domainType = entityTypeMapper.selectById(domainEntityTypeId);
		if (domainType == null) {
			return ValidationContext.failed("定义域实体类型不存在");
		}
		if (!Objects.equals(domainType.getOntologyId(), ontologyId)) {
			return ValidationContext.failed("定义域实体类型必须与当前属性属于同一本体工程");
		}
		String expectedIri = namespace.getUri() + effectiveLocalName;
		if (StringUtils.hasText(requestIri) && !expectedIri.equals(requestIri)) {
			return ValidationContext.failed("IRI必须等于命名空间URI与IRI本地名的拼接结果");
		}
		long iriCount = this.count(Wrappers.<OntDataProperty>lambdaQuery()
			.eq(OntDataProperty::getIri, expectedIri)
			.ne(excludeId != null, OntDataProperty::getId, excludeId));
		if (iriCount > 0) {
			return ValidationContext.failed("IRI已存在");
		}
		long localNameCount = this.count(Wrappers.<OntDataProperty>lambdaQuery()
			.eq(OntDataProperty::getOntologyId, ontologyId)
			.eq(OntDataProperty::getNamespaceId, namespaceId)
			.eq(OntDataProperty::getIriLocalName, effectiveLocalName)
			.ne(excludeId != null, OntDataProperty::getId, excludeId));
		if (localNameCount > 0) {
			return ValidationContext.failed("同一命名空间下IRI本地名已存在");
		}
		long nameCount = this.count(Wrappers.<OntDataProperty>lambdaQuery()
			.eq(OntDataProperty::getOntologyId, ontologyId)
			.eq(OntDataProperty::getNamespaceId, namespaceId)
			.eq(OntDataProperty::getDomainEntityTypeId, domainEntityTypeId)
			.eq(OntDataProperty::getName, name)
			.ne(excludeId != null, OntDataProperty::getId, excludeId));
		if (nameCount > 0) {
			return ValidationContext.failed("同一命名空间和定义域下英文名称已存在");
		}
		return new ValidationContext(expectedIri, effectiveLocalName, null);
	}

	private String validateValueMode(String baseType, String valueMode, String valueSourceRef,
			Long unitCategoryId, String unitRefMode, String regexPattern, String isUnique, List<String> enumValues) {
		boolean hasEnums = enumValues != null && !enumValues.isEmpty();
		// value_mode 组合校验
		if ("FREE".equals(valueMode) && hasEnums) {
			return "自由值模式不允许提交枚举值";
		}
		if ("CLOSED_ENUM".equals(valueMode) && !hasEnums) {
			return "闭合枚举模式至少需要一个枚举值";
		}
		if ("EXTERNAL_DICTIONARY".equals(valueMode)) {
			if (!StringUtils.hasText(valueSourceRef)) {
				return "外部字典模式必须指定值源代码";
			}
			if (hasEnums) {
				return "外部字典模式不允许提交本地枚举值";
			}
		}
		if ("UNIT_DICTIONARY".equals(valueMode)) {
			if (!"UNIT_REF".equals(baseType)) {
				return "单位字典模式要求值域类型为UNIT_REF";
			}
			if (hasEnums) {
				return "单位字典模式不允许提交本地枚举值";
			}
		}
		// base_type 组合校验
		if ("UNIT_REF".equals(baseType)) {
			if (!"UNIT_DICTIONARY".equals(valueMode)) {
				return "UNIT_REF值域类型的值模式必须为UNIT_DICTIONARY";
			}
			if (!"DICTIONARY_SYMBOL".equals(unitRefMode)) {
				return "UNIT_REF值域类型的单位引用模式必须为DICTIONARY_SYMBOL";
			}
		} else {
			if (unitRefMode != null) {
				return "非UNIT_REF值域类型不能设置单位引用模式";
			}
		}
		if (unitCategoryId != null && !"NUMERIC".equals(baseType) && !"UNIT_REF".equals(baseType)) {
			return "单位分类仅适用于NUMERIC或UNIT_REF值域类型";
		}
		if ("BOOLEAN".equals(baseType) || "DATE".equals(baseType)) {
			if (StringUtils.hasText(regexPattern)) {
				return baseType + "类型不允许设置正则约束";
			}
			if (BUILTIN.equals(isUnique)) {
				return baseType + "类型不允许设置唯一性约束";
			}
		}
		return null;
	}

	private boolean hasSemanticChanges(OntDataPropertyUpdateDTO request, OntDataProperty old) {
		return request.getNamespaceId() != null && !Objects.equals(request.getNamespaceId(), old.getNamespaceId())
				|| StringUtils.hasText(request.getName()) && !request.getName().equals(old.getName())
				|| StringUtils.hasText(request.getIriLocalName())
				|| StringUtils.hasText(request.getIri())
				|| request.getDomainEntityTypeId() != null
						&& !Objects.equals(request.getDomainEntityTypeId(), old.getDomainEntityTypeId())
				|| StringUtils.hasText(request.getBaseType())
				|| StringUtils.hasText(request.getValueMode())
				|| request.getValueSourceRef() != null
				|| request.getRegexPattern() != null
				|| request.getIsUnique() != null
				|| request.getUnitCategoryId() != null
				|| request.getUnitRefMode() != null
				|| request.getPreferredAlias() != null
				|| request.getDefinition() != null
				|| request.getFormatHint() != null
				|| request.getEnumValues() != null;
	}

	// ==================== 辅助方法 ====================

	private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OntDataProperty> buildQueryWrapper(
			OntDataPropertyQuery query) {
		return Wrappers.<OntDataProperty>lambdaQuery()
			.like(StrUtil.isNotBlank(query.getName()), OntDataProperty::getName, query.getName())
			.eq(query.getOntologyId() != null, OntDataProperty::getOntologyId, query.getOntologyId())
			.eq(query.getNamespaceId() != null, OntDataProperty::getNamespaceId, query.getNamespaceId())
			.eq(query.getDomainEntityTypeId() != null, OntDataProperty::getDomainEntityTypeId,
				query.getDomainEntityTypeId())
			.eq(StrUtil.isNotBlank(query.getBaseType()), OntDataProperty::getBaseType, query.getBaseType())
			.eq(StrUtil.isNotBlank(query.getValueMode()), OntDataProperty::getValueMode, query.getValueMode())
			.eq(StrUtil.isNotBlank(query.getSourceType()), OntDataProperty::getSourceType, query.getSourceType())
			.eq(StrUtil.isNotBlank(query.getIsBuiltin()), OntDataProperty::getIsBuiltin, query.getIsBuiltin())
			.orderByAsc(OntDataProperty::getSortOrder)
			.orderByAsc(OntDataProperty::getId);
	}

	private OntDataPropertySummaryVO toSummary(OntDataProperty prop) {
		OntDataPropertySummaryVO vo = new OntDataPropertySummaryVO();
		vo.setDataProperty(prop);
		vo.setDisplayName(resolveDisplayName(prop));
		List<OntDataPropertyLabel> labels = labelMapper.selectList(Wrappers.<OntDataPropertyLabel>lambdaQuery()
			.eq(OntDataPropertyLabel::getDataPropertyId, prop.getId())
			.eq(OntDataPropertyLabel::getLocale, ZH));
		if (!labels.isEmpty()) {
			vo.setLabel(labels.get(0).getLabel());
		}
		OntEntityType domainType = entityTypeMapper.selectById(prop.getDomainEntityTypeId());
		if (domainType != null) {
			vo.setDomainEntityTypeName(domainType.getName());
			List<OntEntityTypeLabel> domainLabels = entityTypeLabelMapper
				.selectList(Wrappers.<OntEntityTypeLabel>lambdaQuery()
					.eq(OntEntityTypeLabel::getEntityTypeId, domainType.getId())
					.eq(OntEntityTypeLabel::getLocale, ZH));
			if (!domainLabels.isEmpty()) {
				vo.setDomainEntityTypeLabel(domainLabels.get(0).getLabel());
			}
		}
		long enumCount = enumMapper.selectCount(Wrappers.<OntDataPropertyEnum>lambdaQuery()
			.eq(OntDataPropertyEnum::getDataPropertyId, prop.getId()));
		vo.setEnumCount((int) enumCount);
		if (prop.getUnitCategoryId() != null) {
			OntUnitCategory category = unitCategoryMapper.selectById(prop.getUnitCategoryId());
			if (category != null) {
				vo.setUnitCategoryName(category.getCategoryName());
			}
		}
		return vo;
	}

	private String resolveDisplayName(OntDataProperty prop) {
		if (StringUtils.hasText(prop.getPreferredAlias())) {
			return prop.getPreferredAlias();
		}
		return prop.getName();
	}

	private void saveLabel(Long dataPropertyId, String locale, String label) {
		int updated = labelMapper.update(null, Wrappers.<OntDataPropertyLabel>lambdaUpdate()
			.eq(OntDataPropertyLabel::getDataPropertyId, dataPropertyId)
			.eq(OntDataPropertyLabel::getLocale, locale)
			.set(OntDataPropertyLabel::getLabel, label));
		if (updated == 0) {
			OntDataPropertyLabel newLabel = new OntDataPropertyLabel();
			newLabel.setDataPropertyId(dataPropertyId);
			newLabel.setLocale(locale);
			newLabel.setLabel(label);
			labelMapper.insert(newLabel);
		}
	}

	private void replaceEnums(Long dataPropertyId, List<String> enumValues) {
		enumMapper.delete(Wrappers.<OntDataPropertyEnum>lambdaQuery()
			.eq(OntDataPropertyEnum::getDataPropertyId, dataPropertyId));
		if (enumValues == null || enumValues.isEmpty()) {
			return;
		}
		// 去首尾空格、去重并保留顺序
		Set<String> seen = new LinkedHashSet<>();
		int order = 1;
		for (String raw : enumValues) {
			String trimmed = raw == null ? "" : raw.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			if (seen.add(trimmed)) {
				OntDataPropertyEnum enumEntry = new OntDataPropertyEnum();
				enumEntry.setDataPropertyId(dataPropertyId);
				enumEntry.setEnumValue(trimmed);
				enumEntry.setIsStandard(EXTENSION);
				enumEntry.setSortOrder(order++);
				enumMapper.insert(enumEntry);
			}
		}
	}

	private record ValidationContext(String expectedIri, String iriLocalName, String error) {

		private static ValidationContext failed(String error) {
			return new ValidationContext(null, null, error);
		}

	}

}
