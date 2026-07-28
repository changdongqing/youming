package com.pig4cloud.pig.ontology.modeling.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pig4cloud.pig.ontology.api.entity.AnnotationProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelProject;
import com.pig4cloud.pig.ontology.modeling.entity.ModelSubclassOf;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelClassMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelDatatypePropertyMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelObjectPropertyMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelProjectMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelSubclassOfMapper;
import com.pig4cloud.pig.ontology.modeling.vo.SerializePreviewVO;
import com.pig4cloud.pig.ontology.service.AnnotationPropertyService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * RDF 序列化核心 Service（FR-15）
 * <p>
 * 查建模域 4 张表构建 Jena Model -> RDFDataMgr.write 序列化为 Turtle/OWL XML。
 * 方案 B 默认（带前缀独立副本），注释属性注册表校验 ont:xxx 注解合法性。
 *
 * @author pig
 * @date 2026-07-28
 */
@Slf4j
@AllArgsConstructor
@Service
public class SerializationService {

	private static final String ONT_NS = "http://youming.com/ontology/annotation#";

	/** 序列化缓存：key=projectId+format，TTL 5min，建模操作时失效 */
	private final Cache<String, String> serializeCache = Caffeine.newBuilder()
		.expireAfterWrite(5, TimeUnit.MINUTES)
		.maximumSize(50)
		.build();

	private final ModelClassMapper modelClassMapper;
	private final ModelDatatypePropertyMapper dtPropMapper;
	private final ModelObjectPropertyMapper objPropMapper;
	private final ModelSubclassOfMapper subclassOfMapper;
	private final ModelProjectMapper modelProjectMapper;
	private final AnnotationPropertyService annotationPropertyService;

	/**
	 * 序列化预览（AC-15.8）
	 */
	public SerializePreviewVO preview(Long projectId, String format) {
		// 方案 A 禁用检查（AC-15.4）
		ModelProject project = modelProjectMapper.selectById(projectId);
		if (project == null) {
			return null;
		}
		if ("A".equals(project.getSerializationStrategy())) {
			SerializePreviewVO vo = new SerializePreviewVO();
			vo.setFormat(format);
			vo.setContent("# 方案 A（共享属性+多domain）尚未启用，当前使用方案 B。请在项目设置中切换为方案 B。");
			return vo;
		}

		// 缓存
		String cacheKey = projectId + ":" + format;
		String cached = serializeCache.getIfPresent(cacheKey);

		SerializePreviewVO vo = new SerializePreviewVO();
		vo.setFormat(format);

		if (cached != null) {
			vo.setContent(cached);
			fillCounts(vo, projectId);
			return vo;
		}

		// 构建 Model 并序列化
		Model model = buildModel(projectId);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Lang lang = "OWL_XML".equals(format) ? RDFLanguages.RDFXML : RDFLanguages.TURTLE;
		RDFDataMgr.write(out, model, lang);
		String content = out.toString();
		vo.setContent(content);
		fillCounts(vo, projectId);
		serializeCache.put(cacheKey, content);
		return vo;
	}

	private void fillCounts(SerializePreviewVO vo, Long projectId) {
		vo.setClassCount(Math.toIntExact(modelClassMapper.selectCount(
				Wrappers.<ModelClass>lambdaQuery().eq(ModelClass::getProjectId, projectId))));
		vo.setDatatypePropertyCount(Math.toIntExact(dtPropMapper.selectCount(
				Wrappers.<ModelDatatypeProperty>lambdaQuery().eq(ModelDatatypeProperty::getProjectId, projectId))));
		vo.setObjectPropertyCount(Math.toIntExact(objPropMapper.selectCount(
				Wrappers.<ModelObjectProperty>lambdaQuery().eq(ModelObjectProperty::getProjectId, projectId))));
		vo.setSubclassOfCount(Math.toIntExact(subclassOfMapper.selectCount(
				Wrappers.<ModelSubclassOf>lambdaQuery().eq(ModelSubclassOf::getProjectId, projectId))));
	}

	/**
	 * 失效缓存（建模操作后调用，P-5）
	 */
	public void invalidateCache(Long projectId) {
		serializeCache.invalidateAll();
	}

	/**
	 * 构建 Jena Model（方案 B，AC-15.3/15.5/15.6/15.7）
	 */
	private Model buildModel(Long projectId) {
		Model model = ModelFactory.createDefaultModel();
		// 命名空间声明（附录 C.2）
		model.setNsPrefix("rdf", RDF.getURI());
		model.setNsPrefix("rdfs", RDFS.getURI());
		model.setNsPrefix("owl", OWL.getURI());
		model.setNsPrefix("xsd", XSD.getURI());
		model.setNsPrefix("ont", ONT_NS);

		// 拉取注释属性注册表 localName 集合（AC-15.5，方案B：同模块 AnnotationPropertyService）
		Set<String> validAnnotationNames = fetchAnnotationNames();

		// 查项目前缀
		ModelProject project = modelProjectMapper.selectById(projectId);
		String nsBase = project.getNamespaceBase();
		model.setNsPrefix(extractPrefixName(nsBase, projectId), nsBase);

		// 查类 ID -> IRI 映射
		List<ModelClass> classes = modelClassMapper.selectList(
				Wrappers.<ModelClass>lambdaQuery().eq(ModelClass::getProjectId, projectId));
		Map<Long, String> classIriMap = new HashMap<>();
		for (ModelClass cls : classes) {
			classIriMap.put(cls.getId(), cls.getClassIri());
		}

		// 1. 序列化类（AC-15.7）
		for (ModelClass cls : classes) {
			Resource classRes = model.createResource(cls.getClassIri(), OWL.Class);
			if (StrUtil.isNotBlank(cls.getLabel())) {
				classRes.addProperty(RDFS.label, cls.getLabel());
			}
			writeClassAnnotations(model, classRes, cls, validAnnotationNames);
		}

		// 2. 序列化 subClassOf（AC-15.7）
		List<ModelSubclassOf> edges = subclassOfMapper.selectList(
				Wrappers.<ModelSubclassOf>lambdaQuery().eq(ModelSubclassOf::getProjectId, projectId));
		for (ModelSubclassOf edge : edges) {
			String childIri = classIriMap.get(edge.getChildClassId());
			String parentIri = classIriMap.get(edge.getParentClassId());
			if (childIri != null && parentIri != null) {
				model.getResource(childIri).addProperty(RDFS.subClassOf, model.getResource(parentIri));
			}
		}

		// 3. 序列化数据属性（AC-15.3/15.5/15.6）
		List<ModelDatatypeProperty> dtProps = dtPropMapper.selectList(
				Wrappers.<ModelDatatypeProperty>lambdaQuery().eq(ModelDatatypeProperty::getProjectId, projectId));
		for (ModelDatatypeProperty prop : dtProps) {
			Resource propRes = model.createResource(prop.getPropertyIri(), OWL.DatatypeProperty);
			String domainIri = classIriMap.get(prop.getClassId());
			if (domainIri != null) {
				propRes.addProperty(RDFS.domain, model.getResource(domainIri));
			}
			propRes.addProperty(RDFS.range, model.getResource(mapXsdUri(prop.getXsdType())));
			if (StrUtil.isNotBlank(prop.getLabel())) {
				propRes.addProperty(RDFS.label, prop.getLabel());
			}
			writeDatatypePropertyAnnotations(model, propRes, prop, validAnnotationNames);
		}

		// 4. 序列化对象属性（AC-15.3/15.5）
		List<ModelObjectProperty> objProps = objPropMapper.selectList(
				Wrappers.<ModelObjectProperty>lambdaQuery().eq(ModelObjectProperty::getProjectId, projectId));
		for (ModelObjectProperty prop : objProps) {
			Resource propRes = model.createResource(prop.getPropertyIri(), OWL.ObjectProperty);
			String domainIri = classIriMap.get(prop.getDomainClassId());
			String rangeIri = classIriMap.get(prop.getRangeClassId());
			if (domainIri != null) {
				propRes.addProperty(RDFS.domain, model.getResource(domainIri));
			}
			if (rangeIri != null) {
				propRes.addProperty(RDFS.range, model.getResource(rangeIri));
			}
			if (StrUtil.isNotBlank(prop.getLabel())) {
				propRes.addProperty(RDFS.label, prop.getLabel());
			}
			writeObjectPropertyAnnotations(model, propRes, prop, validAnnotationNames);
		}

		return model;
	}

	/**
	 * 拉取注释属性注册表 localName 集合（AC-15.5，方案B：同模块 AnnotationPropertyService）
	 */
	private Set<String> fetchAnnotationNames() {
		try {
			List<AnnotationProperty> list = annotationPropertyService.list((String) null);
			return list.stream().map(AnnotationProperty::getLocalName).collect(Collectors.toSet());
		}
		catch (Exception e) {
			log.warn("拉取注释属性注册表失败，序列化注解降级为不校验", e);
			return java.util.Collections.emptySet();
		}
	}

	/**
	 * 写注解前校验 localName 是否在注册表内（AC-15.5）
	 */
	private boolean isValidAnnotation(String localName, Set<String> validNames) {
		if (validNames.isEmpty()) {
			return true; // 注册表拉取失败时降级为全部允许
		}
		return validNames.contains(localName);
	}

	private void writeClassAnnotations(Model model, Resource res, ModelClass cls, Set<String> validNames) {
		if (StrUtil.isNotBlank(cls.getClassificationCode()) && isValidAnnotation("classificationCode", validNames)) {
			res.addProperty(model.createProperty(ONT_NS + "classificationCode"), cls.getClassificationCode());
		}
		if (StrUtil.isNotBlank(cls.getIcon()) && isValidAnnotation("icon", validNames)) {
			res.addProperty(model.createProperty(ONT_NS + "icon"), cls.getIcon());
		}
		if (StrUtil.isNotBlank(cls.getColor()) && isValidAnnotation("color", validNames)) {
			res.addProperty(model.createProperty(ONT_NS + "color"), cls.getColor());
		}
		if (StrUtil.isNotBlank(cls.getTemplateCode()) && isValidAnnotation("templateRef", validNames)) {
			res.addProperty(model.createProperty(ONT_NS + "templateRef"), cls.getTemplateCode());
		}
	}

	private void writeDatatypePropertyAnnotations(Model model, Resource res, ModelDatatypeProperty prop,
			Set<String> validNames) {
		if (StrUtil.isNotBlank(prop.getTemplateCode()) && isValidAnnotation("templateRef", validNames)) {
			res.addProperty(model.createProperty(ONT_NS + "templateRef"), prop.getTemplateCode());
		}
		if (StrUtil.isNotBlank(prop.getUnitRef()) && isValidAnnotation("unitRef", validNames)) {
			res.addProperty(model.createProperty(ONT_NS + "unitRef"), model.createResource(prop.getUnitRef()));
		}
		if ("1".equals(prop.getIsIdentifier()) && isValidAnnotation("isIdentifier", validNames)) {
			res.addLiteral(model.createProperty(ONT_NS + "isIdentifier"), true);
		}
		if (StrUtil.isNotBlank(prop.getEnumValues()) && isValidAnnotation("enumValues", validNames)) {
			res.addProperty(model.createProperty(ONT_NS + "enumValues"), prop.getEnumValues());
		}
	}

	private void writeObjectPropertyAnnotations(Model model, Resource res, ModelObjectProperty prop,
			Set<String> validNames) {
		if (StrUtil.isNotBlank(prop.getTemplateCode()) && isValidAnnotation("templateRef", validNames)) {
			res.addProperty(model.createProperty(ONT_NS + "templateRef"), prop.getTemplateCode());
		}
		if (isValidAnnotation("cardinality", validNames)) {
			String cardinality = formatCardinality(prop.getMinCardinality(), prop.getMaxCardinality());
			res.addProperty(model.createProperty(ONT_NS + "cardinality"), cardinality);
		}
	}

	private String mapXsdUri(String xsdType) {
		if (xsdType == null) {
			return XSD.xstring.getURI();
		}
		return switch (xsdType) {
			case "xsd:integer" -> XSD.integer.getURI();
			case "xsd:decimal" -> XSD.decimal.getURI();
			case "xsd:boolean" -> XSD.xboolean.getURI();
			case "xsd:dateTime" -> XSD.dateTime.getURI();
			default -> XSD.xstring.getURI();
		};
	}

	private String formatCardinality(Integer min, Integer max) {
		int minVal = (min != null) ? min : 0;
		int maxVal = (max != null) ? max : -1;
		return minVal + ".." + (maxVal == -1 ? "n" : maxVal);
	}

	private String extractPrefixName(String nsBase, Long projectId) {
		return "onto";
	}

}
