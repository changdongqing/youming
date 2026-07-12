/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.executor.owl;

import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntAxiomRuleTarget;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.validation.executor.AbstractValidationExecutor;
import com.pig4cloud.pig.ontology.validation.model.OntologyModelAssembler;
import com.pig4cloud.pig.ontology.validation.model.ValidationContext;
import com.pig4cloud.pig.ontology.validation.model.ValidationResult;
import com.pig4cloud.pig.ontology.validation.reasoner.JenaReasonerAdapter;
import com.pig4cloud.pig.ontology.validation.reasoner.ReasonerAdapter;
import com.pig4cloud.pig.ontology.validation.reasoner.ConsistencyReport;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体类型不相交检测执行器（OWL_CONSISTENCY 模式）。
 * <p>
 * 对应规则 970001 GB8_ENTITY_DISJOINT。
 * 构建 Schema Model（含 owl:disjointWith 声明）+ Instance Model，
 * 调用 Jena OWL Reasoner 执行一致性检测，提取不相交冲突。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
public class EntityDisjointExecutor extends AbstractValidationExecutor {

	private final OntologyModelAssembler modelAssembler;

	private final JenaReasonerAdapter reasonerAdapter;

	public EntityDisjointExecutor(OntEntityTypeMapper entityTypeMapper,
			OntEntityTypeHierarchyMapper hierarchyMapper,
			OntologyModelAssembler modelAssembler,
			JenaReasonerAdapter reasonerAdapter) {
		super(entityTypeMapper, hierarchyMapper);
		this.modelAssembler = modelAssembler;
		this.reasonerAdapter = reasonerAdapter;
	}

	@Override
	public String getExecutorCode() {
		return "ENTITY_DISJOINT";
	}

	@Override
	public String getValidationMode() {
		return "OWL_CONSISTENCY";
	}

	@Override
	public List<ValidationResult> validate(OntAxiomRule rule, List<OntAxiomRuleTarget> targets,
			ValidationContext context) {
		List<ValidationResult> results = new ArrayList<>();

		try {
			// 1. 组装 Schema Model（含类层次 + 不相交声明）
			Model schemaModel = context.getOrCreateSchemaModel();

			// 2. 组装 Instance Model（含 rdf:type 三元组）
			Model instanceModel = modelAssembler.buildInstanceModel(context.getOntologyId());

			// 3. 通过 ReasonerAdapter 执行一致性检测
			ConsistencyReport report = reasonerAdapter.checkConsistency(schemaModel, instanceModel);

			// 4. 将一致性违规转为 ValidationResult
			if (!report.isConsistent() && report.getViolations() != null) {
				for (ConsistencyReport.ConsistencyViolation v : report.getViolations()) {
					results.add(ValidationResult.builder()
						.severity(severityOf(rule))
						.focusNode(v.getInstanceIRI())
						.resultPath("owl:disjointWith")
						.ruleName(rule.getName())
						.ruleCode(rule.getRuleCode())
						.message("实体类型不相交冲突: " + v.getDescription())
						.suggestion("该实例同时属于互斥的实体类型，请检查 rdf:type 绑定")
						.build());
				}
			}
		}
		catch (Exception e) {
			log.error("OWL 不相交检测异常: ruleCode={}, error={}", rule.getRuleCode(), e.getMessage(), e);
			results.add(ValidationResult.builder()
				.severity(ValidationResult.Severity.WARNING)
				.ruleName(rule.getName())
				.ruleCode(rule.getRuleCode())
				.message("OWL 一致性检测执行异常: " + e.getMessage())
				.build());
		}

		return results;
	}

}
