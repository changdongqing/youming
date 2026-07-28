-- ============================================================
-- V14: 建模域 - 类层级关系（FR-14）
-- 建 ont_model_subclassof（含 retry_count）+ 菜单种子（11300 段）
-- 注：range_class_id DD8 V13 已建为可空，无需 ALTER（评审修订 P-2）
-- 详见《详细设计-DD9-属性与关系建模与类层级回推.md》
-- ============================================================

-- ---------- (a) 建表 ----------

CREATE TABLE ont_model_subclassof (
    id                   bigint       NOT NULL,
    project_id           bigint       NOT NULL,
    child_class_id       bigint       NOT NULL,
    parent_class_id      bigint       NOT NULL,
    source_template_ref  varchar(64),
    sync_status          char(1)      DEFAULT '0',
    sync_time            timestamp,
    retry_count          int          DEFAULT 0,
    create_by            varchar(64)  DEFAULT ' ',
    create_time          timestamp    DEFAULT now(),
    update_by            varchar(64)  DEFAULT ' ',
    update_time          timestamp    DEFAULT now(),
    del_flag             char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_model_subclassof PRIMARY KEY (id),
    CONSTRAINT uk_ont_model_subclassof UNIQUE (project_id, child_class_id, parent_class_id)
);
COMMENT ON TABLE  ont_model_subclassof IS '类层级关系（rdfs:subClassOf，建模域权威源，FR-14）';
COMMENT ON COLUMN ont_model_subclassof.id                  IS '主键';
COMMENT ON COLUMN ont_model_subclassof.project_id          IS '所属项目 ID';
COMMENT ON COLUMN ont_model_subclassof.child_class_id      IS '子类 ID';
COMMENT ON COLUMN ont_model_subclassof.parent_class_id     IS '父类 ID';
COMMENT ON COLUMN ont_model_subclassof.source_template_ref IS '溯源：建议来源的分类模板 template_code';
COMMENT ON COLUMN ont_model_subclassof.sync_status         IS '0=待同步 1=已同步 2=已失效 3=同步失败(重试耗尽)';
COMMENT ON COLUMN ont_model_subclassof.sync_time           IS '最近镜像回推时间';
COMMENT ON COLUMN ont_model_subclassof.retry_count         IS '镜像回推重试次数（上限3，AC-14.5）';
COMMENT ON COLUMN ont_model_subclassof.create_by           IS '创建人';
COMMENT ON COLUMN ont_model_subclassof.create_time         IS '创建时间';
COMMENT ON COLUMN ont_model_subclassof.update_by           IS '修改人';
COMMENT ON COLUMN ont_model_subclassof.update_time         IS '修改时间';
COMMENT ON COLUMN ont_model_subclassof.del_flag            IS '删除标记，0未删除，1已删除';

-- ---------- (b) 索引 ----------

CREATE INDEX idx_ont_model_subclass_child  ON ont_model_subclassof (child_class_id)  WHERE del_flag = '0';
CREATE INDEX idx_ont_model_subclass_parent ON ont_model_subclassof (parent_class_id) WHERE del_flag = '0';

-- ---------- (c) sys_menu 菜单种子（11300 段） ----------
-- 字段位序（17 列）：menu_id / name / permission / path / component / parent_id / icon /
--                  visible / sort_order / keep_alive / embedded / menu_type /
--                  create_by / create_time / update_by / update_time / del_flag

INSERT INTO sys_menu VALUES (11300, '本体属性建模', NULL, '/admin/ontology-model/property/index', NULL, 11000, 'iconfont icon-shuxing', '1', 3, '0', '0', '0', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11301, '属性新增', 'ont_prop_model_manage', NULL, NULL, 11300, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11302, '属性编辑', 'ont_prop_model_manage', NULL, NULL, 11300, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11303, '属性删除', 'ont_prop_model_manage', NULL, NULL, 11300, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11304, '属性查看', 'ont_prop_model_view',   NULL, NULL, 11300, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
