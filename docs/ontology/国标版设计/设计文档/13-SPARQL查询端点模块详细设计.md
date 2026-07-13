# SPARQL 查询端点模块详细设计

> 设计编号：13
> 设计状态：评审修订版（仅设计，尚未开发）
> 设计依据：`标准本体建模平台PRD.md` §3.2.2、§7.2、§9、§10；`现有设计与IoT场景落地差距分析.md` §3.12、§5.2；已落地模块 03～12 及当前源码
> 前置模块：03-命名空间与IRI、04-实体类型、05-数据属性、06-对象属性、07-公理与规则、08-实体对象实例、09-校验引擎、10-序列化与交换、11-扩展管理、12-可视化
> 评审日期：2026-07-13

---

## 1. 评审结论

### 1.1 结论

SPARQL 查询能力应补充，但必须保持当前系统的事实边界：

1. **PostgreSQL 仍是一期唯一事实源**。本模块不是第二套可写图数据库，不提供 SPARQL Update，不允许绕过现有 Service 写关系库。
2. **SPARQL Model 是关系库的只读 RDF 投影**。一期按请求构建 Jena 内存 Model，优先保证权限隔离和数据一致性；达到明确阈值后再评估 TDB2/独立 Triplestore。
3. **首期只开放 `SELECT` 和 `ASK`**。`CONSTRUCT` 的大结果集、`DESCRIBE` 的实现相关语义、`SERVICE` 联邦查询和全部 Update 操作均不在首期范围。RDF 文件导出继续由模块 10 负责。
4. **复用实际已存在的完整模型组装器**：`OntologyModelExporter.buildCompleteModel(...)`。不得使用仅面向校验的 `OntologyModelAssembler` 作为 SPARQL 完整图来源，因为后者当前只组装校验所需子集。
5. **不在首期引入跨请求 Jena Model 缓存**。当前代码没有统一的数据修订号或跨进程失效机制，共享缓存会产生陈旧图和字段权限泄漏。后续可在模块 26 事件骨干和模块 36 数据权限落地后再增加按“工程修订号 + 权限视图”键控的缓存。
6. **端点默认只授予管理员**。PRD 中“建模员/审核员/访客”是逻辑角色，当前数据库实际只预置角色 1（管理员）和角色 2（普通用户），迁移脚本不得写入不存在的角色 3、4。

### 1.2 对原设计的关键修正

| 原设计问题 | 修正 |
|---|---|
| 调用不存在的 `NamespacePrefixResolver.getPrefixMap(ontologyId)` | 直接复用 `OntologyModelExporter`；其已调用实际存在的 `NamespacePrefixResolver.registerPrefixes(Model)` |
| 使用 `OntologyModelAssembler` 拼 Schema + Instance | 改为 `OntologyModelExporter.buildCompleteModel(ontologyId, ExportScope.FULL, PredicateStrategy.PREFERRED_ALIAS, null)`，确保类、属性、公理、实例声明完整 |
| 声称 `jena-arq` 已足够且“无需新增依赖”，同时又使用未引入的 Caffeine | 首期不使用 Caffeine；未来引入缓存时必须显式增加依赖并给出内存上限 |
| 用正则搜索 `SERVICE`/`USING` 作为主要沙箱 | 先由 ARQ 解析，再遍历查询语法树检查 `ElementService`、数据集描述等结构；文本扫描只能作为补充，不得作为唯一安全措施 |
| 查询执行后才检查 10000 行 | 对 `SELECT` 在执行前将查询上限收紧为 `min(用户LIMIT, 服务端上限)`；迭代结果时最多读取“上限+1”判断截断 |
| GET `/export` 携带查询 | 改为 POST，请求体传查询，避免 URL、网关日志和浏览器历史泄漏查询文本 |
| 共享 Model 未考虑并发与字段权限 | 首期每请求独立 Model；模块 36 接入后，先按用户生成授权 RDF 投影，再执行查询 |
| Flyway 使用 `BIGSERIAL`、错误菜单列和不存在角色 | 主键使用 `bigint + IdType.ASSIGN_ID`；菜单列完全对齐现有 `sys_menu`；只给角色 1 授权 |

---

## 2. 设计边界

### 2.1 本期包含

- 本体工程范围内的只读 `SELECT`、`ASK` 查询。
- SPARQL 1.1 基础图模式、过滤、聚合、子查询、属性路径。
- 查询语法校验、超时、结果行数、查询文本长度和并发限制。
- 预置查询模板。
- 查询历史与执行审计摘要。
- `SELECT` 结果 CSV 导出。
- 前端 SPARQL 控制台。

### 2.2 本期不包含

- `INSERT`、`DELETE`、`LOAD`、`CLEAR`、`CREATE`、`DROP` 等 SPARQL Update。
- `SERVICE` 联邦查询、外部 RDF 抓取、任意文件读取。
- `FROM`/`FROM NAMED` 指定外部或自定义 Dataset。
- 命名图管理、跨本体工程查询。
- 公网匿名 SPARQL Endpoint。
- `DESCRIBE`、通用 `CONSTRUCT` 和 RDF 下载；相关能力继续走模块 10 的受控导出。
- TDB2/GraphDB 的持久化双写。
- 大规模推理物化。

### 2.3 权威数据口径

| 数据 | 权威来源 | SPARQL 中的表现 |
|---|---|---|
| 实体类型、继承、不相交、等价 | PostgreSQL 模块 04 | `owl:Class`、`rdfs:subClassOf`、`owl:disjointWith`、`owl:equivalentClass` |
| 数据属性及约束元数据 | PostgreSQL 模块 05/07 | `owl:DatatypeProperty` 及导出器当前支持的声明 |
| 对象属性、domain/range | PostgreSQL 模块 06 | `owl:ObjectProperty`、`rdfs:domain`、`rdfs:range` |
| 实例及断言 | PostgreSQL 模块 08 | `rdf:type`、数据属性三元组、对象属性三元组 |
| 前缀 | `ont_namespace` | Jena Model prefix mapping，仅用于查询书写和结果缩写，不改变 IRI |
| SPARQL 结果 | 当次只读投影 | 非事实源，不允许回写 |

---

## 3. 总体架构

```text
Vue SPARQL 控制台
        │ POST /ontology/sparql/query
        ▼
OntSparqlController
        │ 权限 + 参数校验
        ▼
OntologyQueryAccessGuard
        │ 工程访问校验；模块36落地后扩展为字段/数据级授权
        ▼
SparqlQueryPolicy
        │ ARQ解析、类型白名单、SERVICE/FROM检查、LIMIT收紧
        ▼
SparqlModelProvider
        │
        ├─ OntologyModelExporter.buildCompleteModel(... FULL ...)
        └─ SparqlModelProjectionPolicy（模块36接入点）
        ▼
Jena ARQ QueryExecution（请求级 Model，try-with-resources）
        │
        ├─ SELECT → 行式结果 VO / CSV 流
        └─ ASK    → boolean
        ▼
查询日志（成功与失败均记录摘要）
```

### 3.1 复用现有代码

```java
Model model = ontologyModelExporter.buildCompleteModel(
    ontologyId,
    ExportScope.FULL,
    PredicateStrategy.INTERNAL_IRI,
    null
);
```

说明：

- `OntologyModelExporter` 当前已覆盖完整 Schema 与实例层，并在内部调用 `NamespacePrefixResolver.registerPrefixes(model)`。
- 查询端点固定使用 `PredicateStrategy.INTERNAL_IRI`，因为 `ont_data_property.iri` 是平台内全局唯一绝对 IRI；`preferred_alias` 可能只是本地别名，不适合作为查询图的稳定谓词 IRI。模块 10 的兼容导出策略不应改变查询端点语义。
- Model 为请求私有对象，查询完成后 `close()`，不得放入静态集合。

### 3.2 性能演进阈值

一期不承诺“10 万实例以内必然满足某固定响应时间”。是否升级持久化 Triplestore，应以压测数据决定。满足任一条件时进入专项选型：

- 单工程完整 RDF 投影持续超过 100 万三元组；
- P95 Model 构建时间超过 2 秒；
- P95 查询总耗时超过 5 秒；
- Model 构建造成 JVM 老年代或 GC 明显抖动；
- 需要跨请求稳定查询、命名图或跨服务访问。

升级时优先抽象 `SparqlDatasetProvider`，上层 Controller、查询策略和结果转换不直接依赖 TDB2。

---

## 4. 查询安全与资源治理

### 4.1 请求限制

| 配置 | 默认值 | 最大可配置值 | 说明 |
|---|---:|---:|---|
| `query-max-length` | 65536 字符 | 262144 | 超限直接拒绝 |
| `timeout-ms` | 10000 | 30000 | 包含 ARQ 执行，不包含排队时间 |
| `default-row-limit` | 1000 | 10000 | 用户不写 LIMIT 时使用 |
| `max-row-limit` | 10000 | 50000 | 用户 LIMIT 更大时收紧 |
| `max-offset` | 100000 | 1000000 | 防止超大 OFFSET 扫描 |
| `max-concurrent-queries` | 4 | 16 | 单 JVM 信号量；获取超时返回繁忙 |
| `export-max-rows` | 100000 | 500000 | 仅管理员 CSV 导出 |

### 4.2 语法树校验

处理顺序：

1. 空值、长度、编码和控制字符检查。
2. `QueryFactory.create(queryText, Syntax.syntaxSPARQL_11)` 解析。
3. 仅允许 `query.isSelectType()` 或 `query.isAskType()`。
4. 拒绝查询自带 Dataset 描述（`FROM`、`FROM NAMED`）。
5. 遍历 `query.getQueryPattern()`，发现 `ElementService` 即拒绝。
6. 限制 OFFSET；对 SELECT 的 LIMIT 做服务端收紧。
7. 不执行任何 `UpdateFactory` 路径。Update 文本由 Query 解析失败后统一返回“不支持只读查询之外的操作”，不得回显底层堆栈。

不得把“关键词正则”作为安全边界，因为关键词可能出现在注释或字符串字面量中，也可能被语法变体绕过。

### 4.3 超时与取消

- 使用 Jena 5 当前支持的 `QueryExecution` builder/timeout API；具体调用以实施时编译通过的 5.5.0 API 为准，设计文档不固化可能已废弃的方法签名。
- 必须使用 `try (QueryExecution qexec = ...)` 关闭资源。
- 请求断开或导出取消时调用 `abort()`/关闭执行器。
- 超时返回稳定业务码 `SPARQL_QUERY_TIMEOUT`，不返回 ARQ 完整异常文本。

### 4.4 结果限制

- SELECT 最多读取 `effectiveLimit + 1` 行；多出的第 1 行只用于设置 `truncated=true`。
- ASK 仅返回布尔值。
- CSV 使用流式写出，不先把全部结果装入 `List<Map<...>>`。
- 每个 RDFNode 返回 `value`、`nodeType`、`datatypeIri`、`language`；空绑定不生成伪字符串 `null`。
- 对超长字面量，前端表格预览截断，后端结果值不擅自改变；导出权限由模块 36 再约束。

### 4.5 权限隔离

一期：

- Controller 使用 `@HasPermission("ontology_sparql_query")`。
- `OntologyQueryAccessGuard` 校验 ontologyId 存在且未删除。
- 迁移脚本只给管理员角色 1 授权。

模块 36 落地后：

- `OntologyQueryAccessGuard` 增加本体工程 ACL。
- `SparqlModelProjectionPolicy` 在查询前移除当前用户不可见的数据属性三元组和实例；**不能仅在查询结果返回后脱敏**，否则 ASK、COUNT、FILTER、聚合已泄漏受限信息。
- 不同用户授权视图不得共享同一个可变 Model 缓存。

---

## 5. 数据模型

### 5.1 查询日志 `ont_sparql_query_log`

查询日志兼具“用户查询历史”和“执行审计摘要”，不是不可删除的合规审计账本；合规审计由模块 36 的 `ont_data_access_log` 承担。

```sql
CREATE TABLE ont_sparql_query_log (
  id bigint NOT NULL,
  ontology_id bigint NOT NULL,
  query_type varchar(16) NOT NULL,
  query_text text DEFAULT NULL,
  query_preview varchar(2000) NOT NULL,
  query_hash varchar(64) NOT NULL,
  result_format varchar(16) NOT NULL DEFAULT 'JSON',
  row_count integer NOT NULL DEFAULT 0,
  duration_ms bigint NOT NULL DEFAULT 0,
  truncated char(1) NOT NULL DEFAULT '0',
  status varchar(16) NOT NULL,
  error_code varchar(64) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_sparql_log_project
    FOREIGN KEY (ontology_id) REFERENCES ont_ontology_project(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_sparql_log_type
    CHECK (query_type IN ('SELECT', 'ASK')),
  CONSTRAINT ck_ont_sparql_log_status
    CHECK (status IN ('SUCCESS', 'REJECTED', 'TIMEOUT', 'FAILED')),
  CONSTRAINT ck_ont_sparql_log_truncated
    CHECK (truncated IN ('0', '1')),
  CONSTRAINT ck_ont_sparql_log_del_flag
    CHECK (del_flag IN ('0', '1'))
);

CREATE INDEX idx_ont_sparql_log_project_time
  ON ont_sparql_query_log (ontology_id, create_time DESC)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_sparql_log_creator_time
  ON ont_sparql_query_log (create_by, create_time DESC)
  WHERE del_flag = '0';
```

约束：

- `query_hash` 为规范化查询文本的 SHA-256，用于统计和问题定位。
- `query_preview` 是去除/替换字符串字面量后的受控摘要，不能简单截取原查询。
- `query_text` 默认不落库；只有部署明确开启完整历史且模块 36 提供加密与访问控制时才保存。
- 查询文本最大长度由 DTO 校验；历史默认仅本人可见，管理员查看也必须审计。
- 错误日志只存稳定错误码，不存数据库口令、堆栈或请求头。
- 后续应配置历史保留期；删除历史不等同于删除模块 36 合规审计。

### 5.2 请求与响应

```java
public class SparqlQueryRequest {
    @NotNull
    private Long ontologyId;

    @NotBlank
    @Size(max = 65536)
    private String query;

    /** JSON；CSV 走独立导出接口 */
    private String format = "JSON";
}
```

```java
public class SparqlQueryResultVO {
    private String queryType;
    private List<String> variables;
    private List<Map<String, SparqlBindingVO>> rows;
    private Boolean booleanResult;
    private Integer rowCount;
    private Long durationMs;
    private Boolean truncated;
}
```

`SparqlBindingVO.nodeType` 取值固定为 `IRI`、`BNODE`、`LITERAL`；字面量可带 `datatypeIri` 或 `language`。

---

## 6. 接口设计

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/ontology/sparql/query` | `ontology_sparql_query` | 执行 SELECT/ASK |
| POST | `/ontology/sparql/query/export` | `ontology_sparql_export` | 流式导出 SELECT CSV |
| GET | `/ontology/sparql/templates` | `ontology_sparql_view` | 获取预置模板 |
| GET | `/ontology/sparql/history/page` | `ontology_sparql_view` | 查询本人历史；管理员可按用户筛选 |
| GET | `/ontology/sparql/history/{id}` | `ontology_sparql_view` | 执行详情/脱敏预览，需所有权校验 |
| DELETE | `/ontology/sparql/history/{id}` | `ontology_sparql_query` | 删除本人查询历史 |

不设计公开 GET 查询端点，不允许把完整查询放在 URL 参数中。

### 6.1 统一错误码

| 错误码 | 场景 |
|---|---|
| `SPARQL_QUERY_INVALID` | 语法错误或非 SELECT/ASK |
| `SPARQL_QUERY_FORBIDDEN_FEATURE` | SERVICE、FROM/FROM NAMED 等 |
| `SPARQL_QUERY_LIMIT_EXCEEDED` | 文本、OFFSET 或导出上限超限 |
| `SPARQL_QUERY_TIMEOUT` | 查询超时 |
| `SPARQL_QUERY_BUSY` | 并发槽位耗尽 |
| `ONTOLOGY_PROJECT_NOT_FOUND` | 工程不存在或已删除 |
| `ONTOLOGY_PROJECT_FORBIDDEN` | 无工程访问权限 |

---

## 7. 预置查询模板

模板以只读资源文件维护，不建表。占位符值在前端填充时必须作为完整 IRI 放入 `<...>`，不得拼接到前缀本地名后再假定所有工程都使用 `std:`。

| ID | 名称 | 类型 | 说明 |
|---|---|---|---|
| `subclass-tree` | 子类层次 | SELECT | 查询指定类的传递子类 |
| `instance-properties` | 实例属性 | SELECT | 查询实例全部断言 |
| `one-hop-neighbors` | 一跳邻居 | SELECT | 查询入边和出边 |
| `type-statistics` | 类型统计 | SELECT | 按 `rdf:type` 统计实例数 |
| `iri-label-search` | IRI/标签搜索 | SELECT | 按 IRI 或 `rdfs:label` 检索 |
| `appendix-d-summary` | 附录 D 摘要 | SELECT | 按附录 D 实例 IRI 前缀统计，不硬编码错误总数 |

示例：

```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
SELECT ?subClass ?label WHERE {
  ?subClass rdfs:subClassOf+ <${classIri}> .
  OPTIONAL { ?subClass rdfs:label ?label }
}
ORDER BY ?subClass
LIMIT 1000
```

模板只是编辑器辅助，不是服务器端“参数化 SPARQL”安全机制。

---

## 8. 前端设计

### 8.1 页面与目录

```text
web/src/api/ontology/sparql.ts
web/src/types/ontology/sparql.ts
web/src/views/ontology/sparql/index.vue
web/src/views/ontology/sparql/components/QueryEditor.vue
web/src/views/ontology/sparql/components/QueryResultTable.vue
web/src/views/ontology/sparql/components/QueryHistoryDrawer.vue
```

- 使用项目已安装的 CodeMirror，不新增 Monaco。
- 使用 Vue 3 Composition API + `<script setup lang="ts">`。
- API 统一通过 `/@/utils/request`。
- 所有动态列必须用稳定变量名作为 key。
- 查询执行期间可取消；重复点击需防抖并禁用按钮。
- 大结果只渲染当前页/虚拟列表，避免 DOM 卡顿。
- IRI 单元格支持复制，默认不自动访问外部链接。

### 8.2 页面布局

```text
本体工程选择 | 模板选择 | 执行 | 取消 | 导出CSV | 历史
------------------------------------------------------
CodeMirror SPARQL 编辑器
------------------------------------------------------
结果：类型 / 行数 / 耗时 / 是否截断
SELECT 表格 或 ASK 布尔结果
```

---

## 9. Flyway 设计

迁移文件：

```text
server/pig-common/pig-common-data/src/main/resources/db/migration/
V15__ontology_sparql_endpoint.sql
```

要求：

1. 使用 `bigint` 主键，Java 实体使用 `@TableId(type = IdType.ASSIGN_ID)`。
2. 审计字段统一为 `create_by/create_time/update_by/update_time/del_flag`。
3. `sys_menu` 必须写全现有 17 列：

```sql
INSERT INTO sys_menu
(menu_id, name, permission, path, component, parent_id, icon, visible,
 sort_order, keep_alive, embedded, menu_type,
 create_by, create_time, update_by, update_time, del_flag)
VALUES
(901200, 'SPARQL查询', NULL, '/ontology/sparql/index', NULL, 900000,
 'ele-Search', '1', 12, '0', NULL, '0',
 'admin', now(), 'admin', now(), '0'),
(901201, 'SPARQL查看', 'ontology_sparql_view', NULL, NULL, 901200,
 NULL, '1', 1, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0'),
(901202, 'SPARQL执行', 'ontology_sparql_query', NULL, NULL, 901200,
 NULL, '1', 2, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0'),
(901203, 'SPARQL导出', 'ontology_sparql_export', NULL, NULL, 901200,
 NULL, '1', 3, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
VALUES (1, 901200), (1, 901201), (1, 901202), (1, 901203)
ON CONFLICT (role_id, menu_id) DO NOTHING;
```

不得在该迁移中创建或假定“审核员/访客”等角色。

---

## 10. 后端类清单

| 类 | 建议包 | 职责 |
|---|---|---|
| `OntSparqlController` | `controller` | REST 入口 |
| `OntSparqlService` / `Impl` | `sparql.service` | 查询编排 |
| `SparqlQueryPolicy` | `sparql.policy` | 语法树与资源限制 |
| `SparqlModelProvider` | `sparql.model` | 调用完整导出器构建请求级 Model |
| `SparqlModelProjectionPolicy` | `sparql.model` | 模块 36 授权投影扩展点 |
| `OntologyQueryAccessGuard` | `sparql.security` | 工程访问校验 |
| `SparqlResultConverter` | `sparql.converter` | RDFNode 到 VO |
| `SparqlTemplateProvider` | `sparql.template` | 读取只读模板 |
| `OntSparqlQueryLog` / Mapper | `sparql.log` | 查询历史 |
| `SparqlProperties` | `sparql.config` | 限制参数 |

不新建与现有导出器重复的 RDF 组装器或序列化器。

---

## 11. 测试与验收

### 11.1 必测项

- `SELECT`、`ASK` 正常执行。
- Update、SERVICE、FROM/FROM NAMED、超长查询、超大 OFFSET 被拒绝。
- 用户 LIMIT 缺失、过大、较小时的服务端收紧逻辑。
- 超时、取消和并发槽位释放。
- typed literal、语言标签、blank node、未绑定变量转换。
- 工程 A 查询结果不包含工程 B 的实例。
- 查询日志成功/拒绝/超时均正确记录，且不记录堆栈和令牌。
- CSV 流式导出内存稳定。
- 模块 36 接入后，受限属性不会参与 SELECT、ASK、COUNT、FILTER。

### 11.2 验收标准

| 用例 | 预期 |
|---|---|
| 查询附录 D 实例类型统计 | 返回与当前数据库事实一致的统计，不依赖文档硬编码数量 |
| 查询类层次 | 支持 `rdfs:subClassOf+` 属性路径 |
| 执行 `INSERT DATA` | 返回 `SPARQL_QUERY_INVALID`，数据库不变 |
| 执行带 `SERVICE` 查询 | 返回 `SPARQL_QUERY_FORBIDDEN_FEATURE` |
| 无 LIMIT 的 SELECT | 最多返回默认行数并正确标记是否截断 |
| 查询超时 | 资源关闭、并发许可释放、日志状态为 TIMEOUT |
| 越权 ontologyId | 在构建 Model 前拒绝 |

---

## 12. 与后续模块的关系

| 模块 | 关系 |
|---|---|
| 14 本体版本演化 | 可对版本快照提供受控离线查询；不直接查询已被覆盖的工作区历史状态 |
| 26 EDA | Schema/实例变更事件可用于未来 RDF 投影缓存失效；首期无共享缓存，不形成强依赖 |
| 36 安全与合规 | 提供工程 ACL、RDF 授权投影、SPARQL 数据访问审计；是扩大非管理员使用范围的前提 |
| 17 推理物化 | 若引入推理结果，必须明确查询 Dataset 是否包含推理图及其版本 |
| 21 时序数据 | 高频时序数据不全量复制进本模块内存 Model；只查询语义索引和摘要 |

---

> 最终定位：本模块提供“受控的 RDF 只读查询视图”，而不是第二个写入通道。首期以正确性、工程隔离和可审计为优先，不用不可靠的共享内存缓存换取表面性能，也不把 SPARQL 端点误当成公网开放的通用 Triplestore。
