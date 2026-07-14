/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 来源身份标识。
 * <p>
 * 唯一标识外部数据源中的一条记录。sourceRecordKey 是规范化复合键字符串，
 * 规范格式：{@code column1=base64url(value1)&column2=base64url(value2)}，列名按映射定义顺序排序。
 *
 * @param sourceId 数据源ID
 * @param mappingProjectId 映射工程ID
 * @param mappingVersionId 映射版本ID
 * @param entityMappingCode 实体映射编码
 * @param sourceObject 源对象名
 * @param sourceRecordKey 规范化复合键字符串
 *
 * @author youming
 */
public record SourceIdentity(
	Long sourceId,
	Long mappingProjectId,
	Long mappingVersionId,
	String entityMappingCode,
	String sourceObject,
	String sourceRecordKey
) {

	/**
	 * 计算 sourceRecordKey 的 SHA-256 哈希，用于索引和日志。
	 * @return 64 字符小写十六进制哈希
	 */
	public String keyHash() {
		return sha256Hex(sourceRecordKey);
	}

	/**
	 * 从键值对构建规范化 sourceRecordKey。
	 * @param keyValuePairs 列名→值，按映射定义顺序
	 * @return 规范化复合键字符串
	 */
	public static String buildRecordKey(LinkedHashMap<String, String> keyValuePairs) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
			if (sb.length() > 0) {
				sb.append('&');
			}
			sb.append(entry.getKey()).append('=').append(base64Url(entry.getValue()));
		}
		return sb.toString();
	}

	private static String base64Url(String value) {
		if (value == null) {
			return "";
		}
		return Base64.getUrlEncoder().withoutPadding()
			.encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private static String sha256Hex(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

}
