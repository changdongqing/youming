-- ----------------------------
-- youming ontology axiom rule management
-- 依据：GB/T 48000.3—2026 第8章（公理与规则）+ PRD §4.5单位一致性派生规则
-- ----------------------------

-- 公理规则主表
CREATE TABLE ont_axiom_rule (
  id bigint NOT NULL,
  rule_code varchar(64) NOT NULL,
  name varchar(128) NOT NULL,
  category varchar(32) NOT NULL,
  sub_type varchar(64) NOT NULL,
  description varchar(512) DEFAULT NULL,
  template_code varchar(64) NOT NULL,
  template_version integer NOT NULL DEFAULT 1,
  formalization_mode varchar(32) NOT NULL,
  validation_mode varchar(32) NOT NULL,
  executor_code varchar(64) DEFAULT NULL,
  config_json jsonb NOT NULL DEFAULT '{}'::jsonb,
  owl_axiom text DEFAULT NULL,
  shacl_shape text DEFAULT NULL,
  status varchar(16) NOT NULL,
  is_enabled char(1) NOT NULL DEFAULT '0',
  severity varchar(16) NOT NULL,
  source_type varchar(32) NOT NULL,
  source_reference varchar(128) DEFAULT NULL,
  blocked_reason varchar(255) DEFAULT NULL,
  is_builtin char(1) NOT NULL DEFAULT '0',
  ontology_id bigint NOT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  remarks varchar(255) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_axiom_rule_ontology FOREIGN KEY (ontology_id) REFERENCES ont_ontology_project (id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_axiom_rule_category CHECK (category IN ('ENTITY_TYPE', 'PROPERTY', 'RELATION')),
  CONSTRAINT ck_ont_axiom_rule_formalization_mode CHECK (formalization_mode IN ('SCHEMA_DERIVED', 'GENERATED', 'CUSTOM_DRAFT')),
  CONSTRAINT ck_ont_axiom_rule_validation_mode CHECK (validation_mode IN ('NONE', 'OWL_CONSISTENCY', 'SHACL_CORE', 'SHACL_SPARQL', 'APPLICATION', 'COMPOSITE')),
  CONSTRAINT ck_ont_axiom_rule_status CHECK (status IN ('ACTIVE', 'DRAFT', 'BLOCKED')),
  CONSTRAINT ck_ont_axiom_rule_severity CHECK (severity IN ('VIOLATION', 'WARNING', 'INFO')),
  CONSTRAINT ck_ont_axiom_rule_source_type CHECK (source_type IN ('GB_CLAUSE_8', 'PRD_DERIVED', 'EXTENSION')),
  CONSTRAINT ck_ont_axiom_rule_is_enabled CHECK (is_enabled IN ('0', '1')),
  CONSTRAINT ck_ont_axiom_rule_builtin CHECK (is_builtin IN ('0', '1')),
  CONSTRAINT ck_ont_axiom_rule_del_flag CHECK (del_flag IN ('0', '1')),
  CONSTRAINT ck_ont_axiom_rule_sort_order CHECK (sort_order >= 0),
  CONSTRAINT ck_ont_axiom_rule_template_version CHECK (template_version >= 1),
  CONSTRAINT ck_ont_axiom_rule_source_builtin CHECK (
    (source_type = 'EXTENSION' AND is_builtin = '0') OR
    (source_type <> 'EXTENSION' AND is_builtin = '1')
  ),
  CONSTRAINT ck_ont_axiom_rule_status_enabled CHECK (
    status = 'ACTIVE' OR is_enabled = '0'
  ),
  CONSTRAINT ck_ont_axiom_rule_active_executor CHECK (
    status <> 'ACTIVE' OR executor_code IS NOT NULL
  ),
  CONSTRAINT ck_ont_axiom_rule_blocked_reason CHECK (
    status <> 'BLOCKED' OR blocked_reason IS NOT NULL
  )
);

COMMENT ON TABLE ont_axiom_rule IS '本体建模-公理规则表';
COMMENT ON COLUMN ont_axiom_rule.id IS '公理规则ID';
COMMENT ON COLUMN ont_axiom_rule.rule_code IS '稳定机器代码';
COMMENT ON COLUMN ont_axiom_rule.name IS '显示名称';
COMMENT ON COLUMN ont_axiom_rule.category IS '规则类别：ENTITY_TYPE/PROPERTY/RELATION';
COMMENT ON COLUMN ont_axiom_rule.sub_type IS '规则子类型';
COMMENT ON COLUMN ont_axiom_rule.description IS '规则说明';
COMMENT ON COLUMN ont_axiom_rule.template_code IS '形式化/表单模板代码';
COMMENT ON COLUMN ont_axiom_rule.template_version IS '模板版本';
COMMENT ON COLUMN ont_axiom_rule.formalization_mode IS '形式化模式：SCHEMA_DERIVED/GENERATED/CUSTOM_DRAFT';
COMMENT ON COLUMN ont_axiom_rule.validation_mode IS '验证机制：NONE/OWL_CONSISTENCY/SHACL_CORE/SHACL_SPARQL/APPLICATION/COMPOSITE';
COMMENT ON COLUMN ont_axiom_rule.executor_code IS '稳定执行器代码，ACTIVE规则必填';
COMMENT ON COLUMN ont_axiom_rule.config_json IS '结构化参数JSON';
COMMENT ON COLUMN ont_axiom_rule.owl_axiom IS 'OWL形式化预览快照';
COMMENT ON COLUMN ont_axiom_rule.shacl_shape IS 'SHACL约束预览快照';
COMMENT ON COLUMN ont_axiom_rule.status IS '规则状态：ACTIVE/DRAFT/BLOCKED';
COMMENT ON COLUMN ont_axiom_rule.is_enabled IS '是否启用，1是0否';
COMMENT ON COLUMN ont_axiom_rule.severity IS '严重级别：VIOLATION/WARNING/INFO';
COMMENT ON COLUMN ont_axiom_rule.source_type IS '来源类型：GB_CLAUSE_8/PRD_DERIVED/EXTENSION';
COMMENT ON COLUMN ont_axiom_rule.source_reference IS '来源引用';
COMMENT ON COLUMN ont_axiom_rule.blocked_reason IS 'BLOCKED时必填的阻塞原因';
COMMENT ON COLUMN ont_axiom_rule.is_builtin IS '是否内置，1是0否';
COMMENT ON COLUMN ont_axiom_rule.ontology_id IS '本体工程ID';
COMMENT ON COLUMN ont_axiom_rule.sort_order IS '排序值';
COMMENT ON COLUMN ont_axiom_rule.remarks IS '治理备注';
COMMENT ON COLUMN ont_axiom_rule.create_by IS '创建人';
COMMENT ON COLUMN ont_axiom_rule.create_time IS '创建时间';
COMMENT ON COLUMN ont_axiom_rule.update_by IS '修改人';
COMMENT ON COLUMN ont_axiom_rule.update_time IS '更新时间';
COMMENT ON COLUMN ont_axiom_rule.del_flag IS '删除标志，0未删除，1已删除';

CREATE UNIQUE INDEX uk_ont_axiom_rule_code ON ont_axiom_rule (ontology_id, rule_code) WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_axiom_rule_name ON ont_axiom_rule (ontology_id, name) WHERE del_flag = '0';
CREATE INDEX idx_ont_axiom_rule_query ON ont_axiom_rule (ontology_id, category, status, is_enabled) WHERE del_flag = '0';

-- 规则目标绑定表
CREATE TABLE ont_axiom_rule_target (
  id bigint NOT NULL,
  axiom_rule_id bigint NOT NULL,
  binding_role varchar(64) NOT NULL,
  binding_order integer NOT NULL DEFAULT 0,
  target_type varchar(32) NOT NULL,
  entity_type_id bigint DEFAULT NULL,
  data_property_id bigint DEFAULT NULL,
  object_property_id bigint DEFAULT NULL,
  unit_category_id bigint DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_axiom_target_rule FOREIGN KEY (axiom_rule_id) REFERENCES ont_axiom_rule (id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_axiom_target_entity FOREIGN KEY (entity_type_id) REFERENCES ont_entity_type (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_axiom_target_data_prop FOREIGN KEY (data_property_id) REFERENCES ont_data_property (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_axiom_target_obj_prop FOREIGN KEY (object_property_id) REFERENCES ont_object_property (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_axiom_target_unit_cat FOREIGN KEY (unit_category_id) REFERENCES ont_unit_category (id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_axiom_target_type CHECK (target_type IN ('ENTITY_TYPE', 'DATA_PROPERTY', 'OBJECT_PROPERTY', 'UNIT_CATEGORY')),
  CONSTRAINT ck_ont_axiom_target_binding_order CHECK (binding_order >= 0),
  CONSTRAINT ck_ont_axiom_target_exactly_one CHECK (
    (CASE WHEN entity_type_id IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN data_property_id IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN object_property_id IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN unit_category_id IS NOT NULL THEN 1 ELSE 0 END) = 1
  ),
  CONSTRAINT ck_ont_axiom_target_type_match CHECK (
    (target_type = 'ENTITY_TYPE' AND entity_type_id IS NOT NULL AND data_property_id IS NULL AND object_property_id IS NULL AND unit_category_id IS NULL) OR
    (target_type = 'DATA_PROPERTY' AND data_property_id IS NOT NULL AND entity_type_id IS NULL AND object_property_id IS NULL AND unit_category_id IS NULL) OR
    (target_type = 'OBJECT_PROPERTY' AND object_property_id IS NOT NULL AND entity_type_id IS NULL AND data_property_id IS NULL AND unit_category_id IS NULL) OR
    (target_type = 'UNIT_CATEGORY' AND unit_category_id IS NOT NULL AND entity_type_id IS NULL AND data_property_id IS NULL AND object_property_id IS NULL)
  )
);

COMMENT ON TABLE ont_axiom_rule_target IS '本体建模-公理规则目标绑定';
COMMENT ON COLUMN ont_axiom_rule_target.id IS '绑定ID';
COMMENT ON COLUMN ont_axiom_rule_target.axiom_rule_id IS '公理规则ID';
COMMENT ON COLUMN ont_axiom_rule_target.binding_role IS '绑定语义角色';
COMMENT ON COLUMN ont_axiom_rule_target.binding_order IS '同角色多值顺序';
COMMENT ON COLUMN ont_axiom_rule_target.target_type IS '目标类型';
COMMENT ON COLUMN ont_axiom_rule_target.entity_type_id IS '实体类型ID（四选一）';
COMMENT ON COLUMN ont_axiom_rule_target.data_property_id IS '数据属性ID（四选一）';
COMMENT ON COLUMN ont_axiom_rule_target.object_property_id IS '对象属性ID（四选一）';
COMMENT ON COLUMN ont_axiom_rule_target.unit_category_id IS '单位分类ID（四选一）';

CREATE UNIQUE INDEX uk_ont_axiom_rule_target_role ON ont_axiom_rule_target (axiom_rule_id, binding_role, binding_order);
CREATE INDEX idx_ont_axiom_target_entity ON ont_axiom_rule_target (entity_type_id) WHERE entity_type_id IS NOT NULL;
CREATE INDEX idx_ont_axiom_target_data_property ON ont_axiom_rule_target (data_property_id) WHERE data_property_id IS NOT NULL;
CREATE INDEX idx_ont_axiom_target_object_property ON ont_axiom_rule_target (object_property_id) WHERE object_property_id IS NOT NULL;
CREATE INDEX idx_ont_axiom_target_unit_category ON ont_axiom_rule_target (unit_category_id) WHERE unit_category_id IS NOT NULL;

-- ----------------------------
-- 菜单：本体建模 / 公理与规则管理
-- ----------------------------
INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES (900600, '公理与规则管理', NULL, '/ontology/axiom-rule/index', NULL, 900000, 'iconfont icon-shujujiegou', '1', 6, '0', NULL, '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES
  (900601, '公理规则查看', 'ontology_axiom_rule_view', NULL, NULL, 900600, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900602, '公理规则新增', 'ontology_axiom_rule_add', NULL, NULL, 900600, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900603, '公理规则修改', 'ontology_axiom_rule_edit', NULL, NULL, 900600, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900604, '公理规则删除', 'ontology_axiom_rule_del', NULL, NULL, 900600, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
  (1, 900600), (1, 900601), (1, 900602), (1, 900603), (1, 900604)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ----------------------------
-- 核心公理规则预置（12项，id 970001-970012）
-- 本体工程935001(core)
-- ----------------------------
INSERT INTO ont_axiom_rule (id, rule_code, name, category, sub_type, description, template_code, template_version, formalization_mode, validation_mode, executor_code, config_json, owl_axiom, shacl_shape, status, is_enabled, severity, source_type, source_reference, blocked_reason, is_builtin, ontology_id, sort_order, remarks, create_by, create_time, update_by, update_time, del_flag) VALUES
  -- a) 实体类型规则（2项）
  (970001, 'GB8_ENTITY_DISJOINT', '规范性要素与资料性要素不相交', 'ENTITY_TYPE', 'DISJOINT', '规范性要素与资料性要素的实例集合无交集，同一实例不可同时属于两类', 'ENTITY_DISJOINT', 1, 'SCHEMA_DERIVED', 'OWL_CONSISTENCY', 'ENTITY_DISJOINT', '{}'::jsonb, 'std:NormativeElement owl:disjointWith std:InformativeElement .', NULL, 'ACTIVE', '1', 'VIOLATION', 'GB_CLAUSE_8', '8.2 a)1)', NULL, '1', 935001, 10, '权威来源：V6 ont_entity_type_disjoint', 'admin', now(), 'admin', now(), '0'),
  (970002, 'GB8_INFO_UNIT_IDENTIFIER_UNIQUE', '信息单元全局唯一标识', 'ENTITY_TYPE', 'UNIQUE_IDENTIFIER', '信息单元需具备全局唯一标识符，唯一性按本体工程内属性IRI和规范化值校验', 'GLOBAL_UNIQUE_VALUE', 1, 'GENERATED', 'SHACL_SPARQL', 'GLOBAL_UNIQUE_VALUE', '{"keyPropertyRole":"KEY_PROPERTY","targetClassRole":"TARGET_CLASS"}'::jsonb, NULL, '[] a sh:NodeShape ; sh:targetClass std:InformationUnit ; sh:sparql [ sh:message "信息单元唯一标识符在本体工程内重复" ; sh:select "SELECT $this ?value WHERE { $this std:uniqueIdentifier ?value . ?other a std:InformationUnit ; std:uniqueIdentifier ?value . FILTER(?other != $this) }" ] .' , 'ACTIVE', '1', 'VIOLATION', 'GB_CLAUSE_8', '8.2 a)2)', NULL, '1', 935001, 11, NULL, 'admin', now(), 'admin', now(), '0'),
  -- b) 属性规则（4项国标 + 1项PRD派生 = 5项）
  (970003, 'GB8_STANDARD_NUMBER_UNIQUE', '标准编号唯一性', 'PROPERTY', 'UNIQUENESS', '标准编号在本体工程内全局唯一，使用SHACL-SPARQL或应用唯一索引校验', 'GLOBAL_UNIQUE_VALUE', 1, 'GENERATED', 'SHACL_SPARQL', 'GLOBAL_UNIQUE_VALUE', '{"keyPropertyRole":"KEY_PROPERTY","targetClassRole":"TARGET_CLASS"}'::jsonb, NULL, '[] a sh:NodeShape ; sh:targetClass std:Standard ; sh:sparql [ sh:message "标准编号在本体工程内重复" ; sh:select "SELECT $this ?value WHERE { $this std:standardNumber ?value . ?other a std:Standard ; std:standardNumber ?value . FILTER(?other != $this) }" ] .' , 'ACTIVE', '1', 'VIOLATION', 'GB_CLAUSE_8', '8.2 b)1)', NULL, '1', 935001, 20, '不擅自增加必填约束', 'admin', now(), 'admin', now(), '0'),
  (970004, 'GB8_EFFECTIVE_DATE_ORDER', '实施日期不早于发布日期', 'PROPERTY', 'DATE_VALIDITY', '标准的实施日期必须不早于发布日期', 'DATE_ORDER', 1, 'GENERATED', 'SHACL_SPARQL', 'DATE_ORDER', '{"earlierPropertyRole":"EARLIER_PROPERTY","laterPropertyRole":"LATER_PROPERTY","targetClassRole":"TARGET_CLASS"}'::jsonb, NULL, '[] a sh:NodeShape ; sh:targetClass std:Standard ; sh:sparql [ sh:message "实施日期早于发布日期" ; sh:select "SELECT $this WHERE { $this std:issuedDate ?issued . $this std:effectiveDate ?eff . FILTER(?eff < ?issued) }" ] .' , 'ACTIVE', '1', 'VIOLATION', 'GB_CLAUSE_8', '8.2 b)2)', NULL, '1', 935001, 21, NULL, 'admin', now(), 'admin', now(), '0'),
  (970005, 'GB8_STANDARD_STATUS_ENUM', '标准状态枚举约束', 'PROPERTY', 'ENUM_VALUE', '标准状态取值限定为草案、现行、废止、修订中', 'ENUM_MEMBERSHIP', 1, 'SCHEMA_DERIVED', 'SHACL_CORE', 'ENUM_MEMBERSHIP', '{"enumPropertyRole":"ENUM_PROPERTY","targetClassRole":"TARGET_CLASS"}'::jsonb, NULL, '[] a sh:NodeShape ; sh:targetClass std:Standard ; sh:property [ sh:path std:status ; sh:in ( "草案" "现行" "废止" "修订中" ) ; sh:severity sh:Violation ; sh:message "标准状态取值不合法" ] .' , 'ACTIVE', '1', 'VIOLATION', 'GB_CLAUSE_8', '8.2 b)3)', NULL, '1', 935001, 22, '枚举值从V7 ont_data_property_enum派生', 'admin', now(), 'admin', now(), '0'),
  (970006, 'GB8_STANDARD_CONSTRAINT_TYPE', '约束类型取值约束', 'PROPERTY', 'VALUE_CONSTRAINT', '标准实体的约束类型限定为强制性、推荐性', 'ENUM_MEMBERSHIP', 1, 'SCHEMA_DERIVED', 'SHACL_CORE', 'ENUM_MEMBERSHIP', '{"enumPropertyRole":"VALUE_PROPERTY","targetClassRole":"TARGET_CLASS"}'::jsonb, NULL, '[] a sh:NodeShape ; sh:targetClass std:Standard ; sh:property [ sh:path std:constraintType ; sh:in ( "强制性" "推荐性" ) ; sh:severity sh:Violation ; sh:message "约束类型取值不合法" ] .' , 'ACTIVE', '1', 'VIOLATION', 'GB_CLAUSE_8', '8.2 b)4)', NULL, '1', 935001, 23, NULL, 'admin', now(), 'admin', now(), '0'),
  (970007, 'PRD_CONSTRAINT_UNIT_CONSISTENCY', '约束逻辑单位一致性', 'PROPERTY', 'UNIT_CONSISTENCY', '同一约束逻辑实例的maxValue/minValue/thresholdRange须与measurementUnit引用的单位字典条目同属一个物理量分类', 'UNIT_DIMENSION_CONSISTENCY', 1, 'GENERATED', 'APPLICATION', 'UNIT_DIMENSION_CONSISTENCY', '{"maxPropertyRole":"MAX","minPropertyRole":"MIN","rangePropertyRole":"RANGE","unitPropertyRole":"UNIT","targetClassRole":"TARGET_CLASS"}'::jsonb, NULL, NULL, 'ACTIVE', '1', 'VIOLATION', 'PRD_DERIVED', 'PRD §4.5单位一致性', NULL, '1', 935001, 24, 'PRD派生规则，附录C表C.38~C.41联动', 'admin', now(), 'admin', now(), '0'),
  -- c) 关系规则（5项）
  (970008, 'GB8_ISSUED_BY_FUNCTIONAL', '标准发布机构唯一性（功能性）', 'RELATION', 'FUNCTIONAL', '每个标准只能由一个机构发布，V8已声明owl:FunctionalProperty，操作性校验还需sh:maxCount 1', 'FUNCTIONAL_OBJECT_PROPERTY', 1, 'SCHEMA_DERIVED', 'COMPOSITE', 'FUNCTIONAL_OBJECT_PROPERTY', '{"targetClassRole":"TARGET_CLASS","relationPropertyRole":"RELATION_PROPERTY"}'::jsonb, 'std:issuedBy a owl:FunctionalProperty .', '[] a sh:NodeShape ; sh:targetClass std:Standard ; sh:property [ sh:path std:issuedBy ; sh:maxCount 1 ; sh:severity sh:Violation ; sh:message "每个标准只能由一个机构发布" ] .' , 'ACTIVE', '1', 'VIOLATION', 'GB_CLAUSE_8', '8.2 c)1)', NULL, '1', 935001, 30, 'V8 issuedBy.is_functional=1为权威来源', 'admin', now(), 'admin', now(), '0'),
  (970009, 'GB8_VERSION_REPLACEMENT', '废止标准须指向替代标准', 'RELATION', 'VERSION_REPLACEMENT', '废止标准须指向替代标准或标明废止日期；当前Schema缺少废止日期属性，规则阻塞', 'VERSION_REPLACEMENT', 1, 'GENERATED', 'SHACL_SPARQL', NULL, '{"targetClassRole":"TARGET_CLASS","statusPropertyRole":"STATUS","replacesRole":"REPLACES","replacedByRole":"REPLACED_BY"}'::jsonb, NULL, NULL, 'BLOCKED', '0', 'WARNING', 'GB_CLAUSE_8', '8.2 c)2)', '当前Schema缺少废止日期属性，无法完整表达国标要求的替代或废止日期条件', '1', 935001, 31, 'BLOCKED：缺少废止日期Schema', 'admin', now(), 'admin', now(), '0'),
  (970010, 'GB8_HIERARCHY_CONTAINMENT', '章可包含零或多个条', 'RELATION', 'HIERARCHY_CONTAINMENT', '章(Section)可通过hasClause包含零或多个条(Clause)，通过定义域/值域校验表达，不生成sh:minCount 0', 'OBJECT_RELATION_RANGE', 1, 'SCHEMA_DERIVED', 'APPLICATION', 'OBJECT_RELATION_RANGE', '{"parentClassRole":"PARENT_CLASS","childClassRole":"CHILD_CLASS","relationPropertyRole":"RELATION_PROPERTY"}'::jsonb, NULL, NULL, 'ACTIVE', '1', 'INFO', 'GB_CLAUSE_8', '8.2 c)3)第一项', NULL, '1', 935001, 32, '复用对象属性定义域/值域校验', 'admin', now(), 'admin', now(), '0'),
  (970011, 'GB8_UNTITLED_NO_SUBCLAUSE', '无标题条不可含子条', 'RELATION', 'STRUCTURAL_LIMIT', '无标题条(UntitledClause)不可通过hasSubClause包含子条', 'MAX_RELATION_COUNT', 1, 'GENERATED', 'SHACL_CORE', 'MAX_RELATION_COUNT', '{"targetClassRole":"TARGET_CLASS","relationPropertyRole":"RELATION_PROPERTY","maxCount":0}'::jsonb, NULL, '[] a sh:NodeShape ; sh:targetClass std:UntitledClause ; sh:property [ sh:path std:hasSubClause ; sh:maxCount 0 ; sh:severity sh:Violation ; sh:message "无标题条不可包含子条" ] .' , 'ACTIVE', '1', 'VIOLATION', 'GB_CLAUSE_8', '8.2 c)3)第二项', NULL, '1', 935001, 33, NULL, 'admin', now(), 'admin', now(), '0'),
  (970012, 'GB8_REFERENCE_DISTINCTION', '标准间引用与条款引用区分', 'RELATION', 'REFERENCE_DISTINCTION', '标准间引用使用cites，条款级引用使用citesStandard，由定义域和写入选择器治理', 'OBJECT_RELATION_RANGE', 1, 'SCHEMA_DERIVED', 'APPLICATION', 'OBJECT_RELATION_RANGE', '{"standardClassRole":"STANDARD_CLASS","clauseClassRole":"CLAUSE_CLASS","standardRelationRole":"STANDARD_RELATION","clauseRelationRole":"CLAUSE_RELATION"}'::jsonb, NULL, NULL, 'ACTIVE', '1', 'INFO', 'GB_CLAUSE_8', '8.2 c)4)', NULL, '1', 935001, 34, '不声明owl:propertyDisjointWith', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 规则目标绑定
-- ----------------------------
INSERT INTO ont_axiom_rule_target (id, axiom_rule_id, binding_role, binding_order, target_type, entity_type_id, data_property_id, object_property_id, unit_category_id) VALUES
  -- 970001 不相交：NormativeElement(940028) 与 InformativeElement(940034)
  (97000101, 970001, 'TYPE_A', 0, 'ENTITY_TYPE', 940028, NULL, NULL, NULL),
  (97000102, 970001, 'TYPE_B', 0, 'ENTITY_TYPE', 940034, NULL, NULL, NULL),
  -- 970002 信息单元唯一标识：InformationUnit(940048) + uniqueIdentifier(950028)
  (97000201, 970002, 'TARGET_CLASS', 0, 'ENTITY_TYPE', 940048, NULL, NULL, NULL),
  (97000202, 970002, 'KEY_PROPERTY', 0, 'DATA_PROPERTY', NULL, 950028, NULL, NULL),
  -- 970003 标准编号唯一：Standard(940001) + standardNumber(950006)
  (97000301, 970003, 'TARGET_CLASS', 0, 'ENTITY_TYPE', 940001, NULL, NULL, NULL),
  (97000302, 970003, 'KEY_PROPERTY', 0, 'DATA_PROPERTY', NULL, 950006, NULL, NULL),
  -- 970004 日期顺序：Standard(940001) + issuedDate(950007) + effectiveDate(950008)
  (97000401, 970004, 'TARGET_CLASS', 0, 'ENTITY_TYPE', 940001, NULL, NULL, NULL),
  (97000402, 970004, 'EARLIER_PROPERTY', 0, 'DATA_PROPERTY', NULL, 950007, NULL, NULL),
  (97000403, 970004, 'LATER_PROPERTY', 0, 'DATA_PROPERTY', NULL, 950008, NULL, NULL),
  -- 970005 标准状态枚举：Standard(940001) + status(950003)
  (97000501, 970005, 'TARGET_CLASS', 0, 'ENTITY_TYPE', 940001, NULL, NULL, NULL),
  (97000502, 970005, 'ENUM_PROPERTY', 0, 'DATA_PROPERTY', NULL, 950003, NULL, NULL),
  -- 970006 约束类型取值：Standard(940001) + constraintType(950004)
  (97000601, 970006, 'TARGET_CLASS', 0, 'ENTITY_TYPE', 940001, NULL, NULL, NULL),
  (97000602, 970006, 'VALUE_PROPERTY', 0, 'DATA_PROPERTY', NULL, 950004, NULL, NULL),
  -- 970007 单位一致性：Constraint(940067) + maxValue(950038) + minValue(950039) + thresholdRange(950040) + unit(950041)
  (97000701, 970007, 'TARGET_CLASS', 0, 'ENTITY_TYPE', 940067, NULL, NULL, NULL),
  (97000702, 970007, 'MAX', 0, 'DATA_PROPERTY', NULL, 950038, NULL, NULL),
  (97000703, 970007, 'MIN', 0, 'DATA_PROPERTY', NULL, 950039, NULL, NULL),
  (97000704, 970007, 'RANGE', 0, 'DATA_PROPERTY', NULL, 950040, NULL, NULL),
  (97000705, 970007, 'UNIT', 0, 'DATA_PROPERTY', NULL, 950041, NULL, NULL),
  -- 970008 功能性：Standard(940001) + issuedBy(960006)
  (97000801, 970008, 'TARGET_CLASS', 0, 'ENTITY_TYPE', 940001, NULL, NULL, NULL),
  (97000802, 970008, 'RELATION_PROPERTY', 0, 'OBJECT_PROPERTY', NULL, NULL, 960006, NULL),
  -- 970009 版本替代(BLOCKED)：Standard(940001) + status(950003) + replaces(960002) + isReplacedBy(960035)
  (97000901, 970009, 'TARGET_CLASS', 0, 'ENTITY_TYPE', 940001, NULL, NULL, NULL),
  (97000902, 970009, 'STATUS', 0, 'DATA_PROPERTY', NULL, 950003, NULL, NULL),
  (97000903, 970009, 'REPLACES', 0, 'OBJECT_PROPERTY', NULL, NULL, 960002, NULL),
  (97000904, 970009, 'REPLACED_BY', 0, 'OBJECT_PROPERTY', NULL, NULL, 960035, NULL),
  -- 970010 层次包含：Section(940039) + Clause(940040) + hasClause(960015)
  (97001001, 970010, 'PARENT_CLASS', 0, 'ENTITY_TYPE', 940039, NULL, NULL, NULL),
  (97001002, 970010, 'CHILD_CLASS', 0, 'ENTITY_TYPE', 940040, NULL, NULL, NULL),
  (97001003, 970010, 'RELATION_PROPERTY', 0, 'OBJECT_PROPERTY', NULL, NULL, 960015, NULL),
  -- 970011 无标题条结构限制：UntitledClause(940042) + hasSubClause(960016)
  (97001101, 970011, 'TARGET_CLASS', 0, 'ENTITY_TYPE', 940042, NULL, NULL, NULL),
  (97001102, 970011, 'RELATION_PROPERTY', 0, 'OBJECT_PROPERTY', NULL, NULL, 960016, NULL),
  -- 970012 引用区分：Standard(940001) + Clause(940040) + cites(960003) + citesStandard(960022)
  (97001201, 970012, 'STANDARD_CLASS', 0, 'ENTITY_TYPE', 940001, NULL, NULL, NULL),
  (97001202, 970012, 'CLAUSE_CLASS', 0, 'ENTITY_TYPE', 940040, NULL, NULL, NULL),
  (97001203, 970012, 'STANDARD_RELATION', 0, 'OBJECT_PROPERTY', NULL, NULL, 960003, NULL),
  (97001204, 970012, 'CLAUSE_RELATION', 0, 'OBJECT_PROPERTY', NULL, NULL, 960022, NULL)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 核心种子完整性断言
-- ----------------------------
DO $$
BEGIN
  -- 1. 970001~970012连续存在，受保护预置总数12
  IF (SELECT count(*) FROM ont_axiom_rule WHERE is_builtin = '1' AND del_flag = '0') <> 12 THEN
    RAISE EXCEPTION '受保护预置公理规则总数不是12条';
  END IF;
  -- 2. GB_CLAUSE_8=11，PRD_DERIVED=1，970007来源正确
  IF (SELECT count(*) FROM ont_axiom_rule WHERE source_type = 'GB_CLAUSE_8' AND is_builtin = '1' AND del_flag = '0') <> 11 THEN
    RAISE EXCEPTION 'GB_CLAUSE_8公理规则数量不是11条';
  END IF;
  IF (SELECT count(*) FROM ont_axiom_rule WHERE source_type = 'PRD_DERIVED' AND is_builtin = '1' AND del_flag = '0') <> 1 THEN
    RAISE EXCEPTION 'PRD_DERIVED公理规则数量不是1条';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_axiom_rule WHERE id = 970007 AND source_type = 'PRD_DERIVED') THEN
    RAISE EXCEPTION '970007单位一致性规则来源必须为PRD_DERIVED';
  END IF;
  -- 3. 分类分布：2 ENTITY_TYPE + 5 PROPERTY + 5 RELATION
  IF (SELECT count(*) FROM ont_axiom_rule WHERE category = 'ENTITY_TYPE' AND is_builtin = '1' AND del_flag = '0') <> 2 THEN
    RAISE EXCEPTION 'ENTITY_TYPE规则数量不是2条';
  END IF;
  IF (SELECT count(*) FROM ont_axiom_rule WHERE category = 'PROPERTY' AND is_builtin = '1' AND del_flag = '0') <> 5 THEN
    RAISE EXCEPTION 'PROPERTY规则数量不是5条';
  END IF;
  IF (SELECT count(*) FROM ont_axiom_rule WHERE category = 'RELATION' AND is_builtin = '1' AND del_flag = '0') <> 5 THEN
    RAISE EXCEPTION 'RELATION规则数量不是5条';
  END IF;
  -- 4. 11条ACTIVE/启用，970009为BLOCKED/停用且有原因
  IF (SELECT count(*) FROM ont_axiom_rule WHERE status = 'ACTIVE' AND is_enabled = '1' AND is_builtin = '1' AND del_flag = '0') <> 11 THEN
    RAISE EXCEPTION 'ACTIVE且启用的内置规则数量不是11条';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_axiom_rule WHERE id = 970009 AND status = 'BLOCKED' AND is_enabled = '0' AND blocked_reason IS NOT NULL) THEN
    RAISE EXCEPTION '970009版本替代规则必须为BLOCKED且停用且有阻塞原因';
  END IF;
  -- 5. 所有ACTIVE规则有executor_code和完整目标角色
  IF EXISTS (SELECT 1 FROM ont_axiom_rule WHERE status = 'ACTIVE' AND executor_code IS NULL AND del_flag = '0') THEN
    RAISE EXCEPTION '存在ACTIVE但缺少executor_code的规则';
  END IF;
  IF EXISTS (
    SELECT 1 FROM ont_axiom_rule r WHERE r.status = 'ACTIVE' AND r.del_flag = '0'
    AND NOT EXISTS (SELECT 1 FROM ont_axiom_rule_target t WHERE t.axiom_rule_id = r.id)
  ) THEN
    RAISE EXCEPTION '存在ACTIVE但缺少目标绑定的规则';
  END IF;
  -- 6. 970007同时绑定950038、950039、950040、950041
  IF (SELECT count(*) FROM ont_axiom_rule_target WHERE axiom_rule_id = 970007 AND data_property_id IN (950038, 950039, 950040, 950041)) <> 4 THEN
    RAISE EXCEPTION '970007单位一致性规则必须绑定maxValue(950038)、minValue(950039)、thresholdRange(950040)、unit(950041)';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_axiom_rule_target WHERE axiom_rule_id = 970007 AND data_property_id = 950040) THEN
    RAISE EXCEPTION '970007单位一致性规则必须包含thresholdRange(950040)绑定';
  END IF;
  -- 7. 970008绑定960006，且V8 issuedBy.is_functional=1
  IF NOT EXISTS (SELECT 1 FROM ont_axiom_rule_target WHERE axiom_rule_id = 970008 AND object_property_id = 960006) THEN
    RAISE EXCEPTION '970008功能性规则必须绑定issuedBy(960006)';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_object_property WHERE id = 960006 AND is_functional = '1') THEN
    RAISE EXCEPTION 'V8 issuedBy(960006)必须标记为功能性';
  END IF;
  -- 8. 970001目标与V6核心不相交对一致
  IF NOT EXISTS (SELECT 1 FROM ont_axiom_rule_target WHERE axiom_rule_id = 970001 AND entity_type_id = 940028 AND binding_role = 'TYPE_A') THEN
    RAISE EXCEPTION '970001不相交规则TYPE_A必须为NormativeElement(940028)';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_axiom_rule_target WHERE axiom_rule_id = 970001 AND entity_type_id = 940034 AND binding_role = 'TYPE_B') THEN
    RAISE EXCEPTION '970001不相交规则TYPE_B必须为InformativeElement(940034)';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_entity_type_disjoint WHERE type_a = 940028 AND type_b = 940034) THEN
    RAISE EXCEPTION 'V6核心不相交对(940028,940034)不存在';
  END IF;
  -- 9. 目标表四列恰一非空，所有物理外键有效且目标未逻辑删除
  IF EXISTS (
    SELECT 1 FROM ont_axiom_rule_target t
    LEFT JOIN ont_entity_type et ON t.entity_type_id = et.id
    LEFT JOIN ont_data_property dp ON t.data_property_id = dp.id
    LEFT JOIN ont_object_property op ON t.object_property_id = op.id
    LEFT JOIN ont_unit_category uc ON t.unit_category_id = uc.id
    WHERE
      (t.entity_type_id IS NOT NULL AND et.del_flag = '1') OR
      (t.data_property_id IS NOT NULL AND dp.del_flag = '1') OR
      (t.object_property_id IS NOT NULL AND op.del_flag = '1')
  ) THEN
    RAISE EXCEPTION '规则目标存在指向已逻辑删除的Schema元素';
  END IF;
  -- 10. 所有内置非APPLICATION模式规则有非空OWL或SHACL（至少一项）
  -- APPLICATION模式规则由Java执行器校验，不强制要求形式化文本
  IF EXISTS (
    SELECT 1 FROM ont_axiom_rule
    WHERE is_builtin = '1' AND del_flag = '0' AND status <> 'BLOCKED'
    AND validation_mode NOT IN ('APPLICATION')
    AND owl_axiom IS NULL AND shacl_shape IS NULL
  ) THEN
    RAISE EXCEPTION '存在非APPLICATION模式内置规则缺少OWL和SHACL形式化文本';
  END IF;
  -- 11. 核心规则没有DRAFT
  IF EXISTS (SELECT 1 FROM ont_axiom_rule WHERE is_builtin = '1' AND status = 'DRAFT' AND del_flag = '0') THEN
    RAISE EXCEPTION '内置规则不应为DRAFT状态';
  END IF;
  -- 12. 菜单900600~900604及管理员授权完整
  IF (SELECT count(*) FROM sys_menu WHERE menu_id BETWEEN 900600 AND 900604 AND del_flag = '0') <> 5 THEN
    RAISE EXCEPTION '公理规则菜单900600~900604不完整';
  END IF;
  IF (SELECT count(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 900600 AND 900604) <> 5 THEN
    RAISE EXCEPTION '公理规则管理员授权不完整';
  END IF;
END $$;
