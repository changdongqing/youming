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
import com.pig4cloud.pig.ontology.dto.OntEntityInstanceCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntEntityInstanceQuery;
import com.pig4cloud.pig.ontology.dto.OntEntityInstanceUpdateDTO;
import com.pig4cloud.pig.ontology.dto.OntInstanceDataValueDTO;
import com.pig4cloud.pig.ontology.dto.OntInstanceObjectRelationDTO;
import com.pig4cloud.pig.ontology.dto.OntInstanceOptionQuery;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntDataPropertyEnum;
import com.pig4cloud.pig.ontology.entity.OntDataPropertyLabel;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel;
import com.pig4cloud.pig.ontology.entity.OntInstanceDataValue;
import com.pig4cloud.pig.ontology.entity.OntInstanceObjectRelation;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyDomain;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyLabel;
import com.pig4cloud.pig.ontology.entity.OntObjectPropertyRange;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.event.model.OntologyDomainEvent;
import com.pig4cloud.pig.ontology.event.model.OntologyEventTypes;
import com.pig4cloud.pig.ontology.event.service.OntDomainEventPublisher;
import com.pig4cloud.pig.ontology.security.masking.DataMaskingService;
import com.pig4cloud.pig.ontology.security.policy.DataAction;
import com.pig4cloud.pig.ontology.security.policy.DecisionEffect;
import com.pig4cloud.pig.ontology.security.policy.OntologyDataPolicyService;
import com.pig4cloud.pig.ontology.security.policy.PolicyDecision;
import com.pig4cloud.pig.ontology.security.policy.SecuredResource;
import com.pig4cloud.pig.ontology.security.policy.SecuritySubject;
import com.pig4cloud.pig.ontology.security.policy.SecuritySubjectResolver;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyEnumMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
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
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitMapper;
import com.pig4cloud.pig.ontology.service.OntEntityInstanceService;
import com.pig4cloud.pig.ontology.service.OntIriUniquenessService;
import com.pig4cloud.pig.ontology.vo.OntEntityTypeRefVO;
import com.pig4cloud.pig.ontology.vo.OntEntityInstanceDetailVO;
import com.pig4cloud.pig.ontology.vo.OntEntityInstanceOptionVO;
import com.pig4cloud.pig.ontology.vo.OntEntityInstanceSummaryVO;
import com.pig4cloud.pig.ontology.vo.OntInstanceDataValueVO;
import com.pig4cloud.pig.ontology.vo.OntInstanceFormMetaVO;
import com.pig4cloud.pig.ontology.vo.OntInstanceInboundRelationVO;
import com.pig4cloud.pig.ontology.vo.OntInstanceObjectRelationVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 实体对象实例服务实现。
 *
 * @author youming
 */
@Service
@AllArgsConstructor
public class OntEntityInstanceServiceImpl extends ServiceImpl<OntEntityInstanceMapper, OntEntityInstance>
		implements OntEntityInstanceService {

	private static final String BUILTIN = "1";

	private static final String EXTENSION = "0";

	private static final String ZH = "zh";

	private static final String TABLE_NAME = "ont_entity_instance";

	private static final int MAX_DEPTH = 100;

	private static final int MAX_OPTION_SIZE = 100;

	private static final Pattern LOCAL_NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{0,127}$");

	private static final Pattern ABSOLUTE_IRI_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:.");

	private static final DateTimeFormatter DATE_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private static final DateTimeFormatter DATE_COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntEntityTypeLabelMapper entityTypeLabelMapper;

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	private final OntNamespaceMapper namespaceMapper;

	private final OntOntologyProjectMapper ontologyProjectMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntDataPropertyLabelMapper dataPropertyLabelMapper;

	private final OntDataPropertyEnumMapper dataPropertyEnumMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	private final OntObjectPropertyLabelMapper objectPropertyLabelMapper;

	private final OntObjectPropertyDomainMapper objectPropertyDomainMapper;

	private final OntObjectPropertyRangeMapper objectPropertyRangeMapper;

	private final OntInstanceDataValueMapper dataValueMapper;

	private final OntInstanceObjectRelationMapper relationMapper;

	private final OntUnitMapper unitMapper;

	private final OntIriUniquenessService iriUniquenessService;

	private final OntDomainEventPublisher eventPublisher;

	private final OntologyDataPolicyService dataPolicyService;

	private final SecuritySubjectResolver securitySubjectResolver;

	private final DataMaskingService dataMaskingService;

	private final com.pig4cloud.pig.ontology.mapping.ingestion.MappedValueWriteGuard mappedValueWriteGuard;

	// ==================== 查询 ====================

	@Override
	public IPage<OntEntityInstanceSummaryVO> pageSummary(Page<OntEntityInstance> page,
			OntEntityInstanceQuery query) {
		Long ontologyId = query.getOntologyId() == null ? CORE_ONTOLOGY_ID : query.getOntologyId();
		List<Long> typeIds = filterTypeIdsBySubtypes(query, ontologyId);

		Page<OntEntityInstance> rawPage = this.page(page, buildQueryWrapper(query, ontologyId, typeIds));
		Page<OntEntityInstanceSummaryVO> resultPage = new Page<>(rawPage.getCurrent(), rawPage.getSize(),
				rawPage.getTotal());
		resultPage.setRecords(buildSummaryList(rawPage.getRecords()));
		return resultPage;
	}

	@Override
	public OntEntityInstanceDetailVO getDetail(Long id) {
		OntEntityInstance instance = this.getById(id);
		if (instance == null) {
			return null;
		}
		return buildDetailVO(instance);
	}

	@Override
	public OntInstanceFormMetaVO getFormMeta(Long entityTypeId) {
		OntEntityType entityType = entityTypeMapper.selectById(entityTypeId);
		if (entityType == null) {
			return null;
		}
		OntInstanceFormMetaVO vo = new OntInstanceFormMetaVO();
		vo.setRdfTypeId(entityTypeId);
		vo.setRdfTypeName(entityType.getName());
		vo.setIsAbstract(entityType.getIsAbstract());
		vo.setRdfTypeLabel(getEntityTypeZhLabel(entityTypeId));

		Map<Long, Integer> distanceMap = collectAncestorsWithDistance(entityTypeId);
		Set<Long> candidateTypeIds = distanceMap.keySet();

		vo.setApplicableDataProperties(buildDataPropertyMetas(candidateTypeIds, distanceMap));
		vo.setApplicableObjectProperties(buildObjectPropertyMetas(candidateTypeIds, distanceMap));
		return vo;
	}

	@Override
	public IPage<OntEntityInstanceOptionVO> pageOptions(Page<OntEntityInstance> page,
			OntInstanceOptionQuery query) {
		Long ontologyId = query.getOntologyId() == null ? CORE_ONTOLOGY_ID : query.getOntologyId();
		int size = (int) Math.min(page.getSize(), MAX_OPTION_SIZE);
		page.setSize(size);

		List<Long> typeIds = null;
		if (query.getObjectPropertyId() != null) {
			typeIds = collectRangeTypeIds(query.getObjectPropertyId());
			if (typeIds.isEmpty()) {
				Page<OntEntityInstanceOptionVO> emptyPage = new Page<>(page.getCurrent(), size, 0);
				emptyPage.setRecords(List.of());
				return emptyPage;
			}
		}

		var wrapper = Wrappers.<OntEntityInstance>lambdaQuery()
			.eq(OntEntityInstance::getOntologyId, ontologyId)
			.eq(OntEntityInstance::getDeclarationMode, "EXPLICIT")
			.and(StrUtil.isNotBlank(query.getKeyword()), w -> w
				.like(OntEntityInstance::getIriLocalName, query.getKeyword())
				.or()
				.like(OntEntityInstance::getLabel, query.getKeyword()))
			.in(typeIds != null, OntEntityInstance::getRdfTypeId, typeIds)
			.orderByAsc(OntEntityInstance::getSortOrder)
			.orderByAsc(OntEntityInstance::getId);

		Page<OntEntityInstance> rawPage = this.page(page, wrapper);
		Page<OntEntityInstanceOptionVO> resultPage = new Page<>(rawPage.getCurrent(), rawPage.getSize(),
				rawPage.getTotal());

		List<OntEntityInstance> records = rawPage.getRecords();
		if (records.isEmpty()) {
			resultPage.setRecords(List.of());
			return resultPage;
		}
		Set<Long> rdfTypeIds = records.stream().map(OntEntityInstance::getRdfTypeId)
			.filter(Objects::nonNull).collect(Collectors.toSet());
		Map<Long, String> typeNameMap = batchLoadEntityTypeNames(rdfTypeIds);

		resultPage.setRecords(records.stream().map(inst -> {
			OntEntityInstanceOptionVO vo = new OntEntityInstanceOptionVO();
			vo.setId(inst.getId());
			vo.setIri(inst.getIri());
			vo.setIriLocalName(inst.getIriLocalName());
			vo.setLabel(inst.getLabel());
			vo.setRdfTypeId(inst.getRdfTypeId());
			vo.setRdfTypeName(typeNameMap.get(inst.getRdfTypeId()));
			return vo;
		}).toList());
		return resultPage;
	}

	// ==================== 新增 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntEntityInstance> createInstance(OntEntityInstanceCreateDTO request) {
		Long ontologyId = request.getOntologyId() == null ? CORE_ONTOLOGY_ID : request.getOntologyId();

		// 1. 校验工程、命名空间、实体类型有效且同工程，类型非抽象
		String error = validateCreateContext(ontologyId, request.getNamespaceId(), request.getRdfTypeId());
		if (error != null) {
			return R.failed(error);
		}

		OntNamespace namespace = namespaceMapper.selectById(request.getNamespaceId());
		OntEntityType rdfType = entityTypeMapper.selectById(request.getRdfTypeId());

		// 2. 使用ASSIGN_ID分配实例ID（先保存以取得ID，再回填IRI；事务保证失败整体回滚）
		OntEntityInstance instance = new OntEntityInstance();
		instance.setOntologyId(ontologyId);
		instance.setNamespaceId(request.getNamespaceId());
		instance.setRdfTypeId(request.getRdfTypeId());
		instance.setLabel(request.getLabel());
		instance.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
		instance.setRemarks(request.getRemarks());
		instance.setSourceType("EXTENSION");
		instance.setDeclarationMode("EXPLICIT");
		instance.setIsBuiltin(EXTENSION);
		this.save(instance);

		// 3. 规范化本地名，未提交则按类型本地名_ID生成
		String effectiveLocalName = StringUtils.hasText(request.getIriLocalName())
				? request.getIriLocalName()
				: rdfType.getName() + "_" + instance.getId();

		if (!LOCAL_NAME_PATTERN.matcher(effectiveLocalName).matches()) {
			throw new IllegalArgumentException("IRI本地名必须以字母开头，仅支持英文字母、数字、下划线和短横线");
		}

		// 4. 后端拼接完整IRI并调用全局唯一性校验
		String expectedIri = namespace.getUri() + effectiveLocalName;
		String iriConflict = iriUniquenessService.checkIriConflict(expectedIri, TABLE_NAME, instance.getId());
		if (iriConflict != null) {
			throw new IllegalArgumentException(iriConflict);
		}
		// 检查实例表内部冲突（排除自身）
		long localConflict = this.count(Wrappers.<OntEntityInstance>lambdaQuery()
			.eq(OntEntityInstance::getOntologyId, ontologyId)
			.eq(OntEntityInstance::getNamespaceId, request.getNamespaceId())
			.eq(OntEntityInstance::getIriLocalName, effectiveLocalName)
			.ne(OntEntityInstance::getId, instance.getId()));
		if (localConflict > 0) {
			throw new IllegalArgumentException("同一命名空间下IRI本地名已存在");
		}

		instance.setIriLocalName(effectiveLocalName);
		instance.setIri(expectedIri);
		this.updateById(instance);

		// 5. 校验并插入数据值
		if (request.getDataValues() != null && !request.getDataValues().isEmpty()) {
			Map<Long, Integer> ancestors = collectAncestorsWithDistance(rdfType.getId());
			// 锁定涉及的 is_unique 数据属性行，避免并发唯一值穿透（设计§7.4-4）
			request.getDataValues().stream()
				.map(OntInstanceDataValueDTO::getDataPropertyId)
				.distinct()
				.map(dataPropertyMapper::selectById)
				.filter(Objects::nonNull)
				.filter(p -> BUILTIN.equals(p.getIsUnique()))
				.map(OntDataProperty::getId)
				.distinct()
				.forEach(dataPropertyMapper::selectByIdForUpdate);
			for (int i = 0; i < request.getDataValues().size(); i++) {
				OntInstanceDataValueDTO dto = request.getDataValues().get(i);
				String valError = validateDataValue(dto, instance.getId(), ontologyId, ancestors, true);
				if (valError != null) {
					throw new IllegalArgumentException("dataValues[" + i + "]: " + valError);
				}
			}
			insertDataValues(instance.getId(), request.getDataValues());
		}

		// 6. 校验并插入指向已存在实例的对象断言
		if (request.getObjectRelations() != null && !request.getObjectRelations().isEmpty()) {
			for (int i = 0; i < request.getObjectRelations().size(); i++) {
				OntInstanceObjectRelationDTO dto = request.getObjectRelations().get(i);
				String relError = validateRelation(instance.getId(), dto, ontologyId, rdfType, true);
				if (relError != null) {
					throw new IllegalArgumentException("objectRelations[" + i + "]: " + relError);
				}
			}
			insertRelations(instance.getId(), request.getObjectRelations());
		}

		publishInstanceChanged(instance, "CREATED");
		return R.ok(this.getById(instance.getId()));
	}

	// ==================== 修改 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntEntityInstance> updateInstance(OntEntityInstanceUpdateDTO request) {
		OntEntityInstance old = this.getById(request.getId());
		if (old == null) {
			return R.failed("实例不存在");
		}

		// 内置实例：仅可修改label/sortOrder/remarks
		if (BUILTIN.equals(old.getIsBuiltin())) {
			if (hasSemanticChanges(request, old)) {
				return R.failed("内置实例语义字段不可修改");
			}
			this.update(Wrappers.<OntEntityInstance>lambdaUpdate()
				.eq(OntEntityInstance::getId, old.getId())
				.set(OntEntityInstance::getLabel, request.getLabel())
				.set(OntEntityInstance::getSortOrder,
					request.getSortOrder() == null ? old.getSortOrder() : request.getSortOrder())
				.set(OntEntityInstance::getRemarks, request.getRemarks()));
			publishInstanceChanged(old, "UPDATED");
			return R.ok(this.getById(old.getId()));
		}

		// 扩展实例：可修改本地名、命名空间、类型、label、排序、备注
		Long namespaceId = request.getNamespaceId() != null ? request.getNamespaceId() : old.getNamespaceId();
		Long rdfTypeId = request.getRdfTypeId() != null ? request.getRdfTypeId() : old.getRdfTypeId();
		Long ontologyId = old.getOntologyId();

		OntNamespace namespace = namespaceMapper.selectById(namespaceId);
		if (namespace == null) {
			return R.failed("命名空间不存在");
		}
		OntEntityType rdfType = entityTypeMapper.selectById(rdfTypeId);
		if (rdfType == null) {
			return R.failed("实体类型不存在");
		}
		if (!Objects.equals(rdfType.getOntologyId(), ontologyId)) {
			return R.failed("实体类型必须与实例属于同一本体工程");
		}
		if (BUILTIN.equals(rdfType.getIsAbstract())) {
			return R.failed("不能实例化抽象实体类型");
		}

		String iriLocalName = StringUtils.hasText(request.getIriLocalName()) ? request.getIriLocalName()
				: old.getIriLocalName();
		if (!LOCAL_NAME_PATTERN.matcher(iriLocalName).matches()) {
			return R.failed("IRI本地名必须以字母开头，仅支持英文字母、数字、下划线和短横线");
		}
		String expectedIri = namespace.getUri() + iriLocalName;

		// IRI全局唯一性校验（排除自身）
		String iriConflict = iriUniquenessService.checkIriConflict(expectedIri, TABLE_NAME, old.getId());
		if (iriConflict != null) {
			return R.failed(iriConflict);
		}
		long localConflict = this.count(Wrappers.<OntEntityInstance>lambdaQuery()
			.eq(OntEntityInstance::getOntologyId, ontologyId)
			.eq(OntEntityInstance::getNamespaceId, namespaceId)
			.eq(OntEntityInstance::getIriLocalName, iriLocalName)
			.ne(OntEntityInstance::getId, old.getId()));
		if (localConflict > 0) {
			return R.failed("同一命名空间下IRI本地名已存在");
		}

		// 修改类型前必须以新类型重新校验全部数据值和出向断言定义域、入向断言值域
		if (request.getRdfTypeId() != null && !request.getRdfTypeId().equals(old.getRdfTypeId())) {
			String recheckError = revalidateForTypeChange(old.getId(), rdfTypeId, ontologyId);
			if (recheckError != null) {
				return R.failed(recheckError);
			}
		}

		this.update(Wrappers.<OntEntityInstance>lambdaUpdate()
			.eq(OntEntityInstance::getId, old.getId())
			.set(OntEntityInstance::getIri, expectedIri)
			.set(OntEntityInstance::getIriLocalName, iriLocalName)
			.set(OntEntityInstance::getNamespaceId, namespaceId)
			.set(OntEntityInstance::getRdfTypeId, rdfTypeId)
			.set(OntEntityInstance::getLabel, request.getLabel())
			.set(OntEntityInstance::getSortOrder,
				request.getSortOrder() == null ? old.getSortOrder() : request.getSortOrder())
			.set(OntEntityInstance::getRemarks, request.getRemarks()));
		publishInstanceChanged(old, "UPDATED");
		return R.ok(this.getById(old.getId()));
	}

	// ==================== 删除 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> deleteInstance(Long id) {
		OntEntityInstance instance = this.getById(id);
		if (instance == null) {
			return R.failed("实例不存在");
		}
		if (BUILTIN.equals(instance.getIsBuiltin())) {
			return R.failed("内置实例不可删除");
		}

		// 查询作为INSTANCE客体的有效入向断言
		List<OntInstanceObjectRelation> inbound = relationMapper.selectList(
			Wrappers.<OntInstanceObjectRelation>lambdaQuery()
				.eq(OntInstanceObjectRelation::getObjectInstanceId, id)
				.eq(OntInstanceObjectRelation::getObjectKind, "INSTANCE"));
		if (!inbound.isEmpty()) {
			return R.failed("该实例被" + inbound.size() + "条入向断言引用，不能删除");
		}

		// 物理删除数据值和出向断言，逻辑删除主表
		dataValueMapper.delete(Wrappers.<OntInstanceDataValue>lambdaQuery()
			.eq(OntInstanceDataValue::getInstanceId, id));
		relationMapper.delete(Wrappers.<OntInstanceObjectRelation>lambdaQuery()
			.eq(OntInstanceObjectRelation::getSubjectInstanceId, id));
		this.removeById(id);
		publishInstanceChanged(instance, "DELETED");
		return R.ok(true);
	}

	// ==================== 数据值接口 ====================

	@Override
	public List<OntInstanceDataValueVO> getDataValues(Long instanceId) {
		List<OntInstanceDataValue> values = dataValueMapper.selectList(
			Wrappers.<OntInstanceDataValue>lambdaQuery()
				.eq(OntInstanceDataValue::getInstanceId, instanceId)
				.orderByAsc(OntInstanceDataValue::getDataPropertyId)
				.orderByAsc(OntInstanceDataValue::getSortOrder));
		if (values.isEmpty()) {
			return List.of();
		}
		Set<Long> propIds = values.stream().map(OntInstanceDataValue::getDataPropertyId)
			.collect(Collectors.toSet());
		Map<Long, OntDataProperty> propMap = dataPropertyMapper.selectBatchIds(propIds).stream()
			.collect(Collectors.toMap(OntDataProperty::getId, Function.identity()));
		Map<Long, String> labelMap = batchLoadDataPropertyZhLabels(propIds);
		Set<Long> unitIds = values.stream().map(OntInstanceDataValue::getUnitId)
			.filter(Objects::nonNull).collect(Collectors.toSet());
		Map<Long, String> unitSymbolMap = batchLoadUnitSymbols(unitIds);

		// 模块36：安全策略过滤和脱敏
		OntEntityInstance instance = this.getById(instanceId);
		Long ontologyId = instance != null ? instance.getOntologyId() : null;
		SecuritySubject subject = securitySubjectResolver.resolve();

		return values.stream()
			// 策略决策：DENY 的属性值不返回
			.filter(v -> {
				if (ontologyId == null) {
					return true;
				}
				String levelCode = v.getSecurityLevelCode() != null ? v.getSecurityLevelCode()
					: (propMap.containsKey(v.getDataPropertyId())
						? propMap.get(v.getDataPropertyId()).getSecurityLevelCode() : "INTERNAL");
				SecuredResource resource = SecuredResource.builder()
					.resourceType("DATA_PROPERTY")
					.resourceId(v.getDataPropertyId())
					.securityLevelCode(levelCode)
					.build();
				PolicyDecision decision = dataPolicyService.decide(subject, ontologyId, resource, DataAction.VIEW);
				return decision.getEffect() != DecisionEffect.DENY;
			})
			// MASK 的属性值替换为脱敏展示值
			.map(v -> {
				if (ontologyId == null) {
					return toDataValueVO(v, propMap, labelMap, unitSymbolMap);
				}
				String levelCode = v.getSecurityLevelCode() != null ? v.getSecurityLevelCode()
					: (propMap.containsKey(v.getDataPropertyId())
						? propMap.get(v.getDataPropertyId()).getSecurityLevelCode() : "INTERNAL");
				SecuredResource resource = SecuredResource.builder()
					.resourceType("DATA_PROPERTY")
					.resourceId(v.getDataPropertyId())
					.securityLevelCode(levelCode)
					.build();
				PolicyDecision decision = dataPolicyService.decide(subject, ontologyId, resource, DataAction.VIEW);
				OntInstanceDataValueVO vo = toDataValueVO(v, propMap, labelMap, unitSymbolMap);
				if (decision.getEffect() == DecisionEffect.MASK && vo.getLiteralValue() != null) {
					vo.setLiteralValue(dataMaskingService.mask(vo.getLiteralValue(),
						decision.getMaskType(), null));
				}
				return vo;
			})
			.toList();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> replaceDataValues(Long instanceId, List<OntInstanceDataValueDTO> dataValues) {
		OntEntityInstance instance = this.getById(instanceId);
		if (instance == null) {
			return R.failed("实例不存在");
		}
		if (BUILTIN.equals(instance.getIsBuiltin())) {
			return R.failed("内置实例语义值不可通过普通接口修改");
		}
		// 锁定实例行，串行化同实例的并发数据值写入
		baseMapper.selectByIdForUpdate(instanceId);

		OntEntityType rdfType = entityTypeMapper.selectById(instance.getRdfTypeId());
		Map<Long, Integer> ancestors = collectAncestorsWithDistance(rdfType.getId());

		// 完整校验前锁定涉及的 is_unique 数据属性行，避免并发唯一值穿透（设计§7.4-4）
		Set<Long> uniquePropIds = dataValues.stream()
			.map(OntInstanceDataValueDTO::getDataPropertyId)
			.distinct()
			.map(dataPropertyMapper::selectById)
			.filter(Objects::nonNull)
			.filter(p -> BUILTIN.equals(p.getIsUnique()))
			.map(OntDataProperty::getId)
			.collect(Collectors.toSet());
		uniquePropIds.forEach(dataPropertyMapper::selectByIdForUpdate);

		// 完整校验
		for (int i = 0; i < dataValues.size(); i++) {
			OntInstanceDataValueDTO dto = dataValues.get(i);
			String error = validateDataValue(dto, instanceId, instance.getOntologyId(), ancestors, true);
			if (error != null) {
				return R.failed("dataValues[" + i + "]: " + error);
			}
		}

		// 来源所有权守卫：检查是否有映射来源拥有的值
		com.pig4cloud.pig.ontology.security.policy.SecuritySubject writeSubject = securitySubjectResolver.resolve();
		List<OntInstanceDataValue> existingValues = dataValueMapper.selectList(
			Wrappers.<OntInstanceDataValue>lambdaQuery()
				.eq(OntInstanceDataValue::getInstanceId, instanceId)
				.eq(OntInstanceDataValue::getDelFlag, "0"));
		for (OntInstanceDataValue existing : existingValues) {
			mappedValueWriteGuard.assertWritable(existing.getId(), writeSubject, false);
		}

		// 物理删除旧有效值，批量插入新值
		dataValueMapper.delete(Wrappers.<OntInstanceDataValue>lambdaQuery()
			.eq(OntInstanceDataValue::getInstanceId, instanceId));
		insertDataValues(instanceId, dataValues);
		return R.ok(true);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> deleteDataValues(Long instanceId, Long dataPropertyId) {
		OntEntityInstance instance = this.getById(instanceId);
		if (instance == null) {
			return R.failed("实例不存在");
		}
		if (BUILTIN.equals(instance.getIsBuiltin())) {
			return R.failed("内置实例语义值不可通过普通接口删除");
		}
		// 来源所有权守卫：检查待删除值是否被映射来源拥有
		com.pig4cloud.pig.ontology.security.policy.SecuritySubject deleteSubject = securitySubjectResolver.resolve();
		List<OntInstanceDataValue> toDelete = dataValueMapper.selectList(
			Wrappers.<OntInstanceDataValue>lambdaQuery()
				.eq(OntInstanceDataValue::getInstanceId, instanceId)
				.eq(OntInstanceDataValue::getDataPropertyId, dataPropertyId)
				.eq(OntInstanceDataValue::getDelFlag, "0"));
		for (OntInstanceDataValue dv : toDelete) {
			mappedValueWriteGuard.assertWritable(dv.getId(), deleteSubject, false);
		}

		dataValueMapper.delete(Wrappers.<OntInstanceDataValue>lambdaQuery()
			.eq(OntInstanceDataValue::getInstanceId, instanceId)
			.eq(OntInstanceDataValue::getDataPropertyId, dataPropertyId));
		return R.ok(true);
	}

	// ==================== 对象断言接口 ====================

	@Override
	public List<OntInstanceObjectRelationVO> getRelations(Long instanceId, String direction) {
		boolean incoming = "INCOMING".equalsIgnoreCase(direction);
		List<OntInstanceObjectRelation> relations;
		if (incoming) {
			relations = relationMapper.selectList(Wrappers.<OntInstanceObjectRelation>lambdaQuery()
				.eq(OntInstanceObjectRelation::getObjectInstanceId, instanceId)
				.eq(OntInstanceObjectRelation::getObjectKind, "INSTANCE")
				.orderByAsc(OntInstanceObjectRelation::getObjectPropertyId)
				.orderByAsc(OntInstanceObjectRelation::getSortOrder));
		} else {
			relations = relationMapper.selectList(Wrappers.<OntInstanceObjectRelation>lambdaQuery()
				.eq(OntInstanceObjectRelation::getSubjectInstanceId, instanceId)
				.orderByAsc(OntInstanceObjectRelation::getObjectPropertyId)
				.orderByAsc(OntInstanceObjectRelation::getSortOrder));
		}
		if (relations.isEmpty()) {
			return List.of();
		}
		return buildRelationVOs(relations);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntInstanceObjectRelationVO> addRelation(Long instanceId, OntInstanceObjectRelationDTO request) {
		OntEntityInstance subject = this.getById(instanceId);
		if (subject == null) {
			return R.failed("主体实例不存在");
		}
		if (BUILTIN.equals(subject.getIsBuiltin())) {
			return R.failed("内置实例断言不可通过普通接口增删");
		}

		// 按ID升序锁定主体实例和客体实例行，避免死锁（设计§7.4-5）
		Long minId = Math.min(instanceId, request.getObjectInstanceId());
		Long maxId = Math.max(instanceId, request.getObjectInstanceId());
		baseMapper.selectByIdForUpdate(minId);
		if (!minId.equals(maxId)) {
			baseMapper.selectByIdForUpdate(maxId);
		}
		// 锁定对象属性行，串行化功能性断言并发校验（设计§7.4-3）
		objectPropertyMapper.selectByIdForUpdate(request.getObjectPropertyId());

		OntEntityType subjectType = entityTypeMapper.selectById(subject.getRdfTypeId());
		String error = validateRelation(instanceId, request, subject.getOntologyId(), subjectType, false);
		if (error != null) {
			return R.failed(error);
		}

		OntInstanceObjectRelation rel = new OntInstanceObjectRelation();
		rel.setSubjectInstanceId(instanceId);
		rel.setObjectPropertyId(request.getObjectPropertyId());
		rel.setObjectKind("INSTANCE");
		rel.setObjectInstanceId(request.getObjectInstanceId());
		rel.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
		rel.setAssertionOrigin("MANUAL");
		relationMapper.insert(rel);

		return R.ok(buildRelationVOs(List.of(rel)).get(0));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> removeRelation(Long instanceId, Long relationId) {
		OntEntityInstance subject = this.getById(instanceId);
		if (subject == null) {
			return R.failed("主体实例不存在");
		}
		if (BUILTIN.equals(subject.getIsBuiltin())) {
			return R.failed("内置实例断言不可通过普通接口增删");
		}
		OntInstanceObjectRelation rel = relationMapper.selectById(relationId);
		if (rel == null || !Objects.equals(rel.getSubjectInstanceId(), instanceId)) {
			return R.failed("断言不存在或不属于该实例");
		}
		// 来源所有权守卫：映射来源创建的断言不可通过普通接口删除
		if (rel.getAssertionOrigin() != null && !"SEED".equals(rel.getAssertionOrigin())
			&& !"MANUAL".equals(rel.getAssertionOrigin())) {
			return R.failed("该断言由来源" + rel.getAssertionOrigin() + "创建，不可通过普通接口删除");
		}
		relationMapper.deleteById(relationId);
		return R.ok(true);
	}

	// ==================== 校验 ====================

	private String validateCreateContext(Long ontologyId, Long namespaceId, Long rdfTypeId) {
		OntOntologyProject ontology = ontologyProjectMapper.selectById(ontologyId);
		if (ontology == null) {
			return "本体工程不存在";
		}
		OntNamespace namespace = namespaceMapper.selectById(namespaceId);
		if (namespace == null) {
			return "命名空间不存在";
		}
		OntEntityType rdfType = entityTypeMapper.selectById(rdfTypeId);
		if (rdfType == null) {
			return "实体类型不存在";
		}
		if (!Objects.equals(rdfType.getOntologyId(), ontologyId)) {
			return "实体类型必须与实例属于同一本体工程";
		}
		if (BUILTIN.equals(rdfType.getIsAbstract())) {
			return "不能实例化抽象实体类型";
		}
		return null;
	}

	/**
	 * 数据属性值校验和规范化。
	 * 按base_type+value_mode校验，枚举校验，日期/数值/BOOLEAN规范化。
	 */
	private String validateDataValue(OntInstanceDataValueDTO dto, Long instanceId, Long ontologyId,
			Map<Long, Integer> ancestors, boolean checkUnique) {
		OntDataProperty prop = dataPropertyMapper.selectById(dto.getDataPropertyId());
		if (prop == null) {
			return "数据属性不存在: " + dto.getDataPropertyId();
		}
		// 定义域向上展开校验
		if (!ancestors.containsKey(prop.getDomainEntityTypeId())) {
			return "数据属性" + prop.getName() + "的定义域不匹配当前实例类型";
		}
		if (!Objects.equals(prop.getOntologyId(), ontologyId)) {
			return "数据属性必须与实例属于同一本体工程";
		}

		String baseType = prop.getBaseType();
		String valueMode = prop.getValueMode();
		String literalType = dto.getLiteralType();
		String value = dto.getLiteralValue();

		// TEXT_OR_NUMERIC必须显式提交literalType
		if ("TEXT_OR_NUMERIC".equals(baseType)) {
			if (!List.of("STRING", "INTEGER", "DECIMAL").contains(literalType)) {
				return "TEXT_OR_NUMERIC属性必须提交literalType为STRING/INTEGER/DECIMAL";
			}
		}

		// 按literalType校验和规范化值
		String normalized = normalizeLiteralValue(value, literalType);
		if (normalized == null) {
			return "字面量值与类型" + literalType + "不匹配";
		}
		dto.setLiteralValue(normalized);

		// 基础类型兼容性校验
		String compatError = checkBaseTypeCompatibility(baseType, literalType);
		if (compatError != null) {
			return compatError;
		}

		// URI必须为绝对IRI
		if ("URI".equals(literalType) && !ABSOLUTE_IRI_PATTERN.matcher(normalized).matches()) {
			return "URI值必须为绝对IRI";
		}

		// 正则约束
		if (StringUtils.hasText(prop.getRegexPattern())) {
			if (!Pattern.matches(prop.getRegexPattern(), normalized)) {
				return "值不符合数据属性正则约束: " + prop.getRegexPattern();
			}
		}

		// 枚举校验
		if ("CLOSED_ENUM".equals(valueMode)) {
			String enumError = validateClosedEnum(prop.getId(), normalized);
			if (enumError != null) {
				return enumError;
			}
		} else if ("OPEN_ENUM".equals(valueMode)) {
			normalizeOpenEnum(prop.getId(), dto);
		}

		// UNIT_REF校验
		if ("UNIT_REF".equals(baseType)) {
			String unitError = validateUnitRef(dto, prop);
			if (unitError != null) {
				return unitError;
			}
		}

		// 同一请求内重复值检查由调用方在循环前做，这里做is_unique工程级检查
		if (checkUnique && BUILTIN.equals(prop.getIsUnique())) {
			long count = dataValueMapper.selectCount(Wrappers.<OntInstanceDataValue>lambdaQuery()
				.eq(OntInstanceDataValue::getDataPropertyId, dto.getDataPropertyId())
				.eq(OntInstanceDataValue::getLiteralValue, normalized)
				.eq(OntInstanceDataValue::getDelFlag, EXTENSION)
				.ne(instanceId != null, OntInstanceDataValue::getInstanceId, instanceId));
			if (count > 0) {
				return "唯一数据属性值在工程内已存在";
			}
		}
		return null;
	}

	private String checkBaseTypeCompatibility(String baseType, String literalType) {
		return switch (baseType) {
			case "TEXT" -> "STRING".equals(literalType) ? null : "TEXT基础类型只能使用STRING字面量";
			case "URI" -> "URI".equals(literalType) ? null : "URI基础类型只能使用URI字面量";
			case "DATE" -> "DATE".equals(literalType) ? null : "DATE基础类型只能使用DATE字面量";
			case "NUMERIC" -> ("INTEGER".equals(literalType) || "DECIMAL".equals(literalType)) ? null
					: "NUMERIC基础类型只能使用INTEGER或DECIMAL字面量";
			case "BOOLEAN" -> "BOOLEAN".equals(literalType) ? null : "BOOLEAN基础类型只能使用BOOLEAN字面量";
			case "UNIT_REF" -> "STRING".equals(literalType) ? null : "UNIT_REF基础类型只能使用STRING字面量";
			case "TEXT_OR_NUMERIC" -> List.of("STRING", "INTEGER", "DECIMAL").contains(literalType) ? null
					: "TEXT_OR_NUMERIC只能使用STRING/INTEGER/DECIMAL字面量";
			default -> "未知基础类型: " + baseType;
		};
	}

	private String normalizeLiteralValue(String value, String literalType) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return switch (literalType) {
				case "STRING" -> value;
				case "URI" -> value.trim();
				case "DATE" -> normalizeDate(value.trim());
				case "INTEGER" -> new BigDecimal(value.trim()).toBigInteger().toString();
				case "DECIMAL" -> new BigDecimal(value.trim()).toPlainString();
				case "BOOLEAN" -> {
					String v = value.trim().toLowerCase();
					if ("true".equals(v) || "1".equals(v)) {
						yield "true";
					}
					if ("false".equals(v) || "0".equals(v)) {
						yield "false";
					}
					yield null;
				}
				default -> null;
			};
		} catch (Exception e) {
			return null;
		}
	}

	private String normalizeDate(String value) {
		try {
			if (value.contains("-")) {
				LocalDate.parse(value, DATE_DASH);
				return value;
			}
			// YYYYMMDD -> YYYY-MM-DD
			LocalDate date = LocalDate.parse(value, DATE_COMPACT);
			return date.format(DATE_DASH);
		} catch (Exception e) {
			return null;
		}
	}

	private String validateClosedEnum(Long dataPropertyId, String value) {
		List<OntDataPropertyEnum> enums = dataPropertyEnumMapper.selectList(
			Wrappers.<OntDataPropertyEnum>lambdaQuery()
				.eq(OntDataPropertyEnum::getDataPropertyId, dataPropertyId));
		for (OntDataPropertyEnum e : enums) {
			if (e.getEnumValue().equals(value)) {
				return null;
			}
			if (e.getCanonicalValue() != null && e.getCanonicalValue().equals(value)) {
				return null;
			}
		}
		return "值不在闭枚举范围内: " + value;
	}

	private void normalizeOpenEnum(Long dataPropertyId, OntInstanceDataValueDTO dto) {
		List<OntDataPropertyEnum> enums = dataPropertyEnumMapper.selectList(
			Wrappers.<OntDataPropertyEnum>lambdaQuery()
				.eq(OntDataPropertyEnum::getDataPropertyId, dataPropertyId));
		for (OntDataPropertyEnum e : enums) {
			if (e.getEnumValue().equals(dto.getLiteralValue()) && e.getCanonicalValue() != null) {
				dto.setLiteralValue(e.getCanonicalValue());
				return;
			}
		}
		// 未命中时允许自定义值
	}

	private String validateUnitRef(OntInstanceDataValueDTO dto, OntDataProperty prop) {
		if (dto.getUnitId() == null) {
			return "UNIT_REF属性必须指定单位";
		}
		OntUnit unit = unitMapper.selectById(dto.getUnitId());
		if (unit == null) {
			return "单位不存在";
		}
		if (!unit.getUnitSymbol().equals(dto.getLiteralSymbol())) {
			return "单位符号与字典不一致";
		}
		if (!unit.getUnitSymbol().equals(dto.getLiteralValue())) {
			return "UNIT_REF的literalValue必须等于单位符号";
		}
		if (prop.getUnitCategoryId() != null && !Objects.equals(prop.getUnitCategoryId(), unit.getCategoryId())) {
			return "单位不属于数据属性限定的单位分类";
		}
		return null;
	}

	/**
	 * 对象属性断言校验：定义域/值域继承、功能性约束。
	 */
	private String validateRelation(Long subjectInstanceId, OntInstanceObjectRelationDTO dto, Long ontologyId,
			OntEntityType subjectType, boolean isCreate) {
		OntObjectProperty prop = objectPropertyMapper.selectById(dto.getObjectPropertyId());
		if (prop == null) {
			return "对象属性不存在";
		}
		if (!Objects.equals(prop.getOntologyId(), ontologyId)) {
			return "对象属性必须与实例属于同一本体工程";
		}

		// 主体类型满足对象属性任一声明定义域或其子类
		Set<Long> subjectAncestors = collectAncestorsWithDistance(subjectType.getId()).keySet();
		List<OntObjectPropertyDomain> domains = objectPropertyDomainMapper.selectList(
			Wrappers.<OntObjectPropertyDomain>lambdaQuery()
				.eq(OntObjectPropertyDomain::getObjectPropertyId, dto.getObjectPropertyId()));
		boolean domainMatched = domains.stream().anyMatch(d -> subjectAncestors.contains(d.getEntityTypeId()));
		if (!domainMatched) {
			return "主体类型不满足对象属性定义域";
		}

		// 客体实例类型满足任一声明值域或其子类
		OntEntityInstance objectInstance = this.getById(dto.getObjectInstanceId());
		if (objectInstance == null) {
			return "客体实例不存在";
		}
		if (!Objects.equals(objectInstance.getOntologyId(), ontologyId)) {
			return "客体实例必须与主体属于同一本体工程";
		}
		Set<Long> objectAncestors = collectAncestorsWithDistance(objectInstance.getRdfTypeId()).keySet();
		List<OntObjectPropertyRange> ranges = objectPropertyRangeMapper.selectList(
			Wrappers.<OntObjectPropertyRange>lambdaQuery()
				.eq(OntObjectPropertyRange::getObjectPropertyId, dto.getObjectPropertyId()));
		boolean rangeMatched = ranges.stream().anyMatch(r -> objectAncestors.contains(r.getEntityTypeId()));
		if (!rangeMatched) {
			return "客体实例类型不满足对象属性值域";
		}

		// 显式三元组不得重复
		long dupCount = relationMapper.selectCount(Wrappers.<OntInstanceObjectRelation>lambdaQuery()
			.eq(OntInstanceObjectRelation::getSubjectInstanceId, subjectInstanceId)
			.eq(OntInstanceObjectRelation::getObjectPropertyId, dto.getObjectPropertyId())
			.eq(OntInstanceObjectRelation::getObjectKind, "INSTANCE")
			.eq(OntInstanceObjectRelation::getObjectInstanceId, dto.getObjectInstanceId()));
		if (dupCount > 0) {
			return "显式三元组已存在，不能重复";
		}

		// 功能性属性：同一主体+谓词在两种客体类型合计至多一条
		if (BUILTIN.equals(prop.getIsFunctional())) {
			long funcCount = relationMapper.selectCount(Wrappers.<OntInstanceObjectRelation>lambdaQuery()
				.eq(OntInstanceObjectRelation::getSubjectInstanceId, subjectInstanceId)
				.eq(OntInstanceObjectRelation::getObjectPropertyId, dto.getObjectPropertyId()));
			if (funcCount > 0) {
				return "功能性对象属性已有断言，不能再添加";
			}
		}
		return null;
	}

	/**
	 * 修改类型前以新类型重新校验全部数据值和出向断言定义域、入向断言值域。
	 */
	private String revalidateForTypeChange(Long instanceId, Long newTypeId, Long ontologyId) {
		Map<Long, Integer> newAncestors = collectAncestorsWithDistance(newTypeId);

		// 重新校验数据值定义域
		List<OntInstanceDataValue> values = dataValueMapper.selectList(
			Wrappers.<OntInstanceDataValue>lambdaQuery()
				.eq(OntInstanceDataValue::getInstanceId, instanceId));
		for (OntInstanceDataValue v : values) {
			OntDataProperty prop = dataPropertyMapper.selectById(v.getDataPropertyId());
			if (prop == null || !newAncestors.containsKey(prop.getDomainEntityTypeId())) {
				return "修改类型后数据属性" + (prop != null ? prop.getName() : v.getDataPropertyId())
						+ "的定义域不匹配";
			}
		}

		// 重新校验出向断言定义域
		List<OntInstanceObjectRelation> outgoing = relationMapper.selectList(
			Wrappers.<OntInstanceObjectRelation>lambdaQuery()
				.eq(OntInstanceObjectRelation::getSubjectInstanceId, instanceId));
		for (OntInstanceObjectRelation rel : outgoing) {
			List<OntObjectPropertyDomain> domains = objectPropertyDomainMapper.selectList(
				Wrappers.<OntObjectPropertyDomain>lambdaQuery()
					.eq(OntObjectPropertyDomain::getObjectPropertyId, rel.getObjectPropertyId()));
			boolean matched = domains.stream().anyMatch(d -> newAncestors.containsKey(d.getEntityTypeId()));
			if (!matched) {
				return "修改类型后出向断言定义域不匹配";
			}
		}

		// 重新校验入向断言值域
		List<OntInstanceObjectRelation> incoming = relationMapper.selectList(
			Wrappers.<OntInstanceObjectRelation>lambdaQuery()
				.eq(OntInstanceObjectRelation::getObjectInstanceId, instanceId)
				.eq(OntInstanceObjectRelation::getObjectKind, "INSTANCE"));
		for (OntInstanceObjectRelation rel : incoming) {
			OntEntityInstance subjectInstance = this.getById(rel.getSubjectInstanceId());
			if (subjectInstance == null) {
				continue;
			}
			Set<Long> subjectAncestors = collectAncestorsWithDistance(subjectInstance.getRdfTypeId()).keySet();
			List<OntObjectPropertyRange> ranges = objectPropertyRangeMapper.selectList(
				Wrappers.<OntObjectPropertyRange>lambdaQuery()
					.eq(OntObjectPropertyRange::getObjectPropertyId, rel.getObjectPropertyId()));
			boolean matched = ranges.stream().anyMatch(r -> {
				Set<Long> objectAncestors = collectAncestorsWithDistance(newTypeId).keySet();
				return objectAncestors.contains(r.getEntityTypeId());
			});
			if (!matched) {
				return "修改类型后入向断言值域不匹配";
			}
		}
		return null;
	}

	// ==================== 辅助方法 ====================

	private boolean hasSemanticChanges(OntEntityInstanceUpdateDTO request, OntEntityInstance old) {
		return request.getNamespaceId() != null && !Objects.equals(request.getNamespaceId(), old.getNamespaceId())
				|| StringUtils.hasText(request.getIriLocalName())
						&& !request.getIriLocalName().equals(old.getIriLocalName())
				|| request.getRdfTypeId() != null && !Objects.equals(request.getRdfTypeId(), old.getRdfTypeId());
	}

	private void insertDataValues(Long instanceId, List<OntInstanceDataValueDTO> dataValues) {
		int order = 1;
		for (OntInstanceDataValueDTO dto : dataValues) {
			OntInstanceDataValue value = new OntInstanceDataValue();
			value.setInstanceId(instanceId);
			value.setDataPropertyId(dto.getDataPropertyId());
			value.setLiteralValue(dto.getLiteralValue());
			value.setLiteralType(dto.getLiteralType());
			value.setUnitId(dto.getUnitId());
			value.setLiteralSymbol(dto.getLiteralSymbol());
			value.setSortOrder(dto.getSortOrder() == null ? order : dto.getSortOrder());
			dataValueMapper.insert(value);
			order++;
		}
	}

	private void insertRelations(Long instanceId, List<OntInstanceObjectRelationDTO> relations) {
		int order = 1;
		for (OntInstanceObjectRelationDTO dto : relations) {
			OntInstanceObjectRelation rel = new OntInstanceObjectRelation();
			rel.setSubjectInstanceId(instanceId);
			rel.setObjectPropertyId(dto.getObjectPropertyId());
			rel.setObjectKind("INSTANCE");
			rel.setObjectInstanceId(dto.getObjectInstanceId());
			rel.setSortOrder(dto.getSortOrder() == null ? order : dto.getSortOrder());
			relationMapper.insert(rel);
			order++;
		}
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

	/**
	 * 按值域类型收集候选实例类型ID集合（含子类）。
	 */
	private List<Long> collectRangeTypeIds(Long objectPropertyId) {
		List<OntObjectPropertyRange> ranges = objectPropertyRangeMapper.selectList(
			Wrappers.<OntObjectPropertyRange>lambdaQuery()
				.eq(OntObjectPropertyRange::getObjectPropertyId, objectPropertyId));
		if (ranges.isEmpty()) {
			return List.of();
		}
		Set<Long> result = new HashSet<>();
		for (OntObjectPropertyRange r : ranges) {
			result.add(r.getEntityTypeId());
			result.addAll(collectDescendants(r.getEntityTypeId()));
		}
		return new ArrayList<>(result);
	}

	private Set<Long> collectDescendants(Long rootId) {
		Set<Long> result = new HashSet<>();
		Deque<Long> queue = new ArrayDeque<>();
		queue.addLast(rootId);
		while (!queue.isEmpty()) {
			Long current = queue.removeFirst();
			List<Long> children = hierarchyMapper.selectList(Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
				.eq(OntEntityTypeHierarchy::getParentId, current)).stream()
				.map(OntEntityTypeHierarchy::getChildId)
				.toList();
			for (Long child : children) {
				if (!result.contains(child)) {
					result.add(child);
					queue.addLast(child);
				}
			}
		}
		return result;
	}

	private List<Long> filterTypeIdsBySubtypes(OntEntityInstanceQuery query, Long ontologyId) {
		if (query.getRdfTypeId() == null) {
			return null;
		}
		if (query.getIncludeSubtypes() == null || !query.getIncludeSubtypes()) {
			return List.of(query.getRdfTypeId());
		}
		Set<Long> typeIds = new HashSet<>();
		typeIds.add(query.getRdfTypeId());
		typeIds.addAll(collectDescendants(query.getRdfTypeId()));
		return new ArrayList<>(typeIds);
	}

	private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OntEntityInstance> buildQueryWrapper(
			OntEntityInstanceQuery query, Long ontologyId, List<Long> typeIds) {
		return Wrappers.<OntEntityInstance>lambdaQuery()
			.eq(OntEntityInstance::getOntologyId, ontologyId)
			.and(StrUtil.isNotBlank(query.getKeyword()), w -> w
				.like(OntEntityInstance::getIriLocalName, query.getKeyword())
				.or()
				.like(OntEntityInstance::getLabel, query.getKeyword()))
			.eq(query.getRdfTypeId() != null && (query.getIncludeSubtypes() == null || !query.getIncludeSubtypes()),
					OntEntityInstance::getRdfTypeId, query.getRdfTypeId())
			.in(typeIds != null && !typeIds.isEmpty(), OntEntityInstance::getRdfTypeId, typeIds)
			.eq(query.getNamespaceId() != null, OntEntityInstance::getNamespaceId, query.getNamespaceId())
			.eq(StrUtil.isNotBlank(query.getSourceType()), OntEntityInstance::getSourceType, query.getSourceType())
			.eq(StrUtil.isNotBlank(query.getIsBuiltin()), OntEntityInstance::getIsBuiltin, query.getIsBuiltin())
			.orderByAsc(OntEntityInstance::getSortOrder)
			.orderByAsc(OntEntityInstance::getId);
	}

	private List<OntEntityInstanceSummaryVO> buildSummaryList(List<OntEntityInstance> instances) {
		if (instances == null || instances.isEmpty()) {
			return List.of();
		}
		List<Long> ids = instances.stream().map(OntEntityInstance::getId).toList();

		Map<Long, String> typeLabelMap = batchLoadEntityTypeZhLabels(
			instances.stream().map(OntEntityInstance::getRdfTypeId).collect(Collectors.toSet()));
		Map<Long, OntEntityType> typeMap = batchLoadEntityTypes(
			instances.stream().map(OntEntityInstance::getRdfTypeId).collect(Collectors.toSet()));
		Map<Long, String> nsPrefixMap = batchLoadNamespacePrefixes(
			instances.stream().map(OntEntityInstance::getNamespaceId).collect(Collectors.toSet()));
		Map<Long, Integer> valueCountMap = batchLoadDataValueCounts(ids);
		Map<Long, Integer> outCountMap = batchLoadOutgoingRelationCounts(ids);
		Map<Long, Integer> inCountMap = batchLoadIncomingRelationCounts(ids);

		return instances.stream().map(inst -> {
			OntEntityInstanceSummaryVO vo = new OntEntityInstanceSummaryVO();
			vo.setId(inst.getId());
			vo.setIri(inst.getIri());
			vo.setIriLocalName(inst.getIriLocalName());
			vo.setLabel(inst.getLabel());
			vo.setRdfTypeId(inst.getRdfTypeId());
			OntEntityType type = typeMap.get(inst.getRdfTypeId());
			if (type != null) {
				vo.setRdfTypeName(type.getName());
			}
			vo.setRdfTypeLabel(typeLabelMap.get(inst.getRdfTypeId()));
			vo.setNamespaceId(inst.getNamespaceId());
			vo.setNamespacePrefix(nsPrefixMap.get(inst.getNamespaceId()));
			vo.setSourceType(inst.getSourceType());
			vo.setIsBuiltin(inst.getIsBuiltin());
			vo.setSortOrder(inst.getSortOrder());
			vo.setDataValueCount(valueCountMap.getOrDefault(inst.getId(), 0));
			vo.setOutgoingRelationCount(outCountMap.getOrDefault(inst.getId(), 0));
			vo.setIncomingRelationCount(inCountMap.getOrDefault(inst.getId(), 0));
			return vo;
		}).toList();
	}

	private OntEntityInstanceDetailVO buildDetailVO(OntEntityInstance instance) {
		OntEntityInstanceDetailVO vo = new OntEntityInstanceDetailVO();
		vo.setId(instance.getId());
		vo.setIri(instance.getIri());
		vo.setIriLocalName(instance.getIriLocalName());
		vo.setLabel(instance.getLabel());
		vo.setRdfTypeId(instance.getRdfTypeId());
		OntEntityType type = entityTypeMapper.selectById(instance.getRdfTypeId());
		if (type != null) {
			vo.setRdfTypeName(type.getName());
		}
		vo.setRdfTypeLabel(getEntityTypeZhLabel(instance.getRdfTypeId()));
		vo.setNamespaceId(instance.getNamespaceId());
		OntNamespace ns = namespaceMapper.selectById(instance.getNamespaceId());
		if (ns != null) {
			vo.setNamespacePrefix(ns.getPrefix());
			vo.setNamespaceUri(ns.getUri());
		}
		vo.setOntologyId(instance.getOntologyId());
		vo.setSourceType(instance.getSourceType());
		vo.setSourceReference(instance.getSourceReference());
		vo.setDeclarationMode(instance.getDeclarationMode());
		vo.setIsBuiltin(instance.getIsBuiltin());
		vo.setSortOrder(instance.getSortOrder());
		vo.setRemarks(instance.getRemarks());

		vo.setDataValues(getDataValues(instance.getId()));
		vo.setOutgoingRelations(getRelations(instance.getId(), "OUTGOING"));

		// 入向引用
		List<OntInstanceObjectRelation> inbound = relationMapper.selectList(
			Wrappers.<OntInstanceObjectRelation>lambdaQuery()
				.eq(OntInstanceObjectRelation::getObjectInstanceId, instance.getId())
				.eq(OntInstanceObjectRelation::getObjectKind, "INSTANCE")
				.orderByAsc(OntInstanceObjectRelation::getObjectPropertyId));
		if (!inbound.isEmpty()) {
			Set<Long> subjectIds = inbound.stream().map(OntInstanceObjectRelation::getSubjectInstanceId)
				.collect(Collectors.toSet());
			Map<Long, OntEntityInstance> subjectMap = this.listByIds(subjectIds).stream()
				.collect(Collectors.toMap(OntEntityInstance::getId, Function.identity()));
			Set<Long> propIds = inbound.stream().map(OntInstanceObjectRelation::getObjectPropertyId)
				.collect(Collectors.toSet());
			Map<Long, String> propLabelMap = batchLoadObjectPropertyZhLabels(propIds);
			Map<Long, OntObjectProperty> propMap = objectPropertyMapper.selectBatchIds(propIds).stream()
				.collect(Collectors.toMap(OntObjectProperty::getId, Function.identity()));

			vo.setInboundRelations(inbound.stream().map(rel -> {
				OntInstanceInboundRelationVO inVO = new OntInstanceInboundRelationVO();
				inVO.setId(rel.getId());
				inVO.setSubjectInstanceId(rel.getSubjectInstanceId());
				OntEntityInstance subject = subjectMap.get(rel.getSubjectInstanceId());
				if (subject != null) {
					inVO.setSubjectIri(subject.getIri());
					inVO.setSubjectLabel(subject.getLabel());
				}
				inVO.setObjectPropertyId(rel.getObjectPropertyId());
				OntObjectProperty prop = propMap.get(rel.getObjectPropertyId());
				if (prop != null) {
					inVO.setObjectPropertyName(prop.getName());
				}
				inVO.setObjectPropertyLabel(propLabelMap.get(rel.getObjectPropertyId()));
				return inVO;
			}).toList());
		}
		return vo;
	}

	private List<OntInstanceObjectRelationVO> buildRelationVOs(List<OntInstanceObjectRelation> relations) {
		Set<Long> propIds = relations.stream().map(OntInstanceObjectRelation::getObjectPropertyId)
			.collect(Collectors.toSet());
		Map<Long, OntObjectProperty> propMap = propIds.isEmpty() ? Collections.emptyMap()
				: objectPropertyMapper.selectBatchIds(propIds).stream()
					.collect(Collectors.toMap(OntObjectProperty::getId, Function.identity()));
		Map<Long, String> propLabelMap = batchLoadObjectPropertyZhLabels(propIds);

		// 收集INSTANCE客体的实例ID
		Set<Long> instanceIds = relations.stream()
			.filter(r -> "INSTANCE".equals(r.getObjectKind()))
			.map(OntInstanceObjectRelation::getObjectInstanceId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		Map<Long, OntEntityInstance> instanceMap = instanceIds.isEmpty() ? Collections.emptyMap()
				: this.listByIds(instanceIds).stream()
					.collect(Collectors.toMap(OntEntityInstance::getId, Function.identity()));

		// 收集ENTITY_TYPE客体的类型ID
		Set<Long> entityTypeIds = relations.stream()
			.filter(r -> "ENTITY_TYPE".equals(r.getObjectKind()))
			.map(OntInstanceObjectRelation::getObjectEntityTypeId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		Map<Long, OntEntityType> entityTypeMap = batchLoadEntityTypes(entityTypeIds);
		Map<Long, String> entityTypeLabelMap = batchLoadEntityTypeZhLabels(entityTypeIds);

		return relations.stream().map(rel -> {
			OntInstanceObjectRelationVO vo = new OntInstanceObjectRelationVO();
			vo.setId(rel.getId());
			vo.setObjectPropertyId(rel.getObjectPropertyId());
			OntObjectProperty prop = propMap.get(rel.getObjectPropertyId());
			if (prop != null) {
				vo.setObjectPropertyName(prop.getName());
			}
			vo.setObjectPropertyLabel(propLabelMap.get(rel.getObjectPropertyId()));
			vo.setObjectKind(rel.getObjectKind());
			vo.setAsserted(true);
			vo.setSortOrder(rel.getSortOrder());

			if ("INSTANCE".equals(rel.getObjectKind())) {
				OntEntityInstance obj = instanceMap.get(rel.getObjectInstanceId());
				if (obj != null) {
					vo.setObjectResourceId(obj.getId());
					vo.setObjectIri(obj.getIri());
					vo.setObjectLabel(obj.getLabel());
					vo.setObjectRdfTypeId(obj.getRdfTypeId());
				}
			} else {
				OntEntityType et = entityTypeMap.get(rel.getObjectEntityTypeId());
				if (et != null) {
					vo.setObjectResourceId(et.getId());
					vo.setObjectIri(et.getIri());
					vo.setObjectLabel(entityTypeLabelMap.get(et.getId()));
				}
			}
			return vo;
		}).toList();
	}

	private List<OntInstanceFormMetaVO.DataPropertyMeta> buildDataPropertyMetas(Set<Long> candidateTypeIds,
			Map<Long, Integer> distanceMap) {
		List<OntDataProperty> props = dataPropertyMapper.selectList(Wrappers.<OntDataProperty>lambdaQuery()
			.in(OntDataProperty::getDomainEntityTypeId, candidateTypeIds)
			.eq(OntDataProperty::getDelFlag, EXTENSION));
		if (props.isEmpty()) {
			return List.of();
		}
		Map<Long, String> labelMap = batchLoadDataPropertyZhLabels(
			props.stream().map(OntDataProperty::getId).collect(Collectors.toSet()));
		return props.stream().map(prop -> {
			OntInstanceFormMetaVO.DataPropertyMeta meta = new OntInstanceFormMetaVO.DataPropertyMeta();
			meta.setDataPropertyId(prop.getId());
			meta.setDataPropertyName(prop.getName());
			meta.setDataPropertyLabel(labelMap.get(prop.getId()));
			meta.setBaseType(prop.getBaseType());
			meta.setValueMode(prop.getValueMode());
			meta.setIsUnique(prop.getIsUnique());
			meta.setUnitCategoryId(prop.getUnitCategoryId());
			meta.setRegexPattern(prop.getRegexPattern());
			meta.setFormatHint(prop.getFormatHint());
			int distance = distanceMap.getOrDefault(prop.getDomainEntityTypeId(), 0);
			meta.setInherited(distance > 0);
			meta.setInheritanceDistance(distance);
			return meta;
		}).sorted(Comparator
			.comparing(OntInstanceFormMetaVO.DataPropertyMeta::getInheritanceDistance,
					Comparator.nullsLast(Integer::compareTo))
			.thenComparing(m -> m.getDataPropertyId()))
			.toList();
	}

	private List<OntInstanceFormMetaVO.ObjectPropertyMeta> buildObjectPropertyMetas(Set<Long> candidateTypeIds,
			Map<Long, Integer> distanceMap) {
		List<OntObjectPropertyDomain> domains = objectPropertyDomainMapper.selectList(
			Wrappers.<OntObjectPropertyDomain>lambdaQuery()
				.in(OntObjectPropertyDomain::getEntityTypeId, candidateTypeIds));
		if (domains.isEmpty()) {
			return List.of();
		}
		Map<Long, Integer> propDistance = new HashMap<>();
		for (OntObjectPropertyDomain d : domains) {
			int distance = distanceMap.getOrDefault(d.getEntityTypeId(), 0);
			Integer existing = propDistance.get(d.getObjectPropertyId());
			if (existing == null || distance < existing) {
				propDistance.put(d.getObjectPropertyId(), distance);
			}
		}
		List<OntObjectProperty> props = objectPropertyMapper.selectBatchIds(propDistance.keySet());
		Map<Long, String> labelMap = batchLoadObjectPropertyZhLabels(propDistance.keySet());

		return props.stream().map(prop -> {
			OntInstanceFormMetaVO.ObjectPropertyMeta meta = new OntInstanceFormMetaVO.ObjectPropertyMeta();
			meta.setObjectPropertyId(prop.getId());
			meta.setObjectPropertyName(prop.getName());
			meta.setObjectPropertyLabel(labelMap.get(prop.getId()));
			meta.setIsFunctional(prop.getIsFunctional());
			int distance = propDistance.get(prop.getId());
			meta.setInherited(distance > 0);
			meta.setInheritanceDistance(distance);
			meta.setRangeEntityTypes(buildRangeRefs(prop.getId()));
			return meta;
		}).sorted(Comparator
			.comparing(OntInstanceFormMetaVO.ObjectPropertyMeta::getInheritanceDistance,
					Comparator.nullsLast(Integer::compareTo))
			.thenComparing(m -> m.getObjectPropertyId()))
			.toList();
	}

	private List<OntEntityTypeRefVO> buildRangeRefs(Long objectPropertyId) {
		List<OntObjectPropertyRange> ranges = objectPropertyRangeMapper.selectList(
			Wrappers.<OntObjectPropertyRange>lambdaQuery()
				.eq(OntObjectPropertyRange::getObjectPropertyId, objectPropertyId)
				.orderByAsc(OntObjectPropertyRange::getSortOrder));
		if (ranges.isEmpty()) {
			return List.of();
		}
		Set<Long> typeIds = ranges.stream().map(OntObjectPropertyRange::getEntityTypeId).collect(Collectors.toSet());
		Map<Long, OntEntityType> typeMap = batchLoadEntityTypes(typeIds);
		Map<Long, String> typeLabelMap = batchLoadEntityTypeZhLabels(typeIds);
		return ranges.stream().map(r -> toRefVO(r.getEntityTypeId(), typeMap, typeLabelMap)).toList();
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

	private OntInstanceDataValueVO toDataValueVO(OntInstanceDataValue v, Map<Long, OntDataProperty> propMap,
			Map<Long, String> labelMap, Map<Long, String> unitSymbolMap) {
		OntInstanceDataValueVO vo = new OntInstanceDataValueVO();
		vo.setId(v.getId());
		vo.setDataPropertyId(v.getDataPropertyId());
		OntDataProperty prop = propMap.get(v.getDataPropertyId());
		if (prop != null) {
			vo.setDataPropertyName(prop.getName());
		}
		vo.setDataPropertyLabel(labelMap.get(v.getDataPropertyId()));
		vo.setLiteralValue(v.getLiteralValue());
		vo.setLiteralType(v.getLiteralType());
		vo.setUnitId(v.getUnitId());
		if (v.getUnitId() != null) {
			vo.setUnitSymbol(unitSymbolMap.get(v.getUnitId()));
		}
		vo.setSortOrder(v.getSortOrder());
		return vo;
	}

	// ==================== 批量加载 ====================

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

	private Map<Long, String> batchLoadEntityTypeNames(Set<Long> typeIds) {
		return typeIds.isEmpty() ? Collections.emptyMap()
				: entityTypeMapper.selectBatchIds(typeIds).stream()
					.collect(Collectors.toMap(OntEntityType::getId, OntEntityType::getName,
						(existing, replacement) -> existing));
	}

	private String getEntityTypeZhLabel(Long typeId) {
		Map<Long, String> map = batchLoadEntityTypeZhLabels(Set.of(typeId));
		return map.get(typeId);
	}

	private Map<Long, String> batchLoadNamespacePrefixes(Set<Long> nsIds) {
		return nsIds.isEmpty() ? Collections.emptyMap()
				: namespaceMapper.selectBatchIds(nsIds).stream()
					.collect(Collectors.toMap(OntNamespace::getId, OntNamespace::getPrefix,
						(existing, replacement) -> existing));
	}

	private Map<Long, String> batchLoadDataPropertyZhLabels(Set<Long> propIds) {
		return propIds.isEmpty() ? Collections.emptyMap()
				: dataPropertyLabelMapper.selectList(Wrappers.<OntDataPropertyLabel>lambdaQuery()
					.eq(OntDataPropertyLabel::getLocale, ZH)
					.in(OntDataPropertyLabel::getDataPropertyId, propIds))
					.stream()
					.collect(Collectors.toMap(OntDataPropertyLabel::getDataPropertyId, OntDataPropertyLabel::getLabel,
						(existing, replacement) -> existing));
	}

	private Map<Long, String> batchLoadObjectPropertyZhLabels(Set<Long> propIds) {
		return propIds.isEmpty() ? Collections.emptyMap()
				: objectPropertyLabelMapper.selectList(Wrappers.<OntObjectPropertyLabel>lambdaQuery()
					.eq(OntObjectPropertyLabel::getLocale, ZH)
					.in(OntObjectPropertyLabel::getObjectPropertyId, propIds))
					.stream()
					.collect(Collectors.toMap(OntObjectPropertyLabel::getObjectPropertyId,
						OntObjectPropertyLabel::getLabel, (existing, replacement) -> existing));
	}

	private Map<Long, String> batchLoadUnitSymbols(Set<Long> unitIds) {
		return unitIds.isEmpty() ? Collections.emptyMap()
				: unitMapper.selectBatchIds(unitIds).stream()
					.collect(Collectors.toMap(OntUnit::getId, OntUnit::getUnitSymbol,
						(existing, replacement) -> existing));
	}

	private Map<Long, Integer> batchLoadDataValueCounts(List<Long> instanceIds) {
		if (instanceIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<OntInstanceDataValue> values = dataValueMapper.selectList(
			Wrappers.<OntInstanceDataValue>lambdaQuery()
				.in(OntInstanceDataValue::getInstanceId, instanceIds));
		return values.stream()
			.collect(Collectors.groupingBy(OntInstanceDataValue::getInstanceId,
				Collectors.summingInt(v -> 1)));
	}

	private Map<Long, Integer> batchLoadOutgoingRelationCounts(List<Long> instanceIds) {
		if (instanceIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<OntInstanceObjectRelation> rels = relationMapper.selectList(
			Wrappers.<OntInstanceObjectRelation>lambdaQuery()
				.in(OntInstanceObjectRelation::getSubjectInstanceId, instanceIds));
		return rels.stream()
			.collect(Collectors.groupingBy(OntInstanceObjectRelation::getSubjectInstanceId,
				Collectors.summingInt(r -> 1)));
	}

	private Map<Long, Integer> batchLoadIncomingRelationCounts(List<Long> instanceIds) {
		if (instanceIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<OntInstanceObjectRelation> rels = relationMapper.selectList(
			Wrappers.<OntInstanceObjectRelation>lambdaQuery()
				.in(OntInstanceObjectRelation::getObjectInstanceId, instanceIds)
				.eq(OntInstanceObjectRelation::getObjectKind, "INSTANCE"));
		return rels.stream()
			.collect(Collectors.groupingBy(OntInstanceObjectRelation::getObjectInstanceId,
				Collectors.summingInt(r -> 1)));
	}

	/**
	 * 发布实例变更事件到 Outbox。
	 */
	private void publishInstanceChanged(OntEntityInstance instance, String operation) {
		eventPublisher.append(OntologyDomainEvent.builder()
			.eventType(OntologyEventTypes.ONTOLOGY_INSTANCE_CHANGED)
			.ontologyId(instance.getOntologyId())
			.aggregateType("ENTITY_INSTANCE")
			.aggregateId(instance.getId() != null ? instance.getId().toString() : null)
			.operation(operation)
			.build());
	}

}
