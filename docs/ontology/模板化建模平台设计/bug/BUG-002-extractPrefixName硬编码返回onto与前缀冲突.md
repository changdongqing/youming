# BUG-002: extractPrefixName 硬编码返回 "onto"，与注释属性命名空间前缀冲突

| 项目 | 内容 |
|------|------|
| **编号** | BUG-002 |
| **严重级别** | 中（Medium） |
| **影响域** | 建模域 — RDF 序列化（FR-15） / 前缀管理（FR-10.2） |
| **发现日期** | 2026-07-29 |
| **发现场景** | 组织域本体建模（V17 种子数据验证） |
| **状态** | 待修复（Open） |

## 一、问题描述

### 现象

`SerializationService` 中的 `extractPrefixName` 方法硬编码返回字符串 `"onto"`，忽略了传入的 `nsBase` 和 `projectId` 参数。这导致：

1. **项目命名空间前缀固定为 `onto`**，而非从 `ont_model_prefix` 表中读取用户自定义的项目前缀名。
2. **与注释属性命名空间前缀冲突**：`buildModel` 第 140 行已将 `ont` 前缀绑定到注释属性命名空间 `http://youming.com/ontology/annotation#`，而第 148 行又用 `extractPrefixName` 返回的 `"onto"` 绑定到项目命名空间。虽然 `onto` ≠ `ont`（差一个字母），但这只是巧合避免了直接覆盖，而非有意设计。

### 根因

```java
// SerializationService.java 第 301-303 行
private String extractPrefixName(String nsBase, Long projectId) {
    return "onto";   // 硬编码，忽略 nsBase / projectId
}
```

方法签名暗示要根据 `nsBase` 和 `projectId` 提取前缀名，但实现完全忽略参数，硬编码返回 `"onto"`。

### 应有行为

应从 `ont_model_prefix` 表中查询该项目的 **`is_default='1'` 前缀**（V17 种子数据中为 `ex`），用其 `prefix` 字段值作为项目命名空间前缀名。

## 二、证据链

### 证据 1：extractPrefixName 硬编码

`SerializationService.java` 第 301-303 行：

```java
private String extractPrefixName(String nsBase, Long projectId) {
    return "onto";
}
```

### 证据 2：调用处传入参数但未被使用

第 146-148 行：

```java
ModelProject project = modelProjectMapper.selectById(projectId);
String nsBase = project.getNamespaceBase();
model.setNsPrefix(extractPrefixName(nsBase, projectId), nsBase);
```

`nsBase` 和 `projectId` 被传入但完全被忽略。

### 证据 3：与注释属性前缀的潜在冲突

第 136-140 行已注册的标准前缀：

```java
model.setNsPrefix("rdf", RDF.getURI());
model.setNsPrefix("rdfs", RDFS.getURI());
model.setNsPrefix("owl", OWL.getURI());
model.setNsPrefix("xsd", XSD.getURI());
model.setNsPrefix("ont", ONT_NS);   // ont -> http://youming.com/ontology/annotation#
```

第 148 行用 `extractPrefixName` 返回的 `"onto"` 注册项目前缀：

```java
model.setNsPrefix("onto", nsBase);  // onto -> http://youming.com/ontology/organization#
```

`onto` 和 `ont` 仅差一个字母，在 Turtle 输出中容易混淆：
- `ont:icon` → 注释属性（`http://youming.com/ontology/annotation#icon`）
- `onto:User` → 项目类（`http://youming.com/ontology/organization#User`）

### 证据 4：ont_model_prefix 表的 is_default 前缀从未被读取

全代码库中无任何 `eq(ModelPrefix::getIsDefault, "1")` 的查询。V17 种子数据中注册了 `ex`（is_default='1'）、`ont`、`xsd` 三个前缀，但序列化时完全没有读取该表。

### 证据 5：ont_model_prefix 表设计意图

`V12__ont_model_project_seed.sql` 第 56-60 行：

```sql
COMMENT ON COLUMN ont_model_prefix.prefix      IS '前缀名，如 ex / qudt / brick（NCName 规范）';
COMMENT ON COLUMN ont_model_prefix.namespace   IS '命名空间 URI，如 http://example.com/onto/';
COMMENT ON COLUMN ont_model_prefix.is_default   IS '1=项目默认前缀（类/属性 IRI 基址来源）';
```

`is_default='1'` 的前缀明确标注为「类/属性 IRI 基址来源」，但序列化代码未消费此字段。

## 三、影响范围

| 影响面 | 说明 |
|-------|------|
| **序列化输出前缀名固定** | 所有项目序列化输出中，项目命名空间前缀固定为 `onto`，无法自定义 |
| **前缀管理功能形同虚设** | 用户在「本体项目管理 → 前缀管理」中配置的 `ex`/`qudt`/`brick` 等前缀，在序列化时完全不被使用 |
| **前缀名混淆风险** | `onto`（项目）与 `ont`（注释属性）高度相似，Turtle 输出中易混淆 |
| **多前缀未声明** | 项目可能注册多个前缀（如 `ex` + `qudt` + `brick`），序列化时只声明了一个硬编码前缀，其他前缀对应的命名空间未在 RDF 输出中声明 |

## 四、修复建议

### 修复方案：从 ont_model_prefix 表读取项目前缀

在 `SerializationService.buildModel` 中，替换第 148 行的硬编码调用：

```java
// 修复前（第 148 行）：
model.setNsPrefix(extractPrefixName(nsBase, projectId), nsBase);

// 修复后：从 ont_model_prefix 表读取该项目所有前缀
List<ModelPrefix> prefixes = modelPrefixMapper.selectList(
    Wrappers.<ModelPrefix>lambdaQuery().eq(ModelPrefix::getProjectId, projectId));
for (ModelPrefix pf : prefixes) {
    model.setNsPrefix(pf.getPrefix(), pf.getNamespace());
}
// 若无前缀注册，回退用 nsBase 注册默认前缀
if (prefixes.isEmpty()) {
    model.setNsPrefix("ex", nsBase);
}
```

同时删除无用的 `extractPrefixName` 方法。

需注入 `ModelPrefixMapper`：

```java
private final ModelPrefixMapper modelPrefixMapper;
```

### 注意事项

- 如果 `ont_model_prefix` 表中注册了与标准前缀冲突的前缀名（如 `rdf`/`rdfs`/`owl`/`xsd`/`ont`），`setNsPrefix` 会覆盖第 136-140 行的标准前缀声明。应在循环中跳过这些保留前缀名，或按先标准后项目的注册顺序让项目前缀覆盖标准前缀（需评估是否合理）。

- 建议在循环中添加保留前缀检查：

```java
private static final Set<String> RESERVED_PREFIXES = Set.of("rdf", "rdfs", "owl", "xsd", "ont");

for (ModelPrefix pf : prefixes) {
    if (!RESERVED_PREFIXES.contains(pf.getPrefix())) {
        model.setNsPrefix(pf.getPrefix(), pf.getNamespace());
    }
}
```

## 五、关联文件

| 文件 | 行号 | 说明 |
|------|-----|------|
| `server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/modeling/service/SerializationService.java` | 148, 301-303 | **Bug 发生点**：硬编码 extractPrefixName 返回 "onto" |
| `server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/modeling/entity/ModelPrefix.java` | 42 | `isDefault` 字段定义 |
| `server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/modeling/service/impl/ModelPrefixServiceImpl.java` | 36, 54-55 | is_default 仅用于排序和默认值，未被序列化消费 |
| `server/pig-common/pig-common-data/src/main/resources/db/migration/V12__ont_model_project_seed.sql` | 56-60 | DDL 注释：is_default 标注为「类/属性 IRI 基址来源」 |

## 六、验证方法

修复后，对 V17 组织域本体种子数据执行序列化预览，确认输出中：

1. 项目前缀为 `ex`（而非 `onto`）：`ex:User` → `http://youming.com/ontology/organization#User` ✅
2. 注释属性前缀仍为 `ont`：`ont:icon` → `http://youming.com/ontology/annotation#icon` ✅
3. 多前缀声明齐全：Turtle 头部 `@prefix ex: ...`、`@prefix ont: ...`、`@prefix xsd: ...` 均出现 ✅
