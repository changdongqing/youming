/*
 * V26 — 数据源映射设计器隐藏路由菜单
 *
 * 背景：映射工程「编辑DRAFT」按钮跳转 /ontology/data-mapping/designer，但 sys_menu 中
 * 未注册该路径，后端驱动路由 backEnd.ts 无法解析到 designer.vue，导致 404。
 * 本脚本新增一条 visible='0' 的隐藏菜单（不出现在左侧导航，但参与路由注册），
 * 使 designer 页面可被 router 正确加载。
 *
 * 约定（与 V22 §6 一致）：component 字段留 NULL，由 backEnd.ts 用 path 动态匹配
 * /@/views/ontology/data-mapping/designer.vue。
 */
INSERT INTO sys_menu
(menu_id, name, permission, path, component, parent_id, icon, visible,
 sort_order, keep_alive, embedded, menu_type,
 create_by, create_time, update_by, update_time, del_flag)
VALUES
(901610, '映射设计器', NULL, '/ontology/data-mapping/designer', NULL, 900020,
 NULL, '0', 99, '0', NULL, '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO UPDATE
SET path        = EXCLUDED.path,
    visible     = EXCLUDED.visible,
    menu_type   = EXCLUDED.menu_type,
    update_by   = 'admin',
    update_time = now(),
    del_flag    = '0';

-- 授权管理员角色（role_id=1），否则后端菜单过滤时被剔除，路由仍不会下发
INSERT INTO sys_role_menu(role_id, menu_id)
VALUES (1, 901610)
ON CONFLICT (role_id, menu_id) DO NOTHING;
