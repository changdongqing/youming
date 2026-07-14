/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.security;

import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import com.pig4cloud.pig.ontology.security.policy.DataAction;
import com.pig4cloud.pig.ontology.security.policy.DecisionEffect;
import com.pig4cloud.pig.ontology.security.policy.OntologyDataPolicyService;
import com.pig4cloud.pig.ontology.security.policy.PolicyDecision;
import com.pig4cloud.pig.ontology.security.policy.SecuredResource;
import com.pig4cloud.pig.ontology.security.policy.SecuritySubject;
import com.pig4cloud.pig.ontology.security.policy.SecuritySubjectResolver;
import com.pig4cloud.pig.ontology.sparql.policy.SparqlQueryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 本体工程访问守卫。
 * <p>
 * 模块 36 落地后：在原工程存在性校验基础上，增加本体工程 ACL 校验。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OntologyQueryAccessGuard {

	/** 稳定错误码 */
	public static final String CODE_NOT_FOUND = "ONTOLOGY_PROJECT_NOT_FOUND";

	public static final String CODE_FORBIDDEN = "ONTOLOGY_PROJECT_FORBIDDEN";

	private final OntOntologyProjectMapper ontologyProjectMapper;

	private final SecuritySubjectResolver subjectResolver;

	private final OntologyDataPolicyService policyService;

	/**
	 * 校验当前用户对指定本体工程的访问权限。
	 *
	 * @param ontologyId 本体工程 ID
	 * @throws SparqlQueryException 工程不存在或无权限时抛出
	 */
	public void checkAccess(Long ontologyId) {
		if (ontologyId == null) {
			throw new SparqlQueryException(CODE_NOT_FOUND, "本体工程ID不能为空");
		}

		OntOntologyProject project = ontologyProjectMapper.selectById(ontologyId);
		if (project == null) {
			throw new SparqlQueryException(CODE_NOT_FOUND, "本体工程不存在或已删除");
		}

		// 模块 36：工程 ACL 校验
		SecuritySubject subject = subjectResolver.resolve();
		SecuredResource resource = SecuredResource.builder()
			.resourceType("ONTOLOGY")
			.resourceId(ontologyId)
			.securityLevelCode("INTERNAL")
			.build();
		PolicyDecision decision = policyService.decide(subject, ontologyId, resource, DataAction.SPARQL);
		if (decision.getEffect() == DecisionEffect.DENY) {
			log.warn("SPARQL access denied: ontologyId={}, userId={}, reason={}",
				ontologyId, subject.getUserId(), decision.getReasonCode());
			throw new SparqlQueryException(CODE_FORBIDDEN, "无本体工程访问权限");
		}

		log.debug("本体工程访问校验通过: ontologyId={}, userId={}", ontologyId, subject.getUserId());
	}

}
