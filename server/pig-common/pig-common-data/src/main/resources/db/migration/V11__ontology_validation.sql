-- ----------------------------
-- youming ontology validation engine
-- 依据：GB/T 48000.3—2026 第5.3条（形式化要求）、第8章（公理与规则）
-- 前置：V4~V10（单位字典、命名空间、实体类型、数据属性、对象属性、公理规则、实体实例）
-- ----------------------------

-- ----------------------------
-- 1. 校验报告主表 ont_validation_report
-- ----------------------------
CREATE TABLE ont_validation_report (
  id bigint NOT NULL,
  ontology_id bigint NOT NULL,
  conforms boolean DEFAULT NULL,
  total_count integer NOT NULL DEFAULT 0,
  violation_count integer NOT NULL DEFAULT 0,
  warning_count integer NOT NULL DEFAULT 0,
  info_count integer NOT NULL DEFAULT 0,
  rule_count integer NOT NULL DEFAULT 0,
  instance_count integer NOT NULL DEFAULT 0,
  duration_ms bigint DEFAULT NULL,
  scope varchar(16) NOT NULL DEFAULT 'FULL',
  target_instance_id bigint DEFAULT NULL,
  status varchar(16) NOT NULL DEFAULT 'RUNNING',
  triggered_by varchar(64) DEFAULT NULL,
  triggered_at timestamp NOT NULL DEFAULT now(),
  completed_at timestamp DEFAULT NULL,
  error_message text DEFAULT NULL,
  result_json jsonb DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

ALTER TABLE ont_validation_report
  ADD CONSTRAINT fk_ont_val_report_ontology FOREIGN KEY (ontology_id)
    REFERENCES ont_ontology_project (id) ON DELETE CASCADE;

ALTER TABLE ont_validation_report
  ADD CONSTRAINT fk_ont_val_report_instance FOREIGN KEY (target_instance_id)
    REFERENCES ont_entity_instance (id) ON DELETE SET NULL;

ALTER TABLE ont_validation_report
  ADD CONSTRAINT ck_ont_val_report_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'));

ALTER TABLE ont_validation_report
  ADD CONSTRAINT ck_ont_val_report_scope CHECK (scope IN ('FULL', 'INSTANCE'));

ALTER TABLE ont_validation_report
  ADD CONSTRAINT ck_ont_val_report_instance_scope
    CHECK ((scope = 'INSTANCE' AND target_instance_id IS NOT NULL) OR scope = 'FULL');

ALTER TABLE ont_validation_report
  ADD CONSTRAINT ck_ont_val_report_conforms
    CHECK ((status = 'COMPLETED' AND conforms IS NOT NULL) OR status IN ('RUNNING', 'FAILED'));

CREATE INDEX idx_ont_val_report_ontology ON ont_validation_report (ontology_id, triggered_at DESC) WHERE del_flag = '0';
CREATE INDEX idx_ont_val_report_status ON ont_validation_report (status) WHERE status = 'RUNNING' AND del_flag = '0';

COMMENT ON TABLE ont_validation_report IS '本体建模-校验报告主表';
COMMENT ON COLUMN ont_validation_report.id IS '报告ID';
COMMENT ON COLUMN ont_validation_report.ontology_id IS '本体工程ID';
COMMENT ON COLUMN ont_validation_report.conforms IS '是否全部通过（0 VIOLATION）';
COMMENT ON COLUMN ont_validation_report.total_count IS '总结果数';
COMMENT ON COLUMN ont_validation_report.violation_count IS '违规数';
COMMENT ON COLUMN ont_validation_report.warning_count IS '警告数';
COMMENT ON COLUMN ont_validation_report.info_count IS '信息数';
COMMENT ON COLUMN ont_validation_report.rule_count IS '执行规则数';
COMMENT ON COLUMN ont_validation_report.instance_count IS '校验实例数';
COMMENT ON COLUMN ont_validation_report.duration_ms IS '校验耗时（毫秒）';
COMMENT ON COLUMN ont_validation_report.scope IS '校验范围：FULL/INSTANCE';
COMMENT ON COLUMN ont_validation_report.target_instance_id IS '增量校验目标实例ID';
COMMENT ON COLUMN ont_validation_report.status IS '状态：RUNNING/COMPLETED/FAILED';
COMMENT ON COLUMN ont_validation_report.triggered_by IS '触发人';
COMMENT ON COLUMN ont_validation_report.triggered_at IS '触发时间';
COMMENT ON COLUMN ont_validation_report.completed_at IS '完成时间';
COMMENT ON COLUMN ont_validation_report.error_message IS 'FAILED时的错误信息';
COMMENT ON COLUMN ont_validation_report.result_json IS '完整JSON快照';
COMMENT ON COLUMN ont_validation_report.create_by IS '创建人';
COMMENT ON COLUMN ont_validation_report.create_time IS '创建时间';
COMMENT ON COLUMN ont_validation_report.update_by IS '修改人';
COMMENT ON COLUMN ont_validation_report.update_time IS '修改时间';
COMMENT ON COLUMN ont_validation_report.del_flag IS '删除标记,1:已删除,0:正常';

-- ----------------------------
-- 2. 校验结果明细表 ont_validation_result
-- ----------------------------
CREATE TABLE ont_validation_result (
  id bigint NOT NULL,
  report_id bigint NOT NULL,
  severity varchar(16) NOT NULL,
  focus_node text DEFAULT NULL,
  result_path text DEFAULT NULL,
  rule_name varchar(255) DEFAULT NULL,
  rule_code varchar(128) DEFAULT NULL,
  message text NOT NULL,
  expected_value text DEFAULT NULL,
  actual_value text DEFAULT NULL,
  suggestion text DEFAULT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

ALTER TABLE ont_validation_result
  ADD CONSTRAINT fk_ont_val_result_report FOREIGN KEY (report_id)
    REFERENCES ont_validation_report (id) ON DELETE CASCADE;

ALTER TABLE ont_validation_result
  ADD CONSTRAINT ck_ont_val_result_severity CHECK (severity IN ('VIOLATION', 'WARNING', 'INFO'));

CREATE INDEX idx_ont_val_result_report_severity ON ont_validation_result (report_id, severity, sort_order) WHERE del_flag = '0';
CREATE INDEX idx_ont_val_result_focus_node ON ont_validation_result (focus_node) WHERE del_flag = '0';
CREATE INDEX idx_ont_val_result_rule_code ON ont_validation_result (rule_code) WHERE del_flag = '0';

COMMENT ON TABLE ont_validation_result IS '本体建模-校验结果明细';
COMMENT ON COLUMN ont_validation_result.id IS '结果ID';
COMMENT ON COLUMN ont_validation_result.report_id IS '校验报告ID';
COMMENT ON COLUMN ont_validation_result.severity IS '严重程度：VIOLATION/WARNING/INFO';
COMMENT ON COLUMN ont_validation_result.focus_node IS '违规实例IRI';
COMMENT ON COLUMN ont_validation_result.result_path IS '违规属性IRI';
COMMENT ON COLUMN ont_validation_result.rule_name IS '违反规则名称';
COMMENT ON COLUMN ont_validation_result.rule_code IS '违反规则编码';
COMMENT ON COLUMN ont_validation_result.message IS '说明';
COMMENT ON COLUMN ont_validation_result.expected_value IS '期望值';
COMMENT ON COLUMN ont_validation_result.actual_value IS '实际值';
COMMENT ON COLUMN ont_validation_result.suggestion IS '修复建议';
COMMENT ON COLUMN ont_validation_result.sort_order IS '排序';
COMMENT ON COLUMN ont_validation_result.create_by IS '创建人';
COMMENT ON COLUMN ont_validation_result.create_time IS '创建时间';
COMMENT ON COLUMN ont_validation_result.update_by IS '修改人';
COMMENT ON COLUMN ont_validation_result.update_time IS '修改时间';
COMMENT ON COLUMN ont_validation_result.del_flag IS '删除标记,1:已删除,0:正常';

-- ----------------------------
-- 3. 菜单：本体建模 / 校验中心
-- ----------------------------
INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES (900800, '校验中心', NULL, '/ontology/validation/index', NULL, 900000, 'iconfont icon-shujujiegou', '1', 8, '0', NULL, '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES
  (900801, '校验报告查看', 'ontology_validation_view', NULL, NULL, 900800, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900802, '执行校验', 'ontology_validation_run', NULL, NULL, 900800, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900803, '查看报告', 'ontology_validation_report', NULL, NULL, 900800, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
  (1, 900800), (1, 900801), (1, 900802), (1, 900803)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ----------------------------
-- 4. 附录D全量校验验收种子（预期全部通过，0 VIOLATION）
-- ----------------------------
INSERT INTO ont_validation_report
  (id, ontology_id, conforms, total_count, violation_count, warning_count, info_count,
   rule_count, instance_count, duration_ms, scope, target_instance_id, status,
   triggered_by, triggered_at, completed_at, error_message, result_json,
   create_by, create_time, update_by, update_time, del_flag)
VALUES
  (990001, 935001, TRUE, 0, 0, 0, 0,
   11, 29, 0, 'FULL', NULL, 'COMPLETED',
   'system', '2026-07-12 10:00:00', '2026-07-12 10:00:00', NULL,
   '{"conforms": true, "violationCount": 0, "note": "附录D全量校验验收种子"}'::jsonb,
   'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 5. 迁移完整性断言
-- ----------------------------
DO $$
BEGIN
  -- 1. 校验报告表存在
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_validation_report') THEN
    RAISE EXCEPTION 'ont_validation_report表未创建';
  END IF;

  -- 2. 校验结果表存在
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_validation_result') THEN
    RAISE EXCEPTION 'ont_validation_result表未创建';
  END IF;

  -- 3. 附录D验收报告存在且校验通过
  IF (SELECT count(*) FROM ont_validation_report WHERE id = 990001 AND conforms = TRUE AND del_flag = '0') <> 1 THEN
    RAISE EXCEPTION '附录D验收报告990001应存在且校验通过';
  END IF;

  -- 4. 附录D验收报告无违规结果
  IF (SELECT count(*) FROM ont_validation_result WHERE report_id = 990001 AND del_flag = '0') <> 0 THEN
    RAISE EXCEPTION '附录D验收报告990001不应有违规结果';
  END IF;

  -- 5. 菜单900800~900803完整
  IF (SELECT count(*) FROM sys_menu WHERE menu_id BETWEEN 900800 AND 900803 AND del_flag = '0') <> 4 THEN
    RAISE EXCEPTION '校验中心菜单900800~900803不完整';
  END IF;

  -- 6. 管理员授权完整
  IF (SELECT count(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 900800 AND 900803) <> 4 THEN
    RAISE EXCEPTION '校验中心管理员授权不完整';
  END IF;

  -- 7. CHECK约束存在
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.check_constraints
    WHERE constraint_name = 'ck_ont_val_report_status'
  ) THEN
    RAISE EXCEPTION 'ck_ont_val_report_status约束未创建';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.check_constraints
    WHERE constraint_name = 'ck_ont_val_result_severity'
  ) THEN
    RAISE EXCEPTION 'ck_ont_val_result_severity约束未创建';
  END IF;
END $$;
