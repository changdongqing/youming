/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization;

import com.pig4cloud.pig.ontology.serialization.format.RdfFormat;
import com.pig4cloud.pig.ontology.serialization.format.RdfSerializer;
import com.pig4cloud.pig.ontology.serialization.format.RdfSerializerRegistry;
import com.pig4cloud.pig.ontology.serialization.format.TurtleSerializer;
import com.pig4cloud.pig.ontology.serialization.format.JsonLdSerializer;
import com.pig4cloud.pig.ontology.serialization.format.RdfXmlSerializer;
import com.pig4cloud.pig.ontology.serialization.format.NTriplesSerializer;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RDF 格式序列化器测试。
 *
 * @author youming
 */
class RdfSerializerTest {

	@Test
	void shouldSerializeTurtle() {
		Model model = buildTestModel();
		RdfSerializer serializer = new TurtleSerializer();

		String result = serializer.serialize(model);

		assertThat(result).containsPattern("(?i)(@prefix|PREFIX)");
		assertThat(result).contains("owl:Class");
	}

	@Test
	void shouldSerializeJsonLd() {
		Model model = buildTestModel();
		RdfSerializer serializer = new JsonLdSerializer();

		String result = serializer.serialize(model);

		assertThat(result).contains("@context");
	}

	@Test
	void shouldSerializeRdfXml() {
		Model model = buildTestModel();
		RdfSerializer serializer = new RdfXmlSerializer();

		String result = serializer.serialize(model);

		assertThat(result).contains("rdf:RDF");
	}

	@Test
	void shouldSerializeNTriples() {
		Model model = buildTestModel();
		RdfSerializer serializer = new NTriplesSerializer();

		String result = serializer.serialize(model);

		assertThat(result).contains(" .");
	}

	@Test
	void shouldRegisterAllSerializers() {
		RdfSerializerRegistry registry = new RdfSerializerRegistry(List.of(
			new TurtleSerializer(),
			new JsonLdSerializer(),
			new RdfXmlSerializer(),
			new NTriplesSerializer()));

		assertThat(registry.getSerializer(RdfFormat.TURTLE)).isNotNull();
		assertThat(registry.getSerializer(RdfFormat.JSON_LD)).isNotNull();
		assertThat(registry.getSerializer(RdfFormat.RDF_XML)).isNotNull();
		assertThat(registry.getSerializer(RdfFormat.N_TRIPLES)).isNotNull();
	}

	@Test
	void shouldThrowForUnsupportedFormat() {
		RdfSerializerRegistry registry = new RdfSerializerRegistry(List.of(new TurtleSerializer()));

		assertThatThrownBy(() -> registry.getSerializer(RdfFormat.JSON_LD))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private Model buildTestModel() {
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("ex", "http://example.org/");
		model.setNsPrefix("owl", org.apache.jena.vocabulary.OWL.getURI());
		org.apache.jena.rdf.model.Resource cls = model.createResource("http://example.org/Test");
		model.add(cls, RDF.type, org.apache.jena.vocabulary.OWL.Class);
		model.add(cls, RDFS.label, "Test");
		return model;
	}

}
