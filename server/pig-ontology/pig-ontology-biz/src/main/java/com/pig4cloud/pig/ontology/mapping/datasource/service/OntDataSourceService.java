/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.ontology.mapping.datasource.dto.DataSourceCreateDTO;
import com.pig4cloud.pig.ontology.mapping.datasource.dto.DataSourceUpdateDTO;
import com.pig4cloud.pig.ontology.mapping.datasource.dto.MetadataObjectQuery;
import com.pig4cloud.pig.ontology.mapping.datasource.dto.SchemaPreviewRequest;
import com.pig4cloud.pig.ontology.mapping.datasource.entity.OntDataSource;
import com.pig4cloud.pig.ontology.mapping.datasource.vo.DataSourceVO;
import com.pig4cloud.pig.ontology.mapping.datasource.vo.SourceObjectMetadataVO;

import java.util.List;

/**
 * 数据源注册服务接口。
 *
 * @author youming
 */
public interface OntDataSourceService extends IService<OntDataSource> {

	/**
	 * 分页查询数据源。
	 */
	Page<DataSourceVO> page(Page<OntDataSource> page, String sourceCode, String sourceName, String status);

	/**
	 * 获取数据源详情（脱敏）。
	 */
	DataSourceVO getDetail(Long id);

	/**
	 * 创建数据源。
	 */
	DataSourceVO create(DataSourceCreateDTO dto);

	/**
	 * 更新数据源（递增 revision + 重加密 + 失效池 + 回 DRAFT）。
	 */
	DataSourceVO update(DataSourceUpdateDTO dto);

	/**
	 * 逻辑删除数据源（要求 DISABLED 状态）。
	 */
	boolean remove(Long id);

	/**
	 * 测试连接。
	 */
	ConnectionTestOutcome testConnection(Long id);

	/**
	 * 更新状态（ACTIVE 需最近测试成功）。
	 */
	DataSourceVO updateStatus(Long id, String status);

	/**
	 * 刷新元数据。
	 */
	int refreshMetadata(Long id);

	/**
	 * 列出 Schema。
	 */
	List<String> listSchemas(Long id);

	/**
	 * 预览 Schema（新建向导第4步使用，不落库直连发现）。
	 */
	List<String> previewSchemas(SchemaPreviewRequest request);

	/**
	 * 列出表/视图。
	 */
	List<SourceObjectMetadataVO.SourceObjectSummary> listObjects(Long id, MetadataObjectQuery query);

	/**
	 * 获取对象元数据。
	 */
	SourceObjectMetadataVO getObjectMetadata(Long id, String schemaName, String objectName);

	/**
	 * 连接测试结果。
	 *
	 * @param success     是否成功
	 * @param latencyMs   延迟毫秒
	 * @param errorCode    错误码
	 * @param errorMessage 错误消息
	 */
	record ConnectionTestOutcome(boolean success, long latencyMs, String errorCode, String errorMessage) {
	}

}
