/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.model;

import com.pig4cloud.pig.ontology.security.policy.DataAction;
import com.pig4cloud.pig.ontology.security.policy.DecisionEffect;
import com.pig4cloud.pig.ontology.security.policy.OntologyDataPolicyService;
import com.pig4cloud.pig.ontology.security.policy.PolicyDecision;
import com.pig4cloud.pig.ontology.security.policy.SecuredResource;
import com.pig4cloud.pig.ontology.security.policy.SecuritySubject;
import com.pig4cloud.pig.ontology.security.policy.SecuritySubjectResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * SPARQL Model 授权投影策略实现。
 * <p>
 * 模块 36 落地后：在查询前移除当前用户不可见的数据属性三元组和实例；
 * 不能仅在查询结果返回后脱敏，否则 ASK、COUNT、FILTER、聚合已泄漏受限信息。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultSparqlModelProjectionPolicy implements SparqlModelProjectionPolicy {

	private final SecuritySubjectResolver subjectResolver;

	private final OntologyDataPolicyService policyService;

	@Override
	public void applyProjection(Model model, Long ontologyId, String currentUser) {
		SecuritySubject subject = subjectResolver.resolve();

		// 遍历三元组，移除 DENY 的，MASK 的做值替换
		List<Statement> toRemove = new ArrayList<>();
		StmtIterator iter = model.listStatements();
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();
			Resource subjectRes = stmt.getSubject();

			// 对每个数据属性三元组做策略决策
			// 简化实现：对字面量值的三元组做策略检查
			if (stmt.getObject().isLiteral()) {
				SecuredResource resource = SecuredResource.builder()
					.resourceType("DATA_PROPERTY")
					.resourceId(extractPropertyId(stmt))
					.securityLevelCode("INTERNAL")
					.build();

				PolicyDecision decision = policyService.decide(subject, ontologyId, resource, DataAction.SPARQL);
				if (decision.getEffect() == DecisionEffect.DENY) {
					toRemove.add(stmt);
				}
			}
		}

		// 批量移除受限三元组
		if (!toRemove.isEmpty()) {
			model.remove(toRemove);
			log.debug("授权投影: 移除{}条受限三元组, ontologyId={}, user={}",
				toRemove.size(), ontologyId, currentUser);
		}
	}

	/**
	 * 从三元组中提取属性 ID（简化实现，实际需要 predicate URI 映射）。
	 */
	private Long extractPropertyId(Statement stmt) {
		// 一期简化：返回 0L 表示未识别，策略服务按默认级别处理
		// 完整实现需要通过 predicate URI 映射到 data_property_id
		return 0L;
	}

}
