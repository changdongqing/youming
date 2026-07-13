-- =============================================
-- V14: 可视化模块菜单与权限注册
-- PRD §4.10 可视化模块
-- 本模块为只读展示层，不新建业务表，仅注册菜单和权限。
-- =============================================

-- ----------------------------
-- 1. 菜单：本体建模 / 可视化
-- ----------------------------
INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES (901100, '可视化', NULL, '/ontology/visualization/index', NULL, 900000, 'ele-View', '1', 11, '0', NULL, '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

-- ----------------------------
-- 2. 按钮权限：图谱查看
-- ----------------------------
INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES
  (901101, '图谱查看', 'ontology_visualization_view', NULL, NULL, 901100, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

-- ----------------------------
-- 3. 角色-菜单映射
-- ----------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
  (1, 901100), (1, 901101)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ----------------------------
-- 4. 迁移完整性断言
-- ----------------------------
DO $$
BEGIN
  -- 1. 可视化菜单存在
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901100 AND del_flag = '0') THEN
    RAISE EXCEPTION '可视化菜单(901100)未创建';
  END IF;

  -- 2. 图谱查看权限存在
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901101 AND permission = 'ontology_visualization_view' AND del_flag = '0') THEN
    RAISE EXCEPTION '图谱查看权限(901101)未创建';
  END IF;

  -- 3. 角色-菜单映射存在
  IF NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 901100) THEN
    RAISE EXCEPTION '管理员角色未分配可视化菜单';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 901101) THEN
    RAISE EXCEPTION '管理员角色未分配图谱查看权限';
  END IF;

  -- 4. 菜单数量校验
  IF (SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 901100 AND 901101 AND del_flag = '0') != 2 THEN
    RAISE EXCEPTION '可视化菜单数量不正确，期望2条';
  END IF;

  RAISE NOTICE 'V14 可视化模块菜单与权限注册完成';
END $$;
