-- ----------------------------
-- youming ontology entity type management
-- ----------------------------

-- 最小本体工程作用域（实体类型首期统一归属核心工程，后续工程模块复用）
CREATE TABLE ont_ontology_project (
  id bigint NOT NULL,
  project_code varchar(64) NOT NULL,
  project_name varchar(128) NOT NULL,
  is_builtin char(1) NOT NULL DEFAULT '0',
  description varchar(255) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT ck_ont_ontology_project_builtin CHECK (is_builtin IN ('0', '1')),
  CONSTRAINT ck_ont_ontology_project_del_flag CHECK (del_flag IN ('0', '1'))
);

COMMENT ON TABLE ont_ontology_project IS '本体建模-本体工程最小作用域表';
COMMENT ON COLUMN ont_ontology_project.project_code IS '本体工程编码';
COMMENT ON COLUMN ont_ontology_project.project_name IS '本体工程名称';
COMMENT ON COLUMN ont_ontology_project.is_builtin IS '是否内置，1是0否';
COMMENT ON COLUMN ont_ontology_project.description IS '描述';
COMMENT ON COLUMN ont_ontology_project.del_flag IS '删除标志，0未删除，1已删除';

CREATE UNIQUE INDEX uk_ont_ontology_project_code ON ont_ontology_project (project_code) WHERE del_flag = '0';

-- 实体类型主表
CREATE TABLE ont_entity_type (
  id bigint NOT NULL,
  iri varchar(512) NOT NULL,
  name varchar(128) NOT NULL,
  definition varchar(512) DEFAULT NULL,
  is_abstract char(1) NOT NULL DEFAULT '0',
  is_builtin char(1) NOT NULL DEFAULT '0',
  ontology_id bigint NOT NULL,
  namespace_id bigint NOT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  remarks varchar(255) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_entity_type_ontology FOREIGN KEY (ontology_id) REFERENCES ont_ontology_project (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_entity_type_namespace FOREIGN KEY (namespace_id) REFERENCES ont_namespace (id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_entity_type_abstract CHECK (is_abstract IN ('0', '1')),
  CONSTRAINT ck_ont_entity_type_builtin CHECK (is_builtin IN ('0', '1')),
  CONSTRAINT ck_ont_entity_type_del_flag CHECK (del_flag IN ('0', '1')),
  CONSTRAINT ck_ont_entity_type_sort_order CHECK (sort_order >= 0)
);

COMMENT ON TABLE ont_entity_type IS '本体建模-实体类型表';
COMMENT ON COLUMN ont_entity_type.id IS '实体类型ID';
COMMENT ON COLUMN ont_entity_type.iri IS '全局唯一IRI';
COMMENT ON COLUMN ont_entity_type.name IS '英文名称（IRI本地标识符）';
COMMENT ON COLUMN ont_entity_type.definition IS '定义';
COMMENT ON COLUMN ont_entity_type.is_abstract IS '是否抽象类，1是0否';
COMMENT ON COLUMN ont_entity_type.is_builtin IS '是否内置，1是0否';
COMMENT ON COLUMN ont_entity_type.ontology_id IS '本体工程ID，引用ont_ontology_project.id';
COMMENT ON COLUMN ont_entity_type.namespace_id IS '命名空间ID，引用ont_namespace.id';
COMMENT ON COLUMN ont_entity_type.sort_order IS '排序值';
COMMENT ON COLUMN ont_entity_type.remarks IS '备注';
COMMENT ON COLUMN ont_entity_type.create_by IS '创建人';
COMMENT ON COLUMN ont_entity_type.create_time IS '创建时间';
COMMENT ON COLUMN ont_entity_type.update_by IS '修改人';
COMMENT ON COLUMN ont_entity_type.update_time IS '更新时间';
COMMENT ON COLUMN ont_entity_type.del_flag IS '删除标志，0未删除，1已删除';

CREATE UNIQUE INDEX uk_ont_entity_type_iri ON ont_entity_type (iri) WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_entity_type_name ON ont_entity_type (ontology_id, namespace_id, name) WHERE del_flag = '0';
CREATE INDEX idx_ont_entity_type_ontology ON ont_entity_type (ontology_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_entity_type_namespace ON ont_entity_type (namespace_id) WHERE del_flag = '0';

-- 多语言标签
CREATE TABLE ont_entity_type_label (
  entity_type_id bigint NOT NULL,
  locale varchar(16) NOT NULL,
  label varchar(128) NOT NULL,
  PRIMARY KEY (entity_type_id, locale),
  CONSTRAINT fk_ont_entity_type_label_type FOREIGN KEY (entity_type_id) REFERENCES ont_entity_type (id) ON DELETE CASCADE,
  CONSTRAINT ck_ont_entity_type_label_locale CHECK (btrim(locale) <> ''),
  CONSTRAINT ck_ont_entity_type_label_text CHECK (btrim(label) <> '')
);

COMMENT ON TABLE ont_entity_type_label IS '本体建模-实体类型多语言标签';
COMMENT ON COLUMN ont_entity_type_label.entity_type_id IS '实体类型ID';
COMMENT ON COLUMN ont_entity_type_label.locale IS '语言区域，如zh、en';
COMMENT ON COLUMN ont_entity_type_label.label IS '标签文本';

-- 继承关系
CREATE TABLE ont_entity_type_hierarchy (
  parent_id bigint NOT NULL,
  child_id bigint NOT NULL,
  PRIMARY KEY (parent_id, child_id),
  CONSTRAINT fk_ont_entity_type_hierarchy_parent FOREIGN KEY (parent_id) REFERENCES ont_entity_type (id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_entity_type_hierarchy_child FOREIGN KEY (child_id) REFERENCES ont_entity_type (id) ON DELETE CASCADE,
  CONSTRAINT ck_ont_entity_type_hierarchy_not_self CHECK (parent_id <> child_id)
);

COMMENT ON TABLE ont_entity_type_hierarchy IS '本体建模-实体类型继承关系';
COMMENT ON COLUMN ont_entity_type_hierarchy.parent_id IS '父类实体类型ID';
COMMENT ON COLUMN ont_entity_type_hierarchy.child_id IS '子类实体类型ID';

CREATE INDEX idx_ont_entity_type_hierarchy_child ON ont_entity_type_hierarchy (child_id);

-- 不相交类
CREATE TABLE ont_entity_type_disjoint (
  type_a bigint NOT NULL,
  type_b bigint NOT NULL,
  PRIMARY KEY (type_a, type_b),
  CONSTRAINT fk_ont_entity_type_disjoint_a FOREIGN KEY (type_a) REFERENCES ont_entity_type (id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_entity_type_disjoint_b FOREIGN KEY (type_b) REFERENCES ont_entity_type (id) ON DELETE CASCADE,
  CONSTRAINT ck_ont_entity_type_disjoint_order CHECK (type_a < type_b)
);

COMMENT ON TABLE ont_entity_type_disjoint IS '本体建模-实体类型不相交关系';
COMMENT ON COLUMN ont_entity_type_disjoint.type_a IS '实体类型A';
COMMENT ON COLUMN ont_entity_type_disjoint.type_b IS '实体类型B';

CREATE INDEX idx_ont_entity_type_disjoint_reverse ON ont_entity_type_disjoint (type_b);

-- 等价类
CREATE TABLE ont_entity_type_equivalent (
  entity_type_id bigint NOT NULL,
  equivalent_id bigint NOT NULL,
  PRIMARY KEY (entity_type_id, equivalent_id),
  CONSTRAINT fk_ont_entity_type_equivalent_type FOREIGN KEY (entity_type_id) REFERENCES ont_entity_type (id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_entity_type_equivalent_target FOREIGN KEY (equivalent_id) REFERENCES ont_entity_type (id) ON DELETE CASCADE,
  CONSTRAINT ck_ont_entity_type_equivalent_order CHECK (entity_type_id < equivalent_id)
);

COMMENT ON TABLE ont_entity_type_equivalent IS '本体建模-实体类型等价关系';
COMMENT ON COLUMN ont_entity_type_equivalent.entity_type_id IS '实体类型ID';
COMMENT ON COLUMN ont_entity_type_equivalent.equivalent_id IS '等价实体类型ID';

CREATE INDEX idx_ont_entity_type_equivalent_reverse ON ont_entity_type_equivalent (equivalent_id);

-- 首期核心工程
INSERT INTO ont_ontology_project (id, project_code, project_name, is_builtin, description, create_by, create_time, update_by, update_time, del_flag)
VALUES (935001, 'core', '标准本体核心工程', '1', '国标核心Schema及首期扩展实体类型的默认归属工程', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 菜单：本体建模 / 实体类型管理（复用 900000 顶级目录）
-- ----------------------------
INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES (900300, '实体类型管理', NULL, '/ontology/entity-type/index', NULL, 900000, 'iconfont icon-juxingkaungjia', '1', 3, '0', NULL, '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES
  (900301, '实体类型查看', 'ontology_entity_type_view', NULL, NULL, 900300, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900302, '实体类型新增', 'ontology_entity_type_add', NULL, NULL, 900300, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900303, '实体类型修改', 'ontology_entity_type_edit', NULL, NULL, 900300, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900304, '实体类型删除', 'ontology_entity_type_del', NULL, NULL, 900300, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
  (1, 900300),
  (1, 900301),
  (1, 900302),
  (1, 900303),
  (1, 900304)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ----------------------------
-- 内置实体类型（86条，id 940001-940086）
-- ----------------------------
INSERT INTO ont_entity_type (id, iri, name, definition, is_abstract, is_builtin, ontology_id, namespace_id, sort_order, remarks, create_by, create_time, update_by, update_time, del_flag) VALUES
  -- 第一组：标准实体（9条）
  (940001, 'http://example.org/standard-ontology#Standard', 'Standard', '标准文献的核心实体', '0', '1', 935001, 930001, 10, NULL, 'admin', now(), 'admin', now(), '0'),
  (940002, 'http://example.org/standard-ontology#TerminologyStandard', 'TerminologyStandard', '术语标准', '0', '1', 935001, 930001, 11, NULL, 'admin', now(), 'admin', now(), '0'),
  (940003, 'http://example.org/standard-ontology#SymbolStandard', 'SymbolStandard', '符号标准', '0', '1', 935001, 930001, 12, NULL, 'admin', now(), 'admin', now(), '0'),
  (940004, 'http://example.org/standard-ontology#ClassificationStandard', 'ClassificationStandard', '分类标准', '0', '1', 935001, 930001, 13, NULL, 'admin', now(), 'admin', now(), '0'),
  (940005, 'http://example.org/standard-ontology#TestStandard', 'TestStandard', '试验标准', '0', '1', 935001, 930001, 14, NULL, 'admin', now(), 'admin', now(), '0'),
  (940006, 'http://example.org/standard-ontology#SpecificationStandard', 'SpecificationStandard', '规范标准', '0', '1', 935001, 930001, 15, NULL, 'admin', now(), 'admin', now(), '0'),
  (940007, 'http://example.org/standard-ontology#ProcedureStandard', 'ProcedureStandard', '规程标准', '0', '1', 935001, 930001, 16, NULL, 'admin', now(), 'admin', now(), '0'),
  (940008, 'http://example.org/standard-ontology#GuideStandard', 'GuideStandard', '指南标准', '0', '1', 935001, 930001, 17, NULL, 'admin', now(), 'admin', now(), '0'),
  (940009, 'http://example.org/standard-ontology#EvaluationStandard', 'EvaluationStandard', '评价标准', '0', '1', 935001, 930001, 18, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 第二组：元数据（17条）
  (940010, 'http://example.org/standard-ontology#StandardizationObject', 'StandardizationObject', '标准化对象', '0', '1', 935001, 930001, 20, NULL, 'admin', now(), 'admin', now(), '0'),
  (940011, 'http://example.org/standard-ontology#Stakeholder', 'Stakeholder', '相关方（抽象类）', '1', '1', 935001, 930001, 21, NULL, 'admin', now(), 'admin', now(), '0'),
  (940012, 'http://example.org/standard-ontology#Organization', 'Organization', '机构', '0', '1', 935001, 930001, 22, NULL, 'admin', now(), 'admin', now(), '0'),
  (940013, 'http://example.org/standard-ontology#PublishingOrganization', 'PublishingOrganization', '发布机构', '0', '1', 935001, 930001, 23, NULL, 'admin', now(), 'admin', now(), '0'),
  (940014, 'http://example.org/standard-ontology#ProposingOrganization', 'ProposingOrganization', '提出单位', '0', '1', 935001, 930001, 24, NULL, 'admin', now(), 'admin', now(), '0'),
  (940015, 'http://example.org/standard-ontology#AdministeringOrganization', 'AdministeringOrganization', '归口单位', '0', '1', 935001, 930001, 25, NULL, 'admin', now(), 'admin', now(), '0'),
  (940016, 'http://example.org/standard-ontology#OrganizingOrganization', 'OrganizingOrganization', '组织实施单位', '0', '1', 935001, 930001, 26, NULL, 'admin', now(), 'admin', now(), '0'),
  (940017, 'http://example.org/standard-ontology#DraftingOrganization', 'DraftingOrganization', '起草单位', '0', '1', 935001, 930001, 27, NULL, 'admin', now(), 'admin', now(), '0'),
  (940018, 'http://example.org/standard-ontology#ProposingParty', 'ProposingParty', '建议方', '0', '1', 935001, 930001, 28, NULL, 'admin', now(), 'admin', now(), '0'),
  (940019, 'http://example.org/standard-ontology#ConsultingOrganization', 'ConsultingOrganization', '征求意见单位', '0', '1', 935001, 930001, 29, NULL, 'admin', now(), 'admin', now(), '0'),
  (940020, 'http://example.org/standard-ontology#PublishingHouse', 'PublishingHouse', '出版机构', '0', '1', 935001, 930001, 30, NULL, 'admin', now(), 'admin', now(), '0'),
  (940021, 'http://example.org/standard-ontology#Individual', 'Individual', '个人', '0', '1', 935001, 930001, 31, NULL, 'admin', now(), 'admin', now(), '0'),
  (940022, 'http://example.org/standard-ontology#Drafter', 'Drafter', '起草人', '0', '1', 935001, 930001, 32, NULL, 'admin', now(), 'admin', now(), '0'),
  (940023, 'http://example.org/standard-ontology#Reviewer', 'Reviewer', '评审人', '0', '1', 935001, 930001, 33, NULL, 'admin', now(), 'admin', now(), '0'),
  (940024, 'http://example.org/standard-ontology#DomainCategory', 'DomainCategory', '领域类别', '0', '1', 935001, 930001, 34, NULL, 'admin', now(), 'admin', now(), '0'),
  (940025, 'http://example.org/standard-ontology#InternationalClassificationOfStandard', 'InternationalClassificationOfStandard', '国际标准分类', '0', '1', 935001, 930001, 35, NULL, 'admin', now(), 'admin', now(), '0'),
  (940026, 'http://example.org/standard-ontology#ChineseClassificationOfStandard', 'ChineseClassificationOfStandard', '中国标准文献分类', '0', '1', 935001, 930001, 36, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 第三组：结构（20条）
  (940027, 'http://example.org/standard-ontology#ContentElement', 'ContentElement', '内容要素', '0', '1', 935001, 930001, 40, NULL, 'admin', now(), 'admin', now(), '0'),
  (940028, 'http://example.org/standard-ontology#NormativeElement', 'NormativeElement', '规范性要素', '0', '1', 935001, 930001, 41, NULL, 'admin', now(), 'admin', now(), '0'),
  (940029, 'http://example.org/standard-ontology#Scope', 'Scope', '范围', '0', '1', 935001, 930001, 42, NULL, 'admin', now(), 'admin', now(), '0'),
  (940030, 'http://example.org/standard-ontology#TermsAndDefinitions', 'TermsAndDefinitions', '术语和定义', '0', '1', 935001, 930001, 43, NULL, 'admin', now(), 'admin', now(), '0'),
  (940031, 'http://example.org/standard-ontology#SymbolsAndAbbreviations', 'SymbolsAndAbbreviations', '符号和缩略语', '0', '1', 935001, 930001, 44, NULL, 'admin', now(), 'admin', now(), '0'),
  (940032, 'http://example.org/standard-ontology#CoreTechnicalElement', 'CoreTechnicalElement', '核心技术要素', '0', '1', 935001, 930001, 45, NULL, 'admin', now(), 'admin', now(), '0'),
  (940033, 'http://example.org/standard-ontology#OtherTechnicalElement', 'OtherTechnicalElement', '其他技术要素', '0', '1', 935001, 930001, 46, NULL, 'admin', now(), 'admin', now(), '0'),
  (940034, 'http://example.org/standard-ontology#InformativeElement', 'InformativeElement', '资料性要素', '0', '1', 935001, 930001, 47, NULL, 'admin', now(), 'admin', now(), '0'),
  (940035, 'http://example.org/standard-ontology#NormativeReferences', 'NormativeReferences', '规范性引用文件', '0', '1', 935001, 930001, 48, NULL, 'admin', now(), 'admin', now(), '0'),
  (940036, 'http://example.org/standard-ontology#Bibliography', 'Bibliography', '参考文献', '0', '1', 935001, 930001, 49, NULL, 'admin', now(), 'admin', now(), '0'),
  (940037, 'http://example.org/standard-ontology#Index', 'Index', '索引', '0', '1', 935001, 930001, 50, NULL, 'admin', now(), 'admin', now(), '0'),
  (940038, 'http://example.org/standard-ontology#StructuralElement', 'StructuralElement', '层次', '0', '1', 935001, 930001, 51, NULL, 'admin', now(), 'admin', now(), '0'),
  (940039, 'http://example.org/standard-ontology#Section', 'Section', '章', '0', '1', 935001, 930001, 52, NULL, 'admin', now(), 'admin', now(), '0'),
  (940040, 'http://example.org/standard-ontology#Clause', 'Clause', '条（兼信息单元子类，等价InformationUnit）', '0', '1', 935001, 930001, 53, NULL, 'admin', now(), 'admin', now(), '0'),
  (940041, 'http://example.org/standard-ontology#TitledClause', 'TitledClause', '有标题条', '0', '1', 935001, 930001, 54, NULL, 'admin', now(), 'admin', now(), '0'),
  (940042, 'http://example.org/standard-ontology#UntitledClause', 'UntitledClause', '无标题条（不可含子条）', '0', '1', 935001, 930001, 55, NULL, 'admin', now(), 'admin', now(), '0'),
  (940043, 'http://example.org/standard-ontology#Paragraph', 'Paragraph', '段', '0', '1', 935001, 930001, 56, NULL, 'admin', now(), 'admin', now(), '0'),
  (940044, 'http://example.org/standard-ontology#ListItem', 'ListItem', '列项', '0', '1', 935001, 930001, 57, NULL, 'admin', now(), 'admin', now(), '0'),
  (940045, 'http://example.org/standard-ontology#UnnumberedListItem', 'UnnumberedListItem', '无编号列项', '0', '1', 935001, 930001, 58, NULL, 'admin', now(), 'admin', now(), '0'),
  (940046, 'http://example.org/standard-ontology#NumberedListItem', 'NumberedListItem', '有编号列项', '0', '1', 935001, 930001, 59, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 第四组：技术内容（30条）
  (940047, 'http://example.org/standard-ontology#Term', 'Term', '术语（第6.2.5.1条补齐）', '0', '1', 935001, 930001, 60, NULL, 'admin', now(), 'admin', now(), '0'),
  (940048, 'http://example.org/standard-ontology#InformationUnit', 'InformationUnit', '信息单元（抽象类）', '1', '1', 935001, 930001, 61, NULL, 'admin', now(), 'admin', now(), '0'),
  (940049, 'http://example.org/standard-ontology#AdditionalInformation', 'AdditionalInformation', '附加信息', '0', '1', 935001, 930001, 62, NULL, 'admin', now(), 'admin', now(), '0'),
  (940050, 'http://example.org/standard-ontology#Example', 'Example', '示例', '0', '1', 935001, 930001, 63, NULL, 'admin', now(), 'admin', now(), '0'),
  (940051, 'http://example.org/standard-ontology#Note', 'Note', '注', '0', '1', 935001, 930001, 64, NULL, 'admin', now(), 'admin', now(), '0'),
  (940052, 'http://example.org/standard-ontology#Footnote', 'Footnote', '脚注', '0', '1', 935001, 930001, 65, NULL, 'admin', now(), 'admin', now(), '0'),
  (940053, 'http://example.org/standard-ontology#Checklist', 'Checklist', '清单', '0', '1', 935001, 930001, 66, NULL, 'admin', now(), 'admin', now(), '0'),
  (940054, 'http://example.org/standard-ontology#List', 'List', '列表', '0', '1', 935001, 930001, 67, NULL, 'admin', now(), 'admin', now(), '0'),
  (940055, 'http://example.org/standard-ontology#InformationForm', 'InformationForm', '信息单元表述形式', '0', '1', 935001, 930001, 68, NULL, 'admin', now(), 'admin', now(), '0'),
  (940056, 'http://example.org/standard-ontology#TextualParagraph', 'TextualParagraph', '条文', '0', '1', 935001, 930001, 69, NULL, 'admin', now(), 'admin', now(), '0'),
  (940057, 'http://example.org/standard-ontology#Figure', 'Figure', '图', '0', '1', 935001, 930001, 70, NULL, 'admin', now(), 'admin', now(), '0'),
  (940058, 'http://example.org/standard-ontology#Table', 'Table', '表', '0', '1', 935001, 930001, 71, NULL, 'admin', now(), 'admin', now(), '0'),
  (940059, 'http://example.org/standard-ontology#MathematicalFormula', 'MathematicalFormula', '数学公式', '0', '1', 935001, 930001, 72, NULL, 'admin', now(), 'admin', now(), '0'),
  (940060, 'http://example.org/standard-ontology#Annex', 'Annex', '附录', '0', '1', 935001, 930001, 73, NULL, 'admin', now(), 'admin', now(), '0'),
  (940061, 'http://example.org/standard-ontology#Citation', 'Citation', '引用', '0', '1', 935001, 930001, 74, NULL, 'admin', now(), 'admin', now(), '0'),
  (940062, 'http://example.org/standard-ontology#Hint', 'Hint', '提示', '0', '1', 935001, 930001, 75, NULL, 'admin', now(), 'admin', now(), '0'),
  (940063, 'http://example.org/standard-ontology#Object', 'Object', '对象', '0', '1', 935001, 930001, 76, NULL, 'admin', now(), 'admin', now(), '0'),
  (940064, 'http://example.org/standard-ontology#StandardObject', 'StandardObject', '标准对象', '0', '1', 935001, 930001, 77, NULL, 'admin', now(), 'admin', now(), '0'),
  (940065, 'http://example.org/standard-ontology#IndicatorObject', 'IndicatorObject', '指标对象', '0', '1', 935001, 930001, 78, NULL, 'admin', now(), 'admin', now(), '0'),
  (940066, 'http://example.org/standard-ontology#Property', 'Property', '特性', '0', '1', 935001, 930001, 79, NULL, 'admin', now(), 'admin', now(), '0'),
  (940067, 'http://example.org/standard-ontology#Constraint', 'Constraint', '约束逻辑', '0', '1', 935001, 930001, 80, NULL, 'admin', now(), 'admin', now(), '0'),
  (940068, 'http://example.org/standard-ontology#ActionClass', 'ActionClass', '行动', '0', '1', 935001, 930001, 81, NULL, 'admin', now(), 'admin', now(), '0'),
  (940069, 'http://example.org/standard-ontology#Action', 'Action', '动作', '0', '1', 935001, 930001, 82, NULL, 'admin', now(), 'admin', now(), '0'),
  (940070, 'http://example.org/standard-ontology#Path', 'Path', '路径', '0', '1', 935001, 930001, 83, NULL, 'admin', now(), 'admin', now(), '0'),
  (940071, 'http://example.org/standard-ontology#Condition', 'Condition', '判断条件', '0', '1', 935001, 930001, 84, NULL, 'admin', now(), 'admin', now(), '0'),
  (940072, 'http://example.org/standard-ontology#ExternalResource', 'ExternalResource', '外部约束', '0', '1', 935001, 930001, 85, NULL, 'admin', now(), 'admin', now(), '0'),
  (940073, 'http://example.org/standard-ontology#Regulation', 'Regulation', '法规', '0', '1', 935001, 930001, 86, NULL, 'admin', now(), 'admin', now(), '0'),
  (940074, 'http://example.org/standard-ontology#Patent', 'Patent', '专利', '0', '1', 935001, 930001, 87, NULL, 'admin', now(), 'admin', now(), '0'),
  (940075, 'http://example.org/standard-ontology#Literature', 'Literature', '文献', '0', '1', 935001, 930001, 88, NULL, 'admin', now(), 'admin', now(), '0'),
  (940076, 'http://example.org/standard-ontology#PublicDatabase', 'PublicDatabase', '公共数据库', '0', '1', 935001, 930001, 89, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 第五组：制定程序（10条）
  (940077, 'http://example.org/standard-ontology#StandardizationProcess', 'StandardizationProcess', '制定程序', '0', '1', 935001, 930001, 90, NULL, 'admin', now(), 'admin', now(), '0'),
  (940078, 'http://example.org/standard-ontology#PreliminaryStage', 'PreliminaryStage', '预备阶段', '0', '1', 935001, 930001, 91, NULL, 'admin', now(), 'admin', now(), '0'),
  (940079, 'http://example.org/standard-ontology#ProposalStage', 'ProposalStage', '立项阶段', '0', '1', 935001, 930001, 92, NULL, 'admin', now(), 'admin', now(), '0'),
  (940080, 'http://example.org/standard-ontology#DraftingStage', 'DraftingStage', '起草阶段', '0', '1', 935001, 930001, 93, NULL, 'admin', now(), 'admin', now(), '0'),
  (940081, 'http://example.org/standard-ontology#ConsultingStage', 'ConsultingStage', '征求意见阶段', '0', '1', 935001, 930001, 94, NULL, 'admin', now(), 'admin', now(), '0'),
  (940082, 'http://example.org/standard-ontology#TechnicalReviewStage', 'TechnicalReviewStage', '技术审查阶段', '0', '1', 935001, 930001, 95, NULL, 'admin', now(), 'admin', now(), '0'),
  (940083, 'http://example.org/standard-ontology#ApprovalStage', 'ApprovalStage', '批准发布阶段', '0', '1', 935001, 930001, 96, NULL, 'admin', now(), 'admin', now(), '0'),
  (940084, 'http://example.org/standard-ontology#PublicationStage', 'PublicationStage', '出版阶段', '0', '1', 935001, 930001, 97, NULL, 'admin', now(), 'admin', now(), '0'),
  (940085, 'http://example.org/standard-ontology#ReviewStage', 'ReviewStage', '复审阶段', '0', '1', 935001, 930001, 98, NULL, 'admin', now(), 'admin', now(), '0'),
  (940086, 'http://example.org/standard-ontology#WithdrawalStage', 'WithdrawalStage', '废止阶段', '0', '1', 935001, 930001, 99, NULL, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 中文标签（zh locale，86条）
-- ----------------------------
INSERT INTO ont_entity_type_label (entity_type_id, locale, label) VALUES
  (940001, 'zh', '标准实体'), (940002, 'zh', '术语标准'), (940003, 'zh', '符号标准'), (940004, 'zh', '分类标准'), (940005, 'zh', '试验标准'),
  (940006, 'zh', '规范标准'), (940007, 'zh', '规程标准'), (940008, 'zh', '指南标准'), (940009, 'zh', '评价标准'),
  (940010, 'zh', '标准化对象'), (940011, 'zh', '相关方'), (940012, 'zh', '机构'), (940013, 'zh', '发布机构'),
  (940014, 'zh', '提出单位'), (940015, 'zh', '归口单位'), (940016, 'zh', '组织实施单位'), (940017, 'zh', '起草单位'),
  (940018, 'zh', '建议方'), (940019, 'zh', '征求意见单位'), (940020, 'zh', '出版机构'), (940021, 'zh', '个人'),
  (940022, 'zh', '起草人'), (940023, 'zh', '评审人'), (940024, 'zh', '领域类别'),
  (940025, 'zh', '国际标准分类'), (940026, 'zh', '中国标准文献分类'),
  (940027, 'zh', '内容要素'), (940028, 'zh', '规范性要素'), (940029, 'zh', '范围'), (940030, 'zh', '术语和定义'),
  (940031, 'zh', '符号和缩略语'), (940032, 'zh', '核心技术要素'), (940033, 'zh', '其他技术要素'),
  (940034, 'zh', '资料性要素'), (940035, 'zh', '规范性引用文件'), (940036, 'zh', '参考文献'), (940037, 'zh', '索引'),
  (940038, 'zh', '层次'), (940039, 'zh', '章'), (940040, 'zh', '条'), (940041, 'zh', '有标题条'),
  (940042, 'zh', '无标题条'), (940043, 'zh', '段'), (940044, 'zh', '列项'), (940045, 'zh', '无编号列项'), (940046, 'zh', '有编号列项'),
  (940047, 'zh', '术语'), (940048, 'zh', '信息单元'), (940049, 'zh', '附加信息'), (940050, 'zh', '示例'),
  (940051, 'zh', '注'), (940052, 'zh', '脚注'), (940053, 'zh', '清单'), (940054, 'zh', '列表'),
  (940055, 'zh', '信息单元表述形式'), (940056, 'zh', '条文'), (940057, 'zh', '图'), (940058, 'zh', '表'),
  (940059, 'zh', '数学公式'), (940060, 'zh', '附录'), (940061, 'zh', '引用'), (940062, 'zh', '提示'),
  (940063, 'zh', '对象'), (940064, 'zh', '标准对象'), (940065, 'zh', '指标对象'), (940066, 'zh', '特性'),
  (940067, 'zh', '约束逻辑'), (940068, 'zh', '行动'), (940069, 'zh', '动作'), (940070, 'zh', '路径'),
  (940071, 'zh', '判断条件'), (940072, 'zh', '外部约束'), (940073, 'zh', '法规'), (940074, 'zh', '专利'),
  (940075, 'zh', '文献'), (940076, 'zh', '公共数据库'),
  (940077, 'zh', '制定程序'), (940078, 'zh', '预备阶段'), (940079, 'zh', '立项阶段'), (940080, 'zh', '起草阶段'),
  (940081, 'zh', '征求意见阶段'), (940082, 'zh', '技术审查阶段'), (940083, 'zh', '批准发布阶段'),
  (940084, 'zh', '出版阶段'), (940085, 'zh', '复审阶段'), (940086, 'zh', '废止阶段')
ON CONFLICT (entity_type_id, locale) DO NOTHING;

-- ----------------------------
-- 继承关系（parent_id, child_id）
-- ----------------------------
INSERT INTO ont_entity_type_hierarchy (parent_id, child_id) VALUES
  -- 标准实体子类
  (940001, 940002), (940001, 940003), (940001, 940004), (940001, 940005), (940001, 940006), (940001, 940007), (940001, 940008), (940001, 940009),
  -- 相关方→机构→子类
  (940011, 940012), (940012, 940013), (940012, 940014), (940012, 940015), (940012, 940016), (940012, 940017), (940012, 940018), (940012, 940019), (940012, 940020),
  -- 相关方→个人→子类
  (940011, 940021), (940021, 940022), (940021, 940023),
  -- 领域类别→子类
  (940024, 940025), (940024, 940026),
  -- 要素→规范性/资料性
  (940027, 940028), (940028, 940029), (940028, 940030), (940028, 940031), (940028, 940032), (940028, 940033),
  (940027, 940034), (940034, 940035), (940034, 940036), (940034, 940037),
  -- 层次→子类
  (940038, 940039), (940038, 940040), (940048, 940040), (940040, 940041), (940040, 940042), (940038, 940043), (940038, 940044), (940044, 940045), (940044, 940046),
  -- 信息单元→附加信息→子类
  (940048, 940049), (940049, 940050), (940049, 940051), (940049, 940052), (940049, 940053), (940049, 940054),
  -- 信息单元表述形式→子类
  (940055, 940056), (940055, 940057), (940055, 940058), (940055, 940059), (940055, 940060), (940055, 940061), (940055, 940062),
  -- 对象→子类
  (940063, 940064), (940063, 940065),
  -- 行动→子类
  (940068, 940069), (940068, 940070), (940068, 940071),
  -- 外部约束→子类
  (940072, 940073), (940072, 940074), (940072, 940075), (940072, 940076),
  -- 制定程序→子类
  (940077, 940078), (940077, 940079), (940077, 940080), (940077, 940081), (940077, 940082), (940077, 940083), (940077, 940084), (940077, 940085), (940077, 940086)
ON CONFLICT (parent_id, child_id) DO NOTHING;

-- ----------------------------
-- 等价类
-- ----------------------------
-- 首期无核心等价类。Clause 通过同时继承 StructuralElement 与 InformationUnit 表达双重归属，
-- 不能与 InformationUnit 声明为等价类。

-- ----------------------------
-- 不相交类（规范性要素 ⇄ 资料性要素）
-- ----------------------------
INSERT INTO ont_entity_type_disjoint (type_a, type_b) VALUES
  (940028, 940034)
ON CONFLICT (type_a, type_b) DO NOTHING;


-- ----------------------------
-- 核心种子完整性断言
-- ----------------------------
DO $$
BEGIN
  IF (SELECT count(*) FROM ont_entity_type WHERE id BETWEEN 940001 AND 940086 AND is_builtin = '1' AND del_flag = '0') <> 86 THEN
    RAISE EXCEPTION '实体类型核心种子数量不是86条';
  END IF;
  IF (SELECT count(*) FROM ont_entity_type_label WHERE entity_type_id BETWEEN 940001 AND 940086 AND locale = 'zh') <> 86 THEN
    RAISE EXCEPTION '实体类型中文标签数量不是86条';
  END IF;
  IF (SELECT count(*) FROM ont_entity_type_hierarchy WHERE parent_id BETWEEN 940001 AND 940086 AND child_id BETWEEN 940001 AND 940086) <> 72 THEN
    RAISE EXCEPTION '实体类型核心继承关系数量不是72条';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_entity_type_hierarchy WHERE parent_id = 940038 AND child_id = 940040)
     OR NOT EXISTS (SELECT 1 FROM ont_entity_type_hierarchy WHERE parent_id = 940048 AND child_id = 940040) THEN
    RAISE EXCEPTION 'Clause 必须同时继承 StructuralElement 与 InformationUnit';
  END IF;
  IF EXISTS (SELECT 1 FROM ont_entity_type_equivalent WHERE (entity_type_id = 940040 AND equivalent_id = 940048) OR (entity_type_id = 940048 AND equivalent_id = 940040)) THEN
    RAISE EXCEPTION 'Clause 不能与 InformationUnit 声明为等价类';
  END IF;
  IF EXISTS (
    SELECT iri FROM ont_entity_type WHERE del_flag = '0' GROUP BY iri HAVING count(*) > 1
  ) THEN
    RAISE EXCEPTION '实体类型IRI存在重复';
  END IF;
  IF EXISTS (
    SELECT ontology_id, namespace_id, name FROM ont_entity_type WHERE del_flag = '0'
    GROUP BY ontology_id, namespace_id, name HAVING count(*) > 1
  ) THEN
    RAISE EXCEPTION '同一本体工程和命名空间下实体类型名称存在重复';
  END IF;
END $$;
