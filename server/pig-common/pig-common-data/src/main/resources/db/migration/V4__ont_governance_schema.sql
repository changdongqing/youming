-- ============================================================
-- V4__ont_governance_schema.sql
-- 本体模板化治理功能表结构（8 张表）
-- 对应 PRD v1.2 第九节 9.1~9.8
-- ============================================================

-- 9.1 属性模板（数据属性 + 对象属性，同构）
CREATE TABLE ont_property_template (
    id                  bigint       NOT NULL,
    template_code       varchar(64)  NOT NULL,
    kind                varchar(16)  NOT NULL,
    label               varchar(128) NOT NULL,
    description         varchar(512),
    category            varchar(64),
    type                varchar(32),
    is_identifier       char(1)      DEFAULT '0',
    unit_ref            varchar(255),
    values              text,
    default_cardinality varchar(32),
    source              varchar(16)  NOT NULL DEFAULT 'custom',
    deprecated          char(1)      DEFAULT '0',
    create_by           varchar(64)  DEFAULT ' ',
    create_time         timestamp    DEFAULT now(),
    update_by           varchar(64)  DEFAULT ' ',
    update_time         timestamp    DEFAULT now(),
    del_flag            char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_property_template PRIMARY KEY (id),
    CONSTRAINT uk_ont_prop_tpl_code UNIQUE (template_code)
);
COMMENT ON TABLE  ont_property_template IS '属性模板（数据属性+对象属性同构）';
COMMENT ON COLUMN ont_property_template.template_code       IS '唯一标识，如 name/contains/price';
COMMENT ON COLUMN ont_property_template.kind                IS 'datatype / object';
COMMENT ON COLUMN ont_property_template.category            IS '分组：basic/contact/monetary/temporal/status/containment/attribution';
COMMENT ON COLUMN ont_property_template.type                IS 'datatype 专属：string/integer/decimal/boolean/datetime';
COMMENT ON COLUMN ont_property_template.is_identifier       IS '0/1，datatype 专属标识符';
COMMENT ON COLUMN ont_property_template.unit_ref            IS '预设 QUDT 单位 IRI（FR-3 交汇）';
COMMENT ON COLUMN ont_property_template.values              IS '枚举值（逗号分隔），datatype 专属';
COMMENT ON COLUMN ont_property_template.default_cardinality IS 'object 专属：one-to-many/many-to-one/...';
COMMENT ON COLUMN ont_property_template.source              IS 'builtin / custom';
COMMENT ON COLUMN ont_property_template.deprecated          IS '0/1 弃用标记';

-- 9.2 分类模板（外观+骨架+父子继承+编码，融合表）
CREATE TABLE ont_class_template (
    id                   bigint       NOT NULL,
    template_code        varchar(64)  NOT NULL,
    classification_code  varchar(64)  NOT NULL,
    label                varchar(128) NOT NULL,
    label_cn             varchar(128),
    description          varchar(512),
    parent_id            bigint,
    tree_root            varchar(64)  NOT NULL,
    icon                 varchar(64),
    color                varchar(16),
    inherit_appearance   char(1)      DEFAULT '1',
    source               varchar(16)  NOT NULL DEFAULT 'custom',
    source_ref           varchar(64),
    deprecated           char(1)      DEFAULT '0',
    sort_order           int          DEFAULT 0,
    create_by            varchar(64)  DEFAULT ' ',
    create_time          timestamp    DEFAULT now(),
    update_by            varchar(64)  DEFAULT ' ',
    update_time          timestamp    DEFAULT now(),
    del_flag             char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_class_template PRIMARY KEY (id),
    CONSTRAINT uk_ont_class_tpl_code      UNIQUE (template_code),
    CONSTRAINT uk_ont_class_tpl_cls_code  UNIQUE (classification_code)
);
COMMENT ON TABLE  ont_class_template IS '分类模板（外观+骨架+父子继承+编码）';
COMMENT ON COLUMN ont_class_template.classification_code IS '规范分类编码，如 30-01-01（承载层级，FR-8）';
COMMENT ON COLUMN ont_class_template.parent_id           IS '父分类模板 id，NULL=根节点（FR-2 父子继承）';
COMMENT ON COLUMN ont_class_template.tree_root           IS '所属分类树标识，如 equipment';
COMMENT ON COLUMN ont_class_template.inherit_appearance  IS '0/1 是否继承父外观（默认 1）';
COMMENT ON COLUMN ont_class_template.source_ref          IS '来源本体标识，如 brick（FR-7 导入）';

-- 9.3 分类模板属性/关系引用（结构骨架）
CREATE TABLE ont_class_template_ref (
    id                      bigint      NOT NULL,
    class_template_id       bigint      NOT NULL,
    property_template_code  varchar(64) NOT NULL,
    ref_type                varchar(16) NOT NULL,
    sort_order              int         DEFAULT 0,
    inherit_flag            char(1)     DEFAULT '0',
    create_by               varchar(64) DEFAULT ' ',
    create_time             timestamp   DEFAULT now(),
    update_by               varchar(64) DEFAULT ' ',
    update_time             timestamp   DEFAULT now(),
    del_flag                char(1)     DEFAULT '0',
    CONSTRAINT pk_ont_class_template_ref PRIMARY KEY (id)
);
COMMENT ON TABLE  ont_class_template_ref IS '分类模板属性/关系引用（结构骨架）';
COMMENT ON COLUMN ont_class_template_ref.class_template_id      IS '-> ont_class_template.id';
COMMENT ON COLUMN ont_class_template_ref.property_template_code IS '-> ont_property_template.template_code';
COMMENT ON COLUMN ont_class_template_ref.ref_type               IS 'property / relationship';
COMMENT ON COLUMN ont_class_template_ref.inherit_flag           IS '0/1 继承自父 vs 本节点新增';

-- 9.4 量纲
CREATE TABLE ont_quantity_kind (
    id               bigint       NOT NULL,
    qudt_iri         varchar(255) NOT NULL,
    label            varchar(64)  NOT NULL,
    label_cn         varchar(64),
    dimension_vector varchar(32),
    sort_order       int          DEFAULT 0,
    create_by        varchar(64)  DEFAULT ' ',
    create_time      timestamp    DEFAULT now(),
    update_by        varchar(64)  DEFAULT ' ',
    update_time      timestamp    DEFAULT now(),
    del_flag         char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_quantity_kind PRIMARY KEY (id),
    CONSTRAINT uk_ont_qk_iri UNIQUE (qudt_iri)
);
COMMENT ON TABLE  ont_quantity_kind IS '量纲（引用 QUDT）';
COMMENT ON COLUMN ont_quantity_kind.qudt_iri         IS '如 .../quantitykind/Length';
COMMENT ON COLUMN ont_quantity_kind.dimension_vector IS 'A0E0L1I0M0H0T0D0';

-- 9.5 单位
CREATE TABLE ont_unit (
    id                    bigint       NOT NULL,
    qudt_iri              varchar(255) NOT NULL,
    symbol                varchar(32),
    label                 varchar(64)  NOT NULL,
    label_cn              varchar(64),
    quantity_kind_id      bigint       NOT NULL,
    conversion_multiplier numeric,
    conversion_offset     numeric,
    scaling_of            varchar(255),
    ucum_code             varchar(32),
    source                varchar(16)  NOT NULL DEFAULT 'custom',
    source_ref            varchar(64),
    deprecated            char(1)      DEFAULT '0',
    create_by             varchar(64)  DEFAULT ' ',
    create_time           timestamp    DEFAULT now(),
    update_by             varchar(64)  DEFAULT ' ',
    update_time           timestamp    DEFAULT now(),
    del_flag              char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_unit PRIMARY KEY (id),
    CONSTRAINT uk_ont_unit_iri UNIQUE (qudt_iri)
);
COMMENT ON TABLE  ont_unit IS '单位（引用 QUDT）';
COMMENT ON COLUMN ont_unit.qudt_iri              IS '如 .../unit/KiloGM';
COMMENT ON COLUMN ont_unit.symbol                IS '符号，冗余存储降级用';
COMMENT ON COLUMN ont_unit.conversion_multiplier IS '相对基准换算系数';
COMMENT ON COLUMN ont_unit.conversion_offset     IS '换算偏移（温度等）';
COMMENT ON COLUMN ont_unit.scaling_of            IS '基准单位 qudt_iri';
COMMENT ON COLUMN ont_unit.source_ref            IS '来源本体标识，如 qudt';

-- 9.6 注释属性注册表
CREATE TABLE ont_annotation_property (
    id          bigint       NOT NULL,
    local_name  varchar(64)  NOT NULL,
    label       varchar(128) NOT NULL,
    range_xsd   varchar(64),
    applies_to  varchar(32),
    description varchar(512),
    sort_order  int          DEFAULT 0,
    create_by   varchar(64)  DEFAULT ' ',
    create_time timestamp    DEFAULT now(),
    update_by   varchar(64)  DEFAULT ' ',
    update_time timestamp    DEFAULT now(),
    del_flag    char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_annotation_property PRIMARY KEY (id),
    CONSTRAINT uk_ont_ap_local_name UNIQUE (local_name)
);
COMMENT ON TABLE  ont_annotation_property IS '注释属性注册表';
COMMENT ON COLUMN ont_annotation_property.local_name IS '如 icon/unitRef';
COMMENT ON COLUMN ont_annotation_property.range_xsd  IS 'xsd:string / xsd:boolean';
COMMENT ON COLUMN ont_annotation_property.applies_to IS 'class/datatypeProperty/objectProperty/individual/all';

-- 9.7 分类编码规则
CREATE TABLE ont_classification_rule (
    id          bigint       NOT NULL,
    tree_root   varchar(64)  NOT NULL,
    separator   varchar(4)   DEFAULT '-',
    level_digits int         DEFAULT 2,
    base_number  int         DEFAULT 0,
    zero_pad    char(1)      DEFAULT '1',
    description varchar(512),
    create_by   varchar(64)  DEFAULT ' ',
    create_time timestamp    DEFAULT now(),
    update_by   varchar(64)  DEFAULT ' ',
    update_time timestamp    DEFAULT now(),
    del_flag    char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_classification_rule PRIMARY KEY (id),
    CONSTRAINT uk_ont_cls_rule_tree_root UNIQUE (tree_root)
);
COMMENT ON TABLE  ont_classification_rule IS '分类编码规则（FR-8）';
COMMENT ON COLUMN ont_classification_rule.tree_root     IS '所属分类树标识，对应 ont_class_template.tree_root';
COMMENT ON COLUMN ont_classification_rule.separator     IS '分隔符，默认 -';
COMMENT ON COLUMN ont_classification_rule.level_digits  IS '每级位数，默认 2';
COMMENT ON COLUMN ont_classification_rule.base_number   IS '根级编码基数，如 30';
COMMENT ON COLUMN ont_classification_rule.zero_pad      IS '0/1 是否零填充（默认 1）';

-- 9.8 本体类分类树镜像（subClassOf，只读）
CREATE TABLE ont_class_hierarchy (
    id                   bigint       NOT NULL,
    child_class_iri      varchar(255) NOT NULL,
    parent_class_iri     varchar(255) NOT NULL,
    source_template_ref  varchar(64),
    tree_root            varchar(64),
    sync_status          char(1)      DEFAULT '0',
    sync_time            timestamp,
    create_by            varchar(64)  DEFAULT ' ',
    create_time          timestamp    DEFAULT now(),
    update_by            varchar(64)  DEFAULT ' ',
    update_time          timestamp    DEFAULT now(),
    del_flag             char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_class_hierarchy PRIMARY KEY (id)
);
COMMENT ON TABLE  ont_class_hierarchy IS '本体类分类树镜像（subClassOf，FR-9，只读）';
COMMENT ON COLUMN ont_class_hierarchy.child_class_iri     IS '子类 IRI（建模侧类，只镜像不创建）';
COMMENT ON COLUMN ont_class_hierarchy.parent_class_iri    IS '父类 IRI';
COMMENT ON COLUMN ont_class_hierarchy.source_template_ref IS '溯源：建议来源的分类模板 template_code';
COMMENT ON COLUMN ont_class_hierarchy.sync_status         IS '0=待同步 1=已同步 2=已失效';
COMMENT ON COLUMN ont_class_hierarchy.sync_time           IS '最近同步时间';

-- 索引（高频查询）
CREATE INDEX idx_ont_prop_tpl_kind      ON ont_property_template (kind) WHERE del_flag = '0';
CREATE INDEX idx_ont_class_tpl_parent   ON ont_class_template (parent_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_class_tpl_tree     ON ont_class_template (tree_root) WHERE del_flag = '0';
CREATE INDEX idx_ont_class_tpl_ref_tid  ON ont_class_template_ref (class_template_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_unit_qk            ON ont_unit (quantity_kind_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_hierarchy_child    ON ont_class_hierarchy (child_class_iri) WHERE del_flag = '0';
