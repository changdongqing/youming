# 04 · Flyway 集成设计 评审

> 评审对象：pig 原始设计《04-Flyway集成设计》
> 评审基线：本仓库 `server/`（pig 二次开发）`youming` 分支源码

## 一、总体评价

**通过，附 4 项需落地前确认（N1/N3/N4/N6）。**

设计在迁移到 PG 之后引入 Flyway 统一管理库结构版本，采用时间戳命名 + 单点执行（方案 A：upms-biz/pig-boot 启用、codegen/quartz disabled）+ baseline 双轨策略。设计完整、决策合理、风险对策到位，可作为 Flyway 集成的执行依据。

## 二、源码核验

| 设计算断 | 核验 | 结果 |
|---|---|---|
| pig 无数据库迁移工具，靠 db/pig.sql 裸 SQL | 无 flyway/liquibase 依赖 | ✅ |
| 业务库 pig 被 upms-biz/codegen/quartz + pig-boot 共享 | 各 pom 均连 `${MYSQL_DB:pig}` | ✅（与 01 盘点一致） |
| Nacos pig_config 由 register 管理、有自身 platform 机制 | application.properties L6 `spring.sql.init.platform=mysql` | ✅ |
| quartz-config.yml `initialize-schema: never` | 读源码 | ✅ |
| upms-biz/codegen/quartz 共享 pig 库 | pom 依赖与配置 | ✅ |

设计对现状的判断准确。

## 三、需落地前确认项

### N1. baseline 双轨与 pgloader 执行顺序需固化（与 02 评审 N1 同源）🟡

5.2 节给了两条路径（空库全量基线 / 存量库 baseline），末尾推荐"空库 + pgloader `WITH data only`"。但 02-7.1 的 pgloader 配置是 `create tables`（建结构）。两文档口径冲突已在 02 评审 N1 标记。

**本节确认**：04 文档自身逻辑自洽（5.2 推荐 data only），问题在跨文档口径。建议 04-5.2 显式标注"结构由 Flyway 基线脚本建，pgloader 仅 `WITH data only` 迁数据"，并反向引用 02-7.1 修订。

> ✅ **2026-07-08 youming 落地结论**：youming 空库迁移，直接由 Flyway 基线脚本建表 + 种子数据，不使用 pgloader。N1 闭环，详见 [07-youming-Flyway落地设计](./07-youming-Flyway落地设计.md)。

### N3. Nacos PG 不纳入 Flyway 的边界正确，但 Nacos 自身 PG 成熟度仍是外部风险 🟡

1.3 节明确"Nacos 库 pig_config 不纳入 pig 的 Flyway，由 Nacos 自身管理"——**这个边界划分正确**，避免了应用层 Flyway 接管 Nacos server schema 的越界。评审赞同。

但 Nacos 自身切 PG 的成熟度风险（社区插件历史）依然存在，已与 01 评审 N3 衔接。Flyway 集成本身不受 Nacos 影响（Nacos 库不在 Flyway 管辖），故此风险不影响 Flyway 设计成立性，仅影响整体迁移的 P2 环节。

> ✅ **2026-07-08 更新**：Nacos 3.2.2 原生内置 PG 支持（见 00-总评审报告 N3），成熟度风险消除。"Nacos 库不纳入 Flyway"的边界划分在 youming 中依然成立——Nacos 元数据库由其自身 `pg-schema.sql` 管理。

### N4. Flyway 与 Druid wall 的共存需在基线前验证 🟡

8.2 节识别了 wall 可能拦 `CREATE FUNCTION`/`CREATE TRIGGER`，9 节风险表也有。但仅停在"建议放行或独立 DataSource"，未给具体配置。

**补强建议**：在 8.2 或 9 节补充独立 DataSource 的配置示例（Spring Boot 的 `@FlywayDataSource` 用法），作为 wall 拦截时的现成对策：

```java
@Bean
@FlywayDataSource
@Primary
public DataSource flywayDataSource(DataSourceProperties properties) {
    // 无 wall 过滤器的 DataSource，专供 Flyway
    return DataSourceBuilder.create()
        .url(properties.getUrl()).username(properties.getUsername())
        .password(properties.getPassword()).driverClassName(properties.getDriverClassName())
        .build();
}
```

或给出 wall config 放行 DDL 的具体 key。把"建议"升级为"可直接复制"的配置。

### N6. 时间戳命名"规划日"定义需防歧义 🟡

3.1/12 节定义 `{yyyyMMdd}` 为"规划日，非提交日"。"规划日"在实践中模糊（需求评审日？PR 起草日？），跨日协作时可能出现日期倒挂（虽然 out-of-order=false 会报错暴露，但仍需人工改号）。

**补强建议**：

1. 明确"规划日 = PR 起草日（本地创建迁移脚本当日）"。
2. CI 预检（6.5）增加规则：新脚本日期不得早于该分支 base 之后已合入的最新脚本日期，否则 fail。把人工 .seq 兜底升级为 CI 自动兜底。

## 四、亮点

1. **单点执行决策（4.1/4.2）**：准确识别"单库多服务共享"下并发跑 Flyway 的争锁风险，方案 A（upms-biz 单点）稳妥，方案对比表 A/B/C/D 四选一论证清晰。
2. **baseline 双轨设计（5.2）**：兼顾新建库与存量库，且 5.2 末尾的"推荐空库+全量基线"指明了规避 baseline 复杂性的标准路径——这是对 Flyway baseline 语义理解到位的体现（baseline-on-migrate 对空库的副作用那段分析准确）。
3. **不可变规则（3.3）+ 失败处理（7.3）**：checksum 不可改、版本号不可复用、repair 流程，Flyway 最佳实践覆盖完整。
4. **与现有机制协作（第 8 节）**：Flyway 与 MyBatis-Plus / Druid / Nacos / Quartz / 动态数据源的边界逐一界定，codegen 的动态数据源"Flyway 不介入"判断正确。
5. **CI 预检（6.5）**：干净库全量 migrate 预检，保证每次合并前迁移脚本可从零跑通——这是迁移工具用得稳的关键工程化动作。

## 五、结论

Flyway 集成设计通过，方案 A（单点执行）+ 时间戳命名 + baseline 标准路径是稳妥组合。落地前补 N4（wall 独立 DataSource 配置示例）、固化 N1（与 pgloader 顺序）、明确 N6（规划日定义 + CI 兜底）。N3 不影响 Flyway 设计成立性，属整体迁移的外部风险。
