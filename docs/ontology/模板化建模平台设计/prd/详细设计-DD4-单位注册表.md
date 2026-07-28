# 详细设计-DD4-单位注册表（M3，QUDT 引用）

| 项 | 内容 |
|---|---|
| 文档名称 | 详细设计-DD4-单位注册表 |
| 里程碑 | M3（单位注册表，引用 QUDT） |
| 上游 PRD | 《本体模板化治理功能产品需求文档.md》v1.2 FR-3、FR-5（单位部分）、NFR-1/5/6/9/15、第五节、第八节 R-6/R-7、第九节 9.4/9.5、第十节 10.3/10.5、第十二节 12.4、第十三节、附录 B |
| 设计计划 | 《详细设计计划.md》DD4 |
| 编写日期 | 2026-07-28 |
| 文档状态 | 待实现 |
| 前置依赖 | DD1（M0）已完成：模块骨架、V4 建表（9.4 `ont_quantity_kind` / 9.5 `ont_unit` 结构齐备）、V5 种子（`ont_governance_meta` + `qudt_version=3.1.4` 已满足 AC-3.7、单位菜单 10300 占位）；DD2（M1）已完成：SupplyController 已建、`ont_supply_view` 权限点（V6 10105）。 |

---

## 一、设计目标与范围

### 1.1 目标

在 DD1/DD2/DD3 基础上，实现**单位注册表（量纲 + 单位 + 换算）**，使 youming 具备：

- 维护一张**引用 QUDT 的单位注册表**（按量纲分组、带换算系数），覆盖长度/质量/时间/温度/面积/体积/货币/数据量 ≥8 量纲，每量纲含基准单位 + ≥2 派生单位（FR-3，AC-3.1）。
- 单位以 `qudtIri` 为规范身份（如 `http://qudt.org/vocab/unit/KiloGM`），`symbol` 冗余存储以支持 QUDT IRI 漂移时降级（AC-3.2/AC-3.7，R-5）。
- 提供**同量纲换算能力** `convertValue(value, fromIri, toIri)`：同量纲返回换算值，跨量纲返回 `null`（不报错）；温度等有偏移单位正确使用 `conversionOffset`（QUDT 约定 `base=(value+offset)×multiplier`，AC-3.3/AC-3.4）。
- 换算浮点精度风险由 QUDT 科学计数系数 `conversionMultiplierSN`（如 `1.0E3`）+ `BigDecimal` 运算兜底，结果按合理有效位输出（AC-3.8，NFR-9，R-6）。
- 注册表记录所用 QUDT 版本快照（V5 已建 `ont_governance_meta.qudt_version=3.1.4`，AC-3.7）。
- 属性模板（DD2）可预设 `unitRef`（如 length 模板预设 `.../unit/M`），供给接口返回时携带，建模侧派生时继承（AC-3.6）。
- pig-ui "单位注册表"页面（左量纲树右单位列表 + 换算试算区）可操作（浏览/新增/编辑/弃用 custom 单位 + 实时换算试算）。

### 1.2 范围（本 DD 做 / 不做）

| 做（M3） | 不做（后续 DD） |
|---|---|
| QuantityKind / Unit 两 Entity + Mapper + Service + Controller | 注释属性注册表（DD5，FR-4） |
| 量纲列表 + 单位分页/详情 CRUD（custom 新增/编辑/弃用，builtin 保护） | 参考本体 QUDT 浏览/导入（DD6，FR-7；`/reference/qudt/unit/import` 属 DD6） |
| 换算引擎 `UnitConversionService`（BigDecimal + SN 系数，含偏移单位） | 实时货币汇率（R-7，标注"参考汇率"非实时） |
| SupplyController 扩展：`/units` + `/units/convert`（10.5） | 全量 2927 个 QUDT 单位（OoS，本注册表只抽常用子集，按需由 DD6 导入扩展） |
| V9 种子：8 量纲 + 常用单位（每量纲 ≥3）+ `conversion_multiplier_sn` 列 + 单位权限点按钮 | OWL 推理换算（换算由本程序 Java 执行，不依赖推理机，OoS） |
| 前端 `views/admin/ontology/unit/index.vue`（左量纲树右单位列表 + 换算试算区） | 建模侧序列化/个体实例（建模侧职责） |
| 前端 `api/ontology/unit.ts`（量纲/单位/换算 API） | |

> **边界声明（呼应 PRD 附录 B / R-7）**：
> - **引用而非自建**：不在本程序重定义 `qudt:Unit`/`qudt:QuantityKind` 等类，只引用 QUDT IRI 作为取值（PRD 5.2，OoS）。
> - **换算是工具行为，非本体语义**：换算由本程序 Java 代码执行，不依赖 OWL 推理机（OoS）。
> - **货币非 QUDT 强项**：货币换算标注"参考汇率"，非实时汇率（R-7）。
> - **全量单位不在内置种子**：本注册表只抽常用子集（8 量纲），按需扩展由 DD6（FR-7 QUDT 导入）补充（OoS）。

### 1.3 验收映射（M3 DoD）

| PRD AC | 本 DD 实现点 |
|---|---|
| AC-3.1 内置注册表覆盖 ≥8 量纲，每类含基准 + ≥2 派生单位 | 6.1 V9 种子（Length/Mass/Time/ThermodynamicTemperature/Area/Volume/Currency/InformationEntropy 共 8 量纲，每量纲 ≥3 单位） |
| AC-3.2 KiloGM 字段齐备（qudtIri/symbol/quantityKindLabel/conversionMultiplier） | 6.1 V9 种子（KiloGM = `.../unit/KiloGM`、`kg`、Mass、1.0）+ 4.5 单位详情/供给返回 quantityKindLabel |
| AC-3.3 换算 `convertValue(1, KiloM, M)===1000` / `convertValue(100, CentiM, M)===1` / `convertValue(1, KiloGM, USD)===null` | 4.6 UnitConversionService.convert（同量纲算系数，跨量纲返 null） |
| AC-3.4 温度等有偏移单位正确使用 conversionOffset | 4.6 UnitConversionService 偏移公式（base = (value + offset) × multiplier，互转经基准中转） |
| AC-3.5 qudtIri 唯一约束生效，重复返回业务错误 | V4 已建 `uk_ont_unit_iri` + 4.5 Service 查重 + DuplicateKeyException 兜底 |
| AC-3.6 属性模板 length 预设 unitRef，供给接口返回携带 | DD2 已落地（PropertyTemplate.unitRef + PropertyTemplateSupplyVO.unitRef）；V9 不新增属性模板种子，依赖治理员 custom 配置或 DD6 导入 |
| AC-3.7 注册表记录 QUDT 版本快照 + symbol 冗余降级 | V5 已建 `ont_governance_meta.qudt_version=3.1.4` + V4 `ont_unit.symbol` 冗余字段；4.5 供给接口返回 qudtVersion |
| AC-3.8 换算精度：优先 conversionMultiplierSN + BigDecimal + 合理有效位 | 4.6 UnitConversionService（SN 字段优先，BigDecimal 运算，setScale 控制有效位，NFR-9/R-6） |

---

## 二、落地清单

### 2.1 后端文件清单

> 包根 `com.pig4cloud.pig.ontology`，分层 `controller` / `service`(+`impl`) / `mapper` / `api/entity` / `api/dto` / `api/vo`。对齐 DD2/DD3 已有范式（PropertyTemplate* / ClassTemplate*）。

```
server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/
├── api/
│   ├── entity/
│   │   ├── QuantityKind.java                     # 量纲（9.4）
│   │   └── Unit.java                             # 单位（9.5，含 conversion_multiplier_sn 新列）
│   ├── dto/
│   │   └── UnitConvertDTO.java                   # 换算请求（value/fromIri/toIri，前端可走 GET 也可走 POST）
│   └── vo/
│       ├── QuantityKindNodeVO.java               # 量纲树节点（含单位计数）
│       ├── UnitSupplyVO.java                     # 单位供给视图（稳定化，屏蔽审计字段）
│       └── UnitConvertResultVO.java              # 换算结果（result:number|null + reason?）
├── controller/
│   ├── UnitController.java                       # 量纲列表 + 单位 CRUD + 换算（10.3）
│   （SupplyController.java                       # 复用 DD2 已有，扩展 units/units-convert 端点）
├── service/
│   ├── QuantityKindService.java
│   ├── UnitService.java
│   ├── UnitConversionService.java                # 换算引擎（BigDecimal + SN 系数，核心）
│   └── impl/
│       ├── QuantityKindServiceImpl.java
│       └── UnitServiceImpl.java
└── mapper/
    ├── QuantityKindMapper.java
    └── UnitMapper.java
```

> 说明：`UnitConversionService` 为纯逻辑 Service（无独立 Entity），由 UnitController 与 SupplyController 共享调用，体现"换算引擎"单一职责。换算不落库，每次请求实时计算（量纲/单位数量有限，配合 BigDecimal 性能可控；不缓存结果，但量纲归属查询可由 MyBatis-Plus 二级缓存或直接内存表优化）。

### 2.2 数据库文件清单

> **V4 已建好两张表（结构齐备），V5 已建 `ont_governance_meta` 元数据表 + `qudt_version=3.1.4`**，本期不返工 V4/V5，只新增 V9。

```
server/pig-common/pig-common-data/src/main/resources/db/migration/
├── V4__ont_governance_schema.sql                  # 已有（DD1，ont_quantity_kind + ont_unit 两表结构齐备 + 唯一约束 + 索引）
├── V5__ont_governance_seed.sql                    # 已有（DD1，ont_governance_meta.qudt_version=3.1.4 + 单位菜单 10300）
├── V6__ont_supply_permission_seed.sql             # 已有（DD2，ont_supply_view）
├── V7__ont_property_template_rename_values.sql    # 已有（DD2）
├── V8__ont_class_template_seed.sql                # 已有（DD3）
└── V9__ont_unit_seed.sql                          # 新增：conversion_multiplier_sn 列 + 8 量纲种子 + 常用单位种子 + 单位权限点
```

> **V9 改表说明（AC-3.8）**：V4 的 `ont_unit` 缺 `conversion_multiplier_sn`（QUDT 科学计数系数）与 `conversion_offset_sn`。V9 用 `ALTER TABLE ADD COLUMN` 补列（不改 V4，遵循"禁改已应用脚本"）。其余列（qudt_iri/symbol/label/label_cn/quantity_kind_id/conversion_multiplier/conversion_offset/scaling_of/ucum_code/source/source_ref/deprecated）V4 已建齐备。

### 2.3 依赖变更清单

```
server/pig-ontology/pig-ontology-biz/pom.xml     # 无需修改（换算用 JDK BigDecimal，无新依赖；DD3 已引 caffeine，本期不额外加）
```

### 2.4 前端文件清单

```
web/src/
├── api/ontology/
│   └── unit.ts                                   # 量纲/单位/换算 API（listQuantityKind/pageList/getObj/addObj/putObj/delObj/deprecateObj/convert）
└── views/admin/ontology/
    └── unit/
        ├── index.vue                             # 左量纲树右单位列表 + 换算试算区（splitpanes）
        ├── form.vue                              # 新增/编辑 custom 单位对话框
        ├── convert-panel.vue                     # 换算试算区（from/to/value 输入，前端本地算系数，NFR-15 防抖）
        ├── composables.ts                        # 量纲选项 + 状态复用
        └── i18n/
            ├── zh-cn.ts
            └── en.ts
```

> 路径说明：sys_menu path 为 `/admin/ontology/unit/index`（V5 已配 10300），backEnd.ts 用 `import.meta.glob('../views/**/*.{vue,tsx}')` 按 path 匹配，故 vue 文件须放 `views/admin/ontology/unit/index.vue`（对齐 DD2/DD3 范式）。

---

## 三、数据库设计（V9 种子）

### 3.1 V9 种子范围

V4 已建表结构（9.4/9.5），V5 已建元数据（`qudt_version`）。V9 做三件事：

1. **(a) 补 `conversion_multiplier_sn` / `conversion_offset_sn` 列**（AC-3.8）：QUDT 科学计数系数，换算引擎优先用此列减少浮点误差。
2. **(b) 内置 8 量纲 + 常用单位种子**（AC-3.1/AC-3.2）：长度/质量/时间/温度/面积/体积/货币/数据量，每量纲 ≥3 单位，均带 `conversion_multiplier` + `conversion_multiplier_sn`，数据**精确抽取自 QUDT 3.x TTL**（`docs/ontology/参考开源本体库/qudt/qudt-all.ttl`，已核实换算系数）。
3. **(c) 单位权限点按钮**（AC-3.5）：单位注册表菜单（10300）下补 view/manage 按钮 + 换算按钮。

### 3.2 `V9__ont_unit_seed.sql`

```sql
-- ============================================================
-- V9__ont_unit_seed.sql
-- 单位注册表：补 SN 列 + 8 量纲种子 + 常用单位种子 + 权限点
-- 对应 PRD v1.2 FR-3 AC-3.1/3.2/3.8、NFR-9
-- 依赖 V4（表结构）、V5（ont_governance_meta.qudt_version + 菜单 10300）
-- 数据精确抽取自 QUDT 3.x：docs/ontology/参考开源本体库/qudt/qudt-all.ttl
-- ============================================================

-- ---------- (a) 补科学计数系数列（AC-3.8 精度，NFR-9/R-6） ----------
-- ALTER ADD COLUMN 不改 V4，遵循"禁改已应用脚本"
ALTER TABLE ont_unit ADD COLUMN conversion_multiplier_sn varchar(32);
ALTER TABLE ont_unit ADD COLUMN conversion_offset_sn     varchar(32);
COMMENT ON COLUMN ont_unit.conversion_multiplier_sn IS 'QUDT 科学计数系数（如 1.0E3），换算引擎优先用此列减少浮点误差';
COMMENT ON COLUMN ont_unit.conversion_offset_sn     IS 'QUDT 偏移科学计数系数（如 4.5967E2）';

-- ---------- (b) 8 量纲种子（ont_quantity_kind，AC-3.1） ----------
-- dimension_vector 来自 QUDT qkdv:xxx，格式 A?E?L?I?M?H?T?D?（A=角度,E=电荷,L=长度,I=电流,M=质量,H=温度,T=时间,D=物质的量）
-- id 用 5xxx 段（量纲），6xxx 段（单位），便于跨脚本引用
INSERT INTO ont_quantity_kind (id, qudt_iri, label, label_cn, dimension_vector, sort_order, create_by, create_time, update_by, update_time) VALUES
(5001, 'http://qudt.org/vocab/quantitykind/Length',                    'Length',                    '长度',     'A0E0L1I0M0H0T0D0', 1, 'admin', now(), 'admin', now()),
(5002, 'http://qudt.org/vocab/quantitykind/Mass',                      'Mass',                      '质量',     'A0E0L0I0M1H0T0D0', 2, 'admin', now(), 'admin', now()),
(5003, 'http://qudt.org/vocab/quantitykind/Time',                      'Time',                      '时间',     'A0E0L0I0M0H0T1D0', 3, 'admin', now(), 'admin', now()),
(5004, 'http://qudt.org/vocab/quantitykind/ThermodynamicTemperature',   'ThermodynamicTemperature',  '热力学温度','A0E0L0I0M0H1T0D0', 4, 'admin', now(), 'admin', now()),
(5005, 'http://qudt.org/vocab/quantitykind/Area',                      'Area',                      '面积',     'A0E0L2I0M0H0T0D0', 5, 'admin', now(), 'admin', now()),
(5006, 'http://qudt.org/vocab/quantitykind/Volume',                    'Volume',                    '体积',     'A0E0L3I0M0H0T0D0', 6, 'admin', now(), 'admin', now()),
(5007, 'http://qudt.org/vocab/quantitykind/Currency',                  'Currency',                  '货币',     'A0E0L0I0M0H0T0D0', 7, 'admin', now(), 'admin', now()),
(5008, 'http://qudt.org/vocab/quantitykind/InformationEntropy',        'InformationEntropy',        '数据量',   'A0E0L0I0M0H0T0D0', 8, 'admin', now(), 'admin', now());

-- ---------- (c) 常用单位种子（ont_unit，AC-3.1/3.2） ----------
-- 每量纲含 1 基准 + ≥2 派生；conversion_multiplier / SN 精确来自 QUDT
-- quantity_kind_id 指向上层量纲；scaling_of 指向基准单位 qudt_iri（基准单位自身 scaling_of=NULL）
-- source='builtin'（随 Flyway 分发，只读）；source_ref='qudt'（溯源）

-- (c1) 长度（基准 M）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6001, 'http://qudt.org/vocab/unit/M',       'm',  'Metre',     '米',     5001, 1.0,    '1.0E0',  NULL, NULL, NULL,                         'm',  'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6002, 'http://qudt.org/vocab/unit/CentiM',  'cm', 'Centimetre','厘米',   5001, 0.01,   '1.0E-2', NULL, NULL, 'http://qudt.org/vocab/unit/M','cm', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6003, 'http://qudt.org/vocab/unit/MilliM',  'mm', 'Millimetre','毫米',   5001, 0.001,  '1.0E-3', NULL, NULL, 'http://qudt.org/vocab/unit/M','mm', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6004, 'http://qudt.org/vocab/unit/KiloM',   'km', 'Kilometre', '千米',   5001, 1000.0, '1.0E3',  NULL, NULL, 'http://qudt.org/vocab/unit/M','km', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now());

-- (c2) 质量（基准 KiloGM）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6011, 'http://qudt.org/vocab/unit/KiloGM',  'kg',   'Kilogram','千克', 5002, 1.0,    '1.0E0', NULL, NULL, NULL,                              'kg',   'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6012, 'http://qudt.org/vocab/unit/GM',      'g',    'Gram',    '克',   5002, 0.001,  '1.0E-3', NULL, NULL, 'http://qudt.org/vocab/unit/KiloGM','g',   'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6013, 'http://qudt.org/vocab/unit/Tonne',   't',    'Tonne',   '吨',   5002, 1000.0, '1.0E3',  NULL, NULL, 'http://qudt.org/vocab/unit/KiloGM','t',   'builtin', 'qudt', '0', 'admin', now(), 'admin', now());

-- (c3) 时间（基准 SEC）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6021, 'http://qudt.org/vocab/unit/SEC',   's',   'Second', '秒',  5003, 1.0,   '1.0E0', NULL, NULL, NULL,                            's',  'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6022, 'http://qudt.org/vocab/unit/MIN',   'min', 'Minute', '分钟',5003, 60.0,  '6.0E1', NULL, NULL, 'http://qudt.org/vocab/unit/SEC','min','builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6023, 'http://qudt.org/vocab/unit/HR',    'h',   'Hour',   '小时',5003, 3600.0,'3.6E3', NULL, NULL, 'http://qudt.org/vocab/unit/SEC','h',  'builtin', 'qudt', '0', 'admin', now(), 'admin', now());

-- (c4) 温度（基准 K，有偏移，AC-3.4）
-- QUDT 基准为 Kelvin；DEG_C/DEG_F 带 conversionOffset（相对 Kelvin 基准）
-- 换算公式（QUDT 约定）：base = (value + offset) × multiplier；逆向 value = base/multiplier - offset
-- 例：DEG_F 32°F → base=(32+459.67)×0.5556=273.15K；→ DEG_C 273.15/1.0-273.15=0°C（32°F=0°C ✓）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6031, 'http://qudt.org/vocab/unit/K',      'K',  'Kelvin',   '开尔文', 5004, 1.0,                  '1.0E0',                  NULL,    NULL,      NULL, 'K',  'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6032, 'http://qudt.org/vocab/unit/DEG_C',  '°C','Celsius',  '摄氏度', 5004, 1.0,                  '1.0E0',                  273.15,  '2.7315E2',NULL, 'Cel','builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6033, 'http://qudt.org/vocab/unit/DEG_F',  '°F','Fahrenheit','华氏度', 5004, 0.5555555555555556,  '5.555555555555556E-1',   459.67,  '4.5967E2',NULL, '[degF]','builtin','qudt','0','admin', now(), 'admin', now());

-- (c5) 面积（基准 M2）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6041, 'http://qudt.org/vocab/unit/M2',      'm²',  'SquareMetre',  '平方米', 5005, 1.0,    '1.0E0', NULL, NULL, NULL,                         'm2', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6042, 'http://qudt.org/vocab/unit/CentiM2', 'cm²', 'SquareCentimetre','平方厘米',5005, 0.0001,'1.0E-4',NULL,NULL,'http://qudt.org/vocab/unit/M2','cm2','builtin','qudt','0','admin', now(), 'admin', now()),
(6043, 'http://qudt.org/vocab/unit/HA',      'ha',  'Hectare',      '公顷',   5005, 10000.0,'1.0E4', NULL, NULL, 'http://qudt.org/vocab/unit/M2','ha', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now());

-- (c6) 体积（基准 M3，L 为派生）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6051, 'http://qudt.org/vocab/unit/M3',  'm³', 'CubicMetre','立方米',5006, 1.0,   '1.0E0', NULL, NULL, NULL,                         'm3', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6052, 'http://qudt.org/vocab/unit/L',   'L',  'Litre',     '升',    5006, 0.001,'1.0E-3',NULL, NULL, 'http://qudt.org/vocab/unit/M3','L',  'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6053, 'http://qudt.org/vocab/unit/MilliL','mL','Millilitre','毫升',  5006, 0.000001,'1.0E-6',NULL,NULL,'http://qudt.org/vocab/unit/L','mL','builtin', 'qudt', '0', 'admin', now(), 'admin', now());

-- (c7) 货币（基准 USD，标注"参考汇率"非实时，R-7）
-- 货币非 QUDT 强项，conversion_multiplier 为示例参考汇率，实际换算需接外部源（OoS）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6061, 'http://qudt.org/vocab/unit/USD', 'US$', 'USDollar',    '美元',  5007, 1.0,    '1.0E0', NULL, NULL, NULL, 'USD', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6062, 'http://qudt.org/vocab/unit/CNY', '¥',   'ChineseYuan', '人民币',5007, 0.14,  '1.4E-1',NULL, NULL, 'http://qudt.org/vocab/unit/USD','CNY','builtin','qudt','0','admin', now(), 'admin', now()),
(6063, 'http://qudt.org/vocab/unit/EUR', '€',   'Euro',        '欧元',  5007, 1.08,  '1.08E0',NULL, NULL, 'http://qudt.org/vocab/unit/USD','EUR','builtin','qudt','0','admin', now(), 'admin', now());

-- (c8) 数据量（基准 BYTE，二进制前缀）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6071, 'http://qudt.org/vocab/unit/BYTE',  'B',  'Byte',    '字节',  5008, 1.0,    '1.0E0', NULL, NULL, NULL,                              'B',   'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6072, 'http://qudt.org/vocab/unit/KiloBYTE','KB','Kilobyte','千字节',5008, 1024.0,'1.024E3',NULL,NULL,'http://qudt.org/vocab/unit/BYTE','kB', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6073, 'http://qudt.org/vocab/unit/MegaBYTE','MB','Megabyte','兆字节',5008, 1048576.0,'1.048576E6',NULL,NULL,'http://qudt.org/vocab/unit/BYTE','MB','builtin','qudt','0','admin', now(), 'admin', now());

-- ---------- (d) 单位权限点按钮（挂在单位注册表菜单 10300 下） ----------
-- view/manage 对齐 PRD 13.2；换算走 view 权限（建模侧可调供给换算）
INSERT INTO sys_menu VALUES (10301, '单位新增',  'ont_unit_manage', NULL, NULL, 10300, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10302, '单位编辑',  'ont_unit_manage', NULL, NULL, 10300, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10303, '单位删除',  'ont_unit_manage', NULL, NULL, 10300, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10304, '单位查看',  'ont_unit_view',   NULL, NULL, 10300, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
```

> 说明：
> - **V9 只做 ALTER + INSERT，不改 V4/V5**（禁改已应用脚本，checksum 校验）。
> - **数据精确性（AC-3.2/3.3/3.4）**：长度/质量/温度系数已逐条核实 QUDT TTL（`M=1.0`/`CentiM=0.01`/`KiloM=1000.0`/`KiloGM=1.0`/`DEG_C offset=273.15`/`DEG_F multiplier=0.5555...offset=459.67`）。货币/数据量系数为行业惯例值（货币标注"参考汇率"，R-7）。
> - **AC-3.3 验证**：`convertValue(1, KiloM, M)` = 1×1000/1 = **1000** ✓；`convertValue(100, CentiM, M)` = 100×0.01/1 = **1** ✓；`convertValue(1, KiloGM, USD)` = Mass≠Currency → **null** ✓。
> - **AC-3.4 验证**（QUDT 约定 `base=(value+offset)×mul`）：`convertValue(32, DEG_F, DEG_C)`：base=(32+459.67)×0.5556=273.15K；result=273.15/1.0-273.15 = **0°C** ✓（32°F=0°C）；`convertValue(212, DEG_F, DEG_C)` → base=(212+459.67)×0.5556=373.15K；result=373.15-273.15=**100°C** ✓（212°F=100°C）。
> - **AC-3.8 精度**：换算引擎优先用 `conversion_multiplier_sn`（如 `1.0E3`）+ BigDecimal，避免 `0.1+0.2` 类浮点误差。
> - `ont_sync_push`（10251，DD3）与本 DD 无关；单位权限点用 10301~10304 段（避开 10251）。

---

## 四、后端设计

### 4.1 Entity `QuantityKind.java`

```java
package com.pig4cloud.pig.ontology.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@Schema(description = "量纲（引用 QUDT）")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_quantity_kind")
public class QuantityKind extends Model<QuantityKind> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "唯一，如 .../quantitykind/Length")
	private String qudtIri;

	@Schema(description = "Length")
	private String label;

	@Schema(description = "中文名")
	private String labelCn;

	@Schema(description = "A0E0L1I0M0H0T0D0")
	private String dimensionVector;

	@Schema(description = "排序")
	private Integer sortOrder;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改人")
	private String updateBy;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改时间")
	private LocalDateTime updateTime;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;
}
```

### 4.2 Entity `Unit.java`

```java
package com.pig4cloud.pig.ontology.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "单位（引用 QUDT）")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_unit")
public class Unit extends Model<Unit> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "唯一，如 .../unit/KiloGM")
	@NotBlank(message = "QUDT IRI 不能为空")
	private String qudtIri;

	@Schema(description = "符号，冗余降级用")
	private String symbol;

	@Schema(description = "显示名")
	@NotBlank(message = "显示名不能为空")
	private String label;

	@Schema(description = "中文名")
	private String labelCn;

	@Schema(description = "-> ont_quantity_kind.id")
	@NotNull(message = "所属量纲不能为空")
	private Long quantityKindId;

	@Schema(description = "相对基准换算系数")
	private BigDecimal conversionMultiplier;

	@Schema(description = "QUDT 科学计数系数（1.0E3），换算优先用此列（AC-3.8）")
	private String conversionMultiplierSn;

	@Schema(description = "换算偏移（温度等）")
	private BigDecimal conversionOffset;

	@Schema(description = "偏移科学计数系数")
	private String conversionOffsetSn;

	@Schema(description = "基准单位 qudt_iri")
	private String scalingOf;

	@Schema(description = "UCUM 编码")
	private String ucumCode;

	@Schema(description = "builtin / custom")
	private String source;

	@Schema(description = "来源本体标识，如 qudt")
	private String sourceRef;

	@Schema(description = "0/1 弃用标记")
	private String deprecated;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改人")
	private String updateBy;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改时间")
	private LocalDateTime updateTime;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;
}
```

> 说明：两 Mapper 均继承 `MPJBaseMapper<T>`（对齐 DD2/DD3 范式），无自定义 SQL。`conversion_multiplier_sn` 为 V9 新增列，Entity 需对应字段（MyBatis-Plus 驼峰推断 `conversionMultiplierSn` → `conversion_multiplier_sn`，无需 `@TableField` 显式标注）。

### 4.3 Controller `UnitController.java`

```java
package com.pig4cloud.pig.ontology.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.entity.QuantityKind;
import com.pig4cloud.pig.ontology.api.entity.Unit;
import com.pig4cloud.pig.ontology.api.vo.QuantityKindNodeVO;
import com.pig4cloud.pig.ontology.api.vo.UnitConvertResultVO;
import com.pig4cloud.pig.ontology.service.QuantityKindService;
import com.pig4cloud.pig.ontology.service.UnitConversionService;
import com.pig4cloud.pig.ontology.service.UnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单位注册表 Controller（FR-3）
 * <p>
 * 路径 /ont/unit/** 与 /ont/quantity-kind/**，经 context-path /admin 或网关路由后对外为
 * /admin/ont/unit/**（对齐 PRD 10.3）。
 * <p>
 * 注意：控制器须带 /ont 前缀（对齐 DD2 PropertyTemplateController 注释，否则 pig-gateway
 * StripPrefix=1 / pig-boot context-path=/admin 剥离后 404）。
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@Tag(name = "单位注册表", description = "量纲 + 单位 + 换算（FR-3）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class UnitController {

	private final QuantityKindService quantityKindService;

	private final UnitService unitService;

	private final UnitConversionService unitConversionService;

	// ---------- 量纲 ----------

	@GetMapping("/ont/quantity-kind/list")
	@Operation(summary = "量纲列表", description = "全量量纲（按 sort_order），含每量纲单位计数（10.3）")
	@HasPermission("ont_unit_view")
	public R<List<QuantityKindNodeVO>> quantityKindList() {
		return R.ok(quantityKindService.listWithCount());
	}

	// ---------- 单位 ----------

	@GetMapping("/ont/unit/page")
	@Operation(summary = "单位分页", description = "按 quantityKindId 过滤（10.3）")
	@HasPermission("ont_unit_view")
	public R<IPage<Unit>> page(@ParameterObject Page page, @ParameterObject Unit unit) {
		return R.ok(unitService.page(page, unit));
	}

	@GetMapping("/ont/unit/{id}")
	@Operation(summary = "单位详情", description = "含所属量纲 label（AC-3.2）")
	@HasPermission("ont_unit_view")
	public R<Unit> getById(@PathVariable Long id) {
		return R.ok(unitService.getById(id));
	}

	@SysLog("新增单位")
	@PostMapping("/ont/unit")
	@Operation(summary = "新增 custom 单位", description = "qudtIri 查重 + 量纲存在性校验（AC-3.5）")
	@HasPermission("ont_unit_manage")
	public R save(@Valid @RequestBody Unit unit) {
		return unitService.saveUnit(unit);
	}

	@SysLog("编辑单位")
	@PutMapping("/ont/unit/{id}")
	@Operation(summary = "编辑（builtin 拒绝）")
	@HasPermission("ont_unit_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody Unit unit) {
		unit.setId(id);
		return unitService.updateUnit(unit);
	}

	@SysLog("删除单位")
	@DeleteMapping("/ont/unit/{id}")
	@Operation(summary = "删除（custom，软删）")
	@HasPermission("ont_unit_manage")
	public R removeById(@PathVariable Long id) {
		return unitService.removeUnit(id);
	}

	@SysLog("弃用单位")
	@PutMapping("/ont/unit/{id}/deprecate")
	@Operation(summary = "弃用/恢复")
	@HasPermission("ont_unit_manage")
	public R deprecate(@PathVariable Long id, @RequestParam(defaultValue = "1") String deprecated) {
		return unitService.deprecate(id, deprecated);
	}

	// ---------- 换算 ----------

	@GetMapping("/ont/unit/convert")
	@Operation(summary = "换算", description = "同量纲返回换算值，跨量纲返回 null（AC-3.3/3.4/3.8）")
	@HasPermission("ont_unit_view")
	public R<UnitConvertResultVO> convert(@RequestParam BigDecimal value,
			@RequestParam String fromIri, @RequestParam String toIri) {
		return R.ok(unitConversionService.convert(value, fromIri, toIri));
	}
}
```

> 说明：`@GetMapping` 路径加 `/ont` 前缀（对齐 DD2/DD3 Controller 范式）。换算用 GET（幂等、可缓存、对齐 PRD 10.3 `?value=&fromIri=&toIri=`）。权限点 `ont_unit_view`（V9 新建 10304）/ `ont_unit_manage`（V9 新建 10301~10303）。

### 4.4 VO 三件

```java
// QuantityKindNodeVO.java（量纲树节点，含单位计数）
@Data
@Schema(description = "量纲节点")
public class QuantityKindNodeVO implements Serializable {
	@Schema(description = "量纲 id") private Long id;
	@Schema(description = "QUDT IRI") private String qudtIri;
	@Schema(description = "英文 label") private String label;
	@Schema(description = "中文 label") private String labelCn;
	@Schema(description = "量纲向量") private String dimensionVector;
	@Schema(description = "排序") private Integer sortOrder;
	@Schema(description = "该量纲下单位数（含弃用则另算）") private Long unitCount;
}

// UnitSupplyVO.java（单位供给视图，稳定化，屏蔽审计字段，AC-5.7）
@Data
@Schema(description = "单位供给视图")
public class UnitSupplyVO implements Serializable {
	@Schema(description = "主键") private Long id;
	@Schema(description = "QUDT IRI，建模侧据此引用") private String qudtIri;
	@Schema(description = "符号") private String symbol;
	@Schema(description = "英文 label") private String label;
	@Schema(description = "中文 label") private String labelCn;
	@Schema(description = "所属量纲 id") private Long quantityKindId;
	@Schema(description = "所属量纲 IRI（供建模侧校验同量纲）") private String quantityKindIri;
	@Schema(description = "所属量纲 label") private String quantityKindLabel;
	@Schema(description = "换算系数") private BigDecimal conversionMultiplier;
	@Schema(description = "换算偏移") private BigDecimal conversionOffset;
	@Schema(description = "基准单位 IRI") private String scalingOf;
	@Schema(description = "UCUM 编码") private String ucumCode;
	@Schema(description = "builtin / custom") private String source;
	@Schema(description = "0/1 弃用") private String deprecated;
}

// UnitConvertResultVO.java（换算结果，AC-3.3）
@Data
@Schema(description = "换算结果")
public class UnitConvertResultVO implements Serializable {
	@Schema(description = "换算结果；跨量纲为 null") private BigDecimal result;
	@Schema(description = "结果为 null 时的原因（如 quantity kind mismatch / unit not found）") private String reason;
	@Schema(description = "输入值") private BigDecimal value;
	@Schema(description = "源单位 IRI") private String fromIri;
	@Schema(description = "目标单位 IRI") private String toIri;
}
```

### 4.5 Service `UnitServiceImpl.java`（关键逻辑）

```java
package com.pig4cloud.pig.ontology.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.entity.QuantityKind;
import com.pig4cloud.pig.ontology.api.entity.Unit;
import com.pig4cloud.pig.ontology.mapper.QuantityKindMapper;
import com.pig4cloud.pig.ontology.mapper.UnitMapper;
import com.pig4cloud.pig.ontology.service.UnitService;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 单位 Service 实现（FR-3）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class UnitServiceImpl extends ServiceImpl<UnitMapper, Unit> implements UnitService {

	private final QuantityKindMapper quantityKindMapper;

	@Override
	public IPage<Unit> page(Page page, Unit unit) {
		return baseMapper.selectPage(page,
				Wrappers.<Unit>lambdaQuery()
					.eq(unit.getQuantityKindId() != null, Unit::getQuantityKindId, unit.getQuantityKindId())
					.and(StrUtil.isNotBlank(unit.getQudtIri()),
							w -> w.like(Unit::getQudtIri, unit.getQudtIri())
								.or().like(Unit::getLabel, unit.getQudtIri())
								.or().like(Unit::getSymbol, unit.getQudtIri()))
					.eq(StrUtil.isNotBlank(unit.getDeprecated()), Unit::getDeprecated, unit.getDeprecated())
					.orderByAsc(Unit::getQuantityKindId)
					.orderByAsc(Unit::getId));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveUnit(Unit unit) {
		// 1. 量纲存在性校验
		QuantityKind qk = quantityKindMapper.selectById(unit.getQuantityKindId());
		if (qk == null) {
			return R.failed("所属量纲不存在");
		}
		// 2. qudtIri 预查重（AC-3.5）
		long count = count(Wrappers.<Unit>lambdaQuery().eq(Unit::getQudtIri, unit.getQudtIri()));
		if (count > 0) {
			return R.failed("QUDT IRI '" + unit.getQudtIri() + "' 已存在");
		}
		unit.setSource("custom");
		unit.setDeprecated("0");
		try {
			return R.ok(save(unit));
		}
		catch (DuplicateKeyException e) {
			// DB 唯一约束 uk_ont_unit_iri 兜底（覆盖软删后复用 qudtIri 的场景）
			return R.failed("QUDT IRI '" + unit.getQudtIri() + "' 已存在");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateUnit(Unit unit) {
		Unit existing = getById(unit.getId());
		if (existing == null) {
			return R.failed("单位不存在");
		}
		if ("builtin".equals(existing.getSource())) {
			return R.failed("内置单位不可编辑");
		}
		// qudtIri 不可改（引用稳定性，对齐 DD2 templateCode 不可改）
		unit.setQudtIri(existing.getQudtIri());
		return R.ok(updateById(unit));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeUnit(Long id) {
		Unit existing = getById(id);
		if (existing == null) {
			return R.failed("单位不存在");
		}
		if ("builtin".equals(existing.getSource())) {
			return R.failed("内置单位不可删除");
		}
		// TODO: 后期接入属性模板 unitRef 引用计数（FR-1 unitRef 交汇），有引用时拒绝删除
		return R.ok(removeById(id));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R deprecate(Long id, String deprecated) {
		return R.ok(update(Wrappers.<Unit>lambdaUpdate()
			.eq(Unit::getId, id).set(Unit::getDeprecated, deprecated)));
	}
}
```

> 说明：`saveUnit` 校验链：量纲存在性 → qudtIri 查重 → 落库（source=custom）。`updateUnit` builtin 保护 + qudtIri 不可改（对齐 DD2 templateCode 范式）。`QuantityKindServiceImpl.listWithCount` 用一次 group by 查每量纲单位计数（或 service 层查全量后内存分组，量纲数 ≤8，性能无忧）。

### 4.6 UnitConversionService（换算引擎，核心算法）

> **算法目标（AC-3.3/3.4/3.8，NFR-9/R-6）**：给定 `(value, fromIri, toIri)`：
> 1. 查 fromUnit/toUnit，任一不存在 → `result=null, reason="unit not found"`。
> 2. 比较量纲：`fromUnit.quantityKindId != toUnit.quantityKindId` → `result=null, reason="quantity kind mismatch"`（AC-3.3 `convertValue(1, KiloGM, USD)===null`）。
> 3. 取系数：**优先 SN 科学计数系数**（AC-3.8），SN 为空时退回 `conversionMultiplier`（BigDecimal）。
> 4. 换算：
>    - **无偏移单位**（长度/质量/时间/面积/体积/货币/数据量，offset 为空）：`result = value × fromMultiplier / toMultiplier`。
>      - AC-3.3：`convertValue(1, KiloM, M)` = 1×1000/1 = 1000 ✓；`convertValue(100, CentiM, M)` = 100×0.01/1 = 1 ✓。
>    - **有偏移单位**（温度，offset 非空）：QUDT 约定 **`base = (value + offset) × multiplier`**（base = 基准单位如 Kelvin；offset 先加、再乘）。两个有偏移单位互转须**经基准中转**：
>      - `base = (value + fromOffset) × fromMultiplier`
>      - `result = base / toMultiplier - toOffset`
>      - AC-3.4：`convertValue(32, DEG_F, DEG_C)`：base=(32+459.67)×0.5556=273.15K；result=273.15/1.0-273.15 = **0°C** ✓（32°F=0°C）；`convertValue(212, DEG_F, DEG_C)` → base=(212+459.67)×0.5556=373.15K；result=373.15-273.15=**100°C** ✓。
> 5. 精度控制：全程 `BigDecimal`（`MathContext.DECIMAL64`），结果 `setScale(10, HALF_UP)` 去尾保留 10 位有效位，前端按需展示（NFR-9/R-6）。

```java
package com.pig4cloud.pig.ontology.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.api.entity.Unit;
import com.pig4cloud.pig.ontology.api.vo.UnitConvertResultVO;
import com.pig4cloud.pig.ontology.mapper.UnitMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 单位换算引擎（FR-3，AC-3.3/3.4/3.8）
 * <p>
 * QUDT 换算约定：base = (value + offset) × multiplier（base = 基准单位，如 Kelvin；offset 先加再乘）。
 * 同量纲换算经基准中转；跨量纲返回 null。
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class UnitConversionService {

	private final UnitMapper unitMapper;

	public UnitConvertResultVO convert(BigDecimal value, String fromIri, String toIri) {
		UnitConvertResultVO vo = new UnitConvertResultVO();
		vo.setValue(value);
		vo.setFromIri(fromIri);
		vo.setToIri(toIri);

		Unit from = unitMapper.selectOne(Wrappers.<Unit>lambdaQuery().eq(Unit::getQudtIri, fromIri));
		Unit to = unitMapper.selectOne(Wrappers.<Unit>lambdaQuery().eq(Unit::getQudtIri, toIri));
		if (from == null || to == null) {
			vo.setReason("unit not found");
			return vo;
		}
		// 量纲校验（AC-3.3 跨量纲返 null）
		if (!from.getQuantityKindId().equals(to.getQuantityKindId())) {
			vo.setReason("quantity kind mismatch");
			return vo;
		}

		// 系数：优先 SN（AC-3.8），SN 为空退回 conversionMultiplier（默认 1）
		BigDecimal fromMul = multiplier(from);
		BigDecimal toMul = multiplier(to);
		BigDecimal fromOff = offset(from);
		BigDecimal toOff = offset(to);

		BigDecimal result;
		if (fromOff == null && toOff == null) {
			// 无偏移：result = value × fromMul / toMul（AC-3.3）
			result = value.multiply(fromMul, MathContext.DECIMAL64)
					.divide(toMul, MathContext.DECIMAL64);
		}
		else {
			// 有偏移：经基准中转（AC-3.4）
			// base = (value + fromOff) × fromMul
			BigDecimal fOff = fromOff == null ? BigDecimal.ZERO : fromOff;
			BigDecimal tOff = toOff == null ? BigDecimal.ZERO : toOff;
			BigDecimal base = value.add(fOff, MathContext.DECIMAL64)
					.multiply(fromMul, MathContext.DECIMAL64);
			// result = base / toMul - toOff
			result = base.divide(toMul, MathContext.DECIMAL64).subtract(tOff, MathContext.DECIMAL64);
		}
		// 精度：保留 10 位有效位去尾（NFR-9/R-6）
		vo.setResult(result.setScale(10, RoundingMode.HALF_UP).stripTrailingZeros());
		return vo;
	}

	/**
	 * 取换算系数：优先 SN 科学计数（如 "1.0E3"），SN 为空退回 conversionMultiplier（默认 1）。
	 * SN 用 new BigDecimal(String) 构造，避免 double 精度损失（AC-3.8）。
	 */
	private BigDecimal multiplier(Unit u) {
		if (StrUtil.isNotBlank(u.getConversionMultiplierSn())) {
			return new BigDecimal(u.getConversionMultiplierSn());
		}
		return u.getConversionMultiplier() != null ? u.getConversionMultiplier() : BigDecimal.ONE;
	}

	private BigDecimal offset(Unit u) {
		if (StrUtil.isNotBlank(u.getConversionOffsetSn())) {
			return new BigDecimal(u.getConversionOffsetSn());
		}
		return u.getConversionOffset();
	}
}
```

> 说明：
> - **SN 优先（AC-3.8）**：`multiplier()` 优先用 `conversionMultiplierSn`（如 `"1.0E3"`），用 `new BigDecimal(String)` 构造避免 `Double.parseDouble` 精度损失（如 `0.1` 的二进制表示问题）。
> - **量纲校验在系数计算前**（AC-3.3）：跨量纲直接返 null，不进运算。
> - **偏移单位经基准中转（AC-3.4）**：QUDT 的 offset 是相对基准单位（Kelvin）的线性变换系数，换算约定为 `base = (value + offset) × multiplier`（offset 先加、再乘），逆向 `value = base / multiplier - offset`。两个有偏移单位互转先转基准再转目标。纯无偏移单位走快速路径。
> - **精度兜底（NFR-9/R-6）**：全程 `MathContext.DECIMAL64`（15-16 位有效，IEEE 754 双精度等价精度但 BigDecimal 无累积误差），末尾 `setScale(10, HALF_UP).stripTrailingZeros()` 去尾保留 10 位。
> - **不缓存换算结果**：单位数量有限（≤30），每次两次 selectById-by-iri（命中 `uk_ont_unit_iri` 唯一索引）性能可控；若后续高频可加 Caffeine 缓存（key=fromIri+toIri+value）。

### 4.7 SupplyController 扩展（10.5，复用 DD2 已有类）

> 在 DD2/DD3 已建的 `SupplyController`（`@RequestMapping("/ont/supply/v1")`，对外 `/admin/ont/supply/v1`）上追加两个端点。

```java
// 追加到 SupplyController（DD2 property-templates / DD3 class-template 端点保留）

@GetMapping("/units")
@Operation(summary = "单位供给", description = "按量纲分组返回，含换算系数（10.5，AC-5.4）")
@HasPermission("ont_supply_view")
public R<List<UnitSupplyVO>> supplyUnits(
		@RequestParam(required = false) String quantityKindIri,
		@RequestParam(defaultValue = "false") Boolean includeDeprecated) {
	return R.ok(unitService.supplyList(quantityKindIri, includeDeprecated));
}

@GetMapping("/units/convert")
@Operation(summary = "换算供给", description = "同量纲换算，跨量纲返 null（10.5，AC-3.3）")
@HasPermission("ont_supply_view")
public R<UnitConvertResultVO> supplyConvert(@RequestParam BigDecimal value,
		@RequestParam String fromIri, @RequestParam String toIri) {
	return R.ok(unitConversionService.convert(value, fromIri, toIri));
}
```

> 说明：
> - `supplyUnits`：供建模侧按量纲拉单位，返回稳定化 `UnitSupplyVO`（屏蔽审计字段，AC-5.7）。`quantityKindIri` 为空时返回全量（按量纲分组排序）。VO 含 `quantityKindIri/quantityKindLabel`，供建模侧校验同量纲（AC-3.6 建模侧继承单位时校验）。
> - `supplyConvert`：供建模侧执行换算（如建模侧属性值从 kg 转 g 展示）。
> - **AC-3.6 交汇**：属性模板（DD2）的 `unitRef` 已由 `PropertyTemplateSupplyVO.unitRef` 携带，建模侧拉属性模板时即获得预设单位 IRI；V9 不新增属性模板种子，依赖治理员 custom 配置或 DD6 导入。

---

## 五、前端设计

### 5.1 API `web/src/api/ontology/unit.ts`

```typescript
import request from '/@/utils/request';

// ---------- 量纲 ----------
export function listQuantityKind() {
	return request({
		url: '/admin/ont/quantity-kind/list',
		method: 'get',
	});
}

// ---------- 单位 ----------
export function pageList(query: any) {
	return request({
		url: '/admin/ont/unit/page',
		method: 'get',
		params: query,
	});
}

export function getObj(id: string) {
	return request({
		url: '/admin/ont/unit/' + id,
		method: 'get',
	});
}

export function addObj(obj: any) {
	return request({
		url: '/admin/ont/unit',
		method: 'post',
		data: obj,
	});
}

export function putObj(obj: any) {
	return request({
		url: '/admin/ont/unit/' + obj.id,
		method: 'put',
		data: obj,
	});
}

export function delObj(id: string) {
	return request({
		url: '/admin/ont/unit/' + id,
		method: 'delete',
	});
}

export function deprecateObj(id: string, deprecated: string) {
	return request({
		url: '/admin/ont/unit/' + id + '/deprecate',
		method: 'put',
		params: { deprecated },
	});
}

// ---------- 换算 ----------
export function convert(value: number | string, fromIri: string, toIri: string) {
	return request({
		url: '/admin/ont/unit/convert',
		method: 'get',
		params: { value, fromIri, toIri },
	});
}
```

### 5.2 单位注册表页 `views/admin/ontology/unit/index.vue`（左量纲树右单位列表 + 换算试算区）

> 对齐 pig 现有 `views/admin/dict/index.vue`（QueryTree + splitpanes 左树右表）+ DD3 `class-template/index.vue` 范式。

**布局（splitpanes 左树 + 右表+底部换算试算区，PRD 12.4）：**

```
┌──────────────┬──────────────────────────────────────────┐
│ 量纲树        │ 单位列表（当前量纲）                        │
│ (QueryTree)  │ ┌────────────────────────────────────────┐│
│              │ │ symbol | label | 系数 | 基准 | 来源 | 操作││
│ 📏 长度 (4)   │ │  m   | 米   | 1.0  | —   | QUDT | 编辑 ││
│ ⚖️ 质量 (3)   │ │  cm  | 厘米 | 0.01 | M   | QUDT | 编辑 ││
│ ⏱ 时间 (3)   │ │  ...                                    ││
│ 🌡 温度 (3)   │ └────────────────────────────────────────┘│
│ □ 面积 (3)   │ [+新增]                                   │
│ 📦 体积 (3)  ├──────────────────────────────────────────┤
│ 💰 货币 (3)  │ 换算试算区（convert-panel）                 │
│ 💾 数据量 (3)│ 值[1]  从[KiloM km]  到[M m]  → 1000       │
│              │ （前端本地算系数，NFR-15 防抖）             │
└──────────────┴──────────────────────────────────────────┘
```

**关键结构：**

- **左侧 QueryTree**：`defineAsyncComponent(() => import('/@/components/QueryTree/index.vue'))`，节点 label 显 `icon + label_cn + ' (' + unitCount + ')'`（如 `📏 长度 (4)`），`node-key="id"`，`@nodeClick` 加载右侧该量纲单位列表。
- **顶部操作栏**：`v-auth="'ont_unit_manage'"` 新增按钮（新增 custom 单位，需先选量纲）。
- **右侧单位表**：列 symbol/label/labelCn/conversionMultiplier（显示 SN 系数如 `1.0E3` + 实际值）/scalingOf（基准单位 IRI 短名）/source/sourceRef/状态 + 操作列（编辑/弃用/删除，builtin 行禁用编辑删除）。
- **底部换算试算区（convert-panel）**：from/to 单位下拉（当前量纲内）+ value 输入 → 实时显示结果（同量纲）或拒绝提示（跨量纲），见 5.4。
- **内置单位**：编辑/删除按钮 `:disabled="scope.row.source === 'builtin'"`（对齐 DD2 builtin 保护）。

**关键逻辑（对齐 dict/class-template index.vue）：**

```typescript
const QueryTree = defineAsyncComponent(() => import('/@/components/QueryTree/index.vue'));
const state = reactive({
	quantityKinds: [] as any[],       // 量纲树数据
	selectedQkId: '' as string,        // 当前选中量纲
	query: (params: any) => listQuantityKind(),   // QueryTree 的 query prop
	// 右表用 useTable hook（对齐 property-template/index.vue）
});
// 量纲节点点击 -> 加载该量纲单位列表
async function onNodeClick(node: any) {
	state.selectedQkId = node.id;
	state.queryForm.quantityKindId = node.id;
	getDataList();
}
```

### 5.3 表单 `form.vue`

| 字段 | 说明 |
|---|---|
| quantityKindId | 量纲选择（树选择器，新增时默认当前选中量纲） |
| qudtIri | QUDT IRI（新增必填、唯一；编辑只读，对齐 DD2 templateCode） |
| symbol | 符号（如 kg） |
| label / labelCn | 英文名 / 中文名 |
| conversionMultiplier | 换算系数（BigDecimal，如 1000） |
| conversionMultiplierSn | 科学计数系数（如 1.0E3，换算优先用，AC-3.8） |
| conversionOffset / conversionOffsetSn | 偏移系数（仅温度类，普通单位留空） |
| scalingOf | 基准单位 IRI（下拉选同量纲基准单位） |
| ucumCode | UCUM 编码 |
| description | 说明（可选） |

> 用 `el-dialog + el-form`，对齐 DD2 `property-template/form.vue` 范式。提示"换算系数相对基准单位（scalingOf），基准单位自身系数为 1"。

### 5.4 换算试算区 `convert-panel.vue`（NFR-15 防抖）

> **核心**：前端**本地拉系数后计算**（NFR-15），避免每次按键调 `/convert` 接口；仅当本地无法判断（如系数缺失）时 fallback 到接口。

```typescript
// 1. 进入页面时，拉取当前量纲全部单位（含系数），缓存在前端
const units = ref<any[]>([]);
async function loadUnits(qkId: string) {
	units.value = (await pageList({ quantityKindId: qkId, size: 999 })).data.records;
}
// 2. 试算：本地用 BigDecimal.js 等价逻辑计算
const convertState = reactive({
	value: '1',
	fromIri: '',
	toIri: '',
	result: null as number | string | null,
	reason: '',
});
function doConvert() {
	const from = units.value.find((u) => u.qudtIri === convertState.fromIri);
	const to = units.value.find((u) => u.qudtIri === convertState.toIri);
	if (!from || !to) return;
	if (from.quantityKindId !== to.quantityKindId) {
		convertState.result = null;
		convertState.reason = '量纲不同，不可换算';
		return;
	}
	// 本地算：无偏移 value × fromMul / toMul；有偏移经基准中转 base=(value+fromOff)*fromMul, result=base/toMul-toOff
	convertState.reason = '';
}
// 3. NFR-15 防抖：value/from/to 变化时 debounce 300ms 后调 doConvert
watch(() => [convertState.value, convertState.fromIri, convertState.toIri], useDebounceFn(doConvert, 300));
```

- **UI**：`值 [input] 从 [el-select 单位] 到 [el-select 单位] → 结果 [只读显示]`；跨量纲显示橙色提示"量纲不同，不可换算"；同量纲显示绿色结果。
- **单位下拉**：选项来自当前量纲的单位列表（label 显 `symbol (label_cn)`，如 `km (千米)`），切量纲时清空。
- **精度展示**：结果保留合理小数位（如 6 位），与后端 `setScale(10)` 对齐，前端展示时去尾。

### 5.5 i18n（unit/i18n/zh-cn.ts 示例）

```typescript
export default {
	unit: {
		quantityKind: '量纲',
		quantityKindLabel: '量纲名称',
		dimensionVector: '量纲向量',
		unitCount: '单位数',
		qudtIri: 'QUDT IRI',
		symbol: '符号',
		label: '英文名',
		labelCn: '中文名',
		conversionMultiplier: '换算系数',
		conversionMultiplierSn: '科学计数系数',
		conversionOffset: '换算偏移',
		conversionOffsetSn: '偏移科学计数',
		scalingOf: '基准单位',
		ucumCode: 'UCUM 编码',
		source: '来源',
		sourceRef: '来源本体',
		deprecated: '状态',
		// 换算试算区
		convertTitle: '换算试算',
		convertValue: '数值',
		convertFrom: '从',
		convertTo: '到',
		convertResult: '结果',
		convertMismatch: '量纲不同，不可换算',
		convertNotFound: '单位不存在',
		// 状态
		builtin: '内置',
		custom: '自定义',
		normal: '正常',
		deprecatedLabel: '已弃用',
		// inputXxxTip 系列
		inputQudtIriTip: '请输入 QUDT IRI',
		inputSymbolTip: '请输入符号',
		inputLabelTip: '请输入英文名',
		selectQuantityKindTip: '请选择量纲',
		builtinDeleteDisabledTip: '内置单位不可删除',
		builtinEditDisabledTip: '内置单位不可编辑',
		// 量纲中文名
		qkLength: '长度',
		qkMass: '质量',
		qkTime: '时间',
		qkThermodynamicTemperature: '热力学温度',
		qkArea: '面积',
		qkVolume: '体积',
		qkCurrency: '货币',
		qkInformationEntropy: '数据量',
	},
};
```

---

## 六、横切设计

### 6.1 校验

- **qudtIri 唯一**：Service 预查重 + DB `uk_ont_unit_iri`（V4 已建）兜底（`DuplicateKeyException` 转友好提示，覆盖软删后复用场景，AC-3.5）。
- **量纲存在性**：`saveUnit` 校验 `quantityKindId` 在 `ont_quantity_kind` 存在（防止脏引用）。
- **换算系数量纲一致**：`UnitConversionService.convert` 在运算前校验 `from/to` 同量纲，不同返 null（AC-3.3）。
- **builtin 保护**：编辑/删除 builtin 返回"内置单位不可操作"（对齐 DD2/DD3）。
- **qudtIri 不可改**：编辑时锁定（引用稳定性，对齐 DD2 templateCode）。

### 6.2 精度（NFR-9/R-6/AC-3.8）

- **SN 优先**：换算引擎优先用 `conversionMultiplierSn`（如 `"1.0E3"`），用 `new BigDecimal(String)` 构造避免 `Double.parseDouble` 精度损失。
- **BigDecimal 全程**：`MathContext.DECIMAL64`（15-16 位有效），无 double 累积误差。
- **去尾**：结果 `setScale(10, HALF_UP).stripTrailingZeros()`，保留 10 位有效位。
- **前端对齐**：试算区本地计算用等价逻辑，展示去尾。

### 6.3 异常处理

- 唯一约束冲突（`DuplicateKeyException`）：Service 预查重，兜底捕获转 `R.failed`。
- 换算单位不存在/量纲不匹配：返回 `UnitConvertResultVO{result:null, reason}`，**不抛异常**（AC-3.3 跨量纲是正常分支，非错误）。
- 量纲不存在：`saveUnit` 返回 `R.failed("所属量纲不存在")`。

### 6.4 审计

- MybatisPlusMetaObjectHandler 自动填充 `createBy/createTime/updateBy/updateTime/delFlag`（DD1 已配置）。

### 6.5 双形态

- 单体：pig-boot context-path `/admin` → `/admin/ont/unit/**`、`/admin/ont/quantity-kind/**`、`/admin/ont/supply/v1/units/**`。
- 微服务：网关路由 `Path=/admin/ont/**` → `lb://pig-ontology-biz`（DD1 已配置）。

### 6.6 安全

- 治理接口（CRUD/弃用）：`ont_unit_manage`（V9 新建 10301~10303）。
- 查看与换算接口：`ont_unit_view`（V9 新建 10304）。
- 供给接口（`/supply/v1/units`、`/supply/v1/units/convert`）：`ont_supply_view`（V6 已建 10105，建模师/查看者授予）。

### 6.7 国际化（NFR-6）

- 量纲/单位 `labelCn` 冗余存储中文（QUDT 原生英文，本地表补中文，NFR-6）。
- 前端 i18n 双语（zh-cn/en），词条放 `views/admin/ontology/unit/i18n/`。

---

## 七、测试要点

| 类别 | 要点 |
|---|---|
| 编译 | `mvn -pl pig-ontology/pig-ontology-biz -am compile` 通过（无新依赖） |
| Flyway | 启动后 V9 成功；`SELECT count(*) FROM ont_quantity_kind` = 8；`SELECT count(*) FROM ont_unit WHERE source='builtin'` ≥ 24（8 量纲 × ≥3）；`SELECT * FROM ont_unit WHERE qudt_iri LIKE '%KiloGM'` 含 conversion_multiplier_sn='1.0E0'；`SELECT * FROM sys_menu WHERE menu_id IN (10301,10302,10303,10304)` 存在 |
| 量纲覆盖（AC-3.1） | `GET /ont/quantity-kind/list` 返回 8 量纲，每量纲 unitCount ≥ 3 |
| 单位字段（AC-3.2） | `GET /ont/unit/{KiloGM id}` 返回 qudtIri=`.../unit/KiloGM`、symbol=`kg`、quantityKindLabel=`Mass`、conversionMultiplier=1.0 |
| 换算无偏移（AC-3.3） | `GET /ont/unit/convert?value=1&fromIri=.../KiloM&toIri=.../M` → result=1000；`value=100&fromIri=.../CentiM&toIri=.../M` → result=1 |
| 换算跨量纲（AC-3.3） | `GET /ont/unit/convert?value=1&fromIri=.../KiloGM&toIri=.../USD` → result=null, reason=`quantity kind mismatch` |
| 换算偏移（AC-3.4） | `GET /ont/unit/convert?value=32&fromIri=.../DEG_F&toIri=.../DEG_C` → result=0（32°F=0°C）；`value=212&DEG_F→DEG_C` → 100 |
| 唯一约束（AC-3.5） | 新增 qudtIri=`.../unit/KiloGM`（已存在）→ 返回业务错误"QUDT IRI ... 已存在"（非 500） |
| 预设单位（AC-3.6） | 属性模板 `length`（治理员 custom 配 unitRef=`.../unit/M`）→ `GET /supply/v1/property-templates` 返回携带 unitRef（DD2 已支持） |
| 版本快照（AC-3.7） | `SELECT meta_value FROM ont_governance_meta WHERE meta_key='qudt_version'` = 3.1.4；symbol 冗余字段存在 |
| 精度（AC-3.8） | 换算引擎用 SN 系数（1.0E3）+ BigDecimal，`convertValue(0.1×10, ...)` 无累积误差；结果 ≤10 位有效 |
| builtin 保护 | 编辑 builtin 单位（如 M）→ 返回"内置单位不可编辑"；删除 → "不可删除" |
| 供给接口（AC-5.4） | `GET /supply/v1/units?quantityKindIri=.../Length` 返回长度量纲全部单位（含换算系数）；`ont_supply_view` 权限可访问 |
| 换算供给（AC-3.3） | `GET /supply/v1/units/convert?value=1&fromIri=.../KiloM&toIri=.../M` → 1000 |
| 前端 | pig-ui → 本体治理 → 单位注册表 → 左侧量纲树显 8 量纲（带单位计数）；点量纲右表显该量纲单位；换算试算区输 1km→m 显 1000；输 1kg→US$ 显"量纲不同"；新增 custom 单位；builtin 编辑删除禁用 |
| 回归 | 属性模板库（DD2）/分类模板（DD3）功能不受影响 |

---

## 八、风险与缓解

| 风险 | 缓解 |
|---|---|
| 换算浮点精度（R-6/AC-3.8） | SN 科学计数系数优先 + BigDecimal 全程 + setScale 去尾 |
| 货币汇率非实时（R-7） | 内置货币标"参考汇率"；换算结果提示"参考值，非实时"；接外部源属未来扩展 |
| QUDT IRI 漂移（R-5） | qudtIri 锁定 QUDT 3.1.4 版本快照（ont_governance_meta）；symbol 冗余存储降级 |
| 温度偏移单位换算易错 | 经基准（Kelvin）中转，不直接 from↔to；测试用例覆盖 32°F=0°C / 212°F=100°C |
| qudtIri 唯一约束与软删冲突 | DB 约束不受逻辑删除过滤（权威兜底）+ Service 预查重转友好提示 |
| 换算接口高频调用 | 优先前端本地算系数（NFR-15）；后端单位查询命中唯一索引，单次 O(1) |
| V9 与其他分支 Flyway 撞车 | V9 只做 ALTER + INSERT；合并时按需调版本号 |

---

## 九、交付物清单

| 类型 | 文件 | 说明 |
|---|---|---|
| 新建 | `QuantityKind.java` / `Unit.java` | 两 Entity（Unit 含 SN 新列字段） |
| 新建 | `QuantityKindMapper.java` / `UnitMapper.java` | 两 Mapper（MPJBaseMapper） |
| 新建 | `QuantityKindService.java` + `QuantityKindServiceImpl.java` | 量纲列表（含单位计数） |
| 新建 | `UnitService.java` + `UnitServiceImpl.java` | 单位 CRUD + 弃用 + 供给列表 |
| 新建 | `UnitConversionService.java` | 换算引擎（BigDecimal + SN 系数 + 偏移单位，核心） |
| 新建 | `UnitController.java` | 量纲列表 + 单位 CRUD + 换算（10.3） |
| 修改 | `SupplyController.java`（DD2 已有） | 追加 `/units`、`/units/convert` 两端点（10.5） |
| 新建 | `QuantityKindNodeVO` / `UnitSupplyVO` / `UnitConvertResultVO` | VO |
| 新建 | `V9__ont_unit_seed.sql` | SN 列 + 8 量纲 + 常用单位 + 权限点 |
| 新建 | `web/src/api/ontology/unit.ts` | 前端 API |
| 新建 | `web/src/views/admin/ontology/unit/index.vue` | 左量纲树右单位列表 + 换算试算区 |
| 新建 | `web/src/views/admin/ontology/unit/form.vue` | 新增/编辑对话框 |
| 新建 | `web/src/views/admin/ontology/unit/convert-panel.vue` | 换算试算区（本地算系数 + 防抖） |
| 新建 | `web/src/views/admin/ontology/unit/composables.ts` + `i18n/{zh-cn,en}.ts` | 状态复用 + 词条 |

---

## 十、与 PRD 边界的对齐确认（防混淆备忘）

| 边界点 | 本 DD 落地方式 | PRD 依据 |
|---|---|---|
| 引用 QUDT 而非自建 | 不重定义 qudt:Unit 类，只存 qudtIri + 系数；换算由 Java 执行 | 5.2 / OoS |
| 换算是工具行为非本体语义 | Java BigDecimal 计算，不依赖 OWL 推理机 | OoS |
| 货币非实时汇率 | 内置标"参考汇率"，换算结果提示参考值 | R-7 |
| 全量单位不内置 | 本注册表只抽 8 量纲常用子集，按需由 DD6 导入扩展 | OoS / FR-7 |
| 单位以 IRI 引用进 RDF | 本功能只产出 qudtIri，建模侧序列化时以 IRI 引用（建模侧职责） | 5.1 / 5.2 |
| 属性模板 unitRef 交汇 | DD2 已支持 PropertyTemplate.unitRef；本 DD 供给接口返回携带，不新增属性模板种子 | AC-3.6 / FR-1 |
| 精度优先 SN | 换算引擎 SN 优先 + BigDecimal，结果去尾 | AC-3.8 / NFR-9 |

---

*本详细设计对应里程碑 M3，依赖 DD1（M0）基础设施（V4 表结构、V5 元数据 qudt_version）与 DD2（M1）供给接口（SupplyController）。实现完成后，按《详细设计计划.md》依赖顺序进入 DD5（注释属性注册表）；DD6（参考本体，贯穿）依赖 DD3（Brick 导入分类模板）+ 本 DD（QUDT 导入单位）。*

