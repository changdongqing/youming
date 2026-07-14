/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.sparql.dto.SparqlQueryRequest;
import com.pig4cloud.pig.ontology.sparql.policy.SparqlQueryException;
import com.pig4cloud.pig.ontology.sparql.service.OntSparqlService;
import com.pig4cloud.pig.ontology.sparql.vo.SparqlQueryLogVO;
import com.pig4cloud.pig.ontology.sparql.vo.SparqlQueryResultVO;
import com.pig4cloud.pig.ontology.sparql.vo.SparqlTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SPARQL 查询端点 REST API。
 * <p>
 * 提供受控的只读 RDF 查询视图，不提供 SPARQL Update。
 * </p>
 *
 * @author youming
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ontology/sparql")
@Tag(description = "SPARQL查询端点", name = "SPARQL查询端点")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntSparqlController {

	private final OntSparqlService sparqlService;

	/**
	 * 执行 SELECT/ASK 查询。
	 */
	@PostMapping("/query")
	@SysLog("执行SPARQL查询")
	@HasPermission("ontology_sparql_query")
	@Operation(summary = "执行SPARQL查询", description = "支持SELECT和ASK，请求体传查询文本，避免URL泄漏")
	public R<SparqlQueryResultVO> query(@Valid @RequestBody SparqlQueryRequest request) {
		try {
			return R.ok(sparqlService.executeQuery(request, getCurrentUser()));
		}
		catch (SparqlQueryException e) {
			log.debug("SPARQL查询被拒绝: code={}, msg={}", e.getErrorCode(), e.getMessage());
			R<SparqlQueryResultVO> r = R.failed(e.getMessage());
			r.setData(buildErrorResult(e));
			return r;
		}
	}

	/**
	 * 流式导出 SELECT 结果为 CSV。
	 */
	@PostMapping("/query/export")
	@SysLog("导出SPARQL查询结果")
	@HasPermission("ontology_sparql_export")
	@Operation(summary = "导出SPARQL查询结果CSV", description = "流式导出，不预先装入全部结果")
	public void export(@Valid @RequestBody SparqlQueryRequest request, HttpServletResponse response) {
		sparqlService.exportCsv(request, response, getCurrentUser());
	}

	/**
	 * 获取预置查询模板。
	 */
	@GetMapping("/templates")
	@HasPermission("ontology_sparql_view")
	@Operation(summary = "获取预置查询模板")
	public R<List<SparqlTemplateVO>> templates() {
		return R.ok(sparqlService.getTemplates());
	}

	/**
	 * 分页查询历史。
	 */
	@GetMapping("/history/page")
	@HasPermission("ontology_sparql_view")
	@Operation(summary = "查询本人SPARQL查询历史", description = "管理员可按用户筛选")
	public R<IPage<SparqlQueryLogVO>> historyPage(
			@RequestParam(required = false) Long ontologyId,
			@RequestParam(required = false) String createBy,
			@RequestParam(defaultValue = "1") Integer page,
			@RequestParam(defaultValue = "20") Integer size) {
		return R.ok(sparqlService.queryHistory(ontologyId, createBy, page, size));
	}

	/**
	 * 查询历史详情（脱敏预览，需所有权校验）。
	 */
	@GetMapping("/history/{id}")
	@HasPermission("ontology_sparql_view")
	@Operation(summary = "查询历史详情")
	public R<SparqlQueryLogVO> historyDetail(@PathVariable Long id) {
		return R.ok(sparqlService.getHistoryDetail(id, getCurrentUser()));
	}

	/**
	 * 删除本人查询历史。
	 */
	@DeleteMapping("/history/{id}")
	@HasPermission("ontology_sparql_query")
	@Operation(summary = "删除本人查询历史")
	public R<Boolean> deleteHistory(@PathVariable Long id) {
		return R.ok(sparqlService.deleteHistory(id, getCurrentUser()));
	}

	/**
	 * 获取当前登录用户名。
	 */
	private String getCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return (auth != null && auth.getName() != null) ? auth.getName() : "system";
	}

	/**
	 * 构建错误结果 VO（携带错误码供前端判断）。
	 */
	private SparqlQueryResultVO buildErrorResult(SparqlQueryException e) {
		SparqlQueryResultVO vo = new SparqlQueryResultVO();
		vo.setQueryType("ERROR");
		vo.setRowCount(0);
		vo.setDurationMs(0L);
		vo.setTruncated(false);
		return vo;
	}

}
