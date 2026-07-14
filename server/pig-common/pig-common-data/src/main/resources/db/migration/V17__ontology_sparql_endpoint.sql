-- =============================================
-- V17: SPARQL查询端点模块
-- 设计文档: 13-SPARQL查询端点模块详细设计.md
-- 本模块为只读RDF投影查询层，不提供SPARQL Update。
-- 仅开放SELECT和ASK，端点默认只授予管理员角色。
-- =============================================

-- ----------------------------
-- 1. 查询日志表 ont_sparql_query_log
-- 兼具"用户查询历史"和"执行审计摘要"功能
-- ----------------------------
CREATE TABLE ont_sparql_query_log (
  id bigint NOT NULL,
  ontology_id bigint NOT NULL,
  query_type varchar(16) NOT NULL,
  query_text text DEFAULT NULL,
  query_preview varchar(2000) NOT NULL,
  query_hash varchar(64) NOT NULL,
  result_format varchar(16) NOT NULL DEFAULT 'JSON',
  row_count integer NOT NULL DEFAULT 0,
  duration_ms bigint NOT NULL DEFAULT 0,
  truncated char(1) NOT NULL DEFAULT '0',
  status varchar(16) NOT NULL,
  error_code varchar(64) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_sparql_log_project
    FOREIGN KEY (ontology_id) REFERENCES ont_ontology_project(id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_sparql_log_type
    CHECK (query_type IN ('SELECT', 'ASK')),
  CONSTRAINT ck_ont_sparql_log_status
    CHECK (status IN ('SUCCESS', 'REJECTED', 'TIMEOUT', 'FAILED')),
  CONSTRAINT ck_ont_sparql_log_truncated
    CHECK (truncated IN ('0', '1')),
  CONSTRAINT ck_ont_sparql_log_del_flag
    CHECK (del_flag IN ('0', '1'))
);

CREATE INDEX idx_ont_sparql_log_project_time
  ON ont_sparql_query_log (ontology_id, create_time DESC)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_sparql_log_creator_time
  ON ont_sparql_query_log (create_by, create_time DESC)
  WHERE del_flag = '0';

COMMENT ON TABLE ont_sparql_query_log IS 'SPARQL查询日志，兼具用户查询历史与执行审计摘要';
COMMENT ON COLUMN ont_sparql_query_log.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN ont_sparql_query_log.ontology_id IS '本体工程ID';
COMMENT ON COLUMN ont_sparql_query_log.query_type IS '查询类型：SELECT/ASK';
COMMENT ON COLUMN ont_sparql_query_log.query_text IS '完整查询文本（默认不落库，需部署显式开启）';
COMMENT ON COLUMN ont_sparql_query_log.query_preview IS '查询预览（去除字符串字面量后的受控摘要，最长2000字符）';
COMMENT ON COLUMN ont_sparql_query_log.query_hash IS '规范化查询文本的SHA-256，用于统计和问题定位';
COMMENT ON COLUMN ont_sparql_query_log.result_format IS '结果格式：JSON/CSV';
COMMENT ON COLUMN ont_sparql_query_log.row_count IS '结果行数';
COMMENT ON COLUMN ont_sparql_query_log.duration_ms IS '执行耗时（毫秒）';
COMMENT ON COLUMN ont_sparql_query_log.truncated IS '结果是否截断：0/1';
COMMENT ON COLUMN ont_sparql_query_log.status IS '执行状态：SUCCESS/REJECTED/TIMEOUT/FAILED';
COMMENT ON COLUMN ont_sparql_query_log.error_code IS '稳定错误码（不含堆栈或令牌）';

-- ----------------------------
-- 2. 菜单：本体建模 / SPARQL查询
-- ----------------------------
INSERT INTO sys_menu
(menu_id, name, permission, path, component, parent_id, icon, visible,
 sort_order, keep_alive, embedded, menu_type,
 create_by, create_time, update_by, update_time, del_flag)
VALUES
(901200, 'SPARQL查询', NULL, '/ontology/sparql/index', NULL, 900000,
 'ele-Search', '1', 12, '0', NULL, '0',
 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

-- ----------------------------
-- 3. 按钮权限：查看 / 执行 / 导出
-- ----------------------------
INSERT INTO sys_menu
(menu_id, name, permission, path, component, parent_id, icon, visible,
 sort_order, keep_alive, embedded, menu_type,
 create_by, create_time, update_by, update_time, del_flag)
VALUES
(901201, 'SPARQL查看', 'ontology_sparql_view', NULL, NULL, 901200,
 NULL, '1', 1, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0'),
(901202, 'SPARQL执行', 'ontology_sparql_query', NULL, NULL, 901200,
 NULL, '1', 2, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0'),
(901203, 'SPARQL导出', 'ontology_sparql_export', NULL, NULL, 901200,
 NULL, '1', 3, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

-- ----------------------------
-- 4. 角色-菜单映射（仅管理员角色1）
-- ----------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
  (1, 901200), (1, 901201), (1, 901202), (1, 901203)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ----------------------------
-- 5. 迁移完整性断言
-- ----------------------------
DO $$
BEGIN
  -- 1. 查询日志表存在
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_sparql_query_log') THEN
    RAISE EXCEPTION '查询日志表 ont_sparql_query_log 未创建';
  END IF;

  -- 2. SPARQL菜单存在
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901200 AND del_flag = '0') THEN
    RAISE EXCEPTION 'SPARQL查询菜单(901200)未创建';
  END IF;

  -- 3. 按钮权限存在
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901201 AND permission = 'ontology_sparql_view' AND del_flag = '0') THEN
    RAISE EXCEPTION 'SPARQL查看权限(901201)未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901202 AND permission = 'ontology_sparql_query' AND del_flag = '0') THEN
    RAISE EXCEPTION 'SPARQL执行权限(901202)未创建';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901203 AND permission = 'ontology_sparql_export' AND del_flag = '0') THEN
    RAISE EXCEPTION 'SPARQL导出权限(901203)未创建';
  END IF;

  -- 4. 角色-菜单映射存在
  IF NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 901200) THEN
    RAISE EXCEPTION '管理员角色未分配SPARQL查询菜单';
  END IF;

  -- 5. 菜单数量校验
  IF (SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 901200 AND 901203 AND del_flag = '0') != 4 THEN
    RAISE EXCEPTION 'SPARQL菜单数量不正确，期望4条';
  END IF;

  RAISE NOTICE 'V17 SPARQL查询端点模块迁移完成';
END $$;
