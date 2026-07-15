-- ============================================================
-- V24: 组织账号扩展本体与UPMS映射种子数据 (模块18 功能18-09)
-- 设计文档: docs/ontology/国标版设计/设计文档/18-09-组织账号扩展本体与UPMS映射详细设计.md
--
-- 本迁移脚本包含两部分：
--   A. DATETIME前置改造（18-04 §7.2要求，V22未落地）：
--      扩展 ont_data_property.base_type 和 ont_instance_data_value.literal_type 的 CHECK 约束
--   B. 组织账号扩展本体种子数据（18-09 §3~§13）：
--      命名空间930200、扩展模块939002、实体类型942001~942004、
--      数据属性954001~954017、对象属性962001~962004、扩展资源关联
--
-- 不插入业务实例；不插入数据源密码；不自动创建/发布映射版本。
-- 所有 INSERT 使用 ON CONFLICT DO NOTHING 保证幂等。
-- ============================================================

-- ============================================================
-- A. DATETIME 前置改造（18-04 §7.2）
-- ============================================================

-- A.1 扩展 ont_data_property.base_type CHECK 增加 DATETIME
ALTER TABLE ont_data_property
  DROP CONSTRAINT IF EXISTS ck_ont_data_property_base_type;
ALTER TABLE ont_data_property
  ADD CONSTRAINT ck_ont_data_property_base_type
  CHECK (base_type IN ('BOOLEAN','DATE','DATETIME','NUMERIC','TEXT','URI','UNIT_REF','TEXT_OR_NUMERIC'));

-- A.2 扩展 ont_instance_data_value.literal_type CHECK 增加 DATETIME
ALTER TABLE ont_instance_data_value
  DROP CONSTRAINT IF EXISTS ck_ont_instance_value_literal_type;
ALTER TABLE ont_instance_data_value
  ADD CONSTRAINT ck_ont_instance_value_literal_type
  CHECK (literal_type IN ('STRING','URI','DATE','DATETIME','INTEGER','DECIMAL','BOOLEAN'));


-- ============================================================
-- B. 组织账号扩展本体种子数据
-- ============================================================

-- ----------------------------
-- B.1 扩展命名空间 ymorg (930200)
-- ----------------------------
INSERT INTO ont_namespace (id, prefix, uri, is_default, is_builtin, sort_order, description,
  create_by, create_time, update_by, update_time, del_flag)
VALUES
  (930200, 'ymorg', 'http://example.org/youming/organization#',
   '0', '0', 300, 'youming组织与账号扩展命名空间；正式域名确定后通过命名空间迁移机制替换',
   'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- B.2 扩展模块 youming-organization (939002)
-- 归属核心本体工程 935001、命名空间 930200
-- ----------------------------
INSERT INTO ont_extension_module (id, module_code, module_name, namespace_id, ontology_id,
  description, version, is_builtin, sort_order, remarks,
  create_by, create_time, update_by, update_time, del_flag)
VALUES
  (939002, 'youming-organization', 'youming组织与账号扩展', 930200, 935001,
   '组织单元、岗位和平台账号语义模型，用于数据源映射自举验证',
   '1.0.0', '0', 20, NULL,
   'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- B.3 扩展实体类型（4条，id 942001~942004）
-- 命名空间 930200(ymorg)、本体工程 935001(core)
-- IRI 前缀 http://example.org/youming/organization#
-- ----------------------------
INSERT INTO ont_entity_type (id, iri, name, definition, is_abstract, is_builtin,
  ontology_id, namespace_id, sort_order, remarks, security_level_code,
  create_by, create_time, update_by, update_time, del_flag) VALUES
  (942001, 'http://example.org/youming/organization#OrganizationDomainEntity', 'OrganizationDomainEntity',
   '组织账号领域抽象对象，组织单元、岗位和平台账号的公共父类', '1', '0',
   935001, 930200, 300, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (942002, 'http://example.org/youming/organization#UserAccount', 'UserAccount',
   '可用于登录或被系统识别的平台账号', '0', '0',
   935001, 930200, 301, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (942003, 'http://example.org/youming/organization#OrganizationalUnit', 'OrganizationalUnit',
   '部门、机构等组织单元', '0', '0',
   935001, 930200, 302, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (942004, 'http://example.org/youming/organization#Position', 'Position',
   '可分配给账号/人员的岗位', '0', '0',
   935001, 930200, 303, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- B.4 实体类型中文标签（4条）
-- ----------------------------
INSERT INTO ont_entity_type_label (entity_type_id, locale, label) VALUES
  (942001, 'zh', '组织账号领域对象'),
  (942002, 'zh', '用户账号'),
  (942003, 'zh', '组织单元'),
  (942004, 'zh', '岗位')
ON CONFLICT (entity_type_id, locale) DO NOTHING;

-- ----------------------------
-- B.5 继承关系（4条）
-- OrganizationDomainEntity 继承核心 Object(940063)
-- UserAccount / OrganizationalUnit / Position 继承 OrganizationDomainEntity(942001)
-- 不直接继承核心 Stakeholder，因为账号不等于相关方
-- ----------------------------
INSERT INTO ont_entity_type_hierarchy (parent_id, child_id) VALUES
  (940063, 942001),
  (942001, 942002),
  (942001, 942003),
  (942001, 942004)
ON CONFLICT (parent_id, child_id) DO NOTHING;

-- ----------------------------
-- B.6 扩展数据属性（17条，id 954001~954017）
-- 命名空间 930200(ymorg)、本体工程 935001(core)
-- source_type='EXTENSION', is_builtin='0', source_reference='youming-organization'
-- 安全级别：email(954009)/phone(954010) 为 CONFIDENTIAL，其余为 INTERNAL
-- 唯一性：username(954005)/positionCode(954013) is_unique='1'，externalId(954002) is_unique='0'
-- DATETIME：createdAt(954003)/updatedAt(954004) base_type='DATETIME'（依赖A部分CHECK扩展）
-- ----------------------------
INSERT INTO ont_data_property (id, iri, iri_local_name, standard_iri, name, preferred_alias,
  definition, domain_entity_type_id, base_type, value_mode, value_source_ref,
  regex_pattern, format_hint, is_unique, unit_category_id, unit_ref_mode,
  source_type, source_reference, is_builtin, ontology_id, namespace_id,
  sort_order, remarks, security_level_code,
  create_by, create_time, update_by, update_time, del_flag) VALUES
  -- OrganizationDomainEntity 公共属性 (domain=942001)
  (954001, 'http://example.org/youming/organization#externalSystem', 'externalSystem', NULL, 'externalSystem', NULL,
   '来源系统标识，首期YOU_MING_UPMS', 942001, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 300, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (954002, 'http://example.org/youming/organization#externalId', 'externalId', NULL, 'externalId', NULL,
   '源系统内部ID，不声明跨类全局唯一', 942001, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 301, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (954003, 'http://example.org/youming/organization#createdAt', 'createdAt', NULL, 'createdAt', NULL,
   '创建时间', 942001, 'DATETIME', 'FREE', NULL, NULL, NULL, '0', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 302, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (954004, 'http://example.org/youming/organization#updatedAt', 'updatedAt', NULL, 'updatedAt', NULL,
   '更新时间', 942001, 'DATETIME', 'FREE', NULL, NULL, NULL, '0', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 303, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (954017, 'http://example.org/youming/organization#lifecycleStatus', 'lifecycleStatus', NULL, 'lifecycleStatus', NULL,
   '来源记录生命周期：ACTIVE/INACTIVE', 942001, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 304, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  -- UserAccount 专属属性 (domain=942002)
  (954005, 'http://example.org/youming/organization#username', 'username', NULL, 'username', NULL,
   '登录名，在UserAccount范围唯一', 942002, 'TEXT', 'FREE', NULL, NULL, NULL, '1', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 310, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (954006, 'http://example.org/youming/organization#displayName', 'displayName', NULL, 'displayName', NULL,
   '展示名称', 942002, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 311, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (954007, 'http://example.org/youming/organization#nickname', 'nickname', NULL, 'nickname', NULL,
   '昵称', 942002, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 312, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (954008, 'http://example.org/youming/organization#accountStatus', 'accountStatus', NULL, 'accountStatus', NULL,
   '账号状态：ACTIVE/LOCKED', 942002, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 313, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (954009, 'http://example.org/youming/organization#email', 'email', NULL, 'email', NULL,
   '邮箱，静态加密', 942002, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 314, NULL, 'CONFIDENTIAL', 'admin', now(), 'admin', now(), '0'),
  (954010, 'http://example.org/youming/organization#phone', 'phone', NULL, 'phone', NULL,
   '手机号，静态加密', 942002, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 315, NULL, 'CONFIDENTIAL', 'admin', now(), 'admin', now(), '0'),
  -- OrganizationalUnit 专属属性 (domain=942003)
  (954011, 'http://example.org/youming/organization#organizationName', 'organizationName', NULL, 'organizationName', NULL,
   '部门名称', 942003, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 320, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (954012, 'http://example.org/youming/organization#organizationSortOrder', 'organizationSortOrder', NULL, 'organizationSortOrder', NULL,
   '部门排序', 942003, 'NUMERIC', 'FREE', NULL, NULL, NULL, '0', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 321, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  -- Position 专属属性 (domain=942004)
  (954013, 'http://example.org/youming/organization#positionCode', 'positionCode', NULL, 'positionCode', NULL,
   '岗位编码，在Position范围唯一', 942004, 'TEXT', 'FREE', NULL, NULL, NULL, '1', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 330, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (954014, 'http://example.org/youming/organization#positionName', 'positionName', NULL, 'positionName', NULL,
   '岗位名称', 942004, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 331, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (954015, 'http://example.org/youming/organization#positionDescription', 'positionDescription', NULL, 'positionDescription', NULL,
   '岗位描述', 942004, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 332, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (954016, 'http://example.org/youming/organization#positionSortOrder', 'positionSortOrder', NULL, 'positionSortOrder', NULL,
   '岗位排序', 942004, 'NUMERIC', 'FREE', NULL, NULL, NULL, '0', NULL, NULL,
   'EXTENSION', 'youming-organization', '0', 935001, 930200, 333, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- B.7 数据属性中文标签（17条）
-- ----------------------------
INSERT INTO ont_data_property_label (data_property_id, locale, label) VALUES
  (954001, 'zh', '来源系统'),
  (954002, 'zh', '外部ID'),
  (954003, 'zh', '创建时间'),
  (954004, 'zh', '更新时间'),
  (954005, 'zh', '用户名'),
  (954006, 'zh', '展示名称'),
  (954007, 'zh', '昵称'),
  (954008, 'zh', '账号状态'),
  (954009, 'zh', '邮箱'),
  (954010, 'zh', '手机号'),
  (954011, 'zh', '组织名称'),
  (954012, 'zh', '组织排序'),
  (954013, 'zh', '岗位编码'),
  (954014, 'zh', '岗位名称'),
  (954015, 'zh', '岗位描述'),
  (954016, 'zh', '岗位排序'),
  (954017, 'zh', '生命周期状态')
ON CONFLICT (data_property_id, locale) DO NOTHING;

-- ----------------------------
-- B.8 数据属性枚举值（3组 CLOSED_ENUM 属性，共7条）
-- externalSystem(954001): YOU_MING_UPMS
-- accountStatus(954008): ACTIVE/LOCKED
-- lifecycleStatus(954017): ACTIVE/INACTIVE
-- ----------------------------
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  -- externalSystem 枚举
  (954001, 'YOU_MING_UPMS', NULL, '1', 'youming-organization', 1)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  -- accountStatus 枚举
  (954008, 'ACTIVE', NULL, '1', 'youming-organization', 1),
  (954008, 'LOCKED', NULL, '1', 'youming-organization', 2)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  -- lifecycleStatus 枚举
  (954017, 'ACTIVE', NULL, '1', 'youming-organization', 1),
  (954017, 'INACTIVE', NULL, '1', 'youming-organization', 2)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- ----------------------------
-- B.9 扩展对象属性（4条，id 962001~962004）
-- 命名空间 930200(ymorg)、本体工程 935001(core)
-- source_type='EXTENSION', is_builtin='0', source_reference='youming-organization'
-- functional: parentOrganizationUnit(962001)和primaryDepartment(962002)为函数型
-- 不配置 inverseOf
-- ----------------------------
INSERT INTO ont_object_property (id, iri, iri_local_name, name, definition, inverse_of_id,
  is_functional, is_inverse_functional, is_transitive, is_symmetric,
  source_type, source_reference, is_builtin, ontology_id, namespace_id,
  sort_order, remarks, security_level_code,
  create_by, create_time, update_by, update_time, del_flag) VALUES
  (962001, 'http://example.org/youming/organization#parentOrganizationUnit', 'parentOrganizationUnit', 'parentOrganizationUnit',
   '组织单元的父级组织单元，来自sys_dept.parent_id', NULL,
   '1', '0', '0', '0',
   'EXTENSION', 'youming-organization', '0', 935001, 930200,
   300, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (962002, 'http://example.org/youming/organization#primaryDepartment', 'primaryDepartment', 'primaryDepartment',
   '用户账号的主属部门，来自sys_user.dept_id', NULL,
   '1', '0', '0', '0',
   'EXTENSION', 'youming-organization', '0', 935001, 930200,
   310, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (962003, 'http://example.org/youming/organization#memberOfDepartment', 'memberOfDepartment', 'memberOfDepartment',
   '用户账号所属部门（多部门），来自sys_user_dept关联表', NULL,
   '0', '0', '0', '0',
   'EXTENSION', 'youming-organization', '0', 935001, 930200,
   311, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0'),
  (962004, 'http://example.org/youming/organization#assignedPosition', 'assignedPosition', 'assignedPosition',
   '用户账号分配的岗位，来自sys_user_post关联表', NULL,
   '0', '0', '0', '0',
   'EXTENSION', 'youming-organization', '0', 935001, 930200,
   312, NULL, 'INTERNAL', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- B.10 对象属性中文标签（4条）
-- ----------------------------
INSERT INTO ont_object_property_label (object_property_id, locale, label) VALUES
  (962001, 'zh', '上级组织单元'),
  (962002, 'zh', '主属部门'),
  (962003, 'zh', '所属部门'),
  (962004, 'zh', '分配岗位')
ON CONFLICT (object_property_id, locale) DO NOTHING;

-- ----------------------------
-- B.11 对象属性定义域关联（4条）
-- ----------------------------
INSERT INTO ont_object_property_domain (object_property_id, entity_type_id, sort_order) VALUES
  (962001, 942003, 1),  -- parentOrganizationUnit: domain=OrganizationalUnit
  (962002, 942002, 1),  -- primaryDepartment: domain=UserAccount
  (962003, 942002, 1),  -- memberOfDepartment: domain=UserAccount
  (962004, 942002, 1)   -- assignedPosition: domain=UserAccount
ON CONFLICT (object_property_id, entity_type_id) DO NOTHING;

-- ----------------------------
-- B.12 对象属性值域关联（4条）
-- ----------------------------
INSERT INTO ont_object_property_range (object_property_id, entity_type_id, sort_order) VALUES
  (962001, 942003, 1),  -- parentOrganizationUnit: range=OrganizationalUnit
  (962002, 942003, 1),  -- primaryDepartment: range=OrganizationalUnit
  (962003, 942003, 1),  -- memberOfDepartment: range=OrganizationalUnit
  (962004, 942004, 1)   -- assignedPosition: range=Position
ON CONFLICT (object_property_id, entity_type_id) DO NOTHING;

-- ----------------------------
-- B.13 扩展资源关联（25条，注册到 module_id=939002）
-- ID段 983050~983074（避开消防模块 983001~983043）
-- 4 实体类型 + 17 数据属性 + 4 对象属性 = 25
-- ----------------------------

-- 实体类型资源（4条）
INSERT INTO ont_extension_resource (id, module_id, resource_type, resource_id, resource_iri, resource_name,
  create_by, create_time, update_by, update_time, del_flag) VALUES
  (983050, 939002, 'ENTITY_TYPE', 942001, 'http://example.org/youming/organization#OrganizationDomainEntity', 'OrganizationDomainEntity', 'admin', now(), 'admin', now(), '0'),
  (983051, 939002, 'ENTITY_TYPE', 942002, 'http://example.org/youming/organization#UserAccount', 'UserAccount', 'admin', now(), 'admin', now(), '0'),
  (983052, 939002, 'ENTITY_TYPE', 942003, 'http://example.org/youming/organization#OrganizationalUnit', 'OrganizationalUnit', 'admin', now(), 'admin', now(), '0'),
  (983053, 939002, 'ENTITY_TYPE', 942004, 'http://example.org/youming/organization#Position', 'Position', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 数据属性资源（17条）
INSERT INTO ont_extension_resource (id, module_id, resource_type, resource_id, resource_iri, resource_name,
  create_by, create_time, update_by, update_time, del_flag) VALUES
  (983054, 939002, 'DATA_PROPERTY', 954001, 'http://example.org/youming/organization#externalSystem', 'externalSystem', 'admin', now(), 'admin', now(), '0'),
  (983055, 939002, 'DATA_PROPERTY', 954002, 'http://example.org/youming/organization#externalId', 'externalId', 'admin', now(), 'admin', now(), '0'),
  (983056, 939002, 'DATA_PROPERTY', 954003, 'http://example.org/youming/organization#createdAt', 'createdAt', 'admin', now(), 'admin', now(), '0'),
  (983057, 939002, 'DATA_PROPERTY', 954004, 'http://example.org/youming/organization#updatedAt', 'updatedAt', 'admin', now(), 'admin', now(), '0'),
  (983058, 939002, 'DATA_PROPERTY', 954005, 'http://example.org/youming/organization#username', 'username', 'admin', now(), 'admin', now(), '0'),
  (983059, 939002, 'DATA_PROPERTY', 954006, 'http://example.org/youming/organization#displayName', 'displayName', 'admin', now(), 'admin', now(), '0'),
  (983060, 939002, 'DATA_PROPERTY', 954007, 'http://example.org/youming/organization#nickname', 'nickname', 'admin', now(), 'admin', now(), '0'),
  (983061, 939002, 'DATA_PROPERTY', 954008, 'http://example.org/youming/organization#accountStatus', 'accountStatus', 'admin', now(), 'admin', now(), '0'),
  (983062, 939002, 'DATA_PROPERTY', 954009, 'http://example.org/youming/organization#email', 'email', 'admin', now(), 'admin', now(), '0'),
  (983063, 939002, 'DATA_PROPERTY', 954010, 'http://example.org/youming/organization#phone', 'phone', 'admin', now(), 'admin', now(), '0'),
  (983064, 939002, 'DATA_PROPERTY', 954011, 'http://example.org/youming/organization#organizationName', 'organizationName', 'admin', now(), 'admin', now(), '0'),
  (983065, 939002, 'DATA_PROPERTY', 954012, 'http://example.org/youming/organization#organizationSortOrder', 'organizationSortOrder', 'admin', now(), 'admin', now(), '0'),
  (983066, 939002, 'DATA_PROPERTY', 954013, 'http://example.org/youming/organization#positionCode', 'positionCode', 'admin', now(), 'admin', now(), '0'),
  (983067, 939002, 'DATA_PROPERTY', 954014, 'http://example.org/youming/organization#positionName', 'positionName', 'admin', now(), 'admin', now(), '0'),
  (983068, 939002, 'DATA_PROPERTY', 954015, 'http://example.org/youming/organization#positionDescription', 'positionDescription', 'admin', now(), 'admin', now(), '0'),
  (983069, 939002, 'DATA_PROPERTY', 954016, 'http://example.org/youming/organization#positionSortOrder', 'positionSortOrder', 'admin', now(), 'admin', now(), '0'),
  (983070, 939002, 'DATA_PROPERTY', 954017, 'http://example.org/youming/organization#lifecycleStatus', 'lifecycleStatus', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 对象属性资源（4条）
INSERT INTO ont_extension_resource (id, module_id, resource_type, resource_id, resource_iri, resource_name,
  create_by, create_time, update_by, update_time, del_flag) VALUES
  (983071, 939002, 'OBJECT_PROPERTY', 962001, 'http://example.org/youming/organization#parentOrganizationUnit', 'parentOrganizationUnit', 'admin', now(), 'admin', now(), '0'),
  (983072, 939002, 'OBJECT_PROPERTY', 962002, 'http://example.org/youming/organization#primaryDepartment', 'primaryDepartment', 'admin', now(), 'admin', now(), '0'),
  (983073, 939002, 'OBJECT_PROPERTY', 962003, 'http://example.org/youming/organization#memberOfDepartment', 'memberOfDepartment', 'admin', now(), 'admin', now(), '0'),
  (983074, 939002, 'OBJECT_PROPERTY', 962004, 'http://example.org/youming/organization#assignedPosition', 'assignedPosition', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- C. 完整性断言校验
-- ============================================================
DO $$
DECLARE
  v_ns_count integer;
  v_module_count integer;
  v_entity_count integer;
  v_data_prop_count integer;
  v_obj_prop_count integer;
  v_ext_resource_count integer;
  v_confidential_count integer;
BEGIN
  -- 命名空间
  SELECT count(*) INTO v_ns_count FROM ont_namespace WHERE id = 930200 AND prefix = 'ymorg' AND del_flag = '0';
  IF v_ns_count <> 1 THEN
    RAISE EXCEPTION 'V24校验失败: 命名空间930200(ymorg)未正确插入，期望1条，实际%', v_ns_count;
  END IF;

  -- 扩展模块
  SELECT count(*) INTO v_module_count FROM ont_extension_module WHERE id = 939002 AND module_code = 'youming-organization' AND del_flag = '0';
  IF v_module_count <> 1 THEN
    RAISE EXCEPTION 'V24校验失败: 扩展模块939002未正确插入，期望1条，实际%', v_module_count;
  END IF;

  -- 实体类型
  SELECT count(*) INTO v_entity_count FROM ont_entity_type WHERE id BETWEEN 942001 AND 942004 AND del_flag = '0';
  IF v_entity_count <> 4 THEN
    RAISE EXCEPTION 'V24校验失败: 实体类型942001~942004期望4条，实际%', v_entity_count;
  END IF;

  -- 数据属性
  SELECT count(*) INTO v_data_prop_count FROM ont_data_property WHERE id BETWEEN 954001 AND 954017 AND del_flag = '0';
  IF v_data_prop_count <> 17 THEN
    RAISE EXCEPTION 'V24校验失败: 数据属性954001~954017期望17条，实际%', v_data_prop_count;
  END IF;

  -- 对象属性
  SELECT count(*) INTO v_obj_prop_count FROM ont_object_property WHERE id BETWEEN 962001 AND 962004 AND del_flag = '0';
  IF v_obj_prop_count <> 4 THEN
    RAISE EXCEPTION 'V24校验失败: 对象属性962001~962004期望4条，实际%', v_obj_prop_count;
  END IF;

  -- 扩展资源关联
  SELECT count(*) INTO v_ext_resource_count FROM ont_extension_resource WHERE module_id = 939002 AND del_flag = '0';
  IF v_ext_resource_count <> 25 THEN
    RAISE EXCEPTION 'V24校验失败: 扩展资源关联期望25条，实际%', v_ext_resource_count;
  END IF;

  -- 敏感字段安全级别校验：email/phone 必须为 CONFIDENTIAL
  SELECT count(*) INTO v_confidential_count FROM ont_data_property
  WHERE id IN (954009, 954010) AND security_level_code = 'CONFIDENTIAL' AND del_flag = '0';
  IF v_confidential_count <> 2 THEN
    RAISE EXCEPTION 'V24校验失败: email/phone安全级别期望CONFIDENTIAL，实际符合%', v_confidential_count;
  END IF;

  -- IRI拼接校验：iri = namespace.uri + iri_local_name
  IF NOT EXISTS (
    SELECT 1 FROM ont_data_property dp
    JOIN ont_namespace ns ON ns.id = dp.namespace_id
    WHERE dp.id = 954005
      AND dp.iri = ns.uri || dp.iri_local_name
  ) THEN
    RAISE EXCEPTION 'V24校验失败: 数据属性954005(username)的IRI不满足 iri = namespace.uri + iri_local_name 拼接规则';
  END IF;

  -- DATETIME类型校验：createdAt/updatedAt 必须为 DATETIME base_type
  IF NOT EXISTS (
    SELECT 1 FROM ont_data_property WHERE id IN (954003, 954004) AND base_type = 'DATETIME'
  ) THEN
    RAISE EXCEPTION 'V24校验失败: createdAt/updatedAt(954003/954004)的base_type必须为DATETIME';
  END IF;

  RAISE NOTICE 'V24校验通过: 命名空间=%, 模块=%, 实体=%, 数据属性=%, 对象属性=%, 扩展资源=%',
    v_ns_count, v_module_count, v_entity_count, v_data_prop_count, v_obj_prop_count, v_ext_resource_count;
END $$;
