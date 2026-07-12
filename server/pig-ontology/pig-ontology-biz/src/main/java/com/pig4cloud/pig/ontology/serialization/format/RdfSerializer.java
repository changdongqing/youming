/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.format;

import org.apache.jena.rdf.model.Model;

import java.io.OutputStream;

/**
 * RDF 序列化接口。
 *
 * @author youming
 */
public interface RdfSerializer {

	/**
	 * 将 Jena Model 序列化为字符串。
	 * @param model Jena Model
	 * @return RDF 文本
	 */
	String serialize(Model model);

	/**
	 * 将 Jena Model 序列化并写入输出流。
	 * @param model Jena Model
	 * @param out 输出流
	 */
	void write(Model model, OutputStream out);

	/**
	 * 支持的格式。
	 * @return 格式枚举
	 */
	RdfFormat getFormat();

}
