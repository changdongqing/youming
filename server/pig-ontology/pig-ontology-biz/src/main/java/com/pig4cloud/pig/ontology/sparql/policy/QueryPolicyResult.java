/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.policy;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.jena.query.Query;

/**
 * SPARQL 查询策略校验结果。
 *
 * @author youming
 */
@Data
@AllArgsConstructor
public class QueryPolicyResult {

	/** 解析后的 ARQ Query 对象（校验通过时非空） */
	private Query query;

	/** 服务端收紧后的有效行上限（SELECT 有效，ASK 忽略） */
	private int effectiveLimit;

	/** 查询类型：SELECT / ASK */
	private String queryType;

	/** 拒绝时的稳定错误码（校验通过时为 null） */
	private String errorCode;

	/** 拒绝时的错误消息 */
	private String errorMessage;

	public static QueryPolicyResult rejected(String errorCode, String errorMessage) {
		return new QueryPolicyResult(null, 0, null, errorCode, errorMessage);
	}

	public static QueryPolicyResult ok(Query query, int effectiveLimit, String queryType) {
		return new QueryPolicyResult(query, effectiveLimit, queryType, null, null);
	}

	public boolean isRejected() {
		return errorCode != null;
	}

}
