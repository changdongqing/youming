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
 * JSON-LD 格式序列化器。
 *
 * @author youming
 */
@Component
public class JsonLdSerializer implements RdfSerializer {

	@Override
	public String serialize(Model model) {
		StringWriter writer = new StringWriter();
		RDFDataMgr.write(writer, model, Lang.JSONLD);
		return writer.toString();
	}

	@Override
	public void write(Model model, OutputStream out) {
		RDFDataMgr.write(out, model, Lang.JSONLD);
	}

	@Override
	public RdfFormat getFormat() {
		return RdfFormat.JSON_LD;
	}

}
