-- ================================================================
-- V20: 安全与合规模块
-- 设计文档: 36-安全与合规模块详细设计.md
-- 依赖: V4(本体工程/菜单), V6(entity_type), V7(data_property),
--       V8(object_property), V10(entity_instance), V19(EDA事件骨干)
-- ================================================================

-- ================================================================
-- 1. 安全级别表 ont_security_level
-- ================================================================
CREATE TABLE ont_security_level (
  id bigint NOT NULL,
  level_code varchar(32) NOT NULL,
  level_name varchar(64) NOT NULL,
  level_rank integer NOT NULL,
  default_view_effect varchar(16) NOT NULL,
  default_export_effect varchar(16) NOT NULL,
  description text DEFAULT NULL,
  is_builtin char(1) NOT NULL DEFAULT '1',
  sort_order integer NOT NULL DEFAULT 0,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT uk_ont_security_level_code UNIQUE (level_code),
  CONSTRAINT ck_ont_security_view_effect
    CHECK (default_view_effect IN ('ALLOW', 'MASK', 'DENY')),
  CONSTRAINT ck_ont_security_export_effect
    CHECK (default_export_effect IN ('ALLOW', 'MASK', 'DENY')),
  CONSTRAINT ck_ont_security_builtin CHECK (is_builtin IN ('0', '1')),
  CONSTRAINT ck_ont_security_level_del CHECK (del_flag IN ('0', '1'))
);

COMMENT ON TABLE ont_security_level IS '本体建模-安全级别定义表';
COMMENT ON COLUMN ont_security_level.id IS '安全级别ID';
COMMENT ON COLUMN ont_security_level.level_code IS '级别编码，唯一';
COMMENT ON COLUMN ont_security_level.level_name IS '级别名称';
COMMENT ON COLUMN ont_security_level.level_rank IS '级别排序值，越大越敏感';
COMMENT ON COLUMN ont_security_level.default_view_effect IS '默认查看效果：ALLOW/MASK/DENY';
COMMENT ON COLUMN ont_security_level.default_export_effect IS '默认导出效果：ALLOW/MASK/DENY';
COMMENT ON COLUMN ont_security_level.description IS '描述';
COMMENT ON COLUMN ont_security_level.is_builtin IS '是否内置：1是 0否';
COMMENT ON COLUMN ont_security_level.sort_order IS '排序值';
COMMENT ON COLUMN ont_security_level.del_flag IS '删除标志，0未删除，1已删除';

CREATE INDEX idx_ont_security_level_rank ON ont_security_level (level_rank);

-- ================================================================
-- 2. 工程ACL表 ont_ontology_project_acl
-- ================================================================
CREATE TABLE ont_ontology_project_acl (
  id bigint NOT NULL,
  ontology_id bigint NOT NULL,
  subject_type varchar(16) NOT NULL,
  subject_id bigint NOT NULL,
  access_level varchar(16) NOT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_project_acl_project
    FOREIGN KEY (ontology_id) REFERENCES ont_ontology_project(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_project_acl_subject
    CHECK (subject_type IN ('USER', 'ROLE', 'DEPT')),
  CONSTRAINT ck_ont_project_acl_level
    CHECK (access_level IN ('VIEW', 'EDIT', 'PUBLISH', 'ADMIN')),
  CONSTRAINT ck_ont_project_acl_del CHECK (del_flag IN ('0', '1'))
);

COMMENT ON TABLE ont_ontology_project_acl IS '本体建模-工程ACL表';
COMMENT ON COLUMN ont_ontology_project_acl.ontology_id IS '本体工程ID';
COMMENT ON COLUMN ont_ontology_project_acl.subject_type IS '主体类型：USER/ROLE/DEPT';
COMMENT ON COLUMN ont_ontology_project_acl.subject_id IS '主体ID';
COMMENT ON COLUMN ont_ontology_project_acl.access_level IS '访问级别：VIEW/EDIT/PUBLISH/ADMIN';

CREATE UNIQUE INDEX uk_ont_project_acl_subject
  ON ont_ontology_project_acl (ontology_id, subject_type, subject_id)
  WHERE del_flag = '0';

-- ================================================================
-- 3. 主体安全许可表 ont_security_subject_clearance
-- ================================================================
CREATE TABLE ont_security_subject_clearance (
  id bigint NOT NULL,
  ontology_id bigint NOT NULL,
  subject_type varchar(16) NOT NULL,
  subject_id bigint NOT NULL,
  max_level_code varchar(32) NOT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_clearance_project
    FOREIGN KEY (ontology_id) REFERENCES ont_ontology_project(id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_clearance_level
    FOREIGN KEY (max_level_code) REFERENCES ont_security_level(level_code) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_clearance_subject
    CHECK (subject_type IN ('USER', 'ROLE', 'DEPT')),
  CONSTRAINT ck_ont_clearance_del CHECK (del_flag IN ('0', '1'))
);

COMMENT ON TABLE ont_security_subject_clearance IS '本体建模-主体安全许可表';
COMMENT ON COLUMN ont_security_subject_clearance.max_level_code IS '主体可访问的最高安全级别编码';

CREATE UNIQUE INDEX uk_ont_clearance_subject
  ON ont_security_subject_clearance (ontology_id, subject_type, subject_id)
  WHERE del_flag = '0';

-- ================================================================
-- 4. 数据策略例外表 ont_data_policy_rule
-- ================================================================
CREATE TABLE ont_data_policy_rule (
  id bigint NOT NULL,
  ontology_id bigint NOT NULL,
  subject_type varchar(16) NOT NULL,
  subject_id bigint NOT NULL,
  resource_type varchar(24) NOT NULL,
  resource_id bigint NOT NULL,
  action varchar(16) NOT NULL,
  effect varchar(16) NOT NULL,
  mask_type varchar(32) DEFAULT NULL,
  mask_parameter jsonb DEFAULT NULL,
  description varchar(255) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_policy_project
    FOREIGN KEY (ontology_id) REFERENCES ont_ontology_project(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_policy_subject
    CHECK (subject_type IN ('USER', 'ROLE', 'DEPT')),
  CONSTRAINT ck_ont_policy_resource
    CHECK (resource_type IN ('ENTITY_TYPE', 'DATA_PROPERTY', 'OBJECT_PROPERTY')),
  CONSTRAINT ck_ont_policy_action
    CHECK (action IN ('VIEW', 'EDIT', 'EXPORT', 'SPARQL')),
  CONSTRAINT ck_ont_policy_effect
    CHECK (effect IN ('ALLOW', 'MASK', 'DENY')),
  CONSTRAINT ck_ont_policy_mask
    CHECK ((effect = 'MASK' AND mask_type IS NOT NULL) OR effect <> 'MASK'),
  CONSTRAINT ck_ont_policy_del CHECK (del_flag IN ('0', '1'))
);

COMMENT ON TABLE ont_data_policy_rule IS '本体建模-数据策略例外规则表';
COMMENT ON COLUMN ont_data_policy_rule.resource_type IS '资源类型：ENTITY_TYPE/DATA_PROPERTY/OBJECT_PROPERTY';
COMMENT ON COLUMN ont_data_policy_rule.action IS '动作：VIEW/EDIT/EXPORT/SPARQL';
COMMENT ON COLUMN ont_data_policy_rule.effect IS '效果：ALLOW/MASK/DENY';
COMMENT ON COLUMN ont_data_policy_rule.mask_type IS '脱敏类型，effect=MASK时必填';

CREATE UNIQUE INDEX uk_ont_policy_rule
  ON ont_data_policy_rule
  (ontology_id, subject_type, subject_id, resource_type, resource_id, action)
  WHERE del_flag = '0';

-- ================================================================
-- 5. 数据访问审计表 ont_data_access_log（不做逻辑删除）
-- ================================================================
CREATE TABLE ont_data_access_log (
  id bigint NOT NULL,
  audit_event_id varchar(36) NOT NULL,
  chain_scope varchar(64) NOT NULL,
  chain_seq bigint NOT NULL,
  ontology_id bigint DEFAULT NULL,
  user_id bigint DEFAULT NULL,
  username varchar(64) NOT NULL,
  access_type varchar(32) NOT NULL,
  resource_type varchar(32) NOT NULL,
  resource_ref varchar(512) DEFAULT NULL,
  action_summary varchar(512) DEFAULT NULL,
  result_count bigint NOT NULL DEFAULT 0,
  max_security_level varchar(32) DEFAULT NULL,
  decision varchar(16) NOT NULL,
  outcome varchar(16) NOT NULL,
  error_code varchar(64) DEFAULT NULL,
  trace_id varchar(128) DEFAULT NULL,
  remote_addr varchar(255) DEFAULT NULL,
  user_agent_hash varchar(64) DEFAULT NULL,
  occurred_at timestamp NOT NULL DEFAULT now(),
  prev_hash varchar(64) DEFAULT NULL,
  current_hash varchar(64) NOT NULL,
  create_by varchar(64) DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uk_ont_audit_event UNIQUE (audit_event_id),
  CONSTRAINT fk_ont_audit_project
    FOREIGN KEY (ontology_id) REFERENCES ont_ontology_project(id) ON DELETE SET NULL,
  CONSTRAINT uk_ont_audit_chain_seq UNIQUE (chain_scope, chain_seq),
  CONSTRAINT ck_ont_audit_decision CHECK (decision IN ('ALLOW', 'MASK', 'DENY')),
  CONSTRAINT ck_ont_audit_outcome CHECK (outcome IN ('SUCCESS', 'FAILED'))
);

COMMENT ON TABLE ont_data_access_log IS '本体建模-数据访问审计日志表（不做逻辑删除）';
COMMENT ON COLUMN ont_data_access_log.audit_event_id IS '审计事件唯一ID';
COMMENT ON COLUMN ont_data_access_log.chain_scope IS '哈希链作用域，格式yyyyMMdd:ontologyId';
COMMENT ON COLUMN ont_data_access_log.chain_seq IS '链内序号';
COMMENT ON COLUMN ont_data_access_log.decision IS '策略决策：ALLOW/MASK/DENY';
COMMENT ON COLUMN ont_data_access_log.outcome IS '执行结果：SUCCESS/FAILED';
COMMENT ON COLUMN ont_data_access_log.prev_hash IS '前一条记录哈希';
COMMENT ON COLUMN ont_data_access_log.current_hash IS '当前记录哈希';

CREATE INDEX idx_ont_audit_project_time
  ON ont_data_access_log (ontology_id, occurred_at DESC);
CREATE INDEX idx_ont_audit_user_time
  ON ont_data_access_log (user_id, occurred_at DESC);

-- ================================================================
-- 6. 设备凭证表 ont_device_credential
-- ================================================================
CREATE TABLE ont_device_credential (
  id bigint NOT NULL,
  device_code varchar(128) NOT NULL,
  auth_type varchar(16) NOT NULL,
  token_digest varchar(128) DEFAULT NULL,
  digest_key_version varchar(32) DEFAULT NULL,
  certificate_fingerprint varchar(128) DEFAULT NULL,
  certificate_subject varchar(512) DEFAULT NULL,
  status varchar(16) NOT NULL DEFAULT 'ACTIVE',
  expires_at timestamp DEFAULT NULL,
  last_authenticated_at timestamp DEFAULT NULL,
  last_authenticated_ip varchar(255) DEFAULT NULL,
  failed_count integer NOT NULL DEFAULT 0,
  locked_until timestamp DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT ck_ont_device_auth_type CHECK (auth_type IN ('TOKEN_HASH', 'MTLS')),
  CONSTRAINT ck_ont_device_status CHECK (status IN ('ACTIVE', 'LOCKED', 'REVOKED', 'EXPIRED')),
  CONSTRAINT ck_ont_device_material CHECK (
    (auth_type = 'TOKEN_HASH' AND token_digest IS NOT NULL AND digest_key_version IS NOT NULL)
    OR (auth_type = 'MTLS' AND certificate_fingerprint IS NOT NULL)
  ),
  CONSTRAINT ck_ont_device_del CHECK (del_flag IN ('0', '1'))
);

COMMENT ON TABLE ont_device_credential IS '本体建模-设备凭证元数据表';
COMMENT ON COLUMN ont_device_credential.device_code IS '设备编码';
COMMENT ON COLUMN ont_device_credential.auth_type IS '认证类型：TOKEN_HASH/MTLS';
COMMENT ON COLUMN ont_device_credential.token_digest IS 'Token的HMAC-SHA-256摘要，不存可逆Token';
COMMENT ON COLUMN ont_device_credential.digest_key_version IS '摘要密钥版本';
COMMENT ON COLUMN ont_device_credential.certificate_fingerprint IS '证书指纹（mTLS）';
COMMENT ON COLUMN ont_device_credential.certificate_subject IS '证书主题（mTLS）';
COMMENT ON COLUMN ont_device_credential.status IS '状态：ACTIVE/LOCKED/REVOKED/EXPIRED';

CREATE UNIQUE INDEX uk_ont_device_code
  ON ont_device_credential (device_code)
  WHERE del_flag = '0';

-- ================================================================
-- 7. 高风险审批表 ont_operation_approval_request / action（不做逻辑删除）
-- ================================================================
CREATE TABLE ont_operation_approval_request (
  id bigint NOT NULL,
  request_no varchar(36) NOT NULL,
  operation_type varchar(64) NOT NULL,
  target_ref varchar(512) NOT NULL,
  target_digest varchar(64) NOT NULL,
  payload_digest varchar(64) NOT NULL,
  requested_by bigint NOT NULL,
  required_approvals integer NOT NULL DEFAULT 1,
  status varchar(16) NOT NULL DEFAULT 'PENDING',
  expires_at timestamp NOT NULL,
  executed_at timestamp DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_ont_approval_request_no UNIQUE (request_no),
  CONSTRAINT ck_ont_approval_required CHECK (required_approvals IN (1, 2)),
  CONSTRAINT ck_ont_approval_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED', 'CONSUMED', 'CANCELLED'))
);

COMMENT ON TABLE ont_operation_approval_request IS '本体建模-高风险操作审批请求表（不做逻辑删除）';
COMMENT ON COLUMN ont_operation_approval_request.request_no IS '审批编号';
COMMENT ON COLUMN ont_operation_approval_request.operation_type IS '操作类型';
COMMENT ON COLUMN ont_operation_approval_request.target_digest IS '目标摘要（SHA-256）';
COMMENT ON COLUMN ont_operation_approval_request.payload_digest IS '参数摘要（SHA-256）';
COMMENT ON COLUMN ont_operation_approval_request.required_approvals IS '需要审批人数：1或2';
COMMENT ON COLUMN ont_operation_approval_request.status IS '状态：PENDING/APPROVED/REJECTED/EXPIRED/CONSUMED/CANCELLED';

CREATE TABLE ont_operation_approval_action (
  id bigint NOT NULL,
  request_id bigint NOT NULL,
  approver_id bigint NOT NULL,
  decision varchar(16) NOT NULL,
  comment varchar(512) DEFAULT NULL,
  decided_at timestamp NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_approval_action_request
    FOREIGN KEY (request_id) REFERENCES ont_operation_approval_request(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_approval_decision CHECK (decision IN ('APPROVE', 'REJECT')),
  CONSTRAINT uk_ont_approval_approver UNIQUE (request_id, approver_id)
);

COMMENT ON TABLE ont_operation_approval_action IS '本体建模-审批明细表';
COMMENT ON COLUMN ont_operation_approval_action.approver_id IS '审批人ID';
COMMENT ON COLUMN ont_operation_approval_action.decision IS '审批决定：APPROVE/REJECT';

-- ================================================================
-- 8. 扩展本体 Schema/实例值安全字段
-- ================================================================
ALTER TABLE ont_entity_type
  ADD COLUMN IF NOT EXISTS security_level_code varchar(32) NOT NULL DEFAULT 'INTERNAL';

ALTER TABLE ont_data_property
  ADD COLUMN IF NOT EXISTS security_level_code varchar(32) NOT NULL DEFAULT 'INTERNAL';

ALTER TABLE ont_object_property
  ADD COLUMN IF NOT EXISTS security_level_code varchar(32) NOT NULL DEFAULT 'INTERNAL';

ALTER TABLE ont_entity_instance
  ADD COLUMN IF NOT EXISTS security_level_code varchar(32) DEFAULT NULL;

ALTER TABLE ont_instance_data_value
  ADD COLUMN IF NOT EXISTS security_level_code varchar(32) DEFAULT NULL;

COMMENT ON COLUMN ont_entity_type.security_level_code IS '实体类型安全级别编码，默认INTERNAL';
COMMENT ON COLUMN ont_data_property.security_level_code IS '数据属性安全级别编码，默认INTERNAL';
COMMENT ON COLUMN ont_object_property.security_level_code IS '对象属性安全级别编码，默认INTERNAL';
COMMENT ON COLUMN ont_entity_instance.security_level_code IS '实例安全级别覆盖，NULL继承上级';
COMMENT ON COLUMN ont_instance_data_value.security_level_code IS '属性值安全级别覆盖，NULL继承上级';

-- ================================================================
-- 9. 扩展实例值加密字段
-- ================================================================
ALTER TABLE ont_instance_data_value
  ALTER COLUMN literal_value DROP NOT NULL,
  ADD COLUMN IF NOT EXISTS is_encrypted char(1) NOT NULL DEFAULT '0',
  ADD COLUMN IF NOT EXISTS encrypted_value bytea DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS crypto_key_id varchar(64) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS crypto_iv bytea DEFAULT NULL;

COMMENT ON COLUMN ont_instance_data_value.is_encrypted IS '是否加密：0明文 1密文';
COMMENT ON COLUMN ont_instance_data_value.encrypted_value IS 'AES-GCM密文（含认证标签）';
COMMENT ON COLUMN ont_instance_data_value.crypto_key_id IS '密钥版本ID';
COMMENT ON COLUMN ont_instance_data_value.crypto_iv IS '加密随机IV';

-- 加密一致性约束（明文/密文互斥）
-- 注意：原 literal_value 有 NOT NULL 约束，加密时需先 DROP NOT NULL
ALTER TABLE ont_instance_data_value
  ADD CONSTRAINT ck_ont_instance_value_encryption CHECK (
    (is_encrypted = '0' AND literal_value IS NOT NULL
      AND encrypted_value IS NULL AND crypto_key_id IS NULL AND crypto_iv IS NULL)
    OR
    (is_encrypted = '1' AND literal_value IS NULL
      AND encrypted_value IS NOT NULL AND crypto_key_id IS NOT NULL AND crypto_iv IS NOT NULL)
  );

-- ================================================================
-- 10. 插入四级安全级别种子数据（固定种子ID 995001-995004）
-- ================================================================
INSERT INTO ont_security_level
(id, level_code, level_name, level_rank, default_view_effect,
 default_export_effect, description, is_builtin, sort_order,
 create_by, create_time, update_by, update_time, del_flag)
VALUES
(995001, 'PUBLIC', '公开', 10, 'ALLOW', 'ALLOW', '可公开数据', '1', 1, 'admin', now(), 'admin', now(), '0'),
(995002, 'INTERNAL', '内部', 20, 'ALLOW', 'ALLOW', '工程内部数据', '1', 2, 'admin', now(), 'admin', now(), '0'),
(995003, 'CONFIDENTIAL', '机密', 30, 'MASK', 'DENY', '个人或商业敏感数据', '1', 3, 'admin', now(), 'admin', now(), '0'),
(995004, 'RESTRICTED', '受限', 40, 'DENY', 'DENY', '高敏感数据或控制凭证', '1', 4, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ================================================================
-- 11. 为已有安全级别字段添加外键（种子数据已插入后方可校验）
-- ================================================================
ALTER TABLE ont_entity_type
  ADD CONSTRAINT fk_ont_entity_type_security_level
    FOREIGN KEY (security_level_code) REFERENCES ont_security_level(level_code);

ALTER TABLE ont_data_property
  ADD CONSTRAINT fk_ont_data_property_security_level
    FOREIGN KEY (security_level_code) REFERENCES ont_security_level(level_code);

ALTER TABLE ont_object_property
  ADD CONSTRAINT fk_ont_object_property_security_level
    FOREIGN KEY (security_level_code) REFERENCES ont_security_level(level_code);

-- 实例和属性值的安全级别允许 NULL（继承上级），外键需 NULLABLE
ALTER TABLE ont_entity_instance
  ADD CONSTRAINT fk_ont_entity_instance_security_level
    FOREIGN KEY (security_level_code) REFERENCES ont_security_level(level_code);

ALTER TABLE ont_instance_data_value
  ADD CONSTRAINT fk_ont_instance_value_security_level
    FOREIGN KEY (security_level_code) REFERENCES ont_security_level(level_code);

-- ================================================================
-- 12. 菜单注册（901500段，父菜单900000本体建模）
-- ================================================================
INSERT INTO sys_menu
(menu_id, name, permission, path, component, parent_id, icon, visible,
 sort_order, keep_alive, embedded, menu_type,
 create_by, create_time, update_by, update_time, del_flag)
VALUES
(901500, '安全与合规', NULL, '/ontology/security/index', NULL, 900000,
 'ele-Lock', '1', 15, '0', NULL, '0',
 'admin', now(), 'admin', now(), '0'),
(901501, '安全查看', 'ontology_security_view', NULL, NULL, 901500,
 NULL, '1', 1, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0'),
(901502, '安全管理', 'ontology_security_admin', NULL, NULL, 901500,
 NULL, '1', 2, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0'),
(901503, '审计查看', 'ontology_audit_view', NULL, NULL, 901500,
 NULL, '1', 3, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0'),
(901504, '审计校验', 'ontology_audit_verify', NULL, NULL, 901500,
 NULL, '1', 4, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0'),
(901505, '设备凭证管理', 'ontology_device_credential_manage', NULL, NULL, 901500,
 NULL, '1', 5, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0'),
(901506, '安全审批', 'ontology_security_approve', NULL, NULL, 901500,
 NULL, '1', 6, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

-- ================================================================
-- 13. 角色-菜单映射（仅管理员角色1）
-- ================================================================
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
(1, 901500), (1, 901501), (1, 901502), (1, 901503),
(1, 901504), (1, 901505), (1, 901506)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ================================================================
-- 14. 迁移完整性断言
-- ================================================================
DO $$
BEGIN
  -- 1. 八张表存在
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_security_level') THEN
    RAISE EXCEPTION '安全级别表 ont_security_level 未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_ontology_project_acl') THEN
    RAISE EXCEPTION '工程ACL表 ont_ontology_project_acl 未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_security_subject_clearance') THEN
    RAISE EXCEPTION '主体安全许可表 ont_security_subject_clearance 未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_data_policy_rule') THEN
    RAISE EXCEPTION '数据策略例外表 ont_data_policy_rule 未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_data_access_log') THEN
    RAISE EXCEPTION '审计日志表 ont_data_access_log 未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_device_credential') THEN
    RAISE EXCEPTION '设备凭证表 ont_device_credential 未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_operation_approval_request') THEN
    RAISE EXCEPTION '审批请求表 ont_operation_approval_request 未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_operation_approval_action') THEN
    RAISE EXCEPTION '审批明细表 ont_operation_approval_action 未创建';
  END IF;

  -- 2. 四级安全级别种子存在
  IF (SELECT COUNT(*) FROM ont_security_level WHERE id BETWEEN 995001 AND 995004) != 4 THEN
    RAISE EXCEPTION '四级安全级别种子数据未完整插入';
  END IF;

  -- 3. 安全与合规菜单存在
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901500 AND del_flag = '0') THEN
    RAISE EXCEPTION '安全与合规菜单(901500)未创建';
  END IF;

  -- 4. 按钮权限存在
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901501 AND permission = 'ontology_security_view' AND del_flag = '0') THEN
    RAISE EXCEPTION '安全查看权限(901501)未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901502 AND permission = 'ontology_security_admin' AND del_flag = '0') THEN
    RAISE EXCEPTION '安全管理权限(901502)未创建';
  END IF;

  -- 5. 角色-菜单映射存在
  IF (SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 901500 AND 901506) != 7 THEN
    RAISE EXCEPTION '管理员角色未完整分配安全与合规菜单';
  END IF;

  -- 6. 加密字段扩展验证
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'ont_instance_data_value' AND column_name = 'is_encrypted') THEN
    RAISE EXCEPTION 'ont_instance_data_value 加密字段 is_encrypted 未添加';
  END IF;

  RAISE NOTICE 'V20 安全与合规模块迁移完成';
END $$;
