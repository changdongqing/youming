/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.compiler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 关系键编译器（18-05 §4）。
 * <p>
 * 从键映射 JSON 和样例值编译出规范化的 recordKey 和 relationKey，并计算 SHA-256 hash。
 * 不保存源整行，只保存规范化关系键和两端记录键。
 * <p>
 * 键映射 JSON 格式：
 * <pre>
 * {"subject":[{"sourceColumn":"user_id","bindingKeyColumn":"user_id"}],
 *  "object":[{"sourceColumn":"dept_id","bindingKeyColumn":"dept_id"}]}
 * </pre>
 * relationKeyColumns JSON 格式：{@code ["user_id","post_id"]}
 *
 * @author youming
 */
@Slf4j
@Component
public class RelationKeyCompiler {

	private static final Pattern COLUMN_PAIR_PATTERN = Pattern.compile(
			"\"sourceColumn\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"bindingKeyColumn\"\\s*:\\s*\"([^\"]+)\"");

	private static final Pattern STRING_ARRAY_PATTERN = Pattern.compile("\"([^\"]+)\"");

	/**
	 * 编译主体记录键。
	 * @param subjectKeyMappingJson 主体键映射 JSON
	 * @param sampleValues 样例值
	 * @return recordKey 字符串
	 */
	public String compileSubjectKey(String subjectKeyMappingJson, Map<String, String> sampleValues) {
		return compileRecordKey(subjectKeyMappingJson, sampleValues);
	}

	/**
	 * 编译客体记录键。
	 * @param objectKeyMappingJson 客体键映射 JSON
	 * @param sampleValues 样例值
	 * @return recordKey 字符串
	 */
	public String compileObjectKey(String objectKeyMappingJson, Map<String, String> sampleValues) {
		return compileRecordKey(objectKeyMappingJson, sampleValues);
	}

	/**
	 * 编译关系键。
	 * @param relationKeyColumnsJson 关系键列 JSON，如 {@code ["user_id","post_id"]}
	 * @param sampleValues 样例值
	 * @return relationKey 字符串
	 */
	public String compileRelationKey(String relationKeyColumnsJson, Map<String, String> sampleValues) {
		List<String> columnNames = parseStringArray(relationKeyColumnsJson);
		List<String> parts = new ArrayList<>();
		for (String col : columnNames) {
			String value = sampleValues.getOrDefault(col, "");
			parts.add(value);
		}
		return String.join("|", parts);
	}

	/**
	 * 计算 SHA-256 哈希。
	 * @param input 输入字符串
	 * @return 十六进制哈希
	 */
	public String computeHash(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hashBytes) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to compute hash", e);
		}
	}

	/**
	 * 生成键摘要（hash 前8字符），用于脱敏展示。
	 * @param hash 完整 hash
	 * @return 摘要字符串
	 */
	public String toDigest(String hash) {
		if (hash == null || hash.length() < 8) {
			return hash;
		}
		return "..." + hash.substring(hash.length() - 8);
	}

	/**
	 * 从键映射 JSON 中提取 sourceColumn 和 bindingKeyColumn 对。
	 * @param keyMappingJson 键映射 JSON
	 * @return 列对列表
	 */
	public List<ColumnPair> extractColumnPairs(String keyMappingJson) {
		List<ColumnPair> pairs = new ArrayList<>();
		if (keyMappingJson == null || keyMappingJson.isBlank()) {
			return pairs;
		}
		Matcher matcher = COLUMN_PAIR_PATTERN.matcher(keyMappingJson);
		while (matcher.find()) {
			pairs.add(new ColumnPair(matcher.group(1), matcher.group(2)));
		}
		return pairs;
	}

	/**
	 * 从 JSON 数组字符串中提取字符串元素。
	 * @param jsonArrayJson JSON 数组字符串
	 * @return 字符串列表
	 */
	private List<String> parseStringArray(String jsonArrayJson) {
		List<String> items = new ArrayList<>();
		if (jsonArrayJson == null || jsonArrayJson.isBlank()) {
			return items;
		}
		Matcher matcher = STRING_ARRAY_PATTERN.matcher(jsonArrayJson);
		while (matcher.find()) {
			items.add(matcher.group(1));
		}
		return items;
	}

	/**
	 * 编译记录键（主体/客体通用）。
	 * @param keyMappingJson 键映射 JSON
	 * @param sampleValues 样例值
	 * @return 规范化 recordKey
	 */
	private String compileRecordKey(String keyMappingJson, Map<String, String> sampleValues) {
		List<ColumnPair> pairs = extractColumnPairs(keyMappingJson);
		List<String> parts = new ArrayList<>();
		for (ColumnPair pair : pairs) {
			String value = sampleValues.getOrDefault(pair.sourceColumn(), "");
			parts.add(value);
		}
		return String.join("|", parts);
	}

	/**
	 * 列对。
	 *
	 * @param sourceColumn 源列名
	 * @param bindingKeyColumn 绑定键列名
	 */
	public record ColumnPair(String sourceColumn, String bindingKeyColumn) {
	}

}
