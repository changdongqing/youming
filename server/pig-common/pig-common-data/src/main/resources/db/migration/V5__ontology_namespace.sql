-- ----------------------------
-- youming ontology namespace & IRI management
-- ----------------------------

CREATE TABLE ont_namespace (
  id bigint NOT NULL,
  prefix varchar(64) NOT NULL,
  uri varchar(512) NOT NULL,
  is_default char(1) NOT NULL DEFAULT '0',
  is_builtin char(1) NOT NULL DEFAULT '0',
  sort_order integer NOT NULL DEFAULT 0,
  description varchar(255) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (id)
);

COMMENT ON TABLE ont_namespace IS '本体建模-命名空间表';
COMMENT ON COLUMN ont_namespace.id IS '命名空间ID';
COMMENT ON COLUMN ont_namespace.prefix IS '前缀标识';
COMMENT ON COLUMN ont_namespace.uri IS '命名空间URI';
COMMENT ON COLUMN ont_namespace.is_default IS '是否默认，1是0否';
COMMENT ON COLUMN ont_namespace.is_builtin IS '是否内置，1是0否';
COMMENT ON COLUMN ont_namespace.sort_order IS '排序值';
COMMENT ON COLUMN ont_namespace.description IS '描述';
COMMENT ON COLUMN ont_namespace.create_by IS '创建人';
COMMENT ON COLUMN ont_namespace.create_time IS '创建时间';
COMMENT ON COLUMN ont_namespace.update_by IS '修改人';
COMMENT ON COLUMN ont_namespace.update_time IS '更新时间';
COMMENT ON COLUMN ont_namespace.del_flag IS '删除标志，0未删除，1已删除';

CREATE UNIQUE INDEX uk_ont_namespace_prefix ON ont_namespace (prefix) WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_namespace_uri ON ont_namespace (uri) WHERE del_flag = '0';

-- ----------------------------
-- ont_unit 新增 namespace_id 外键列（同步迁移，旧 namespace varchar 列保留过渡）
-- ----------------------------
ALTER TABLE ont_unit ADD COLUMN IF NOT EXISTS namespace_id bigint;
COMMENT ON COLUMN ont_unit.namespace_id IS '命名空间ID，引用ont_namespace.id';
ALTER TABLE ont_unit ADD CONSTRAINT fk_ont_unit_namespace FOREIGN KEY (namespace_id) REFERENCES ont_namespace (id);
CREATE INDEX IF NOT EXISTS idx_ont_unit_namespace_id ON ont_unit (namespace_id) WHERE del_flag = '0';

-- ----------------------------
-- 菜单：本体建模 / 命名空间管理（复用 900000 顶级目录）
-- ----------------------------
INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES (900200, '命名空间管理', NULL, '/ontology/namespace/index', NULL, 900000, 'iconfont icon-canshupeizhi', '1', 2, '0', NULL, '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES
  (900201, '命名空间查看', 'ontology_namespace_view', NULL, NULL, 900200, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900202, '命名空间新增', 'ontology_namespace_add', NULL, NULL, 900200, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900203, '命名空间修改', 'ontology_namespace_edit', NULL, NULL, 900200, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900204, '命名空间删除', 'ontology_namespace_del', NULL, NULL, 900200, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
  (1, 900200),
  (1, 900201),
  (1, 900202),
  (1, 900203),
  (1, 900204)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ----------------------------
-- 内置命名空间（核心本体 + W3C 标准前缀）
-- ----------------------------
INSERT INTO ont_namespace (id, prefix, uri, is_default, is_builtin, sort_order, description, create_by, create_time, update_by, update_time, del_flag)
VALUES
  (930001, 'std', 'http://example.org/standard-ontology#', '1', '1', 10, '核心本体命名空间（默认），正式发布后由管理机构更新域名', 'admin', now(), 'admin', now(), '0'),
  (930002, 'rdf', 'http://www.w3.org/1999/02/22-rdf-syntax-ns#', '0', '1', 20, 'W3C RDF 前缀', 'admin', now(), 'admin', now(), '0'),
  (930003, 'rdfs', 'http://www.w3.org/2000/01/rdf-schema#', '0', '1', 30, 'W3C RDFS 前缀', 'admin', now(), 'admin', now(), '0'),
  (930004, 'owl', 'http://www.w3.org/2002/07/owl#', '0', '1', 40, 'W3C OWL 前缀', 'admin', now(), 'admin', now(), '0'),
  (930005, 'xsd', 'http://www.w3.org/2001/XMLSchema#', '0', '1', 50, 'W3C XML Schema 前缀', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;
