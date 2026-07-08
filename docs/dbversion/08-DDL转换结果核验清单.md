# 08 · DDL 转换结果核验清单

> 本文档是 `db/pig.sql`（MySQL）→ PostgreSQL DDL 转换的**人工复核清单**，逐表列出转换要点与易错项，作为 V1__init_schema.sql / V1__init_data.sql 生成后的核对依据。
> 编写日期：2026-07-08
> 转换规则依据：[03-DDL转换映射参考-评审](./03-DDL转换映射参考-评审.md)

---

## 一、全局转换规则（所有表适用）

转换脚本/手工转换时，以下规则对 38 张业务表逐表适用：

| # | MySQL 特性 | PostgreSQL 处理 | 核验 |
|---|-----------|----------------|------|
| G1 | 反引号标识符 `` `col` `` | 去除反引号，全小写 | grep 确认无反引号残留 |
| G2 | 表尾 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=... ROW_FORMAT=DYNAMIC` | 全部移除 | grep 确认无 ENGINE/CHARSET/COLLATE/ROW_FORMAT |
| G3 | 列级 `CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci` | 全部移除 | grep 确认无 CHARACTER SET |
| G4 | `SET NAMES utf8mb4;` / `SET FOREIGN_KEY_CHECKS=0;` | 移除 | 脚本头无 SET 语句 |
| G5 | `bigint unsigned` / `int unsigned` / `tinyint unsigned` | `bigint` / `integer` / `smallint`（PG 无 unsigned） | grep 确认无 unsigned |
| G6 | `datetime` | `timestamp` | 92 处 datetime 全转换 |
| G7 | `DEFAULT CURRENT_TIMESTAMP` | `DEFAULT now()` | 18 处转换 |
| G8 | `ON UPDATE CURRENT_TIMESTAMP`（列级） | **删除**（应用层 MetaObjectHandler 兜底） | 11 处列级删除（不含 qrtz 外键的 ON UPDATE RESTRICT） |
| G9 | `AUTO_INCREMENT` | 移除（GenFieldType 用 ASSIGN_ID 雪花，bigint 无序列） | 1 处（gen_field_type）不用 bigserial |
| G10 | 列级 `COMMENT 'xx'` | 拆为表后 `COMMENT ON COLUMN tbl.col IS 'xx';` | 520 处拆分 |
| G11 | 表级 `COMMENT='xx'` | 拆为 `COMMENT ON TABLE tbl IS 'xx';` | 38 处拆分 |
| G12 | `PRIMARY KEY (col) USING BTREE` | `PRIMARY KEY (col)`（移除 USING BTREE，PG 默认 btree） | grep 确认无 USING BTREE |
| G13 | 列级 `KEY idx (col) USING BTREE` | `CREATE INDEX idx ON tbl (col);` | 见各表 |
| G14 | 列级 `UNIQUE KEY uk (col) USING BTREE` | `CREATE UNIQUE INDEX uk ON tbl (col);` | 见各表 |
| G15 | `longtext` | `text` | sys_system_config.config_value |
| G16 | `text` | `text`（保持） | 多处 |
| G17 | `char(1)` / `varchar(n)` | 保持（PG 原生支持） | — |
| G18 | `decimal(m,n)` | `numeric(m,n)`（或保持 decimal，PG 支持 decimal 作为 numeric 别名） | sys_clarity_data |
| G19 | `json` | `json`（保持）或 `jsonb` | gen_field_type 数据中的类型映射值 |
| G20 | `DROP TABLE IF EXISTS` | 保留（幂等）；Flyway 下可不带（Flyway 管理生命周期） | — |

### 字符串转义规则（种子数据，G21~G23，重点核验 gen_template）

| # | MySQL | PostgreSQL | 说明 |
|---|-------|-----------|------|
| G21 | `\'`（反斜杠转义单引号） | `''`（两个单引号） | PG 默认 `standard_conforming_strings=on` |
| G22 | `\"`（反斜杠转义双引号） | 外层用单引号包裹，内部 `"` 原样 | — |
| G23 | 含大量 `\n`、`${}`、`#if` 的长串（gen_template.template_code） | 优先用**美元引号** `$$...$$` 包裹，避免转义地狱 | gen_template 种子数据是 Velocity 模板，含模板语法 |

> ⚠️ **gen_template.template_code 是转换最大难点**：该列存储代码生成器的 Velocity 模板，含大量 `\n`（换行转义）、`${package}`（变量）、`#if/#end`（指令）、`'`（Java 字符串）。转换时需：①MySQL 的 `\n` 还原为真实换行；②用美元引号 `$$ ... $$` 包裹整个模板；③核对生成的 Java 文件可被代码生成器正确渲染。详见 [06-youming迁移执行设计](./06-youming迁移执行设计.md) 8.3。

---

## 二、逐表转换核验清单（38 业务表）

### 2.1 sys_ 前缀业务表（27 张 + sys_job/sys_job_log 2 张）

| 表名 | 主键 | 特殊列/索引 | 转换要点 |
|------|------|------------|---------|
| sys_area | id | — | 标准，G1~G18 |
| sys_api_key | id | UNIQUE uk_api_key_hash(api_key_hash)；KEY idx_user_id(user_id) | G13/G14：拆 2 个索引为 CREATE INDEX |
| sys_dept | dept_id | PK USING BTREE | G12：移除 USING BTREE |
| sys_dict | id | KEY sys_dict_del_flag(del_flag) | G13：拆索引 |
| sys_dict_item | id | 3 个 KEY（item_value/label/del_flag） | G13：拆 3 个索引 |
| sys_file | id | — | 标准 |
| sys_file_group | id | — | 标准 |
| sys_sensitive_word | sensitive_id | — | 主键名 sensitive_id（非 id），注意 COMMENT ON COLUMN 列名 |
| sys_i18n | id | — | 标准 |
| sys_log | id | 3 个 KEY（request_uri/log_type/create_time） | G13：拆 3 个索引 |
| sys_menu | menu_id | — | 主键 menu_id；种子数据量大（菜单树），核对行数 |
| sys_oauth_client_details | id | — | ⚠️ 含 OAuth 客户端密钥，种子数据可能有转义，G21~G23 核验 |
| sys_post | post_id | — | 主键 post_id |
| sys_public_param | public_id | — | 主键 public_id |
| sys_role | role_id | KEY role_idx1_role_code(role_code) | G13：拆索引 |
| sys_role_menu | (role_id,menu_id) | 复合主键 | G12：复合 PK 保持 |
| sys_role_widget | id | — | 标准 |
| sys_schedule | id | content: text | G16：text 保持 |
| sys_social_details | id | — | 标准 |
| sys_clarity_data | id | device_data: text；pages_per_session: decimal(10,2) | G16/G18：text 保持、decimal→numeric |
| sys_user | user_id | — | ⚠️ updateTime = `@TableField(fill = FieldFill.UPDATE)`（已核实 S2），ON UPDATE 移除后应用层兜底；密码字段种子数据核对 |
| sys_user_post | (user_id,post_id) | 复合主键 | G12：复合 PK |
| sys_user_role | (user_id,role_id) | 复合主键 | G12：复合 PK |
| sys_message | id | content: text | G16：text 保持 |
| sys_message_relation | id | content: text | G16：text 保持 |
| sys_system_config | id | **config_value: longtext** | **G15：longtext → text（本库唯一 longtext 列）** |
| sys_user_dept | (user_id,dept_id) | 复合主键 | G12：复合 PK |
| **sys_job** | (job_id,job_name,job_group) | **3 列 timestamp**（start_time/previous_time/next_time） | G6：timestamp 保持；复合主键；Quartz 调度业务表（非 qrtz_ 前缀） |
| **sys_job_log** | job_log_id | — | 标准；日志表，无 ON UPDATE |

### 2.2 gen_ 前缀代码生成器表（9 张）

| 表名 | 主键 | 特殊列/索引 | 转换要点 |
|------|------|------------|---------|
| gen_datasource_conf | id | url: text | G16：text 保持；codegen 动态数据源配置（Flyway 不管第三方库，但本表在业务库内） |
| **gen_field_type** | id | **UNIQUE column_type(column_type)** | **G9：id 用 bigint 无序列（ASSIGN_ID 雪花，不用 bigserial）；G14：拆 UNIQUE 索引；种子数据含 PG 类型映射（int8/timestamptz/json 等），核对完整性** |
| gen_form_conf | id | form_info: text；KEY table_name(table_name) | G13/G16：拆索引 + text 保持 |
| gen_group | id | — | 标准 |
| gen_table | id | UNIQUE table_name_idx(table_name,ds_name) | G14：拆复合 UNIQUE 索引 |
| gen_table_column | id | — | 标准 |
| **gen_template** | id | **template_code: text（Velocity 模板）** | **G16/G23：text 保持；⚠️ 种子数据 template_code 含大量 `\n`/`${}`/`#if`，转换最大难点，用美元引号包裹，逐一核对模板可渲染** |
| gen_template_group | (group_id,template_id) | 复合主键 | G12：复合 PK |
| gen_create_table | id | column_info: text | G16：text 保持 |

### 2.3 Quartz 表（11 张，单独处理）

| 处理方式 | 说明 |
|---------|------|
| **不用手工转换** | 直接用 Quartz 官方 `tables_postgres.sql`（11 表） |
| 文件位置 | `server/db/postgresql/quartz/tables_postgres.sql` |
| 关键列 | blob → bytea（官方脚本已处理）；外键 ON DELETE/UPDATE RESTRICT 保留（PG 支持） |
| 配套配置 | `quartz-config.yml` 的 `driverDelegateClass` → `PostgreSQLDelegate` |

---

## 三、种子数据核验（V1__init_data.sql / Flyway V2）

### 3.1 含种子数据的表（635 行 INSERT）

转换后按表核对行数，确保与 MySQL 源一致：

| 表名 | 核验要点 |
|------|---------|
| sys_menu | 菜单树完整性（parentId 层级） |
| sys_role | 角色 seed（含 admin/ROLE_） |
| sys_role_menu | 角色-菜单关联行数 |
| sys_dict / sys_dict_item | 字典 seed |
| sys_user | admin 用户 seed（密码哈希、部门关联） |
| sys_user_role | admin 角色关联 |
| sys_dept | 部门树 seed |
| sys_post | 岗位 seed |
| sys_oauth_client_details | OAuth 客户端 seed（client_id/secret，⚠️ 转义） |
| sys_public_param | 公共参数 seed |
| sys_field_type | **代码生成器类型映射 seed（含 PG 类型 int8/timestamptz/json/jsonb 等，核对 22 种类型完整）** |
| gen_template | **⚠️ 代码生成器模板 seed（template_code 含复杂转义，逐一核对）** |
| gen_template_group | 模板分组关联 |
| 其他 | 视实际 INSERT 而定 |

### 3.2 字符串转义重点核验表

| 表名 | 列 | 风险 | 核验方法 |
|------|-----|------|---------|
| gen_template | template_code | `\n`/`${}`/`#if`/`'` 混杂 | 转换后用代码生成器实际生成一次代码，验证模板可渲染 |
| sys_oauth_client_details | web_server_redirect_uri 等 | URL 含特殊字符 | 导入后查询验证 |
| sys_dict_item | remark 等 | 可能含引号 | 导入后查询验证 |

---

## 四、转换后整体验证

### 4.1 结构验证

```sql
-- 业务表数量 = 38
SELECT count(*) FROM information_schema.tables 
WHERE table_schema='public' AND table_name NOT LIKE 'qrtz_%' AND table_name NOT LIKE 'flyway_%';

-- Quartz 表数量 = 11
SELECT count(*) FROM information_schema.tables 
WHERE table_schema='public' AND table_name LIKE 'qrtz_%';

-- 各表行数（与 MySQL 源对比）
SELECT 'sys_menu' AS t, count(*) FROM sys_menu
UNION ALL SELECT 'sys_role', count(*) FROM sys_role
-- ... 逐表
;
```

### 4.2 COMMENT 验证

```sql
-- 表注释
SELECT obj_description('sys_user'::regclass);
-- 列注释
SELECT col_description('sys_user'::regclass, 1);  -- 第1列
```

### 4.3 索引验证

```sql
-- 各表索引
SELECT indexname, indexdef FROM pg_indexes 
WHERE schemaname='public' AND tablename='sys_api_key';
```

### 4.4 应用层验证（pig-boot 启动后）

- [ ] 登录正常（sys_user/sys_role/sys_menu 种子数据 + 权限校验）
- [ ] 字典查询正常（sys_dict/sys_dict_item）
- [ ] 代码生成器可用（gen_template 模板渲染 + gen_field_type 类型映射）
- [ ] Quartz 调度正常（qrtz 表 + PostgreSQLDelegate）
- [ ] 分页正常（DbType.POSTGRE_SQL）
- [ ] 审计字段自动填充（create_time/update_time，验证 MetaObjectHandler）

---

## 五、转换失败排查

| 症状 | 可能原因 | 对策 |
|------|---------|------|
| 导入报 `syntax error near "ENGINE"` | ENGINE 未移除 | G2 |
| 导入报 `relation "xxx" already exists` | DROP TABLE 缺失或重复执行 | 检查 DROP TABLE，或清库重导 |
| 导入报 `unterminated quoted string` | 字符串转义错误（gen_template） | G23 美元引号 |
| 启动报 `column "xxx" does not exist` | 列名大小写不一致（PG 折叠小写） | G1 标识符全小写 |
| 分页报错 / SQL 语法错误 | DbType 未生效 | 显式 DbType.POSTGRE_SQL |
| 代码生成器类型映射缺失 | gen_field_type 种子数据不完整 | 核对 22 种 PG 类型 |
| Quartz 任务不执行 | PostgreSQLDelegate 未配置 / qrtz 表未建 | 检查 quartz-config.yml + V3 脚本 |
