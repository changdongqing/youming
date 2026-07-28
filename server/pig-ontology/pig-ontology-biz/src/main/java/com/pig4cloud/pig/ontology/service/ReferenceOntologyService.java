package com.pig4cloud.pig.ontology.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pig4cloud.pig.ontology.api.vo.ReferenceAnnotationPropertyVO;
import com.pig4cloud.pig.ontology.api.vo.ReferenceClassVO;
import com.pig4cloud.pig.ontology.api.vo.ReferenceOntologyVO;
import com.pig4cloud.pig.ontology.api.vo.ReferenceUnitVO;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 参考本体库浏览 Service（FR-7，AC-7.1/7.4）
 * <p>
 * Jena 解析 TTL + Caffeine 缓存 Model（复用 DD3 InheritedViewService 缓存范式）。
 * 三套本体：qudt（单位）/ brick（类）/ cco（注释属性）。首次解析 ~5s，缓存后查询 <50ms（AC-7.4）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Service
public class ReferenceOntologyService {

	/** 参考本体库根目录（相对 classpath 或绝对路径） */
	@Value("${pig.ontology.reference-base:docs/ontology/参考开源本体库}")
	private String referenceBase;

	/** Caffeine 缓存：key=本体标识（qudt/brick/cco），value=Jena Model，TTL 30min（AC-7.4） */
	private final Cache<String, Model> modelCache = Caffeine.newBuilder()
		.expireAfterWrite(30, TimeUnit.MINUTES)
		.maximumSize(3)
		.build();

	/** 本体文件映射 */
	private static final Map<String, String> ONT_FILES = Map.of(
		"qudt", "qudt/qudt-all.ttl",
		"brick", "brick/Brick-only.ttl",
		"cco", "CommonCoreOntologies/CommonCoreOntologiesMerged.ttl");

	/** 本体显示名 */
	private static final Map<String, String> ONT_NAMES = Map.of(
		"qudt", "QUDT（单位/量纲）",
		"brick", "Brick（建筑领域）",
		"cco", "Common Core Ontologies（中层本体）");

	/**
	 * 列出三套参考本体元信息（AC-7.1）。
	 */
	public List<ReferenceOntologyVO> ontologies() {
		List<ReferenceOntologyVO> list = new ArrayList<>();
		for (String key : new String[] { "qudt", "brick", "cco" }) {
			ReferenceOntologyVO vo = new ReferenceOntologyVO();
			vo.setKey(key);
			vo.setName(ONT_NAMES.get(key));
			vo.setFile(ONT_FILES.get(key));
			vo.setItemCount(countItems(key));
			list.add(vo);
		}
		return list;
	}

	/**
	 * QUDT 单位分页（AC-7.1）。SPARQL 查 qudt:Unit 实体的 label/symbol/系数/量纲。
	 * @param ont 本体标识（仅 qudt 有效）
	 * @param page 分页对象
	 * @param quantityKindIri 量纲 IRI 过滤（可选）
	 * @param keyword 关键字过滤 label（可选）
	 */
	public IPage<ReferenceUnitVO> pageUnits(String ont, Page page, String quantityKindIri, String keyword) {
		Model model = getModel("qudt");
		StringBuilder sparql = new StringBuilder();
		sparql.append("PREFIX qudt: <http://qudt.org/schema/qudt/> ");
		sparql.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ");
		sparql.append("SELECT ?iri ?label ?symbol ?mul ?mulSn ?ucum ?qk ?desc WHERE { ");
		sparql.append("  ?iri a qudt:Unit . ");
		sparql.append("  OPTIONAL { ?iri rdfs:label ?label . FILTER(LANG(?label) = '' || LANGMATCHES(LANG(?label), 'en')) } ");
		sparql.append("  OPTIONAL { ?iri qudt:symbol ?symbol } ");
		sparql.append("  OPTIONAL { ?iri qudt:conversionMultiplier ?mul } ");
		sparql.append("  OPTIONAL { ?iri qudt:conversionMultiplierSN ?mulSn } ");
		sparql.append("  OPTIONAL { ?iri qudt:ucumCode ?ucum } ");
		sparql.append("  OPTIONAL { ?iri qudt:hasQuantityKind ?qk } ");
		sparql.append("  OPTIONAL { ?iri qudt:plainTextDescription ?desc } ");
		if (StrUtil.isNotBlank(quantityKindIri)) {
			sparql.append("  FILTER(STR(?qk) = '").append(escapeSparql(quantityKindIri)).append("') ");
		}
		if (StrUtil.isNotBlank(keyword)) {
			sparql.append("  FILTER(CONTAINS(LCASE(COALESCE(STR(?label), '')), LCASE('")
				.append(escapeSparql(keyword)).append("'))) ");
		}
		sparql.append("} ORDER BY ?label");
		return executeUnitQuery(model, sparql.toString(), page);
	}

	/**
	 * Brick 类分页（AC-7.1）。SPARQL 查 owl:Class 实体的 label/definition/subClassOf/deprecated。
	 * @param ont 本体标识（仅 brick 有效）
	 * @param page 分页对象
	 * @param keyword 关键字过滤 label（可选）
	 * @param parentIri 父类 IRI 过滤（可选）
	 */
	public IPage<ReferenceClassVO> pageClasses(String ont, Page page, String keyword, String parentIri) {
		Model model = getModel("brick");
		StringBuilder sparql = new StringBuilder();
		sparql.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ");
		sparql.append("PREFIX owl: <http://www.w3.org/2002/07/owl#> ");
		sparql.append("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> ");
		sparql.append("SELECT ?iri ?label ?def ?parent ?dep WHERE { ");
		sparql.append("  ?iri a owl:Class . ");
		sparql.append("  FILTER(STRSTARTS(STR(?iri), 'https://brickschema.org/schema/Brick#')) ");
		sparql.append("  OPTIONAL { ?iri rdfs:label ?label } ");
		sparql.append("  OPTIONAL { ?iri skos:definition ?def } ");
		sparql.append("  OPTIONAL { ?iri rdfs:subClassOf ?parent . FILTER(STRSTARTS(STR(?parent), 'https://brickschema.org/schema/Brick#')) } ");
		sparql.append("  OPTIONAL { ?iri <http://www.w3.org/2002/07/owl#deprecated> ?dep } ");
		if (StrUtil.isNotBlank(keyword)) {
			sparql.append("  FILTER(CONTAINS(LCASE(COALESCE(STR(?label), STR(?iri))), LCASE('")
				.append(escapeSparql(keyword)).append("'))) ");
		}
		if (StrUtil.isNotBlank(parentIri)) {
			sparql.append("  FILTER(STR(?parent) = '").append(escapeSparql(parentIri)).append("') ");
		}
		sparql.append("} ORDER BY ?label");
		return executeClassQuery(model, sparql.toString(), page);
	}

	/**
	 * CCO 注释属性分页（AC-7.1）。SPARQL 查 owl:AnnotationProperty 实体的 label/definition。
	 * @param ont 本体标识（仅 cco 有效）
	 * @param page 分页对象
	 * @param keyword 关键字过滤 label（可选）
	 */
	public IPage<ReferenceAnnotationPropertyVO> pageAnnotationProperties(String ont, Page page, String keyword) {
		Model model = getModel("cco");
		StringBuilder sparql = new StringBuilder();
		sparql.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ");
		sparql.append("PREFIX owl: <http://www.w3.org/2002/07/owl#> ");
		sparql.append("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> ");
		sparql.append("SELECT ?iri ?label ?def WHERE { ");
		sparql.append("  ?iri a owl:AnnotationProperty . ");
		sparql.append("  OPTIONAL { ?iri rdfs:label ?label . FILTER(LANGMATCHES(LANG(?label), 'en')) } ");
		sparql.append("  OPTIONAL { ?iri skos:definition ?def } ");
		if (StrUtil.isNotBlank(keyword)) {
			sparql.append("  FILTER(CONTAINS(LCASE(COALESCE(STR(?label), '')), LCASE('")
				.append(escapeSparql(keyword)).append("'))) ");
		}
		sparql.append("} ORDER BY ?label");
		return executeApQuery(model, sparql.toString(), page);
	}

	// ---------- 缓存与解析 ----------

	/**
	 * 获取（或加载并缓存）Jena Model（AC-7.4）。首次解析 ~5s，缓存后 <1ms。
	 */
	public Model getModel(String ont) {
		return modelCache.get(ont, this::loadModel);
	}

	private Model loadModel(String ont) {
		String file = ONT_FILES.get(ont);
		if (file == null) {
			throw new IllegalArgumentException("未知的参考本体: " + ont);
		}
		String path = referenceBase + "/" + file;
		Model model = ModelFactory.createDefaultModel();
		RDFDataMgr.read(model, path, RDFLanguages.TURTLE);
		return model;
	}

	private Long countItems(String ont) {
		try {
			Model model = getModel(ont);
			String type = "qudt".equals(ont) ? "http://qudt.org/schema/qudt/Unit"
				: "brick".equals(ont) ? "http://www.w3.org/2002/07/owl#Class"
					: "http://www.w3.org/2002/07/owl#AnnotationProperty";
			return model
				.listSubjectsWithProperty(
					model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
					model.createResource(type))
				.toSet()
				.stream()
				.filter(s -> {
					// brick 只计 brick: 命名空间
					if ("brick".equals(ont)) {
						return s.getURI() != null && s.getURI().startsWith("https://brickschema.org/schema/Brick#");
					}
					return s.getURI() != null;
				})
				.count();
		}
		catch (Exception e) {
			return 0L;
		}
	}

	// ---------- 分页执行（SPARQL 全查后内存分页，数据已缓存） ----------

	private IPage<ReferenceUnitVO> executeUnitQuery(Model model, String sparql, Page page) {
		Query query = QueryFactory.create(sparql);
		List<ReferenceUnitVO> all = new ArrayList<>();
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet rs = qexec.execSelect();
			while (rs.hasNext()) {
				QuerySolution sol = rs.next();
				ReferenceUnitVO vo = new ReferenceUnitVO();
				vo.setIri(str(sol, "iri"));
				vo.setLabel(str(sol, "label"));
				vo.setSymbol(str(sol, "symbol"));
				vo.setConversionMultiplier(str(sol, "mul"));
				vo.setConversionMultiplierSn(str(sol, "mulSn"));
				vo.setUcumCode(str(sol, "ucum"));
				vo.setQuantityKindIri(str(sol, "qk"));
				vo.setDescription(str(sol, "desc"));
				all.add(vo);
			}
		}
		return paginate(all, page);
	}

	private IPage<ReferenceClassVO> executeClassQuery(Model model, String sparql, Page page) {
		Query query = QueryFactory.create(sparql);
		List<ReferenceClassVO> all = new ArrayList<>();
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet rs = qexec.execSelect();
			while (rs.hasNext()) {
				QuerySolution sol = rs.next();
				ReferenceClassVO vo = new ReferenceClassVO();
				vo.setIri(str(sol, "iri"));
				vo.setLabel(str(sol, "label"));
				vo.setDefinition(str(sol, "def"));
				vo.setParentIri(str(sol, "parent"));
				vo.setDeprecated("true".equalsIgnoreCase(str(sol, "dep")));
				all.add(vo);
			}
		}
		return paginate(all, page);
	}

	private IPage<ReferenceAnnotationPropertyVO> executeApQuery(Model model, String sparql, Page page) {
		Query query = QueryFactory.create(sparql);
		List<ReferenceAnnotationPropertyVO> all = new ArrayList<>();
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet rs = qexec.execSelect();
			while (rs.hasNext()) {
				QuerySolution sol = rs.next();
				ReferenceAnnotationPropertyVO vo = new ReferenceAnnotationPropertyVO();
				vo.setIri(str(sol, "iri"));
				vo.setLabel(str(sol, "label"));
				vo.setDefinition(str(sol, "def"));
				all.add(vo);
			}
		}
		return paginate(all, page);
	}

	@SuppressWarnings("unchecked")
	private <T> IPage<T> paginate(List<T> all, Page page) {
		long current = page.getCurrent() <= 0 ? 1 : page.getCurrent();
		long size = page.getSize() <= 0 ? 10 : page.getSize();
		int from = (int) Math.min((current - 1) * size, all.size());
		int to = (int) Math.min(from + size, all.size());
		Page<T> result = new Page<>(current, size);
		result.setTotal(all.size());
		result.setRecords(all.subList(from, to));
		return result;
	}

	private String str(QuerySolution sol, String var) {
		return sol.contains(var) && sol.get(var) != null ? sol.get(var).toString() : null;
	}

	private String escapeSparql(String s) {
		return s == null ? "" : s.replace("'", "\\'");
	}

}
