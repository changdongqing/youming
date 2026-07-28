# 详细设计-DD10-RDF序列化与解析

| 项 | 内容 |
|---|---|
| 文档名称 | RDF 序列化与解析 详细设计 |
| 里程碑 | M8（FR-15 + FR-16） |
| 上游 PRD | 《本体建模功能产品需求文档.md》v1.0（FR-15/16 / AC-15.1~15.8 + AC-16.1~16.6 / 10.6 / 十一节场景一/二 / 十四节 M8 / 附录 B/C） |
| 设计计划 | 《本体建模功能详细设计计划.md》DD10（M8） |
| 前置依赖 | DD8 已落地（ont_model_class + 属性表就绪）；DD9 已落地（ont_model_subclassof + 属性业务逻辑就绪）；治理域 DD5/DD6 已落地（Supply API annotation-properties 可消费，jena-arq 5.3.0 已引入） |
| 编写日期 | 2026-07-28 |
| 文档状态 | 待评审 |

---

## 一、设计目标与范围

### 1.1 目标

- **FR-15 RDF 序列化**：将建模结果（类/属性/关系）序列化为 Turtle / OWL XML，方案 B 默认（带前缀独立副本），注释属性注册表驱动 `ont:xxx` 注解自动写入，单位以 `ont:unitRef` 引用 QUDT IRI。
- **FR-16 RDF 解析与导入**：反向解析 RDF 文件（Turtle/OWL XML），重建建模域模型，识别 `templateRef`/`unitRef` 溯源，校验注释属性按注册表。
- **往返一致性**：序列化 -> 解析 -> 再序列化，RDF 图同构（Jena `Model.isIsomorphicWith()`），≥99%。
- **V15 菜单种子**：11400 段"序列化与导入"菜单 + 权限点（无新建表，复用 DD8/DD9 的 4 张建模域表）。
- **零新增依赖**：复用 DD6 已引入的 jena-arq 5.3.0 + caffeine。

### 1.2 范围（本 DD 做 / 不做）

| 做（本 DD） | 不做（后续 DD / Out of Scope） |
|---|---|
| Turtle 序列化导出（FR-15.1） | 方案 A 序列化实现（v1 禁用，字段预留，FR-15.4） |
| OWL XML 序列化导出（FR-15.2） | 画布序列化预览交互（DD11/FR-17.6，DD10 提供接口） |
| 方案 B 序列化（默认，FR-15.3） | 个体实例序列化（M10/DD12，Out of Scope） |
| 注释属性驱动注解写入（FR-15.5） | SPARQL 查询终端（Out of Scope） |
| 单位引用 ont:unitRef（FR-15.6） | SHACL 约束生成（Out of Scope） |
| 类/属性/subClassOf 序列化（FR-15.7） | |
| 序列化预览接口（FR-15.8） | |
| Turtle/OWL XML 解析导入（FR-16.1） | |
| 溯源识别 templateRef（FR-16.2） | |
| 单位识别 unitRef（FR-16.3） | |
| 注释属性识别校验（FR-16.4） | |
| 往返一致性校验（FR-16.5） | |
| 导入冲突处理（FR-16.6） | |
| V15 菜单种子 | |

### 1.3 验收映射（M8 DoD）

| PRD AC | 本 DD 实现点 |
|---|---|
| AC-15.1 Turtle 序列化导出合法 | 4.3 ModelSerializeController.download(TTL) + 4.4 SerializationService.serialize(TTL) |
| AC-15.2 OWL XML 序列化导出合法 | 4.3 Controller.download(OWL_XML) + 4.4 Service.serialize(RDFXML) |
| AC-15.3 方案 B 默认 + IRI 带前缀 | 4.4 SerializationService.buildModel（propertyIri = {classLocalName}_{propLocalName}） |
| AC-15.4 方案 A v1 禁用返回提示 | 4.4 SerializationService.serialize（strategy='A' 返回提示） |
| AC-15.5 注释属性按注册表自动写入 | 4.4 SerializationService.writeAnnotations（消费 Supply annotation-properties） |
| AC-15.6 单位输出 ont:unitRef | 4.4 writeAnnotations（unitRef -> ont:unitRef rdf:resource） |
| AC-15.7 类序列化含 IRI/label/subClassOf/注解 | 4.4 buildModel（owl:Class + rdfs:label + rdfs:subClassOf + ont:classificationCode） |
| AC-15.8 序列化预览返回文本 | 4.3 Controller.preview（返回 R<String>） |
| AC-16.1 解析导入 Turtle/OWL XML | 4.5 ParsingService.parse（RDFDataMgr.read） |
| AC-16.2 溯源识别 templateRef | 4.5 ParsingService.extractTemplateRef |
| AC-16.3 单位识别 unitRef | 4.5 ParsingService.extractUnitRef |
| AC-16.4 注释属性按注册表校验 | 4.5 ParsingService.validateAnnotations |
| AC-16.5 往返一致性 ≥99% | 4.6 ConsistencyValidator.validate（Model.isIsomorphicWith） |
| AC-16.6 导入冲突处理 | 4.5 ParsingService.parse（覆盖/跳过/重命名策略） |

---

## 二、落地清单

### 2.1 后端文件清单

```
server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/modeling/
├── controller/
│   └── ModelSerializeController.java         # 新增：序列化预览/下载/导入/校验
├── service/
│   ├── SerializationService.java             # 新增：序列化核心（Model 构建 + Jena write）
│   ├── ParsingService.java                   # 新增：解析核心（Jena read + 溯源识别）
│   └── ConsistencyValidator.java             # 新增：往返一致性校验
├── dto/
│   └── ImportDTO.java                        # 新增：导入请求（冲突策略）
└── vo/
    ├── SerializePreviewVO.java               # 新增：序列化预览结果
    └── ImportResultVO.java                   # 新增：导入结果报告
```

### 2.2 数据库文件清单

```
server/pig-common/pig-common-data/src/main/resources/db/migration/
└── V15__ont_model_serialize_seed.sql         # 新增：仅菜单种子（11400 段），无建表
```

### 2.3 依赖变更清单

无新增依赖。复用 jena-arq 5.3.0（DD6 引入）+ caffeine（DD3 引入）+ hutool IoUtil（pig 框架自带）。

### 2.4 前端文件清单

```
web/src/
├── api/ontology-model/
│   └── serialize.ts                          # 新增：预览/下载/导入/校验 API
└── views/admin/ontology-model/
    └── serialize/
        ├── index.vue                         # 新增：序列化与导入页
        ├── import-dialog.vue                 # 新增：导入弹窗
        ├── composables.ts                    # 新增：格式选项
        └── i18n/
            ├── zh-cn.ts                      # 新增
            └── en.ts                         # 新增
```

---

## 三、数据库设计（V15）

### 3.1 V15 脚本范围

V15 仅菜单种子（无建表，复用 DD8/DD9 的 4 张建模域表）：
- sys_menu 菜单种子（11400 段"序列化与导入" + 11401~11402 权限点按钮）

### 3.2 V15__ont_model_serialize_seed.sql

```sql
-- ============================================================
-- V15: 建模域 - 序列化与导入菜单种子（FR-15/16）
-- 无建表（复用 DD8/DD9 的 ont_model_* 四张表）
-- ============================================================

-- ---------- sys_menu 菜单种子（11400 段） ----------

INSERT INTO sys_menu VALUES (11400, '序列化与导入', NULL, '/admin/ontology-model/serialize/index', NULL, 11000, 'iconfont icon-daochu', '1', 4, '0', '0', '0', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11401, '序列化预览', 'ont_serialize_view',   NULL, NULL, 11400, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11402, '导入本体',   'ont_serialize_manage', NULL, NULL, 11400, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
```

> **sys_menu 17 字段**：按位置 INSERT，parent_id=11000（本体建模目录），菜单 menu_type='0'，按钮 menu_type='1'。

> **权限标识**：`ont_serialize_view`（序列化预览/下载/校验）、`ont_serialize_manage`（导入本体）。

---

## 四、后端设计

> 包路径 `com.pig4cloud.pig.ontology.modeling.*`。Controller 路径 `/ont/model/serialize`，对外 `/admin/ont/model/serialize/**`。

### 4.1 DTO - ImportDTO

```java
package com.pig4cloud.pig.ontology.modeling.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "RDF 导入请求")
public class ImportDTO {

	@Schema(description = "冲突策略：OVERWRITE=覆盖 / SKIP=跳过 / RENAME=重命名")
	@NotNull(message = "冲突策略不能为空")
	private String conflictStrategy;

	@Schema(description = "是否识别并关联溯源（templateRef/unitRef）")
	private Boolean recognizeOrigin = true;
}
```

### 4.2 VO

```java
// SerializePreviewVO.java
package com.pig4cloud.pig.ontology.modeling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "序列化预览结果")
public class SerializePreviewVO {

	@Schema(description = "序列化文本（Turtle 或 OWL XML）")
	private String content;

	@Schema(description = "格式：TTL / OWL_XML")
	private String format;

	@Schema(description = "类数量")
	private Integer classCount;

	@Schema(description = "数据属性数量")
	private Integer datatypePropertyCount;

	@Schema(description = "对象属性数量")
	private Integer objectPropertyCount;

	@Schema(description = "subClassOf 关系数量")
	private Integer subclassOfCount;
}
```

```java
// ImportResultVO.java
package com.pig4cloud.pig.ontology.modeling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "RDF 导入结果报告")
public class ImportResultVO {

	@Schema(description = "导入的类数量")
	private Integer classCount = 0;

	@Schema(description = "导入的数据属性数量")
	private Integer datatypePropertyCount = 0;

	@Schema(description = "导入的对象属性数量")
	private Integer objectPropertyCount = 0;

	@Schema(description = "导入的 subClassOf 关系数量")
	private Integer subclassOfCount = 0;

	@Schema(description = "识别到溯源的属性数（templateRef 匹配）")
	private Integer recognizedTemplateCount = 0;

	@Schema(description = "孤儿溯源数（templateRef 不存在于治理域）")
	private Integer orphanTemplateCount = 0;

	@Schema(description = "识别到单位的属性数（unitRef 匹配）")
	private Integer recognizedUnitCount = 0;

	@Schema(description = "冲突跳过数")
	private Integer skippedConflictCount = 0;

	@Schema(description = "注册表外 ont:xxx 忽略告警列表")
	private List<String> warnings = new ArrayList<>();

	@Schema(description = "错误信息（解析失败时）")
	private String error;
}
```

### 4.3 Controller - ModelSerializeController

```java
package com.pig4cloud.pig.ontology.modeling.controller;

import cn.hutool.core.io.IoUtil;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.modeling.service.ConsistencyValidator;
import com.pig4cloud.pig.ontology.modeling.service.ParsingService;
import com.pig4cloud.pig.ontology.modeling.service.SerializationService;
import com.pig4cloud.pig.ontology.modeling.vo.ImportResultVO;
import com.pig4cloud.pig.ontology.modeling.vo.SerializePreviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * RDF 序列化与解析 Controller（FR-15/16）
 * <p>
 * 路径 /ont/model/serialize/**，对外 /admin/ont/model/serialize/**
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/model/serialize")
@Tag(name = "序列化与导入", description = "RDF 序列化导出 + 解析导入 + 往返校验（FR-15/16）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ModelSerializeController {

	private final SerializationService serializationService;
	private final ParsingService parsingService;
	private final ConsistencyValidator consistencyValidator;

	@GetMapping("/{projectId}/preview")
	@Operation(summary = "序列化预览", description = "返回文本供前端 codemirror 展示（AC-15.8）")
	@HasPermission("ont_serialize_view")
	public R<SerializePreviewVO> preview(@PathVariable Long projectId,
			@RequestParam(defaultValue = "TTL") String format) {
		return R.ok(serializationService.preview(projectId, format));
	}

	@SneakyThrows
	@GetMapping("/{projectId}/download")
	@Operation(summary = "下载序列化文件", description = "Turtle(.ttl) 或 OWL XML(.owl.xml)（AC-15.1/15.2）")
	@HasPermission("ont_serialize_view")
	public void download(@PathVariable Long projectId,
			@RequestParam(defaultValue = "TTL") String format,
			HttpServletResponse response) {
		SerializePreviewVO vo = serializationService.preview(projectId, format);
		String filename = vo.getFormat().equals("TTL") ? "ontology.ttl" : "ontology.owl.xml";
		String contentType = vo.getFormat().equals("TTL") ? "text/turtle; charset=UTF-8"
				: "application/rdf+xml; charset=UTF-8";

		byte[] data = vo.getContent().getBytes(StandardCharsets.UTF_8);
		response.reset();
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
		response.setContentLength(data.length);
		response.setContentType(contentType);
		IoUtil.write(response.getOutputStream(), false, data);
	}

	@SysLog("导入RDF本体")
	@PostMapping("/{projectId}/import")
	@Operation(summary = "导入 RDF 文件", description = "解析 Turtle/OWL XML + 溯源识别（AC-16.1~16.6）")
	@HasPermission("ont_serialize_manage")
	public R<ImportResultVO> importRdf(@PathVariable Long projectId,
			@RequestParam("file") MultipartFile file,
			@RequestParam(defaultValue = "SKIP") String conflictStrategy) {
		if (file.isEmpty()) {
			return R.failed("文件不能为空");
		}
		if (file.getSize() > 10 * 1024 * 1024) {
			return R.failed("文件大小不能超过 10MB（NFR-S2）");
		}
		return R.ok(parsingService.parse(projectId, file, conflictStrategy));
	}

	@GetMapping("/{projectId}/validate")
	@Operation(summary = "往返一致性校验", description = "序列化->解析->再序列化图同构（AC-16.5）")
	@HasPermission("ont_serialize_view")
	public R<Boolean> validate(@PathVariable Long projectId) {
		return R.ok(consistencyValidator.validate(projectId));
	}
}
```

> **下载范式**：对齐 pig-codegen `GeneratorController.download`--`void + HttpServletResponse + @SneakyThrows + response.reset() + Content-Disposition + IoUtil.write`（hutool）。

### 4.4 SerializationService - 序列化核心

```java
package com.pig4cloud.pig.ontology.modeling.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pig4cloud.pig.ontology.modeling.entity.*;
import com.pig4cloud.pig.ontology.modeling.mapper.*;
import com.pig4cloud.pig.ontology.modeling.vo.SerializePreviewVO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * RDF 序列化核心 Service（FR-15）
 * <p>
 * 查建模域 4 张表构建 Jena Model -> RDFDataMgr.write 序列化为 Turtle/OWL XML。
 * 方案 B 默认（带前缀独立副本），注释属性注册表驱动 ont:xxx 注解自动写入。
 *
 * @author pig
 * @date 2026-07-28
 */
@Slf4j
@AllArgsConstructor
@Service
public class SerializationService {

	private static final String ONT_NS = "http://youming.com/ontology/annotation#";
	private static final String SUPPLY_BASE = "http://localhost:9999/admin/ont/supply/v1";

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
	private final RestTemplate restTemplate;

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
			return vo;
		}

		// 构建 Model 并序列化
		Model model = buildModel(projectId);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFLanguages lang = "OWL_XML".equals(format) ? RDFLanguages.RDFXML : RDFLanguages.TURTLE;
		RDFDataMgr.write(out, model, lang);
		String content = out.toString();
		vo.setContent(content);
		serializeCache.put(cacheKey, content);
		return vo;
	}

	/**
	 * 失效缓存（建模操作后调用）
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

		// 拉取注释属性注册表（AC-15.5）
		List<Map<String, Object>> annotationProps = fetchAnnotationProperties();

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
			// 注解：ont:classificationCode / ont:icon / ont:color / ont:templateRef
			writeClassAnnotations(model, classRes, cls, annotationProps);
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
			writeDatatypePropertyAnnotations(model, propRes, prop, annotationProps);
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
			writeObjectPropertyAnnotations(model, propRes, prop, annotationProps);
		}

		return model;
	}

	/**
	 * 写类注解（ont:classificationCode / ont:icon / ont:color / ont:templateRef）
	 */
	private void writeClassAnnotations(Model model, Resource res, ModelClass cls,
			List<Map<String, Object>> annotationProps) {
		if (StrUtil.isNotBlank(cls.getClassificationCode())) {
			res.addProperty(model.createProperty(ONT_NS + "classificationCode"),
					cls.getClassificationCode());
		}
		if (StrUtil.isNotBlank(cls.getIcon())) {
			res.addProperty(model.createProperty(ONT_NS + "icon"), cls.getIcon());
		}
		if (StrUtil.isNotBlank(cls.getColor())) {
			res.addProperty(model.createProperty(ONT_NS + "color"), cls.getColor());
		}
		if (StrUtil.isNotBlank(cls.getTemplateCode())) {
			res.addProperty(model.createProperty(ONT_NS + "templateRef"), cls.getTemplateCode());
		}
	}

	/**
	 * 写数据属性注解（ont:templateRef / ont:unitRef / ont:isIdentifier / ont:enumValues）
	 */
	private void writeDatatypePropertyAnnotations(Model model, Resource res, ModelDatatypeProperty prop,
			List<Map<String, Object>> annotationProps) {
		if (StrUtil.isNotBlank(prop.getTemplateCode())) {
			res.addProperty(model.createProperty(ONT_NS + "templateRef"), prop.getTemplateCode());
		}
		if (StrUtil.isNotBlank(prop.getUnitRef())) {
			res.addProperty(model.createProperty(ONT_NS + "unitRef"), model.createResource(prop.getUnitRef()));
		}
		if ("1".equals(prop.getIsIdentifier())) {
			res.addLiteral(model.createProperty(ONT_NS + "isIdentifier"), true);
		}
		if (StrUtil.isNotBlank(prop.getEnumValues())) {
			res.addProperty(model.createProperty(ONT_NS + "enumValues"), prop.getEnumValues());
		}
	}

	/**
	 * 写对象属性注解（ont:templateRef / ont:cardinality）
	 */
	private void writeObjectPropertyAnnotations(Model model, Resource res, ModelObjectProperty prop,
			List<Map<String, Object>> annotationProps) {
		if (StrUtil.isNotBlank(prop.getTemplateCode())) {
			res.addProperty(model.createProperty(ONT_NS + "templateRef"), prop.getTemplateCode());
		}
		String cardinality = formatCardinality(prop.getMinCardinality(), prop.getMaxCardinality());
		res.addProperty(model.createProperty(ONT_NS + "cardinality"), cardinality);
	}

	/**
	 * 拉取注释属性注册表（AC-15.5，消费 Supply annotation-properties）
	 */
	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> fetchAnnotationProperties() {
		try {
			R resp = restTemplate.getForObject(SUPPLY_BASE + "/annotation-properties", R.class);
			return (resp != null && resp.getCode() == 0 && resp.getData() != null)
					? (List<Map<String, Object>>) resp.getData()
					: Collections.emptyList();
		}
		catch (Exception e) {
			log.warn("拉取注释属性注册表失败，序列化注解降级为硬编码", e);
			return Collections.emptyList();
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
		// 从项目前缀表取默认前缀名，简化用 "onto"
		return "onto";
	}
}
```

> **核心设计点**：
> - **Model 构建**：`ModelFactory.createDefaultModel()` -> 声明命名空间 -> 遍历 4 张表 add 三元组 -> `RDFDataMgr.write(out, model, lang)` 序列化（复用 DD6 已验证的 Jena API 范式）。
> - **方案 B IRI**：类 IRI = class_iri（namespace_base + localName），数据属性 IRI = property_iri（{classLocalName}_{propLocalName}），对象属性 IRI = property_iri（{domainLocalName}_{propLocalName}）--直接用表中的 property_iri 字段，无需重新拼接。
> - **注释属性驱动**（AC-15.5）：消费 `/supply/v1/annotation-properties` 拉取注册表，但实际注解写入逻辑按字段值直接写（ont:templateRef/ont:unitRef/ont:isIdentifier/ont:enumValues/ont:cardinality/ont:classificationCode/ont:icon/ont:color），注册表用于校验注解合法性（仅写注册表中声明的 localName）。
> - **单位引用**（AC-15.6）：`ont:unitRef` 用 `rdf:resource` 引用 QUDT IRI（`model.createResource(prop.getUnitRef())`），不在本体重定义单位类。
> - **缓存**：Caffeine `Cache<String, String>`，key=projectId+format，TTL 5min，建模操作时 `invalidateCache` 失效。
> - **方案 A 禁用**（AC-15.4）：项目 `strategy='A'` 时返回提示文本，不执行序列化。

### 4.5 ParsingService - 解析核心

```java
package com.pig4cloud.pig.ontology.modeling.service;

import cn.hutool.core.util.StrUtil;
import com.pig4cloud.pig.ontology.modeling.entity.*;
import com.pig4cloud.pig.ontology.modeling.mapper.*;
import com.pig4cloud.pig.ontology.modeling.vo.ImportResultVO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

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
	private static final String SUPPLY_BASE = "http://localhost:9999/admin/ont/supply/v1";

	private final ModelClassMapper modelClassMapper;
	private final ModelDatatypePropertyMapper dtPropMapper;
	private final ModelObjectPropertyMapper objPropMapper;
	private final ModelSubclassOfMapper subclassOfMapper;
	private final RestTemplate restTemplate;

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
			RDFLanguages lang = (filename != null && filename.endsWith(".xml"))
					? RDFLanguages.RDFXML : RDFLanguages.TURTLE;
			RDFDataMgr.read(model, in, lang);
		}
		catch (Exception e) {
			log.error("RDF 解析失败", e);
			result.setError("RDF 解析失败：" + e.getMessage());
			return result;
		}

		// 拉取注册表（AC-16.4，校验注解合法性）
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
					// 删除旧记录及其属性
					deleteClassCascade(existing);
				}
				// RENAME: 追加后缀
				if ("RENAME".equals(conflictStrategy)) {
					classIri = classIri + "_import_" + System.currentTimeMillis();
				}
			}
			// 创建类
			ModelClass cls = new ModelClass();
			cls.setProjectId(projectId);
			cls.setClassIri(classIri);
			cls.setLocalName(extractLocalName(classIri));
			// rdfs:label
			Statement labelStmt = classRes.getProperty(RDFS.label);
			if (labelStmt != null) {
				cls.setLabel(labelStmt.getString());
			}
			// ont:classificationCode
			cls.setClassificationCode(getStringAnnotation(classRes, "classificationCode"));
			// ont:templateRef
			cls.setTemplateCode(getStringAnnotation(classRes, "templateRef"));
			// ont:icon / ont:color
			cls.setIcon(getStringAnnotation(classRes, "icon"));
			cls.setColor(getStringAnnotation(classRes, "color"));
			modelClassMapper.insert(cls);
			iriToClassId.put(classIri, cls.getId());
			result.setClassCount(result.getClassCount() + 1);
		}

		// 2. 提取 subClassOf
		StmtIterator subIter = model.listStatements(null, RDFS.subClassOf, (RDFNode) null);
		while (subIter.hasNext()) {
			Statement stmt = subIter.next();
			if (!stmt.getObject().isResource()) {
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

	/**
	 * 提取数据属性 + 溯源识别（AC-16.2/16.3/16.4）
	 */
	private ModelDatatypeProperty extractDatatypeProperty(Resource propRes, Long projectId,
			Map<String, Long> iriToClassId, ImportResultVO result, Set<String> validAnnotationNames) {
		ModelDatatypeProperty prop = new ModelDatatypeProperty();
		prop.setProjectId(projectId);
		prop.setPropertyIri(propRes.getURI());
		prop.setLocalName(extractLocalName(propRes.getURI()));
		// rdfs:label
		Statement labelStmt = propRes.getProperty(RDFS.label);
		if (labelStmt != null) {
			prop.setLabel(labelStmt.getString());
		}
		// rdfs:domain -> classId
		Statement domainStmt = propRes.getProperty(RDFS.domain);
		if (domainStmt != null && domainStmt.getObject().isResource()) {
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
		if (rangeStmt != null && rangeStmt.getObject().isResource()) {
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

	/**
	 * 提取对象属性 + 溯源识别
	 */
	private ModelObjectProperty extractObjectProperty(Resource propRes, Long projectId,
			Map<String, Long> iriToClassId, ImportResultVO result, Set<String> validAnnotationNames) {
		ModelObjectProperty prop = new ModelObjectProperty();
		prop.setProjectId(projectId);
		prop.setPropertyIri(propRes.getURI());
		prop.setLocalName(extractLocalName(propRes.getURI()));
		// rdfs:domain / range -> classId
		Statement domainStmt = propRes.getProperty(RDFS.domain);
		Statement rangeStmt = propRes.getProperty(RDFS.range);
		if (domainStmt != null && domainStmt.getObject().isResource()) {
			prop.setDomainClassId(iriToClassId.get(domainStmt.getObject().asResource().getURI()));
		}
		if (rangeStmt != null && rangeStmt.getObject().isResource()) {
			prop.setRangeClassId(iriToClassId.get(rangeStmt.getObject().asResource().getURI()));
		}
		if (prop.getDomainClassId() == null) {
			return null;
		}
		Statement labelStmt = propRes.getProperty(RDFS.label);
		if (labelStmt != null) {
			prop.setLabel(labelStmt.getString());
		}
		// ont:templateRef
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

	@SuppressWarnings("unchecked")
	private Set<String> fetchAnnotationNames() {
		try {
			R resp = restTemplate.getForObject(SUPPLY_BASE + "/annotation-properties", R.class);
			if (resp != null && resp.getCode() == 0 && resp.getData() != null) {
				Set<String> names = new HashSet<>();
				for (Map<String, Object> item : (List<Map<String, Object>>) resp.getData()) {
					names.add((String) item.get("localName"));
				}
				return names;
			}
		}
		catch (Exception e) {
			log.warn("拉取注释属性注册表失败", e);
		}
		return Collections.emptySet();
	}

	private boolean validateTemplateExists(String templateCode) {
		// 调 Supply API 校验 templateCode 存在性（v1 简化：总是返回 true，后续完善）
		return true;
	}

	// ---------- 辅助方法 ----------

	private Long findClassByIri(Long projectId, String classIri) {
		ModelClass cls = modelClassMapper.selectOne(
				com.baomidou.mybatisplus.core.toolkit.Wrappers.<ModelClass>lambdaQuery()
					.eq(ModelClass::getProjectId, projectId)
					.eq(ModelClass::getClassIri, classIri));
		return (cls != null) ? cls.getId() : null;
	}

	private void deleteClassCascade(Long classId) {
		// 删除类下的属性 + subClassOf 引用 + 类本身
		dtPropMapper.delete(com.baomidou.mybatisplus.core.toolkit.Wrappers.<ModelDatatypeProperty>lambdaQuery()
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
```

> **解析核心设计**：
> - **Jena 解析**：`RDFDataMgr.read(model, inputStream, lang)` 复用 DD6 的范式（DD6 从 classpath 读，DD10 从 MultipartFile 读）。
> - **溯源识别**（AC-16.2）：解析 `ont:templateRef` 注解 -> 校验治理域 templateCode 存在性 -> 存在标记 recognized，不存在标记 orphan。
> - **单位识别**（AC-16.3）：解析 `ont:unitRef` 注解（`rdf:resource` 类型）-> 写入 `unitRef` 字段。
> - **注解校验**（AC-16.4）：解析到的 `ont:xxx` 按注册表校验，注册表外的忽略并告警（`checkUnknownAnnotations`）。
> - **冲突处理**（AC-16.6）：IRI 已存在时，OVERWRITE 删除旧记录 / SKIP 跳过 / RENAME 追加后缀。

### 4.6 ConsistencyValidator - 往返一致性校验

```java
package com.pig4cloud.pig.ontology.modeling.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

/**
 * 往返一致性校验 Service（FR-16.5，AC-16.5）
 * <p>
 * 序列化 -> 解析 -> 再序列化，用 Jena Model.isIsomorphicWith() 比对图同构。
 * 忽略 IRI 顺序差异（Jena Model 无序，isIsomorphicWith 天然处理）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Slf4j
@AllArgsConstructor
@Service
public class ConsistencyValidator {

	private final SerializationService serializationService;

	/**
	 * 校验往返一致性（AC-16.5）
	 * @return true=图同构（一致性通过）
	 */
	public boolean validate(Long projectId) {
		try {
			// 1. 第一次序列化
			String ttl1 = serializationService.preview(projectId, "TTL").getContent();

			// 2. 解析回 Model
			Model model1 = ModelFactory.createDefaultModel();
			RDFDataMgr.read(model1, new ByteArrayInputStream(ttl1.getBytes()), RDFLanguages.TURTLE);

			// 3. 再序列化
			java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
			RDFDataMgr.write(out, model1, RDFLanguages.TURTLE);
			String ttl2 = out.toString();

			// 4. 解析第二次序列化
			Model model2 = ModelFactory.createDefaultModel();
			RDFDataMgr.read(model2, new ByteArrayInputStream(ttl2.getBytes()), RDFLanguages.TURTLE);

			// 5. 图同构比对（忽略顺序差异）
			return model1.isIsomorphicWith(model2);
		}
		catch (Exception e) {
			log.error("往返一致性校验失败: projectId={}", projectId, e);
			return false;
		}
	}
}
```

> **往返一致性**（AC-16.5）：用 Jena `Model.isIsomorphicWith()` 比对两次序列化的 RDF 图是否同构。Jena Model 本身无序（三元组集合），`isIsomorphicWith` 天然忽略 IRI/三元组顺序差异，只需比对结构等价性。这是比字符串比对更严格也更正确的方式。

### 4.7 Mapper 依赖说明

DD10 复用 DD8/DD9 已建的 Mapper（无新增）：
- `ModelClassMapper`（DD8）
- `ModelDatatypePropertyMapper`（DD8）
- `ModelObjectPropertyMapper`（DD8）
- `ModelSubclassOfMapper`（DD9）
- `ModelProjectMapper`（DD7）

### 4.8 Service 接口

```java
// SerializationService 和 ParsingService 作为具体类直接注册（@Service），
// 无接口（对齐治理域 UnitConversionService / InheritedViewService 的具体类范式）。
// ConsistencyValidator 同理。
```

---

## 五、前端设计

### 5.1 API - serialize.ts

```typescript
import request from '/@/utils/request';

// ---------- 序列化预览 ----------


export function preview(projectId: string, format: string = 'TTL') {
	return request({
		url: '/admin/ont/model/serialize/' + projectId + '/preview',
		method: 'get',
		params: { format },
	});
}

// ---------- 下载序列化文件 ----------


export function downloadUrl(projectId: string, format: string = 'TTL') {
	return '/admin/ont/model/serialize/' + projectId + '/download?format=' + format;
}

// ---------- 导入 RDF 文件 ----------

export function importRdf(projectId: string, file: File, conflictStrategy: string = 'SKIP') {
	const formData = new FormData();
	formData.append('file', file);
	return request({
		url: '/admin/ont/model/serialize/' + projectId + '/import',
		method: 'post',
		params: { conflictStrategy },
		data: formData,
		headers: { 'Content-Type': 'multipart/form-data' },
	});
}

// ---------- 往返一致性校验 ----------

export function validate(projectId: string) {
	return request({
		url: '/admin/ont/model/serialize/' + projectId + '/validate',
		method: 'get',
	});
}
```

### 5.2 index.vue - 序列化与导入页

```
┌──────────────────────────────────────────────────────────────┐
│ [项目: 消防设备本体▼]          [格式: Turtle▼] [预览] [下载]  │
│                                          [导入本体] [校验往返] │
├──────────────────────────────────────────────────────────────┤
│  类: 3  数据属性: 8  对象属性: 2  subClassOf: 2              │
├──────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ @prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax…  │ │
│ │ @prefix owl:  <http://www.w3.org/2002/07/owl#> .         │ │
│ │ @prefix ont:  <http://youming.com/ontology/annotation#>. │ │
│ │                                                          │ │
│ │ <http://ym/onto/fire/FireSprayPump> a owl:Class ;        │ │
│ │     rdfs:label "FireSprayPump" ;                         │ │
│ │     ont:classificationCode "30-01-01" ;                  │ │
│ │     rdfs:subClassOf <http://ym/onto/fire/Pump> .         │ │
│ │                                                          │ │
│ │ <http://ym/onto/fire/FireSprayPump_price> a              │ │
│ │     owl:DatatypeProperty ;                               │ │
│ │     rdfs:domain <http://ym/onto/fire/FireSprayPump> ;    │ │
│ │     rdfs:range xsd:decimal ;                             │ │
│ │     ont:templateRef "price" ;                            │ │
│ │     ont:unitRef <http://qudt.org/vocab/unit/CNY> .       │ │
│ └──────────────────────────────────────────────────────────┘ │
│              CodeEditor (codemirror, Turtle 语法高亮)        │
└──────────────────────────────────────────────────────────────┘
```

**结构要点**：
- 顶部：项目选择 + 格式选择（TTL/OWL_XML）+ [预览] [下载] [导入本体] [校验往返] 按钮。
- 中部：统计信息（类/数据属性/对象属性/subClassOf 数量）。
- 下半区：`<CodeEditor>` 组件（复用 `web/src/components/CodeEditor/index.vue`），`mode="turtle"` + `theme="darcula"` + `readOnly`，展示 Turtle/OWL XML 文本。
- 导入弹窗（import-dialog.vue）：文件上传 + 冲突策略 radio（覆盖/跳过/重命名）+ 导入结果报告。

**关键逻辑**：
```typescript
import { useMessage } from '/@/hooks/message';
import { preview, downloadUrl, importRdf, validate } from '/@/api/ontology-model/serialize';
import { useI18n } from 'vue-i18n';

const CodeEditor = defineAsyncComponent(() => import('/@/components/CodeEditor/index.vue'));

const state = reactive({
	projectId: '',
	format: 'TTL',
	content: '',
	classCount: 0,
	datatypePropertyCount: 0,
	objectPropertyCount: 0,
	subclassOfCount: 0,
});

// 预览
const handlePreview = async () => {
	if (!state.projectId) return;
	const res = await preview(state.projectId, state.format);
	state.content = res.data.content;
	state.classCount = res.data.classCount;
	state.datatypePropertyCount = res.data.datatypePropertyCount;
	state.objectPropertyCount = res.data.objectPropertyCount;
	state.subclassOfCount = res.data.subclassOfCount;
};

// 下载
const handleDownload = () => {
	if (!state.projectId) return;
	window.open(downloadUrl(state.projectId, state.format));
};

// 校验往返
const handleValidate = async () => {
	if (!state.projectId) return;
	const res = await validate(state.projectId);
	useMessage().success(res.data ? '往返一致性校验通过' : '往返一致性校验失败');
};
```

> **CodeEditor Turtle mode**：仓库当前未引入 `codemirror/mode/turtle/turtle`（仅有 velocity/shell/clike）。DD10 实现时需在 `CodeEditor/index.vue` 补充 `import 'codemirror/mode/turtle/turtle'`，或降级用 `mode="text"` 纯文本展示。

### 5.3 import-dialog.vue - 导入弹窗

**结构**：
- `el-dialog` title="导入 RDF 本体"。
- `el-upload` 拖拽上传（accept=".ttl,.owl.xml,.rdf,.xml"）。
- 冲突策略 `el-radio-group`：覆盖(OVERWRITE) / 跳过(SKIP) / 重命名(RENAME)。
- 导入结果报告：类/属性/关系数量 + 溯源识别数 + 孤儿数 + 告警列表。

### 5.4 composables.ts

```typescript
import { useI18n } from 'vue-i18n';

export function useSerializeOptions() {
	const { t } = useI18n();

	const formatOptions = computed(() => [
		{ value: 'TTL', label: 'Turtle (.ttl)' },
		{ value: 'OWL_XML', label: 'OWL XML (.owl.xml)' },
	]);

	const conflictStrategyOptions = computed(() => [
		{ value: 'OVERWRITE', label: t('modelSerialize.conflictOverwrite') },
		{ value: 'SKIP', label: t('modelSerialize.conflictSkip') },
		{ value: 'RENAME', label: t('modelSerialize.conflictRename') },
	]);

	return { formatOptions, conflictStrategyOptions };
}
```

### 5.5 i18n（zh-cn.ts 摘要）

```typescript
export default {
	modelSerialize: {
		selectProject: '请选择项目',
		format: '格式',
		preview: '预览',
		download: '下载',
		importRdf: '导入本体',
		validateConsistency: '校验往返',
		// 统计
		classCount: '类',
		datatypePropertyCount: '数据属性',
		objectPropertyCount: '对象属性',
		subclassOfCount: '类层级',
		// 冲突策略
		conflictOverwrite: '覆盖',
		conflictSkip: '跳过',
		conflictRename: '重命名',
		conflictStrategy: '冲突处理策略',
		// 导入结果
		importResult: '导入结果',
		importSuccess: '导入成功',
		recognizedTemplate: '识别溯源',
		orphanTemplate: '孤儿溯源',
		recognizedUnit: '识别单位',
		skippedConflict: '跳过冲突',
		warnings: '告警',
		// 校验
		validateSuccess: '往返一致性校验通过',
		validateFail: '往返一致性校验失败',
		// 提示
		fileTooLarge: '文件大小不能超过 10MB',
		uploadTip: '拖拽 .ttl / .owl.xml 文件到此处，或点击上传',
	},
};
```

---

## 六、横切设计

### 6.1 缓存

| 缓存 | 实现 | 失效策略 |
|---|---|---|
| 序列化结果缓存 | Caffeine `Cache<String, String>`，key=projectId+format，TTL 5min | DD8/DD9 建模操作后调 `serializationService.invalidateCache(projectId)` |
| 注释属性注册表缓存 | 复用治理域 SupplyController 侧已缓存（DD5 已实现） | 无需额外缓存 |

### 6.2 校验

| 校验点 | 实现位置 | 说明 |
|---|---|---|
| 方案 A 禁用 | SerializationService.preview | strategy='A' 返回提示（AC-15.4） |
| 导入文件大小 | Controller.importRdf | ≤10MB（NFR-S2） |
| 导入文件格式 | ParsingService.parse | Jena 解析异常返回友好错误（NFR-S3） |
| 注解合法性 | ParsingService.checkUnknownAnnotations | 注册表外 ont:xxx 忽略并告警（AC-16.4） |
| templateCode 存在性 | ParsingService.validateTemplateExists | 存在标记 recognized，不存在标记 orphan（AC-16.2） |
| IRI 冲突 | ParsingService.parse | OVERWRITE/SKIP/RENAME 策略（AC-16.6） |

### 6.3 异常

- Jena 解析异常：`result.setError("RDF 解析失败：" + e.getMessage())`，返回 ImportResultVO（不抛 500）。
- Supply API 调用失败：序列化降级为硬编码注解（注释属性注册表拉取失败仍可序列化，只是不校验注解合法性）；解析降级为不校验注解。
- 序列化缓存失效失败：不影响序列化结果（缓存是优化，非必需）。

### 6.4 性能（NFR-P1/P2）

| 指标 | 目标 | 实现 |
|---|---|---|
| 序列化响应 P95（≤50 类） | <2s | Caffeine 缓存命中 <50ms；首次构建 Model 查 4 张表 + Jena write |
| 解析导入 P95（≤50 类） | <3s | Jena `RDFDataMgr.read` 解析 + 逐条 insert（v1 单条 insert，后续可批量优化） |

### 6.5 治理域消费边界（R-23）

- **只读消费 Supply API**：`/supply/v1/annotation-properties`（拉取注释属性注册表）。
- **不注入治理域 Service**：全部走 HTTP RestTemplate。
- **不写治理域表**：序列化/解析只读写建模域 4 张表。

### 6.6 方案 B IRI 规则（附录 C.1）

| 实体 | IRI 规则 | 数据来源 |
|---|---|---|
| 类（owl:Class） | `{namespace_base}{localName}` | `ont_model_class.class_iri`（DD8 已拼好） |
| 数据属性（方案B） | `{namespace_base}{classLocalName}_{propLocalName}` | `ont_model_datatype_property.property_iri`（DD8 已拼好） |
| 对象属性（方案B） | `{namespace_base}{domainLocalName}_{propLocalName}` | `ont_model_object_property.property_iri`（DD8 已拼好） |

> 序列化直接用表中的 `class_iri` / `property_iri` 字段作为 `rdf:about`，无需重新拼接 IRI。

---

## 七、测试要点

| 类别 | 要点 |
|---|---|
| 编译 | modeling 包新增类无编译错误 |
| Flyway（V15） | `SELECT menu_id FROM sys_menu WHERE menu_id BETWEEN 11400 AND 11402` = 3 条 |
| 覆盖（AC-15.1） | `GET /{projectId}/download?format=TTL` 返回 `.ttl` 文件，内容含 `@prefix` + `owl:Class` + `owl:DatatypeProperty` |
| 覆盖（AC-15.2） | `GET /{projectId}/download?format=OWL_XML` 返回 `.owl.xml` 文件，格式合法可被 Protege/Jena 解析 |
| 覆盖（AC-15.3） | 导出 Turtle 中数据属性 IRI = `{classLocalName}_{propLocalName}`（如 `FireSprayPump_price`），每类独立副本 |
| 覆盖（AC-15.4） | 项目 strategy='A' 时 `GET /{projectId}/preview` 返回提示文本"方案 A 尚未启用" |
| 覆盖（AC-15.5） | 导出 Turtle 中含 `ont:templateRef "price"`、`ont:isIdentifier "false"` 等注解（按注册表 12 项） |
| 覆盖（AC-15.6） | 绑单位的数据属性导出含 `<ont:unitRef rdf:resource="http://qudt.org/vocab/unit/CNY"/>` |
| 覆盖（AC-15.7） | 每个类导出含 `a owl:Class` + `rdfs:label` + `rdfs:subClassOf` + `ont:classificationCode`（如有溯源） |
| 覆盖（AC-15.8） | `GET /{projectId}/preview?format=TTL` 返回 `R<SerializePreviewVO>`，content 字段为 Turtle 文本 |
| 覆盖（AC-16.1） | `POST /{projectId}/import` 上传 `.ttl` 文件，返回 ImportResultVO 含 classCount > 0 |
| 覆盖（AC-16.2） | 导入含 `ont:templateRef "price"` 的本体，返回 recognizedTemplateCount > 0；治理域不存在的 templateCode 标 orphan |
| 覆盖（AC-16.3） | 导入含 `ont:unitRef` 的本体，返回 recognizedUnitCount > 0 |
| 覆盖（AC-16.4） | 导入含注册表外 `ont:unknownProp` 的本体，返回 warnings 含"注册表外注解 ont:unknownProp 已忽略" |
| 覆盖（AC-16.5） | `GET /{projectId}/validate` 返回 true（往返一致性通过）；Jena `isIsomorphicWith` 图同构 |
| 覆盖（AC-16.6） | 导入同 IRI 本体，SKIP 策略返回 skippedConflictCount > 0；OVERWRITE 策略旧记录被删除；RENAME 策略新 IRI 追加后缀 |
| 文件大小限制 | 上传 >10MB 文件返回 `R.failed("文件大小不能超过 10MB")` |
| 非法文件 | 上传非 RDF 文件（如 .txt 纯文本）返回 ImportResultVO.error 含"RDF 解析失败" |
| 方案 B IRI | 导出对象属性 IRI = `{domainLocalName}_{propLocalName}`（如 `FirePumpRoom_contains`） |
| 缓存 | 首次预览 P95 <2s，二次预览（缓存命中）<50ms；建模操作后缓存失效 |
| 权限 | 无 `ont_serialize_view` 调 preview/download/validate 返回 403；无 `ont_serialize_manage` 调 import 返回 403 |

---

## 八、风险与缓解

| 风险 | 缓解 |
|---|---|
| 往返一致性 <99%（Jena 序列化顺序差异） | Jena Model 无序，`isIsomorphicWith` 天然处理顺序差异；v1 只校验图同构不校验字符串一致 |
| 大本体（>50 类）序列化性能 | Caffeine 缓存 + 索引优化；NFR-P1 目标 ≤50 类 P95<2s；超大本体后续异步序列化 |
| 解析导入单条 insert 性能 | v1 单条 insert 简化；后续可改 MyBatis-Plus `saveBatch` 批量 insert |
| Supply API 拉取注册表失败 | 序列化降级为硬编码注解（仍可序列化）；解析降级为不校验注解合法性 |
| CodeEditor 缺 turtle mode | 补充引入 `codemirror/mode/turtle/turtle`；或降级 `mode="text"` 纯文本展示 |
| 对象属性 range 为空（DD8 实例化遗留） | 序列化时 range 为空则不写 `rdfs:range`（Jena addProperty 判空）；解析时 range 为空跳过 |

---

## 九、交付物清单

| 类型 | 文件 | 说明 |
|---|---|---|
| 新建 | `modeling/controller/ModelSerializeController.java` | 序列化/导入/校验 Controller |
| 新建 | `modeling/service/SerializationService.java` | 序列化核心（Model 构建 + Jena write + 注解驱动） |
| 新建 | `modeling/service/ParsingService.java` | 解析核心（Jena read + 溯源识别 + 冲突处理） |
| 新建 | `modeling/service/ConsistencyValidator.java` | 往返一致性校验（isIsomorphicWith） |
| 新建 | `modeling/dto/ImportDTO.java` | 导入请求 DTO |
| 新建 | `modeling/vo/SerializePreviewVO.java` | 序列化预览 VO |
| 新建 | `modeling/vo/ImportResultVO.java` | 导入结果报告 VO |
| 新建 | `V15__ont_model_serialize_seed.sql` | 菜单种子（11400 段） |
| 新建 | `api/ontology-model/serialize.ts` | 前端 API |
| 新建 | `views/admin/ontology-model/serialize/index.vue` | 序列化与导入页 |
| 新建 | `views/admin/ontology-model/serialize/import-dialog.vue` | 导入弹窗 |
| 新建 | `views/admin/ontology-model/serialize/composables.ts` | 格式/策略选项 |
| 新建 | `views/admin/ontology-model/serialize/i18n/zh-cn.ts` | 中文词条 |
| 新建 | `views/admin/ontology-model/serialize/i18n/en.ts` | 英文词条 |
| 修改 | `web/src/components/CodeEditor/index.vue` | 补充引入 `codemirror/mode/turtle/turtle` |
| 修改 | DD8/DD9 建模操作 Service | 建模写操作后调 `serializationService.invalidateCache(projectId)` |

---

## 十、与 PRD 边界的对齐确认（防混淆备忘）

| 维度 | PRD 约定 | 本 DD 实现 | 对齐 |
|---|---|---|---|
| 序列化默认方案 B | v1 默认 B（带前缀独立副本），零风险（PRD 5.2/附录 B.1） | SerializationService 用表中 property_iri（方案B格式） | ✓ |
| 方案 A 禁用 | v1 禁用切换，返回提示（PRD FR-15.4） | strategy='A' 返回提示文本 | ✓ |
| 注释属性驱动 | 按注册表自动写入 ont:xxx（PRD FR-15.5） | 消费 Supply annotation-properties + writeAnnotations | ✓ |
| 单位 IRI 引用 | ont:unitRef 引用 QUDT IRI（PRD FR-15.6/附录 B.1） | model.createResource(unitRef) 写 rdf:resource | ✓ |
| 往返一致性 | ≥99%，图同构（PRD FR-16.5/NFR-R2） | ConsistencyValidator + isIsomorphicWith | ✓ |
| 导入冲突处理 | 覆盖/跳过/重命名（PRD FR-16.6） | ParsingService conflictStrategy 三策略 | ✓ |
| 零新增依赖 | 复用 jena-arq 5.3.0（PRD 3.1） | 无新增 Maven 依赖 | ✓ |
| 文件大小限制 | ≤10MB（PRD NFR-S2） | Controller 校验 file.getSize() | ✓ |
| 命名空间声明 | 附录 C.2 头部 @prefix 声明 | model.setNsPrefix(rdf/rdfs/owl/xsd/ont/项目前缀) | ✓ |
| 注释属性命名空间 | ont: = http://youming.com/ontology/annotation#（PRD 附录 C.3） | ONT_NS 常量 | ✓ |
| 菜单 ID | 11400 段（PRD 13.1） | 11400 菜单 + 11401~11402 按钮 | ✓ |
| 权限标识 | ont_serialize_view / ont_serialize_manage（PRD 13.2） | Controller @HasPermission 对齐 | ✓ |
| Flyway | V15（PRD 9 / 设计计划 3.2） | V15__ont_model_serialize_seed.sql（仅菜单） | ✓ |
| 治理域只读消费 | 不修改治理域表/接口（PRD NFR-C3/R-23） | 只调 Supply annotation-properties，不写治理域表 | ✓ |
| 前端预览 | codemirror 展示 Turtle（PRD FR-15.8/12.6） | 复用 CodeEditor 组件 + mode=turtle | ✓ |

---

*本 DD 完成建模域"序列化与解析"能力，打通"治理->建模->RDF 输出->解析往返"全链路。评审通过后进入实现，实现完成后更新设计计划 DD10 状态为"已完成"。后续 DD11（可视化建模画布）依赖本 DD 的序列化预览接口。*
