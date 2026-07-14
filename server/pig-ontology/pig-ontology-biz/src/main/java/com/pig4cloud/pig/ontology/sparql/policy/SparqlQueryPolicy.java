/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.policy;

import com.pig4cloud.pig.ontology.sparql.config.SparqlProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.core.DatasetDescription;
import org.apache.jena.sparql.syntax.ElementService;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.springframework.stereotype.Component;

/**
 * SPARQL 查询策略校验器。
 * <p>
 * 处理顺序（设计文档 §4.2）：
 * <ol>
 *   <li>空值、长度、编码和控制字符检查</li>
 *   <li>QueryFactory.create 解析</li>
 *   <li>仅允许 SELECT 或 ASK</li>
 *   <li>拒绝查询自带 Dataset 描述（FROM / FROM NAMED）</li>
 *   <li>遍历查询语法树，发现 ElementService 即拒绝</li>
 *   <li>限制 OFFSET；对 SELECT 的 LIMIT 做服务端收紧</li>
 * </ol>
 * 不得把"关键词正则"作为安全边界。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SparqlQueryPolicy {

	private final SparqlProperties properties;

	/** 稳定错误码 */
	public static final String CODE_INVALID = "SPARQL_QUERY_INVALID";
	public static final String CODE_FORBIDDEN = "SPARQL_QUERY_FORBIDDEN_FEATURE";
	public static final String CODE_LIMIT_EXCEEDED = "SPARQL_QUERY_LIMIT_EXCEEDED";

	/**
	 * 校验查询文本，返回校验结果。
	 * @param queryText SPARQL 查询文本
	 * @return 校验结果（通过则含 Query 对象，拒绝则含错误码）
	 */
	public QueryPolicyResult validate(String queryText) {
		// 1. 空值检查
		if (queryText == null || queryText.isBlank()) {
			return QueryPolicyResult.rejected(CODE_INVALID, "查询文本不能为空");
		}

		// 2. 长度检查
		if (queryText.length() > properties.getQueryMaxLength()) {
			return QueryPolicyResult.rejected(CODE_LIMIT_EXCEEDED,
					"查询文本长度超过限制：" + properties.getQueryMaxLength());
		}

		// 3. 控制字符检查（拒绝包含 NUL 等不可见控制字符的查询）
		if (containsControlChars(queryText)) {
			return QueryPolicyResult.rejected(CODE_INVALID, "查询文本包含非法控制字符");
		}

		// 4. ARQ 解析
		Query query;
		try {
			query = QueryFactory.create(queryText, Syntax.syntaxSPARQL_11);
		}
		catch (Exception e) {
			log.debug("SPARQL解析失败: {}", e.getMessage());
			return QueryPolicyResult.rejected(CODE_INVALID, "查询语法错误或不支持只读查询之外的操作");
		}

		// 5. 仅允许 SELECT 或 ASK
		if (!query.isSelectType() && !query.isAskType()) {
			return QueryPolicyResult.rejected(CODE_INVALID,
					"仅支持 SELECT 和 ASK 查询，不支持只读查询之外的操作");
		}

		String queryType = query.isSelectType() ? "SELECT" : "ASK";

		// 6. 拒绝 FROM / FROM NAMED（查询自带 Dataset 描述）
		DatasetDescription datasetDesc = query.getDatasetDescription();
		if (datasetDesc != null && (!datasetDesc.getDefaultGraphURIs().isEmpty()
				|| !datasetDesc.getNamedGraphURIs().isEmpty())) {
			return QueryPolicyResult.rejected(CODE_FORBIDDEN,
					"不允许使用 FROM / FROM NAMED 指定外部 Dataset");
		}

		// 7. 遍历语法树，检查 ElementService
		ElementServiceDetector serviceDetector = new ElementServiceDetector();
		if (query.getQueryPattern() != null) {
			ElementWalker.walk(query.getQueryPattern(), serviceDetector);
		}
		if (serviceDetector.isServiceDetected()) {
			return QueryPolicyResult.rejected(CODE_FORBIDDEN,
					"不允许使用 SERVICE 联邦查询");
		}

		// 8. OFFSET 检查
		long offset = query.hasOffset() ? query.getOffset() : 0;
		if (offset > properties.getMaxOffset()) {
			return QueryPolicyResult.rejected(CODE_LIMIT_EXCEEDED,
					"OFFSET 超过最大限制：" + properties.getMaxOffset());
		}

		// 9. SELECT 的 LIMIT 收紧
		int effectiveLimit = 0;
		if (query.isSelectType()) {
			long userLimit = query.hasLimit() ? query.getLimit() : properties.getDefaultRowLimit();
			effectiveLimit = (int) Math.min(userLimit, properties.getMaxRowLimit());
			// 收紧查询的 LIMIT，后续执行时以 effectiveLimit 为准
			query.setLimit(effectiveLimit);
		}

		return QueryPolicyResult.ok(query, effectiveLimit, queryType);
	}

	/**
	 * 检查文本是否包含控制字符（允许制表符、换行符、回车符）。
	 */
	private boolean containsControlChars(String text) {
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\t' || c == '\n' || c == '\r') {
				continue;
			}
			if (Character.isISOControl(c)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 语法树访问器，检测 ElementService。
	 */
	private static class ElementServiceDetector extends ElementVisitorBase {

		private boolean serviceDetected = false;

		@Override
		public void visit(ElementService element) {
			serviceDetected = true;
		}

		public boolean isServiceDetected() {
			return serviceDetected;
		}

	}

}
