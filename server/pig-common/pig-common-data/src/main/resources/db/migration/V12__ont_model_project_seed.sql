-- ============================================================
-- V12: 建模域 - 本体项目管理（FR-10）
-- 建 ont_model_project + ont_model_prefix 两表 + 菜单种子（11000 段）
-- 详见《详细设计-DD7-本体项目管理与命名空间.md》
-- ============================================================

-- ---------- (a) 建表 ----------

CREATE TABLE ont_model_project (
    id                      bigint       NOT NULL,
    project_code            varchar(64)  NOT NULL,
    name                    varchar(128) NOT NULL,
    description             varchar(512),
    namespace_base          varchar(255) NOT NULL,
    default_format          varchar(16)  NOT NULL DEFAULT 'TTL',
    serialization_strategy  char(1)      NOT NULL DEFAULT 'B',
    status                  varchar(16)  NOT NULL DEFAULT 'draft',
    create_by               varchar(64)  DEFAULT ' ',
    create_time             timestamp    DEFAULT now(),
    update_by               varchar(64)  DEFAULT ' ',
    update_time             timestamp    DEFAULT now(),
    del_flag                char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_model_project PRIMARY KEY (id),
    CONSTRAINT uk_ont_model_project_code UNIQUE (project_code)
);
COMMENT ON TABLE  ont_model_project IS '本体项目（建模域顶层容器，FR-10）';
COMMENT ON COLUMN ont_model_project.id                     IS '主键';
COMMENT ON COLUMN ont_model_project.project_code           IS '项目编码（唯一），如 fire-equipment';
COMMENT ON COLUMN ont_model_project.name                   IS '项目名称';
COMMENT ON COLUMN ont_model_project.description            IS '项目描述';
COMMENT ON COLUMN ont_model_project.namespace_base         IS 'IRI 命名空间基址，类/属性 IRI = namespace_base + localName';
COMMENT ON COLUMN ont_model_project.default_format         IS '默认序列化格式：TTL=Turtle / OWL_XML=OWL XML';
COMMENT ON COLUMN ont_model_project.serialization_strategy IS '序列化策略：B=带前缀独立副本(默认,v1) / A=共享单一属性+多domain(预留,v1禁用)';
COMMENT ON COLUMN ont_model_project.status                 IS '项目状态：draft=草稿 / active=活跃 / archived=归档(只读)';
COMMENT ON COLUMN ont_model_project.create_by              IS '创建人';
COMMENT ON COLUMN ont_model_project.create_time            IS '创建时间';
COMMENT ON COLUMN ont_model_project.update_by              IS '修改人';
COMMENT ON COLUMN ont_model_project.update_time            IS '修改时间';
COMMENT ON COLUMN ont_model_project.del_flag               IS '删除标记，0未删除，1已删除';

CREATE TABLE ont_model_prefix (
    id              bigint       NOT NULL,
    project_id      bigint       NOT NULL,
    prefix          varchar(64)  NOT NULL,
    namespace       varchar(255) NOT NULL,
    is_default      char(1)      DEFAULT '0',
    create_by       varchar(64)  DEFAULT ' ',
    create_time     timestamp    DEFAULT now(),
    update_by       varchar(64)  DEFAULT ' ',
    update_time     timestamp    DEFAULT now(),
    del_flag        char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_model_prefix PRIMARY KEY (id),
    CONSTRAINT uk_ont_model_prefix UNIQUE (project_id, prefix)
);
COMMENT ON TABLE  ont_model_prefix IS 'IRI 前缀注册（项目级，FR-10.2）';
COMMENT ON COLUMN ont_model_prefix.id          IS '主键';
COMMENT ON COLUMN ont_model_prefix.project_id  IS '所属项目 ID';
COMMENT ON COLUMN ont_model_prefix.prefix      IS '前缀名，如 ex / qudt / brick（NCName 规范）';
COMMENT ON COLUMN ont_model_prefix.namespace   IS '命名空间 URI，如 http://example.com/onto/';
COMMENT ON COLUMN ont_model_prefix.is_default  IS '1=项目默认前缀（类/属性 IRI 基址来源）';
COMMENT ON COLUMN ont_model_prefix.create_by   IS '创建人';
COMMENT ON COLUMN ont_model_prefix.create_time IS '创建时间';
COMMENT ON COLUMN ont_model_prefix.update_by   IS '修改人';
COMMENT ON COLUMN ont_model_prefix.update_time IS '修改时间';
COMMENT ON COLUMN ont_model_prefix.del_flag    IS '删除标记，0未删除，1已删除';

-- ---------- (b) 索引 ----------

CREATE INDEX idx_ont_model_project_status ON ont_model_project (status) WHERE del_flag = '0';
CREATE INDEX idx_ont_model_prefix_project ON ont_model_prefix (project_id) WHERE del_flag = '0';

-- ---------- (c) sys_menu 菜单种子（11000 段，建模域） ----------
-- 字段位序（17 列）：menu_id / name / permission / path / component / parent_id / icon /
--                  visible / sort_order / keep_alive / embedded / menu_type /
--                  create_by / create_time / update_by / update_time / del_flag
-- 一级目录 parent_id=-1（对齐 V5 的 10000 写法）；菜单 menu_type='0' embedded='0'；
-- 按钮 menu_type='1' embedded=NULL permission=权限串。

-- 本体建模 一级目录（parent_id = -1，与"本体治理"10000 平级）
INSERT INTO sys_menu VALUES (11000, '本体建模', NULL, '/ontology-model', NULL, -1, 'iconfont icon-shujujicheng', '1', 35, '0', '0', '0', 'admin', now(), 'admin', now(), '0');

-- 11100 本体项目管理（菜单，parent_id = 11000）
INSERT INTO sys_menu VALUES (11100, '本体项目管理', NULL, '/admin/ontology-model/project/index', NULL, 11000, 'iconfont icon-xiangmu', '1', 1, '0', '0', '0', 'admin', now(), 'admin', now(), '0');

-- 11101~11104 本体项目管理 权限按钮（menu_type='1'，embedded=NULL）
INSERT INTO sys_menu VALUES (11101, '项目新增', 'ont_project_manage', NULL, NULL, 11100, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11102, '项目编辑', 'ont_project_manage', NULL, NULL, 11100, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11103, '项目删除', 'ont_project_manage', NULL, NULL, 11100, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11104, '项目查看', 'ont_project_view',   NULL, NULL, 11100, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
