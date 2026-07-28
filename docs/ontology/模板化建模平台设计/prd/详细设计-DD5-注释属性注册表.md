# 详细设计-DD5-注释属性注册表（M4）

| 项 | 内容 |
|---|---|
| 文档名称 | 详细设计-DD5-注释属性注册表 |
| 里程碑 | M4（注释属性注册表） |
| 上游 PRD | 《本体模板化治理功能产品需求文档.md》v1.2 FR-4、FR-5（注释属性部分）、NFR-4/6、第五节、第九节 9.6、第十节 10.4/10.5、第十一节 11.6、第十二节 12.5、第十三节 13.1/13.2、第十四节 M4、附录 A |
| 设计计划 | 《详细设计计划.md》DD5 |
| 编写日期 | 2026-07-28 |
| 文档状态 | 待实现 |
| 前置依赖 | DD1（M0）已完成：模块骨架、V4 建表（9.6 `ont_annotation_property` 结构齐备 + `uk_ont_ap_local_name` 唯一约束）、V5 种子（注释属性菜单 10400，permission=NULL 占位）；DD2（M1）已完成：SupplyController 已建、`ont_supply_view` 权限点（V6 10105）。 |

---

## 一、设计目标与范围

### 1.1 目标

在 DD1/DD2/DD3/DD4 基础上，实现**注释属性注册表**，使 youming 具备：

- 维护一张**集中声明所有 `ont:xxx` 注释属性**的注册表（`localName/label/rangeXsd/appliesTo/description`），内置覆盖 ≥12 项（icon/color/unit/propertyType/cardinality/isIdentifier/enumValues/fromEntityId/toEntityId/templateRef + unitRef/quantityKindRef），驱动建模侧序列化/解析，而非散落硬编码 `ont:` 字面量（FR-4，AC-4.1，NFR-4）。
- `appliesTo` 约束作用对象（class/datatypeProperty/objectProperty/individual/all），供给接口支持按 `appliesTo` 过滤，使 `isIdentifier`（appliesTo=datatypeProperty）不会被建议用于 class（AC-4.3）。
- `localName` 唯一约束生效（V4 已建 `uk_ont_ap_local_name`），新增重复返回业务错误（AC-4.5）。
- 新增注释属性（如 `ont:displayName`）只改注册表一处，供给接口自动返回，建模侧序列化/解析自动支持（AC-4.2，NFR-4 可扩展性）。
- 可导出注释属性清单（Markdown，含 localName/label/rangeXsd/appliesTo/description）（AC-4.4）。
- pig-ui "注释属性注册表"页面（列表 + appliesTo 过滤 + 导出 + CRUD）可操作。

### 1.2 范围（本 DD 做 / 不做）

| 做（M4） | 不做（后续 DD） |
|---|---|
| AnnotationProperty Entity + Mapper + Service + Controller | 参考本体 CCO 注释属性浏览/导入（DD6，FR-7；CCO 注释属性浏览属 DD6） |
| 注册表全量列表（appliesTo 过滤）+ CRUD + 导出（Markdown） | 建模侧序列化器/解析器实现（建模侧职责，本功能只供给注册表驱动数据） |
| SupplyController 扩展：`/annotation-properties`（10.5，按 appliesTo 过滤） | 注释属性版本变更日志子表（本期预留，不强制） |
| V10 种子：≥12 项内置注释属性 + source 列（builtin 保护）+ 10401~10405 权限点按钮 | 注释属性的 OWL 推理（注释属性是元数据声明，不参与推理，OoS） |
| 前端 `views/admin/ontology/annotation-property/index.vue`（列表 + appliesTo 过滤 + 导出） | |
| 前端 `api/ontology/annotation-property.ts` | |

> **边界声明（呼应 PRD 第五节 / 附录 A / R-11）**：
> - **注册表而非副本**：注释属性本身即全局唯一，做注册表集中声明，不做属性模板式的"副本"（附录 A：注释属性 ✅(中度) 注册表（非副本））。
> - **只产资产 + 供给，不侵入序列化**：本功能只维护注册表并对建模侧供给；序列化/解析时如何遍历注册表驱动读写属建模侧职责（R-11 边界）。
> - **`templateRef`/`unitRef`/`quantityKindRef` 作为溯源注解纳入注册表**：这三项是治理资产（FR-1 属性模板 / FR-3 单位）产出的溯源字段，建模侧序列化时以注释属性形式挂载，故纳入注册表统一声明（FR-4 第 6 条）。
> - **PRD 9.6 表无 `deprecated` 字段**：`ont_annotation_property` 按 PRD 9.6 定义不含 `deprecated`，故供给接口对本端点不适用 AC-5.6 的弃用过滤（返回全量）；`source` 列为本期 V10 增补（见 3.2 说明）。

### 1.3 验收映射（M4 DoD）

| PRD AC | 本 DD 实现点 |
|---|---|
| AC-4.1 注册表完整覆盖现有 `ont:xxx`（≥10 项 + unitRef/quantityKindRef） | 6.1 V10 种子（12 项：icon/color/unit/propertyType/cardinality/isIdentifier/enumValues/fromEntityId/toEntityId/templateRef + unitRef/quantityKindRef） |
| AC-4.2 新增注释属性只改注册表一处，供给接口自动返回 | 4.3 AnnotationPropertyController CRUD（新增 custom）+ 4.6 SupplyController `/annotation-properties`（全量返回，含新增项）；建模侧遍历注册表驱动，无需改硬编码 |
| AC-4.3 `appliesTo` 校验：`isIdentifier`（appliesTo=datatypeProperty）不会在供给接口中被建议用于 class | 4.6 SupplyController `/annotation-properties?appliesTo=class` 过滤；4.5 Service `appliesTo` 枚举校验（拒绝非法值） |
| AC-4.4 可导出注释属性清单（含 localName/label/rangeXsd/appliesTo/description） | 4.4 `GET /ont/annotation-property/export`（Markdown 表格，`ont_ap_view`） |
| AC-4.5 `localName` 唯一约束生效 | V4 已建 `uk_ont_ap_local_name` + 4.5 Service 查重 + DuplicateKeyException 兜底 |
| AC-5.5 注释属性供给接口返回全量注册表，供建模侧序列化驱动 | 4.6 SupplyController `/annotation-properties`（返回稳定化 `AnnotationPropertySupplyVO`，AC-5.7） |

---

## 二、落地清单

### 2.1 后端文件清单

> 包根 `com.pig4cloud.pig.ontology`，分层 `controller` / `service`(+`impl`) / `mapper` / `api/entity` / `api/vo`。对齐 DD2/DD3/DD4 已有范式（PropertyTemplate* / Unit*）。

```
server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/
├── api/
│   ├── entity/
│   │   └── AnnotationProperty.java                # 注释属性（9.6，含 V10 增补 source 列）
│   └── vo/
│       └── AnnotationPropertySupplyVO.java        # 注释属性供给视图（稳定化，屏蔽审计字段，AC-5.7）
├── controller/
│   ├── AnnotationPropertyController.java          # 注册表列表 + CRUD + 导出（10.4）
│   （SupplyController.java                        # 复用 DD2 已有，扩展 annotation-properties 端点）
├── service/
│   ├── AnnotationPropertyService.java
│   └── impl/
│       └── AnnotationPropertyServiceImpl.java
└── mapper/
    └── AnnotationPropertyMapper.java
```

> 说明：注释属性注册表体量小（内置 12 项 + 治理员 custom 扩展，预计 ≤100），无复杂关联与算法 Service，结构精简。无独立 DTO（CRUD 直接用 Entity + `@Valid`，导出直接生成 Markdown 字符串）。

### 2.2 数据库文件清单

> **V4 已建好表结构（9.6，含 `uk_ont_ap_local_name` 唯一约束），V5 已建菜单 10400（permission=NULL 占位）**，本期不返工 V4/V5，只新增 V10。

```
server/pig-common/pig-common-data/src/main/resources/db/migration/
├── V4__ont_governance_schema.sql                  # 已有（DD1，ont_annotation_property 表结构 + uk_ont_ap_local_name）
├── V5__ont_governance_seed.sql                    # 已有（DD1，注释属性菜单 10400）
├── V6__ont_supply_permission_seed.sql             # 已有（DD2，ont_supply_view）
├── V7__ont_property_template_rename_values.sql    # 已有（DD2）
├── V8__ont_class_template_seed.sql                # 已有（DD3）
├── V9__ont_unit_seed.sql                          # 已有（DD4）
└── V10__ont_annotation_property_seed.sql          # 新增：source 列 + 12 项内置注释属性种子 + 10401~10405 权限点
```

> **V10 改表说明**：V4 的 `ont_annotation_property` 按 PRD 9.6 定义不含 `source`（builtin/custom）列。为对齐全平台治理表范式（property_template/class_template/unit 均有 source + builtin 保护）并保护 AC-4.1"完整覆盖 ont:xxx"的稳定性（防止内置项被误删），V10 用 `ALTER TABLE ADD COLUMN` 增补 `source` 列（不改 V4，遵循"禁改已应用脚本"）。PRD 9.6 未定义 `deprecated`，本期不增补（供给接口对本端点不适用弃用过滤）。

### 2.3 依赖变更清单

```
server/pig-ontology/pig-ontology-biz/pom.xml     # 无需修改（导出用 JDK StringBuilder 生成 Markdown，无新依赖）
```

### 2.4 前端文件清单

```
web/src/
├── api/ontology/
│   └── annotation-property.ts                     # 注册表 API（listObj/getObj/addObj/putObj/delObj/exportObj）
└── views/admin/ontology/
    └── annotation-property/
        ├── index.vue                              # 列表（appliesTo 过滤 + 导出 + CRUD）
        ├── form.vue                               # 新增/编辑对话框
        └── i18n/
            ├── zh-cn.ts
            └── en.ts
```

> 路径说明：sys_menu path 为 `/admin/ontology/annotation-property/index`（V5 已配 10400），backEnd.ts 用 `import.meta.glob('../views/**/*.{vue,tsx}')` 按 path 匹配，故 vue 文件须放 `views/admin/ontology/annotation-property/index.vue`（对齐 DD2/DD3/DD4 范式）。

---

## 三、数据库设计（V10 种子）

### 3.1 V10 种子范围

V4 已建表结构（9.6），V5 已建菜单 10400。V10 做三件事：

1. **(a) 增补 `source` 列**：对齐全平台治理表范式 + builtin 保护（保护 AC-4.1 内置覆盖稳定性）。
2. **(b) 内置 12 项注释属性种子**（AC-4.1）：icon/color/unit/propertyType/cardinality/isIdentifier/enumValues/fromEntityId/toEntityId/templateRef + unitRef/quantityKindRef，均带 `appliesTo`/`rangeXsd`/`description`。
3. **(c) 注释属性权限点按钮**（10.4）：注释属性注册表菜单（10400）下补 view/manage/export 按钮。

### 3.2 `V10__ont_annotation_property_seed.sql`

```sql
-- ============================================================
-- V10__ont_annotation_property_seed.sql
-- 注释属性注册表：增补 source 列 + 12 项内置种子 + 权限点
-- 对应 PRD v1.2 FR-4 AC-4.1/4.5、NFR-4
-- 依赖 V4（表结构 + uk_ont_ap_local_name）、V5（菜单 10400）
-- ============================================================

-- ---------- (a) 增补 source 列（builtin/custom，对齐全平台治理表范式） ----------
-- ALTER ADD COLUMN 不改 V4，遵循"禁改已应用脚本"
-- 用途：builtin（随 Flyway 分发，只读保护） / custom（治理员新增）
ALTER TABLE ont_annotation_property ADD COLUMN source varchar(16) DEFAULT 'builtin';
COMMENT ON COLUMN ont_annotation_property.source IS 'builtin（内置只读）/ custom（治理员新增）';

-- ---------- (b) 12 项内置注释属性种子（ont_annotation_property，AC-4.1） ----------
-- localName 即 ont:xxx 的 xxx（序列化时拼成 ont:icon 等）
-- applies_to 取值：class/datatypeProperty/objectProperty/individual/all（PRD 9.6）
-- id 用 7xxx 段（5xxx 量纲 / 6xxx 单位 / 7xxx 注释属性，便于跨脚本引用）
-- source='builtin'（随 Flyway 分发，只读）；sort_order 决定列表与导出顺序
INSERT INTO ont_annotation_property (id, local_name, label, range_xsd, applies_to, description, sort_order, source, create_by, create_time, update_by, update_time) VALUES
(7001, 'icon',             '显示图标',   'xsd:string',  'class',            '类的展示图标（外观治理，FR-2）',                                                         1,  'builtin', 'admin', now(), 'admin', now()),
(7002, 'color',            '显示颜色',   'xsd:string',  'class',            '类的展示颜色（外观治理，FR-2）',                                                         2,  'builtin', 'admin', now(), 'admin', now()),
(7003, 'unit',             '单位符号',   'xsd:string',  'datatypeProperty', '数据属性的单位符号（旧版裸字符串；建议用 unitRef 引用 QUDT IRI，FR-3）',                3,  'builtin', 'admin', now(), 'admin', now()),
(7004, 'propertyType',     '属性类型',   'xsd:string',  'all',              '属性类型标注（data/object），供建模侧区分数据/对象属性',                                  4,  'builtin', 'admin', now(), 'admin', now()),
(7005, 'cardinality',      '基数',       'xsd:string',  'objectProperty',   '对象属性基数（如 0..1/0..n/1..1），对齐 FR-1 defaultCardinality',                        5,  'builtin', 'admin', now(), 'admin', now()),
(7006, 'isIdentifier',     '是否标识符', 'xsd:boolean', 'datatypeProperty', '标注数据属性是否为标识符（appliesTo=datatypeProperty，不可用于 class，AC-4.3）',         6,  'builtin', 'admin', now(), 'admin', now()),
(7007, 'enumValues',       '枚举值',     'xsd:string',  'datatypeProperty', '数据属性的枚举取值集合（对齐 FR-1 enumValues）',                                         7,  'builtin', 'admin', now(), 'admin', now()),
(7008, 'fromEntityId',     '起始实体',   'xsd:string',  'objectProperty',   '对象属性的起始实体标识（建模侧关系建模辅助）',                                           8,  'builtin', 'admin', now(), 'admin', now()),
(7009, 'toEntityId',       '目标实体',   'xsd:string',  'objectProperty',   '对象属性的目标实体标识（建模侧关系建模辅助）',                                           9,  'builtin', 'admin', now(), 'admin', now()),
(7010, 'templateRef',      '模板溯源',   'xsd:string',  'all',              '属性溯源到治理模板 templateCode（FR-1/FR-6，序列化方案 A/B 判据来源）',                    10, 'builtin', 'admin', now(), 'admin', now()),
(7011, 'unitRef',          '单位引用',   'xsd:string',  'datatypeProperty', '数据属性引用的 QUDT 单位 IRI（FR-3，如 .../unit/KiloGM）',                                11, 'builtin', 'admin', now(), 'admin', now()),
(7012, 'quantityKindRef',  '量纲引用',   'xsd:string',  'datatypeProperty', '数据属性引用的 QUDT 量纲 IRI（FR-3，如 .../quantitykind/Mass）',                          12, 'builtin', 'admin', now(), 'admin', now());

-- ---------- (c) 注释属性权限点按钮（挂在注释属性注册表菜单 10400 下） ----------
-- view/manage 对齐 PRD 13.2；导出走 view 权限（AC-4.4）
-- 10401~10403 manage（新增/编辑/删除），10404~10405 view（查看/导出）
INSERT INTO sys_menu VALUES (10401, '注释属性新增', 'ont_ap_manage', NULL, NULL, 10400, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10402, '注释属性编辑', 'ont_ap_manage', NULL, NULL, 10400, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10403, '注释属性删除', 'ont_ap_manage', NULL, NULL, 10400, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10404, '注释属性查看', 'ont_ap_view',   NULL, NULL, 10400, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10405, '注释属性导出', 'ont_ap_view',   NULL, NULL, 10400, NULL, '1', 5, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
```

> 说明：
> - **V10 只做 ALTER + INSERT，不改 V4/V5**（禁改已应用脚本，checksum 校验）。
> - **AC-4.1 覆盖**：12 项 = 10 项基础（icon/color/unit/propertyType/cardinality/isIdentifier/enumValues/fromEntityId/toEntityId/templateRef）+ unitRef + quantityKindRef，满足"≥10 项 + unitRef/quantityKindRef"。
> - **AC-4.3 appliesTo**：`isIdentifier` 的 `applies_to='datatypeProperty'`，供给接口按 `appliesTo=class` 过滤时不返回（class 只匹配 appliesTo=class/all 的项）。
> - **AC-4.5 唯一约束**：V4 已建 `uk_ont_ap_local_name UNIQUE (local_name)`，12 项 localName 两两不同。
> - `source` 列 DEFAULT 'builtin'：V10 INSERT 显式给 'builtin'；治理员通过 UI 新增的走 Service 设 'custom'。
> - 10400 段菜单（V5）+ 10401~10405 权限点（V10）齐全；避开 10301~10304（DD4 单位）与 10251（DD3 同步）。

---

## 四、后端设计

### 4.1 Entity `AnnotationProperty.java`

```java
package com.pig4cloud.pig.ontology.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 注释属性注册表（引用 ont:xxx 命名空间）
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "注释属性注册表")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_annotation_property")
public class AnnotationProperty extends Model<AnnotationProperty> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "唯一，如 icon/unitRef（序列化拼成 ont:icon）")
	@NotBlank(message = "localName 不能为空")
	private String localName;

	@Schema(description = "显示名")
	@NotBlank(message = "显示名不能为空")
	private String label;

	@Schema(description = "值域 XSD，如 xsd:string/xsd:boolean")
	private String rangeXsd;

	@Schema(description = "作用对象：class/datatypeProperty/objectProperty/individual/all")
	private String appliesTo;

	@Schema(description = "说明")
	private String description;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "builtin / custom")
	private String source;

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

> 说明：Mapper 继承 `MPJBaseMapper<AnnotationProperty>`（对齐 DD2/DD3/DD4 范式），无自定义 SQL。`source` 为 V10 新增列，Entity 对应字段（MyBatis-Plus 驼峰推断 `source` -> `source`，无需 `@TableField` 显式标注）。`range_xsd`/`applies_to`/`sort_order` 驼峰自动映射 `rangeXsd`/`appliesTo`/`sortOrder`。

### 4.2 Mapper `AnnotationPropertyMapper.java`

```java
package com.pig4cloud.pig.ontology.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.pig4cloud.pig.ontology.api.entity.AnnotationProperty;
import org.apache.ibatis.annotations.Mapper;

/**
 * 注释属性 Mapper
 *
 * @author pig
 * @date 2026-07-28
 */
@Mapper
public interface AnnotationPropertyMapper extends MPJBaseMapper<AnnotationProperty> {

}
```

### 4.3 Controller `AnnotationPropertyController.java`

```java
package com.pig4cloud.pig.ontology.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.entity.AnnotationProperty;
import com.pig4cloud.pig.ontology.service.AnnotationPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 注释属性注册表 Controller（FR-4）
 * <p>
 * 路径 /ont/annotation-property/**，经 context-path /admin 或网关路由后对外为
 * /admin/ont/annotation-property/**（对齐 PRD 10.4）。
 * <p>
 * 注意：控制器须带 /ont 前缀（对齐 DD2/DD3/DD4 Controller 范式，否则 pig-gateway
 * StripPrefix=1 / pig-boot context-path=/admin 剥离后 404）。
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/annotation-property")
@Tag(name = "注释属性注册表", description = "注释属性 CRUD + 导出（FR-4）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class AnnotationPropertyController {

	private final AnnotationPropertyService annotationPropertyService;

	@GetMapping("/list")
	@Operation(summary = "全量列表", description = "按 appliesTo 过滤（10.4，AC-4.3）；注册表体量小，前端客户端分页")
	@HasPermission("ont_ap_view")
	public R<List<AnnotationProperty>> list(@RequestParam(required = false) String appliesTo) {
		return R.ok(annotationPropertyService.list(appliesTo));
	}

	@GetMapping("/{id}")
	@Operation(summary = "详情")
	@HasPermission("ont_ap_view")
	public R<AnnotationProperty> getById(@PathVariable Long id) {
		return R.ok(annotationPropertyService.getById(id));
	}

	@SysLog("新增注释属性")
	@PostMapping
	@Operation(summary = "新增 custom 注释属性", description = "localName 查重 + appliesTo 枚举校验（AC-4.2/4.5）")
	@HasPermission("ont_ap_manage")
	public R save(@Valid @RequestBody AnnotationProperty ap) {
		return annotationPropertyService.saveAp(ap);
	}

	@SysLog("编辑注释属性")
	@PutMapping("/{id}")
	@Operation(summary = "编辑（builtin 拒绝）")
	@HasPermission("ont_ap_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody AnnotationProperty ap) {
		ap.setId(id);
		return annotationPropertyService.updateAp(ap);
	}

	@SysLog("删除注释属性")
	@DeleteMapping("/{id}")
	@Operation(summary = "删除（custom，软删）")
	@HasPermission("ont_ap_manage")
	public R removeById(@PathVariable Long id) {
		return annotationPropertyService.removeAp(id);
	}

	@GetMapping("/export")
	@Operation(summary = "导出清单", description = "Markdown 表格，含 localName/label/rangeXsd/appliesTo/description（AC-4.4）")
	@HasPermission("ont_ap_view")
	public R<String> export(@RequestParam(required = false) String appliesTo) {
		return R.ok(annotationPropertyService.exportMarkdown(appliesTo));
	}
}
```

> 说明：`@GetMapping("/list")` 全量返回（注册表体量小，PRD 10.4 指定 `/list` 全量列表；前端客户端分页/过滤，对齐 NFR 治理列表 <200ms）。权限点 `ont_ap_view`（V10 新建 10404/10405）/ `ont_ap_manage`（V10 新建 10401~10303）。导出走 view 权限（AC-4.4）。

### 4.4 VO `AnnotationPropertySupplyVO.java`

```java
package com.pig4cloud.pig.ontology.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 注释属性供给视图（稳定化，屏蔽审计字段，AC-5.7）
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "注释属性供给视图")
public class AnnotationPropertySupplyVO implements Serializable {

	@Schema(description = "主键") private Long id;

	@Schema(description = "唯一，如 icon/unitRef（建模侧据此拼 ont:xxx 序列化）") private String localName;

	@Schema(description = "显示名") private String label;

	@Schema(description = "值域 XSD，如 xsd:string/xsd:boolean") private String rangeXsd;

	@Schema(description = "作用对象：class/datatypeProperty/objectProperty/individual/all") private String appliesTo;

	@Schema(description = "说明") private String description;

	@Schema(description = "排序") private Integer sortOrder;

	@Schema(description = "builtin / custom") private String source;
}
```

> 说明：供给 VO 屏蔽 `createBy/createTime/updateBy/updateTime/delFlag` 审计字段（AC-5.7 稳定化），供建模侧序列化器/解析器遍历驱动。`localName` 是建模侧拼接 `ont:` 命名空间注释属性的关键字段。

### 4.5 Service `AnnotationPropertyServiceImpl.java`（关键逻辑）

```java
package com.pig4cloud.pig.ontology.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.entity.AnnotationProperty;
import com.pig4cloud.pig.ontology.api.vo.AnnotationPropertySupplyVO;
import com.pig4cloud.pig.ontology.mapper.AnnotationPropertyMapper;
import com.pig4cloud.pig.ontology.service.AnnotationPropertyService;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 注释属性 Service 实现（FR-4）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class AnnotationPropertyServiceImpl
		extends ServiceImpl<AnnotationPropertyMapper, AnnotationProperty> implements AnnotationPropertyService {

	/**
	 * appliesTo 合法取值（PRD 9.6）。
	 */
	private static final Set<String> APPLIES_TO_VALUES = Set.of(
			"class", "datatypeProperty", "objectProperty", "individual", "all");

	@Override
	public List<AnnotationProperty> list(String appliesTo) {
		return list(Wrappers.<AnnotationProperty>lambdaQuery()
				.eq(StrUtil.isNotBlank(appliesTo), AnnotationProperty::getAppliesTo, appliesTo)
				.orderByAsc(AnnotationProperty::getSortOrder)
				.orderByAsc(AnnotationProperty::getId));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveAp(AnnotationProperty ap) {
		// 1. appliesTo 枚举校验（AC-4.3，空值允许=不限作用对象）
		if (StrUtil.isNotBlank(ap.getAppliesTo()) && !APPLIES_TO_VALUES.contains(ap.getAppliesTo())) {
			return R.failed("appliesTo 取值非法，允许：class/datatypeProperty/objectProperty/individual/all");
		}
		// 2. localName 预查重（AC-4.5）
		long count = count(Wrappers.<AnnotationProperty>lambdaQuery().eq(AnnotationProperty::getLocalName, ap.getLocalName()));
		if (count > 0) {
			return R.failed("localName '" + ap.getLocalName() + "' 已存在");
		}
		ap.setSource("custom");
		try {
			return R.ok(save(ap));
		}
		catch (DuplicateKeyException e) {
			// DB 唯一约束 uk_ont_ap_local_name 兜底（覆盖软删后复用 localName 的场景）
			return R.failed("localName '" + ap.getLocalName() + "' 已存在");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateAp(AnnotationProperty ap) {
		AnnotationProperty existing = getById(ap.getId());
		if (existing == null) {
			return R.failed("注释属性不存在");
		}
		if ("builtin".equals(existing.getSource())) {
			return R.failed("内置注释属性不可编辑");
		}
		// appliesTo 枚举校验
		if (StrUtil.isNotBlank(ap.getAppliesTo()) && !APPLIES_TO_VALUES.contains(ap.getAppliesTo())) {
			return R.failed("appliesTo 取值非法，允许：class/datatypeProperty/objectProperty/individual/all");
		}
		// localName 不可改（引用稳定性，对齐 DD2 templateCode / DD4 qudtIri 范式）
		ap.setLocalName(existing.getLocalName());
		return R.ok(updateById(ap));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeAp(Long id) {
		AnnotationProperty existing = getById(id);
		if (existing == null) {
			return R.failed("注释属性不存在");
		}
		if ("builtin".equals(existing.getSource())) {
			return R.failed("内置注释属性不可删除");
		}
		return R.ok(removeById(id));
	}

	@Override
	public List<AnnotationPropertySupplyVO> supplyList(String appliesTo) {
		List<AnnotationProperty> list = list(appliesTo);
		return list.stream().map(ap -> {
			AnnotationPropertySupplyVO vo = new AnnotationPropertySupplyVO();
			BeanUtils.copyProperties(ap, vo);
			return vo;
		}).collect(Collectors.toList());
	}

	@Override
	public String exportMarkdown(String appliesTo) {
		List<AnnotationProperty> list = list(appliesTo);
		StringBuilder sb = new StringBuilder();
		sb.append("# 注释属性注册表\n\n");
		sb.append("> 共 ").append(list.size()).append(" 项").append(StrUtil.isNotBlank(appliesTo)
				? "（appliesTo=" + appliesTo + "）" : "").append("\n\n");
		sb.append("| # | localName | label | rangeXsd | appliesTo | description |\n");
		sb.append("|---|---|---|---|---|---|\n");
		if (CollUtil.isEmpty(list)) {
			sb.append("| - | - | - | - | - | - |\n");
		}
		else {
			int i = 1;
			for (AnnotationProperty ap : list) {
				sb.append("| ").append(i++).append(" | ")
						.append(nullSafe(ap.getLocalName())).append(" | ")
						.append(nullSafe(ap.getLabel())).append(" | ")
						.append(nullSafe(ap.getRangeXsd())).append(" | ")
						.append(nullSafe(ap.getAppliesTo())).append(" | ")
						.append(nullSafe(ap.getDescription())).append(" |\n");
			}
		}
		return sb.toString();
	}

	private String nullSafe(String s) {
		return s == null ? "" : s;
	}
}
```

> 说明：
> - **`saveAp` 校验链**：appliesTo 枚举校验 -> localName 查重 -> 落库（source=custom）。`updateAp` builtin 保护 + localName 不可改（对齐 DD2 templateCode / DD4 qudtIri 范式）。`removeAp` builtin 保护。
> - **appliesTo 枚举校验（AC-4.3）**：合法值 `class/datatypeProperty/objectProperty/individual/all`；空值允许（=不限作用对象）。非法值返回业务错误。
> - **唯一约束兜底（AC-4.5）**：Service 预查重 + DB `uk_ont_ap_local_name` 兜底（`DuplicateKeyException` 转友好提示，覆盖软删后复用 localName 场景）。
> - **导出 Markdown（AC-4.4）**：`exportMarkdown` 用 StringBuilder 生成 Markdown 表格，含 localName/label/rangeXsd/appliesTo/description；空值用空串占位避免 `null` 字面量。

### 4.6 SupplyController 扩展（10.5，复用 DD2 已有类）

> 在 DD2/DD3/DD4 已建的 `SupplyController`（`@RequestMapping("/ont/supply/v1")`，对外 `/admin/ont/supply/v1`）上追加一个端点。

```java
// 追加到 SupplyController（DD2 property-templates / DD3 class-template / DD4 units 端点保留）

@GetMapping("/annotation-properties")
@Operation(summary = "注释属性供给", description = "全量注册表，支持 appliesTo 过滤（10.5，AC-4.3/5.5）")
@HasPermission("ont_supply_view")
public R<List<AnnotationPropertySupplyVO>> supplyAnnotationProperties(
		@RequestParam(required = false) String appliesTo) {
	return R.ok(annotationPropertyService.supplyList(appliesTo));
}
```

> 说明：
> - `supplyAnnotationProperties`：供建模侧序列化器/解析器拉取全量注册表驱动读写注释属性，而非硬编码 `ont:` 字面量（AC-4.5/AC-5.5）。`appliesTo` 过滤：建模 class 时拉 `appliesTo=class`（含 appliesTo=all），`isIdentifier`（datatypeProperty）不返回（AC-4.3）。
> - 返回稳定化 `AnnotationPropertySupplyVO`（屏蔽审计字段，AC-5.7）。
> - **AC-5.6 弃用过滤豁免**：`ont_annotation_property` 按 PRD 9.6 无 `deprecated` 字段，本端点不适用弃用过滤，返回全量（已在边界声明）。
> - **AC-4.2 一处新增**：治理员通过 UI 新增 custom 注释属性后，本供给接口自动返回（建模侧无需改硬编码）。

---

## 五、前端设计

### 5.1 API `web/src/api/ontology/annotation-property.ts`

```typescript
import request from '/@/utils/request';

// ---------- 注释属性注册表 ----------
export function listObj(query?: any) {
	return request({
		url: '/admin/ont/annotation-property/list',
		method: 'get',
		params: query,
	});
}

export function getObj(id: string) {
	return request({
		url: '/admin/ont/annotation-property/' + id,
		method: 'get',
	});
}

export function addObj(obj: any) {
	return request({
		url: '/admin/ont/annotation-property',
		method: 'post',
		data: obj,
	});
}

export function putObj(obj: any) {
	return request({
		url: '/admin/ont/annotation-property/' + obj.id,
		method: 'put',
		data: obj,
	});
}

export function delObj(id: string) {
	return request({
		url: '/admin/ont/annotation-property/' + id,
		method: 'delete',
	});
}

// ---------- 导出（Markdown） ----------
export function exportObj(query?: any) {
	return request({
		url: '/admin/ont/annotation-property/export',
		method: 'get',
		params: query,
	});
}
```

### 5.2 注释属性注册表页 `views/admin/ontology/annotation-property/index.vue`（列表 + appliesTo 过滤 + 导出 + CRUD）

> 对齐 pig 现有 `views/admin/ontology/property-template/index.vue`（列表 + 表单对话框）范式，无左树（注册表是平铺全局表）。

**布局（顶部过滤 + 操作栏 + 表格 + 表单对话框，PRD 12.5）：**

```
┌──────────────────────────────────────────────────────────────┐
│ appliesTo [全部▼]  关键字 [____]  [查询]   [+新增] [导出]     │
├──────────────────────────────────────────────────────────────┤
│ # | localName | label  | rangeXsd   | appliesTo       | 操作  │
│ 1 | icon       | 显示图标| xsd:string | class           | 编辑 │
│ 2 | color      | 显示颜色| xsd:string | class           | 编辑 │
│ 6 | isIdentifier|是否标识|xsd:boolean| datatypeProperty| 编辑 │
│ ..                                                          │
└──────────────────────────────────────────────────────────────┘
```

**关键结构：**

- **顶部过滤栏**：`appliesTo` 下拉（全部/class/datatypeProperty/objectProperty/individual/all）+ 关键字输入（前端本地按 localName/label 过滤）+ 查询按钮。
- **操作栏**：`v-auth="'ont_ap_manage'"` 新增按钮（新增 custom 注释属性）；`v-auth="'ont_ap_view'"` 导出按钮（调 `exportObj`，前端转 Blob 下载 `.md`）。
- **表格**：列 localName/label/rangeXsd/appliesTo/description/source/操作（编辑/删除，builtin 行禁用编辑删除）。
- **内置项**：编辑/删除按钮 `:disabled="scope.row.source === 'builtin'"`（对齐 DD2/DD4 builtin 保护）。
- **客户端分页**：注册表体量小，`listObj` 一次拉全量，前端 `el-pagination` 客户端分页（对齐 NFR 列表 <200ms）。

**关键逻辑（对齐 property-template/index.vue）：**

```typescript
const state = reactive({
	dataList: [] as any[],       // 全量注册表
	filtered: [] as any[],       // 过滤后（appliesTo + 关键字）
	appliesTo: '' as string,     // 过滤值（空=全部）
	keyword: '' as string,
	page: { current: 1, size: 20, total: 0 },
});

// 拉全量（appliesTo 过滤走后端，关键字走前端本地）
async function getDataList() {
	const { data } = await listObj({ appliesTo: state.appliesTo || undefined });
	state.dataList = data ?? [];
	applyFilter();
}
// 前端关键字过滤 + 客户端分页
function applyFilter() {
	const kw = state.keyword.trim().toLowerCase();
	state.filtered = state.dataList.filter((r) => !kw
		|| r.localName?.toLowerCase().includes(kw)
		|| r.label?.toLowerCase().includes(kw));
	state.page.total = state.filtered.length;
}
// 导出 Markdown -> 下载 .md
async function handleExport() {
	const { data } = await exportObj({ appliesTo: state.appliesTo || undefined });
	const blob = new Blob([data], { type: 'text/markdown;charset=utf-8' });
	const url = URL.createObjectURL(blob);
	const a = document.createElement('a');
	a.href = url;
	a.download = 'annotation-property.md';
	a.click();
	URL.revokeObjectURL(url);
}
```

### 5.3 表单 `form.vue`

| 字段 | 说明 |
|---|---|
| localName | 注释属性名（新增必填、唯一；编辑只读，对齐 DD2 templateCode / DD4 qudtIri） |
| label | 显示名（必填） |
| rangeXsd | 值域 XSD（下拉：xsd:string/xsd:boolean/xsd:integer/xsd:decimal/xsd:anyURI，可自定义） |
| appliesTo | 作用对象（下拉：class/datatypeProperty/objectProperty/individual/all） |
| description | 说明（可选） |
| sortOrder | 排序（整数，默认 0） |

> 用 `el-dialog + el-form`，对齐 DD2 `property-template/form.vue` 范式。提示"localName 即 ont:xxx 的 xxx，序列化时拼成 ont:localName；appliesTo 约束建模侧可挂载该注释属性的对象类型"。

### 5.4 i18n（annotation-property/i18n/zh-cn.ts 示例）

```typescript
export default {
	annotationProperty: {
		localName: '属性名',
		label: '显示名',
		rangeXsd: '值域 XSD',
		appliesTo: '作用对象',
		description: '说明',
		sortOrder: '排序',
		source: '来源',
		// appliesTo 选项
		appliesAll: '全部',
		appliesClass: '类（class）',
		appliesDatatypeProperty: '数据属性（datatypeProperty）',
		appliesObjectProperty: '对象属性（objectProperty）',
		appliesIndividual: '个体（individual）',
		appliesAllValue: '不限（all）',
		// 操作
		add: '新增',
		edit: '编辑',
		delete: '删除',
		export: '导出',
		// 状态
		builtin: '内置',
		custom: '自定义',
		// inputXxxTip 系列
		inputLocalNameTip: '请输入属性名（如 displayName）',
		inputLabelTip: '请输入显示名',
		selectRangeXsdTip: '请选择值域 XSD',
		selectAppliesToTip: '请选择作用对象',
		builtinDeleteDisabledTip: '内置注释属性不可删除',
		builtinEditDisabledTip: '内置注释属性不可编辑',
		// 导出
		exportSuccess: '导出成功',
	},
};
```

---

## 六、横切设计

### 6.1 校验

- **localName 唯一**：Service 预查重 + DB `uk_ont_ap_local_name`（V4 已建）兜底（`DuplicateKeyException` 转友好提示，覆盖软删后复用场景，AC-4.5）。
- **appliesTo 枚举校验**：合法值 `class/datatypeProperty/objectProperty/individual/all`，非法值返回业务错误（AC-4.3）。
- **builtin 保护**：编辑/删除 builtin 返回"内置注释属性不可操作"（对齐 DD2/DD4）。
- **localName 不可改**：编辑时锁定（引用稳定性，对齐 DD2 templateCode / DD4 qudtIri）。

### 6.2 异常处理

- 唯一约束冲突（`DuplicateKeyException`）：Service 预查重，兜底捕获转 `R.failed`。
- appliesTo 非法：`saveAp`/`updateAp` 返回 `R.failed("appliesTo 取值非法...")`。

### 6.3 审计

- MybatisPlusMetaObjectHandler 自动填充 `createBy/createTime/updateBy/updateTime/delFlag`（DD1 已配置）。

### 6.4 双形态

- 单体：pig-boot context-path `/admin` -> `/admin/ont/annotation-property/**`、`/admin/ont/supply/v1/annotation-properties`。
- 微服务：网关路由 `Path=/admin/ont/**` -> `lb://pig-ontology-biz`（DD1 已配置）。

### 6.5 安全

- 治理接口（CRUD）：`ont_ap_manage`（V10 新建 10401~10403）。
- 查看与导出接口：`ont_ap_view`（V10 新建 10404/10405）。
- 供给接口（`/supply/v1/annotation-properties`）：`ont_supply_view`（V6 已建 10105，建模师/查看者授予）。

### 6.6 国际化（NFR-6）

- `label`/`description` 内置种子给中文（治理员可改）；前端 i18n 双语（zh-cn/en），词条放 `views/admin/ontology/annotation-property/i18n/`。

---

## 七、测试要点

| 类别 | 要点 |
|---|---|
| 编译 | `mvn -pl pig-ontology/pig-ontology-biz -am compile` 通过（无新依赖） |
| Flyway | 启动后 V10 成功；`SELECT count(*) FROM ont_annotation_property` = 12；`SELECT * FROM ont_annotation_property WHERE local_name='isIdentifier'` 的 applies_to='datatypeProperty'；`SELECT * FROM sys_menu WHERE menu_id IN (10401,10402,10403,10404,10405)` 存在；`ont_annotation_property.source` 列存在 |
| 覆盖（AC-4.1） | `GET /ont/annotation-property/list` 返回 12 项，含 unitRef/quantityKindRef |
| 一处新增（AC-4.2） | `POST /ont/annotation-property`（localName=displayName）成功；`GET /supply/v1/annotation-properties` 自动返回 displayName |
| appliesTo 过滤（AC-4.3） | `GET /supply/v1/annotation-properties?appliesTo=class` 不返回 isIdentifier（datatypeProperty）；`?appliesTo=datatypeProperty` 返回 isIdentifier |
| 导出（AC-4.4） | `GET /ont/annotation-property/export` 返回 Markdown 表格，含表头 localName/label/rangeXsd/appliesTo/description 与 12 行 |
| 唯一约束（AC-4.5） | 新增 localName=icon（已存在）-> 返回业务错误"localName 'icon' 已存在"（非 500） |
| appliesTo 校验 | 新增 appliesTo=invalidValue -> 返回业务错误"appliesTo 取值非法" |
| builtin 保护 | 编辑 builtin 项（如 icon）-> 返回"内置注释属性不可编辑"；删除 -> "不可删除" |
| 供给接口（AC-5.5） | `GET /supply/v1/annotation-properties` 返回稳定化 VO（无审计字段），`ont_supply_view` 权限可访问 |
| 前端 | pig-ui -> 本体治理 -> 注释属性注册表 -> 列表显 12 项；appliesTo 过滤 class 仅显 class/all 项；导出生成 .md 下载；新增 custom 注释属性；builtin 编辑删除禁用 |
| 回归 | 属性模板库（DD2）/分类模板（DD3）/单位注册表（DD4）功能不受影响；SupplyController 既有端点不受影响 |

---

## 八、风险与缓解

| 风险 | 缓解 |
|---|---|
| 内置 ont:xxx 被误删破坏 AC-4.1 覆盖 | V10 增补 source 列 + builtin 保护（编辑/删除内置项拒绝）；AC-4.1 覆盖由 Flyway 种子保证 |
| localName 唯一约束与软删冲突 | DB 约束不受逻辑删除过滤（权威兜底）+ Service 预查重转友好提示 |
| appliesTo 取值漂移 | Service 枚举校验（白名单），非法值拒绝；前端下拉限定选项 |
| 供给接口破坏建模侧 | `/supply/v1/` 版本化 + 稳定化 VO（屏蔽审计字段，AC-5.7）；表结构变更走新 Flyway 版本（R-3） |
| V10 与其他分支 Flyway 撞车 | V10 只做 ALTER + INSERT；合并时按需调版本号 |
| 序列化/解析边界侵入 | 本功能只产注册表 + 供给；序列化/解析属建模侧（R-11），边界已在 1.2 声明 |

---

## 九、交付物清单

| 类型 | 文件 | 说明 |
|---|---|---|
| 新建 | `AnnotationProperty.java` | Entity（含 V10 增补 source 字段） |
| 新建 | `AnnotationPropertyMapper.java` | Mapper（MPJBaseMapper） |
| 新建 | `AnnotationPropertyService.java` + `AnnotationPropertyServiceImpl.java` | 注册表 CRUD + 弃用保护 + 供给列表 + 导出 Markdown |
| 新建 | `AnnotationPropertyController.java` | 注册表列表 + CRUD + 导出（10.4） |
| 修改 | `SupplyController.java`（DD2 已有） | 追加 `/annotation-properties` 端点（10.5） |
| 新建 | `AnnotationPropertySupplyVO.java` | 供给 VO（稳定化） |
| 新建 | `V10__ont_annotation_property_seed.sql` | source 列 + 12 项内置注释属性 + 权限点 10401~10405 |
| 新建 | `web/src/api/ontology/annotation-property.ts` | 前端 API |
| 新建 | `web/src/views/admin/ontology/annotation-property/index.vue` | 列表 + appliesTo 过滤 + 导出 + CRUD |
| 新建 | `web/src/views/admin/ontology/annotation-property/form.vue` | 新增/编辑对话框 |
| 新建 | `web/src/views/admin/ontology/annotation-property/i18n/{zh-cn,en}.ts` | 词条 |

---

## 十、与 PRD 边界的对齐确认（防混淆备忘）

| 边界点 | 本 DD 落地方式 | PRD 依据 |
|---|---|---|
| 注册表而非副本 | 注释属性全局唯一，集中声明注册表，不做属性模板式副本 | 附录 A / FR-4 |
| 只产资产 + 供给，不侵入序列化 | 本功能维护注册表 + 供给；序列化/解析遍历注册表驱动属建模侧 | 5.2 / R-11 / OoS |
| templateRef/unitRef/quantityKindRef 纳入注册表 | 三项作为溯源注解纳入 V10 种子（7010/7011/7012） | FR-4 第 6 条 |
| 表无 deprecated 字段 | PRD 9.6 未定义 deprecated，供给接口返回全量，不适用 AC-5.6 弃用过滤 | 9.6 / AC-5.6 |
| source 列为 V10 增补 | PRD 9.6 未定义 source；V10 增补以对齐全平台 builtin 保护范式 + 保护 AC-4.1 覆盖稳定性 | 9.6 / AC-4.1 |
| 注释属性不参与 OWL 推理 | 注释属性是元数据声明，注册表只供序列化/解析驱动 | OoS |
| localName 即 ont:xxx 的 xxx | 建模侧序列化时拼 `ont:` + localName；本功能只产 localName | FR-4 / 5.2 |

---

*本详细设计对应里程碑 M4，依赖 DD1（M0）基础设施（V4 表结构 + uk_ont_ap_local_name、V5 菜单 10400）与 DD2（M1）供给接口（SupplyController）。实现完成后，按《详细设计计划.md》依赖顺序进入 DD6（参考本体库浏览与导入，贯穿）；DD6 依赖 DD3（Brick 导入分类模板）+ DD4（QUDT 导入单位），CCO 注释属性浏览可复用本 DD 注册表范式。至此 FR-1~FR-5 治理资产（属性模板/分类模板/单位/注释属性/供给）全部就绪，M1~M4 收尾。*
