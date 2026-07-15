/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.mapping.project.vo.MappingVersionDiffVO;
import com.pig4cloud.pig.ontology.mapping.project.vo.MappingVersionVO;
import com.pig4cloud.pig.ontology.mapping.vo.PublishPrepareResultVO;

/**
 * 映射版本服务接口（18-03 §8）。
 *
 * @author youming
 */
public interface OntMappingVersionService extends IService<OntMappingVersion> {

	/**
	 * 获取工程下版本列表（按创建时间降序）。
	 */
	Page<MappingVersionVO> listVersions(Page<OntMappingVersion> page, Long projectId);

	/**
	 * 从 active PUBLISHED 版本复制下一 DRAFT 版本。
	 * @param projectId 工程ID
	 * @return 新建的 DRAFT 版本
	 */
	MappingVersionVO createNextVersion(Long projectId);

	/**
	 * 获取版本详情。
	 */
	MappingVersionVO getVersionDetail(Long id);

	/**
	 * VALIDATED 退回 DRAFT（CAS），清除发布候选信息。
	 */
	MappingVersionVO reopen(Long id);

	/**
	 * 发布版本（设计 §8.4 发布事务）。
	 * <p>
	 * 事务内：
	 * <ol>
	 *   <li>锁映射工程行</li>
	 *   <li>确认版本 VALIDATED 且配置 revision/报告 hash 未变化</li>
	 *   <li>确认 Ontology 当前版本和工作区修订仍与校验一致</li>
	 *   <li>构建规范快照和 hash</li>
	 *   <li>旧 PUBLISHED 改 RETIRED</li>
	 *   <li>当前版本改 PUBLISHED</li>
	 *   <li>更新 project.activeVersionId 和 ACTIVE 状态</li>
	 *   <li>写 Outbox MAPPING_VERSION_PUBLISHED</li>
	 * </ol>
	 */
	MappingVersionVO publish(Long id);

	/**
	 * 停用当前发布版本，同时工程转 DISABLED。
	 */
	MappingVersionVO retire(Long id);

	/**
	 * 管理员查看脱敏快照（不含凭证）。
	 */
	String getSnapshot(Long id);

	/**
	 * 配置差异对比。
	 */
	MappingVersionDiffVO diff(Long baseId, Long compareId);

	/**
	 * 发布准备：返回风险摘要（18-06 §14 publish/prepare）。
	 * <p>
	 * 检查发布门禁条件并返回高风险变化摘要，但不执行发布。
	 * @param id 版本ID
	 * @return 风险摘要
	 */
	PublishPrepareResultVO preparePublish(Long id);

}
