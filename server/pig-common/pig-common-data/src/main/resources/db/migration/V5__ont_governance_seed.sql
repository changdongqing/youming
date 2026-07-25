-- ============================================================
-- V5__ont_governance_seed.sql
-- 本体治理菜单 + 权限点 + 内置属性模板种子
-- 对应 PRD v1.2 第十三节、FR-1 AC-1.1
-- ============================================================

-- ---------- (a) sys_menu 种子（17 字段按位置 INSERT） ----------
-- 字段顺序：menu_id, name, permission, path, component, parent_id, icon, visible,
--           sort_order, keep_alive, embedded, menu_type, create_by, create_time,
--           update_by, update_time, del_flag
-- menu_type: '0'=菜单(含目录) '1'=按钮 （对齐 MenuTypeEnum: LEFT_MENU='0', BUTTON='1'）
-- parent_id: 顶级目录用 -1 （对齐 CommonConstants.MENU_TREE_ROOT_ID）

INSERT INTO sys_menu VALUES (10000, '本体治理', NULL, '/ontology', NULL, -1, 'iconfont icon-shujujicheng', '1', 30, '0', '0', '0', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10100, '属性模板库', NULL, '/admin/ontology/property-template/index', NULL, 10000, 'iconfont icon-shuxing', '1', 1, '0', '0', '0', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10101, '属性模板新增', 'ont_prop_tpl_manage', NULL, NULL, 10100, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10102, '属性模板编辑', 'ont_prop_tpl_manage', NULL, NULL, 10100, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10103, '属性模板删除', 'ont_prop_tpl_manage', NULL, NULL, 10100, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10104, '属性模板查看', 'ont_prop_tpl_view', NULL, NULL, 10100, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10200, '分类模板', NULL, '/admin/ontology/class-template/index', NULL, 10000, 'iconfont icon-fenlei', '1', 2, '0', '0', '0', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10201, '分类模板新增', 'ont_class_tpl_manage', NULL, NULL, 10200, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10202, '分类模板编辑', 'ont_class_tpl_manage', NULL, NULL, 10200, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10203, '分类模板删除', 'ont_class_tpl_manage', NULL, NULL, 10200, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10204, '编码规则配置', 'ont_class_tpl_manage', NULL, NULL, 10200, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10250, '类分类树', NULL, '/admin/ontology/class-hierarchy/index', NULL, 10000, 'iconfont icon-jiegoushujuguanli', '1', 3, '0', '0', '0', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10300, '单位注册表', NULL, '/admin/ontology/unit/index', NULL, 10000, 'iconfont icon-danwei', '1', 4, '0', '0', '0', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10400, '注释属性注册表', NULL, '/admin/ontology/annotation-property/index', NULL, 10000, 'iconfont icon-biaoqian', '1', 5, '0', '0', '0', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10500, '参考本体库', NULL, '/admin/ontology/reference/index', NULL, 10000, 'iconfont icon-canshupeizhi', '1', 6, '0', '0', '0', 'admin', now(), 'admin', now(), '0');

-- ---------- (b) 内置属性模板 12 项（AC-1.1：8 数据属性 + 4 对象属性） ----------
-- id 用固定值便于引用（雪花 ID 由 ASSIGN_ID 生成，种子用显式 bigint 占位）

-- 8 数据属性
INSERT INTO ont_property_template (id, template_code, kind, label, description, category, type, is_identifier, source, deprecated, create_by, create_time, update_by, update_time)
VALUES
(1001, 'name',          'datatype', '名称',     '实体名称',           'basic',     'string',   '1', 'builtin', '0', 'admin', now(), 'admin', now()),
(1002, 'description',   'datatype', '描述',     '实体描述',           'basic',     'string',   '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1003, 'email',         'datatype', '邮箱',     '联系邮箱',           'contact',   'string',   '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1004, 'phone',         'datatype', '电话',     '联系电话',           'contact',   'string',   '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1005, 'price',         'datatype', '价格',     '货币金额',           'monetary',  'decimal',  '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1006, 'amount',        'datatype', '数量',     '数值量',             'monetary',  'decimal',  '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1007, 'createdDate',   'datatype', '创建日期', '创建时间',           'temporal',  'datetime', '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1008, 'isActive',      'datatype', '是否启用', '状态标记',           'status',    'boolean',  '0', 'builtin', '0', 'admin', now(), 'admin', now());

-- 4 对象属性
INSERT INTO ont_property_template (id, template_code, kind, label, description, category, default_cardinality, source, deprecated, create_by, create_time, update_by, update_time)
VALUES
(2001, 'contains',   'object', '包含', '包含关系',     'containment',  'one-to-many',  'builtin', '0', 'admin', now(), 'admin', now()),
(2002, 'belongsTo',  'object', '属于', '归属关系',     'attribution',  'many-to-one',  'builtin', '0', 'admin', now(), 'admin', now()),
(2003, 'hasPart',    'object', '拥有部分', '组合关系', 'containment',  'one-to-many',  'builtin', '0', 'admin', now(), 'admin', now()),
(2004, 'references', 'object', '引用', '引用关系',     'attribution',  'many-to-one',  'builtin', '0', 'admin', now(), 'admin', now());

-- ---------- (c) 治理元数据占位（QUDT 版本快照，AC-3.7） ----------
-- 独立元数据表 ont_governance_meta（V4 之外的最小补充，用动态 DDL 避免改 V4）
CREATE TABLE IF NOT EXISTS ont_governance_meta (
    meta_key   varchar(64)  NOT NULL,
    meta_value varchar(255),
    create_by  varchar(64)  DEFAULT ' ',
    create_time timestamp   DEFAULT now(),
    update_by  varchar(64)  DEFAULT ' ',
    update_time timestamp   DEFAULT now(),
    CONSTRAINT pk_ont_governance_meta PRIMARY KEY (meta_key)
);
COMMENT ON TABLE ont_governance_meta IS '治理元数据（QUDT 版本快照等）';

INSERT INTO ont_governance_meta (meta_key, meta_value) VALUES
('qudt_version', '3.1.4'),
('qudt_source_ref', 'docs/ontology/参考开源本体库/qudt/qudt-all.ttl')
ON CONFLICT (meta_key) DO NOTHING;
