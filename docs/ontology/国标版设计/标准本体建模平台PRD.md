# 标准本体建模平台 产品需求文档（PRD）

> 依据标准：GB/T 48000.3—2026《标准数字化 第3部分：本体建模要求》
>
> 文档版本：v1.3　　编制日期：2026-07-09
> v1.1 变更：依据第6.2.5.6条+附录C表C.41+附录D步骤5，新增单位字典管理模块（§4.11），并在数据属性值域、公理规则、数据库、前端、里程碑、术语映射等处同步增加配置。
> v1.2 变更：澄清§1.3推理边界（轻量公理检测内置/大规模推理预留扩展），新增§4.7.4推理引擎适配层（ReasonerAdapter SPI），澄清§3.2.2/§7.2三元组存储选型（GraphDB vs Neo4j赛道差异、信创替代路径），消除§1.3与§4.7的推理表述矛盾。
> v1.3 变更：按本次评审补齐§6.2.5.1“术语”实体类型与术语名称/术语定义属性；修正 `measurementUnit` 为实例层单位字典引用而非数据属性定义层 `unitId`；统一国标日期 `YYYYMMDD` 与 `xsd:date` 序列化边界；修正附录D验收流程中 `standardizes`、`describesAction`、`hasClause` 等对象属性方向；新增§0.4评审结论、§3.3标准化治理策略、§6.3关键交互规范。

---

## 0. 文档说明

### 0.1 编写目的

本产品需求文档（PRD）基于 GB/T 48000.3—2026 的全部建模要求，设计一套"标准本体建模平台"。平台以**功能充分解耦**为核心设计原则，将实体类型、数据属性、对象属性、本体公理与规则、实体对象实例等建模要素拆分为独立可配置、可组合、可标准化的模块，使任意行业均可基于本平台完成符合国标的本体建模，并最终实现"附录D 实例化示例"的配置化生成。

### 0.2 名词约定

| 缩写 | 全称 | 说明 |
|------|------|------|
| 本体 | Ontology | 表示实体类型及关系、属性及关联的模型 |
| 实体类型 | Entity Type | 一组具有相同属性实体集合的抽象（类/Class） |
| 实体对象 | Entity Instance | 实体类型的具体实例（Individual/Instance） |
| 数据属性 | Data Property | 描述实体类型内在特征（DatatypeProperty） |
| 对象属性 | Object Property | 描述实体类型间关系（ObjectProperty） |
| 公理 | Axiom | 声明实体类型、属性、实例或关系的不可违反语句 |
| IRI | 国际化资源标识符 | 实体类型/属性的全局唯一标识 |
| SHACL | 结构性约束语言 | 用于约束验证 |

### 0.3 读者对象

产品经理、前端/后端研发工程师、本体建模工程师、标准数字化实施人员、测试工程师。

### 0.4 本次评审结论与优化原则（v1.3）

本次评审以 GB/T 48000.3—2026 条款完整性、平台可实施性、交互可控性、数据治理可持续性为准绳。总体结论：原 PRD 的模块解耦、单位字典扩展、推理边界与三元组存储设计方向正确，但仍存在若干会影响“按国标标准化建模、减少随意录入”的产品风险，已在本文直接修订。

| 评审项 | 发现的问题 | 优化结论 | PRD落点 |
|--------|------------|----------|---------|
| 国标实体完整性 | 原文遗漏第6.2.5.1“术语”实体类型，导致对象属性 `defines`、`usesTerm` 的值域无承载实体 | 补齐 `Term` 实体类型，并将“术语名称、术语定义”作为条款直采核心属性预置；附录C 47项仍按国标附录C管理 | §2.4、§4.2.3、§4.3.3 |
| 单位字典建模 | 原文将 `measurementUnit` 解释为 `DataProperty.RangeSpec.unitId`，容易把“属性定义的单位”和“实例约束的单位值”混淆 | 改为实例层 `UnitRef`：`maxValue/minValue/thresholdRange` 等数值约束引用单位分类，`measurementUnit` 的实例值引用单位字典，序列化时输出单位符号字面量 | §4.3.2、§4.11、§7.1 |
| 日期标准化 | 国标附录A.3要求日期为 GB/T 7408.1 的 `YYYYMMDD`，原文仅写 `YYYY-MM-DD` | UI层用日期控件避免手输；平台规范值保存为 `YYYYMMDD`，语义导出为 RDF `xsd:date` 时转换为 `YYYY-MM-DD` | §4.3.2、§4.3.3、§6.3 |
| 附录D关系方向 | `standardizes`、`describesAction`、`hasClause/hasSubClause` 的部分描述方向与国标表1不一致 | 按表1统一：标准实体 `standardizes` 标准化对象；信息单元/条款 `describesAction` 行动；结构层次到条款使用 `hasClause`，条到子条使用 `hasSubClause` | §5.1、§5.2 |
| 减少随意录入 | 原文强调 CRUD，但缺少“核心只读、扩展审批、模板化表单、字典引用、校验前置”等交互管控 | 新增标准化治理策略与关键交互规范：核心Schema锁定、扩展命名空间强制、枚举/单位/实体引用下拉、向导式实例化、发布前阻断校验 | §3.3、§6.3、§10 |
| 原型可落地性 | 页面设计较概念化，缺少菜单布局、抽屉/弹窗/批量操作/校验报告等交互细节 | v1.3配套在 `yuanxing/index.html` 输出 Ant Design Pro 风格高保真交互原型 | §6 |

> 说明：用户任务中提到“GBT+49000.3-2026.pdf”，当前目录实际文件名为 `GBT+48000.3-2026.pdf`，且 PRD 引用标准为 GB/T 48000.3—2026；本文按目录实际标准文件与 PRD 依据继续评审。

---

## 1. 背景与目标

### 1.1 背景

GB/T 48000.3—2026 规定了标准数字化活动中标准本体构建的通用要求、实体类型定义、实体类型属性、本体公理与扩展方式。当前各类主体开展标准数字化时面临：

- 语义理解不一致、知识整合困难；
- 缺少统一的概念体系与工具支撑；
- 异构标准本体之间整合与映射机制难以构建；
- 标准信息在语义层次的互操作性不足。

手工编写 OWL/RDF 本体门槛高、易出错、难复用、难校验，亟需一款**配置驱动、模块解耦、标准化输出**的本体建模平台。

### 1.2 产品目标

| 目标 | 说明 |
|------|------|
| **标准化** | 严格遵循 GB/T 48000.3—2026 全部建模要求，内置国标核心实体类型、数据属性、对象属性、公理规则 |
| **解耦化** | 实体类型、数据属性、对象属性、公理规则、实体实例五大模块独立管理、松耦合组合 |
| **配置化** | 通过表单/可视化界面配置即可完成本体建模，无需手写 OWL 代码 |
| **可扩展** | 支持按行业、按领域模块化扩展，遵循第9章扩展原则（新命名空间、不修改核心定义） |
| **可校验** | 内置 SHACL 约束验证，确保本体逻辑一致性、完整性、准确性 |
| **可交换** | 支持 Turtle、JSON-LD、RDF/XML 等标准化序列化格式导入导出 |
| **可复现** | 能够完整复现附录D 实例化示例（GB/T 31486—2024 动力蓄电池标准） |

### 1.3 非目标（本期不做）

- 不做标准的文本自动解析抽取（属上游工具职责）；
- 不做大规模知识图谱推理引擎集成；
- 不做协同制定流程管理（属 GB/T 48000.4 范畴）。

> **关于"推理"的边界澄清**：本期不做大规模图谱推理，但**国标第5.3条要求的OWL公理一致性检测属于轻量推理**（如`owl:disjointWith`不相交冲突、`owl:FunctionalProperty`唯一性校验），平台内置 Jena Reasoner 承接此能力（§4.7）。两者边界如下：

| 推理类型 | 本期是否做 | 承载方式 | 说明 |
|---------|-----------|---------|------|
| OWL公理一致性检测（国标5.3要求） | ✅ 做 | Jena Reasoner（内置） | 不相交、功能属性、传递性等公理级推理，国标硬性要求 |
| SHACL约束校验（国标5.3要求） | ✅ 做 | TopBraid SHACL API（内置） | 基于图的约束验证，非推理但依赖图引擎 |
| 大规模图谱推理（传递闭包、规则链） | ❌ 不做 | — | 本期不做，但**预留推理引擎适配层扩展点**（§4.7.4） |

> **扩展预留**：校验引擎模块（§4.7）通过 `ReasonerAdapter` 适配层解耦推理引擎实现，本期接入 Jena Reasoner；未来行业扩展若需深度推理（如医疗本体传递性推理、石油本体规则链），可通过实现 `ReasonerAdapter` 接口接入 GraphDB 推理机或国产RDF库，无需改动上层校验逻辑。

---

## 2. 国标核心要求分析（设计依据）

> 本节是平台设计的源头依据，所有功能模块均映射至国标条款。

### 2.1 本体核心组成（第5.2条）

标准本体应包括三类内容：

```
本体
├── 实体类型（Entity Type）：相同属性实体集合的抽象
├── 属性（Property）
│   ├── 数据属性（Data Property）：描述实体类型内在特征
│   └── 对象属性（Object Property）：描述实体类型间关系
└── 公理（Axiom）：定义实体类型与属性的逻辑约束
```

**→ 平台顶层五大模块由此确定**：实体类型管理、数据属性管理、对象属性管理、公理规则管理、实体对象实例管理。

### 2.2 形式化要求（第5.3条）

| 要求 | 平台实现 |
|------|----------|
| 采用 W3C 本体描述语言（XML、RDF/RDFS、OWL） | 内置 OWL/RDFS 生成引擎 |
| 标准化序列化格式（Turtle、JSON-LD 等） | 支持多格式导入导出 |
| 支持 SHACL 约束验证 | 内置 SHACL 校验引擎 |

### 2.3 命名方式（第5.4条）

- XML 命名空间：`http://example.org/standard-ontology#`（正式发布后由管理机构确定）
- IRI = XML 命名空间 + 本地标识符
- 实体类型/属性均具有全球唯一 IRI

**→ 平台内置命名空间管理与 IRI 自动生成机制。**

### 2.4 实体类型体系（第6章 + 附录B）

国标定义 **18 个核心实体类型**，分6大组：

| 组别 | 实体类型 | 子类 |
|------|----------|------|
| **标准实体** | 标准实体 | 术语标准、符号标准、分类标准、试验标准、规范标准、规程标准、指南标准、评价标准 |
| **元数据** | 标准化对象 | — |
| | 相关方（抽象类） | 机构、个人 |
| | 机构 | 发布机构、提出单位、归口单位、组织实施单位、起草单位、建议方、征求意见单位、出版机构 |
| | 个人 | 起草人、评审人 |
| | 领域类别 | 国际标准分类(ICS)、中国标准文献分类(CCS) |
| **结构** | 要素 | 规范性要素（范围、术语和定义、符号和缩略语、核心技术要素、其他技术要素）、资料性要素（规范性引用文件、参考文献、索引） |
| | 层次 | 章、条（有标题条/无标题条）、段、列项（无编号/有编号） |
| **技术内容** | 术语 | — |
| | 信息单元（抽象） | 条款、附加信息（示例、注、脚注、清单、列表） |
| | 信息单元表述形式 | 条文、图、表、数学公式、附录、引用、提示 |
| | 对象 | 标准对象、指标对象 |
| | 特性 | — |
| | 约束逻辑 | — |
| | 行动 | 动作、路径、判断条件 |
| | 外部约束 | 法规、专利、文献、公共数据库 |
| **制定程序** | 制定程序 | 预备、立项、起草、征求意见、技术审查、批准发布、出版、复审、废止 |
| **扩展** | 版本（可选）、文件编号类（可选） | 按需 |

### 2.5 数据属性体系（第7.2条 + 附录C）

国标在**附录C**中按 12 个实体类型类定义了 47 个数据属性，每个数据属性含7项元数据描述（附录A.2）：标识符(IRI)、名称(Name)、标签(Label)、定义(Definition)、定义域(Domain)、值域(Range)、属性类型(Type)。此外，第6.2.5.1明确要求“术语”类包括“术语名称、术语定义”等数据属性；平台将其作为**条款直采核心属性**预置，不改变“附录C 47项”的统计口径。

值域支持 5 种基本数据类型（附录A.3）：布尔型、日期(国标规范值 `YYYYMMDD`)、数值型、文本型、统一资源链接。多个数据属性值为枚举类型（如标准状态、约束类型、条款类型等）。当导出 RDF/OWL 且目标类型为 `xsd:date` 时，平台将 `YYYYMMDD` 转换为 `YYYY-MM-DD` 的 XML Schema 词法形式。

**约束逻辑类的“测量单位”属性（第6.2.5.6条 + 附录C表C.41）需特别处理**：国标将其定义为约束逻辑类的数据属性之一（附录C表C.41标识符/名称为 `#unit`/`unit`，定义域=`Constraint`，值域=`xsd:string`），并在表C.41备注“值域为枚举类型：V、W、℃ **等**”；附录D Turtle 示例又实际使用 `measurementUnit` 谓词且取值 `%`。因此平台需要同时记录**标准原始标识 `unit`**与**示例/业务别名 `measurementUnit`**，UI/API默认展示 `measurementUnit`，导入导出支持二者映射，以兼容国标正文与附录示例之间的命名差异。其中“等”表明枚举**开放**，附录D步骤5实际使用 `%`（不在举例之列）即为例证。此外，附录C表C.38“最大值”、表C.39“最小值”、表C.40“阈值范围”的取值均内嵌单位（如最大值“100W”、阈值范围“80~100W”）。

> **→ 平台设计影响**：测量单位不能作为普通文本枚举内嵌于数据属性值域，须独立为**单位字典**（物理量分类 + 单位符号 + 可选换算参数）统一治理，供约束逻辑类实例及任意数值型数据属性的单位分类约束引用。详见 §4.11。

### 2.6 对象属性体系（第7.3条 + 表1）

国标定义 **34 个核心对象属性**，每个含：中文名、英文名、定义域(Domain)、值域(Range)、语义说明。覆盖标准间关系（采用、代替、引用、参考、有部分）、标准与相关方（发布于、提出于、归口于、起草于、出版于）、标准与结构（包含要素、包含层次、包含条款、包含子条）、技术内容关联（界定、提及术语、具有表述形式、涉及对象、规定特性、有特性、施加约束、约束对象、约束特性、描述行动）、外部资源（引用外部资源、与专利有关）、生命周期（处于阶段、包含标准）等。

### 2.7 公理与规则（第8章）

国标要求定义三大类核心规则：

| 规则类别 | 具体规则 |
|----------|----------|
| **实体类型规则** | ①实体类型不相交规则（如规范性要素与资料性要素互斥）；②全局唯一标识规则（如信息单元需全局唯一标识符） |
| **属性规则** | ①属性唯一性约束（如标准编号唯一）；②日期有效性验证（实施日期≥发布日期）；③枚举值约束（标准状态取值限定）；④取值约束（约束类型限强制/推荐）；⑤**单位一致性约束**（约束逻辑的 maxValue/minValue/thresholdRange 与 measurementUnit 引用的单位字典条目须量纲一致，附录C表C.38~C.41联动） |
| **关系规则** | ①功能性属性约束（每个标准只能由一个机构发布）；②版本替代关系（废止标准指向替代标准或标明废止日期）；③层次结构约束（章可包含零或多个条；无标题条不可含子条）；④引用关系区分（标准间引用与条款引用用不同属性） |

### 2.8 扩展原则（第9章）

- 扩展内容采用新命名空间；
- 不与已有名称相同或定义矛盾；
- 满足本文件约束条件，可进一步限定；
- 行业特殊知识通过扩展子类和属性实现，**不修改本体核心定义**；
- 以模块化形式扩展。

### 2.9 附录D 实例化示例（设计验证目标）

以 GB/T 31486—2024《电动汽车用动力蓄电池电性能要求及试验方法》第5章"性能要求"为示例，国标给出了9个步骤的完整 OWL Turtle 实例化：

1. 标准实体定义
2. 层次结构实例化（章5 → 条5.1~5.4）
3. 定义核心对象（电池单体）
4. 定义特性实体（外观、极性标识、质量、外形尺寸、室温放电容量）
5. 定义约束逻辑（枚举值约束、符合性约束、数值区间约束、相对值约束）
6. 定义测试方法/行动
7. 将条款与技术内容关联（条款↔对象↔特性↔约束↔行动↔表述形式）
8. 定义外部引用（制造商技术条件）
9. 定义标准化对象（电动汽车用动力蓄电池）

**→ 平台必须能够通过配置完整复现此示例，作为核心验收标准。**

---

## 3. 产品总体设计

### 3.1 设计原则：功能充分解耦

平台采用"**模型-实例-校验-输出**"四层架构，建模要素严格解耦为独立模块：

```
┌─────────────────────────────────────────────────────────────┐
│                      本体建模平台                              │
├──────────┬──────────┬──────────┬──────────┬─────────────────┤
│ 实体类型  │ 数据属性  │ 对象属性  │ 公理规则  │  实体对象实例     │
│  模块     │  模块     │  模块     │  模块     │   模块          │
│ (Schema) │(Schema)  │(Schema)  │(Schema)  │ (Instance/Data) │
├──────────┴──────────┴──────────┴──────────┴─────────────────┤
│   命名空间与IRI管理（贯穿全模块）│ 单位字典（数据属性基础字典，§4.11）  │
├─────────────────────────────────────────────────────────────┤
│   SHACL校验引擎  │  序列化引擎(Turtle/JSON-LD/RDF/XML)         │
├─────────────────────────────────────────────────────────────┤
│   可视化展示（本体图谱）│  扩展管理（行业模块）│  导入导出         │
└─────────────────────────────────────────────────────────────┘
```

**解耦关系说明：**

| 模块 | 职责 | 依赖 | 独立性 |
|------|------|------|--------|
| 实体类型模块 | 管理类定义、继承层次、元数据 | 命名空间 | 独立配置 |
| 数据属性模块 | 管理数据属性定义、值域、枚举、单位分类约束 | 引用实体类型(定义域)、引用单位分类(数值型可选) | 独立配置 |
| 对象属性模块 | 管理对象属性定义、定义域/值域 | 引用实体类型(域/值域) | 独立配置 |
| 公理规则模块 | 管理约束规则(不相交/唯一/枚举/功能/层次/单位一致等) | 引用实体类型、属性、单位分类/单位字典 | 独立配置 |
| 实体对象模块 | 管理具体实例及属性赋值、关系连接 | 引用上述Schema | 独立配置 |
| 单位字典模块 | 管理物理量分类、单位符号、可选换算参数 | 命名空间(扩展单位) | 独立配置，被数据属性/公理/实例引用 |

各模块通过**IRI引用**松耦合关联，任一模块可独立编辑，变更后触发依赖校验。

### 3.2 技术架构

#### 3.2.1 整体分层

```
┌──────────────────────────────────────────────┐
│            前端层 (Vue 3 + TypeScript)          │
│  实体类型工作台│属性工作台│公理工作台│实例工作台│图谱可视化│
├──────────────────────────────────────────────┤
│            接口层 (RESTful API + WebSocket)     │
├──────────────────────────────────────────────┤
│            应用服务层 (Spring Boot)             │
│  Schema服务│Instance服务│Validation服务│        │
│  Serialization服务│Extension服务│Namespace服务  │
├──────────────────────────────────────────────┤
│            领域模型层 (DDD)                      │
│  EntityType│DataProperty│ObjectProperty│        │
│  AxiomRule│EntityInstance│Namespace│Ontology    │
├──────────────────────────────────────────────┤
│  存储层: PostgreSQL(元数据) + RDF Triplestore  │
│         (Jena/Apache Jena TDB或GraphDB)        │
└──────────────────────────────────────────────┘
```

#### 3.2.2 关键技术选型

| 层 | 技术 | 选型理由 |
|----|------|----------|
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + G6/AntV(图谱) | 组件化、类型安全、图谱可视化 |
| 后端 | Java 17 + Spring Boot 3 + Spring Security | 企业级、生态成熟 |
| 本体引擎 | Apache Jena（OWL/RDF/SHACL） | W3C标准实现、支持Turtle/JSON-LD |
| 校验 | SHACL（TopBraid SHACL API） | 国标5.3明确要求 |
| 关系数据库 | PostgreSQL | 元数据、配置、审计 |
| 三元组存储 | Apache Jena TDB（默认）/ 可选 GraphDB | 本体实例存储与SPARQL查询；选型理由见下注 |
| 缓存 | Redis | IRI解析、命名空间缓存 |

> **三元组存储选型说明（GraphDB vs Neo4j vs 国产RDF库）**：
>
> 国标第5.3条**强制要求** W3C本体语言（RDF/RDFS/OWL）、标准化序列化（Turtle/JSON-LD）、SHACL约束验证——这三项**只有RDF三元组库赛道能满足**，属性图数据库（Neo4j）赛道不满足。
>
> | 维度 | Apache Jena TDB（默认） | GraphDB（可选） | Neo4j | 国产gStore（远期） |
> |------|------------------------|-----------------|-------|-------------------|
> | 赛道 | RDF三元组库 | RDF三元组库 | 属性图(LPG) | RDF三元组库 |
> | RDF/RDFS/OWL | ✅ 原生 | ✅ 原生 | ❌ 需映射层 | ✅ |
> | Turtle/JSON-LD序列化 | ✅ 原生 | ✅ 原生 | ❌ 不支持 | ✅ |
> | SHACL | ✅（TopBraid API） | ✅ 原生 | ❌ 不支持 | ⚠️ 较弱 |
> | OWL推理 | ✅ Jena Reasoner | ✅ 内置推理机 | ❌ 无 | ⚠️ 较弱 |
> | 开源协议 | ✅ Apache 2.0 | ⚠️ 双协议(社区Apache2/企业商业) | ⚠️ GPL/商业 | ✅ BSD |
> | 国产化/信创 | ❌ Apache基金会(海外) | ❌ 保加利亚Ontotext | ❌ 美国 | ✅ 北京大学 |
> | 自主可控 | ⚠️ 开源可控 | ❌ 闭源商业 | ❌ 闭源商业 | ✅ |
>
> **选型决策**：
> 1. **默认 Apache Jena TDB**：开源Apache2、W3C标准全支持、国标硬性要求全覆盖，是国标场景的基准选型，满足M1-M5全部交付；
> 2. **可选 GraphDB**：规模化部署时的企业级替代，内置推理机更强，但闭源商业，按需引入；
> 3. **Neo4j 不纳入候选**：赛道不符，无法满足国标5.3条RDF/SHACL/序列化硬性要求，强行使用需自建"属性图↔RDF"双向转换层，违反最小化原则；
> 4. **信创替代路径**：若需纯国产RDF库，远期可评估 gStore（北大开源，支持SPARQL），但其SHACL/OWL推理能力弱于Jena，接入需补齐推理短板。本期推理引擎通过 `ReasonerAdapter` 适配层解耦（§4.7.4），更换底层RDF库不影响上层。

#### 3.2.3 后端模块划分（Java 包结构）

```
com.std.ontology
├── namespace          // 命名空间与IRI管理
├── entitytype         // 实体类型模块（解耦）
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── interfaces (REST)
├── dataproperty       // 数据属性模块（解耦）
├── objectproperty     // 对象属性模块（解耦）
├── axiom              // 公理与规则模块（解耦）
├── instance           // 实体对象实例模块（解耦）
├── unit               // 单位字典模块（解耦，第6.2.5.6条测量单位落地）
├── validation         // SHACL校验引擎
├── serialization      // 序列化引擎(Turtle/JSON-LD/RDF-XML)
├── extension          // 扩展管理(行业模块)
├── visualization      // 图谱数据生成
└── shared             // 共享内核(值对象、事件、基础类)
```

每个业务模块遵循 DDD 分层（domain/application/infrastructure/interfaces），模块间通过应用服务接口或领域事件通信，**不直接依赖彼此内部实现**。

---

### 3.3 标准化治理策略（减少随意录入）

平台不是通用“自由画图”工具，而是面向标准数字化的**受控建模工作台**。为降低后续数据治理成本，所有功能默认遵循以下治理策略：

| 策略 | 产品规则 | 交互体现 |
|------|----------|----------|
| 核心Schema锁定 | GB/T 48000.3—2026 内置实体类型、属性、对象属性、公理规则只读，不允许删除或改变语义 | 列表中显示“国标内置/🔒”；编辑抽屉仅开放标签、备注、排序等非语义字段 |
| 扩展命名空间强制 | 新增实体类型/属性/单位必须选择扩展命名空间，不得写入核心命名空间 | 新增表单第一步选择“扩展模块/命名空间”，未选不可提交 |
| 字典化输入优先 | 枚举、单位、实体类型、属性、实例关系均通过选择器引用，不提供默认自由文本 | 下拉树、穿梭框、级联选择、单位选择器；仅“定义/描述/备注”允许长文本 |
| 模板化实例创建 | 实例表单由 `rdf:type` 自动生成字段，字段顺序、必填、枚举、日期、单位由Schema驱动 | 选择实体类型后动态渲染表单；不显示该类型不适用字段 |
| 校验前置与阻断 | 创建/修改时实时做局部校验；发布/导出前做全量 SHACL + 轻量 OWL 公理检测 | 表单项红色错误、右侧校验面板；存在 Error 级违规时禁止发布/导出 |
| 变更影响分析 | 修改扩展Schema、单位或公理前计算被引用范围 | 删除/停用弹窗列出影响实例、规则、导出任务，并要求二次确认 |
| 审计可追溯 | 核心/扩展Schema、实例、导入导出、校验报告均留痕 | 详情页显示版本、创建人、更新时间、最近校验结果 |

---

## 4. 功能模块详细设计

### 4.1 命名空间与IRI管理模块

> 依据：第5.4条

#### 4.1.1 功能说明

| 功能 | 描述 |
|------|------|
| 命名空间注册 | 维护核心命名空间 `http://example.org/standard-ontology#` 及扩展命名空间 |
| IRI 自动生成 | IRI = 命名空间 + 本地标识符，本地标识符按命名规范（实体类型首字母大写、属性首字母小写）自动生成 |
| IRI 唯一性校验 | 全局校验 IRI 不重复 |
| 前缀映射 | 维护 prefix→namespace 映射（如 `std:`、`xsd:`、`rdf:`、`rdfs:`、`owl:`） |
| 命名空间切换 | 支持正式发布后由管理机构更新域名 |

#### 4.1.2 数据模型

```
Namespace
├── id: Long
├── prefix: String          // 如 std
├── uri: String             // http://example.org/standard-ontology#
├── isDefault: Boolean
├── scope: Enum{CORE, EXTENSION}  // 核心命名空间不可删
├── description: String
└── createdAt / updatedAt
```

#### 4.1.3 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/namespaces | 列表 |
| POST | /api/namespaces | 新增扩展命名空间 |
| PUT | /api/namespaces/{id} | 修改（核心命名空间仅可改描述） |
| GET | /api/iris/validate?iri= | 校验唯一性 |
| POST | /api/iris/generate | 根据类型+名称生成IRI |

---

### 4.2 实体类型管理模块

> 依据：第6章、附录A.1、附录B

#### 4.2.1 功能说明

| 功能 | 描述 |
|------|------|
| 实体类型CRUD | 创建/查询/修改/删除实体类型 |
| 元数据描述项维护 | 维护附录A.1的8项元数据：IRI、名称、标签、定义、属性集、父类、子类、等价类 |
| 继承层次管理 | 维护父子类关系（subClassOf），支持多级继承树 |
| 核心实体类型预置 | 开箱即用预置国标18个核心实体类型及其继承结构（附录B） |
| 抽象类标记 | 标记抽象类（如相关方、信息单元），不可实例化 |
| 等价类管理 | 维护 equivalentClass 关系 |
| 不相交类管理 | 维护 disjointWith 关系（公理规则前置） |
| 实体类型校验 | 校验命名规范、IRI唯一、继承无环、父类属性继承一致性 |

#### 4.2.2 数据模型（附录A.1 映射）

```
EntityType
├── id: Long
├── iri: String                    // 表A.1 描述项1
├── name: String                   // 表A.1 描述项2（首字母大写）
├── labels: Map<Locale,String>     // 表A.1 描述项3（多语言标签）
├── definition: String             // 表A.1 描述项4
├── dataTypeProperties: Set<IRI>   // 表A.1 描述项5（属性集）
├── superClassOf: List<IRI>        // 表A.1 描述项6（父类，subClassOf）
├── subClasses: List<IRI>          // 表A.1 描述项7（子类，hasSubclass）
├── equivalentClasses: Set<IRI>    // 表A.1 描述项8（等价类）
├── disjointWith: Set<IRI>         // 不相交类
├── isAbstract: Boolean            // 是否抽象类
├── scope: Enum{CORE, EXTENSION}   // 核心/扩展
├── namespace: Namespace           // 所属命名空间
└── auditFields
```

#### 4.2.3 预置核心实体类型树（附录B落地）

平台初始化时自动装载以下实体类型树（18个核心类型+全部子类），用户可直接使用或在其上扩展：

```
标准实体(Standard)
├── 术语标准/符号标准/分类标准/试验标准/规范标准/规程标准/指南标准/评价标准
标准化对象(StandardizationObject)
相关方(Stakeholder)【抽象】
├── 机构(Organization)
│   └── 发布机构/提出单位/归口单位/组织实施单位/起草单位/建议方/征求意见单位/出版机构
└── 个人(Individual)
    └── 起草人/评审人
领域类别(DomainCategory)
├── 国际标准分类(InternationalClassificationOfStandard)
└── 中国标准文献分类(ChineseClassificationOfStandard)
要素(ContentElement)
├── 规范性要素
│   └── 范围/术语和定义/符号和缩略语/核心技术要素/其他技术要素
└── 资料性要素
    └── 规范性引用文件/参考文献/索引
层次(StructuralElement)
├── 章(Section)
├── 条(Clause)
│   ├── 有标题条(TitledClause)
│   └── 无标题条(UntitledClause)
├── 段(Paragraph)
└── 列项(List)
    ├── 无编号列项
    └── 有编号列项
术语(Term)
信息单元(InformationUnit)【抽象】
├── 条款(Clause作为信息单元子类，即InformationUnit下的Clause)
└── 附加信息
    └── 示例/注/脚注/清单/列表
信息单元表述形式(InformationForm)
└── 条文/图/表/数学公式/附录/引用/提示
对象(Object)
├── 标准对象
└── 指标对象
特性(Property)
约束逻辑(Constraint)
行动(ActionClass)
├── 动作/路径/判断条件
外部约束(ExternalResource)
└── 法规/专利/文献/公共数据库
制定程序(StandardizationProcess)
└── 预备/立项/起草/征求意见/技术审查/批准发布/出版/复审/废止
```

> 说明：附录B中“条款”既是层次子类又是信息单元子类，平台使用**多继承**建模：Clause 同时 `subClassOf StructuralElement` 与 `subClassOf InformationUnit`。不得将 Clause 与 InformationUnit 声明为等价类，否则会错误合并两个类的全部实例。

#### 4.2.4 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/entity-types | 列表（支持按scope/namespace/父类过滤） |
| GET | /api/entity-types/tree | 继承树 |
| POST | /api/entity-types | 新建（扩展类型用扩展命名空间） |
| PUT | /api/entity-types/{id} | 修改（核心类型仅可加子类、不可删） |
| DELETE | /api/entity-types/{id} | 删除（核心类型禁删，扩展类型校验无实例引用） |
| GET | /api/entity-types/{id}/properties | 该类型全部数据属性（含继承） |
| POST | /api/entity-types/init-core | 预置核心实体类型（首次部署调用） |

---

### 4.3 数据属性管理模块

> 依据：第7.2条、附录A.2、附录A.3、附录C

#### 4.3.1 功能说明

| 功能 | 描述 |
|------|------|
| 数据属性CRUD | 创建/查询/修改/删除数据属性 |
| 元数据描述项维护 | 维护附录A.2的7项元数据：IRI、名称、标签、定义、定义域、值域、属性类型 |
| 值域配置 | 支持附录A.3的5种基本数据类型 + 枚举值 + 正则约束 + 格式约束 |
| 定义域绑定 | 绑定数据属性所属实体类型（rdfs:domain） |
| 核心数据属性预置 | 预置附录C全部47个核心数据属性 |
| 枚举值管理 | 维护枚举型数据属性的取值集合（如标准状态、约束类型、条款类型） |
| 唯一性/格式校验 | 标准编号唯一、信用代码正则、日期格式等 |
| 属性继承 | 子类自动继承父类数据属性 |

#### 4.3.2 数据模型（附录A.2 + A.3 映射）

```
DataProperty
├── id: Long
├── iri: String                    // 表A.2 描述项1
├── name: String                   // 表A.2 描述项2（首字母小写）
├── labels: Map<Locale,String>     // 表A.2 描述项3
├── definition: String             // 表A.2 描述项4
├── domain: IRI                    // 表A.2 描述项5（定义域=实体类型IRI）
├── range: RangeSpec               // 表A.2 描述项6（值域）
├── propertyType: Enum{DATATYPE}   // 表A.2 描述项7（恒为owl:DatatypeProperty）
├── scope: Enum{CORE, EXTENSION}
├── namespace: Namespace
└── auditFields

RangeSpec（值域规范）
├── baseType: Enum{BOOLEAN, DATE, NUMERIC, TEXT, URI, UNIT_REF}  // 附录A.3 五种基本类型 + 平台单位引用派生型
├── enumeration: List<String>      // 枚举值集合（可空）
├── regexPattern: String           // 正则约束（如信用代码 ^[A-Z0-9]{18}$）
├── formatHint: String             // 格式提示（如 YYYYMMDD、标准代号+顺序号+发布年份）
├── isUnique: Boolean              // 唯一性约束（如标准编号）
├── unitCategoryId: Long           // 数值型可选：限定可用单位分类，如功率/温度/无量纲
├── unitRefMode: Enum{NONE, DICTIONARY_SYMBOL} // unit/measurementUnit 等单位引用属性使用，序列化为单位符号字面量
├── standardAlias: String          // 标准/示例别名映射，如 unit ↔ measurementUnit
└── subDataType: String            // 派生类型说明（如统一资源链接派生自文本型、UNIT_REF派生自文本型）
```

#### 4.3.3 预置核心数据属性（附录C落地）

平台预置附录C全部12组47个数据属性，举例如下（完整清单见附录C）：

| 所属实体类型 | 数据属性 | 值域类型 | 特殊约束 |
|--------------|----------|----------|----------|
| 标准实体 | 编制目的 purpose | 文本型 | — |
| 标准实体 | 语言版本 languageVersion | 文本型(枚举) | 中文版本/英文版本 |
| 标准实体 | 标准状态 status | 文本型(枚举) | 草案/现行/废止/修订中 |
| 标准实体 | 约束类型 constraintType | 文本型(枚举) | 强制性/推荐性 |
| 标准实体 | 文件名称 documentName | 文本型 | — |
| 标准实体 | 文件编号 standardNumber | 文本型(格式) | 标准代号+顺序号+发布年份，唯一 |
| 标准实体 | 发布日期 issuedDate | 日期 | 国标规范值 YYYYMMDD；导出 `xsd:date` 时为 YYYY-MM-DD |
| 标准实体 | 实施日期 effectiveDate | 日期 | ≥发布日期（公理校验）；同上格式策略 |
| 机构 | 统一信用代码 creditCode | 文本型(正则) | ^[A-Z0-9]{18}$ |
| 条款 | 条款类型 clauseType | 文本型(枚举) | 要求型/推荐型/指示型/允许型/陈述型 |
| 约束逻辑 | 约束类型 constraintType | 文本型(枚举) | 数值区间/枚举值/逻辑表达式 |
| 术语 | 术语名称 termName | 文本型 | 第6.2.5.1条款直采核心属性 |
| 术语 | 术语定义 termDefinition | 文本型 | 第6.2.5.1条款直采核心属性 |
| 约束逻辑 | 最大值/最小值 maxValue/minValue | 数值型 | 可声明单位分类；实例层须与 measurementUnit 量纲一致（公理校验） |
| 约束逻辑 | 阈值范围 thresholdRange | 文本型(格式) | 格式为最小值-最大值；实例层可通过 measurementUnit 统一表达单位，兼容导入 80~100W |
| 约束逻辑 | 测量单位 unit / measurementUnit | UNIT_REF(文本型派生) | 附录C为 `unit`、附录D示例为 `measurementUnit`；实例值引用单位字典 ont_unit，导出为单位符号字面量；国标枚举 V/W/℃/% 等 |
| 制定程序 | 阶段代码 stageCode | 文本型 | — |
| 制定程序 | 开始日期/结束日期 | 日期 | 开始≤结束 |

#### 4.3.4 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/data-properties | 列表（按domain/namespace过滤） |
| POST | /api/data-properties | 新建 |
| PUT | /api/data-properties/{id} | 修改（核心属性值域不可缩窄语义） |
| DELETE | /api/data-properties/{id} | 删除（核心禁删） |
| GET | /api/data-properties/by-domain/{entityTypeIri} | 按定义域查询 |
| POST | /api/data-properties/init-core | 预置核心数据属性 |

---

### 4.4 对象属性管理模块

> 依据：第7.3条、表1

#### 4.4.1 功能说明

| 功能 | 描述 |
|------|------|
| 对象属性CRUD | 创建/查询/修改/删除对象属性 |
| 定义域/值域配置 | 配置 rdfs:domain 与 rdfs:range（指向实体类型） |
| 核心对象属性预置 | 预置表1全部34个核心对象属性 |
| 逆属性管理 | 维护逆属性（inverseOf），如"代替"↔"被代替(isReplacedBy)" |
| 功能性/反功能性标记 | 标记 FunctionalProperty / InverseFunctionalProperty（公理前置） |
| 传递/对称标记 | 标记传递性、对称性（注：第7.2条数据属性禁有此特性，对象属性可用） |
| 关系方向性展示 | 可视化展示定义域→值域方向 |

#### 4.4.2 数据模型

```
ObjectProperty
├── id: Long
├── iri: String
├── name: String                   // 首字母小写
├── labels: Map<Locale,String>     // 中英文标签
├── definition: String
├── domain: IRI                    // 定义域实体类型IRI
├── range: IRI                     // 值域实体类型IRI
├── semanticNote: String           // 语义说明（表1）
├── inverseOf: IRI                 // 逆属性
├── isFunctional: Boolean          // 功能性属性
├── isInverseFunctional: Boolean
├── isTransitive: Boolean
├── isSymmetric: Boolean
├── propertyType: Enum{OBJECT}     // owl:ObjectProperty
├── scope: Enum{CORE, EXTENSION}
├── namespace: Namespace
└── auditFields
```

#### 4.4.3 预置核心对象属性（表1落地，34项）

| 序号 | 中文 | 英文 | 定义域 | 值域 |
|------|------|------|--------|------|
| 1 | 采用 | adopts | 标准实体 | 标准实体 |
| 2 | 代替 | replaces | 标准实体 | 标准实体 |
| 3 | 引用(标准) | cites | 标准实体 | 标准实体 |
| 4 | 参考 | references | 标准实体 | 标准实体 |
| 5 | 有部分 | hasPart | 标准实体 | 标准实体 |
| 6 | 发布于 | issuedBy | 标准实体 | 机构 |
| 7 | 提出于 | proposedBy | 标准实体 | 机构 |
| 8 | 归口于 | administeredBy | 标准实体 | 机构 |
| 9 | 起草于 | draftedBy | 标准实体 | 机构/个人 |
| 10 | 出版于 | publishedBy | 标准实体 | 机构 |
| 11 | 属于领域 | classifiedUnder | 标准实体 | 领域类别 |
| 12 | 标准化对象 | standardizes | 标准实体 | 标准化对象 |
| 13 | 包含要素 | hasNormativeElement | 标准实体 | 要素 |
| 14 | 包含层次 | hasStructuralElement | 标准实体/层次 | 层次 |
| 15 | 包含条款 | hasClause | 要素/层次 | 条款 |
| 16 | 包含子条 | hasSubClause | 条 | 条 |
| 17 | 界定 | defines | 标准实体/要素 | 术语 |
| 18 | 提及术语 | usesTerm | 信息单元 | 术语 |
| 19 | 具有表述形式 | hasRepresentationForm | 信息单元 | 信息单元表述形式 |
| 20 | 有示例 | hasExample | 条款 | 附加信息 |
| 21 | 有注 | hasNote | 条款 | 附加信息 |
| 22 | 引用标准(条款) | citesStandard | 条款 | 标准实体 |
| 23 | 引用章条 | referencesClause | 信息单元 | 层次 |
| 24 | 涉及对象 | involvesObject | 条款 | 对象 |
| 25 | 规定特性 | specifiesCharacteristic | 条款 | 特性 |
| 26 | 有特性 | hasCharacteristic | 对象 | 特性 |
| 27 | 施加约束 | imposesConstraint | 信息单元/特性 | 约束逻辑 |
| 28 | 约束对象 | constrainsObject | 约束逻辑 | 对象 |
| 29 | 约束特性 | constrainsCharacteristic | 约束逻辑 | 特性 |
| 30 | 描述行动 | describesAction | 信息单元 | 行动 |
| 31 | 引用外部资源 | referencesExternalResource | 标准实体/条款 | 外部约束 |
| 32 | 与专利有关 | isRelatedToPatent | 条款 | 专利 |
| 33 | 处于阶段 | hasDevelopmentStage | 标准实体 | 制定程序 |
| 34 | 包含标准 | includesStandard | 制定程序 | 标准实体 |

> 注：第16号"包含子条 hasSubClause"为逆属性场景；第34号为第33号的逆属性。

#### 4.4.4 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/object-properties | 列表 |
| POST | /api/object-properties | 新建 |
| PUT | /api/object-properties/{id} | 修改 |
| DELETE | /api/object-properties/{id} | 删除（核心禁删） |
| GET | /api/object-properties/by-domain/{entityTypeIri} | 按定义域查询 |
| GET | /api/object-properties/by-range/{entityTypeIri} | 按值域查询 |
| POST | /api/object-properties/init-core | 预置核心对象属性 |

---

### 4.5 公理与规则管理模块

> 依据：第8章

#### 4.5.1 功能说明

本模块是保证本体逻辑一致性、完整性、准确性的核心。规则按国标第8.2条三大类组织，并以 OWL + SHACL 双轨形式化表达。

| 功能 | 描述 |
|------|------|
| 规则CRUD | 创建/查询/修改/删除公理规则 |
| 规则分类管理 | 实体类型规则 / 属性规则 / 关系规则 三大类 |
| 规则形式化 | 自动生成 OWL 公理 + SHACL 约束 |
| 核心规则预置 | 预置第8.2条全部核心规则 |
| 规则启停 | 规则可启用/停用（扩展可进一步限定） |
| 规则校验执行 | 调用SHACL引擎对实例执行校验 |
| 校验报告 | 输出违规明细（违反规则、实例IRI、期望值/实际值） |

#### 4.5.2 规则数据模型

```
AxiomRule
├── id: Long
├── name: String                   // 规则名称
├── category: Enum{ENTITY_TYPE, PROPERTY, RELATION}  // 第8.2条三类
├── subType: String                // 如 不相交/唯一标识/唯一性/日期有效/枚举/取值/功能性/版本替代/层次/引用区分
├── description: String
├── owlAxiom: String               // OWL形式化表达（Turtle片段）
├── shaclShape: String             // SHACL约束表达
├── targetEntityTypes: Set<IRI>    // 规则作用实体类型
├── targetProperties: Set<IRI>     // 规则作用属性
├── isEnabled: Boolean
├── scope: Enum{CORE, EXTENSION}
└── auditFields
```

#### 4.5.3 预置核心规则（第8.2条落地）

**a) 实体类型规则**

| 规则 | OWL/SHACL表达 | 说明 |
|------|---------------|------|
| 实体类型不相交 | `owl:disjointWith`（规范性要素 ⇄ 资料性要素） | 互斥实体类型不可同属 |
| 全局唯一标识 | SHACL: `sh:nodeKind sh:IRI` + 信息单元 uniqueIdentifier 唯一 | 信息单元需全局唯一标识符 |

**b) 属性规则**

| 规则 | 表达 | 说明 |
|------|------|------|
| 属性唯一性约束 | SHACL: `sh:hasValue`/`sh:minCount 1; sh:maxCount 1` 或 OWL `owl:hasKey` | 标准编号唯一 |
| 日期有效性验证 | SHACL SPARQL: `effectiveDate >= issuedDate` | 实施日期≥发布日期 |
| 枚举值约束 | SHACL: `sh:in (草案 现行 废止 修订中)` | 标准状态取值限定 |
| 取值约束 | SHACL: `sh:in (强制性 推荐性)` | 约束类型限定 |
| 单位一致性约束 | SHACL SPARQL: 同一 Constraint 实例的 maxValue/minValue/thresholdRange 须与 measurementUnit 引用的单位字典条目同属一个物理量分类 | 附录C表C.38~C.41联动，附录D步骤5验证 |

**c) 关系规则**

| 规则 | 表达 | 说明 |
|------|------|------|
| 功能性属性约束 | OWL: `owl:FunctionalProperty`（issuedBy） | 每个标准只能由一个机构发布 |
| 版本替代关系 | SHACL: 废止标准必须 hasReplacement 或废止日期 | 废止标准指向替代标准 |
| 层级包含关系 | SHACL: 章 `hasSubClause` minCount 0 | 章可包含零或多个条 |
| 结构限制 | SHACL: 无标题条 `hasSubClause maxCount 0` | 无标题条不可含子条 |
| 引用关系区分 | OWL: cites(标准间) ≠ citesStandard(条款级) | 不同引用用不同属性 |

#### 4.5.4 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/axiom-rules | 列表（按category过滤） |
| POST | /api/axiom-rules | 新建扩展规则 |
| PUT | /api/axiom-rules/{id}/toggle | 启停 |
| POST | /api/axiom-rules/validate | 执行SHACL校验，返回报告 |
| GET | /api/axiom-rules/{id}/owl | 查看OWL形式化 |
| GET | /api/axiom-rules/{id}/shacl | 查看SHACL形式化 |
| POST | /api/axiom-rules/init-core | 预置核心规则 |

---

### 4.6 实体对象实例管理模块

> 依据：附录D 实例化示例

#### 4.6.1 功能说明

本模块基于前述Schema（实体类型+数据属性+对象属性+公理）创建具体实例，是"附录D配置化实现"的主阵地。

| 功能 | 描述 |
|------|------|
| 实例CRUD | 创建/查询/修改/删除实体对象实例 |
| 实例类型绑定 | 为实例指定所属实体类型（rdf:type） |
| 数据属性赋值 | 为实例填写数据属性值（按值域校验：枚举/正则/日期/数值） |
| 对象属性连接 | 建立实例间关系（按对象属性定义域/值域校验） |
| 实例IRI生成 | 实例IRI生成（支持自定义本地标识符） |
| 实例校验 | 创建/修改时实时触发公理与SHACL校验 |
| 批量实例导入 | 支持从结构化数据(Excel/JSON/Turtle)批量导入实例 |
| 实例序列化 | 将实例导出为 OWL Turtle / JSON-LD |

#### 4.6.2 数据模型

```
EntityInstance
├── id: Long
├── iri: String                    // 实例IRI
├── rdfType: IRI                   // 所属实体类型IRI（rdf:type）
├── label: String
├── dataValues: Map<DataPropertyIRI, Object>  // 数据属性赋值
├── objectRelations: Map<ObjectPropertyIRI, List<InstanceIRI>>  // 对象属性连接
├── ontologyId: Long               // 所属本体工程
└── auditFields
```

#### 4.6.3 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/instances | 列表（按type/ontology过滤） |
| POST | /api/instances | 创建实例（含校验） |
| PUT | /api/instances/{id} | 修改 |
| DELETE | /api/instances/{id} | 删除（校验无被引用） |
| POST | /api/instances/{id}/relations | 建立对象属性关系 |
| POST | /api/instances/batch-import | 批量导入 |
| GET | /api/instances/{id}/turtle | 单实例Turtle序列化 |
| POST | /api/ontologies/{id}/serialize | 整体序列化（见4.8） |

---

### 4.7 校验引擎模块

> 依据：第5.3条、第8章

#### 4.7.1 功能说明

> 边界说明：本模块承接国标第5.3条要求的OWL公理一致性检测（轻量推理）与SHACL约束校验，**不包含**大规模图谱推理（传递闭包、规则链）。两者边界见 §1.3。

| 功能 | 描述 | 推理层级 |
|------|------|---------|
| SHACL校验 | 基于公理规则模块生成的SHACL Shapes对实例执行约束验证 | 约束校验（非推理） |
| OWL公理一致性检测 | 检测不相交冲突、功能属性唯一性、传递性等公理级逻辑一致性（国标5.3要求） | 轻量推理（内置） |
| 实时校验 | 实例创建/修改时即时校验数据属性值域、对象属性域/值域 | 约束校验 |
| 全量校验 | 对整个本体工程执行全量校验 | 约束校验+轻量推理 |
| 校验报告 | 结构化报告：通过/警告/错误，含定位与修复建议 | — |
| IRI唯一性校验 | 全局IRI重复检测 | 约束校验 |
| **推理引擎扩展** | 通过 `ReasonerAdapter` 适配层接入可插拔推理引擎（见§4.7.4） | 扩展点（本期不预置大规模推理） |

#### 4.7.2 校验报告模型

```
ValidationReport
├── ontologyId: Long
├── triggeredAt: DateTime
├── conforms: Boolean              // 是否全部通过
├── results: List<ValidationResult>

ValidationResult
├── severity: Enum{INFO, WARNING, VIOLATION}
├── focusNode: IRI                 // 违规实例IRI
├── resultPath: IRI                // 违规属性IRI
├── ruleName: String               // 违反规则
├── message: String                // 说明
├── expectedValue: String
├── actualValue: String
└── suggestion: String             // 修复建议
```

#### 4.7.3 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/validation/run?ontologyId= | 全量校验（含SHACL+OWL公理一致性） |
| POST | /api/validation/instance/{id} | 单实例校验 |
| GET | /api/validation/report/{ontologyId} | 获取报告 |
| POST | /api/validation/reason?ontologyId= | 触发扩展推理（需接入扩展ReasonerAdapter，否则返回未启用） |

#### 4.7.4 推理引擎适配层（扩展点）

> 依据：第9章扩展原则——行业扩展可引入深度推理能力

为消除"非目标一刀切排除推理"与"国标5.3要求OWL公理一致性检测"的矛盾，并为行业扩展预留深度推理接入能力，校验引擎通过 `ReasonerAdapter` 适配层解耦推理引擎实现：

```
ReasonerAdapter（SPI 接口，可插拔）
├── checkConsistency(OntologyModel): ConsistencyReport   // 公理一致性检测（国标5.3，本期必做）
├── inferEntailments(OntologyModel): InferenceReport      // 推理闭包（扩展，本期返回未启用）
├── getReasonerName(): String                             // 引擎标识
└── getReasonerCapabilities(): Set<ReasonerCapability>    // 能力声明
```

**本期实现**：`JenaReasonerAdapter`（默认）—— 仅实现 `checkConsistency`，承接国标5.3 OWL公理一致性检测（不相交、功能属性、传递性）；`inferEntailments` 返回 `UnsupportedOperationException("大规模推理本期未启用")`。

**扩展接入方式**：

| 扩展场景 | 实现方式 | 说明 |
|---------|---------|------|
| 行业深度推理（医疗传递性、石油规则链） | 实现 `ReasonerAdapter` 接入 GraphDB 推理机 | 通过 `META-INF/services` SPI 机制注册，配置切换 |
| 国产化替代 | 实现 `ReasonerAdapter` 适配 gStore/其它国产RDF库推理能力 | 适配层屏蔽底层差异，上层校验逻辑零改动 |
| 自定义规则推理 | 实现 `ReasonerAdapter` + 注册自定义规则集 | 配合公理规则模块（§4.5）扩展规则 |

**设计约束**：
1. 适配层为**SPI接口**，不在核心代码硬编码具体Reasoner实现，符合"功能充分解耦"原则；
2. 本期不预置任何大规模推理实现，仅预留接口与 `JenaReasonerAdapter`（轻量公理检测）；
3. 扩展推理引擎需通过扩展管理模块（§4.9）注册，使用扩展命名空间标识，不修改核心校验逻辑。

---

### 4.8 序列化与交换模块

> 依据：第5.3条

| 功能 | 描述 |
|------|------|
| 多格式导出 | Turtle、JSON-LD、RDF/XML、N-Triples |
| 多格式导入 | 支持上述格式导入并解析为内部模型 |
| 本体工程整体序列化 | Schema(实体类型/属性/公理) + Data(实例) 一体导出 |
| 按模块序列化 | 可仅导出某实体类型子树或某实例集 |
| 前缀声明自动生成 | 根据命名空间模块生成 `@prefix` 声明 |
| 校验前置 | 导出前可选执行一致性校验 |

**导出结构示例（对应附录D Turtle）：**

```turtle
@prefix : <http://example.org/standard-ontology#>.
@prefix std: <http://example.org/standard/GB-T-31486-2024/>.
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix xsd: <http://www.w3.org/2001/XMLSchema#>.
@prefix owl: <http://www.w3.org/2002/07/owl#>.

# 1.标准实体定义
std:GB-T_31486-2024 a :Standard ;
    :standardNumber "GB/T 31486—2024" ;
    :documentName "电动汽车用动力蓄电池电性能要求及试验方法" ;
    :issuedDate "2024-09-29"^^xsd:date ;
    :effectiveDate "2025-04-01"^^xsd:date ;
    :purpose "规定电动汽车用动力蓄电池的电性能要求及试验方法" ;
    :languageVersion "中文" ;
    :status "现行" ;
    :constraintType "推荐性" ;
    :hasStructuralElement std:Clause_5, std:Clause_5_1, ... .
# ...(后续实例见4.10)
```

#### 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/serialization/export?ontologyId=&format=TURTLE | 导出 |
| POST | /api/serialization/import | 导入解析 |

---

### 4.9 扩展管理模块

> 依据：第9章

| 功能 | 描述 |
|------|------|
| 行业模块管理 | 以模块化形式管理行业扩展本体（如医疗、石油、电梯） |
| 扩展命名空间隔离 | 每个行业扩展使用独立命名空间，不污染核心 |
| 扩展合法性校验 | 校验扩展不与核心同名/矛盾、不修改核心定义、仅可加子类/属性 |
| 扩展导出 | 行业扩展可独立导出为模块包 |
| 核心锁定 | 核心实体类型/属性/规则只读，仅可在其上扩展 |

**扩展校验规则（第9.2条落地）：**

1. 扩展实体类型/属性必须使用扩展命名空间（非 `standard-ontology#`）；
2. 名称不得与核心重复；
3. 定义不得与核心矛盾；
4. 满足核心约束条件，可进一步限定（如枚举值子集化）；
5. 只允许 subClassOf 核心类型，不允许修改核心类型本身。

#### 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/extensions/modules | 创建行业扩展模块 |
| GET | /api/extensions/modules | 列表 |
| POST | /api/extensions/validate | 扩展合法性校验 |
| GET | /api/extensions/modules/{id}/export | 导出模块包 |

---

### 4.10 可视化模块

| 功能 | 描述 |
|------|------|
| 实体类型继承树 | 树形展示实体类型父子层次 |
| 对象属性关系图 | 有向图展示实体类型间对象属性（节点=实体类型，边=对象属性） |
| 实例图谱 | 展示某本体工程内实例及关系（如附录D电池标准实例图谱） |
| 数据属性面板 | 选中实体类型展示其数据属性 |
| 公理标注 | 图谱上标注不相交、功能属性等约束 |
| 图谱导出 | 导出为 PNG/SVG |

前端采用 AntV G6 渲染有向图，支持缩放、过滤、高亮。

---

### 4.11 单位字典管理模块

> 依据：第6.2.5.6条（约束逻辑类属性含测量单位）、附录C表C.41（测量单位数据属性定义）、附录D步骤5（实例化使用）

#### 4.11.1 功能说明

国标将"测量单位"作为约束逻辑类的核心数据属性（47个核心数据属性之一），并在附录D实际使用。表C.41备注枚举为"V、W、℃ **等**"——"等"表明枚举开放，附录D使用的 `%` 不在举例之列即为佐证。因此测量单位不能作为内嵌文本枚举，须独立为单位字典统一治理，供约束逻辑类实例及任意数值型数据属性的单位分类约束引用。

| 功能 | 描述 |
|------|------|
| 单位分类CRUD | 维护物理量分类（如温度、电压、功率、无量纲百分比），每个分类有基准单位 |
| 单位条目CRUD | 维护单位符号/名称/所属分类/是否基准/是否内置 |
| 国标枚举预置 | 预置表C.41例举单位（V、W、℃）+ 附录D实际使用单位（%）+ 常用SI单位，开箱即用 |
| 用户扩展 | 支持行业扩展单位（如电梯行业的 m/s、石油行业的 bbl），用扩展命名空间标识 |
| 内置保护 | 平台预置单位不可删除，仅允许改中文名/排序/备注 |
| 单位引用 | 数值型数据属性可绑定单位分类；`unit/measurementUnit` 实例值引用单位字典条目（§4.3.2），按导出策略输出标准原名或示例别名 |
| 换算参数（可选增强） | 保留 factor/offset 字段位与 convert 接口，但首期不预置换算数据——国标未要求换算，避免过度设计 |

#### 4.11.2 数据模型

```
UnitCategory（单位分类）
├── id: Long
├── categoryCode: String       // 如 temperature、voltage、dimensionless
├── categoryName: String       // 如 温度、电压、无量纲
├── baseUnitSymbol: String     // 基准单位符号（如 K、V、—）
├── isBuiltin: Boolean         // 内置分类不可删
├── sortOrder: Integer
└── auditFields

Unit（单位条目）
├── id: Long
├── categoryId: Long           // 逻辑外键 → UnitCategory.id
├── unitCode: String           // 全局唯一英文代码（如 volt、watt、percent）
├── unitSymbol: String         // 显示符号（如 V、W、%、℃），允许 Unicode
├── unitName: String           // 中文名（如 伏特、瓦特、百分比）
├── isBaseUnit: Boolean        // 是否分类基准
├── isBuiltin: Boolean         // 内置单位不可删，语义字段锁定
├── factor: BigDecimal         // 换算乘系数（可选，y = factor*x + offset；首期不预置）
├── offsetValue: BigDecimal    // 换算偏移（可选；首期不预置）
├── sortOrder: Integer
├── namespace: Namespace       // 扩展单位归属扩展命名空间
└── auditFields
```

> **与postgresql版04单位字典的差异说明**（仅供参考，不作为本PRD依据）：postgresql版04基于"GB 3100-1993 + IoT物模型"预置231条/41类并强制换算参数；本PRD以GB/T 48000.3—2026国标为唯一依据，国标仅例举"V、W、℃ 等"且未要求换算，故本字典为**国标口径最小可用版本**：预置国标例举+附录D使用+常用SI单位，换算参数与convert接口作为可选增强保留但首期不预置数据，符合"Prefer Simplicity"原则。

#### 4.11.3 预置单位（国标口径）

| 分类 | categoryCode | 基准 | 预置单位（unitSymbol） | 国标依据 |
|------|--------------|------|----------------------|----------|
| 电压 | voltage | V | V（伏特） | 表C.41例举 |
| 功率 | power | W | W（瓦特）、kW | 表C.41例举 |
| 温度 | temperature | ℃ | ℃（摄氏度）、K | 表C.41例举 |
| 无量纲 | dimensionless | — | %（百分比） | 附录D步骤5实际使用 |
| 电流 | current | A | A（安培） | 常用SI，附录D对象"电池单体"隐含 |
| 质量 | mass | kg | kg（千克）、g | 常用SI |
| 长度 | length | m | m（米）、mm、cm | 常用SI |
| 时间 | time | s | s（秒）、min、h | 常用SI |
| 容量 | battery | Ah | Ah（安时）、Wh | 附录D对象"电池单体"隐含 |
| 计数 | count | — | 次、台、个 | 约束逻辑可能引用 |

> 完整预置清单以初始化脚本为准，上表为国标明确依据的最小集。用户可通过扩展命名空间增补行业单位（如电梯业 m/s²、石油业 bbl）。

#### 4.11.4 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/unit-categories | 单位分类列表 |
| POST | /api/unit-categories | 新增分类（扩展命名空间） |
| GET | /api/units | 单位列表（按 categoryId 过滤） |
| GET | /api/units/tree | 分类+单位两级树（首屏加载） |
| GET | /api/units/by-symbol/{symbol} | 按符号解析单位（导入与附录D兼容） |
| POST | /api/units | 新增单位（扩展命名空间） |
| PUT | /api/units/{id} | 修改（内置单位语义字段锁定） |
| DELETE | /api/units/{id} | 删除（内置禁删；校验无实例、属性分类或规则引用） |
| GET | /api/units/convert?from=&to=&value= | 单位换算（可选增强，同分类内 y=factor*x+offset） |

#### 4.11.5 与其他模块的关系

| 关联模块 | 关系 | 说明 |
|---------|------|------|
| 数据属性模块（§4.3） | RangeSpec.unitCategoryId / unitRefMode | 数值型属性限定单位分类；`measurementUnit` 作为 UNIT_REF 派生属性引用单位字典 |
| 公理规则模块（§4.5） | 单位一致性约束规则 | maxValue/minValue/thresholdRange 与 measurementUnit 引用单位同分类校验 |
| 扩展管理模块（§4.9） | 扩展单位用扩展命名空间 | 行业扩展单位不污染核心单位字典 |
| 实例建模（§4.6） | 实例赋值时单位校验 | 约束逻辑实例的 measurementUnit 通过单位选择器引用有效单位字典条目 |

---

## 5. 附录D 实例化示例配置实现方案（核心验收场景）

> 本节是平台的**核心验收标准**：通过平台配置，完整复现国标附录D中 GB/T 31486—2024 动力蓄电池标准的 OWL Turtle 实例化。

### 5.1 实现路径

附录D共9个步骤，平台对应操作如下：

| 附录D步骤 | 平台操作模块 | 配置内容 |
|-----------|--------------|----------|
| 1. 标准实体定义 | 实体对象实例模块 | 新建 `Standard` 实例，填8个数据属性 |
| 2. 层次结构实例化 | 实体对象实例模块 | 新建 1个章(Section)+4个条(Clause)，章到条使用 hasClause，条到子条使用 hasSubClause |
| 3. 定义核心对象 | 实体对象实例模块 | 新建 `Object` 实例"电池单体"，用 hasCharacteristic 连5个特性 |
| 4. 定义特性实体 | 实体对象实例模块 | 新建5个 `Property` 实例，用 imposesConstraint 连约束 |
| 5. 定义约束逻辑 | 实体对象实例模块 | 新建7个 `Constraint` 实例（枚举值/符合性/数值区间/相对值） |
| 6. 定义测试方法(行动) | 实体对象实例模块 | 新建4个 `ActionClass` 实例，由条款/信息单元通过 describesAction 连接行动 |
| 7. 条款与技术内容关联 | 实体对象实例模块 | 新建4个 `Clause`(信息单元子类)实例，连 involvesObject/specifiesCharacteristic/describesAction/hasRepresentationForm |
| 8. 定义外部引用 | 实体对象实例模块 | 新建 `ExternalResource` 实例"制造商技术条件" |
| 9. 定义标准化对象 | 实体对象实例模块 | 新建 `StandardizationObject` 实例，由标准实体通过 standardizes 连接标准化对象 |

### 5.2 详细配置清单

#### 5.2.1 步骤1：标准实体定义

实例IRI：`std:GB-T_31486-2024`　　rdf:type：`:Standard`

| 数据属性 | 值 |
|----------|----|
| standardNumber | "GB/T 31486—2024" |
| documentName | "电动汽车用动力蓄电池电性能要求及试验方法" |
| issuedDate | "2024-09-29"^^xsd:date |
| effectiveDate | "2025-04-01"^^xsd:date |
| purpose | "规定电动汽车用动力蓄电池的电性能要求及试验方法" |
| languageVersion | "中文" |
| status | "现行" |
| constraintType | "推荐性" |

对象属性 hasStructuralElement → `std:Clause_5`；`std:Clause_5` 再通过 hasClause 连接 `std:Clause_5_1`~`std:Clause_5_4`。

#### 5.2.2 步骤2：层次结构实例化

| 实例IRI | rdf:type | 数据属性 | 对象属性 |
|---------|----------|----------|----------|
| std:Clause_5 | :Section | sectionNumber="5"; sectionTitle="性能要求" | hasClause → Clause_5_1~5_4 |
| std:Clause_5_1 | :Clause | clauseNumber="5.1"; clauseTitle="外观" | hasClause → Appearance_Requirement |
| std:Clause_5_2 | :Clause | clauseNumber="5.2"; clauseTitle="极性标识" | hasClause → Polarity_Requirement |
| std:Clause_5_3 | :Clause | clauseNumber="5.3"; clauseTitle="质量和外形尺寸" | hasClause → Dimensions_Requirement |
| std:Clause_5_4 | :Clause | clauseNumber="5.4"; clauseTitle="室温放电容量" | hasClause → Discharge_Capacity_Requirement |

#### 5.2.3 步骤3：核心对象

| 实例IRI | rdf:type | 数据属性 | 对象属性 |
|---------|----------|----------|----------|
| std:BatteryCell | :Object | objectName="电池单体"; objectCategory="产品" | hasCharacteristic → Appearance_Prop, Polarity_Prop, Mass_Prop, Dimensions_Prop, Discharge_Capacity_Prop |

#### 5.2.4 步骤4：特性实体（5个）

| 实例IRI | propertyName | propertyType | propertyValue | imposesConstraint |
|---------|--------------|--------------|---------------|-------------------|
| std:Appearance_Prop | 外观 | 描述型 | — | Appearance_Constraint |
| std:Polarity_Prop | 极性标识 | 描述型 | — | Polarity_Constraint |
| std:Mass_Prop | 质量 | 描述型 | 符合制造商产品技术条件 | Mass_Constraint |
| std:Dimensions_Prop | 外形尺寸 | 描述型 | 符合制造商产品技术条件 | Dimensions_Constraint |
| std:Discharge_Capacity_Prop | 室温放电容量 | 能力型 | — | Capacity_Constraint_1/2/3 |

#### 5.2.5 步骤5：约束逻辑（7个）

> measurementUnit 字段值须引用单位字典（§4.11）中的条目；`%` 对应单位字典无量纲分类的"百分比"单位。

| 实例IRI | constraintType | 其他属性 |
|---------|----------------|----------|
| std:Appearance_Constraint | 枚举值 | allowedValue="无变形、无裂纹、无毛刺、干燥、无外伤、无污物、有清晰正确的标志" |
| std:Polarity_Constraint | 枚举值 | allowedValue="正确、清晰" |
| std:Mass_Constraint | 符合性 | conformsTo="制造商提供的产品技术条件" |
| std:Dimensions_Constraint | 符合性 | conformsTo="制造商提供的产品技术条件" |
| std:Capacity_Constraint_1 | 数值区间 | minValue=100; maxValue=110; measurementUnit=单位字典:percent(%); comparedTo="额定容量" |
| std:Capacity_Constraint_2 | 相对值 | maxValue=5; measurementUnit=单位字典:percent(%); comparedTo="初始容量平均值"; constraintTarget="所有测试对象初始容量极差" |

#### 5.2.6 步骤6：行动（4个测试方法）

| 实例IRI | actionName | actionDescription | 关联方式 |
|---------|------------|-------------------|----------|
| std:Test_6_2_1 | 外观检验 | 按6.2.1规定的方法进行外观检验 | Appearance_Requirement describesAction → 本行动 |
| std:Test_6_2_2 | 极性标识检验 | 按6.2.2规定的方法进行极性标识检验 | Polarity_Requirement describesAction → 本行动 |
| std:Test_6_2_3 | 质量和外形尺寸检验 | 按6.2.3规定的方法进行质量和外形尺寸检验 | Dimensions_Requirement describesAction → 本行动 |
| std:Test_6_2_5 | 室温放电容量试验 | 按6.2.5规定的方法进行室温放电容量试验 | Discharge_Capacity_Requirement describesAction → 本行动 |

#### 5.2.7 步骤7：条款与技术内容关联（4个要求型条款）

每个条款（信息单元子类 Clause）实例配置统一结构：

```
std:Appearance_Requirement a :Clause ;
    :uniqueIdentifier "IU-5-1001" ;
    :contentDescription "电池单体按6.2.1检验,外观应无变形及裂纹..." ;
    :clauseType "要求型" ;
    :constraintType "强制" ;
    :involvesObject std:BatteryCell ;
    :specifiesCharacteristic std:Appearance_Prop ;
    :describesAction std:Test_6_2_1 ;
    :hasRepresentationForm :TextualParagraph .
```

四个条款实例：Appearance_Requirement(IU-5-1001)、Polarity_Requirement(IU-5-2001)、Dimensions_Requirement(IU-5-3001)、Discharge_Capacity_Requirement(IU-5-4001)。

#### 5.2.8 步骤8：外部引用

```
std:ManufacturerTechSpec a :ExternalResource ;
    :fileType "产品技术条件" ;
    :responsibleParty "制造商" .
```

#### 5.2.9 步骤9：标准化对象

```
std:BatteryStandardizationObject a :StandardizationObject ;
    :subjectName "电动汽车用动力蓄电池" ;
    :industrialSector "C3841 锂离子电池制造" .
```
并由标准实体 `std:GB-T_31486-2024 :standardizes std:BatteryStandardizationObject` 连接，方向与国标表1一致。

### 5.3 配置化导出验证

完成上述9步配置后，平台执行"整体序列化导出（Turtle）"，输出结果应与国标附录D的 OWL 实例化文本在语义上等价（允许 IRI 分隔符规范化差异）。导出后执行全量SHACL校验，应全部通过。

### 5.4 验收标准

| 验收项 | 标准 |
|--------|------|
| 实例完整性 | 9个步骤全部实例化，无遗漏 |
| 关系完整性 | 所有对象属性连接按附录D建立 |
| 数据属性值域 | 全部符合附录C定义（枚举/日期/数值） |
| **单位引用完整性** | 步骤5约束逻辑的 `measurementUnit`（兼容附录C `unit`）须引用单位字典有效条目，默认按附录D序列化为 `:measurementUnit "%"`，也可按严格附录C策略输出 `:unit "%"` |
| **单位一致性校验** | Capacity_Constraint_1/2 的 maxValue/minValue 与 measurementUnit(%) 量纲一致，SHACL 校验通过 |
| 公理校验 | 全量SHACL校验通过，0 VIOLATION |
| 序列化等价 | 导出Turtle与附录D语义等价 |
| 可复现性 | 不同操作者按配置指南可复现 |

---

## 6. 前端页面设计

### 6.1 信息架构

```
主导航
├── 工作台（概览：本体工程列表、校验状态、快捷操作）
├── 本体Schema设计
│   ├── 命名空间管理
│   ├── 实体类型管理（继承树 + 详情面板）
│   ├── 数据属性管理（列表 + 值域配置）
│   ├── 对象属性管理（列表 + 域/值域配置）
│   ├── 公理规则管理（分类列表 + OWL/SHACL预览）
│   └── 单位字典管理（分类树 + 单位列表）  ← 第6.2.5.6条测量单位落地
├── 实例建模
│   ├── 实例管理（按实体类型分组）
│   ├── 关系编辑器（图谱式连接）
│   └── 批量导入
├── 校验中心（校验报告、违规明细）
├── 可视化（继承树、关系图、实例图谱）
├── 扩展管理（行业模块）
└── 导入导出（多格式序列化）
```

### 6.2 关键页面

#### 6.2.1 实体类型管理页

- 左：实体类型继承树（可拖拽调整父子）
- 右：选中类型详情（附录A.1八项元数据表单 + 数据属性列表 + 公理标注）
- 顶部：新建/扩展/导出按钮；核心类型标记 🔒

#### 6.2.2 数据属性管理页

- 列表（按定义域实体类型分组）
- 详情：附录A.2七项元数据 + 值域规范配置（基本类型/枚举/正则/格式/唯一）
- 枚举值编辑器（可视化增删枚举项）

#### 6.2.3 实例建模页（附录D配置主界面）

- 顶部：本体工程选择、序列化导出、全量校验按钮
- 左：实体类型分类导航
- 中：实例列表 + 实例详情表单（数据属性赋值 + 对象属性连接选择器）
- 右：实时校验提示
- 底部：图谱预览（当前实例的关系）

#### 6.2.4 可视化图谱页

- AntV G6 有向图
- 节点：实体类型/实例（按类型着色）
- 边：对象属性（带标签）
- 工具：过滤、高亮路径、展开/折叠子图、导出图片

#### 6.2.5 单位字典管理页

- 左：单位分类树（物理量维度，如温度/电压/功率/无量纲）
- 右：选中分类下的单位列表（符号/名称/代码/是否基准/是否内置）
- 数据属性值域配置处（§4.3）通过下拉限定单位分类；实例 `measurementUnit` 通过单位选择器引用单位字典条目
- 内置单位标记 🔒，仅允许改中文名/排序/备注
- 顶部：新增分类、新增单位、换算工具（可选增强）

### 6.3 关键交互规范（原型实现要求）

| 场景 | 交互规则 | 目的 |
|------|----------|------|
| 菜单布局 | 采用 Ant Design Pro 风格：左侧深色一级菜单，顶部项目选择与全局操作，内容区卡片化 | 降低企业用户学习成本 |
| Schema列表 | 左树右表/详情；核心项显示蓝色“国标内置”标签与锁图标 | 区分核心与扩展，防止误改 |
| 新增扩展 | 抽屉式分步表单：基础信息 → 定义域/值域 → 约束/预览 → 校验 | 将复杂建模拆为可控步骤 |
| 实例建模 | 选择实体类型后动态生成表单；枚举/单位/对象关系必须选择引用；日期通过日期控件输入 | 消除字段自由发挥 |
| 单位选择 | 单位字段使用“分类 + 单位”级联选择器，支持按符号搜索；附录D导入 `%` 时自动解析为 percent | 保证单位治理一致 |
| 校验反馈 | 页面右侧固定“校验结果”面板，按 Error/Warning/Info 分组，点击定位到字段或图谱节点 | 让用户边建模边修复 |
| 菜单切换 | 保留当前项目上下文；离开有未保存变更的页面时弹出确认 | 防止误丢配置 |
| 导入导出 | 导入先预检映射，导出前强制全量校验；Error未清零时禁用发布导出 | 保证交换质量 |

---

## 7. 数据库设计（核心表）

> 关系库存储元数据与配置；三元组库存储本体实例。以下为关系库核心表。

### 7.1 核心表结构

```sql
-- 命名空间
CREATE TABLE namespace (
  id BIGSERIAL PRIMARY KEY,
  prefix VARCHAR(64) NOT NULL UNIQUE,
  uri TEXT NOT NULL UNIQUE,
  is_default BOOLEAN DEFAULT FALSE,
  scope VARCHAR(16) NOT NULL,  -- CORE/EXTENSION
  description TEXT,
  created_at TIMESTAMP, updated_at TIMESTAMP
);

-- 实体类型
CREATE TABLE entity_type (
  id BIGSERIAL PRIMARY KEY,
  iri TEXT NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  definition TEXT,
  is_abstract BOOLEAN DEFAULT FALSE,
  scope VARCHAR(16) NOT NULL,
  namespace_id BIGINT REFERENCES namespace(id),
  ontology_id BIGINT NOT NULL,
  created_at TIMESTAMP, updated_at TIMESTAMP
);
-- 多语言标签
CREATE TABLE entity_type_label (
  entity_type_id BIGINT REFERENCES entity_type(id),
  locale VARCHAR(16), label TEXT,
  PRIMARY KEY(entity_type_id, locale)
);
-- 继承关系
CREATE TABLE entity_type_hierarchy (
  parent_id BIGINT REFERENCES entity_type(id),
  child_id BIGINT REFERENCES entity_type(id),
  PRIMARY KEY(parent_id, child_id)
);
-- 不相交
CREATE TABLE entity_type_disjoint (
  type_a BIGINT, type_b BIGINT, PRIMARY KEY(type_a, type_b)
);

-- 数据属性
CREATE TABLE data_property (
  id BIGSERIAL PRIMARY KEY,
  iri TEXT NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  definition TEXT,
  domain_entity_type_id BIGINT REFERENCES entity_type(id),
  base_type VARCHAR(32) NOT NULL,  -- BOOLEAN/DATE/NUMERIC/TEXT/URI
  regex_pattern TEXT,
  format_hint TEXT,
  is_unique BOOLEAN DEFAULT FALSE,
  unit_category_id BIGINT,         -- 数值型可选：引用 ont_unit_category.id，限定量纲/单位分类
  unit_ref_mode VARCHAR(32),       -- NONE/DICTIONARY_SYMBOL；measurementUnit 等 UNIT_REF 派生属性使用
  scope VARCHAR(16), namespace_id BIGINT, ontology_id BIGINT,
  created_at TIMESTAMP, updated_at TIMESTAMP
);
-- 枚举值
CREATE TABLE data_property_enum (
  data_property_id BIGINT REFERENCES data_property(id),
  value TEXT, PRIMARY KEY(data_property_id, value)
);

-- 单位分类（第6.2.5.6条测量单位落地）
CREATE TABLE ont_unit_category (
  id BIGSERIAL PRIMARY KEY,
  category_code VARCHAR(64) NOT NULL UNIQUE,
  category_name VARCHAR(64) NOT NULL,
  base_unit_symbol VARCHAR(32),
  is_builtin BOOLEAN DEFAULT FALSE,
  sort_order INTEGER DEFAULT 0,
  description TEXT,
  created_at TIMESTAMP, updated_at TIMESTAMP
);

-- 单位字典（供实例层 measurementUnit 引用，并供数据属性限定单位分类）
CREATE TABLE ont_unit (
  id BIGSERIAL PRIMARY KEY,
  category_id BIGINT REFERENCES ont_unit_category(id),
  unit_code VARCHAR(64) NOT NULL UNIQUE,
  unit_symbol VARCHAR(32) NOT NULL,
  unit_name VARCHAR(64) NOT NULL,
  is_base_unit BOOLEAN DEFAULT FALSE,
  factor NUMERIC(38,18),           -- 可选增强：换算乘系数 y=factor*x+offset（首期不预置）
  offset_value NUMERIC(38,18),     -- 可选增强：换算偏移（首期不预置）
  is_builtin BOOLEAN DEFAULT FALSE,
  scope VARCHAR(16), namespace_id BIGINT,
  sort_order INTEGER DEFAULT 0,
  description TEXT,
  created_at TIMESTAMP, updated_at TIMESTAMP
);

-- 对象属性
CREATE TABLE object_property (
  id BIGSERIAL PRIMARY KEY,
  iri TEXT NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  definition TEXT,
  domain_entity_type_id BIGINT REFERENCES entity_type(id),
  range_entity_type_id BIGINT REFERENCES entity_type(id),
  semantic_note TEXT,
  inverse_of_id BIGINT REFERENCES object_property(id),
  is_functional BOOLEAN, is_inverse_functional BOOLEAN,
  is_transitive BOOLEAN, is_symmetric BOOLEAN,
  scope VARCHAR(16), namespace_id BIGINT, ontology_id BIGINT,
  created_at TIMESTAMP, updated_at TIMESTAMP
);

-- 公理规则
CREATE TABLE axiom_rule (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  category VARCHAR(32) NOT NULL,  -- ENTITY_TYPE/PROPERTY/RELATION
  sub_type VARCHAR(64),
  description TEXT,
  owl_axiom TEXT,
  shacl_shape TEXT,
  is_enabled BOOLEAN DEFAULT TRUE,
  scope VARCHAR(16), ontology_id BIGINT,
  created_at TIMESTAMP, updated_at TIMESTAMP
);
CREATE TABLE axiom_rule_target (
  axiom_rule_id BIGINT REFERENCES axiom_rule(id),
  target_type VARCHAR(16),  -- ENTITY_TYPE/PROPERTY
  target_iri TEXT
);

-- 实体实例（元数据索引；实例三元组存于Triplestore）
CREATE TABLE entity_instance (
  id BIGSERIAL PRIMARY KEY,
  iri TEXT NOT NULL UNIQUE,
  rdf_type_iri TEXT NOT NULL,
  label TEXT,
  ontology_id BIGINT NOT NULL,
  created_at TIMESTAMP, updated_at TIMESTAMP
);

-- 实例层单位引用索引（真实三元组仍在Triplestore；本表便于校验与查询）
CREATE TABLE instance_unit_value (
  instance_iri TEXT NOT NULL,
  data_property_iri TEXT NOT NULL,       -- 如 measurementUnit
  unit_id BIGINT REFERENCES ont_unit(id),
  literal_symbol VARCHAR(32) NOT NULL,   -- 序列化字面量，如 %、V、W、℃
  ontology_id BIGINT NOT NULL,
  PRIMARY KEY(instance_iri, data_property_iri)
);

-- 本体工程
CREATE TABLE ontology_project (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  description TEXT,
  base_namespace_id BIGINT REFERENCES namespace(id),
  status VARCHAR(16),
  created_at TIMESTAMP, updated_at TIMESTAMP
);

-- 校验报告
CREATE TABLE validation_report (
  id BIGSERIAL PRIMARY KEY,
  ontology_id BIGINT NOT NULL,
  conforms BOOLEAN,
  triggered_at TIMESTAMP,
  result_json JSONB
);
```

### 7.2 Triplestore 存储

实例的数据属性值与对象属性关系以 RDF 三元组存入 Apache Jena TDB（默认）或 GraphDB（可选），支持 SPARQL 查询。关系库仅存索引与配置元数据，二者通过 IRI 关联。

> **选型决策与信创路径**（详见 §3.2.2 选型说明）：
> - **默认 Jena TDB**：开源Apache2，满足国标5.3条全部硬性要求（RDF/OWL/SHACL/Turtle序列化）；
> - **可选 GraphDB**：企业级RDF库，内置推理机更强，闭源商业，规模化部署时引入；
> - **Neo4j 不适用**：属性图赛道，无法满足国标5.3条RDF/SHACL/序列化硬性要求；
> - **信创替代**：远期可评估 gStore（北大开源RDF库），推理引擎通过 `ReasonerAdapter` 适配层解耦（§4.7.4），更换底层RDF库不影响上层校验逻辑。

---

## 8. 关键业务流程

### 8.1 本体建模主流程

```
新建本体工程
  → 配置命名空间
  → (预置核心Schema：实体类型/数据属性/对象属性/公理)
  → [可选]扩展行业子类与属性(扩展命名空间)
  → 创建实体对象实例(赋数据属性值、连对象属性)
  → 执行公理与SHACL校验
  → 序列化导出(Turtle/JSON-LD)
```

### 8.2 实例创建校验流程

```
用户填写实例表单
  → 前端实时校验值域(枚举/正则/日期/数值/单位引用)
  → 提交后端
  → 后端校验rdf.type合法性、对象属性域/值域、单位字典引用有效性
  → 触发SHACL校验(唯一性/不相交/功能属性/层次/日期/单位一致性)
  → 校验通过 → 持久化(关系库索引+Triplestore三元组)
  → 校验失败 → 返回校验报告，前端高亮违规项
```

### 8.3 扩展流程

```
创建行业扩展模块(新命名空间)
  → 在核心实体类型下新增子类(扩展命名空间IRI)
  → 新增扩展数据属性/对象属性(扩展命名空间)
  → 扩展合法性校验(不与核心矛盾、不修改核心)
  → 模块化导出
```

---

## 9. 非功能性需求

| 维度 | 要求 |
|------|------|
| 性能 | 单本体工程实例数≤10万时，全量SHACL校验≤30s；图谱渲染≤2s |
| 并发 | 支持50并发用户在线建模 |
| 安全 | RBAC权限（建模员/审核员/管理员）；操作审计日志 |
| 兼容 | 支持主流浏览器（Chrome/Edge/Firefox最新版） |
| 可用性 | 99.5% |
| 数据备份 | 每日自动备份关系库与Triplestore |
| 国际化 | 前端中英文双语；标签支持多语言（附录A.1/A.2要求） |
| 可维护性 | 五大模块代码独立部署、独立测试；单位字典作为基础字典独立维护 |

---

## 10. 权限设计

| 角色 | 权限 |
|------|------|
| 管理员 | 全部功能、命名空间管理、用户管理、核心Schema维护 |
| 建模工程师 | 实体类型/属性/公理扩展、实例建模、校验、导出 |
| 审核员 | 查看全部、执行校验、审批导出 |
| 访客 | 只读查看与图谱浏览 |

---

## 11. 里程碑与交付

| 阶段 | 交付物 | 周期 |
|------|--------|------|
| M1 基础平台 | 命名空间+实体类型+数据属性+对象属性+**单位字典**模块，预置核心Schema（含表C.41测量单位预置单位） | 第1-3周 |
| M2 公理与校验 | 公理规则模块+SHACL校验引擎，预置核心规则（含单位一致性约束） | 第4-5周 |
| M3 实例建模 | 实体对象实例模块+可视化图谱 | 第6-7周 |
| M4 序列化与扩展 | 多格式导入导出+扩展管理 | 第8周 |
| M5 附录D验收 | 完整复现附录D示例（含步骤5测量单位引用），全量校验通过 | 第9周 |
| M6 优化发布 | 性能优化、权限、国际化、文档 | 第10周 |

---

## 12. 风险与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| 核心命名空间域名未正式发布 | IRI需后续迁移 | 命名空间模块支持全局替换，IRI解耦存储 |
| 附录D中“条款”多继承语义复杂 | 建模歧义 | 使用两条直接父类关系表达多继承，并以推理测试验证；禁止误用等价类 |
| SHACL复杂规则性能瓶颈 | 全量校验慢 | 增量校验+异步全量校验+缓存 |
| 行业扩展不规范破坏核心 | 本体污染 | 扩展合法性强制校验+核心锁定只读 |
| Turtle格式IRI分隔符差异 | 导出比对不一致 | 提供规范化导出选项 |
| 推理能力与国标要求边界模糊 | §1.3非目标与§4.7 OWL公理检测矛盾 | 已通过§1.3边界澄清+§4.7.4 `ReasonerAdapter` 适配层解耦，轻量公理检测内置、大规模推理预留扩展 |
| 三元组存储信创不达标 | Jena TDB/GraphDB均为海外产品 | `ReasonerAdapter`适配层屏蔽底层差异；远期可替换gStore等国产RDF库，上层校验零改动（§3.2.2/§7.2） |

---

## 13. 术语映射表（国标条款→平台模块）

| 国标条款 | 内容 | 平台模块 |
|----------|------|----------|
| 5.2 本体核心组成 | 实体类型/数据属性/对象属性/公理 | 实体类型/数据属性/对象属性/公理 四模块 |
| 5.3 形式化要求 | OWL/RDF/SHACL/Turtle/JSON-LD | 序列化模块+校验引擎 |
| 5.4 命名方式 | 命名空间+IRI | 命名空间模块 |
| 6 实体类型 | 18核心实体类型 | 实体类型模块(预置) |
| 7.2 数据属性 | 47核心数据属性 | 数据属性模块(预置) |
| 7.3 对象属性 | 34核心对象属性 | 对象属性模块(预置) |
| 8 公理与规则 | 三类核心规则 | 公理规则模块(预置)+校验引擎 |
| 9 扩展方式 | 扩展原则 | 扩展管理模块 |
| 附录A 元数据描述项 | 8项实体类型/7项属性/5种数据类型 | 各模块元数据字段 |
| 附录B 核心实体类型定义 | 18类型详情 | 实体类型模块预置数据 |
| 附录C 数据属性定义 | 47属性详情 | 数据属性模块预置数据 |
| 6.2.5.6 约束逻辑类属性(含测量单位) | 约束类型/最大值/最小值/阈值范围/测量单位 | 约束逻辑类数据属性+**单位字典模块(§4.11)** |
| 附录C 表C.41 测量单位 | `unit`（标识符/名称，定义域Constraint，值域xsd:string，枚举V/W/℃等）；附录D示例使用 `measurementUnit` | **单位字典模块(预置国标单位)**+UNIT_REF派生值域+实例层单位字典引用+别名映射 |
| 附录D 实例化示例 | GB/T31486实例化(步骤5使用measurementUnit"%"） | 实体对象实例模块+序列化导出+单位字典引用 |

---

## 14. 附录

### 14.1 附录A.1 实体类型元数据描述项对照

| 序号 | 描述项 | 英文名 | 平台字段 |
|------|--------|--------|----------|
| 1 | 标识符 | IRI | entity_type.iri |
| 2 | 名称 | Name | entity_type.name |
| 3 | 标签 | Label | entity_type_label |
| 4 | 定义 | Definition | entity_type.definition |
| 5 | 属性集 | Properties | data_property(domain=本类型) |
| 6 | 父类 | Subclassof | entity_type_hierarchy |
| 7 | 子类 | HasSubclass | entity_type_hierarchy |
| 8 | 等价类 | EquivalentClass | equivalent_class |

### 14.2 附录A.2 属性元数据描述项对照

| 序号 | 描述项 | 英文名 | 平台字段 |
|------|--------|--------|----------|
| 1 | 标识符 | IRI | *.iri |
| 2 | 名称 | Name | *.name |
| 3 | 标签 | Label | *_label |
| 4 | 定义 | Definition | *.definition |
| 5 | 定义域 | Domain | *.domain_entity_type_id |
| 6 | 值域 | Range | *.range / base_type |
| 7 | 属性类型 | TypeofTerms | owl:ObjectProperty / owl:DatatypeProperty |

### 14.3 附录A.3 基本数据类型对照

| 数据类型 | 平台枚举 | XSD映射 |
|----------|----------|---------|
| 布尔型 | BOOLEAN | xsd:boolean |
| 日期 | DATE | 平台规范值YYYYMMDD；导出RDF时映射为xsd:date (YYYY-MM-DD) |
| 数值型 | NUMERIC | xsd:integer/xsd:decimal |
| 文本型 | TEXT | xsd:string |
| 统一资源链接 | URI | xsd:anyURI (文本型派生) |

---

> **文档结束**
>
> 本PRD严格依据 GB/T 48000.3—2026 全部条款设计，核心设计原则为"功能充分解耦"——实体类型、数据属性、对象属性、公理规则、实体实例五大模块独立管理、松耦合组合，通过IRI引用关联，确保标准化、可扩展、可校验、可交换，并以附录D实例化示例的配置化复现作为核心验收标准。其中第6.2.5.6条约束逻辑类的"测量单位"数据属性（附录C表C.41）独立为单位字典模块（§4.11）统一治理，供数值型数据属性的单位分类约束、实例层 measurementUnit 引用与公理校验使用，是支撑附录D步骤5实例化的基础字典。
