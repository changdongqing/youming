/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.policy;

import lombok.Getter;

/**
 * SPARQL 查询业务异常。
 * <p>
 * 携带稳定错误码，不回显底层堆栈。
 * </p>
 *
 * @author youming
 */
@Getter
public class SparqlQueryException extends RuntimeException {

	private final String errorCode;

	public SparqlQueryException(String errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

}
