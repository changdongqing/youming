-- Quartz 2.3.x 官方 PostgreSQL 建表脚本 (tables_postgres.sql)
-- 来源: Quartz Scheduler 官方 (quartz-2.3.2 quartz-core/.../tables_postgres.sql)
-- 适配 youming: 表前缀 QRTZ_ (与 quartz-config.yml tablePrefix 一致), 全小写表名
--
-- 重要: Quartz 在 PostgreSQL 上必须使用此脚本 (而非从 MySQL 转换), 因为:
--   - IS_DURABLE / IS_NONCONCURRENT / IS_UPDATE_DATA / REQUESTS_RECOVERY 列在 PG 下是 BOOLEAN
--     (MySQL 下是 varchar(1), StdJDBCDelegate 用字符串比较; PostgreSQLDelegate 用布尔比较)
--   - 从 MySQL 直接转换会丢失类型语义, 导致 "operator does not exist: character varying = boolean" 错误
--
-- 配套配置: quartz-config.yml 的 driverDelegateClass = org.quartz.impl.jdbcjobstore.PostgreSQLDelegate

-- drop tables in reverse order
DROP TABLE IF EXISTS qrtz_fired_triggers;
DROP TABLE IF EXISTS qrtz_paused_trigger_grps;
DROP TABLE IF EXISTS qrtz_scheduler_state;
DROP TABLE IF EXISTS qrtz_locks;
DROP TABLE IF EXISTS qrtz_simple_triggers;
DROP TABLE IF EXISTS qrtz_simprop_triggers;
DROP TABLE IF EXISTS qrtz_cron_triggers;
DROP TABLE IF EXISTS qrtz_blob_triggers;
DROP TABLE IF EXISTS qrtz_triggers;
DROP TABLE IF EXISTS qrtz_job_details;
DROP TABLE IF EXISTS qrtz_calendars;

CREATE TABLE qrtz_job_details (
    sched_name varchar(120) NOT NULL,
    job_name varchar(200) NOT NULL,
    job_group varchar(200) NOT NULL,
    description varchar(250) NULL,
    job_class_name varchar(250) NOT NULL,
    is_durable boolean NOT NULL,
    is_nonconcurrent boolean NOT NULL,
    is_update_data boolean NOT NULL,
    requests_recovery boolean NOT NULL,
    job_data bytea NULL,
    PRIMARY KEY (sched_name, job_name, job_group)
);

CREATE TABLE qrtz_triggers (
    sched_name varchar(120) NOT NULL,
    trigger_name varchar(200) NOT NULL,
    trigger_group varchar(200) NOT NULL,
    job_name varchar(200) NOT NULL,
    job_group varchar(200) NOT NULL,
    description varchar(250) NULL,
    next_fire_time bigint NULL,
    prev_fire_time bigint NULL,
    priority integer NULL,
    trigger_state varchar(16) NOT NULL,
    trigger_type varchar(8) NOT NULL,
    start_time bigint NOT NULL,
    end_time bigint NULL,
    calendar_name varchar(200) NULL,
    misfire_instr smallint NULL,
    job_data bytea NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE TABLE qrtz_simple_triggers (
    sched_name varchar(120) NOT NULL,
    trigger_name varchar(200) NOT NULL,
    trigger_group varchar(200) NOT NULL,
    repeat_count bigint NOT NULL,
    repeat_interval bigint NOT NULL,
    times_triggered bigint NOT NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE TABLE qrtz_cron_triggers (
    sched_name varchar(120) NOT NULL,
    trigger_name varchar(200) NOT NULL,
    trigger_group varchar(200) NOT NULL,
    cron_expression varchar(120) NOT NULL,
    time_zone_id varchar(80),
    PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE TABLE qrtz_simprop_triggers (
    sched_name varchar(120) NOT NULL,
    trigger_name varchar(200) NOT NULL,
    trigger_group varchar(200) NOT NULL,
    str_prop_1 varchar(512) NULL,
    str_prop_2 varchar(512) NULL,
    str_prop_3 varchar(512) NULL,
    int_prop_1 int NULL,
    int_prop_2 int NULL,
    long_prop_1 bigint NULL,
    long_prop_2 bigint NULL,
    dec_prop_1 numeric(13,4) NULL,
    dec_prop_2 numeric(13,4) NULL,
    bool_prop_1 boolean NULL,
    bool_prop_2 boolean NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE TABLE qrtz_blob_triggers (
    sched_name varchar(120) NOT NULL,
    trigger_name varchar(200) NOT NULL,
    trigger_group varchar(200) NOT NULL,
    blob_data bytea NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE TABLE qrtz_calendars (
    sched_name varchar(120) NOT NULL,
    calendar_name varchar(200) NOT NULL,
    calendar bytea NOT NULL,
    PRIMARY KEY (sched_name, calendar_name)
);

CREATE TABLE qrtz_paused_trigger_grps (
    sched_name varchar(120) NOT NULL,
    trigger_group varchar(200) NOT NULL,
    PRIMARY KEY (sched_name, trigger_group)
);

CREATE TABLE qrtz_fired_triggers (
    sched_name varchar(120) NOT NULL,
    entry_id varchar(95) NOT NULL,
    trigger_name varchar(200) NOT NULL,
    trigger_group varchar(200) NOT NULL,
    instance_name varchar(200) NOT NULL,
    fired_time bigint NOT NULL,
    sched_time bigint NOT NULL,
    priority integer NOT NULL,
    state varchar(16) NOT NULL,
    job_name varchar(200) NULL,
    job_group varchar(200) NULL,
    is_nonconcurrent boolean NULL,
    requests_recovery boolean NULL,
    PRIMARY KEY (sched_name, entry_id)
);

CREATE TABLE qrtz_scheduler_state (
    sched_name varchar(120) NOT NULL,
    instance_name varchar(200) NOT NULL,
    last_checkin_time bigint NOT NULL,
    checkin_interval bigint NOT NULL,
    PRIMARY KEY (sched_name, instance_name)
);

CREATE TABLE qrtz_locks (
    sched_name varchar(120) NOT NULL,
    lock_name varchar(40) NOT NULL,
    PRIMARY KEY (sched_name, lock_name)
);

CREATE INDEX idx_qrtz_j_req_recovery ON qrtz_job_details(sched_name, requests_recovery);
CREATE INDEX idx_qrtz_j_grp ON qrtz_job_details(sched_name, job_group);
CREATE INDEX idx_qrtz_t_j ON qrtz_triggers(sched_name, job_name, job_group);
CREATE INDEX idx_qrtz_t_jg ON qrtz_triggers(sched_name, job_group);
CREATE INDEX idx_qrtz_t_c ON qrtz_triggers(sched_name, calendar_name);
CREATE INDEX idx_qrtz_t_n_g_state ON qrtz_triggers(sched_name, next_fire_time, trigger_group, trigger_state);
CREATE INDEX idx_qrtz_t_state ON qrtz_triggers(sched_name, trigger_state);
CREATE INDEX idx_qrtz_t_n_state ON qrtz_triggers(sched_name, next_fire_time, trigger_state);
CREATE INDEX idx_qrtz_t_nft_st ON qrtz_triggers(sched_name, trigger_state, next_fire_time);
CREATE INDEX idx_qrtz_ft_inst_job_req_rcvry ON qrtz_fired_triggers(sched_name, instance_name, requests_recovery);
CREATE INDEX idx_qrtz_ft_t_g ON qrtz_fired_triggers(sched_name, trigger_group);
CREATE INDEX idx_qrtz_ft_trig_inst_name ON qrtz_fired_triggers(sched_name, trigger_name, trigger_group, instance_name);
CREATE INDEX idx_qrtz_ft_t_n_g_state ON qrtz_fired_triggers(sched_name, trigger_group, state);
