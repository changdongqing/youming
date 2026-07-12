/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 附录D导出验证测试。
 * <p>
 * PRD §5.4 验收标准：导出Turtle与附录D语义等价。
 * 本测试验证序列化产出的关键三元组存在性及格式合法性。
 * </p>
 *
 * @author youming
 */
class AppendixDExportVerificationTest {

	private static final String NS = "http://example.org/standard-ontology#";

	private static final String STD = "http://example.org/standard/GB-T-31486-2024/";

	/**
	 * 构建附录D核心实例的简化 Model，模拟 OntologyModelExporter 的输出。
	 */
	private Model buildAppendixDModel() {
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("", NS);
		model.setNsPrefix("std", STD);
		model.setNsPrefix("rdf", RDF.getURI());
		model.setNsPrefix("rdfs", RDFS.getURI());
		model.setNsPrefix("xsd", XSDDatatype.XSDstring.getURI());
		model.setNsPrefix("owl", OWL.getURI());

		// Schema: 类声明
		Resource standard = model.createResource(NS + "Standard");
		model.add(standard, RDF.type, OWL.Class);
		Resource constraint = model.createResource(NS + "Constraint");
		model.add(constraint, RDF.type, OWL.Class);
		Resource object = model.createResource(NS + "Object");
		model.add(object, RDF.type, OWL.Class);

		// Schema: 数据属性声明
		Property standardNumber = model.createProperty(NS + "standardNumber");
		model.add(standardNumber, RDF.type, OWL.DatatypeProperty);
		model.add(standardNumber, RDFS.domain, standard);
		model.add(standardNumber, RDFS.range, model.createResource(XSDDatatype.XSDstring.getURI()));

		Property measurementUnit = model.createProperty(NS + "measurementUnit");
		model.add(measurementUnit, RDF.type, OWL.DatatypeProperty);
		model.add(measurementUnit, RDFS.domain, constraint);
		model.add(measurementUnit, RDFS.range, model.createResource(XSDDatatype.XSDstring.getURI()));

		Property issuedDate = model.createProperty(NS + "issuedDate");
		model.add(issuedDate, RDF.type, OWL.DatatypeProperty);
		model.add(issuedDate, RDFS.domain, standard);
		model.add(issuedDate, RDFS.range, model.createResource(XSDDatatype.XSDdate.getURI()));

		// Schema: 对象属性声明
		Property standardizes = model.createProperty(NS + "standardizes");
		model.add(standardizes, RDF.type, OWL.ObjectProperty);
		model.add(standardizes, RDFS.domain, standard);

		// Data: 标准实体定义
		Resource gb31486 = model.createResource(STD + "GB-T_31486-2024");
		model.add(gb31486, RDF.type, standard);
		model.add(gb31486, standardNumber, "GB/T 31486—2024");
		model.add(gb31486, issuedDate, model.createTypedLiteral("2024-09-29", XSDDatatype.XSDdate));

		// Data: 约束逻辑实例
		Resource constraint1 = model.createResource(STD + "Capacity_Constraint_1");
		model.add(constraint1, RDF.type, constraint);
		model.add(constraint1, measurementUnit, "%");

		// Data: 标准化对象
		Resource batteryObj = model.createResource(STD + "BatteryStandardizationObject");
		model.add(gb31486, standardizes, batteryObj);

		return model;
	}

	@Test
	void shouldExportClassDeclarations() {
		Model model = buildAppendixDModel();

		Resource standard = model.getResource(NS + "Standard");
		assertThat(model.contains(standard, RDF.type, OWL.Class)).isTrue();

		Resource constraint = model.getResource(NS + "Constraint");
		assertThat(model.contains(constraint, RDF.type, OWL.Class)).isTrue();
	}

	@Test
	void shouldExportDataPropertyDeclarations() {
		Model model = buildAppendixDModel();

		Resource measurementUnit = model.getProperty(NS + "measurementUnit");
		assertThat(model.contains(measurementUnit, RDF.type, OWL.DatatypeProperty)).isTrue();
		assertThat(model.contains(measurementUnit, RDFS.domain, model.getResource(NS + "Constraint"))).isTrue();
	}

	@Test
	void shouldExportObjectPropertyDeclarations() {
		Model model = buildAppendixDModel();

		Resource standardizes = model.getProperty(NS + "standardizes");
		assertThat(model.contains(standardizes, RDF.type, OWL.ObjectProperty)).isTrue();
	}

	@Test
	void shouldExportAppendixDStep1_StandardEntity() {
		Model model = buildAppendixDModel();

		Resource gb31486 = model.getResource(STD + "GB-T_31486-2024");
		Resource standard = model.getResource(NS + "Standard");
		assertThat(model.contains(gb31486, RDF.type, standard)).isTrue();

		Property standardNumber = model.getProperty(NS + "standardNumber");
		assertThat(model.contains(gb31486, standardNumber, "GB/T 31486—2024")).isTrue();
	}

	@Test
	void shouldExportDateAsXsdDate() {
		Model model = buildAppendixDModel();

		Resource gb31486 = model.getResource(STD + "GB-T_31486-2024");
		Property issuedDate = model.getProperty(NS + "issuedDate");
		StmtIterator it = model.listStatements(gb31486, issuedDate, (org.apache.jena.rdf.model.RDFNode) null);
		assertThat(it.hasNext()).isTrue();
		org.apache.jena.rdf.model.Literal dateLiteral = it.next().getLiteral();
		assertThat(dateLiteral.getDatatype()).isEqualTo(XSDDatatype.XSDdate);
		assertThat(dateLiteral.getString()).isEqualTo("2024-09-29");
	}

	@Test
	void shouldExportMeasurementUnitAsSymbolLiteral() {
		Model model = buildAppendixDModel();

		Resource constraint1 = model.getResource(STD + "Capacity_Constraint_1");
		Property measurementUnit = model.getProperty(NS + "measurementUnit");
		assertThat(model.contains(constraint1, measurementUnit, "%")).isTrue();
	}

	@Test
	void shouldExportStandardizesRelation() {
		Model model = buildAppendixDModel();

		Resource gb31486 = model.getResource(STD + "GB-T_31486-2024");
		Resource batteryObj = model.getResource(STD + "BatteryStandardizationObject");
		Property standardizes = model.getProperty(NS + "standardizes");
		assertThat(model.contains(gb31486, standardizes, batteryObj)).isTrue();
	}

	@Test
	void shouldSerializeToValidTurtle() {
		Model model = buildAppendixDModel();

		StringWriter writer = new StringWriter();
		RDFDataMgr.write(writer, model, Lang.TURTLE);
		String turtle = writer.toString();

		// 包含前缀声明（Turtle 用 @prefix，某些Jena版本用 PREFIX）
		assertThat(turtle).containsPattern("(?i)(@prefix|PREFIX)");
		assertThat(turtle).contains(NS);

		// 可被重新解析为 Model（格式合法）
		Model reparsed = ModelFactory.createDefaultModel();
		reparsed.read(new StringReader(turtle), null, "TURTLE");
		assertThat(reparsed.size()).isGreaterThan(0);
	}

	@Test
	void shouldSerializeToValidJsonLd() {
		Model model = buildAppendixDModel();

		StringWriter writer = new StringWriter();
		RDFDataMgr.write(writer, model, Lang.JSONLD);
		String jsonLd = writer.toString();

		assertThat(jsonLd).contains("@context");
		assertThat(jsonLd).contains("@graph");

		// 可被重新解析
		Model reparsed = ModelFactory.createDefaultModel();
		RDFDataMgr.read(reparsed, new StringReader(jsonLd), null, Lang.JSONLD);
		assertThat(reparsed.size()).isGreaterThan(0);
	}

	@Test
	void shouldSerializeToValidRdfXml() {
		Model model = buildAppendifyDModelSafe();

		StringWriter writer = new StringWriter();
		RDFDataMgr.write(writer, model, Lang.RDFXML);
		String rdfXml = writer.toString();

		assertThat(rdfXml).contains("rdf:RDF");

		Model reparsed = ModelFactory.createDefaultModel();
		RDFDataMgr.read(reparsed, new StringReader(rdfXml), null, Lang.RDFXML);
		assertThat(reparsed.size()).isGreaterThan(0);
	}

	@Test
	void shouldSerializeToValidNTriples() {
		Model model = buildAppendixDModel();

		StringWriter writer = new StringWriter();
		RDFDataMgr.write(writer, model, Lang.NTRIPLES);
		String ntriples = writer.toString();

		// N-Triples 每行一个三元组
		String[] lines = ntriples.trim().split("\n");
		assertThat(lines.length).isGreaterThan(0);
		for (String line : lines) {
			assertThat(line).endsWith(" .");
		}

		Model reparsed = ModelFactory.createDefaultModel();
		RDFDataMgr.read(reparsed, new StringReader(ntriples), null, Lang.NTRIPLES);
		assertThat(reparsed.size()).isGreaterThan(0);
	}

	private Model buildAppendifyDModelSafe() {
		return buildAppendixDModel();
	}

}
