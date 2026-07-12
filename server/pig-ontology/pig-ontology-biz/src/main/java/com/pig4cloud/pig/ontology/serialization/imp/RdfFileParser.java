/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.imp;

import com.pig4cloud.pig.ontology.serialization.export.NamespacePrefixResolver;
import com.pig4cloud.pig.ontology.serialization.format.RdfFormat;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.springframework.stereotype.Component;

import java.io.StringReader;

/**
 * RDF 文件解析器。
 * <p>
 * 使用 Jena RDFParser 解析上传的 RDF 文件为 Model。
 * 支持自动格式检测或指定格式。
 * </p>
 *
 * @author youming
 */
@Component
@RequiredArgsConstructor
public class RdfFileParser {

	private final NamespacePrefixResolver prefixResolver;

	/**
	 * 解析 RDF 文件内容为 Model。
	 * @param content RDF 文件内容
	 * @param format 指定格式（null 则默认 Turtle）
	 * @return 解析后的 Jena Model（含前缀映射）
	 */
	public Model parse(String content, RdfFormat format) {
		Model model = ModelFactory.createDefaultModel();

		// 注册已知前缀（帮助解析器解析无前缀声明的缩写IRI）
		prefixResolver.registerPrefixes(model);

		Lang lang = format != null ? parseLang(format) : Lang.TURTLE;

		RDFParser.create()
			.lang(lang)
			.source(new StringReader(content))
			.parse(model);

		return model;
	}

	private Lang parseLang(RdfFormat format) {
		switch (format) {
			case TURTLE:
				return Lang.TURTLE;
			case JSON_LD:
				return Lang.JSONLD;
			case RDF_XML:
				return Lang.RDFXML;
			case N_TRIPLES:
				return Lang.NTRIPLES;
			default:
				return Lang.TURTLE;
		}
	}

}
