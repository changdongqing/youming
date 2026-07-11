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
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleEnabledDTO;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleQuery;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleTargetDTO;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleUpdateDTO;
import com.pig4cloud.pig.ontology.dto.OntEntityTypeRelationCreateDTO;
import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntAxiomRuleTarget;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeDisjoint;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeEquivalent;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.entity.OntUnitCategory;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleMapper;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleTargetMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeDisjointMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeEquivalentMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitCategoryMapper;
import com.pig4cloud.pig.ontology.service.OntAxiomRuleService;
import com.pig4cloud.pig.ontology.service.OntEntityTypeService;
import com.pig4cloud.pig.ontology.vo.OntAxiomRuleDetailVO;
import com.pig4cloud.pig.ontology.vo.OntAxiomRuleSummaryVO;
import com.pig4cloud.pig.ontology.vo.OntAxiomRuleTargetVO;
import com.pig4cloud.pig.ontology.vo.OntAxiomRuleTemplateVO;
import com.pig4cloud.pig.ontology.vo.OntEntityTypeRelationVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 公理规则服务实现。
 *
 * @author youming
 */
@Service
@AllArgsConstructor
public class OntAxiomRuleServiceImpl extends ServiceImpl<OntAxiomRuleMapper, OntAxiomRule>
		implements OntAxiomRuleService {

	private static final String BUILTIN = "1";

	private static final String ZH = "zh";

	private final OntAxiomRuleTargetMapper targetMapper;

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntEntityTypeLabelMapper entityTypeLabelMapper;

	private final OntEntityTypeHierarchyMapper hierarchyMapper;

	private final OntEntityTypeDisjointMapper disjointMapper;

	private final OntEntityTypeEquivalentMapper equivalentMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	private final OntUnitCategoryMapper unitCategoryMapper;

	// ==================== 查询 ====================

	@Override
	public IPage<OntAxiomRuleSummaryVO> pageSummary(Page<OntAxiomRule> page, OntAxiomRuleQuery query) {
		Page<OntAxiomRule> rawPage = this.page(page, buildQueryWrapper(query));
		Page<OntAxiomRuleSummaryVO> resultPage = new Page<>(rawPage.getCurrent(), rawPage.getSize(),
				rawPage.getTotal());
		resultPage.setRecords(buildSummaryList(rawPage.getRecords()));
		return resultPage;
	}

	@Override
	public List<OntAxiomRuleSummaryVO> listSummary(OntAxiomRuleQuery query) {
		return buildSummaryList(this.list(buildQueryWrapper(query)));
	}

	@Override
	public OntAxiomRuleDetailVO getDetail(Long id) {
		OntAxiomRule rule = this.getById(id);
		if (rule == null) {
			return null;
		}
		OntAxiomRuleDetailVO detail = new OntAxiomRuleDetailVO();
		detail.setAxiomRule(rule);
		detail.setTargets(buildTargetVOs(id));
		// 形式化预览直接从数据库快照读取
		detail.setGeneratedOwlPreview(rule.getOwlAxiom());
		detail.setGeneratedShaclPreview(rule.getShaclShape());
		return detail;
	}

	@Override
	public List<OntAxiomRuleTemplateVO> listTemplates() {
		return TemplateRegistry.listTemplates();
	}

	// ==================== 新增 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntAxiomRule> saveAxiomRule(OntAxiomRuleCreateDTO request) {
		Long ontologyId = request.getOntologyId() == null
				? OntEntityTypeService.CORE_ONTOLOGY_ID : request.getOntologyId();

		// 校验ruleCode和name唯一
		long codeCount = this.count(Wrappers.<OntAxiomRule>lambdaQuery()
			.eq(OntAxiomRule::getOntologyId, ontologyId)
			.eq(OntAxiomRule::getRuleCode, request.getRuleCode()));
		if (codeCount > 0) {
			return R.failed("规则代码已存在");
		}
		long nameCount = this.count(Wrappers.<OntAxiomRule>lambdaQuery()
			.eq(OntAxiomRule::getOntologyId, ontologyId)
			.eq(OntAxiomRule::getName, request.getName()));
		if (nameCount > 0) {
			return R.failed("规则名称已存在");
		}

		// 校验模板
		OntAxiomRuleTemplateVO template = TemplateRegistry.findByCode(request.getTemplateCode());
		if (template == null) {
			return R.failed("不支持的模板代码: " + request.getTemplateCode());
		}

		// 校验category/subType与模板一致
		if (!template.getCategory().equals(request.getCategory())
				|| !template.getSubType().equals(request.getSubType())) {
			return R.failed("规则的类别/子类型与模板不匹配");
		}

		// 校验目标绑定
		String targetError = validateTargets(request.getTargets(), template, ontologyId);
		if (targetError != null) {
			return R.failed(targetError);
		}

		OntAxiomRule rule = new OntAxiomRule();
		rule.setRuleCode(request.getRuleCode());
		rule.setName(request.getName());
		rule.setCategory(request.getCategory());
		rule.setSubType(request.getSubType());
		rule.setDescription(request.getDescription());
		rule.setTemplateCode(template.getTemplateCode());
		rule.setTemplateVersion(template.getTemplateVersion());
		rule.setFormalizationMode(template.getFormalizationMode());
		rule.setValidationMode(template.getValidationMode());
		rule.setExecutorCode(template.getExecutorCode());
		rule.setConfigJson(request.getConfig() != null ? request.getConfig() : "{}");
		rule.setSeverity(request.getSeverity());
		rule.setOntologyId(ontologyId);
		rule.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
		rule.setRemarks(request.getRemarks());

		if ("CUSTOM".equals(request.getTemplateCode())) {
			// 自定义草稿规则
			rule.setFormalizationMode("CUSTOM_DRAFT");
			rule.setValidationMode("NONE");
			rule.setExecutorCode(null);
			rule.setStatus("DRAFT");
			rule.setIsEnabled("0");
			rule.setOwlAxiom(request.getCustomOwlAxiom());
			rule.setShaclShape(request.getCustomShaclShape());
		} else {
			rule.setStatus("ACTIVE");
			rule.setIsEnabled("1");
		}
		rule.setSourceType("EXTENSION");
		rule.setIsBuiltin("0");
		this.save(rule);

		saveTargets(rule.getId(), request.getTargets());
		return R.ok(rule);
	}

	// ==================== 修改 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntAxiomRule> updateAxiomRule(OntAxiomRuleUpdateDTO request) {
		OntAxiomRule old = this.getById(request.getId());
		if (old == null) {
			return R.failed("公理规则不存在");
		}

		if (BUILTIN.equals(old.getIsBuiltin())) {
			// 内置规则仅允许修改sortOrder/remarks
			if (hasNonGovernanceChanges(request)) {
				return R.failed("内置公理规则仅允许修改排序和备注");
			}
			this.update(Wrappers.<OntAxiomRule>lambdaUpdate()
				.eq(OntAxiomRule::getId, old.getId())
				.set(OntAxiomRule::getSortOrder,
					request.getSortOrder() == null ? old.getSortOrder() : request.getSortOrder())
				.set(OntAxiomRule::getRemarks, request.getRemarks()));
			return R.ok(this.getById(old.getId()));
		}

		// 扩展规则可修改名称、描述、严重级别、配置、目标
		OntAxiomRuleTemplateVO template = TemplateRegistry.findByCode(old.getTemplateCode());
		if (template == null && !"CUSTOM".equals(old.getTemplateCode())) {
			return R.failed("规则关联的模板已失效");
		}

		String name = StringUtils.hasText(request.getName()) ? request.getName() : old.getName();
		if (!name.equals(old.getName())) {
			long nameCount = this.count(Wrappers.<OntAxiomRule>lambdaQuery()
				.eq(OntAxiomRule::getOntologyId, old.getOntologyId())
				.eq(OntAxiomRule::getName, name)
				.ne(OntAxiomRule::getId, old.getId()));
			if (nameCount > 0) {
				return R.failed("规则名称已存在");
			}
		}

		String severity = StringUtils.hasText(request.getSeverity()) ? request.getSeverity() : old.getSeverity();

		this.update(Wrappers.<OntAxiomRule>lambdaUpdate()
			.eq(OntAxiomRule::getId, old.getId())
			.set(OntAxiomRule::getName, name)
			.set(OntAxiomRule::getDescription, request.getDescription())
			.set(OntAxiomRule::getSeverity, severity)
			.set(OntAxiomRule::getConfigJson, request.getConfig() != null ? request.getConfig() : old.getConfigJson())
			.set(OntAxiomRule::getSortOrder,
				request.getSortOrder() == null ? old.getSortOrder() : request.getSortOrder())
			.set(OntAxiomRule::getRemarks, request.getRemarks()));

		// CUSTOM模式可更新文本
		if ("CUSTOM".equals(old.getTemplateCode())) {
			this.update(Wrappers.<OntAxiomRule>lambdaUpdate()
				.eq(OntAxiomRule::getId, old.getId())
				.set(OntAxiomRule::getOwlAxiom, request.getCustomOwlAxiom())
				.set(OntAxiomRule::getShaclShape, request.getCustomShaclShape()));
		}

		// 目标整体替换
		if (request.getTargets() != null && template != null) {
			String targetError = validateTargets(request.getTargets(), template, old.getOntologyId());
			if (targetError != null) {
				return R.failed(targetError);
			}
			replaceTargets(old.getId(), request.getTargets());
		}
		return R.ok(this.getById(old.getId()));
	}

	// ==================== 启用状态 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntAxiomRule> setEnabled(Long id, OntAxiomRuleEnabledDTO request) {
		OntAxiomRule rule = this.getById(id);
		if (rule == null) {
			return R.failed("公理规则不存在");
		}
		// 内置ACTIVE规则固定启用，BLOCKED固定停用
		if (BUILTIN.equals(rule.getIsBuiltin())) {
			return R.failed("内置规则启用状态不可修改");
		}
		if (!"ACTIVE".equals(rule.getStatus())) {
			return R.failed("非ACTIVE状态的扩展规则不可启用，当前状态: " + rule.getStatus());
		}
		this.update(Wrappers.<OntAxiomRule>lambdaUpdate()
			.eq(OntAxiomRule::getId, id)
			.set(OntAxiomRule::getIsEnabled, request.getEnabled()));
		return R.ok(this.getById(id));
	}

	// ==================== 删除 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> removeAxiomRule(Long id) {
		OntAxiomRule rule = this.getById(id);
		if (rule == null) {
			return R.failed("公理规则不存在");
		}
		if (BUILTIN.equals(rule.getIsBuiltin())) {
			return R.failed("内置公理规则不可删除");
		}
		targetMapper.delete(Wrappers.<OntAxiomRuleTarget>lambdaQuery()
			.eq(OntAxiomRuleTarget::getAxiomRuleId, id));
		return R.ok(this.removeById(id));
	}

	// ==================== 不相交关系 ====================

	@Override
	public List<OntEntityTypeRelationVO> listDisjoint() {
		List<OntEntityTypeDisjoint> disjointList = disjointMapper.selectList(null);
		if (disjointList.isEmpty()) {
			return List.of();
		}
		return buildRelationVOs(disjointList.stream()
			.map(d -> new long[] { d.getTypeA(), d.getTypeB() })
			.toList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> saveDisjoint(OntEntityTypeRelationCreateDTO request) {
		return saveRelation(request, "disjoint");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> removeDisjoint(Long typeAId, Long typeBId) {
		long minId = Math.min(typeAId, typeBId);
		long maxId = Math.max(typeAId, typeBId);
		// 核心不相交对不可删除
		if (minId == 940028L && maxId == 940034L) {
			return R.failed("核心不相交关系不可删除");
		}
		int deleted = disjointMapper.delete(Wrappers.<OntEntityTypeDisjoint>lambdaQuery()
			.eq(OntEntityTypeDisjoint::getTypeA, minId)
			.eq(OntEntityTypeDisjoint::getTypeB, maxId));
		return R.ok(deleted > 0);
	}

	// ==================== 等价关系 ====================

	@Override
	public List<OntEntityTypeRelationVO> listEquivalent() {
		List<OntEntityTypeEquivalent> equivList = equivalentMapper.selectList(null);
		if (equivList.isEmpty()) {
			return List.of();
		}
		return buildRelationVOs(equivList.stream()
			.map(e -> new long[] { e.getEntityTypeId(), e.getEquivalentId() })
			.toList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> saveEquivalent(OntEntityTypeRelationCreateDTO request) {
		return saveRelation(request, "equivalent");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> removeEquivalent(Long typeAId, Long typeBId) {
		long minId = Math.min(typeAId, typeBId);
		long maxId = Math.max(typeAId, typeBId);
		int deleted = equivalentMapper.delete(Wrappers.<OntEntityTypeEquivalent>lambdaQuery()
			.eq(OntEntityTypeEquivalent::getEntityTypeId, minId)
			.eq(OntEntityTypeEquivalent::getEquivalentId, maxId));
		return R.ok(deleted > 0);
	}

	// ==================== 辅助方法 ====================

	private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OntAxiomRule> buildQueryWrapper(
			OntAxiomRuleQuery query) {
		return Wrappers.<OntAxiomRule>lambdaQuery()
			.like(StrUtil.isNotBlank(query.getName()), OntAxiomRule::getName, query.getName())
			.like(StrUtil.isNotBlank(query.getRuleCode()), OntAxiomRule::getRuleCode, query.getRuleCode())
			.eq(query.getOntologyId() != null, OntAxiomRule::getOntologyId, query.getOntologyId())
			.eq(StrUtil.isNotBlank(query.getCategory()), OntAxiomRule::getCategory, query.getCategory())
			.eq(StrUtil.isNotBlank(query.getSubType()), OntAxiomRule::getSubType, query.getSubType())
			.eq(StrUtil.isNotBlank(query.getStatus()), OntAxiomRule::getStatus, query.getStatus())
			.eq(StrUtil.isNotBlank(query.getSeverity()), OntAxiomRule::getSeverity, query.getSeverity())
			.eq(StrUtil.isNotBlank(query.getIsBuiltin()), OntAxiomRule::getIsBuiltin, query.getIsBuiltin())
			.eq(StrUtil.isNotBlank(query.getIsEnabled()), OntAxiomRule::getIsEnabled, query.getIsEnabled())
			.orderByAsc(OntAxiomRule::getSortOrder)
			.orderByAsc(OntAxiomRule::getId);
	}

	private List<OntAxiomRuleSummaryVO> buildSummaryList(List<OntAxiomRule> rules) {
		if (rules == null || rules.isEmpty()) {
			return List.of();
		}
		List<Long> ruleIds = rules.stream().map(OntAxiomRule::getId).toList();
		// 批量查询目标数量
		List<OntAxiomRuleTarget> targets = targetMapper.selectList(
			Wrappers.<OntAxiomRuleTarget>lambdaQuery()
				.in(OntAxiomRuleTarget::getAxiomRuleId, ruleIds));
		Map<Long, Long> countMap = targets.stream()
			.collect(Collectors.groupingBy(OntAxiomRuleTarget::getAxiomRuleId, Collectors.counting()));

		return rules.stream().map(rule -> {
			OntAxiomRuleSummaryVO vo = new OntAxiomRuleSummaryVO();
			vo.setAxiomRule(rule);
			vo.setTargetCount(countMap.getOrDefault(rule.getId(), 0L).intValue());
			vo.setValidationModeLabel(getValidationModeLabel(rule.getValidationMode()));
			vo.setStatusLabel(rule.getStatus());
			return vo;
		}).toList();
	}

	private List<OntAxiomRuleTargetVO> buildTargetVOs(Long ruleId) {
		List<OntAxiomRuleTarget> targets = targetMapper.selectList(
			Wrappers.<OntAxiomRuleTarget>lambdaQuery()
				.eq(OntAxiomRuleTarget::getAxiomRuleId, ruleId)
				.orderByAsc(OntAxiomRuleTarget::getBindingRole)
				.orderByAsc(OntAxiomRuleTarget::getBindingOrder));
		if (targets.isEmpty()) {
			return List.of();
		}
		// 批量收集各类型ID
		Set<Long> entityTypeIds = new HashSet<>();
		Set<Long> dataPropertyIds = new HashSet<>();
		Set<Long> objectPropertyIds = new HashSet<>();
		Set<Long> unitCategoryIds = new HashSet<>();
		for (OntAxiomRuleTarget t : targets) {
			if (t.getEntityTypeId() != null) entityTypeIds.add(t.getEntityTypeId());
			if (t.getDataPropertyId() != null) dataPropertyIds.add(t.getDataPropertyId());
			if (t.getObjectPropertyId() != null) objectPropertyIds.add(t.getObjectPropertyId());
			if (t.getUnitCategoryId() != null) unitCategoryIds.add(t.getUnitCategoryId());
		}
		Map<Long, OntEntityType> entityTypeMap = batchLoadEntityTypes(entityTypeIds);
		Map<Long, OntDataProperty> dataPropertyMap = batchLoadDataProperties(dataPropertyIds);
		Map<Long, OntObjectProperty> objectPropertyMap = batchLoadObjectProperties(objectPropertyIds);
		Map<Long, OntUnitCategory> unitCategoryMap = batchLoadUnitCategories(unitCategoryIds);

		return targets.stream().map(t -> {
			OntAxiomRuleTargetVO vo = new OntAxiomRuleTargetVO();
			vo.setId(t.getId());
			vo.setBindingRole(t.getBindingRole());
			vo.setBindingOrder(t.getBindingOrder());
			vo.setTargetType(t.getTargetType());
			switch (t.getTargetType()) {
				case "ENTITY_TYPE" -> {
					vo.setTargetId(t.getEntityTypeId());
					OntEntityType et = entityTypeMap.get(t.getEntityTypeId());
					if (et != null) {
						vo.setName(et.getName());
						vo.setIri(et.getIri());
					}
				}
				case "DATA_PROPERTY" -> {
					vo.setTargetId(t.getDataPropertyId());
					OntDataProperty dp = dataPropertyMap.get(t.getDataPropertyId());
					if (dp != null) vo.setName(dp.getName());
				}
				case "OBJECT_PROPERTY" -> {
					vo.setTargetId(t.getObjectPropertyId());
					OntObjectProperty op = objectPropertyMap.get(t.getObjectPropertyId());
					if (op != null) vo.setName(op.getName());
				}
				case "UNIT_CATEGORY" -> {
					vo.setTargetId(t.getUnitCategoryId());
					OntUnitCategory uc = unitCategoryMap.get(t.getUnitCategoryId());
					if (uc != null) vo.setName(uc.getCategoryName());
				}
			}
			return vo;
		}).toList();
	}

	private String validateTargets(List<OntAxiomRuleTargetDTO> targets, OntAxiomRuleTemplateVO template,
			Long ontologyId) {
		if (targets == null || targets.isEmpty()) {
			return "规则目标不能为空";
		}
		// 检查角色与模板匹配
		Map<String, Long> roleCounts = new HashMap<>();
		for (OntAxiomRuleTargetDTO t : targets) {
			roleCounts.merge(t.getBindingRole(), 1L, Long::sum);
			// 校验角色在模板中存在
			boolean roleFound = template.getBindingRoles().stream()
				.anyMatch(r -> r.getRole().equals(t.getBindingRole()));
			if (!roleFound) {
				return "绑定角色" + t.getBindingRole() + "不在模板" + template.getTemplateCode() + "的支持列表中";
			}
			// 校验目标类型与角色要求一致
			OntAxiomRuleTemplateVO.BindingRoleVO roleDef = template.getBindingRoles().stream()
				.filter(r -> r.getRole().equals(t.getBindingRole())).findFirst().orElse(null);
			if (roleDef != null && !roleDef.getTargetType().equals(t.getTargetType())) {
				return "角色" + t.getBindingRole() + "的目标类型必须为" + roleDef.getTargetType();
			}
			// 校验目标存在性和同工程
			String existenceError = validateTargetExistence(t, ontologyId);
			if (existenceError != null) {
				return existenceError;
			}
		}
		// 校验角色基数
		for (OntAxiomRuleTemplateVO.BindingRoleVO roleDef : template.getBindingRoles()) {
			long count = roleCounts.getOrDefault(roleDef.getRole(), 0L);
			if (count < roleDef.getMinCount()) {
				return "角色" + roleDef.getRole() + "至少需要" + roleDef.getMinCount() + "个目标";
			}
			if (roleDef.getMaxCount() != null && count > roleDef.getMaxCount()) {
				return "角色" + roleDef.getRole() + "最多" + roleDef.getMaxCount() + "个目标";
			}
		}
		return null;
	}

	private String validateTargetExistence(OntAxiomRuleTargetDTO target, Long ontologyId) {
		switch (target.getTargetType()) {
			case "ENTITY_TYPE" -> {
				OntEntityType et = entityTypeMapper.selectById(target.getTargetId());
				if (et == null) return "实体类型不存在: " + target.getTargetId();
				if (!Objects.equals(et.getOntologyId(), ontologyId))
					return "实体类型必须与规则属于同一本体工程";
			}
			case "DATA_PROPERTY" -> {
				OntDataProperty dp = dataPropertyMapper.selectById(target.getTargetId());
				if (dp == null) return "数据属性不存在: " + target.getTargetId();
				if (!Objects.equals(dp.getOntologyId(), ontologyId))
					return "数据属性必须与规则属于同一本体工程";
			}
			case "OBJECT_PROPERTY" -> {
				OntObjectProperty op = objectPropertyMapper.selectById(target.getTargetId());
				if (op == null) return "对象属性不存在: " + target.getTargetId();
				if (!Objects.equals(op.getOntologyId(), ontologyId))
					return "对象属性必须与规则属于同一本体工程";
			}
			case "UNIT_CATEGORY" -> {
				OntUnitCategory uc = unitCategoryMapper.selectById(target.getTargetId());
				if (uc == null) return "单位分类不存在: " + target.getTargetId();
			}
		}
		return null;
	}

	private void saveTargets(Long ruleId, List<OntAxiomRuleTargetDTO> targets) {
		for (OntAxiomRuleTargetDTO dto : targets) {
			OntAxiomRuleTarget target = new OntAxiomRuleTarget();
			target.setAxiomRuleId(ruleId);
			target.setBindingRole(dto.getBindingRole());
			target.setBindingOrder(dto.getBindingOrder() == null ? 0 : dto.getBindingOrder());
			target.setTargetType(dto.getTargetType());
			switch (dto.getTargetType()) {
				case "ENTITY_TYPE" -> target.setEntityTypeId(dto.getTargetId());
				case "DATA_PROPERTY" -> target.setDataPropertyId(dto.getTargetId());
				case "OBJECT_PROPERTY" -> target.setObjectPropertyId(dto.getTargetId());
				case "UNIT_CATEGORY" -> target.setUnitCategoryId(dto.getTargetId());
			}
			targetMapper.insert(target);
		}
	}

	private void replaceTargets(Long ruleId, List<OntAxiomRuleTargetDTO> targets) {
		targetMapper.delete(Wrappers.<OntAxiomRuleTarget>lambdaQuery()
			.eq(OntAxiomRuleTarget::getAxiomRuleId, ruleId));
		saveTargets(ruleId, targets);
	}

	private boolean hasNonGovernanceChanges(OntAxiomRuleUpdateDTO request) {
		return StringUtils.hasText(request.getName())
				|| request.getDescription() != null
				|| StringUtils.hasText(request.getSeverity())
				|| request.getConfig() != null
				|| request.getTargets() != null
				|| request.getCustomOwlAxiom() != null
				|| request.getCustomShaclShape() != null;
	}

	private R<Boolean> saveRelation(OntEntityTypeRelationCreateDTO request, String relationType) {
		long idA = request.getEntityTypeAId();
		long idB = request.getEntityTypeBId();
		if (idA == idB) {
			return R.failed("不能声明自身与自身的关系");
		}
		long minId = Math.min(idA, idB);
		long maxId = Math.max(idA, idB);

		// 校验两端存在且同工程
		OntEntityType typeA = entityTypeMapper.selectById(minId);
		OntEntityType typeB = entityTypeMapper.selectById(maxId);
		if (typeA == null || typeB == null) {
			return R.failed("实体类型不存在");
		}
		if (!Objects.equals(typeA.getOntologyId(), typeB.getOntologyId())) {
			return R.failed("两端实体类型必须属于同一本体工程");
		}

		// 两个内置实体类型之间不得通过普通接口新增关系
		if (BUILTIN.equals(typeA.getIsBuiltin()) && BUILTIN.equals(typeB.getIsBuiltin())) {
			return R.failed("两个核心实体类型之间的关系只能由核心迁移建立");
		}

		// 检查继承可达性（祖先/后代不能声明不相交或等价）
		if (isAncestorDescendant(minId, maxId)) {
			return R.failed("存在继承关系的两个类型不能声明" + ("disjoint".equals(relationType) ? "不相交" : "等价") + "关系");
		}

		// 检查不相交与等价互斥
		if ("disjoint".equals(relationType)) {
			long equivCount = equivalentMapper.selectCount(Wrappers.<OntEntityTypeEquivalent>lambdaQuery()
				.eq(OntEntityTypeEquivalent::getEntityTypeId, minId)
				.eq(OntEntityTypeEquivalent::getEquivalentId, maxId));
			if (equivCount > 0) {
				return R.failed("两个类型已存在等价关系，不能同时声明不相交");
			}
			// 检查重复
			long count = disjointMapper.selectCount(Wrappers.<OntEntityTypeDisjoint>lambdaQuery()
				.eq(OntEntityTypeDisjoint::getTypeA, minId)
				.eq(OntEntityTypeDisjoint::getTypeB, maxId));
			if (count > 0) {
				return R.failed("不相交关系已存在");
			}
			OntEntityTypeDisjoint disjoint = new OntEntityTypeDisjoint();
			disjoint.setTypeA(minId);
			disjoint.setTypeB(maxId);
			disjointMapper.insert(disjoint);
		} else {
			long disjointCount = disjointMapper.selectCount(Wrappers.<OntEntityTypeDisjoint>lambdaQuery()
				.eq(OntEntityTypeDisjoint::getTypeA, minId)
				.eq(OntEntityTypeDisjoint::getTypeB, maxId));
			if (disjointCount > 0) {
				return R.failed("两个类型已存在不相交关系，不能同时声明等价");
			}
			long count = equivalentMapper.selectCount(Wrappers.<OntEntityTypeEquivalent>lambdaQuery()
				.eq(OntEntityTypeEquivalent::getEntityTypeId, minId)
				.eq(OntEntityTypeEquivalent::getEquivalentId, maxId));
			if (count > 0) {
				return R.failed("等价关系已存在");
			}
			OntEntityTypeEquivalent equiv = new OntEntityTypeEquivalent();
			equiv.setEntityTypeId(minId);
			equiv.setEquivalentId(maxId);
			equivalentMapper.insert(equiv);
		}
		return R.ok(true);
	}

	/**
	 * 检查两个类型是否存在祖先/后代关系。
	 */
	private boolean isAncestorDescendant(long idA, long idB) {
		// 检查A是否是B的祖先
		Set<Long> ancestorsOfB = collectAncestors(idB);
		if (ancestorsOfB.contains(idA)) return true;
		// 检查B是否是A的祖先
		Set<Long> ancestorsOfA = collectAncestors(idA);
		return ancestorsOfA.contains(idB);
	}

	private Set<Long> collectAncestors(Long typeId) {
		Set<Long> result = new HashSet<>();
		List<Long> frontier = new ArrayList<>(List.of(typeId));
		while (!frontier.isEmpty()) {
			List<Long> nextFrontier = new ArrayList<>();
			for (Long current : frontier) {
				List<Long> parents = hierarchyMapper.selectList(
					Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
						.eq(OntEntityTypeHierarchy::getChildId, current))
					.stream().map(OntEntityTypeHierarchy::getParentId).toList();
				for (Long parent : parents) {
					if (result.add(parent)) {
						nextFrontier.add(parent);
					}
				}
			}
			frontier = nextFrontier;
		}
		return result;
	}

	private List<OntEntityTypeRelationVO> buildRelationVOs(List<long[]> pairs) {
		Set<Long> allTypeIds = new HashSet<>();
		for (long[] pair : pairs) {
			allTypeIds.add(pair[0]);
			allTypeIds.add(pair[1]);
		}
		if (allTypeIds.isEmpty()) return List.of();
		Map<Long, OntEntityType> typeMap = batchLoadEntityTypes(allTypeIds);
		Map<Long, String> labelMap = entityTypeLabelMapper.selectList(
			Wrappers.<com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel>lambdaQuery()
				.eq(com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel::getLocale, ZH)
				.in(com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel::getEntityTypeId, allTypeIds))
			.stream()
			.collect(Collectors.toMap(
				com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel::getEntityTypeId,
				com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel::getLabel, (a, b) -> a));
		return pairs.stream().map(pair -> {
			OntEntityTypeRelationVO vo = new OntEntityTypeRelationVO();
			vo.setTypeAId(pair[0]);
			vo.setTypeBId(pair[1]);
			OntEntityType a = typeMap.get(pair[0]);
			OntEntityType b = typeMap.get(pair[1]);
			if (a != null) vo.setTypeAName(a.getName());
			if (b != null) vo.setTypeBName(b.getName());
			vo.setTypeALabel(labelMap.get(pair[0]));
			vo.setTypeBLabel(labelMap.get(pair[1]));
			return vo;
		}).toList();
	}

	private Map<Long, OntEntityType> batchLoadEntityTypes(Set<Long> ids) {
		return ids.isEmpty() ? Collections.emptyMap()
				: entityTypeMapper.selectBatchIds(ids).stream()
					.collect(Collectors.toMap(OntEntityType::getId, Function.identity()));
	}

	private Map<Long, OntDataProperty> batchLoadDataProperties(Set<Long> ids) {
		return ids.isEmpty() ? Collections.emptyMap()
				: dataPropertyMapper.selectBatchIds(ids).stream()
					.collect(Collectors.toMap(OntDataProperty::getId, Function.identity()));
	}

	private Map<Long, OntObjectProperty> batchLoadObjectProperties(Set<Long> ids) {
		return ids.isEmpty() ? Collections.emptyMap()
				: objectPropertyMapper.selectBatchIds(ids).stream()
					.collect(Collectors.toMap(OntObjectProperty::getId, Function.identity()));
	}

	private Map<Long, OntUnitCategory> batchLoadUnitCategories(Set<Long> ids) {
		return ids.isEmpty() ? Collections.emptyMap()
				: unitCategoryMapper.selectBatchIds(ids).stream()
					.collect(Collectors.toMap(OntUnitCategory::getId, Function.identity()));
	}

	private String getValidationModeLabel(String validationMode) {
		if (validationMode == null) return "";
		return switch (validationMode) {
			case "OWL_CONSISTENCY" -> "OWL一致性";
			case "SHACL_CORE" -> "SHACL Core";
			case "SHACL_SPARQL" -> "SHACL-SPARQL";
			case "APPLICATION" -> "应用校验";
			case "COMPOSITE" -> "组合校验";
			case "NONE" -> "无";
			default -> validationMode;
		};
	}

	// ==================== 模板注册表 ====================

	/**
	 * 集中式模板注册表，定义可用模板和绑定角色矩阵。
	 */
	private static class TemplateRegistry {

		private static final List<OntAxiomRuleTemplateVO> TEMPLATES = buildTemplates();

		static List<OntAxiomRuleTemplateVO> listTemplates() {
			return TEMPLATES;
		}

		static OntAxiomRuleTemplateVO findByCode(String code) {
			return TEMPLATES.stream().filter(t -> t.getTemplateCode().equals(code)).findFirst().orElse(null);
		}

		private static List<OntAxiomRuleTemplateVO> buildTemplates() {
			List<OntAxiomRuleTemplateVO> list = new ArrayList<>();
			list.add(buildTemplate("ENTITY_DISJOINT", "ENTITY_TYPE", "DISJOINT",
					"SCHEMA_DERIVED", "OWL_CONSISTENCY", "ENTITY_DISJOINT",
					role("TYPE_A", "ENTITY_TYPE", 1, 1, "不相交类型A"),
					role("TYPE_B", "ENTITY_TYPE", 1, 1, "不相交类型B")));
			list.add(buildTemplate("GLOBAL_UNIQUE_VALUE", "PROPERTY", "UNIQUENESS",
					"GENERATED", "SHACL_SPARQL", "GLOBAL_UNIQUE_VALUE",
					role("TARGET_CLASS", "ENTITY_TYPE", 1, 1, "目标类"),
					role("KEY_PROPERTY", "DATA_PROPERTY", 1, 1, "唯一属性")));
			list.add(buildTemplate("DATE_ORDER", "PROPERTY", "DATE_VALIDITY",
					"GENERATED", "SHACL_SPARQL", "DATE_ORDER",
					role("TARGET_CLASS", "ENTITY_TYPE", 1, 1, "目标类"),
					role("EARLIER_PROPERTY", "DATA_PROPERTY", 1, 1, "较早日期属性"),
					role("LATER_PROPERTY", "DATA_PROPERTY", 1, 1, "较晚日期属性")));
			list.add(buildTemplate("ENUM_MEMBERSHIP", "PROPERTY", "ENUM_VALUE",
					"SCHEMA_DERIVED", "SHACL_CORE", "ENUM_MEMBERSHIP",
					role("TARGET_CLASS", "ENTITY_TYPE", 1, 1, "目标类"),
					role("ENUM_PROPERTY", "DATA_PROPERTY", 1, 1, "枚举属性")));
			list.add(buildTemplate("UNIT_DIMENSION_CONSISTENCY", "PROPERTY", "UNIT_CONSISTENCY",
					"GENERATED", "APPLICATION", "UNIT_DIMENSION_CONSISTENCY",
					role("TARGET_CLASS", "ENTITY_TYPE", 1, 1, "目标类"),
					role("MAX", "DATA_PROPERTY", 0, 1, "最大值属性"),
					role("MIN", "DATA_PROPERTY", 0, 1, "最小值属性"),
					role("RANGE", "DATA_PROPERTY", 1, 1, "范围属性"),
					role("UNIT", "DATA_PROPERTY", 1, 1, "单位属性")));
			list.add(buildTemplate("FUNCTIONAL_OBJECT_PROPERTY", "RELATION", "FUNCTIONAL",
					"SCHEMA_DERIVED", "COMPOSITE", "FUNCTIONAL_OBJECT_PROPERTY",
					role("TARGET_CLASS", "ENTITY_TYPE", 1, 1, "目标类"),
					role("RELATION_PROPERTY", "OBJECT_PROPERTY", 1, 1, "功能性关系属性")));
			list.add(buildTemplate("VERSION_REPLACEMENT", "RELATION", "VERSION_REPLACEMENT",
					"GENERATED", "SHACL_SPARQL", null,
					role("TARGET_CLASS", "ENTITY_TYPE", 1, 1, "目标类"),
					role("STATUS", "DATA_PROPERTY", 0, 1, "状态属性"),
					role("REPLACES", "OBJECT_PROPERTY", 0, 1, "代替关系"),
					role("REPLACED_BY", "OBJECT_PROPERTY", 0, 1, "被代替关系")));
			list.add(buildTemplate("OBJECT_RELATION_RANGE", "RELATION", "HIERARCHY_CONTAINMENT",
					"SCHEMA_DERIVED", "APPLICATION", "OBJECT_RELATION_RANGE",
					role("PARENT_CLASS", "ENTITY_TYPE", 1, 1, "父类"),
					role("CHILD_CLASS", "ENTITY_TYPE", 1, 1, "子类"),
					role("RELATION_PROPERTY", "OBJECT_PROPERTY", 1, 1, "关系属性")));
			list.add(buildTemplate("MAX_RELATION_COUNT", "RELATION", "STRUCTURAL_LIMIT",
					"GENERATED", "SHACL_CORE", "MAX_RELATION_COUNT",
					role("TARGET_CLASS", "ENTITY_TYPE", 1, 1, "目标类"),
					role("RELATION_PROPERTY", "OBJECT_PROPERTY", 1, 1, "关系属性")));
			list.add(buildTemplate("CUSTOM", "PROPERTY", "CUSTOM",
					"CUSTOM_DRAFT", "NONE", null,
					role("CUSTOM_TARGET", "ENTITY_TYPE", 0, 5, "自定义目标")));
			return list;
		}

		private static OntAxiomRuleTemplateVO.BindingRoleVO role(String role, String targetType,
				int min, Integer max, String desc) {
			OntAxiomRuleTemplateVO.BindingRoleVO r = new OntAxiomRuleTemplateVO.BindingRoleVO();
			r.setRole(role);
			r.setTargetType(targetType);
			r.setMinCount(min);
			r.setMaxCount(max);
			r.setDescription(desc);
			return r;
		}

		private static OntAxiomRuleTemplateVO buildTemplate(String code, String category, String subType,
				String formalizationMode, String validationMode, String executorCode,
				OntAxiomRuleTemplateVO.BindingRoleVO... roles) {
			OntAxiomRuleTemplateVO t = new OntAxiomRuleTemplateVO();
			t.setTemplateCode(code);
			t.setTemplateVersion(1);
			t.setCategory(category);
			t.setSubType(subType);
			t.setFormalizationMode(formalizationMode);
			t.setValidationMode(validationMode);
			t.setExecutorCode(executorCode);
			t.setBindingRoles(List.of(roles));
			return t;
		}
	}

}
