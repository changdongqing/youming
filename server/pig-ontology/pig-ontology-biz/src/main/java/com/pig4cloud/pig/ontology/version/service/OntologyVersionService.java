/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.version.dto.VersionConfigRequest;
import com.pig4cloud.pig.ontology.version.dto.VersionPrepareRequest;
import com.pig4cloud.pig.ontology.version.dto.VersionQuery;
import com.pig4cloud.pig.ontology.version.entity.OntOntologyVersion;
import com.pig4cloud.pig.ontology.version.vo.OntOntologyVersionDetailVO;
import com.pig4cloud.pig.ontology.version.vo.VersionDiffVO;
import com.pig4cloud.pig.ontology.version.vo.VersionPrepareResultVO;

/**
 * 本体版本服务接口。
 *
 * @author youming
 */
public interface OntologyVersionService extends IService<OntOntologyVersion> {

	/**
	 * 构建候选版本。
	 */
	R<VersionPrepareResultVO> prepare(VersionPrepareRequest request);

	/**
	 * 激活候选版本。
	 */
	R<Boolean> activate(Long versionId);

	/**
	 * 取消候选版本。
	 */
	R<Boolean> cancel(Long versionId);

	/**
	 * 版本分页查询。
	 */
	R<Page<OntOntologyVersionDetailVO>> page(Page<OntOntologyVersion> page, VersionQuery query);

	/**
	 * 版本详情。
	 */
	R<OntOntologyVersionDetailVO> getDetail(Long id);

	/**
	 * 两版本 diff。
	 */
	R<VersionDiffVO> diff(Long id, Long targetId);

	/**
	 * 获取快照 JSON。
	 */
	R<String> getSnapshot(Long id);

	/**
	 * 配置工程版本 IRI。
	 */
	R<Boolean> updateVersionConfig(Long projectId, VersionConfigRequest request);

}
