/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pig4cloud.pig.ontology.version.snapshot.OntologySnapshotCanonicalizer;
import com.pig4cloud.pig.ontology.version.vo.VersionDiffVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IRI 级 diff 服务，比较两个规范化快照的差异并输出 {@link VersionDiffVO}。
 * <p>
 * 按设计文档 §5.3：比较在规范化后的快照 JSON 上进行（已完成 ID→IRI 替换和审计字段移除）。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OntologyVersionDiffService {

	private static final Set<String> NON_SEMANTIC_FIELDS = Set.of("sortOrder", "remarks");

	private static final Map<String, String> ARRAY_TO_RESOURCE_TYPE = new HashMap<>();

	static {
		ARRAY_TO_RESOURCE_TYPE.put("entityTypes", "ENTITY_TYPE");
		ARRAY_TO_RESOURCE_TYPE.put("dataProperties", "DATA_PROPERTY");
		ARRAY_TO_RESOURCE_TYPE.put("objectProperties", "OBJECT_PROPERTY");
		ARRAY_TO_RESOURCE_TYPE.put("axiomRules", "AXIOM_RULE");
		ARRAY_TO_RESOURCE_TYPE.put("entityTypeHierarchies", "HIERARCHY");
		ARRAY_TO_RESOURCE_TYPE.put("entityTypeDisjoints", "DISJOINT");
		ARRAY_TO_RESOURCE_TYPE.put("entityTypeEquivalents", "EQUIVALENT");
		ARRAY_TO_RESOURCE_TYPE.put("dataPropertyEnums", "ENUM");
		ARRAY_TO_RESOURCE_TYPE.put("objectPropertyDomains", "DOMAIN");
		ARRAY_TO_RESOURCE_TYPE.put("objectPropertyRanges", "RANGE");
	}

	private final OntologySnapshotCanonicalizer canonicalizer;

	private final OntologyCompatibilityClassifier classifier;

	/**
	 * 计算两个快照的 diff。
	 * @param oldSnapshotJson 旧版本快照 JSON（规范化后）
	 * @param newSnapshotJson 新版本快照 JSON（规范化后）
	 * @return diff 结果
	 */
	public VersionDiffVO diff(String oldSnapshotJson, String newSnapshotJson) {
		ObjectNode oldSnap = canonicalizer.canonicalize(oldSnapshotJson);
		ObjectNode newSnap = canonicalizer.canonicalize(newSnapshotJson);
		return diffCanonicalized(oldSnap, newSnap);
	}

	/**
	 * 计算两个已规范化快照的 diff。
	 */
	public VersionDiffVO diffCanonicalized(ObjectNode oldSnap, ObjectNode newSnap) {
		VersionDiffVO result = new VersionDiffVO();
		result.setAdded(new ArrayList<>());
		result.setRemoved(new ArrayList<>());
		result.setModified(new ArrayList<>());

		for (Map.Entry<String, String> entry : ARRAY_TO_RESOURCE_TYPE.entrySet()) {
			String arrayField = entry.getKey();
			String resourceType = entry.getValue();
			String iriField = getIriField(arrayField);
			diffArray(oldSnap, newSnap, arrayField, iriField, resourceType, result);
		}

		// 兼容性判定
		OntologyCompatibilityClassifier.CompatibilityResult compatResult = classifier.classify(oldSnap, newSnap);
		result.setCompatibility(compatResult.compatibility());
		result.setBreakingReasons(compatResult.breakingReasons());

		return result;
	}

	private void diffArray(ObjectNode oldSnap, ObjectNode newSnap, String arrayField, String iriField,
			String resourceType, VersionDiffVO result) {
		Map<String, JsonNode> oldMap = indexByIri(oldSnap, arrayField, iriField);
		Map<String, JsonNode> newMap = indexByIri(newSnap, arrayField, iriField);

		// added: 在 new 但不在 old
		for (Map.Entry<String, JsonNode> entry : newMap.entrySet()) {
			if (!oldMap.containsKey(entry.getKey())) {
				VersionDiffVO.DiffResource res = new VersionDiffVO.DiffResource();
				res.setResourceType(resourceType);
				res.setIri(entry.getKey());
				result.getAdded().add(res);
			}
		}

		// removed: 在 old 但不在 new
		for (Map.Entry<String, JsonNode> entry : oldMap.entrySet()) {
			if (!newMap.containsKey(entry.getKey())) {
				VersionDiffVO.DiffResource res = new VersionDiffVO.DiffResource();
				res.setResourceType(resourceType);
				res.setIri(entry.getKey());
				result.getRemoved().add(res);
			}
		}

		// modified: 两边都有但字段有变化
		for (Map.Entry<String, JsonNode> entry : oldMap.entrySet()) {
			JsonNode newItem = newMap.get(entry.getKey());
			if (newItem == null) {
				continue;
			}
			List<VersionDiffVO.FieldChange> changes = compareFields(entry.getValue(), newItem);
			if (!changes.isEmpty()) {
				VersionDiffVO.DiffModified modified = new VersionDiffVO.DiffModified();
				modified.setResourceType(resourceType);
				modified.setIri(entry.getKey());
				modified.setChanges(changes);
				result.getModified().add(modified);
			}
		}
	}

	private List<VersionDiffVO.FieldChange> compareFields(JsonNode oldItem, JsonNode newItem) {
		List<VersionDiffVO.FieldChange> changes = new ArrayList<>();
		Set<String> allFields = new HashSet<>();
		oldItem.fieldNames().forEachRemaining(allFields::add);
		newItem.fieldNames().forEachRemaining(allFields::add);

		for (String field : allFields) {
			if (NON_SEMANTIC_FIELDS.contains(field)) {
				continue;
			}
			String oldVal = textOrNull(oldItem, field);
			String newVal = textOrNull(newItem, field);
			if (!equalsNullSafe(oldVal, newVal)) {
				VersionDiffVO.FieldChange change = new VersionDiffVO.FieldChange();
				change.setField(field);
				change.setOldValue(oldVal);
				change.setNewValue(newVal);
				changes.add(change);
			}
		}
		return changes;
	}

	private String getIriField(String arrayField) {
		return switch (arrayField) {
			case "entityTypes", "dataProperties", "objectProperties" -> "iri";
			case "axiomRules" -> "ruleCode";
			case "entityTypeHierarchies" -> "parentIri"; // 使用 parentIri+childIri 组合
			case "entityTypeDisjoints" -> "iriA";
			case "entityTypeEquivalents" -> "iriA";
			case "dataPropertyEnums" -> "dataPropertyIri";
			case "objectPropertyDomains", "objectPropertyRanges" -> "objectPropertyIri";
			default -> "iri";
		};
	}

	private Map<String, JsonNode> indexByIri(ObjectNode snap, String arrayField, String iriField) {
		Map<String, JsonNode> map = new HashMap<>();
		JsonNode arr = snap.get(arrayField);
		if (arr == null || !arr.isArray()) {
			return map;
		}
		for (JsonNode item : arr) {
			// 对于关系型元素，使用组合键
			if ("entityTypeHierarchies".equals(arrayField)) {
				String k1 = textOrNull(item, "parentIri");
				String k2 = textOrNull(item, "childIri");
				if (k1 != null && k2 != null) {
					map.put(k1 + "|" + k2, item);
				}
			}
			else if ("entityTypeDisjoints".equals(arrayField) || "entityTypeEquivalents".equals(arrayField)) {
				String k1 = textOrNull(item, "iriA");
				String k2 = textOrNull(item, "iriB");
				if (k1 != null && k2 != null) {
					map.put(k1 + "|" + k2, item);
				}
			}
			else if ("objectPropertyDomains".equals(arrayField) || "objectPropertyRanges".equals(arrayField)) {
				String k1 = textOrNull(item, "objectPropertyIri");
				String k2 = textOrNull(item, "entityTypeIri");
				if (k1 != null && k2 != null) {
					map.put(k1 + "|" + k2, item);
				}
			}
			else if ("dataPropertyEnums".equals(arrayField)) {
				String k1 = textOrNull(item, "dataPropertyIri");
				String k2 = textOrNull(item, "value");
				if (k1 != null && k2 != null) {
					map.put(k1 + "|" + k2, item);
				}
			}
			else {
				String iri = textOrNull(item, iriField);
				if (iri != null) {
					map.put(iri, item);
				}
			}
		}
		return map;
	}

	private String textOrNull(JsonNode node, String field) {
		if (node == null) {
			return null;
		}
		JsonNode val = node.get(field);
		return val != null && !val.isNull() ? val.asText() : null;
	}

	private boolean equalsNullSafe(String a, String b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}

}
