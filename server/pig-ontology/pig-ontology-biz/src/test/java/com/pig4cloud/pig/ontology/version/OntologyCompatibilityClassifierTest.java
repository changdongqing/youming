/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pig4cloud.pig.ontology.version.diff.OntologyCompatibilityClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link OntologyCompatibilityClassifier} 单元测试。
 *
 * @author youming
 */
@DisplayName("兼容性分类器测试")
class OntologyCompatibilityClassifierTest {

	private OntologyCompatibilityClassifier classifier;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		classifier = new OntologyCompatibilityClassifier(objectMapper);
	}

	@Test
	@DisplayName("IRI删除判为BREAKING")
	void testIriRemovalIsBreaking() {
		ObjectNode oldSnap = createSnapshotWithEntityType("http://example.org/test#TypeA");
		ObjectNode newSnap = createEmptySnapshot();

		var result = classifier.classify(oldSnap, newSnap);
		assertEquals(OntologyCompatibilityClassifier.BREAKING, result.compatibility());
		assertFalse(result.breakingReasons().isEmpty());
		assertTrue(result.breakingReasons().stream().anyMatch(r -> r.contains("TypeA")));
	}

	@Test
	@DisplayName("新增实体类型判为BACKWARD_COMPATIBLE")
	void testAdditionIsCompatible() {
		ObjectNode oldSnap = createEmptySnapshot();
		ObjectNode newSnap = createSnapshotWithEntityType("http://example.org/test#TypeA");

		var result = classifier.classify(oldSnap, newSnap);
		assertEquals(OntologyCompatibilityClassifier.BACKWARD_COMPATIBLE, result.compatibility());
	}

	@Test
	@DisplayName("无变化判为PATCH_ONLY")
	void testNoChangeIsPatchOnly() {
		ObjectNode snap1 = createSnapshotWithEntityType("http://example.org/test#TypeA");
		ObjectNode snap2 = createSnapshotWithEntityType("http://example.org/test#TypeA");

		var result = classifier.classify(snap1, snap2);
		assertEquals(OntologyCompatibilityClassifier.PATCH_ONLY, result.compatibility());
	}

	@Test
	@DisplayName("数据类型变更判为BREAKING")
	void testDataTypeChangeIsBreaking() {
		ObjectNode oldSnap = createSnapshotWithDataProperty("http://example.org/test#prop", "STRING");
		ObjectNode newSnap = createSnapshotWithDataProperty("http://example.org/test#prop", "INTEGER");

		var result = classifier.classify(oldSnap, newSnap);
		assertEquals(OntologyCompatibilityClassifier.BREAKING, result.compatibility());
	}

	@Test
	@DisplayName("唯一约束增强判为BREAKING")
	void testUniqueConstraintEnhancedIsBreaking() {
		ObjectNode oldSnap = createSnapshotWithDataProperty("http://example.org/test#prop", "STRING", "0");
		ObjectNode newSnap = createSnapshotWithDataProperty("http://example.org/test#prop", "STRING", "1");

		var result = classifier.classify(oldSnap, newSnap);
		assertEquals(OntologyCompatibilityClassifier.BREAKING, result.compatibility());
	}

	@Test
	@DisplayName("新增不相交关系判为BREAKING")
	void testNewDisjointIsBreaking() {
		ObjectNode oldSnap = createSnapshotWithTwoTypes("http://example.org/test#A", "http://example.org/test#B");
		ObjectNode newSnap = createSnapshotWithTwoTypesAndDisjoint("http://example.org/test#A",
				"http://example.org/test#B");

		var result = classifier.classify(oldSnap, newSnap);
		assertEquals(OntologyCompatibilityClassifier.BREAKING, result.compatibility());
	}

	// ==================== 辅助方法 ====================

	private ObjectNode createEmptySnapshot() {
		ObjectNode root = objectMapper.createObjectNode();
		root.set("entityTypes", objectMapper.createArrayNode());
		root.set("dataProperties", objectMapper.createArrayNode());
		root.set("objectProperties", objectMapper.createArrayNode());
		root.set("axiomRules", objectMapper.createArrayNode());
		root.set("entityTypeHierarchies", objectMapper.createArrayNode());
		root.set("entityTypeDisjoints", objectMapper.createArrayNode());
		root.set("objectPropertyDomains", objectMapper.createArrayNode());
		root.set("objectPropertyRanges", objectMapper.createArrayNode());
		root.set("dataPropertyEnums", objectMapper.createArrayNode());
		return root;
	}

	private ObjectNode createSnapshotWithEntityType(String iri) {
		ObjectNode root = createEmptySnapshot();
		ArrayNode types = root.withArray("entityTypes");
		ObjectNode type = objectMapper.createObjectNode();
		type.put("iri", iri);
		type.put("name", iri.substring(iri.lastIndexOf('#') + 1));
		types.add(type);
		return root;
	}

	private ObjectNode createSnapshotWithTwoTypes(String iri1, String iri2) {
		ObjectNode root = createEmptySnapshot();
		ArrayNode types = root.withArray("entityTypes");
		ObjectNode t1 = objectMapper.createObjectNode();
		t1.put("iri", iri1);
		t1.put("name", "A");
		types.add(t1);
		ObjectNode t2 = objectMapper.createObjectNode();
		t2.put("iri", iri2);
		t2.put("name", "B");
		types.add(t2);
		return root;
	}

	private ObjectNode createSnapshotWithTwoTypesAndDisjoint(String iri1, String iri2) {
		ObjectNode root = createSnapshotWithTwoTypes(iri1, iri2);
		ArrayNode disjoints = root.withArray("entityTypeDisjoints");
		ObjectNode d = objectMapper.createObjectNode();
		d.put("iriA", iri1);
		d.put("iriB", iri2);
		disjoints.add(d);
		return root;
	}

	private ObjectNode createSnapshotWithDataProperty(String iri, String baseType) {
		return createSnapshotWithDataProperty(iri, baseType, "0");
	}

	private ObjectNode createSnapshotWithDataProperty(String iri, String baseType, String isUnique) {
		ObjectNode root = createEmptySnapshot();
		ArrayNode props = root.withArray("dataProperties");
		ObjectNode prop = objectMapper.createObjectNode();
		prop.put("iri", iri);
		prop.put("baseType", baseType);
		prop.put("isUnique", isUnique);
		props.add(prop);
		return root;
	}

}
