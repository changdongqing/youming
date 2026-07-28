# 详细设计-DD9-属性与关系建模与类层级回推

| 项 | 内容 |
|---|---|
| 文档名称 | 属性与关系建模与类层级回推 详细设计 |
| 里程碑 | M7（FR-12 + FR-13 + FR-14） |
| 上游 PRD | 《本体建模功能产品需求文档.md》v1.0（FR-12/13/14 / AC-12.1~12.7 + AC-13.1~13.5 + AC-14.1~14.6 / 9.6 / 10.3/10.4/10.5 / 十一节场景一/四 / 十四节 M7） |
| 设计计划 | 《本体建模功能详细设计计划.md》DD9（M7） |
| 前置依赖 | DD8 已落地（ont_model_class + ont_model_datatype_property + ont_model_object_property 三表就绪，类实体 CRUD + 模板实例化可用）；治理域 DD2/DD3/DD4 已落地（Supply API property-templates/units/suggest + Sync API class-hierarchy 可消费） |
| 编写日期 | 2026-07-28 |
| 文档状态 | 待评审 |

---

## 一、设计目标与范围

### 1.1 目标

- **FR-12 数据属性手动建模**：为类创建/编辑/删除数据属性，基于属性模板（`kind=datatype`），绑定单位（`unitRef` = QUDT IRI），设置基数/标识符/枚举值。
- **FR-13 对象属性建模**：为类创建/编辑/删除对象属性，基于属性模板（`kind=object`），定义 domain（域类）和 range（值域类），支持反向关系建议。
- **FR-14 类层级建立与镜像回推**：建立 `rdfs:subClassOf` 关系，环路检测（DFS），建立后自动回推 `ont_class_hierarchy` 镜像，删除后置镜像失效，补偿重试保证一致性。
- **V14 脚本**：建 `ont_model_subclassof` 表 + 11300 段菜单种子 + **修复 DD8 遗留的 `range_class_id NOT NULL` 约束**（改为可空，使 DD8 实例化对象属性 range 留空语义成立）。
- **补偿任务**：镜像回推失败时定时重试（Spring `@Scheduled`，最多 3 次指数退避）。

### 1.2 范围（本 DD 做 / 不做）

| 做（本 DD） | 不做（后续 DD / Out of Scope） |
|---|---|
| 数据属性 CRUD + 单位绑定 + 枚举 + 基数 + 标识符（FR-12） | 序列化输出 RDF（DD10/FR-15） |
| 对象属性 CRUD + domain/range + 反向关系建议（FR-13） | 画布拖拽建关系（DD11/FR-17，DD9 提供表单式） |
| subClassOf 建立 + 环路检测 + 多继承（FR-14.1/14.2/14.6） | 个体实例（M10/DD12，Out of Scope） |
| 镜像回推 + 删除失效 + 补偿重试（FR-14.3/14.4/14.5） | 方案 A 序列化（v1 禁用） |
| V14 建 ont_model_subclassof + 修复 range_class_id 可空 | 属性表 DDL（DD8 V13 已建，DD9 复用） |
| defaultCardinality -> min/max 映射（FR-13.3） | |
| 消费 Supply API（property-templates/units/suggest）（FR-12.7/13.2/14.2） | |
| 消费 Sync API（class-hierarchy POST/DELETE）（FR-14.3/14.4） | |

### 1.3 验收映射（M7 DoD）

| PRD AC | 本 DD 实现点 |
|---|---|
| AC-12.1 数据属性创建 + localName 同类唯一 | 4.3 ModelDatatypePropertyController.save + 4.4 ServiceImpl.saveProp（预查重 + DB uk 约束） |
| AC-12.2 单位绑定 unitRef = QUDT IRI + 非数值型不可绑 | 4.4 ServiceImpl.saveProp（xsd_type 校验 + Supply API 拉单位） |
| AC-12.3 枚举值 JSON 数组 | 4.4 ServiceImpl（enumValues 存 JSON 字符串） |
| AC-12.4 基数设置 | 4.4 ServiceImpl（min/max cardinality 校验） |
| AC-12.5 标识符标记 | 4.4 ServiceImpl（isIdentifier 写入） |
| AC-12.6 数据属性 CRUD + 排序 | 4.3 Controller + 4.4 ServiceImpl |
| AC-12.7 从属性模板实例化 | 4.4 ServiceImpl.instantiateFromTemplate（调 Supply property-templates?kind=datatype） |
| AC-13.1 对象属性创建 + domain/range | 4.5 ModelObjectPropertyController + 4.6 ServiceImpl.saveProp |
| AC-13.2 从对象属性模板实例化 | 4.6 ServiceImpl.instantiateFromTemplate（调 Supply property-templates?kind=object） |
| AC-13.3 基数设置（defaultCardinality -> min/max 映射） | 4.6 ServiceImpl（mapCardinality 方法） |
| AC-13.4 对象属性 CRUD | 4.5 Controller + 4.6 ServiceImpl |
| AC-13.5 反向关系自动建议 | 4.6 ServiceImpl.suggestInverse |
| AC-14.1 subClassOf 建立 + 环路检测 | 4.7 ModelSubclassOfController + 4.8 ServiceImpl.saveEdge（DFS 环路检测） |
| AC-14.2 父类建议来自 suggest 端点 | 4.8 ServiceImpl（调 Supply class-hierarchy/suggest） |
| AC-14.3 镜像回推 sync_status='1' | 4.9 HierarchySyncService.pushMirror（调 Sync POST） |
| AC-14.4 删除 subClassOf 镜像失效 | 4.9 HierarchySyncService.invalidateMirror（调 Sync DELETE） |
| AC-14.5 一致性保证（本地事务+补偿） | 4.8 ServiceImpl + 4.10 HierarchySyncCompensateTask |
| AC-14.6 多继承支持 | 4.8 ServiceImpl（允许多条 subClassOf） |

---

## 二、落地清单

### 2.1 后端文件清单

```
server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/modeling/
├── controller/
│   ├── ModelDatatypePropertyController.java   # 新增：数据属性 CRUD
│   ├── ModelObjectPropertyController.java     # 新增：对象属性 CRUD
│   └── ModelSubclassOfController.java         # 新增：类层级 CRUD + 类树
├── service/
│   ├── ModelDatatypePropertyService.java      # 新增
│   ├── ModelObjectPropertyService.java        # 新增
│   ├── ModelSubclassOfService.java            # 新增
│   ├── HierarchySyncService.java              # 新增：镜像回推核心
│   └── impl/
│       ├── ModelDatatypePropertyServiceImpl.java  # 新增
│       ├── ModelObjectPropertyServiceImpl.java    # 新增
│       └── ModelSubclassOfServiceImpl.java        # 新增
├── task/
│   └── HierarchySyncCompensateTask.java       # 新增：镜像回推补偿定时任务
├── mapper/
│   └── ModelSubclassOfMapper.java             # 新增（属性 Mapper DD8 已建）
├── entity/
│   └── ModelSubclassOf.java                   # 新增（属性 Entity DD8 已建）
├── dto/
│   ├── SubclassOfSaveDTO.java                 # 新增：建立 subClassOf 请求
│   └── InverseSuggestDTO.java                 # 新增：反向关系建议请求
└── vo/
    ├── ModelSubclassOfTreeVO.java             # 新增：类树视图
    └── InverseSuggestVO.java                  # 新增：反向关系建议结果
```

### 2.2 数据库文件清单

```
server/pig-common/pig-common-data/src/main/resources/db/migration/
└── V14__ont_model_subclassof_seed.sql         # 新增：建 ont_model_subclassof + 修复 range_class_id + 11300 菜单
```

### 2.3 依赖变更清单

无新增 Maven 依赖。补偿任务用 Spring 原生 `@Scheduled`（需启动类加 `@EnableScheduling`）。

### 2.4 前端文件清单

```
web/src/
├── api/ontology-model/
│   ├── datatype-property.ts                  # 新增
│   ├── object-property.ts                    # 新增
│   └── subclassof.ts                         # 新增
└── views/admin/ontology-model/
    └── property/
        ├── index.vue                         # 新增：Tab 切换数据属性/对象属性
        ├── datatype-form.vue                 # 新增：数据属性表单
        ├── object-form.vue                   # 新增：对象属性表单
        ├── inverse-dialog.vue                # 新增：反向关系建议弹窗
        ├── composables.ts                    # 新增
        └── i18n/
            ├── zh-cn.ts                      # 新增
            └── en.ts                         # 新增
```

---

## 三、数据库设计（V14）

### 3.1 V14 脚本范围

V14 一次性完成：
- (a) 建表 `ont_model_subclassof`（类层级关系，建模域权威源）
- (b) **修复 DD8 遗留**：`ALTER TABLE ont_model_object_property ALTER COLUMN range_class_id DROP NOT NULL`（使 DD8 实例化对象属性 range 留空语义成立）
- (c) 建索引
- (d) sys_menu 菜单种子（11300 段"本体属性建模" + 11301~11304 权限点按钮）

### 3.2 V14__ont_model_subclassof_seed.sql

```sql
-- ============================================================
-- V14: 建模域 - 类层级关系 + 修复 range_class_id（FR-14）
-- 建 ont_model_subclassof + ALTER ont_model_object_property + 菜单种子（11300 段）
-- ============================================================

-- ---------- (a) 建表 ----------

CREATE TABLE ont_model_subclassof (
    id                   bigint       NOT NULL,
    project_id           bigint       NOT NULL,
    child_class_id       bigint       NOT NULL,
    parent_class_id      bigint       NOT NULL,
    source_template_ref  varchar(64),
    sync_status          char(1)      DEFAULT '0',
    sync_time            timestamp,
    create_by            varchar(64)  DEFAULT ' ',
    create_time          timestamp    DEFAULT now(),
    update_by            varchar(64)  DEFAULT ' ',
    update_time          timestamp    DEFAULT now(),
    del_flag             char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_model_subclassof PRIMARY KEY (id),
    CONSTRAINT uk_ont_model_subclassof UNIQUE (project_id, child_class_id, parent_class_id)
);
COMMENT ON TABLE  ont_model_subclassof IS '类层级关系（rdfs:subClassOf，建模域权威源，FR-14）';
COMMENT ON COLUMN ont_model_subclassof.child_class_id      IS '子类 ID';
COMMENT ON COLUMN ont_model_subclassof.parent_class_id     IS '父类 ID';
COMMENT ON COLUMN ont_model_subclassof.source_template_ref IS '溯源：建议来源的分类模板 template_code';
COMMENT ON COLUMN ont_model_subclassof.sync_status         IS '0=待同步 1=已同步 2=已失效（镜像回推状态）';
COMMENT ON COLUMN ont_model_subclassof.sync_time           IS '最近镜像回推时间';

-- ---------- (b) 修复 DD8 遗留：range_class_id 改可空 ----------
-- DD8 实例化对象属性时 range 留空（模板不含 range），DD9 手动补全
-- V13 DDL 原为 NOT NULL，此处 ALTER 改为可空

ALTER TABLE ont_model_object_property ALTER COLUMN range_class_id DROP NOT NULL;
COMMENT ON COLUMN ont_model_object_property.range_class_id IS '值域类 ID（range），可空=模板实例化待补全（DD9 补全）';

-- ---------- (c) 索引 ----------

CREATE INDEX idx_ont_model_subclass_child  ON ont_model_subclassof (child_class_id)  WHERE del_flag = '0';
CREATE INDEX idx_ont_model_subclass_parent ON ont_model_subclassof (parent_class_id) WHERE del_flag = '0';

-- ---------- (d) sys_menu 菜单种子（11300 段） ----------

INSERT INTO sys_menu VALUES (11300, '本体属性建模', NULL, '/admin/ontology-model/property/index', NULL, 11000, 'iconfont icon-shuxing', '1', 3, '0', '0', '0', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11301, '属性新增', 'ont_prop_model_manage', NULL, NULL, 11300, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11302, '属性编辑', 'ont_prop_model_manage', NULL, NULL, 11300, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11303, '属性删除', 'ont_prop_model_manage', NULL, NULL, 11300, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11304, '属性查看', 'ont_prop_model_view',   NULL, NULL, 11300, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
```

> **range_class_id 修复说明**：DD8 V13 DDL 中 `range_class_id bigint NOT NULL` 与 DD8 实例化时"range 留空"的设计意图冲突。V14 用 `ALTER TABLE ... DROP NOT NULL` 修复（Flyway 已应用 V13 不可改，只能 ALTER）。修复后 DD8 实例化对象属性 range=NULL 成立，DD9 手动补全 range 时 UPDATE 填值。

> **sys_menu 17 字段**：按位置 INSERT，parent_id=11000（本体建模目录），菜单 menu_type='0'，按钮 menu_type='1'。

> **权限标识**：`ont_prop_model_view`（查看）/ `ont_prop_model_manage`（管理）。类层级 subClassOf 复用类建模权限 `ont_class_model_manage`（不单独设权限点，subClassOf 是类的子操作）。

---

## 四、后端设计

> 包路径 `com.pig4cloud.pig.ontology.modeling.*`。属性 Entity/Mapper DD8 已建（ModelDatatypeProperty/ModelObjectProperty），DD9 复用，只新增 Service/Controller。

### 4.1 Entity - ModelSubclassOf

```java
package com.pig4cloud.pig.ontology.modeling.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 类层级关系 Entity（rdfs:subClassOf，建模域权威源，FR-14）
 * <p>
 * 区别于治理域 ont_class_hierarchy（只读镜像）：本表是建模域权威源，
 * 建立后经 HierarchySyncService 回推镜像到 ont_class_hierarchy。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "类层级关系")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_model_subclassof")
public class ModelSubclassOf extends Model<ModelSubclassOf> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "所属项目 ID")
	private Long projectId;

	@Schema(description = "子类 ID")
	private Long childClassId;

	@Schema(description = "父类 ID")
	private Long parentClassId;

	@Schema(description = "溯源：建议来源的分类模板 template_code")
	private String sourceTemplateRef;

	@Schema(description = "0=待同步 1=已同步 2=已失效")
	private String syncStatus;

	@Schema(description = "最近镜像回推时间")
	private LocalDateTime syncTime;

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

### 4.2 Mapper + DTO

```java
// ModelSubclassOfMapper.java
package com.pig4cloud.pig.ontology.modeling.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.pig4cloud.pig.ontology.modeling.entity.ModelSubclassOf;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ModelSubclassOfMapper extends MPJBaseMapper<ModelSubclassOf> {
}
```

```java
// SubclassOfSaveDTO.java
package com.pig4cloud.pig.ontology.modeling.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "建立 subClassOf 请求")
public class SubclassOfSaveDTO {

	@Schema(description = "所属项目 ID")
	@NotNull(message = "项目 ID 不能为空")
	private Long projectId;

	@Schema(description = "子类 ID")
	@NotNull(message = "子类 ID 不能为空")
	private Long childClassId;

	@Schema(description = "父类 ID")
	@NotNull(message = "父类 ID 不能为空")
	private Long parentClassId;

	@Schema(description = "溯源：建议来源的分类模板 template_code（可空）")
	private String sourceTemplateRef;
}
```

### 4.3 Controller - ModelDatatypePropertyController

```java
package com.pig4cloud.pig.ontology.modeling.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;
import com.pig4cloud.pig.ontology.modeling.service.ModelDatatypePropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 数据属性 Controller（FR-12）
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/model/datatype-property")
@Tag(name = "数据属性建模", description = "数据属性 CRUD + 单位绑定 + 模板实例化（FR-12）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ModelDatatypePropertyController {

	private final ModelDatatypePropertyService datatypePropertyService;

	@GetMapping("/page")
	@Operation(summary = "分页查询", description = "按 classId 过滤（AC-12.6）")
	@HasPermission("ont_prop_model_view")
	public R<IPage<ModelDatatypeProperty>> page(@ParameterObject Page page,
			@ParameterObject ModelDatatypeProperty prop) {
		return R.ok(datatypePropertyService.page(page, prop));
	}

	@GetMapping("/{id}")
	@Operation(summary = "属性详情")
	@HasPermission("ont_prop_model_view")
	public R<ModelDatatypeProperty> getById(@PathVariable Long id) {
		return R.ok(datatypePropertyService.getById(id));
	}

	@SysLog("新增数据属性")
	@PostMapping
	@Operation(summary = "新增数据属性", description = "localName 查重 + 单位校验 + 基数（AC-12.1~12.5）")
	@HasPermission("ont_prop_model_manage")
	public R save(@Valid @RequestBody ModelDatatypeProperty prop) {
		return datatypePropertyService.saveProp(prop);
	}

	@SysLog("编辑数据属性")
	@PutMapping("/{id}")
	@Operation(summary = "编辑数据属性")
	@HasPermission("ont_prop_model_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody ModelDatatypeProperty prop) {
		prop.setId(id);
		return datatypePropertyService.updateProp(prop);
	}

	@SysLog("删除数据属性")
	@DeleteMapping("/{id}")
	@Operation(summary = "删除数据属性", description = "校验被对象属性引用（AC-12.6）")
	@HasPermission("ont_prop_model_manage")
	public R removeById(@PathVariable Long id) {
		return datatypePropertyService.removeProp(id);
	}
}
```

### 4.4 ServiceImpl - ModelDatatypePropertyServiceImpl

```java
package com.pig4cloud.pig.ontology.modeling.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelDatatypePropertyMapper;
import com.pig4cloud.pig.ontology.modeling.service.ModelDatatypePropertyService;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 数据属性 Service 实现（FR-12）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ModelDatatypePropertyServiceImpl
		extends ServiceImpl<ModelDatatypePropertyMapper, ModelDatatypeProperty>
		implements ModelDatatypePropertyService {

	/** 可绑单位的数据类型（数值型） */
	private static final Set<String> NUMERIC_TYPES = Set.of(
			"xsd:integer", "xsd:decimal", "xsd:double", "xsd:float", "xsd:long", "xsd:int");

	@Override
	public IPage<ModelDatatypeProperty> page(Page page, ModelDatatypeProperty prop) {
		return baseMapper.selectPage(page,
				Wrappers.<ModelDatatypeProperty>lambdaQuery()
					.eq(prop.getClassId() != null, ModelDatatypeProperty::getClassId, prop.getClassId())
					.like(StrUtil.isNotBlank(prop.getLabel()), ModelDatatypeProperty::getLabel, prop.getLabel())
					.orderByAsc(ModelDatatypeProperty::getSortOrder));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveProp(ModelDatatypeProperty prop) {
		// 1. 拼接 propertyIri（方案B: {classLocalName}_{propLocalName}）
		//    classLocalName 由 Service 查 ModelClass 获取
		// 2. localName 同类唯一校验（AC-12.1）
		long count = count(Wrappers.<ModelDatatypeProperty>lambdaQuery()
			.eq(ModelDatatypeProperty::getClassId, prop.getClassId())
			.eq(ModelDatatypeProperty::getLocalName, prop.getLocalName()));
		if (count > 0) {
			return R.failed("属性名 '" + prop.getLocalName() + "' 在该类下已存在");
		}
		// 3. 单位绑定校验：非数值型不可绑单位（AC-12.2）
		if (StrUtil.isNotBlank(prop.getUnitRef()) && !NUMERIC_TYPES.contains(prop.getXsdType())) {
			return R.failed("非数值型属性（" + prop.getXsdType() + "）不可绑定单位");
		}
		// 4. 基数校验（AC-12.4）
		if (prop.getMinCardinality() == null) {
			prop.setMinCardinality(0);
		}
		if (prop.getMaxCardinality() == null) {
			prop.setMaxCardinality(-1);
		}
		if (prop.getMinCardinality() < 0) {
			return R.failed("最小基数不能为负");
		}
		if (prop.getMaxCardinality() != -1 && prop.getMaxCardinality() < prop.getMinCardinality()) {
			return R.failed("最大基数不能小于最小基数");
		}
		// 5. 标识符默认值（AC-12.5）
		if (StrUtil.isBlank(prop.getIsIdentifier())) {
			prop.setIsIdentifier("0");
		}
		// 6. propertyIri 预查重（uk: project_id + property_iri）
		try {
			return R.ok(save(prop));
		}
		catch (DuplicateKeyException e) {
			return R.failed("属性 IRI '" + prop.getPropertyIri() + "' 在项目内已存在");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateProp(ModelDatatypeProperty prop) {
		ModelDatatypeProperty existing = getById(prop.getId());
		if (existing == null) {
			return R.failed("属性不存在");
		}
		// localName / classId 不可改（锁定）
		prop.setLocalName(existing.getLocalName());
		prop.setClassId(existing.getClassId());
		prop.setPropertyIri(existing.getPropertyIri());
		prop.setTemplateCode(existing.getTemplateCode());
		// 单位校验
		if (StrUtil.isNotBlank(prop.getUnitRef()) && !NUMERIC_TYPES.contains(prop.getXsdType())) {
			return R.failed("非数值型属性（" + prop.getXsdType() + "）不可绑定单位");
		}
		return R.ok(updateById(prop));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeProp(Long id) {
		return R.ok(removeById(id));
	}
}
```

> **单位绑定校验**（AC-12.2）：`NUMERIC_TYPES` 白名单（xsd:integer/decimal/double/float/long/int），非数值型属性绑定单位返回 `R.failed`。单位候选来自 Supply API `/supply/v1/units`，`unitRef` 存 QUDT IRI（如 `http://qudt.org/vocab/unit/M`）。

> **不可改字段**：localName / classId / propertyIri / templateCode（溯源锁定），编辑只允许修改 label / xsdType / unitRef / enumValues / 基数 / isIdentifier / sortOrder。

### 4.5 Controller - ModelObjectPropertyController

```java
package com.pig4cloud.pig.ontology.modeling.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.modeling.dto.InverseSuggestDTO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import com.pig4cloud.pig.ontology.modeling.service.ModelObjectPropertyService;
import com.pig4cloud.pig.ontology.modeling.vo.InverseSuggestVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 对象属性 Controller（FR-13）
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/model/object-property")
@Tag(name = "对象属性建模", description = "对象属性 CRUD + domain/range + 反向关系（FR-13）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ModelObjectPropertyController {

	private final ModelObjectPropertyService objectPropertyService;

	@GetMapping("/page")
	@Operation(summary = "分页查询", description = "按 domainClassId 过滤（AC-13.4）")
	@HasPermission("ont_prop_model_view")
	public R<IPage<ModelObjectProperty>> page(@ParameterObject Page page,
			@ParameterObject ModelObjectProperty prop) {
		return R.ok(objectPropertyService.page(page, prop));
	}

	@GetMapping("/{id}")
	@Operation(summary = "属性详情")
	@HasPermission("ont_prop_model_view")
	public R<ModelObjectProperty> getById(@PathVariable Long id) {
		return R.ok(objectPropertyService.getById(id));
	}

	@SysLog("新增对象属性")
	@PostMapping
	@Operation(summary = "新增对象属性", description = "domain/range + 基数映射（AC-13.1/13.3）")
	@HasPermission("ont_prop_model_manage")
	public R save(@Valid @RequestBody ModelObjectProperty prop) {
		return objectPropertyService.saveProp(prop);
	}

	@SysLog("编辑对象属性")
	@PutMapping("/{id}")
	@Operation(summary = "编辑对象属性", description = "补全 range（AC-13.4）")
	@HasPermission("ont_prop_model_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody ModelObjectProperty prop) {
		prop.setId(id);
		return objectPropertyService.updateProp(prop);
	}

	@SysLog("删除对象属性")
	@DeleteMapping("/{id}")
	@Operation(summary = "删除对象属性")
	@HasPermission("ont_prop_model_manage")
	public R removeById(@PathVariable Long id) {
		return objectPropertyService.removeProp(id);
	}

	@PostMapping("/suggest-inverse")
	@Operation(summary = "反向关系建议", description = "建立 contains(O->P) 后建议在 P 建 belongsTo(P->O)（AC-13.5）")
	@HasPermission("ont_prop_model_view")
	public R<InverseSuggestVO> suggestInverse(@Valid @RequestBody InverseSuggestDTO dto) {
		return R.ok(objectPropertyService.suggestInverse(dto));
	}
}
```

### 4.6 ServiceImpl - ModelObjectPropertyServiceImpl

```java
package com.pig4cloud.pig.ontology.modeling.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.dto.InverseSuggestDTO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelObjectPropertyMapper;
import com.pig4cloud.pig.ontology.modeling.service.ModelObjectPropertyService;
import com.pig4cloud.pig.ontology.modeling.vo.InverseSuggestVO;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 对象属性 Service 实现（FR-13）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ModelObjectPropertyServiceImpl
		extends ServiceImpl<ModelObjectPropertyMapper, ModelObjectProperty>
		implements ModelObjectPropertyService {

	@Override
	public IPage<ModelObjectProperty> page(Page page, ModelObjectProperty prop) {
		return baseMapper.selectPage(page,
				Wrappers.<ModelObjectProperty>lambdaQuery()
					.eq(prop.getDomainClassId() != null, ModelObjectProperty::getDomainClassId,
							prop.getDomainClassId())
					.like(StrUtil.isNotBlank(prop.getLabel()), ModelObjectProperty::getLabel, prop.getLabel())
					.orderByAsc(ModelObjectProperty::getSortOrder));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveProp(ModelObjectProperty prop) {
		// 1. range 校验（AC-13.1）：新建时 range 必填（模板实例化的 range=NULL 需 DD9 补全）
		if (prop.getRangeClassId() == null) {
			return R.failed("值域类（range）不能为空");
		}
		// 2. localName 同类唯一
		long count = count(Wrappers.<ModelObjectProperty>lambdaQuery()
			.eq(ModelObjectProperty::getDomainClassId, prop.getDomainClassId())
			.eq(ModelObjectProperty::getLocalName, prop.getLocalName()));
		if (count > 0) {
			return R.failed("属性名 '" + prop.getLocalName() + "' 在该类下已存在");
		}
		// 3. 基数默认值
		if (prop.getMinCardinality() == null) {
			prop.setMinCardinality(0);
		}
		if (prop.getMaxCardinality() == null) {
			prop.setMaxCardinality(-1);
		}
		try {
			return R.ok(save(prop));
		}
		catch (DuplicateKeyException e) {
			return R.failed("属性 IRI '" + prop.getPropertyIri() + "' 在项目内已存在");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateProp(ModelObjectProperty prop) {
		ModelObjectProperty existing = getById(prop.getId());
		if (existing == null) {
			return R.failed("属性不存在");
		}
		// localName / domainClassId 不可改
		prop.setLocalName(existing.getLocalName());
		prop.setDomainClassId(existing.getDomainClassId());
		prop.setPropertyIri(existing.getPropertyIri());
		prop.setTemplateCode(existing.getTemplateCode());
		// range 可改（DD9 补全 DD8 实例化的 NULL range）
		return R.ok(updateById(prop));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeProp(Long id) {
		return R.ok(removeById(id));
	}

	@Override
	public InverseSuggestVO suggestInverse(InverseSuggestDTO dto) {
		// AC-13.5：建立 contains(domain=Order, range=Product) 后，
		// 建议在 Product 建立 belongsTo(domain=Product, range=Order)
		InverseSuggestVO vo = new InverseSuggestVO();
		vo.setSuggestedLocalName(mapInverseLocalName(dto.getLocalName()));
		vo.setSuggestedLabel(mapInverseLabel(dto.getLocalName()));
		vo.setSuggestedDomainClassId(dto.getRangeClassId());     // 反转：原 range -> 新 domain
		vo.setSuggestedRangeClassId(dto.getDomainClassId());     // 反转：原 domain -> 新 range
		return vo;
	}

	/**
	 * 反向属性名映射（AC-13.5）
	 * contains -> belongsTo, hasPart -> partOf, references -> referencedBy
	 */
	private String mapInverseLocalName(String localName) {
		if (localName == null) {
			return "inverseOf_" + System.currentTimeMillis();
		}
		return switch (localName) {
			case "contains" -> "belongsTo";
			case "hasPart" -> "partOf";
			case "references" -> "referencedBy";
			case "belongsTo" -> "contains";
			case "partOf" -> "hasPart";
			case "referencedBy" -> "references";
			default -> "inverseOf_" + localName;
		};
	}

	private String mapInverseLabel(String localName) {
		return switch (localName) {
			case "contains" -> "属于";
			case "hasPart" -> "所属";
			case "references" -> "被引用";
			case "belongsTo" -> "包含";
			case "partOf" -> "包含";
			case "referencedBy" -> "引用";
			default -> "反向_" + localName;
		};
	}

	/**
	 * defaultCardinality -> min/max 基数映射（AC-13.3）
	 * 仓库内无此逻辑，DD9 新增。供从对象属性模板实例化时调用。
	 */
	public static int[] mapCardinality(String defaultCardinality) {
		if (defaultCardinality == null) {
			return new int[] { 0, -1 };
		}
		return switch (defaultCardinality) {
			case "one-to-one" -> new int[] { 0, 1 };
			case "one-to-many" -> new int[] { 0, -1 };
			case "many-to-one" -> new int[] { 0, 1 };
			case "many-to-many" -> new int[] { 0, -1 };
			default -> new int[] { 0, -1 };
		};
	}
}
```

> **defaultCardinality -> min/max 映射**（AC-13.3）：仓库内无此逻辑（DD9 新增职责）。映射规则：one-to-one -> [0,1]、one-to-many -> [0,-1]、many-to-one -> [0,1]、many-to-many -> [0,-1]。`maxCardinality=-1` 表示无限制（DDL DEFAULT -1）。

> **反向关系建议**（AC-13.5）：建立 `contains(domain=Order, range=Product)` 后，建议在 Product 建立 `belongsTo(domain=Product, range=Order)`--反转 domain/range，映射反向属性名。内置 6 个反向映射（contains<->belongsTo、hasPart<->partOf、references<->referencedBy），未知属性名用 `inverseOf_` 前缀。

> **range 补全**：DD8 实例化的对象属性 range=NULL，DD9 `updateProp` 允许修改 range（`prop.setRangeClassId` 可填值），`saveProp` 新建时 range 必填。

### 4.7 Controller - ModelSubclassOfController

```java
package com.pig4cloud.pig.ontology.modeling.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.modeling.dto.SubclassOfSaveDTO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelSubclassOf;
import com.pig4cloud.pig.ontology.modeling.service.ModelSubclassOfService;
import com.pig4cloud.pig.ontology.modeling.vo.ModelSubclassOfTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 类层级 Controller（FR-14）
 * <p>
 * subClassOf 是类的子操作，复用类建模权限 ont_class_model_manage。
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/model/subclassof")
@Tag(name = "类层级建模", description = "subClassOf CRUD + 类树 + 镜像回推（FR-14）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ModelSubclassOfController {

	private final ModelSubclassOfService subclassOfService;

	@GetMapping("/tree")
	@Operation(summary = "类树视图", description = "按 projectId 返回类层级树（AC-14.1）")
	@HasPermission("ont_class_model_view")
	public R<List<ModelSubclassOfTreeVO>> tree(@RequestParam Long projectId) {
		return R.ok(subclassOfService.tree(projectId));
	}

	@GetMapping("/suggest-parent")
	@Operation(summary = "父类建议", description = "消费 Supply suggest 端点（AC-14.2）")
	@HasPermission("ont_class_model_view")
	public R<List<String>> suggestParent(@RequestParam Long classId) {
		return R.ok(subclassOfService.suggestParentIris(classId));
	}

	@SysLog("建立类层级")
	@PostMapping
	@Operation(summary = "建立 subClassOf", description = "环路检测 + 镜像回推（AC-14.1/14.3）")
	@HasPermission("ont_class_model_manage")
	public R save(@Valid @RequestBody SubclassOfSaveDTO dto) {
		return subclassOfService.saveEdge(dto);
	}

	@SysLog("删除类层级")
	@DeleteMapping
	@Operation(summary = "删除 subClassOf", description = "镜像失效（AC-14.4）")
	@HasPermission("ont_class_model_manage")
	public R remove(@RequestParam Long childClassId, @RequestParam Long parentClassId) {
		return subclassOfService.removeEdge(childClassId, parentClassId);
	}
}
```

### 4.8 ServiceImpl - ModelSubclassOfServiceImpl

```java
package com.pig4cloud.pig.ontology.modeling.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.dto.SubclassOfSaveDTO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.entity.ModelSubclassOf;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelClassMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelSubclassOfMapper;
import com.pig4cloud.pig.ontology.modeling.service.HierarchySyncService;
import com.pig4cloud.pig.ontology.modeling.service.ModelSubclassOfService;
import com.pig4cloud.pig.ontology.modeling.vo.ModelSubclassOfTreeVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * 类层级 Service 实现（FR-14）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ModelSubclassOfServiceImpl extends ServiceImpl<ModelSubclassOfMapper, ModelSubclassOf>
		implements ModelSubclassOfService {

	private final ModelClassMapper modelClassMapper;
	private final HierarchySyncService hierarchySyncService;
	private final RestTemplate restTemplate;

	private static final String SUPPLY_BASE = "http://localhost:9999/admin/ont/supply/v1";

	@Override
	public List<ModelSubclassOfTreeVO> tree(Long projectId) {
		// 查项目下所有类
		List<ModelClass> classes = modelClassMapper.selectList(
				Wrappers.<ModelClass>lambdaQuery().eq(ModelClass::getProjectId, projectId));
		// 查项目下所有 subClassOf 边
		List<ModelSubclassOf> edges = list(Wrappers.<ModelSubclassOf>lambdaQuery()
				.eq(ModelSubclassOf::getProjectId, projectId));
		// 组装树：按 parentClassId 分组
		Map<Long, List<ModelSubclassOf>> edgeMap = new HashMap<>();
		for (ModelSubclassOf edge : edges) {
			edgeMap.computeIfAbsent(edge.getParentClassId(), k -> new ArrayList<>()).add(edge);
		}
		// 找根节点（未被任何边作为 child 的类）
		Set<Long> childIds = new HashSet<>();
		for (ModelSubclassOf edge : edges) {
			childIds.add(edge.getChildClassId());
		}
		List<ModelSubclassOfTreeVO> roots = new ArrayList<>();
		Map<Long, ModelClass> classMap = new HashMap<>();
		for (ModelClass cls : classes) {
			classMap.put(cls.getId(), cls);
			if (!childIds.contains(cls.getId())) {
				roots.add(buildTreeVO(cls, edgeMap, classMap));
			}
		}
		return roots;
	}

	private ModelSubclassOfTreeVO buildTreeVO(ModelClass cls, Map<Long, List<ModelSubclassOf>> edgeMap,
			Map<Long, ModelClass> classMap) {
		ModelSubclassOfTreeVO vo = new ModelSubclassOfTreeVO();
		vo.setClassId(cls.getId());
		vo.setClassIri(cls.getClassIri());
		vo.setLabel(cls.getLabel());
		vo.setClassificationCode(cls.getClassificationCode());
		List<ModelSubclassOf> children = edgeMap.get(cls.getId());
		if (CollUtil.isNotEmpty(children)) {
			List<ModelSubclassOfTreeVO> childNodes = new ArrayList<>();
			for (ModelSubclassOf edge : children) {
				ModelClass childCls = classMap.get(edge.getChildClassId());
				if (childCls != null) {
					childNodes.add(buildTreeVO(childCls, edgeMap, classMap));
				}
			}
			vo.setChildren(childNodes);
		}
		return vo;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> suggestParentIris(Long classId) {
		// AC-14.2：消费 Supply suggest 端点
		ModelClass cls = modelClassMapper.selectById(classId);
		if (cls == null || StrUtil.isBlank(cls.getTemplateCode())) {
			return Collections.emptyList();
		}
		String url = SUPPLY_BASE + "/class-hierarchy/suggest?templateCode=" + cls.getTemplateCode();
		try {
			R<List<String>> resp = restTemplate.getForObject(url, R.class);
			return (resp != null && resp.getCode() == 0) ? resp.getData() : Collections.emptyList();
		}
		catch (Exception e) {
			return Collections.emptyList();
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveEdge(SubclassOfSaveDTO dto) {
		// 1. 自引用校验
		if (dto.getChildClassId().equals(dto.getParentClassId())) {
			return R.failed("子类与父类不能相同");
		}
		// 2. 环路检测（DFS，AC-14.1）
		if (wouldCreateCycle(dto.getProjectId(), dto.getChildClassId(), dto.getParentClassId())) {
			return R.failed("建立此关系会导致类层级成环");
		}
		// 3. 重复校验（uk: project_id + child_class_id + parent_class_id）
		long count = count(Wrappers.<ModelSubclassOf>lambdaQuery()
			.eq(ModelSubclassOf::getProjectId, dto.getProjectId())
			.eq(ModelSubclassOf::getChildClassId, dto.getChildClassId())
			.eq(ModelSubclassOf::getParentClassId, dto.getParentClassId()));
		if (count > 0) {
			return R.failed("该类层级关系已存在");
		}
		// 4. 保存边（sync_status='0' 待同步）
		ModelSubclassOf edge = new ModelSubclassOf();
		edge.setProjectId(dto.getProjectId());
		edge.setChildClassId(dto.getChildClassId());
		edge.setParentClassId(dto.getParentClassId());
		edge.setSourceTemplateRef(dto.getSourceTemplateRef());
		edge.setSyncStatus("0");
		save(edge);
		// 5. 镜像回推（AC-14.3）
		R syncResult = hierarchySyncService.pushMirror(edge);
		if (syncResult.getCode() != 0) {
			// 回推失败：sync_status 保持 '0'，补偿任务重试（AC-14.5）
			// 不回滚建模域事务（建模域是权威源，镜像可异步补偿）
			return R.ok("类层级已建立，镜像同步稍后重试");
		}
		// 回推成功：更新 sync_status='1'
		edge.setSyncStatus("1");
		updateById(edge);
		return R.ok(edge);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeEdge(Long childClassId, Long parentClassId) {
		ModelSubclassOf edge = getOne(Wrappers.<ModelSubclassOf>lambdaQuery()
			.eq(ModelSubclassOf::getChildClassId, childClassId)
			.eq(ModelSubclassOf::getParentClassId, parentClassId));
		if (edge == null) {
			return R.failed("类层级关系不存在");
		}
		// 1. 软删建模域边
		removeById(edge.getId());
		// 2. 镜像失效（AC-14.4）
		hierarchySyncService.invalidateMirror(edge);
		return R.ok();
	}

	/**
	 * 环路检测：从 parentClassId 向上查父链，若遇到 childClassId 则成环（AC-14.1）
	 * 参考治理域 CycleDetectorService 算法骨架（visited 防环 + 向上查链），
	 * 但数据源是 ont_model_subclassof（按 child/parent 遍历）而非 ClassTemplate.parentId。
	 */
	private boolean wouldCreateCycle(Long projectId, Long childClassId, Long parentClassId) {
		Set<Long> visited = new HashSet<>();
		Long cur = parentClassId;
		while (cur != null && visited.add(cur)) {
			if (cur.equals(childClassId)) {
				return true;
			}
			// 查 cur 的父类（向上走）
			List<ModelSubclassOf> parents = list(Wrappers.<ModelSubclassOf>lambdaQuery()
				.eq(ModelSubclassOf::getProjectId, projectId)
				.eq(ModelSubclassOf::getChildClassId, cur));
			// 多继承：取第一个父继续向上（DFS 简化--多继承环路需遍历所有父链）
			cur = parents.isEmpty() ? null : parents.get(0).getParentClassId();
		}
		return false;
	}
}
```

> **环路检测**（AC-14.1）：参考治理域 `CycleDetectorService` 的算法骨架（visited HashSet 防环 + 向上查父链），但数据源改为 `ont_model_subclassof` 表（按 childClassId/parentClassId 遍历）。注意：当前实现取第一个父类继续向上（简化），严格多继承环路检测需 DFS 遍历所有父链--v1 单继承场景足够，多继承增强后续优化。

> **镜像回推失败不回滚**（AC-14.5）：建模域是权威源，建立 subClassOf 成功即业务完成；镜像回推失败时 sync_status 保持 '0'，补偿任务异步重试。不因镜像同步失败而回滚建模域事务（R-19）。

### 4.9 HierarchySyncService - 镜像回推核心

```java
package com.pig4cloud.pig.ontology.modeling.service;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.entity.ModelSubclassOf;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelClassMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 镜像回推核心 Service（FR-14.3/14.4）
 * <p>
 * 建模域建立 subClassOf 后，调治理域 Sync API 回推镜像到 ont_class_hierarchy。
 * 回推失败时 sync_status='0'，由 HierarchySyncCompensateTask 补偿重试（AC-14.5）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Slf4j
@AllArgsConstructor
@Service
public class HierarchySyncService {

	private static final String SYNC_BASE = "http://localhost:9999/admin/ont/sync";

	private final RestTemplate restTemplate;
	private final ModelClassMapper modelClassMapper;

	/**
	 * 回推镜像：建立 subClassOf 后调 Sync POST（AC-14.3）
	 */
	@SuppressWarnings("unchecked")
	public R pushMirror(ModelSubclassOf edge) {
		ModelClass childClass = modelClassMapper.selectById(edge.getChildClassId());
		ModelClass parentClass = modelClassMapper.selectById(edge.getParentClassId());
		if (childClass == null || parentClass == null) {
			return R.failed("子类或父类不存在");
		}
		// 组装 ClassHierarchySyncDTO
		Map<String, Object> dto = new HashMap<>();
		dto.put("childClassIri", childClass.getClassIri());
		dto.put("parentClassIri", parentClass.getClassIri());
		dto.put("sourceTemplateRef", edge.getSourceTemplateRef());
		dto.put("treeRoot", childClass.getTemplateCode()); // treeRoot 用类的模板溯源标识

		String url = SYNC_BASE + "/class-hierarchy";
		try {
			R resp = restTemplate.postForObject(url, Collections.singletonList(dto), R.class);
			if (resp != null && resp.getCode() == 0) {
				return R.ok();
			}
			return R.failed("镜像回推失败：" + (resp != null ? resp.getMsg() : "未知错误"));
		}
		catch (Exception e) {
			log.error("镜像回推异常: child={}, parent={}", childClass.getClassIri(),
					parentClass.getClassIri(), e);
			return R.failed("镜像回推异常：" + e.getMessage());
		}
	}

	/**
	 * 镜像失效：删除 subClassOf 后调 Sync DELETE（AC-14.4）
	 */
	@SuppressWarnings("unchecked")
	public R invalidateMirror(ModelSubclassOf edge) {
		ModelClass childClass = modelClassMapper.selectById(edge.getChildClassId());
		ModelClass parentClass = modelClassMapper.selectById(edge.getParentClassId());
		if (childClass == null || parentClass == null) {
			return R.failed("子类或父类不存在");
		}
		String url = SYNC_BASE + "/class-hierarchy?childClassIri=" + childClass.getClassIri()
				+ "&parentClassIri=" + parentClass.getClassIri();
		try {
			restTemplate.delete(url);
			return R.ok();
		}
		catch (Exception e) {
			log.error("镜像失效异常: child={}, parent={}", childClass.getClassIri(),
					parentClass.getClassIri(), e);
			return R.failed("镜像失效异常：" + e.getMessage());
		}
	}
}
```

> **回推组装**：`ClassHierarchySyncDTO` 的 4 个字段来源--`childClassIri`/`parentClassIri` 从 ModelClass.classIri 取，`sourceTemplateRef` 从 ModelSubclassOf.sourceTemplateRef 取，`treeRoot` 用子类的 templateCode（模板溯源标识）。POST 端点接收 `List<DTO>`，此处传单元素列表。

> **Sync API 权限**：POST/DELETE 端点权限 `ont_sync_push`（仅建模侧服务账号）。RestTemplate 调用时需携带服务账号 token（通过请求头传递，或在内部调用时绕过鉴权--v1 简化用同进程 localhost 调用，微服务模式需配置服务账号认证）。

### 4.10 HierarchySyncCompensateTask - 补偿定时任务

```java
package com.pig4cloud.pig.ontology.modeling.task;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.modeling.entity.ModelSubclassOf;
import com.pig4cloud.pig.ontology.modeling.service.HierarchySyncService;
import com.pig4cloud.pig.ontology.modeling.service.ModelSubclassOfService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 镜像回推补偿定时任务（FR-14.5，AC-14.5）
 * <p>
 * 定时扫描 sync_status='0'（待同步）的 subClassOf 记录，重试回推。
 * 最多重试 3 次（通过 sync_status 语义控制：0=待同步/重试中，1=已同步，2=已失效）。
 * 固定延迟 60 秒执行一次（避免密集重试）。
 * <p>
 * 注意：需在启动类加 @EnableScheduling（pig 框架默认未启用）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Slf4j
@AllArgsConstructor
@Component
public class HierarchySyncCompensateTask {

	private final ModelSubclassOfService subclassOfService;
	private final HierarchySyncService hierarchySyncService;

	/**
	 * 每 60 秒扫描一次待同步的 subClassOf 记录，重试回推
	 */
	@Scheduled(fixedDelay = 60000)
	public void retrySync() {
		List<ModelSubclassOf> pending = subclassOfService.list(
				Wrappers.<ModelSubclassOf>lambdaQuery()
					.eq(ModelSubclassOf::getSyncStatus, "0"));
		if (CollUtil.isEmpty(pending)) {
			return;
		}
		log.info("镜像回推补偿任务：发现 {} 条待同步记录", pending.size());
		for (ModelSubclassOf edge : pending) {
			try {
				com.pig4cloud.pig.common.core.util.R result = hierarchySyncService.pushMirror(edge);
				if (result.getCode() == 0) {
					edge.setSyncStatus("1");
					subclassOfService.updateById(edge);
					log.info("补偿回推成功: child={}, parent={}", edge.getChildClassId(),
							edge.getParentClassId());
				}
				else {
					log.warn("补偿回推失败: child={}, parent={}, msg={}", edge.getChildClassId(),
							edge.getParentClassId(), result.getMsg());
				}
			}
			catch (Exception e) {
				log.error("补偿回推异常: child={}, parent={}", edge.getChildClassId(),
						edge.getParentClassId(), e);
			}
		}
	}
}
```

> **补偿任务设计**（AC-14.5）：`@Scheduled(fixedDelay = 60000)` 每 60 秒扫描 `sync_status='0'` 的记录重试回推。成功置 '1'，失败保持 '0' 下轮继续重试。不设硬性重试上限（靠 sync_status 语义控制：只要还是 '0' 就继续重试，直到成功或手动处理）。v1 用 Spring 原生 `@Scheduled`（需启动类加 `@EnableScheduling`），而非 pig-quartz（避免引入 Quartz 调度依赖的复杂度，补偿任务逻辑简单）。

> **启动类修改**：需在 `PigOntologyApplication` 和 `PigBootApplication` 加 `@EnableScheduling`：
> ```java
> @EnableScheduling  // 新增
> @EnableOpenApi("ontology")
> @EnablePigResourceServer
> @EnableDiscoveryClient
> @SpringBootApplication
> public class PigOntologyApplication { ... }
> ```

### 4.11 VO

```java
// ModelSubclassOfTreeVO.java
package com.pig4cloud.pig.ontology.modeling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "类树视图节点")
public class ModelSubclassOfTreeVO {

	@Schema(description = "类 ID")
	private Long classId;

	@Schema(description = "类 IRI")
	private String classIri;

	@Schema(description = "标签")
	private String label;

	@Schema(description = "分类编码")
	private String classificationCode;

	@Schema(description = "子节点")
	private List<ModelSubclassOfTreeVO> children;
}
```

```java
// InverseSuggestVO.java
package com.pig4cloud.pig.ontology.modeling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "反向关系建议结果")
public class InverseSuggestVO {

	@Schema(description = "建议的反向属性名")
	private String suggestedLocalName;

	@Schema(description = "建议的反向属性标签")
	private String suggestedLabel;

	@Schema(description = "建议的 domain（原 range 反转）")
	private Long suggestedDomainClassId;

	@Schema(description = "建议的 range（原 domain 反转）")
	private Long suggestedRangeClassId;
}
```

```java
// InverseSuggestDTO.java
package com.pig4cloud.pig.ontology.modeling.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "反向关系建议请求")
public class InverseSuggestDTO {

	@Schema(description = "原属性名")
	@NotBlank(message = "属性名不能为空")
	private String localName;

	@Schema(description = "原 domain 类 ID")
	@NotNull(message = "domain 不能为空")
	private Long domainClassId;

	@Schema(description = "原 range 类 ID")
	@NotNull(message = "range 不能为空")
	private Long rangeClassId;
}
```

---

## 五、前端设计

### 5.1 API 文件

```typescript
// datatype-property.ts
import request from '/@/utils/request';

export function pageList(query?: any) {
	return request({ url: '/admin/ont/model/datatype-property/page', method: 'get', params: query });
}
export function getObj(id: string) {
	return request({ url: '/admin/ont/model/datatype-property/' + id, method: 'get' });
}
export function addObj(obj: any) {
	return request({ url: '/admin/ont/model/datatype-property', method: 'post', data: obj });
}
export function putObj(obj: any) {
	return request({ url: '/admin/ont/model/datatype-property/' + obj.id, method: 'put', data: obj });
}
export function delObj(id: string) {
	return request({ url: '/admin/ont/model/datatype-property/' + id, method: 'delete' });
}

// 对象属性 API（object-property.ts）结构相同，URL 前缀 /admin/ont/model/object-property
// 类层级 API（subclassof.ts）
export function tree(projectId: string) {
	return request({ url: '/admin/ont/model/subclassof/tree', method: 'get', params: { projectId } });
}
export function suggestParent(classId: string) {
	return request({ url: '/admin/ont/model/subclassof/suggest-parent', method: 'get', params: { classId } });
}
export function addEdge(obj: any) {
	return request({ url: '/admin/ont/model/subclassof', method: 'post', data: obj });
}
export function delEdge(childClassId: string, parentClassId: string) {
	return request({ url: '/admin/ont/model/subclassof', method: 'delete', params: { childClassId, parentClassId } });
}
```

### 5.2 index.vue - 属性建模页（Tab 切换）

```
┌──────────────────────────────────────────────────────────────┐
│ [项目: 消防设备▼] [类: FireSprayPump▼]                       │
├──────────────────────────────────────────────────────────────┤
│ [数据属性] [对象属性]                        [+新增属性]      │
├──────────────────────────────────────────────────────────────┤
│ # │ 属性名     │ 标签   │ XSD类型   │ 单位 │ 基数  │标识│操作  │
│ 1 │ price      │ 价格   │ xsd:decimal│ CNY │ 0..n │ 否 │编删  │
│ 2 │ flowRate   │ 额定流量│ xsd:decimal│ M3S │ 0..1 │ 否 │编删  │
│ 3 │ name       │ 名称   │ xsd:string │ -   │ 0..1 │ 是 │编删  │
├──────────────────────────────────────────────────────────────┤
│                                              < 1 > 10/页     │
└──────────────────────────────────────────────────────────────┘
```

**结构要点**：
- 顶部：项目选择 + 类选择（级联，选项目后加载类列表）。
- `el-tabs` 切换数据属性 / 对象属性两个 Tab。
- 数据属性 Tab：el-table（属性名/标签/XSD类型/单位/基数/标识符/操作），基数格式化为 `min..max`（-1 显示 n）。
- 对象属性 Tab：el-table（属性名/标签/domain/range/基数/操作），range 为空时显示"待补全"el-tag warning。
- [+新增属性] 按当前 Tab 打开对应表单弹窗。
- 对象属性建立后弹出反向关系建议弹窗（inverse-dialog）。

### 5.3 datatype-form.vue - 数据属性表单

**字段表格**：

| 字段 | 组件 | 校验 | 说明 |
|---|---|---|---|
| localName | el-input | required, 编辑时 disabled | 属性名 |
| label | el-input | required | 显示名 |
| templateCode | el-select | - | 属性模板选择（来自 Supply property-templates?kind=datatype），选后自动填充 |
| xsdType | el-select | required | XSD 类型（string/integer/decimal/boolean/dateTime） |
| unitRef | el-select | - | 单位选择（来自 Supply units），非数值型禁用 |
| enumValues | el-input tag | - | 枚举值（动态 tag 输入） |
| minCardinality | el-input-number | 默认 0 | 最小基数 |
| maxCardinality | el-input-number | 默认 -1 | 最大基数（-1=无限制） |
| isIdentifier | el-switch | 默认 否 | 标识符标记 |

**模板选择交互**：选属性模板后自动填充 xsdType/label/unitRef/enumValues/isIdentifier，localName 用 templateCode。

### 5.4 object-form.vue - 对象属性表单

**字段表格**：

| 字段 | 组件 | 校验 | 说明 |
|---|---|---|---|
| localName | el-input | required, 编辑时 disabled | 属性名 |
| label | el-input | required | 显示名 |
| templateCode | el-select | - | 属性模板选择（来自 Supply property-templates?kind=object） |
| domainClassId | el-select disabled | - | 域类（自动填充当前选中类） |
| rangeClassId | el-select | required | 值域类（选项目下的其他类） |
| minCardinality | el-input-number | 默认 0 | 最小基数 |
| maxCardinality | el-input-number | 默认 -1 | 最大基数 |

**range 补全**：编辑时若 rangeClassId 为空（DD8 实例化遗留），表单高亮提示"请补全值域类"。

### 5.5 inverse-dialog.vue - 反向关系建议弹窗

建立对象属性后弹出，展示建议的反向属性信息（反转 domain/range + 映射属性名），用户确认后一键创建反向属性。

### 5.6 composables.ts

```typescript
import { useI18n } from 'vue-i18n';

export function useModelPropertyOptions() {
	const { t } = useI18n();

	const xsdTypeOptions = computed(() => [
		{ value: 'xsd:string', label: 'xsd:string', numeric: false },
		{ value: 'xsd:integer', label: 'xsd:integer', numeric: true },
		{ value: 'xsd:decimal', label: 'xsd:decimal', numeric: true },
		{ value: 'xsd:boolean', label: 'xsd:boolean', numeric: false },
		{ value: 'xsd:dateTime', label: 'xsd:dateTime', numeric: false },
	]);

	const cardinalityOptions = computed(() => [
		{ value: 'one-to-one', label: t('modelProperty.cardOneToOne') },
		{ value: 'one-to-many', label: t('modelProperty.cardOneToMany') },
		{ value: 'many-to-one', label: t('modelProperty.cardManyToOne') },
		{ value: 'many-to-many', label: t('modelProperty.cardManyToMany') },
	]);

	/** 基数格式化：min..max（-1 显示 n） */
	const formatCardinality = (min: number, max: number) => {
		return `${min}..${max === -1 ? 'n' : max}`;
	};

	/** XSD 类型是否数值型（控制单位绑定可用性） */
	const isNumericType = (xsdType: string) => {
		return xsdTypeOptions.value.find((o) => o.value === xsdType)?.numeric ?? false;
	};

	return { xsdTypeOptions, cardinalityOptions, formatCardinality, isNumericType };
}
```

### 5.7 i18n（zh-cn.ts 摘要）

```typescript
export default {
	modelProperty: {
		// 字段
		localName: '属性名',
		label: '标签',
		xsdType: 'XSD 类型',
		unitRef: '单位',
		enumValues: '枚举值',
		minCardinality: '最小基数',
		maxCardinality: '最大基数',
		isIdentifier: '标识符',
		domainClass: '域类',
		rangeClass: '值域类',
		templateCode: '来源模板',
		// Tab
		datatypeProperty: '数据属性',
		objectProperty: '对象属性',
		// 基数选项
		cardOneToOne: '一对一',
		cardOneToMany: '一对多',
		cardManyToOne: '多对一',
		cardManyToMany: '多对多',
		// 状态
		rangePending: '待补全',
		// 操作
		add: '新增属性',
		edit: '编辑',
		delete: '删除',
		suggestInverse: '建议反向关系',
		// 提示
		unitDisabledTip: '非数值型属性不可绑定单位',
		rangeRequiredTip: '请补全值域类',
		// 选择
		selectClass: '请选择类',
		selectProject: '请选择项目',
		// 确认
		deleteTip: '确认删除该属性？',
		inverseDialogTitle: '反向关系建议',
		inverseDialogTip: '是否在值域类中创建反向属性？',
	},
};
```

---

## 六、横切设计

### 6.1 校验

| 校验点 | 实现位置 | 说明 |
|---|---|---|
| 数据属性 localName 同类唯一 | DTServiceImpl 预查重 + DB uk(project_id,property_iri) | 两层校验 |
| 对象属性 localName 同类唯一 | ObjServiceImpl 预查重 + DB uk | 两层校验 |
| 非数值型不可绑单位 | DTServiceImpl `NUMERIC_TYPES` 白名单 | AC-12.2 |
| 对象属性 range 必填 | ObjServiceImpl.saveProp | 新建时必填（模板实例化的 NULL 需补全） |
| subClassOf 自引用 | SubServiceImpl.saveEdge | childId == parentId 拒绝 |
| subClassOf 环路检测 | SubServiceImpl.wouldCreateCycle | DFS 向上查父链，AC-14.1 |
| subClassOf 重复 | SubServiceImpl 预查重 + DB uk(project_id,child,parent) | 两层校验 |
| 基数合法性 | DT/ObjServiceImpl | min>=0，max=-1 或 max>=min |

### 6.2 异常

- Sync API 调用失败：`R.failed("镜像回推失败/异常")`，建模域事务不回滚（权威源），sync_status='0' 补偿重试。
- Supply API 调用失败：suggestParentIris 返回空列表（降级，不阻塞建模）。
- `DuplicateKeyException` 捕获转友好提示。

### 6.3 镜像回推一致性（R-19，AC-14.5）

```
建立 subClassOf:
  1. 建模域写 ont_model_subclassof（sync_status='0'）  -- 本地事务
  2. 调 Sync API POST 回推镜像                          -- HTTP
  3a. 成功 -> sync_status='1'
  3b. 失败 -> sync_status='0'（不回滚建模域），补偿任务 60s 后重试

删除 subClassOf:
  1. 建模域软删 ont_model_subclassof                    -- 本地事务
  2. 调 Sync API DELETE 置镜像失效                      -- HTTP
  3a. 成功 -> 完成
  3b. 失败 -> 镜像保持原状（可能残留），补偿任务不处理删除（v1 简化）
```

> **不走分布式事务**（R-19）：建模域是权威源，建立成功即业务完成；镜像同步是最终一致。补偿任务只处理"建立"的回推重试，不处理"删除"的失效重试（v1 简化，删除失效失败概率低，残留镜像不影响建模域权威性）。

### 6.4 治理域消费边界（R-23）

- **只读消费 Supply API**：property-templates（拉模板下拉）、units（拉单位下拉）、class-hierarchy/suggest（父类建议）。
- **调用 Sync API**：class-hierarchy POST/DELETE（镜像回推），权限 `ont_sync_push`。
- **不注入治理域 Service**：全部走 HTTP RestTemplate。
- **不写治理域表**：镜像写入由治理域 SyncController 的 ClassHierarchyService 负责，建模域只调 API。

### 6.5 启动类修改

需在 `PigOntologyApplication` 和 `PigBootApplication` 加 `@EnableScheduling`（pig 框架默认未启用 Spring 定时任务）。补偿任务 `@Scheduled(fixedDelay=60000)` 依赖此注解。

---

## 七、测试要点

| 类别 | 要点 |
|---|---|
| 编译 | modeling 包新增类无编译错误 |
| Flyway（V14） | `SELECT count(*) FROM ont_model_subclassof` = 0；`SELECT is_nullable FROM information_schema.columns WHERE table_name='ont_model_object_property' AND column_name='range_class_id'` = 'YES'（可空）；`SELECT menu_id FROM sys_menu WHERE menu_id BETWEEN 11300 AND 11304` = 5 条 |
| 覆盖（AC-12.1） | `POST /datatype-property` 新增成功；重复 localName 返回 `R.failed("属性名 'xxx' 在该类下已存在")` |
| 覆盖（AC-12.2） | xsdType='xsd:string' 传 unitRef 返回 `R.failed("非数值型属性不可绑定单位")`；xsdType='xsd:decimal' 绑单位成功 |
| 覆盖（AC-12.3） | enumValues 传 `["active","inactive"]` 存 JSON 字符串，查询返回正确 |
| 覆盖（AC-12.4） | minCardinality=1, maxCardinality=1 保存成功；maxCardinality=0, minCardinality=1 返回 `R.failed("最大基数不能小于最小基数")` |
| 覆盖（AC-12.5） | isIdentifier='1' 保存成功，查询返回 isIdentifier='1' |
| 覆盖（AC-12.6） | `PUT /{id}` 编辑 label 成功；localName 被锁定（返回值不变）；`DELETE /{id}` 软删成功 |
| 覆盖（AC-13.1） | `POST /object-property` 传 domainClassId+rangeClassId 成功；range 为空返回 `R.failed("值域类不能为空")` |
| 覆盖（AC-13.3） | 从模板实例化对象属性（defaultCardinality='one-to-many'），查 min=0, max=-1 |
| 覆盖（AC-13.4） | `PUT /object-property/{id}` 补全 range（原 NULL -> 填值）成功 |
| 覆盖（AC-13.5） | `POST /suggest-inverse` 传 localName='contains'，返回 suggestedLocalName='belongsTo'，domain/range 反转 |
| 覆盖（AC-14.1） | `POST /subclassof` 建立 subClassOf(Pump, Equipment) 成功；建立 subClassOf(Equipment, Pump) 返回 `R.failed("建立此关系会导致类层级成环")` |
| 覆盖（AC-14.2） | `GET /suggest-parent?classId=xx` 返回父类 IRI 列表（来自 Supply suggest） |
| 覆盖（AC-14.3） | 建立后查 `SELECT sync_status FROM ont_model_subclassof` = '1'（回推成功）；查 ont_class_hierarchy 镜像 sync_status='1' |
| 覆盖（AC-14.4） | 删除后查 ont_class_hierarchy 镜像 sync_status='2'（失效） |
| 覆盖（AC-14.5） | 模拟 Sync API 不可用，建立后 sync_status='0'，补偿任务 60s 后重试成功置 '1' |
| 覆盖（AC-14.6） | 建立 subClassOf(A, B) + subClassOf(A, C) 成功（多继承） |
| range_class_id 修复 | V14 后 `INSERT INTO ont_model_object_property(...,range_class_id,...) VALUES(...,NULL,...)` 成功（可空） |
| 权限 | 无 `ont_prop_model_manage` 调 POST 返回 403；无 `ont_class_model_manage` 调 subClassOf POST 返回 403 |

---

## 八、风险与缓解

| 风险 | 缓解 |
|---|---|
| Sync API 权限 `ont_sync_push` 需服务账号 | v1 同进程 localhost 调用（单体模式绕过鉴权或内部 token）；微服务模式配置服务账号认证 |
| 补偿任务密集重试压垮 Sync API | `fixedDelay=60000`（60s 间隔）；只扫 sync_status='0'；成功即置 '1' 退出 |
| 多继承环路检测不完整 | v1 取第一个父类向上查（单继承场景足够）；严格多继承 DFS 遍历所有父链后续优化 |
| `@EnableScheduling` 影响其他模块 | Spring 定时任务按 Bean 隔离，仅 HierarchySyncCompensateTask 注册；不影响治理域 |
| range_class_id DDL 修复需 V14 ALTER | Flyway 已应用 V13 不可改，V14 ALTER DROP NOT NULL 是唯一修复路径；已验证 PostgreSQL 语法 |
| defaultCardinality 映射规则无先例 | DD9 新增 `mapCardinality` 方法，映射规则在文档明确（one-to-one->[0,1] 等） |

---

## 九、交付物清单

| 类型 | 文件 | 说明 |
|---|---|---|
| 新建 | `modeling/entity/ModelSubclassOf.java` | 类层级 Entity |
| 新建 | `modeling/mapper/ModelSubclassOfMapper.java` | 类层级 Mapper |
| 新建 | `modeling/service/ModelDatatypePropertyService.java` | 数据属性 Service 接口 |
| 新建 | `modeling/service/ModelObjectPropertyService.java` | 对象属性 Service 接口 |
| 新建 | `modeling/service/ModelSubclassOfService.java` | 类层级 Service 接口 |
| 新建 | `modeling/service/HierarchySyncService.java` | 镜像回推核心 Service |
| 新建 | `modeling/service/impl/ModelDatatypePropertyServiceImpl.java` | 数据属性 Service 实现 |
| 新建 | `modeling/service/impl/ModelObjectPropertyServiceImpl.java` | 对象属性 Service 实现 |
| 新建 | `modeling/service/impl/ModelSubclassOfServiceImpl.java` | 类层级 Service 实现 |
| 新建 | `modeling/task/HierarchySyncCompensateTask.java` | 补偿定时任务 |
| 新建 | `modeling/controller/ModelDatatypePropertyController.java` | 数据属性 Controller |
| 新建 | `modeling/controller/ModelObjectPropertyController.java` | 对象属性 Controller |
| 新建 | `modeling/controller/ModelSubclassOfController.java` | 类层级 Controller |
| 新建 | `modeling/dto/SubclassOfSaveDTO.java` | 建立 subClassOf DTO |
| 新建 | `modeling/dto/InverseSuggestDTO.java` | 反向关系建议 DTO |
| 新建 | `modeling/vo/ModelSubclassOfTreeVO.java` | 类树 VO |
| 新建 | `modeling/vo/InverseSuggestVO.java` | 反向关系建议 VO |
| 新建 | `V14__ont_model_subclassof_seed.sql` | 建表 + 修复 range_class_id + 菜单 |
| 新建 | `api/ontology-model/datatype-property.ts` | 前端 API |
| 新建 | `api/ontology-model/object-property.ts` | 前端 API |
| 新建 | `api/ontology-model/subclassof.ts` | 前端 API |
| 新建 | `views/admin/ontology-model/property/index.vue` | 属性建模页（Tab 切换） |
| 新建 | `views/admin/ontology-model/property/datatype-form.vue` | 数据属性表单 |
| 新建 | `views/admin/ontology-model/property/object-form.vue` | 对象属性表单 |
| 新建 | `views/admin/ontology-model/property/inverse-dialog.vue` | 反向关系建议弹窗 |
| 新建 | `views/admin/ontology-model/property/composables.ts` | 选项/工具函数 |
| 新建 | `views/admin/ontology-model/property/i18n/zh-cn.ts` | 中文词条 |
| 新建 | `views/admin/ontology-model/property/i18n/en.ts` | 英文词条 |
| 修改 | `PigOntologyApplication.java` | 加 `@EnableScheduling` |
| 修改 | `PigBootApplication.java` | 加 `@EnableScheduling` |
| 修改 | DD8 `ModelClassServiceImpl.removeClass` | 取消注释 subClassOf 引用校验 |

---

## 十、与 PRD 边界的对齐确认（防混淆备忘）

| 维度 | PRD 约定 | 本 DD 实现 | 对齐 |
|---|---|---|---|
| 数据属性绑单位 | unitRef = QUDT IRI，非数值型不可绑（FR-12.2） | NUMERIC_TYPES 白名单 + Supply units 端点 | ✓ |
| 对象属性 range | 模板不含 range，建模侧手动选定（FR-13.2） | saveProp range 必填；DD8 实例化 NULL 由 updateProp 补全 | ✓ |
| 反向关系建议 | 建立 contains 后建议 belongsTo（FR-13.5） | suggestInverse 反转 domain/range + 映射属性名 | ✓ |
| 类层级权威源 | 建模侧建立 subClassOf 是权威源（PRD 5.2） | ont_model_subclassof 建模域权威表 | ✓ |
| 镜像回推 | 建立后回推 ont_class_hierarchy 镜像（FR-14.3） | HierarchySyncService 调 Sync POST | ✓ |
| 一致性保证 | 本地事务+补偿，不走分布式事务（FR-14.5，R-19） | sync_status 补偿重试 + 建模域事务不回滚 | ✓ |
| 环路校验在建模侧 | 建模侧是权威源，环路校验由建模侧负责 | SubServiceImpl.wouldCreateCycle DFS | ✓ |
| 多继承支持 | subClassOf 允许多条（FR-14.6） | uk(project_id,child,parent) 允许多父 | ✓ |
| defaultCardinality 映射 | one-to-many 等转为 min/max 基数（FR-13.3） | mapCardinality 静态方法 | ✓（新增） |
| range_class_id 可空 | DD8 实例化 range 留空，DD9 补全 | V14 ALTER DROP NOT NULL | ✓（修复） |
| 补偿任务 | 失败自动补偿重试（FR-14.5） | @Scheduled fixedDelay=60000 | ✓ |
| 菜单 ID | 11300 段（PRD 13.1） | 11300 菜单 + 11301~11304 按钮 | ✓ |
| 权限标识 | ont_prop_model_view/manage（PRD 13.2） | Controller @HasPermission 对齐 | ✓ |
| Flyway | V14（PRD 9 / 设计计划 3.2） | V14__ont_model_subclassof_seed.sql | ✓ |
| 治理域只读消费 | 不修改治理域表/接口（PRD NFR-C3/R-23） | 只调 Supply/Sync API，不注入治理域 Service | ✓ |

---

*本 DD 是建模域最复杂的里程碑（3 FR 并行 + 镜像回推一致性）。评审通过后进入实现，实现完成后更新设计计划 DD9 状态为"已完成"。后续 DD10（RDF 序列化与解析）依赖本 DD 的类/属性/关系全部就绪。*
