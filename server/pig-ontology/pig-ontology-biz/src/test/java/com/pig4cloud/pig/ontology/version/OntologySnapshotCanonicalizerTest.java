/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pig4cloud.pig.ontology.version.snapshot.OntologySnapshotCanonicalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link OntologySnapshotCanonicalizer} 单元测试。
 *
 * @author youming
 */
@DisplayName("快照规范化处理器测试")
class OntologySnapshotCanonicalizerTest {

	private OntologySnapshotCanonicalizer canonicalizer;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		canonicalizer = new OntologySnapshotCanonicalizer(objectMapper);
	}

	@Test
	@DisplayName("审计字段被正确移除")
	void testAuditFieldsRemoved() throws Exception {
		ObjectNode root = objectMapper.createObjectNode();
		ArrayNode entityTypes = objectMapper.createArrayNode();

		ObjectNode type = objectMapper.createObjectNode();
		type.put("id", 940001);
		type.put("iri", "http://example.org/test#Type1");
		type.put("name", "Type1");
		type.put("createBy", "admin");
		type.put("createTime", "2026-01-01");
		type.put("sortOrder", 10);
		entityTypes.add(type);

		root.set("entityTypes", entityTypes);

		ObjectNode result = canonicalizer.canonicalize(objectMapper.writeValueAsString(root));
		assertNull(result.get("entityTypes").get(0).get("id"));
		assertNull(result.get("entityTypes").get(0).get("createBy"));
		assertNull(result.get("entityTypes").get(0).get("createTime"));
		assertNull(result.get("entityTypes").get(0).get("sortOrder"));
		assertNotNull(result.get("entityTypes").get(0).get("iri"));
	}

	@Test
	@DisplayName("ID替换为IRI")
	void testIdReplacedWithIri() throws Exception {
		ObjectNode root = objectMapper.createObjectNode();

		// entityTypes 数组
		ArrayNode entityTypes = objectMapper.createArrayNode();
		ObjectNode type1 = objectMapper.createObjectNode();
		type1.put("id", 100);
		type1.put("iri", "http://example.org/test#Type1");
		entityTypes.add(type1);
		ObjectNode type2 = objectMapper.createObjectNode();
		type2.put("id", 200);
		type2.put("iri", "http://example.org/test#Type2");
		entityTypes.add(type2);
		root.set("entityTypes", entityTypes);

		// hierarchies 数组
		ArrayNode hierarchies = objectMapper.createArrayNode();
		ObjectNode h = objectMapper.createObjectNode();
		h.put("parentId", 100);
		h.put("childId", 200);
		hierarchies.add(h);
		root.set("entityTypeHierarchies", hierarchies);

		ObjectNode result = canonicalizer.canonicalize(objectMapper.writeValueAsString(root));

		// parentId 和 childId 应被替换为 parentIri 和 childIri
		assertNull(result.get("entityTypeHierarchies").get(0).get("parentId"));
		assertNull(result.get("entityTypeHierarchies").get(0).get("childId"));
		assertEquals("http://example.org/test#Type1",
				result.get("entityTypeHierarchies").get(0).get("parentIri").asText());
		assertEquals("http://example.org/test#Type2",
				result.get("entityTypeHierarchies").get(0).get("childIri").asText());
	}

	@Test
	@DisplayName("数组按IRI稳定排序")
	void testStableSorting() throws Exception {
		ObjectNode root = objectMapper.createObjectNode();
		ArrayNode entityTypes = objectMapper.createArrayNode();

		// 故意打乱顺序
		ObjectNode type3 = objectMapper.createObjectNode();
		type3.put("id", 3);
		type3.put("iri", "http://example.org/test#C");
		entityTypes.add(type3);
		ObjectNode type1 = objectMapper.createObjectNode();
		type1.put("id", 1);
		type1.put("iri", "http://example.org/test#A");
		entityTypes.add(type1);
		ObjectNode type2 = objectMapper.createObjectNode();
		type2.put("id", 2);
		type2.put("iri", "http://example.org/test#B");
		entityTypes.add(type2);

		root.set("entityTypes", entityTypes);

		ObjectNode result = canonicalizer.canonicalize(objectMapper.writeValueAsString(root));

		assertEquals("http://example.org/test#A", result.get("entityTypes").get(0).get("iri").asText());
		assertEquals("http://example.org/test#B", result.get("entityTypes").get(1).get("iri").asText());
		assertEquals("http://example.org/test#C", result.get("entityTypes").get(2).get("iri").asText());
	}

	@Test
	@DisplayName("相同数据不同输入顺序产生相同hash")
	void testHashStability() throws Exception {
		// 构建两个相同数据但不同顺序的快照
		String snapshot1 = "{\"entityTypes\":[" +
				"{\"id\":1,\"iri\":\"http://example.org/test#A\"}," +
				"{\"id\":2,\"iri\":\"http://example.org/test#B\"}" +
				"]}";
		String snapshot2 = "{\"entityTypes\":[" +
				"{\"id\":2,\"iri\":\"http://example.org/test#B\"}," +
				"{\"id\":1,\"iri\":\"http://example.org/test#A\"}" +
				"]}";

		String hash1 = canonicalizer.canonicalizeAndHash(snapshot1);
		String hash2 = canonicalizer.canonicalizeAndHash(snapshot2);

		assertEquals(hash1, hash2, "相同数据不同顺序应产生相同hash");
	}

	@Test
	@DisplayName("hash为64位十六进制")
	void testHashFormat() throws Exception {
		String snapshot = "{\"entityTypes\":[{\"id\":1,\"iri\":\"http://example.org/test#A\"}]}";
		String hash = canonicalizer.canonicalizeAndHash(snapshot);
		assertNotNull(hash);
		assertEquals(64, hash.length(), "SHA-256 hash应为64位十六进制");
		assertTrue(hash.matches("[0-9a-f]{64}"), "hash应只包含小写十六进制字符");
	}

}
