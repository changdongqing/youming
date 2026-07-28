package com.pig4cloud.pig.ontology.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.entity.ClassTemplate;
import com.pig4cloud.pig.ontology.api.entity.QuantityKind;
import com.pig4cloud.pig.ontology.api.entity.Unit;
import com.pig4cloud.pig.ontology.mapper.ClassTemplateMapper;
import com.pig4cloud.pig.ontology.mapper.QuantityKindMapper;
import com.pig4cloud.pig.ontology.mapper.UnitMapper;
import lombok.AllArgsConstructor;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 参考本体库导入 Service（FR-7，AC-7.2/7.3/7.5，R-16）
 * <p>
 * QUDT 单位 -> ont_unit（source=builtin, sourceRef=qudt）；
 * Brick 类 -> ont_class_template（subClassOf->parent_id 取主父类，source=builtin, sourceRef=brick）。
 * 导入前查重（AC-7.5），导入项挂来源溯源（AC-7.3）。
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ReferenceImportService {

	private final ReferenceOntologyService referenceOntologyService;

	private final UnitMapper unitMapper;

	private final QuantityKindMapper quantityKindMapper;

	private final ClassTemplateMapper classTemplateMapper;

	/**
	 * 导入 QUDT 单位到 ont_unit（AC-7.2/7.3/7.5）。
	 * @param iri QUDT 单位 IRI，如 http://qudt.org/vocab/unit/KiloGM
	 */
	@Transactional(rollbackFor = Exception.class)
	public R importQudtUnit(String iri) {
		// 1. 查重（AC-7.5，不重复新增）
		Long exists = unitMapper.selectCount(Wrappers.<Unit>lambdaQuery().eq(Unit::getQudtIri, iri));
		if (exists > 0) {
			return R.failed("QUDT IRI '" + iri + "' 已存在于单位注册表，不重复导入");
		}
		// 2. 从 Model 提取单位字段
		Model model = referenceOntologyService.getModel("qudt");
		Unit unit = extractUnit(model, iri);
		if (unit == null) {
			return R.failed("QUDT 单位不存在: " + iri);
		}
		// 3. 量纲匹配（按 QUDT 量纲 IRI 匹配 ont_quantity_kind.qudt_iri）
		String qkIri = extractQuantityKindIri(model, iri);
		if (StrUtil.isBlank(qkIri)) {
			return R.failed("单位未关联量纲，无法导入");
		}
		QuantityKind qk = quantityKindMapper
			.selectOne(Wrappers.<QuantityKind>lambdaQuery().eq(QuantityKind::getQudtIri, qkIri));
		if (qk == null) {
			return R.failed("量纲 '" + qkIri + "' 未在量纲注册表注册，请先注册量纲或导入对应量纲");
		}
		unit.setQuantityKindId(qk.getId());
		// 4. 落库（source=builtin, sourceRef=qudt，AC-7.3 溯源）
		unit.setQudtIri(iri);
		unit.setSource("builtin");
		unit.setSourceRef("qudt");
		unit.setDeprecated("0");
		unitMapper.insert(unit);
		return R.ok("导入成功: " + iri);
	}

	/**
	 * 导入 Brick 类为分类模板（AC-7.3/7.5，R-16 取主父类）。
	 * <p>
	 * 类本地名转 kebab-case 作 templateCode（如 HVAC_Equipment -> hvac-equipment）；
	 * subClassOf 取第一个 brick: 命名空间父类作 parent_id（主父类），递归确保父类已导入（先建父后建子）。
	 * @param iri Brick 类 IRI，如 https://brickschema.org/schema/Brick#Pump
	 */
	@Transactional(rollbackFor = Exception.class)
	public R importBrickClass(String iri) {
		// 1. 类本地名 -> templateCode（kebab-case）
		String localName = extractLocalName(iri);
		String templateCode = localName.replace('_', '-').toLowerCase();
		// 2. 查重（AC-7.5，不重复新增）
		Long exists = classTemplateMapper.selectCount(
			Wrappers.<ClassTemplate>lambdaQuery().eq(ClassTemplate::getTemplateCode, templateCode));
		if (exists > 0) {
			return R.failed("模板标识 '" + templateCode + "' 已存在，不重复导入");
		}
		// 3. 从 Model 提取类字段
		Model model = referenceOntologyService.getModel("brick");
		String label = extractLabel(model, iri);
		String definition = extractDefinition(model, iri);
		String parentIri = extractParentIri(model, iri);
		// 4. parent_id：取主父类（brick: 命名空间），递归确保父类已导入（R-16）
		Long parentId = null;
		if (StrUtil.isNotBlank(parentIri) && parentIri.startsWith("https://brickschema.org/schema/Brick#")) {
			String parentCode = extractLocalName(parentIri).replace('_', '-').toLowerCase();
			ClassTemplate parent = classTemplateMapper
				.selectOne(Wrappers.<ClassTemplate>lambdaQuery().eq(ClassTemplate::getTemplateCode, parentCode));
			if (parent == null) {
				// 递归导入父类（先建父后建子，避免 parent_id 悬空）
				R parentResult = importBrickClass(parentIri);
				if (parentResult.getCode() != 0) {
					return R.failed("导入父类失败: " + parentResult.getMsg());
				}
				parent = classTemplateMapper
					.selectOne(Wrappers.<ClassTemplate>lambdaQuery().eq(ClassTemplate::getTemplateCode, parentCode));
			}
			parentId = parent != null ? parent.getId() : null;
		}
		// 5. 落库（source=builtin, sourceRef=brick，AC-7.3 溯源）
		ClassTemplate tpl = new ClassTemplate();
		tpl.setTemplateCode(templateCode);
		tpl.setLabel(StrUtil.isNotBlank(label) ? label : localName.replace('_', ' '));
		tpl.setDescription(definition);
		tpl.setParentId(parentId);
		tpl.setTreeRoot("brick");
		tpl.setSource("builtin");
		tpl.setSourceRef("brick");
		tpl.setDeprecated("0");
		tpl.setInheritAppearance("1");
		tpl.setSortOrder(0);
		classTemplateMapper.insert(tpl);
		return R.ok("导入成功: " + templateCode);
	}

	// ---------- Jena 查询辅助 ----------

	private Unit extractUnit(Model model, String iri) {
		String sparql = "PREFIX qudt: <http://qudt.org/schema/qudt/> "
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
			+ "SELECT ?label ?symbol ?mul ?mulSn ?offset ?offsetSn ?ucum WHERE { "
			+ "  <" + iri + "> a qudt:Unit ."
			+ "  OPTIONAL { <" + iri + "> rdfs:label ?label . FILTER(LANG(?label) = '' || LANGMATCHES(LANG(?label), 'en')) } "
			+ "  OPTIONAL { <" + iri + "> qudt:symbol ?symbol } "
			+ "  OPTIONAL { <" + iri + "> qudt:conversionMultiplier ?mul } "
			+ "  OPTIONAL { <" + iri + "> qudt:conversionMultiplierSN ?mulSn } "
			+ "  OPTIONAL { <" + iri + "> qudt:conversionOffset ?offset } "
			+ "  OPTIONAL { <" + iri + "> qudt:conversionOffsetSN ?offsetSn } "
			+ "  OPTIONAL { <" + iri + "> qudt:ucumCode ?ucum } "
			+ "} LIMIT 1";
		try (QueryExecution qexec = QueryExecutionFactory.create(QueryFactory.create(sparql), model)) {
			ResultSet rs = qexec.execSelect();
			if (!rs.hasNext()) {
				return null;
			}
			QuerySolution sol = rs.next();
			Unit unit = new Unit();
			unit.setLabel(str(sol, "label"));
			unit.setSymbol(str(sol, "symbol"));
			unit.setUcumCode(str(sol, "ucum"));
			if (StrUtil.isNotBlank(str(sol, "mul"))) {
				unit.setConversionMultiplier(new BigDecimal(str(sol, "mul")));
			}
			unit.setConversionMultiplierSn(str(sol, "mulSn"));
			if (StrUtil.isNotBlank(str(sol, "offset"))) {
				unit.setConversionOffset(new BigDecimal(str(sol, "offset")));
			}
			unit.setConversionOffsetSn(str(sol, "offsetSn"));
			return unit;
		}
	}

	private String extractQuantityKindIri(Model model, String iri) {
		String sparql = "PREFIX qudt: <http://qudt.org/schema/qudt/> "
			+ "SELECT ?qk WHERE { <" + iri + "> qudt:hasQuantityKind ?qk } LIMIT 1";
		try (QueryExecution qexec = QueryExecutionFactory.create(QueryFactory.create(sparql), model)) {
			ResultSet rs = qexec.execSelect();
			if (rs.hasNext()) {
				return str(rs.next(), "qk");
			}
		}
		return null;
	}

	private String extractLabel(Model model, String iri) {
		String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
			+ "SELECT ?label WHERE { <" + iri + "> rdfs:label ?label } LIMIT 1";
		try (QueryExecution qexec = QueryExecutionFactory.create(QueryFactory.create(sparql), model)) {
			ResultSet rs = qexec.execSelect();
			if (rs.hasNext()) {
				return str(rs.next(), "label");
			}
		}
		return null;
	}

	private String extractDefinition(Model model, String iri) {
		String sparql = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
			+ "SELECT ?def WHERE { <" + iri + "> skos:definition ?def } LIMIT 1";
		try (QueryExecution qexec = QueryExecutionFactory.create(QueryFactory.create(sparql), model)) {
			ResultSet rs = qexec.execSelect();
			if (rs.hasNext()) {
				return str(rs.next(), "def");
			}
		}
		return null;
	}

	private String extractParentIri(Model model, String iri) {
		String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
			+ "SELECT ?parent WHERE { <" + iri + "> rdfs:subClassOf ?parent . "
			+ "  FILTER(STRSTARTS(STR(?parent), 'https://brickschema.org/schema/Brick#')) } LIMIT 1";
		try (QueryExecution qexec = QueryExecutionFactory.create(QueryFactory.create(sparql), model)) {
			ResultSet rs = qexec.execSelect();
			if (rs.hasNext()) {
				return str(rs.next(), "parent");
			}
		}
		return null;
	}

	private String extractLocalName(String iri) {
		int hash = iri.lastIndexOf('#');
		int slash = iri.lastIndexOf('/');
		int pos = Math.max(hash, slash);
		return pos >= 0 ? iri.substring(pos + 1) : iri;
	}

	private String str(QuerySolution sol, String var) {
		return sol.contains(var) && sol.get(var) != null ? sol.get(var).toString() : null;
	}

}
