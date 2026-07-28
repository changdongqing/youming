-- ============================================================
-- V15: 建模域 - 序列化与导入菜单种子（FR-15/16）
-- 无建表（复用 DD8/DD9 的 ont_model_* 四张表）
-- 详见《详细设计-DD10-RDF序列化与解析.md》
-- ============================================================

-- ---------- sys_menu 菜单种子（11400 段） ----------
-- 字段位序（17 列）：menu_id / name / permission / path / component / parent_id / icon /
--                  visible / sort_order / keep_alive / embedded / menu_type /
--                  create_by / create_time / update_by / update_time / del_flag

INSERT INTO sys_menu VALUES (11400, '序列化与导入', NULL, '/admin/ontology-model/serialize/index', NULL, 11000, 'iconfont icon-daochu', '1', 4, '0', '0', '0', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11401, '序列化预览', 'ont_serialize_view',   NULL, NULL, 11400, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11402, '导入本体',   'ont_serialize_manage', NULL, NULL, 11400, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
