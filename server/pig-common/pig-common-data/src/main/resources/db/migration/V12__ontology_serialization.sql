-- ----------------------------
-- youming ontology serialization & exchange
-- 依据：GB/T 48000.3—2026 第5.3条（形式化要求——OWL/RDF/标准化序列化格式）
-- 前置：V4~V11（单位字典、命名空间、实体类型、数据属性、对象属性、公理规则、实体实例、校验引擎）
-- ----------------------------

-- ----------------------------
-- 1. 序列化审计日志表 ont_serialization_log
-- ----------------------------
CREATE TABLE ont_serialization_log (
  id bigint NOT NULL,
  ontology_id bigint NOT NULL,
  operation_type varchar(16) NOT NULL,
  rdf_format varchar(16) NOT NULL,
  export_scope varchar(32) DEFAULT NULL,
  predicate_strategy varchar(32) DEFAULT NULL,
  target_type_id bigint DEFAULT NULL,
  triple_count integer NOT NULL DEFAULT 0,
  content_size bigint NOT NULL DEFAULT 0,
  instance_count integer NOT NULL DEFAULT 0,
  data_value_count integer NOT NULL DEFAULT 0,
  object_relation_count integer NOT NULL DEFAULT 0,
  skipped_count integer NOT NULL DEFAULT 0,
  failed_count integer NOT NULL DEFAULT 0,
  precheck_passed char(1) DEFAULT NULL,
  validation_report_id bigint DEFAULT NULL,
  force_flag char(1) NOT NULL DEFAULT '0',
  import_iri_merge_mode varchar(16) DEFAULT NULL,
  duration_ms bigint DEFAULT NULL,
  error_message text DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

ALTER TABLE ont_serialization_log
  ADD CONSTRAINT fk_ont_serial_log_ontology FOREIGN KEY (ontology_id)
    REFERENCES ont_ontology_project (id) ON DELETE CASCADE;

ALTER TABLE ont_serialization_log
  ADD CONSTRAINT ck_ont_serial_log_op_type CHECK (operation_type IN ('EXPORT', 'IMPORT'));

ALTER TABLE ont_serialization_log
  ADD CONSTRAINT ck_ont_serial_log_format CHECK (rdf_format IN ('TURTLE', 'JSON-LD', 'RDF-XML', 'N-TRIPLES'));

ALTER TABLE ont_serialization_log
  ADD CONSTRAINT ck_ont_serial_log_scope CHECK (export_scope IS NULL OR export_scope IN ('FULL', 'SCHEMA_ONLY', 'INSTANCE_ONLY', 'INSTANCE_SUBTREE'));

ALTER TABLE ont_serialization_log
  ADD CONSTRAINT ck_ont_serial_log_strategy CHECK (predicate_strategy IS NULL OR predicate_strategy IN ('PREFERRED_ALIAS', 'STANDARD_IRI', 'INTERNAL_IRI'));

ALTER TABLE ont_serialization_log
  ADD CONSTRAINT ck_ont_serial_log_merge CHECK (import_iri_merge_mode IS NULL OR import_iri_merge_mode IN ('SKIP', 'MERGE', 'OVERWRITE'));

CREATE INDEX idx_ont_serial_log_ontology ON ont_serialization_log (ontology_id, create_time DESC) WHERE del_flag = '0';
CREATE INDEX idx_ont_serial_log_type ON ont_serialization_log (operation_type, create_time DESC) WHERE del_flag = '0';

COMMENT ON TABLE ont_serialization_log IS '本体建模-序列化与交换审计日志';
COMMENT ON COLUMN ont_serialization_log.id IS '日志ID';
COMMENT ON COLUMN ont_serialization_log.ontology_id IS '本体工程ID';
COMMENT ON COLUMN ont_serialization_log.operation_type IS '操作类型: EXPORT/IMPORT';
COMMENT ON COLUMN ont_serialization_log.rdf_format IS 'RDF格式: TURTLE/JSON-LD/RDF-XML/N-TRIPLES';
COMMENT ON COLUMN ont_serialization_log.export_scope IS '导出范围（仅EXPORT）';
COMMENT ON COLUMN ont_serialization_log.predicate_strategy IS '谓词IRI策略（仅EXPORT）';
COMMENT ON COLUMN ont_serialization_log.target_type_id IS '子树过滤目标类型ID（仅EXPORT INSTANCE_SUBTREE）';
COMMENT ON COLUMN ont_serialization_log.triple_count IS '导出/导入的三元组数';
COMMENT ON COLUMN ont_serialization_log.content_size IS '文件大小（字节）';
COMMENT ON COLUMN ont_serialization_log.instance_count IS '涉及的实例数';
COMMENT ON COLUMN ont_serialization_log.data_value_count IS '涉及的数据属性值数';
COMMENT ON COLUMN ont_serialization_log.object_relation_count IS '涉及的对象关系数';
COMMENT ON COLUMN ont_serialization_log.skipped_count IS '跳过数（导入SKIP模式）';
COMMENT ON COLUMN ont_serialization_log.failed_count IS '失败数';
COMMENT ON COLUMN ont_serialization_log.precheck_passed IS '导出前置校验是否通过（仅EXPORT）';
COMMENT ON COLUMN ont_serialization_log.validation_report_id IS '关联的校验报告ID';
COMMENT ON COLUMN ont_serialization_log.force_flag IS '是否强制导出，1是0否';
COMMENT ON COLUMN ont_serialization_log.import_iri_merge_mode IS '导入IRI合并模式（仅IMPORT）';
COMMENT ON COLUMN ont_serialization_log.duration_ms IS '耗时（毫秒）';
COMMENT ON COLUMN ont_serialization_log.error_message IS '错误信息';
COMMENT ON COLUMN ont_serialization_log.create_by IS '创建人';
COMMENT ON COLUMN ont_serialization_log.create_time IS '创建时间';
COMMENT ON COLUMN ont_serialization_log.update_by IS '修改人';
COMMENT ON COLUMN ont_serialization_log.update_time IS '更新时间';
COMMENT ON COLUMN ont_serialization_log.del_flag IS '删除标记,1:已删除,0:正常';

-- ----------------------------
-- 2. 菜单：本体建模 / 导入导出
-- ----------------------------
INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES (900900, '导入导出', NULL, '/ontology/serialization/index', NULL, 900000, 'ele-Download', '1', 9, '0', NULL, '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES
  (900901, '导出查看', 'ontology_export_view', NULL, NULL, 900900, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900902, '强制导出', 'ontology_export_force', NULL, NULL, 900900, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900903, '导入查看', 'ontology_import_view', NULL, NULL, 900900, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900904, '导入写入', 'ontology_import_add', NULL, NULL, 900900, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
  (1, 900900), (1, 900901), (1, 900902), (1, 900903), (1, 900904)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ----------------------------
-- 3. 迁移完整性断言
-- ----------------------------
DO $$
BEGIN
  -- 1. 审计日志表存在
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_serialization_log') THEN
    RAISE EXCEPTION 'ont_serialization_log表未创建';
  END IF;

  -- 2. 菜单900900~900904完整
  IF (SELECT count(*) FROM sys_menu WHERE menu_id BETWEEN 900900 AND 900904 AND del_flag = '0') <> 5 THEN
    RAISE EXCEPTION '导入导出菜单900900~900904不完整';
  END IF;

  -- 3. 管理员授权完整
  IF (SELECT count(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 900900 AND 900904) <> 5 THEN
    RAISE EXCEPTION '导入导出管理员授权不完整';
  END IF;

  -- 4. CHECK约束存在
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.check_constraints
    WHERE constraint_name = 'ck_ont_serial_log_op_type'
  ) THEN
    RAISE EXCEPTION 'ck_ont_serial_log_op_type约束未创建';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.check_constraints
    WHERE constraint_name = 'ck_ont_serial_log_format'
  ) THEN
    RAISE EXCEPTION 'ck_ont_serial_log_format约束未创建';
  END IF;
END $$;
