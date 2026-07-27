-- ============================================================
-- V8__ont_class_template_seed.sql
-- 分类模板内置设备树 + 编码规则 + 建模侧同步权限点
-- 对应 PRD v1.2 FR-2 AC-2.1、FR-8 AC-8.1/8.6、FR-9 AC-9.2
-- 依赖 V4（表结构）、V5（菜单/属性模板）、V6（ont_class_tpl_view）
-- 说明：V4 已建好 ont_class_template / ont_class_template_ref /
--       ont_classification_rule / ont_class_hierarchy 四张表（结构齐备），
--       本脚本只做 INSERT，不改 V4/V5/V6/V7。
-- ============================================================

-- ---------- (a) 编码规则（先于分类树，分类树编码遵循此规则） ----------
-- equipment 树：分隔符 '-'、每级 2 位、根级基数 30、零填充（GB/T 51269 式）
INSERT INTO ont_classification_rule (id, tree_root, separator, level_digits, base_number, zero_pad, description, create_by, create_time, update_by, update_time, del_flag)
VALUES (3001, 'equipment', '-', 2, 30, '1', '设备分类树默认编码规则（分隔符-、每级2位、基数30、零填充）', 'admin', now(), 'admin', now(), '0');

-- ---------- (b) 内置设备分类树（参照 Brick 设备类，3 级 / 3 子类，AC-2.1） ----------
-- 编码前缀即父链：30 -> 30-01 -> 30-01-01/02/03
-- id 用语义化固定值便于跨脚本引用；parent_id 指向上层分类模板 id
-- tree_root='equipment'（与编码规则 tree_root 对应）
-- L2（3102~3104）icon/color 留 NULL + inherit_appearance='1'，用于验证 AC-2.2「子模板未覆盖外观时继承父外观」

-- L0 根：设备
INSERT INTO ont_class_template (id, template_code, classification_code, label, label_cn, description, parent_id, tree_root, icon, color, inherit_appearance, source, source_ref, deprecated, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (3100, 'equipment', '30', '设备', '设备', '设备根分类（参照 Brick:Equipment）', NULL, 'equipment', '🏗️', '#1890ff', '1', 'builtin', 'brick', '0', 0, 'admin', now(), 'admin', now(), '0');

-- L1：泵（设备下第 1 个子类）
INSERT INTO ont_class_template (id, template_code, classification_code, label, label_cn, description, parent_id, tree_root, icon, color, inherit_appearance, source, source_ref, deprecated, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (3101, 'pump', '30-01', '泵', '泵', '泵类设备（参照 Brick:Pump）', 3100, 'equipment', '💧', '#1890ff', '1', 'builtin', 'brick', '0', 1, 'admin', now(), 'admin', now(), '0');

-- L2：喷淋泵 / 给水泵 / 污水泵（泵下 3 个子类）
INSERT INTO ont_class_template (id, template_code, classification_code, label, label_cn, description, parent_id, tree_root, icon, color, inherit_appearance, source, source_ref, deprecated, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES
(3102, 'spray-pump',     '30-01-01', '喷淋泵', '喷淋泵', '喷淋系统用泵',         3101, 'equipment', NULL, NULL, '1', 'builtin', 'brick', '0', 1, 'admin', now(), 'admin', now(), '0'),
(3103, 'feedwater-pump', '30-01-02', '给水泵', '给水泵', '给水系统用泵',         3101, 'equipment', NULL, NULL, '1', 'builtin', 'brick', '0', 2, 'admin', now(), 'admin', now(), '0'),
(3104, 'sewage-pump',    '30-01-03', '污水泵', '污水泵', '排污用泵',             3101, 'equipment', NULL, NULL, '1', 'builtin', 'brick', '0', 3, 'admin', now(), 'admin', now(), '0');

-- ---------- (c) 泵模板的结构骨架（本节点新增属性，AC-2.3 的父 4 属性） ----------
-- 引用 V5 内置属性模板（template_code）：name/description/amount/isActive
-- 注：V5 仅含 12 个通用属性，不含设备专属属性（ratedFlow/head 等）；
--     为满足 AC-2.3「泵 4 属性」演示继承，此处用通用属性模拟骨架，ref_type=property。
--     真实设备属性由治理员后续 custom 扩展，不在内置种子。
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
VALUES
(4001, 3101, 'name',        'property', 1, '0', 'admin', now(), 'admin', now(), '0'),
(4002, 3101, 'description', 'property', 2, '0', 'admin', now(), 'admin', now(), '0'),
(4003, 3101, 'amount',      'property', 3, '0', 'admin', now(), 'admin', now(), '0'),
(4004, 3101, 'isActive',    'property', 4, '0', 'admin', now(), 'admin', now(), '0');

-- ---------- (d) 同步权限点 ont_sync_push（建模侧服务账号授予，FR-9 AC-9.2） ----------
-- 挂在「类分类树」菜单（10250）下，仅建模侧服务账号/角色持有，治理员不持有
-- menu_type='1'（按钮），对齐 MenuTypeEnum.BUTTON
INSERT INTO sys_menu VALUES (10251, '类层级同步', 'ont_sync_push', NULL, NULL, 10250, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
