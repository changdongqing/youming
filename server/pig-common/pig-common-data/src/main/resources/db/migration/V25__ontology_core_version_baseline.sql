-- ============================================================
-- V25: 核心本体工程935001版本基线预置 (18-09模板导入前置条件)
--
-- 背景：
--   OntMappingProjectServiceImpl.create 校验本体工程 current_version_id 非空，
--   否则抛 ONT_MAP_016。V6 创建工程935001时无 current_version_id 列(V18添加)，
--   V18 注释"初始为空"，无脚本回填。OntologyVersionServiceImpl.prepare 还要求
--   ontology_iri 和 version_iri_base 非空。
--   本脚本为模板导入服务扫清前置条件：配置IRI → 插入PUBLISHED版本 → 回填指针。
--
-- 幂等：所有操作使用 ON CONFLICT DO NOTHING 或条件 UPDATE。
-- ============================================================

-- A. 配置本体工程935001的版本IRI基础路径
UPDATE ont_ontology_project
SET ontology_iri     = 'http://example.org/youming/core-ontology',
    version_iri_base = 'http://example.org/youming/core-ontology/version/',
    update_by        = 'admin',
    update_time      = now()
WHERE id = 935001
  AND (ontology_iri IS NULL OR version_iri_base IS NULL);

-- B. 插入PUBLISHED本体版本1.0.0 (基线版本)
INSERT INTO ont_ontology_version (id, ontology_id, version_number, version_iri,
  prior_version_id, compatibility, release_status, release_notes,
  snapshot_format_version, schema_snapshot, snapshot_hash, diff_summary,
  workspace_revision, published_by, published_at,
  create_by, create_time, update_by, update_time, del_flag)
VALUES
  (935101, 935001, '1.0.0', 'http://example.org/youming/core-ontology/version/1.0.0',
   NULL, 'BACKWARD_COMPATIBLE', 'PUBLISHED', '核心本体基线版本（V25脚本预置，含V6核心实体类型及V16消防/V24组织账号扩展模块的初始Schema快照）',
   1, '{}'::jsonb, '44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a',
   '{"firstRelease":true,"compatibility":"BACKWARD_COMPATIBLE"}'::jsonb,
   0, 'admin', now(),
   'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- C. 回填本体工程当前版本指针
UPDATE ont_ontology_project
SET current_version_id = 935101,
    update_by          = 'admin',
    update_time        = now()
WHERE id = 935001
  AND current_version_id IS NULL;

-- D. 完整性断言校验
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM ont_ontology_project
    WHERE id = 935001 AND current_version_id IS NOT NULL
      AND ontology_iri IS NOT NULL AND version_iri_base IS NOT NULL
  ) THEN
    RAISE EXCEPTION 'V25校验失败: 本体工程935001的current_version_id/ontology_iri/version_iri_base未正确配置';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM ont_ontology_version
    WHERE id = 935101 AND ontology_id = 935001 AND release_status = 'PUBLISHED'
  ) THEN
    RAISE EXCEPTION 'V25校验失败: 本体版本935101(1.0.0 PUBLISHED)未正确插入';
  END IF;

  RAISE NOTICE 'V25校验通过: 本体工程935001已配置版本基线1.0.0(id=935101)';
END $$;
