-- ============================================================
-- V18__rm_menu_seed.sql
-- 研发管理模块菜单骨架（一级目录 + 二级菜单）
-- 对应《研发管理工具详细设计计划_v1.0》§1.4 菜单与权限码规划
-- 三级按钮权限码（rm_req_view 等）在后续版本补充
-- ============================================================

-- ---------- sys_menu 种子（17 字段按位置 INSERT） ----------
-- 字段顺序：menu_id, name, permission, path, component, parent_id, icon, visible,
--           sort_order, keep_alive, embedded, menu_type, create_by, create_time,
--           update_by, update_time, del_flag
-- menu_type: '0'=菜单(含目录) '1'=按钮
-- parent_id: 顶级目录用 -1
-- ID 段规划：19000(一级) 19100~19500(二级) 1910x~(三级按钮，后续版本)
--            与 ontology(10000~10500) 不冲突

-- 一级目录：研发管理
INSERT INTO sys_menu VALUES (19000, '研发管理', NULL, '/rm', NULL, -1, 'iconfont icon-shujujicheng', '1', 40, '0', '0', '0', 'admin', now(), 'admin', now(), '0');

-- 二级菜单：需求管理
INSERT INTO sys_menu VALUES (19100, '需求管理', NULL, '/admin/rm/requirement/index', NULL, 19000, 'iconfont icon-rili', '1', 1, '0', '0', '0', 'admin', now(), 'admin', now(), '0');

-- 二级菜单：开发管理
INSERT INTO sys_menu VALUES (19200, '开发管理', NULL, '/admin/rm/dev-task/index', NULL, 19000, 'iconfont icon-tool', '1', 2, '0', '0', '0', 'admin', now(), 'admin', now(), '0');

-- 二级菜单：测试管理
INSERT INTO sys_menu VALUES (19300, '测试管理', NULL, '/admin/rm/test/case/index', NULL, 19000, 'iconfont icon-Check', '1', 3, '0', '0', '0', 'admin', now(), 'admin', now(), '0');

-- 二级菜单：报表统计
INSERT INTO sys_menu VALUES (19400, '报表统计', NULL, '/admin/rm/report/requirement-detail', NULL, 19000, 'iconfont icon-tubiao', '1', 4, '0', '0', '0', 'admin', now(), 'admin', now(), '0');

-- 二级菜单：待办中心
INSERT INTO sys_menu VALUES (19500, '待办中心', NULL, '/admin/rm/todo/index', NULL, 19000, 'iconfont icon-news', '1', 5, '0', '0', '0', 'admin', now(), 'admin', now(), '0');
