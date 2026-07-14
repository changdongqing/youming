/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pig4cloud.pig.ontology.sparql.dto.SparqlQueryRequest;
import com.pig4cloud.pig.ontology.sparql.vo.SparqlQueryLogVO;
import com.pig4cloud.pig.ontology.sparql.vo.SparqlQueryResultVO;
import com.pig4cloud.pig.ontology.sparql.vo.SparqlTemplateVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * SPARQL 查询编排服务。
 *
 * @author youming
 */
public interface OntSparqlService {

	/**
	 * 执行 SELECT/ASK 查询。
	 * @param request 查询请求
	 * @param currentUser 当前用户
	 * @return 查询结果
	 */
	SparqlQueryResultVO executeQuery(SparqlQueryRequest request, String currentUser);

	/**
	 * 流式导出 SELECT 结果为 CSV。
	 * @param request 查询请求
	 * @param response HTTP 响应
	 * @param currentUser 当前用户
	 */
	void exportCsv(SparqlQueryRequest request, HttpServletResponse response, String currentUser);

	/**
	 * 获取预置查询模板。
	 * @return 模板列表
	 */
	List<SparqlTemplateVO> getTemplates();

	/**
	 * 分页查询历史。
	 * @param ontologyId 本体工程 ID（可选）
	 * @param createBy 创建人（可选筛选）
	 * @param page 页码
	 * @param size 每页条数
	 * @return 分页结果
	 */
	IPage<SparqlQueryLogVO> queryHistory(Long ontologyId, String createBy, int page, int size);

	/**
	 * 查询历史详情。
	 * @param id 日志 ID
	 * @param currentUser 当前用户
	 * @return 日志 VO
	 */
	SparqlQueryLogVO getHistoryDetail(Long id, String currentUser);

	/**
	 * 删除本人查询历史。
	 * @param id 日志 ID
	 * @param currentUser 当前用户
	 * @return 是否删除成功
	 */
	boolean deleteHistory(Long id, String currentUser);

}
