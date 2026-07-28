-- ============================================================
-- V17__ont_organization_seed.sql
-- 组织域本体种子：用户 / 部门 / 岗位 三功能建模
-- 严格遵循「治理域（模板原型）→ 建模域（owl:Class 实例）」两层架构
-- 依赖 V4（治理表 DDL）、V5（属性模板/菜单）、V8（分类模板范式）、V10（注释属性）
--      V12（建模表 DDL）、V13（类/属性表 DDL）、V14（subClassOf 表 DDL）
-- ============================================================
--
-- 本体概览：
--   Organization（组织根类，owl:Class）
--   ├── User（用户，subClassOf Organization）
--   ├── Dept（部门，subClassOf Organization）
--   └── Post（岗位，subClassOf Organization）
--
--   对象属性：
--     User --belongsTo--> Dept     （用户属于部门，many-to-one）
--     User --hasPost--> Post       （用户拥有岗位，many-to-many）
--     User --primaryDept--> Dept  （用户主部门，many-to-one）
--     Dept --hasSubDept--> Dept   （部门包含子部门，one-to-many，自引用）
--
-- ID 段规划（不与 V4-V16 任何已有 ID 冲突）：
--   治理域：
--     ont_property_template     1009-1018（数据属性）、2005-2008（对象属性）
--     ont_classification_rule   3002
--     ont_class_template        3200-3203
--     ont_class_template_ref    4010-4040
--   建模域：
--     ont_model_project         1700001
--     ont_model_prefix          1700010-1700012
--     ont_model_class           1700100-1700103
--     ont_model_dt_property     1700201-1700224
--     ont_model_obj_property    1700301-1700304
--     ont_model_subclassof      1700401-1700403
--   治理域镜像：
--     ont_class_hierarchy       18001-18003
-- ============================================================


-- ======================
-- (a) 治理域 — 属性模板
-- ======================

-- ---------- (a1) 新增 10 个数据属性模板 ----------
-- 复用 V5 内置属性：name(1001)/description(1002)/email(1003)/phone(1004)/createdDate(1007)/isActive(1008)
-- 新增用户/部门/岗位专属属性，category='organization'
-- id 段 1009-1018（紧邻 V5 的 1008，不冲突）
INSERT INTO ont_property_template (id, template_code, kind, label, description, category, type, is_identifier, source, deprecated, create_by, create_time, update_by, update_time)
VALUES
(1009, 'username',  'datatype', '用户名',   '用户登录名（sys_user.username）',     'organization', 'string',   '1', 'builtin', '0', 'admin', now(), 'admin', now()),
(1010, 'nickname',  'datatype', '昵称',     '用户昵称（sys_user.nickname）',       'organization', 'string',   '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1011, 'avatar',    'datatype', '头像',     '用户头像 URL（sys_user.avatar）',     'organization', 'string',   '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1012, 'lockFlag',  'datatype', '锁定标记', '用户锁定状态（sys_user.lock_flag）',  'organization', 'boolean',  '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1013, 'deptName',  'datatype', '部门名称', '部门名称（sys_dept.name）',           'organization', 'string',   '1', 'builtin', '0', 'admin', now(), 'admin', now()),
(1014, 'deptSort',  'datatype', '部门排序', '部门排序号（sys_dept.sort_order）',   'organization', 'integer',  '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1015, 'postCode',  'datatype', '岗位编码', '岗位编码（sys_post.post_code）',      'organization', 'string',   '1', 'builtin', '0', 'admin', now(), 'admin', now()),
(1016, 'postName',  'datatype', '岗位名称', '岗位名称（sys_post.post_name）',      'organization', 'string',   '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1017, 'postSort',  'datatype', '岗位排序', '岗位排序号（sys_post.post_sort）',    'organization', 'integer',  '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1018, 'remark',    'datatype', '备注',     '备注描述（sys_post.remark）',         'organization', 'string',   '0', 'builtin', '0', 'admin', now(), 'admin', now())
ON CONFLICT (id) DO NOTHING;

-- ---------- (a2) 新增 4 个对象属性模板 ----------
-- 复用 V5 内置对象属性：belongsTo(2002)
-- 新增组织域专属对象属性，id 段 2005-2008（紧邻 V5 的 2004）
INSERT INTO ont_property_template (id, template_code, kind, label, description, category, default_cardinality, source, deprecated, create_by, create_time, update_by, update_time)
VALUES
(2005, 'hasPost',     'object', '拥有岗位',   '用户拥有岗位（sys_user_post 关联）',      'organization', 'many-to-many', 'builtin', '0', 'admin', now(), 'admin', now()),
(2006, 'hasSubDept',  'object', '包含子部门', '部门包含子部门（sys_dept.parent_id 自引用）', 'organization', 'one-to-many',  'builtin', '0', 'admin', now(), 'admin', now()),
(2007, 'primaryDept', 'object', '主部门',     '用户主部门（sys_user.dept_id）',          'organization', 'many-to-one',  'builtin', '0', 'admin', now(), 'admin', now()),
(2008, 'managedBy',   'object', '管理者',     '通用管理关系（预留扩展）',               'organization', 'many-to-one',  'builtin', '0', 'admin', now(), 'admin', now())
ON CONFLICT (id) DO NOTHING;


-- ======================
-- (b) 治理域 — 分类编码规则 + 分类模板树
-- ======================

-- ---------- (b1) 编码规则 ----------
-- organization 树：分隔符 '-'、每级 2 位、根级基数 10、零填充
-- id=3002（紧邻 V8 的 equipment 规则 3001，不冲突）
INSERT INTO ont_classification_rule (id, tree_root, separator, level_digits, base_number, zero_pad, description, create_by, create_time, update_by, update_time, del_flag)
VALUES (3002, 'organization', '-', 2, 10, '1', '组织分类树编码规则（分隔符-、每级2位、基数10、零填充）', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ---------- (b2) 内置组织分类模板树（4 节点：1 根 + 3 子类） ----------
-- 编码：10 -> 10-01/10-02/10-03
-- id 段 3200-3203（紧邻 V8 设备树 3100-3104，不冲突）
-- tree_root='organization'（与编码规则 tree_root 对应）
-- source='builtin'，source_ref=NULL（组织域本体为系统自建，非参考本体导入）

-- L0 根：组织
INSERT INTO ont_class_template (id, template_code, classification_code, label, label_cn, description, parent_id, tree_root, icon, color, inherit_appearance, source, source_ref, deprecated, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (3200, 'organization', '10', '组织', '组织', '组织域根分类（用户/部门/岗位的抽象父类）', NULL, 'organization', '🏢', '#1890ff', '1', 'builtin', NULL, '0', 0, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- L1：用户（组织下第 1 个子类）
INSERT INTO ont_class_template (id, template_code, classification_code, label, label_cn, description, parent_id, tree_root, icon, color, inherit_appearance, source, source_ref, deprecated, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (3201, 'user', '10-01', '用户', '用户', '用户实体（映射 sys_user 表）', 3200, 'organization', '👤', '#52c41a', '1', 'builtin', NULL, '0', 1, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- L1：部门（组织下第 2 个子类）
INSERT INTO ont_class_template (id, template_code, classification_code, label, label_cn, description, parent_id, tree_root, icon, color, inherit_appearance, source, source_ref, deprecated, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (3202, 'dept', '10-02', '部门', '部门', '部门实体（映射 sys_dept 表，自引用树结构）', 3200, 'organization', '🏛️', '#722ed1', '1', 'builtin', NULL, '0', 2, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- L1：岗位（组织下第 3 个子类）
INSERT INTO ont_class_template (id, template_code, classification_code, label, label_cn, description, parent_id, tree_root, icon, color, inherit_appearance, source, source_ref, deprecated, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (3203, 'post', '10-03', '岗位', '岗位', '岗位实体（映射 sys_post 表）', 3200, 'organization', '💼', '#fa8c16', '1', 'builtin', NULL, '0', 3, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;


-- ======================
-- (c) 治理域 — 结构骨架引用
-- ======================
-- ont_class_template_ref 无 uk 约束，用 WHERE NOT EXISTS 保护幂等
-- ref_type='property'=数据属性，ref_type='relationship'=对象属性
-- inherit_flag='1'=继承自父（根类 organization 的属性），'0'=本节点新增

-- ---------- (c1) User(3201) 结构骨架：8 数据属性 + 3 对象属性 ----------
-- 数据属性：username/nickname/email/phone/avatar/lockFlag/isActive/createdDate
-- 对象属性：belongsTo（复用内置）/hasPost/primaryDept
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4010, 3201, 'username', 'property', 1, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4010);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4011, 3201, 'nickname', 'property', 2, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4011);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4012, 3201, 'email', 'property', 3, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4012);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4013, 3201, 'phone', 'property', 4, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4013);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4014, 3201, 'avatar', 'property', 5, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4014);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4015, 3201, 'lockFlag', 'property', 6, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4015);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4016, 3201, 'isActive', 'property', 7, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4016);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4017, 3201, 'createdDate', 'property', 8, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4017);
-- User 对象属性
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4018, 3201, 'belongsTo', 'relationship', 9, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4018);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4019, 3201, 'hasPost', 'relationship', 10, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4019);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4020, 3201, 'primaryDept', 'relationship', 11, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4020);

-- ---------- (c2) Dept(3202) 结构骨架：3 数据属性 + 1 对象属性 ----------
-- 数据属性：deptName/deptSort/description
-- 对象属性：hasSubDept（自引用）
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4030, 3202, 'deptName', 'property', 1, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4030);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4031, 3202, 'deptSort', 'property', 2, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4031);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4032, 3202, 'description', 'property', 3, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4032);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4033, 3202, 'hasSubDept', 'relationship', 4, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4033);

-- ---------- (c3) Post(3203) 结构骨架：4 数据属性 + 1 数据属性（description） ----------
-- 数据属性：postCode/postName/postSort/remark/description
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4040, 3203, 'postCode', 'property', 1, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4040);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4041, 3203, 'postName', 'property', 2, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4041);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4042, 3203, 'postSort', 'property', 3, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4042);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4043, 3203, 'remark', 'property', 4, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4043);
INSERT INTO ont_class_template_ref (id, class_template_id, property_template_code, ref_type, sort_order, inherit_flag, create_by, create_time, update_by, update_time, del_flag)
SELECT 4044, 3203, 'description', 'property', 5, '0', 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_template_ref WHERE id = 4044);


-- ======================
-- (d) 建模域 — 本体项目 + IRI 前缀
-- ======================

-- ---------- (d1) 本体项目 ----------
-- namespace_base 末尾以 '#' 结尾（OWL 站点惯用 hash 命名空间，与 V12 注释示例 '/' 不同但均为合法 IRI）
-- 序列化策略 B（带前缀独立副本），状态 active
-- id=1700001（语义化固定值，雪花 ID 段不冲突）
INSERT INTO ont_model_project (id, project_code, name, description, namespace_base, default_format, serialization_strategy, status, create_by, create_time, update_by, update_time, del_flag)
VALUES (1700001, 'iam-organization', '组织域本体', '用户/部门/岗位三功能本体建模（映射 sys_user/sys_dept/sys_post 业务表）',
        'http://youming.com/ontology/organization#', 'TTL', 'B', 'active', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ---------- (d2) IRI 前缀注册 ----------
-- ex=项目默认前缀（类/属性 IRI 基址来源）
-- ont=注释属性命名空间（http://youming.com/ontology/annotation#，与 V10 注释属性注册表配套）
-- xsd=XML Schema 数据类型命名空间
INSERT INTO ont_model_prefix (id, project_id, prefix, namespace, is_default, create_by, create_time, update_by, update_time, del_flag)
VALUES (1700010, 1700001, 'ex',  'http://youming.com/ontology/organization#', '1', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;
INSERT INTO ont_model_prefix (id, project_id, prefix, namespace, is_default, create_by, create_time, update_by, update_time, del_flag)
VALUES (1700011, 1700001, 'ont', 'http://youming.com/ontology/annotation#',     '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;
INSERT INTO ont_model_prefix (id, project_id, prefix, namespace, is_default, create_by, create_time, update_by, update_time, del_flag)
VALUES (1700012, 1700001, 'xsd', 'http://www.w3.org/2001/XMLSchema#',           '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;


-- ======================
-- (e) 建模域 — 类实体（owl:Class）
-- ======================
-- class_iri = namespace_base + local_name（遵循 ModelClassServiceImpl 约定）
-- template_code / classification_code 溯源到治理域分类模板

-- Organization 根类
INSERT INTO ont_model_class (id, project_id, class_iri, local_name, label, label_cn, description, template_code, classification_code, icon, color, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (1700100, 1700001, 'http://youming.com/ontology/organization#Organization', 'Organization', 'Organization', '组织', '组织域根类（用户/部门/岗位的抽象父类）', 'organization', '10', '🏢', '#1890ff', 0, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- User 类
INSERT INTO ont_model_class (id, project_id, class_iri, local_name, label, label_cn, description, template_code, classification_code, icon, color, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (1700101, 1700001, 'http://youming.com/ontology/organization#User', 'User', 'User', '用户', '用户实体（映射 sys_user 表）', 'user', '10-01', '👤', '#52c41a', 1, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- Dept 类
INSERT INTO ont_model_class (id, project_id, class_iri, local_name, label, label_cn, description, template_code, classification_code, icon, color, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (1700102, 1700001, 'http://youming.com/ontology/organization#Dept', 'Dept', 'Dept', '部门', '部门实体（映射 sys_dept 表，自引用树结构）', 'dept', '10-02', '🏛️', '#722ed1', 2, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- Post 类
INSERT INTO ont_model_class (id, project_id, class_iri, local_name, label, label_cn, description, template_code, classification_code, icon, color, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (1700103, 1700001, 'http://youming.com/ontology/organization#Post', 'Post', 'Post', '岗位', '岗位实体（映射 sys_post 表）', 'post', '10-03', '💼', '#fa8c16', 3, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;


-- ======================
-- (f) 建模域 — 数据属性 + 对象属性
-- ======================
-- property_iri 格式 = {classLocalName}_{templateCode}（遵循 ClassInstantiationService / ModelDatatypePropertyServiceImpl 约定）
-- xsd_type 格式 = xsd:string / xsd:integer / xsd:boolean / xsd:dateTime

-- ---------- (f1) User 类数据属性（class_id=1700101） ----------
INSERT INTO ont_model_datatype_property (id, project_id, class_id, property_iri, local_name, label, template_code, xsd_type, min_cardinality, max_cardinality, is_identifier, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES
(1700201, 1700001, 1700101, 'User_username',    'username',    '用户名',   'username',    'xsd:string',   0, -1, '1', 1, 'admin', now(), 'admin', now(), '0'),
(1700202, 1700001, 1700101, 'User_nickname',   'nickname',    '昵称',     'nickname',    'xsd:string',   0, -1, '0', 2, 'admin', now(), 'admin', now(), '0'),
(1700203, 1700001, 1700101, 'User_email',      'email',       '邮箱',     'email',       'xsd:string',   0, -1, '0', 3, 'admin', now(), 'admin', now(), '0'),
(1700204, 1700001, 1700101, 'User_phone',      'phone',       '电话',     'phone',       'xsd:string',   0, -1, '0', 4, 'admin', now(), 'admin', now(), '0'),
(1700205, 1700001, 1700101, 'User_avatar',     'avatar',      '头像',     'avatar',      'xsd:string',   0, -1, '0', 5, 'admin', now(), 'admin', now(), '0'),
(1700206, 1700001, 1700101, 'User_lockFlag',   'lockFlag',    '锁定标记', 'lockFlag',    'xsd:boolean',  0, -1, '0', 6, 'admin', now(), 'admin', now(), '0'),
(1700207, 1700001, 1700101, 'User_isActive',   'isActive',    '是否启用', 'isActive',    'xsd:boolean',  0, -1, '0', 7, 'admin', now(), 'admin', now(), '0'),
(1700208, 1700001, 1700101, 'User_createdDate','createdDate', '创建日期', 'createdDate', 'xsd:dateTime', 0, -1, '0', 8, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ---------- (f2) Dept 类数据属性（class_id=1700102） ----------
INSERT INTO ont_model_datatype_property (id, project_id, class_id, property_iri, local_name, label, template_code, xsd_type, min_cardinality, max_cardinality, is_identifier, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES
(1700211, 1700001, 1700102, 'Dept_deptName',     'deptName',     '部门名称', 'deptName',     'xsd:string',  0, -1, '1', 1, 'admin', now(), 'admin', now(), '0'),
(1700212, 1700001, 1700102, 'Dept_deptSort',     'deptSort',     '部门排序', 'deptSort',     'xsd:integer', 0, -1, '0', 2, 'admin', now(), 'admin', now(), '0'),
(1700213, 1700001, 1700102, 'Dept_description',  'description',  '描述',     'description',  'xsd:string',  0, -1, '0', 3, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ---------- (f3) Post 类数据属性（class_id=1700103） ----------
INSERT INTO ont_model_datatype_property (id, project_id, class_id, property_iri, local_name, label, template_code, xsd_type, min_cardinality, max_cardinality, is_identifier, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES
(1700221, 1700001, 1700103, 'Post_postCode',     'postCode',     '岗位编码', 'postCode',     'xsd:string',  0, -1, '1', 1, 'admin', now(), 'admin', now(), '0'),
(1700222, 1700001, 1700103, 'Post_postName',     'postName',     '岗位名称', 'postName',     'xsd:string',  0, -1, '0', 2, 'admin', now(), 'admin', now(), '0'),
(1700223, 1700001, 1700103, 'Post_postSort',     'postSort',     '岗位排序', 'postSort',     'xsd:integer', 0, -1, '0', 3, 'admin', now(), 'admin', now(), '0'),
(1700224, 1700001, 1700103, 'Post_remark',       'remark',       '备注',     'remark',       'xsd:string',  0, -1, '0', 4, 'admin', now(), 'admin', now(), '0'),
(1700225, 1700001, 1700103, 'Post_description',  'description',  '描述',     'description',  'xsd:string',  0, -1, '0', 5, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ---------- (f4) 对象属性（owl:ObjectProperty） ----------
-- domain_class_id / range_class_id 指向 ont_model_class.id
-- property_iri 格式 = {domainClassLocalName}_{templateCode}
-- min/max_cardinality：many-to-one 用 0..1，many-to-many/one-to-many 用 0..n（max=-1）

-- User --belongsTo--> Dept（用户属于部门，复用内置 belongsTo 模板）
INSERT INTO ont_model_object_property (id, project_id, domain_class_id, range_class_id, property_iri, local_name, label, template_code, min_cardinality, max_cardinality, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (1700301, 1700001, 1700101, 1700102, 'User_belongsTo', 'belongsTo', '属于', 'belongsTo', 0, -1, 1, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- User --hasPost--> Post（用户拥有岗位，多对多）
INSERT INTO ont_model_object_property (id, project_id, domain_class_id, range_class_id, property_iri, local_name, label, template_code, min_cardinality, max_cardinality, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (1700302, 1700001, 1700101, 1700103, 'User_hasPost', 'hasPost', '拥有岗位', 'hasPost', 0, -1, 2, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- User --primaryDept--> Dept（用户主部门，区别于多部门 belongsTo，基数 0..1）
INSERT INTO ont_model_object_property (id, project_id, domain_class_id, range_class_id, property_iri, local_name, label, template_code, min_cardinality, max_cardinality, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (1700303, 1700001, 1700101, 1700102, 'User_primaryDept', 'primaryDept', '主部门', 'primaryDept', 0, 1, 3, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- Dept --hasSubDept--> Dept（部门包含子部门，自引用 one-to-many）
INSERT INTO ont_model_object_property (id, project_id, domain_class_id, range_class_id, property_iri, local_name, label, template_code, min_cardinality, max_cardinality, sort_order, create_by, create_time, update_by, update_time, del_flag)
VALUES (1700304, 1700001, 1700102, 1700102, 'Dept_hasSubDept', 'hasSubDept', '包含子部门', 'hasSubDept', 0, -1, 1, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;


-- ======================
-- (g) 建模域 — 类层级 subClassOf + 治理域镜像回推
-- ======================
-- ont_model_subclassof 有 uk(project_id, child_class_id, parent_class_id)，用 ON CONFLICT DO NOTHING
-- sync_status='1'（已同步）：种子数据直接写入，无需异步回推补偿

-- ---------- (g1) 建模域类层级（权威源） ----------
-- User subClassOf Organization
INSERT INTO ont_model_subclassof (id, project_id, child_class_id, parent_class_id, source_template_ref, sync_status, sync_time, retry_count, create_by, create_time, update_by, update_time, del_flag)
VALUES (1700401, 1700001, 1700101, 1700100, 'organization', '1', now(), 0, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- Dept subClassOf Organization
INSERT INTO ont_model_subclassof (id, project_id, child_class_id, parent_class_id, source_template_ref, sync_status, sync_time, retry_count, create_by, create_time, update_by, update_time, del_flag)
VALUES (1700402, 1700001, 1700102, 1700100, 'organization', '1', now(), 0, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- Post subClassOf Organization
INSERT INTO ont_model_subclassof (id, project_id, child_class_id, parent_class_id, source_template_ref, sync_status, sync_time, retry_count, create_by, create_time, update_by, update_time, del_flag)
VALUES (1700403, 1700001, 1700103, 1700100, 'organization', '1', now(), 0, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ---------- (g2) 治理域类层级镜像（只读，建模侧回推） ----------
-- ont_class_hierarchy 无 uk 约束，用 WHERE NOT EXISTS 保护幂等
-- child/parent_class_iri 使用完整 IRI（与 ont_model_class.class_iri 一致）

INSERT INTO ont_class_hierarchy (id, child_class_iri, parent_class_iri, source_template_ref, tree_root, sync_status, sync_time, create_by, create_time, update_by, update_time, del_flag)
SELECT 18001,
       'http://youming.com/ontology/organization#User',
       'http://youming.com/ontology/organization#Organization',
       'organization', 'organization', '1', now(), 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_hierarchy WHERE id = 18001);

INSERT INTO ont_class_hierarchy (id, child_class_iri, parent_class_iri, source_template_ref, tree_root, sync_status, sync_time, create_by, create_time, update_by, update_time, del_flag)
SELECT 18002,
       'http://youming.com/ontology/organization#Dept',
       'http://youming.com/ontology/organization#Organization',
       'organization', 'organization', '1', now(), 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_hierarchy WHERE id = 18002);

INSERT INTO ont_class_hierarchy (id, child_class_iri, parent_class_iri, source_template_ref, tree_root, sync_status, sync_time, create_by, create_time, update_by, update_time, del_flag)
SELECT 18003,
       'http://youming.com/ontology/organization#Post',
       'http://youming.com/ontology/organization#Organization',
       'organization', 'organization', '1', now(), 'admin', now(), 'admin', now(), '0'
WHERE NOT EXISTS (SELECT 1 FROM ont_class_hierarchy WHERE id = 18003);
