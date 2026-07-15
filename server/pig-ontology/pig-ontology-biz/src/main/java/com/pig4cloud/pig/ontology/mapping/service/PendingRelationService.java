/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.ontology.mapping.entity.OntPendingRelation;
import com.pig4cloud.pig.ontology.mapping.vo.PendingRelationVO;

/**
 * 待解析关系服务接口（18-05 §13）。
 * <p>
 * 提供查询和手动管理（人工重试、管理员忽略），不实现后台自动调度。
 *
 * @author youming
 */
public interface PendingRelationService extends IService<OntPendingRelation> {

	/**
	 * 分页查询待解析关系（脱敏）。
	 */
	Page<PendingRelationVO> page(Page<OntPendingRelation> page, Long mappingProjectId,
			String pendingStatus, String relationMappingCode);

	/**
	 * 人工标记重试（重置 nextRetryAt、清错误信息）。
	 */
	PendingRelationVO retry(Long id);

	/**
	 * 管理员忽略（pendingStatus → IGNORED）。
	 */
	PendingRelationVO ignore(Long id);

}
