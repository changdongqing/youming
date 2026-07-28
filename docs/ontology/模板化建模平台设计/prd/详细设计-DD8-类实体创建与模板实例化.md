# 详细设计-DD8-类实体创建与模板实例化

| 项 | 内容 |
|---|---|
| 文档名称 | 类实体创建与模板实例化 详细设计 |
| 里程碑 | M6（FR-11） |
| 上游 PRD | 《本体建模功能产品需求文档.md》v1.0（FR-11 / AC-11.1~11.8 / 9.3（+9.4/9.5 属性表前移）/ 10.2 / 第十一节场景一/三 / 第十四节 M6） |
| 设计计划 | 《本体建模功能详细设计计划.md》DD8（M6） |
| 前置依赖 | DD7 已落地（ont_model_project / ont_model_prefix 就绪，项目容器与命名空间可用）；治理域 DD2/DD3 已落地（Supply API class-template/tree + class-template/{code}/inherited + property-templates 可消费） |
| 编写日期 | 2026-07-28 |
| 文档状态 | 评审通过（附优化项已合入），进入实现 |

> **评审决策记录**（2026-07-28 评审合入）：
> - **消费方式改为方案 B**：`ClassInstantiationService` 通过**同模块只读 Service 注入**（`ClassTemplateService.inheritedView` + `PropertyTemplateService`）消费治理域能力，不再走 HTTP RestTemplate。依据 PRD R-23 "同模块内只读查询"豁免；消除鉴权 token 透传问题（P-1）、序列化开销、localhost 自调用线程占用。只读调用，不写治理域表。
> - **补充 instantiate 端点**：本 DD 实现 PRD 10.2 规定的 `POST /{id}/instantiate`（对已有空白类追加模板属性，服务场景三），不只做新建时实例化。
> - **删除 ModelingConfig**：pig-common-core 已全局注册 RestTemplate Bean（方案 B 改用 Service 注入后连 RestTemplate 也不需要）。

---

## 一、设计目标与范围

### 1.1 目标

- 创建 `owl:Class` 实体（`ont_model_class`），有独立 IRI（项目命名空间 + localName），区别于治理域分类模板。
- 支持基于分类模板创建：选模板 -> 调 Supply API inherited 端点 -> 批量实例化属性（挂 templateRef 溯源）。
- 支持空白类创建（不基于模板，手动添加属性）。
- 实现核心算法 `ClassInstantiationService`：从 InheritedViewVO 的 properties 清单批量生成数据属性/对象属性记录。
- **属性表前移**：`ont_model_datatype_property` + `ont_model_object_property` 两张属性表从 DD9/V14 前移到 DD8/V13，使模板实例化时能完整创建类 + 属性。

### 1.2 范围（本 DD 做 / 不做）

| 做（本 DD） | 不做（后续 DD / Out of Scope） |
|---|---|
| 类实体 CRUD（FR-11.1/11.6/11.7/11.8） | 数据属性独立建模（手动创建/编辑/删除，DD9/FR-12） |
| 基于分类模板创建 + 实例化（FR-11.2） | 对象属性独立建模（domain/range 编辑，DD9/FR-13） |
| 空白类创建（FR-11.3） | 类层级 subClassOf 建立（DD9/FR-14） |
| **对已有类追加模板实例化（`POST /{id}/instantiate`，场景三）** | 镜像回推（DD9/FR-14.3） |
| 模板实例化溯源 templateRef（FR-11.4） | 序列化（DD10/FR-15） |
| 分类模板溯源 classificationCode（FR-11.5） | 画布拖拽创建（DD11/FR-17，DD8 提供表单式创建） |
| 属性表 DDL 前移到 V13 | 个体实例（M10/DD12，Out of Scope） |
| 消费治理域（方案 B：同模块只读 Service 注入） | |

> **属性表前移说明**：PRD 9.4/9.5 原将 `ont_model_datatype_property`/`ont_model_object_property` 划归 DD9/V14。但 DD8 模板实例化（FR-11.2）必须同时创建类 + 属性，否则"实例化"只产空壳类无意义。因此将两张属性表的 **DDL 前移到 V13**（与 ont_model_class 同脚本），DD9 不再建表只实现属性的手动 CRUD/单位绑定/枚举/基数等业务逻辑。设计计划相应调整。

### 1.3 验收映射（M6 DoD）

| PRD AC | 本 DD 实现点 |
|---|---|
| AC-11.1 类创建 + IRI 唯一 | 4.3 ModelClassController.save + 4.5 ServiceImpl.saveClass（IRI = namespace_base + localName，预查重 + DB 唯一约束） |
| AC-11.2 基于模板创建调 inherited + 批量实例化 | 4.6 ClassInstantiationService（同模块注入 `ClassTemplateService.inheritedView`，遍历 properties 批量建属性）；**新建时实例化**（saveClass 内）+ **独立 instantiate 端点**（POST /{id}/instantiate，场景三） |
| AC-11.3 空白类创建 + 属性列表初始为空 | 4.5 ServiceImpl.saveClass（templateCode 为空时跳过实例化） |
| AC-11.4 实例化属性 templateRef 非空 | 4.6 ClassInstantiationService（每条属性 template_ref = propertyTemplateCode） |
| AC-11.5 基于模板的类 classificationCode 非空 | 4.5 ServiceImpl.saveClass（从 InheritedViewVO 复制 classificationCode） |
| AC-11.6 类 CRUD + 删除校验 + 软删 | 4.3 Controller + 4.5 ServiceImpl（删除校验被 subClassOf 引用 + 有属性） |
| AC-11.7 列表查询 + projectId 过滤 + 名称搜索 | 4.3 Controller.page + 4.5 ServiceImpl.page |
| AC-11.8 详情含属性列表 + 父类子类 + 溯源 | 4.3 Controller.getById + 4.5 ServiceImpl.getDetail（含数据属性/对象属性/父类子类 IRI） |

---

## 二、落地清单

### 2.1 后端文件清单

```
server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/modeling/
├── controller/
│   └── ModelClassController.java            # 新增：类实体 CRUD + 分页 + 实例化
├── service/
│   ├── ModelClassService.java               # 新增：类 Service 接口
│   ├── ClassInstantiationService.java       # 新增：模板实例化核心算法 Service
│   └── impl/
│       └── ModelClassServiceImpl.java       # 新增：类 Service 实现
├── mapper/
│   ├── ModelClassMapper.java                # 新增：类 Mapper
│   ├── ModelDatatypePropertyMapper.java     # 新增：数据属性 Mapper（表前移，DD9 复用）
│   └── ModelObjectPropertyMapper.java       # 新增：对象属性 Mapper（表前移，DD9 复用）
├── entity/
│   ├── ModelClass.java                      # 新增：类实体 Entity
│   ├── ModelDatatypeProperty.java           # 新增：数据属性 Entity（表前移，DD9 复用）
│   └── ModelObjectProperty.java             # 新增：对象属性 Entity（表前移，DD9 复用）
├── dto/
│   └── ClassInstantiateDTO.java             # 新增：模板实例化请求 DTO
└── vo/
    ├── ModelClassDetailVO.java              # 新增：类详情 VO（含属性列表+父类子类）
    └── ModelClassPageVO.java                # 新增：类分页 VO（轻量，含属性数+溯源标记）
```

### 2.2 数据库文件清单

```
server/pig-common/pig-common-data/src/main/resources/db/migration/
└── V13__ont_model_class_seed.sql            # 新增：建 ont_model_class + 两张属性表 + 菜单种子
```

### 2.3 依赖变更清单

无新增依赖。复用 pig-ontology-biz 已有依赖（MyBatis-Plus / hutool / Caffeine / springdoc）。
治理域消费采用**方案 B**：同模块只读 Service 注入（`ClassTemplateService` + `PropertyTemplateService` + `InheritedViewService`），不走 HTTP，无新增 RestTemplate 配置。

### 2.4 前端文件清单

```
web/src/
├── api/ontology-model/
│   └── class.ts                             # 新增：类实体 API
└── views/admin/ontology-model/
    └── class/
        ├── index.vue                        # 新增：类列表页（左项目树右类列表）
        ├── form.vue                         # 新增：类表单弹窗（含模板选择+继承属性预览）
        ├── inherited-preview.vue            # 新增：继承属性预览面板（选模板后展示）
        ├── composables.ts                   # 新增：选项/工具函数
        └── i18n/
            ├── zh-cn.ts                     # 新增
            └── en.ts                        # 新增
```

---

## 三、数据库设计（V13）

### 3.1 V13 脚本范围

V13 一次性完成：
- (a) 建表 `ont_model_class`（本体类实体）+ `ont_model_datatype_property`（数据属性，前移）+ `ont_model_object_property`（对象属性，前移）
- (b) 建索引
- (c) sys_menu 菜单种子（11200 段"本体类建模" + 11201~11204 权限点按钮）
- (d) 回填 DD7 的 removeProject 删除校验（DD7 预留的关联类实体校验，V13 建表后生效，无需额外脚本--DD7 代码取消注释即可）

### 3.2 V13__ont_model_class_seed.sql

```sql
-- ============================================================
-- V13: 建模域 - 类实体 + 数据属性 + 对象属性（FR-11）
-- 建 ont_model_class + ont_model_datatype_property + ont_model_object_property
-- + 菜单种子（11200 段）
-- 注：数据属性/对象属性表从 DD9/V14 前移至此，因模板实例化需同时建类+属性
-- ============================================================

-- ---------- (a) 建表 ----------

CREATE TABLE ont_model_class (
    id                   bigint       NOT NULL,
    project_id           bigint       NOT NULL,
    class_iri            varchar(255) NOT NULL,
    local_name           varchar(128) NOT NULL,
    label                varchar(128),
    label_cn             varchar(128),
    description          varchar(512),
    template_code        varchar(64),
    classification_code  varchar(64),
    icon                 varchar(64),
    color                varchar(16),
    sort_order           int          DEFAULT 0,
    create_by            varchar(64)  DEFAULT ' ',
    create_time          timestamp    DEFAULT now(),
    update_by            varchar(64)  DEFAULT ' ',
    update_time          timestamp    DEFAULT now(),
    del_flag             char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_model_class PRIMARY KEY (id),
    CONSTRAINT uk_ont_model_class_iri UNIQUE (project_id, class_iri)
);
COMMENT ON TABLE  ont_model_class IS '本体类实体（owl:Class，建模域权威，FR-11）';
COMMENT ON COLUMN ont_model_class.project_id          IS '所属项目 ID';
COMMENT ON COLUMN ont_model_class.class_iri           IS '类的 IRI（rdf:about），项目内唯一';
COMMENT ON COLUMN ont_model_class.local_name          IS 'IRI 本地名（class_iri = namespace_base + local_name）';
COMMENT ON COLUMN ont_model_class.template_code       IS '溯源：来源分类模板 template_code，NULL=非模板创建';
COMMENT ON COLUMN ont_model_class.classification_code IS '溯源：来源分类模板 classification_code，NULL=非模板创建';
COMMENT ON COLUMN ont_model_class.icon                IS '外观：图标（从模板继承或自定义）';
COMMENT ON COLUMN ont_model_class.color               IS '外观：颜色（从模板继承或自定义）';

CREATE TABLE ont_model_datatype_property (
    id                  bigint       NOT NULL,
    project_id          bigint       NOT NULL,
    class_id            bigint       NOT NULL,
    property_iri        varchar(255) NOT NULL,
    local_name          varchar(128) NOT NULL,
    label               varchar(128),
    template_code       varchar(64),
    xsd_type            varchar(64)  NOT NULL,
    unit_ref            varchar(255),
    enum_values         text,
    min_cardinality     int          DEFAULT 0,
    max_cardinality     int          DEFAULT -1,
    is_identifier       char(1)      DEFAULT '0',
    sort_order          int          DEFAULT 0,
    create_by           varchar(64)  DEFAULT ' ',
    create_time         timestamp    DEFAULT now(),
    update_by           varchar(64)  DEFAULT ' ',
    update_time         timestamp    DEFAULT now(),
    del_flag            char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_model_dt_prop PRIMARY KEY (id),
    CONSTRAINT uk_ont_model_dt_prop_iri UNIQUE (project_id, property_iri)
);
COMMENT ON TABLE  ont_model_datatype_property IS '数据属性（owl:DatatypeProperty，FR-12，表前移自 DD9）';
COMMENT ON COLUMN ont_model_datatype_property.class_id      IS '所属类 ID（domain）';
COMMENT ON COLUMN ont_model_datatype_property.property_iri  IS '属性 IRI（方案B: {classLocalName}_{propLocalName}）';
COMMENT ON COLUMN ont_model_datatype_property.template_code IS '溯源：来源属性模板 template_code，NULL=手建';
COMMENT ON COLUMN ont_model_datatype_property.xsd_type      IS 'XSD 数据类型，如 xsd:string / xsd:integer';
COMMENT ON COLUMN ont_model_datatype_property.unit_ref      IS '单位引用（QUDT IRI），序列化输出 ont:unitRef';
COMMENT ON COLUMN ont_model_datatype_property.enum_values   IS '枚举值（JSON 数组），序列化输出 owl:oneOf';
COMMENT ON COLUMN ont_model_datatype_property.is_identifier IS '是否标识符，序列化输出 ont:isIdentifier';
COMMENT ON COLUMN ont_model_datatype_property.max_cardinality IS '最大基数（-1=无限制）';

CREATE TABLE ont_model_object_property (
    id                  bigint       NOT NULL,
    project_id          bigint       NOT NULL,
    domain_class_id     bigint       NOT NULL,
    range_class_id      bigint       NOT NULL,
    property_iri        varchar(255) NOT NULL,
    local_name          varchar(128) NOT NULL,
    label               varchar(128),
    template_code       varchar(64),
    min_cardinality     int          DEFAULT 0,
    max_cardinality     int          DEFAULT -1,
    inverse_of          bigint,
    sort_order          int          DEFAULT 0,
    create_by           varchar(64)  DEFAULT ' ',
    create_time         timestamp    DEFAULT now(),
    update_by           varchar(64)  DEFAULT ' ',
    update_time         timestamp    DEFAULT now(),
    del_flag            char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_model_obj_prop PRIMARY KEY (id),
    CONSTRAINT uk_ont_model_obj_prop_iri UNIQUE (project_id, property_iri)
);
COMMENT ON TABLE  ont_model_object_property IS '对象属性（owl:ObjectProperty，FR-13，表前移自 DD9）';
COMMENT ON COLUMN ont_model_object_property.domain_class_id IS '域类 ID（domain）';
COMMENT ON COLUMN ont_model_object_property.range_class_id  IS '值域类 ID（range）';
COMMENT ON COLUMN ont_model_object_property.template_code   IS '溯源：来源属性模板 template_code，NULL=手建';
COMMENT ON COLUMN ont_model_object_property.inverse_of      IS '反向属性 ID，可空（owl:inverseOf）';

-- ---------- (b) 索引 ----------

CREATE INDEX idx_ont_model_class_project ON ont_model_class (project_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_model_dt_prop_class ON ont_model_datatype_property (class_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_model_obj_prop_dom  ON ont_model_object_property (domain_class_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_model_obj_prop_rng  ON ont_model_object_property (range_class_id) WHERE del_flag = '0';

-- ---------- (c) sys_menu 菜单种子（11200 段） ----------

INSERT INTO sys_menu VALUES (11200, '本体类建模', NULL, '/admin/ontology-model/class/index', NULL, 11000, 'iconfont icon-leixing', '1', 2, '0', '0', '0', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11201, '类新增', 'ont_class_model_manage', NULL, NULL, 11200, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11202, '类编辑', 'ont_class_model_manage', NULL, NULL, 11200, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11203, '类删除', 'ont_class_model_manage', NULL, NULL, 11200, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11204, '类查看', 'ont_class_model_view',   NULL, NULL, 11200, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
```

> **属性表前移说明**：`ont_model_datatype_property` + `ont_model_object_property` 原 PRD 9.4/9.5 划归 DD9/V14。前移原因：DD8 模板实例化（FR-11.2）必须同时创建类 + 属性记录，否则实例化只产空壳类。DD9 不再建表，只实现属性的手动 CRUD / 单位绑定 / 枚举 / 基数 / domain-range 等业务逻辑。设计计划 V14 相应调整为"仅建 ont_model_subclassof + 11300 菜单种子"。

> **sys_menu 17 字段**：按位置 INSERT，parent_id=11000（本体建模目录），菜单 menu_type='0' embedded='0'，按钮 menu_type='1' embedded=NULL。

---

## 四、后端设计

> 包路径 `com.pig4cloud.pig.ontology.modeling.*`。Controller 路径 `/ont/model/class`，对外 `/admin/ont/model/class/**`。

### 4.1 Entity - ModelClass

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
 * 本体类实体 Entity（owl:Class，建模域权威，FR-11）
 * <p>
 * 区别于治理域 ClassTemplate：本表是实际 owl:Class 实体，有独立 IRI（rdf:about），
 * 进入 RDF 序列化。ClassTemplate 是治理资产侧原型，不含 IRI，不进 RDF。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "本体类实体")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_model_class")
public class ModelClass extends Model<ModelClass> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "所属项目 ID")
	private Long projectId;

	@Schema(description = "类的 IRI（rdf:about），项目内唯一")
	@NotBlank(message = "类 IRI 不能为空")
	private String classIri;

	@Schema(description = "IRI 本地名")
	@NotBlank(message = "本地名不能为空")
	private String localName;

	@Schema(description = "rdfs:label")
	private String label;

	@Schema(description = "中文标签")
	private String labelCn;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "溯源：来源分类模板 template_code，NULL=非模板创建")
	private String templateCode;

	@Schema(description = "溯源：来源分类模板 classification_code，NULL=非模板创建")
	private String classificationCode;

	@Schema(description = "外观：图标")
	private String icon;

	@Schema(description = "外观：颜色")
	private String color;

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

### 4.2 Entity - ModelDatatypeProperty / ModelObjectProperty

```java
// ModelDatatypeProperty.java
package com.pig4cloud.pig.ontology.modeling.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据属性 Entity（owl:DatatypeProperty，FR-12，表前移自 DD9）
 * <p>
 * 模板实例化时由 ClassInstantiationService 批量创建；DD9 实现手动 CRUD/单位绑定/枚举/基数。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "数据属性")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_model_datatype_property")
public class ModelDatatypeProperty extends Model<ModelDatatypeProperty> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "所属项目 ID")
	private Long projectId;

	@Schema(description = "所属类 ID（domain）")
	private Long classId;

	@Schema(description = "属性 IRI（方案B: {classLocalName}_{propLocalName}）")
	@NotBlank(message = "属性 IRI 不能为空")
	private String propertyIri;

	@Schema(description = "本地名")
	@NotBlank(message = "本地名不能为空")
	private String localName;

	@Schema(description = "rdfs:label")
	private String label;

	@Schema(description = "溯源：来源属性模板 template_code，NULL=手建")
	private String templateCode;

	@Schema(description = "XSD 数据类型，如 xsd:string / xsd:integer")
	@NotBlank(message = "XSD 类型不能为空")
	private String xsdType;

	@Schema(description = "单位引用（QUDT IRI）")
	private String unitRef;

	@Schema(description = "枚举值（JSON 数组）")
	private String enumValues;

	@Schema(description = "最小基数")
	private Integer minCardinality;

	@Schema(description = "最大基数（-1=无限制）")
	private Integer maxCardinality;

	@Schema(description = "是否标识符")
	private String isIdentifier;

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

```java
// ModelObjectProperty.java
package com.pig4cloud.pig.ontology.modeling.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 对象属性 Entity（owl:ObjectProperty，FR-13，表前移自 DD9）
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "对象属性")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_model_object_property")
public class ModelObjectProperty extends Model<ModelObjectProperty> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "所属项目 ID")
	private Long projectId;

	@Schema(description = "域类 ID（domain）")
	private Long domainClassId;

	@Schema(description = "值域类 ID（range）")
	private Long rangeClassId;

	@Schema(description = "属性 IRI")
	@NotBlank(message = "属性 IRI 不能为空")
	private String propertyIri;

	@Schema(description = "本地名")
	@NotBlank(message = "本地名不能为空")
	private String localName;

	@Schema(description = "rdfs:label")
	private String label;

	@Schema(description = "溯源：来源属性模板 template_code，NULL=手建")
	private String templateCode;

	@Schema(description = "最小基数")
	private Integer minCardinality;

	@Schema(description = "最大基数（-1=无限制）")
	private Integer maxCardinality;

	@Schema(description = "反向属性 ID，可空（owl:inverseOf）")
	private Long inverseOf;

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

### 4.3 Controller - ModelClassController

```java
package com.pig4cloud.pig.ontology.modeling.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.service.ModelClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 本体类实体 Controller（FR-11）
 * <p>
 * 路径 /ont/model/class/**，对外 /admin/ont/model/class/**
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/model/class")
@Tag(name = "本体类建模", description = "类实体 CRUD + 模板实例化（FR-11）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ModelClassController {

	private final ModelClassService modelClassService;

	@GetMapping("/page")
	@Operation(summary = "分页查询", description = "按项目/名称/模板溯源过滤（AC-11.7）")
	@HasPermission("ont_class_model_view")
	public R<IPage<ModelClass>> page(@ParameterObject Page page, @ParameterObject ModelClass cls) {
		return R.ok(modelClassService.page(page, cls));
	}

	@GetMapping("/{id}")
	@Operation(summary = "类详情", description = "含数据属性/对象属性列表 + 父类子类 IRI + 溯源（AC-11.8）")
	@HasPermission("ont_class_model_view")
	public R<ModelClassDetailVO> getById(@PathVariable Long id) {
		return R.ok(modelClassService.getDetail(id));
	}

	@SysLog("新增本体类")
	@PostMapping
	@Operation(summary = "新建类", description = "支持基于模板创建（传 templateCode 触发实例化，AC-11.1~11.5）")
	@HasPermission("ont_class_model_manage")
	public R save(@Valid @RequestBody ModelClass cls) {
		return modelClassService.saveClass(cls);
	}

	@SysLog("编辑本体类")
	@PutMapping("/{id}")
	@Operation(summary = "编辑类", description = "可修改 label/描述/外观（AC-11.6）")
	@HasPermission("ont_class_model_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody ModelClass cls) {
		cls.setId(id);
		return modelClassService.updateClass(cls);
	}

	@SysLog("删除本体类")
	@DeleteMapping("/{id}")
	@Operation(summary = "删除类", description = "校验被 subClassOf 引用 + 有属性，软删（AC-11.6）")
	@HasPermission("ont_class_model_manage")
	public R removeById(@PathVariable Long id) {
		return modelClassService.removeClass(id);
	}

	@SysLog("模板实例化属性")
	@PostMapping("/{id}/instantiate")
	@Operation(summary = "基于分类模板实例化属性", description = "对已有空白类追加模板属性（场景三，PRD 10.2）")
	@HasPermission("ont_class_model_manage")
	public R instantiate(@PathVariable Long id, @Valid @RequestBody ClassInstantiateDTO dto) {
		return modelClassService.instantiateFromClass(id, dto);
	}
}
```

> **端点齐全性**（评审补充 P-2）：本 Controller 实现 PRD 10.2 全部 6 个端点（page/getById/save/PUT/DELETE/instantiate）。`POST /{id}/instantiate` 服务于场景三"空白类右键从模板实例化"，对已有类追加属性；`save` 内部实例化服务于场景一"新建时即选模板"。两者复用同一 `ClassInstantiationService.instantiate`。

### 4.4 DTO - ClassInstantiateDTO

```java
package com.pig4cloud.pig.ontology.modeling.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模板实例化请求 DTO
 * <p>
 * 用于已有空白类追加模板属性的场景（画布右键"从模板实例化"）。
 * 新建类时直接在 ModelClass.templateCode 中传模板编码，走 saveClass 内部实例化。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "模板实例化请求")
public class ClassInstantiateDTO {

	@Schema(description = "分类模板 template_code（必填）")
	@NotBlank(message = "分类模板编码不能为空")
	private String templateCode;
}
```

### 4.5 ServiceImpl - ModelClassServiceImpl

```java
package com.pig4cloud.pig.ontology.modeling.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.dto.ClassInstantiateDTO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelProject;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelClassMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelDatatypePropertyMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelObjectPropertyMapper;
import com.pig4cloud.pig.ontology.modeling.service.ClassInstantiationService;
import com.pig4cloud.pig.ontology.modeling.service.ModelClassService;
import com.pig4cloud.pig.ontology.modeling.service.ModelProjectService;
import com.pig4cloud.pig.ontology.modeling.vo.ModelClassDetailVO;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * 本体类实体 Service 实现（FR-11）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ModelClassServiceImpl extends ServiceImpl<ModelClassMapper, ModelClass>
		implements ModelClassService {

	private final ClassInstantiationService instantiationService;
	private final ModelDatatypePropertyMapper datatypePropertyMapper;
	private final ModelObjectPropertyMapper objectPropertyMapper;
	private final ModelProjectService modelProjectService;

	@Override
	public IPage<ModelClass> page(Page page, ModelClass cls) {
		return baseMapper.selectPage(page,
				Wrappers.<ModelClass>lambdaQuery()
					.eq(cls.getProjectId() != null, ModelClass::getProjectId, cls.getProjectId())
					.like(StrUtil.isNotBlank(cls.getLabel()), ModelClass::getLabel, cls.getLabel())
					.eq(StrUtil.isNotBlank(cls.getTemplateCode()),
							ModelClass::getTemplateCode, cls.getTemplateCode())
					.orderByDesc(ModelClass::getCreateTime));
	}

	@Override
	public ModelClassDetailVO getDetail(Long id) {
		ModelClass cls = getById(id);
		if (cls == null) {
			return null;
		}
		ModelClassDetailVO vo = new ModelClassDetailVO();
		vo.setId(cls.getId());
		vo.setProjectId(cls.getProjectId());
		vo.setClassIri(cls.getClassIri());
		vo.setLocalName(cls.getLocalName());
		vo.setLabel(cls.getLabel());
		vo.setLabelCn(cls.getLabelCn());
		vo.setDescription(cls.getDescription());
		vo.setTemplateCode(cls.getTemplateCode());
		vo.setClassificationCode(cls.getClassificationCode());
		vo.setIcon(cls.getIcon());
		vo.setColor(cls.getColor());
		// 数据属性列表
		vo.setDatatypeProperties(datatypePropertyMapper.selectList(
				Wrappers.<ModelDatatypeProperty>lambdaQuery()
					.eq(ModelDatatypeProperty::getClassId, id)
					.orderByAsc(ModelDatatypeProperty::getSortOrder)));
		// 对象属性列表（作为 domain）
		vo.setObjectProperties(objectPropertyMapper.selectList(
				Wrappers.<ModelObjectProperty>lambdaQuery()
					.eq(ModelObjectProperty::getDomainClassId, id)
					.orderByAsc(ModelObjectProperty::getSortOrder)));
		// 父类/子类 IRI 列表（DD9 建 ont_model_subclassof 表后填充，v1 返回空列表）
		vo.setParentClassIris(new ArrayList<>());
		vo.setChildClassIris(new ArrayList<>());
		return vo;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveClass(ModelClass cls) {
		// 1. localName 必填（IRI 本地名）
		if (StrUtil.isBlank(cls.getLocalName())) {
			return R.failed("本地名不能为空");
		}
		// 2. IRI 拼接与一致性校验（评审补充 P-5）
		//    IRI = project.namespace_base + localName；若前端已传 classIri 则校验一致性
		ModelProject project = modelProjectService.getById(cls.getProjectId());
		if (project == null) {
			return R.failed("项目不存在");
		}
		String expectedIri = project.getNamespaceBase() + cls.getLocalName();
		if (StrUtil.isBlank(cls.getClassIri())) {
			cls.setClassIri(expectedIri);
		}
		else if (!expectedIri.equals(cls.getClassIri())) {
			return R.failed("类 IRI 与命名空间基址/本地名不一致，期望：" + expectedIri);
		}
		// 3. 同项目 IRI 预查重
		long count = count(Wrappers.<ModelClass>lambdaQuery()
			.eq(ModelClass::getProjectId, cls.getProjectId())
			.eq(ModelClass::getClassIri, cls.getClassIri()));
		if (count > 0) {
			return R.failed("类 IRI '" + cls.getClassIri() + "' 在项目内已存在");
		}
		// 4. 保存类实体
		try {
			save(cls);
		}
		catch (DuplicateKeyException e) {
			return R.failed("类 IRI '" + cls.getClassIri() + "' 在项目内已存在");
		}
		// 5. 基于模板实例化属性（AC-11.2~11.5）
		if (StrUtil.isNotBlank(cls.getTemplateCode())) {
			R instantiateResult = instantiationService.instantiate(cls);
			if (instantiateResult.getCode() != 0) {
				// 实例化失败回滚（@Transactional 已标记 rollback）
				return R.failed("模板实例化失败：" + instantiateResult.getMsg());
			}
		}
		// 空白类创建（AC-11.3）：templateCode 为空时跳过实例化，属性列表初始为空
		return R.ok(cls);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateClass(ModelClass cls) {
		ModelClass existing = getById(cls.getId());
		if (existing == null) {
			return R.failed("类不存在");
		}
		// classIri / projectId / localName 不可改（IRI 标识锁定）
		cls.setClassIri(existing.getClassIri());
		cls.setProjectId(existing.getProjectId());
		cls.setLocalName(existing.getLocalName());
		// templateCode / classificationCode 不可改（溯源锁定）
		cls.setTemplateCode(existing.getTemplateCode());
		cls.setClassificationCode(existing.getClassificationCode());
		// 空值保护（评审补充 P-5）：前端只传部分字段时，保留 existing 的非锁定可编辑字段
		if (cls.getLabel() == null) {
			cls.setLabel(existing.getLabel());
		}
		if (cls.getDescription() == null) {
			cls.setDescription(existing.getDescription());
		}
		if (cls.getSortOrder() == null) {
			cls.setSortOrder(existing.getSortOrder());
		}
		return R.ok(updateById(cls));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R instantiateFromClass(Long id, ClassInstantiateDTO dto) {
		// 场景三：对已有空白类追加模板属性（PRD 10.2 POST /{id}/instantiate）
		ModelClass cls = getById(id);
		if (cls == null) {
			return R.failed("类不存在");
		}
		if (StrUtil.isNotBlank(cls.getTemplateCode()) && cls.getTemplateCode().equals(dto.getTemplateCode())) {
			return R.failed("该类已基于模板 " + dto.getTemplateCode() + " 创建，不可重复实例化");
		}
		// 设置模板编码后走实例化；溯源字段（templateCode/classificationCode）由 instantiate 回填
		cls.setTemplateCode(dto.getTemplateCode());
		return instantiationService.instantiate(cls);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeClass(Long id) {
		// 删除校验：是否有数据属性
		long dtCount = datatypePropertyMapper.selectCount(Wrappers.<ModelDatatypeProperty>lambdaQuery()
			.eq(ModelDatatypeProperty::getClassId, id));
		if (dtCount > 0) {
			return R.failed("类下存在 " + dtCount + " 个数据属性，无法删除");
		}
		// 删除校验：是否有对象属性（domain 或 range）
		long objCount = objectPropertyMapper.selectCount(Wrappers.<ModelObjectProperty>lambdaQuery()
			.eq(ModelObjectProperty::getDomainClassId, id)
			.or()
			.eq(ModelObjectProperty::getRangeClassId, id));
		if (objCount > 0) {
			return R.failed("类被 " + objCount + " 个对象属性引用，无法删除");
		}
		// 删除校验：是否被 subClassOf 引用（DD9 建表后启用，v1 跳过）
		return R.ok(removeById(id));
	}
}
```

> **删除校验**：v1（DD8）校验数据属性 + 对象属性引用；subClassOf 引用校验以注释预留，DD9 建表后启用。

> **不可改字段**：classIri / projectId / localName（IRI 标识）/ templateCode / classificationCode（溯源锁定），编辑只允许修改 label / labelCn / description / icon / color / sortOrder。

### 4.6 ClassInstantiationService - 模板实例化核心算法

```java
package com.pig4cloud.pig.ontology.modeling.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.entity.ClassTemplate;
import com.pig4cloud.pig.ontology.api.entity.PropertyTemplate;
import com.pig4cloud.pig.ontology.api.vo.InheritedPropertyVO;
import com.pig4cloud.pig.ontology.api.vo.InheritedViewVO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelClassMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelDatatypePropertyMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelObjectPropertyMapper;
import com.pig4cloud.pig.ontology.service.ClassTemplateService;
import com.pig4cloud.pig.ontology.service.PropertyTemplateService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板实例化核心算法 Service（FR-11.2~11.5）
 * <p>
 * 消费治理域能力（方案 B：同模块只读 Service 注入，PRD R-23 "同模块内只读查询"豁免）：
 * - {@link ClassTemplateService#getByCode} + {@link ClassTemplateService#inheritedView} 取继承视图
 * - {@link PropertyTemplateService} 取属性模板详情
 * <p>
 * 不走 HTTP，不写治理域表，只读调用；强类型 VO/Entity，无 Map 字段名脆弱转换。
 *
 * @author pig
 * @date 2026-07-28
 */
@Slf4j
@AllArgsConstructor
@Service
public class ClassInstantiationService {

	private final ClassTemplateService classTemplateService;
	private final PropertyTemplateService propertyTemplateService;
	private final ModelClassMapper modelClassMapper;
	private final ModelDatatypePropertyMapper datatypePropertyMapper;
	private final ModelObjectPropertyMapper objectPropertyMapper;

	/**
	 * 从分类模板实例化属性到指定类（AC-11.2~11.5）
	 *
	 * @param cls 已保存的类实体（含 id/projectId/localName/templateCode）
	 * @return R.ok() 成功 / R.failed() 失败
	 */
	@Transactional(rollbackFor = Exception.class)
	public R instantiate(ModelClass cls) {
		// 1. 取分类模板实体（按 templateCode），不存在则失败
		ClassTemplate template = classTemplateService.getByCode(cls.getTemplateCode());
		if (template == null) {
			return R.failed("分类模板不存在: " + cls.getTemplateCode());
		}
		// 2. 取继承视图（InheritedViewService 缓存，强类型）
		InheritedViewVO inheritedView = classTemplateService.inheritedView(template.getId());
		if (inheritedView == null) {
			return R.failed("获取分类模板继承视图失败");
		}

		// 3. 从继承视图复制类级溯源字段（AC-11.5）
		cls.setClassificationCode(inheritedView.getClassificationCode());
		// 外观从继承视图复制（如类自身未设）
		if (cls.getIcon() == null) {
			cls.setIcon(inheritedView.getIcon());
		}
		if (cls.getColor() == null) {
			cls.setColor(inheritedView.getColor());
		}
		modelClassMapper.updateById(cls);

		// 4. 取属性清单（已合并父链，子覆盖父，最终生效版本）
		List<InheritedPropertyVO> properties = inheritedView.getProperties();
		if (CollUtil.isEmpty(properties)) {
			return R.ok("模板无属性，已创建空白类");
		}

		// 5. 批量取属性模板详情，按 templateCode 建索引（强类型 PropertyTemplate）
		Map<String, PropertyTemplate> templateMap = fetchPropertyTemplates();

		// 6. 遍历属性清单，按 kind 分流创建
		int sortOrder = 0;
		int created = 0;
		for (InheritedPropertyVO prop : properties) {
			String propertyTemplateCode = prop.getPropertyTemplateCode();
			sortOrder++;

			PropertyTemplate propTpl = templateMap.get(propertyTemplateCode);
			if (propTpl == null) {
				log.warn("属性模板不存在或已弃用: {}，跳过", propertyTemplateCode);
				continue;
			}
			String kind = propTpl.getKind();

			if ("datatype".equals(kind)) {
				// 创建数据属性（复制 type/unitRef/enumValues/isIdentifier，AC-11.4）
				ModelDatatypeProperty dtProp = new ModelDatatypeProperty();
				dtProp.setProjectId(cls.getProjectId());
				dtProp.setClassId(cls.getId());
				dtProp.setLocalName(propertyTemplateCode);
				dtProp.setPropertyIri(cls.getLocalName() + "_" + propertyTemplateCode);
				dtProp.setLabel(propTpl.getLabel());
				dtProp.setTemplateCode(propertyTemplateCode);
				dtProp.setXsdType(mapXsdType(propTpl.getType()));
				dtProp.setUnitRef(propTpl.getUnitRef());
				dtProp.setEnumValues(propTpl.getEnumValues());
				dtProp.setIsIdentifier(propTpl.getIsIdentifier());
				dtProp.setMinCardinality(0);
				dtProp.setMaxCardinality(-1);
				dtProp.setSortOrder(sortOrder);
				datatypePropertyMapper.insert(dtProp);
				created++;
			}
			else if ("object".equals(kind)) {
				// 创建对象属性（range 留空，DD9 手动补全，AC-11.4）
				ModelObjectProperty objProp = new ModelObjectProperty();
				objProp.setProjectId(cls.getProjectId());
				objProp.setDomainClassId(cls.getId());
				objProp.setRangeClassId(null); // 对象属性 range 需手动选定（模板不含 range，PRD FR-13.2）
				objProp.setLocalName(propertyTemplateCode);
				objProp.setPropertyIri(cls.getLocalName() + "_" + propertyTemplateCode);
				objProp.setLabel(propTpl.getLabel());
				objProp.setTemplateCode(propertyTemplateCode);
				objProp.setMinCardinality(0);
				objProp.setMaxCardinality(-1);
				objProp.setSortOrder(sortOrder);
				objectPropertyMapper.insert(objProp);
				created++;
			}
		}
		return R.ok("实例化 " + created + " 个属性");
	}

	/**
	 * 批量拉取未弃用的属性模板，按 templateCode 建索引（强类型）
	 */
	private Map<String, PropertyTemplate> fetchPropertyTemplates() {
		List<PropertyTemplate> list = propertyTemplateService.list(Wrappers.<PropertyTemplate>lambdaQuery()
			.eq(PropertyTemplate::getDeprecated, "0"));
		Map<String, PropertyTemplate> map = new HashMap<>();
		for (PropertyTemplate item : list) {
			map.put(item.getTemplateCode(), item);
		}
		return map;
	}

	/**
	 * 属性模板 type -> XSD 类型映射
	 */
	private String mapXsdType(String templateType) {
		if (templateType == null) {
			return "xsd:string";
		}
		return switch (templateType) {
			case "string" -> "xsd:string";
			case "integer" -> "xsd:integer";
			case "decimal" -> "xsd:decimal";
			case "boolean" -> "xsd:boolean";
			case "datetime" -> "xsd:dateTime";
			default -> "xsd:string";
		};
	}
}
```

> **核心设计点**（评审修订，方案 B）：
> - **同模块只读 Service 注入**：注入 `ClassTemplateService`/`PropertyTemplateService`（治理域 Service，同 pig-ontology-biz 模块同 JVM），符合 PRD R-23 "同模块内只读查询"豁免。**不走 HTTP，无鉴权 token 问题（P-1），无序列化开销，无 localhost 自调用线程占用**。
> - **强类型调用**：用 `InheritedViewVO`/`InheritedPropertyVO`/`PropertyTemplate` 强类型 getter，替代原方案的 `Map<String,Object>` 字符串 key 转换（消除字段名拼写错误风险）。
> - **不写治理域表**：仅调用只读方法（getByCode/inheritedView/list），不调用任何写方法，满足 R-23 "不写治理域表"。
> - 继承视图 `properties` 清单中的每条已是**最终生效版本**（InheritedViewService.mergeRefs 已用 LinkedHashMap 保证子覆盖父）。
> - 对象属性 range 留空（属性模板不含 range，PRD FR-13.2），DD9 手动补全。
> - `mapXsdType` 将属性模板的 type（string/integer/decimal/boolean/datetime）映射为标准 XSD 类型。

> **无需 ModelingConfig**（评审修订 P-4）：方案 B 不使用 RestTemplate，pig-common-core 的全局 RestTemplate Bean 也无需关注。删除原计划的 `ModelingConfig.java`。

### 4.7 Mapper + Service 接口

```java
// ModelClassMapper.java
package com.pig4cloud.pig.ontology.modeling.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ModelClassMapper extends MPJBaseMapper<ModelClass> {
}
```

```java
// ModelDatatypePropertyMapper.java（表前移，DD9 复用）
package com.pig4cloud.pig.ontology.modeling.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ModelDatatypePropertyMapper extends MPJBaseMapper<ModelDatatypeProperty> {
}
```

```java
// ModelObjectPropertyMapper.java（表前移，DD9 复用）
package com.pig4cloud.pig.ontology.modeling.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ModelObjectPropertyMapper extends MPJBaseMapper<ModelObjectProperty> {
}
```

```java
// ModelClassService.java
package com.pig4cloud.pig.ontology.modeling.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.vo.ModelClassDetailVO;

public interface ModelClassService extends IService<ModelClass> {

	IPage<ModelClass> page(Page page, ModelClass cls);

	ModelClassDetailVO getDetail(Long id);

	R saveClass(ModelClass cls);

	R updateClass(ModelClass cls);

	R removeClass(Long id);

	/** 对已有类追加模板实例化（场景三，PRD 10.2 POST /{id}/instantiate） */
	R instantiateFromClass(Long id, ClassInstantiateDTO dto);
}
```

### 4.8 VO

```java
// ModelClassDetailVO.java
package com.pig4cloud.pig.ontology.modeling.vo;

import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "类详情视图（含属性列表+父类子类+溯源）")
public class ModelClassDetailVO {

	@Schema(description = "主键")
	private Long id;

	@Schema(description = "所属项目 ID")
	private Long projectId;

	@Schema(description = "类 IRI")
	private String classIri;

	@Schema(description = "本地名")
	private String localName;

	@Schema(description = "rdfs:label")
	private String label;

	@Schema(description = "中文标签")
	private String labelCn;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "溯源：分类模板 template_code")
	private String templateCode;

	@Schema(description = "溯源：分类编码")
	private String classificationCode;

	@Schema(description = "图标")
	private String icon;

	@Schema(description = "颜色")
	private String color;

	@Schema(description = "数据属性列表")
	private List<ModelDatatypeProperty> datatypeProperties;

	@Schema(description = "对象属性列表（作为 domain）")
	private List<ModelObjectProperty> objectProperties;

	@Schema(description = "父类 IRI 列表（DD9 建表后填充）")
	private List<String> parentClassIris;

	@Schema(description = "子类 IRI 列表（DD9 建表后填充）")
	private List<String> childClassIris;
}
```

---

## 五、前端设计

### 5.1 API - class.ts

```typescript
import request from '/@/utils/request';

// ---------- 本体类实体 ----------

export function pageList(query?: any) {
	return request({
		url: '/admin/ont/model/class/page',
		method: 'get',
		params: query,
	});
}

export function getObj(id: string) {
	return request({
		url: '/admin/ont/model/class/' + id,
		method: 'get',
	});
}

export function addObj(obj: any) {
	return request({
		url: '/admin/ont/model/class',
		method: 'post',
		data: obj,
	});
}

export function putObj(obj: any) {
	return request({
		url: '/admin/ont/model/class/' + obj.id,
		method: 'put',
		data: obj,
	});
}

export function delObj(id: string) {
	return request({
		url: '/admin/ont/model/class/' + id,
		method: 'delete',
	});
}

// ---------- 供给接口（消费治理域，选模板用，前端预览继承属性） ----------

export function supplyClassTemplateTree(treeRoot: string) {
	return request({
		url: '/admin/ont/supply/v1/class-template/tree',
		method: 'get',
		params: { treeRoot, includeDeprecated: false },
	});
}

export function supplyInherited(templateCode: string) {
	return request({
		url: '/admin/ont/supply/v1/class-template/' + templateCode + '/inherited',
		method: 'get',
	});
}

// ---------- 模板实例化（对已有类追加属性，场景三） ----------

export function instantiateObj(id: string, templateCode: string) {
	return request({
		url: '/admin/ont/model/class/' + id + '/instantiate',
		method: 'post',
		data: { templateCode },
	});
}
```

### 5.2 index.vue - 类列表页

```
┌──────────────┬─────────────────────────────────────────────────┐
│ 项目列表      │  [类名称: ______] [溯源: 全部▼] [查询] [重置]  │
│              │                                  [+新增类]      │
│ ▼ 消防设备    │ ┌─────────────────────────────────────────────┐ │
│   fire-equip │ │# │ IRI              │ 标签    │ 分类编码 │属性│操作│
│ ▼ 暖通本体    │ │1 │ .../FireSprayPump│ 喷淋泵  │30-01-01 │ 5  │详删│
│   hvac       │ │2 │ .../FirePumpRoom │ 泵房    │         │ 0  │详删│
│              │ │3 │ .../Pump         │ 泵      │30-01    │ 4  │详删│
│              │ └─────────────────────────────────────────────┘ │
│              │                              < 1 2 > 10/页      │
└──────────────┴─────────────────────────────────────────────────┘
```

**结构要点**：
- splitpanes 左树右表（对齐治理域 class-template 页面范式）。
- 左栏：项目列表树（来自 DD7 的 `/admin/ont/model/project/page`），`@node-click` 切换 projectId 过滤。
- 右栏：搜索栏（类名称 + 溯源过滤）+ 工具栏（[+新增类] `v-auth="'ont_class_model_manage'"`）+ el-table + pagination。
- el-table 列：序号 / classIri（show-overflow-tooltip）/ label / classificationCode（有值显示 el-tag）/ 属性数（datatypeProperties.length + objectProperties.length）/ 操作（详情/删除）。
- 详情走弹窗或抽屉（展示 ModelClassDetailVO：基本信息 + 数据属性表 + 对象属性表 + 溯源标记）。

**关键逻辑**：
```typescript
import { BasicTableProps, useTable } from '/@/hooks/table';
import { pageList, delObj } from '/@/api/ontology-model/class';

const state: BasicTableProps = reactive<BasicTableProps>({
	isPage: true,
	queryForm: {
		projectId: '',
		label: '',
		templateCode: '',
	},
	pageList: pageList,
});

const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state);

// 左栏项目树点击 -> 切换 projectId
const handleProjectClick = (project: any) => {
	state.queryForm.projectId = project.id;
	getDataList();
};

// 新增类
const formDialogRef = ref();
const onOpenDialog = () => {
	formDialogRef.value.openDialog(state.queryForm.projectId);
};

// 删除
const handleDelete = async (row: any) => {
	try {
		await useMessageBox().confirm('确认删除类「' + row.label + '」？');
		await delObj(row.id);
		useMessage().success('删除成功');
		getDataList();
	} catch (err: any) {
		if (err !== 'cancel') useMessage().error(err.msg);
	}
};
```

### 5.3 form.vue - 类表单弹窗

**字段表格**：

| 字段 | 组件 | 校验 | 说明 |
|---|---|---|---|
| projectId | 隐藏 | required | 从父组件传入（左栏选中项目） |
| localName | el-input | required, 编辑时 disabled | IRI 本地名（如 FireSprayPump） |
| classIri | el-input readonly | - | 自动拼接 = namespaceBase + localName，不可手动编辑 |
| label | el-input | required | rdfs:label |
| labelCn | el-input | - | 中文标签 |
| description | el-input textarea | - | 描述 |
| templateCode | el-tree-select | - | 选择分类模板（可选，选后触发实例化） |
| icon | el-input | - | 图标 |
| color | el-color-picker | - | 颜色 |

**模板选择交互**（核心）：
- `el-tree-select` 展示分类模板树（数据来自 `supplyClassTemplateTree('equipment')`），节点显示"编码 + label"。
- 选中模板后调 `supplyInherited(templateCode)` 获取继承视图，下方 `<inherited-preview>` 组件展示属性清单（带 source 三态标记）。
- 用户确认后提交，后端 saveClass 内部调 ClassInstantiationService 批量实例化。
- 不选模板 = 空白类创建（AC-11.3）。

**classIri 自动拼接**：
```typescript
// 监听 localName 变化，自动拼 IRI
watch(() => form.localName, (val) => {
	if (val && form.namespaceBase) {
		form.classIri = form.namespaceBase + val;
	}
});

// 项目命名空间基址在 openDialog 时拉取
const openDialog = async (projectId: string, id?: string) => {
	visible.value = true;
	form.id = '';
	// 拉项目详情获取 namespaceBase
	const project = await getProject(projectId);
	form.namespaceBase = project.namespaceBase;
	// ...
};
```

### 5.4 inherited-preview.vue - 继承属性预览面板

**结构**：
- 展示选中分类模板的继承视图（InheritedViewVO）。
- el-table 渲染 `view.properties` 清单：propertyTemplateCode / refType（property/relationship tag）/ source（node/inherited/overridden 三态 tag，对齐治理域 inherited-panel 范式）/ sourceClassTemplateCode。
- 底部展示父链 `view.parentChain.join(' ← ')`。
- 标题提示"以下属性将随类一起创建，每条挂 templateRef 溯源"。

> 组件对齐治理域 `inherited-panel.vue` 范式，但用途不同：治理域是只读展示，DD8 是"实例化预览"（创建前确认）。

### 5.5 composables.ts

```typescript
import { useI18n } from 'vue-i18n';

export function useModelClassOptions() {
	const { t } = useI18n();

	/** refType 选项 */
	const refTypeOptions = computed(() => [
		{ value: 'property', label: t('modelClass.refTypeProperty'), tagType: 'primary' },
		{ value: 'relationship', label: t('modelClass.refTypeRelationship'), tagType: 'success' },
	]);

	/** source 三态选项（继承视图标记） */
	const sourceOptions = computed(() => [
		{ value: 'node', label: t('modelClass.sourceNode'), tagType: 'primary' },
		{ value: 'inherited', label: t('modelClass.sourceInherited'), tagType: 'info' },
		{ value: 'overridden', label: t('modelClass.sourceOverridden'), tagType: 'warning' },
	]);

	const refTypeLabel = (value: string) => refTypeOptions.value.find((o) => o.value === value)?.label ?? value;
	const refTypeTagType = (value: string) => refTypeOptions.value.find((o) => o.value === value)?.tagType ?? 'info';
	const sourceLabel = (value: string) => sourceOptions.value.find((o) => o.value === value)?.label ?? value;
	const sourceTagType = (value: string) => sourceOptions.value.find((o) => o.value === value)?.tagType ?? 'info';

	return { refTypeOptions, sourceOptions, refTypeLabel, refTypeTagType, sourceLabel, sourceTagType };
}
```

### 5.6 i18n - zh-cn.ts

```typescript
export default {
	modelClass: {
		index: '#',
		classIri: '类 IRI',
		localName: '本地名',
		label: '标签',
		labelCn: '中文名',
		description: '描述',
		templateCode: '来源模板',
		classificationCode: '分类编码',
		icon: '图标',
		color: '颜色',
		propertyCount: '属性数',
		// refType 选项
		refTypeProperty: '属性',
		refTypeRelationship: '关系',
		// source 三态
		sourceNode: '本节点',
		sourceInherited: '继承',
		sourceOverridden: '覆盖',
		// 操作
		add: '新增类',
		edit: '编辑',
		delete: '删除',
		detail: '详情',
		// 模板选择
		selectTemplate: '选择分类模板（可选）',
		selectTemplateTip: '选择后自动实例化属性，不选则创建空白类',
		inheritedPreview: '继承属性预览',
		inheritedPreviewTip: '以下属性将随类一起创建，每条挂 templateRef 溯源',
		parentChain: '父链',
		// inputXxxTip
		inputLocalNameTip: '请输入本地名（如 FireSprayPump）',
		inputLabelTip: '请输入标签',
		inputLabelCnTip: '请输入中文名',
		inputDescriptionTip: '请输入描述',
		// 确认提示
		deleteTip: '确认删除该类？',
		// 详情
		datatypeProperties: '数据属性',
		objectProperties: '对象属性',
		parentClasses: '父类',
		childClasses: '子类',
		hasTemplate: '模板派生',
		noTemplate: '手建',
	},
};
```

### 5.7 i18n - en.ts

```typescript
export default {
	modelClass: {
		index: '#',
		classIri: 'Class IRI',
		localName: 'Local Name',
		label: 'Label',
		labelCn: 'Chinese Name',
		description: 'Description',
		templateCode: 'Source Template',
		classificationCode: 'Classification Code',
		icon: 'Icon',
		color: 'Color',
		propertyCount: 'Properties',
		refTypeProperty: 'Property',
		refTypeRelationship: 'Relationship',
		sourceNode: 'Node',
		sourceInherited: 'Inherited',
		sourceOverridden: 'Overridden',
		add: 'New Class',
		edit: 'Edit',
		delete: 'Delete',
		detail: 'Detail',
		selectTemplate: 'Select Class Template (Optional)',
		selectTemplateTip: 'Select to auto-instantiate properties, leave empty for blank class',
		inheritedPreview: 'Inherited Properties Preview',
		inheritedPreviewTip: 'These properties will be created with the class, each with templateRef',
		parentChain: 'Parent Chain',
		inputLocalNameTip: 'Enter local name (e.g. FireSprayPump)',
		inputLabelTip: 'Enter label',
		inputLabelCnTip: 'Enter Chinese name',
		inputDescriptionTip: 'Enter description',
		deleteTip: 'Confirm to delete this class?',
		datatypeProperties: 'Datatype Properties',
		objectProperties: 'Object Properties',
		parentClasses: 'Parent Classes',
		childClasses: 'Child Classes',
		hasTemplate: 'Template',
		noTemplate: 'Manual',
	},
};
```

---

## 六、横切设计

### 6.1 校验

| 校验点 | 实现位置 | 说明 |
|---|---|---|
| classIri 同项目唯一 | ServiceImpl 预查重 + DB `uk_ont_model_class_iri(project_id,class_iri)` 兜底 | 两层校验，覆盖软删后复用 |
| localName 必填 | Entity `@NotBlank` | IRI 本地名 |
| templateCode 存在性 | ClassInstantiationService 调 Supply API 验证 | 不存在返回 R.failed |
| 删除校验-数据属性 | ServiceImpl.removeClass 查 ont_model_datatype_property | 有属性拒绝删除 |
| 删除校验-对象属性 | ServiceImpl.removeClass 查 ont_model_object_property（domain+range） | 被引用拒绝删除 |
| 删除校验-subClassOf | 注释预留，DD9 建表后启用 | 被继承拒绝删除 |
| 不可改字段 | ServiceImpl.updateClass 锁定 | classIri/projectId/localName/templateCode/classificationCode |

### 6.2 异常

- Supply API 调用失败（网络/超时）：`R.failed("获取分类模板继承视图失败")`，`@Transactional` 回滚已保存的类实体。
- `DuplicateKeyException` 捕获转友好提示（IRI 唯一约束兜底）。
- 属性模板不存在：log.warn + 跳过该属性（不中断实例化）。

### 6.3 审计

- MybatisPlusMetaObjectHandler 自动填充审计字段（同治理域）。
- `@TableField(fill = FieldFill.INSERT/UPDATE)` + `@TableLogic` 标注（同 DD7）。

### 6.4 治理域消费边界（R-23，方案 B）

- **同模块只读 Service 注入**：ClassInstantiationService 注入治理域 `ClassTemplateService`/`PropertyTemplateService`（同 pig-ontology-biz 模块同 JVM），只调用只读方法（`getByCode`/`inheritedView`/`list`）。符合 PRD R-23 "建模域不引用治理域 Mapper/Service（只调 Supply/Sync API **或同模块内只读查询**）"中的"同模块内只读查询"豁免。
- **不写治理域表**：不调用治理域 Service 的任何写方法（save/update/remove/deprecate 等），不向 ont_class_template / ont_property_template 等表写入。
- **强类型调用**：直接用治理域 VO/Entity（InheritedViewVO/PropertyTemplate），无 HTTP 序列化与字段名 Map 转换。
- **无需鉴权透传**：同进程方法调用，无 HTTP 鉴权问题（消除原方案 P-1 的 token 透传难题）。

### 6.5 事务

- `saveClass` + `instantiate` 在同一 `@Transactional` 内：类实体保存 + 属性批量创建原子性，实例化失败回滚类实体。
- `instantiateFromClass` + `instantiate` 同一事务：场景三追加实例化原子性。
- `removeClass` 事务内校验 + 软删。

### 6.6 双形态验证

| 形态 | 治理域消费 | 预期 |
|---|---|---|
| 单体 pig-boot | 同 JVM 方法调用 ClassTemplateService/PropertyTemplateService | 继承视图数据正确返回 |
| 微服务 | 同 JVM 方法调用（pig-ontology-biz 单模块，治理域与建模域同进程） | 继承视图数据正确返回 |

> **双形态一致性**（评审修订）：方案 B 下治理域与建模域同在 pig-ontology-biz 单模块，单体/微服务均为同 JVM 方法调用，**无 URL 配置差异，无需 ont.supply.base-url 配置项**。这是方案 B 相对原 HTTP 方案的额外优势（原方案微服务需改 localhost:9999 为网关地址）。

---

## 七、测试要点

| 类别 | 要点 |
|---|---|
| 编译 | `mvn -pl pig-ontology/pig-ontology-biz compile` 通过；modeling 包新增类无编译错误 |
| Flyway（V13） | pig-boot 启动后 `SELECT count(*) FROM ont_model_class` = 0；`SELECT count(*) FROM ont_model_datatype_property` = 0；`SELECT count(*) FROM ont_model_object_property` = 0；`SELECT menu_id FROM sys_menu WHERE menu_id BETWEEN 11200 AND 11204` = 5 条 |
| 覆盖（AC-11.1） | `POST /admin/ont/model/class` 传 localName + projectId（不传 classIri），后端拼 IRI = namespaceBase + localName，返回成功且 classIri 正确；重复 IRI 返回 `R.failed("类 IRI 'xxx' 在项目内已存在")`；传不一致的 classIri 返回 `R.failed("类 IRI 与命名空间基址/本地名不一致...")` |
| 覆盖（场景三/instantiate） | 先建空白类（id=A），再 `POST /A/instantiate` 传 templateCode='pump'，返回成功，查 ont_model_datatype_property class_id=A 新增 4 条；对已基于模板的类重复 instantiate 同模板返回 `R.failed("该类已基于模板 ... 创建，不可重复实例化")` |
| 覆盖（AC-11.2） | `POST` 传 templateCode='spray-pump'，后端调 Supply API inherited 端点，返回的类含 classificationCode='30-01-01'，属性表新增 5 条记录（泵 4 + 喷淋泵 1） |
| 覆盖（AC-11.3） | `POST` 不传 templateCode，创建成功，查 ont_model_datatype_property 该 class_id 下 0 条 |
| 覆盖（AC-11.4） | 实例化后查 `SELECT template_code FROM ont_model_datatype_property WHERE class_id=xx`，每条非空，值等于源属性模板 templateCode |
| 覆盖（AC-11.5） | 实例化后查 `SELECT classification_code FROM ont_model_class WHERE id=xx`，值等于源分类模板 classification_code（如 30-01-01） |
| 覆盖（AC-11.6） | `PUT /{id}` 编辑 label 成功；编辑 classIri 被锁定（返回值 classIri 不变）；`DELETE /{id}` 有属性时返回 `R.failed("类下存在 N 个数据属性，无法删除")` |
| 覆盖（AC-11.7） | `GET /page?projectId=xx&label=泵` 返回分页，label 模糊匹配；`GET /page?templateCode=pump` 只返回模板派生的类 |
| 覆盖（AC-11.8） | `GET /{id}` 返回 DetailVO，含 datatypeProperties + objectProperties 列表 + parentClassIris（v1 空列表） |
| Supply 消费 | 模板不存在时 `POST` 传 templateCode='xyz'，返回 `R.failed("分类模板不存在: xyz")`；类实体已回滚（查不到）。方案 B 同模块 Service 调用，无 HTTP 鉴权问题 |
| 对象属性 range 留空 | 实例化对象属性模板（如 contains）后，查 `SELECT range_class_id FROM ont_model_object_property`，值为 NULL |
| XSD 类型映射 | 实例化 price 模板（type=decimal）后，查 xsd_type='xsd:decimal'；name 模板 -> xsd:string |
| 删除校验 | 类有对象属性引用（作为 range）时，删除返回 `R.failed("类被 N 个对象属性引用，无法删除")` |
| 权限 | 无 `ont_class_model_view` 调 page 返回 403；无 `ont_class_model_manage` 调 POST/PUT/DELETE 返回 403 |

---

## 八、风险与缓解

| 风险 | 缓解 |
|---|---|
| 治理域 Service 调用失败导致实例化中断 | `@Transactional` 回滚已保存的类实体；返回友好错误；InheritedViewService 已有 Caffeine 缓存，稳定性高 |
| 边界模糊（建模域注入治理域 Service） | 方案 B 仅注入只读方法（getByCode/inheritedView/list），不调写方法；符合 R-23 "同模块内只读查询"豁免；强类型调用降低误用风险 |
| 属性表前移影响 DD9 设计计划 | 设计计划 V14 相应调整（仅建 ont_model_subclassof + 11300 菜单）；DD9 不再建属性表，只实现手动 CRUD 业务逻辑 |
| 对象属性 range 为空（模板不含 range） | 实例化时 range 留 NULL，DD9 前端属性编辑页提示补全 range；画布 DD11 拖拽建关系时直接补 range |
| 属性模板已弃用（deprecated=1） | fetchPropertyTemplates 用 `deprecated='0'` 过滤；实例化时若模板已弃用查不到则跳过并 warn |

---

## 九、交付物清单

| 类型 | 文件 | 说明 |
|---|---|---|
| 新建 | `modeling/entity/ModelClass.java` | 类实体 Entity |
| 新建 | `modeling/entity/ModelDatatypeProperty.java` | 数据属性 Entity（表前移，DD9 复用） |
| 新建 | `modeling/entity/ModelObjectProperty.java` | 对象属性 Entity（表前移，DD9 复用） |
| 新建 | `modeling/mapper/ModelClassMapper.java` | 类 Mapper |
| 新建 | `modeling/mapper/ModelDatatypePropertyMapper.java` | 数据属性 Mapper（DD9 复用） |
| 新建 | `modeling/mapper/ModelObjectPropertyMapper.java` | 对象属性 Mapper（DD9 复用） |
| 新建 | `modeling/service/ModelClassService.java` | 类 Service 接口 |
| 新建 | `modeling/service/ClassInstantiationService.java` | 模板实例化核心算法 Service |
| 新建 | `modeling/service/impl/ModelClassServiceImpl.java` | 类 Service 实现 |
| 新建 | `modeling/controller/ModelClassController.java` | 类 Controller |
| 新建 | `modeling/dto/ClassInstantiateDTO.java` | 实例化请求 DTO |
| 新建 | `modeling/vo/ModelClassDetailVO.java` | 类详情 VO |
| 新建 | `V13__ont_model_class_seed.sql` | 建三表 + 菜单种子 |
| 新建 | `api/ontology-model/class.ts` | 前端 API |
| 新建 | `views/admin/ontology-model/class/index.vue` | 类列表页 |
| 新建 | `views/admin/ontology-model/class/form.vue` | 类表单弹窗 |
| 新建 | `views/admin/ontology-model/class/inherited-preview.vue` | 继承属性预览面板 |
| 新建 | `views/admin/ontology-model/class/composables.ts` | 选项/工具函数 |
| 新建 | `views/admin/ontology-model/class/i18n/zh-cn.ts` | 中文词条 |
| 新建 | `views/admin/ontology-model/class/i18n/en.ts` | 英文词条 |
| 修改 | DD7 `ModelProjectServiceImpl.removeProject` | 取消注释关联类实体校验（V13 建表后生效） |
| 修改 | 《本体建模功能详细设计计划.md》 | V14 调整为仅建 ont_model_subclassof + 11300 菜单 |

---

## 十、与 PRD 边界的对齐确认（防混淆备忘）

| 维度 | PRD 约定 | 本 DD 实现 | 对齐 |
|---|---|---|---|
| 类实体有独立 IRI | class_iri = namespace_base + localName（PRD 5.2） | Entity classIri 字段 + ServiceImpl 校验 | ✓ |
| 模板实例化复制字段值 | 复制字段并挂 templateRef，非引用模板（PRD 5.2） | ClassInstantiationService 复制 type/unitRef 等 + 挂 templateCode | ✓ |
| 分类模板不进 RDF | 分类模板是原型，类是独立资源（PRD 5.1/5.2） | ModelClass 与 ClassTemplate 分表，class_iri 独立 | ✓ |
| 类层级权威源在建模域 | 建模侧建立 subClassOf 是权威源（PRD 5.2） | 本 DD 不建 subClassOf（DD9），父类子类列表预留空 | ✓（预留） |
| 治理域只读消费 | 不修改治理域表/接口/代码（PRD NFR-C3/R-23） | 方案 B：同模块只读 Service 注入（getByCode/inheritedView/list），不写治理域表，符合 R-23 "同模块内只读查询"豁免 | ✓ |
| 属性表归属 | PRD 9.4/9.5 原划 DD9/V14 | 前移到 V13（因实例化需同时建类+属性），DD9 不建表只做业务逻辑 | ✓（调整，已说明） |
| 菜单 ID | 11200 段（PRD 13.1） | 11200 菜单 + 11201~11204 按钮 | ✓ |
| 权限标识 | ont_class_model_view / ont_class_model_manage（PRD 13.2） | Controller @HasPermission 对齐 | ✓ |
| Flyway | V13（PRD 9 / 设计计划 3.2） | V13__ont_model_class_seed.sql | ✓ |
| 接口路径 | /admin/ont/model/class（PRD 10.2） | @RequestMapping("/ont/model/class") | ✓ |
| 对象属性 range | 模板不含 range，建模侧手动选定（PRD FR-13.2） | 实例化时 range 留 NULL，DD9 补全 | ✓ |

---

*本 DD 是建模域核心价值点--首次消费治理域 Supply API 实现模板实例化。评审通过后进入实现，实现完成后更新设计计划 DD8 状态为"已完成"，并同步调整设计计划中 V14 的范围（仅建 ont_model_subclassof + 11300 菜单）。*
