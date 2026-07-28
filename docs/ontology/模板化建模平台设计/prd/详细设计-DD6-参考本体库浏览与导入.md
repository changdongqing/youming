# 详细设计-DD6-参考本体库浏览与导入（贯穿）

| 项 | 内容 |
|---|---|
| 文档名称 | 详细设计-DD6-参考本体库浏览与导入 |
| 里程碑 | 贯穿（FR-7，依赖 M2+M3） |
| 上游 PRD | 《本体模板化治理功能产品需求文档.md》v1.2 FR-7、NFR-8、第五节、第九节 9.5（sourceRef）、第十节 10.6、第十一节 11.7、第十二节 12.6、第十三节 13.1/13.2、第十四节 贯穿、附录 C、R-8/R-16 |
| 设计计划 | 《详细设计计划.md》DD6 |
| 编写日期 | 2026-07-28 |
| 文档状态 | 待实现 |
| 前置依赖 | DD1（M0）已完成：模块骨架、V5 种子（参考本体菜单 10500）；DD3（M2）已完成：`ClassTemplate`/`ClassTemplateSaveDTO`/`ClassTemplateService` + 编码规则；DD4（M3）已完成：`Unit`/`UnitService` + `ont_quantity_kind`/`ont_unit`；DD5（M4）已完成：注释属性注册表。 |

---

## 一、设计目标与范围

### 1.1 目标

在 DD1~DD5 基础上，实现**参考本体库的只读浏览与引用式导入**，使 youming 具备：

- 平台内置只读浏览三套国际通用本体（QUDT / Brick / CCO），展示 `rdfs:label`/`rdfs:comment`/`skos:definition`/IRI（FR-7，AC-7.1）。
- 浏览器与治理资产打通：QUDT 单位一键导入 `ont_unit`（FR-3 custom/builtin 单位，含 qudtIri/symbol/换算系数）；Brick 类一键导入 `ont_class_template`（`rdfs:subClassOf` 映射 `parent_id` 形成分类模板树，R-16 取主父类）（FR-7，AC-7.2/7.3）。
- 大文件性能：参考本体为 MB 级 TTL（最大 6.8M），后端 Jena 解析 + Caffeine 缓存 Model，前端分页，不全量加载（AC-7.4，R-8）。
- 导入去重：重复导入已存在的 `qudtIri`/`templateCode` 返回提示，不重复新增（AC-7.5）。
- 导入项挂来源溯源（`sourceRef='qudt'`/`sourceRef='brick'`，AC-7.3）。

### 1.2 范围（本 DD 做 / 不做）

| 做（贯穿） | 不做（后续/边界） |
|---|---|
| 三套本体只读浏览（QUDT 单位/Brick 类/CCO 注释属性，分页） | CCO 注释属性导入（DD5 已有内置 12 项，CCO 仅浏览） |
| QUDT 单位导入 `ont_unit`（AC-7.2） | 建模侧以 IRI 引用（建模侧职责，本功能只导入到治理资产） |
| Brick 类导入 `ont_class_template`（subClassOf->parent_id，R-16 取主父类） | OWL 推理（导入是数据映射，不依赖推理机，OoS） |
| Jena TTL 解析 + Caffeine Model 缓存（AC-7.4） | 全量 2925 个 QUDT 单位批量导入（按需单条/多条导入，OoS） |
| V11 种子：参考本体权限点按钮 | Brick 多继承完整保留（多继承仅取主父类进 parent_id，其余记 source_ref 溯源，R-16） |
| 前端三 Tab 浏览 + 导入按钮 | 参考本体版本升级/在线更新（本版本锁定本地 TTL 文件） |

> **边界声明（呼应 PRD 5.2 / R-8 / R-11 / R-16）**：
> - **引用而非自建**：不在本程序重定义 QUDT/Brick/CCO 的类，只引用其 IRI 作为取值/导入来源（PRD 5.2）。
> - **导入是数据映射，非本体语义**：Brick subClassOf 映射为 ont_class_template.parent_id 是结构映射，不依赖 OWL 推理机（OoS）。
> - **Brick 多继承取主父类**：Brick 类可能有多重 subClassOf，导入时取第一个 `brick:` 命名空间父类作 parent_id（主父类），多继承仅记 source_ref 溯源、不进 ont_class_hierarchy（R-16）。
> - **只读浏览**：参考本体文件只读，本功能不修改 TTL 文件；浏览数据缓存内存，不落库。

### 1.3 验收映射（贯穿 DoD）

| PRD AC | 本 DD 实现点 |
|---|---|
| AC-7.1 可浏览 QUDT 单位（按量纲）、Brick 类层次（subClassOf）、CCO 注释属性，展示 label 与 IRI | 4.3 ReferenceController 三个浏览端点 + 4.4 Jena Model 查询 |
| AC-7.2 从 QUDT 浏览器选中 unit:KiloGM 导入到 ont_unit，等价于新增 custom/builtin 单位（含 qudtIri/symbol/换算系数） | 4.5 ReferenceImportService.importQudtUnit（按 IRI 查 Model 提取字段 -> 查重 -> 量纲匹配 -> 落库） |
| AC-7.3 导入项挂来源溯源（sourceRef='qudt'/'brick'） | 4.5 导入落库设 source='builtin' + sourceRef='qudt'/'brick' |
| AC-7.4 浏览 MB 级 TTL 不卡顿（后端解析缓存，前端首屏 <1s、分页） | 4.4 Caffeine 缓存 Jena Model（TTL 30min，首解析 ~5s，缓存后 <50ms）+ 前端分页 |
| AC-7.5 重复导入已存在 qudtIri 返回提示（不重复新增） | 4.5 导入前查 ont_unit.qudtIri / ont_class_template.templateCode 查重 |

---

## 二、落地清单

### 2.1 后端文件清单

> 包根 `com.pig4cloud.pig.ontology`。浏览/导入逻辑较重，用具体 Service 类实现（浏览无独立 Entity，导入复用 DD3/DD4 已有 Entity/Service）。

```
server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/
├── api/vo/
│   ├── ReferenceOntologyVO.java           # 参考本体元信息（key/name/file/version/itemCount）
│   ├── ReferenceUnitVO.java               # QUDT 单位浏览项
│   ├── ReferenceClassVO.java              # Brick 类浏览项
│   └── ReferenceAnnotationPropertyVO.java  # CCO 注释属性浏览项
├── service/
│   ├── ReferenceOntologyService.java       # 浏览（Jena 解析 + Caffeine 缓存 + 分页查询）
│   └── ReferenceImportService.java         # 导入（QUDT 单位 + Brick 类）
└── controller/
    └── ReferenceController.java            # /ont/reference/**（10.6）
```

### 2.2 数据库文件清单

> **V5 已建参考本体菜单 10500（permission=NULL 占位）**，本期不返工 V5，只新增 V11。

```
server/pig-common/pig-common-data/src/main/resources/db/migration/
└── V11__ont_reference_seed.sql             # 新增：参考本体权限点按钮
```

### 2.3 依赖变更清单

```
server/pig-ontology/pig-ontology-biz/pom.xml  # 新增 org.apache.jena:jena-arq（TTL 解析）
```

### 2.4 前端文件清单

```
web/src/
├── api/ontology/
│   └── reference.ts                         # 浏览 + 导入 API
└── views/admin/ontology/
    └── reference/
        ├── index.vue                        # 三 Tab（QUDT/Brick/CCO）+ 分页表格 + 导入按钮
        └── i18n/
            ├── zh-cn.ts
            └── en.ts
```

> 路径说明：sys_menu path 为 `/admin/ontology/reference/index`（V5 已配 10500），vue 文件须放 `views/admin/ontology/reference/index.vue`。

---

## 三、数据库设计（V11 种子）

### 3.1 `V11__ont_reference_seed.sql`

```sql
-- ============================================================
-- V11__ont_reference_seed.sql
-- 参考本体库：权限点按钮
-- 对应 PRD v1.2 FR-7 AC-7.1、第十三节 13.2
-- 依赖 V5（菜单 10500）
-- ============================================================

-- ---------- 参考本体权限点按钮（挂在参考本体库菜单 10500 下） ----------
-- 浏览走 ont_ref_view；导入复用 ont_unit_manage（QUDT）/ ont_class_tpl_manage（Brick），对齐 PRD 10.6
INSERT INTO sys_menu VALUES (10501, '参考本体浏览', 'ont_ref_view', NULL, NULL, 10500, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
```

> 说明：
> - 浏览权限点 `ont_ref_view`（V11 新建 10501）。
> - 导入权限复用已有：QUDT 单位导入走 `ont_unit_manage`（DD4 V9 已建 10301~10303），Brick 类导入走 `ont_class_tpl_manage`（DD3 V8 已建 10201~10203），不重复建权限点（对齐 PRD 10.6 权限列）。
> - 10500 段菜单（V5）+ 10501 浏览权限点（V11）齐全。

---

## 四、后端设计

### 4.1 依赖 `pig-ontology-biz/pom.xml`

```xml
<!-- RDF/Turtle 解析：Apache Jena（DD6 参考本体库浏览，AC-7.4） -->
<dependency>
    <groupId>org.apache.jena</groupId>
    <artifactId>jena-arq</artifactId>
</dependency>
```

> jena-arq 含 jena-core + riot TTL 解析器。版本由显式声明（5.3.0，兼容 JDK 21 + Spring Boot 4）。

### 4.2 VO 四件

```java
// ReferenceOntologyVO.java（参考本体元信息）
@Data
@Schema(description = "参考本体元信息")
public class ReferenceOntologyVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "本体标识：qudt/brick/cco") private String key;
    @Schema(description = "显示名") private String name;
    @Schema(description = "TTL 文件路径") private String file;
    @Schema(description = "版本") private String version;
    @Schema(description = "条目数（单位/类/注释属性）") private Long itemCount;
}

// ReferenceUnitVO.java（QUDT 单位浏览项，AC-7.1）
@Data
@Schema(description = "QUDT 单位浏览项")
public class ReferenceUnitVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "QUDT IRI") private String iri;
    @Schema(description = "英文 label") private String label;
    @Schema(description = "符号") private String symbol;
    @Schema(description = "量纲 IRI") private String quantityKindIri;
    @Schema(description = "换算系数") private String conversionMultiplier;
    @Schema(description = "科学计数系数") private String conversionMultiplierSn;
    @Schema(description = "UCUM 编码") private String ucumCode;
    @Schema(description = "描述") private String description;
}

// ReferenceClassVO.java（Brick 类浏览项，AC-7.1）
@Data
@Schema(description = "Brick 类浏览项")
public class ReferenceClassVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "类 IRI") private String iri;
    @Schema(description = "显示名（rdfs:label 或本地名）") private String label;
    @Schema(description = "定义（skos:definition）") private String definition;
    @Schema(description = "父类 IRI（主父类）") private String parentIri;
    @Schema(description = "是否弃用") private Boolean deprecated;
}

// ReferenceAnnotationPropertyVO.java（CCO 注释属性浏览项，AC-7.1）
@Data
@Schema(description = "CCO 注释属性浏览项")
public class ReferenceAnnotationPropertyVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "注释属性 IRI") private String iri;
    @Schema(description = "显示名（rdfs:label）") private String label;
    @Schema(description = "定义（skos:definition）") private String definition;
}
```

### 4.3 Controller `ReferenceController.java`

```java
package com.pig4cloud.pig.ontology.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.vo.ReferenceAnnotationPropertyVO;
import com.pig4cloud.pig.ontology.api.vo.ReferenceClassVO;
import com.pig4cloud.pig.ontology.api.vo.ReferenceOntologyVO;
import com.pig4cloud.pig.ontology.api.vo.ReferenceUnitVO;
import com.pig4cloud.pig.ontology.service.ReferenceImportService;
import com.pig4cloud.pig.ontology.service.ReferenceOntologyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 参考本体库 Controller（FR-7）
 * <p>
 * 路径 /ont/reference/**，对外 /admin/ont/reference/**（对齐 PRD 10.6）。
 * <p>
 * 注意：控制器须带 /ont 前缀（对齐 DD2~DD5 Controller 范式，否则 404）。
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/reference")
@Tag(name = "参考本体库", description = "QUDT/Brick/CCO 只读浏览 + 引用式导入（FR-7）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ReferenceController {

	private final ReferenceOntologyService referenceOntologyService;

	private final ReferenceImportService referenceImportService;

	@GetMapping("/ontologies")
	@Operation(summary = "参考本体列表", description = "列出三套本体元信息（10.6）")
	@HasPermission("ont_ref_view")
	public R<List<ReferenceOntologyVO>> ontologies() {
		return R.ok(referenceOntologyService.ontologies());
	}

	@GetMapping("/{ont}/units")
	@Operation(summary = "QUDT 单位分页", description = "按量纲过滤，展示 label/IRI（10.6，AC-7.1）")
	@HasPermission("ont_ref_view")
	public R<IPage<ReferenceUnitVO>> units(@PathVariable String ont, @ParameterObject Page page,
			@RequestParam(required = false) String quantityKindIri,
			@RequestParam(required = false) String keyword) {
		return R.ok(referenceOntologyService.pageUnits(ont, page, quantityKindIri, keyword));
	}

	@GetMapping("/{ont}/classes")
	@Operation(summary = "Brick 类分页", description = "展示 label/IRI/subClassOf（10.6，AC-7.1）")
	@HasPermission("ont_ref_view")
	public R<IPage<ReferenceClassVO>> classes(@PathVariable String ont, @ParameterObject Page page,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String parentIri) {
		return R.ok(referenceOntologyService.pageClasses(ont, page, keyword, parentIri));
	}

	@GetMapping("/{ont}/annotation-properties")
	@Operation(summary = "CCO 注释属性分页", description = "展示 label/IRI（10.6，AC-7.1）")
	@HasPermission("ont_ref_view")
	public R<IPage<ReferenceAnnotationPropertyVO>> annotationProperties(@PathVariable String ont,
			@ParameterObject Page page, @RequestParam(required = false) String keyword) {
		return R.ok(referenceOntologyService.pageAnnotationProperties(ont, page, keyword));
	}

	@SysLog("导入QUDT单位")
	@PostMapping("/qudt/unit/import")
	@Operation(summary = "导入 QUDT 单位", description = "导入到 ont_unit，含 qudtIri/symbol/换算系数（10.6，AC-7.2/7.3/7.5）")
	@HasPermission("ont_unit_manage")
	public R importQudtUnit(@RequestParam String iri) {
		return referenceImportService.importQudtUnit(iri);
	}

	@SysLog("导入Brick类")
	@PostMapping("/brick/class/import")
	@Operation(summary = "导入 Brick 类", description = "导入为分类模板，subClassOf->parent_id（10.6，AC-7.3/7.5，R-16）")
	@HasPermission("ont_class_tpl_manage")
	public R importBrickClass(@RequestParam String iri) {
		return referenceImportService.importBrickClass(iri);
	}

}
```

### 4.4 ReferenceOntologyService（Jena 解析 + 缓存 + 浏览，AC-7.4 核心）

> **算法目标（AC-7.4，R-8）**：
> 1. 首次访问某本体时，用 Jena `RDFParser` 加载 TTL 为 `Model`，缓存到 Caffeine（TTL 30min）。
> 2. 浏览查询从缓存 Model 用 SPARQL/`model.listStatements()` 过滤 + 内存分页。
> 3. 后续访问命中缓存，查询 <50ms。

```java
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
import lombok.AllArgsConstructor;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 参考本体库浏览 Service（FR-7，AC-7.1/7.4）
 * <p>
 * Jena 解析 TTL + Caffeine 缓存 Model（复用 DD3 InheritedViewService 缓存范式）。
 * 三套本体：qudt（单位）/ brick（类）/ cco（注释属性）。
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
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
	private static final java.util.Map<String, String> ONT_FILES = java.util.Map.of(
			"qudt", "qudt/qudt-all.ttl",
			"brick", "brick/Brick-only.ttl",
			"cco", "CommonCoreOntologies/CommonCoreOntologiesMerged.ttl");

	/** 本体显示名 */
	private static final java.util.Map<String, String> ONT_NAMES = java.util.Map.of(
			"qudt", "QUDT（单位/量纲）",
			"brick", "Brick（建筑领域）",
			"cco", "Common Core Ontologies（中层本体）");

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
	 * QUDT 单位分页（AC-7.1）。
	 * SPARQL 查 qudt:Unit 实体的 label/symbol/conversionMultiplier/hasQuantityKind/ucumCode。
	 */
	public IPage<ReferenceUnitVO> pageUnits(String ont, Page page, String quantityKindIri, String keyword) {
		Model model = getModel("qudt");
		StringBuilder sparql = new StringBuilder();
		sparql.append("PREFIX qudt: <http://qudt.org/schema/qudt/> ");
		sparql.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ");
		sparql.append("PREFIX unit: <http://qudt.org/vocab/unit/> ");
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
			sparql.append("  FILTER(STR(?qk) = '").append(quantityKindIri).append("') ");
		}
		if (StrUtil.isNotBlank(keyword)) {
			sparql.append("  FILTER(CONTAINS(LCASE(COALESCE(STR(?label), '')), LCASE('").append(escapeSparql(keyword)).append("'))) ");
		}
		sparql.append("} ORDER BY ?label");
		return executeUnitQuery(model, sparql.toString(), page);
	}

	/**
	 * Brick 类分页（AC-7.1）。
	 * SPARQL 查 owl:Class 实体的 label/definition/subClassOf/deprecated。
	 */
	public IPage<ReferenceClassVO> pageClasses(String ont, Page page, String keyword, String parentIri) {
		Model model = getModel("brick");
		StringBuilder sparql = new StringBuilder();
		sparql.append("PREFIX brick: <https://brickschema.org/schema/Brick#> ");
		sparql.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ");
		sparql.append("PREFIX owl: <http://www.w3.org/2002/07/owl#> ");
		sparql.append("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> ");
		sparql.append("SELECT ?iri ?label ?def ?parent ?dep WHERE { ");
		sparql.append("  ?iri a owl:Class . ");
		sparql.append("  FILTER(STRSTARTS(STR(?iri), 'https://brickschema.org/schema/Brick#')) ");
		sparql.append("  OPTIONAL { ?iri rdfs:label ?label } ");
		sparql.append("  OPTIONAL { ?iri skos:definition ?def } ");
		sparql.append("  OPTIONAL { ?iri rdfs:subClassOf ?parent . FILTER(STRSTARTS(STR(?parent), 'https://brickschema.org/schema/Brick#')) } ");
		sparql.append("  OPTIONAL { ?iri owl:deprecated ?dep } ");
		if (StrUtil.isNotBlank(keyword)) {
			sparql.append("  FILTER(CONTAINS(LCASE(COALESCE(STR(?label), STR(?iri))), LCASE('").append(escapeSparql(keyword)).append("'))) ");
		}
		if (StrUtil.isNotBlank(parentIri)) {
			sparql.append("  FILTER(STR(?parent) = '").append(escapeSparql(parentIri)).append("') ");
		}
		sparql.append("} ORDER BY ?label");
		return executeClassQuery(model, sparql.toString(), page);
	}

	/**
	 * CCO 注释属性分页（AC-7.1）。
	 * SPARQL 查 owl:AnnotationProperty 实体的 label/definition。
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
			sparql.append("  FILTER(CONTAINS(LCASE(COALESCE(STR(?label), '')), LCASE('").append(escapeSparql(keyword)).append("'))) ");
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
			return model.listSubjectsWithProperty(
					model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
					model.createResource(type)).toSet().stream().filter(s -> {
						// brick 只计 brick: 命名空间
						if ("brick".equals(ont)) {
							return s.getURI() != null && s.getURI().startsWith("https://brickschema.org/schema/Brick#");
						}
						return s.getURI() != null;
					}).count();
		}
		catch (Exception e) {
			return 0L;
		}
	}

	// ---------- 分页执行（SPARQL 全查后内存分页，数据已缓存） ----------

	@SuppressWarnings("unchecked")
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

	@SuppressWarnings("unchecked")
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

	@SuppressWarnings("unchecked")
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

	private <T> IPage<T> paginate(List<T> all, Page page) {
		int current = (int) (page.getCurrent() <= 0 ? 1 : page.getCurrent());
		int size = (int) (page.getSize() <= 0 ? 10 : page.getSize());
		int from = Math.min((current - 1) * size, all.size());
		int to = Math.min(from + size, all.size());
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
```

> 说明：
> - **Jena Model 缓存**：`Caffeine.newBuilder().expireAfterWrite(30,MINUTES).maximumSize(3).build()`，复用 DD3 `InheritedViewService` 缓存范式。首次解析 6.8M TTL ~5s，缓存后查询 <50ms（AC-7.4）。
> - **SPARQL 查询**：用 Jena ARQ 的 `QueryExecutionFactory` 执行 SPARQL，过滤前缀/命名空间（brick 只查 `brick:` 命名空间，过滤 rec/s223 等）。
> - **内存分页**：SPARQL 全查后内存分页（数据已缓存，单位 2925/类 1472/注释属性 44，内存分页性能无忧）。
> - **路径配置**：`pig.ontology.reference-base` 可配（默认 `docs/ontology/参考开源本体库`），支持相对/绝对路径。

### 4.5 ReferenceImportService（QUDT 单位 + Brick 类导入，AC-7.2/7.3/7.5）

```java
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
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 参考本体库导入 Service（FR-7，AC-7.2/7.3/7.5，R-16）
 * <p>
 * QUDT 单位 -> ont_unit（source=builtin, sourceRef=qudt）；
 * Brick 类 -> ont_class_template（subClassOf->parent_id 取主父类，source=builtin, sourceRef=brick）。
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
	 */
	@Transactional(rollbackFor = Exception.class)
	public R importQudtUnit(String iri) {
		// 1. 查重（AC-7.5）
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
		if (StrUtil.isNotBlank(unit.getQuantityKindId() == null ? null : "matched")) {
			// quantityKindIri 临时存在 scalingOf，后续修正
		}
		String qkIri = extractQuantityKindIri(model, iri);
		if (StrUtil.isNotBlank(qkIri)) {
			QuantityKind qk = quantityKindMapper.selectOne(
					Wrappers.<QuantityKind>lambdaQuery().eq(QuantityKind::getQudtIri, qkIri));
			if (qk == null) {
				return R.failed("量纲 '" + qkIri + "' 未在量纲注册表注册，请先注册量纲或导入对应量纲");
			}
			unit.setQuantityKindId(qk.getId());
		}
		if (unit.getQuantityKindId() == null) {
			return R.failed("单位未关联量纲，无法导入");
		}
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
	 */
	@Transactional(rollbackFor = Exception.class)
	public R importBrickClass(String iri) {
		// 1. 类本地名 -> templateCode（kebab-case）
		String localName = extractLocalName(iri);
		String templateCode = localName.replace('_', '-').toLowerCase();
		// 2. 查重（AC-7.5）
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
			ClassTemplate parent = classTemplateMapper.selectOne(
					Wrappers.<ClassTemplate>lambdaQuery().eq(ClassTemplate::getTemplateCode, parentCode));
			if (parent == null) {
				// 递归导入父类（先建父后建子）
				R parentResult = importBrickClass(parentIri);
				if (parentResult.getCode() != 0) {
					return R.failed("导入父类失败: " + parentResult.getMsg());
				}
				parent = classTemplateMapper.selectOne(
						Wrappers.<ClassTemplate>lambdaQuery().eq(ClassTemplate::getTemplateCode, parentCode));
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
				+ "PREFIX brick: <https://brickschema.org/schema/Brick#> "
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
```

> 说明：
> - **QUDT 单位导入**：查重 -> Model 提取字段 -> 量纲匹配（按 QUDT 量纲 IRI 查 `ont_quantity_kind`，未注册则报错）-> 落库 source=builtin/sourceRef=qudt。builtin 保护由 DD4 `updateUnit`/`removeUnit` 已就位。
> - **Brick 类导入**（R-16 取主父类）：类本地名转 kebab-case templateCode -> 查重 -> Model 提取 label/definition/parentIri（取第一个 brick: 命名空间父类）-> 递归确保父类已导入（先建父后建子）-> 落库 source=builtin/sourceRef=brick/treeRoot=brick。
> - **递归导入父类**：若父类未导入，递归调用 `importBrickClass(parentIri)` 先建父，避免 parent_id 悬空。非 brick: 父类（owl:Thing 等）作根节点（parentId=null）。
> - **classificationCode**：Brick 导入的模板暂不生成规范编码（treeRoot=brick 无编码规则），用 templateCode 作标识；后续治理员可配置 brick 分类树的编码规则。

---

## 五、前端设计

### 5.1 API `web/src/api/ontology/reference.ts`

```typescript
import request from '/@/utils/request';

// ---------- 浏览 ----------
export function listOntologies() {
	return request({ url: '/admin/ont/reference/ontologies', method: 'get' });
}

export function pageUnits(query: any) {
	return request({ url: '/admin/ont/reference/qudt/units', method: 'get', params: query });
}

export function pageClasses(query: any) {
	return request({ url: '/admin/ont/reference/brick/classes', method: 'get', params: query });
}

export function pageAnnotationProperties(query: any) {
	return request({ url: '/admin/ont/reference/cco/annotation-properties', method: 'get', params: query });
}

// ---------- 导入 ----------
export function importQudtUnit(iri: string) {
	return request({ url: '/admin/ont/reference/qudt/unit/import', method: 'post', params: { iri } });
}

export function importBrickClass(iri: string) {
	return request({ url: '/admin/ont/reference/brick/class/import', method: 'post', params: { iri } });
}
```

### 5.2 参考本体库页 `views/admin/ontology/reference/index.vue`（三 Tab + 分页 + 导入）

> 对齐 pig 现有 `el-tabs` 范式。三 Tab 切换时各自独立分页查询。

**布局：**

```
┌──────────────────────────────────────────────────────────────┐
│ [QUDT 单位] [Brick 类] [CCO 注释属性]                         │
├──────────────────────────────────────────────────────────────┤
│ Tab 1（QUDT）：                                                │
│ 量纲 [全部▼] 关键字 [____] [查询]                              │
│ IRI | label | symbol | 量纲 | 系数 | 操作[导入到单位注册表]    │
│ ...                                                           │
│ Tab 2（Brick）：                                               │
│ 关键字 [____] [查询]                                           │
│ IRI | label | 定义 | 父类 | 操作[导入为分类模板]               │
│ Tab 3（CCO）：                                                 │
│ 关键字 [____] [查询]                                           │
│ IRI | label | 定义                                            │
└──────────────────────────────────────────────────────────────┘
```

### 5.3 i18n（reference/i18n/zh-cn.ts 示例）

```typescript
export default {
	reference: {
		tabQudt: 'QUDT 单位',
		tabBrick: 'Brick 类',
		tabCco: 'CCO 注释属性',
		iri: 'IRI',
		label: '显示名',
		symbol: '符号',
		quantityKind: '量纲',
		conversionMultiplier: '换算系数',
		definition: '定义',
		parentClass: '父类',
		deprecated: '已弃用',
		keyword: '关键字',
		inputKeywordTip: '请输入关键字',
		selectQuantityKindTip: '请选择量纲',
		importUnit: '导入到单位注册表',
		importClass: '导入为分类模板',
		importSuccess: '导入成功',
		importExistTip: '已存在，不重复导入',
	},
};
```

---

## 六、横切设计

### 6.1 缓存（AC-7.4，R-8）

- **Caffeine 缓存 Jena Model**：key=本体标识（qudt/brick/cco），TTL 30min，maximumSize=3。首次解析 6.8M ~5s，缓存后查询 <50ms。
- 复用 DD3 `InheritedViewService` 缓存范式（`Caffeine.newBuilder().expireAfterWrite().maximumSize().build()` + `cache.get(key, loader)`）。
- 无需主动失效（参考本体只读不变，TTL 过期自动重新解析）。

### 6.2 性能（NFR，AC-7.4）

- 后端：Jena Model 缓存 + SPARQL 查询 + 内存分页。
- 前端：分页加载（默认 10 条/页），不全量渲染；Tab 切换时按需查询。

### 6.3 异常处理

- TTL 文件缺失/解析失败：`loadModel` 抛异常，Controller 返回 `R.failed("参考本体加载失败")`。
- 导入查重：返回友好提示（非 500）。
- 量纲未注册：返回提示"量纲未注册，请先注册量纲"。

### 6.4 审计

- 导入操作加 `@SysLog`（"导入QUDT单位"/"导入Brick类"），记录审计日志。
- 导入落库的 `createBy/createTime` 由 MybatisPlusMetaObjectHandler 自动填充。

### 6.5 双形态

- 单体：pig-boot context-path `/admin` -> `/admin/ont/reference/**`。
- 微服务：网关路由 `Path=/admin/ont/**` -> `lb://pig-ontology-biz`（DD1 已配置）。

### 6.6 安全

- 浏览接口：`ont_ref_view`（V11 新建 10501）。
- QUDT 单位导入：`ont_unit_manage`（DD4 V9 已建 10301~10303）。
- Brick 类导入：`ont_class_tpl_manage`（DD3 V8 已建 10201~10203）。

---

## 七、测试要点

| 类别 | 要点 |
|---|---|
| 编译 | `mvn -pl pig-ontology/pig-ontology-biz -am compile` 通过（含 jena-arq 新依赖） |
| Flyway | 启动后 V11 成功；`SELECT * FROM sys_menu WHERE menu_id = 10501` 存在（ont_ref_view） |
| 本体列表（AC-7.1） | `GET /ont/reference/ontologies` 返回 3 项（qudt/brick/cco，含 itemCount） |
| QUDT 浏览（AC-7.1） | `GET /ont/reference/qudt/units?keyword=KiloGM` 返回 KiloGM 单位（含 label/symbol/系数） |
| Brick 浏览（AC-7.1） | `GET /ont/reference/brick/classes?keyword=Pump` 返回 brick:Pump（含 label/definition/parentIri） |
| CCO 浏览（AC-7.1） | `GET /ont/reference/cco/annotation-properties` 返回 CCO 注释属性（含 label/IRI） |
| 性能（AC-7.4） | 首次访问 ~5s（解析）；缓存后再次访问 <1s；前端首屏 <1s |
| QUDT 导入（AC-7.2/7.3） | `POST /ont/reference/qudt/unit/import?iri=.../unit/KiloGM` -> ont_unit 新增（source=builtin, sourceRef=qudt, 含 qudtIri/symbol/系数） |
| Brick 导入（AC-7.3/7.5） | `POST /ont/reference/brick/class/import?iri=.../Pump` -> ont_class_template 新增（source=builtin, sourceRef=brick, treeRoot=brick, parent_id 指向父类） |
| 溯源（AC-7.3） | 导入的 ont_unit.sourceRef='qudt'；ont_class_template.sourceRef='brick' |
| 去重（AC-7.5） | 重复导入已存在的 qudtIri/templateCode -> 返回"已存在，不重复导入" |
| 量纲未注册 | 导入单位关联的量纲不在 ont_quantity_kind -> 返回"量纲未注册"提示 |
| 父类递归导入 | 导入 brick:Pump（父 brick:HVAC_Equipment 未导入）-> 递归先导入父类 |
| 前端 | pig-ui -> 本体治理 -> 参考本体库 -> 三 Tab 切换；QUDT Tab 搜 KiloGM -> 导入；Brick Tab 搜 Pump -> 导入；CCO Tab 浏览 |
| 回归 | 属性模板库（DD2）/分类模板（DD3）/单位注册表（DD4）/注释属性（DD5）功能不受影响 |

---

## 八、风险与缓解

| 风险 | 缓解 |
|---|---|
| 大文件解析慢（R-8/AC-7.4） | Jena Model 一次性解析 + Caffeine 缓存（TTL 30min），首次 ~5s，缓存后 <50ms |
| Brick 多继承映射失真（R-16） | 导入时取主父类（第一个 brick: 命名空间 subClassOf），多继承仅记 sourceRef 溯源、不进 ont_class_hierarchy |
| QUDT 量纲未注册致导入失败 | 导入前查 ont_quantity_kind，未注册返回提示（不静默失败） |
| 父类未导入致 parent_id 悬空 | 递归导入父类（先建父后建子），确保 parent_id 有效 |
| jena-arq 依赖体积大 | 仅引 jena-arq（~15MB），不含 jena-fuseki 等服务组件；版本锁定 5.3.0 |
| TTL 文件路径跨环境差异 | `pig.ontology.reference-base` 可配（默认相对路径），支持绝对路径 |
| SPARQL 注入 | 关键字参数 escapeSparql 转义单引号；IRI 参数为只读浏览不拼 SQL |

---

## 九、交付物清单

| 类型 | 文件 | 说明 |
|---|---|---|
| 修改 | `pig-ontology-biz/pom.xml` | 新增 jena-arq 依赖 |
| 新建 | `ReferenceOntologyVO` / `ReferenceUnitVO` / `ReferenceClassVO` / `ReferenceAnnotationPropertyVO` | 4 VO |
| 新建 | `ReferenceOntologyService.java` | 浏览（Jena 解析 + Caffeine 缓存 + SPARQL 分页） |
| 新建 | `ReferenceImportService.java` | 导入（QUDT 单位 + Brick 类，含递归父类导入） |
| 新建 | `ReferenceController.java` | 浏览 + 导入端点（10.6） |
| 新建 | `V11__ont_reference_seed.sql` | 参考本体权限点 10501 |
| 新建 | `web/src/api/ontology/reference.ts` | 前端 API |
| 新建 | `web/src/views/admin/ontology/reference/index.vue` | 三 Tab 浏览 + 导入 |
| 新建 | `web/src/views/admin/ontology/reference/i18n/{zh-cn,en}.ts` | 词条 |

---

## 十、与 PRD 边界的对齐确认（防混淆备忘）

| 边界点 | 本 DD 落地方式 | PRD 依据 |
|---|---|---|
| 引用而非自建 | 不重定义 QUDT/Brick/CCO 类，只引用 IRI 作为导入来源 | 5.2 / OoS |
| 导入是数据映射非本体语义 | Brick subClassOf->parent_id 是结构映射，不依赖 OWL 推理机 | OoS |
| Brick 多继承取主父类 | 取第一个 brick: 命名空间 subClassOf，多继承仅记 sourceRef | R-16 |
| CCO 仅浏览不导入 | DD5 已有内置注释属性，CCO 注释属性仅浏览参考 | FR-4 / FR-7 |
| 全量单位不批量导入 | 按需单条导入（AC-7.2），全量 2925 单位属 OoS | OoS / FR-7 |
| 参考本体只读 | 不修改 TTL 文件，浏览数据缓存内存不落库 | FR-7 |
| 建模侧以 IRI 引用 | 本功能只导入到治理资产，建模侧以 IRI 引用属建模侧 | 5.2 / R-11 |

---

*本详细设计对应贯穿里程碑（FR-7），依赖 DD1（M0）基础设施（V5 菜单 10500）、DD3（M2）分类模板（ClassTemplate/编码规则）、DD4（M3）单位注册表（Unit/QuantityKind）。实现完成后，FR-1~FR-7 全部治理资产（属性模板/分类模板/单位/注释属性/供给/参考本体）就绪，M0~M4 + 贯穿全部收尾。*
