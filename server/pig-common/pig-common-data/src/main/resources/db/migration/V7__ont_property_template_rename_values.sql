-- ============================================================
-- V7__ont_property_template_rename_values.sql
-- 修复：ont_property_template.values 列名是 SQL 保留字（VALUES），
-- 被 pig 的 Druid SQL 解析器识别为 INSERT 子句关键字，
-- 导致 selectList/selectPage 生成 "SELECT ..., values, ..." 时报
-- ParserException: syntax error, expect (, actual ,（pos 100）。
--
-- 处理：重命名为 enum_values（语义更准确：枚举值集合），并补 COMMENT。
-- 禁止改 V4（已应用，Flyway checksum 校验），故以新版本脚本 ALTER。
-- ============================================================

-- 兼容已/未建出 values 列的环境（幂等）
ALTER TABLE ont_property_template RENAME COLUMN values TO enum_values;
COMMENT ON COLUMN ont_property_template.enum_values IS '枚举值（逗号分隔），datatype 专属';
