# 空间/BIM/GIS本体模块详细设计

> 模块编号：27  
> 对应规划：`aidocs/临时分析/现有设计与IoT场景落地差距分析.md` §5.2-D / §6 M7路线图  
> 前置模块：02 单位字典、03 命名空间与IRI、04 实体类型、05 数据属性、06 对象属性、08 实体实例、11 扩展管理、14 版本演化、18 数据源映射、20 物模型层  
> 设计日期：2026-07-16  
> 文档状态：详细设计基线  

---

## 1. 评审结论

空间是园区、医院、楼宇一切对象（设备、人员、事件）的锚点。没有空间本体，设备和人员无处安放，所有场景无法落地。本模块建设"园→楼→层→区→室"的纯语义空间层次本体，V1 不接 IFC 解析器和 GIS 引擎，但预留 BIM/GIS 扩展点。空间本体与物模型设备实例通过对象属性关联，为后续园区/医院场景提供空间定位能力。

| 级别 | 问题 | 设计结论 |
|---|---|---|
| P0 | 空间层次必须自上而下，不能扁平化 | 定义 Space 抽象根类 + Campus→Building→Floor→Zone→Room 五级实体类型，通过 ont_entity_type_hierarchy 建立 subClassOf 层次 |
| P0 | 空间层次关系是树形（严格一对多父引用），不能用 DAG | 对象属性 parentSpace 为 functional，并增加自引用检测和环路检测 |
| P0 | 设备/人员/事件需关联空间，但空间本体不依赖物模型 | 空间定义 hasOccupant/hasDevice 对象属性，domain 为空间类型，range 为 Object 核心类（多态引用） |
| P0 | 空间本体是行业扩展，不得污染核心实体类型 | 走扩展命名空间（ymspace, 930400），所有实体类型 subClassOf 核心 Object（R5规则） |
| P1 | BIM IFC 对接复杂，一期不做 | 预留 bimElementId/bimGuid 数据属性和 hasBimModel 对象属性扩展点，V1 不实现 IFC 解析 |
| P1 | GIS 坐标需支持 WGS84 和 CGCS2000 | 定义 longitude/latitude/altitude/elevation 数据属性，坐标系通过 crs 枚举标注 |
| P1 | 空间面积/体积需单位约束 | 复用模块02单位字典，面积单位为 m²，体积为 m³ |

---

## 2. 范围

### 2.1 V1 包含

- 空间扩展本体定义：Space 抽象根类 + Campus/Building/Floor/Zone/Room 五级实体类型；
- 空间数据属性：编码、名称、坐标、面积/体积、楼层、BIM扩展点等；
- 空间对象属性：parentSpace/childSpace（层次）、hasOccupant（占用者）、hasDevice（设备）、adjacentSpace（邻接）、hasBimModel（BIM引用）；
- 空间层次关系树形校验（环路检测、自引用检测）；
- 空间实例通过数据源映射或 IngestionService 写入；
- 空间管理前端控制台（空间树浏览、实例CRUD、层次可视化）；
- 与模块20物模型设备实例的空间关联能力。

### 2.2 V1 不包含

- IFC（Industry Foundation Classes）文件解析与导入（属后续 BIM 集成阶段）；
- GIS 地图引擎集成与空间拓扑计算（属后续 GIS 集成阶段）；
- 三维空间模型渲染与数字孪生可视化（属模块33 数字孪生）；
- 空间路径规划与导航（属后续空间分析阶段）；
- BIM 模型碰撞检测与合规审查（属 BIM 专业工具）；
- 空间占用率实时计算与热力图（属后续运营分析阶段）；
- 室内导航与蓝牙信标定位（属后续定位集成）。

---

## 3. 核心术语

| 术语 | 定义 |
|---|---|
| 空间 `Space` | 所有空间实体的抽象根类，园区/楼宇/楼层/区域/房间的统一父类 |
| 园区 `Campus` | 空间层次最高级，含多栋建筑的空间集合 |
| 楼宇 `Building` | 园区下的独立建筑体 |
| 楼层 `Floor` | 楼宇内的水平楼层 |
| 区域 `Zone` | 楼层或楼宇内的功能分区（如办公区、设备区、安防区） |
| 房间 `Room` | 最细粒度的封闭空间单元 |
| 空间层次 `SpaceHierarchy` | 空间实体的树形父子关系（严格树形，非DAG） |
| 邻接 `AdjacentSpace` | 同级或跨级空间的物理邻接关系（非层次，对称） |
| BIM引用 `BimReference` | 空间实体到 BIM 模型元素的标识引用（扩展点） |
| 空间坐标 `SpatialCoordinate` | 空间实体的地理/几何坐标（经纬度/海拔/楼层标高） |

---

## 4. ID 分配

### 4.1 命名空间与扩展模块

| 对象 | ID | 值 | 说明 |
|---|---|---|---|
| 命名空间 | 930400 | prefix=`ymspace`, uri=`http://example.org/youming/space#` | is_builtin='0', is_default='0', sort_order=500 |
| 扩展模块 | 939004 | module_code=`youming-space`, module_name=`youming空间本体扩展` | namespace_id=930400, ontology_id=935001, version='1.0.0' |
| 菜单目录 | 901800 | 空间管理二级目录 | parent=900000, sort_order=80 |

### 4.2 实体类型ID分配（944001~944010）

| ID | name | 中文标签 | 抽象 | 父类 | 说明 |
|---|---|---|---|---|---|
| 944001 | Space | 空间 | 是 | 940063(Object) | 空间抽象根类，满足R5 |
| 944002 | Campus | 园区 | 否 | 944001 | 园区 |
| 944003 | Building | 楼宇 | 否 | 944001 | 楼宇 |
| 944004 | Floor | 楼层 | 否 | 944001 | 楼层 |
| 944005 | Zone | 区域 | 否 | 944001 | 功能分区 |
| 944006 | Room | 房间 | 否 | 944001 | 房间 |
| 944007 | SpatialCoordinate | 空间坐标 | 否 | 940063(Object) | 坐标值对象（可独立实例或嵌入） |
| 944008 | BimModelReference | BIM模型引用 | 否 | 940063(Object) | BIM扩展点 |
| 944009 | SpaceType | 空间类型分类 | 否 | 944001 | 空间功能分类（办公/设备/安防/仓储等） |
| 944010 | SpaceBoundary | 空间边界 | 否 | 940063(Object) | 空间几何边界定义 |

### 4.3 数据属性ID分配（956001~956040）

| ID | name | domain | base_type | value_mode | 单位 | 安全级别 | 说明 |
|---|---|---|---|---|---|---|---|
| 956001 | spaceCode | 944001(Space) | TEXT | FREE | — | INTERNAL | 空间编码，is_unique=1 |
| 956002 | spaceName | 944001 | TEXT | FREE | — | INTERNAL | 空间名称 |
| 956003 | spaceDescription | 944001 | TEXT | FREE | — | INTERNAL | 空间描述 |
| 956004 | spaceSortOrder | 944001 | NUMERIC | FREE | — | INTERNAL | 排序序号 |
| 956005 | spaceStatus | 944001 | TEXT | CLOSED_ENUM | — | INTERNAL | ACTIVE/INACTIVE/MAINTENANCE |
| 956006 | campusCode | 944002 | TEXT | FREE | — | INTERNAL | 园区编码，is_unique=1 |
| 956007 | campusName | 944002 | TEXT | FREE | — | INTERNAL | 园区名称 |
| 956008 | campusAddress | 944002 | TEXT | FREE | — | INTERNAL | 园区地址 |
| 956009 | campusArea | 944002 | NUMERIC | FREE | m² | INTERNAL | 园区占地面积 |
| 956010 | buildingCode | 944003 | TEXT | FREE | — | INTERNAL | 楼宇编码，is_unique=1 |
| 956011 | buildingName | 944003 | TEXT | FREE | — | INTERNAL | 楼宇名称 |
| 956012 | buildingAddress | 944003 | TEXT | FREE | — | INTERNAL | 楼宇地址 |
| 956013 | buildingArea | 944003 | NUMERIC | FREE | m² | INTERNAL | 建筑面积 |
| 956014 | buildingVolume | 944003 | NUMERIC | FREE | m³ | INTERNAL | 建筑体积 |
| 956015 | buildingFloorsAbove | 944003 | NUMERIC | FREE | — | INTERNAL | 地上层数 |
| 956016 | buildingFloorsBelow | 944003 | NUMERIC | FREE | — | INTERNAL | 地下层数 |
| 956017 | buildingHeight | 944003 | NUMERIC | FREE | m | INTERNAL | 建筑高度 |
| 956018 | buildingYearBuilt | 944003 | NUMERIC | FREE | — | INTERNAL | 建成年份 |
| 956019 | floorCode | 944004 | TEXT | FREE | — | INTERNAL | 楼层编码，is_unique=1 |
| 956020 | floorName | 944004 | TEXT | FREE | — | INTERNAL | 楼层名称（如B1, 1F, 2F） |
| 956021 | floorNumber | 944004 | NUMERIC | FREE | — | INTERNAL | 楼层序号（负数=地下） |
| 956022 | floorArea | 944004 | NUMERIC | FREE | m² | INTERNAL | 楼层面积 |
| 956023 | floorElevation | 944004 | NUMERIC | FREE | m | INTERNAL | 楼层标高 |
| 956024 | zoneCode | 944005 | TEXT | FREE | — | INTERNAL | 区域编码，is_unique=1 |
| 956025 | zoneName | 944005 | TEXT | FREE | — | INTERNAL | 区域名称 |
| 956026 | zoneType | 944005 | TEXT | CLOSED_ENUM | — | INTERNAL | OFFICE/EQUIPMENT/SECURITY/STORAGE/CIRCULATION |
| 956027 | zoneArea | 944005 | NUMERIC | FREE | m² | INTERNAL | 区域面积 |
| 956028 | roomCode | 944006 | TEXT | FREE | — | INTERNAL | 房间编码，is_unique=1 |
| 956029 | roomName | 944006 | TEXT | FREE | — | INTERNAL | 房间名称 |
| 956030 | roomArea | 944006 | NUMERIC | FREE | m² | INTERNAL | 房间面积 |
| 956031 | roomCapacity | 944006 | NUMERIC | FREE | — | INTERNAL | 房间容纳人数 |
| 956032 | longitude | 944007 | NUMERIC | FREE | ° | INTERNAL | 经度（WGS84/CGCS2000） |
| 956033 | latitude | 944007 | NUMERIC | FREE | ° | INTERNAL | 纬度（WGS84/CGCS2000） |
| 956034 | altitude | 944007 | NUMERIC | FREE | m | INTERNAL | 海拔高度 |
| 956035 | coordinateSystem | 944007 | TEXT | CLOSED_ENUM | — | INTERNAL | WGS84/CGCS2000/GCJ02/BD09 |
| 956036 | bimElementId | 944008 | TEXT | FREE | — | INTERNAL | BIM元素ID（IFC GUID） |
| 956037 | bimElementType | 944008 | TEXT | CLOSED_ENUM | — | INTERNAL | IfcSpace/IfcBuilding/IfcBuildingStorey等 |
| 956038 | bimModelSource | 944008 | TEXT | FREE | — | INTERNAL | BIM模型来源文件/服务标识 |
| 956039 | spaceTypeCode | 944009 | TEXT | CLOSED_ENUM | — | INTERNAL | 空间类型分类编码 |
| 956040 | boundaryDefinition | 944010 | TEXT | FREE | — | INTERNAL | 边界定义（GeoJSON/WKT格式） |

### 4.4 对象属性ID分配（964001~964015）

| ID | name | domain | range | 特征 | 说明 |
|---|---|---|---|---|---|
| 964001 | parentSpace | 944001(Space) | 944001(Space) | functional, transitive | 空间层次父引用（树形） |
| 964002 | childSpace | 944001 | 944001 | — | parentSpace的逆（inverse_of 964001） |
| 964003 | adjacentSpace | 944001 | 944001 | symmetric | 空间邻接关系（对称，非层次） |
| 964004 | hasOccupant | 944001 | 940063(Object) | — | 空间占用者（人员/组织，多态引用） |
| 964005 | hasDevice | 944001 | 940063(Object) | — | 空间内设备（多态引用，可指向模块20 DeviceInstance） |
| 964006 | hasCoordinate | 944001 | 944007(SpatialCoordinate) | functional | 空间坐标 |
| 964007 | hasBimModel | 944001 | 944008(BimModelReference) | functional | BIM模型引用（扩展点） |
| 964008 | hasSpaceType | 944001 | 944009(SpaceType) | functional | 空间类型分类 |
| 964009 | hasBoundary | 944001 | 944010(SpaceBoundary) | functional | 空间边界 |
| 964010 | belongsToCampus | 944003/944004/944005/944006 | 944002(Campus) | functional | 非园区空间归属园区 |
| 964011 | belongsToBuilding | 944004/944005/944006 | 944003(Building) | functional | 楼层/区域/房间归属楼宇 |
| 964012 | belongsToFloor | 944005/944006 | 944004(Floor) | functional | 区域/房间归属楼层 |
| 964013 | belongsToZone | 944006 | 944005(Zone) | functional | 房间归属区域 |
| 964014 | deviceLocatedIn | 940063(Object) | 944001(Space) | functional | 设备位于空间（hasDevice的逆） |
| 964015 | occupantLocatedIn | 940063(Object) | 944001(Space) | functional | 占用者位于空间（hasOccupant的逆） |

---

## 5. 总体架构

```text
┌─────────────────── Vue 空间管理控制台 ──────────────────────┐
│ 空间树浏览 │ 空间实例CRUD │ 层次可视化 │ BIM引用 │ 坐标管理 │
└───────────────────────────┬────────────────────────────────────┘
                            │ REST
┌───────────────────────────▼────────────────────────────────────┐
│                   pig-ontology-biz / space                       │
│                                                                  │
│  SpaceService              SpaceHierarchyService                 │
│  SpaceInstanceService      SpaceTreeBuilder                      │
│  SpaceValidationService    SpaceCoordinateService               │
│                                                                  │
│  SpaceTreeCycleDetector ── SpaceHierarchyValidator              │
└───────────────────────────┬────────────────────────────────────┘
                            │ Normalized Ingestion Command
┌───────────────────────────▼────────────────────────────────────┐
│ OntologyInstanceIngestionService（复用模块18）                   │
│ source_type=DATA_MAPPING/EXTENSION │ VersionGuard │ Idempotency │
└───────────────┬─────────────────────────────┬────────────────────┘
                │                             │
       PostgreSQL实例表               OntDomainEventPublisher
  ont_entity_instance等           Outbox → Redis Streams
                │
       SPARQL / 可视化 / 序列化
```

### 5.1 分层说明

| 层 | 职责 | 复用/新建 |
|---|---|---|
| 空间本体层 | Space/Campus/Building/Floor/Zone/Room 的 T-Box 定义 | 新建扩展本体（V26种子） |
| 空间层次层 | 树形层次关系、环路检测、层次校验 | 新建层次服务 |
| 空间实例层 | 空间实例CRUD、层次关系建立 | 复用 `OntologyInstanceIngestionService` |
| 空间关联层 | 设备→空间、人员→空间关联 | 新建关联对象属性 + IngestionService.upsertRelation |
| BIM/GIS扩展层 | BIM引用、坐标管理（扩展点） | 新建数据属性，V1仅存储不解析 |
| 事件骨干层 | 空间实例变化事件 | 复用模块26 EDA |
| 安全合规层 | 空间访问权限、敏感区域审计 | 复用模块36 |

---

## 6. 空间层次模型

### 6.1 标准层次结构

```text
Campus（园区）
  └── Building（楼宇）
       └── Floor（楼层）
            └── Zone（区域）
                 └── Room（房间）
```

### 6.2 层次关系存储

空间层次通过两种机制表达：

| 机制 | 存储 | 语义 | 用途 |
|---|---|---|---|
| subClassOf（类型层次） | `ont_entity_type_hierarchy` | 实体类型继承（Campus is-a Space） | T-Box定义 |
| parentSpace（实例层次） | `ont_instance_object_relation`（对象属性964001） | 实例间父子关系（Building-A 的 parentSpace = Campus-X） | A-Box实例关系 |

### 6.3 层次校验规则

| 序号 | 规则 | 级别 | 说明 |
|---|---|---|---|
| H1 | parentSpace 自引用检测 | VIOLATION | 空间实例的 parentSpace 不能指向自身 |
| H2 | parentSpace 环路检测 | VIOLATION | parentSpace 链不得形成环路（A→B→C→A） |
| H3 | 层次类型约束 | WARNING | Campus 的 parentSpace 应为 Campus 或无（顶层）；Building 的 parentSpace 应为 Campus；Floor 应为 Building；Zone 应为 Floor 或 Building；Room 应为 Zone 或 Floor |
| H4 | 严格树形 | VIOLATION | 每个空间实例只能有一个 parentSpace（functional约束保证） |
| H5 | 邻接对称性 | VIOLATION | adjacentSpace 必须双向建立（A→B 则 B→A） |
| H6 | 归属关系一致性 | WARNING | belongsToBuilding 的目标应为 parentSpace 链上的 Building 实例 |

### 6.4 环路检测算法

```text
SpaceTreeCycleDetector.detectCycle(spaceInstanceId, proposedParentId):
  1. current = proposedParentId
  2. while current != null:
       a. if current == spaceInstanceId: return true（环路）
       b. current = parentSpace(current)  // 沿 parentSpace 链上溯
  3. return false
```

最大上溯深度限制为 50 层（防止异常数据导致无限循环）。

---

## 7. 数据模型

### 7.1 新建表

| 表名 | 用途 | 版本脚本 |
|---|---|---|
| `ont_space_hierarchy_check` | 空间层次校验快照（环路检测结果缓存） | V26 |
| `ont_space_bim_reference` | BIM模型引用关联表（空间实例↔BIM元素） | V26 |

### 7.2 改造现有表

| 表 | 改造 | 原因 |
|---|---|---|
| `ont_entity_instance` | 无DDL改造 | 复用现有实例表，空间实例为普通实体实例 |
| `ont_instance_object_relation` | 无DDL改造 | 复用现有对象关系表，parentSpace/adjacentSpace 存于此 |

### 7.3 种子数据范围

| 对象 | ID范围 | 数量 | 说明 |
|---|---|---|---|
| 命名空间 | 930400 | 1 | ymspace |
| 扩展模块 | 939004 | 1 | youming-space |
| 实体类型 | 944001~944010 | 10 | 空间域实体 |
| 数据属性 | 956001~956040 | 40 | 空间属性定义 |
| 对象属性 | 964001~964015 | 15 | 空间关系定义 |
| 扩展资源 | 983200~983266 | 67 | 10实体+40数据属性+15对象属性+1命名空间+1模块 |
| 菜单 | 901800~901810 | 11 | 空间管理菜单 |
| 枚举种子 | — | 若干 | spaceStatus/zoneType/coordinateSystem/bimElementType/spaceTypeCode等 |

### 7.4 建表顺序

```text
V26__ontology_space_bim_gis.sql
  第1段：空间扩展本体种子（命名空间→模块→实体类型→数据属性→对象属性→扩展资源→菜单）
  第2段：ont_space_hierarchy_check（层次校验快照表）
  第3段：ont_space_bim_reference（BIM引用关联表）
  第4段：完整性断言DO块
```

---

## 8. 表结构DDL

### 8.1 ont_space_hierarchy_check

```sql
CREATE TABLE ont_space_hierarchy_check (
    id                   BIGSERIAL PRIMARY KEY,
    space_instance_id    BIGINT NOT NULL,        -- 空间实例ID（ont_entity_instance.id）
    parent_instance_id   BIGINT,                 -- 父空间实例ID
    hierarchy_depth      INTEGER NOT NULL DEFAULT 0,  -- 距根的深度
    hierarchy_path       TEXT,                   -- 从根到当前实例的IRI路径（如 /Campus-X/Building-A/Floor-3）
    has_cycle            CHAR(1) NOT NULL DEFAULT '0',  -- 是否检测到环路
    cycle_path           TEXT,                   -- 环路路径描述（has_cycle=1时填充）
    checked_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by            VARCHAR(64),
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by            VARCHAR(64),
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag             CHAR(1) DEFAULT '0',
    FOREIGN KEY (space_instance_id) REFERENCES ont_entity_instance(id)
);

CREATE INDEX idx_space_hierarchy_instance ON ont_space_hierarchy_check(space_instance_id)
    WHERE del_flag='0';
CREATE INDEX idx_space_hierarchy_cycle ON ont_space_hierarchy_check(has_cycle)
    WHERE del_flag='0' AND has_cycle='1';

COMMENT ON TABLE ont_space_hierarchy_check IS '空间层次校验快照，缓存环路检测结果和层次路径';
```

### 8.2 ont_space_bim_reference

```sql
CREATE TABLE ont_space_bim_reference (
    id                   BIGSERIAL PRIMARY KEY,
    space_instance_id    BIGINT NOT NULL,        -- 空间实例ID
    bim_element_guid     VARCHAR(128) NOT NULL,  -- IFC GUID（全局唯一）
    bim_element_type     VARCHAR(64) NOT NULL,   -- IfcSpace/IfcBuilding/IfcBuildingStorey等
    bim_model_source     VARCHAR(512),           -- BIM模型来源文件路径或服务标识
    bim_model_version    VARCHAR(32),            -- BIM模型版本
    spatial_mapping_conf JSONB,                  -- 空间映射配置（坐标转换参数等）
    is_active            CHAR(1) NOT NULL DEFAULT '1',
    create_by            VARCHAR(64),
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by            VARCHAR(64),
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag             CHAR(1) DEFAULT '0',
    FOREIGN KEY (space_instance_id) REFERENCES ont_entity_instance(id),
    UNIQUE (bim_element_guid, bim_model_source, del_flag)
);

CREATE INDEX idx_space_bim_instance ON ont_space_bim_reference(space_instance_id)
    WHERE del_flag='0';
CREATE INDEX idx_space_bim_guid ON ont_space_bim_reference(bim_element_guid)
    WHERE del_flag='0';

COMMENT ON TABLE ont_space_bim_reference IS '空间实例到BIM模型元素的引用关联，V1仅存储引用不解析IFC';
```

---

## 9. 服务接口设计

### 9.1 核心服务接口

```java
package com.pig4cloud.pig.ontology.space;

/**
 * 空间实例管理服务。
 * 负责空间实例的创建、更新、删除和层次关系管理。
 */
public interface SpaceInstanceService {

    /**
     * 创建空间实例。
     * @param request 空间创建请求（含类型、编码、名称、父空间）
     */
    SpaceInstanceResult create(SpaceCreateRequest request);

    /**
     * 更新空间实例。
     */
    SpaceInstanceResult update(Long instanceId, SpaceUpdateRequest request);

    /**
     * 设置空间父关系（parentSpace）。
     * @param spaceInstanceId 空间实例
     * @param parentInstanceId 父空间实例
     * @throws SpaceCycleException 如果检测到环路
     */
    void setParentSpace(Long spaceInstanceId, Long parentInstanceId);

    /**
     * 建立空间邻接关系（双向）。
     */
    void setAdjacentSpace(Long spaceA, Long spaceB);

    /**
     * 解除空间邻接关系（双向删除）。
     */
    void removeAdjacentSpace(Long spaceA, Long spaceB);

    /**
     * 删除空间实例。
     * @param deleteStrategy IGNORE/MARK_INACTIVE/SOFT_DELETE
     */
    void delete(Long instanceId, SpaceDeleteStrategy deleteStrategy);

    /**
     * 查询空间子树。
     * @param rootInstanceId 根空间实例
     * @param maxDepth 最大深度（-1=不限）
     */
    SpaceTreeNode getSubTree(Long rootInstanceId, int maxDepth);

    /**
     * 查询空间实例详情（含层次路径、坐标、BIM引用）。
     */
    SpaceInstanceDetail getDetail(Long instanceId);
}

/**
 * 空间层次校验服务。
 */
public interface SpaceHierarchyService {

    /**
     * 检测环路。
     * @return true 如果 parentSpace 链形成环路
     */
    boolean detectCycle(Long spaceInstanceId, Long proposedParentId);

    /**
     * 批量校验空间层次完整性。
     */
    SpaceHierarchyValidationReport validateHierarchy(Long rootInstanceId);

    /**
     * 构建层次路径（从根到当前实例的IRI路径）。
     */
    String buildHierarchyPath(Long spaceInstanceId);

    /**
     * 刷新层次校验快照。
     */
    void refreshHierarchyCheck(Long rootInstanceId);
}

/**
 * 空间坐标服务。
 */
public interface SpaceCoordinateService {

    /**
     * 设置空间坐标。
     */
    void setCoordinate(Long spaceInstanceId, SpatialCoordinate coordinate);

    /**
     * 批量导入坐标。
     */
    BatchResult importCoordinates(List<CoordinateImportItem> items);

    /**
     * 坐标系转换（如 GCJ02→WGS84）。
     * V1仅标注坐标系，不执行转换。
     */
    SpatialCoordinate convertCoordinate(SpatialCoordinate source, String targetCrs);
}

/**
 * BIM引用管理服务。
 */
public interface SpaceBimReferenceService {

    /**
     * 关联BIM元素。
     */
    void associateBimElement(Long spaceInstanceId, BimReferenceRequest request);

    /**
     * 解除BIM关联。
     */
    void dissociateBimElement(Long spaceInstanceId, String bimElementGuid);

    /**
     * 按BIM GUID查询空间实例。
     */
    List<SpaceInstanceDetail> findByBimGuid(String bimElementGuid);
}
```

### 9.2 关键Record定义

```java
public record SpaceCreateRequest(
    Long entityTypeId,          // 944002~944006
    String spaceCode,
    String spaceName,
    String spaceDescription,
    Long parentSpaceInstanceId, // 可选，顶层空间为null
    Map<Long, String> dataValues,  // 数据属性ID→值
    SpatialCoordinate coordinate,  // 可选
    BimReferenceRequest bimReference  // 可选
) {}

public record SpaceTreeNode(
    Long instanceId,
    String iri,
    String spaceCode,
    String spaceName,
    Long entityTypeId,
    String entityTypeLabel,
    Integer depth,
    List<SpaceTreeNode> children
) {}

public record SpatialCoordinate(
    BigDecimal longitude,
    BigDecimal latitude,
    BigDecimal altitude,
    String coordinateSystem  // WGS84/CGCS2000/GCJ02/BD09
) {}

public record BimReferenceRequest(
    String bimElementGuid,
    String bimElementType,
    String bimModelSource,
    String bimModelVersion
) {}

public record SpaceHierarchyValidationReport(
    int totalChecked,
    int cycleCount,
    int orphanCount,        // 无parentSpace的非顶层实例
    int typeMismatchCount,  // 层次类型不匹配
    List<HierarchyIssue> issues
) {}
```

---

## 10. 空间实例摄入流程

### 10.1 通过数据源映射写入空间实例

```text
外部空间数据（如楼宇资产管理系统数据库）
  → 模块18数据源映射配置（源表→空间实体类型映射）
  → MappingPlanCompiler 编译执行计划
  → OntologyInstanceIngestionService.upsertEntity(command)
    - source_type = DATA_MAPPING
    - entityTypeId = 944002~944006（根据映射目标）
    - IRI = namespace.uri + iriTemplate（如 http://example.org/youming/space#Building/A栋）
    - values = 空间数据属性值（编码/名称/面积/楼层等）
  → IngestionService 内部执行：
    - 版本守卫
    - 幂等检查（source_binding + content_hash）
    - SHACL校验
    - 来源绑定 upsert
    - 事件发布（INSTANCE_INGESTED）
```

### 10.2 通过前端CRUD写入空间实例

```text
前端空间管理页面
  → POST /ontology/space/instances
  → SpaceInstanceService.create(request)
    - 构造 EntityIngestionCommand：
      - entityTypeId = request.entityTypeId
      - sourceType = EXTENSION
      - SourceIdentity(sourceId=null, entityMappingCode=spaceCode, sourceRecordKey=spaceCode)
      - values = request.dataValues
    - 调用 OntologyInstanceIngestionService.upsertEntity(command)
  → 设置 parentSpace 关系：
    - 构造 RelationIngestionCommand：
      - objectPropertyId = 964001 (parentSpace)
      - subjectInstanceId = 新创建实例
      - objectInstanceId = request.parentSpaceInstanceId
    - 调用 IngestionService.upsertRelation(command)
    - 执行环路检测（SpaceTreeCycleDetector.detectCycle）
  → 设置坐标（如有）：
    - 创建 SpatialCoordinate 实例
    - 建立 hasCoordinate 关系（964006）
```

### 10.3 设备→空间关联

```text
模块20设备实例创建/更新
  → 设备实例有空间位置信息
  → 调用 IngestionService.upsertRelation：
    - objectPropertyId = 964005 (hasDevice) 或 964014 (deviceLocatedIn)
    - subjectInstanceId = 空间实例
    - objectInstanceId = 设备实例（943010）
  → 或反向：设备实例的 deviceLocatedIn 指向空间实例
```

### 10.4 幂等控制

| 幂等键 | 构成 | 作用 |
|---|---|---|
| 空间实例幂等 | `(sourceId, entityMappingCode=spaceCode, sourceRecordKey=spaceCode)` | 同一空间不重复创建 |
| parentSpace幂等 | `(subjectInstanceId, objectPropertyId=964001)` functional约束 | 每个空间仅一个父空间 |
| BIM引用幂等 | `(bimElementGuid, bimModelSource)` | 同一BIM元素不重复关联 |

---

## 11. 事件

| 事件 | 触发条件 | 载荷 |
|---|---|---|
| `SPACE_INSTANCE_CREATED` | 空间实例创建 | instanceIri, entityTypeId, spaceCode, spaceName |
| `SPACE_INSTANCE_UPDATED` | 空间实例更新 | instanceIri, updatedFields |
| `SPACE_HIERARCHY_CHANGED` | parentSpace 关系变更 | spaceInstanceId, oldParentIri, newParentIri |
| `SPACE_CYCLE_DETECTED` | 环路检测发现 | spaceInstanceId, cyclePath |
| `SPACE_ADJACENCY_ESTABLISHED` | 邻接关系建立 | spaceA_Iri, spaceB_Iri |
| `SPACE_BIM_ASSOCIATED` | BIM元素关联 | spaceInstanceId, bimElementGuid |
| `SPACE_INSTANCE_DEACTIVATED` | 空间实例失活 | instanceIri, reason |

---

## 12. 安全与合规

| 对象 | 安全措施 | 复用 |
|---|---|---|
| 敏感区域访问审计 | 安防区/设备区空间实例访问记录审计 | 模块36 DataAccessAuditService |
| 空间层次变更审批 | 顶层空间（Campus/Building）层次变更需审批 | 模块36 OperationApprovalService |
| 空间坐标数据分级 | 坐标数据安全级别 INTERNAL，敏感区域坐标可提级 | 模块36 OntologyDataPolicyService |
| 空间实例事件载荷 | 载荷不含未脱敏的敏感区域坐标 | 数据最小化原则 |

---

## 13. REST API

前缀：`/ontology/space`

| 方法 | 路径 | 权限标识 | 说明 |
|---|---|---|---|
| GET | `/instances` | `ontology_space_instance_view` | 空间实例列表（支持按类型/层级过滤） |
| POST | `/instances` | `ontology_space_instance_add` | 创建空间实例 |
| PUT | `/instances/{id}` | `ontology_space_instance_edit` | 更新空间实例 |
| DELETE | `/instances/{id}` | `ontology_space_instance_del` | 删除/失活空间实例 |
| GET | `/instances/{id}` | `ontology_space_instance_view` | 空间实例详情 |
| GET | `/instances/{id}/subtree` | `ontology_space_instance_view` | 查询子树 |
| PUT | `/instances/{id}/parent` | `ontology_space_instance_edit` | 设置parentSpace |
| PUT | `/instances/{id}/adjacent` | `ontology_space_instance_edit` | 建立邻接关系 |
| DELETE | `/instances/{id}/adjacent/{targetId}` | `ontology_space_instance_edit` | 解除邻接 |
| POST | `/hierarchy/validate` | `ontology_space_hierarchy_view` | 批量层次校验 |
| GET | `/instances/{id}/path` | `ontology_space_instance_view` | 查询层次路径 |
| PUT | `/instances/{id}/coordinate` | `ontology_space_instance_edit` | 设置坐标 |
| POST | `/coordinates/import` | `ontology_space_instance_edit` | 批量导入坐标 |
| POST | `/instances/{id}/bim` | `ontology_space_bim_add` | 关联BIM元素 |
| DELETE | `/instances/{id}/bim/{bimGuid}` | `ontology_space_bim_del` | 解除BIM关联 |
| GET | `/bim/{bimGuid}/instances` | `ontology_space_bim_view` | 按BIM GUID查空间实例 |

---

## 14. 错误码

| 错误码 | 含义 |
|---|---|
| `SPACE_CODE_DUPLICATE` | 空间编码重复 |
| `SPACE_CYCLE_DETECTED` | parentSpace 形成环路 |
| `SPACE_SELF_REFERENCE` | parentSpace 指向自身 |
| `SPACE_TYPE_MISMATCH` | 层次类型不匹配（如Room的parentSpace为Campus） |
| `SPACE_PARENT_NOT_FOUND` | 父空间实例不存在 |
| `SPACE_BIM_GUID_DUPLICATE` | BIM元素GUID重复关联 |
| `SPACE_COORDINATE_INVALID` | 坐标值非法（经度>180或纬度>90） |
| `SPACE_ADJACENCY_NOT_SYMMETRIC` | 邻接关系未双向建立 |
| `SPACE_DELETE_HAS_CHILDREN` | 空间实例有子空间，不可直接删除 |

---

## 15. 包结构

```text
com.pig4cloud.pig.ontology.space
├── controller/
│   ├── SpaceInstanceController.java
│   ├── SpaceHierarchyController.java
│   ├── SpaceCoordinateController.java
│   └── SpaceBimReferenceController.java
├── service/
│   ├── SpaceInstanceService.java + Impl
│   ├── SpaceHierarchyService.java + Impl
│   ├── SpaceCoordinateService.java + Impl
│   ├── SpaceBimReferenceService.java + Impl
│   └── SpaceTreeBuilder.java
├── validator/
│   ├── SpaceHierarchyValidator.java
│   └── SpaceTreeCycleDetector.java
├── entity/
│   ├── OntSpaceHierarchyCheck.java
│   └── OntSpaceBimReference.java
├── mapper/
│   ├── OntSpaceHierarchyCheckMapper.java
│   └── OntSpaceBimReferenceMapper.java
├── dto/
│   ├── SpaceCreateRequest.java
│   ├── SpaceTreeNode.java
│   ├── SpatialCoordinate.java
│   └── SpaceHierarchyValidationReport.java
└── enums/
    ├── SpaceEntityType.java
    ├── SpaceStatus.java
    ├── ZoneType.java
    └── CoordinateSystem.java
```

---

## 16. 前端管理控制台

### 16.1 菜单结构

```text
本体建模
  └── 空间管理 (901800)
      ├── 空间树浏览 (901801)
      ├── 空间实例 (901802)
      ├── 层次可视化 (901803)
      ├── 坐标管理 (901804)
      ├── BIM引用 (901805)
      └── 邻接关系 (901806)
```

### 16.2 页面组件

| 页面 | 路径 | 核心组件 |
|---|---|---|
| 空间树浏览 | `views/ontology/space/tree.vue` | SpaceTree（树形组件，支持展开/折叠/拖拽） |
| 空间实例 | `views/ontology/space/index.vue` | SpaceInstanceTable, SpaceFormDialog, SpaceTypeSelect |
| 层次可视化 | `views/ontology/space/hierarchy.vue` | HierarchyGraph（D3/antv 树形图）, CycleWarningAlert |
| 坐标管理 | `views/ontology/space/coordinate.vue` | CoordinateTable, CoordinateImportDialog, CrsSelect |
| BIM引用 | `views/ontology/space/bim.vue` | BimReferenceTable, BimAssociationDialog, BimGuidSearch |
| 邻接关系 | `views/ontology/space/adjacency.vue` | AdjacencyTable, AdjacencyEditDialog |

### 16.3 API层

```typescript
// web/src/api/ontology/space.ts
// 空间实例/层次/坐标/BIM引用 全部接口封装
```

### 16.4 关键交互

- **空间树浏览**：左侧树形导航，右侧选中节点的详情面板，支持按 Campus→Building→Floor→Zone→Room 层级展示
- **空间实例表单**：根据 entityTypeId 动态渲染字段（Campus 显示 address/area；Building 显示 floors/height；Floor 显示 floorNumber/elevation；Room 显示 area/capacity）
- **层次可视化**：D3 树形图展示空间层次，环路节点红色高亮
- **坐标导入**：支持 CSV/Excel 批量导入坐标，标注坐标系

---

## 17. 配置项

```yaml
pig:
  ontology:
    space:
      # 空间层次最大深度
      max-hierarchy-depth: 50
      # 环路检测最大上溯深度
      cycle-detection-max-depth: 50
      # 子树查询默认最大深度
      subtree-default-max-depth: 5
      # 坐标小数精度
      coordinate-precision: 8
      # BIM引用缓存大小
      bim-reference-cache-size: 200
```

---

## 18. 测试与验收

### 18.1 功能验收

1. 能创建 Campus/Building/Floor/Zone/Room 五级空间实例；
2. 能设置 parentSpace 建立空间层次关系；
3. 空间实例有子空间时不可直接删除（返回 `SPACE_DELETE_HAS_CHILDREN`）；
4. parentSpace 指向自身时被拒绝（`SPACE_SELF_REFERENCE`）；
5. parentSpace 形成环路时被拒绝（`SPACE_CYCLE_DETECTED`）；
6. 邻接关系双向建立，删除时双向删除；
7. 能批量导入空间坐标并标注坐标系；
8. 能关联 BIM 元素引用并按 GUID 查询；
9. 重复创建相同 spaceCode 的空间实例不产生重复（幂等）；
10. 空间层次校验能检出环路、孤儿节点和类型不匹配；
11. 设备实例能通过 hasDevice/deviceLocatedIn 关联到空间实例；
12. 空间实例变化能通过 EDA 可靠发布事件。

### 18.2 语义验收

通过 SPARQL 能查询：

- 某园区下有哪些楼宇；
- 某楼宇下有哪些楼层；
- 某楼层下有哪些区域和房间；
- 某房间所在的空间层次路径（Room→Zone→Floor→Building→Campus）；
- 某空间实例有哪些邻接空间；
- 某空间实例内有哪些设备（通过 hasDevice）；
- 某设备位于哪个空间（通过 deviceLocatedIn）；
- 某空间实例的坐标和坐标系；
- 某BIM GUID关联了哪个空间实例。

### 18.3 一致性验收

- 空间扩展本体通过模块11扩展合法性5条规则校验；
- parentSpace functional 约束保证每个空间仅一个父空间（严格树形）；
- adjacentSpace symmetric 约束保证邻接关系双向一致；
- 空间实例的 IRI 基于 spaceCode 稳定生成，spaceCode 变更后实例不变。

---

## 19. 与后续模块的关系

| 后续模块 | 依赖本模块 | 说明 |
|---|---|---|
| 模块28 设备生命周期 | ✅ 依赖 | 设备实例通过 deviceLocatedIn 关联空间 |
| 模块20 物模型层 | ✅ 被依赖 | 设备实例（943010）通过 hasDevice/deviceLocatedIn 关联空间 |
| 模块23 告警规则 | ✅ 依赖 | 告警可关联空间（如某区域温度异常） |
| 模块29 工单/巡检 | ✅ 依赖 | 巡检计划按空间层次编排 |
| 模块31 能源管理 | ✅ 依赖 | 能耗按空间分项计量 |
| 模块32 安防/视频 | ✅ 依赖 | 摄像头/门禁关联空间 |
| 模块33 数字孪生 | ✅ 强依赖 | 数字孪生三维可视化基于空间层次 |
| BIM集成（后续） | ✅ 强依赖 | IFC解析器填充 bimElementGuid 关联 |
| GIS集成（后续） | ✅ 强依赖 | 地图引擎基于空间坐标渲染 |

---

## 20. 交付顺序

```text
阶段1：空间本体种子 + 层次管理
  ├── V26 Flyway 迁移（扩展本体种子 + 层次校验表 + BIM引用表）
  ├── SpaceInstanceService（CRUD + parentSpace + 邻接）
  ├── SpaceHierarchyService（环路检测 + 层次校验 + 路径构建）
  └── 前端空间树浏览 + 空间实例表单

阶段2：坐标与BIM引用
  ├── SpaceCoordinateService（坐标设置 + 批量导入）
  ├── SpaceBimReferenceService（BIM关联 + GUID查询）
  └── 前端坐标管理 + BIM引用页面

阶段3：空间关联集成
  ├── hasDevice/deviceLocatedIn 关联（与模块20设备实例联调）
  ├── hasOccupant/occupantLocatedIn 关联（与组织账号实例联调）
  └── 前端层次可视化 + 邻接关系页面

阶段4：集成验收
  ├── 空间层次→SPARQL查询验收
  ├── 设备→空间关联验收
  ├── 层次校验（环路/自引用/类型不匹配）验收
  └── 安全合规（敏感区域审计/层次变更审批）验收
```

---

## 21. 后续BIM/GIS集成扩展点

### 21.1 BIM集成路线（V2+）

```text
当前（V1）：存储 bimElementGuid/bimElementType/bimModelSource 引用
  ↓
后续（V2+）：
  ├── IFC文件解析器（读取.ifc文件，提取IfcSpace/IfcBuilding等元素）
  ├── IFC→本体映射（IfcBuilding→944003, IfcBuildingStorey→944004, IfcSpace→944006）
  ├── BIM模型版本管理（IFC文件版本快照）
  └── 空间几何属性扩展（房间几何形状、墙体、门窗）
```

### 21.2 GIS集成路线（V2+）

```text
当前（V1）：存储 longitude/latitude/altitude/coordinateSystem 坐标值
  ↓
后续（V2+）：
  ├── 地图引擎集成（Mapbox/Leaflet/Cesium）
  ├── 空间拓扑计算（包含/相交/距离）
  ├── 空间路径规划
  ├── GIS图层管理（卫星图/矢量图/热力图）
  └── 坐标系自动转换（GCJ02↔WGS84↔CGCS2000）
```

### 21.3 预留扩展点

| 扩展点 | 当前状态 | 后续用途 |
|---|---|---|
| `bimElementId`(956036) | 数据属性已建 | BIM元素ID引用 |
| `bimElementType`(956037) | 数据属性已建 | IFC类型分类 |
| `hasBimModel`(964007) | 对象属性已建 | 空间→BimModelReference关联 |
| `boundaryDefinition`(956040) | 数据属性已建 | GeoJSON/WKT空间边界 |
| `coordinateSystem`(956035) | 数据属性已建 | 坐标系标注 |
| `SpaceBoundary`(944010) | 实体类型已建 | 空间几何边界定义 |
| `BimModelReference`(944008) | 实体类型已建 | BIM模型引用容器 |
