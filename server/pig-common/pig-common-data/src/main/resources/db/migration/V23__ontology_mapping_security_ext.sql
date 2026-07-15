-- ============================================================
-- youming ontology mapping security, audit, EDA & observability (module 18-08)
-- 设计文档: docs/ontology/国标版设计/设计文档/18-08-安全溯源EDA与可观测性功能详细设计.md
--
-- 本迁移仅扩展表结构和种子数据，不修改已应用的V22脚本（checksum 校验）。
-- ============================================================

-- ============================================================
-- 1. ont_mapping_job 增加安全拒绝计数千列 (18-08 §13 指标)
-- ============================================================
ALTER TABLE ont_mapping_job
  ADD COLUMN IF NOT EXISTS security_denied_count bigint NOT NULL DEFAULT 0;

COMMENT ON COLUMN ont_mapping_job.security_denied_count IS '安全策略拒绝记录数（18-08 §13）';

-- ============================================================
-- 2. 高风险审批操作类型注释 (18-08 §10)
--    operation_type 是 varchar(64) 无 CHECK 约束，无需 ALTER。
--    以下操作类型由应用层常量定义，记录在此供运维参考：
--    - MAPPING_PUBLISH_IRI_CHANGE     已有ACTIVE绑定时修改IRI策略
--    - MAPPING_ENABLE_SOFT_DELETE     启用SOFT_DELETE并预计影响超过阈值
--    - MAPPING_RESET_CURSOR           强制重置增量游标
--    - MAPPING_FORCE_DELETE_INSTANCE  批量删除映射实例
--    - MAPPING_CREDENTIAL_ROTATE      RESTRICTED数据源凭证轮换
-- ============================================================

-- ============================================================
-- 3. ont_data_source 增加 credential_rotated_at 列 (18-08 §5 凭证轮换追溯)
-- ============================================================
ALTER TABLE ont_data_source
  ADD COLUMN IF NOT EXISTS credential_rotated_at timestamp DEFAULT NULL;

COMMENT ON COLUMN ont_data_source.credential_rotated_at IS '凭证最近轮换时间（18-08 §5）';

-- ============================================================
-- 4. ont_mapping_project 增加 last_security_denial_at 列 (18-08 §13 安全拒绝指标)
-- ============================================================
ALTER TABLE ont_mapping_project
  ADD COLUMN IF NOT EXISTS last_security_denial_at timestamp DEFAULT NULL;

COMMENT ON COLUMN ont_mapping_project.last_security_denial_at IS '最近安全拒绝时间（18-08 §13）';
