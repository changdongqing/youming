/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.semantic;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Clause 多继承语义测试。
 *
 * @author youming
 */
class ClauseMultipleInheritanceSemanticTests {

	private static final String NS = "http://example.org/standard-ontology#";

	@Test
	void shouldInferClauseAsStructuralElementAndInformationUnitWithoutCollapsingOtherInformationUnits() {
		Model schema = ModelFactory.createDefaultModel();
		Resource structuralElement = schema.createResource(NS + "StructuralElement");
		Resource informationUnit = schema.createResource(NS + "InformationUnit");
		Resource clause = schema.createResource(NS + "Clause");
		Resource additionalInformation = schema.createResource(NS + "AdditionalInformation");
		Resource clauseInstance = schema.createResource(NS + "Clause_5_1");
		Resource noteInstance = schema.createResource(NS + "Note_1");

		schema.add(clause, RDFS.subClassOf, structuralElement);
		schema.add(clause, RDFS.subClassOf, informationUnit);
		schema.add(additionalInformation, RDFS.subClassOf, informationUnit);
		schema.add(clauseInstance, RDF.type, clause);
		schema.add(noteInstance, RDF.type, additionalInformation);

		InfModel inferred = ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), schema);

		assertThat(inferred.contains(clauseInstance, RDF.type, structuralElement)).isTrue();
		assertThat(inferred.contains(clauseInstance, RDF.type, informationUnit)).isTrue();
		assertThat(inferred.contains(noteInstance, RDF.type, informationUnit)).isTrue();
		assertThat(inferred.contains(noteInstance, RDF.type, clause)).isFalse();
		assertThat(inferred.contains(informationUnit, RDFS.subClassOf, clause)).isFalse();
	}

}
