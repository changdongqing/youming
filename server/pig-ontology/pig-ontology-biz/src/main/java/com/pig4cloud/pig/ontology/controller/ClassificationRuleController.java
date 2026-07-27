package com.pig4cloud.pig.ontology.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.entity.ClassificationRule;
import com.pig4cloud.pig.ontology.service.ClassificationRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 分类编码规则 Controller（FR-8）
 * <p>
 * 路径 /ont/classification-rule，对外 /admin/ont/classification-rule（对齐 PRD 10.2）。
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/classification-rule")
@Tag(name = "分类编码规则", description = "编码规则查询/配置（FR-8）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ClassificationRuleController {

	private final ClassificationRuleService classificationRuleService;

	@GetMapping
	@Operation(summary = "查询编码规则", description = "按 treeRoot 查（10.2）")
	@HasPermission("ont_class_tpl_view")
	public R<ClassificationRule> get(@RequestParam String treeRoot) {
		return R.ok(classificationRuleService.getByTreeRoot(treeRoot));
	}

	@SysLog("配置分类编码规则")
	@PutMapping
	@Operation(summary = "配置编码规则", description = "仅影响新节点（AC-8.1）；treeRoot 不存在则新建")
	@HasPermission("ont_class_tpl_manage")
	public R saveOrUpdate(@RequestBody ClassificationRule rule) {
		return classificationRuleService.saveOrUpdateRule(rule);
	}

}
