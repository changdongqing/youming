-- ----------------------------
-- youming ontology extension management
-- 依据：GB/T 48000.3—2026 第9章（扩展方式）
-- 前置：V4~V12（单位字典、命名空间、实体类型、数据属性、对象属性、公理规则、实体实例、校验引擎、序列化与交换）
-- ----------------------------

-- ----------------------------
-- 1. 扩展模块表 ont_extension_module
-- ----------------------------
CREATE TABLE ont_extension_module (
  id bigint NOT NULL,
  module_code varchar(64) NOT NULL,
  module_name varchar(128) NOT NULL,
  namespace_id bigint NOT NULL,
  ontology_id bigint NOT NULL DEFAULT 935001,
  description text DEFAULT NULL,
  version varchar(32) DEFAULT NULL,
  is_builtin char(1) NOT NULL DEFAULT '0',
  sort_order integer NOT NULL DEFAULT 0,
  remarks text DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_ext_module_namespace FOREIGN KEY (namespace_id)
    REFERENCES ont_namespace (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_ext_module_ontology FOREIGN KEY (ontology_id)
    REFERENCES ont_ontology_project (id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_ext_module_builtin CHECK (is_builtin IN ('0', '1')),
  CONSTRAINT ck_ont_ext_module_del_flag CHECK (del_flag IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_ont_ext_module_code ON ont_extension_module (module_code) WHERE del_flag = '0';
CREATE INDEX idx_ont_ext_module_namespace ON ont_extension_module (namespace_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_ext_module_ontology ON ont_extension_module (ontology_id) WHERE del_flag = '0';

COMMENT ON TABLE ont_extension_module IS '本体建模-扩展管理模块（行业扩展资源归属容器）';
COMMENT ON COLUMN ont_extension_module.id IS '扩展模块ID';
COMMENT ON COLUMN ont_extension_module.module_code IS '模块代码（全局唯一，如 medical/petroleum/elevator）';
COMMENT ON COLUMN ont_extension_module.module_name IS '模块名称（如 医疗行业扩展）';
COMMENT ON COLUMN ont_extension_module.namespace_id IS '绑定的扩展命名空间ID（ont_namespace.id, is_builtin=0）';
COMMENT ON COLUMN ont_extension_module.ontology_id IS '归属本体工程ID（默认935001核心工程）';
COMMENT ON COLUMN ont_extension_module.description IS '模块描述';
COMMENT ON COLUMN ont_extension_module.version IS '模块版本号（首期自由文本）';
COMMENT ON COLUMN ont_extension_module.is_builtin IS '是否内置模块（首期恒为0，预留）';
COMMENT ON COLUMN ont_extension_module.sort_order IS '排序';
COMMENT ON COLUMN ont_extension_module.remarks IS '备注';
COMMENT ON COLUMN ont_extension_module.create_by IS '创建人';
COMMENT ON COLUMN ont_extension_module.create_time IS '创建时间';
COMMENT ON COLUMN ont_extension_module.update_by IS '修改人';
COMMENT ON COLUMN ont_extension_module.update_time IS '更新时间';
COMMENT ON COLUMN ont_extension_module.del_flag IS '删除标记,1:已删除,0:正常';

-- ----------------------------
-- 2. 扩展资源关联表 ont_extension_resource
-- ----------------------------
CREATE TABLE ont_extension_resource (
  id bigint NOT NULL,
  module_id bigint NOT NULL,
  resource_type varchar(32) NOT NULL,
  resource_id bigint NOT NULL,
  resource_iri text DEFAULT NULL,
  resource_name varchar(128) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_ext_resource_module FOREIGN KEY (module_id)
    REFERENCES ont_extension_module (id) ON DELETE CASCADE,
  CONSTRAINT ck_ont_ext_resource_type CHECK (resource_type IN ('ENTITY_TYPE', 'DATA_PROPERTY', 'OBJECT_PROPERTY', 'AXIOM_RULE', 'UNIT')),
  CONSTRAINT ck_ont_ext_resource_del_flag CHECK (del_flag IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_ont_ext_resource ON ont_extension_resource (module_id, resource_type, resource_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_ext_resource_module ON ont_extension_resource (module_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_ext_resource_type ON ont_extension_resource (resource_type, resource_id) WHERE del_flag = '0';

COMMENT ON TABLE ont_extension_resource IS '本体建模-扩展资源关联表（扩展模块与资源的逻辑关联）';
COMMENT ON COLUMN ont_extension_resource.id IS '关联记录ID';
COMMENT ON COLUMN ont_extension_resource.module_id IS '扩展模块ID';
COMMENT ON COLUMN ont_extension_resource.resource_type IS '资源类型: ENTITY_TYPE/DATA_PROPERTY/OBJECT_PROPERTY/AXIOM_RULE/UNIT';
COMMENT ON COLUMN ont_extension_resource.resource_id IS '资源记录ID（多态引用，无物理外键）';
COMMENT ON COLUMN ont_extension_resource.resource_iri IS '资源IRI（冗余快照，便于导出和展示）';
COMMENT ON COLUMN ont_extension_resource.resource_name IS '资源名称（冗余快照，便于列表展示）';
COMMENT ON COLUMN ont_extension_resource.create_by IS '创建人';
COMMENT ON COLUMN ont_extension_resource.create_time IS '创建时间';
COMMENT ON COLUMN ont_extension_resource.update_by IS '修改人';
COMMENT ON COLUMN ont_extension_resource.update_time IS '更新时间';
COMMENT ON COLUMN ont_extension_resource.del_flag IS '删除标记,1:已删除,0:正常';

-- ----------------------------
-- 3. 菜单：本体建模 / 扩展管理
-- ----------------------------
INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES (901000, '扩展管理', NULL, '/ontology/extension/index', NULL, 900000, 'ele-Expand', '1', 10, '0', NULL, '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES
  (901001, '模块查看', 'ontology_extension_view', NULL, NULL, 901000, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (901002, '模块新增', 'ontology_extension_add', NULL, NULL, 901000, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (901003, '模块编辑', 'ontology_extension_edit', NULL, NULL, 901000, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (901004, '模块删除', 'ontology_extension_del', NULL, NULL, 901000, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (901005, '合法性校验', 'ontology_extension_validate', NULL, NULL, 901000, NULL, '1', 5, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (901006, '模块导出', 'ontology_extension_export', NULL, NULL, 901000, NULL, '1', 6, '0', NULL, '1', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
  (1, 901000), (1, 901001), (1, 901002), (1, 901003), (1, 901004), (1, 901005), (1, 901006)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ----------------------------
-- 4. 迁移完整性断言
-- ----------------------------
DO $$
BEGIN
  -- 1. 扩展模块表存在
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_extension_module') THEN
    RAISE EXCEPTION 'ont_extension_module表未创建';
  END IF;

  -- 2. 扩展资源关联表存在
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_extension_resource') THEN
    RAISE EXCEPTION 'ont_extension_resource表未创建';
  END IF;

  -- 3. 菜单901000~901006完整
  IF (SELECT count(*) FROM sys_menu WHERE menu_id BETWEEN 901000 AND 901006 AND del_flag = '0') <> 7 THEN
    RAISE EXCEPTION '扩展管理菜单901000~901006不完整';
  END IF;

  -- 4. 管理员授权完整
  IF (SELECT count(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 901000 AND 901006) <> 7 THEN
    RAISE EXCEPTION '扩展管理管理员授权不完整';
  END IF;

  -- 5. CHECK约束存在
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.check_constraints
    WHERE constraint_name = 'ck_ont_ext_resource_type'
  ) THEN
    RAISE EXCEPTION 'ck_ont_ext_resource_type约束未创建';
  END IF;

  -- 6. 唯一索引存在
  IF NOT EXISTS (
    SELECT 1 FROM pg_indexes WHERE indexname = 'uk_ont_ext_module_code'
  ) THEN
    RAISE EXCEPTION 'uk_ont_ext_module_code唯一索引未创建';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_indexes WHERE indexname = 'uk_ont_ext_resource'
  ) THEN
    RAISE EXCEPTION 'uk_ont_ext_resource唯一索引未创建';
  END IF;
END $$;
