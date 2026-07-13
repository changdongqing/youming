-- =============================================
-- V19: 事件驱动骨干 EDA 模块
-- 设计文档: 26-事件驱动骨干EDA模块详细设计.md
-- Transactional Outbox + Redis Streams + Inbox 幂等
-- =============================================

-- ----------------------------
-- 1. 事件 Outbox 表 ont_event_outbox
-- ----------------------------
CREATE TABLE ont_event_outbox (
  id bigint NOT NULL,
  event_id varchar(36) NOT NULL,
  event_type varchar(96) NOT NULL,
  event_version integer NOT NULL DEFAULT 1,
  ontology_id bigint DEFAULT NULL,
  aggregate_type varchar(64) DEFAULT NULL,
  aggregate_id varchar(256) DEFAULT NULL,
  operation varchar(32) DEFAULT NULL,
  occurred_at timestamp NOT NULL,
  payload jsonb NOT NULL,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  trace_id varchar(128) DEFAULT NULL,
  actor_id bigint DEFAULT NULL,
  status varchar(16) NOT NULL DEFAULT 'PENDING',
  available_at timestamp NOT NULL DEFAULT now(),
  lease_until timestamp DEFAULT NULL,
  locked_by varchar(128) DEFAULT NULL,
  delivery_attempt integer NOT NULL DEFAULT 0,
  stream_record_id varchar(64) DEFAULT NULL,
  published_at timestamp DEFAULT NULL,
  last_error_code varchar(64) DEFAULT NULL,
  last_error_message varchar(512) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT uk_ont_event_outbox_event UNIQUE (event_id),
  CONSTRAINT ck_ont_outbox_status
    CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED')),
  CONSTRAINT ck_ont_outbox_del CHECK (del_flag IN ('0', '1'))
);

CREATE INDEX idx_ont_outbox_poll
  ON ont_event_outbox (status, available_at, create_time)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_outbox_lease
  ON ont_event_outbox (lease_until)
  WHERE del_flag = '0' AND status = 'PROCESSING';
CREATE INDEX idx_ont_outbox_aggregate
  ON ont_event_outbox (aggregate_type, aggregate_id, create_time)
  WHERE del_flag = '0';

COMMENT ON TABLE ont_event_outbox IS '事件 Outbox 表，DB 事务内写入的待投递领域事件';
COMMENT ON COLUMN ont_event_outbox.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN ont_event_outbox.event_id IS '领域事件唯一ID（UUID），重复投递保持不变';
COMMENT ON COLUMN ont_event_outbox.event_type IS '事件类型，如 ONTOLOGY_INSTANCE_CHANGED';
COMMENT ON COLUMN ont_event_outbox.event_version IS '事件契约版本号，默认1';
COMMENT ON COLUMN ont_event_outbox.ontology_id IS '所属本体工程ID';
COMMENT ON COLUMN ont_event_outbox.aggregate_type IS '聚合类型，如 ENTITY_INSTANCE';
COMMENT ON COLUMN ont_event_outbox.aggregate_id IS '聚合ID，字符串兼容雪花ID/IRI';
COMMENT ON COLUMN ont_event_outbox.operation IS '操作类型，如 CREATED/UPDATED/DELETED';
COMMENT ON COLUMN ont_event_outbox.occurred_at IS '事件发生时间（UTC Instant）';
COMMENT ON COLUMN ont_event_outbox.payload IS '事件负载JSONB，仅放消费所需最小信息';
COMMENT ON COLUMN ont_event_outbox.metadata IS '事件元数据JSONB，如 targetConsumerGroup';
COMMENT ON COLUMN ont_event_outbox.trace_id IS '链路追踪ID';
COMMENT ON COLUMN ont_event_outbox.actor_id IS '触发操作的用户ID';
COMMENT ON COLUMN ont_event_outbox.status IS '投递状态：PENDING/PROCESSING/PUBLISHED/FAILED';
COMMENT ON COLUMN ont_event_outbox.available_at IS '可投递时间，用于延迟投递';
COMMENT ON COLUMN ont_event_outbox.lease_until IS '领取租约到期时间';
COMMENT ON COLUMN ont_event_outbox.locked_by IS '当前领取实例标识';
COMMENT ON COLUMN ont_event_outbox.delivery_attempt IS '投递尝试次数';
COMMENT ON COLUMN ont_event_outbox.stream_record_id IS 'Redis Stream 返回的记录ID';
COMMENT ON COLUMN ont_event_outbox.published_at IS '确认 XADD 成功的时间';
COMMENT ON COLUMN ont_event_outbox.last_error_code IS '最近一次投递错误码';
COMMENT ON COLUMN ont_event_outbox.last_error_message IS '最近一次投递错误信息';
COMMENT ON COLUMN ont_event_outbox.del_flag IS '删除标志，0未删除，1已删除';

-- ----------------------------
-- 2. 消费记录表 ont_event_consume_record
-- ----------------------------
CREATE TABLE ont_event_consume_record (
  id bigint NOT NULL,
  consumer_group varchar(128) NOT NULL,
  event_id varchar(36) NOT NULL,
  replay_no integer NOT NULL DEFAULT 0,
  event_type varchar(96) NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'PROCESSING',
  lease_until timestamp DEFAULT NULL,
  consumer_name varchar(128) DEFAULT NULL,
  process_attempt integer NOT NULL DEFAULT 1,
  started_at timestamp NOT NULL DEFAULT now(),
  completed_at timestamp DEFAULT NULL,
  last_error_code varchar(64) DEFAULT NULL,
  last_error_message varchar(512) DEFAULT NULL,
  create_by varchar(64) DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT 'system',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT ck_ont_consume_status
    CHECK (status IN ('PROCESSING', 'SUCCEEDED', 'FAILED')),
  CONSTRAINT ck_ont_consume_del CHECK (del_flag IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_ont_consume_idempotency
  ON ont_event_consume_record (consumer_group, event_id, replay_no)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_consume_lease
  ON ont_event_consume_record (status, lease_until)
  WHERE del_flag = '0';

COMMENT ON TABLE ont_event_consume_record IS '事件消费记录（Inbox），保证重复投递时的业务幂等';
COMMENT ON COLUMN ont_event_consume_record.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN ont_event_consume_record.consumer_group IS '消费者组名称';
COMMENT ON COLUMN ont_event_consume_record.event_id IS '领域事件唯一ID';
COMMENT ON COLUMN ont_event_consume_record.replay_no IS '回放序号，0为原始投递，人工回放递增';
COMMENT ON COLUMN ont_event_consume_record.event_type IS '事件类型';
COMMENT ON COLUMN ont_event_consume_record.status IS '消费状态：PROCESSING/SUCCEEDED/FAILED';
COMMENT ON COLUMN ont_event_consume_record.lease_until IS '消费处理租约到期时间';
COMMENT ON COLUMN ont_event_consume_record.consumer_name IS '当前处理消费者实例名称';
COMMENT ON COLUMN ont_event_consume_record.process_attempt IS '处理尝试次数';
COMMENT ON COLUMN ont_event_consume_record.started_at IS '开始处理时间';
COMMENT ON COLUMN ont_event_consume_record.completed_at IS '完成处理时间';
COMMENT ON COLUMN ont_event_consume_record.last_error_code IS '最近一次处理错误码';
COMMENT ON COLUMN ont_event_consume_record.last_error_message IS '最近一次处理错误信息';
COMMENT ON COLUMN ont_event_consume_record.del_flag IS '删除标志，0未删除，1已删除';

-- ----------------------------
-- 3. 死信表 ont_event_dead_letter
-- ----------------------------
CREATE TABLE ont_event_dead_letter (
  id bigint NOT NULL,
  consumer_group varchar(128) NOT NULL,
  event_id varchar(36) NOT NULL,
  replay_no integer NOT NULL DEFAULT 0,
  event_type varchar(96) NOT NULL,
  stream_record_id varchar(64) NOT NULL,
  payload jsonb NOT NULL,
  failure_category varchar(32) NOT NULL,
  error_code varchar(64) DEFAULT NULL,
  error_message varchar(512) DEFAULT NULL,
  delivery_count integer NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'OPEN',
  replayed_as_no integer DEFAULT NULL,
  first_failed_at timestamp NOT NULL,
  last_failed_at timestamp NOT NULL,
  resolved_at timestamp DEFAULT NULL,
  create_by varchar(64) DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT 'system',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT ck_ont_dlq_status CHECK (status IN ('OPEN', 'REPLAYED', 'RESOLVED', 'IGNORED')),
  CONSTRAINT ck_ont_dlq_del CHECK (del_flag IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_ont_dlq_event_group
  ON ont_event_dead_letter (consumer_group, event_id, replay_no)
  WHERE del_flag = '0';

COMMENT ON TABLE ont_event_dead_letter IS '事件死信表，达到最大重试次数或不可重试错误的死信记录';
COMMENT ON COLUMN ont_event_dead_letter.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN ont_event_dead_letter.consumer_group IS '消费者组名称';
COMMENT ON COLUMN ont_event_dead_letter.event_id IS '领域事件唯一ID';
COMMENT ON COLUMN ont_event_dead_letter.replay_no IS '回放序号';
COMMENT ON COLUMN ont_event_dead_letter.event_type IS '事件类型';
COMMENT ON COLUMN ont_event_dead_letter.stream_record_id IS 'Redis Stream 记录ID';
COMMENT ON COLUMN ont_event_dead_letter.payload IS '事件负载JSONB';
COMMENT ON COLUMN ont_event_dead_letter.failure_category IS '失败分类：RETRYABLE/NON_RETRYABLE';
COMMENT ON COLUMN ont_event_dead_letter.error_code IS '错误码';
COMMENT ON COLUMN ont_event_dead_letter.error_message IS '错误信息';
COMMENT ON COLUMN ont_event_dead_letter.delivery_count IS '投递尝试总次数';
COMMENT ON COLUMN ont_event_dead_letter.status IS '死信状态：OPEN/REPLAYED/RESOLVED/IGNORED';
COMMENT ON COLUMN ont_event_dead_letter.replayed_as_no IS '回放后分配的新 replayNo';
COMMENT ON COLUMN ont_event_dead_letter.first_failed_at IS '首次失败时间';
COMMENT ON COLUMN ont_event_dead_letter.last_failed_at IS '最近失败时间';
COMMENT ON COLUMN ont_event_dead_letter.resolved_at IS '人工处理完成时间';
COMMENT ON COLUMN ont_event_dead_letter.del_flag IS '删除标志，0未删除，1已删除';

-- ----------------------------
-- 4. 回放日志表 ont_event_replay_log
-- ----------------------------
CREATE TABLE ont_event_replay_log (
  id bigint NOT NULL,
  event_id varchar(36) NOT NULL,
  source_replay_no integer NOT NULL,
  target_replay_no integer NOT NULL,
  target_consumer_group varchar(128) NOT NULL,
  reason varchar(512) NOT NULL,
  approval_request_no varchar(36) DEFAULT NULL,
  replayed_by bigint NOT NULL,
  replayed_at timestamp NOT NULL DEFAULT now(),
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT uk_ont_replay_target
    UNIQUE (event_id, target_replay_no, target_consumer_group),
  CONSTRAINT ck_ont_replay_del CHECK (del_flag IN ('0', '1'))
);

COMMENT ON TABLE ont_event_replay_log IS '事件回放日志，记录每次受控回放的来源和目标';
COMMENT ON COLUMN ont_event_replay_log.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN ont_event_replay_log.event_id IS '被回放的领域事件ID';
COMMENT ON COLUMN ont_event_replay_log.source_replay_no IS '源回放序号';
COMMENT ON COLUMN ont_event_replay_log.target_replay_no IS '目标回放序号（新分配）';
COMMENT ON COLUMN ont_event_replay_log.target_consumer_group IS '目标消费者组';
COMMENT ON COLUMN ont_event_replay_log.reason IS '回放原因';
COMMENT ON COLUMN ont_event_replay_log.approval_request_no IS '模块36审批号（批量或高风险回放）';
COMMENT ON COLUMN ont_event_replay_log.replayed_by IS '执行回放的用户ID';
COMMENT ON COLUMN ont_event_replay_log.replayed_at IS '回放执行时间';
COMMENT ON COLUMN ont_event_replay_log.del_flag IS '删除标志，0未删除，1已删除';

-- ----------------------------
-- 5. 菜单：本体建模 / 事件中心
-- ----------------------------
INSERT INTO sys_menu
(menu_id, name, permission, path, component, parent_id, icon, visible,
 sort_order, keep_alive, embedded, menu_type,
 create_by, create_time, update_by, update_time, del_flag)
VALUES
(901400, '事件中心', NULL, '/ontology/event/index', NULL, 900000,
 'ele-Connection', '1', 14, '0', NULL, '0',
 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

-- ----------------------------
-- 6. 按钮权限：查看 / 管理
-- ----------------------------
INSERT INTO sys_menu
(menu_id, name, permission, path, component, parent_id, icon, visible,
 sort_order, keep_alive, embedded, menu_type,
 create_by, create_time, update_by, update_time, del_flag)
VALUES
(901401, '事件查看', 'ontology_event_view', NULL, NULL, 901400,
 NULL, '1', 1, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0'),
(901402, '事件管理', 'ontology_event_admin', NULL, NULL, 901400,
 NULL, '1', 2, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

-- ----------------------------
-- 7. 角色-菜单映射（仅管理员角色1）
-- ----------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES (1, 901400), (1, 901401), (1, 901402)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ----------------------------
-- 8. 迁移完整性断言
-- ----------------------------
DO $$
BEGIN
  -- 1. 四张表存在
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_event_outbox') THEN
    RAISE EXCEPTION 'Outbox 表 ont_event_outbox 未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_event_consume_record') THEN
    RAISE EXCEPTION '消费记录表 ont_event_consume_record 未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_event_dead_letter') THEN
    RAISE EXCEPTION '死信表 ont_event_dead_letter 未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_event_replay_log') THEN
    RAISE EXCEPTION '回放日志表 ont_event_replay_log 未创建';
  END IF;

  -- 2. 事件中心菜单存在
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901400 AND del_flag = '0') THEN
    RAISE EXCEPTION '事件中心菜单(901400)未创建';
  END IF;

  -- 3. 按钮权限存在
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901401 AND permission = 'ontology_event_view' AND del_flag = '0') THEN
    RAISE EXCEPTION '事件查看权限(901401)未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901402 AND permission = 'ontology_event_admin' AND del_flag = '0') THEN
    RAISE EXCEPTION '事件管理权限(901402)未创建';
  END IF;

  -- 4. 角色-菜单映射存在
  IF (SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 901400 AND 901402) != 3 THEN
    RAISE EXCEPTION '管理员角色未完整分配事件中心菜单';
  END IF;

  RAISE NOTICE 'V19 事件驱动骨干EDA模块迁移完成';
END $$;
