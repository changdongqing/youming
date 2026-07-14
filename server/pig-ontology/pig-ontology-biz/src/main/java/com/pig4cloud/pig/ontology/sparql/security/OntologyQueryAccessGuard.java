/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.security;

import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import com.pig4cloud.pig.ontology.sparql.policy.SparqlQueryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 本体工程访问守卫。
 * <p>
 * 一期：校验 ontologyId 存在且未删除。
 * 模块 36 落地后：增加本体工程 ACL。
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

	/**
	 * 校验当前用户对指定本体工程的访问权限。
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

		// 模块 36 接入点：在此处增加工程 ACL 校验
		// 如：if (!aclService.canAccess(currentUserId, ontologyId)) {
		//     throw new SparqlQueryException(CODE_FORBIDDEN, "无本体工程访问权限");
		// }

		log.debug("本体工程访问校验通过: ontologyId={}", ontologyId);
	}

}
