/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pig4cloud.pig.ontology.sparql.config.SparqlProperties;
import com.pig4cloud.pig.ontology.sparql.converter.SparqlResultConverter;
import com.pig4cloud.pig.ontology.sparql.dto.SparqlQueryRequest;
import com.pig4cloud.pig.ontology.sparql.log.SparqlQueryLogService;
import com.pig4cloud.pig.ontology.sparql.model.SparqlModelProvider;
import com.pig4cloud.pig.ontology.sparql.model.SparqlModelProjectionPolicy;
import com.pig4cloud.pig.ontology.sparql.policy.QueryPolicyResult;
import com.pig4cloud.pig.ontology.sparql.policy.SparqlQueryException;
import com.pig4cloud.pig.ontology.sparql.policy.SparqlQueryPolicy;
import com.pig4cloud.pig.ontology.sparql.security.OntologyQueryAccessGuard;
import com.pig4cloud.pig.ontology.sparql.service.OntSparqlService;
import com.pig4cloud.pig.ontology.sparql.template.SparqlTemplateProvider;
import com.pig4cloud.pig.ontology.sparql.vo.SparqlQueryLogVO;
import com.pig4cloud.pig.ontology.sparql.vo.SparqlQueryResultVO;
import com.pig4cloud.pig.ontology.sparql.vo.SparqlTemplateVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionDatasetBuilder;
import org.apache.jena.query.QueryExecException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * SPARQL 查询编排服务实现。
 * <p>
 * 编排流程（设计文档 §3）：
 * <ol>
 *   <li>accessGuard.checkAccess — 工程访问校验</li>
 *   <li>queryPolicy.validate — ARQ解析、类型白名单、SERVICE/FROM检查、LIMIT收紧</li>
 *   <li>信号量获取 — 并发槽位控制</li>
 *   <li>modelProvider.buildModel — 请求级独立 Model</li>
 *   <li>projectionPolicy.applyProjection — 模块36授权投影（一期空实现）</li>
 *   <li>QueryExecution — try-with-resources，超时控制</li>
 *   <li>resultConverter — RDFNode 到 VO</li>
 *   <li>查询日志 — 成功与失败均记录摘要</li>
 * </ol>
 * </p>
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OntSparqlServiceImpl implements OntSparqlService {

	private final OntologyQueryAccessGuard accessGuard;
	private final SparqlQueryPolicy queryPolicy;
	private final SparqlModelProvider modelProvider;
	private final SparqlModelProjectionPolicy projectionPolicy;
	private final SparqlResultConverter resultConverter;
	private final SparqlTemplateProvider templateProvider;
	private final SparqlQueryLogService queryLogService;
	private final SparqlProperties properties;

	/** 并发查询信号量 */
	private Semaphore concurrentSemaphore;

	@jakarta.annotation.PostConstruct
	public void init() {
		this.concurrentSemaphore = new Semaphore(properties.getMaxConcurrentQueries(), true);
	}

	@Override
	public SparqlQueryResultVO executeQuery(SparqlQueryRequest request, String currentUser) {
		Long ontologyId = request.getOntologyId();
		String queryText = request.getQuery();
		long startTime = System.currentTimeMillis();

		// 1. 工程访问校验
		try {
			accessGuard.checkAccess(ontologyId);
		}
		catch (SparqlQueryException e) {
			recordLog(ontologyId, null, queryText, request.getFormat(), 0, startTime, "REJECTED",
					e.getErrorCode(), currentUser);
			throw e;
		}

		// 2. 查询策略校验（ARQ解析、类型白名单、SERVICE/FROM检查、LIMIT收紧）
		QueryPolicyResult policyResult = queryPolicy.validate(queryText);
		if (policyResult.isRejected()) {
			recordLog(ontologyId, policyResult.getQueryType(), queryText, request.getFormat(),
					0, startTime, "REJECTED", policyResult.getErrorCode(), currentUser);
			throw new SparqlQueryException(policyResult.getErrorCode(), policyResult.getErrorMessage());
		}

		// 3. 并发槽位获取
		boolean acquired = false;
		try {
			acquired = concurrentSemaphore.tryAcquire(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
			if (!acquired) {
				recordLog(ontologyId, policyResult.getQueryType(), queryText, request.getFormat(),
						0, startTime, "REJECTED", "SPARQL_QUERY_BUSY", currentUser);
				throw new SparqlQueryException("SPARQL_QUERY_BUSY", "当前并发查询数过多，请稍后重试");
			}

			// 4. 构建请求级 Model
			Model model = modelProvider.buildModel(ontologyId);

			try {
				// 5. 授权投影（模块36扩展点，一期空实现）
				projectionPolicy.applyProjection(model, ontologyId, currentUser);

				// 6. 执行查询
				Query query = policyResult.getQuery();
				try (QueryExecution qexec = QueryExecutionDatasetBuilder.create()
						.query(query)
						.model(model)
						.timeout(properties.getTimeoutMs(), TimeUnit.MILLISECONDS)
						.build()) {

					if (query.isSelectType()) {
						ResultSet rs = qexec.execSelect();
						long duration = System.currentTimeMillis() - startTime;
						SparqlQueryResultVO vo = resultConverter.convertSelect(rs,
								policyResult.getEffectiveLimit(), duration);

						// 8. 记录查询日志
						recordLog(ontologyId, policyResult.getQueryType(), queryText, request.getFormat(),
								vo.getRowCount(), startTime, "SUCCESS", null, currentUser);

						return vo;
					}
					else {
						boolean result = qexec.execAsk();
						long duration = System.currentTimeMillis() - startTime;
						SparqlQueryResultVO vo = resultConverter.convertAsk(result, duration);

						recordLog(ontologyId, policyResult.getQueryType(), queryText, request.getFormat(),
								1, startTime, "SUCCESS", null, currentUser);

						return vo;
					}
				}
				catch (QueryExecException e) {
					// 查询超时或执行异常
					String errorCode = isTimeoutException(e) ? "SPARQL_QUERY_TIMEOUT" : "SPARQL_QUERY_INVALID";
					String status = isTimeoutException(e) ? "TIMEOUT" : "FAILED";
					recordLog(ontologyId, policyResult.getQueryType(), queryText, request.getFormat(),
							0, startTime, status, errorCode, currentUser);
					throw new SparqlQueryException(errorCode,
							isTimeoutException(e) ? "查询超时" : "查询执行失败");
				}
			}
			finally {
				// Model 为请求私有对象，查询完成后 close
				model.close();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			recordLog(ontologyId, policyResult.getQueryType(), queryText, request.getFormat(),
					0, startTime, "TIMEOUT", "SPARQL_QUERY_TIMEOUT", currentUser);
			throw new SparqlQueryException("SPARQL_QUERY_TIMEOUT", "查询等待超时");
		}
		finally {
			if (acquired) {
				concurrentSemaphore.release();
			}
		}
	}

	@Override
	public void exportCsv(SparqlQueryRequest request, HttpServletResponse response, String currentUser) {
		Long ontologyId = request.getOntologyId();
		String queryText = request.getQuery();
		long startTime = System.currentTimeMillis();

		// 1. 工程访问校验
		accessGuard.checkAccess(ontologyId);

		// 2. 查询策略校验
		QueryPolicyResult policyResult = queryPolicy.validate(queryText);
		if (policyResult.isRejected()) {
			recordLog(ontologyId, policyResult.getQueryType(), queryText, request.getFormat(),
					0, startTime, "REJECTED", policyResult.getErrorCode(), currentUser);
			throw new SparqlQueryException(policyResult.getErrorCode(), policyResult.getErrorMessage());
		}

		// 3. 并发槽位获取
		boolean acquired = false;
		try {
			acquired = concurrentSemaphore.tryAcquire(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
			if (!acquired) {
				throw new SparqlQueryException("SPARQL_QUERY_BUSY", "当前并发查询数过多，请稍后重试");
			}

			// 导出使用 exportMaxRows 作为行上限
			Query query = policyResult.getQuery();
			if (query.isSelectType()) {
				// 导出时放宽到 exportMaxRows
				query.setLimit(properties.getExportMaxRows());
			}

			Model model = modelProvider.buildModel(ontologyId);
			try {
				projectionPolicy.applyProjection(model, ontologyId, currentUser);

				// 设置响应头
				String fileName = URLEncoder.encode("sparql-export-" + System.currentTimeMillis() + ".csv",
						StandardCharsets.UTF_8);
				response.setContentType("text/csv;charset=UTF-8");
				response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

				try (QueryExecution qexec = QueryExecutionDatasetBuilder.create()
						.query(query)
						.model(model)
						.timeout(properties.getTimeoutMs(), TimeUnit.MILLISECONDS)
						.build()) {

					ResultSet rs = qexec.execSelect();
					writeCsvStream(rs, response);

					recordLog(ontologyId, policyResult.getQueryType(), queryText, "CSV",
							null, startTime, "SUCCESS", null, currentUser);
				}
			}
			finally {
				model.close();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SparqlQueryException("SPARQL_QUERY_TIMEOUT", "查询等待超时");
		}
		catch (SparqlQueryException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("CSV导出失败", e);
			throw new SparqlQueryException("SPARQL_QUERY_INVALID", "导出失败");
		}
		finally {
			if (acquired) {
				concurrentSemaphore.release();
			}
		}
	}

	@Override
	public List<SparqlTemplateVO> getTemplates() {
		return templateProvider.getTemplates();
	}

	@Override
	public IPage<SparqlQueryLogVO> queryHistory(Long ontologyId, String createBy, int page, int size) {
		return queryLogService.queryHistory(ontologyId, createBy, page, size);
	}

	@Override
	public SparqlQueryLogVO getHistoryDetail(Long id, String currentUser) {
		return queryLogService.queryById(id, currentUser);
	}

	@Override
	public boolean deleteHistory(Long id, String currentUser) {
		return queryLogService.deleteById(id, currentUser);
	}

	/**
	 * 流式写入 CSV。
	 */
	private void writeCsvStream(ResultSet rs, HttpServletResponse response) throws Exception {
		OutputStream os = response.getOutputStream();
		PrintWriter writer = new PrintWriter(os, false, StandardCharsets.UTF_8);

		// 写入 BOM 以便 Excel 正确识别 UTF-8
		os.write(0xEF);
		os.write(0xBB);
		os.write(0xBF);

		// 表头
		List<String> variables = rs.getResultVars();
		writer.println(String.join(",", variables));

		// 数据行
		int count = 0;
		while (rs.hasNext()) {
			org.apache.jena.query.QuerySolution solution = rs.next();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < variables.size(); i++) {
				if (i > 0) {
					sb.append(",");
				}
				org.apache.jena.rdf.model.RDFNode node = solution.get(variables.get(i));
				if (node != null) {
					String value = csvEscape(node.toString());
					sb.append(value);
				}
			}
			writer.println(sb);
			count++;

			// 定期刷新
			if (count % 1000 == 0) {
				writer.flush();
			}
		}
		writer.flush();
		log.debug("CSV导出完成: {} 行", count);
	}

	/**
	 * CSV 单元格转义：包含逗号、引号、换行时用双引号包裹并转义内部双引号。
	 */
	private String csvEscape(String value) {
		if (value == null) {
			return "";
		}
		if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

	/**
	 * 判断是否为超时异常。
	 */
	private boolean isTimeoutException(Exception e) {
		if (e == null) {
			return false;
		}
		String message = e.getMessage();
		if (message != null && message.toLowerCase().contains("timeout")) {
			return true;
		}
		// Jena 超时通常抛出 QueryExecException 或包装的 InterruptedException
		return e.getCause() instanceof InterruptedException
				|| e.getCause() != null && e.getCause().getMessage() != null
						&& e.getCause().getMessage().toLowerCase().contains("timeout");
	}

	/**
	 * 记录查询日志。
	 * @param queryType 查询类型（SELECT/ASK），拒绝阶段未解析时可为 null，将记为 REJECTED 状态
	 */
	private void recordLog(Long ontologyId, String queryType, String queryText, String format,
			Integer rowCount, long startTime, String status, String errorCode, String currentUser) {
		long duration = System.currentTimeMillis() - startTime;
		// query_type 不允许为 null（CHECK 约束），拒绝阶段无法确定类型时记为 SELECT
		String safeQueryType = queryType != null ? queryType : "SELECT";
		String truncated = rowCount != null && rowCount > 0 ? "0" : "0";
		queryLogService.log(ontologyId, safeQueryType, queryText, format, rowCount, duration,
				truncated, status, errorCode, currentUser);
	}

}
