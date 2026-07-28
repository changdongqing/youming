# BUG-001: 属性 property_iri 序列化时缺失 namespace_base 拼接

| 项目 | 内容 |
|------|------|
| **编号** | BUG-001 |
| **严重级别** | 高（High） |
| **影响域** | 建模域 — RDF 序列化（FR-15） |
| **发现日期** | 2026-07-29 |
| **发现场景** | 组织域本体建模（V17 种子数据验证） |
| **状态** | 待修复（Open） |

## 一、问题描述

### 现象

在用户/部门/岗位本体建模过程中，发现 **属性的 `property_iri` 字段存储格式与序列化时的使用方式不一致**，导致序列化产出的 RDF 中属性 Resource 缺少正确的命名空间基址，产出的 Turtle/OWL XML 中属性 IRI 为无效的相对路径。

### 根因

系统中 `property_iri` 的**写入路径**与**读取（序列化）路径**对 IRI 格式的预期不对称：

| 路径 | 代码位置 | 写入/读取的 property_iri 格式 | 是否含 namespace_base |
|------|---------|----------------------------|---------------------|
| 手工创建数据属性 | `ModelDatatypePropertyServiceImpl` 第 57 行 | `{classLocalName}_{propLocalName}`（如 `User_username`） | ❌ 否 |
| 手工创建对象属性 | `ModelObjectPropertyServiceImpl` 第 57 行 | `{classLocalName}_{propLocalName}`（如 `User_belongsTo`） | ❌ 否 |
| 模板实例化 | `ClassInstantiationService` 第 109/129 行 | `{classLocalName}_{templateCode}`（如 `User_name`） | ❌ 否 |
| RDF 解析导入 | `ParsingService` 第 178/235 行 | `propRes.getURI()`（如 `http://...#hasName`） | ✅ 是（绝对 IRI） |
| **RDF 序列化导出** | **`SerializationService` 第 182/198 行** | **直接用 `prop.getPropertyIri()` 创建 Resource** | **要求是完整 IRI** |

**序列化时（`SerializationService.buildModel`）直接将 `property_iri` 字段值传给 `model.createResource(prop.getPropertyIri(), OWL.DatatypeProperty)`，期望它是完整的绝对 IRI。但手工创建和模板实例化路径写入的仅是本地名短串（如 `User_username`），不含 namespace_base。**

### 对比：类 IRI 的处理是正确的

类的 `class_iri` 在创建时（`ModelClassServiceImpl` 第 105 行）已拼接 namespace_base：

```java
String expectedIri = project.getNamespaceBase() + cls.getLocalName();
// 如 http://youming.com/ontology/organization#User
```

序列化时（第 160 行）直接用 `cls.getClassIri()` 创建 Resource，格式一致，产出正确。

但属性的 `property_iri` 缺少对应的 namespace_base 拼接逻辑，导致类 IRI 与属性 IRI 的处理不对称。

## 二、证据链

### 证据 1：数据库 DDL 注释明确期望本地名格式

`V13__ont_model_class_seed.sql` 第 78 行：

```sql
COMMENT ON COLUMN ont_model_datatype_property.property_iri IS '属性 IRI（方案B: {classLocalName}_{propLocalName}）';
```

第 100 行（对象属性）：

```sql
COMMENT ON COLUMN ont_model_object_property.property_iri IS '属性 IRI';
```

DDL 注释说明设计意图是存储**本地名格式**（不含 namespace_base）。

### 证据 2：三条写入路径均不拼接 namespace_base

**手工创建（`ModelDatatypePropertyServiceImpl.java` 第 51-58 行）：**

```java
// 1. 拼接 propertyIri（方案B: {classLocalName}_{propLocalName}）
if (StrUtil.isBlank(prop.getPropertyIri())) {
    ModelClass cls = modelClassMapper.selectById(prop.getClassId());
    if (cls == null) {
        return R.failed("所属类不存在");
    }
    prop.setPropertyIri(cls.getLocalName() + "_" + prop.getLocalName());
    prop.setProjectId(cls.getProjectId());
}
```

**模板实例化（`ClassInstantiationService.java` 第 109 行）：**

```java
dtProp.setPropertyIri(cls.getLocalName() + "_" + propertyTemplateCode);
```

第 129 行（对象属性）：

```java
objProp.setPropertyIri(cls.getLocalName() + "_" + propertyTemplateCode);
```

**三条路径全部只拼接 `{classLocalName}_{propLocalName}`，无 `namespace_base` 参与。**

### 证据 3：序列化时直接用 property_iri 创建 Resource，无前缀拼接

`SerializationService.java` 第 182 行（数据属性）：

```java
Resource propRes = model.createResource(prop.getPropertyIri(), OWL.DatatypeProperty);
```

第 198 行（对象属性）：

```java
Resource propRes = model.createResource(prop.getPropertyIri(), OWL.ObjectProperty);
```

`buildModel` 方法在第 146-148 行查出了 `nsBase = project.getNamespaceBase()` 并注册了前缀：

```java
ModelProject project = modelProjectMapper.selectById(projectId);
String nsBase = project.getNamespaceBase();
model.setNsPrefix(extractPrefixName(nsBase, projectId), nsBase);
```

但 `nsBase` **仅用于 `setNsPrefix` 声明**，从未用于拼接 `property_iri`。

### 证据 4：解析导入路径写入绝对 IRI，与手工创建格式不一致

`ParsingService.java` 第 178 行（数据属性）：

```java
prop.setPropertyIri(propRes.getURI());  // 直接写入完整 IRI
```

第 235 行（对象属性）：

```java
prop.setPropertyIri(propRes.getURI());  // 直接写入完整 IRI
```

解析导入写入的是绝对 IRI（如 `http://youming.com/ontology/organization#User_username`），而手工创建写入的是相对短串（如 `User_username`）。两种格式共存于同一 `property_iri` 列。

### 证据 5：实际序列化输出验证（V17 种子数据）

对 V17 组织域本体种子数据执行序列化预览，类 IRI 正确输出为绝对 IRI，但属性 IRI 输出为裸短串：

- 类（正确）：`<http://youming.com/ontology/organization#User>` ✅
- 属性（错误）：`User_username`（缺少 namespace_base） ❌

## 三、影响范围

| 影响面 | 说明 |
|-------|------|
| **RDF 序列化输出** | 手工创建/模板实例化的属性在序列化后产出无效的相对 IRI，不符合 OWL/RDF 规范（IRI 应为绝对 URI） |
| **RDF 解析导入** | 导入写入绝对 IRI，与手工创建的相对短串格式不一致，同一项目中 `property_iri` 列混合两种格式 |
| **序列化→解析往返（Round-trip）** | 序列化导出的 RDF 再导入时，属性 IRI 可能因格式变化导致重复创建或匹配失败 |
| **跨项目属性冲突** | 相对短串 `User_username` 在不同项目中可能重复（uk 约束为 `project_id + property_iri`，项目内唯一但语义上仍是相对路径） |
| **外部工具兼容性** | 产出的 RDF 无法被 Protégé、Jena ARQ 等外部工具正确解析属性 IRI |

## 四、修复建议

### 方案 A（推荐）：序列化时拼接 namespace_base

在 `SerializationService.buildModel` 中，创建属性 Resource 前拼接 `nsBase`：

```java
// 第 182 行，修改前：
Resource propRes = model.createResource(prop.getPropertyIri(), OWL.DatatypeProperty);

// 修改后：
String fullPropIri = prop.getPropertyIri().startsWith("http")
    ? prop.getPropertyIri()                                    // 导入路径已含完整 IRI，原样使用
    : nsBase + prop.getPropertyIri();                           // 手工创建路径，拼接 namespace_base
Resource propRes = model.createResource(fullPropIri, OWL.DatatypeProperty);
```

同理修改第 198 行（对象属性）。

**优点**：
- 改动范围最小（仅 SerializationService 两行）
- 不影响数据库已有数据（`property_iri` 字段值不变）
- 不影响 `uk_ont_model_dt_prop_iri` 唯一约束语义（仍为项目内本地名唯一）
- 兼容导入路径（已含完整 IRI 的原样使用，通过 `startsWith("http")` 判断）

**缺点**：
- `property_iri` 列存储格式仍混合（本地名 + 绝对 IRI），需要 DDL 注释更新说明

### 方案 B：写入时拼接完整 IRI（与类 IRI 对齐）

在 `ModelDatatypePropertyServiceImpl.saveProp`、`ModelObjectPropertyServiceImpl.saveProp`、`ClassInstantiationService` 中，拼接 `namespace_base + localName + "_" + templateCode`：

```java
ModelProject project = modelProjectService.getById(cls.getProjectId());
String nsBase = project.getNamespaceBase();
prop.setPropertyIri(nsBase + cls.getLocalName() + "_" + prop.getLocalName());
```

**优点**：
- 数据库存储格式统一为绝对 IRI，与 `class_iri` 对齐
- 序列化无需修改

**缺点**：
- 需修改三处写入路径
- 需同步更新 `uk_ont_model_dt_prop_iri` 唯一约束的语义（从 `project_id + 本地名` 变为 `project_id + 绝对 IRI`，实际值不变但语义变化）
- 需更新 DDL 注释（V13 第 78 行）
- 已有数据需数据迁移（若有手工创建的属性数据）

### 推荐

**方案 A**，改动最小且不影响已有数据和约束。同时建议在 DDL 注释中补充说明 `property_iri` 在手工创建路径存储本地名、在导入路径存储绝对 IRI 的双格式约定。

## 五、关联文件

| 文件 | 行号 | 说明 |
|------|-----|------|
| `server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/modeling/service/SerializationService.java` | 182, 198 | **Bug 发生点**：序列化时直接用 property_iri 创建 Resource，缺少 namespace_base 拼接 |
| `server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/modeling/service/impl/ModelDatatypePropertyServiceImpl.java` | 57 | 数据属性写入路径：拼本地名不含 namespace_base |
| `server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/modeling/service/impl/ModelObjectPropertyServiceImpl.java` | 57 | 对象属性写入路径：同上 |
| `server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/modeling/service/ClassInstantiationService.java` | 109, 129 | 模板实例化路径：拼本地名不含 namespace_base |
| `server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/modeling/service/ParsingService.java` | 178, 235 | 解析导入路径：写入绝对 IRI（与手工创建格式不一致） |
| `server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/modeling/service/impl/ModelClassServiceImpl.java` | 105 | 类 IRI 拼接（对照：正确拼接了 namespace_base） |
| `server/pig-common/pig-common-data/src/main/resources/db/migration/V13__ont_model_class_seed.sql` | 78 | DDL 注释：设计意图为本地名格式 |

## 六、验证方法

修复后，对 V17 组织域本体种子数据执行序列化预览（`/admin/ont/model/serialize/preview?projectId=1700001&format=TTL`），确认输出中：

1. 类 IRI 为绝对 URI：`<http://youming.com/ontology/organization#User>` ✅
2. 属性 IRI 也为绝对 URI：`<http://youming.com/ontology/organization#User_username>` ✅（修复前为裸 `User_username`）
3. 对象属性 IRI 同样为绝对 URI：`<http://youming.com/ontology/organization#User_belongsTo>` ✅
