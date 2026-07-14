/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * 兼容性分类器，根据 diff 结果判定 BREAKING / BACKWARD_COMPATIBLE / PATCH_ONLY。
 * <p>
 * 按设计文档 §3.2 实现：发布者可以把等级调得更严格，但不能把 BREAKING 人工降为
 * BACKWARD_COMPATIBLE。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OntologyCompatibilityClassifier {

	private final ObjectMapper objectMapper;

	/**
	 * 兼容性等级常量。
	 */
	public static final String PATCH_ONLY = "PATCH_ONLY";

	public static final String BACKWARD_COMPATIBLE = "BACKWARD_COMPATIBLE";

	public static final String BREAKING = "BREAKING";

	/**
	 * 非语义字段，modified 检测时忽略。
	 */
	private static final Set<String> NON_SEMANTIC_FIELDS = Set.of("sortOrder", "remarks", "sort_order");

	/**
	 * 根据两个规范化快照判定兼容性。
	 * @param oldSnapshot 旧版本规范化快照
	 * @param newSnapshot 新版本规范化快照
	 * @return 兼容性判定结果
	 */
	public CompatibilityResult classify(ObjectNode oldSnapshot, ObjectNode newSnapshot) {
		List<String> breakingReasons = new ArrayList<>();

		// 1. 检查 IRI 删除（entityTypes / dataProperties / objectProperties / axiomRules）
		checkIriRemovals(oldSnapshot, newSnapshot, "entityTypes", "iri", "实体类型", breakingReasons);
		checkIriRemovals(oldSnapshot, newSnapshot, "dataProperties", "iri", "数据属性", breakingReasons);
		checkIriRemovals(oldSnapshot, newSnapshot, "objectProperties", "iri", "对象属性", breakingReasons);
		checkIriRemovals(oldSnapshot, newSnapshot, "axiomRules", "ruleCode", "公理规则", breakingReasons);

		// 2. 检查实体类型语义字段变化
		checkEntityTypeChanges(oldSnapshot, newSnapshot, breakingReasons);

		// 3. 检查数据属性语义字段变化
		checkDataPropertyChanges(oldSnapshot, newSnapshot, breakingReasons);

		// 4. 检查对象属性语义字段变化
		checkObjectPropertyChanges(oldSnapshot, newSnapshot, breakingReasons);

		// 5. 检查关系删除（继承/不相交/等价/domain/range）
		checkRelationRemovals(oldSnapshot, newSnapshot, "entityTypeHierarchies", "parentIri", "childIri", "继承关系",
				breakingReasons);
		checkRelationRemovals(oldSnapshot, newSnapshot, "objectPropertyDomains", "objectPropertyIri", "entityTypeIri",
				"对象属性domain", breakingReasons);
		checkRelationRemovals(oldSnapshot, newSnapshot, "objectPropertyRanges", "objectPropertyIri", "entityTypeIri",
				"对象属性range", breakingReasons);

		// 6. 检查枚举收窄
		checkEnumNarrowing(oldSnapshot, newSnapshot, breakingReasons);

		// 7. 检查新增不相交关系
		checkNewDisjoints(oldSnapshot, newSnapshot, breakingReasons);

		// 判定兼容性等级
		String compatibility;
		if (!breakingReasons.isEmpty()) {
			compatibility = BREAKING;
		}
		else if (hasAdditions(oldSnapshot, newSnapshot)) {
			compatibility = BACKWARD_COMPATIBLE;
		}
		else {
			compatibility = PATCH_ONLY;
		}

		return new CompatibilityResult(compatibility, breakingReasons);
	}

	private void checkIriRemovals(ObjectNode oldSnap, ObjectNode newSnap, String arrayField, String iriField,
			String label, List<String> reasons) {
		Set<String> oldIris = extractIriSet(oldSnap, arrayField, iriField);
		Set<String> newIris = extractIriSet(newSnap, arrayField, iriField);

		for (String iri : oldIris) {
			if (!newIris.contains(iri)) {
				reasons.add(label + "删除: " + iri);
			}
		}
	}

	private void checkEntityTypeChanges(ObjectNode oldSnap, ObjectNode newSnap, List<String> reasons) {
		Map<String, JsonNode> oldMap = indexByIri(oldSnap, "entityTypes", "iri");
		Map<String, JsonNode> newMap = indexByIri(newSnap, "entityTypes", "iri");

		for (Map.Entry<String, JsonNode> entry : oldMap.entrySet()) {
			JsonNode newItem = newMap.get(entry.getKey());
			if (newItem == null) {
				continue;
			}
			JsonNode oldItem = entry.getValue();

			// isAbstract 从 0 变 1 是 BREAKING
			if ("0".equals(textOrNull(oldItem, "isAbstract")) && "1".equals(textOrNull(newItem, "isAbstract"))) {
				reasons.add("实体类型变为抽象: " + entry.getKey());
			}
		}
	}

	private void checkDataPropertyChanges(ObjectNode oldSnap, ObjectNode newSnap, List<String> reasons) {
		Map<String, JsonNode> oldMap = indexByIri(oldSnap, "dataProperties", "iri");
		Map<String, JsonNode> newMap = indexByIri(newSnap, "dataProperties", "iri");

		for (Map.Entry<String, JsonNode> entry : oldMap.entrySet()) {
			JsonNode newItem = newMap.get(entry.getKey());
			if (newItem == null) {
				continue;
			}
			JsonNode oldItem = entry.getValue();
			String iri = entry.getKey();

			// baseType 变更
			String oldType = textOrNull(oldItem, "baseType");
			String newType = textOrNull(newItem, "baseType");
			if (oldType != null && !oldType.equals(newType)) {
				reasons.add("数据属性数据类型变更: " + iri + " (" + oldType + " → " + newType + ")");
			}

			// isUnique 从 0 变 1（约束增强）
			if ("0".equals(textOrNull(oldItem, "isUnique")) && "1".equals(textOrNull(newItem, "isUnique"))) {
				reasons.add("数据属性唯一约束增强: " + iri);
			}

			// regexPattern 从空变为非空（约束增强）
			String oldRegex = textOrNull(oldItem, "regexPattern");
			String newRegex = textOrNull(newItem, "regexPattern");
			if ((oldRegex == null || oldRegex.isBlank()) && newRegex != null && !newRegex.isBlank()) {
				reasons.add("数据属性新增正则约束: " + iri);
			}

			// domain 收窄（domain 从 null 变为非 null，或 domain 变更）
			String oldDomain = textOrNull(oldItem, "domainEntityIri");
			String newDomain = textOrNull(newItem, "domainEntityIri");
			if (oldDomain == null && newDomain != null) {
				reasons.add("数据属性新增 domain 约束: " + iri);
			}
		}
	}

	private void checkObjectPropertyChanges(ObjectNode oldSnap, ObjectNode newSnap, List<String> reasons) {
		Map<String, JsonNode> oldMap = indexByIri(oldSnap, "objectProperties", "iri");
		Map<String, JsonNode> newMap = indexByIri(newSnap, "objectProperties", "iri");

		for (Map.Entry<String, JsonNode> entry : oldMap.entrySet()) {
			JsonNode newItem = newMap.get(entry.getKey());
			if (newItem == null) {
				continue;
			}
			JsonNode oldItem = entry.getValue();
			String iri = entry.getKey();

			// 语义标记从 0 变 1（约束增强）
			checkFlagEnhancement(oldItem, newItem, "isFunctional", "函数型约束增强", iri, reasons);
			checkFlagEnhancement(oldItem, newItem, "isInverseFunctional", "逆函数型约束增强", iri, reasons);
			checkFlagEnhancement(oldItem, newItem, "isSymmetric", "对称性约束增强", iri, reasons);
			checkFlagEnhancement(oldItem, newItem, "isTransitive", "传递性约束增强", iri, reasons);
		}
	}

	private void checkFlagEnhancement(JsonNode oldItem, JsonNode newItem, String field, String reason, String iri,
			List<String> reasons) {
		if ("0".equals(textOrNull(oldItem, field)) && "1".equals(textOrNull(newItem, field))) {
			reasons.add(reason + ": " + iri);
		}
	}

	private void checkRelationRemovals(ObjectNode oldSnap, ObjectNode newSnap, String arrayField, String key1Field,
			String key2Field, String label, List<String> reasons) {
		Set<String> oldRels = extractRelationSet(oldSnap, arrayField, key1Field, key2Field);
		Set<String> newRels = extractRelationSet(newSnap, arrayField, key1Field, key2Field);

		for (String rel : oldRels) {
			if (!newRels.contains(rel)) {
				reasons.add(label + "删除: " + rel);
			}
		}
	}

	private void checkEnumNarrowing(ObjectNode oldSnap, ObjectNode newSnap, List<String> reasons) {
		Map<String, Set<String>> oldEnums = extractEnumMap(oldSnap);
		Map<String, Set<String>> newEnums = extractEnumMap(newSnap);

		for (Map.Entry<String, Set<String>> entry : oldEnums.entrySet()) {
			Set<String> newValues = newEnums.get(entry.getKey());
			if (newValues == null) {
				continue;
			}
			for (String oldVal : entry.getValue()) {
				if (!newValues.contains(oldVal)) {
					reasons.add("枚举值收窄: " + entry.getKey() + " 删除值 " + oldVal);
				}
			}
		}
	}

	private void checkNewDisjoints(ObjectNode oldSnap, ObjectNode newSnap, List<String> reasons) {
		Set<String> oldDisjoints = extractRelationSet(oldSnap, "entityTypeDisjoints", "iriA", "iriB");
		Set<String> newDisjoints = extractRelationSet(newSnap, "entityTypeDisjoints", "iriA", "iriB");

		for (String rel : newDisjoints) {
			if (!oldDisjoints.contains(rel)) {
				reasons.add("新增不相交关系可能导致既有实例冲突: " + rel);
			}
		}
	}

	private boolean hasAdditions(ObjectNode oldSnap, ObjectNode newSnap) {
		for (String arrayField : List.of("entityTypes", "dataProperties", "objectProperties", "axiomRules",
				"entityTypeHierarchies", "objectPropertyDomains", "objectPropertyRanges", "dataPropertyEnums")) {
			Set<String> oldSet = extractAllKeys(oldSnap, arrayField);
			Set<String> newSet = extractAllKeys(newSnap, arrayField);
			for (String key : newSet) {
				if (!oldSet.contains(key)) {
					return true;
				}
			}
		}
		return false;
	}

	private Set<String> extractAllKeys(ObjectNode snap, String arrayField) {
		Set<String> keys = new HashSet<>();
		JsonNode arr = snap.get(arrayField);
		if (arr == null || !arr.isArray()) {
			return keys;
		}
		for (JsonNode item : arr) {
			if (item.isObject()) {
				keys.add(item.toString());
			}
		}
		return keys;
	}

	private Set<String> extractIriSet(ObjectNode snap, String arrayField, String iriField) {
		Set<String> iris = new HashSet<>();
		JsonNode arr = snap.get(arrayField);
		if (arr == null || !arr.isArray()) {
			return iris;
		}
		for (JsonNode item : arr) {
			String iri = textOrNull(item, iriField);
			if (iri != null) {
				iris.add(iri);
			}
		}
		return iris;
	}

	private Set<String> extractRelationSet(ObjectNode snap, String arrayField, String key1Field, String key2Field) {
		Set<String> rels = new HashSet<>();
		JsonNode arr = snap.get(arrayField);
		if (arr == null || !arr.isArray()) {
			return rels;
		}
		for (JsonNode item : arr) {
			String k1 = textOrNull(item, key1Field);
			String k2 = textOrNull(item, key2Field);
			if (k1 != null && k2 != null) {
				rels.add(k1 + "|" + k2);
			}
		}
		return rels;
	}

	private Map<String, JsonNode> indexByIri(ObjectNode snap, String arrayField, String iriField) {
		Map<String, JsonNode> map = new HashMap<>();
		JsonNode arr = snap.get(arrayField);
		if (arr == null || !arr.isArray()) {
			return map;
		}
		for (JsonNode item : arr) {
			String iri = textOrNull(item, iriField);
			if (iri != null) {
				map.put(iri, item);
			}
		}
		return map;
	}

	private Map<String, Set<String>> extractEnumMap(ObjectNode snap) {
		Map<String, Set<String>> map = new HashMap<>();
		JsonNode arr = snap.get("dataPropertyEnums");
		if (arr == null || !arr.isArray()) {
			return map;
		}
		for (JsonNode item : arr) {
			String propIri = textOrNull(item, "dataPropertyIri");
			String value = textOrNull(item, "value");
			if (propIri != null && value != null) {
				map.computeIfAbsent(propIri, k -> new HashSet<>()).add(value);
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

	/**
	 * 兼容性判定结果。
	 */
	public record CompatibilityResult(String compatibility, List<String> breakingReasons) {
	}

}
