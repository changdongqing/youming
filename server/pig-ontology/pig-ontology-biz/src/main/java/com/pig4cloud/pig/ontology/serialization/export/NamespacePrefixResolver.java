/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.export;

import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 命名空间前缀映射解析器。
 * <p>
 * 从 ont_namespace 表加载所有命名空间，注册到 Jena Model 的前缀映射。
 * 同时注册标准前缀（rdf/rdfs/xsd/owl/sh）。
 * </p>
 *
 * @author youming
 */
@Component
@RequiredArgsConstructor
public class NamespacePrefixResolver {

	private final OntNamespaceMapper namespaceMapper;

	/** 标准前缀常量（国标固定，不从数据库加载） */
	private static final Map<String, String> STANDARD_PREFIXES = Map.of(
		"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
		"rdfs", "http://www.w3.org/2000/01/rdf-schema#",
		"xsd", "http://www.w3.org/2001/XMLSchema#",
		"owl", "http://www.w3.org/2002/07/owl#",
		"sh", "http://www.w3.org/ns/shacl#"
	);

	/**
	 * 注册前缀映射到 Model。
	 * @param model Jena Model
	 */
	public void registerPrefixes(Model model) {
		// 1. 注册标准前缀
		STANDARD_PREFIXES.forEach(model::setNsPrefix);

		// 2. 注册用户命名空间（从数据库加载）
		List<OntNamespace> namespaces = namespaceMapper.selectList(null);
		for (OntNamespace ns : namespaces) {
			if (ns.getPrefix() != null && ns.getUri() != null) {
				model.setNsPrefix(ns.getPrefix(), ns.getUri());
			}
		}
	}

	/**
	 * 获取所有命名空间（供导入解析器使用）。
	 * @return prefix -> uri 映射
	 */
	public Map<String, String> getAllPrefixes() {
		Map<String, String> prefixes = new HashMap<>(STANDARD_PREFIXES);
		List<OntNamespace> namespaces = namespaceMapper.selectList(null);
		for (OntNamespace ns : namespaces) {
			if (ns.getPrefix() != null && ns.getUri() != null) {
				prefixes.put(ns.getPrefix(), ns.getUri());
			}
		}
		return prefixes;
	}

}
