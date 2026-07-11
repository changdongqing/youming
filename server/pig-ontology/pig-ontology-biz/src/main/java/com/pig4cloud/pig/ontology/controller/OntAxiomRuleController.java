/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleEnabledDTO;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleQuery;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleUpdateDTO;
import com.pig4cloud.pig.ontology.dto.OntEntityTypeRelationCreateDTO;
import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.service.OntAxiomRuleService;
import com.pig4cloud.pig.ontology.vo.OntAxiomRuleDetailVO;
import com.pig4cloud.pig.ontology.vo.OntAxiomRuleSummaryVO;
import com.pig4cloud.pig.ontology.vo.OntAxiomRuleTemplateVO;
import com.pig4cloud.pig.ontology.vo.OntEntityTypeRelationVO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公理与规则管理。
 *
 * @author youming
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ontology/axiom-rules")
@Tag(description = "ontology-axiom-rule", name = "公理与规则管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntAxiomRuleController {

	private final OntAxiomRuleService ontAxiomRuleService;

	@GetMapping
	@HasPermission("ontology_axiom_rule_view")
	public R<IPage<OntAxiomRuleSummaryVO>> page(@ParameterObject Page<OntAxiomRule> page,
			@ParameterObject OntAxiomRuleQuery query) {
		return R.ok(ontAxiomRuleService.pageSummary(page, query));
	}

	@GetMapping("/list")
	@HasPermission("ontology_axiom_rule_view")
	public R<List<OntAxiomRuleSummaryVO>> list(@ParameterObject OntAxiomRuleQuery query) {
		return R.ok(ontAxiomRuleService.listSummary(query));
	}

	@GetMapping("/templates")
	@HasPermission("ontology_axiom_rule_view")
	public R<List<OntAxiomRuleTemplateVO>> templates() {
		return R.ok(ontAxiomRuleService.listTemplates());
	}

	@GetMapping("/{id}")
	@HasPermission("ontology_axiom_rule_view")
	public R<OntAxiomRuleDetailVO> getById(@PathVariable Long id) {
		OntAxiomRuleDetailVO detail = ontAxiomRuleService.getDetail(id);
		if (detail == null) {
			return R.failed("公理规则不存在");
		}
		return R.ok(detail);
	}

	@SysLog("新增公理规则")
	@PostMapping
	@HasPermission("ontology_axiom_rule_add")
	public R<OntAxiomRule> save(@Valid @RequestBody OntAxiomRuleCreateDTO request) {
		return ontAxiomRuleService.saveAxiomRule(request);
	}

	@SysLog("修改公理规则")
	@PutMapping
	@HasPermission("ontology_axiom_rule_edit")
	public R<OntAxiomRule> update(@Valid @RequestBody OntAxiomRuleUpdateDTO request) {
		return ontAxiomRuleService.updateAxiomRule(request);
	}

	@SysLog("设置公理规则启用状态")
	@PutMapping("/{id}/enabled")
	@HasPermission("ontology_axiom_rule_edit")
	public R<OntAxiomRule> setEnabled(@PathVariable Long id, @Valid @RequestBody OntAxiomRuleEnabledDTO request) {
		return ontAxiomRuleService.setEnabled(id, request);
	}

	@SysLog("删除公理规则")
	@DeleteMapping("/{id}")
	@HasPermission("ontology_axiom_rule_del")
	public R<Boolean> remove(@PathVariable Long id) {
		return ontAxiomRuleService.removeAxiomRule(id);
	}

	// ==================== 不相交关系 ====================

	@GetMapping("/disjoint")
	@HasPermission("ontology_axiom_rule_view")
	public R<List<OntEntityTypeRelationVO>> listDisjoint() {
		return R.ok(ontAxiomRuleService.listDisjoint());
	}

	@SysLog("新增不相交关系")
	@PostMapping("/disjoint")
	@HasPermission("ontology_axiom_rule_edit")
	public R<Boolean> saveDisjoint(@Valid @RequestBody OntEntityTypeRelationCreateDTO request) {
		return ontAxiomRuleService.saveDisjoint(request);
	}

	@SysLog("删除不相交关系")
	@DeleteMapping("/disjoint/{typeAId}/{typeBId}")
	@HasPermission("ontology_axiom_rule_edit")
	public R<Boolean> removeDisjoint(@PathVariable Long typeAId, @PathVariable Long typeBId) {
		return ontAxiomRuleService.removeDisjoint(typeAId, typeBId);
	}

	// ==================== 等价关系 ====================

	@GetMapping("/equivalent")
	@HasPermission("ontology_axiom_rule_view")
	public R<List<OntEntityTypeRelationVO>> listEquivalent() {
		return R.ok(ontAxiomRuleService.listEquivalent());
	}

	@SysLog("新增等价关系")
	@PostMapping("/equivalent")
	@HasPermission("ontology_axiom_rule_edit")
	public R<Boolean> saveEquivalent(@Valid @RequestBody OntEntityTypeRelationCreateDTO request) {
		return ontAxiomRuleService.saveEquivalent(request);
	}

	@SysLog("删除等价关系")
	@DeleteMapping("/equivalent/{typeAId}/{typeBId}")
	@HasPermission("ontology_axiom_rule_edit")
	public R<Boolean> removeEquivalent(@PathVariable Long typeAId, @PathVariable Long typeBId) {
		return ontAxiomRuleService.removeEquivalent(typeAId, typeBId);
	}

}
