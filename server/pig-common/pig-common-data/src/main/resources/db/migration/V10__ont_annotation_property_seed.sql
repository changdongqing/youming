-- ============================================================
-- V10__ont_annotation_property_seed.sql
-- 注释属性注册表：增补 source 列 + 12 项内置种子 + 权限点
-- 对应 PRD v1.2 FR-4 AC-4.1/4.5、NFR-4
-- 依赖 V4（表结构 + uk_ont_ap_local_name）、V5（菜单 10400）
-- ============================================================

-- ---------- (a) 增补 source 列（builtin/custom，对齐全平台治理表范式） ----------
-- ALTER ADD COLUMN 不改 V4，遵循"禁改已应用脚本"
-- 用途：builtin（随 Flyway 分发，只读保护） / custom（治理员新增）
ALTER TABLE ont_annotation_property ADD COLUMN source varchar(16) DEFAULT 'builtin';
COMMENT ON COLUMN ont_annotation_property.source IS 'builtin（内置只读）/ custom（治理员新增）';

-- ---------- (b) 12 项内置注释属性种子（ont_annotation_property，AC-4.1） ----------
-- localName 即 ont:xxx 的 xxx（序列化时拼成 ont:icon 等）
-- applies_to 取值：class/datatypeProperty/objectProperty/individual/all（PRD 9.6）
-- id 用 7xxx 段（5xxx 量纲 / 6xxx 单位 / 7xxx 注释属性，便于跨脚本引用）
-- source='builtin'（随 Flyway 分发，只读）；sort_order 决定列表与导出顺序
INSERT INTO ont_annotation_property (id, local_name, label, range_xsd, applies_to, description, sort_order, source, create_by, create_time, update_by, update_time) VALUES
(7001, 'icon',             '显示图标',   'xsd:string',  'class',            '类的展示图标（外观治理，FR-2）',                                                         1,  'builtin', 'admin', now(), 'admin', now()),
(7002, 'color',            '显示颜色',   'xsd:string',  'class',            '类的展示颜色（外观治理，FR-2）',                                                         2,  'builtin', 'admin', now(), 'admin', now()),
(7003, 'unit',             '单位符号',   'xsd:string',  'datatypeProperty', '数据属性的单位符号（旧版裸字符串；建议用 unitRef 引用 QUDT IRI，FR-3）',                3,  'builtin', 'admin', now(), 'admin', now()),
(7004, 'propertyType',     '属性类型',   'xsd:string',  'all',              '属性类型标注（data/object），供建模侧区分数据/对象属性',                                  4,  'builtin', 'admin', now(), 'admin', now()),
(7005, 'cardinality',      '基数',       'xsd:string',  'objectProperty',   '对象属性基数（如 0..1/0..n/1..1），对齐 FR-1 defaultCardinality',                        5,  'builtin', 'admin', now(), 'admin', now()),
(7006, 'isIdentifier',     '是否标识符', 'xsd:boolean', 'datatypeProperty', '标注数据属性是否为标识符（appliesTo=datatypeProperty，不可用于 class，AC-4.3）',         6,  'builtin', 'admin', now(), 'admin', now()),
(7007, 'enumValues',       '枚举值',     'xsd:string',  'datatypeProperty', '数据属性的枚举取值集合（对齐 FR-1 enumValues）',                                         7,  'builtin', 'admin', now(), 'admin', now()),
(7008, 'fromEntityId',     '起始实体',   'xsd:string',  'objectProperty',   '对象属性的起始实体标识（建模侧关系建模辅助）',                                           8,  'builtin', 'admin', now(), 'admin', now()),
(7009, 'toEntityId',       '目标实体',   'xsd:string',  'objectProperty',   '对象属性的目标实体标识（建模侧关系建模辅助）',                                           9,  'builtin', 'admin', now(), 'admin', now()),
(7010, 'templateRef',      '模板溯源',   'xsd:string',  'all',              '属性溯源到治理模板 templateCode（FR-1/FR-6，序列化方案 A/B 判据来源）',                    10, 'builtin', 'admin', now(), 'admin', now()),
(7011, 'unitRef',          '单位引用',   'xsd:string',  'datatypeProperty', '数据属性引用的 QUDT 单位 IRI（FR-3，如 .../unit/KiloGM）',                                11, 'builtin', 'admin', now(), 'admin', now()),
(7012, 'quantityKindRef',  '量纲引用',   'xsd:string',  'datatypeProperty', '数据属性引用的 QUDT 量纲 IRI（FR-3，如 .../quantitykind/Mass）',                          12, 'builtin', 'admin', now(), 'admin', now());

-- ---------- (c) 注释属性权限点按钮（挂在注释属性注册表菜单 10400 下） ----------
-- view/manage 对齐 PRD 13.2；导出走 view 权限（AC-4.4）
-- 10401~10403 manage（新增/编辑/删除），10404~10405 view（查看/导出）
INSERT INTO sys_menu VALUES (10401, '注释属性新增', 'ont_ap_manage', NULL, NULL, 10400, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10402, '注释属性编辑', 'ont_ap_manage', NULL, NULL, 10400, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10403, '注释属性删除', 'ont_ap_manage', NULL, NULL, 10400, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10404, '注释属性查看', 'ont_ap_view',   NULL, NULL, 10400, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10405, '注释属性导出', 'ont_ap_view',   NULL, NULL, 10400, NULL, '1', 5, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
