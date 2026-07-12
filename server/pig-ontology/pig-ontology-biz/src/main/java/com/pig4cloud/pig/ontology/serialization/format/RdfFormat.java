/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.format;

/**
 * RDF 序列化格式枚举。
 *
 * @author youming
 */
public enum RdfFormat {

	TURTLE("Turtle", "ttl"),
	JSON_LD("JSON-LD", "jsonld"),
	RDF_XML("RDF/XML", "rdf"),
	N_TRIPLES("N-Triples", "nt");

	private final String displayName;

	private final String fileExtension;

	RdfFormat(String displayName, String fileExtension) {
		this.displayName = displayName;
		this.fileExtension = fileExtension;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getFileExtension() {
		return fileExtension;
	}

}
