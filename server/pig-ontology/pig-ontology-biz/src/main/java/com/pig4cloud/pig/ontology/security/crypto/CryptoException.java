/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.crypto;

/**
 * 加解密异常，密钥不可用时抛出，禁止回退明文。
 *
 * @author youming
 */
public class CryptoException extends RuntimeException {

	public CryptoException(String message) {
		super(message);
	}

	public CryptoException(String message, Throwable cause) {
		super(message, cause);
	}

}
