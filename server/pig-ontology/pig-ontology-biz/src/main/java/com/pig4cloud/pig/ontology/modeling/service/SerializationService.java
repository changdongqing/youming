package com.pig4cloud.pig.ontology.modeling.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pig4cloud.pig.ontology.api.entity.AnnotationProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelPrefix;
import com.pig4cloud.pig.ontology.modeling.entity.ModelProject;
import com.pig4cloud.pig.ontology.modeling.entity.ModelSubclassOf;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelClassMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelDatatypePropertyMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelObjectPropertyMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelPrefixMapper;
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

	/** 保留前缀名，项目级前缀不得覆盖（BUG-002） */
	private static final Set<String> RESERVED_PREFIXES = Set.of("rdf", "rdfs", "owl", "xsd", "ont");

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
	private final ModelPrefixMapper modelPrefixMapper;
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

		// 注册项目级前缀（BUG-002 修复：从 ont_model_prefix 表读取，替代硬编码 extractPrefixName）
		List<ModelPrefix> prefixes = modelPrefixMapper.selectList(
				Wrappers.<ModelPrefix>lambdaQuery().eq(ModelPrefix::getProjectId, projectId));
		boolean projectPrefixRegistered = false;
		for (ModelPrefix pf : prefixes) {
			// 跳过与标准前缀冲突的保留前缀名（rdf/rdfs/owl/xsd/ont）
			if (RESERVED_PREFIXES.contains(pf.getPrefix())) {
				continue;
			}
			model.setNsPrefix(pf.getPrefix(), pf.getNamespace());
			if ("1".equals(pf.getIsDefault())) {
				projectPrefixRegistered = true;
			}
		}
		// 若无默认前缀注册（表为空或默认前缀名与保留前缀冲突被跳过），回退用 nsBase 注册 ex 前缀
		if (!projectPrefixRegistered) {
			model.setNsPrefix("ex", nsBase);
		}

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
			// BUG-001 修复：property_iri 在手工创建/模板实例化路径存本地名（如 User_username），
			// 需拼接 nsBase 生成完整 IRI；导入路径已存绝对 IRI（含 http），原样使用
			String fullPropIri = resolveIri(prop.getPropertyIri(), nsBase);
			Resource propRes = model.createResource(fullPropIri, OWL.DatatypeProperty);
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
			// BUG-001 修复：同数据属性，拼接 nsBase 生成完整 IRI
			String fullPropIri = resolveIri(prop.getPropertyIri(), nsBase);
			Resource propRes = model.createResource(fullPropIri, OWL.ObjectProperty);
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

	/**
	 * 解析属性 IRI 为完整绝对 IRI（BUG-001 修复）
	 * <p>
	 * 手工创建/模板实例化路径存本地名（如 User_username），需拼接 nsBase；
	 * 解析导入路径已存绝对 IRI（含 http/https 前缀），原样返回。
	 *
	 * @param propertyIri 数据库中存储的 property_iri 值
	 * @param nsBase      项目命名空间基址
	 * @return 完整绝对 IRI
	 */
	private String resolveIri(String propertyIri, String nsBase) {
		if (propertyIri == null) {
			return nsBase;
		}
		if (propertyIri.startsWith("http://") || propertyIri.startsWith("https://")) {
			return propertyIri;
		}
		return nsBase + propertyIri;
	}

}
