/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.log;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pig4cloud.pig.ontology.sparql.vo.SparqlQueryLogVO;

/**
 * SPARQL 查询日志服务。
 * <p>
 * 兼具用户查询历史和执行审计摘要功能。合规审计由模块 36 的 ont_data_access_log 承担。
 * </p>
 *
 * @author youming
 */
public interface SparqlQueryLogService {

	/**
	 * 记录查询日志。
	 * @param ontologyId 本体工程 ID
	 * @param queryType 查询类型 SELECT/ASK
	 * @param queryText 查询文本（默认不落库，仅用于计算 hash 和 preview）
	 * @param resultFormat 结果格式
	 * @param rowCount 结果行数
	 * @param durationMs 执行耗时（毫秒）
	 * @param truncated 是否截断
	 * @param status 执行状态：SUCCESS/REJECTED/TIMEOUT/FAILED
	 * @param errorCode 错误码（成功时为 null）
	 * @param currentUser 当前用户
	 */
	void log(Long ontologyId, String queryType, String queryText, String resultFormat,
			Integer rowCount, Long durationMs, String truncated, String status,
			String errorCode, String currentUser);

	/**
	 * 分页查询历史（默认仅本人可见，管理员可按用户筛选）。
	 * @param ontologyId 本体工程 ID（可选）
	 * @param createBy 创建人（可选筛选）
	 * @param page 页码
	 * @param size 每页条数
	 * @return 分页结果
	 */
	IPage<SparqlQueryLogVO> queryHistory(Long ontologyId, String createBy, int page, int size);

	/**
	 * 查询详情（需所有权校验）。
	 * @param id 日志 ID
	 * @param currentUser 当前用户
	 * @return 日志 VO
	 */
	SparqlQueryLogVO queryById(Long id, String currentUser);

	/**
	 * 删除本人查询历史。
	 * @param id 日志 ID
	 * @param currentUser 当前用户
	 * @return 是否删除成功
	 */
	boolean deleteById(Long id, String currentUser);

}
