# SPARQL 查询端点模块详细设计

> 对应 PRD：§3.2.2（技术架构-三元组存储选型）、§5.3（形式化要求-标准化序列化与查询）、§12（风险与对策-Triplestore选型）
>
> 对应差距分析：§3.12 维度⑫ SPARQL 查询端点（🔴 缺失）、§5.2 A 本体能力增强层-模块 13
>
> 前置模块：01-模块化架构、02-单位字典、03-命名空间与IRI、04-实体类型、05-数据属性、06-对象属性、07-公理与规则、08-实体对象实例、09-校验引擎（`OntologyModelAssembler`/`DataGraphBuilder`）、10-序列化与交换（`OntologyModelExporter`/`NamespacePrefixResolver`）、11-扩展管理、12-可视化
>
> 设计日期：2026-07-13

---

## 1. 评审结论与设计边界

### 1.1 现状评审

截至本设计时点，前述 12 个模块已全部落地。在 SPARQL 查询能力方面，代码库现状如下：

| 缺口 | 现状 | 影响 |
|------|------|------|
| **无 SPARQL 查询端点** | 后端无 `/ontology/sparql` 端点；模块 10（序列化）明确声明"无 SPARQL 查询端点" | 差距分析 §3.12 指出：复杂多跳关联查询（如"某维保员负责的楼宇里所有带特定品牌曳引机的电梯近一年困人记录"）只能退化为 PostgreSQL 递归 CTE + 多层 JOIN，3 跳以上性能急剧下降且语义表达力远不如 SPARQL（路径查询、属性路径、CONSTRUCT 构造子图、联邦查询） |
| **Jena 仅用于内存校验/序列化** | `OntologyModelAssembler` 在每次调用时用 `ModelFactory.createDefaultModel()` 构建内存 Model，执行完即丢弃；`OntologyModelExporter` 同理 | 无法支持持久化 RDF 存储；每次查询需全量重建 Model，查询规模稍大即超时；无法利用 TDB 的索引优化 |
| **jena-arq 已引入但未使用** | `pig-ontology-biz/pom.xml` 已声明 `jena-arq 5.5.0`（ARQ 是 Jena 的 SPARQL 引擎），但代码库中无任何 `QueryExecution`、`Dataset`、`RDFConnection` 使用 | ARQ 依赖已就绪，可直接用于 SPARQL 查询执行，无需新增依赖 |
| **无查询超时与资源限制** | 现有 API 无查询资源限制机制 | SPARQL 是图灵完备查询语言，不当查询（如超大笛卡尔积、无限路径表达式）可导致 OOM 或 CPU 镡尽；需查询超时 + 结果行数限制 + 语法白名单 |
| **可视化模块直查关系库** | 模块 12（可视化）`OntVisualizationService` 直接 SQL 聚合查询组装 graph JSON，不走 SPARQL | 图谱交互（如"展开 N 跳邻居"）若用 SPARQL 属性路径查询更自然，但本期可视化不改造，SPARQL 端点作为独立查询入口 |
| **无命名空间前缀注册** | `NamespacePrefixResolver`（模块 10）在序列化时组装 `@prefix` 声明，但未注册到 Jena Model 的 prefix mapping | SPARQL 查询中使用前缀（如 `std:Standard`）需 Jena Model 支持 prefix 映射，否则只能用完整 IRI |

### 1.2 优化结论

| 级别 | 评审发现 | 优化结论 |
|------|----------|----------|
| P0 | 无 SPARQL 查询端点，多跳图查询退化为 SQL JOIN 地狱 | **新建 SPARQL 查询端点模块**：提供 `POST /ontology/sparql/query` 端点，接收 SPARQL 查询字符串 + ontologyId，返回 JSON 结果集；复用 `OntologyModelAssembler` 构建内存 Model（一期），通过 ARQ 执行查询 |
| P0 | 每次查询全量重建 Model 性能差 | **一期内存 Model + 二级缓存**：单次请求内复用同一 Model（Schema + Instance 合并），同一 ontologyId 的 Model 在请求间缓存 LRU（默认 5 个本体工程，TTL 10 分钟）；缓存失效在实例/Schema 变更时触发（监听模块 08/04 变更事件） |
| P0 | SPARQL 查询无资源限制，存在安全风险 | **查询沙箱**：查询超时（默认 10s）、结果行数上限（默认 10000 行）、禁止更新操作（仅允许 SELECT/ASK/CONSTRUCT/DESCRIBE）、禁止 SERVICE 联邦查询（一期不暴露外部端点）、SPARQL 语法注入检查 |
| P1 | 前缀映射未注册到 Jena Model | **注册 prefix mapping**：在构建 Model 时调用 `model.setNsPrefix(prefix, uri)` 注册命名空间前缀（复用 `NamespacePrefixResolver` 获取前缀列表），使 SPARQL 查询可用 `std:`/`owl:`/`rdf:` 等前缀 |
| P1 | 无预置查询模板，用户需手写 SPARQL | **预置常用查询模板**：提供 8 个常用查询模板（子类层次、实例属性、N 跳邻居、关系路径、约束违规实体、类型统计、实例搜索、附录D验证查询），前端下拉选择后填充查询编辑器 |
| P1 | 无查询历史与审计 | **查询日志**：记录每次查询的 ontologyId、查询语句、执行耗时、结果行数、操作人；复用 `@SysLog` 注解 + 独立 `ont_sparql_query_log` 表（比 sys_log 更精细，含完整查询文本） |
| P2 | 查询结果仅支持 JSON，不支持 RDF 格式 | **多格式输出**：JSON（默认，前端消费）、Turtle、CSV（表格类查询导出）；复用模块 10 的 `RdfSerializer` 系列对 CONSTRUCT 查询结果序列化 |

### 1.3 本期范围

**本期包含：**

1. SPARQL 查询 REST API（`POST /ontology/sparql/query` + `GET /ontology/sparql/templates`）
2. SPARQL 查询应用服务层（`OntSparqlService`——Model 构建 + 缓存 + 查询执行 + 结果转换）
3. 查询沙箱（超时、行数限制、操作类型白名单、语法校验）
4. Model 二级缓存（LRU + TTL + 变更失效）
5. 预置查询模板（8 个常用查询）
6. 查询日志（`ont_sparql_query_log` 表 + `@SysLog` 审计）
7. Flyway V15 迁移脚本（查询日志表 + 菜单权限）
8. 前端查询控制台（SPARQL 编辑器 + 模板选择 + 结果表格/RDF 预览 + 查询历史）

**本期不包含：**

- 持久化 RDF 存储（Jena TDB/GraphDB）——一期用内存 Model + 缓存，实例规模≤10万时性能可接受；当实例规模超 10万或查询深度超 3 跳时，评估引入 TDB（差距分析 §3.9/§3.16 触发条件）
- SPARQL 更新（INSERT/DELETE）——本期为只读查询端点，实例写入仍走模块 08 REST API；SPARQL Update 远期按需评估
- SERVICE 联邦查询——一期不暴露外部 SPARQL 端点联邦；远期按需评估跨本体联邦查询
- 可视化模块改造——模块 12 继续直查关系库，SPARQL 端点作为独立查询入口；远期可评估可视化模块通过 SPARQL 获取图谱数据
- 推理物化——本期不预计算传递闭包；`ReasonerAdapter.inferEntailments()` 仍返回未启用（差距分析 §3.16）
- 自定义函数（SPARQL 函数扩展）——本期仅支持标准 SPARQL 1.1 内置函数
- SPARQL 查询计划可视化——本期不暴露查询执行计划

### 1.4 与前置模块的边界

| 能力 | 前置模块（已完成） | 本模块（SPARQL 端点） |
|------|-------------------|----------------------|
| Schema Model 构建 | 校验引擎（09）`OntologyModelAssembler.buildSchemaModel()` | **复用**——调用 `buildSchemaModel` 获取 Schema 三元组 |
| Instance Model 构建 | 校验引擎（09）`OntologyModelAssembler.buildInstanceModel()` | **复用**——调用 `buildInstanceModel` 获取实例三元组 |
| 命名空间前缀 | 序列化模块（10）`NamespacePrefixResolver` | **复用**——获取前缀列表注册到 Jena Model prefix mapping |
| RDF 格式序列化 | 序列化模块（10）`RdfSerializer` 系列 | **复用**——CONSTRUCT 查询结果序列化为 Turtle/JSON-LD/RDF-XML |
| 本体工程 | 实体类型模块（04）`ont_ontology_project` | 只读引用——按 `ontologyId` 隔离查询 |
| 可视化 | 可视化模块（12） | **不复用**——可视化直查关系库；SPARQL 端点为独立查询入口，远期可评估整合 |
| 校验引擎 | 校验引擎（09） | **不复用**——校验引擎用 SHACL 约束校验，不执行用户 SPARQL；SPARQL 端点不做约束校验 |

### 1.5 权威语义口径

1. SPARQL 端点是**只读查询入口**，不拥有任何数据的写入权限——所有 RDF 三元组来源于 `OntologyModelAssembler` 从关系库组装的内存 Model，SPARQL 端点不创建、修改、删除任何三元组。
2. SPARQL 端点**复用 `OntologyModelAssembler`** 构建 Model，不直连 Triplestore——一期无持久化 RDF 存储，Model 在内存中按需构建并缓存。这是"PostgreSQL 为唯一真相源"架构原则的延续。
3. 查询按**本体工程隔离**：所有查询 API 均接收 `ontologyId` 参数，仅返回该工程范围内的三元组查询结果。跨工程查询不在本期范围。
4. **Model 合并策略**：SPARQL 查询需同时访问 Schema（类层次、属性定义）和实例（rdf:type、属性值、关系断言），因此将 `buildSchemaModel` + `buildInstanceModel` 合并为一个 Model 供查询使用。Schema Model 和 Instance Model 的构建逻辑不变，合并操作为 `schemaModel.add(instanceModel)`（Jena Model 的 `add` 方法支持合并）。
5. **查询沙箱强制只读**：通过 ARQ 的 `Query.isSelectType()` / `isAskType()` / `isConstructType()` / `isDescribeType()` 校验查询类型；拒绝 `INSERT`/`DELETE`/`LOAD`/`CLEAR`/`CREATE`/`DROP` 等更新操作。一期不支持 SPARQL Update。
6. **前缀映射注册**：在合并 Model 时，从 `ont_namespace` 表读取全部命名空间前缀，调用 `model.setNsPrefix(prefix, uri)` 注册，使 SPARQL 查询可用 `std:Standard`、`owl:Class` 等前缀简写。
7. **缓存策略**：同一 `ontologyId` 的合并 Model 缓存在 LRU（最大 5 个工程，TTL 10 分钟）；当模块 04（实体类型）/05（数据属性）/06（对象属性）/07（公理规则）/08（实例）发生写入时，通过 Spring `ApplicationEvent` 通知缓存失效。缓存统计通过 actuator 暴露。
8. **权限模型**：SPARQL 查询需 `ontology_sparql_query` 权限；查询模板列表需 `ontology_sparql_view` 权限；管理员/建模工程师/审核员默认拥有查询权限，访客默认拥有模板查看权限但无查询权限（查询消耗资源，限制访客使用）。
9. **结果格式**：SELECT/ASK 查询返回 JSON（`results.bindings` 数组，每个 binding 为 `variable → {value, type, datatype/lang}`）；CONSTRUCT/DESCRIBE 查询返回 Jena Model，可序列化为 Turtle/JSON-LD/RDF-XML，也可转为 JSON 三元组数组。前端默认展示 SELECT 结果为表格，CONSTRUCT 结果为 RDF 文本预览。
10. **附录D验证查询**：预置一个用于验证附录D实例化完整性的查询模板（查询所有实例及其 rdf:type，统计每类型实例数），作为 SPARQL 端点的核心验收用例。

---

## 2. 总体架构

### 2.1 架构分层

```
┌──────────────────────────────────────────────────────────────────┐
│                     前端层 (Vue 3 + Element Plus + CodeMirror)       │
│                                                                    │
│  ┌──────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ │
│  │查询编辑器│ │ 模板选择器    │ │ 结果表格     │ │ RDF 预览     │ │
│  │(CodeMirror│ │(下拉选择8个  │ │(SELECT结果   │ │(CONSTRUCT结果│ │
│  │ SPARQL高亮)│ │ 预置模板)    │ │ 表格展示)    │ │ Turtle/JSON-LD)│
│  └──────────┘ └──────────────┘ └──────────────┘ └──────────────┘ │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │  查询历史抽屉 │ 本体工程选择 │ 执行按钮 │ 格式切换 │ 导出CSV  │ │
│  └──────────────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────────┤
│                     接口层 (REST API)                               │
│  POST /ontology/sparql/query          (执行SPARQL查询)              │
│  GET  /ontology/sparql/templates      (预置查询模板列表)            │
│  GET  /ontology/sparql/history         (查询历史)                   │
│  GET  /ontology/sparql/export          (导出查询结果CSV)           │
├──────────────────────────────────────────────────────────────────┤
│                   SPARQL 应用服务层                                   │
│  OntSparqlService ── 查询编排                                         │
│    ├── getModel(ontologyId) → Model [缓存]                          │
│    ├── executeQuery(query, ontologyId, format) → SparqlResultVO     │
│    ├── validateQuery(query) → ValidationResult [沙箱校验]            │
│    ├── buildCombinedModel(ontologyId) → Model [Schema+Instance合并] │
│    └── getTemplates() → List<QueryTemplateVO>                       │
│  OntSparqlCacheManager ── Model缓存管理                              │
│    ├── get(ontologyId) → Model [LRU + TTL]                          │
│    ├── evict(ontologyId) [变更失效]                                   │
│    └── stats() → CacheStatsVO                                        │
├──────────────────────────────────────────────────────────────────┤
│                   查询沙箱层                                          │
│  SparqlQuerySandbox                                                  │
│    ├── validateQueryType(query) [只读白名单]                         │
│    ├── checkSyntax(query) [ARQ语法解析]                              │
│    ├── applyTimeout(queryExecution, timeout) [超时控制]              │
│    └── limitResults(rowLimit) [结果行数限制]                         │
├──────────────────────────────────────────────────────────────────┤
│                   数据访问层 (复用已有 Mapper)                         │
│  OntologyModelAssembler ── buildSchemaModel + buildInstanceModel     │
│  NamespacePrefixResolver ── 前缀映射                                  │
│  RdfSerializerRegistry ── CONSTRUCT结果序列化                         │
│  OntOntologyProjectMapper ── 本体工程校验                             │
└──────────────────────────────────────────────────────────────────┘
```

### 2.2 模块定位

SPARQL 查询端点模块位于 PRD §3.1 四层架构的**应用服务层**，是本体能力增强层（差距分析 §5.2 A 层）的第一个模块：

```
┌─────────────────────────────────────────────────────────────┐
│          本体能力增强层 (§5.2 A 层)                           │
│  13. SPARQL查询端点(本模块) │ 14. 版本演化 │ 15. 对齐        │
│  16. SKOS │ 17. 推理物化                                      │
├─────────────────────────────────────────────────────────────┤
│  可视化展示层: 可视化(12)                                     │
├─────────────────────────────────────────────────────────────┤
│  输出层: 序列化(10) │ 扩展(11)                                │
├─────────────────────────────────────────────────────────────┤
│  约束层: 校验引擎(09)                                         │
├─────────────────────────────────────────────────────────────┤
│  实例层: 实体对象实例(08)                                     │
├─────────────────────────────────────────────────────────────┤
│  Schema层: 命名空间(03)│实体类型(04)│数据属性(05)             │
│           │对象属性(06)│公理规则(07)│单位字典(02)             │
└─────────────────────────────────────────────────────────────┘
```

SPARQL 端点横跨 Schema 层和实例层，从 `OntologyModelAssembler` 获取合并 Model，通过 ARQ 引擎执行 SPARQL 查询，将结果转换为前端可消费的 JSON 或 RDF 格式。

### 2.3 技术选型

| 组件 | 选型 | 版本 | 选型理由 |
|------|------|------|----------|
| SPARQL 引擎 | Apache Jena ARQ | 5.5.0（已声明） | `pig-ontology-biz/pom.xml` 已引入 `jena-arq`；ARQ 是 Jena 的 SPARQL 1.1 完整实现，支持 SELECT/ASK/CONSTRUCT/DESCRIBE、属性路径、聚合、子查询、FILTER函数 |
| RDF 存储 | 内存 Model + LRU 缓存 | — | 一期无 TDB；`ModelFactory.createDefaultModel()` 构建内存 Model，LRU 缓存避免重复构建；触发 TDB 的条件见 §1.3 |
| 前端编辑器 | CodeMirror 6 | 6.x | SPARQL 语法高亮、行号、自动缩进；轻量（~130KB gzip），支持自定义语言模式；可替换为 Monaco Editor（更重但功能更强） |
| 查询超时 | ARQ `QueryExecution.setTimeout` | — | ARQ 原生支持查询超时（毫秒级），超时后自动中止查询执行线程 |
| 结果序列化 | `RdfSerializer`（模块 10） | — | CONSTRUCT/DESCRIBE 查询结果为 Jena Model，复用模块 10 的 Turtle/JSON-LD/RDF-XML 序列化器 |
| 缓存 | Caffeine | — | Spring Boot 默认缓存提供者，支持 LRU + TTL + 事件监听；比 Guava Cache 更高性能 |

### 2.4 与 OntologyModelAssembler 的关系

| 维度 | OntologyModelAssembler（模块 09） | OntSparqlService（本模块） |
|------|-----------------------------------|---------------------------|
| **调用方** | 校验引擎（SHACL/OWL 公理检测）、序列化导出 | SPARQL 查询端点 |
| **Model 用途** | SHACL 校验数据图、OWL 公理推理、RDF 序列化导出 | 用户 SPARQL 查询执行 |
| **Model 范围** | `buildSchemaModel` 或 `buildInstanceModel`（按需分别构建） | **合并** Schema + Instance Model（SPARQL 查询需同时访问 Schema 和实例） |
| **生命周期** | 每次调用构建新 Model，用完丢弃 | **缓存**——同一 ontologyId 的合并 Model 缓存在 LRU，TTL 10 分钟 |
| **前缀映射** | 不注册 prefix mapping（SHACL Shapes 自带 `@prefix`） | **注册**——从 `ont_namespace` 表读取前缀，调用 `model.setNsPrefix()` |

SPARQL 端点**复用** `OntologyModelAssembler` 的 `buildSchemaModel` 和 `buildInstanceModel` 方法，不重复实现 Model 构建逻辑。合并操作为：

```java
Model schemaModel = ontologyModelAssembler.buildSchemaModel(ontologyId);
Model instanceModel = ontologyModelAssembler.buildInstanceModel(ontologyId);
Model combined = ModelFactory.createDefaultModel();
combined.add(schemaModel);
combined.add(instanceModel);
// 注册前缀
namespacePrefixResolver.getPrefixMap(ontologyId)
    .forEach(combined::setNsPrefix);
```

---

## 3. 数据模型

### 3.1 查询日志表（ont_sparql_query_log）

SPARQL 端点新增一张查询日志表，记录每次查询的完整信息用于审计与性能分析：

```sql
-- SPARQL 查询日志
CREATE TABLE ont_sparql_query_log (
  id BIGSERIAL PRIMARY KEY,
  ontology_id BIGINT NOT NULL,                    -- 本体工程ID
  query_text TEXT NOT NULL,                       -- SPARQL 查询语句（完整）
  query_type VARCHAR(16) NOT NULL,                -- SELECT/ASK/CONSTRUCT/DESCRIBE
  result_format VARCHAR(16) NOT NULL DEFAULT 'JSON', -- JSON/TURTLE/JSONLD/RDFXML/CSV
  result_row_count INTEGER,                       -- 结果行数（SELECT为绑定行数，CONSTRUCT为三元组数）
  execution_time_ms BIGINT,                       -- 执行耗时（毫秒）
  status VARCHAR(16) NOT NULL,                    -- SUCCESS/TIMEOUT/SYNTAX_ERROR/SECURITY_ERROR/INTERNAL_ERROR
  error_message TEXT,                             -- 失败时的错误信息
  operated_by VARCHAR(64) NOT NULL,                -- 操作人（用户名）
  operated_at TIMESTAMP NOT NULL DEFAULT NOW(),    -- 操作时间
  remote_addr VARCHAR(255),                       -- 请求来源IP
  del_flag CHAR(1) DEFAULT '0' NOT NULL           -- 逻辑删除
);
CREATE INDEX idx_sparql_log_ontology ON ont_sparql_query_log (ontology_id);
CREATE INDEX idx_sparql_log_operated_at ON ont_sparql_query_log (operated_at);
CREATE INDEX idx_sparql_log_status ON ont_sparql_query_log (status);
COMMENT ON TABLE ont_sparql_query_log IS 'SPARQL查询日志';
```

### 3.2 SPARQL 结果结构（SparqlResultVO）

```java
/**
 * SPARQL 查询结果 VO。
 * <p>
 * SELECT 查询返回 head + results.bindings；
 * ASK 查询返回 boolean；
 * CONSTRUCT/DESCRIBE 查询返回 triples + 可选的 rdfFormat 序列化文本。
 * </p>
 */
@Data
@Schema(description = "SPARQL查询结果")
public class SparqlResultVO {

    @Schema(description = "查询类型：SELECT/ASK/CONSTRUCT/DESCRIBE")
    private String queryType;

    @Schema(description = "SELECT查询的变量名列表")
    private List<String> headVars;

    @Schema(description = "SELECT查询的结果绑定列表")
    private List<Map<String, BindingVO>> results;

    @Schema(description = "ASK查询的布尔结果")
    private Boolean booleanResult;

    @Schema(description = "CONSTRUCT/DESCRIBE查询的三元组列表")
    private List<TripleVO> triples;

    @Schema(description = "RDF格式序列化文本（CONSTRUCT/DESCRIBE 且 format 非 JSON 时返回）")
    private String rdfText;

    @Schema(description = "结果行数")
    private Integer rowCount;

    @Schema(description = "执行耗时（毫秒）")
    private Long executionTimeMs;

    @Schema(description = "是否被行数限制截断")
    private Boolean truncated;

    /**
     * 单个绑定值（SELECT 查询的每个 cell）。
     */
    @Data
    @Schema(description = "SPARQL绑定值")
    public static class BindingVO {
        @Schema(description = "值")
        private String value;
        @Schema(description = "类型：uri/literal/typed-literal")
        private String type;
        @Schema(description = "数据类型（typed-literal时，如 xsd:integer）")
        private String datatype;
        @Schema(description = "语言标签（literal时，如 zh/en）")
        private String lang;
    }

    /**
     * 三元组 VO（CONSTRUCT/DESCRIBE 查询结果）。
     */
    @Data
    @Schema(description = "RDF三元组")
    public static class TripleVO {
        @Schema(description = "主语IRI")
        private String subject;
        @Schema(description = "谓词IRI")
        private String predicate;
        @Schema(description = "宾语（IRI或字面量）")
        private String object;
        @Schema(description = "宾语类型：uri/literal")
        private String objectType;
        @Schema(description = "数据类型（字面量时）")
        private String datatype;
        @Schema(description = "语言标签（字面量时）")
        private String lang;
    }
}
```

### 3.3 查询模板结构（QueryTemplateVO）

```java
/**
 * 预置 SPARQL 查询模板 VO。
 */
@Data
@Schema(description = "SPARQL查询模板")
public class QueryTemplateVO {

    @Schema(description = "模板ID")
    private String id;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "模板描述")
    private String description;

    @Schema(description = "查询类型：SELECT/ASK/CONSTRUCT/DESCRIBE")
    private String queryType;

    @Schema(description = "SPARQL查询模板（含占位符）")
    private String queryTemplate;

    @Schema(description = "占位符说明")
    private List<PlaceholderVO> placeholders;

    @Schema(description = "分类：SCHEMA/INSTANCE/RELATION/VALIDATION")
    private String category;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Data
    @Schema(description = "查询模板占位符")
    public static class PlaceholderVO {
        @Schema(description = "占位符名称（如 typeIri）")
        private String name;
        @Schema(description = "占位符描述")
        private String description;
        @Schema(description = "默认值")
        private String defaultValue;
        @Schema(description = "是否必填")
        private Boolean required;
    }
}
```

---

## 4. 核心设计

### 4.1 查询执行流程

```
用户在前端输入 SPARQL 查询 + 选择本体工程
  → 前端 POST /ontology/sparql/query { ontologyId, query, format }
  → 后端 OntSparqlController 接收请求
  → OntSparqlService.executeQuery(query, ontologyId, format)
    ├── 1. 沙箱校验（SparqlQuerySandbox.validate）
    │     ├── 查询类型白名单校验（仅 SELECT/ASK/CONSTRUCT/DESCRIBE）
    │     ├── 语法解析（ARQ QueryFactory.create(query)）
    │     └── 安全检查（禁止 SERVICE 联邦查询、禁止 USING/NAMED 子句）
    ├── 2. 获取 Model（OntSparqlCacheManager.get(ontologyId)）
    │     ├── 缓存命中 → 直接返回缓存的合并 Model
    │     └── 缓存未命中 → buildCombinedModel(ontologyId)
    │         ├── OntologyModelAssembler.buildSchemaModel(ontologyId)
    │         ├── OntologyModelAssembler.buildInstanceModel(ontologyId)
    │         ├── 合并 Schema + Instance Model
    │         ├── 注册命名空间前缀（NamespacePrefixResolver）
    │         └── 存入 LRU 缓存
    ├── 3. 创建 QueryExecution（ARQ QueryExecutionFactory.create(query, model)）
    ├── 4. 设置超时（queryExecution.setTimeout(10_000)）
    ├── 5. 按查询类型执行
    │     ├── SELECT → ResultSetFormatter → 转 SparqlResultVO.results
    │     ├── ASK → queryExecution.execAsk() → 转 SparqlResultVO.booleanResult
    │     ├── CONSTRUCT → queryExecution.execConstruct() → Model → 转 triples + rdfText
    │     └── DESCRIBE → queryExecution.execDescribe() → Model → 转 triples + rdfText
    ├── 6. 结果行数限制检查（超 10000 行则 truncated=true）
    ├── 7. 记录查询日志（ont_sparql_query_log 异步写入）
    └── 8. 返回 SparqlResultVO
  → 前端展示结果（表格/RDF预览/CSV导出）
```

### 4.2 查询沙箱设计

```java
/**
 * SPARQL 查询沙箱。
 * <p>
 * 强制只读查询，限制资源消耗，防止恶意查询。
 * </p>
 */
@Component
@Slf4j
public class SparqlQuerySandbox {

    /** 允许的查询类型 */
    private static final Set<Query.QueryType> ALLOWED_TYPES = EnumSet.of(
        Query.QueryType.SELECT, Query.QueryType.ASK,
        Query.QueryType.CONSTRUCT, Query.QueryType.DESCRIBE
    );

    /** 查询超时（毫秒） */
    private static final long QUERY_TIMEOUT_MS = 10_000L;

    /** 结果行数上限 */
    private static final int ROW_LIMIT = 10_000;

    /** 禁止的关键词（SERVICE 联邦查询、USING/NAMED 子句） */
    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
        Pattern.compile("\\bSERVICE\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bUSING\\s+(NAMED\\s+)?GRAPH\\b", Pattern.CASE_INSENSITIVE)
    );

    /**
     * 校验查询安全性。
     * @param queryText SPARQL 查询文本
     * @throws OntSparqlException 查询不合法或不安全时抛出
     */
    public Query validate(String queryText) {
        // 1. 空值检查
        if (!StringUtils.hasText(queryText)) {
            throw new OntSparqlException("查询语句不能为空");
        }

        // 2. 禁止关键词检查（在语法解析前做文本级检查，防止 ARQ 解析时触发联邦查询）
        for (Pattern p : FORBIDDEN_PATTERNS) {
            if (p.matcher(queryText).find()) {
                throw new OntSparqlException("查询包含禁止的关键词: " + p.pattern());
            }
        }

        // 3. 语法解析（ARQ QueryFactory 解析失败则抛 QueryException）
        Query query;
        try {
            query = QueryFactory.create(queryText);
        } catch (QueryException e) {
            throw new OntSparqlException("SPARQL 语法错误: " + e.getMessage(), e);
        }

        // 4. 查询类型白名单校验
        if (!ALLOWED_TYPES.contains(query.queryType())) {
            throw new OntSparqlException("仅支持 SELECT/ASK/CONSTRUCT/DESCRIBE 查询，不支持更新操作");
        }

        return query;
    }

    /**
     * 创建带超时的 QueryExecution。
     */
    public QueryExecution createExecution(Query query, Model model) {
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        qexec.setTimeout(QUERY_TIMEOUT_MS);
        return qexec;
    }

    /**
     * 检查结果行数是否超限。
     */
    public boolean isOverLimit(int rowCount) {
        return rowCount > ROW_LIMIT;
    }
}
```

### 4.3 Model 缓存管理

```java
/**
 * SPARQL 查询 Model 缓存管理器。
 * <p>
 * LRU 缓存合并 Model（Schema + Instance），按 ontologyId 隔离。
 * 缓存变更通过 Spring ApplicationEvent 通知失效。
 * </p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OntSparqlCacheManager {

    private final OntologyModelAssembler modelAssembler;
    private final NamespacePrefixResolver prefixResolver;

    /** LRU 缓存：ontologyId → Model，最大 5 个工程，TTL 10 分钟 */
    private final Cache<Long, Model> modelCache = Caffeine.newBuilder()
        .maximumSize(5)
        .expireAfterAccess(Duration.ofMinutes(10))
        .recordStats()
        .build();

    /**
     * 获取合并 Model（缓存优先）。
     */
    public Model getModel(Long ontologyId) {
        return modelCache.get(ontologyId, this::buildCombinedModel);
    }

    /**
     * 构建合并 Model（Schema + Instance + 前缀映射）。
     */
    private Model buildCombinedModel(Long ontologyId) {
        long start = System.currentTimeMillis();
        Model schemaModel = modelAssembler.buildSchemaModel(ontologyId);
        Model instanceModel = modelAssembler.buildInstanceModel(ontologyId);

        Model combined = ModelFactory.createDefaultModel();
        combined.add(schemaModel);
        combined.add(instanceModel);

        // 注册命名空间前缀
        prefixResolver.getPrefixMap(ontologyId)
            .forEach(combined::setNsPrefix);

        long elapsed = System.currentTimeMillis() - start;
        log.info("构建SPARQL查询Model: ontologyId={}, triples={}, elapsed={}ms",
            ontologyId, combined.size(), elapsed);
        return combined;
    }

    /**
     * 失效指定本体工程的缓存。
     */
    public void evict(Long ontologyId) {
        modelCache.invalidate(ontologyId);
        log.info("SPARQL Model缓存失效: ontologyId={}", ontologyId);
    }

    /**
     * 失效全部缓存。
     */
    public void evictAll() {
        modelCache.invalidateAll();
        log.info("SPARQL Model全部缓存失效");
    }

    /**
     * 缓存统计。
     */
    public CacheStatsVO stats() {
        CacheStats stats = modelCache.stats();
        return CacheStatsVO.builder()
            .size(modelCache.estimatedSize())
            .hitCount(stats.hitCount())
            .missCount(stats.missCount())
            .hitRate(stats.hitRate())
            .evictionCount(stats.evictionCount())
            .averageLoadPenaltyMs(stats.averageLoadPenalty())
            .build();
    }
}
```

### 4.4 缓存失效事件

```java
/**
 * 本体数据变更事件（由模块 04/05/06/07/08 发出）。
 * <p>
 * 当实体类型、数据属性、对象属性、公理规则、实例发生写入时，
 * 发布此事件通知 SPARQL 缓存失效。
 * </p>
 */
@Getter
public class OntologyDataChangedEvent extends ApplicationEvent {

    private final Long ontologyId;
    private final String changeType;  // ENTITY_TYPE/DATA_PROPERTY/OBJECT_PROPERTY/AXIOM_RULE/INSTANCE

    public OntologyDataChangedEvent(Object source, Long ontologyId, String changeType) {
        super(source);
        this.ontologyId = ontologyId;
        this.changeType = changeType;
    }
}

/**
 * 缓存失效事件监听器。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OntSparqlCacheListener {

    private final OntSparqlCacheManager cacheManager;

    @EventListener
    @Async
    public void onOntologyDataChanged(OntologyDataChangedEvent event) {
        log.info("接收本体数据变更事件: ontologyId={}, changeType={}",
            event.getOntologyId(), event.getChangeType());
        cacheManager.evict(event.getOntologyId());
    }
}
```

> **集成点**：在 `OntEntityTypeServiceImpl`、`OntDataPropertyServiceImpl`、`OntObjectPropertyServiceImpl`、`OntAxiomRuleServiceImpl`、`OntEntityInstanceServiceImpl` 的写入方法（save/update/remove）末尾，添加 `applicationEventPublisher.publishEvent(new OntologyDataChangedEvent(this, ontologyId, changeType))`。此改造属于模块 04/05/06/07/08 的小型增量改造（每个类 2-3 行代码），不影响现有功能。

### 4.5 结果转换

```java
/**
 * SPARQL 查询结果转换器。
 * <p>
 * 将 ARQ 的 ResultSet / Model 转换为 SparqlResultVO。
 * </p>
 */
@Component
@Slf4j
public class SparqlResultConverter {

    private final RdfSerializerRegistry serializerRegistry;

    /**
     * 转换 SELECT 查询结果。
     */
    public SparqlResultVO convertSelect(ResultSet rs, long executionTimeMs) {
        SparqlResultVO vo = new SparqlResultVO();
        vo.setQueryType("SELECT");
        vo.setHeadVars(rs.getResultVars());

        List<Map<String, SparqlResultVO.BindingVO>> results = new ArrayList<>();
        int count = 0;
        boolean truncated = false;
        while (rs.hasNext()) {
            if (count >= SparqlQuerySandbox.ROW_LIMIT) {
                truncated = true;
                break;
            }
            QuerySolution sol = rs.next();
            Map<String, SparqlResultVO.BindingVO> row = new LinkedHashMap<>();
            for (String var : rs.getResultVars()) {
                RDFNode node = sol.get(var);
                row.put(var, toBinding(node));
            }
            results.add(row);
            count++;
        }
        vo.setResults(results);
        vo.setRowCount(count);
        vo.setExecutionTimeMs(executionTimeMs);
        vo.setTruncated(truncated);
        return vo;
    }

    /**
     * 转换 ASK 查询结果。
     */
    public SparqlResultVO convertAsk(boolean result, long executionTimeMs) {
        SparqlResultVO vo = new SparqlResultVO();
        vo.setQueryType("ASK");
        vo.setBooleanResult(result);
        vo.setExecutionTimeMs(executionTimeMs);
        return vo;
    }

    /**
     * 转换 CONSTRUCT/DESCRIBE 查询结果。
     */
    public SparqlResultVO convertConstruct(Model model, String queryType,
                                           String rdfFormat, long executionTimeMs) {
        SparqlResultVO vo = new SparqlResultVO();
        vo.setQueryType(queryType);

        List<SparqlResultVO.TripleVO> triples = new ArrayList<>();
        StmtIterator it = model.listStatements();
        int count = 0;
        boolean truncated = false;
        while (it.hasNext()) {
            if (count >= SparqlQuerySandbox.ROW_LIMIT) {
                truncated = true;
                break;
            }
            Statement stmt = it.next();
            triples.add(toTriple(stmt));
            count++;
        }
        vo.setTriples(triples);
        vo.setRowCount(count);
        vo.setExecutionTimeMs(executionTimeMs);
        vo.setTruncated(truncated);

        // 如果请求 RDF 格式输出，序列化模型
        if (rdfFormat != null && !rdfFormat.equalsIgnoreCase("JSON")) {
            RdfSerializer serializer = serializerRegistry.getSerializer(rdfFormat);
            vo.setRdfText(serializer.serialize(model));
        }
        return vo;
    }

    private SparqlResultVO.BindingVO toBinding(RDFNode node) {
        // ... 将 Jena RDFNode 转换为 BindingVO
    }

    private SparqlResultVO.TripleVO toTriple(Statement stmt) {
        // ... 将 Jena Statement 转换为 TripleVO
    }
}
```

---

## 5. 预置查询模板

### 5.1 模板清单（8 个）

| ID | 名称 | 类型 | 分类 | 用途 |
|----|------|------|------|------|
| `subclass-tree` | 子类层次查询 | SELECT | SCHEMA | 查询某实体类型的全部子类（含多级） |
| `instance-props` | 实例属性查询 | SELECT | INSTANCE | 查询某实例的全部数据属性值 |
| `n-hop-neighbors` | N跳邻居查询 | SELECT | RELATION | 查询某实例的 N 跳对象属性邻居 |
| `relation-path` | 关系路径查询 | SELECT | RELATION | 查询两个实例之间的关系路径 |
| `constraint-violations` | 约束违规实体 | SELECT | VALIDATION | 查询可能违反公理约束的实例 |
| `type-statistics` | 类型统计 | SELECT | SCHEMA | 统计每类实体类型的实例数 |
| `instance-search` | 实例搜索 | SELECT | INSTANCE | 按标签/IRI模糊搜索实例 |
| `appendix-d-verify` | 附录D验证查询 | SELECT | VALIDATION | 验证附录D 9步实例化完整性 |

### 5.2 模板详情

#### 5.2.1 子类层次查询

```sparql
# 查询某实体类型的全部子类（含多级递归）
PREFIX std: <http://example.org/standard-ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?subclass ?label WHERE {
  ?subclass rdfs:subClassOf+ ?type .
  OPTIONAL { ?subclass rdfs:label ?label . }
  FILTER(?type = std:${typeIri})
}
ORDER BY ?subclass
```

占位符：`typeIri`（实体类型本地标识，如 `Standard`，必填）

#### 5.2.2 实例属性查询

```sparql
# 查询某实例的全部数据属性值
PREFIX std: <http://example.org/standard-ontology#>

SELECT ?property ?value ?datatype WHERE {
  std:${instanceIri} ?property ?value .
  BIND(DATATYPE(?value) AS ?datatype)
}
ORDER BY ?property
```

占位符：`instanceIri`（实例本地标识，如 `GB-T_31486-2024`，必填）

#### 5.2.3 N跳邻居查询

```sparql
# 查询某实例的 N 跳对象属性邻居（属性路径查询）
PREFIX std: <http://example.org/standard-ontology#>

SELECT ?neighbor ?property ?direction WHERE {
  {
    std:${instanceIri} ?property ?neighbor .
    FILTER(ISIRI(?neighbor))
    BIND("outgoing" AS ?direction)
  } UNION {
    ?neighbor ?property std:${instanceIri} .
    FILTER(ISIRI(?neighbor))
    BIND("incoming" AS ?direction)
  }
}
ORDER BY ?direction ?property
```

占位符：`instanceIri`（实例本地标识，必填）

#### 5.2.4 类型统计

```sparql
# 统计每类实体类型的实例数
PREFIX std: <http://example.org/standard-ontology#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

SELECT ?type (COUNT(?instance) AS ?count) WHERE {
  ?instance rdf:type ?type .
}
GROUP BY ?type
ORDER BY DESC(?count)
```

无占位符，直接执行。

#### 5.2.5 附录D验证查询

```sparql
# 验证附录D 9步实例化完整性：统计每类实例数，对照预期
PREFIX std: <http://example.org/standard-ontology#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

SELECT ?type (COUNT(?instance) AS ?instanceCount) WHERE {
  ?instance rdf:type ?type .
  FILTER(STRSTARTS(STR(?instance), "http://example.org/standard/GB-T-31486-2024/"))
}
GROUP BY ?type
ORDER BY ?type
```

无占位符，直接执行。预期结果：Standard=1, Section=1, Clause=8, Object=1, Property=5, Constraint=7, ActionClass=4, ExternalResource=1, StandardizationObject=1。

---

## 6. 接口设计

### 6.1 REST API

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/ontology/sparql/query` | `ontology_sparql_query` | 执行 SPARQL 查询 |
| GET | `/ontology/sparql/templates` | `ontology_sparql_view` | 获取预置查询模板列表 |
| GET | `/ontology/sparql/history` | `ontology_sparql_view` | 查询历史（分页） |
| GET | `/ontology/sparql/export` | `ontology_sparql_query` | 导出查询结果为 CSV |
| GET | `/ontology/sparql/cache/stats` | `ontology_sparql_view` | 缓存统计（管理员监控用） |
| DELETE | `/ontology/sparql/cache/{ontologyId}` | `ontology_sparql_admin` | 手动清除缓存（管理员） |

### 6.2 接口详情

#### POST /ontology/sparql/query

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/ontology/sparql")
@Tag(description = "SPARQL查询", name = "SPARQL查询端点")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntSparqlController {

    private final OntSparqlService sparqlService;

    @PostMapping("/query")
    @Operation(summary = "执行SPARQL查询", description = "执行只读SPARQL查询（SELECT/ASK/CONSTRUCT/DESCRIBE）")
    @HasPermission("ontology_sparql_query")
    @SysLog("SPARQL查询")
    public R<SparqlResultVO> query(@Valid @RequestBody SparqlQueryRequest req) {
        return R.ok(sparqlService.executeQuery(req));
    }
}
```

请求体：

```json
{
  "ontologyId": 1,
  "query": "PREFIX std: <http://example.org/standard-ontology#>\nSELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10",
  "format": "JSON"
}
```

响应体（SELECT 示例）：

```json
{
  "code": 0,
  "data": {
    "queryType": "SELECT",
    "headVars": ["s", "p", "o"],
    "results": [
      {
        "s": {"value": "http://example.org/standard-ontology#Standard", "type": "uri"},
        "p": {"value": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "type": "uri"},
        "o": {"value": "http://www.w3.org/2002/07/owl#Class", "type": "uri"}
      }
    ],
    "rowCount": 1,
    "executionTimeMs": 45,
    "truncated": false
  }
}
```

---

## 7. 前端设计

### 7.1 页面布局

```
┌────────────────────────────────────────────────────────────────────┐
│  SPARQL 查询控制台                                     [本体工程▼] │
├────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐           │
│  │  查询模板：[子类层次查询          ▼]  [填充模板]     │           │
│  ├─────────────────────────────────────────────────────┤           │
│  │  ┌─────────────────────────────────────────────────┐ │           │
│  │  │  CodeMirror SPARQL 编辑器                       │ │           │
│  │  │  PREFIX std: <http://example.org/...#>         │ │           │
│  │  │  SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10  │ │           │
│  │  └─────────────────────────────────────────────────┘ │           │
│  ├─────────────────────────────────────────────────────┤           │
│  │  格式：[JSON▼]  [执行查询]  [导出CSV]  [查询历史]     │           │
│  └─────────────────────────────────────────────────────┘           │
├────────────────────────────────────────────────────────────────────┤
│  查询结果（SELECT · 10 行 · 45ms · 未截断）                         │
│  ┌──────────────┬──────────────┬──────────────┐                   │
│  │ s            │ p            │ o            │                   │
│  ├──────────────┼──────────────┼──────────────┤                   │
│  │ std:Standard │ rdf:type     │ owl:Class    │                   │
│  │ ...          │ ...          │ ...          │                   │
│  └──────────────┴──────────────┴──────────────┘                   │
├────────────────────────────────────────────────────────────────────┤
│  查询历史抽屉（右侧滑出）                                           │
│  ┌──────────────────┬────────┬────────┬──────┬───────┐           │
│  │ 查询语句          │ 类型   │ 行数   │ 耗时 │ 时间   │           │
│  ├──────────────────┼────────┼────────┼──────┼───────┤           │
│  │ SELECT ?s ?p... │ SELECT │ 10     │ 45ms │ 10:30  │           │
│  └──────────────────┴────────┴────────┴──────┴───────┘           │
└────────────────────────────────────────────────────────────────────┘
```

### 7.2 前端组件

| 组件 | 路径 | 职责 |
|------|------|------|
| `index.vue` | `web/src/views/ontology/sparql/index.vue` | 查询控制台主页面 |
| `SparqlEditor.vue` | `web/src/views/ontology/sparql/components/SparqlEditor.vue` | CodeMirror SPARQL 编辑器封装 |
| `ResultTable.vue` | `web/src/views/ontology/sparql/components/ResultTable.vue` | SELECT 结果表格展示 |
| `RdfPreview.vue` | `web/src/views/ontology/sparql/components/RdfPreview.vue` | CONSTRUCT/DESCRIBE 结果 RDF 预览 |
| `QueryHistory.vue` | `web/src/views/ontology/sparql/components/QueryHistory.vue` | 查询历史抽屉 |

### 7.3 API 文件

`web/src/api/ontology/sparql.ts`：

```ts
import request from '/@/utils/request';
import type { SparqlQueryRequest, SparqlResult, QueryTemplate } from '/@/types/ontology/sparql';

/** 执行 SPARQL 查询 */
export function executeSparqlQuery(data: SparqlQueryRequest) {
    return request({ url: '/admin/ontology/sparql/query', method: 'post', data });
}

/** 获取预置查询模板列表 */
export function fetchQueryTemplates() {
    return request({ url: '/admin/ontology/sparql/templates', method: 'get' });
}

/** 查询历史（分页） */
export function fetchQueryHistory(params: { page: number; size: number; ontologyId?: string }) {
    return request({ url: '/admin/ontology/sparql/history', method: 'get', params });
}

/** 导出查询结果为 CSV */
export function exportSparqlResult(params: { ontologyId: string; query: string }) {
    return request({ url: '/admin/ontology/sparql/export', method: 'get', params, responseType: 'blob' });
}

/** 缓存统计 */
export function fetchCacheStats() {
    return request({ url: '/admin/ontology/sparql/cache/stats', method: 'get' });
}

/** 清除缓存 */
export function evictCache(ontologyId: string) {
    return request({ url: `/admin/ontology/sparql/cache/${ontologyId}`, method: 'delete' });
}
```

---

## 8. Flyway 迁移脚本（V15）

```sql
-- ============================================================
-- V15__ontology_sparql_endpoint.sql
-- SPARQL查询端点模块
-- 对应PRD: §3.2.2 三元组存储选型 / §5.3 形式化要求
-- ============================================================

-- 1. SPARQL 查询日志表
CREATE TABLE IF NOT EXISTS ont_sparql_query_log (
  id BIGSERIAL PRIMARY KEY,
  ontology_id BIGINT NOT NULL,
  query_text TEXT NOT NULL,
  query_type VARCHAR(16) NOT NULL,
  result_format VARCHAR(16) NOT NULL DEFAULT 'JSON',
  result_row_count INTEGER,
  execution_time_ms BIGINT,
  status VARCHAR(16) NOT NULL,
  error_message TEXT,
  operated_by VARCHAR(64) NOT NULL,
  operated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  remote_addr VARCHAR(255),
  del_flag CHAR(1) DEFAULT '0' NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_sparql_log_ontology ON ont_sparql_query_log (ontology_id);
CREATE INDEX IF NOT EXISTS idx_sparql_log_operated_at ON ont_sparql_query_log (operated_at);
CREATE INDEX IF NOT EXISTS idx_sparql_log_status ON ont_sparql_query_log (status);
COMMENT ON TABLE ont_sparql_query_log IS 'SPARQL查询日志';
COMMENT ON COLUMN ont_sparql_query_log.ontology_id IS '本体工程ID';
COMMENT ON COLUMN ont_sparql_query_log.query_text IS 'SPARQL查询语句';
COMMENT ON COLUMN ont_sparql_query_log.query_type IS '查询类型 SELECT/ASK/CONSTRUCT/DESCRIBE';
COMMENT ON COLUMN ont_sparql_query_log.result_format IS '结果格式 JSON/TURTLE/JSONLD/RDFXML/CSV';
COMMENT ON COLUMN ont_sparql_query_log.result_row_count IS '结果行数';
COMMENT ON COLUMN ont_sparql_query_log.execution_time_ms IS '执行耗时毫秒';
COMMENT ON COLUMN ont_sparql_query_log.status IS '执行状态 SUCCESS/TIMEOUT/SYNTAX_ERROR/SECURITY_ERROR/INTERNAL_ERROR';
COMMENT ON COLUMN ont_sparql_query_log.error_message IS '错误信息';
COMMENT ON COLUMN ont_sparql_query_log.operated_by IS '操作人';
COMMENT ON COLUMN ont_sparql_query_log.operated_at IS '操作时间';
COMMENT ON COLUMN ont_sparql_query_log.remote_addr IS '请求来源IP';

-- 2. 菜单与权限注册
INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, menu_type, sort_order, del_flag, create_time) VALUES
(901200, 'SPARQL查询', NULL, '/ontology/sparql/index', 'ontology/sparql/index', 900000, '0', 12, '0', NOW()),
(901201, '查询执行', 'ontology_sparql_query', NULL, NULL, 901200, '1', 1, '0', NOW()),
(901202, '模板查看', 'ontology_sparql_view', NULL, NULL, 901200, '1', 2, '0', NOW()),
(901203, '缓存管理', 'ontology_sparql_admin', NULL, NULL, 901200, '1', 3, '0', NOW())
ON CONFLICT (menu_id) DO NOTHING;

-- 3. 角色权限分配（管理员/建模工程师/审核员拥有查询权限；访客仅模板查看）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 901200), (1, 901201), (1, 901202), (1, 901203),  -- 管理员
(2, 901200), (2, 901201), (2, 901202),                -- 建模工程师
(3, 901200), (3, 901201), (3, 901202),                -- 审核员
(4, 901202)                                            -- 访客仅模板查看
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 4. 迁移完整性校验
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_sparql_query_log') THEN
    RAISE EXCEPTION 'V15: ont_sparql_query_log 表创建失败';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901200) THEN
    RAISE EXCEPTION 'V15: SPARQL查询菜单注册失败';
  END IF;
  RAISE NOTICE 'V15__ontology_sparql_endpoint 完成';
END $$;
```

---

## 9. 后端类清单

### 9.1 Controller 层

| 类 | 路径 | 职责 |
|----|------|------|
| `OntSparqlController` | `controller/OntSparqlController.java` | REST API 入口 |

### 9.2 Service 层

| 类 | 路径 | 职责 |
|----|------|------|
| `OntSparqlService` | `service/OntSparqlService.java` | 查询编排接口 |
| `OntSparqlServiceImpl` | `service/impl/OntSparqlServiceImpl.java` | 查询编排实现 |
| `OntSparqlQueryLogService` | `service/OntSparqlQueryLogService.java` | 查询日志 CRUD |
| `OntSparqlQueryLogServiceImpl` | `service/impl/OntSparqlQueryLogServiceImpl.java` | 查询日志实现 |

### 9.3 沙箱与转换

| 类 | 路径 | 职责 |
|----|------|------|
| `SparqlQuerySandbox` | `sparql/SparqlQuerySandbox.java` | 查询校验与安全限制 |
| `SparqlResultConverter` | `sparql/SparqlResultConverter.java` | ARQ 结果 → VO 转换 |
| `OntSparqlCacheManager` | `sparql/OntSparqlCacheManager.java` | Model LRU 缓存管理 |
| `OntSparqlCacheListener` | `sparql/OntSparqlCacheListener.java` | 缓存失效事件监听 |
| `QueryTemplateProvider` | `sparql/QueryTemplateProvider.java` | 预置查询模板提供者 |

### 9.4 Entity / Mapper / DTO / VO

| 类 | 路径 | 职责 |
|----|------|------|
| `OntSparqlQueryLog` | `sparql/entity/OntSparqlQueryLog.java` | 查询日志实体 |
| `OntSparqlQueryLogMapper` | `sparql/mapper/OntSparqlQueryLogMapper.java` | 查询日志 Mapper |
| `SparqlQueryRequest` | `sparql/dto/SparqlQueryRequest.java` | 查询请求 DTO |
| `SparqlResultVO` | `vo/SparqlResultVO.java` | 查询结果 VO |
| `QueryTemplateVO` | `vo/QueryTemplateVO.java` | 查询模板 VO |
| `CacheStatsVO` | `vo/CacheStatsVO.java` | 缓存统计 VO |

### 9.5 事件

| 类 | 路径 | 职责 |
|----|------|------|
| `OntologyDataChangedEvent` | `sparql/event/OntologyDataChangedEvent.java` | 数据变更事件 |

---

## 10. 测试设计

### 10.1 单元测试

| 测试类 | 测试内容 |
|--------|----------|
| `SparqlQuerySandboxTest` | 查询类型白名单、禁止关键词、语法错误、空查询 |
| `SparqlResultConverterTest` | SELECT/ASK/CONSTRUCT/DESCRIBE 结果转换、行数限制截断 |
| `OntSparqlCacheManagerTest` | 缓存命中/未命中、LRU 淘汰、TTL 过期、手动失效 |
| `QueryTemplateProviderTest` | 模板列表完整性、占位符替换 |

### 10.2 集成测试

| 测试类 | 测试内容 |
|--------|----------|
| `OntSparqlControllerIT` | 端到端查询：附录D种子数据 SELECT 查询、CONSTRUCT 序列化、CSV 导出、查询历史、权限校验 |
| `OntSparqlCacheEvictionIT` | 数据变更事件触发缓存失效（需模块 04/08 改造配合） |

### 10.3 验收用例

| 用例 | 查询 | 预期结果 |
|------|------|----------|
| 附录D验证 | `appendix-d-verify` 模板 | 9 类实体均有实例，总数=29 |
| 子类层次 | `subclass-tree` 模板，typeIri=Standard | 返回 8 个子类 |
| 实例属性 | `instance-props` 模板，instanceIri=GB-T_31486-2024 | 返回 8 个数据属性值 |
| 类型统计 | `type-statistics` 模板 | 返回各类型实例计数 |
| 查询超时 | 构造超大笛卡尔积查询 | 10s 超时，返回 TIMEOUT 状态 |
| 安全拦截 | 带 SERVICE 关键词的查询 | 拒绝执行，返回 SECURITY_ERROR |
| INSERT 拦截 | `INSERT DATA { ... }` | 拒绝执行 |

---

## 11. 与后续模块的关系

| 后续模块 | 与本模块的关系 |
|---------|----------------|
| 14. 本体版本演化 | 版本演化后需触发 SPARQL 缓存失效；版本快照可序列化为 RDF 供 SPARQL 查询 |
| 15. 本体对齐引擎 | 跨本体对齐后，对齐映射（equivalentClass）注册到 Model，SPARQL 查询可跨越等价类 |
| 16. SKOS 受控词表 | SKOS 概念存入 Model，SPARQL 查询可检索概念层级 |
| 17. 推理物化 | 推理结果（传递闭包）写入 Model 后，SPARQL 查询可利用物化结果加速 |
| 18. 数据源映射 | 数据源映射写入的实例，通过 SPARQL 可查询验证 |
| 26. 事件驱动骨干 EDA | 数据变更事件可通过 EDA 总线发布，SPARQL 缓存监听失效（替代 Spring ApplicationEvent） |

---

> 本模块是本体能力增强层的第一个模块，补齐了"图查询语言"这一核心缺口。SPARQL 端点使平台从"SQL JOIN 地狱"升级为"图查询语义"，为后续多跳关联查询、故障传播分析、知识图谱问答奠定基础。一期采用内存 Model + LRU 缓存的轻量方案，满足≤10万实例规模；当规模或查询深度超阈值时，可平滑升级为 Jena TDB 持久化存储，上层查询逻辑零改动。
