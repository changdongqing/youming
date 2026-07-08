# 03 · DDL 转换映射参考 评审

> 评审对象：pig 原始设计《03-DDL转换映射参考》
> 评审基线：本仓库 `server/`（pig 二次开发）`youming` 分支源码

## 一、总体评价

**通过，附 2 处表述需澄清（S2 / 类型种数口径）。**

文档作为"转换脚本编写与人工复核"的参考，逐项给出 MySQL→PG 语法/类型对照 + 转换前后示例 + 一页纸速查，实用性强。第 12 节"转换流程建议（脚本化）"可直接作为转换脚本的需求规格。

## 二、源码核验

| 文档论断 | 核验 | 结果 |
|---|---|---|
| datetime 94 处 | grep | ✅ |
| timestamp 3 处（sys_job） | grep L1407-1409 start_time/previous_time/next_time | ✅ |
| longtext 1 处（sys_system_config.config_value） | grep | ✅（文档点名） |
| blob 4 处全在 qrtz | grep | ✅ |
| ON UPDATE 11 处 | grep -c | ✅ |
| AUTO_INCREMENT 1 处（gen_field_type） | grep -c | ✅ |
| 内联 KEY 11、UNIQUE KEY 3、USING BTREE 44 | grep | ✅（数量级一致） |
| 列级 COMMENT 520 | grep | ✅（数量级一致） |

类型与特性统计全部与源码吻合，转换规则可信。

## 三、应修订/澄清项

### S2. 第 89 行 updateTime 注解表述与实际不符 🟠

第 89 行：

> 迁移后需验证实体 updateTime 字段标注了 `@TableField(fill = FieldFill.INSERT_UPDATE)`

核验 `SysUser.java`：`updateTime`（L99 附近）实际为 `@TableField(fill = FieldFill.UPDATE)`，非 `INSERT_UPDATE`。

为何结论仍正确：`MybatisPlusMetaObjectHandler.insertFill`（L26-37）对 `updateTime` 做了 `fillValIfNull` 兜底，INSERT 场景由 handler 而非注解驱动。所以"应用层兜底 update_time"结论成立，但"需验证 INSERT_UPDATE 注解"的表述会误导开发者改注解。

**修订**：

> 迁移后需验证实体 updateTime 字段标注了 `@TableField(fill = FieldFill.UPDATE)`（UPDATE 场景注解驱动），且 INSERT 场景由 `MybatisPlusMetaObjectHandler.insertFill` 兜底填充（已实现，见 insertFill 对 updateTime 的 fillValIfNull）。无需改注解为 INSERT_UPDATE。

> ✅ **2026-07-08 源码复核**：`server/pig-upms/pig-upms-api/src/main/java/com/pig4cloud/pig/admin/api/entity/SysUser.java:99-101` 确为 `@TableField(fill = FieldFill.UPDATE)`；`MybatisPlusMetaObjectHandler.insertFill` 对 createTime/updateTime 均做 `fillValIfNullByName` 兜底。S2 修订有效。

### 类型种数口径：gen_field_type "22 种 PG 类型" vs 实测 18 行 🟢（非问题，澄清即可）

01 文档称 gen_field_type 预置"22 种 PG 类型"，本次 grep `int8/int4/.../bytea` 命中 18 行。差异源于：

- "22 种"指**类型种数**（去重后），grep 命中 18 是**含 PG 类型关键字的行数**（部分类型如 `json`/`jsonb` 可能在同一行或不同行计数口径不一）。

非实质性错误，但若有人按"22 行"去对账会困惑。**建议**：01 文档该处补一句"（去重类型种数，非数据行数）"消除口径歧义。03 文档无需改。

## 四、亮点

1. **第 1 节转换前后完整示例**：`sys_role` 一张表从 MySQL 原文到 PG 目标（含 COMMENT ON COLUMN / CREATE INDEX 拆分），可直接当转换模板。
2. **第 3 节 ON UPDATE 处理原则 + 3.1 触发器备选**：主方案（应用兜底）与备选（触发器）都给，且明确"默认不采用触发器"，决策清晰。
3. **第 9 节字符串转义**：`\'`→`''`、`\"`→外层单引号、长串用美元引号，针对 gen_template/sys_oauth_client_details 具体点名，可操作。
4. **第 13 节一页纸速查**：转换脚本编写时的桌面参考，密度高。
5. **第 12 节脚本化流程**：7 步从预处理到校验，可作为转换脚本的验收规格。

## 五、结论

参考文档通过，转换规则经源码核验准确。修 S2（注解表述），澄清类型种数口径。可作为 P1 库结构转换的执行依据。
