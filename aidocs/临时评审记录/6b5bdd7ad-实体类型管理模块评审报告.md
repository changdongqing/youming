# Git 提交 6b5bdd7ad 实体类型管理模块评审报告

> 评审对象：`6b5bdd7ad 实体类型管理开发完成待评审测试`
> 基线提交：`00c191c1e 命名空间功能开发`
> 评审范围：详细设计、Flyway V6、后端 Controller/Entity/Mapper/Service、前端 API/页面、跨模块集成与测试可验收性
> 评审角色：本体建模、Java/Spring/MyBatis-Plus、PostgreSQL/Flyway、Vue 3/TypeScript
> 评审日期：2026-07-10
> 评审性质：仅评审；除本报告外未修改被评审代码与设计

---

## 一、执行摘要

### 1.1 最终结论

**评审结论：不通过，必须修正后重新评审。**

当前提交不能进入功能测试或合并验收，主要原因不是一般性代码质量问题，而是同时存在以下两类阻断问题：

1. **数据库迁移不可执行**：V6 预置数据中“列项”和“列表”同时使用 `List` 作为 `name` 和 IRI 本地名，违反脚本自身先创建的唯一索引。真实 PostgreSQL 事务演练在插入 86 条预置实体类型时立即失败，Flyway 会阻止应用启动。
2. **核心本体语义错误**：设计和种子数据把 `Clause` 与抽象父类 `InformationUnit` 建模为 `owl:equivalentClass`。等价类表示两个类具有相同实例集合，并不表示“Clause 同时属于两个父类”。该建模会把所有 `InformationUnit` 等价为 `Clause`，进而使 `Example`、`Note`、`Footnote`、`Checklist`、`List` 等信息单元子类都被推理为 Clause，破坏核心 Schema。

在排除上述阻断项后，仍存在新增标签未保存、IRI 自动生成路径被 Bean Validation 提前拦截、复合主键标签更新方式错误、数据库缺少引用约束、命名空间可被错误删除或改 URI、继承环检测不可靠、请求实体存在批量赋值风险、缺少任何自动化测试等高风险问题。

### 1.2 维度结论

| 维度 | 结论 | 说明 |
|------|------|------|
| **本体语义正确性** | ❌ 不通过 | Clause/InformationUnit 等价关系错误；List 术语冲突暴露核心词汇表未做唯一性核验 |
| **详细设计质量** | ⚠️ 需重构 | 结构较完整，但 72/86 条口径矛盾，职责、API、关系语义和演进方案不完全一致 |
| **数据库/Flyway** | ❌ 不通过 | V6 实际执行失败；无外键、CHECK、反向索引和关系规范化约束 |
| **Java 后端** | ❌ 不通过 | 编译通过，但关键 CRUD 路径存在确定性或高概率运行时缺陷，领域不变量保护不足 |
| **Vue 前端** | ⚠️ 有条件 | 构建和 ESLint 通过，基本页面可编译；交互、类型、父类候选和错误处理仍需完善 |
| **跨模块一致性** | ❌ 不通过 | 命名空间生命周期未纳入实体类型引用检查，允许产生失效 IRI/失效 namespace 引用 |
| **测试与可验收性** | ❌ 不通过 | 无后端测试、无前端测试、无迁移测试；详细设计的核心验收点当前无法达成 |

### 1.3 值得保留的设计与实现

尽管结论为不通过，本次提交仍有以下可保留基础：

- 模块目录、Controller/Service/Mapper 分层及权限命名与现有 `pig-ontology-biz` 保持一致。
- 主表、标签表、层次关系表的拆分方向正确，支持多语言标签和多继承的关系型表达。
- 主表使用逻辑删除、部分唯一索引和 `ASSIGN_ID`，与仓库既有 PostgreSQL/Flyway 风格基本一致。
- 内置实体类型的不可删除和语义字段保护意图正确。
- 新增、修改、删除使用事务编排，关系表没有单独暴露 Controller，聚合边界合理。
- 前端采用“左树右详情”布局，核心/抽象标记、内置删除禁用、详情展示等基本交互已具备。
- 后端 clean compile、前端生产构建、目标文件 ESLint 均通过，说明静态编译层面没有阻断错误。

这些优点可以作为修复版本的基础，不建议推倒重写整个模块。

---

## 二、评审范围与验证记录

### 2.1 提交范围

目标提交新增 18 个文件、约 2124 行：

- 详细设计：`docs/ontology/国标版设计/设计文档/04-实体类型管理模块详细设计.md`
- Flyway：`V6__ontology_entity_type.sql`
- 后端：1 个 Controller、5 个 Entity、5 个 Mapper、1 个 Service、1 个 ServiceImpl、1 个树节点 VO
- 前端：`entity-type.ts`、`entity-type/index.vue`

### 2.2 实际执行的验证

| 验证项 | 命令/方式 | 结果 |
|--------|-----------|------|
| Git 差异格式检查 | `git diff ... --check` | ✅ 通过 |
| 后端全量重编译 | `mvn -pl pig-ontology/pig-ontology-biz -am -DskipTests clean compile` | ✅ BUILD SUCCESS；31 个 ontology 源文件重新编译 |
| 前端生产构建 | `pnpm build` | ✅ Vite build 成功 |
| 前端目标文件 ESLint | `pnpm exec eslint ...entity-type.ts ...index.vue` | ✅ 通过 |
| 当前开发库 Flyway 状态 | 查询 `flyway_schema_history` | 当前仅 V1-V5 成功，V6 尚未应用 |
| V6 原脚本 PostgreSQL 演练 | 在 `youmingdb` 中 `BEGIN` 后执行原 V6，失败后连接回滚 | ❌ 插入实体类型时因重复 IRI `...#List` 失败 |
| V6 后续结构核验 | 仅在输入流中临时消除 List 冲突后，于事务内执行并回滚 | 可插入 86 类型、86 标签、71 层次、2 等价、1 不相交；确认只有主键/非空约束，没有任何外键和 CHECK |
| 自动化测试扫描 | 扫描 ontology `src/test` 和前端 ontology spec/test | ❌ 未发现任何测试 |

> 数据库验证全程位于 PostgreSQL 事务中，失败连接自动回滚或显式 `ROLLBACK`，未改变当前业务库；复核后 `ont_entity_type` 仍不存在，Flyway 历史仍停留在 V5。

### 2.3 构建通过不等于功能通过

本次后端和前端构建均成功，但以下问题不会被编译器发现：

- Flyway 种子数据唯一约束冲突；
- OWL 等价类语义错误；
- `BaseMapper.updateById` 对复合主键实体的不适用；
- 新增流程漏保存标签；
- `@Valid` 与服务层自动生成 IRI 的执行顺序冲突；
- 深层/宽层继承图的环检测漏判；
- 命名空间逻辑删除后产生悬空业务引用。

因此当前“编译成功”只能作为静态基线，不能作为提交可验收的证据。

---

## 三、阻断级问题（P0）

## P0-01：V6 迁移脚本因 `List` 重名必然失败

**证据：**

- `V6__ontology_entity_type.sql:164`：
  - `940044`，IRI `http://example.org/standard-ontology#List`，name `List`，中文“列项”。
- `V6__ontology_entity_type.sql:175`：
  - `940054`，同一 IRI、同一 name，中文“列表”。
- `V6:40-41` 在插入数据前已创建未删除数据的 IRI 和 name 唯一索引。

**实测结果：**

```text
ERROR: duplicate key value violates unique constraint "uk_ont_entity_type_iri"
DETAIL: Key (iri)=(http://example.org/standard-ontology#List) already exists.
```

**影响：**

- Flyway V6 事务失败；
- `pig-boot` 下次启动时会因迁移失败而无法完成启动；
- 设计验收点“V6 创建表并预置 86 条实体类型”无法达成；
- 当前开发库历史停留在 V5，实体类型页面没有对应数据库表。

**本体建模判断：**

“列项”和“列表”是两个不同概念，不能只依赖中文标签区分而共享同一 IRI。应回到国标术语源表确认权威英文名。较合理的候选是把“列项”建模为 `ListItem`，其子类为 `UnnumberedListItem` / `NumberedListItem`，把“列表”保留为 `List`；最终名称必须以标准原文或项目统一词汇表为准，不应只为绕开数据库约束随意改名。

**修复要求：**

1. 先建立“中文术语—英文 Name—IRI 本地名—来源条款—父类”的权威词汇核验表；
2. 修正详细设计和 V6 中三个“列项”相关本地名；
3. 增加迁移前静态重复检查和迁移后数量/唯一性断言；
4. 当前 V6 尚未成功应用，可直接修正 V6；不要为了规避校验新增一个建立在失败 V6 之上的 V7。

---

## P0-02：`Clause equivalentClass InformationUnit` 是错误的 OWL 语义

**证据：**

- 详细设计 `04...md:241-249` 声明 Clause 与 InformationUnit 双向等价；
- `V6:272-277` 插入 `(Clause, InformationUnit)` 和反向记录；
- 同时 `InformationUnit` 又是 `AdditionalInformation` 的父类，后者再派生 Example、Note、Footnote、Checklist、List。

**问题本质：**

`owl:equivalentClass` 表示两个类具有相同的类扩展，即：

```text
Clause ⊑ InformationUnit
InformationUnit ⊑ Clause
```

这不是“双重归属”的表达方式。若 Clause 既属于 StructuralElement 又属于 InformationUnit，正确模型是多继承：

```text
Clause rdfs:subClassOf StructuralElement
Clause rdfs:subClassOf InformationUnit
```

当前等价模型会产生以下错误推理：

```text
Example ⊑ AdditionalInformation ⊑ InformationUnit ≡ Clause
Note ⊑ AdditionalInformation ⊑ InformationUnit ≡ Clause
Footnote ⊑ AdditionalInformation ⊑ InformationUnit ≡ Clause
```

也就是说示例、注、脚注等都会被推理成 Clause；同时抽象的 `InformationUnit` 与具体 `Clause` 被完全合并，抽象类标记也失去清晰语义。

**影响：**

- 核心 Schema 语义失真；
- 后续 Jena/OWL Reasoner 一致性与分类结果错误；
- 对象属性定义域/值域、实例 `rdf:type`、SHACL/公理校验都会被污染；
- 导出的 RDF/OWL 即使语法正确，也不符合预期概念模型。

**修复要求：**

1. 删除 Clause/InformationUnit 等价记录；
2. 在 hierarchy 中增加 `(parent=InformationUnit, child=Clause)`，保留 `(StructuralElement, Clause)`，形成真正多继承；
3. 修正文档中“等价类表示双重归属”的错误说明；
4. 增加 Jena 推理测试：Clause 实例应推理为 InformationUnit；InformationUnit 实例不得反向推理为 Clause；Example/Note 不得被推理为 Clause；
5. 等价类功能仍可保留，但必须只用于概念外延确实相同的两个类。

---

## 四、高优先级问题（P1）

## P1-01：新增实体类型时中文标签完全没有保存

**证据：**

- `OntEntityTypeServiceImpl.java:81-82`：保存主表后只调用 `saveRelations(entityType)`；
- 新增路径没有调用 `saveLabel(...)`；
- 修改路径 `117-119` 才调用保存标签。

**影响：**

- 前端新增弹窗填写的中文标签被静默丢弃；
- 详情 `labels` 为空，继承树回退显示英文 name；
- 附录 A.1 的 Label 元数据未完整落地；
- 用户收到“保存成功”，但实际数据不完整，属于高风险静默数据丢失。

**建议：**

- 新增事务中主表保存成功后，若 Label 是必填元数据则校验非空并保存 zh 标签；
- 若允许缺省，应在设计中明确，并由前端显示回退规则；
- 添加“新增后查询详情，zh 标签与请求一致”的集成测试。

---

## P1-02：服务层 IRI 自动生成逻辑被 `@Valid` 提前拦截

**证据：**

- `OntEntityType.iri` 标注 `@NotBlank`（实体类 39-41 行）；
- Controller 的 POST/PUT 使用 `@Valid @RequestBody`；
- 服务层 `validateEntityType` 的 289-291 行才尝试在 IRI 为空时自动生成；
- 前端 IRI 输入框提示“可由命名空间+名称自动生成”，但只有手动点击“自动生成”才会赋值。

**执行顺序：**

```text
HTTP 请求 -> Bean Validation(@NotBlank) -> Controller -> Service 自动生成
```

IRI 为空时请求在进入 Service 前就失败，因此服务层自动生成分支不可达。

**建议：**

- 使用独立 CreateDTO；允许 `iri` 为空，由服务根据 namespace + name 生成；
- 或要求 IRI 必填并删除服务自动生成分支与“可自动”文案；
- 推荐前者，并由后端作为唯一权威生成方，前端只做预览，避免两端规则漂移。

---

## P1-03：标签表为复合主键，但使用了 `updateById`

**证据：**

- `ont_entity_type_label` 主键为 `(entity_type_id, locale)`；
- `OntEntityTypeLabel` 没有且无法用单个 `@TableId` 正确表达该复合主键；
- `saveLabel` 在已有标签分支调用 `labelMapper.updateById(existing)`（362-364 行）。

**问题：**

MyBatis-Plus 的 `updateById` 面向单列主键。当前实体没有单一主键元数据，无法可靠生成基于 `(entity_type_id, locale)` 的更新条件，可能在 Mapper 初始化时告警，或在运行时生成错误/缺失主键条件并失败。

**建议：**

使用显式条件更新：

```java
labelMapper.update(
    updateEntity,
    Wrappers.<OntEntityTypeLabel>lambdaUpdate()
        .eq(OntEntityTypeLabel::getEntityTypeId, entityTypeId)
        .eq(OntEntityTypeLabel::getLocale, locale)
);
```

或者执行 PostgreSQL upsert。不要为适配 `updateById` 人为添加无业务意义的代理 id，除非全项目关系表统一采用代理键。

---

## P1-04：数据库没有任何外键和核心 CHECK 约束

**实测：**

临时消除 List 冲突后执行 V6，`pg_constraint` 只显示主键和 NOT NULL，没有外键、CHECK。

**缺失项：**

- `ont_entity_type.namespace_id -> ont_namespace.id`；
- label/hierarchy/disjoint/equivalent 到 `ont_entity_type.id` 的引用；
- `is_abstract/is_builtin/del_flag IN ('0','1')`；
- `parent_id <> child_id`；
- `entity_type_id <> equivalent_id`；
- `type_a <> type_b`；
- 设计声明的 `type_a < type_b` 规范化约束。

**影响：**

绕过 Service 的脚本、批量导入、未来新接口或程序缺陷都可写入悬空关系、自环、不规范布尔值、重复对称关系。对于本体 Schema，数据库是最后一道完整性边界，不能完全依赖单个 Service。

**建议：**

- 补充 `ON DELETE RESTRICT` 外键；逻辑删除仍由应用层做“未删除引用”检查，外键负责防止物理孤儿；
- 为 char(1) 和关系自反性增加 CHECK；
- disjoint/equivalent 使用规范化单行存储，或由 Service 强制双向一致，二者择一并写入设计；
- 添加迁移测试验证非法关系被数据库拒绝。

---

## P1-05：关系表缺少反向查询索引

当前复合主键只优化第一列：

- hierarchy PK `(parent_id, child_id)`，但获取父类、保存父类关系、环检测都高频按 `child_id` 查询；
- equivalent PK `(entity_type_id, equivalent_id)`，反向引用检查需要 `equivalent_id`；
- disjoint PK `(type_a, type_b)`，双向查询需要 `type_b`。

**建议新增：**

```text
idx_ont_entity_type_hierarchy_child(child_id)
idx_ont_entity_type_equivalent_reverse(equivalent_id)
idx_ont_entity_type_disjoint_reverse(type_b)
```

若改为单行规范化对称关系，上述反向索引仍然必要。

---

## P1-06：命名空间生命周期没有接入实体类型引用保护

**证据：**

- `OntNamespaceServiceImpl.removeNamespace` 只检查单位条目引用（98-101 行）；
- 本提交新增了 `OntEntityType.namespaceId`，但没有修改命名空间删除和修改逻辑；
- V6 也没有 namespace 外键。

**可复现场景：**

1. 创建扩展命名空间 N；
2. 在 N 下创建实体类型 T；
3. 删除 N：命名空间 Service 不检查实体类型，可逻辑删除成功；
4. T 仍保存 namespace_id，详情中的 namespace 可能为空，IRI 治理链断裂。

更严重的是，扩展命名空间 URI 当前允许修改。若 N 已被实体类型使用，修改 URI 后既有 T.iri 不会同步变化，`namespace_id` 与 `iri` 前缀发生语义不一致。

**建议：**

- 删除命名空间前检查所有未删除实体类型引用；
- 命名空间被任何 Schema 元素引用后锁定 URI；前缀可否修改需单独定义，因为前缀通常是序列化别名而非身份；
- 增加数据库外键和跨模块集成测试；
- 后续数据属性、对象属性、公理上线时统一维护引用计数/依赖检查，避免每个模块遗漏。

---

## P1-07：继承环检测的“100 层限制”实现错误，可能漏判

**证据：**

`hasCycle` 使用队列遍历，但 `depth++` 是“已处理节点数”，不是图路径深度；当处理 100 个节点后，循环直接结束并返回 false。

**风险：**

- 宽度较大的多继承图即使路径不深，也可能在访问到目标 child 前耗尽 100 次额度；
- 超过限制时当前实现选择“视为无环”，属于 fail-open；
- 一旦存在环，树构建会形成对象循环，JSON 序列化可能无限递归或栈溢出；纯环上的节点也可能没有根，直接从树结果消失。

**建议：**

- 使用标准 DFS 三色标记，或 PostgreSQL recursive CTE 检查“新父类是否可达当前 child”；
- 限制应作用于实际路径深度；超过限制必须拒绝并提示，不得返回“无环”；
- parentIds 先去重；拒绝自身为父类；
- 测试直接环、间接环、菱形多继承、宽图、超过 100 层和已有脏环。

---

## P1-08：直接用持久化 Entity 接收请求，存在批量赋值和校验缺口

`OntEntityType` 同时承担数据库实体、查询参数、Create/Update 请求体和响应体，前端编辑时还通过 `{...row}` 把全部字段原样回传。

**客户端可提交的敏感字段包括：**

- `isBuiltin`、`delFlag`；
- `createBy/createTime/updateBy/updateTime`；
- 其他非当前操作允许修改的字段。

扩展类型路径直接 `updateById(entityType)`，没有白名单复制。虽然服务会重置 `isBuiltin='0'`，但没有同样保护 `delFlag` 和审计字段，也没有字段长度、布尔枚举、IRI 格式、label 长度等完整校验。

**建议：**

拆分：

- `OntEntityTypeCreateDTO`
- `OntEntityTypeUpdateDTO`
- `OntEntityTypeQuery`
- `OntEntityTypeDetailVO`

由 Service 显式复制允许字段；审计字段和逻辑删除字段永不从请求体绑定。新增 `@Size`、`@Pattern`、合法 IRI/namespace 一致性等校验。

---

## P1-09：IRI、namespace、name 三者没有一致性约束

当前只检查：namespace 存在、IRI 全局唯一、name 全局唯一。没有检查：

```text
iri == namespace.uri + name
```

因此可以创建：

```text
namespace = std
name      = Vehicle
iri       = http://other.example/AnotherClass
```

这会使 namespace_id、name 与真正身份 IRI 相互矛盾，后续 QName 序列化和前缀展示不可相信。

同时，全局 `name` 唯一对本体系统过严：不同命名空间本来可以存在相同 local name，只要完整 IRI 不同。未来引入 ontology_id 后，全局 name 唯一会进一步阻碍多本体工程隔离。

**建议：**

- 完整 IRI 保持全局唯一；
- local name 唯一范围改为 `(namespace_id, name)`，引入工程后为 `(ontology_id, namespace_id, name)`；
- 后端统一生成或严格验证 IRI 与 namespace/name 的对应关系；
- 若允许自定义完整 IRI，则不要再把 namespace_id/name 作为可独立修改的冗余事实，需定义权威来源。

---

## P1-10：扩展类型默认写入核心 `std` 命名空间，治理边界不清

前端新增时默认选择 prefix=`std` 的命名空间；后端只强制 `isBuiltin='0'`，并不限制扩展类型使用内置核心命名空间。

结果是用户可在标准核心 IRI 空间中创建非内置类型。后续导出时，这些扩展类看起来像官方核心词汇，破坏核心/扩展边界。

**建议：**

- `isBuiltin='0'` 的类型默认要求选择扩展命名空间；
- 是否允许管理员向核心命名空间追加类，应作为显式治理权限，而不是普通新增能力；
- 前端不要默认 std，优先选择用户默认扩展命名空间，若不存在则提示先创建。

---

## P1-11：`ontology_id` 延后方案产生明显迁移债务

详细设计明确删除 PRD 中的 `ontology_id`，理由是本体工程尚未开发。这样虽然减少首期代码，但会造成：

- 所有实体类型成为全局共享 Schema；
- 无法区分核心本体、行业扩展本体、用户工程；
- name/IRI/关系查询都没有工程范围；
- 后续添加 NOT NULL ontology_id 时，需要给 86 条核心数据和所有用户数据回填归属，并重建唯一索引、关系约束和 API；
- 当前前端和 API 形态将形成单本体假设，后续改造面较大。

**建议的最小方案：**

- 如果产品近期确定只支持单一核心本体，应在 PRD/设计/页面明确“单本体阶段”，并设计固定的 core ontology 标识和未来迁移脚本；
- 更稳妥的是尽早建立最小 `ont_ontology_project`/`ont_ontology` 表，先预置一个 CORE 工程，实体类型从第一天带 ontology_id；
- 至少不要继续扩大无 scope 的数据属性、对象属性、公理表，否则迁移成本会按模块叠加。

---

## P1-12：没有任何自动化测试，关键领域规则无回归保障

扫描未发现 ontology 后端 `src/test`，前端也没有 entity-type spec/test。

本模块包含多项非常适合自动化验证的规则：

- Flyway 86 条词汇唯一性和关系完整性；
- 内置保护；
- 新增标签；
- IRI 生成和唯一性；
- 多继承与环检测；
- 命名空间引用；
- 关系对称性；
- 删除引用检查；
- Clause 多继承推理语义。

本次 P0/P1 问题中多数都能被低成本测试直接拦截。缺少测试是这些问题进入提交的主要过程原因之一。

---

## 五、中优先级问题（P2）

## P2-01：详细设计内部存在 72/86 条和 ID 范围矛盾

- 设计 116 行写“共 72 条”；
- 120 行写 id `940001-940072`；
- 后续实际列到 `940086`；
- 233 行和验收点又写 86 条。

该矛盾会误导迁移核验、测试断言和后续文档维护。应统一为最终经标准源表核验后的数量，并说明“18 个核心实体类型”与“86 个预置节点”的统计口径区别，例如：18 个国标核心类目/类型定义，86 个含标准化子类的完整预置类节点。

---

## P2-02：模块职责声称“管理等价类/不相交类”，但首期只有查询和种子

设计开头把等价类管理、不相交类管理列为模块职责，但 API 没有任何增删接口，前端也仅展示。演进预留又说明后续再补。

**建议：**

- 若首期只读，职责改为“预置与展示”；
- 若属于本期验收，补齐受控关系管理 API、权限、校验和前端入口；
- 不相交/等价关系究竟归实体类型模块还是公理模块，应尽早确定唯一写入口，避免两个模块同时维护同一表。

---

## P2-03：删除规则与“级联清理”设计表述不一致

设计 293 行称删除时清理 hierarchy/equivalent/disjoint 双向关系；实现却在存在子类、等价或不相交引用时直接拒绝，只清理“作为 child 的父类关系”和标签。

实现本身采用“有引用即拒绝”更安全，但文档必须改为：

- 阻断性引用：子类、等价、不相交、属性、实例；
- 可清理的附属数据：标签，以及该类型作为 child 的父类边；
- 是否允许用户选择级联删除关系，应单独设计，不应文档和代码各说一套。

---

## P2-04：对称关系的存储规则不统一

- disjoint 设计为 `type_a < type_b` 单行规范化；
- equivalent 却设计为双向两行；
- 查询 disjoint 同时查两列，查询 equivalent 只查 `entity_type_id`；
- 未来若某入口只插一条等价记录，详情会出现单向可见。

建议统一采用“无向边规范化单行存储”：`least(id1,id2), greatest(id1,id2)`，查询两列；序列化到 OWL 时输出一次即可，因为语义本身对称。若坚持双向两行，必须用 Service 原子写入/删除双行并增加一致性测试。

---

## P2-05：父类列表允许选择自身、后代和重复项

前端 `allTypes` 原样作为父类候选：

- 编辑时包含当前实体；
- 包含其所有后代；
- 多选值理论上可带重复 ID；
- 后端 duplicate parentIds 会触发复合主键冲突，错误信息不友好。

建议前端过滤当前节点和后代，后端仍做最终校验并对 parentIds 去重。

---

## P2-06：树搜索没有实现设计要求的自动展开

设计要求“过滤时自动展开匹配节点的父链”。当前 `filteredTree` 虽保留父链，但 `el-tree` 仍 `default-expand-all=false`，没有维护 `default-expanded-keys` 或调用展开方法。匹配节点位于折叠父节点下时，用户仍可能看不到结果。

建议搜索关键词非空时展开过滤结果中的全部父链，清空关键词后恢复用户原展开状态。

---

## P2-07：树节点子项排序不稳定，并存在未使用变量

Service 先按 `sortOrder/id` 查询 types，但 hierarchy 查询无 order，随后按关系返回顺序向 children 添加节点。`childrenMap` 被构造却从未使用。

建议：

- 删除无用 `childrenMap`；
- 构建完成后按 `sortOrder/id` 显式排序；
- TreeNode 可携带 sortOrder，或 childrenMap 按预先排序的 type 序列构造。

---

## P2-08：`/{id}/properties` 对不存在的实体类型也返回成功空列表

当前接口无论 id 是否存在都返回 `R.ok(List.of())`。这会把“类型不存在”和“类型存在但尚无属性”混为一谈。

建议先校验实体类型存在；首期可返回空列表，但响应语义应准确。后续属性模块上线时移入 Service，避免 Controller 中长期保留占位实现。

---

## P2-09：前端和 API 大量使用 `any`

新增 API 和页面几乎所有 query、form、tree、detail、error 均为 `any`。这违反仓库前端约定中“避免 stray any、类型放到 src/types”的要求，也使以下错误无法在开发期发现：

- detail 字段名和后端 Map key 漂移；
- ID 的 string/number 混用；
- parentIds、labels、namespace 的空值形态不一致；
- 表单误带审计和 delFlag 字段。

建议定义 `EntityType`, `EntityTypeTreeNode`, `EntityTypeDetail`, `EntityTypeForm`, `EntityTypeLabel`, `NamespaceOption` 等接口，并使 API 返回值和表单显式类型化。

---

## P2-10：当前前端构建不包含严格 Vue 类型检查

项目 `pnpm build` 使用 Vite 构建，package 中未配置 `vue-tsc`。Vite 成功只能说明模板和转译可完成，不能替代完整 TypeScript 类型检查。

建议后续引入 `typecheck` 脚本，至少在 CI 对新增 ontology 页面运行；在此之前更应减少 `any`，避免类型系统形同虚设。

---

## P2-11：页面异步状态和快速切换竞态未处理

`loadTree/loadAllTypes/loadNamespaces/handleNodeClick` 没有 loading 与统一错误处理。用户快速点击两个节点时，较早请求可能较晚返回并覆盖当前选择，出现“高亮 A、详情 B”的竞态。

建议：

- 增加树、详情、弹窗各自 loading；
- 用请求序号或取消机制丢弃过期详情响应；
- 首屏使用 `Promise.allSettled` 并显示可恢复错误；
- 刷新树后保持当前节点并验证它仍存在。

---

## P2-12：字段长度和枚举校验不足，数据库错误会泄漏为通用失败

实体类只有 iri/name 非空和 namespace 非空，没有：

- name ≤128；iri ≤512；definition ≤512；remarks ≤255；label ≤128；
- `isAbstract` 只能为 0/1；
- sortOrder 范围；
- locale 合法性；
- IRI 语法和本地名规则一致性。

前端 maxlength 不是安全边界，API 可被直接调用。建议 DTO 与 DB CHECK 双层校验，并把唯一冲突映射为稳定业务错误，避免并发插入时只返回数据库异常。

---

## P2-13：核心 IRI 仍使用 `example.org` 占位域名

V5 已说明核心命名空间“正式发布后由管理机构更新域名”，当前开发阶段可接受；但一旦实体类型、属性、实例开始产生并导出数据，IRI 就成为不可轻易修改的身份。

建议在继续开发数据属性/对象属性/实例模块前明确：

- 正式核心 namespace；
- 标准版本与 IRI 版本策略；
- 占位 IRI 到正式 IRI 的迁移/映射策略；
- 是否允许既有核心 IRI 后续整体变更。

否则越晚修正，数据和引用迁移成本越高。

---

## 六、详细设计专项评审

### 6.1 领域边界

把实体类型主表、标签、继承、等价、不相交放在同一聚合 Service 下是合理的。但“等价/不相交是实体类型元数据，还是公理规则”的职责边界尚未稳定。建议采用以下划分：

- 实体类型模块负责类声明、标签、父子层次和只读公理摘要；
- 公理模块负责 equivalent/disjoint 等逻辑公理的统一写入、验证、推理和序列化；
- 如果首期为了简化由实体类型模块写入，也必须保证未来只有一个领域服务拥有写权限。

### 6.2 元数据完整性

附录 A.1 的 8 项描述已经有表结构映射，但当前实现中：

- Label 新增丢失；
- Properties 永远为空；
- EquivalentClass 被错误使用；
- 父类/子类只有直接关系，没有明确是否展示推理闭包；
- 多语言标签接口只支持写 zh，表模型与 API 能力不一致。

建议设计明确“直接父类/直接子类”与“全部祖先/全部后代”的边界，详情接口字段名可使用 `directParents/directChildren`，避免后续推理功能上线后语义混乱。

### 6.3 抽象类语义

`is_abstract` 是平台治理标记，不是 OWL 原生类修饰符。OWL 开放世界语义中没有直接的“抽象类不可实例化”关键字。后续实例模块必须把该字段转化为应用层/SHACL 约束：禁止用户直接创建 rdf:type 为抽象类的实例，但允许通过子类推理得到其类型。

详细设计目前只写“不可实例化”，应补充执行层：实例保存校验、导入校验和 SHACL/Jena 校验各自如何处理。

### 6.4 多继承与树展示

关系模型能表达 DAG，但前端把 DAG 投影为树时会重复展示多继承节点。该方案可接受，但应补充：

- 节点重复时以实体 ID 识别同一概念；
- 点击任一副本都定位同一详情；
- 拖拽调整父子关系时是“新增/删除一条父边”，不是移动并删除其他父边；
- 防止树组件把同 ID 的重复节点当作 key 冲突。

当前 `node-key="id"` 与“同一 child 在多个父节点下重复出现”存在潜在冲突：Element Plus 树通常假设 node-key 全树唯一。多继承节点重复使用同一 id 时，当前节点、高亮、展开和查找可能定位到任一副本。建议 TreeNode 增加展示路径 key，例如 `pathKey=parentPath + '/' + id`，业务实体 ID 单独保留。

### 6.5 关系闭包与推理边界

当前 Service 只存直接边，这是正确的；不要在 hierarchy 表冗余存储传递闭包。祖先/后代查询应使用递归 CTE 或应用层遍历。OWL 推理闭包与管理页面直接关系要分开：管理页编辑直接父类，推理页展示 inferred types。

---

## 七、后端实现专项评审

### 7.1 Controller

优点：REST 路径统一、权限粒度清晰、写操作有 SysLog。

需改进：

- Query/Create/Update/Response 不应复用 Entity；
- 原始 `R` 返回类型应补泛型；
- `/list` 在 86 条时可用，未来按 ontology/namespace 过滤并限制返回字段；
- 占位 properties 接口应校验 id；
- 对唯一冲突、引用冲突、环检测等建立统一错误码，而不只返回中文字符串。

### 7.2 Service

优点：写操作事务化、内置类型使用白名单更新、删除前检查子类/逻辑公理引用。

需改进：

- 修复新增标签、复合键更新和环检测；
- parentIds 去重并限制规模；
- 把 IRI 生成、命名空间治理、字段标准化集中到领域服务；
- 不要返回 `R` 作为 Service 层领域结果。更推荐 Service 返回对象/抛业务异常，由 Controller/全局异常处理器包装 R，确保事务异常和错误码一致；
- `tree()` 与 `getDetail()` 应返回明确 VO，而非 `Map<String,Object>`；
- 对树构建检测脏环和无根节点，不能静默遗漏。

### 7.3 数据访问

BaseMapper 足够支持首期简单 CRUD，但关系查询开始增多后建议增加明确 Mapper 方法或 XML/注解 SQL：

- recursive CTE 可达性/祖先/后代查询；
- 一次性详情聚合或批量标签查询；
- PostgreSQL upsert 标签；
- namespace 引用计数；
- 规范化无向关系写入。

避免在 Service 内反复拼装容易遗漏方向的 `.eq().or().eq()`。

---

## 八、前端实现专项评审

### 8.1 页面结构

“左树右详情 + 弹窗编辑”符合详细设计，核心/抽象标签、父子类、等价/不相交展示也基本完整。作为首期管理页，布局方向可以保留。

### 8.2 表单与领域规则

应重点调整：

- IRI 由后端权威生成，前端实时预览；
- 新增扩展类型不能默认进入 std；
- 内置类型编辑使用专门表单，只提交 definition/label/sortOrder/remarks；
- 扩展类型编辑过滤自身和后代父类；
- Label 若是必填元数据，应增加前端 required；
- 表单提交构造 DTO，不要直接提交 reactive form 中的所有数据库字段。

### 8.3 DAG 展示

如前述，多继承副本不能使用重复 node-key。建议后端返回：

```ts
interface EntityTypeTreeNode {
  key: string;       // 展示路径唯一
  entityTypeId: number;
  parentPath: number[];
  children: EntityTypeTreeNode[];
}
```

业务操作使用 `entityTypeId`，树状态使用 `key`。

### 8.4 可用性

建议增加：

- 首屏和详情 skeleton/loading；
- 搜索自动展开与“无匹配”状态；
- 复制 IRI；
- 父/子/等价/不相交 tag 可点击跳转，并同步树定位；
- 删除确认中显示名称与受影响关系；
- 保存后选中新建节点；
- 接口错误统一交由 request 层或页面处理，避免重复消息。

---

## 九、优化实施计划

## 阶段 A：解除发布阻断并修正核心语义（必须最先完成）

### A1. 权威词汇核验

- 对 86 个预置节点执行 IRI/name/label/父类/抽象性逐条核验；
- 解决“列项 List”和“列表 List”冲突，连同 Unnumbered/Numbered 子类一起统一命名；
- 明确 18 与 86 的统计口径；
- 确认正式或阶段性核心 namespace。

### A2. 修正 Clause 模型

- 移除 Clause 与 InformationUnit 的 equivalent；
- 增加 Clause 的 InformationUnit 父边，保留 StructuralElement 父边；
- 用 Jena 推理测试证明期望分类；
- 搜索其他把“子类/双重归属”误写成“等价类”的种子。

### A3. 修正 V6

- 由于当前 V6 未成功应用，直接修改 V6；
- 增加外键、CHECK、反向索引；
- 规范化 equivalent/disjoint 存储；
- 添加迁移后断言：86 类型、86 zh 标签、预期层次数、无重复 IRI/name、所有关系端点存在、无自环。

**阶段 A 验收门槛：**原始 V6 能在 V1-V5 的真实 PostgreSQL 库上一次成功迁移，不需要手工改 SQL；核心语义测试通过。

---

## 阶段 B：修复后端关键 CRUD 与领域不变量

1. 新增 Create/Update/Query/Detail DTO/VO；
2. 新增时保存 zh 标签；已有标签用条件 update/upsert；
3. 统一后端生成 IRI，校验 namespace/name/IRI 一致；
4. 限制扩展类型写入核心命名空间；
5. 重写可达性/环检测，超限 fail-closed；
6. parentIds 去重，校验自身/后代/不存在节点；
7. 命名空间删除和 URI 修改增加实体类型引用保护；
8. 扩展更新使用字段白名单，禁止请求修改 delFlag/审计字段；
9. 树构建检测环和无根脏数据，子节点稳定排序；
10. 明确等价/不相交关系唯一写入口。

**阶段 B 验收门槛：**新增、编辑、删除、内置保护、多继承、标签、IRI、命名空间引用全部由自动化集成测试覆盖。

---

## 阶段 C：补齐前端类型与交互

1. 定义完整 TS 类型，移除本模块新增代码中的主要 `any`；
2. 表单只提交 DTO 字段；
3. 新增类型选择扩展命名空间，后端生成 IRI；
4. 父类候选过滤自身和后代；
5. 多继承树使用路径唯一 key；
6. 搜索自动展开父链；
7. 增加 loading、错误态和过期请求保护；
8. 保存后定位节点，详情关系 tag 支持跳转；
9. 增加 `typecheck`，并补至少一条关键页面 E2E。

---

## 阶段 D：解决本体工程作用域与长期演进

在开发数据属性和对象属性前做一次架构决策：

### 方案 1：尽早引入 ontology_id（推荐）

- 建最小本体工程表；
- 预置 CORE 工程；
- entity_type 及后续 Schema 表全部带 ontology_id；
- 唯一约束和查询按工程作用域设计。

### 方案 2：明确单本体阶段

- 文档、API、UI 明确当前只管理一个全局本体；
- 预留固定 core ontology 标识；
- 编写未来回填和唯一索引迁移设计；
- 在多工程上线前禁止产生不可归属的数据。

不建议继续以“以后 ALTER TABLE 再补”为唯一方案，因为数据属性、对象属性、公理、实例都会复制同一作用域缺口。

---

## 阶段 E：建立验收与回归测试矩阵

### 数据库/Flyway

- 空库 V1-V6；
- 现有 V1-V5 升级到 V6；
- 86 条种子唯一性；
- FK/CHECK/索引存在性；
- 非法自环、悬空端点、重复对称关系被拒绝。

### 后端

- 新增自动 IRI + zh 标签；
- IRI/name 冲突和 namespace 不一致；
- 内置字段不可修改/删除；
- 扩展字段白名单；
- 多父类、重复父类、直接环、间接环、深层/宽层图；
- namespace 删除/改 URI 引用保护；
- 删除时的子类、等价、不相交引用阻断；
- 标签新增与更新。

### 本体语义

- Clause 是 StructuralElement 和 InformationUnit 的子类；
- InformationUnit 不等价于 Clause；
- Example/Note 不属于 Clause；
- NormativeElement 与 InformativeElement 不相交；
- 导出 RDF/OWL 后由 Jena 加载和推理，结果符合断言。

### 前端/E2E

- 菜单与权限；
- 树加载、搜索、自动展开；
- 新增、编辑内置、编辑扩展、删除；
- 多继承重复节点定位；
- 接口失败提示和 loading；
- 新增后标签与详情一致。

---

## 十、建议的修复提交拆分

遵循仓库细粒度提交偏好，建议不要把所有修复压成一个大提交：

1. `fix(ontology): 修正实体类型核心词汇与V6迁移`
2. `fix(ontology): 修正Clause多继承语义与关系约束`
3. `fix(ontology): 修复实体类型标签和IRI保存流程`
4. `refactor(ontology): 使用DTO收敛实体类型更新字段`
5. `fix(ontology): 加固继承环检测与命名空间引用保护`
6. `refactor(web): 完善实体类型类型定义和树交互`
7. `test(ontology): 补充迁移领域规则与本体推理测试`
8. `docs(ontology): 同步实体类型详细设计与验收口径`

若阶段 D 选择立即引入 ontology_id，应单独提交并先于数据属性模块开发。

---

## 十一、复评准入清单

以下项目全部完成后再申请复评：

- [ ] 原始 V6 在 V1-V5 PostgreSQL 库上一次执行成功；
- [ ] List/列项/列表词汇冲突按标准源表解决；
- [ ] Clause 使用多继承，不再等价于 InformationUnit；
- [ ] 新增实体类型能保存 zh 标签；
- [ ] IRI 自动生成路径真实可达，且 namespace/name/IRI 一致；
- [ ] 标签更新不再使用复合主键实体的 updateById；
- [ ] FK、CHECK、反向索引和关系规范化规则落地；
- [ ] 命名空间删除/改 URI 有实体类型引用保护；
- [ ] 环检测对深图、宽图、多继承可靠且超限拒绝；
- [ ] Create/Update DTO 阻止 delFlag/审计字段批量赋值；
- [ ] 详细设计统一 18/72/86 口径和等价/不相交职责；
- [ ] 前端父类候选、搜索展开、多继承 node-key 修正；
- [ ] 后端 clean compile、前端 build、ESLint、数据库迁移测试全部通过；
- [ ] 至少具备服务集成测试和 Clause 本体推理测试；
- [ ] 明确 ontology_id 是立即落地还是正式记录为单本体阶段约束。

---

## 十二、最终评语

本次提交已经搭出了实体类型管理的页面、接口和关系表骨架，说明开发方向和模块拆分基本正确；但实体类型是后续数据属性、对象属性、公理、实例和序列化的根基，不能以“页面可编译、CRUD 大致可用”作为验收标准。

当前版本一方面因预置词汇冲突导致 V6 无法执行，另一方面把“多继承”错误表达为“等价类”，会从数据库启动层和本体语义层同时阻断后续开发。再叠加标签静默丢失、IRI 自动生成不可达、复合键更新、命名空间生命周期和环检测问题，说明该提交仍处于“功能原型完成”而非“开发完成待测试”的状态。

**建议状态：退回修改；完成阶段 A、B 和最小自动化测试后复评。**

---

## 附录：评审依据

1. 仓库内《标准本体建模平台PRD.md》§2.4、§4.2、§7.1、§14.1。
2. 仓库内《04-实体类型管理模块详细设计.md》。
3. W3C Recommendation《OWL 2 Web Ontology Language Primer (Second Edition)》：EquivalentClasses 表示两个类包含完全相同的个体，等价于双向 SubClassOf。
4. W3C Recommendation《OWL 2 Web Ontology Language Direct Semantics (Second Edition)》：SubClassOf 的类扩展为子集关系，EquivalentClasses 的类扩展为相等关系。
5. PostgreSQL 18.4 实际事务演练、`pg_constraint`/`pg_indexes` 核验结果。
6. MyBatis-Plus 3.5.16 单主键 `updateById` 模型及本仓库现有编码约定。
