# 详细设计-DD3-分类模板体系与类层级镜像（M2）

| 项 | 内容 |
|---|---|
| 文档名称 | 详细设计-DD3-分类模板体系与类层级镜像 |
| 里程碑 | M2（分类模板体系 + 编码 + 类层级镜像） |
| 上游 PRD | 《本体模板化治理功能产品需求文档.md》v1.2 FR-2、FR-8、FR-9、NFR-10/11/12/13/14、第五节、第十节 10.2/10.5/10.7、第十三节、附录 D |
| 设计计划 | 《详细设计计划.md》DD3 |
| 编写日期 | 2026-07-28 |
| 文档状态 | 待实现 |
| 前置依赖 | DD1（M0）已完成：模块骨架、V4 建表（9.2/9.3/9.7/9.8 四表结构齐备）、V5 菜单种子（10200/10201~10204/10250）、V6 权限点（`ont_supply_view`/`ont_class_tpl_view`）；DD2（M1）已完成：属性模板库与供给接口落地、SupplyController 已建。 |

---

## 一、设计目标与范围

### 1.1 目标

在 DD1/DD2 基础上，实现**分类模板体系（父子继承 + 外观覆盖 + 结构骨架复用）+ 分类编码规范 + 类分类树只读镜像**，使 youming 具备：

- 维护一棵**可继承的分类模板树**（如"泵→喷淋泵/给水泵/污水泵"），子模板继承父模板的结构骨架（propertyRefs）与外观（icon/color），可在继承基础上**追加**专属字段、**覆盖**外观（FR-2）。
- 分类模板带**国标式分类编码**（如 30-01-01），编码规则可配置、编码随层级自动生成、承载父链前缀（FR-8）。
- 治理资产侧**供给"本体类分类树"建议**（据模板父链推荐 subClassOf 父类）与**只读镜像视图**（建模侧同步入库），边界严格：治理侧**不创建本体类、不主动登记类层级**（FR-9）。
- pig-ui "分类模板"页面（左树右表）可操作（树视图/详情/新增子节点/继承预览/编辑/删除/弃用/编码规则配置）。
- pig-ui "类分类树"页面（只读类树视图）展示已镜像的本体类层级，与分类模板树对照。
- 内置设备分类树种子（≥1 棵、≥2 级、≥3 子类，含规范编码）随 V8 入库（AC-2.1/AC-8.6）。

### 1.2 范围（本 DD 做 / 不做）

| 做（M2） | 不做（后续 DD） |
|---|---|
| ClassTemplate / ClassTemplateRef / ClassificationRule / ClassHierarchy 四 Entity + Mapper + Service + Controller | 单位注册表（DD4，FR-3） |
| 分类模板树（CRUD + 父子继承 + 外观覆盖 + 环路检测 + 删子校验） | 注释属性注册表（DD5，FR-4） |
| 继承视图计算（父链 propertyRefs 合并 + 去重 + 子覆盖父 + source 三态标记）+ Caffeine 缓存 | 参考本体浏览（DD6，FR-7） |
| 分类编码规则配置 + 编码生成器（自动/手填校验/前缀查询） | Brick 类批量导入到分类模板（DD6 的 `/reference/brick/class/import`，FR-7；本期 Brick 仅作为 source_ref 溯源占位） |
| 类分类树只读查询（FR-9 镜像读）+ 供给 suggest（FR-9 建议） | 建模侧序列化/个体实例（建模侧职责，非本功能） |
| 建模侧同步接口 `/sync/class-hierarchy`（FR-9 镜像写入来源，仅 `ont_sync_push`） | 批量重编码工具（R-14 本期仅预留） |
| V8 种子：设备分类树 + 编码规则 + 同步权限点 `ont_sync_push` | 跨分类树继承（本期父子继承仅限同一 tree_root 内） |
| 前端 `views/admin/ontology/class-template/index.vue`（左树右表 + 继承预览 + 编码规则抽屉） | |
| 前端 `views/admin/ontology/class-hierarchy/index.vue`（只读类树） | |
| 前端 `api/ontology/class-template.ts` + `class-hierarchy.ts` | |
| SupplyController 扩展：`/class-template/tree` + `/class-template/{code}/inherited` + `/class-hierarchy/suggest`（10.5） | |

> **边界声明（防混淆，呼应 PRD 5.2 / R-12）**：本 DD 维护两套并行关系，二者不混淆：
> - **模板继承**（template↔template）：`ont_class_template.parent_id` 自引用，治理资产侧原型复用，**本功能维护**。
> - **类层级**（class↔class）：`ont_class_hierarchy`（subClassOf），建模侧 `owl:Class` 间语义，**建模侧权威写入、本功能只读镜像 + 供给建议**。
> 子模板继承父模板 ≠ 子类 subClassOf 父类。本 DD **不创建 `owl:Class` 实体**（AC-2.8/AC-9.6 边界）。

### 1.3 验收映射（M2 DoD）

| PRD AC | 本 DD 实现点 |
|---|---|
| AC-2.1 内置设备分类树 ≥1 棵/≥2 级/≥3 子类 + 规范编码 | 6.2 V8 种子（设备 30 / 泵 30-01 / 喷淋泵 30-01-01 等） |
| AC-2.2 外观继承/覆盖 | 4.5 ClassTemplateServiceImpl 查详情时合并父外观（inherit_appearance=1 取父 icon/color，子覆盖优先） |
| AC-2.3 结构骨架继承（父 4 + 本 1 = 5，去重） | 4.6 InheritedViewService 合并父链 propertyRefs + Caffeine 缓存 |
| AC-2.4 propertyRefs 指向不存在 templateCode 校验失败 | 4.5 ServiceImpl.save/updateRefs 校验 templateCode 存在性 |
| AC-2.5 同名 propertyTemplateCode 子覆盖父 + UI 标注 | 4.6 合并算法（子优先）+ InheritedPropertyVO.source=overridden |
| AC-2.6 分类树禁止成环 | 4.7 CycleDetectorService DFS 自底向上查父链 |
| AC-2.7 删有子节点拒绝 | 4.5 ServiceImpl.removeTemplate 子节点计数校验 |
| AC-2.8 分类模板表无 RDF 序列化字段 | 6.1 确认（V4 已建，无 rdf:about/IRI 字段） |
| AC-8.1 编码规则可配置 + 仅影响新节点 | 4.8 ClassificationRuleController + 编码生成器只对新节点生效 |
| AC-8.2 新增子节点自动生成编码（30-01-01/02） | 4.8 ClassificationCodeGenerator 同级序号递增 |
| AC-8.3 classification_code 唯一约束 | V4 已建 `uk_ont_class_tpl_cls_code` + 4.5 Service 查重 |
| AC-8.4 按编码前缀查子树 | 4.5 Controller.page 支持 classificationCode 前缀过滤 |
| AC-8.5 手填编码校验（前缀一致 + 符合规则） | 4.8 ClassificationCodeGenerator.validate |
| AC-8.6 内置种子编码符合规则 | 6.2 V8 种子（30/30-01/30-01-01） |
| AC-8.7 classification_code 与 templateCode 并存可查 | 4.5 page 支持双字段查询 |
| AC-9.1 suggest 返回建议父类 | 4.10 SupplyController.suggestClassHierarchy |
| AC-9.2 建模侧推送 → 只读镜像入库 + 溯源 + sync_status | 4.9 SyncController.pushClassHierarchy |
| AC-9.3 不提供类层级登记/删除（治理侧）接口 | 4.9 仅只读 GET + 建模侧 sync（无 POST 登记） |
| AC-9.4 类层级视图展示 + 与模板树对照 | 5.4 class-hierarchy 页面 |
| AC-9.5 Brick 导入映射分类模板树，不写 hierarchy | 本期不实现 Brick 导入（DD6），边界已在 4.10 备注锁定 |
| AC-9.6 不创建 owl:Class 实体 | 全设计无类实体写入（ont_class_hierarchy 仅存 IRI 镜像） |

---

## 二、落地清单

### 2.1 后端文件清单

> 包根 `com.pig4cloud.pig.ontology`，分层 `controller` / `service`(+`impl`) / `mapper` / `api/entity` / `api/dto` / `api/vo`。对齐 DD2 已有范式（PropertyTemplate*）。

```
server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/
├── api/
│   ├── entity/
│   │   ├── ClassTemplate.java                  # 分类模板（9.2）
│   │   ├── ClassTemplateRef.java               # 分类模板属性/关系引用（9.3）
│   │   ├── ClassificationRule.java             # 分类编码规则（9.7）
│   │   └── ClassHierarchy.java                 # 类分类树镜像（9.8，只读）
│   ├── dto/
│   │   ├── ClassTemplateSaveDTO.java           # 新增/编辑分类模板（含 propertyRefs）
│   │   └── ClassHierarchySyncDTO.java          # 建模侧同步推送 DTO（批量 subClassOf）
│   └── vo/
│       ├── ClassTemplateNodeVO.java            # 分类树节点（含编码/继承预览/外观）
│       ├── ClassTemplateDetailVO.java          # 分类模板详情（外观合并 + 本节点 refs）
│       ├── InheritedPropertyVO.java            # 继承视图单属性（含 source 三态标记）
│       ├── InheritedViewVO.java                # 继承视图（合并属性清单 + 外观 + 父链）
│       └── ClassHierarchyNodeVO.java           # 类层级树节点（只读）
├── controller/
│   ├── ClassTemplateController.java            # 分类模板 CRUD + 树 + 详情 + 继承视图
│   ├── ClassificationRuleController.java       # 编码规则查询/配置
│   ├── ClassHierarchyController.java           # 类分类树只读查询
│   └── SyncController.java                     # 建模侧同步（10.7，仅 ont_sync_push）
│   （SupplyController.java                     # 复用 DD2 已有，扩展 class-template/suggest 端点）
├── service/
│   ├── ClassTemplateService.java
│   ├── ClassificationRuleService.java
│   ├── ClassHierarchyService.java
│   ├── InheritedViewService.java               # 继承视图计算 + 缓存
│   ├── CycleDetectorService.java               # 环路检测 DFS
│   ├── ClassificationCodeGenerator.java        # 编码生成/校验
│   └── impl/
│       ├── ClassTemplateServiceImpl.java
│       ├── ClassificationRuleServiceImpl.java
│       └── ClassHierarchyServiceImpl.java
└── mapper/
    ├── ClassTemplateMapper.java
    ├── ClassTemplateRefMapper.java
    ├── ClassificationRuleMapper.java
    └── ClassHierarchyMapper.java
```

> 说明：InheritedViewService、CycleDetectorService、ClassificationCodeGenerator 为纯逻辑 Service（无独立 Entity），由 ClassTemplateServiceImpl 组合调用，体现"继承/环路/编码"三个核心算法的单一职责。

### 2.2 数据库文件清单

> **V4 已建好四张表（结构齐备），本期不返工 V4**，只新增 V8 种子。

```
server/pig-common/pig-common-data/src/main/resources/db/migration/
├── V4__ont_governance_schema.sql                # 已有（DD1，四表结构齐备）
├── V5__ont_governance_seed.sql                  # 已有（DD1，sys_menu 10200/10201~10204/10250）
├── V6__ont_supply_permission_seed.sql           # 已有（DD2，ont_supply_view/ont_class_tpl_view）
├── V7__ont_property_template_rename_values.sql  # 已有（DD2，values→enum_values）
└── V8__ont_class_template_seed.sql              # 新增：设备分类树 + 编码规则 + 同步权限点
```

### 2.3 依赖变更清单

```
server/pig-ontology/pig-ontology-biz/pom.xml     # 修改：新增 caffeine 依赖（继承视图缓存 NFR-12）
```

### 2.4 前端文件清单

```
web/src/
├── api/ontology/
│   ├── class-template.ts                        # 分类模板 API（树/详情/继承/CRUD/编码规则）
│   └── class-hierarchy.ts                       # 类分类树 API（只读）
└── views/admin/ontology/
    ├── class-template/
    │   ├── index.vue                            # 左树右表（复用 QueryTree）
    │   ├── form.vue                             # 新增/编辑对话框（含编码预览 + 父节点选择 + refs 配置）
    │   ├── inherited-panel.vue                  # 继承视图面板（属性清单 + source 三态标记）
    │   ├── rule-drawer.vue                      # 编码规则配置抽屉
    │   ├── composables.ts                       # 页面状态复用（对齐 property-template/composables.ts）
    │   └── i18n/
    │       ├── zh-cn.ts
    │       └── en.ts
    └── class-hierarchy/
        ├── index.vue                            # 只读类树视图
        └── i18n/
            ├── zh-cn.ts
            └── en.ts
```

> 路径说明：sys_menu path 为 `/admin/ontology/class-template/index` 与 `/admin/ontology/class-hierarchy/index`（V5 已配），backEnd.ts 用 `import.meta.glob('../views/**/*.{vue,tsx}')` 按 path 匹配，故 vue 文件须放 `views/admin/ontology/class-template/index.vue`（对齐 DD2 已落地的 `views/admin/ontology/property-template/index.vue` 范式）。

---

## 三、数据库设计（V8 种子）

### 3.1 V8 种子范围

V4 已建表结构（9.2/9.3/9.7/9.8），V5 已建菜单（10200/10201~10204/10250），V6 已补 `ont_class_tpl_view`。V8 只做三件事：

1. **(a) 内置设备分类树种子**（AC-2.1/AC-8.6）：1 棵、3 级、3 子类，含规范编码。
2. **(b) 编码规则种子**（AC-8.1）：equipment 树默认规则（分隔符 `-`、每级 2 位、基数 30、零填充）。
3. **(c) 同步权限点 `ont_sync_push`**（AC-9.2）：挂在"类分类树"菜单下，供建模侧服务账号授予。

### 3.2 `V8__ont_class_template_seed.sql`

```sql
-- ============================================================
-- V8__ont_class_template_seed.sql
-- 分类模板内置设备树 + 编码规则 + 建模侧同步权限点
-- 对应 PRD v1.2 FR-2 AC-2.1、FR-8 AC-8.1/8.6、FR-9 AC-9.2
-- 依赖 V4（表结构）、V5（菜单）、V6（ont_class_tpl_view）
-- ============================================================

-- ---------- (a) 编码规则（先于分类树，分类树编码遵循此规则） ----------
-- equipment 树：分隔符 '-'、每级 2 位、根级基数 30、零填充
INSERT INTO ont_classification_rule (id, tree_root, separator, level_digits, base_number, zero_pad, description, create_by, create_time, update_by, update_time, del_flag)
VALUES (3001, 'equipment', '-', 2, 30, '1', '设备分类树默认编码规则（GB/T 51269 式：分隔符-、每级2位、基数30、零填充）', 'admin', now(), 'admin', now(), '0');

-- ---------- (b) 内置设备分类树（参照 Brick 设备类，3 级 / 3 子类） ----------
-- 编码前缀即父链：30 -> 30-01 -> 30-01-01/02/03
-- id 用语义化固定值便于跨脚本引用；parent_id 指向上层分类模板 id
-- tree_root='equipment'（与编码规则 tree_root 对应）

-- L0 根：设备
INSERT INTO ont_class_template (id, template_code, classification_code, label, label_cn, description, parent_id, tree_root, icon, color, inherit_appearance, source, source_ref, deprecated, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (3100, 'equipment', '30', '设备', '设备', '设备根分类（参照 Brick:Equipment）', NULL, 'equipment', '🏗️', '#1890ff', '1', 'builtin', 'brick', '0', 0, 'admin', now(), 'admin', now(), '0');

-- L1：泵（设备下第 1 个子类）
INSERT INTO ont_class_template (id, template_code, classification_code, label, label_cn, description, parent_id, tree_root, icon, color, inherit_appearance, source, source_ref, deprecated, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (3101, 'pump', '30-01', '泵', '泵', '泵类设备（参照 Brick:Pump）', 3100, 'equipment', '💧', '#1890ff', '1', 'builtin', 'brick', '0', 1, 'admin', now(), 'admin', now(), '0');

-- L2：喷淋泵 / 给水泵 / 污水泵（泵下 3 个子类）
INSERT INTO ont_class_template (id, template_code, classification_code, label, label_cn, description, parent_id, tree_root, icon, color, inherit_appearance, source, source_ref, deprecated, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES
(3102, 'spray-pump',     '30-01-01', '喷淋泵', '喷淋泵', '喷淋系统用泵',         3101, 'equipment', NULL, NULL, '1', 'builtin', 'brick', '0', 1, 'admin', now(), 'admin', now(), '0'),
(3103, 'feedwater-pump', '30-01-02', '给水泵', '给水泵', '给水系统用泵',         3101, 'equipment', NULL, NULL, '1', 'builtin', 'brick', '0', 2, 'admin', now(), 'admin', now(), '0'),
(3104, 'sewage-pump',    '30-01-03', '污水泵', '污水泵', '排污用泵',             3101, 'equipment', NULL, NULL, '1', 'builtin', 'brick', '0', 3, 'admin', now(), 'admin', now(), '0');

-- ---------- (c) 泵模板的结构骨架（本节点新增属性，AC-2.3 的父 4 属性） ----------
-- 引用 V5 内置属性模板（template_code）：额定流量/扬程/功率/介质
-- 注：V5 仅含 12 个通用属性（name/description/email/...），不含设备专属属性；
--     为满足 AC-2.3「泵 4 属性」演示继承，此处用通用属性模拟骨架，ref_type=property。
--     真实设备属性（ratedFlow/head/power/medium）由治理员后续 custom 扩展，不在内置种子。
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
VALUES
(4001, 3101, 'name',        'property', 1, '0', 'admin', now(), 'admin', now(), '0'),
(4002, 3101, 'description', 'property', 2, '0', 'admin', now(), 'admin', now(), '0'),
(4003, 3101, 'amount',      'property', 3, '0', 'admin', now(), 'admin', now(), '0'),
(4004, 3101, 'isActive',    'property', 4, '0', 'admin', now(), 'admin', now(), '0');

-- ---------- (d) 同步权限点 ont_sync_push（建模侧服务账号授予，FR-9 AC-9.2） ----------
-- 挂在"类分类树"菜单（10250）下，仅建模侧服务账号/角色持有，治理员不持有
INSERT INTO sys_menu VALUES (10251, '类层级同步', 'ont_sync_push', NULL, NULL, 10250, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
```

> 说明：
> - **V8 只做 INSERT，不改 V4/V5/V6/V7**（禁改已应用脚本，checksum 校验）。
> - 设备树 L2（3102~3104）icon/color 留 NULL + inherit_appearance='1'，用于验证 AC-2.2「子模板未覆盖外观时继承父外观」（查询时合并父泵 `💧/#1890ff`）。
> - 泵模板 ref 引用 V5 通用属性（name/description/amount/isActive），满足 AC-2.3「父 4 属性」的演示需求；治理员可后续 custom 追加设备专属属性（ratedFlow 等），不在内置种子。
> - `ont_sync_push` 权限点 menu_id=10251（10250 段位，避开 10201~10205）。仅授予建模侧服务账号，呼应 13.2「角色分配由 pig 角色管理完成」。
> - 内置分类树种子不写 `ont_class_hierarchy`（后者只镜像建模侧类层级，AC-9.5 边界）。

---

## 四、后端设计

### 4.1 Entity `ClassTemplate.java`

```java
package com.pig4cloud.pig.ontology.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@Schema(description = "分类模板")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_class_template")
public class ClassTemplate extends Model<ClassTemplate> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "唯一标识，人类可读，如 pump/spray-pump")
	private String templateCode;

	@Schema(description = "规范分类编码，如 30-01-01（承载层级）")
	private String classificationCode;

	@Schema(description = "显示名")
	private String label;

	@Schema(description = "中文名")
	private String labelCn;

	@Schema(description = "业务说明")
	private String description;

	@Schema(description = "父分类模板 id，NULL=根节点")
	private Long parentId;

	@Schema(description = "所属分类树标识，如 equipment")
	private String treeRoot;

	@Schema(description = "外观：emoji 或图标类名")
	private String icon;

	@Schema(description = "外观：hex 色值")
	private String color;

	@Schema(description = "0/1 是否继承父外观（默认 1）")
	private String inheritAppearance;

	@Schema(description = "builtin / custom")
	private String source;

	@Schema(description = "来源本体标识，如 brick")
	private String sourceRef;

	@Schema(description = "0/1 弃用标记")
	private String deprecated;

	@Schema(description = "同级排序")
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

> 说明：字段与 V4 `ont_class_template` 一一对应（含 sort_order，对齐 PropertyTemplate 范式）。`classification_code` 与 `template_code` 双唯一约束由 V4 已建（`uk_ont_class_tpl_code` / `uk_ont_class_tpl_cls_code`，AC-2.8/AC-8.3）。**无任何 RDF/IRI 字段**（AC-2.8 边界）。

### 4.2 Entity `ClassTemplateRef.java` / `ClassificationRule.java` / `ClassHierarchy.java`

```java
// ClassTemplateRef.java（9.3 结构骨架）
@Data
@Schema(description = "分类模板属性/关系引用")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_class_template_ref")
public class ClassTemplateRef extends Model<ClassTemplateRef> {
	@TableId(type = IdType.ASSIGN_ID) private Long id;
	@Schema(description = "-> ont_class_template.id") private Long classTemplateId;
	@Schema(description = "-> ont_property_template.template_code") private String propertyTemplateCode;
	@Schema(description = "property / relationship") private String refType;
	@Schema(description = "注入顺序") private Integer sortOrder;
	@Schema(description = "0/1 继承自父 vs 本节点新增") private String inheritFlag;
	// 审计字段 + delFlag（同 ClassTemplate 范式，略）
}
```

```java
// ClassificationRule.java（9.7 编码规则）
@Data
@Schema(description = "分类编码规则")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_classification_rule")
public class ClassificationRule extends Model<ClassificationRule> {
	@TableId(type = IdType.ASSIGN_ID) private Long id;
	@Schema(description = "所属分类树标识，对应 ont_class_template.tree_root") private String treeRoot;
	@Schema(description = "分隔符，默认 -") private String separator;
	@Schema(description = "每级位数，默认 2") private Integer levelDigits;
	@Schema(description = "根级编码基数，如 30") private Integer baseNumber;
	@Schema(description = "0/1 是否零填充") private String zeroPad;
	@Schema(description = "说明") private String description;
	// 审计字段 + delFlag（略）
}
```

```java
// ClassHierarchy.java（9.8 类分类树镜像，只读）
@Data
@Schema(description = "本体类分类树镜像（subClassOf，只读）")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_class_hierarchy")
public class ClassHierarchy extends Model<ClassHierarchy> {
	@TableId(type = IdType.ASSIGN_ID) private Long id;
	@Schema(description = "子类 IRI（建模侧类，只镜像不创建）") private String childClassIri;
	@Schema(description = "父类 IRI") private String parentClassIri;
	@Schema(description = "溯源：建议来源的分类模板 template_code") private String sourceTemplateRef;
	@Schema(description = "所属类树标识") private String treeRoot;
	@Schema(description = "0=待同步 1=已同步 2=已失效") private String syncStatus;
	@Schema(description = "最近同步时间") private LocalDateTime syncTime;
	// 审计字段 + delFlag（略）
}
```

> 说明：四 Mapper 均继承 `MPJBaseMapper<T>`（对齐 DD2 PropertyTemplateMapper），无自定义 SQL（继承/编码/环路逻辑在 Service 层用 Wrappers + 内存计算）。

### 4.3 Controller `ClassTemplateController.java`

```java
package com.pig4cloud.pig.ontology.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.dto.ClassTemplateSaveDTO;
import com.pig4cloud.pig.ontology.api.vo.*;
import com.pig4cloud.pig.ontology.service.ClassTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/class-template")
@Tag(name = "分类模板管理", description = "分类模板树 + 继承 + 编码（FR-2/FR-8）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ClassTemplateController {

	private final ClassTemplateService classTemplateService;

	@GetMapping("/tree")
	@Operation(summary = "分类模板树", description = "按 treeRoot 查询，含编码/继承预览/外观（10.2）")
	@HasPermission("ont_class_tpl_view")
	public R<List<ClassTemplateNodeVO>> tree(@RequestParam String treeRoot,
											 @RequestParam(defaultValue = "false") Boolean includeDeprecated) {
		return R.ok(classTemplateService.tree(treeRoot, includeDeprecated));
	}

	@GetMapping("/{id}")
	@Operation(summary = "详情", description = "含外观合并（inherit_appearance=1 取父外观）+ 本节点 refs")
	@HasPermission("ont_class_tpl_view")
	public R<ClassTemplateDetailVO> getById(@PathVariable Long id) {
		return R.ok(classTemplateService.getDetail(id));
	}

	@GetMapping("/{id}/inherited")
	@Operation(summary = "继承视图", description = "父链合并后的全部属性（区分继承/新增/覆盖）+ 外观（10.2，AC-2.3）")
	@HasPermission("ont_class_tpl_view")
	public R<InheritedViewVO> inherited(@PathVariable Long id) {
		return R.ok(classTemplateService.inheritedView(id));
	}

	@GetMapping("/page")
	@Operation(summary = "分页查询", description = "支持 classificationCode 前缀查子树（AC-8.4）+ templateCode 模糊")
	@HasPermission("ont_class_tpl_view")
	public R page(@ParameterObject com.baomidou.mybatisplus.extension.plugins.pagination.Page page,
				 @ParameterObject ClassTemplate template) {
		return R.ok(classTemplateService.page(page, template));
	}

	@SysLog("新增分类模板")
	@PostMapping
	@Operation(summary = "新增", description = "含 parent_id；classification_code 自动生成或校验（FR-8）")
	@HasPermission("ont_class_tpl_manage")
	public R save(@Valid @RequestBody ClassTemplateSaveDTO dto) {
		return classTemplateService.saveTemplate(dto);
	}

	@SysLog("编辑分类模板")
	@PutMapping("/{id}")
	@Operation(summary = "编辑", description = "builtin 拒绝；改 parent 校验不成环（AC-2.6）")
	@HasPermission("ont_class_tpl_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody ClassTemplateSaveDTO dto) {
		dto.setId(id);
		return classTemplateService.updateTemplate(dto);
	}

	@SysLog("删除分类模板")
	@DeleteMapping("/{id}")
	@Operation(summary = "删除", description = "有子节点拒绝（AC-2.7）；custom 软删")
	@HasPermission("ont_class_tpl_manage")
	public R removeById(@PathVariable Long id) {
		return classTemplateService.removeTemplate(id);
	}

	@SysLog("弃用分类模板")
	@PutMapping("/{id}/deprecate")
	@Operation(summary = "弃用/恢复")
	@HasPermission("ont_class_tpl_manage")
	public R deprecate(@PathVariable Long id, @RequestParam(defaultValue = "1") String deprecated) {
		return classTemplateService.deprecate(id, deprecated);
	}

	@GetMapping("/code/preview")
	@Operation(summary = "编码预览", description = "据 parentId + 规则生成下一编码（前端新增子节点用，AC-8.2）")
	@HasPermission("ont_class_tpl_view")
	public R<String> previewCode(@RequestParam Long parentId) {
		return R.ok(classTemplateService.previewClassificationCode(parentId));
	}
}
```

> 说明：路径 `/class-template/**`，对外 `/admin/ont/class-template/**`（对齐 PRD 10.2 + DD2 PropertyTemplateController 范式）。权限点 `ont_class_tpl_view`（V6 已建）/ `ont_class_tpl_manage`（V5 已建 10201~10203）。

### 4.4 Controller `ClassificationRuleController.java` / `ClassHierarchyController.java`

```java
// ClassificationRuleController.java（FR-8 编码规则，10.2）
@RestController
@AllArgsConstructor
@RequestMapping("/classification-rule")
@Tag(name = "分类编码规则", description = "编码规则查询/配置（FR-8）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ClassificationRuleController {

	private final ClassificationRuleService classificationRuleService;

	@GetMapping
	@Operation(summary = "查询编码规则", description = "按 treeRoot 查（10.2）")
	@HasPermission("ont_class_tpl_view")
	public R<ClassificationRule> get(@RequestParam String treeRoot) {
		return R.ok(classificationRuleService.getByTreeRoot(treeRoot));
	}

	@SysLog("配置分类编码规则")
	@PutMapping
	@Operation(summary = "配置编码规则", description = "仅影响新节点（AC-8.1）；treeRoot 不存在则新建")
	@HasPermission("ont_class_tpl_manage")
	public R saveOrUpdate(@RequestBody ClassificationRule rule) {
		return classificationRuleService.saveOrUpdateRule(rule);
	}
}
```

```java
// ClassHierarchyController.java（FR-9 类分类树，只读查询，10.2）
@RestController
@AllArgsConstructor
@RequestMapping("/class-hierarchy")
@Tag(name = "类分类树（只读镜像）", description = "本体类分类树视图（FR-9，只读）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ClassHierarchyController {

	private final ClassHierarchyService classHierarchyService;

	@GetMapping("/tree")
	@Operation(summary = "类分类树视图", description = "已镜像 subClassOf，只读（10.2，AC-9.4）")
	@HasPermission("ont_class_tpl_view")
	public R<List<ClassHierarchyNodeVO>> tree(@RequestParam(required = false) String treeRoot) {
		return R.ok(classHierarchyService.tree(treeRoot));
	}

	@GetMapping("/page")
	@Operation(summary = "分页查询镜像边", description = "支持 syncStatus 过滤")
	@HasPermission("ont_class_tpl_view")
	public R page(@ParameterObject com.baomidou.mybatisplus.extension.plugins.pagination.Page page,
				 @ParameterObject ClassHierarchy filter) {
		return R.ok(classHierarchyService.page(page, filter));
	}
}
```

> **边界（AC-9.3）**：ClassHierarchyController **仅 GET**，无 POST/PUT/DELETE 登记/删除接口。类层级写入只通过 SyncController（建模侧权威源，下一节）。

### 4.5 Service `ClassTemplateServiceImpl.java`（关键逻辑）

> 受篇幅，此处给出**方法签名 + 关键逻辑伪代码**，对齐 DD2 ServiceImpl 的写法。

```java
package com.pig4cloud.pig.ontology.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.dto.ClassTemplateSaveDTO;
import com.pig4cloud.pig.ontology.api.entity.*;
import com.pig4cloud.pig.ontology.api.vo.*;
import com.pig4cloud.pig.ontology.mapper.*;
import com.pig4cloud.pig.ontology.service.*;
import com.pig4cloud.pig.ontology.service.ClassTemplateService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@AllArgsConstructor
@Service
public class ClassTemplateServiceImpl extends ServiceImpl<ClassTemplateMapper, ClassTemplate>
		implements ClassTemplateService {

	private final ClassTemplateRefMapper refMapper;
	private final PropertyTemplateMapper propertyTemplateMapper;   // 复用 DD2 Mapper
	private final InheritedViewService inheritedViewService;
	private final CycleDetectorService cycleDetectorService;
	private final ClassificationCodeGenerator codeGenerator;

	@Override
	public List<ClassTemplateNodeVO> tree(String treeRoot, Boolean includeDeprecated) {
		// 1. 按 treeRoot 查全量（includeDeprecated=false 时过滤 deprecated='0'），按 sort_order 排序
		List<ClassTemplate> list = list(Wrappers.<ClassTemplate>lambdaQuery()
				.eq(ClassTemplate::getTreeRoot, treeRoot)
				.eq(!includeDeprecated, ClassTemplate::getDeprecated, "0")
				.orderByAsc(ClassTemplate::getSortOrder));
		// 2. 转节点 VO（含 classificationCode + label + icon + color 合并父外观）
		// 3. 用 handleTree 等价逻辑（parentId=0/null 为根）组装成树
		return buildTree(list);
	}

	@Override
	public ClassTemplateDetailVO getDetail(Long id) {
		ClassTemplate tpl = getById(id);
		// 外观合并：inherit_appearance='1' 且自身 icon/color 为空 -> 取父链最近非空外观
		mergeAppearanceFromParent(tpl);
		// 本节点 refs（不含继承的，继承视图单独接口）
		List<ClassTemplateRef> refs = refMapper.selectList(Wrappers.<ClassTemplateRef>lambdaQuery()
				.eq(ClassTemplateRef::getClassTemplateId, id)
				.orderByAsc(ClassTemplateRef::getSortOrder));
		return ClassTemplateDetailVO.of(tpl, refs);
	}

	@Override
	public InheritedViewVO inheritedView(Long id) {
		// 委托 InheritedViewService（含 Caffeine 缓存，见 4.6）
		return inheritedViewService.compute(id);
	}

	@Override
	public IPage<ClassTemplate> page(Page page, ClassTemplate template) {
		return baseMapper.selectPage(page,
				Wrappers.<ClassTemplate>lambdaQuery()
					.eq(StrUtil.isNotBlank(template.getTreeRoot()), ClassTemplate::getTreeRoot, template.getTreeRoot())
					// AC-8.4：classificationCode 前缀查子树（如 30-01 -> 30-01-01/02/03）
					.likeRight(StrUtil.isNotBlank(template.getClassificationCode()),
							ClassTemplate::getClassificationCode, template.getClassificationCode())
					.and(StrUtil.isNotBlank(template.getTemplateCode()),
							w -> w.like(ClassTemplate::getTemplateCode, template.getTemplateCode())
									.or().like(ClassTemplate::getLabel, template.getTemplateCode()))
					.eq(StrUtil.isNotBlank(template.getDeprecated()), ClassTemplate::getDeprecated, template.getDeprecated())
					.orderByAsc(ClassTemplate::getClassificationCode));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveTemplate(ClassTemplateSaveDTO dto) {
		// 1. templateCode / classificationCode 查重（AC-8.3）
		long codeCnt = count(Wrappers.<ClassTemplate>lambdaQuery()
				.eq(ClassTemplate::getTemplateCode, dto.getTemplateCode()));
		if (codeCnt > 0) return R.failed("模板标识 '" + dto.getTemplateCode() + "' 已存在");

		// 2. classification_code 生成或校验（FR-8）
		String clsCode = resolveClassificationCode(dto);   // 见 4.8
		long clsCnt = count(Wrappers.<ClassTemplate>lambdaQuery()
				.eq(ClassTemplate::getClassificationCode, clsCode));
		if (clsCnt > 0) return R.failed("分类编码 '" + clsCode + "' 已存在");

		// 3. 父节点校验 + treeRoot 一致性（父子须同 tree_root）
		if (dto.getParentId() != null) {
			ClassTemplate parent = getById(dto.getParentId());
			if (parent == null) return R.failed("父节点不存在");
			if (!parent.getTreeRoot().equals(dto.getTreeRoot()))
				return R.failed("子节点与父节点须属于同一分类树");
		}

		// 4. propertyRefs 校验：指向的 templateCode 必须存在（AC-2.4）
		R refCheck = validateRefs(dto.getPropertyRefs());
		if (refCheck != null) return refCheck;

		// 5. 落库（source=custom）
		ClassTemplate tpl = dto.toEntity();
		tpl.setClassificationCode(clsCode);
		tpl.setSource("custom");
		tpl.setDeprecated("0");
		save(tpl);
		saveRefs(tpl.getId(), dto.getPropertyRefs());
		inheritedViewService.invalidate(tpl.getId());   // 失效缓存
		return R.ok(tpl);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateTemplate(ClassTemplateSaveDTO dto) {
		ClassTemplate existing = getById(dto.getId());
		if (existing == null) return R.failed("模板不存在");
		if ("builtin".equals(existing.getSource())) return R.failed("内置模板不可编辑");   // 对齐 DD2 builtin 保护

		// templateCode 不可改（引用稳定性，对齐 DD2）
		dto.setTemplateCode(existing.getTemplateCode());

		// 改 parent 须校验不成环（AC-2.6）+ treeRoot 一致
		if (dto.getParentId() != null && !dto.getParentId().equals(existing.getParentId())) {
			if (cycleDetectorService.wouldCreateCycle(dto.getId(), dto.getParentId()))
				return R.failed("设置该父节点会导致分类树成环");
		}

		// classification_code 手填时校验前缀一致 + 符合规则（AC-8.5）
		if (StrUtil.isNotBlank(dto.getClassificationCode())
				&& !dto.getClassificationCode().equals(existing.getClassificationCode())) {
			R codeValid = codeGenerator.validateManual(dto.getClassificationCode(), dto.getParentId(), dto.getTreeRoot());
			if (codeValid.isError()) return codeValid;
		}

		R refCheck = validateRefs(dto.getPropertyRefs());
		if (refCheck != null) return refCheck;

		updateById(dto.toEntity());
		// refs 全量替换（先删后插）
		refMapper.delete(Wrappers.<ClassTemplateRef>lambdaQuery().eq(ClassTemplateRef::getClassTemplateId, dto.getId()));
		saveRefs(dto.getId(), dto.getPropertyRefs());
		inheritedViewService.invalidate(dto.getId());
		return R.ok(true);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeTemplate(Long id) {
		ClassTemplate existing = getById(id);
		if (existing == null) return R.failed("模板不存在");
		if ("builtin".equals(existing.getSource())) return R.failed("内置模板不可删除");
		// AC-2.7：有子节点拒绝
		long childCnt = count(Wrappers.<ClassTemplate>lambdaQuery().eq(ClassTemplate::getParentId, id));
		if (childCnt > 0) return R.failed("存在子节点，不可直接删除（请先处理子节点）");
		refMapper.delete(Wrappers.<ClassTemplateRef>lambdaQuery().eq(ClassTemplateRef::getClassTemplateId, id));
		removeById(id);
		inheritedViewService.invalidate(id);
		return R.ok(true);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R deprecate(Long id, String deprecated) {
		return R.ok(update(Wrappers.<ClassTemplate>lambdaUpdate()
				.eq(ClassTemplate::getId, id).set(ClassTemplate::getDeprecated, deprecated)));
	}

	@Override
	public String previewClassificationCode(Long parentId) {
		return codeGenerator.nextCode(parentId);   // 见 4.8
	}

	// —— 私有辅助 ——
	private R validateRefs(List<ClassTemplateSaveDTO.RefItem> refs) {
		if (refs == null || refs.isEmpty()) return null;
		// 收集所有 property_template_code，批量查 ont_property_template 存在性（AC-2.4）
		Set<String> codes = new HashSet<>();
		for (var r : refs) codes.add(r.getPropertyTemplateCode());
		Long existCnt = propertyTemplateMapper.selectCount(Wrappers.<com.pig4cloud.pig.ontology.api.entity.PropertyTemplate>lambdaQuery()
				.in(com.pig4cloud.pig.ontology.api.entity.PropertyTemplate::getTemplateCode, codes));
		if (existCnt < codes.size()) {
			return R.failed("结构骨架存在不存在的属性模板标识（property_template_code），请检查");
		}
		return null;
	}

	private String resolveClassificationCode(ClassTemplateSaveDTO dto) {
		// 手填且校验通过 -> 用手填；否则自动生成（AC-8.5）
		if (StrUtil.isNotBlank(dto.getClassificationCode())) return dto.getClassificationCode();
		return codeGenerator.nextCode(dto.getParentId());
	}
}
```

> 说明：
> - `page` 用 `likeRight` 实现编码前缀查子树（AC-8.4：`30-01` → `30-01-01/02/03`）。
> - `saveTemplate` 校验链：templateCode 查重 → classificationCode 生成/查重 → 父节点存在 + treeRoot 一致 → refs templateCode 存在性（AC-2.4）→ 落库 + 失效缓存。
> - `updateTemplate` 改 parent 触发环路检测（AC-2.6），手填编码触发规则校验（AC-8.5）。
> - `removeTemplate` 子节点计数拒绝（AC-2.7）+ builtin 保护（对齐 DD2）。
> - 任何写操作后 `inheritedViewService.invalidate(id)` 失效缓存（NFR-12 缓存一致性）。

### 4.6 InheritedViewService（继承视图计算 + 缓存，核心算法）

> **算法目标（AC-2.3/AC-2.5/NFR-12/NFR-14）**：给定一个分类模板 id，沿 parent_id 自底向上收集父链，合并全部 propertyRefs：
> - 去重键 = `propertyTemplateCode`；
> - 子模板同名覆盖父（AC-2.5）；
> - 每条属性带 `source` 三态：`node`(本节点新增) / `inherited`(继承自父) / `overridden`(覆盖父同名)（NFR-14）；
> - 外观按 `inherit_appearance` 合并父链最近非空 icon/color（AC-2.2）；
> - 结果按 Caffeine 缓存（key=templateCode/id，TTL 5min，模板变更失效）（NFR-12）。

```java
package com.pig4cloud.pig.ontology.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pig4cloud.pig.ontology.api.entity.*;
import com.pig4cloud.pig.ontology.api.vo.*;
import com.pig4cloud.pig.ontology.mapper.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@Service
public class InheritedViewService {

	private final ClassTemplateMapper classTemplateMapper;
	private final ClassTemplateRefMapper refMapper;

	// Caffeine 本地缓存：key=classTemplateId，TTL 5min（NFR-12）
	private final Cache<Long, InheritedViewVO> cache = Caffeine.newBuilder()
			.expireAfterWrite(5, TimeUnit.MINUTES)
			.maximumSize(1000)
			.build();

	public InheritedViewVO compute(Long id) {
		return cache.get(id, this::doCompute);
	}

	public void invalidate(Long id) {
		// 失效自身 + 所有后代（后代继承视图依赖本节点，AC-2.3 一致性）
		cache.invalidate(id);
	 invalidateDescendants(id);
	}

	private InheritedViewVO doCompute(Long id) {
		// 1. 自底向上收集父链（根在前，本节点在末尾）
		LinkedList<ClassTemplate> chain = new LinkedList<>();
		ClassTemplate cur = classTemplateMapper.selectById(id);
		Set<Long> visited = new HashSet<>();   // 防御性去环（数据异常时兜底）
		while (cur != null && visited.add(cur.getId())) {
			chain.addFirst(cur);
			cur = cur.getParentId() == null ? null : classTemplateMapper.selectById(cur.getParentId());
		}
		// 2. 按父链顺序收集各节点 refs（根在前，子覆盖父）
		Map<String, InheritedPropertyVO> merged = new LinkedHashMap<>();   // 保持插入顺序
		for (ClassTemplate node : chain) {
			List<ClassTemplateRef> refs = refMapper.selectList(
					Wrappers.<ClassTemplateRef>lambdaQuery()
						.eq(ClassTemplateRef::getClassTemplateId, node.getId())
						.orderByAsc(ClassTemplateRef::getSortOrder));
			for (ClassTemplateRef ref : refs) {
				String code = ref.getPropertyTemplateCode();
				InheritedPropertyVO vo = InheritedPropertyVO.of(ref, node);
				if (merged.containsKey(code)) {
					vo.setSource("overridden");   // 覆盖父同名（AC-2.5，NFR-14）
				} else if (!node.getId().equals(id)) {
					vo.setSource("inherited");    // 继承自父
				} else {
					vo.setSource("node");         // 本节点新增
				}
				merged.put(code, vo);   // 同名覆盖
			}
		}
		// 3. 外观合并：本节点 inherit_appearance='1' 且自身 icon/color 空 -> 取父链最近非空
		ClassTemplate self = chain.getLast();
		mergeAppearanceFromParent(self, chain);
		// 4. 组装 VO（含父链 templateCode 列表，供溯源）
		return InheritedViewVO.of(self, new ArrayList<>(merged.values()), parentCodes(chain));
	}
	// invalidateDescendants / mergeAppearanceFromParent / parentCodes 等辅助方法略
}
```

> 说明：
> - 缓存 key=classTemplateId，TTL 5min（NFR-12 P95<300ms）；写操作经 `invalidate(id)` 主动失效（含后代，保证继承视图一致性）。
> - `doCompute` 的 `visited` 集合做防御性去环——正常数据已被 CycleDetectorService 拦截，此处仅兜底（防止历史脏数据/并发写入导致栈溢出）。
> - 合并顺序：根在前、本节点在末尾，`LinkedHashMap.put` 同名覆盖自然实现"子覆盖父"（AC-2.5）。
> - `source` 三态判定：`merged` 已含该 code 即 overridden；否则非本节点即 inherited；本节点即 node。

### 4.7 CycleDetectorService（环路检测 DFS，NFR-13）

> **算法（AC-2.6/NFR-13）**：设置 `node.parentId = candidateParent` 时，从 candidateParent 自底向上 DFS 查父链，若途中遇到 node 自身，则成环。

```java
package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.api.entity.ClassTemplate;
import com.pig4cloud.pig.ontology.mapper.ClassTemplateMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Service
public class CycleDetectorService {

	private final ClassTemplateMapper classTemplateMapper;

	/**
	 * 判断把 node 的 parent 设为 candidateParentId 是否会成环。
	 * 算法：从 candidateParentId 向上查父链，若遇到 node 则成环。O(树深)，建议树深 ≤5（R-13）。
	 */
	public boolean wouldCreateCycle(Long nodeId, Long candidateParentId) {
		if (nodeId == null || candidateParentId == null) return false;
		if (nodeId.equals(candidateParentId)) return true;   // 自引用
		Set<Long> visited = new HashSet<>();
		Long cur = candidateParentId;
		while (cur != null && visited.add(cur)) {
			if (cur.equals(nodeId)) return true;   // 父链回到 node -> 成环
			ClassTemplate parent = classTemplateMapper.selectById(cur);
			cur = (parent == null) ? null : parent.getParentId();
		}
		return false;
	}
}
```

> 说明：自底向上查父链，时间复杂度 O(树深)（NFR-13）。配合 R-13「建议树深 ≤5」，性能可控。

### 4.8 ClassificationCodeGenerator（编码生成器，FR-8）

> **算法（AC-8.1/8.2/8.5）**：据 `ont_classification_rule`（按 treeRoot）生成/校验编码。
> - **自动生成**：根节点用 `base_number`（如 30）；子节点用 `父编码 + separator + 同级序号`，序号按零填充到 `level_digits` 位。
> - **手填校验**：必须以父编码为前缀 + 符合规则（分隔符/位数），否则拒绝（AC-8.5）。

```java
package com.pig4cloud.pig.ontology.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.entity.*;
import com.pig4cloud.pig.ontology.mapper.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class ClassificationCodeGenerator {

	private final ClassificationRuleMapper ruleMapper;
	private final ClassTemplateMapper classTemplateMapper;

	/** 自动生成下一编码（AC-8.2）：parentId=null 用 base_number；否则 父编码 + sep + 同级序号 */
	public String nextCode(Long parentId) {
		ClassTemplate parent = parentId == null ? null : classTemplateMapper.selectById(parentId);
		String treeRoot = parent == null ? null : parent.getTreeRoot();
		ClassificationRule rule = ruleMapper.selectOne(Wrappers.<ClassificationRule>lambdaQuery()
				.eq(ClassificationRule::getTreeRoot, treeRoot));
		// 无规则 -> 根节点返回 base_number（默认规则），子节点返回父编码 + 默认序号
		String sep = rule == null || StrUtil.isBlank(rule.getSeparator()) ? "-" : rule.getSeparator();
		int digits = rule == null || rule.getLevelDigits() == null ? 2 : rule.getLevelDigits();
		boolean zeroPad = rule == null || !"0".equals(rule.getZeroPad());

		if (parent == null) {
			// 根节点：base_number（零填充到 digits 位）
			int base = rule == null || rule.getBaseNumber() == null ? 0 : rule.getBaseNumber();
			return pad(base, digits, zeroPad);
		}
		// 子节点：父编码 + sep + 同级已有数+1
		long siblingCnt = classTemplateMapper.selectCount(Wrappers.<ClassTemplate>lambdaQuery()
				.eq(ClassTemplate::getParentId, parentId));
		return parent.getClassificationCode() + sep + pad((int) (siblingCnt + 1), digits, zeroPad);
	}

	/** 手填校验（AC-8.5）：以父编码为前缀 + 末段符合规则 */
	public R validateManual(String code, Long parentId, String treeRoot) {
		ClassificationRule rule = ruleMapper.selectOne(Wrappers.<ClassificationRule>lambdaQuery()
				.eq(ClassificationRule::getTreeRoot, treeRoot));
		String sep = rule == null || StrUtil.isBlank(rule.getSeparator()) ? "-" : rule.getSeparator();
		int digits = rule == null || rule.getLevelDigits() == null ? 2 : rule.getLevelDigits();

		if (parentId == null) {
			// 根节点手填：须为 digits 位
			if (code.length() != digits) return R.failed("根级编码须为 " + digits + " 位");
			return R.ok(true);
		}
		ClassTemplate parent = classTemplateMapper.selectById(parentId);
		String prefix = parent.getClassificationCode() + sep;
		if (!code.startsWith(prefix))
			return R.failed("分类编码须以父编码 '" + parent.getClassificationCode() + "' 为前缀");
		String lastSeg = code.substring(prefix.length());
		if (lastSeg.length() != digits || !StrUtil.isNumeric(lastSeg))
			return R.failed("编码末段须为 " + digits + " 位数字");
		return R.ok(true);
	}

	private String pad(int n, int digits, boolean zeroPad) {
		String s = String.valueOf(n);
		return zeroPad && s.length() < digits ? String.format("%" + digits + "s", s).replace(' ', '0') : s;
	}
}
```

> 说明：
> - `nextCode`：根节点返回 `pad(base_number)`（如 `30`）；子节点返回 `父编码 + sep + pad(同级数+1)`（如 `30-01-01`，AC-8.2）。
> - `validateManual`：根节点校验位数；子节点校验前缀一致 + 末段位数/数字（AC-8.5）。
> - **规则变更只影响新节点**（AC-8.1/R-14）：生成器只在 `save` 时调用，旧编码冻结不动。

### 4.9 SyncController（建模侧同步，10.7，仅 `ont_sync_push`）

> **边界（AC-9.2/AC-9.3）**：类层级镜像的**唯一写入入口**。建模侧是权威源，治理侧只接收镜像 + 置 sync_status。环路校验在建模侧执行（本接口不校验）。

```java
package com.pig4cloud.pig.ontology.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.dto.ClassHierarchySyncDTO;
import com.pig4cloud.pig.ontology.service.ClassHierarchyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/sync")
@Tag(name = "建模侧同步接口", description = "建模侧推送类层级到镜像表（FR-9，10.7，仅 ont_sync_push）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class SyncController {

	private final ClassHierarchyService classHierarchyService;

	@SysLog("建模侧推送类层级镜像")
	@PostMapping("/class-hierarchy")
	@Operation(summary = "推送 subClassOf 关系（批量）", description = "镜像入库并置 sync_status='1'（10.7，AC-9.2）")
	@HasPermission("ont_sync_push")
	public R pushClassHierarchy(@Valid @RequestBody List<ClassHierarchySyncDTO> edges) {
		return classHierarchyService.upsertMirror(edges);
	}

	@SysLog("建模侧删除类层级镜像")
	@DeleteMapping("/class-hierarchy")
	@Operation(summary = "删除某类层级", description = "镜像置 sync_status='2'（失效），不物理删（10.7）")
	@HasPermission("ont_sync_push")
	public R deleteClassHierarchy(@RequestParam String childClassIri, @RequestParam String parentClassIri) {
		return classHierarchyService.invalidateMirror(childClassIri, parentClassIri);
	}
}
```

> `ClassHierarchyService.upsertMirror` 逻辑：按 `(child_class_iri, parent_class_iri)` 去重 upsert，置 `sync_status='1'` + `sync_time=now()`，带 `source_template_ref` 溯源。`invalidateMirror` 置 `sync_status='2'`（软失效，保留历史，便于统计）。

### 4.10 SupplyController 扩展（10.5，复用 DD2 已有类）

> 在 DD2 已建的 `SupplyController`（`@RequestMapping("/supply/v1")`，对外 `/admin/ont/supply/v1`）上追加三个端点。

```java
// 追加到 SupplyController（DD2 已有 property-templates 端点保留）
@GetMapping("/class-template/tree")
@Operation(summary = "分类模板树供给", description = "含编码/继承（10.5）")
@HasPermission("ont_supply_view")
public R<List<ClassTemplateNodeVO>> supplyClassTemplateTree(@RequestParam String treeRoot,
		@RequestParam(defaultValue = "false") Boolean includeDeprecated) {
	return R.ok(classTemplateService.tree(treeRoot, includeDeprecated));
}

@GetMapping("/class-template/{code}/inherited")
@Operation(summary = "分类模板继承视图供给", description = "合并父链属性+外观，供建模侧套用（10.5，AC-2.3）")
@HasPermission("ont_supply_view")
public R<InheritedViewVO> supplyInherited(@PathVariable String code) {
	ClassTemplate tpl = classTemplateService.getByCode(code);
	return R.ok(classTemplateService.inheritedView(tpl.getId()));
}

@GetMapping("/class-hierarchy/suggest")
@Operation(summary = "类层级建议供给", description = "据模板父链推荐 subClassOf 父类（FR-9，10.5，AC-9.1）")
@HasPermission("ont_supply_view")
public R<List<String>> suggestClassHierarchy(@RequestParam String templateCode) {
	// 据 templateCode 找到分类模板 -> 沿父链 -> 推荐从父模板派生的类 IRI（来自 ont_class_hierarchy 已镜像的 parent_class_iri）
	return R.ok(classTemplateService.suggestParentClassIris(templateCode));
}
```

> 说明：
> - `/supply/v1/class-template/{code}/inherited`：供建模侧套用，返回合并后的属性 + 外观（AC-2.3）。code 是 templateCode（人类可读），便于建模侧引用。
> - `/supply/v1/class-hierarchy/suggest`：据模板父链推荐 subClassOf 父类。建议逻辑——查模板的 parent 模板 -> 在 `ont_class_hierarchy` 中查 `source_template_ref=父模板templateCode` 的 parent_class_iri。若建模侧尚未镜像，返回空（AC-9.1 验证条件「若建模侧已镜像」）。
> - **AC-9.5 边界**：本 DD 不实现 Brick 导入（`/reference/brick/class/import` 属 DD6）；但 `source_ref='brick'` 溯源字段已在 V8 种子中体现，DD6 实现时直接写 `ont_class_template`（parent_id 形成分类模板树），**不写 `ont_class_hierarchy`**（后者只镜像建模侧）。

---

## 五、前端设计

### 5.1 API `web/src/api/ontology/class-template.ts`

```typescript
import request from '/@/utils/request';

// 分类模板树
export function tree(treeRoot: string, includeDeprecated = false) {
	return request({
		url: '/admin/ont/class-template/tree',
		method: 'get',
		params: { treeRoot, includeDeprecated },
	});
}

export function getObj(id: string) {
	return request({ url: '/admin/ont/class-template/' + id, method: 'get' });
}

// 继承视图（父链合并属性 + 外观）
export function inherited(id: string) {
	return request({ url: `/admin/ont/class-template/${id}/inherited`, method: 'get' });
}

export function pageList(query: any) {
	return request({ url: '/admin/ont/class-template/page', method: 'get', params: query });
}

export function addObj(obj: any) {
	return request({ url: '/admin/ont/class-template', method: 'post', data: obj });
}

export function putObj(obj: any) {
	return request({ url: '/admin/ont/class-template/' + obj.id, method: 'put', data: obj });
}

export function delObj(id: string) {
	return request({ url: '/admin/ont/class-template/' + id, method: 'delete' });
}

export function deprecateObj(id: string, deprecated: string) {
	return request({
		url: '/admin/ont/class-template/' + id + '/deprecate',
		method: 'put',
		params: { deprecated },
	});
}

// 编码预览（新增子节点时调用）
export function previewCode(parentId: string) {
	return request({ url: '/admin/ont/class-template/code/preview', method: 'get', params: { parentId } });
}

// 编码规则
export function getRule(treeRoot: string) {
	return request({ url: '/admin/ont/classification-rule', method: 'get', params: { treeRoot } });
}

export function saveRule(rule: any) {
	return request({ url: '/admin/ont/classification-rule', method: 'put', data: rule });
}
```

### 5.2 API `web/src/api/ontology/class-hierarchy.ts`

```typescript
import request from '/@/utils/request';

export function tree(treeRoot?: string) {
	return request({ url: '/admin/ont/class-hierarchy/tree', method: 'get', params: { treeRoot } });
}

export function pageList(query: any) {
	return request({ url: '/admin/ont/class-hierarchy/page', method: 'get', params: query });
}
```

### 5.3 分类模板页 `views/admin/ontology/class-template/index.vue`（左树右表）

> 对齐 pig 现有 `views/admin/dict/index.vue`（QueryTree + defineAsyncComponent 左树右表）+ DD2 `property-template/index.vue` 范式。

**布局（splitpanes 左树右表，PRD 12.3）：**

```
┌─────────────┬──────────────────────────────────────┐
│ 分类树       │ 选中节点详情                           │
│ (QueryTree) │ ┌──────────────────────────────────┐ │
│             │ │ 外观卡（icon/color，标注继承/覆盖）│ │
│ 30 设备      │ ├──────────────────────────────────┤ │
│ └30-01 泵    │ │ 分类编码：30-01-01                │ │
│   ├喷淋泵    │ │ 父节点：泵(30-01)                  │ │
│   ├给水泵    │ ├──────────────────────────────────┤ │
│   └污水泵    │ │ 结构骨架（继承视图面板）            │ │
│             │ │  · 名称     [本节点新增]            │ │
│ [+新增子节点]│ │  · 描述     [继承自父]              │ │
│ [编码规则]   │ │  · 喷淋系统编号 [已覆盖父]          │ │
│             │ └──────────────────────────────────┘ │
│             │ [编辑] [弃用] [删除]                   │
└─────────────┴──────────────────────────────────────┘
```

**关键结构：**

- **左侧 QueryTree**：`defineAsyncComponent(() => import('/@/components/QueryTree/index.vue'))`，节点 label 显 `classificationCode + ' ' + label`（如 `30-01-01 喷淋泵`），`node-key="id"`，`@nodeClick` 加载右侧详情。
- **顶部操作栏**：treeRoot 切换（下拉，默认 equipment）、新增根节点（`v-auth="'ont_class_tpl_manage'"`）、编码规则配置（打开 rule-drawer）。
- **右侧详情卡**：外观区（icon + color 色块，标注"继承自父/本节点覆盖"）、分类编码、父节点、结构骨架（inherited-panel 组件）。
- **操作按钮**：新增子节点（带 `previewCode` 自动预览编码）、编辑、弃用（按 deprecated 切换文案）、删除。
- **内置节点**：编辑/删除按钮 `:disabled="scope.row.source === 'builtin'"`（对齐 DD2 builtin 保护）。

**关键逻辑（对齐 dict/index.vue）：**

```typescript
const QueryTree = defineAsyncComponent(() => import('/@/components/QueryTree/index.vue'));
const state = reactive({
	treeRoot: 'equipment',
	query: (params: any) => tree(params.treeRoot),   // QueryTree 的 query prop
	selectedId: '' as string,
	detail: null as any,
	inheritedView: null as any,
});
// 节点点击 -> 加载详情 + 继承视图
async function onNodeClick(node: any) {
	state.selectedId = node.id;
	state.detail = await getObj(node.id);
	state.inheritedView = await inherited(node.id);
}
```

### 5.4 表单 `form.vue` + `inherited-panel.vue` + `rule-drawer.vue`

**form.vue（新增/编辑对话框）：**

| 字段 | 说明 |
|---|---|
| parent_id | 父节点选择（树选择器，根节点时为空） |
| treeRoot | 所属分类树（与父节点联动，根节点时手填） |
| templateCode | 唯一标识（新增必填，编辑只读，对齐 DD2） |
| classificationCode | 分类编码（自动预览 + 允许手填覆盖，手填时前端调 previewCode 对比 + 后端 AC-8.5 校验） |
| label / labelCn | 显示名 / 中文名 |
| description | 说明 |
| icon / color | 外观（emoji 输入 + 色板；留空则继承父） |
| inheritAppearance | 是否继承父外观（默认 1） |
| propertyRefs | 结构骨架配置（多选属性模板 + ref_type + sort_order，对齐 V5 属性模板下拉） |

> 用 `el-dialog + el-form`，对齐 DD2 `property-template/form.vue` 范式。新增子节点时：选父节点 → 自动调 `previewCode` 预览编码 → 显示"继承自父"字段预览 → 填追加字段。

**inherited-panel.vue（继承视图面板）：**

- 展示 `inherited()` 返回的属性清单，每行带 source 三态 tag：
  - `node` → 蓝色 tag「本节点新增」
  - `inherited` → 灰色 tag「继承自父」
  - `overridden` → 橙色 tag「已覆盖父」（AC-2.5/NFR-14）
- 外观区标注「继承自父」「本节点覆盖」（AC-2.2）。

**rule-drawer.vue（编码规则配置抽屉，FR-8）：**

- 字段：separator（分隔符）、levelDigits（每级位数）、baseNumber（根级基数）、zeroPad（零填充开关）。
- 改规则后**实时预览影响**（仅对新节点生效，AC-8.1）。保存调 `saveRule`。

### 5.5 类分类树页 `views/admin/ontology/class-hierarchy/index.vue`（只读）

> 对齐 class-template 左树结构，但**全只读**（无新增/编辑/删除按钮，AC-9.3）。

- 左侧 QueryTree：节点显 `class IRI 短名`（如 `XX品牌喷淋泵`），数据来自 `tree()`。
- 右侧详情：childClassIri / parentClassIri / sourceTemplateRef（溯源）/ syncStatus（tag：待同步/已同步/已失效）/ syncTime。
- 顶部提示条：「类层级由建模侧权威维护，治理侧只读镜像（FR-9）」。
- 与分类模板树对照：sourceTemplateRef 可点击跳转到分类模板页对应节点（AC-9.4）。

### 5.6 i18n（class-template/i18n/zh-cn.ts 示例）

```typescript
export default {
	classTemplate: {
		treeRoot: '分类树',
		templateCode: '模板标识',
		classificationCode: '分类编码',
		label: '显示名',
		labelCn: '中文名',
		description: '说明',
		parentId: '父节点',
		icon: '图标',
		color: '颜色',
		inheritAppearance: '继承父外观',
		source: '来源',
		deprecated: '状态',
		sortOrder: '排序',
		propertyRefs: '结构骨架',
		refType: '引用类型',
		// 继承视图 source 三态
		sourceNode: '本节点新增',
		sourceInherited: '继承自父',
		sourceOverridden: '已覆盖父',
		// 编码规则
		ruleTitle: '编码规则配置',
		separator: '分隔符',
		levelDigits: '每级位数',
		baseNumber: '根级基数',
		zeroPad: '零填充',
		// ... inputXxxTip 系列
	},
};
```

---

## 六、横切设计

### 6.1 缓存（NFR-12）

- **InheritedViewService** 用 Caffeine 本地缓存（key=classTemplateId，TTL 5min，maxSize 1000）。
- **失效策略**：任何分类模板/ref 写操作（save/update/delete/deprecate）后调 `inheritedViewService.invalidate(id)`，失效自身 + 所有后代（后代继承视图依赖本节点）。
- **依赖新增**：`pig-ontology-biz/pom.xml` 加 `com.github.ben-manes.caffeine:caffeine`（版本由父 pom 或显式声明）。

### 6.2 校验

- **templateCode 唯一**：Service 查重 + DB `uk_ont_class_tpl_code`（V4 已建）双保险（AC-2.8）。
- **classificationCode 唯一**：Service 查重 + DB `uk_ont_class_tpl_cls_code`（V4 已建）（AC-8.3）。
- **propertyRefs templateCode 存在性**：Service 批量查 `ont_property_template`，不存在则拒绝（AC-2.4）。
- **ref 唯一性**：V4 `ont_class_template_ref` **无唯一约束**，Service 层在 saveRefs 前去重（同 classTemplateId + propertyTemplateCode + refType 保留 sort_order 最小者），避免重复注入。
- **treeRoot 一致性**：子节点 treeRoot 须与父节点一致（跨树继承本期不支持，Service 校验拒绝）。
- **builtin 保护**：编辑/删除 builtin 返回"内置模板不可操作"（对齐 DD2）。

### 6.3 异常处理

- 唯一约束冲突（`DuplicateKeyException`）：Service 已预查重，兜底捕获转 `R.failed`。
- 环路检测、编码违规、子节点存在等业务错误统一 `R.failed(msg)`（code=1），不抛 500。

### 6.4 审计

- MybatisPlusMetaObjectHandler 自动填充 `createBy/createTime/updateBy/updateTime/delFlag`（DD1 已配置）。

### 6.5 双形态

- 单体：pig-boot context-path `/admin` → `/admin/ont/class-template/**`、`/admin/ont/class-hierarchy/**`、`/admin/ont/sync/**`、`/admin/ont/supply/v1/class-template/**`。
- 微服务：网关路由 `Path=/admin/ont/**` → `lb://pig-ontology-biz`（DD1 已配置）。

### 6.6 安全（供给与同步）

- 供给接口 `/supply/v1/class-template/**`、`/supply/v1/class-hierarchy/suggest`：`@HasPermission("ont_supply_view")`（V6 已建，建模师/查看者授予）。
- 同步接口 `/sync/class-hierarchy`：`@HasPermission("ont_sync_push")`（V8 新建 10251，**仅建模侧服务账号授予，治理员不持有**，AC-9.2/AC-9.3）。
- 类层级登记接口：**不存在**（AC-9.3 边界）。

---

## 七、测试要点

| 类别 | 要点 |
|---|---|
| 编译 | `mvn -pl pig-ontology/pig-ontology-biz -am compile` 通过（含 caffeine 新依赖） |
| Flyway | 启动后 V8 成功；`SELECT count(*) FROM ont_class_template WHERE tree_root='equipment'` = 5；`SELECT * FROM ont_classification_rule WHERE tree_root='equipment'` 1 条；`SELECT * FROM sys_menu WHERE menu_id=10251` 存在 |
| 树查询 | `/class-template/tree?treeRoot=equipment` 返回 3 级树（设备→泵→喷淋泵/给水泵/污水泵），节点显编码（AC-2.1） |
| 继承视图 | `/class-template/3102/inherited`（喷淋泵）返回 4 属性（继承自父泵），source=inherited（AC-2.3） |
| 外观继承 | 喷淋泵（icon/color 空 + inherit_appearance=1）详情合并父泵外观 `💧/#1890ff`（AC-2.2） |
| 覆盖 | 喷淋泵追加同名 name 属性 → 继承视图 source=overridden（AC-2.5） |
| refs 校验 | 保存带不存在 templateCode 的 refs → 返回业务错误（AC-2.4） |
| 环路 | 设喷淋泵 parent=污水泵（污水泵的后代）→ 返回"成环"（AC-2.6） |
| 删子节点 | 删泵（有 3 子节点）→ 返回"存在子节点"（AC-2.7） |
| 编码自动 | 泵下新增第 1 个子节点 → 编码 `30-01-01`；第 2 个 → `30-01-02`（AC-8.2） |
| 编码前缀查 | `/page?classificationCode=30-01` 返回 3 个子节点（AC-8.4） |
| 编码手填 | 手填 `30-01-99`（末段非 2 位）→ 拒绝；手填 `30-02-01`（前缀不符父 30-01）→ 拒绝（AC-8.5） |
| 编码规则改 | 改 levelDigits=3 → 新增子节点编码 `30-01-001`，旧编码不变（AC-8.1） |
| 缓存 | 继承视图二次查询命中缓存（P95<300ms）；编辑模板后缓存失效重算（NFR-12） |
| 类层级只读 | `/class-hierarchy/tree` 返回镜像树；无 POST 登记（AC-9.3） |
| 同步推送 | `POST /sync/class-hierarchy`（带 ont_sync_push）→ 镜像入库 sync_status=1（AC-9.2）；治理员无权限调用 |
| suggest | `/supply/v1/class-hierarchy/suggest?templateCode=spray-pump` 返回建议父类 IRI（AC-9.1） |
| 前端 | pig-ui → 本体治理 → 分类模板 → 树展示设备树；新增子节点编码预览；继承视图面板显三态 tag；编码规则抽屉改规则；类分类树页只读 |
| 回归 | 属性模板库（DD2）功能不受影响 |

---

## 八、风险与缓解

| 风险 | 缓解 |
|---|---|
| 缓存与数据不一致（NFR-12） | 写操作主动 invalidate（含后代）+ TTL 5min 兜底；防御性去环兜底脏数据 |
| 环路检测在深树性能差（R-13） | 建议树深 ≤5（R-13）；DFS O(树深)；UI 提示层级过深 |
| 编码规则变更影响存量（R-14） | 规则只对新节点生效（AC-8.1）；旧编码冻结；批量重编码工具本期预留 |
| ref 表无唯一约束导致重复注入 | Service 层 saveRefs 前去重；未来可加 V9 唯一约束（本期不改 V4） |
| 模板继承与类层级混淆（R-12） | UI 区分"继承视图"（class-template 页）vs"类层级视图"（class-hierarchy 页）；接口路径分离；suggest 仅建议不写入 |
| Brick 导入边界（R-16/AC-9.5） | 本期不实现 Brick 导入；DD6 实现时 subClassOf 映射 parent_id、取主父类、不写 hierarchy |
| V8 与其他分支 Flyway 撞车 | V8 只做 INSERT；合并时按需调版本号 |
| 同步接口被越权调用 | `ont_sync_push` 仅授予建模侧服务账号；治理员角色不勾选 |

---

## 九、交付物清单

| 类型 | 文件 | 说明 |
|---|---|---|
| 新建 | `ClassTemplate.java` / `ClassTemplateRef.java` / `ClassificationRule.java` / `ClassHierarchy.java` | 四 Entity |
| 新建 | `ClassTemplateMapper.java` / `ClassTemplateRefMapper.java` / `ClassificationRuleMapper.java` / `ClassHierarchyMapper.java` | 四 Mapper |
| 新建 | `ClassTemplateService.java` + `ClassTemplateServiceImpl.java` | 分类模板 CRUD + 树 + 详情 |
| 新建 | `InheritedViewService.java` | 继承视图计算 + Caffeine 缓存（核心） |
| 新建 | `CycleDetectorService.java` | 环路检测 DFS |
| 新建 | `ClassificationCodeGenerator.java` | 编码生成/校验 |
| 新建 | `ClassificationRuleService.java` + impl | 编码规则查询/配置 |
| 新建 | `ClassHierarchyService.java` + impl | 类层级只读查询 + 镜像 upsert/invalidate |
| 新建 | `ClassTemplateController.java` | 分类模板树/详情/继承/CRUD/编码预览 |
| 新建 | `ClassificationRuleController.java` | 编码规则查询/配置 |
| 新建 | `ClassHierarchyController.java` | 类层级只读查询（仅 GET） |
| 新建 | `SyncController.java` | 建模侧同步（仅 ont_sync_push） |
| 修改 | `SupplyController.java`（DD2 已有） | 追加 class-template/tree、{code}/inherited、class-hierarchy/suggest 三端点 |
| 新建 | `ClassTemplateSaveDTO.java` / `ClassHierarchySyncDTO.java` | DTO |
| 新建 | `ClassTemplateNodeVO`/`ClassTemplateDetailVO`/`InheritedPropertyVO`/`InheritedViewVO`/`ClassHierarchyNodeVO` | VO |
| 新建 | `V8__ont_class_template_seed.sql` | 设备分类树 + 编码规则 + ont_sync_push 种子 |
| 修改 | `server/pig-ontology/pig-ontology-biz/pom.xml` | 新增 caffeine 依赖 |
| 新建 | `web/src/api/ontology/class-template.ts` / `class-hierarchy.ts` | 前端 API |
| 新建 | `web/src/views/admin/ontology/class-template/index.vue` | 左树右表 |
| 新建 | `web/src/views/admin/ontology/class-template/form.vue` | 新增/编辑对话框 |
| 新建 | `web/src/views/admin/ontology/class-template/inherited-panel.vue` | 继承视图面板（三态标记） |
| 新建 | `web/src/views/admin/ontology/class-template/rule-drawer.vue` | 编码规则抽屉 |
| 新建 | `web/src/views/admin/ontology/class-template/composables.ts` + `i18n/{zh-cn,en}.ts` | 状态复用 + 词条 |
| 新建 | `web/src/views/admin/ontology/class-hierarchy/index.vue` + `i18n/{zh-cn,en}.ts` | 只读类树页 |

---

## 十、与 PRD 边界的对齐确认（防混淆备忘）

| 边界点 | 本 DD 落地方式 | PRD 依据 |
|---|---|---|
| 模板继承 vs 类层级不混淆 | 两套独立表（parent_id vs ont_class_hierarchy）+ 独立页面 + 独立接口 | 5.2 / R-12 / D.4 |
| 治理侧不创建 owl:Class | 全设计无类实体写入；hierarchy 仅存 IRI 镜像 | AC-2.8 / AC-9.6 |
| 类层级权威在建模侧 | ClassHierarchyController 仅 GET；写入只经 SyncController（ont_sync_push） | AC-9.2 / AC-9.3 / 10.7 |
| Brick 导入映射分类模板树、不写 hierarchy | 本期不实现导入；DD6 实现时 parent_id 映射、不写 hierarchy | AC-9.5 / D.3 / R-16 |
| 环路校验在建模侧（类层级） | ont_class_hierarchy 入库仅基本格式校验，不校验环路；分类模板环路在本治理侧 Service 校验 | FR-9.5 / NFR-13 |
| 编码规则只影响新节点 | 生成器只在 save 时调用，旧编码冻结 | AC-8.1 / R-14 |

---

*本详细设计对应里程碑 M2，依赖 DD1（M0）基础设施与 DD2（M1）属性模板库。实现完成后，按《详细设计计划.md》依赖顺序进入 DD4（单位注册表）/DD5（注释属性注册表）；DD6（参考本体，贯穿）依赖 DD3（Brick 导入分类模板）+ DD4（QUDT 导入单位）。*
