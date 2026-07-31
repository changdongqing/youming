-- ============================================================
-- V24__qwenpaw_sync_schema.sql
-- QwenPaw 同步业务表结构（4 张表）
-- 对应《qwenpaw-adaptation.md》§四 数据表设计
-- 风格对齐 V19__rm_schema.sql（bigint 雪花ID / text存JSON / timestamp无时区 / 5审计列）
-- ============================================================

-- ---------- 1. 会话记忆表（阶段2，P1） ----------
CREATE TABLE qwenpaw_conversation_history (
    seq           bigint       NOT NULL,
    session_id    varchar(255) NOT NULL,
    agent_id      varchar(255),
    kind          varchar(50)  NOT NULL,
    role          varchar(50),
    name          varchar(255),
    content       text,
    tool_call_id  varchar(255),
    tool_input    text,
    tool_state    varchar(50),
    headline      varchar(500),
    blocks        text,
    metadata      text,
    created_at    timestamp,
    dedup_key     varchar(255),
    user_id       varchar(255) NOT NULL,
    client_seq    bigint,
    create_by     varchar(64)  DEFAULT ' ',
    create_time   timestamp    DEFAULT now(),
    update_by     varchar(64)  DEFAULT ' ',
    update_time   timestamp    DEFAULT now(),
    del_flag      char(1)      DEFAULT '0',
    CONSTRAINT pk_qwenpaw_ch PRIMARY KEY (seq),
    CONSTRAINT uk_qwenpaw_ch_dedup UNIQUE (session_id, dedup_key, user_id)
);
CREATE INDEX idx_qwenpaw_ch_user_seq ON qwenpaw_conversation_history (user_id, seq);
CREATE INDEX idx_qwenpaw_ch_session  ON qwenpaw_conversation_history (session_id);

COMMENT ON TABLE  qwenpaw_conversation_history IS 'QwenPaw 会话记忆（增量同步母版，按 user_id 隔离）';
COMMENT ON COLUMN qwenpaw_conversation_history.seq          IS '服务端全局自增（雪花ID），客户端用此做增量游标';
COMMENT ON COLUMN qwenpaw_conversation_history.session_id   IS '会话ID';
COMMENT ON COLUMN qwenpaw_conversation_history.kind         IS '记录类型 model_turn | context_msg | tool_result';
COMMENT ON COLUMN qwenpaw_conversation_history.content      IS '消息文本内容';
COMMENT ON COLUMN qwenpaw_conversation_history.tool_input   IS '工具调用入参（JSON 文本）';
COMMENT ON COLUMN qwenpaw_conversation_history.blocks       IS '富文本块（JSON 文本）';
COMMENT ON COLUMN qwenpaw_conversation_history.metadata     IS '扩展元数据（JSON 文本）';
COMMENT ON COLUMN qwenpaw_conversation_history.dedup_key    IS '去重键，与 session_id+user_id 组成唯一约束做幂等上行';
COMMENT ON COLUMN qwenpaw_conversation_history.user_id      IS '所属用户ID（来自 pig SecurityUtils.getUser().getId()）';
COMMENT ON COLUMN qwenpaw_conversation_history.client_seq   IS '客户端原始 seq（可选，便于对账）';

-- ---------- 2. 企业凭据表（阶段3，P2） ----------
CREATE TABLE qwenpaw_enterprise_credentials (
    id           bigint       NOT NULL,
    ref          varchar(255) NOT NULL,
    kind         varchar(50)  NOT NULL DEFAULT 'static',
    public_data  text         DEFAULT '{}',
    secret_data  text         DEFAULT '{}',
    meta_data    text         DEFAULT '{}',
    user_id      varchar(255),
    create_by    varchar(64)  DEFAULT ' ',
    create_time  timestamp    DEFAULT now(),
    update_by    varchar(64)  DEFAULT ' ',
    update_time  timestamp    DEFAULT now(),
    del_flag     char(1)      DEFAULT '0',
    CONSTRAINT pk_qwenpaw_ec PRIMARY KEY (id),
    CONSTRAINT uk_qwenpaw_ec_ref UNIQUE (ref, user_id)
);
CREATE INDEX idx_qwenpaw_ec_user ON qwenpaw_enterprise_credentials (user_id) WHERE del_flag = '0';

COMMENT ON TABLE  qwenpaw_enterprise_credentials IS 'QwenPaw 企业凭据（secret_data 加密存储，返回时解密）';
COMMENT ON COLUMN qwenpaw_enterprise_credentials.ref          IS '凭据引用名（如 openai_api_key）';
COMMENT ON COLUMN qwenpaw_enterprise_credentials.kind         IS '凭据类型 static | oauth 等';
COMMENT ON COLUMN qwenpaw_enterprise_credentials.public_data  IS '公开信息（JSON 文本，如 provider/base_url）';
COMMENT ON COLUMN qwenpaw_enterprise_credentials.secret_data  IS '加密的 secrets（JSON 文本，服务端 Jasypt 加密）';
COMMENT ON COLUMN qwenpaw_enterprise_credentials.meta_data    IS '元数据（JSON 文本，如 managed_by/updated_at）';
COMMENT ON COLUMN qwenpaw_enterprise_credentials.user_id      IS '所属用户ID（NULL = 全员可用）';

-- ---------- 3. 企业技能表（阶段4，P3） ----------
CREATE TABLE qwenpaw_enterprise_skills (
    id                 bigint       NOT NULL,
    name               varchar(255) NOT NULL,
    version            varchar(50),
    md_hash            varchar(255),
    enterprise_version integer      NOT NULL DEFAULT 1,
    description        text,
    storage_path       varchar(500) NOT NULL,
    create_by          varchar(64)  DEFAULT ' ',
    create_time        timestamp    DEFAULT now(),
    update_by          varchar(64)  DEFAULT ' ',
    update_time        timestamp    DEFAULT now(),
    del_flag           char(1)      DEFAULT '0',
    CONSTRAINT pk_qwenpaw_es PRIMARY KEY (id),
    CONSTRAINT uk_qwenpaw_es_name UNIQUE (name)
);
CREATE INDEX idx_qwenpaw_es_name ON qwenpaw_enterprise_skills (name) WHERE del_flag = '0';

COMMENT ON TABLE  qwenpaw_enterprise_skills IS 'QwenPaw 企业技能（zip 包存对象存储，库只存元数据+路径）';
COMMENT ON COLUMN qwenpaw_enterprise_skills.name               IS '技能名（唯一）';
COMMENT ON COLUMN qwenpaw_enterprise_skills.version            IS '技能自身版本（如 2.1.0）';
COMMENT ON COLUMN qwenpaw_enterprise_skills.md_hash            IS '技能内容哈希（sha256:xxx）';
COMMENT ON COLUMN qwenpaw_enterprise_skills.enterprise_version IS '企业分发版本号，每次更新递增（增量同步依据）';
COMMENT ON COLUMN qwenpaw_enterprise_skills.storage_path       IS '对象存储路径（FileTemplate 的 dir/filename）';

-- ---------- 4. 个人技能上传备份表（阶段4，P3 可选） ----------
CREATE TABLE qwenpaw_personal_skills (
    id           bigint       NOT NULL,
    name         varchar(255) NOT NULL,
    version      varchar(50),
    md_hash      varchar(255),
    storage_path varchar(500) NOT NULL,
    user_id      varchar(255) NOT NULL,
    create_by    varchar(64)  DEFAULT ' ',
    create_time  timestamp    DEFAULT now(),
    update_by    varchar(64)  DEFAULT ' ',
    update_time  timestamp    DEFAULT now(),
    del_flag     char(1)      DEFAULT '0',
    CONSTRAINT pk_qwenpaw_ps PRIMARY KEY (id),
    CONSTRAINT uk_qwenpaw_ps_name_user UNIQUE (name, user_id)
);
CREATE INDEX idx_qwenpaw_ps_user ON qwenpaw_personal_skills (user_id) WHERE del_flag = '0';

COMMENT ON TABLE  qwenpaw_personal_skills IS 'QwenPaw 个人技能上传备份（按 user_id 隔离）';
COMMENT ON COLUMN qwenpaw_personal_skills.name         IS '技能名';
COMMENT ON COLUMN qwenpaw_personal_skills.storage_path IS '对象存储路径';
COMMENT ON COLUMN qwenpaw_personal_skills.user_id      IS '上传者用户ID';

-- ---------- 5. MCP 模板表（阶段5，P4） ----------
CREATE TABLE qwenpaw_mcp_templates (
    id              bigint       NOT NULL,
    name            varchar(255) NOT NULL,
    endpoint        varchar(500) NOT NULL,
    description     text,
    version         varchar(50)  NOT NULL,
    protocol        varchar(50)  DEFAULT 'mcp',
    transport       varchar(50)  DEFAULT 'sse',
    credential_ref  varchar(255),
    default_policy  varchar(50)  DEFAULT 'ask',
    capabilities    text         DEFAULT '[]',
    create_by       varchar(64)  DEFAULT ' ',
    create_time     timestamp    DEFAULT now(),
    update_by       varchar(64)  DEFAULT ' ',
    update_time     timestamp    DEFAULT now(),
    del_flag        char(1)      DEFAULT '0',
    CONSTRAINT pk_qwenpaw_mt PRIMARY KEY (id),
    CONSTRAINT uk_qwenpaw_mt_name UNIQUE (name)
);

COMMENT ON TABLE  qwenpaw_mcp_templates IS 'QwenPaw 企业 MCP 模板（只读拉取，含接入地址/默认策略）';
COMMENT ON COLUMN qwenpaw_mcp_templates.name            IS '模板名（唯一）';
COMMENT ON COLUMN qwenpaw_mcp_templates.endpoint        IS 'MCP 服务接入地址';
COMMENT ON COLUMN qwenpaw_mcp_templates.credential_ref  IS '引用的企业凭据 ref（-> qwenpaw_enterprise_credentials.ref）';
COMMENT ON COLUMN qwenpaw_mcp_templates.default_policy  IS '默认策略 ask | allow | deny';
COMMENT ON COLUMN qwenpaw_mcp_templates.capabilities    IS '能力清单（JSON 文本）';
