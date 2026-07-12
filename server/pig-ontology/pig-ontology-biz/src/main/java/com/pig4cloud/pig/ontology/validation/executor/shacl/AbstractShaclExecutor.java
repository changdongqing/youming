/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.executor.shacl;

import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntAxiomRuleTarget;
import com.pig4cloud.pig.ontology.validation.executor.AbstractValidationExecutor;
import com.pig4cloud.pig.ontology.validation.model.ShaclModelBuilder;
import com.pig4cloud.pig.ontology.validation.model.ValidationContext;
import com.pig4cloud.pig.ontology.validation.model.ValidationResult;
import com.pig4cloud.pig.ontology.validation.model.DataGraphBuilder;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * SHACL 执行器公共基类。
 * <p>
 * 提供从规则配置构建 SHACL Shapes 图、从实例数据构建数据图、
 * 调用 Jena SHACL 验证器、解析报告为 ValidationResult 的通用逻辑。
 * </p>
 *
 * @author youming
 */
@Slf4j
public abstract class AbstractShaclExecutor extends AbstractValidationExecutor {

	protected final ShaclModelBuilder shaclBuilder;

	protected final DataGraphBuilder dataGraphBuilder;

	protected AbstractShaclExecutor(OntEntityTypeMapper entityTypeMapper,
			OntEntityTypeHierarchyMapper hierarchyMapper,
			ShaclModelBuilder shaclBuilder,
			DataGraphBuilder dataGraphBuilder) {
		super(entityTypeMapper, hierarchyMapper);
		this.shaclBuilder = shaclBuilder;
		this.dataGraphBuilder = dataGraphBuilder;
	}

	@Override
	public List<ValidationResult> validate(OntAxiomRule rule, List<OntAxiomRuleTarget> targets,
			ValidationContext context) {
		// 1. 构建 SHACL Shapes 图
		Model shapesModel = shaclBuilder.buildShapes(rule);
		if (shapesModel.isEmpty()) {
			log.warn("SHACL Shapes 为空，跳过: ruleCode={}", rule.getRuleCode());
			return new ArrayList<>();
		}

		// 2. 构建数据图（只包含规则目标类型的实例三元组）
		Long targetClassId = resolveTargetClassId(rule, targets);
		Model dataModel = dataGraphBuilder.buildForTargetClass(context.getOntologyId(), targetClassId);

		// 3. 调用 Jena SHACL 验证器
		try {
			Shapes shapes = Shapes.parse(shapesModel);
			ValidationReport shaclReport = ShaclValidator.get().validate(shapes, dataModel.getGraph());
			return parseShaclReport(shaclReport, rule);
		}
		catch (Exception e) {
			log.error("SHACL 校验异常: ruleCode={}, error={}", rule.getRuleCode(), e.getMessage(), e);
			List<ValidationResult> results = new ArrayList<>();
			results.add(ValidationResult.builder()
				.severity(ValidationResult.Severity.WARNING)
				.ruleName(rule.getName())
				.ruleCode(rule.getRuleCode())
				.message("SHACL 校验执行异常: " + e.getMessage())
				.build());
			return results;
		}
	}

	/**
	 * 从规则目标绑定解析目标实体类型ID。
	 * @param rule 规则
	 * @param targets 目标绑定
	 * @return 目标实体类型ID，可能为 null
	 */
	protected Long resolveTargetClassId(OntAxiomRule rule, List<OntAxiomRuleTarget> targets) {
		// 尝试常见的角色名
		String[] classRoles = { "TARGET_CLASS", "STANDARD_CLASS", "CLAUSE_CLASS", "PARENT_CLASS" };
		for (String role : classRoles) {
			OntAxiomRuleTarget target = findTarget(targets, role);
			if (target != null && target.getEntityTypeId() != null) {
				return target.getEntityTypeId();
			}
		}
		return null;
	}

	/**
	 * 解析 SHACL 报告为 ValidationResult 列表。
	 * @param shaclReport Jena SHACL 验证报告
	 * @param rule 规则
	 * @return 校验结果列表
	 */
	protected List<ValidationResult> parseShaclReport(ValidationReport shaclReport, OntAxiomRule rule) {
		List<ValidationResult> results = new ArrayList<>();
		if (shaclReport.conforms()) {
			return results;
		}

		for (ReportEntry entry : shaclReport.getEntries()) {
			ValidationResult.Severity severity = mapSeverity(entry.severity(), rule);
			String focusNode = entry.focusNode() != null ? entry.focusNode().toString() : null;
			String resultPath = entry.resultPath() != null ? entry.resultPath().toString() : null;
			String message = entry.message() != null ? entry.message().toString() : "SHACL 约束违规";

			results.add(ValidationResult.builder()
				.severity(severity)
				.focusNode(focusNode)
				.resultPath(resultPath)
				.ruleName(rule.getName())
				.ruleCode(rule.getRuleCode())
				.message(message)
				.suggestion("请根据 SHACL 约束修正该实例的属性值")
				.build());
		}

		return results;
	}

	/**
	 * 将 SHACL 严重程度映射到平台 Severity。
	 */
	private ValidationResult.Severity mapSeverity(org.apache.jena.shacl.validation.Severity shaclSeverity,
			OntAxiomRule rule) {
		// 优先使用规则配置的 severity
		return severityOf(rule);
	}

}
