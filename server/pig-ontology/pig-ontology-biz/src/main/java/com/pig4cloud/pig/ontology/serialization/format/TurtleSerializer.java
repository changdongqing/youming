/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.format;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.io.StringWriter;

/**
 * Turtle 格式序列化器。
 * <p>
 * 国标附录D使用的 OWL Turtle 格式，是平台默认导出格式。
 * </p>
 *
 * @author youming
 */
@Component
public class TurtleSerializer implements RdfSerializer {

	@Override
	public String serialize(Model model) {
		StringWriter writer = new StringWriter();
		RDFDataMgr.write(writer, model, Lang.TURTLE);
		return writer.toString();
	}

	@Override
	public void write(Model model, OutputStream out) {
		RDFDataMgr.write(out, model, Lang.TURTLE);
	}

	@Override
	public RdfFormat getFormat() {
		return RdfFormat.TURTLE;
	}

}
