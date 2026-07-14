-- ----------------------------
-- youming ontology data source mapping (module 18)
-- 依据：18-00 总体架构 §7.4 建表顺序
-- 前置：V1~V21（含 V10 实例表、V18 版本表、V19 EDA Outbox、V20 安全合规）
-- 本脚本在单个迁移中按依赖顺序创建全部 15 张新表，改造现有表，并写入菜单/权限种子。
-- 后续阶段只补代码，不修改已应用的 V22；若发现缺列须新增 V23/V24。
-- ----------------------------

-- ============================================================
-- 步骤1：无外键依赖的独立表
-- ============================================================

-- ----------------------------
-- 1.1 数据源表 ont_data_source (18-02 §5)
-- ----------------------------
CREATE TABLE ont_data_source (
  id bigint NOT NULL,
  source_code varchar(64) NOT NULL,
  source_name varchar(128) NOT NULL,
  source_type varchar(16) NOT NULL DEFAULT 'JDBC',
  database_type varchar(32) NOT NULL DEFAULT 'POSTGRESQL',
  connection_mode varchar(16) NOT NULL DEFAULT 'HOST',
  connection_config jsonb NOT NULL,
  credential_ciphertext bytea NOT NULL,
  credential_iv bytea NOT NULL,
  credential_key_id varchar(64) NOT NULL,
  allowed_schemas jsonb NOT NULL DEFAULT '[]'::jsonb,
  allowed_objects jsonb NOT NULL DEFAULT '[]'::jsonb,
  status varchar(16) NOT NULL DEFAULT 'DRAFT',
  revision bigint NOT NULL DEFAULT 0,
  last_test_status varchar(16) DEFAULT NULL,
  last_test_revision bigint DEFAULT NULL,
  last_test_at timestamp DEFAULT NULL,
  last_test_latency_ms bigint DEFAULT NULL,
  last_test_error_code varchar(64) DEFAULT NULL,
  metadata_refreshed_at timestamp DEFAULT NULL,
  security_level_code varchar(32) NOT NULL DEFAULT 'RESTRICTED',
  remarks varchar(255) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_ds_security_level FOREIGN KEY (security_level_code)
    REFERENCES ont_security_level(level_code) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_ds_source_type CHECK (source_type IN ('JDBC')),
  CONSTRAINT ck_ont_ds_database_type CHECK (database_type IN ('POSTGRESQL')),
  CONSTRAINT ck_ont_ds_connection_mode CHECK (connection_mode IN ('HOST','JDBC_URL')),
  CONSTRAINT ck_ont_ds_status CHECK (status IN ('DRAFT','ACTIVE','DISABLED')),
  CONSTRAINT ck_ont_ds_test CHECK (last_test_status IS NULL OR last_test_status IN ('SUCCESS','FAILED')),
  CONSTRAINT ck_ont_ds_test_revision CHECK (last_test_revision IS NULL OR last_test_revision >= 0),
  CONSTRAINT ck_ont_ds_revision CHECK (revision >= 0),
  CONSTRAINT ck_ont_ds_del CHECK (del_flag IN ('0','1'))
);

COMMENT ON TABLE ont_data_source IS '本体建模-数据源映射-数据源表';
COMMENT ON COLUMN ont_data_source.id IS '数据源ID';
COMMENT ON COLUMN ont_data_source.source_code IS '数据源编码，唯一';
COMMENT ON COLUMN ont_data_source.source_name IS '数据源名称';
COMMENT ON COLUMN ont_data_source.source_type IS '连接器类型：JDBC';
COMMENT ON COLUMN ont_data_source.database_type IS '数据库类型：POSTGRESQL';
COMMENT ON COLUMN ont_data_source.connection_mode IS '连接模式：HOST/JDBC_URL';
COMMENT ON COLUMN ont_data_source.connection_config IS '连接配置JSONB，不含用户名密码';
COMMENT ON COLUMN ont_data_source.credential_ciphertext IS 'AES-GCM密文（含认证标签）';
COMMENT ON COLUMN ont_data_source.credential_iv IS '加密随机IV';
COMMENT ON COLUMN ont_data_source.credential_key_id IS '密钥版本ID';
COMMENT ON COLUMN ont_data_source.allowed_schemas IS 'Schema白名单JSONB数组';
COMMENT ON COLUMN ont_data_source.allowed_objects IS '对象白名单JSONB数组';
COMMENT ON COLUMN ont_data_source.status IS '状态：DRAFT/ACTIVE/DISABLED';
COMMENT ON COLUMN ont_data_source.revision IS '配置修订号，用于乐观锁和连接池缓存';
COMMENT ON COLUMN ont_data_source.last_test_status IS '最近测试结果：SUCCESS/FAILED';
COMMENT ON COLUMN ont_data_source.last_test_revision IS '测试时的配置修订号';
COMMENT ON COLUMN ont_data_source.last_test_at IS '最近测试时间';
COMMENT ON COLUMN ont_data_source.last_test_latency_ms IS '最近测试延迟毫秒';
COMMENT ON COLUMN ont_data_source.last_test_error_code IS '最近测试错误码';
COMMENT ON COLUMN ont_data_source.metadata_refreshed_at IS '最近元数据刷新时间';
COMMENT ON COLUMN ont_data_source.security_level_code IS '安全级别编码，引用ont_security_level.level_code';
COMMENT ON COLUMN ont_data_source.del_flag IS '删除标志，0未删除，1已删除';

CREATE UNIQUE INDEX uk_ont_ds_code
  ON ont_data_source(source_code) WHERE del_flag = '0';
CREATE INDEX idx_ont_ds_status
  ON ont_data_source(status, source_type, database_type) WHERE del_flag = '0';

-- ----------------------------
-- 1.2 映射工程表 ont_mapping_project (18-03 §3) — 不含 active_version_id / last_job_id 外键
-- ----------------------------
CREATE TABLE ont_mapping_project (
  id bigint NOT NULL,
  mapping_code varchar(64) NOT NULL,
  mapping_name varchar(128) NOT NULL,
  ontology_id bigint NOT NULL,
  default_namespace_id bigint NOT NULL,
  active_version_id bigint DEFAULT NULL,
  project_status varchar(16) NOT NULL DEFAULT 'DRAFT',
  description text DEFAULT NULL,
  schedule_enabled char(1) NOT NULL DEFAULT '0',
  schedule_cron varchar(128) DEFAULT NULL,
  schedule_run_type varchar(16) DEFAULT NULL,
  execution_subject_type varchar(16) DEFAULT NULL,
  execution_subject_id bigint DEFAULT NULL,
  security_level_code varchar(32) NOT NULL DEFAULT 'INTERNAL',
  revision bigint NOT NULL DEFAULT 0,
  last_job_id bigint DEFAULT NULL,
  remarks varchar(255) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_map_project_ontology FOREIGN KEY (ontology_id)
    REFERENCES ont_ontology_project(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_map_project_namespace FOREIGN KEY (default_namespace_id)
    REFERENCES ont_namespace(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_map_project_security_level FOREIGN KEY (security_level_code)
    REFERENCES ont_security_level(level_code) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_map_project_status
    CHECK (project_status IN ('DRAFT','ACTIVE','DISABLED','ARCHIVED')),
  CONSTRAINT ck_ont_map_project_schedule
    CHECK (schedule_enabled IN ('0','1')),
  CONSTRAINT ck_ont_map_project_run_type
    CHECK (schedule_run_type IS NULL OR schedule_run_type IN ('FULL','INCREMENTAL')),
  CONSTRAINT ck_ont_map_project_subject_type
    CHECK (execution_subject_type IS NULL OR execution_subject_type IN ('USER','ROLE')),
  CONSTRAINT ck_ont_map_project_revision CHECK (revision >= 0),
  CONSTRAINT ck_ont_map_project_del CHECK (del_flag IN ('0','1'))
);

COMMENT ON TABLE ont_mapping_project IS '本体建模-数据源映射-映射工程表';
COMMENT ON COLUMN ont_mapping_project.id IS '映射工程ID';
COMMENT ON COLUMN ont_mapping_project.mapping_code IS '映射工程编码，唯一';
COMMENT ON COLUMN ont_mapping_project.mapping_name IS '映射工程名称';
COMMENT ON COLUMN ont_mapping_project.ontology_id IS '本体工程ID，引用ont_ontology_project.id';
COMMENT ON COLUMN ont_mapping_project.default_namespace_id IS '默认命名空间ID，引用ont_namespace.id';
COMMENT ON COLUMN ont_mapping_project.active_version_id IS '当前发布版本ID（延迟外键→ont_mapping_version）';
COMMENT ON COLUMN ont_mapping_project.project_status IS '工程状态：DRAFT/ACTIVE/DISABLED/ARCHIVED';
COMMENT ON COLUMN ont_mapping_project.schedule_enabled IS '是否启用调度：0否1是';
COMMENT ON COLUMN ont_mapping_project.schedule_cron IS 'Quartz cron表达式';
COMMENT ON COLUMN ont_mapping_project.schedule_run_type IS '调度运行类型：FULL/INCREMENTAL';
COMMENT ON COLUMN ont_mapping_project.execution_subject_type IS '执行主体类型：USER/ROLE';
COMMENT ON COLUMN ont_mapping_project.execution_subject_id IS '执行主体ID';
COMMENT ON COLUMN ont_mapping_project.security_level_code IS '安全级别编码';
COMMENT ON COLUMN ont_mapping_project.revision IS '配置修订号，乐观锁';
COMMENT ON COLUMN ont_mapping_project.last_job_id IS '最近作业ID（延迟外键→ont_mapping_job）';
COMMENT ON COLUMN ont_mapping_project.del_flag IS '删除标志，0未删除，1已删除';

CREATE UNIQUE INDEX uk_ont_map_project_code
  ON ont_mapping_project(mapping_code) WHERE del_flag = '0';
CREATE INDEX idx_ont_map_project_ontology
  ON ont_mapping_project(ontology_id, project_status) WHERE del_flag = '0';

-- ----------------------------
-- 1.3 映射版本表 ont_mapping_version (18-03 §4) — 不含 validation_report_id 外键
-- ----------------------------
CREATE TABLE ont_mapping_version (
  id bigint NOT NULL,
  mapping_project_id bigint NOT NULL,
  version_number varchar(32) NOT NULL,
  version_status varchar(16) NOT NULL DEFAULT 'DRAFT',
  prior_version_id bigint DEFAULT NULL,
  ontology_version_constraint varchar(128) NOT NULL,
  validated_ontology_version_id bigint DEFAULT NULL,
  validated_workspace_revision bigint DEFAULT NULL,
  config_snapshot jsonb DEFAULT NULL,
  config_hash varchar(64) DEFAULT NULL,
  metadata_dependencies jsonb NOT NULL DEFAULT '[]'::jsonb,
  validation_report_id bigint DEFAULT NULL,
  validation_summary jsonb DEFAULT NULL,
  release_notes text DEFAULT NULL,
  published_by varchar(64) DEFAULT NULL,
  published_at timestamp DEFAULT NULL,
  retired_at timestamp DEFAULT NULL,
  revision bigint NOT NULL DEFAULT 0,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_map_version_project FOREIGN KEY (mapping_project_id)
    REFERENCES ont_mapping_project(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_map_version_prior FOREIGN KEY (prior_version_id)
    REFERENCES ont_mapping_version(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_map_version_ontology_version FOREIGN KEY (validated_ontology_version_id)
    REFERENCES ont_ontology_version(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_map_version_status
    CHECK (version_status IN ('DRAFT','VALIDATING','VALIDATED','PUBLISHED','RETIRED')),
  CONSTRAINT ck_ont_map_version_revision CHECK (revision >= 0),
  CONSTRAINT ck_ont_map_version_del CHECK (del_flag IN ('0','1'))
);

COMMENT ON TABLE ont_mapping_version IS '本体建模-数据源映射-映射版本表';
COMMENT ON COLUMN ont_mapping_version.id IS '映射版本ID';
COMMENT ON COLUMN ont_mapping_version.mapping_project_id IS '映射工程ID';
COMMENT ON COLUMN ont_mapping_version.version_number IS '语义版本号 MAJOR.MINOR.PATCH';
COMMENT ON COLUMN ont_mapping_version.version_status IS '版本状态：DRAFT/VALIDATING/VALIDATED/PUBLISHED/RETIRED';
COMMENT ON COLUMN ont_mapping_version.prior_version_id IS '前序版本ID';
COMMENT ON COLUMN ont_mapping_version.ontology_version_constraint IS '本体版本兼容约束表达式';
COMMENT ON COLUMN ont_mapping_version.validated_ontology_version_id IS '校验时的本体版本ID';
COMMENT ON COLUMN ont_mapping_version.validated_workspace_revision IS '校验时的工作区修订号';
COMMENT ON COLUMN ont_mapping_version.config_snapshot IS '发布时规范化配置快照JSONB';
COMMENT ON COLUMN ont_mapping_version.config_hash IS '配置SHA-256哈希';
COMMENT ON COLUMN ont_mapping_version.metadata_dependencies IS '元数据依赖JSONB数组';
COMMENT ON COLUMN ont_mapping_version.validation_report_id IS '关联校验报告ID（延迟外键→ont_mapping_validation_report）';
COMMENT ON COLUMN ont_mapping_version.validation_summary IS '校验摘要JSONB';
COMMENT ON COLUMN ont_mapping_version.published_by IS '发布人';
COMMENT ON COLUMN ont_mapping_version.published_at IS '发布时间';
COMMENT ON COLUMN ont_mapping_version.retired_at IS '退役时间';
COMMENT ON COLUMN ont_mapping_version.revision IS '配置修订号，乐观锁';
COMMENT ON COLUMN ont_mapping_version.del_flag IS '删除标志，0未删除，1已删除';

CREATE UNIQUE INDEX uk_ont_map_version_number
  ON ont_mapping_version(mapping_project_id, version_number)
  WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_map_one_draft
  ON ont_mapping_version(mapping_project_id)
  WHERE del_flag = '0' AND version_status IN ('DRAFT','VALIDATING','VALIDATED');
CREATE UNIQUE INDEX uk_ont_map_one_published
  ON ont_mapping_version(mapping_project_id)
  WHERE del_flag = '0' AND version_status = 'PUBLISHED';
CREATE INDEX idx_ont_map_version_project_time
  ON ont_mapping_version(mapping_project_id, create_time DESC)
  WHERE del_flag = '0';

-- ============================================================
-- 步骤2：依赖步骤1的表
-- ============================================================

-- ----------------------------
-- 2.1 数据源元数据缓存表 ont_data_source_metadata (18-02 §7)
-- ----------------------------
CREATE TABLE ont_data_source_metadata (
  id bigint NOT NULL,
  source_id bigint NOT NULL,
  schema_name varchar(128) NOT NULL,
  object_name varchar(128) NOT NULL,
  object_type varchar(16) NOT NULL,
  metadata_json jsonb NOT NULL,
  metadata_hash varchar(64) NOT NULL,
  source_revision bigint NOT NULL,
  refreshed_at timestamp NOT NULL DEFAULT now(),
  expires_at timestamp NOT NULL,
  create_by varchar(64) DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT 'system',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_ds_meta_source FOREIGN KEY (source_id)
    REFERENCES ont_data_source(id) ON DELETE CASCADE,
  CONSTRAINT ck_ont_ds_meta_type CHECK (object_type IN ('TABLE','VIEW')),
  CONSTRAINT ck_ont_ds_meta_del CHECK (del_flag IN ('0','1'))
);

COMMENT ON TABLE ont_data_source_metadata IS '本体建模-数据源映射-元数据缓存表';
COMMENT ON COLUMN ont_data_source_metadata.id IS '元数据缓存ID';
COMMENT ON COLUMN ont_data_source_metadata.source_id IS '数据源ID';
COMMENT ON COLUMN ont_data_source_metadata.schema_name IS 'Schema名';
COMMENT ON COLUMN ont_data_source_metadata.object_name IS '对象名（表/视图）';
COMMENT ON COLUMN ont_data_source_metadata.object_type IS '对象类型：TABLE/VIEW';
COMMENT ON COLUMN ont_data_source_metadata.metadata_json IS '规范化元数据JSONB';
COMMENT ON COLUMN ont_data_source_metadata.metadata_hash IS '元数据SHA-256哈希';
COMMENT ON COLUMN ont_data_source_metadata.source_revision IS '刷新时的数据源修订号';
COMMENT ON COLUMN ont_data_source_metadata.refreshed_at IS '刷新时间';
COMMENT ON COLUMN ont_data_source_metadata.expires_at IS '过期时间';

CREATE UNIQUE INDEX uk_ont_ds_meta_object
  ON ont_data_source_metadata(source_id, schema_name, object_name)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_ds_meta_expire
  ON ont_data_source_metadata(source_id, expires_at)
  WHERE del_flag = '0';

-- ----------------------------
-- 2.2 实体映射表 ont_entity_mapping (18-04 §3)
-- ----------------------------
CREATE TABLE ont_entity_mapping (
  id bigint NOT NULL,
  mapping_version_id bigint NOT NULL,
  mapping_code varchar(64) NOT NULL,
  mapping_name varchar(128) NOT NULL,
  source_id bigint NOT NULL,
  source_schema varchar(128) NOT NULL,
  source_object varchar(128) NOT NULL,
  source_object_type varchar(16) NOT NULL DEFAULT 'TABLE',
  target_entity_type_id bigint NOT NULL,
  target_namespace_id bigint NOT NULL,
  key_columns jsonb NOT NULL,
  iri_template varchar(512) NOT NULL,
  label_template varchar(512) DEFAULT NULL,
  filter_dsl jsonb DEFAULT NULL,
  incremental_column varchar(128) DEFAULT NULL,
  incremental_type varchar(16) DEFAULT NULL,
  source_delete_flag_column varchar(128) DEFAULT NULL,
  source_delete_values jsonb DEFAULT NULL,
  delete_strategy varchar(24) NOT NULL DEFAULT 'MARK_INACTIVE',
  inactive_property_id bigint DEFAULT NULL,
  inactive_literal_value varchar(255) DEFAULT NULL,
  conflict_policy varchar(24) NOT NULL DEFAULT 'SOURCE_WINS',
  sync_order integer NOT NULL DEFAULT 0,
  enabled char(1) NOT NULL DEFAULT '1',
  description text DEFAULT NULL,
  revision bigint NOT NULL DEFAULT 0,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_entity_map_version FOREIGN KEY (mapping_version_id)
    REFERENCES ont_mapping_version(id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_entity_map_source FOREIGN KEY (source_id)
    REFERENCES ont_data_source(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_entity_map_type FOREIGN KEY (target_entity_type_id)
    REFERENCES ont_entity_type(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_entity_map_namespace FOREIGN KEY (target_namespace_id)
    REFERENCES ont_namespace(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_entity_map_inactive_prop FOREIGN KEY (inactive_property_id)
    REFERENCES ont_data_property(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_entity_map_object_type CHECK (source_object_type IN ('TABLE','VIEW')),
  CONSTRAINT ck_ont_entity_map_inc_type
    CHECK (incremental_type IS NULL OR incremental_type IN ('TIMESTAMP','NUMERIC')),
  CONSTRAINT ck_ont_entity_map_delete
    CHECK (delete_strategy IN ('IGNORE','MARK_INACTIVE','SOFT_DELETE','BLOCK_AND_REVIEW')),
  CONSTRAINT ck_ont_entity_map_conflict
    CHECK (conflict_policy IN ('SOURCE_WINS','MANUAL_WINS','REJECT_CONFLICT')),
  CONSTRAINT ck_ont_entity_map_enabled CHECK (enabled IN ('0','1')),
  CONSTRAINT ck_ont_entity_map_order CHECK (sync_order >= 0),
  CONSTRAINT ck_ont_entity_map_revision CHECK (revision >= 0),
  CONSTRAINT ck_ont_entity_map_del CHECK (del_flag IN ('0','1'))
);

COMMENT ON TABLE ont_entity_mapping IS '本体建模-数据源映射-实体映射表';
COMMENT ON COLUMN ont_entity_mapping.id IS '实体映射ID';
COMMENT ON COLUMN ont_entity_mapping.mapping_version_id IS '映射版本ID';
COMMENT ON COLUMN ont_entity_mapping.mapping_code IS '映射编码，版本内唯一';
COMMENT ON COLUMN ont_entity_mapping.source_id IS '数据源ID';
COMMENT ON COLUMN ont_entity_mapping.source_schema IS '源Schema名';
COMMENT ON COLUMN ont_entity_mapping.source_object IS '源表/视图名';
COMMENT ON COLUMN ont_entity_mapping.target_entity_type_id IS '目标实体类型ID';
COMMENT ON COLUMN ont_entity_mapping.target_namespace_id IS '目标命名空间ID';
COMMENT ON COLUMN ont_entity_mapping.key_columns IS '稳定主键列JSONB数组';
COMMENT ON COLUMN ont_entity_mapping.iri_template IS 'IRI模板';
COMMENT ON COLUMN ont_entity_mapping.label_template IS '标签模板';
COMMENT ON COLUMN ont_entity_mapping.filter_dsl IS '结构化过滤DSL JSONB';
COMMENT ON COLUMN ont_entity_mapping.incremental_column IS '增量游标列';
COMMENT ON COLUMN ont_entity_mapping.incremental_type IS '增量类型：TIMESTAMP/NUMERIC';
COMMENT ON COLUMN ont_entity_mapping.delete_strategy IS '删除策略';
COMMENT ON COLUMN ont_entity_mapping.conflict_policy IS '冲突策略';
COMMENT ON COLUMN ont_entity_mapping.sync_order IS '同步顺序';
COMMENT ON COLUMN ont_entity_mapping.enabled IS '是否启用：0否1是';

CREATE UNIQUE INDEX uk_ont_entity_map_code
  ON ont_entity_mapping(mapping_version_id, mapping_code)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_entity_map_source
  ON ont_entity_mapping(source_id, source_schema, source_object)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_entity_map_order
  ON ont_entity_mapping(mapping_version_id, sync_order)
  WHERE del_flag = '0' AND enabled = '1';

-- ----------------------------
-- 2.3 字段映射表 ont_field_mapping (18-04)
-- ----------------------------
CREATE TABLE ont_field_mapping (
  id bigint NOT NULL,
  entity_mapping_id bigint NOT NULL,
  field_mapping_code varchar(64) NOT NULL,
  field_mapping_name varchar(128) DEFAULT NULL,
  target_data_property_id bigint NOT NULL,
  source_column varchar(128) DEFAULT NULL,
  source_kind varchar(16) NOT NULL DEFAULT 'COLUMN',
  constant_value text DEFAULT NULL,
  constant_literal_type varchar(16) DEFAULT NULL,
  constant_unit_id bigint DEFAULT NULL,
  transformer varchar(64) NOT NULL DEFAULT 'IDENTITY',
  transformer_params jsonb DEFAULT NULL,
  null_handling varchar(24) NOT NULL DEFAULT 'SKIP_NULL',
  default_value text DEFAULT NULL,
  default_literal_type varchar(16) DEFAULT NULL,
  multi_value_strategy varchar(24) NOT NULL DEFAULT 'SINGLE',
  unit_id bigint DEFAULT NULL,
  ownership_policy varchar(24) NOT NULL DEFAULT 'SOURCE_WINS',
  sort_order integer NOT NULL DEFAULT 0,
  enabled char(1) NOT NULL DEFAULT '1',
  description text DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_field_map_entity FOREIGN KEY (entity_mapping_id)
    REFERENCES ont_entity_mapping(id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_field_map_property FOREIGN KEY (target_data_property_id)
    REFERENCES ont_data_property(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_field_map_unit FOREIGN KEY (unit_id)
    REFERENCES ont_unit(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_field_map_const_unit FOREIGN KEY (constant_unit_id)
    REFERENCES ont_unit(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_field_map_source_kind CHECK (source_kind IN ('COLUMN','CONSTANT')),
  CONSTRAINT ck_ont_field_map_null CHECK (null_handling IN ('SKIP_NULL','USE_DEFAULT','REJECT_NULL')),
  CONSTRAINT ck_ont_field_map_multi CHECK (multi_value_strategy IN ('SINGLE','FIRST','LAST','ALL')),
  CONSTRAINT ck_ont_field_map_owner
    CHECK (ownership_policy IN ('SOURCE_WINS','MANUAL_WINS','REJECT_CONFLICT')),
  CONSTRAINT ck_ont_field_map_enabled CHECK (enabled IN ('0','1')),
  CONSTRAINT ck_ont_field_map_order CHECK (sort_order >= 0),
  CONSTRAINT ck_ont_field_map_del CHECK (del_flag IN ('0','1'))
);

COMMENT ON TABLE ont_field_mapping IS '本体建模-数据源映射-字段映射表';
COMMENT ON COLUMN ont_field_mapping.id IS '字段映射ID';
COMMENT ON COLUMN ont_field_mapping.entity_mapping_id IS '实体映射ID';
COMMENT ON COLUMN ont_field_mapping.field_mapping_code IS '字段映射编码，实体映射内唯一';
COMMENT ON COLUMN ont_field_mapping.target_data_property_id IS '目标数据属性ID';
COMMENT ON COLUMN ont_field_mapping.source_column IS '源列名（COLUMN模式）';
COMMENT ON COLUMN ont_field_mapping.source_kind IS '来源类型：COLUMN/CONSTANT';
COMMENT ON COLUMN ont_field_mapping.constant_value IS '常量值（CONSTANT模式）';
COMMENT ON COLUMN ont_field_mapping.transformer IS '转换器：IDENTITY等内置';
COMMENT ON COLUMN ont_field_mapping.null_handling IS '空值处理：SKIP_NULL/USE_DEFAULT/REJECT_NULL';
COMMENT ON COLUMN ont_field_mapping.ownership_policy IS '所有权策略';
COMMENT ON COLUMN ont_field_mapping.sort_order IS '同属性多值排序';

CREATE UNIQUE INDEX uk_ont_field_map_code
  ON ont_field_mapping(entity_mapping_id, field_mapping_code)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_field_map_entity
  ON ont_field_mapping(entity_mapping_id, sort_order)
  WHERE del_flag = '0' AND enabled = '1';

-- ----------------------------
-- 2.4 关系映射表 ont_relation_mapping (18-05 §3)
-- ----------------------------
CREATE TABLE ont_relation_mapping (
  id bigint NOT NULL,
  mapping_version_id bigint NOT NULL,
  mapping_code varchar(64) NOT NULL,
  mapping_name varchar(128) NOT NULL,
  relation_mode varchar(24) NOT NULL,
  object_property_id bigint NOT NULL,
  subject_entity_mapping_id bigint NOT NULL,
  object_entity_mapping_id bigint NOT NULL,
  source_id bigint NOT NULL,
  source_schema varchar(128) NOT NULL,
  source_object varchar(128) NOT NULL,
  subject_key_mapping jsonb NOT NULL,
  object_key_mapping jsonb NOT NULL,
  relation_key_columns jsonb NOT NULL,
  filter_dsl jsonb DEFAULT NULL,
  missing_target_policy varchar(16) NOT NULL DEFAULT 'PENDING',
  delete_strategy varchar(24) NOT NULL DEFAULT 'REMOVE_ASSERTION',
  ownership_policy varchar(24) NOT NULL DEFAULT 'SOURCE_WINS',
  sync_order integer NOT NULL DEFAULT 1000,
  enabled char(1) NOT NULL DEFAULT '1',
  description text DEFAULT NULL,
  revision bigint NOT NULL DEFAULT 0,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_rel_map_version FOREIGN KEY (mapping_version_id)
    REFERENCES ont_mapping_version(id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_rel_map_property FOREIGN KEY (object_property_id)
    REFERENCES ont_object_property(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_rel_map_subject FOREIGN KEY (subject_entity_mapping_id)
    REFERENCES ont_entity_mapping(id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_rel_map_object FOREIGN KEY (object_entity_mapping_id)
    REFERENCES ont_entity_mapping(id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_rel_map_source FOREIGN KEY (source_id)
    REFERENCES ont_data_source(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_rel_map_mode
    CHECK (relation_mode IN ('FOREIGN_KEY','SELF_REFERENCE','JOIN_TABLE')),
  CONSTRAINT ck_ont_rel_map_missing
    CHECK (missing_target_policy IN ('PENDING','SKIP','FAIL_RECORD')),
  CONSTRAINT ck_ont_rel_map_delete
    CHECK (delete_strategy IN ('REMOVE_ASSERTION','KEEP_ASSERTION','BLOCK_AND_REVIEW')),
  CONSTRAINT ck_ont_rel_map_owner
    CHECK (ownership_policy IN ('SOURCE_WINS','MANUAL_WINS','REJECT_CONFLICT')),
  CONSTRAINT ck_ont_rel_map_enabled CHECK (enabled IN ('0','1')),
  CONSTRAINT ck_ont_rel_map_order CHECK (sync_order >= 0),
  CONSTRAINT ck_ont_rel_map_revision CHECK (revision >= 0),
  CONSTRAINT ck_ont_rel_map_del CHECK (del_flag IN ('0','1'))
);

COMMENT ON TABLE ont_relation_mapping IS '本体建模-数据源映射-关系映射表';
COMMENT ON COLUMN ont_relation_mapping.id IS '关系映射ID';
COMMENT ON COLUMN ont_relation_mapping.mapping_version_id IS '映射版本ID';
COMMENT ON COLUMN ont_relation_mapping.mapping_code IS '关系映射编码，版本内唯一';
COMMENT ON COLUMN ont_relation_mapping.relation_mode IS '关系模式：FOREIGN_KEY/SELF_REFERENCE/JOIN_TABLE';
COMMENT ON COLUMN ont_relation_mapping.object_property_id IS '目标对象属性ID';
COMMENT ON COLUMN ont_relation_mapping.subject_entity_mapping_id IS '主体实体映射ID';
COMMENT ON COLUMN ont_relation_mapping.object_entity_mapping_id IS '客体实体映射ID';
COMMENT ON COLUMN ont_relation_mapping.missing_target_policy IS '缺失目标策略：PENDING/SKIP/FAIL_RECORD';
COMMENT ON COLUMN ont_relation_mapping.delete_strategy IS '删除策略';
COMMENT ON COLUMN ont_relation_mapping.ownership_policy IS '所有权策略';
COMMENT ON COLUMN ont_relation_mapping.sync_order IS '同步顺序';

CREATE UNIQUE INDEX uk_ont_rel_map_code
  ON ont_relation_mapping(mapping_version_id, mapping_code)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_rel_map_order
  ON ont_relation_mapping(mapping_version_id, sync_order)
  WHERE del_flag = '0' AND enabled = '1';

-- ----------------------------
-- 2.5 来源绑定表 ont_source_instance_binding (18-01 §4)
-- ----------------------------
CREATE TABLE ont_source_instance_binding (
  id bigint NOT NULL,
  source_id bigint NOT NULL,
  mapping_project_id bigint NOT NULL,
  current_mapping_version_id bigint NOT NULL,
  entity_mapping_code varchar(64) NOT NULL,
  source_object varchar(256) NOT NULL,
  source_record_key varchar(1024) NOT NULL,
  source_record_key_hash varchar(64) NOT NULL,
  instance_id bigint NOT NULL,
  source_updated_at timestamp DEFAULT NULL,
  content_hash varchar(64) DEFAULT NULL,
  first_seen_job_id bigint DEFAULT NULL,
  last_seen_job_id bigint DEFAULT NULL,
  first_seen_at timestamp NOT NULL DEFAULT now(),
  last_seen_at timestamp NOT NULL DEFAULT now(),
  binding_status varchar(16) NOT NULL DEFAULT 'ACTIVE',
  miss_count integer NOT NULL DEFAULT 0,
  create_by varchar(64) DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT 'system',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_binding_source FOREIGN KEY (source_id)
    REFERENCES ont_data_source(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_binding_project FOREIGN KEY (mapping_project_id)
    REFERENCES ont_mapping_project(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_binding_version FOREIGN KEY (current_mapping_version_id)
    REFERENCES ont_mapping_version(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_binding_instance FOREIGN KEY (instance_id)
    REFERENCES ont_entity_instance(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_binding_status
    CHECK (binding_status IN ('ACTIVE','INACTIVE','MISSING','CONFLICT')),
  CONSTRAINT ck_ont_binding_del CHECK (del_flag IN ('0','1'))
);

COMMENT ON TABLE ont_source_instance_binding IS '本体建模-数据源映射-来源实例绑定表';
COMMENT ON COLUMN ont_source_instance_binding.id IS '绑定ID';
COMMENT ON COLUMN ont_source_instance_binding.source_id IS '数据源ID';
COMMENT ON COLUMN ont_source_instance_binding.mapping_project_id IS '映射工程ID';
COMMENT ON COLUMN ont_source_instance_binding.current_mapping_version_id IS '当前映射版本ID';
COMMENT ON COLUMN ont_source_instance_binding.entity_mapping_code IS '实体映射编码';
COMMENT ON COLUMN ont_source_instance_binding.source_object IS '源对象名';
COMMENT ON COLUMN ont_source_instance_binding.source_record_key IS '规范化复合键';
COMMENT ON COLUMN ont_source_instance_binding.source_record_key_hash IS '复合键SHA-256哈希';
COMMENT ON COLUMN ont_source_instance_binding.instance_id IS '绑定的实例ID';
COMMENT ON COLUMN ont_source_instance_binding.content_hash IS '内容SHA-256哈希';
COMMENT ON COLUMN ont_source_instance_binding.first_seen_job_id IS '首次发现作业ID（延迟外键）';
COMMENT ON COLUMN ont_source_instance_binding.last_seen_job_id IS '最近发现作业ID（延迟外键）';
COMMENT ON COLUMN ont_source_instance_binding.binding_status IS '绑定状态：ACTIVE/INACTIVE/MISSING/CONFLICT';
COMMENT ON COLUMN ont_source_instance_binding.miss_count IS '连续缺失次数';

CREATE UNIQUE INDEX uk_ont_binding_identity
  ON ont_source_instance_binding
  (mapping_project_id, entity_mapping_code, source_record_key_hash)
  WHERE del_flag = '0';

CREATE UNIQUE INDEX uk_ont_binding_instance_mapping
  ON ont_source_instance_binding (mapping_project_id, entity_mapping_code, instance_id)
  WHERE del_flag = '0';

CREATE INDEX idx_ont_binding_last_seen
  ON ont_source_instance_binding (mapping_project_id, last_seen_job_id, binding_status)
  WHERE del_flag = '0';

-- ----------------------------
-- 2.6 值来源表 ont_instance_value_provenance (18-01 §5)
-- ----------------------------
CREATE TABLE ont_instance_value_provenance (
  id bigint NOT NULL,
  data_value_id bigint NOT NULL,
  source_binding_id bigint NOT NULL,
  mapping_version_id bigint NOT NULL,
  field_mapping_code varchar(64) NOT NULL,
  source_kind varchar(16) NOT NULL,
  source_reference varchar(128) NOT NULL,
  ownership_policy varchar(24) NOT NULL,
  provenance_status varchar(16) NOT NULL DEFAULT 'ACTIVE',
  source_updated_at timestamp DEFAULT NULL,
  value_hash varchar(64) NOT NULL,
  last_job_id bigint DEFAULT NULL,
  generated_at timestamp NOT NULL DEFAULT now(),
  create_by varchar(64) DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT 'system',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_value_prov_value FOREIGN KEY (data_value_id)
    REFERENCES ont_instance_data_value(id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_value_prov_binding FOREIGN KEY (source_binding_id)
    REFERENCES ont_source_instance_binding(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_value_prov_version FOREIGN KEY (mapping_version_id)
    REFERENCES ont_mapping_version(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_value_prov_source_kind
    CHECK (source_kind IN ('COLUMN','CONSTANT')),
  CONSTRAINT ck_ont_value_owner
    CHECK (ownership_policy IN ('SOURCE_WINS','MANUAL_WINS','REJECT_CONFLICT')),
  CONSTRAINT ck_ont_value_prov_status
    CHECK (provenance_status IN ('ACTIVE','OVERRIDDEN','STALE')),
  CONSTRAINT ck_ont_value_prov_del CHECK (del_flag IN ('0','1'))
);

COMMENT ON TABLE ont_instance_value_provenance IS '本体建模-数据源映射-值来源表';
COMMENT ON COLUMN ont_instance_value_provenance.id IS '值来源ID';
COMMENT ON COLUMN ont_instance_value_provenance.data_value_id IS '数据值ID';
COMMENT ON COLUMN ont_instance_value_provenance.source_binding_id IS '来源绑定ID';
COMMENT ON COLUMN ont_instance_value_provenance.mapping_version_id IS '映射版本ID';
COMMENT ON COLUMN ont_instance_value_provenance.field_mapping_code IS '字段映射编码';
COMMENT ON COLUMN ont_instance_value_provenance.source_kind IS '来源类型：COLUMN/CONSTANT';
COMMENT ON COLUMN ont_instance_value_provenance.source_reference IS '来源引用（列名或constant标记）';
COMMENT ON COLUMN ont_instance_value_provenance.ownership_policy IS '所有权策略';
COMMENT ON COLUMN ont_instance_value_provenance.provenance_status IS '溯源状态：ACTIVE/OVERRIDDEN/STALE';
COMMENT ON COLUMN ont_instance_value_provenance.value_hash IS '值SHA-256哈希';
COMMENT ON COLUMN ont_instance_value_provenance.last_job_id IS '最近作业ID（延迟外键）';

CREATE UNIQUE INDEX uk_ont_value_prov_value
  ON ont_instance_value_provenance(data_value_id)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_value_prov_binding
  ON ont_instance_value_provenance(source_binding_id, field_mapping_code)
  WHERE del_flag = '0';

-- ----------------------------
-- 2.7 关系来源表 ont_instance_relation_provenance (18-01 §6)
-- ----------------------------
CREATE TABLE ont_instance_relation_provenance (
  id bigint NOT NULL,
  relation_id bigint NOT NULL,
  mapping_project_id bigint NOT NULL,
  subject_binding_id bigint NOT NULL,
  object_binding_id bigint NOT NULL,
  mapping_version_id bigint NOT NULL,
  relation_mapping_code varchar(64) NOT NULL,
  ownership_policy varchar(24) NOT NULL,
  provenance_status varchar(16) NOT NULL DEFAULT 'ACTIVE',
  source_relation_key varchar(1024) NOT NULL,
  source_relation_key_hash varchar(64) NOT NULL,
  last_job_id bigint DEFAULT NULL,
  generated_at timestamp NOT NULL DEFAULT now(),
  create_by varchar(64) DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT 'system',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_rel_prov_relation FOREIGN KEY (relation_id)
    REFERENCES ont_instance_object_relation(id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_rel_prov_project FOREIGN KEY (mapping_project_id)
    REFERENCES ont_mapping_project(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_rel_prov_subject FOREIGN KEY (subject_binding_id)
    REFERENCES ont_source_instance_binding(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_rel_prov_object FOREIGN KEY (object_binding_id)
    REFERENCES ont_source_instance_binding(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_rel_prov_version FOREIGN KEY (mapping_version_id)
    REFERENCES ont_mapping_version(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_rel_owner
    CHECK (ownership_policy IN ('SOURCE_WINS','MANUAL_WINS','REJECT_CONFLICT')),
  CONSTRAINT ck_ont_rel_prov_status
    CHECK (provenance_status IN ('ACTIVE','OVERRIDDEN','STALE')),
  CONSTRAINT ck_ont_rel_prov_del CHECK (del_flag IN ('0','1'))
);

COMMENT ON TABLE ont_instance_relation_provenance IS '本体建模-数据源映射-关系来源表';
COMMENT ON COLUMN ont_instance_relation_provenance.id IS '关系来源ID';
COMMENT ON COLUMN ont_instance_relation_provenance.relation_id IS '关系断言ID';
COMMENT ON COLUMN ont_instance_relation_provenance.mapping_project_id IS '映射工程ID';
COMMENT ON COLUMN ont_instance_relation_provenance.subject_binding_id IS '主体来源绑定ID';
COMMENT ON COLUMN ont_instance_relation_provenance.object_binding_id IS '客体来源绑定ID';
COMMENT ON COLUMN ont_instance_relation_provenance.relation_mapping_code IS '关系映射编码';
COMMENT ON COLUMN ont_instance_relation_provenance.source_relation_key IS '来源关系键';
COMMENT ON COLUMN ont_instance_relation_provenance.source_relation_key_hash IS '来源关系键SHA-256哈希';
COMMENT ON COLUMN ont_instance_relation_provenance.last_job_id IS '最近作业ID（延迟外键）';

CREATE INDEX idx_ont_rel_prov_relation
  ON ont_instance_relation_provenance(relation_id)
  WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_rel_prov_source
  ON ont_instance_relation_provenance
  (mapping_project_id, relation_mapping_code, source_relation_key_hash)
  WHERE del_flag = '0';

-- ----------------------------
-- 2.8 待解析关系表 ont_pending_relation (18-05 §6)
-- ----------------------------
CREATE TABLE ont_pending_relation (
  id bigint NOT NULL,
  mapping_project_id bigint NOT NULL,
  mapping_version_id bigint NOT NULL,
  relation_mapping_code varchar(64) NOT NULL,
  source_id bigint NOT NULL,
  source_relation_key varchar(1024) NOT NULL,
  source_relation_key_hash varchar(64) NOT NULL,
  subject_entity_mapping_code varchar(64) NOT NULL,
  subject_record_key varchar(1024) NOT NULL,
  subject_record_key_hash varchar(64) NOT NULL,
  object_entity_mapping_code varchar(64) NOT NULL,
  object_record_key varchar(1024) NOT NULL,
  object_record_key_hash varchar(64) NOT NULL,
  pending_reason varchar(32) NOT NULL,
  pending_status varchar(16) NOT NULL DEFAULT 'PENDING',
  retry_count integer NOT NULL DEFAULT 0,
  next_retry_at timestamp DEFAULT NULL,
  last_error_code varchar(64) DEFAULT NULL,
  last_error_message varchar(512) DEFAULT NULL,
  first_job_id bigint NOT NULL,
  last_job_id bigint NOT NULL,
  resolved_relation_id bigint DEFAULT NULL,
  create_by varchar(64) DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT 'system',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_pending_project FOREIGN KEY (mapping_project_id)
    REFERENCES ont_mapping_project(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_pending_version FOREIGN KEY (mapping_version_id)
    REFERENCES ont_mapping_version(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_pending_source FOREIGN KEY (source_id)
    REFERENCES ont_data_source(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_pending_relation FOREIGN KEY (resolved_relation_id)
    REFERENCES ont_instance_object_relation(id) ON DELETE SET NULL,
  CONSTRAINT ck_ont_pending_reason
    CHECK (pending_reason IN ('SUBJECT_MISSING','OBJECT_MISSING','BOTH_MISSING')),
  CONSTRAINT ck_ont_pending_status
    CHECK (pending_status IN ('PENDING','RESOLVED','FAILED','IGNORED')),
  CONSTRAINT ck_ont_pending_del CHECK (del_flag IN ('0','1'))
);

COMMENT ON TABLE ont_pending_relation IS '本体建模-数据源映射-待解析关系表';
COMMENT ON COLUMN ont_pending_relation.id IS '待解析关系ID';
COMMENT ON COLUMN ont_pending_relation.mapping_project_id IS '映射工程ID';
COMMENT ON COLUMN ont_pending_relation.relation_mapping_code IS '关系映射编码';
COMMENT ON COLUMN ont_pending_relation.pending_reason IS '待解析原因：SUBJECT_MISSING/OBJECT_MISSING/BOTH_MISSING';
COMMENT ON COLUMN ont_pending_relation.pending_status IS '状态：PENDING/RESOLVED/FAILED/IGNORED';
COMMENT ON COLUMN ont_pending_relation.first_job_id IS '首次产生作业ID（延迟外键）';
COMMENT ON COLUMN ont_pending_relation.last_job_id IS '最近处理作业ID（延迟外键）';
COMMENT ON COLUMN ont_pending_relation.resolved_relation_id IS '解析成功后的关系断言ID';

CREATE INDEX idx_ont_pending_status
  ON ont_pending_relation(pending_status, next_retry_at)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_pending_project
  ON ont_pending_relation(mapping_project_id, pending_status)
  WHERE del_flag = '0';

-- ============================================================
-- 步骤3：作业表（多表外键循环，最后建）
-- ============================================================

-- ----------------------------
-- 3.1 作业主表 ont_mapping_job (18-07 §3)
-- ----------------------------
CREATE TABLE ont_mapping_job (
  id bigint NOT NULL,
  mapping_project_id bigint NOT NULL,
  mapping_version_id bigint NOT NULL,
  run_type varchar(24) NOT NULL,
  trigger_type varchar(16) NOT NULL,
  job_status varchar(24) NOT NULL DEFAULT 'QUEUED',
  requested_by varchar(64) NOT NULL,
  requested_user_id bigint DEFAULT NULL,
  authorization_snapshot jsonb NOT NULL,
  config_hash varchar(64) NOT NULL,
  ontology_version_id bigint NOT NULL,
  workspace_revision bigint NOT NULL,
  cursor_before jsonb DEFAULT NULL,
  cursor_after jsonb DEFAULT NULL,
  current_phase varchar(24) DEFAULT NULL,
  current_mapping_code varchar(64) DEFAULT NULL,
  current_page_no bigint NOT NULL DEFAULT 0,
  page_size integer NOT NULL,
  total_read bigint NOT NULL DEFAULT 0,
  total_created bigint NOT NULL DEFAULT 0,
  total_updated bigint NOT NULL DEFAULT 0,
  total_unchanged bigint NOT NULL DEFAULT 0,
  total_skipped bigint NOT NULL DEFAULT 0,
  total_failed bigint NOT NULL DEFAULT 0,
  total_relations bigint NOT NULL DEFAULT 0,
  lease_owner varchar(128) DEFAULT NULL,
  lease_until timestamp DEFAULT NULL,
  heartbeat_at timestamp DEFAULT NULL,
  cancel_requested char(1) NOT NULL DEFAULT '0',
  started_at timestamp DEFAULT NULL,
  finished_at timestamp DEFAULT NULL,
  error_code varchar(64) DEFAULT NULL,
  error_message varchar(512) DEFAULT NULL,
  trace_id varchar(64) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_job_project FOREIGN KEY (mapping_project_id)
    REFERENCES ont_mapping_project(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_job_version FOREIGN KEY (mapping_version_id)
    REFERENCES ont_mapping_version(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_job_ontology_version FOREIGN KEY (ontology_version_id)
    REFERENCES ont_ontology_version(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_job_run_type
    CHECK (run_type IN ('PREVIEW','FULL','INCREMENTAL','RETRY','RELATION_RETRY')),
  CONSTRAINT ck_ont_job_trigger
    CHECK (trigger_type IN ('MANUAL','SCHEDULE','API','RECOVERY')),
  CONSTRAINT ck_ont_job_status
    CHECK (job_status IN ('QUEUED','STARTING','RUNNING','RECOVERING','CANCELLING','CANCELLED','SUCCEEDED','PARTIAL_SUCCESS','FAILED')),
  CONSTRAINT ck_ont_job_cancel CHECK (cancel_requested IN ('0','1')),
  CONSTRAINT ck_ont_job_counts CHECK (
    current_page_no >= 0 AND page_size > 0 AND
    total_read >= 0 AND total_created >= 0 AND total_updated >= 0 AND
    total_unchanged >= 0 AND total_skipped >= 0 AND total_failed >= 0 AND total_relations >= 0
  ),
  CONSTRAINT ck_ont_job_del CHECK (del_flag IN ('0','1'))
);

COMMENT ON TABLE ont_mapping_job IS '本体建模-数据源映射-作业主表';
COMMENT ON COLUMN ont_mapping_job.id IS '作业ID';
COMMENT ON COLUMN ont_mapping_job.run_type IS '运行类型：PREVIEW/FULL/INCREMENTAL/RETRY/RELATION_RETRY';
COMMENT ON COLUMN ont_mapping_job.job_status IS '作业状态';
COMMENT ON COLUMN ont_mapping_job.authorization_snapshot IS '授权上下文快照JSONB';
COMMENT ON COLUMN ont_mapping_job.config_hash IS '执行时配置哈希';
COMMENT ON COLUMN ont_mapping_job.cursor_before IS '游标前值JSONB';
COMMENT ON COLUMN ont_mapping_job.cursor_after IS '游标后值JSONB';
COMMENT ON COLUMN ont_mapping_job.lease_owner IS '租约持有者';
COMMENT ON COLUMN ont_mapping_job.lease_until IS '租约到期时间';

CREATE INDEX idx_ont_job_queue
  ON ont_mapping_job(job_status, create_time)
  WHERE del_flag = '0' AND job_status IN ('QUEUED','RECOVERING');
CREATE INDEX idx_ont_job_lease
  ON ont_mapping_job(job_status, lease_until)
  WHERE del_flag = '0' AND job_status IN ('STARTING','RUNNING','CANCELLING');
CREATE INDEX idx_ont_job_project_time
  ON ont_mapping_job(mapping_project_id, create_time DESC)
  WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_job_project_running
  ON ont_mapping_job(mapping_project_id)
  WHERE del_flag = '0'
    AND run_type <> 'PREVIEW'
    AND job_status IN ('STARTING','RUNNING','RECOVERING','CANCELLING');

-- ----------------------------
-- 3.2 记录结果表 ont_mapping_job_record (18-07 §4)
-- ----------------------------
CREATE TABLE ont_mapping_job_record (
  id bigint NOT NULL,
  job_id bigint NOT NULL,
  retry_of_record_id bigint DEFAULT NULL,
  phase varchar(16) NOT NULL,
  mapping_code varchar(64) NOT NULL,
  source_object varchar(256) NOT NULL,
  source_record_key_hash varchar(64) NOT NULL,
  source_record_key_masked varchar(256) DEFAULT NULL,
  record_action varchar(24) NOT NULL,
  record_status varchar(16) NOT NULL,
  instance_id bigint DEFAULT NULL,
  relation_id bigint DEFAULT NULL,
  error_code varchar(64) DEFAULT NULL,
  error_message varchar(512) DEFAULT NULL,
  field_errors jsonb DEFAULT NULL,
  payload_hash varchar(64) DEFAULT NULL,
  source_updated_at timestamp DEFAULT NULL,
  retry_count integer NOT NULL DEFAULT 0,
  duration_ms bigint DEFAULT NULL,
  create_by varchar(64) DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT 'system',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_job_record_job FOREIGN KEY (job_id)
    REFERENCES ont_mapping_job(id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_job_record_retry_of FOREIGN KEY (retry_of_record_id)
    REFERENCES ont_mapping_job_record(id) ON DELETE SET NULL,
  CONSTRAINT ck_ont_job_record_phase CHECK (phase IN ('ENTITY','RELATION','DELETE','PENDING')),
  CONSTRAINT ck_ont_job_record_action
    CHECK (record_action IN ('CREATE','UPDATE','UNCHANGED','SKIP','DEACTIVATE','DELETE','RELATE','UNRELATE','PEND')),
  CONSTRAINT ck_ont_job_record_status CHECK (record_status IN ('SUCCESS','FAILED','SKIPPED','PENDING')),
  CONSTRAINT ck_ont_job_record_retry CHECK (retry_count >= 0),
  CONSTRAINT ck_ont_job_record_del CHECK (del_flag IN ('0','1'))
);

COMMENT ON TABLE ont_mapping_job_record IS '本体建模-数据源映射-作业记录结果表';
COMMENT ON COLUMN ont_mapping_job_record.phase IS '阶段：ENTITY/RELATION/DELETE/PENDING';
COMMENT ON COLUMN ont_mapping_job_record.record_action IS '记录动作';
COMMENT ON COLUMN ont_mapping_job_record.record_status IS '记录状态：SUCCESS/FAILED/SKIPPED/PENDING';
COMMENT ON COLUMN ont_mapping_job_record.source_record_key_masked IS '脱敏后的记录键摘要';
COMMENT ON COLUMN ont_mapping_job_record.field_errors IS '字段级错误JSONB';

CREATE INDEX idx_ont_job_record_job_status
  ON ont_mapping_job_record(job_id, record_status, mapping_code)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_job_record_key
  ON ont_mapping_job_record(mapping_code, source_record_key_hash, create_time DESC)
  WHERE del_flag = '0';

-- ----------------------------
-- 3.3 校验报告表 ont_mapping_validation_report (18-06 §4)
-- ----------------------------
CREATE TABLE ont_mapping_validation_report (
  id bigint NOT NULL,
  mapping_version_id bigint NOT NULL,
  report_status varchar(16) NOT NULL DEFAULT 'RUNNING',
  trigger_type varchar(16) NOT NULL,
  config_revision bigint NOT NULL,
  candidate_config_hash varchar(64) NOT NULL,
  ontology_version_id bigint NOT NULL,
  workspace_revision bigint NOT NULL,
  metadata_hash_summary varchar(64) NOT NULL,
  sample_size integer NOT NULL DEFAULT 0,
  violation_count integer NOT NULL DEFAULT 0,
  warning_count integer NOT NULL DEFAULT 0,
  info_count integer NOT NULL DEFAULT 0,
  summary_json jsonb NOT NULL DEFAULT '{}'::jsonb,
  started_at timestamp NOT NULL DEFAULT now(),
  completed_at timestamp DEFAULT NULL,
  requested_by varchar(64) NOT NULL,
  trace_id varchar(64) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_map_val_version FOREIGN KEY (mapping_version_id)
    REFERENCES ont_mapping_version(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_map_val_ontology_version FOREIGN KEY (ontology_version_id)
    REFERENCES ont_ontology_version(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_map_val_status
    CHECK (report_status IN ('RUNNING','PASSED','FAILED','CANCELLED')),
  CONSTRAINT ck_ont_map_val_trigger
    CHECK (trigger_type IN ('MANUAL','PUBLISH_RECHECK','SYSTEM')),
  CONSTRAINT ck_ont_map_val_counts
    CHECK (sample_size >= 0 AND violation_count >= 0 AND warning_count >= 0 AND info_count >= 0),
  CONSTRAINT ck_ont_map_val_del CHECK (del_flag IN ('0','1'))
);

COMMENT ON TABLE ont_mapping_validation_report IS '本体建模-数据源映射-校验报告表';
COMMENT ON COLUMN ont_mapping_validation_report.id IS '报告ID';
COMMENT ON COLUMN ont_mapping_validation_report.mapping_version_id IS '映射版本ID';
COMMENT ON COLUMN ont_mapping_validation_report.report_status IS '报告状态：RUNNING/PASSED/FAILED/CANCELLED';
COMMENT ON COLUMN ont_mapping_validation_report.trigger_type IS '触发类型：MANUAL/PUBLISH_RECHECK/SYSTEM';

CREATE INDEX idx_ont_map_val_version_time
  ON ont_mapping_validation_report(mapping_version_id, create_time DESC)
  WHERE del_flag = '0';

-- ----------------------------
-- 3.4 校验问题表 ont_mapping_validation_issue (18-06 §5)
-- ----------------------------
CREATE TABLE ont_mapping_validation_issue (
  id bigint NOT NULL,
  report_id bigint NOT NULL,
  severity varchar(16) NOT NULL,
  issue_code varchar(64) NOT NULL,
  scope_type varchar(24) NOT NULL,
  scope_ref varchar(128) DEFAULT NULL,
  message varchar(512) NOT NULL,
  suggestion varchar(512) DEFAULT NULL,
  source_record_key_hash varchar(64) DEFAULT NULL,
  sample_context jsonb DEFAULT NULL,
  acknowledged char(1) NOT NULL DEFAULT '0',
  acknowledged_by varchar(64) DEFAULT NULL,
  acknowledged_at timestamp DEFAULT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  create_by varchar(64) DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT 'system',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_map_issue_report FOREIGN KEY (report_id)
    REFERENCES ont_mapping_validation_report(id) ON DELETE CASCADE,
  CONSTRAINT ck_ont_map_issue_severity CHECK (severity IN ('VIOLATION','WARNING','INFO')),
  CONSTRAINT ck_ont_map_issue_scope
    CHECK (scope_type IN ('PROJECT','VERSION','SOURCE','ENTITY','FIELD','RELATION','RECORD')),
  CONSTRAINT ck_ont_map_issue_ack CHECK (acknowledged IN ('0','1')),
  CONSTRAINT ck_ont_map_issue_del CHECK (del_flag IN ('0','1'))
);

COMMENT ON TABLE ont_mapping_validation_issue IS '本体建模-数据源映射-校验问题表';
COMMENT ON COLUMN ont_mapping_validation_issue.severity IS '严重级别：VIOLATION/WARNING/INFO';
COMMENT ON COLUMN ont_mapping_validation_issue.scope_type IS '问题范围类型';
COMMENT ON COLUMN ont_mapping_validation_issue.acknowledged IS '是否已确认：0否1是';

CREATE INDEX idx_ont_map_issue_report_severity
  ON ont_mapping_validation_issue(report_id, severity, sort_order)
  WHERE del_flag = '0';

-- ============================================================
-- 步骤4：延迟外键追加（全部表已创建后）
-- ============================================================

-- 4.1 映射工程 → 映射版本
ALTER TABLE ont_mapping_project
  ADD CONSTRAINT fk_ont_map_project_active_version
    FOREIGN KEY (active_version_id) REFERENCES ont_mapping_version(id) ON DELETE RESTRICT;

-- 4.2 映射版本 → 校验报告
ALTER TABLE ont_mapping_version
  ADD CONSTRAINT fk_ont_map_version_validation_report
    FOREIGN KEY (validation_report_id)
    REFERENCES ont_mapping_validation_report(id) ON DELETE SET NULL;

-- 4.3 映射工程 → 作业
ALTER TABLE ont_mapping_project
  ADD CONSTRAINT fk_ont_map_project_last_job
    FOREIGN KEY (last_job_id) REFERENCES ont_mapping_job(id) ON DELETE SET NULL;

-- 4.4 来源绑定 → 作业
ALTER TABLE ont_source_instance_binding
  ADD CONSTRAINT fk_ont_binding_first_job
    FOREIGN KEY (first_seen_job_id) REFERENCES ont_mapping_job(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_ont_binding_last_job
    FOREIGN KEY (last_seen_job_id) REFERENCES ont_mapping_job(id) ON DELETE SET NULL;

-- 4.5 值来源 → 作业
ALTER TABLE ont_instance_value_provenance
  ADD CONSTRAINT fk_ont_value_prov_last_job
    FOREIGN KEY (last_job_id) REFERENCES ont_mapping_job(id) ON DELETE SET NULL;

-- 4.6 关系来源 → 作业
ALTER TABLE ont_instance_relation_provenance
  ADD CONSTRAINT fk_ont_rel_prov_last_job
    FOREIGN KEY (last_job_id) REFERENCES ont_mapping_job(id) ON DELETE SET NULL;

-- 4.7 待解析关系 → 作业
ALTER TABLE ont_pending_relation
  ADD CONSTRAINT fk_ont_pending_first_job
    FOREIGN KEY (first_job_id) REFERENCES ont_mapping_job(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_ont_pending_last_job
    FOREIGN KEY (last_job_id) REFERENCES ont_mapping_job(id) ON DELETE RESTRICT;

-- ============================================================
-- 步骤5：改造现有表
-- ============================================================

-- 5.1 放宽 ont_entity_instance 的 source_type 约束 (18-01 §7)
ALTER TABLE ont_entity_instance
  DROP CONSTRAINT IF EXISTS ck_ont_entity_instance_source_type,
  DROP CONSTRAINT IF EXISTS ck_ont_entity_instance_source_builtin;

ALTER TABLE ont_entity_instance
  ADD CONSTRAINT ck_ont_entity_instance_source_type
    CHECK (source_type IN (
      'APPENDIX_D','EXTENSION','MANUAL','DATA_MAPPING',
      'IOT','RULE','IMPORT','API'
    )),
  ADD CONSTRAINT ck_ont_entity_instance_source_builtin
    CHECK (
      (source_type = 'APPENDIX_D' AND is_builtin = '1') OR
      (source_type <> 'APPENDIX_D' AND is_builtin = '0')
    );

COMMENT ON COLUMN ont_entity_instance.source_type IS '来源类型：APPENDIX_D/EXTENSION/MANUAL/DATA_MAPPING/IOT/RULE/IMPORT/API';

-- 5.2 ont_instance_object_relation 新增 assertion_origin 列 (18-00 §7.2 步骤5)
ALTER TABLE ont_instance_object_relation
  ADD COLUMN assertion_origin varchar(16) NOT NULL DEFAULT 'SEED';

ALTER TABLE ont_instance_object_relation
  ADD CONSTRAINT ck_ont_relation_assertion_origin
    CHECK (assertion_origin IN ('SEED','MANUAL','DATA_MAPPING','IOT','RULE','IMPORT','API'));

COMMENT ON COLUMN ont_instance_object_relation.assertion_origin IS '断言来源：SEED/MANUAL/DATA_MAPPING/IOT/RULE/IMPORT/API';

-- 5.3 ont_data_policy_rule 扩展 action 增加 INGEST (18-00 §7.2 步骤5)
ALTER TABLE ont_data_policy_rule
  DROP CONSTRAINT IF EXISTS ck_ont_policy_action;

ALTER TABLE ont_data_policy_rule
  ADD CONSTRAINT ck_ont_policy_action
    CHECK (action IN ('VIEW', 'EDIT', 'EXPORT', 'SPARQL', 'INGEST'));

COMMENT ON COLUMN ont_data_policy_rule.action IS '动作：VIEW/EDIT/EXPORT/SPARQL/INGEST';

-- ============================================================
-- 步骤6：V22菜单和权限种子 (18-00 §10)
-- ============================================================

-- 6.1 新增「数据源映射」二级目录菜单（menu_type='0' 目录，无 component）
INSERT INTO sys_menu
(menu_id, name, permission, path, component, parent_id, icon, visible,
 sort_order, keep_alive, embedded, menu_type,
 create_by, create_time, update_by, update_time, del_flag)
VALUES
(900020, '数据源映射', NULL, '/data-mapping', NULL, 900000,
 'ele-Connection', '1', 2, '0', NULL, '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO UPDATE
SET name        = EXCLUDED.name,
    path        = EXCLUDED.path,
    parent_id   = EXCLUDED.parent_id,
    icon        = EXCLUDED.icon,
    sort_order  = EXCLUDED.sort_order,
    menu_type   = EXCLUDED.menu_type,
    update_by   = 'admin',
    update_time = now(),
    del_flag    = '0';

-- 6.2 数据源映射功能菜单及按钮权限（parent_id 指向 900020）
INSERT INTO sys_menu
(menu_id, name, permission, path, component, parent_id, icon, visible,
 sort_order, keep_alive, embedded, menu_type,
 create_by, create_time, update_by, update_time, del_flag)
VALUES
(901600, '数据源映射', NULL, '/ontology/data-mapping/index', NULL, 900020,
 'ele-Connection', '1', 1, '0', NULL, '0', 'admin', now(), 'admin', now(), '0'),
(901601, '映射查看', 'ontology_mapping_view', NULL, NULL, 901600, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
(901602, '数据源管理', 'ontology_mapping_source_manage', NULL, NULL, 901600, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
(901603, '映射编辑', 'ontology_mapping_edit', NULL, NULL, 901600, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
(901604, '映射校验', 'ontology_mapping_validate', NULL, NULL, 901600, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
(901605, '映射发布', 'ontology_mapping_publish', NULL, NULL, 901600, NULL, '1', 5, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
(901606, '映射执行', 'ontology_mapping_execute', NULL, NULL, 901600, NULL, '1', 6, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
(901607, '作业查看', 'ontology_mapping_job_view', NULL, NULL, 901600, NULL, '1', 7, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
(901608, '失败重试', 'ontology_mapping_retry', NULL, NULL, 901600, NULL, '1', 8, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
(901609, '映射管理', 'ontology_mapping_admin', NULL, NULL, 901600, NULL, '1', 9, '0', NULL, '1', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

-- 6.3 授权：将「数据源映射」目录及功能菜单授予管理员角色（role_id=1）
INSERT INTO sys_role_menu(role_id, menu_id)
VALUES
(1,900020),
(1,901600),(1,901601),(1,901602),(1,901603),(1,901604),
(1,901605),(1,901606),(1,901607),(1,901608),(1,901609)
ON CONFLICT (role_id, menu_id) DO NOTHING;
