-- ============================================================
-- V11__ont_reference_seed.sql
-- 参考本体库：权限点按钮
-- 对应 PRD v1.2 FR-7 AC-7.1、第十三节 13.2
-- 依赖 V5（菜单 10500）
-- ============================================================

-- ---------- 参考本体权限点按钮（挂在参考本体库菜单 10500 下） ----------
-- 浏览走 ont_ref_view；导入复用 ont_unit_manage（QUDT）/ ont_class_tpl_manage（Brick），对齐 PRD 10.6
INSERT INTO sys_menu VALUES (10501, '参考本体浏览', 'ont_ref_view', NULL, NULL, 10500, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
