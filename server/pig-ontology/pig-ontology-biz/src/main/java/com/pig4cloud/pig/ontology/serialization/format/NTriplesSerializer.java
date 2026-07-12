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
 * N-Triples 格式序列化器。
 *
 * @author youming
 */
@Component
public class NTriplesSerializer implements RdfSerializer {

	@Override
	public String serialize(Model model) {
		StringWriter writer = new StringWriter();
		RDFDataMgr.write(writer, model, Lang.NTRIPLES);
		return writer.toString();
	}

	@Override
	public void write(Model model, OutputStream out) {
		RDFDataMgr.write(out, model, Lang.NTRIPLES);
	}

	@Override
	public RdfFormat getFormat() {
		return RdfFormat.N_TRIPLES;
	}

}
