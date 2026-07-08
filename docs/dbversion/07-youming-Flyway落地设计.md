# 07 · youming Flyway 落地设计

> 本文档是 youming 项目 Flyway 集成的**执行级落地设计**，在 PG 迁移完成后引入 Flyway 统一管理业务库 `youmingdb` 的结构版本。
> 编写日期：2026-07-08
> 评审依据：[04-Flyway集成设计-评审](./04-Flyway集成设计-评审.md)（方案 A 单点执行 + baseline 标准路径）

---

## 一、设计目标

在 PG 迁移基础上，引入 Flyway 统一管理业务库 `youmingdb` 的 schema 版本，实现：

1. 库结构变更可追溯、可回滚（通过迁移脚本版本管理）。
2. 应用启动自动迁移到最新版本（无需手工导 SQL）。
3. CI 预检保证每次合并的迁移脚本可从零跑通。

---

## 二、Flyway 落点与执行策略

### 2.1 落点：pig-common-data（共享模块自动配置）

| 决策点 | 选择 | 理由 |
|-------|------|------|
| Flyway 依赖声明模块 | `pig-common/pig-common-data` | pig-common-data 是所有业务模块（upms-biz/codegen/quartz/pig-boot）共享的数据访问基础模块，在此声明 flyway-core 依赖 + 自动配置，所有依赖它的模块自动获得 Flyway 能力 |
| 迁移脚本目录 | `pig-common-data/src/main/resources/db/migration/` | classpath 标准位置，所有模块可见 |
| 执行点 | **pig-boot 单体天然单点**；微服务版需单点化（见 2.3） | 落实评审 04 方案 A |

### 2.2 单体版（pig-boot）天然单点

pig-boot 聚合了 auth/upms-biz/codegen/quartz，单进程单 DataSource 连 `youmingdb`。Flyway 在 pig-boot 启动时执行一次，无多服务并发争 history 锁问题。**这是 youming 单体版集成 Flyway 的最大优势**。

### 2.3 微服务版单点化策略（方案 A）

微服务版下，upms-biz/codegen/quartz 三个服务共享 `youmingdb`，若三者同时启动会并发执行 Flyway 争 `flyway_schema_history` 锁。方案 A（评审 04 推荐）：

- **仅 upms-biz 启用 Flyway**（最先启动、依赖最核心），codegen/quartz 通过配置 `spring.flyway.enabled=false` 禁用。
- 或在 pig-common-data 的自动配置类中加条件：仅当配置 `pig.flyway.enabled=true` 时启用，各服务按需开关。

> youming 单体版优先，微服务版单点化配置作为可选增强。

---

## 三、依赖与配置

### 3.1 依赖（pig-common-data/pom.xml）

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

> 版本由 Spring Boot 4.0.7 的 `spring-boot-dependencies` BOM 管理（`flyway-core` / `flyway-database-postgresql`），无需在 pig-common-bom 显式声明版本。

### 3.2 自动配置（application.yml）

pig-boot 的 `application.yml`（或 application-dev.yml）：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true      # 存量库迁移时建立基线
    baseline-version: 0            # 基线版本（V1 之前）
    validate-on-migrate: true      # 迁移前校验
    table: flyway_schema_history   # 历史表名（默认）
    encoding: UTF-8
```

### 3.3 baseline 标准路径（youming 空库场景）

youming 为**全新空库迁移**，采用评审 04 推荐的标准路径：

1. 目标库 `youmingdb` 为空（无表）。
2. Flyway 启动时检测到空库，直接执行所有 V1 脚本建表 + 种子数据。
3. `baseline-on-migrate=true` 对空库无副作用（空库不会触发 baseline，直接 migrate）。

> 这规避了"存量库 baseline"的复杂性（评审 04-5.2 分析正确）。

---

## 四、迁移脚本组织

### 4.1 命名约定

采用 Flyway 标准版本号命名（非时间戳，youming 项目规模适中，版本号更直观）：

```
db/migration/
├── V1__init_schema.sql          # 基线：38 业务表结构（复用迁移产物）
├── V1__init_data.sql            # ❌ 错误：Flyway 版本号唯一
```

修正为：

```
db/migration/
├── V1__init_schema.sql          # 基线：38 业务表结构
├── V2__init_seed_data.sql       # 基线：种子数据（菜单/字典/角色等）
├── V3__init_quartz_tables.sql   # 基线：Quartz 11 表（官方 PG 脚本）
└── V4__xxx.sql                  # 后续增量变更（按需）
```

> 将迁移产物的 3 个文件（schema / data / quartz）映射为 V1/V2/V3 三个连续版本，确保执行顺序：建表 → 种子数据 → Quartz 表。Flyway 按版本号顺序执行，保证依赖正确。

### 4.2 不可变规则（评审 04-3.3）

- ✅ 已应用的脚本**不可修改**（checksum 变更会导致启动失败）。
- ✅ 版本号**不可复用**（V1 已用，下一个增量必须 V2+）。
- ✅ 需修正已应用脚本时用 `flyway repair`（重算 checksum）或新版本脚本覆盖。

### 4.3 失败处理（评审 04-7.3）

- 迁移失败导致 `flyway_schema_history` 有 failed 记录时，应用启动失败。
- 修复：`flyway repair`（清理 failed 记录）→ 修正脚本 → 重启。

---

## 五、与现有机制的边界（评审 04 第 8 节）

### 5.1 MyBatis-Plus

Flyway 管库结构（DDL + 种子数据），MyBatis-Plus 管运行时 CRUD。两者职责清晰：

- Flyway 在应用启动早期（DataSource 初始化后、MyBatis 初始化前）执行迁移。
- MyBatis-Plus 的 `MybatisPlusMetaObjectHandler`（审计字段兜底）在运行时生效，与 Flyway 无冲突。
- Flyway 建表时审计字段用 `DEFAULT now()`，应用层 isCover 总覆盖，无冲突。

### 5.2 动态数据源（codegen 的 gen_datasource_conf）

- **Flyway 仅管业务库 `youmingdb`**，不介入 codegen 运行时连接的第三方动态数据源（`gen_datasource_conf` 表配置的库）。
- codegen 通过 anyline 连动态数据源做元数据解析，这些库的结构由各自管理，Flyway 不接管（评审 04-8 判断正确）。

### 5.3 Nacos 元数据库（pig_config / youming_config）

- **不纳入 pig 的 Flyway**（评审 04-1.3 边界正确）。
- Nacos 库 schema 由 Nacos 自身 `pg-schema.sql` 管理。

### 5.4 Quartz 表

- Quartz 11 表（`qrtz_*`）纳入 Flyway V3 基线（与业务表同库 `youmingdb`）。
- `quartz-config.yml` 的 `initialize-schema: never` 保持不变（由 Flyway 管理建表，Quartz 不自动建）。

---

## 六、Druid wall 共存（评审 04-N4）

### 6.1 风险

pig-boot 的 DataSource 是 Druid（`DruidDataSource`），配置了 `wall` 过滤器（防 SQL 注入）。wall 可能拦截 Flyway 执行的 DDL（如 `CREATE INDEX`、`COMMENT ON` 等），导致启动失败。

### 6.2 验证步骤（集成时执行）

1. 先用 pig-boot 连 PG 启动一次，观察日志是否有 wall 拦截报错。
2. 若无报错 → wall 不拦标准 DDL，无需特殊处理。
3. 若有报错 → 启用 6.3 的独立 DataSource 方案。

### 6.3 独立 DataSource 方案（wall 拦截时的对策）

为 Flyway 配置无 wall 过滤器的独立 DataSource（评审 04-N4 配置示例）：

```java
import org.flywaydb.core.api.configuration.FlywayConfigurationCustomizer;
// 或直接用 @FlywayDataSource

@Bean
@FlywayDataSource
public DataSource flywayDataSource(DataSourceProperties properties) {
    // 不挂 wall/stat 等 Druid filter 的纯 JDBC DataSource
    return DataSourceBuilder.create()
        .url(properties.getUrl())
        .username(properties.getUsername())
        .password(properties.getPassword())
        .driverClassName(properties.getDriverClassName())
        .build();
}
```

或在 wall config 显式放行 DDL（按实际拦截项调）：

```yaml
spring:
  datasource:
    druid:
      filter:
        wall:
          config:
            alter-table-allow: true       # 允许 ALTER TABLE
            none-base-statement-allow: true  # 允许非基础语句（COMMENT ON 等）
```

> 优先验证（6.2），若不拦则不引入额外复杂度。youming 基线脚本以 `CREATE TABLE` / `CREATE INDEX` / `COMMENT ON` / `INSERT` 为主，wall 默认配置大概率不拦。

---

## 七、CI 预检（评审 04-6.5）

### 7.1 干净库全量 migrate 预检

每次合并 PR 前，CI 执行：

1. 起一个空 PG 容器（或用 docker-compose 的 postgres 服务）。
2. 从零跑 `flyway migrate`（应用全部 V1~Vn 脚本）。
3. 验证无报错、`flyway_schema_history` 记录完整。

保证每次合并的迁移脚本可从零跑通——这是 Flyway 用得稳的关键工程化动作。

### 7.2 命名时间线校验（评审 04-N6，youming 用版本号则简化）

youming 采用版本号命名（V1/V2/Vn）而非时间戳，天然有序，无需"规划日"防歧义。CI 预检简化为：

- 新脚本版本号 > 已有最大版本号（Flyway 自身强制，违反即报错）。

---

## 八、验证 SOP（Flyway 集成后）

### 8.1 干净库自动迁移

- [ ] 清空 `youmingdb`（DROP 所有表，或重建库）
- [ ] 启动 pig-boot，观察日志：Flyway 自动执行 V1/V2/V3
- [ ] `flyway_schema_history` 表有 3 条 Success 记录
- [ ] 业务表/种子数据/Quartz 表全部建成

### 8.2 增量迁移

- [ ] 新增 V4__test.sql（如加一列），启动 pig-boot
- [ ] 验证 V4 执行成功，列已添加
- [ ] 验证 V1~V3 不重复执行

### 8.3 功能回归

- [ ] pig-boot 全功能正常（登录/权限/代码生成/Quartz）
- [ ] 与未集成 Flyway 前行为一致

---

## 九、与评审文档的对应关系

| 本设计章节 | 对应评审文档条目 |
|-----------|----------------|
| 二、方案 A 单点执行 | [04-评审](./04-Flyway集成设计-评审.md) 三、方案对比 A/B/C/D |
| 三、baseline 标准路径 | [04-评审](./04-Flyway集成设计-评审.md) N1（youming 空库闭环） |
| 四、不可变规则 | [04-评审](./04-Flyway集成设计-评审.md) 三、亮点 3 |
| 五、与现有机制边界 | [04-评审](./04-Flyway集成设计-评审.md) 三、亮点 4 + N3 |
| 六、Druid wall | [04-评审](./04-Flyway集成设计-评审.md) N4（方案已备） |
| 七、CI 预检 | [04-评审](./04-Flyway集成设计-评审.md) 三、亮点 5 + N6 |
