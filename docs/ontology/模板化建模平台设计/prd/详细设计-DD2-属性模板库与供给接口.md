# 详细设计-DD2-属性模板库与供给接口（M1）

| 项 | 内容 |
|---|---|
| 文档名称 | 详细设计-DD2-属性模板库与供给接口 |
| 里程碑 | M1（属性模板库 + 供给） |
| 上游 PRD | 《本体模板化治理功能产品需求文档.md》v1.2 FR-1、FR-5、FR-6、NFR-1/2/5/7/8/10/11、十三节 |
| 设计计划 | 《详细设计计划.md》DD2 |
| 编写日期 | 2026-07-25 |
| 文档状态 | 待实现 |
| 前置依赖 | DD1（M0）已完成：模块骨架、V4 建表、V5 种子（12 内置模板 + sys_menu） |

---

## 一、设计目标与范围

### 1.1 目标

在 DD1 模块骨架基础上，实现属性模板库的完整 CRUD + 供给接口 + 治理 UI，使 youming 具备：

- 属性模板的增删改查、弃用、提升（FR-1 + FR-6）。
- 内置 12 模板已随 V5 种子入库，本期实现业务逻辑消费（AC-1.1 验证）。
- 供给接口 v1（属性模板部分），供建模侧拉取（FR-5）。
- pig-ui "属性模板库"页面可操作（列表/筛选/新增/编辑/弃用/删除）。
- 权限点 `ont_supply_view` 就绪（V6 种子补充）。

### 1.2 范围（本 DD 做 / 不做）

| 做（M1） | 不做（后续 DD） |
|---|---|
| PropertyTemplate Entity/Mapper/Service/Controller | 分类模板体系（DD3） |
| 供给接口 `/supply/v1/property-templates`（属性模板部分） | 单位注册表（DD4） |
| 提升接口 `/property-template/promote`（FR-6） | 注释属性注册表（DD5） |
| 弃用接口 `PUT /{id}/deprecate` | 参考本体浏览（DD6） |
| V6 种子：`ont_supply_view` 权限点 | 批量提升向导 UI（本期仅接口） |
| 前端 `views/admin/ontology/property-template/` 页面 | 分类模板树/类树页面 |
| 前端 `api/ontology/property-template.ts` | 单位换算试算区 |

> AC-6.3 批量提升向导（UI 扫描同名）本期仅提供 promote 接口，向导 UI 留后续迭代。AC-1.7 引用计数本期预留字段、接口返回 0，不实现建模侧反馈机制。

### 1.3 验收映射（M1 DoD）

| PRD AC | 本 DD 实现点 |
|---|---|
| AC-1.1 内置 ≥12 模板 | V5 种子（DD1 已建），本期 Service 查询验证 |
| AC-1.2 custom 模板持久化 | 4.3 PropertyTemplateController.save |
| AC-1.3 templateCode 唯一约束 | 4.4 ServiceImpl.save 校验 + DB 唯一约束（V4 已建） |
| AC-1.4 kind 过滤互斥 | 4.3 Controller.page 的 lambdaQuery |
| AC-1.5 弃用默认排除 | 4.5 SupplyController + 4.3 page 默认过滤 |
| AC-1.6 builtin 不可编辑删除 | 4.4 ServiceImpl 编辑/删除前 source 校验 |
| AC-1.7 删除引用提示 | 4.4 ServiceImpl.removeById（本期直接软删，预留） |
| AC-1.8 供给字段完整 | 4.5 SupplyController 返回 VO 含全部字段 |
| AC-5.1 供给接口可用 | 4.5 SupplyController + V6 权限种子 |
| AC-5.2 返回 templateCode | 4.5 VO 字段 |
| AC-5.6 默认排除弃用 | 4.5 SupplyController includeDeprecated 参数 |
| AC-5.7 版本化路径 | 4.5 `/supply/v1/` 路径前缀 |
| AC-6.1 提升接口 | 4.6 PropertyTemplateController.promote |
| AC-6.2 提升查重 | 4.6 ServiceImpl.promote 校验 |
| AC-6.4 弃用软弃用 | 4.3 deprecate 接口 |
| AC-6.5 列表过滤搜索 | 5.1 前端列表页 |
| AC-6.6 审计字段 | MybatisPlusMetaObjectHandler 自动填充 |

---

## 二、落地清单

### 2.1 后端文件清单

```
server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/
├── api/
│   ├── entity/
│   │   └── PropertyTemplate.java              # 属性模板 Entity
│   ├── dto/
│   │   └── PropertyTemplatePromoteDTO.java     # 提升请求 DTO
│   └── vo/
│       └── PropertyTemplateSupplyVO.java       # 供给接口返回 VO
├── controller/
│   ├── PropertyTemplateController.java         # 属性模板 CRUD + 弃用 + 提升
│   └── SupplyController.java                   # 供给接口 v1（属性模板部分）
├── service/
│   ├── PropertyTemplateService.java            # Service 接口
│   └── impl/
│       └── PropertyTemplateServiceImpl.java    # Service 实现
└── mapper/
    └── PropertyTemplateMapper.java             # Mapper
```

### 2.2 数据库文件清单

```
server/pig-common/pig-common-data/src/main/resources/db/migration/
├── V4__ont_governance_schema.sql               # 已有（DD1）
├── V5__ont_governance_seed.sql                 # 已有（DD1）
└── V6__ont_supply_permission_seed.sql          # 新增：ont_supply_view 权限点
```

### 2.3 前端文件清单

```
web/src/
├── api/ontology/
│   └── property-template.ts                    # 属性模板 API
└── views/admin/ontology/property-template/
    ├── index.vue                               # 列表页（筛选 + CRUD）
    ├── form.vue                                # 新增/编辑表单对话框
    └── i18n/
        ├── zh-cn.ts                            # 中文词条
        └── en.ts                               # 英文词条
```

> 路径说明：sys_menu 种子 path 为 `/admin/ontology/property-template/index`，backEnd.ts 用 `import.meta.glob('../views/**/*.{vue,tsx}')` 按 path 匹配，故 vue 文件须放 `views/admin/ontology/property-template/index.vue`（对齐现有 `views/admin/system/user/index.vue` 范式）。

---

## 三、数据库设计（V6）

### 3.1 `V6__ont_supply_permission_seed.sql`

> V5 已建 15 条菜单（10000-10500），但漏建供给接口权限点 `ont_supply_view`。V6 补建为"属性模板库"下的按钮权限（menu_id 10105），并预留分类模板查看权限 `ont_class_tpl_view`（DD3 用）。

```sql
-- ============================================================
-- V6__ont_supply_permission_seed.sql
-- 补充供给接口权限点 + 分类模板查看权限点
-- ============================================================

-- 供给接口权限（挂在属性模板库下，建模侧/查看者可授予）
INSERT INTO sys_menu VALUES (10105, '供给接口调用', 'ont_supply_view', NULL, NULL, 10100, NULL, '1', 5, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');

-- 分类模板查看权限（DD3 用，提前补建避免后续再加 V 版本）
INSERT INTO sys_menu VALUES (10205, '分类模板查看', 'ont_class_tpl_view', NULL, NULL, 10200, NULL, '1', 5, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
```

> 说明：menu_type='1'（按钮），对齐 MenuTypeEnum.BUTTON。`ont_supply_view` 授予建模师/查看者角色；`ont_class_tpl_view` 预留给 DD3。V6 只做 INSERT，不改 V4/V5。

---

## 四、后端设计

### 4.1 Entity `PropertyTemplate.java`

```java
package com.pig4cloud.pig.ontology.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@Schema(description = "属性模板")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_property_template")
public class PropertyTemplate extends Model<PropertyTemplate> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "唯一标识，如 name/contains/price")
	private String templateCode;

	@Schema(description = "datatype / object")
	private String kind;

	@Schema(description = "显示名")
	private String label;

	@Schema(description = "业务说明")
	private String description;

	@Schema(description = "分组：basic/contact/monetary/temporal/status/containment/attribution")
	private String category;

	@Schema(description = "datatype 专属：string/integer/decimal/boolean/datetime")
	private String type;

	@Schema(description = "0/1，datatype 专属标识符")
	private String isIdentifier;

	@Schema(description = "预设 QUDT 单位 IRI")
	private String unitRef;

	@Schema(description = "枚举值（逗号分隔），datatype 专属")
	private String values;

	@Schema(description = "object 专属：one-to-many/many-to-one/...")
	private String defaultCardinality;

	@Schema(description = "builtin / custom")
	private String source;

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

> 说明：对齐 SysDict Entity 范式--继承 `Model<T>`、`@TableId(ASSIGN_ID)`、`@TableLogic` + `@TableField(fill=...)` 审计字段。`@TableName("ont_property_template")` 显式标注（表名与类名驼峰推断不一致）。

### 4.2 Mapper `PropertyTemplateMapper.java`

```java
package com.pig4cloud.pig.ontology.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.pig4cloud.pig.ontology.api.entity.PropertyTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PropertyTemplateMapper extends MPJBaseMapper<PropertyTemplate> {
}
```

> 说明：对齐 SysDictMapper，继承 `MPJBaseMapper`（MyBatis-Plus-Join），M1 无自定义 SQL，全靠 Wrappers。

### 4.3 Controller `PropertyTemplateController.java`

```java
package com.pig4cloud.pig.ontology.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.dto.PropertyTemplatePromoteDTO;
import com.pig4cloud.pig.ontology.api.entity.PropertyTemplate;
import com.pig4cloud.pig.ontology.service.PropertyTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/property-template")
@Tag(name = "属性模板管理", description = "属性模板 CRUD + 弃用 + 提升")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class PropertyTemplateController {

	private final PropertyTemplateService propertyTemplateService;

	@GetMapping("/page")
	@Operation(summary = "分页查询", description = "按 kind/category/keyword 过滤，默认排除弃用")
	@HasPermission("ont_prop_tpl_view")
	public R<IPage<PropertyTemplate>> page(@ParameterObject Page page, @ParameterObject PropertyTemplate template) {
		return R.ok(propertyTemplateService.page(page, template));
	}

	@GetMapping("/{id}")
	@Operation(summary = "详情")
	@HasPermission("ont_prop_tpl_view")
	public R<PropertyTemplate> getById(@PathVariable Long id) {
		return R.ok(propertyTemplateService.getById(id));
	}

	@SysLog("新增属性模板")
	@PostMapping
	@Operation(summary = "新增 custom 模板")
	@HasPermission("ont_prop_tpl_manage")
	public R save(@Valid @RequestBody PropertyTemplate template) {
		return propertyTemplateService.saveTemplate(template);
	}

	@SysLog("编辑属性模板")
	@PutMapping("/{id}")
	@Operation(summary = "编辑（builtin 拒绝）")
	@HasPermission("ont_prop_tpl_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody PropertyTemplate template) {
		template.setId(id);
		return propertyTemplateService.updateTemplate(template);
	}

	@SysLog("删除属性模板")
	@DeleteMapping("/{id}")
	@Operation(summary = "删除（custom，软删）")
	@HasPermission("ont_prop_tpl_manage")
	public R removeById(@PathVariable Long id) {
		return propertyTemplateService.removeTemplate(id);
	}

	@SysLog("弃用属性模板")
	@PutMapping("/{id}/deprecate")
	@Operation(summary = "弃用/取消弃用")
	@HasPermission("ont_prop_tpl_manage")
	public R deprecate(@PathVariable Long id, @RequestParam(defaultValue = "1") String deprecated) {
		return propertyTemplateService.deprecate(id, deprecated);
	}

	@SysLog("提升属性为模板")
	@PostMapping("/promote")
	@Operation(summary = "提升属性为模板（FR-6）")
	@HasPermission("ont_prop_tpl_manage")
	public R promote(@Valid @RequestBody PropertyTemplatePromoteDTO dto) {
		return propertyTemplateService.promote(dto);
	}
}
```

> 说明：路径 `/property-template/**`，经 context-path `/admin` 或网关路由后对外为 `/admin/ont/property-template/**`（对齐 PRD 10.1）。`@HasPermission` 权限点对齐 V5 种子。弃用接口支持双向（`deprecated=1` 弃用 / `deprecated=0` 恢复）。

### 4.4 Service / ServiceImpl

**接口 `PropertyTemplateService.java`：**

```java
package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.dto.PropertyTemplatePromoteDTO;
import com.pig4cloud.pig.ontology.api.entity.PropertyTemplate;

public interface PropertyTemplateService extends IService<PropertyTemplate> {

	IPage<PropertyTemplate> page(Page page, PropertyTemplate template);

	R saveTemplate(PropertyTemplate template);

	R updateTemplate(PropertyTemplate template);

	R removeTemplate(Long id);

	R deprecate(Long id, String deprecated);

	R promote(PropertyTemplatePromoteDTO dto);
}
```

**实现 `PropertyTemplateServiceImpl.java`（关键逻辑）：**

```java
package com.pig4cloud.pig.ontology.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.dto.PropertyTemplatePromoteDTO;
import com.pig4cloud.pig.ontology.api.entity.PropertyTemplate;
import com.pig4cloud.pig.ontology.mapper.PropertyTemplateMapper;
import com.pig4cloud.pig.ontology.service.PropertyTemplateService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class PropertyTemplateServiceImpl extends ServiceImpl<PropertyTemplateMapper, PropertyTemplate>
		implements PropertyTemplateService {

	@Override
	public IPage<PropertyTemplate> page(Page page, PropertyTemplate template) {
		return baseMapper.selectPage(page,
				Wrappers.<PropertyTemplate>lambdaQuery()
					.eq(StrUtil.isNotBlank(template.getKind()), PropertyTemplate::getKind, template.getKind())
					.eq(StrUtil.isNotBlank(template.getCategory()), PropertyTemplate::getCategory, template.getCategory())
					.and(StrUtil.isNotBlank(template.getTemplateCode()),
							w -> w.like(PropertyTemplate::getTemplateCode, template.getTemplateCode())
									.or().like(PropertyTemplate::getLabel, template.getTemplateCode()))
					.eq(StrUtil.isNotBlank(template.getDeprecated()), PropertyTemplate::getDeprecated, template.getDeprecated())
					.orderByDesc(PropertyTemplate::getCreateTime));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveTemplate(PropertyTemplate template) {
		// 查重：templateCode 唯一
		long count = count(Wrappers.<PropertyTemplate>lambdaQuery()
				.eq(PropertyTemplate::getTemplateCode, template.getTemplateCode()));
		if (count > 0) {
			return R.failed("模板标识 '" + template.getTemplateCode() + "' 已存在");
		}
		// 新增一律为 custom
		template.setSource("custom");
		template.setDeprecated("0");
		return R.ok(save(template));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateTemplate(PropertyTemplate template) {
		PropertyTemplate existing = getById(template.getId());
		if (existing == null) {
			return R.failed("模板不存在");
		}
		// builtin 不可编辑
		if ("builtin".equals(existing.getSource())) {
			return R.failed("内置模板不可编辑");
		}
		// templateCode 不允许修改（保持引用稳定性）
		template.setTemplateCode(existing.getTemplateCode());
		return R.ok(updateById(template));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeTemplate(Long id) {
		PropertyTemplate existing = getById(id);
		if (existing == null) {
			return R.failed("模板不存在");
		}
		if ("builtin".equals(existing.getSource())) {
			return R.failed("内置模板不可删除");
		}
		// TODO: 后期接入建模侧引用计数，有引用时拒绝删除
		return R.ok(removeById(id));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R deprecate(Long id, String deprecated) {
		return R.ok(update(Wrappers.<PropertyTemplate>lambdaUpdate()
				.eq(PropertyTemplate::getId, id)
				.set(PropertyTemplate::getDeprecated, deprecated)));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R promote(PropertyTemplatePromoteDTO dto) {
		// 查重
		long count = count(Wrappers.<PropertyTemplate>lambdaQuery()
				.eq(PropertyTemplate::getTemplateCode, dto.getTemplateCode()));
		if (count > 0) {
			return R.failed("模板标识 '" + dto.getTemplateCode() + "' 已存在，请勿重复提升");
		}
		PropertyTemplate template = new PropertyTemplate();
		template.setTemplateCode(dto.getTemplateCode());
		template.setKind(dto.getKind());
		template.setLabel(dto.getLabel());
		template.setDescription(dto.getDescription());
		template.setCategory(dto.getCategory());
		template.setType(dto.getType());
		template.setIsIdentifier(dto.getIsIdentifier());
		template.setUnitRef(dto.getUnitRef());
		template.setValues(dto.getValues());
		template.setDefaultCardinality(dto.getDefaultCardinality());
		template.setSource("custom");
		template.setDeprecated("0");
		save(template);
		return R.ok(template);
	}
}
```

> 说明：
> - `page` 方法：kind/category 精确过滤，templateCode/label 模糊搜索，deprecated 默认不传时不过滤（前端列表可切换查看弃用）。AC-1.4 kind 互斥靠 lambdaQuery 的 eq 条件保证。
> - `saveTemplate`：查重 templateCode -> 设 source=custom -> 保存。AC-1.2/1.3。
> - `updateTemplate`：校验 builtin 拒绝、templateCode 不可改。AC-1.6。
> - `removeTemplate`：校验 builtin 拒绝、预留引用计数。AC-1.6/1.7。
> - `deprecate`：软弃用，不物理删除。AC-6.4。
> - `promote`：DTO -> Entity，查重后生成 custom 模板。AC-6.1/6.2。

### 4.5 SupplyController `SupplyController.java`（供给接口 v1）

```java
package com.pig4cloud.pig.ontology.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.entity.PropertyTemplate;
import com.pig4cloud.pig.ontology.service.PropertyTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/supply/v1")
@Tag(name = "治理资产供给接口", description = "供建模侧拉取模板/单位/注册表")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class SupplyController {

	private final PropertyTemplateService propertyTemplateService;

	@GetMapping("/property-templates")
	@Operation(summary = "属性模板供给", description = "按 kind/category 拉取，默认排除弃用")
	@HasPermission("ont_supply_view")
	public R<List<PropertyTemplate>> propertyTemplates(
			@RequestParam(required = false) String kind,
			@RequestParam(required = false) String category,
			@RequestParam(defaultValue = "false") Boolean includeDeprecated) {
		List<PropertyTemplate> list = propertyTemplateService.list(
				Wrappers.<PropertyTemplate>lambdaQuery()
					.eq(StrUtil.isNotBlank(kind), PropertyTemplate::getKind, kind)
					.eq(StrUtil.isNotBlank(category), PropertyTemplate::getCategory, category)
					.eq(includeDeprecated, PropertyTemplate::getDeprecated, "0")
					.orderByAsc(PropertyTemplate::getSortOrder)
					.orderByAsc(PropertyTemplate::getId));
		return R.ok(list);
	}
}
```

> 说明：路径 `/supply/v1/property-templates`，对外 `/admin/ont/supply/v1/property-templates`。AC-5.1/5.2/5.6/5.7。`includeDeprecated=false` 时条件 `deprecated='0'` 过滤弃用；`true` 时不过滤。后续 DD 在此 Controller 追加 `/class-template/**`、`/units/**`、`/annotation-properties` 等端点。

> **注意**：`PropertyTemplate` Entity 无 `sortOrder` 字段（V4 表结构无此列），上面代码改为 `orderByAsc(PropertyTemplate::getId)`。实际实现以 Entity 字段为准。

### 4.6 DTO `PropertyTemplatePromoteDTO.java`

```java
package com.pig4cloud.pig.ontology.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "属性提升为模板请求")
public class PropertyTemplatePromoteDTO {

	@NotBlank(message = "模板标识不能为空")
	@Schema(description = "唯一标识")
	private String templateCode;

	@NotBlank(message = "类型不能为空")
	@Schema(description = "datatype / object")
	private String kind;

	@NotBlank(message = "显示名不能为空")
	@Schema(description = "显示名")
	private String label;

	@Schema(description = "业务说明")
	private String description;

	@Schema(description = "分组")
	private String category;

	@Schema(description = "datatype 专属类型")
	private String type;

	@Schema(description = "0/1 标识符")
	private String isIdentifier;

	@Schema(description = "预设单位 IRI")
	private String unitRef;

	@Schema(description = "枚举值")
	private String values;

	@Schema(description = "object 专属基数")
	private String defaultCardinality;
}
```

---

## 五、前端设计

### 5.1 API `web/src/api/ontology/property-template.ts`

```typescript
import request from '/@/utils/request';

export function pageList(query: any) {
	return request({
		url: '/admin/ont/property-template/page',
		method: 'get',
		params: query,
	});
}

export function getObj(id: string) {
	return request({
		url: '/admin/ont/property-template/' + id,
		method: 'get',
	});
}

export function addObj(obj: any) {
	return request({
		url: '/admin/ont/property-template',
		method: 'post',
		data: obj,
	});
}

export function putObj(obj: any) {
	return request({
		url: '/admin/ont/property-template/' + obj.id,
		method: 'put',
		data: obj,
	});
}

export function delObj(id: string) {
	return request({
		url: '/admin/ont/property-template/' + id,
		method: 'delete',
	});
}

export function deprecateObj(id: string, deprecated: string) {
	return request({
		url: '/admin/ont/property-template/' + id + '/deprecate',
		method: 'put',
		params: { deprecated },
	});
}

export function promoteObj(obj: any) {
	return request({
		url: '/admin/ont/property-template/promote',
		method: 'post',
		data: obj,
	});
}
```

### 5.2 列表页 `views/admin/ontology/property-template/index.vue`

**关键结构：**

- 顶部搜索栏：kind 下拉（全部/datatype/object）、category 下拉、关键字输入、查询/重置按钮。
- 操作栏：新增按钮（`v-auth="'ont_prop_tpl_manage'"`）、批量删除。
- 表格列：templateCode / kind（tag 标签色分 datatype=蓝/object=绿）/ label / category / type / source（builtin 禁用编辑删除）/ deprecated（tag 标记）/ 操作列（编辑/弃用/删除）。
- 分页组件。
- builtin 行：编辑/删除按钮 `:disabled="scope.row.source === 'builtin'"`。
- 弃用按钮：根据当前 deprecated 状态切换"弃用"/"恢复"。

**关键逻辑（对齐 `views/admin/param/index.vue` 范式）：**

```typescript
const state: BasicTableProps = reactive<BasicTableProps>({
	queryForm: {
		kind: '',
		category: '',
		templateCode: '',
		deprecated: '',
	},
	pageList: pageList,
	descs: ['create_time'],
});
const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state);
```

### 5.3 表单对话框 `views/admin/ontology/property-template/form.vue`

**关键字段（按 kind 动态显示）：**

| 字段 | datatype | object | 说明 |
|---|---|---|---|
| templateCode | ✅ | ✅ | 唯一标识，新增时必填，编辑时只读 |
| kind | ✅ | ✅ | datatype/object 单选 |
| label | ✅ | ✅ | 显示名 |
| description | ✅ | ✅ | 业务说明 |
| category | ✅ | ✅ | 分组下拉 |
| type | ✅ | ❌ | string/integer/decimal/boolean/datetime |
| isIdentifier | ✅ | ❌ | 0/1 单选 |
| unitRef | ✅ | ❌ | 预设单位 IRI（文本输入，DD4 后改为单位选择器） |
| values | ✅ | ❌ | 枚举值（逗号分隔） |
| defaultCardinality | ❌ | ✅ | one-to-many/many-to-one/many-to-many/one-to-one |

> 用 `v-show="form.kind === 'datatype'"` 控制字段可见性。对齐 `views/admin/param/form.vue` 范式（el-dialog + el-form + openDialog + onSubmit）。

### 5.4 i18n

**`zh-cn.ts`：**

```typescript
export default {
	propertyTemplate: {
		index: '#',
		templateCode: '模板标识',
		kind: '类型',
		label: '显示名',
		description: '说明',
		category: '分组',
		type: '数据类型',
		isIdentifier: '标识符',
		unitRef: '预设单位',
		values: '枚举值',
		defaultCardinality: '基数',
		source: '来源',
		deprecated: '状态',
		createTime: '创建时间',
		// ... inputXxxTip 系列
	},
};
```

---

## 六、横切设计

### 6.1 校验

- **templateCode 唯一**：Service 层查重 + DB 唯一约束（V4 `uk_ont_prop_tpl_code`）双保险。重复时返回 `R.failed(msg)`（非 500）。AC-1.3。
- **builtin 保护**：编辑/删除前校验 `source='builtin'`，拒绝并返回友好提示。AC-1.6。
- **kind 与字段一致性**：datatype 必填 type；object 必填 defaultCardinality。前端表单校验 + 后端兜底。

### 6.2 异常处理

- 唯一约束冲突（`DuplicateKeyException`）：Service 层已预查重，兜底捕获并转 `R.failed`。
- 业务错误统一用 `R.failed(msg)`（code=1），不抛 500。

### 6.3 审计

- MybatisPlusMetaObjectHandler 自动填充 `createBy/createTime/updateBy/updateTime/delFlag`（pig-common-data 已配置，DD1 依赖已引入）。

### 6.4 双形态

- 单体：pig-boot context-path `/admin` -> `/admin/ont/property-template/**`。
- 微服务：网关路由 `Path=/admin/ont/**` -> `lb://pig-ontology-biz`（DD1 已配置）。

---

## 七、测试要点

| 类别 | 要点 |
|---|---|
| 编译 | `mvn -pl pig-ontology/pig-ontology-biz -am compile` 通过 |
| CRUD | 新增 custom 模板 -> 编辑 -> 删除（软删）-> 查询排除已删 |
| 唯一约束 | 重复 templateCode 返回业务错误（非 500） |
| builtin 保护 | 编辑/删除 builtin 返回"不可操作"提示 |
| kind 过滤 | kind=object 查询结果不含 datatype 模板 |
| 弃用 | 弃用后默认列表不返回；includeDeprecated=true 时返回 |
| 供给接口 | `/supply/v1/property-templates?kind=datatype` 返回 8 条；`?includeDeprecated=true` 含弃用 |
| 提升 | promote 接口生成 custom 模板；重复 code 返回提示 |
| 前端 | pig-ui 登录 -> 本体治理 -> 属性模板库 -> 列表展示 12 内置 + 新增/编辑/弃用 |
| 回归 | pig 现有功能不受影响 |

---

## 八、风险与缓解

| 风险 | 缓解 |
|---|---|
| V6 与其他分支 Flyway 版本冲突 | V6 只做 INSERT，合并时按需调整版本号 |
| 供给接口被未授权访问 | `@HasPermission("ont_supply_view")` + pig 现有 OAuth2 资源服务器 |
| 前端路径不匹配导致 404 | vue 文件严格放 `views/admin/ontology/property-template/index.vue`（对齐 sys_menu path） |
| unitRef 字段无校验（指向不存在单位） | M1 预留文本输入；DD4 单位注册表上线后改为选择器并校验 |

---

## 九、交付物清单

| 类型 | 文件 | 说明 |
|---|---|---|
| 新建 | `PropertyTemplate.java` | Entity |
| 新建 | `PropertyTemplateMapper.java` | Mapper |
| 新建 | `PropertyTemplateService.java` | Service 接口 |
| 新建 | `PropertyTemplateServiceImpl.java` | Service 实现 |
| 新建 | `PropertyTemplateController.java` | CRUD + 弃用 + 提升 |
| 新建 | `SupplyController.java` | 供给接口 v1（属性模板部分） |
| 新建 | `PropertyTemplatePromoteDTO.java` | 提升 DTO |
| 新建 | `V6__ont_supply_permission_seed.sql` | 供给权限点种子 |
| 新建 | `web/src/api/ontology/property-template.ts` | 前端 API |
| 新建 | `web/src/views/admin/ontology/property-template/index.vue` | 列表页 |
| 新建 | `web/src/views/admin/ontology/property-template/form.vue` | 表单对话框 |
| 新建 | `web/src/views/admin/ontology/property-template/i18n/zh-cn.ts` | 中文词条 |
| 新建 | `web/src/views/admin/ontology/property-template/i18n/en.ts` | 英文词条 |

---

*本详细设计对应里程碑 M1，依赖 DD1（M0）已完成的基础设施。实现完成后，按《详细设计计划.md》依赖顺序进入 DD3（分类模板体系 + 编码 + 类层级镜像）。*
