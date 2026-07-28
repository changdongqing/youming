# 详细设计-DD7-本体项目管理与命名空间

| 项 | 内容 |
|---|---|
| 文档名称 | 本体项目管理与命名空间 详细设计 |
| 里程碑 | M5（FR-10） |
| 上游 PRD | 《本体建模功能产品需求文档.md》v1.0（FR-10 / AC-10.1~10.5 / 5.2 / 10.1 / 第十三节 13.1/13.2 / 第十六节 M5） |
| 设计计划 | 《本体建模功能详细设计计划.md》DD7（M5） |
| 前置依赖 | 治理域 DD1–DD6 已全部落地（pig-ontology-biz 模块就绪、pig-gateway 路由 `Path=/admin/ont/**` 就绪、Flyway V1–V11 已应用） |
| 编写日期 | 2026-07-28 |
| 文档状态 | 评审通过（附优化项已合入），进入实现 |

> **引用条目说明**（评审修订）：本 PRD 功能需求从 FR-10 起（无 FR-9，故无 AC-9.1/9.2）；"13.1/13.2"指**第十三节权限与菜单设计**（非 FR-13，FR-13 是对象属性建模）；M5 里程碑定义在**第十六节**（第十四节为 DoD 验收汇总）。序列化策略 A/B 决策依据为 **5.2 关键约定**。

---

## 一、设计目标与范围

### 1.1 目标

- 提供建模域的顶层容器--**本体项目**，管理命名空间基址、IRI 前缀注册、序列化策略配置、项目状态。
- 在 `pig-ontology-biz` 内新增 `modeling` 包，不新拆 Maven 子模块（PRD 3.1 决策）。
- 建立建模域的菜单骨架：11000 一级目录"本体建模" + 11100 段"本体项目管理"菜单 + 权限点按钮。
- Flyway V12 建表（`ont_model_project` + `ont_model_prefix`）+ V12 菜单种子。
- 为后续 DD8（类实体创建）提供项目容器与命名空间基址。

### 1.2 范围（本 DD 做 / 不做）

| 做（本 DD） | 不做（后续 DD / Out of Scope） |
|---|---|
| 本体项目 CRUD（FR-10.1） | 类实体创建（DD8/FR-11） |
| IRI 前缀 CRUD + NCName 校验（FR-10.2） | 属性建模（DD9/FR-12/13） |
| 序列化策略字段预留（默认 B，A 禁用）（FR-10.3） | 序列化实现（DD10/FR-15） |
| 项目状态管理 draft/active/archived（FR-10.4） | 画布（DD11/FR-17） |
| 项目列表分页查询（FR-10.5） | 个体实例（M10/DD12，Out of Scope） |
| 删除项目时**级联软删项目下前缀**（`ont_model_prefix`） | **类实体关联校验**（DD8 建 `ont_model_class` 后实现） |
| V12 建表 + 11000/11100 段菜单种子 | 方案 A 序列化切换（v1 禁用，仅字段预留） |

> **删除校验范围说明**（评审修订 P-1）：PRD AC-10.1 要求"删除时校验关联类实体"。但 `ont_model_class` 由 DD8 创建，本 DD 阶段不存在，故本 DD 的删除校验**只覆盖已建表的前缀子表**（级联软删，避免前缀孤儿）；**类实体校验整体移交 DD8**。本 DD 不在代码中预埋"引用未存在 Mapper 的注释"，避免实现者照抄产生编译/语义混淆。详见 4.5 removeProject。

### 1.3 验收映射（M5 DoD）

| PRD AC | 本 DD 实现点 |
|---|---|
| AC-10.1 项目 CRUD + 删除校验关联类实体 | 4.3 ModelProjectController + 4.5 ModelProjectServiceImpl（删除时**级联软删项目下前缀** `ont_model_prefix`；**类实体校验移交 DD8**——DD8 建 `ont_model_class` 后在该表删除链路或项目删除链路补校验，本 DD 不预埋引用未存在 Mapper 的注释代码） |
| AC-10.2 前缀 CRUD + NCName 校验 + 同项目唯一 | 4.4 ModelPrefixController + 4.6 ModelPrefixServiceImpl + 6.1 正则校验 |
| AC-10.3 默认策略 B + 方案 A 禁用切换 | 4.5 ModelProjectServiceImpl.saveProject（强制 strategy='B'） |
| AC-10.4 状态管理 archived 只读 | 4.5 ModelProjectServiceImpl（archived 拦截写操作） |
| AC-10.5 分页查询 + 名称搜索 + 状态过滤 | 4.3 ModelProjectController.page + 4.5 ServiceImpl.page |

---

## 二、落地清单

### 2.1 后端文件清单

```
server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/modeling/
├── controller/
│   ├── ModelProjectController.java          # 新增：本体项目 CRUD + 分页
│   └── ModelPrefixController.java           # 新增：IRI 前缀 CRUD
├── service/
│   ├── ModelProjectService.java             # 新增：项目 Service 接口
│   ├── ModelPrefixService.java              # 新增：前缀 Service 接口
│   └── impl/
│       ├── ModelProjectServiceImpl.java     # 新增：项目 Service 实现
│       └── ModelPrefixServiceImpl.java      # 新增：前缀 Service 实现
├── mapper/
│   ├── ModelProjectMapper.java              # 新增：项目 Mapper
│   └── ModelPrefixMapper.java               # 新增：前缀 Mapper
├── entity/
│   ├── ModelProject.java                    # 新增：项目 Entity
│   └── ModelPrefix.java                     # 新增：前缀 Entity
├── dto/
│   └── ModelProjectSaveDTO.java             # 新增：项目保存 DTO（含前缀列表）
└── vo/
    ├── ModelProjectDetailVO.java            # 新增：项目详情 VO（含前缀列表）
    └── ModelPrefixVO.java                   # 新增：前缀 VO
```

### 2.2 数据库文件清单

```
server/pig-common/pig-common-data/src/main/resources/db/migration/
├── V1__init_schema.sql                      # 已存在（sys_menu 表定义在此）
├── V2~V11                                   # 已存在（治理域 V4-V11）
└── V12__ont_model_project_seed.sql          # 新增：建模域首脚本（建表 + 菜单种子）
```

### 2.3 依赖变更清单

无新增依赖。复用 pig-ontology-biz 已有依赖（MyBatis-Plus / hutool / Caffeine / springdoc）。

### 2.4 前端文件清单

```
web/src/
├── api/ontology-model/
│   └── project.ts                           # 新增：项目 + 前缀 API
└── views/admin/ontology-model/
    └── project/
        ├── index.vue                        # 新增：项目列表页
        ├── form.vue                         # 新增：项目表单弹窗
        ├── prefix-dialog.vue                # 新增：前缀管理子弹窗
        ├── composables.ts                   # 新增：状态/格式选项
        └── i18n/
            ├── zh-cn.ts                     # 新增
            └── en.ts                        # 新增
```

---

## 三、数据库设计（V12）

### 3.1 V12 脚本范围

V12 是建模域首个 Flyway 脚本，一次性完成：
- (a) 建表 `ont_model_project`（本体项目）+ `ont_model_prefix`（IRI 前缀注册）
- (b) 建索引
- (c) sys_menu 菜单种子（11000 一级目录 + 11100 项目菜单 + 11101~11104 权限点按钮）

### 3.2 V12__ont_model_project_seed.sql

```sql
-- ============================================================
-- V12: 建模域 - 本体项目管理（FR-10）
-- 建 ont_model_project + ont_model_prefix 两表 + 菜单种子（11000 段）
-- ============================================================

-- ---------- (a) 建表 ----------

CREATE TABLE ont_model_project (
    id                      bigint       NOT NULL,
    project_code            varchar(64)  NOT NULL,
    name                    varchar(128) NOT NULL,
    description             varchar(512),
    namespace_base          varchar(255) NOT NULL,
    default_format          varchar(16)  NOT NULL DEFAULT 'TTL',
    serialization_strategy  char(1)      NOT NULL DEFAULT 'B',
    status                  varchar(16)  NOT NULL DEFAULT 'draft',
    create_by               varchar(64)  DEFAULT ' ',
    create_time             timestamp    DEFAULT now(),
    update_by               varchar(64)  DEFAULT ' ',
    update_time             timestamp    DEFAULT now(),
    del_flag                char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_model_project PRIMARY KEY (id),
    CONSTRAINT uk_ont_model_project_code UNIQUE (project_code)
);
COMMENT ON TABLE  ont_model_project IS '本体项目（建模域顶层容器，FR-10）';
COMMENT ON COLUMN ont_model_project.project_code            IS '项目编码（唯一），如 fire-equipment';
COMMENT ON COLUMN ont_model_project.name                    IS '项目名称';
COMMENT ON COLUMN ont_model_project.description             IS '项目描述';
COMMENT ON COLUMN ont_model_project.namespace_base          IS 'IRI 命名空间基址，类/属性 IRI = namespace_base + localName';
COMMENT ON COLUMN ont_model_project.default_format          IS '默认序列化格式：TTL=Turtle / OWL_XML=OWL XML';
COMMENT ON COLUMN ont_model_project.serialization_strategy  IS '序列化策略：B=带前缀独立副本(默认,v1) / A=共享单一属性+多domain(预留,v1禁用)';
COMMENT ON COLUMN ont_model_project.status                  IS '项目状态：draft=草稿 / active=活跃 / archived=归档(只读)';

CREATE TABLE ont_model_prefix (
    id              bigint       NOT NULL,
    project_id      bigint       NOT NULL,
    prefix          varchar(64)  NOT NULL,
    namespace       varchar(255) NOT NULL,
    is_default      char(1)      DEFAULT '0',
    create_by       varchar(64)  DEFAULT ' ',
    create_time     timestamp    DEFAULT now(),
    update_by       varchar(64)  DEFAULT ' ',
    update_time     timestamp    DEFAULT now(),
    del_flag        char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_model_prefix PRIMARY KEY (id),
    CONSTRAINT uk_ont_model_prefix UNIQUE (project_id, prefix)
);
COMMENT ON TABLE  ont_model_prefix IS 'IRI 前缀注册（项目级，FR-10.2）';
COMMENT ON COLUMN ont_model_prefix.project_id  IS '所属项目 ID';
COMMENT ON COLUMN ont_model_prefix.prefix      IS '前缀名，如 ex / qudt / brick（NCName 规范）';
COMMENT ON COLUMN ont_model_prefix.namespace   IS '命名空间 URI，如 http://example.com/onto/';
COMMENT ON COLUMN ont_model_prefix.is_default  IS '1=项目默认前缀（类/属性 IRI 基址来源）';

-- ---------- (b) 索引 ----------

CREATE INDEX idx_ont_model_project_status ON ont_model_project (status) WHERE del_flag = '0';
CREATE INDEX idx_ont_model_prefix_project ON ont_model_prefix (project_id) WHERE del_flag = '0';

-- ---------- (c) sys_menu 菜单种子（11000 段，建模域） ----------

-- 本体建模 一级目录（parent_id = -1，与"本体治理"10000 平级）
INSERT INTO sys_menu VALUES (11000, '本体建模', NULL, '/ontology-model', NULL, -1, 'iconfont icon-shujujicheng', '1', 35, '0', '0', '0', 'admin', now(), 'admin', now(), '0');

-- 11100 本体项目管理（菜单，parent_id = 11000）
INSERT INTO sys_menu VALUES (11100, '本体项目管理', NULL, '/admin/ontology-model/project/index', NULL, 11000, 'iconfont icon-xiangmu', '1', 1, '0', '0', '0', 'admin', now(), 'admin', now(), '0');

-- 11101~11104 本体项目管理 权限按钮（menu_type='1'，embedded=NULL）
INSERT INTO sys_menu VALUES (11101, '项目新增', 'ont_project_manage', NULL, NULL, 11100, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11102, '项目编辑', 'ont_project_manage', NULL, NULL, 11100, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11103, '项目删除', 'ont_project_manage', NULL, NULL, 11100, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11104, '项目查看', 'ont_project_view',   NULL, NULL, 11100, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
```

> **sys_menu INSERT 说明**：按 17 字段位置 INSERT（menu_id / name / permission / path / component / parent_id / icon / visible / sort_order / keep_alive / embedded / menu_type / create_by / create_time / update_by / update_time / del_flag）。一级目录 parent_id=-1（对齐 V5 的 10000 写法）；菜单 menu_type='0' embedded='0'；按钮 menu_type='1' embedded=NULL permission=权限串。

> **菜单 ID 段位规划**：建模域用 11000–11599（治理域用 10000–10599，不冲突）。11000=本体建模目录，11100=本体项目管理，11200=本体类建模(DD8)，11300=本体属性建模(DD9)，11400=序列化与导入(DD10)，11500=可视化画布(DD11)。

> **权限标识命名**：`ont_project_view`（查看，含列表/详情）、`ont_project_manage`（管理，含新增/编辑/删除）。建模域权限统一 `ont_<功能>_model_*` 或 `ont_<功能>_*` 前缀，与治理域 `ont_*` 区分。本 DD 项目管理用 `ont_project_*`。

---

## 四、后端设计

> 包路径 `com.pig4cloud.pig.ontology.modeling.*`，在 pig-ontology-biz 内新增 modeling 子包。Controller 路径带 `/ont/model` 前缀（经 context-path /admin 或网关后对外为 `/admin/ont/model/**`，复用 pig-gateway 已有路由 `Path=/admin/ont/**`，StripPrefix=1 剥去 `/admin`）。

> **包结构决策说明**（评审补充 P-2）：治理域（DD1–DD6）采用**扁平包**——Controller/Service/Mapper 直接置于 `com.pig4cloud.pig.ontology.{controller,service,mapper}`，Entity 置于 `api.entity`（共 8 Entity + 10 Controller，体量小）。**建模域（DD7–DD11）改用 `modeling` 子包**（PRD 3.1 / 设计计划 3.1 明确决策），因建模域预计新增 30+ 类，子包隔离更清晰。两者差异：
> - 治理域：`com.pig4cloud.pig.ontology.controller.*` + `api.entity.*`（扁平）
> - 建模域：`com.pig4cloud.pig.ontology.modeling.{controller,service,mapper,entity,dto,vo}.*`（子包，Entity 放 `modeling.entity` 而非 `api.entity`，使 modeling 自包含）
>
> 物理模块仍是 `pig-ontology-biz` 单模块（S-4，不新拆 api/biz 子模块）。此为有意演进，非笔误。

### 4.1 Entity - ModelProject

```java
package com.pig4cloud.pig.ontology.modeling.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 本体项目 Entity（建模域顶层容器，FR-10）
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "本体项目")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_model_project")
public class ModelProject extends Model<ModelProject> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "项目编码（唯一），如 fire-equipment")
	@NotBlank(message = "项目编码不能为空")
	private String projectCode;

	@Schema(description = "项目名称")
	@NotBlank(message = "项目名称不能为空")
	private String name;

	@Schema(description = "项目描述")
	private String description;

	@Schema(description = "IRI 命名空间基址，类/属性 IRI = namespace_base + localName")
	@NotBlank(message = "命名空间基址不能为空")
	private String namespaceBase;

	@Schema(description = "默认序列化格式：TTL / OWL_XML")
	private String defaultFormat;

	@Schema(description = "序列化策略：B=方案B(默认) / A=方案A(预留,v1禁用)")
	private String serializationStrategy;

	@Schema(description = "项目状态：draft / active / archived")
	private String status;

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

> 范式对齐治理域 `AnnotationProperty.java`：继承 `Model<T>` + `@TableId(ASSIGN_ID)` + 审计四字段 `@TableField(fill=INSERT/UPDATE)` + `@TableLogic delFlag`。业务字段带 `@NotBlank` 校验（jakarta.validation）。

### 4.2 Entity - ModelPrefix

```java
package com.pig4cloud.pig.ontology.modeling.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * IRI 前缀注册 Entity（项目级，FR-10.2）
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "IRI 前缀注册")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_model_prefix")
public class ModelPrefix extends Model<ModelPrefix> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "所属项目 ID")
	private Long projectId;

	@Schema(description = "前缀名，如 ex / qudt / brick（NCName 规范）")
	@NotBlank(message = "前缀名不能为空")
	private String prefix;

	@Schema(description = "命名空间 URI")
	@NotBlank(message = "命名空间不能为空")
	private String namespace;

	@Schema(description = "1=项目默认前缀")
	private String isDefault;

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

### 4.3 Controller - ModelProjectController

```java
package com.pig4cloud.pig.ontology.modeling.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.modeling.entity.ModelProject;
import com.pig4cloud.pig.ontology.modeling.service.ModelProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 本体项目管理 Controller（FR-10）
 * <p>
 * 路径 /ont/model/project/**，对外 /admin/ont/model/project/**
 * （复用 pig-gateway 已有路由 Path=/admin/ont/**，gateway StripPrefix=1 / boot context-path=/admin 剥离后需 /ont 前缀）。
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/model/project")
@Tag(name = "本体项目管理", description = "项目 CRUD + 前缀管理（FR-10）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ModelProjectController {

	private final ModelProjectService modelProjectService;

	@GetMapping("/page")
	@Operation(summary = "分页查询", description = "按名称/状态过滤（AC-10.5）")
	@HasPermission("ont_project_view")
	public R<IPage<ModelProject>> page(@ParameterObject Page page, @ParameterObject ModelProject project) {
		return R.ok(modelProjectService.page(page, project));
	}

	@GetMapping("/{id}")
	@Operation(summary = "项目详情", description = "含前缀列表（AC-10.1）")
	@HasPermission("ont_project_view")
	public R<ModelProject> getById(@PathVariable Long id) {
		return R.ok(modelProjectService.getDetail(id));
	}

	@SysLog("新增本体项目")
	@PostMapping
	@Operation(summary = "新增项目", description = "projectCode 查重 + 策略强制 B（AC-10.1/10.3）")
	@HasPermission("ont_project_manage")
	public R save(@Valid @RequestBody ModelProject project) {
		return modelProjectService.saveProject(project);
	}

	@SysLog("编辑本体项目")
	@PutMapping("/{id}")
	@Operation(summary = "编辑项目", description = "archived 拒绝写（AC-10.4）")
	@HasPermission("ont_project_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody ModelProject project) {
		project.setId(id);
		return modelProjectService.updateProject(project);
	}

	@SysLog("删除本体项目")
	@DeleteMapping("/{id}")
	@Operation(summary = "删除项目", description = "校验关联类实体（AC-10.1）")
	@HasPermission("ont_project_manage")
	public R removeById(@PathVariable Long id) {
		return modelProjectService.removeProject(id);
	}
}
```

> 范式对齐治理域 `PropertyTemplateController`：分页用 `@ParameterObject Page page, @ParameterObject Entity`（springdoc 注解），Service 返回 `IPage<T>`。写端点注解顺序 `@SysLog` -> `@PostMapping` -> `@Operation` -> `@HasPermission`。

### 4.4 Controller - ModelPrefixController

```java
package com.pig4cloud.pig.ontology.modeling.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.modeling.entity.ModelPrefix;
import com.pig4cloud.pig.ontology.modeling.service.ModelPrefixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * IRI 前缀管理 Controller（FR-10.2）
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/model/project")
@Tag(name = "IRI 前缀管理", description = "项目级前缀 CRUD（FR-10.2）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ModelPrefixController {

	private final ModelPrefixService modelPrefixService;

	@GetMapping("/{projectId}/prefixes")
	@Operation(summary = "项目前缀列表", description = "全量列表（AC-10.2）")
	@HasPermission("ont_project_view")
	public R<List<ModelPrefix>> list(@PathVariable Long projectId) {
		return R.ok(modelPrefixService.listByProject(projectId));
	}

	@SysLog("新增IRI前缀")
	@PostMapping("/{projectId}/prefix")
	@Operation(summary = "新增前缀", description = "NCName 校验 + 同项目查重（AC-10.2）")
	@HasPermission("ont_project_manage")
	public R savePrefix(@PathVariable Long projectId, @Valid @RequestBody ModelPrefix prefix) {
		prefix.setProjectId(projectId);
		return modelPrefixService.savePrefix(prefix);
	}

	@SysLog("编辑IRI前缀")
	@PutMapping("/{projectId}/prefix/{prefixId}")
	@Operation(summary = "编辑前缀")
	@HasPermission("ont_project_manage")
	public R updatePrefix(@PathVariable Long projectId, @PathVariable Long prefixId,
			@Valid @RequestBody ModelPrefix prefix) {
		prefix.setId(prefixId);
		prefix.setProjectId(projectId);
		return modelPrefixService.updatePrefix(prefix);
	}

	@SysLog("删除IRI前缀")
	@DeleteMapping("/{projectId}/prefix/{prefixId}")
	@Operation(summary = "删除前缀")
	@HasPermission("ont_project_manage")
	public R removePrefix(@PathVariable Long projectId, @PathVariable Long prefixId) {
		return modelPrefixService.removePrefix(prefixId);
	}
}
```

> 前缀接口挂在 `/ont/model/project/{projectId}/prefix/**` 下，语义清晰（前缀属于项目）。list 端点全量返回（前缀体量小，同治理域 AnnotationProperty 的 list 范式）。

### 4.5 ServiceImpl - ModelProjectServiceImpl

```java
package com.pig4cloud.pig.ontology.modeling.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.entity.ModelPrefix;
import com.pig4cloud.pig.ontology.modeling.entity.ModelProject;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelPrefixMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelProjectMapper;
import com.pig4cloud.pig.ontology.modeling.service.ModelProjectService;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 本体项目 Service 实现（FR-10）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ModelProjectServiceImpl extends ServiceImpl<ModelProjectMapper, ModelProject>
		implements ModelProjectService {

	private static final Set<String> VALID_STATUS = Set.of("draft", "active", "archived");

	private static final Set<String> VALID_FORMAT = Set.of("TTL", "OWL_XML");

	private final ModelPrefixMapper modelPrefixMapper;

	@Override
	public IPage<ModelProject> page(Page page, ModelProject project) {
		return baseMapper.selectPage(page,
				Wrappers.<ModelProject>lambdaQuery()
					.like(StrUtil.isNotBlank(project.getName()), ModelProject::getName, project.getName())
					.eq(StrUtil.isNotBlank(project.getStatus()), ModelProject::getStatus, project.getStatus())
					.orderByDesc(ModelProject::getCreateTime));
	}

	@Override
	public ModelProject getDetail(Long id) {
		return getById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveProject(ModelProject project) {
		// 1. 状态校验（默认 draft）
		if (StrUtil.isBlank(project.getStatus())) {
			project.setStatus("draft");
		}
		if (!VALID_STATUS.contains(project.getStatus())) {
			return R.failed("无效的项目状态：" + project.getStatus());
		}
		// 2. 格式校验（默认 TTL）
		if (StrUtil.isBlank(project.getDefaultFormat())) {
			project.setDefaultFormat("TTL");
		}
		if (!VALID_FORMAT.contains(project.getDefaultFormat())) {
			return R.failed("无效的序列化格式：" + project.getDefaultFormat());
		}
		// 3. 策略强制 B（v1 禁用 A，AC-10.3）
		project.setSerializationStrategy("B");
		// 4. projectCode 预查重
		long count = count(Wrappers.<ModelProject>lambdaQuery()
			.eq(ModelProject::getProjectCode, project.getProjectCode()));
		if (count > 0) {
			return R.failed("项目编码 '" + project.getProjectCode() + "' 已存在");
		}
		// 5. namespaceBase 末尾补 /（IRI 规范）
		if (!project.getNamespaceBase().endsWith("/")) {
			project.setNamespaceBase(project.getNamespaceBase() + "/");
		}
		try {
			return R.ok(save(project));
		}
		catch (DuplicateKeyException e) {
			return R.failed("项目编码 '" + project.getProjectCode() + "' 已存在");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateProject(ModelProject project) {
		ModelProject existing = getById(project.getId());
		if (existing == null) {
			return R.failed("项目不存在");
		}
		// archived 拒绝写（AC-10.4）
		if ("archived".equals(existing.getStatus())) {
			return R.failed("已归档项目不可编辑");
		}
		// 策略强制 B（v1 禁用 A，AC-10.3）
		project.setSerializationStrategy("B");
		// projectCode 不可改（锁定）
		project.setProjectCode(existing.getProjectCode());
		// namespaceBase 末尾补 /
		if (StrUtil.isNotBlank(project.getNamespaceBase())
				&& !project.getNamespaceBase().endsWith("/")) {
			project.setNamespaceBase(project.getNamespaceBase() + "/");
		}
		// 状态校验
		if (StrUtil.isNotBlank(project.getStatus()) && !VALID_STATUS.contains(project.getStatus())) {
			return R.failed("无效的项目状态：" + project.getStatus());
		}
		return R.ok(updateById(project));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeProject(Long id) {
		ModelProject existing = getById(id);
		if (existing == null) {
			return R.failed("项目不存在");
		}
		// 1. 级联软删项目下前缀（前缀是项目从属资源，删项目应一并清理，避免孤儿）
		//    本 DD 已建 ont_model_prefix，可直接操作
		modelPrefixMapper.delete(Wrappers.<ModelPrefix>lambdaQuery()
			.eq(ModelPrefix::getProjectId, id));
		// 2. 类实体关联校验：移交 DD8
		//    DD8 建 ont_model_class 后，在 ModelClass 删除链路或此处补充：
		//    long classCount = modelClassMapper.selectCount(...projectId=id);
		//    if (classCount > 0) return R.failed("项目下存在 N 个类实体，无法删除");
		//    本 DD 不预埋引用未存在 Mapper 的代码（避免编译/语义混淆）
		return R.ok(removeById(id));
	}
}
```

> 范式对齐治理域 `PropertyTemplateServiceImpl`：`baseMapper.selectPage` + `Wrappers.lambdaQuery()` 条件查询 + `@Transactional(rollbackFor=Exception.class)` + `count()` 预查重 + `DuplicateKeyException` 兜底 + 不可改字段锁定（projectCode）。

> **删除校验范围**（评审修订 P-1）：`removeProject` 实际做两件事——(1) 级联软删项目下前缀（本 DD 已建表，立即生效）；(2) 类实体关联校验**整体移交 DD8**（DD8 建 `ont_model_class` 后补）。不在本 DD 预埋"查未存在表"的代码或注释，避免实现者照抄。

### 4.6 ServiceImpl - ModelPrefixServiceImpl

```java
package com.pig4cloud.pig.ontology.modeling.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.entity.ModelPrefix;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelPrefixMapper;
import com.pig4cloud.pig.ontology.modeling.service.ModelPrefixService;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * IRI 前缀 Service 实现（FR-10.2）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ModelPrefixServiceImpl extends ServiceImpl<ModelPrefixMapper, ModelPrefix>
		implements ModelPrefixService {

	/** NCName 规范：字母/下划线开头，含字母数字下划线连句点 */
	private static final Pattern NCNAME_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_.\\-]*$");

	@Override
	public List<ModelPrefix> listByProject(Long projectId) {
		return list(Wrappers.<ModelPrefix>lambdaQuery()
			.eq(ModelPrefix::getProjectId, projectId)
			.orderByDesc(ModelPrefix::getIsDefault)
			.orderByAsc(ModelPrefix::getId));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R savePrefix(ModelPrefix prefix) {
		// 1. NCName 校验（AC-10.2）
		if (!NCNAME_PATTERN.matcher(prefix.getPrefix()).matches()) {
			return R.failed("前缀名 '" + prefix.getPrefix() + "' 不符合 NCName 规范");
		}
		// 2. 同项目查重
		long count = count(Wrappers.<ModelPrefix>lambdaQuery()
			.eq(ModelPrefix::getProjectId, prefix.getProjectId())
			.eq(ModelPrefix::getPrefix, prefix.getPrefix()));
		if (count > 0) {
			return R.failed("前缀名 '" + prefix.getPrefix() + "' 在本项目内已存在");
		}
		if (StrUtil.isBlank(prefix.getIsDefault())) {
			prefix.setIsDefault("0");
		}
		try {
			return R.ok(save(prefix));
		}
		catch (DuplicateKeyException e) {
			return R.failed("前缀名 '" + prefix.getPrefix() + "' 在本项目内已存在");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updatePrefix(ModelPrefix prefix) {
		if (!NCNAME_PATTERN.matcher(prefix.getPrefix()).matches()) {
			return R.failed("前缀名 '" + prefix.getPrefix() + "' 不符合 NCName 规范");
		}
		return R.ok(updateById(prefix));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removePrefix(Long id) {
		return R.ok(removeById(id));
	}
}
```

### 4.7 Mapper

```java
// ModelProjectMapper.java
package com.pig4cloud.pig.ontology.modeling.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.pig4cloud.pig.ontology.modeling.entity.ModelProject;
import org.apache.ibatis.annotations.Mapper;

/**
 * 本体项目 Mapper
 *
 * @author pig
 * @date 2026-07-28
 */
@Mapper
public interface ModelProjectMapper extends MPJBaseMapper<ModelProject> {

}
```

```java
// ModelPrefixMapper.java
package com.pig4cloud.pig.ontology.modeling.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.pig4cloud.pig.ontology.modeling.entity.ModelPrefix;
import org.apache.ibatis.annotations.Mapper;

/**
 * IRI 前缀 Mapper
 *
 * @author pig
 * @date 2026-07-28
 */
@Mapper
public interface ModelPrefixMapper extends MPJBaseMapper<ModelPrefix> {

}
```

> 范式对齐治理域：继承 `MPJBaseMapper<T>`（MyBatis-Plus-Join 增强基类）+ `@Mapper`，空体无自定义 SQL。

### 4.8 Service 接口

```java
// ModelProjectService.java
package com.pig4cloud.pig.ontology.modeling.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.entity.ModelProject;

public interface ModelProjectService extends IService<ModelProject> {

	IPage<ModelProject> page(Page page, ModelProject project);

	ModelProject getDetail(Long id);

	R saveProject(ModelProject project);

	R updateProject(ModelProject project);

	R removeProject(Long id);
}
```

```java
// ModelPrefixService.java
package com.pig4cloud.pig.ontology.modeling.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.entity.ModelPrefix;

import java.util.List;

public interface ModelPrefixService extends IService<ModelPrefix> {

	List<ModelPrefix> listByProject(Long projectId);

	R savePrefix(ModelPrefix prefix);

	R updatePrefix(ModelPrefix prefix);

	R removePrefix(Long id);
}
```

---

## 五、前端设计

### 5.1 API - project.ts

```typescript
import request from '/@/utils/request';

// ---------- 本体项目 ----------

export function pageList(query?: any) {
	return request({
		url: '/admin/ont/model/project/page',
		method: 'get',
		params: query,
	});
}

export function getObj(id: string) {
	return request({
		url: '/admin/ont/model/project/' + id,
		method: 'get',
	});
}

export function addObj(obj: any) {
	return request({
		url: '/admin/ont/model/project',
		method: 'post',
		data: obj,
	});
}

export function putObj(obj: any) {
	return request({
		url: '/admin/ont/model/project/' + obj.id,
		method: 'put',
		data: obj,
	});
}

export function delObj(id: string) {
	return request({
		url: '/admin/ont/model/project/' + id,
		method: 'delete',
	});
}

// ---------- IRI 前缀 ----------

export function listPrefix(projectId: string) {
	return request({
		url: '/admin/ont/model/project/' + projectId + '/prefixes',
		method: 'get',
	});
}

export function addPrefix(projectId: string, obj: any) {
	return request({
		url: '/admin/ont/model/project/' + projectId + '/prefix',
		method: 'post',
		data: obj,
	});
}

export function putPrefix(projectId: string, obj: any) {
	return request({
		url: '/admin/ont/model/project/' + projectId + '/prefix/' + obj.id,
		method: 'put',
		data: obj,
	});
}

export function delPrefix(projectId: string, id: string) {
	return request({
		url: '/admin/ont/model/project/' + projectId + '/prefix/' + id,
		method: 'delete',
	});
}
```

> 范式对齐治理域 `annotation-property.ts`：`request from '/@/utils/request'` + 命名导出 + `/admin/ont/` 前缀 + GET 用 params / POST-PUT 用 data。项目用服务端分页（`pageList`），前缀用全量列表（`listPrefix`）。

### 5.2 index.vue - 项目列表页

```
┌──────────────────────────────────────────────────────────┐
│ [项目名称: ______] [状态: 全部▼] [查询] [重置]   [+新增] │  ← 搜索栏 + 工具栏
├──────────────────────────────────────────────────────────┤
│ #  │ 项目编码    │ 项目名称   │ 命名空间基址      │ 格式 │ 状态  │ 操作        │
│ 1  │ fire-equip  │ 消防设备   │ http://ym/onto/fire/ │ TTL │ active │ 编辑 前缀 删除 │
│ 2  │ hvac        │ 暖通本体   │ http://ym/onto/hvac/ │ TTL │ draft  │ 编辑 前缀 删除 │
├──────────────────────────────────────────────────────────┤
│                                          < 1 2 3 > 10/页 │  ← pagination
└──────────────────────────────────────────────────────────┘
```

**结构要点**：
- `layout-padding` + `layout-padding-auto` 外壳（对齐治理域页面）。
- 搜索栏 `el-row v-show="showSearch"` + `el-form :inline`：项目名称（el-input）、状态（el-select）、查询/重置按钮。
- 工具栏 `el-row`：[+新增] `v-auth="'ont_project_manage'"`、`right-toolbar v-model:showSearch`。
- `el-table :data="state.dataList"`：序号列 / projectCode / name / namespaceBase / defaultFormat（el-tag）/ status（el-tag 颜色区分 draft/active/archived）/ 操作列（编辑/前缀管理/删除，`v-auth="'ont_project_manage'"`）。
- `<pagination v-bind="state.pagination" @current-change @size-change>`。
- `<form-dialog @refresh="getDataList()" ref="formDialogRef" />` 异步组件。
- `<prefix-dialog ref="prefixDialogRef" />` 前缀管理子弹窗。

**关键逻辑**：
```typescript
import { BasicTableProps, useTable } from '/@/hooks/table';
import { pageList, delObj } from '/@/api/ontology-model/project';
import { useMessage, useMessageBox } from '/@/hooks/message';

const state: BasicTableProps = reactive<BasicTableProps>({
	isPage: true,                    // 服务端分页
	queryForm: {
		name: '',
		status: '',
	},
	pageList: pageList,              // 分页 API 函数引用
});

const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state);

// 新增/编辑
const formDialogRef = ref();
const onOpenDialog = (id?: string) => {
	formDialogRef.value.openDialog(id);
};

// 前缀管理
const prefixDialogRef = ref();
const onOpenPrefix = (row: any) => {
	prefixDialogRef.value.openDialog(row.id, row.name);
};

// 删除
const handleDelete = async (row: any) => {
	try {
		await useMessageBox().confirm('确认删除项目「' + row.name + '」？');
		await delObj(row.id);
		useMessage().success('删除成功');
		getDataList();
	} catch (err: any) {
		if (err !== 'cancel') useMessage().error(err.msg);
	}
};
```

### 5.3 form.vue - 项目表单弹窗

**字段表格**：

| 字段 | 组件 | 校验 | 说明 |
|---|---|---|---|
| projectCode | el-input | required, 编辑时 disabled | 项目编码（唯一） |
| name | el-input | required | 项目名称 |
| description | el-input textarea | - | 项目描述 |
| namespaceBase | el-input | required | 命名空间基址（末尾自动补 /） |
| defaultFormat | el-select(TTL/OWL_XML) | required, 默认 TTL | 默认序列化格式 |
| status | el-select(draft/active/archived) | required, 默认 draft | 项目状态 |

> 序列化策略 `serializationStrategy` 不在表单中暴露（v1 强制 B，由后端 saveProject/updateProject 自动设置）。

**范式**：对齐治理域 `form.vue`--`el-dialog` 包 `el-form` + `defineExpose({ openDialog })` + `form.id ? putObj : addObj` + `emit('refresh')` + `useMessage` 反馈 + `nextTick + resetFields` 重置。编辑时 projectCode 输入框 `:disabled="form.id !== ''"` 锁定。

### 5.4 prefix-dialog.vue - 前缀管理子弹窗

**结构**：
- `el-dialog` title="前缀管理 - {项目名称}"，宽度 700px。
- 上方工具栏：[+新增前缀] `v-auth="'ont_project_manage'"`。
- `el-table :data="prefixList"`：prefix / namespace / isDefault（el-tag）/ 操作（编辑/删除）。
- 内嵌 `el-dialog` 前缀表单弹窗（prefix 输入 + namespace 输入 + isDefault 开关）。

> 前缀管理是项目详情的子功能，用独立 `prefix-dialog.vue` 组件，通过 `ref.openDialog(projectId, projectName)` 调用。前缀体量小，用全量列表（`listPrefix`）+ 客户端操作。

### 5.5 composables.ts

```typescript
import { useI18n } from 'vue-i18n';

export function useModelProjectOptions() {
	const { t } = useI18n();

	/** 项目状态下拉选项 */
	const statusOptions = computed(() => [
		{ value: 'draft', label: t('modelProject.statusDraft'), tagType: 'info' },
		{ value: 'active', label: t('modelProject.statusActive'), tagType: 'success' },
		{ value: 'archived', label: t('modelProject.statusArchived'), tagType: 'warning' },
	]);

	/** 序列化格式下拉选项 */
	const formatOptions = computed(() => [
		{ value: 'TTL', label: 'Turtle (.ttl)' },
		{ value: 'OWL_XML', label: 'OWL XML (.owl.xml)' },
	]);

	/** 状态中文标签（列表展示用） */
	const statusLabel = (value: string) => {
		return statusOptions.value.find((o) => o.value === value)?.label ?? value;
	};

	/** 状态标签颜色（el-tag type） */
	const statusTagType = (value: string) => {
		return statusOptions.value.find((o) => o.value === value)?.tagType ?? 'info';
	};

	return { statusOptions, formatOptions, statusLabel, statusTagType };
}
```

> 范式对齐治理域 `composables.ts`：导出 `useXxxOptions()` 返回 computed 选项 + label 反查函数，不持 state。computed 包选项响应 i18n 切换。

### 5.6 i18n - zh-cn.ts

```typescript
export default {
	modelProject: {
		// 字段标签
		index: '#',
		projectCode: '项目编码',
		name: '项目名称',
		description: '项目描述',
		namespaceBase: '命名空间基址',
		defaultFormat: '默认格式',
		serializationStrategy: '序列化策略',
		status: '状态',
		// 状态选项
		statusDraft: '草稿',
		statusActive: '活跃',
		statusArchived: '归档',
		// 操作
		add: '新增项目',
		edit: '编辑',
		delete: '删除',
		prefixManage: '前缀管理',
		// 前缀管理
		prefix: '前缀名',
		namespace: '命名空间',
		isDefault: '默认前缀',
		addPrefix: '新增前缀',
		// inputXxxTip 系列
		inputProjectCodeTip: '请输入项目编码（如 fire-equipment）',
		inputNameTip: '请输入项目名称',
		inputDescriptionTip: '请输入项目描述',
		inputNamespaceBaseTip: '请输入命名空间基址（如 http://youming.com/onto/fire/）',
		selectDefaultFormatTip: '请选择默认序列化格式',
		selectStatusTip: '请选择项目状态',
		inputPrefixTip: '请输入前缀名（如 ex、qudt）',
		inputNamespaceTip: '请输入命名空间 URI',
		// 确认提示
		deleteTip: '确认删除该项目？',
		// 前缀子弹窗标题
		prefixDialogTitle: '前缀管理',
	},
};
```

> 范式对齐治理域 `i18n/zh-cn.ts`：顶层 key=功能名（`modelProject`），字段标签 / 选项 / 操作 / inputXxxTip / 确认提示分组。通用词（addBtn/queryBtn/cancelButtonText/confirmButtonText/addSuccessText 等）走 `common.*` 不在此文件。

### 5.7 i18n - en.ts

```typescript
export default {
	modelProject: {
		index: '#',
		projectCode: 'Project Code',
		name: 'Name',
		description: 'Description',
		namespaceBase: 'Namespace Base',
		defaultFormat: 'Default Format',
		serializationStrategy: 'Serialization Strategy',
		status: 'Status',
		statusDraft: 'Draft',
		statusActive: 'Active',
		statusArchived: 'Archived',
		add: 'New Project',
		edit: 'Edit',
		delete: 'Delete',
		prefixManage: 'Prefixes',
		prefix: 'Prefix',
		namespace: 'Namespace',
		isDefault: 'Default',
		addPrefix: 'New Prefix',
		inputProjectCodeTip: 'Enter project code (e.g. fire-equipment)',
		inputNameTip: 'Enter project name',
		inputDescriptionTip: 'Enter description',
		inputNamespaceBaseTip: 'Enter namespace base (e.g. http://youming.com/onto/fire/)',
		selectDefaultFormatTip: 'Select default format',
		selectStatusTip: 'Select status',
		inputPrefixTip: 'Enter prefix (e.g. ex, qudt)',
		inputNamespaceTip: 'Enter namespace URI',
		deleteTip: 'Confirm to delete this project?',
		prefixDialogTitle: 'Prefix Management',
	},
};
```

---

## 六、横切设计

### 6.1 校验

| 校验点 | 实现位置 | 说明 |
|---|---|---|
| projectCode 唯一 | ServiceImpl 预查重 + DB `uk_ont_model_project_code` 兜底 | 两层校验，覆盖软删后复用场景 |
| prefix 同项目唯一 | ServiceImpl 预查重 + DB `uk_ont_model_prefix(project_id,prefix)` 兜底 | 两层校验 |
| NCName 规范 | ServiceImpl 正则 `^[A-Za-z_][A-Za-z0-9_.\-]*$` | AC-10.2 |
| 状态枚举 | ServiceImpl `Set.of("draft","active","archived")` 白名单 | 非法状态拒绝 |
| 格式枚举 | ServiceImpl `Set.of("TTL","OWL_XML")` 白名单 | 非法格式拒绝 |
| 策略强制 B | ServiceImpl `setSerializationStrategy("B")` | v1 禁用 A，AC-10.3 |
| archived 只读 | ServiceImpl `updateProject` 拦截 | AC-10.4 |
| namespaceBase 补 / | ServiceImpl 末尾检查 | IRI 规范（基址以 / 结尾） |
| projectCode 不可改 | ServiceImpl `setProjectCode(existing.getProjectCode())` 锁定 | 编码是稳定标识 |
| 删除级联清理 | ServiceImpl `removeProject` 级联软删 `ont_model_prefix` | 避免前缀孤儿（P-1） |
| 类实体关联校验 | **移交 DD8**（`ont_model_class` 建表后） | 本 DD 不预埋，AC-10.1 类实体部分由 DD8 兜底 |

### 6.2 异常

- 业务错误用 `R.failed(msg)`（非 500），中文友好提示。
- `DuplicateKeyException` 捕获转友好提示（唯一约束兜底）。
- NCName 正则不匹配返回 `R.failed("前缀名 'xxx' 不符合 NCName 规范")`。

### 6.3 审计

- MybatisPlusMetaObjectHandler 自动填充 `createBy/createTime/updateBy/updateTime/delFlag`（同治理域，无需手写）。
- `@TableField(fill = FieldFill.INSERT/UPDATE)` 标注在 Entity 审计字段上。

### 6.4 双形态验证

| 形态 | 验证步骤 | 预期 |
|---|---|---|
| 单体 pig-boot | 启动 pig-boot（9999，context-path=/admin），前端调 `/admin/ont/model/project/page` | 200 + 分页数据 |
| 微服务 | 启动 pig-ontology-biz + gateway，前端经 gateway 调 `/admin/ont/model/project/page` | 200 + 分页数据 |

### 6.5 安全

- 所有接口 `@HasPermission` 鉴权（`ont_project_view` / `ont_project_manage`）。
- 前端按钮 `v-auth` 指令控制显隐。
- 前缀接口复用项目权限（前缀是项目的子资源，不单独设权限点）。
- **菜单可见性与角色授权**（评审补充 P-3）：V12 仅写入 `sys_menu` 菜单/按钮种子，**不写 `sys_role_menu`**（与治理域 V5~V11 一致）。pig 框架对 `ROLE_ADMIN` 角色有"全部菜单"兜底，故 **admin 登录即可见"本体建模"目录及子菜单**；非 admin 角色（如建模师）需在「角色管理」页手动分配 `ont_project_view`/`ont_project_manage` 权限点后方可见/可操作。此为隐性约定，无需额外种子脚本。

### 6.6 国际化

- 中英双语词条（`i18n/zh-cn.ts` + `i18n/en.ts`）。
- 状态/格式选项用 `computed` 包裹响应 i18n 切换。
- 后端校验消息暂用中文（与治理域一致，后端消息不做 i18n）。

---

## 七、测试要点

| 类别 | 要点 |
|---|---|
| 编译 | `mvn -pl pig-ontology/pig-ontology-biz compile` 通过；modeling 包无编译错误 |
| Flyway（V12） | pig-boot 启动后 `SELECT count(*) FROM ont_model_project` = 0（空表）；`SELECT count(*) FROM ont_model_prefix` = 0；`SELECT menu_id,name FROM sys_menu WHERE menu_id BETWEEN 11000 AND 11104` = 6 条（1 目录 + 1 菜单 + 4 按钮） |
| 菜单可见性 | admin 登录后侧边栏出现"本体建模"一级目录 + "本体项目管理"子菜单 |
| 覆盖（AC-10.5） | `GET /admin/ont/model/project/page?name=fire&status=active` 返回分页结构；名称模糊 + 状态精确过滤 |
| 覆盖（AC-10.1） | `POST /admin/ont/model/project` 新增成功；`GET /{id}` 返回详情；`PUT /{id}` 编辑成功；`DELETE /{id}` 软删成功 |
| 删除级联（AC-10.1） | 项目下有前缀时 `DELETE /{id}`，项目软删**同时**前缀被级联软删（`SELECT count(*) FROM ont_model_prefix WHERE project_id=? AND del_flag='0'` 归零）；类实体校验移交 DD8 |
| 唯一约束（AC-10.1） | 重复 projectCode 新增返回 `R.failed("项目编码 'xxx' 已存在")`；软删后复用同 code 也被 DB 约束拦截 |
| 策略强制（AC-10.3） | 传入 `serializationStrategy='A'` 保存后，DB 中仍为 `'B'`；`GET /{id}` 返回 strategy='B' |
| 状态校验（AC-10.4） | archived 项目调 `PUT /{id}` 返回 `R.failed("已归档项目不可编辑")`；传入非法状态 `xyz` 返回 `R.failed("无效的项目状态：xyz")` |
| NCName（AC-10.2） | 前缀 `1abc`（数字开头）返回 `R.failed("前缀名 '1abc' 不符合 NCName 规范")`；前缀 `ex` 通过 |
| 前缀唯一（AC-10.2） | 同项目重复前缀返回 `R.failed("前缀名 'ex' 在本项目内已存在")`；不同项目可同名前缀 |
| namespaceBase 补 / | 传入 `http://ym/onto/fire` 保存后 DB 中为 `http://ym/onto/fire/` |
| 前缀列表（AC-10.2） | `GET /{projectId}/prefixes` 返回全量前缀，默认前缀排前 |
| 权限 | 无 `ont_project_view` 权限调 page 返回 403；无 `ont_project_manage` 调 POST/PUT/DELETE 返回 403 |
| 菜单授权（P-3） | admin 登录即可见"本体建模"目录 + "本体项目管理"菜单（框架 ROLE_ADMIN 兜底）；非 admin 角色分配 `ont_project_view` 后可见 |
| 双形态 | pig-boot 单体 + 微服务 gateway 两种形态均验证 page/save/prefix 接口 |

---

## 八、风险与缓解

| 风险 | 缓解 |
|---|---|
| DD8 建表前类实体关联校验不生效（AC-10.1 部分） | **类实体校验整体移交 DD8**（建 `ont_model_class` 后补）；本 DD 删除时已**级联软删前缀**子表避免孤儿，项目容器本身可删。不在本 DD 预埋查未存在表的代码 |
| 前缀 NCName 正则过于宽松（允许句点） | NCName 1.1 规范允许句点；若需严格可后续收紧，v1 兼容 Turtle 前缀规范 |
| 建模域菜单与治理域菜单平级可能混淆 | 11000 段独立目录"本体建模"，与治理域 10000"本体治理"分离；权限标识 `ont_project_*` 与治理域 `ont_*` 命名区分 |
| 菜单种子未做 sys_role_menu 授权 | 与治理域一致（不写 role_menu）；admin 由框架 ROLE_ADMIN 兜底可见，非 admin 角色手动分配（P-3） |
| namespaceBase 格式不规范（如缺协议头） | v1 仅校验末尾补 /；后续可加 http(s):// 前缀校验 |

---

## 九、交付物清单

| 类型 | 文件 | 说明 |
|---|---|---|
| 新建 | `modeling/entity/ModelProject.java` | 本体项目 Entity |
| 新建 | `modeling/entity/ModelPrefix.java` | IRI 前缀 Entity |
| 新建 | `modeling/mapper/ModelProjectMapper.java` | 项目 Mapper |
| 新建 | `modeling/mapper/ModelPrefixMapper.java` | 前缀 Mapper |
| 新建 | `modeling/service/ModelProjectService.java` | 项目 Service 接口 |
| 新建 | `modeling/service/ModelPrefixService.java` | 前缀 Service 接口 |
| 新建 | `modeling/service/impl/ModelProjectServiceImpl.java` | 项目 Service 实现 |
| 新建 | `modeling/service/impl/ModelPrefixServiceImpl.java` | 前缀 Service 实现 |
| 新建 | `modeling/controller/ModelProjectController.java` | 项目 Controller |
| 新建 | `modeling/controller/ModelPrefixController.java` | 前缀 Controller |
| 新建 | `V12__ont_model_project_seed.sql` | 建表 + 菜单种子 |
| 新建 | `api/ontology-model/project.ts` | 前端 API |
| 新建 | `views/admin/ontology-model/project/index.vue` | 项目列表页 |
| 新建 | `views/admin/ontology-model/project/form.vue` | 项目表单弹窗 |
| 新建 | `views/admin/ontology-model/project/prefix-dialog.vue` | 前缀管理子弹窗 |
| 新建 | `views/admin/ontology-model/project/composables.ts` | 状态/格式选项 |
| 新建 | `views/admin/ontology-model/project/i18n/zh-cn.ts` | 中文词条 |
| 新建 | `views/admin/ontology-model/project/i18n/en.ts` | 英文词条 |
| 修改 | 无 | 复用 pig-gateway 已有路由、pig-ontology-biz 已有依赖，无配置修改 |

---

## 十、与 PRD 边界的对齐确认（防混淆备忘）

| 维度 | PRD 约定 | 本 DD 实现 | 对齐 |
|---|---|---|---|
| 模块 | pig-ontology-biz 内新增 modeling 包（PRD 3.1） | `com.pig4cloud.pig.ontology.modeling.*` | ✓ |
| 序列化策略 | v1 默认 B，A 禁用切换（PRD FR-10.3 / 5.2） | saveProject/updateProject 强制 `strategy='B'` | ✓ |
| 个体实例 | Out of Scope，后置 M10（PRD 第十八节） | 不涉及 | ✓ |
| 菜单 ID | 11000–11599（PRD 13.1） | 11000 目录 + 11100 菜单 + 11101~11104 按钮 | ✓ |
| 权限标识 | ont_project_view / ont_project_manage（PRD 13.2） | Controller @HasPermission 对齐 | ✓ |
| Flyway | V12 起（PRD 9 / 设计计划 3.2） | V12__ont_model_project_seed.sql | ✓ |
| 接口路径 | /admin/ont/model/project（PRD 10.1） | @RequestMapping("/ont/model/project") | ✓ |
| 表前缀 | ont_model_（PRD 9 / 设计计划 3.2） | ont_model_project / ont_model_prefix | ✓ |
| 治理域消费 | 不修改治理域表/接口/代码（PRD NFR-C3 / R-23） | 本 DD 不引用治理域任何代码/表 | ✓ |
| 删除校验 | 删除时校验关联类实体（PRD FR-10.1） | 本 DD 级联软删前缀；类实体校验移交 DD8 | ✓（前缀本 DD / 类实体 DD8） |

---

*本 DD 是建模域首个详细设计，为 DD8（类实体创建）提供项目容器与命名空间基址。评审通过后进入实现，实现完成后更新设计计划 DD7 状态为"已完成"。*
