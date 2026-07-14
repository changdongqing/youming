-- ============================================================
-- V21: 本体建模菜单结构优化 —— 新增「静态建模」二级分组
-- ============================================================
-- 背景：
--   现有「本体建模」(900000) 顶级目录下直接挂载了 15 个子菜单
--   (900100 单位字典 ~ 901500 安全与合规)。为区分静态建模与后续
--   动态/运行时建模形态，新增一个二级目录「静态建模」，将既有
--   15 个菜单统一归入其下。
--
-- 结构变化：
--   优化前：
--     本体建模 (900000, parent_id=-1)
--     ├─ 单位字典 (900100)
--     ├─ 命名空间管理 (900200)
--     └─ ... (共 15 个)
--
--   优化后：
--     本体建模 (900000, parent_id=-1)
--     └─ 静态建模 (900010, parent_id=900000)  ← 新增二级目录
--        ├─ 单位字典 (900100, parent_id 900000→900010)
--        ├─ 命名空间管理 (900200, parent_id 900000→900010)
--        └─ ... (共 15 个)
--
-- 说明：
--   * 菜单树由后端 TreeUtil 按 parent_id 构建，前端无硬编码 ID，
--     仅改 parent_id 即可在侧边栏生效（应用重启后刷新缓存）。
--   * 子菜单自身 path / permission / id 均不变，不影响权限校验。
--   * ON CONFLICT 与幂等 UPDATE 保证脚本可重复执行。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 新增「静态建模」二级目录菜单（menu_type='0' 目录，无 component）
-- ------------------------------------------------------------
INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES (900010, '静态建模', NULL, '/static', NULL, 900000, 'ele-FolderOpened', '1', 1, '0', NULL, '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO UPDATE
SET name        = EXCLUDED.name,
    path        = EXCLUDED.path,
    parent_id   = EXCLUDED.parent_id,
    icon        = EXCLUDED.icon,
    sort_order  = EXCLUDED.sort_order,
    menu_type   = EXCLUDED.menu_type,
    update_by   = 'admin',
    update_time = now(),
    del_flag    = '0';

-- ------------------------------------------------------------
-- 2. 将原「本体建模」下的 15 个子菜单（900100~901500）挂到「静态建模」下
--    排除新菜单 900010 自身；按钮权限(menu_type='1')的 parent_id 指向
--    各功能菜单，不受影响。
-- ------------------------------------------------------------
UPDATE sys_menu
   SET parent_id   = 900010,
       update_by   = 'admin',
       update_time = now()
 WHERE parent_id = 900000
   AND menu_id  <> 900010;

-- ------------------------------------------------------------
-- 3. 授权：将「静态建模」目录授予管理员角色（role_id=1）
--    （admin 超管本应全量可见，但 pig-ui 仍按 sys_role_menu 渲染，
--     故需补一条授权，确保目录在侧边栏出现。）
-- ------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES (1, 900010)
ON CONFLICT (role_id, menu_id) DO NOTHING;
