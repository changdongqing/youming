/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.snapshot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 快照规范化处理器。
 * <p>
 * 对快照 JSON 执行以下步骤（设计文档 §4.4.1）：
 * <ol>
 *   <li>移除审计字段（id、createBy、createTime、updateBy、updateTime、delFlag、sortOrder、remarks）</li>
 *   <li>ID 替换为 IRI/业务编码</li>
 *   <li>数组按自然键稳定排序</li>
 *   <li>JSON 序列化时字段按字母序输出</li>
 *   <li>计算 SHA-256 哈希</li>
 * </ol>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OntologySnapshotCanonicalizer {

	private static final List<String> AUDIT_FIELDS = List.of("id", "createBy", "createTime", "updateBy", "updateTime",
			"delFlag", "sortOrder", "remarks");

	/**
	 * 各数组的排序键定义。key = 数组字段名，value = 排序键列表（按优先级）。
	 */
	private static final Map<String, List<String>> SORT_KEYS = new HashMap<>();

	static {
		SORT_KEYS.put("namespaces", List.of("prefix"));
		SORT_KEYS.put("entityTypes", List.of("iri"));
		SORT_KEYS.put("entityTypeHierarchies", List.of("parentIri", "childIri"));
		SORT_KEYS.put("entityTypeDisjoints", List.of("iriA", "iriB"));
		SORT_KEYS.put("entityTypeEquivalents", List.of("iriA", "iriB"));
		SORT_KEYS.put("dataProperties", List.of("iri"));
		SORT_KEYS.put("dataPropertyEnums", List.of("dataPropertyIri", "value"));
		SORT_KEYS.put("objectProperties", List.of("iri"));
		SORT_KEYS.put("objectPropertyDomains", List.of("objectPropertyIri", "entityTypeIri"));
		SORT_KEYS.put("objectPropertyRanges", List.of("objectPropertyIri", "entityTypeIri"));
		SORT_KEYS.put("axiomRules", List.of("ruleCode"));
		SORT_KEYS.put("axiomRuleTargets", List.of("ruleCode", "targetType", "targetIri"));
		SORT_KEYS.put("extensionDependencies", List.of("moduleCode"));
		SORT_KEYS.put("unitDependencies", List.of("unitIri"));
	}

	private final ObjectMapper objectMapper;

	/**
	 * 规范化快照 JSON：移除审计字段、ID→IRI 替换、稳定排序。
	 * <p>
	 * 此方法需要 ID→IRI 映射来执行替换。映射从快照自身的 entityTypes/dataProperties 等数组中提取。
	 * @param snapshotJson 原始快照 JSON
	 * @return 规范化后的 ObjectNode
	 */
	public ObjectNode canonicalize(String snapshotJson) {
		try {
			JsonNode root = objectMapper.readTree(snapshotJson);
			ObjectNode canonical = (ObjectNode) root;

			// 1. 构建 ID→IRI 映射
			Map<String, Map<Long, String>> idToIriMaps = buildIdToIriMaps(canonical);

			// 2. 对每个数组段执行规范化
			canonicalizeArray(canonical, "namespaces", idToIriMaps);
			canonicalizeArray(canonical, "entityTypes", idToIriMaps);
			canonicalizeArray(canonical, "entityTypeHierarchies", idToIriMaps);
			canonicalizeArray(canonical, "entityTypeDisjoints", idToIriMaps);
			canonicalizeArray(canonical, "entityTypeEquivalents", idToIriMaps);
			canonicalizeArray(canonical, "dataProperties", idToIriMaps);
			canonicalizeArray(canonical, "dataPropertyEnums", idToIriMaps);
			canonicalizeArray(canonical, "objectProperties", idToIriMaps);
			canonicalizeArray(canonical, "objectPropertyDomains", idToIriMaps);
			canonicalizeArray(canonical, "objectPropertyRanges", idToIriMaps);
			canonicalizeArray(canonical, "axiomRules", idToIriMaps);
			canonicalizeArray(canonical, "axiomRuleTargets", idToIriMaps);
			canonicalizeArray(canonical, "extensionDependencies", idToIriMaps);
			canonicalizeArray(canonical, "unitDependencies", idToIriMaps);

			return canonical;
		}
		catch (Exception e) {
			throw new RuntimeException("规范化快照失败", e);
		}
	}

	/**
	 * 计算规范化快照的 SHA-256 哈希。
	 * @param canonical 规范化后的 ObjectNode
	 * @return 十六进制小写 SHA-256
	 */
	public String computeHash(ObjectNode canonical) {
		try {
			ObjectMapper sortedMapper = new ObjectMapper();
			sortedMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
			String json = sortedMapper.writeValueAsString(canonical);
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
			return bytesToHex(hash);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 不可用", e);
		}
		catch (Exception e) {
			throw new RuntimeException("计算快照哈希失败", e);
		}
	}

	/**
	 * 规范化快照并计算哈希的便捷方法。
	 * @param snapshotJson 原始快照 JSON
	 * @return SHA-256 哈希
	 */
	public String canonicalizeAndHash(String snapshotJson) {
		return computeHash(canonicalize(snapshotJson));
	}

	/**
	 * 将规范化 ObjectNode 序列化为 JSON 字符串（字段按字母序）。
	 */
	public String toJsonString(ObjectNode canonical) {
		try {
			ObjectMapper sortedMapper = new ObjectMapper();
			sortedMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
			return sortedMapper.writeValueAsString(canonical);
		}
		catch (Exception e) {
			throw new RuntimeException("序列化规范化快照失败", e);
		}
	}

	private Map<String, Map<Long, String>> buildIdToIriMaps(ObjectNode root) {
		Map<String, Map<Long, String>> maps = new HashMap<>();

		// entityType: id → iri
		maps.put("entityType", extractIdToIri(root, "entityTypes", "id", "iri"));
		// dataProperty: id → iri
		maps.put("dataProperty", extractIdToIri(root, "dataProperties", "id", "iri"));
		// objectProperty: id → iri
		maps.put("objectProperty", extractIdToIri(root, "objectProperties", "id", "iri"));
		// axiomRule: id → ruleCode
		maps.put("axiomRule", extractIdToIri(root, "axiomRules", "id", "ruleCode"));
		// extensionModule: id → moduleCode
		maps.put("extensionModule", extractIdToIri(root, "extensionDependencies", "moduleId", "moduleCode"));
		// unit: id → unitCode (作为 unitIri 的来源)
		maps.put("unit", extractIdToIri(root, "unitDependencies", "id", "unitCode"));

		return maps;
	}

	private Map<Long, String> extractIdToIri(ObjectNode root, String arrayField, String idField, String iriField) {
		Map<Long, String> map = new HashMap<>();
		JsonNode arr = root.get(arrayField);
		if (arr != null && arr.isArray()) {
			for (JsonNode item : arr) {
				JsonNode idNode = item.get(idField);
				JsonNode iriNode = item.get(iriField);
				if (idNode != null && iriNode != null) {
					map.put(idNode.asLong(), iriNode.asText());
				}
			}
		}
		return map;
	}

	private void canonicalizeArray(ObjectNode root, String arrayField, Map<String, Map<Long, String>> idMaps) {
		JsonNode arrNode = root.get(arrayField);
		if (arrNode == null || !arrNode.isArray()) {
			return;
		}

		List<ObjectNode> items = new ArrayList<>();
		for (JsonNode item : arrNode) {
			if (item.isObject()) {
				ObjectNode obj = (ObjectNode) item;
				// 移除审计字段
				removeAuditFields(obj);
				// ID→IRI 替换
				replaceIdsWithIris(obj, arrayField, idMaps);
				items.add(obj);
			}
		}

		// 稳定排序
		List<String> sortKeyList = SORT_KEYS.get(arrayField);
		if (sortKeyList != null && !items.isEmpty()) {
			items.sort(Comparator.comparing(node -> getSortKey(node, sortKeyList)));
		}

		// 替换原数组
		ArrayNode sortedArray = objectMapper.createArrayNode();
		for (ObjectNode item : items) {
			sortedArray.add(item);
		}
		root.set(arrayField, sortedArray);
	}

	private String getSortKey(ObjectNode node, List<String> keys) {
		StringBuilder sb = new StringBuilder();
		for (String key : keys) {
			JsonNode val = node.get(key);
			sb.append(val != null ? val.asText() : "");
			sb.append("\0");
		}
		return sb.toString();
	}

	private void removeAuditFields(ObjectNode obj) {
		Iterator<String> fieldNames = obj.fieldNames();
		List<String> toRemove = new ArrayList<>();
		while (fieldNames.hasNext()) {
			String field = fieldNames.next();
			if (AUDIT_FIELDS.contains(field)) {
				toRemove.add(field);
			}
		}
		for (String field : toRemove) {
			obj.remove(field);
		}
		// 递归处理嵌套数组（如 labels、resources）
		Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> entry = fields.next();
			JsonNode val = entry.getValue();
			if (val.isArray()) {
				for (JsonNode child : val) {
					if (child.isObject()) {
						removeAuditFields((ObjectNode) child);
					}
				}
			}
		}
	}

	private void replaceIdsWithIris(ObjectNode obj, String arrayField, Map<String, Map<Long, String>> idMaps) {
		switch (arrayField) {
			case "entityTypes" -> {
				replaceIdField(obj, "namespaceId", null); // namespace 无 IRI，保留 prefix 即可
				// namespaceId 不替换为 IRI（namespace 表无 iri 字段），移除即可
			}
			case "entityTypeHierarchies" -> {
				replaceIdWithIriField(obj, "parentId", "parentIri", idMaps.get("entityType"));
				replaceIdWithIriField(obj, "childId", "childIri", idMaps.get("entityType"));
			}
			case "entityTypeDisjoints" -> {
				replaceIdWithIriField(obj, "typeA", "iriA", idMaps.get("entityType"));
				replaceIdWithIriField(obj, "typeB", "iriB", idMaps.get("entityType"));
			}
			case "entityTypeEquivalents" -> {
				replaceIdWithIriField(obj, "entityTypeId", "iriA", idMaps.get("entityType"));
				replaceIdWithIriField(obj, "equivalentId", "iriB", idMaps.get("entityType"));
			}
			case "dataProperties" -> {
				replaceIdWithIriField(obj, "domainEntityTypeId", "domainEntityIri", idMaps.get("entityType"));
			}
			case "dataPropertyEnums" -> {
				replaceIdWithIriField(obj, "dataPropertyId", "dataPropertyIri", idMaps.get("dataProperty"));
			}
			case "objectProperties" -> {
				replaceIdWithIriField(obj, "inverseOfId", "inverseOfIri", idMaps.get("objectProperty"));
			}
			case "objectPropertyDomains" -> {
				replaceIdWithIriField(obj, "objectPropertyId", "objectPropertyIri", idMaps.get("objectProperty"));
				replaceIdWithIriField(obj, "entityTypeId", "entityTypeIri", idMaps.get("entityType"));
			}
			case "objectPropertyRanges" -> {
				replaceIdWithIriField(obj, "objectPropertyId", "objectPropertyIri", idMaps.get("objectProperty"));
				replaceIdWithIriField(obj, "entityTypeId", "entityTypeIri", idMaps.get("entityType"));
			}
			case "axiomRuleTargets" -> {
				replaceIdWithIriField(obj, "axiomRuleId", "ruleCode", idMaps.get("axiomRule"));
			}
			case "extensionDependencies" -> {
				replaceIdWithIriField(obj, "moduleId", "moduleCode", idMaps.get("extensionModule"));
				// resources 数组中的 resourceId 不替换（多态类型，保留 resourceIri 即可）
			}
			case "unitDependencies" -> {
				replaceIdWithIriField(obj, "id", "unitIri", idMaps.get("unit"));
			}
			default -> {
				// namespaces 等无需替换
			}
		}
	}

	private void replaceIdWithIriField(ObjectNode obj, String idFieldName, String iriFieldName,
			Map<Long, String> idMap) {
		if (idMap == null) {
			return;
		}
		JsonNode idNode = obj.get(idFieldName);
		if (idNode != null && idNode.isNumber()) {
			String iri = idMap.get(idNode.asLong());
			if (iri != null) {
				obj.put(iriFieldName, iri);
			}
			obj.remove(idFieldName);
		}
	}

	private void replaceIdField(ObjectNode obj, String fieldName, String replacement) {
		if (replacement == null) {
			obj.remove(fieldName);
		}
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

}
