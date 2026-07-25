# 06 · youming 迁移执行设计（MySQL → PostgreSQL）

> 本文档是 youming 项目（基于 pig 二次开发）PostgreSQL 迁移的**执行级落地设计**，依据已复核的评审文档（00~05）与本仓 `server/` 源码现状编写。
> 编写日期：2026-07-08
> 评审依据：[00-总评审报告](./00-总评审报告.md)（含 2026-07-08 复核结论）

---

## 一、迁移目标与范围

### 1.1 目标

将 youming 项目从 MySQL 彻底迁移到 PostgreSQL，**完全去除对 MySQL 的依赖**。

### 1.2 范围

| 范围 | 是否迁移 | 说明 |
|------|---------|------|
| 业务库（原 `pig`，49 张表） | ✅ 迁移 | 38 业务表手工转换 DDL + 11 Quartz 表用官方 PG 脚本；库名改为 `mingwanwudb` |
| Nacos 元数据库（原 `pig_config`） | ✅ 迁移 | 由 Nacos 3.2.2 原生 PG 支持（`nacos-datasource-plugin-postgresql` 内 `pg-schema.sql`）初始化 |
| Redis | ❌ 不涉及 | 无变化 |
| 数据迁移 | 仅种子数据 | youming 为新项目，无存量业务数据；`db/pig.sql` 中 635 行 INSERT（菜单/字典/角色等种子）需迁入 |

### 1.3 不迁移项（边界）

- **Nacos 元数据库的 schema 不由应用层 Flyway 管理**——由 Nacos 自身 `pg-schema.sql` 管理（评审 04-1.3 边界正确）。
- **codegen 的动态数据源**（`gen_datasource_conf` 表配置的第三方库）不纳入本次迁移，保持其运行时动态特性。

---

## 二、现状核实结论（迁移依据）

### 2.1 库结构

| 特性 | 数量/现状 | 迁移处理 |
|------|----------|---------|
| 表总数 | 49（38 业务 + 11 qrtz） | 业务表转换，qrtz 用官方脚本 |
| `ENGINE=` / `CHARSET=` / `COLLATE=` | 49 表表尾 | 全部移除 |
| 列级 `CHARACTER SET utf8mb4 COLLATE ...` | 大量 | 全部移除（PG 用库级编码） |
| 反引号标识符 `` ` `` | 1338 处 | 全部去除（PG 标识符折叠小写，不加引号） |
| `datetime` | 92 处 | → `timestamp` |
| `timestamp` | 5 处（sys_job 3 + 2） | → `timestamp` |
| `DEFAULT CURRENT_TIMESTAMP` | 18 处 | → `DEFAULT now()`（审计字段；与应用层 MetaObjectHandler 不冲突，应用层 isCover 总覆盖） |
| `ON UPDATE CURRENT_TIMESTAMP`（列级） | 11 处 | **移除**（应用层 `MybatisPlusMetaObjectHandler` 兜底） |
| `bigint unsigned` / `int unsigned` | 大量 | → `bigint` / `integer`（PG 无 unsigned） |
| `longtext` | 2 处（sys_system_config.config_value 等） | → `text` |
| `blob` | 4 处（全在 qrtz） | → `bytea`（随 qrtz 官方脚本处理） |
| `AUTO_INCREMENT` | 1 处（gen_field_type） | **不用 bigserial**（GenFieldType 是 ASSIGN_ID 雪花，用普通 bigint） |
| 业务表外键约束 | 0 处 | 无需处理（qrtz 的外键随官方脚本） |
| 列级 `KEY` / `UNIQUE KEY` | 若干 | 拆为 `CREATE INDEX` / `CREATE UNIQUE INDEX` |
| 列级 `COMMENT` | 520 处 | 拆为 `COMMENT ON COLUMN tbl.col IS '...'` |
| 表级 `COMMENT` | 49 处 | 拆为 `COMMENT ON TABLE tbl IS '...'` |
| `DROP TABLE IF EXISTS` | 49 处 | 保留（幂等，Flyway 下转为 CREATE IF NOT EXISTS 或由 Flyway 管理生命周期） |
| 字符串转义 `\'` / `\"` | sys_oauth_client_details / gen_template 等含 | `\'`→`''`、`\"`→外层单引号；长串用美元引号 `$$...$$` |
| 种子数据 INSERT | 635 行 | 转义后迁入 |

### 2.2 代码层（经源码核实，详见 [00-总评审报告](./00-总评审报告.md) 第二节）

- ✅ **Mapper 手写 SQL 零重写**：18 个 XML，8 个含手写 SQL，MySQL 专有函数（`IF/FIND_IN_SET/GROUP_CONCAT/DATE_FORMAT/IFNULL/NOW()/SUBSTRING_INDEX`）0 命中，全 ANSI 兼容。
- ✅ **主键应用层雪花**：业务实体普遍 `IdType.ASSIGN_ID`（含 GenFieldType），PG 表用 bigint 无序列。
- ✅ **审计字段应用层兜底**：`MybatisPlusMetaObjectHandler.insertFill/updateFill` 实现 createTime/updateTime 兜底，`ON UPDATE` 可安全移除。
- ✅ **DsTypeEnum.PG 已存在**：URL 模板 `jdbc:postgresql://%s:%s/%s` + anyline `PostgresqlAdapter` 齐全。
- ⚠️ **codegen 缺 anyline PG 适配器**：`pig-visual/pig-codegen/pom.xml` 仅引 `anyline-data-jdbc-mysql`，需补 `anyline-data-jdbc-postgresql`（BOM 已管理版本）。
- ⚠️ **MybatisPlusConfiguration 未显式设 DbType**：`PaginationInnerInterceptor` 用无参构造依赖自动探测，迁移后建议显式 `DbType.POSTGRE_SQL`。
- ✅ **PG 驱动已在依赖**：upms-biz/codegen/quartz 三处声明 `org.postgresql:postgresql`。
- ✅ **Nacos 3.2.2 原生支持 PG**：`nacos-datasource-plugin-postgresql-3.2.2.jar` 内含 `META-INF/pg-schema.sql`（13 张表）+ `PostgresqlDatabaseDialect`。

### 2.3 连接配置（3 处硬编码 MySQL，需改造）

| # | 位置 | 当前值 | 改造 |
|---|------|-------|------|
| C1 | `server/pig-boot/src/main/resources/application-dev.yml`（单体业务库） | `com.mysql.cj.jdbc.Driver` + `jdbc:mysql://pig-mysql:3306/pig?...` | → `org.postgresql.Driver` + `jdbc:postgresql://127.0.0.1:5432/mingwanwudb`，移除 MySQL 专有参数 |
| C2 | `server/pig-register/src/main/resources/application.properties`（Nacos 元数据库） | `spring.sql.init.platform=mysql` + `db.url.0=jdbc:mysql://...pig_config` | → `platform=postgresql` + `jdbc:postgresql://.../youming_config`（库名按实际） |
| C3 | `server/db/pig_config.sql` 内 3 个 data_id 的 content（微服务版 Nacos 下发的 pig-codegen/pig-upms-biz/pig-quartz 的 yml） | 内含 `jdbc:mysql://${MYSQL_HOST}:...` | → PG 连接（供微服务版） |

> 单体版 pig-boot 不经 Nacos，datasource 在本地 `application-dev.yml`，故**优先验证单体版**（C1），再同步微服务版配置（C3）。

---

## 三、本机环境对接

本机通过 1Panel 管理 Docker，基础设施已就绪（详见根 `AGENTS.md`）：

| 服务 | 容器 | 迁移用途 |
|------|------|---------|
| PostgreSQL 18.4 | `1Panel-postgresql-ANBv`（5432） | **目标库**；已建库 `mingwanwudb`；用户 `user_PAmcy2` / 密码 `password_bkQ4JT` |
| Nacos 3.2.2 | `1Panel-nacos-4GLs`（8848） | 配置中心（微服务版用）；可复用本机容器或内嵌 pig-register |
| Redis 8.8 | `1Panel-redis-2G4M`（6379） | 缓存，无变化 |
| MySQL 8.4 | `1Panel-mysql-AFGI`（3306） | 迁移源（逐步弃用） |

**操作 PG 的便捷命令**（本机无 psql CLI，通过 docker exec）：

```bash
# 建库 / 执行 SQL
docker exec -e PGPASSWORD=password_bkQ4JT 1Panel-postgresql-ANBv \
  psql -U user_PAmcy2 -d postgres -c "CREATE DATABASE mingwanwudb;"

# 导入 SQL 文件
docker exec -i -e PGPASSWORD=password_bkQ4JT 1Panel-postgresql-ANBv \
  psql -U user_PAmcy2 -d mingwanwudb < server/db/postgresql/V1__init_schema.sql
```

---

## 四、Quartz 迁移

### 4.1 表结构

Quartz 11 张表（`qrtz_*`）用官方 PostgreSQL 建表脚本 `tables_postgres.sql` 替换 MySQL 版（来源：Quartz 发行包 `docs/dbTables/tables_postgres.sql`）。

- 脚本落地：`server/db/postgresql/quartz/tables_postgres.sql`
- 该脚本含 `DROP TABLE` + `CREATE TABLE`，含 `bytea`（blob 列）、外键约束（`ON DELETE RESTRICT`/`ON UPDATE RESTRICT`），与 MySQL 版语义一致。

### 4.2 配置变更

`server/pig-visual/pig-quartz/src/main/resources/quartz-config.yml`：

```yaml
# 改前
org:
  quartz:
    jobStore:
      driverDelegateClass: org.quartz.impl.jdbcjobstore.StdJDBCDelegate

# 改后
      driverDelegateClass: org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
```

- `initialize-schema: never` 保持不变（表由脚本预置 / Flyway 基线管理）。
- `PostgreSQLDelegate` 提供原生 PG 方言兼容（布尔类型、`SELECT FOR UPDATE` 等）。

---

## 五、Nacos 元数据库迁移

### 5.1 平台切换

`server/pig-register/src/main/resources/application.properties`：

```properties
# 改前
spring.sql.init.platform=mysql
db.url.0=jdbc:mysql://${MYSQL_HOST:pig-mysql}:${MYSQL_PORT:3306}/${MYSQL_DB:pig_config}?...
db.user=root
db.password=root

# 改后
spring.sql.init.platform=postgresql
db.url.0=jdbc:postgresql://${PG_HOST:127.0.0.1}:${PG_PORT:5432}/${PG_DB:youming_config}?...
db.user=user_PAmcy2
db.password=password_bkQ4JT
```

### 5.2 schema 初始化

Nacos 启动时据 `platform=postgresql` 自动加载 `META-INF/pg-schema.sql`（来自 `nacos-datasource-plugin-postgresql-3.2.2.jar`），在目标库创建 13 张表（config_info / his_config_info / roles / users / tenant_info 等）。**无需手工转译 `db/pig_config.sql`**。

### 5.3 种子配置导入

`db/pig_config.sql` 中的 7 条 `config_info` INSERT（业务 datasource 等配置）需重新导入到 PG 的 config_info 表。其中 3 条含 `jdbc:mysql` 的 content（C3）需先改为 PG 连接串，再导入。

### 5.4 复用本机 Nacos 容器（可选）

本机已有运行的 Nacos v3.2.2 容器（`1Panel-nacos-4GLs`），微服务版可直接连该容器，**不一定需要内嵌 pig-register**。若复用，需将该容器的元数据源也配置为 PG（在容器 `application.conf` 或环境变量中设 `nacos.datasource.platform=postgresql`）。

---

## 六、DDL 转换产物与执行

### 6.1 产物文件

新建 `server/db/postgresql/`：

| 文件 | 内容 | 用途 |
|------|------|------|
| `V1__init_schema.sql` | 38 业务表 PG DDL（CREATE TABLE + CREATE INDEX + COMMENT ON） | 建表（复用为 Flyway 基线 V1） |
| `V1__init_data.sql` | 635 行种子数据 INSERT（转义后） | 种子数据（复用为 Flyway 基线） |
| `quartz/tables_postgres.sql` | Quartz 11 表 PG 官方脚本 | Quartz 表 |

### 6.2 转换规则（详见 [08-DDL转换结果核验清单](./08-DDL转换结果核验清单.md)）

逐表转换要点：

1. **标识符**：去除反引号，全小写（PG 折叠为小写，与 MyBatis-Plus 默认一致）。
2. **类型映射**：`bigint unsigned`→`bigint`、`int unsigned`→`integer`、`datetime/timestamp`→`timestamp`、`longtext`→`text`、`tinyint`→`smallint`、`char(1)`保持。
3. **默认值**：`DEFAULT CURRENT_TIMESTAMP`→`DEFAULT now()`；`ON UPDATE CURRENT_TIMESTAMP` 删除；字符串默认 `''` 保持。
4. **COMMENT 拆分**：列级 `COMMENT 'xx'` → 表后追加 `COMMENT ON COLUMN tbl.col IS 'xx';`；表级 → `COMMENT ON TABLE tbl IS 'xx';`。
5. **索引拆分**：`PRIMARY KEY` 保留在表内；`KEY idx (col)` → `CREATE INDEX idx ON tbl (col);`；`UNIQUE KEY uk (col)` → `CREATE UNIQUE INDEX uk ON tbl (col);`。
6. **移除**：`ENGINE=`、`CHARSET=`、`COLLATE=`、`CHARACTER SET`、`ROW_FORMAT`、`SET FOREIGN_KEY_CHECKS`、`SET NAMES`、`AUTO_INCREMENT`。
7. **gen_field_type**：`id` 用 `bigint NOT NULL`（ASSIGN_ID 雪花，无序列）。
8. **种子数据转义**：`\'`→`''`、`\"`→外层单引号；含特殊字符的长串用美元引号 `$$...$$`。

### 6.3 验证执行顺序（本阶段无 Flyway，直导验证 DDL）

```bash
# 1. 建库（已存在则跳过）
docker exec -e PGPASSWORD=password_bkQ4JT 1Panel-postgresql-ANBv \
  psql -U user_PAmcy2 -d postgres -c "CREATE DATABASE mingwanwudb;"

# 2. 导入业务表结构
docker exec -i -e PGPASSWORD=password_bkQ4JT 1Panel-postgresql-ANBv \
  psql -U user_PAmcy2 -d mingwanwudb < server/db/postgresql/V1__init_schema.sql

# 3. 导入 Quartz 表
docker exec -i -e PGPASSWORD=password_bkQ4JT 1Panel-postgresql-ANBv \
  psql -U user_PAmcy2 -d mingwanwudb < server/db/postgresql/quartz/tables_postgres.sql

# 4. 导入种子数据
docker exec -i -e PGPASSWORD=password_bkQ4JT 1Panel-postgresql-ANBv \
  psql -U user_PAmcy2 -d mingwanwudb < server/db/postgresql/V1__init_data.sql
```

> Flyway 集成后（任务 3），上述脚本迁入 `db/migration/`，由应用启动自动执行，无需手工导入。

---

## 七、代码改造清单

| # | 文件 | 改造 |
|---|------|------|
| 1 | `server/pig-visual/pig-codegen/pom.xml` | 新增 `anyline-data-jdbc-postgresql` 依赖（让 codegen 连 PG 源做元数据解析） |
| 2 | `server/pig-common/pig-common-data/.../MybatisPlusConfiguration.java` | `new PaginationInnerInterceptor()` → `new PaginationInnerInterceptor(DbType.POSTGRE_SQL)`（显式方言） |
| 3 | `server/pig-visual/pig-quartz/.../quartz-config.yml` | `driverDelegateClass` → `PostgreSQLDelegate` |
| 4 | `server/pig-boot/src/main/resources/application-dev.yml` | driver → `org.postgresql.Driver`，url → `jdbc:postgresql://127.0.0.1:5432/mingwanwudb` |
| 5 | `server/pig-register/src/main/resources/application.properties` | `platform=postgresql` + `db.url.0=jdbc:postgresql://...` |
| 6 | `server/db/pig_config.sql`（3 个 data_id content） | jdbc:mysql → jdbc:postgresql（微服务版配置） |
| 7 | `server/docker-compose.yml` / `docker-compose-boot.yml` | 新增 postgres 服务定义或注释说明连本机 1Panel-postgresql |

---

## 八、验证 SOP（pig-boot 单体优先）

### 8.1 DDL 验证

- [ ] V1__init_schema.sql 导入无报错（38 表全部建成）
- [ ] quartz/tables_postgres.sql 导入无报错（11 表）
- [ ] V1__init_data.sql 导入无报错（种子数据行数核对）
- [ ] `SELECT count(*) FROM sys_menu;` 等关键表行数与 MySQL 源一致

### 8.2 pig-boot 启动验证

- [ ] pig-boot 连接 PG 启动无报错（验证 C1 配置）
- [ ] 登录接口正常（验证 sys_user 种子数据、密码校验）
- [ ] 分页查询正常（验证 DbType.POSTGRE_SQL）
- [ ] 菜单/角色/字典查询正常（验证种子数据完整性）

### 8.3 功能验证

- [ ] 代码生成器连 PG 数据源正常（验证 anyline PG adapter）
- [ ] Quartz 调度任务正常（验证 PostgreSQLDelegate + qrtz 表）
- [ ] 审计字段自动填充正常（验证 MetaObjectHandler，create_time/update_time 写入）

### 8.4 微服务版配置同步（C3，可选）

- [ ] Nacos 中 pig-codegen/pig-upms-biz/pig-quartz 的 data_id content 改为 PG
- [ ] 微服务版启动验证

---

## 九、回滚方案

### 9.1 回滚触发条件

- pig-boot 连 PG 启动失败且短期无法修复
- 核心业务功能（登录、权限、代码生成）异常

### 9.2 回滚步骤（< 30 分钟）

1. **配置回切**：`application-dev.yml` 的 driver/url 改回 MySQL（git revert 配置文件即可）。
2. **代码回切**：`git revert` 本次迁移提交（DbType、pom 依赖、quartz-config 等）。
3. **数据**：MySQL 源库（`1Panel-mysql-AFGI`）保持不变，无需恢复。
4. **验证**：pig-boot 连 MySQL 启动，确认功能正常。

### 9.3 回滚保障

- 迁移前 MySQL 源库不删除、不改动，作为回滚后援。
- 所有改动通过 git 提交，`git revert` 可精确回退。
- PG 的 mingwanwudb 可保留（不干扰 MySQL 回滚）。

---

## 十、与评审文档的对应关系

| 本设计章节 | 对应评审文档条目 |
|-----------|----------------|
| 二、现状核实 | [00-总评审报告](./00-总评审报告.md) 第二节证据链（2026-07-08 新增核验） |
| 二、gen_field_type 处理 | [00 N2](./00-总评审报告.md)（已核实 ASSIGN_ID，闭环） |
| 二、ON UPDATE 移除 | [03-评审](./03-DDL转换映射参考-评审.md) S2（已核实 FieldFill.UPDATE） |
| 六、DDL 转换规则 | [08-DDL转换结果核验清单](./08-DDL转换结果核验清单.md) |
| 六、不用 pgloader | [00 N1](./00-总评审报告.md)（youming 空库迁移闭环） |
| 五、Nacos 迁移 | [00 N3](./00-总评审报告.md)（Nacos 3.2.2 原生支持 PG，风险消除） |
| 四、Quartz 迁移 | [02-评审](./02-迁移详细设计-评审.md) S1（quartz-config.yml 在 pig-quartz，已核实） |
| Flyway 集成 | [07-youming-Flyway落地设计](./07-youming-Flyway落地设计.md) |
