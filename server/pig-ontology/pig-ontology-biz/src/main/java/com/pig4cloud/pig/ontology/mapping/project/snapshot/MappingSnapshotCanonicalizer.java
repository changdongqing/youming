/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.snapshot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 映射配置快照规范化器（18-03 §6）。
 * <p>
 * 规范化规则：
 * <ul>
 *   <li>对象键按字典序排列</li>
 *   <li>JSON 数字不使用科学计数法</li>
 *   <li>哈希算法 SHA-256，UTF-8</li>
 * </ul>
 * 相同配置多次构建哈希一致。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MappingSnapshotCanonicalizer {

	private final ObjectMapper objectMapper;

	/**
	 * 规范化 JSON 字符串（键按字典序排序）。
	 * @param json 原始 JSON
	 * @return 规范化后的 JSON 字符串
	 */
	public String canonicalize(String json) {
		try {
			// 使用排序后的 ObjectMapper 解析和重新序列化
			ObjectMapper sortedMapper = objectMapper.copy()
					.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
			JsonNode root = sortedMapper.readTree(json);
			JsonNode sorted = sortJsonNode(root);
			return sortedMapper.writeValueAsString(sorted);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to canonicalize snapshot JSON", e);
		}
	}

	/**
	 * 计算配置快照的 SHA-256 哈希。
	 * @param canonicalJson 已规范化的 JSON 字符串
	 * @return SHA-256 十六进制哈希
	 */
	public String computeHash(String canonicalJson) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hashBytes) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to compute snapshot hash", e);
		}
	}

	/**
	 * 规范化 JSON 并计算哈希（一步完成）。
	 * @param json 原始 JSON
	 * @return SHA-256 十六进制哈希
	 */
	public String canonicalizeAndHash(String json) {
		return computeHash(canonicalize(json));
	}

	/**
	 * 递归排序 JSON 节点的键。
	 */
	private JsonNode sortJsonNode(JsonNode node) {
		if (node.isObject()) {
			ObjectNode sorted = objectMapper.createObjectNode();
			// 收集并排序键
			List<String> keys = new java.util.ArrayList<>();
			Iterator<String> fieldNames = node.fieldNames();
			while (fieldNames.hasNext()) {
				keys.add(fieldNames.next());
			}
			keys.sort(String::compareTo);
			for (String key : keys) {
				JsonNode child = node.get(key);
				sorted.set(key, sortJsonNode(child));
			}
			return sorted;
		}
		return node;
	}

}
