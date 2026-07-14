-- =============================================
-- V18: 本体版本演化模块
-- 设计文档: 14-本体版本演化模块详细设计.md
-- 工作区 + 不可变发布快照方案。
-- 不插入伪基线快照，首次基线由应用服务发布。
-- =============================================

-- ----------------------------
-- 1. 扩展 ont_ontology_project
-- ----------------------------
ALTER TABLE ont_ontology_project
  ADD COLUMN IF NOT EXISTS ontology_iri varchar(512),
  ADD COLUMN IF NOT EXISTS version_iri_base varchar(512),
  ADD COLUMN IF NOT EXISTS current_version_id bigint,
  ADD COLUMN IF NOT EXISTS workspace_status varchar(16) NOT NULL DEFAULT 'EDITABLE',
  ADD COLUMN IF NOT EXISTS workspace_revision bigint NOT NULL DEFAULT 0;

ALTER TABLE ont_ontology_project
  ADD CONSTRAINT ck_ont_project_workspace_status
  CHECK (workspace_status IN ('EDITABLE', 'PREPARED', 'MIGRATING'));

CREATE UNIQUE INDEX IF NOT EXISTS uk_ont_project_ontology_iri
  ON ont_ontology_project (ontology_iri)
  WHERE del_flag = '0' AND ontology_iri IS NOT NULL;

COMMENT ON COLUMN ont_ontology_project.ontology_iri IS '本体标识IRI，如 http://example.org/standard-ontology';
COMMENT ON COLUMN ont_ontology_project.version_iri_base IS '版本IRI基础路径，如 http://example.org/standard-ontology/version/';
COMMENT ON COLUMN ont_ontology_project.current_version_id IS '当前已发布版本ID，引用ont_ontology_version.id，初始为空';
COMMENT ON COLUMN ont_ontology_project.workspace_status IS '工作区状态：EDITABLE/PREPARED/MIGRATING';
COMMENT ON COLUMN ont_ontology_project.workspace_revision IS '工作区修订号，每次语义写入递增，用于乐观锁校验';

-- ----------------------------
-- 2. 本体版本表 ont_ontology_version
-- ----------------------------
CREATE TABLE ont_ontology_version (
  id bigint NOT NULL,
  ontology_id bigint NOT NULL,
  version_number varchar(32) NOT NULL,
  version_iri varchar(512) NOT NULL,
  prior_version_id bigint DEFAULT NULL,
  restore_source_version_id bigint DEFAULT NULL,
  compatibility varchar(32) NOT NULL,
  release_status varchar(16) NOT NULL DEFAULT 'PREPARED',
  release_notes text DEFAULT NULL,
  snapshot_format_version integer NOT NULL DEFAULT 1,
  schema_snapshot jsonb NOT NULL,
  snapshot_hash varchar(64) NOT NULL,
  diff_summary jsonb NOT NULL,
  migration_plan jsonb DEFAULT NULL,
  validation_report_id bigint DEFAULT NULL,
  workspace_revision bigint NOT NULL,
  published_by varchar(64) DEFAULT NULL,
  published_at timestamp DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_version_project
    FOREIGN KEY (ontology_id) REFERENCES ont_ontology_project(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_version_prior
    FOREIGN KEY (prior_version_id) REFERENCES ont_ontology_version(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_version_restore_source
    FOREIGN KEY (restore_source_version_id) REFERENCES ont_ontology_version(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_version_validation
    FOREIGN KEY (validation_report_id) REFERENCES ont_validation_report(id) ON DELETE SET NULL,
  CONSTRAINT ck_ont_version_compatibility
    CHECK (compatibility IN ('PATCH_ONLY', 'BACKWARD_COMPATIBLE', 'BREAKING')),
  CONSTRAINT ck_ont_version_status
    CHECK (release_status IN ('PREPARED', 'MIGRATING', 'PUBLISHED', 'FAILED', 'CANCELLED')),
  CONSTRAINT ck_ont_version_del_flag CHECK (del_flag IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_ont_version_number
  ON ont_ontology_version (ontology_id, version_number)
  WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_version_iri
  ON ont_ontology_version (version_iri)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_version_project_time
  ON ont_ontology_version (ontology_id, create_time DESC)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_version_prior
  ON ont_ontology_version (prior_version_id)
  WHERE del_flag = '0' AND prior_version_id IS NOT NULL;
CREATE INDEX idx_ont_version_restore
  ON ont_ontology_version (restore_source_version_id)
  WHERE del_flag = '0' AND restore_source_version_id IS NOT NULL;

COMMENT ON TABLE ont_ontology_version IS '本体版本表，存储不可变Schema快照及发布元数据';
COMMENT ON COLUMN ont_ontology_version.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN ont_ontology_version.ontology_id IS '本体工程ID';
COMMENT ON COLUMN ont_ontology_version.version_number IS '语义版本号 MAJOR.MINOR.PATCH';
COMMENT ON COLUMN ont_ontology_version.version_iri IS '版本IRI，version_iri_base+version_number';
COMMENT ON COLUMN ont_ontology_version.prior_version_id IS '前序版本ID（自引用）';
COMMENT ON COLUMN ont_ontology_version.restore_source_version_id IS '恢复来源版本ID，指向被恢复的历史版本';
COMMENT ON COLUMN ont_ontology_version.compatibility IS '兼容性：PATCH_ONLY/BACKWARD_COMPATIBLE/BREAKING';
COMMENT ON COLUMN ont_ontology_version.release_status IS '发布状态：PREPARED/MIGRATING/PUBLISHED/FAILED/CANCELLED';
COMMENT ON COLUMN ont_ontology_version.release_notes IS '发布说明';
COMMENT ON COLUMN ont_ontology_version.snapshot_format_version IS '快照格式版本，当前为1';
COMMENT ON COLUMN ont_ontology_version.schema_snapshot IS '规范化全量Schema快照JSONB';
COMMENT ON COLUMN ont_ontology_version.snapshot_hash IS '快照SHA-256哈希';
COMMENT ON COLUMN ont_ontology_version.diff_summary IS '与priorVersion的差异摘要JSONB';
COMMENT ON COLUMN ont_ontology_version.migration_plan IS '实例迁移计划JSONB，BREAKING时必填';
COMMENT ON COLUMN ont_ontology_version.validation_report_id IS '关联校验报告ID';
COMMENT ON COLUMN ont_ontology_version.workspace_revision IS 'prepare时记录的工作区修订号，用于激活时乐观锁校验';
COMMENT ON COLUMN ont_ontology_version.published_by IS '发布人';
COMMENT ON COLUMN ont_ontology_version.published_at IS '发布时间';
COMMENT ON COLUMN ont_ontology_version.del_flag IS '删除标志，0未删除，1已删除';

-- ----------------------------
-- 3. 实例迁移作业表 ont_instance_migration_job
-- ----------------------------
CREATE TABLE ont_instance_migration_job (
  id bigint NOT NULL,
  ontology_id bigint NOT NULL,
  candidate_version_id bigint NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'PENDING',
  plan_snapshot jsonb NOT NULL,
  cursor_data jsonb DEFAULT NULL,
  total_count bigint NOT NULL DEFAULT 0,
  processed_count bigint NOT NULL DEFAULT 0,
  success_count bigint NOT NULL DEFAULT 0,
  failed_count bigint NOT NULL DEFAULT 0,
  error_summary text DEFAULT NULL,
  started_at timestamp DEFAULT NULL,
  completed_at timestamp DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_migration_project
    FOREIGN KEY (ontology_id) REFERENCES ont_ontology_project(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_migration_candidate
    FOREIGN KEY (candidate_version_id) REFERENCES ont_ontology_version(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_migration_status
    CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'PARTIAL_FAILED', 'FAILED', 'CANCELLED')),
  CONSTRAINT ck_ont_migration_del_flag CHECK (del_flag IN ('0', '1'))
);

CREATE INDEX idx_ont_migration_candidate
  ON ont_instance_migration_job (candidate_version_id, create_time DESC)
  WHERE del_flag = '0';

COMMENT ON TABLE ont_instance_migration_job IS '实例迁移作业表，记录BREAKING版本发布时的实例迁移执行';
COMMENT ON COLUMN ont_instance_migration_job.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN ont_instance_migration_job.ontology_id IS '本体工程ID';
COMMENT ON COLUMN ont_instance_migration_job.candidate_version_id IS '候选版本ID';
COMMENT ON COLUMN ont_instance_migration_job.status IS '作业状态：PENDING/RUNNING/SUCCEEDED/PARTIAL_FAILED/FAILED/CANCELLED';
COMMENT ON COLUMN ont_instance_migration_job.plan_snapshot IS '迁移计划快照JSONB';
COMMENT ON COLUMN ont_instance_migration_job.cursor_data IS '游标数据JSONB，记录上次处理位置用于断点恢复';
COMMENT ON COLUMN ont_instance_migration_job.total_count IS '待迁移实例总数';
COMMENT ON COLUMN ont_instance_migration_job.processed_count IS '已处理实例数';
COMMENT ON COLUMN ont_instance_migration_job.success_count IS '成功实例数';
COMMENT ON COLUMN ont_instance_migration_job.failed_count IS '失败实例数';
COMMENT ON COLUMN ont_instance_migration_job.error_summary IS '错误摘要文本';
COMMENT ON COLUMN ont_instance_migration_job.started_at IS '开始执行时间';
COMMENT ON COLUMN ont_instance_migration_job.completed_at IS '完成时间';
COMMENT ON COLUMN ont_instance_migration_job.del_flag IS '删除标志，0未删除，1已删除';

-- ----------------------------
-- 4. ont_ontology_project.current_version_id 外键（建表后添加，避免循环依赖）
-- ----------------------------
ALTER TABLE ont_ontology_project
  ADD CONSTRAINT fk_ont_project_current_version
  FOREIGN KEY (current_version_id)
  REFERENCES ont_ontology_version(id)
  ON DELETE RESTRICT;

-- ----------------------------
-- 5. 菜单：本体建模 / 版本管理
-- ----------------------------
INSERT INTO sys_menu
(menu_id, name, permission, path, component, parent_id, icon, visible,
 sort_order, keep_alive, embedded, menu_type,
 create_by, create_time, update_by, update_time, del_flag)
VALUES
(901300, '版本管理', NULL, '/ontology/version/index', NULL, 900000,
 'ele-Clock', '1', 13, '0', NULL, '0',
 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

-- ----------------------------
-- 6. 按钮权限：查看 / 发布 / 恢复
-- ----------------------------
INSERT INTO sys_menu
(menu_id, name, permission, path, component, parent_id, icon, visible,
 sort_order, keep_alive, embedded, menu_type,
 create_by, create_time, update_by, update_time, del_flag)
VALUES
(901301, '版本查看', 'ontology_version_view', NULL, NULL, 901300,
 NULL, '1', 1, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0'),
(901302, '版本发布', 'ontology_version_publish', NULL, NULL, 901300,
 NULL, '1', 2, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0'),
(901303, '版本恢复', 'ontology_version_restore', NULL, NULL, 901300,
 NULL, '1', 3, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

-- ----------------------------
-- 7. 角色-菜单映射（仅管理员角色1）
-- ----------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
  (1, 901300), (1, 901301), (1, 901302), (1, 901303)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ----------------------------
-- 8. 迁移完整性断言
-- ----------------------------
DO $$
BEGIN
  -- 1. 版本表存在
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_ontology_version') THEN
    RAISE EXCEPTION '版本表 ont_ontology_version 未创建';
  END IF;

  -- 2. 迁移作业表存在
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_instance_migration_job') THEN
    RAISE EXCEPTION '迁移作业表 ont_instance_migration_job 未创建';
  END IF;

  -- 3. 本体工程扩展列存在
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'ont_ontology_project' AND column_name = 'current_version_id') THEN
    RAISE EXCEPTION 'ont_ontology_project 缺少 current_version_id 列';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'ont_ontology_project' AND column_name = 'workspace_status') THEN
    RAISE EXCEPTION 'ont_ontology_project 缺少 workspace_status 列';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'ont_ontology_project' AND column_name = 'workspace_revision') THEN
    RAISE EXCEPTION 'ont_ontology_project 缺少 workspace_revision 列';
  END IF;

  -- 4. 版本管理菜单存在
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901300 AND del_flag = '0') THEN
    RAISE EXCEPTION '版本管理菜单(901300)未创建';
  END IF;

  -- 5. 按钮权限存在
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901301 AND permission = 'ontology_version_view' AND del_flag = '0') THEN
    RAISE EXCEPTION '版本查看权限(901301)未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901302 AND permission = 'ontology_version_publish' AND del_flag = '0') THEN
    RAISE EXCEPTION '版本发布权限(901302)未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901303 AND permission = 'ontology_version_restore' AND del_flag = '0') THEN
    RAISE EXCEPTION '版本恢复权限(901303)未创建';
  END IF;

  -- 6. 角色-菜单映射存在
  IF (SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 901300 AND 901303) != 4 THEN
    RAISE EXCEPTION '管理员角色未完整分配版本管理菜单';
  END IF;

  RAISE NOTICE 'V18 本体版本演化模块迁移完成';
END $$;
