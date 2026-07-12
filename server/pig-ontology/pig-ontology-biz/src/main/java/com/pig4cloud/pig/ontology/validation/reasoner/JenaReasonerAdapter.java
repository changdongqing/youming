/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.reasoner;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.reasoner.ValidityReport;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Jena OWL Reasoner 默认实现。
 * <p>
 * 承接国标 §5.3 OWL 公理一致性检测（不相交冲突、功能属性唯一性等）。
 * 使用 Jena 内置 OWL Reasoner（ReasonerRegistry.getOWLReasoner()）。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
public class JenaReasonerAdapter implements ReasonerAdapter {

	@Override
	public String getReasonerName() {
		return "JenaOWLReasoner";
	}

	@Override
	public Set<ReasonerCapability> getReasonerCapabilities() {
		return EnumSet.of(
				ReasonerCapability.DISJOINT_CHECK,
				ReasonerCapability.FUNCTIONAL_CHECK,
				ReasonerCapability.SUBCLASS_INFERENCE);
	}

	@Override
	public ConsistencyReport checkConsistency(Model schemaModel, Model instanceModel) {
		ConsistencyReport report = new ConsistencyReport();
		List<ConsistencyReport.ConsistencyViolation> violations = new ArrayList<>();

		try {
			InfModel infModel = ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner(), schemaModel);
			infModel.add(instanceModel);

			ValidityReport validity = infModel.validate();
			report.setConsistent(validity.isValid());

			if (!validity.isValid()) {
				for (Iterator<ValidityReport.Report> it = validity.getReports(); it.hasNext();) {
					ValidityReport.Report r = it.next();
					ConsistencyReport.ConsistencyViolation v = new ConsistencyReport.ConsistencyViolation();
					v.setViolationType(r.getType());
					v.setDescription(r.getDescription());
					v.setInstanceIRI(extractInstanceIRI(r.getDescription()));
					violations.add(v);
				}
			}

			infModel.close();
		}
		catch (Exception e) {
			log.error("Jena OWL 一致性检测异常: {}", e.getMessage(), e);
			report.setConsistent(false);
			ConsistencyReport.ConsistencyViolation v = new ConsistencyReport.ConsistencyViolation();
			v.setViolationType("ERROR");
			v.setDescription("推理引擎执行异常: " + e.getMessage());
			violations.add(v);
		}

		report.setViolations(violations);
		return report;
	}

	/**
	 * 从报告描述中提取实例IRI。
	 * <p>
	 * Jena 的描述通常格式为 "Individual X is both of type A and type B which are disjoint"。
	 * 尝试提取其中的 IRI。
	 * </p>
	 */
	private String extractInstanceIRI(String description) {
		if (description == null) {
			return null;
		}
		// 尝试匹配 http:// 开头的 IRI
		int start = description.indexOf("http://");
		if (start >= 0) {
			int end = start + 7;
			while (end < description.length()
					&& !Character.isWhitespace(description.charAt(end))
					&& description.charAt(end) != ','
					&& description.charAt(end) != ')') {
				end++;
			}
			return description.substring(start, end);
		}
		return null;
	}

}
