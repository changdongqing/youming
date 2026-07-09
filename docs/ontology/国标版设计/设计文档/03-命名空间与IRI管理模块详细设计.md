# 命名空间与IRI管理模块详细设计

> 对应 PRD：§4.1 命名空间与IRI管理模块、§3.3 标准化治理策略、§7.1 数据库设计、§13 术语映射表
> 编写日期：2026-07-09

## 1. 领域定位

命名空间是本体建模的基石。国标第 5.4 条规定：XML 命名空间 + 本地标识符构成 IRI，实体类型与属性均具有全球唯一 IRI。PRD §3.1 架构图将命名空间与 IRI 管理定位为"贯穿全模块"的底层基础设施，所有 Schema 模块（实体类型、数据属性、对象属性、扩展单位等）均通过 `namespace_id` 引用命名空间。

本模块职责：

1. **命名空间注册**：维护核心命名空间 `http://example.org/standard-ontology#` 及扩展命名空间，区分核心（CORE）与扩展（EXTENSION）。
2. **IRI 自动生成**：IRI = 命名空间 URI + 本地标识符，按命名规范（实体类型首字母大写、属性首字母小写）自动拼接。
3. **IRI 唯一性校验**：全局校验 IRI 不重复，为后续实体类型、数据属性、对象属性、实例的 IRI 分配提供前置保障。
4. **前缀映射**：维护 prefix→namespace 映射（如 `std:`、`xsd:`、`rdf:`、`rdfs:`、`owl:`），用于 Turtle/JSON-LD 序列化时生成 `@prefix` 声明。
5. **核心/扩展隔离**：核心命名空间只读，扩展命名空间可增删改，落实 PRD §3.3"核心 Schema 锁定"与"扩展命名空间强制"治理策略。

模块依赖链定位：

```text
namespace（命名空间/IRI）  ← 全模块地基
   ↑
entitytype（实体类型）      ← 被数据属性/对象属性/实例依赖
   ↑
dataproperty / objectproperty
   ↑
axiom → instance → validation / serialization
```

本模块处于依赖链最底层，是后续每一个 Schema 模块引用的前提。

## 2. 数据模型

### 2.1 命名空间表 `ont_namespace`

> 字段风格对齐 youming 实际落地规范（参考 V4 `ont_unit` 的 char(1) 布尔、ASSIGN_ID 主键、审计字段），不采用 PRD §7.1 的原生 PG 风格（BIGSERIAL/BOOLEAN）。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | bigint | 是 | MyBatis-Plus `ASSIGN_ID`（雪花算法） |
| `prefix` | varchar(64) | 是 | 前缀标识，全局唯一，如 `std`、`rdf`、`ext_medical` |
| `uri` | varchar(512) | 是 | 命名空间 URI，全局唯一，如 `http://example.org/standard-ontology#` |
| `is_default` | char(1) | 是 | 是否默认命名空间，`1` 是 `0` 否；全局仅一条为 `1` |
| `is_builtin` | char(1) | 是 | 是否内置（核心），`1` 核心（不可删） `0` 扩展 |
| `sort_order` | integer | 是 | 排序值 |
| `description` | varchar(255) | 否 | 描述/备注 |
| `create_by` | varchar(64) | 否 | 创建人 |
| `create_time` | timestamp | 是 | 创建时间 |
| `update_by` | varchar(64) | 否 | 修改人 |
| `update_time` | timestamp | 否 | 更新时间 |
| `del_flag` | char(1) | 是 | 逻辑删除标志 |

> **scope 语义说明**：PRD §4.1.2 数据模型使用 `scope: Enum{CORE, EXTENSION}`。本设计将其落地为 `is_builtin char(1)`，与已上线的 `ont_unit.is_builtin` 字段风格统一：`'1'`=核心（CORE）、`'0'`=扩展（EXTENSION）。语义等价，避免引入枚举类型增加复杂度。

约束：

1. `prefix` 在未删除数据中唯一。
2. `uri` 在未删除数据中唯一。
3. `is_default='1'` 的记录全局唯一（核心本体命名空间为默认）。
4. 内置（核心）命名空间不可删除。
5. 内置命名空间语义字段锁定：`prefix`、`uri`、`is_default`、`is_builtin` 不允许通过接口改变；允许修改 `description`、`sort_order`。
6. 扩展命名空间删除前必须确认无引用（首期检查 `ont_unit.namespace_id`，后续扩展到实体类型、数据属性、对象属性等）。

### 2.2 单位字典表 `ont_unit` 新增列（同步迁移）

| 新增字段 | 类型 | 必填 | 说明 |
|----------|------|------|------|
| `namespace_id` | bigint | 否 | 外键，指向 `ont_namespace.id`；内置单位为 NULL，扩展单位必填 |

> 旧 `namespace varchar(255)` 列保留不删除，标记为 deprecated（过渡双写期）。新逻辑优先使用 `namespace_id`，旧列在全部引用方迁移完毕后于后续版本脚本中删除。详见 §5 迁移方案。

## 3. 预置数据

首期按"核心本体命名空间 + 常用 W3C 前缀"预置 5 条：

| id | prefix | uri | is_default | is_builtin | 说明 |
|----|--------|-----|------------|------------|------|
| 930001 | `std` | `http://example.org/standard-ontology#` | `1` | `1` | 核心本体命名空间（默认） |
| 930002 | `rdf` | `http://www.w3.org/1999/02/22-rdf-syntax-ns#` | `0` | `1` | W3C RDF 前缀 |
| 930003 | `rdfs` | `http://www.w3.org/2000/01/rdf-schema#` | `0` | `1` | W3C RDFS 前缀 |
| 930004 | `owl` | `http://www.w3.org/2002/07/owl#` | `0` | `1` | W3C OWL 前缀 |
| 930005 | `xsd` | `http://www.w3.org/2001/XMLSchema#` | `0` | `1` | W3C XML Schema 前缀 |

说明：

1. `std` 为核心本体命名空间，`is_default='1'`，后续实体类型/属性默认归属此命名空间。URI 域名 `example.org` 为占位，正式发布后由管理机构通过命名空间切换更新。
2. `rdf`/`rdfs`/`owl`/`xsd` 为 W3C 标准前缀，用于序列化时生成 `@prefix` 声明，不可删除、不可修改 URI。
3. 其余可能被行业扩展引用的前缀（如 `skos`、`dct`、`foaf`）不预置，后续按需由管理员通过扩展命名空间新增（`is_builtin='0'`）。

## 4. 服务规则

### 4.1 新增命名空间

校验流程：

1. `prefix`、`uri` 必填。
2. `prefix` 格式：小写英文/数字/下划线/中划线，正则 `^[a-z][a-z0-9_-]*$`，如 `ext_elevator`。
3. `prefix` 不重复（未删除数据中）。
4. `uri` 格式：必须以 `#` 或 `/` 结尾（IRI 拼接规范要求命名空间本身是合法前缀）。
5. `uri` 不重复（未删除数据中）。
6. 新增数据默认 `is_builtin='0'`、`is_default='0'`（接口只能创建扩展命名空间，核心命名空间由种子数据预置）。

### 4.2 修改命名空间

1. 若命名空间不存在，返回失败。
2. 若为内置命名空间，只更新 `description`、`sort_order`。
3. 若为扩展命名空间，可更新 `prefix`、`uri`、`description`、`sort_order`。
4. 修改 `prefix` 或 `uri` 时重新检查唯一性。
5. 不允许通过接口修改 `is_builtin`、`is_default`（默认命名空间的设定由初始化脚本保证）。

### 4.3 删除命名空间

1. 内置命名空间禁止删除。
2. 删除前检查被引用情况：首期检查 `ont_unit.namespace_id` 是否存在引用该命名空间的扩展单位。后续实体类型/数据属性/对象属性模块上线后追加相应引用检查。
3. 存在引用时返回失败，提示被引用记录数量。
4. 通过 MyBatis-Plus 逻辑删除更新 `del_flag`。

### 4.4 IRI 生成

`POST /admin/ontology/iris/generate`

入参：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `namespaceId` | Long | 是 | 命名空间 ID |
| `localName` | String | 是 | 本地标识符 |
| `elementType` | String | 否 | 元素类型提示：`ENTITY_TYPE`（首字母大写）/ `PROPERTY`（首字母小写），用于命名规范校验 |

出参：完整 IRI 字符串，如 `http://example.org/standard-ontology#Standard`。

规则：

1. 校验 `namespaceId` 对应的命名空间存在且未删除。
2. `localName` 非空，格式为驼峰：首字母按 `elementType` 决定大小写；允许字母/数字/下划线，正则 `^[a-zA-Z][a-zA-Z0-9_]*$`。
3. IRI = `namespace.uri` + `localName`。
4. 生成后可调用 IRI 唯一性校验确认不重复（生成接口本身不做阻断性校验，只做格式拼接，唯一性由调用方按需校验）。

### 4.5 IRI 唯一性校验

`GET /admin/ontology/iris/validate?iri=`

入参：`iri`（完整 IRI 字符串）。

出参：`{ "unique": true/false, "message": "..." }`。

规则：

1. 首期在 `ont_namespace` 表范围内校验 URI 本身是否重复（命名空间层面的唯一性）。
2. 后续实体类型/数据属性/对象属性/实例模块上线后，扩展为全局 IRI 唯一性校验：检查所有已注册的实体类型 IRI、属性 IRI、实例 IRI 是否与入参 IRI 冲突。
3. 首期返回 `unique=true` 即可（命名空间层面不冲突），完整全局校验随后续模块逐步补齐。

## 5. ont_unit 命名空间字段同步迁移方案

> 用户决策：本模块内同步迁移。V4 演进预留第 4 条标注的迁移点在本模块完成。

### 5.1 数据库迁移（V5 脚本）

1. **建表**：`CREATE TABLE ont_namespace`（字段见 §2.1）。
2. **预置**：插入 5 条核心命名空间（§3）。
3. **加列**：`ALTER TABLE ont_unit ADD COLUMN namespace_id bigint`（可空）。
4. **加外键**：`ALTER TABLE ont_unit ADD CONSTRAINT fk_ont_unit_namespace FOREIGN KEY (namespace_id) REFERENCES ont_namespace(id)`。
5. **加索引**：`CREATE INDEX idx_ont_unit_namespace_id ON ont_unit (namespace_id) WHERE del_flag = '0'`。
6. **回填**：V4 预置的 20 条内置单位 `namespace` 字段均为 NULL（`is_builtin='1'`），回填逻辑为 `namespace_id` 保持 NULL。内置单位属于核心命名空间口径，但首期不强制绑定 `namespace_id`（内置单位的命名空间治理由平台保证，无需外键约束）。
7. **保留旧列**：`ont_unit.namespace varchar(255)` 列不删除、不修改，作为 deprecated 过渡字段。新逻辑读写 `namespace_id`，旧列仅保留历史数据可读性。

### 5.2 后端改造

| 文件 | 改造内容 |
|------|----------|
| `OntUnit.java` | 新增 `namespaceId` 字段（Long 类型，`@Schema(description="命名空间ID")`），保留旧 `namespace` 字段（标注 `@Deprecated`） |
| `OntUnitServiceImpl.java` | `validateUnit` 方法中扩展单位校验：`is_builtin='0'` 时必须填写 `namespaceId` 且引用的 `ont_namespace` 记录存在且未删除（替代旧校验"扩展单位必须填写 namespace 字符串"）；内置单位 `namespaceId` 允许为空 |
| 新增 `OntNamespace.java` | 命名空间实体，继承 `Model<OntNamespace>`，`@TableName("ont_namespace")`，字段见 §2.1 |
| 新增 `OntNamespaceMapper.java` | `@Mapper interface extends BaseMapper<OntNamespace>` |
| 新增 `OntNamespaceService.java` | 接口 `extends IService<OntNamespace>`，自定义 `saveNamespace/updateNamespace/removeNamespace` |
| 新增 `OntNamespaceServiceImpl.java` | `extends ServiceImpl<OntNamespaceMapper, OntNamespace>`，校验逻辑见 §4，构造注入 `OntUnitMapper` 检查被引用 |
| 新增 `OntNamespaceController.java` | REST 端点见 §6，权限码 `ontology_namespace_*` |
| 新增 `OntIriController.java` | IRI 生成与校验端点见 §6 |

### 5.3 前端改造

| 文件 | 改造内容 |
|------|----------|
| `web/src/api/ontology/namespace.ts` | 新增命名空间 API 函数（`fetchNamespacePage`/`fetchNamespaceList`/`addNamespaceObj`/`putNamespaceObj`/`delNamespaceObj`/`validateIri`/`generateIri`） |
| `web/src/views/ontology/namespace/index.vue` | 新增命名空间管理页面（布局见 §7） |
| `web/src/views/ontology/unit/index.vue` | 单位表单中"命名空间"项从 `el-input` 文本输入改为 `el-select` 下拉选择器，options 来自 `fetchNamespaceList()`；内置单位编辑时该项置灰；扩展单位必选 |

## 6. API 设计

### 6.1 命名空间 CRUD

| 方法 | 单体实际路径 | 说明 | 权限 |
|------|--------------|------|------|
| GET | `/admin/ontology/namespaces` | 命名空间分页 | `ontology_namespace_view` |
| GET | `/admin/ontology/namespaces/list` | 命名空间列表（不分页，供下拉引用） | `ontology_namespace_view` |
| GET | `/admin/ontology/namespaces/{id}` | 命名空间详情 | `ontology_namespace_view` |
| POST | `/admin/ontology/namespaces` | 新增扩展命名空间 | `ontology_namespace_add` |
| PUT | `/admin/ontology/namespaces` | 修改命名空间 | `ontology_namespace_edit` |
| DELETE | `/admin/ontology/namespaces/{id}` | 删除扩展命名空间 | `ontology_namespace_del` |

### 6.2 IRI 工具

| 方法 | 单体实际路径 | 说明 | 权限 |
|------|--------------|------|------|
| GET | `/admin/ontology/iris/validate` | IRI 唯一性校验，query 参数 `iri` | `ontology_namespace_view` |
| POST | `/admin/ontology/iris/generate` | IRI 生成，body 参数 `namespaceId`/`localName`/`elementType` | `ontology_namespace_view` |

> IRI 校验与生成复用 `ontology_namespace_view` 权限，不单独定义权限码。

### 6.3 菜单与权限种子

| menu_id | name | permission | path | parent_id | menu_type | 说明 |
|---------|------|-----------|------|-----------|-----------|------|
| 900200 | 命名空间管理 | NULL | `/ontology/namespace/index` | 900000 | `0`(菜单) | 复用"本体建模"顶级目录 |
| 900201 | 命名空间查看 | `ontology_namespace_view` | NULL | 900200 | `1`(按钮) | |
| 900202 | 命名空间新增 | `ontology_namespace_add` | NULL | 900200 | `1`(按钮) | |
| 900203 | 命名空间修改 | `ontology_namespace_edit` | NULL | 900200 | `1`(按钮) | |
| 900204 | 命名空间删除 | `ontology_namespace_del` | NULL | 900200 | `1`(按钮) | |

`sys_role_menu` 为 `role_id=1`（管理员）授权 900200-900204。所有 INSERT 带 `ON CONFLICT DO NOTHING` 幂等。

> menu_id 编号约定：900000(顶级目录，V4 已建) → 900100(单位字典，V4) → 900200(命名空间，V5)。百位区分模块，个位区分操作按钮。

## 7. 前端交互设计

### 7.1 命名空间管理页面结构

命名空间是扁平结构无层级关系，采用常规 CRUD 列表布局（不用左树右表）：

```text
命名空间管理页面
├── 查询表单
│   ├── 前缀（prefix）输入框
│   ├── URI 输入框
│   └── 核心/扩展（scope）下拉筛选
├── 操作栏
│   ├── 新增按钮（v-auth="ontology_namespace_add"）
│   └── 刷新按钮
├── 命名空间表格
│   ├── 前缀（prefix）
│   ├── URI
│   ├── 默认（is_default，el-tag）
│   ├── 核心/扩展（is_builtin，el-tag：核心→蓝色"核心"，扩展→黄色"扩展"）
│   ├── 描述（description）
│   ├── 排序（sort_order）
│   └── 操作（编辑/删除，内置删除禁用）
└── 分页组件
```

### 7.2 新增/编辑弹窗

表单字段（label-width 100px）：

| 字段 | 控件 | 备注 |
|------|------|------|
| 前缀 (prefix) | el-input | 内置编辑时 disabled；正则校验 `^[a-z][a-z0-9_-]*$` |
| URI (uri) | el-input | 内置编辑时 disabled；校验须以 `#` 或 `/` 结尾 |
| 描述 (description) | el-input textarea | maxlength 255 |
| 排序 (sort_order) | el-input-number | min 0 |

> `is_builtin`、`is_default` 不在表单中展示（由系统控制，新增时默认 `'0'`）。内置命名空间编辑时仅 description 和 sort_order 可编辑。

### 7.3 内置保护交互

1. 核心命名空间显示蓝色"核心"标签。
2. 删除按钮对核心命名空间禁用（`:disabled="row.isBuiltin === '1'"`），加 tooltip 提示"核心命名空间不可删除"。
3. 编辑核心命名空间时，`prefix`、`uri` 置灰 disabled。

### 7.4 IRI 工具区

页面顶部操作栏提供两个辅助工具入口（按钮触发弹窗/抽屉）：

1. **IRI 生成器**：选择命名空间（下拉）→ 输入本地标识符 → 选择元素类型（实体类型/属性）→ 显示生成的完整 IRI，可复制。
2. **IRI 校验器**：输入完整 IRI → 返回唯一性结果（是否重复）。

> 首期 IRI 工具为辅助功能，不强制阻断建模流程。后续实体类型模块上线后，IRI 生成将内嵌到实体类型/属性新增表单中，此页面工具作为独立调试入口保留。

### 7.5 单位字典页面联动改造

`web/src/views/ontology/unit/index.vue` 中单位表单的"命名空间"项改造：

1. 控件从 `el-input` 改为 `el-select`，options 来自 `fetchNamespaceList()`（页面 onMounted 时加载）。
2. 新增单位时默认选中 `std`（核心命名空间）。
3. 内置单位编辑时该项 disabled。
4. 扩展单位必选（校验规则 `namespaceId` required when `isBuiltin !== '1'`）。
5. 表格列"命名空间"显示对应命名空间的 `prefix`（通过 `namespaceId` 映射），替代旧的 `namespace` 字符串列。

## 8. 后端包结构

命名空间模块代码纳入已有的 `pig-ontology-biz` 模块（不新建独立 Maven 模块），包路径：

```text
com.pig4cloud.pig.ontology
├── controller/
│   ├── OntNamespaceController.java      # 新增
│   └── OntIriController.java           # 新增
├── entity/
│   └── OntNamespace.java               # 新增
├── mapper/
│   └── OntNamespaceMapper.java         # 新增
├── service/
│   ├── OntNamespaceService.java        # 新增
│   └── impl/
│       └── OntNamespaceServiceImpl.java # 新增
└── vo/                                  # 本模块无特殊 VO，列表/分页直接用实体
```

设计理由：命名空间与单位字典同属 ontology 领域，共用 `pig-ontology-biz` 模块可避免模块碎片化。后续实体类型、数据属性等模块同样在此模块内新增包/类即可。

## 9. 演进预留

1. **后续 Schema 模块引用**：实体类型（`ont_entity_type.namespace_id`）、数据属性（`ont_data_property.namespace_id`）、对象属性（`ont_object_property.namespace_id`）均通过外键引用 `ont_namespace.id`，实现"扩展命名空间强制"策略（PRD §3.3）。
2. **ont_unit 旧列清理**：`ont_unit.namespace varchar(255)` 旧列在全部引用方迁移完毕后，于后续版本脚本中删除（`ALTER TABLE ont_unit DROP COLUMN namespace`）。
3. **命名空间切换**：正式发布后域名变更，预留全局 URI 替换接口（`POST /admin/ontology/namespaces/{id}/relocate`），批量更新该命名空间下所有引用方的 IRI 前缀。首期不实现，预留接口设计。
4. **全局 IRI 唯一性校验**：IRI 校验接口随实体类型/属性/实例模块上线逐步补齐全局校验范围（§4.5）。
5. **扩展命名空间被引用后锁定**：扩展命名空间一旦被实体类型/属性/单位引用，其 `prefix`/`uri` 的修改需追加引用检查，避免破坏引用一致性。首期仅检查 `ont_unit.namespace_id`。

## 10. 验收点

1. Flyway V5 能创建 `ont_namespace` 表、预置 5 条核心命名空间、注册菜单与按钮权限、给 `ont_unit` 表新增 `namespace_id` 列与外键。
2. 后端 Maven 编译通过（`pig-ontology-biz` 新增类无编译错误）。
3. 前端 TypeScript/Vite 构建通过（新增 `namespace/index.vue` 与 `namespace.ts`，改造 `unit/index.vue`）。
4. 登录管理员后可在"本体建模 / 命名空间管理"看到页面，预置 5 条核心命名空间可见。
5. 可新增扩展命名空间（prefix/uri 唯一性校验生效），核心命名空间不可删除、不可改 prefix/uri。
6. 单位字典页面新增/编辑扩展单位时，命名空间字段为下拉选择器（来自命名空间列表），必选校验生效。
7. IRI 生成接口可拼接出合法 IRI，IRI 校验接口可返回唯一性结果。
