-- ============================================================
-- V13: 建模域 - 类实体 + 数据属性 + 对象属性（FR-11）
-- 建 ont_model_class + ont_model_datatype_property + ont_model_object_property
-- + 菜单种子（11200 段）
-- 注：数据属性/对象属性表从 DD9/V14 前移至此，因模板实例化需同时建类+属性
-- 详见《详细设计-DD8-类实体创建与模板实例化.md》
-- ============================================================

-- ---------- (a) 建表 ----------

CREATE TABLE ont_model_class (
    id                   bigint       NOT NULL,
    project_id           bigint       NOT NULL,
    class_iri            varchar(255) NOT NULL,
    local_name           varchar(128) NOT NULL,
    label                varchar(128),
    label_cn             varchar(128),
    description          varchar(512),
    template_code        varchar(64),
    classification_code  varchar(64),
    icon                 varchar(64),
    color                varchar(16),
    sort_order           int          DEFAULT 0,
    create_by            varchar(64)  DEFAULT ' ',
    create_time          timestamp    DEFAULT now(),
    update_by            varchar(64)  DEFAULT ' ',
    update_time          timestamp    DEFAULT now(),
    del_flag             char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_model_class PRIMARY KEY (id),
    CONSTRAINT uk_ont_model_class_iri UNIQUE (project_id, class_iri)
);
COMMENT ON TABLE  ont_model_class IS '本体类实体（owl:Class，建模域权威，FR-11）';
COMMENT ON COLUMN ont_model_class.id                  IS '主键';
COMMENT ON COLUMN ont_model_class.project_id          IS '所属项目 ID';
COMMENT ON COLUMN ont_model_class.class_iri           IS '类的 IRI（rdf:about），项目内唯一';
COMMENT ON COLUMN ont_model_class.local_name          IS 'IRI 本地名（class_iri = namespace_base + local_name）';
COMMENT ON COLUMN ont_model_class.label               IS 'rdfs:label';
COMMENT ON COLUMN ont_model_class.label_cn            IS '中文标签';
COMMENT ON COLUMN ont_model_class.description         IS '描述';
COMMENT ON COLUMN ont_model_class.template_code       IS '溯源：来源分类模板 template_code，NULL=非模板创建';
COMMENT ON COLUMN ont_model_class.classification_code IS '溯源：来源分类模板 classification_code，NULL=非模板创建';
COMMENT ON COLUMN ont_model_class.icon                IS '外观：图标（从模板继承或自定义）';
COMMENT ON COLUMN ont_model_class.color               IS '外观：颜色（从模板继承或自定义）';
COMMENT ON COLUMN ont_model_class.sort_order          IS '排序';
COMMENT ON COLUMN ont_model_class.create_by           IS '创建人';
COMMENT ON COLUMN ont_model_class.create_time         IS '创建时间';
COMMENT ON COLUMN ont_model_class.update_by           IS '修改人';
COMMENT ON COLUMN ont_model_class.update_time         IS '修改时间';
COMMENT ON COLUMN ont_model_class.del_flag            IS '删除标记，0未删除，1已删除';

CREATE TABLE ont_model_datatype_property (
    id                  bigint       NOT NULL,
    project_id          bigint       NOT NULL,
    class_id            bigint       NOT NULL,
    property_iri        varchar(255) NOT NULL,
    local_name          varchar(128) NOT NULL,
    label               varchar(128),
    template_code       varchar(64),
    xsd_type            varchar(64)  NOT NULL,
    unit_ref            varchar(255),
    enum_values         text,
    min_cardinality     int          DEFAULT 0,
    max_cardinality     int          DEFAULT -1,
    is_identifier       char(1)      DEFAULT '0',
    sort_order          int          DEFAULT 0,
    create_by           varchar(64)  DEFAULT ' ',
    create_time         timestamp    DEFAULT now(),
    update_by           varchar(64)  DEFAULT ' ',
    update_time         timestamp    DEFAULT now(),
    del_flag            char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_model_dt_prop PRIMARY KEY (id),
    CONSTRAINT uk_ont_model_dt_prop_iri UNIQUE (project_id, property_iri)
);
COMMENT ON TABLE  ont_model_datatype_property IS '数据属性（owl:DatatypeProperty，FR-12，表前移自 DD9）';
COMMENT ON COLUMN ont_model_datatype_property.id              IS '主键';
COMMENT ON COLUMN ont_model_datatype_property.project_id      IS '所属项目 ID';
COMMENT ON COLUMN ont_model_datatype_property.class_id        IS '所属类 ID（domain）';
COMMENT ON COLUMN ont_model_datatype_property.property_iri    IS '属性 IRI（方案B: {classLocalName}_{propLocalName}）';
COMMENT ON COLUMN ont_model_datatype_property.local_name      IS '本地名';
COMMENT ON COLUMN ont_model_datatype_property.label           IS 'rdfs:label';
COMMENT ON COLUMN ont_model_datatype_property.template_code   IS '溯源：来源属性模板 template_code，NULL=手建';
COMMENT ON COLUMN ont_model_datatype_property.xsd_type        IS 'XSD 数据类型，如 xsd:string / xsd:integer';
COMMENT ON COLUMN ont_model_datatype_property.unit_ref        IS '单位引用（QUDT IRI），序列化输出 ont:unitRef';
COMMENT ON COLUMN ont_model_datatype_property.enum_values     IS '枚举值（逗号分隔），序列化输出 owl:oneOf';
COMMENT ON COLUMN ont_model_datatype_property.min_cardinality IS '最小基数';
COMMENT ON COLUMN ont_model_datatype_property.max_cardinality IS '最大基数（-1=无限制）';
COMMENT ON COLUMN ont_model_datatype_property.is_identifier   IS '是否标识符，序列化输出 ont:isIdentifier';
COMMENT ON COLUMN ont_model_datatype_property.sort_order      IS '排序';
COMMENT ON COLUMN ont_model_datatype_property.create_by       IS '创建人';
COMMENT ON COLUMN ont_model_datatype_property.create_time     IS '创建时间';
COMMENT ON COLUMN ont_model_datatype_property.update_by       IS '修改人';
COMMENT ON COLUMN ont_model_datatype_property.update_time     IS '修改时间';
COMMENT ON COLUMN ont_model_datatype_property.del_flag        IS '删除标记，0未删除，1已删除';

CREATE TABLE ont_model_object_property (
    id                  bigint       NOT NULL,
    project_id          bigint       NOT NULL,
    domain_class_id     bigint       NOT NULL,
    range_class_id      bigint,
    property_iri        varchar(255) NOT NULL,
    local_name          varchar(128) NOT NULL,
    label               varchar(128),
    template_code       varchar(64),
    min_cardinality     int          DEFAULT 0,
    max_cardinality     int          DEFAULT -1,
    inverse_of          bigint,
    sort_order          int          DEFAULT 0,
    create_by           varchar(64)  DEFAULT ' ',
    create_time         timestamp    DEFAULT now(),
    update_by           varchar(64)  DEFAULT ' ',
    update_time         timestamp    DEFAULT now(),
    del_flag            char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_model_obj_prop PRIMARY KEY (id),
    CONSTRAINT uk_ont_model_obj_prop_iri UNIQUE (project_id, property_iri)
);
COMMENT ON TABLE  ont_model_object_property IS '对象属性（owl:ObjectProperty，FR-13，表前移自 DD9）';
COMMENT ON COLUMN ont_model_object_property.id              IS '主键';
COMMENT ON COLUMN ont_model_object_property.project_id      IS '所属项目 ID';
COMMENT ON COLUMN ont_model_object_property.domain_class_id IS '域类 ID（domain）';
COMMENT ON COLUMN ont_model_object_property.range_class_id  IS '值域类 ID（range），NULL=待补全（模板实例化的对象属性）';
COMMENT ON COLUMN ont_model_object_property.property_iri    IS '属性 IRI';
COMMENT ON COLUMN ont_model_object_property.local_name      IS '本地名';
COMMENT ON COLUMN ont_model_object_property.label           IS 'rdfs:label';
COMMENT ON COLUMN ont_model_object_property.template_code   IS '溯源：来源属性模板 template_code，NULL=手建';
COMMENT ON COLUMN ont_model_object_property.min_cardinality IS '最小基数';
COMMENT ON COLUMN ont_model_object_property.max_cardinality IS '最大基数（-1=无限制）';
COMMENT ON COLUMN ont_model_object_property.inverse_of      IS '反向属性 ID，可空（owl:inverseOf）';
COMMENT ON COLUMN ont_model_object_property.sort_order      IS '排序';
COMMENT ON COLUMN ont_model_object_property.create_by       IS '创建人';
COMMENT ON COLUMN ont_model_object_property.create_time     IS '创建时间';
COMMENT ON COLUMN ont_model_object_property.update_by       IS '修改人';
COMMENT ON COLUMN ont_model_object_property.update_time     IS '修改时间';
COMMENT ON COLUMN ont_model_object_property.del_flag        IS '删除标记，0未删除，1已删除';

-- ---------- (b) 索引 ----------

CREATE INDEX idx_ont_model_class_project ON ont_model_class (project_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_model_dt_prop_class ON ont_model_datatype_property (class_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_model_obj_prop_dom  ON ont_model_object_property (domain_class_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_model_obj_prop_rng  ON ont_model_object_property (range_class_id) WHERE del_flag = '0';

-- ---------- (c) sys_menu 菜单种子（11200 段） ----------
-- 字段位序（17 列）：menu_id / name / permission / path / component / parent_id / icon /
--                  visible / sort_order / keep_alive / embedded / menu_type /
--                  create_by / create_time / update_by / update_time / del_flag

INSERT INTO sys_menu VALUES (11200, '本体类建模', NULL, '/admin/ontology-model/class/index', NULL, 11000, 'iconfont icon-leixing', '1', 2, '0', '0', '0', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11201, '类新增', 'ont_class_model_manage', NULL, NULL, 11200, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11202, '类编辑', 'ont_class_model_manage', NULL, NULL, 11200, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11203, '类删除', 'ont_class_model_manage', NULL, NULL, 11200, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11204, '类查看', 'ont_class_model_view',   NULL, NULL, 11200, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
