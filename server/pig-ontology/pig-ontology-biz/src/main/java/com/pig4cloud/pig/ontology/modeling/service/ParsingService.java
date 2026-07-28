package com.pig4cloud.pig.ontology.modeling.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.api.entity.AnnotationProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelSubclassOf;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelClassMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelDatatypePropertyMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelObjectPropertyMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelSubclassOfMapper;
import com.pig4cloud.pig.ontology.modeling.vo.ImportResultVO;
import com.pig4cloud.pig.ontology.service.AnnotationPropertyService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RDF 解析与导入核心 Service（FR-16）
 * <p>
 * Jena 解析上传文件为 Model -> 提取 owl:Class / DatatypeProperty / ObjectProperty / subClassOf
 * -> 识别 ont:xxx 注解（templateRef/unitRef 等）-> 重建建模域记录。
 *
 * @author pig
 * @date 2026-07-28
 */
@Slf4j
@AllArgsConstructor
@Service
public class ParsingService {

	private static final String ONT_NS = "http://youming.com/ontology/annotation#";

	private final ModelClassMapper modelClassMapper;
	private final ModelDatatypePropertyMapper dtPropMapper;
	private final ModelObjectPropertyMapper objPropMapper;
	private final ModelSubclassOfMapper subclassOfMapper;
	private final AnnotationPropertyService annotationPropertyService;

	/**
	 * 解析导入 RDF 文件（AC-16.1~16.6）
	 */
	@Transactional(rollbackFor = Exception.class)
	public ImportResultVO parse(Long projectId, MultipartFile file, String conflictStrategy) {
		ImportResultVO result = new ImportResultVO();
		Model model;
		try (InputStream in = file.getInputStream()) {
			model = ModelFactory.createDefaultModel();
			String filename = file.getOriginalFilename();
			Lang lang = (filename != null && filename.endsWith(".xml"))
					? RDFLanguages.RDFXML : RDFLanguages.TURTLE;
			RDFDataMgr.read(model, in, lang);
		}
		catch (Exception e) {
			log.error("RDF 解析失败", e);
			result.setError("RDF 解析失败：" + e.getMessage());
			return result;
		}

		// 拉取注册表（AC-16.4，校验注解合法性，方案B：同模块 AnnotationPropertyService）
		Set<String> validAnnotationNames = fetchAnnotationNames();

		// IRI -> classId 映射（用于建属性/关系引用）
		Map<String, Long> iriToClassId = new HashMap<>();

		// 1. 提取类（owl:Class）
		ResIterator classIter = model.listSubjectsWithProperty(RDF.type, OWL.Class);
		while (classIter.hasNext()) {
			Resource classRes = classIter.next();
			String classIri = classRes.getURI();
			if (classIri == null) {
				continue;
			}
			// 冲突检查（AC-16.6）
			Long existing = findClassByIri(projectId, classIri);
			if (existing != null) {
				if ("SKIP".equals(conflictStrategy)) {
					result.setSkippedConflictCount(result.getSkippedConflictCount() + 1);
					iriToClassId.put(classIri, existing);
					continue;
				}
				if ("OVERWRITE".equals(conflictStrategy)) {
					deleteClassCascade(existing);
				}
				if ("RENAME".equals(conflictStrategy)) {
					classIri = classIri + "_import_" + System.currentTimeMillis();
				}
			}
			// 创建类
			ModelClass cls = new ModelClass();
			cls.setProjectId(projectId);
			cls.setClassIri(classIri);
			cls.setLocalName(extractLocalName(classIri));
			Statement labelStmt = classRes.getProperty(RDFS.label);
			if (labelStmt != null) {
				cls.setLabel(labelStmt.getString());
			}
			cls.setClassificationCode(getStringAnnotation(classRes, "classificationCode"));
			cls.setTemplateCode(getStringAnnotation(classRes, "templateRef"));
			cls.setIcon(getStringAnnotation(classRes, "icon"));
			cls.setColor(getStringAnnotation(classRes, "color"));
			modelClassMapper.insert(cls);
			iriToClassId.put(classIri, cls.getId());
			result.setClassCount(result.getClassCount() + 1);
		}

		// 2. 提取 subClassOf（过滤匿名类 owl:Restriction，P-3）
		StmtIterator subIter = model.listStatements(null, RDFS.subClassOf, (RDFNode) null);
		while (subIter.hasNext()) {
			Statement stmt = subIter.next();
			// 只处理具名父类（isURIResource 过滤匿名 bnode 如 owl:Restriction 约束类）
			if (!stmt.getObject().isURIResource()) {
				continue;
			}
			String childIri = stmt.getSubject().getURI();
			String parentIri = stmt.getObject().asResource().getURI();
			Long childId = iriToClassId.get(childIri);
			Long parentId = iriToClassId.get(parentIri);
			if (childId != null && parentId != null) {
				ModelSubclassOf edge = new ModelSubclassOf();
				edge.setProjectId(projectId);
				edge.setChildClassId(childId);
				edge.setParentClassId(parentId);
				edge.setSyncStatus("0");
				edge.setRetryCount(0);
				subclassOfMapper.insert(edge);
				result.setSubclassOfCount(result.getSubclassOfCount() + 1);
			}
		}

		// 3. 提取数据属性（owl:DatatypeProperty）
		ResIterator dtIter = model.listSubjectsWithProperty(RDF.type, OWL.DatatypeProperty);
		while (dtIter.hasNext()) {
			Resource propRes = dtIter.next();
			ModelDatatypeProperty prop = extractDatatypeProperty(propRes, projectId, iriToClassId, result,
					validAnnotationNames);
			if (prop != null) {
				dtPropMapper.insert(prop);
				result.setDatatypePropertyCount(result.getDatatypePropertyCount() + 1);
			}
		}

		// 4. 提取对象属性（owl:ObjectProperty）
		ResIterator objIter = model.listSubjectsWithProperty(RDF.type, OWL.ObjectProperty);
		while (objIter.hasNext()) {
			Resource propRes = objIter.next();
			ModelObjectProperty prop = extractObjectProperty(propRes, projectId, iriToClassId, result,
					validAnnotationNames);
			if (prop != null) {
				objPropMapper.insert(prop);
				result.setObjectPropertyCount(result.getObjectPropertyCount() + 1);
			}
		}

		return result;
	}

	private ModelDatatypeProperty extractDatatypeProperty(Resource propRes, Long projectId,
			Map<String, Long> iriToClassId, ImportResultVO result, Set<String> validAnnotationNames) {
		ModelDatatypeProperty prop = new ModelDatatypeProperty();
		prop.setProjectId(projectId);
		prop.setPropertyIri(propRes.getURI());
		prop.setLocalName(extractLocalName(propRes.getURI()));
		Statement labelStmt = propRes.getProperty(RDFS.label);
		if (labelStmt != null) {
			prop.setLabel(labelStmt.getString());
		}
		// rdfs:domain -> classId
		Statement domainStmt = propRes.getProperty(RDFS.domain);
		if (domainStmt != null && domainStmt.getObject().isURIResource()) {
			Long classId = iriToClassId.get(domainStmt.getObject().asResource().getURI());
			if (classId != null) {
				prop.setClassId(classId);
			}
		}
		if (prop.getClassId() == null) {
			return null; // 无 domain 的属性跳过
		}
		// rdfs:range -> xsdType
		Statement rangeStmt = propRes.getProperty(RDFS.range);
		if (rangeStmt != null && rangeStmt.getObject().isURIResource()) {
			prop.setXsdType(mapUriToXsd(rangeStmt.getObject().asResource().getURI()));
		}
		else {
			prop.setXsdType("xsd:string");
		}
		// ont:templateRef 溯源识别（AC-16.2）
		String templateRef = getStringAnnotation(propRes, "templateRef");
		if (StrUtil.isNotBlank(templateRef)) {
			prop.setTemplateCode(templateRef);
			if (validateTemplateExists(templateRef)) {
				result.setRecognizedTemplateCount(result.getRecognizedTemplateCount() + 1);
			}
			else {
				result.setOrphanTemplateCount(result.getOrphanTemplateCount() + 1);
			}
		}
		// ont:unitRef 单位识别（AC-16.3）
		String unitRef = getResourceAnnotation(propRes, "unitRef");
		if (StrUtil.isNotBlank(unitRef)) {
			prop.setUnitRef(unitRef);
			result.setRecognizedUnitCount(result.getRecognizedUnitCount() + 1);
		}
		// ont:isIdentifier
		prop.setIsIdentifier(getBooleanAnnotation(propRes, "isIdentifier") ? "1" : "0");
		// ont:enumValues
		prop.setEnumValues(getStringAnnotation(propRes, "enumValues"));
		// 校验注册表外注解（AC-16.4）
		checkUnknownAnnotations(propRes, validAnnotationNames, result);
		prop.setMinCardinality(0);
		prop.setMaxCardinality(-1);
		return prop;
	}

	private ModelObjectProperty extractObjectProperty(Resource propRes, Long projectId,
			Map<String, Long> iriToClassId, ImportResultVO result, Set<String> validAnnotationNames) {
		ModelObjectProperty prop = new ModelObjectProperty();
		prop.setProjectId(projectId);
		prop.setPropertyIri(propRes.getURI());
		prop.setLocalName(extractLocalName(propRes.getURI()));
		Statement domainStmt = propRes.getProperty(RDFS.domain);
		Statement rangeStmt = propRes.getProperty(RDFS.range);
		if (domainStmt != null && domainStmt.getObject().isURIResource()) {
			prop.setDomainClassId(iriToClassId.get(domainStmt.getObject().asResource().getURI()));
		}
		if (rangeStmt != null && rangeStmt.getObject().isURIResource()) {
			prop.setRangeClassId(iriToClassId.get(rangeStmt.getObject().asResource().getURI()));
		}
		if (prop.getDomainClassId() == null) {
			return null;
		}
		Statement labelStmt = propRes.getProperty(RDFS.label);
		if (labelStmt != null) {
			prop.setLabel(labelStmt.getString());
		}
		prop.setTemplateCode(getStringAnnotation(propRes, "templateRef"));
		prop.setMinCardinality(0);
		prop.setMaxCardinality(-1);
		checkUnknownAnnotations(propRes, validAnnotationNames, result);
		return prop;
	}

	// ---------- 注解提取工具方法 ----------

	private String getStringAnnotation(Resource res, String localName) {
		Statement stmt = res.getProperty(res.getModel().createProperty(ONT_NS + localName));
		return (stmt != null && stmt.getObject().isLiteral()) ? stmt.getString() : null;
	}

	private String getResourceAnnotation(Resource res, String localName) {
		Statement stmt = res.getProperty(res.getModel().createProperty(ONT_NS + localName));
		return (stmt != null && stmt.getObject().isResource()) ? stmt.getObject().asResource().getURI() : null;
	}

	private boolean getBooleanAnnotation(Resource res, String localName) {
		Statement stmt = res.getProperty(res.getModel().createProperty(ONT_NS + localName));
		return stmt != null && stmt.getBoolean();
	}

	private void checkUnknownAnnotations(Resource res, Set<String> validNames, ImportResultVO result) {
		if (validNames.isEmpty()) {
			return; // 注册表拉取失败时不校验
		}
		StmtIterator iter = res.listProperties();
		while (iter.hasNext()) {
			Statement stmt = iter.next();
			String uri = stmt.getPredicate().getURI();
			if (uri != null && uri.startsWith(ONT_NS)) {
				String localName = uri.substring(ONT_NS.length());
				if (!validNames.contains(localName)) {
					result.getWarnings().add("注册表外注解 ont:" + localName + " 已忽略");
				}
			}
		}
	}

	private Set<String> fetchAnnotationNames() {
		try {
			List<AnnotationProperty> list = annotationPropertyService.list((String) null);
			return list.stream().map(AnnotationProperty::getLocalName).collect(Collectors.toSet());
		}
		catch (Exception e) {
			log.warn("拉取注释属性注册表失败", e);
			return Collections.emptySet();
		}
	}

	private boolean validateTemplateExists(String templateCode) {
		// v1 简化：总是返回 true，后续可调治理域 ClassTemplateService/PropertyTemplateService 校验
		return true;
	}

	// ---------- 辅助方法 ----------

	private Long findClassByIri(Long projectId, String classIri) {
		ModelClass cls = modelClassMapper.selectOne(
				Wrappers.<ModelClass>lambdaQuery()
					.eq(ModelClass::getProjectId, projectId)
					.eq(ModelClass::getClassIri, classIri));
		return (cls != null) ? cls.getId() : null;
	}

	private void deleteClassCascade(Long classId) {
		dtPropMapper.delete(Wrappers.<ModelDatatypeProperty>lambdaQuery()
			.eq(ModelDatatypeProperty::getClassId, classId));
		modelClassMapper.deleteById(classId);
	}

	private String extractLocalName(String iri) {
		int idx = iri.lastIndexOf('#');
		if (idx < 0) {
			idx = iri.lastIndexOf('/');
		}
		return (idx >= 0) ? iri.substring(idx + 1) : iri;
	}

	private String mapUriToXsd(String uri) {
		if (uri == null) {
			return "xsd:string";
		}
		if (uri.contains("integer")) {
			return "xsd:integer";
		}
		if (uri.contains("decimal")) {
			return "xsd:decimal";
		}
		if (uri.contains("boolean")) {
			return "xsd:boolean";
		}
		if (uri.contains("dateTime")) {
			return "xsd:dateTime";
		}
		return "xsd:string";
	}

}
