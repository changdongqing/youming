/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.converter;

import com.pig4cloud.pig.ontology.sparql.vo.SparqlBindingVO;
import com.pig4cloud.pig.ontology.sparql.vo.SparqlQueryResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SPARQL 结果转换器。
 * <p>
 * 将 Jena RDFNode 转换为 VO。每个 RDFNode 返回 value、nodeType、datatypeIri、language；
 * 空绑定不生成伪字符串 null。
 * SELECT 最多读取 effectiveLimit + 1 行；多出的第 1 行只用于设置 truncated=true。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
public class SparqlResultConverter {

	/**
	 * 转换 SELECT 结果。
	 * @param rs Jena ResultSet
	 * @param effectiveLimit 服务端收紧后的有效行上限
	 * @param durationMs 执行耗时（毫秒）
	 * @return 查询结果 VO
	 */
	public SparqlQueryResultVO convertSelect(ResultSet rs, int effectiveLimit, long durationMs) {
		SparqlQueryResultVO vo = new SparqlQueryResultVO();
		vo.setQueryType("SELECT");

		List<String> variables = rs.getResultVars();
		vo.setVariables(variables);

		List<Map<String, SparqlBindingVO>> rows = new ArrayList<>();
		int maxRows = effectiveLimit + 1;
		int count = 0;
		boolean truncated = false;

		while (rs.hasNext()) {
			count++;
			if (count > maxRows) {
				// 超过上限+1行，不再读取
				truncated = true;
				break;
			}

			QuerySolution solution = rs.next();
			Map<String, SparqlBindingVO> row = new HashMap<>(variables.size());
			for (String varName : variables) {
				RDFNode node = solution.get(varName);
				row.put(varName, convertNode(node));
			}
			rows.add(row);
		}

		if (count > effectiveLimit) {
			truncated = true;
			// 移除多出的第1行（仅用于判断截断）
			if (rows.size() > effectiveLimit) {
				rows.remove(rows.size() - 1);
			}
		}

		vo.setRows(rows);
		vo.setRowCount(rows.size());
		vo.setDurationMs(durationMs);
		vo.setTruncated(truncated);

		return vo;
	}

	/**
	 * 转换 ASK 结果。
	 * @param result 布尔结果
	 * @param durationMs 执行耗时（毫秒）
	 * @return 查询结果 VO
	 */
	public SparqlQueryResultVO convertAsk(boolean result, long durationMs) {
		SparqlQueryResultVO vo = new SparqlQueryResultVO();
		vo.setQueryType("ASK");
		vo.setBooleanResult(result);
		vo.setDurationMs(durationMs);
		vo.setTruncated(false);
		return vo;
	}

	/**
	 * 将 Jena RDFNode 转换为绑定 VO。空绑定返回 null（不生成伪字符串 null）。
	 */
	private SparqlBindingVO convertNode(RDFNode node) {
		if (node == null) {
			return null;
		}

		SparqlBindingVO vo = new SparqlBindingVO();

		if (node.isResource()) {
			Resource resource = node.asResource();
			if (resource.isAnon()) {
				// Blank node
				vo.setValue(resource.getId().getLabelString());
				vo.setNodeType("BNODE");
			}
			else {
				// IRI
				vo.setValue(resource.getURI());
				vo.setNodeType("IRI");
			}
		}
		else if (node.isLiteral()) {
			Literal literal = node.asLiteral();
			vo.setValue(literal.getLexicalForm());
			vo.setNodeType("LITERAL");
			String datatypeUri = literal.getDatatypeURI();
			if (datatypeUri != null) {
				vo.setDatatypeIri(datatypeUri);
			}
			String lang = literal.getLanguage();
			if (lang != null && !lang.isEmpty()) {
				vo.setLanguage(lang);
			}
		}
		else {
			// 未知类型，降级处理
			vo.setValue(node.toString());
			vo.setNodeType("LITERAL");
		}

		return vo;
	}

}
