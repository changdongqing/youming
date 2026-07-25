-- ============================================================
-- V6__ont_supply_permission_seed.sql
-- 补充供给接口权限点 + 分类模板查看权限点（DD2）
-- ============================================================

-- 供给接口权限（挂在属性模板库下，建模侧/查看者可授予）
INSERT INTO sys_menu VALUES (10105, '供给接口调用', 'ont_supply_view', NULL, NULL, 10100, NULL, '1', 5, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');

-- 分类模板查看权限（DD3 用，提前补建避免后续再加 V 版本）
INSERT INTO sys_menu VALUES (10205, '分类模板查看', 'ont_class_tpl_view', NULL, NULL, 10200, NULL, '1', 5, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
