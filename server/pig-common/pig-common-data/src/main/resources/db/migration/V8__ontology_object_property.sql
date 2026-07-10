-- ----------------------------
-- youming ontology object property management
-- 依据：GB/T 48000.3—2026 第7.3条、表1（34个核心对象属性）+ 表1第2项语义说明派生1项逆属性
-- ----------------------------

-- 对象属性主表
CREATE TABLE ont_object_property (
  id bigint NOT NULL,
  iri varchar(512) NOT NULL,
  iri_local_name varchar(128) NOT NULL,
  name varchar(128) NOT NULL,
  definition varchar(512) DEFAULT NULL,
  inverse_of_id bigint DEFAULT NULL,
  is_functional char(1) NOT NULL DEFAULT '0',
  is_inverse_functional char(1) NOT NULL DEFAULT '0',
  is_transitive char(1) NOT NULL DEFAULT '0',
  is_symmetric char(1) NOT NULL DEFAULT '0',
  source_type varchar(32) NOT NULL,
  source_reference varchar(64) DEFAULT NULL,
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
  CONSTRAINT fk_ont_object_property_ontology FOREIGN KEY (ontology_id) REFERENCES ont_ontology_project (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_object_property_namespace FOREIGN KEY (namespace_id) REFERENCES ont_namespace (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_object_property_inverse FOREIGN KEY (inverse_of_id) REFERENCES ont_object_property (id) ON DELETE SET NULL,
  CONSTRAINT ck_ont_object_property_source_type CHECK (source_type IN ('GB_TABLE1', 'GB_TABLE1_DERIVED', 'EXTENSION')),
  CONSTRAINT ck_ont_object_property_is_functional CHECK (is_functional IN ('0', '1')),
  CONSTRAINT ck_ont_object_property_is_inverse_functional CHECK (is_inverse_functional IN ('0', '1')),
  CONSTRAINT ck_ont_object_property_is_transitive CHECK (is_transitive IN ('0', '1')),
  CONSTRAINT ck_ont_object_property_is_symmetric CHECK (is_symmetric IN ('0', '1')),
  CONSTRAINT ck_ont_object_property_builtin CHECK (is_builtin IN ('0', '1')),
  CONSTRAINT ck_ont_object_property_del_flag CHECK (del_flag IN ('0', '1')),
  CONSTRAINT ck_ont_object_property_sort_order CHECK (sort_order >= 0),
  CONSTRAINT ck_ont_object_property_inverse_self CHECK (inverse_of_id IS NULL OR inverse_of_id <> id),
  CONSTRAINT ck_ont_object_property_source_builtin CHECK (
    (source_type = 'EXTENSION' AND is_builtin = '0') OR
    (source_type <> 'EXTENSION' AND is_builtin = '1')
  ),
  CONSTRAINT ck_ont_object_property_transitive_func CHECK (
    is_transitive = '0' OR (is_functional = '0' AND is_inverse_functional = '0')
  )
);

COMMENT ON TABLE ont_object_property IS '本体建模-对象属性表';
COMMENT ON COLUMN ont_object_property.id IS '对象属性ID';
COMMENT ON COLUMN ont_object_property.iri IS '平台内部全局唯一IRI';
COMMENT ON COLUMN ont_object_property.iri_local_name IS 'IRI本地标识符，用于拼接iri';
COMMENT ON COLUMN ont_object_property.name IS '附录A.2 Name，核心保持表1英文名';
COMMENT ON COLUMN ont_object_property.definition IS '定义（表1语义说明）';
COMMENT ON COLUMN ont_object_property.inverse_of_id IS '逆属性ID，双向指向';
COMMENT ON COLUMN ont_object_property.is_functional IS '是否功能性属性，1是0否';
COMMENT ON COLUMN ont_object_property.is_inverse_functional IS '是否反功能性属性，1是0否';
COMMENT ON COLUMN ont_object_property.is_transitive IS '是否传递性属性，1是0否';
COMMENT ON COLUMN ont_object_property.is_symmetric IS '是否对称性属性，1是0否';
COMMENT ON COLUMN ont_object_property.source_type IS '来源类型：GB_TABLE1/GB_TABLE1_DERIVED/EXTENSION';
COMMENT ON COLUMN ont_object_property.source_reference IS '来源引用，如表1-1';
COMMENT ON COLUMN ont_object_property.is_builtin IS '是否内置，1是0否';
COMMENT ON COLUMN ont_object_property.ontology_id IS '本体工程ID';
COMMENT ON COLUMN ont_object_property.namespace_id IS '命名空间ID';
COMMENT ON COLUMN ont_object_property.sort_order IS '排序值';
COMMENT ON COLUMN ont_object_property.remarks IS '平台治理备注';
COMMENT ON COLUMN ont_object_property.create_by IS '创建人';
COMMENT ON COLUMN ont_object_property.create_time IS '创建时间';
COMMENT ON COLUMN ont_object_property.update_by IS '修改人';
COMMENT ON COLUMN ont_object_property.update_time IS '更新时间';
COMMENT ON COLUMN ont_object_property.del_flag IS '删除标志，0未删除，1已删除';

CREATE UNIQUE INDEX uk_ont_object_property_iri ON ont_object_property (iri) WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_object_property_local_name ON ont_object_property (ontology_id, namespace_id, iri_local_name) WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_object_property_name ON ont_object_property (ontology_id, name) WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_object_property_inverse ON ont_object_property (inverse_of_id) WHERE inverse_of_id IS NOT NULL AND del_flag = '0';
CREATE INDEX idx_ont_object_property_ontology ON ont_object_property (ontology_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_object_property_namespace ON ont_object_property (namespace_id) WHERE del_flag = '0';

-- 定义域关联表
CREATE TABLE ont_object_property_domain (
  object_property_id bigint NOT NULL,
  entity_type_id bigint NOT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  PRIMARY KEY (object_property_id, entity_type_id),
  CONSTRAINT fk_ont_obj_prop_domain_prop FOREIGN KEY (object_property_id) REFERENCES ont_object_property (id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_obj_prop_domain_entity FOREIGN KEY (entity_type_id) REFERENCES ont_entity_type (id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_obj_prop_domain_sort CHECK (sort_order >= 0)
);

COMMENT ON TABLE ont_object_property_domain IS '本体建模-对象属性定义域（并集）';
COMMENT ON COLUMN ont_object_property_domain.object_property_id IS '对象属性ID';
COMMENT ON COLUMN ont_object_property_domain.entity_type_id IS '定义域实体类型ID';
COMMENT ON COLUMN ont_object_property_domain.sort_order IS '并集成员排序';

CREATE INDEX idx_ont_obj_prop_domain_entity ON ont_object_property_domain (entity_type_id, object_property_id);

-- 值域关联表
CREATE TABLE ont_object_property_range (
  object_property_id bigint NOT NULL,
  entity_type_id bigint NOT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  PRIMARY KEY (object_property_id, entity_type_id),
  CONSTRAINT fk_ont_obj_prop_range_prop FOREIGN KEY (object_property_id) REFERENCES ont_object_property (id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_obj_prop_range_entity FOREIGN KEY (entity_type_id) REFERENCES ont_entity_type (id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_obj_prop_range_sort CHECK (sort_order >= 0)
);

COMMENT ON TABLE ont_object_property_range IS '本体建模-对象属性值域（并集）';
COMMENT ON COLUMN ont_object_property_range.object_property_id IS '对象属性ID';
COMMENT ON COLUMN ont_object_property_range.entity_type_id IS '值域实体类型ID';
COMMENT ON COLUMN ont_object_property_range.sort_order IS '并集成员排序';

CREATE INDEX idx_ont_obj_prop_range_entity ON ont_object_property_range (entity_type_id, object_property_id);

-- 多语言标签
CREATE TABLE ont_object_property_label (
  object_property_id bigint NOT NULL,
  locale varchar(16) NOT NULL,
  label varchar(128) NOT NULL,
  PRIMARY KEY (object_property_id, locale),
  CONSTRAINT fk_ont_obj_prop_label_prop FOREIGN KEY (object_property_id) REFERENCES ont_object_property (id) ON DELETE CASCADE,
  CONSTRAINT ck_ont_obj_prop_label_locale CHECK (btrim(locale) <> ''),
  CONSTRAINT ck_ont_obj_prop_label_text CHECK (btrim(label) <> '')
);

COMMENT ON TABLE ont_object_property_label IS '本体建模-对象属性多语言标签';
COMMENT ON COLUMN ont_object_property_label.object_property_id IS '对象属性ID';
COMMENT ON COLUMN ont_object_property_label.locale IS '语言区域，如zh、en';
COMMENT ON COLUMN ont_object_property_label.label IS '标签文本';

-- ----------------------------
-- 菜单：本体建模 / 对象属性管理
-- ----------------------------
INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES (900500, '对象属性管理', NULL, '/ontology/object-property/index', NULL, 900000, 'iconfont icon-shujujiegou', '1', 5, '0', NULL, '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES
  (900501, '对象属性查看', 'ontology_object_property_view', NULL, NULL, 900500, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900502, '对象属性新增', 'ontology_object_property_add', NULL, NULL, 900500, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900503, '对象属性修改', 'ontology_object_property_edit', NULL, NULL, 900500, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900504, '对象属性删除', 'ontology_object_property_del', NULL, NULL, 900500, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
  (1, 900500), (1, 900501), (1, 900502), (1, 900503), (1, 900504)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ----------------------------
-- 表1核心对象属性（34项，id 960001-960034）
-- 命名空间930001(std)、本体工程935001(core)
-- ----------------------------
INSERT INTO ont_object_property (id, iri, iri_local_name, name, definition, inverse_of_id, is_functional, is_inverse_functional, is_transitive, is_symmetric, source_type, source_reference, is_builtin, ontology_id, namespace_id, sort_order, remarks, create_by, create_time, update_by, update_time, del_flag) VALUES
  (960001, 'http://example.org/standard-ontology#adopts', 'adopts', 'adopts', '一个标准采用另一个标准作为其编制依据', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-1', '1', 935001, 930001, 10, NULL, 'admin', now(), 'admin', now(), '0'),
  (960002, 'http://example.org/standard-ontology#replaces', 'replaces', 'replaces', '一个标准代替另一个标准，其逆属性为被代替', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-2', '1', 935001, 930001, 11, NULL, 'admin', now(), 'admin', now(), '0'),
  (960003, 'http://example.org/standard-ontology#cites', 'cites', 'cites', '一个标准在正文中引用另一个标准', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-3', '1', 935001, 930001, 12, NULL, 'admin', now(), 'admin', now(), '0'),
  (960004, 'http://example.org/standard-ontology#references', 'references', 'references', '一个标准参考另一个标准作为参考资料', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-4', '1', 935001, 930001, 13, NULL, 'admin', now(), 'admin', now(), '0'),
  (960005, 'http://example.org/standard-ontology#hasPart', 'hasPart', 'hasPart', '一个标准包含另一个标准作为其部分', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-5', '1', 935001, 930001, 14, NULL, 'admin', now(), 'admin', now(), '0'),
  (960006, 'http://example.org/standard-ontology#issuedBy', 'issuedBy', 'issuedBy', '一个标准由某个机构发布', NULL, '1', '0', '0', '0', 'GB_TABLE1', '表1-6', '1', 935001, 930001, 15, '第8.2条示例：每个标准只能由一个机构发布', 'admin', now(), 'admin', now(), '0'),
  (960007, 'http://example.org/standard-ontology#proposedBy', 'proposedBy', 'proposedBy', '一个标准由某个机构提出', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-7', '1', 935001, 930001, 16, NULL, 'admin', now(), 'admin', now(), '0'),
  (960008, 'http://example.org/standard-ontology#administeredBy', 'administeredBy', 'administeredBy', '一个标准由某个机构归口管理', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-8', '1', 935001, 930001, 17, NULL, 'admin', now(), 'admin', now(), '0'),
  (960009, 'http://example.org/standard-ontology#draftedBy', 'draftedBy', 'draftedBy', '一个标准由机构或个人起草', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-9', '1', 935001, 930001, 18, NULL, 'admin', now(), 'admin', now(), '0'),
  (960010, 'http://example.org/standard-ontology#publishedBy', 'publishedBy', 'publishedBy', '一个标准由某个机构出版', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-10', '1', 935001, 930001, 19, NULL, 'admin', now(), 'admin', now(), '0'),
  (960011, 'http://example.org/standard-ontology#classifiedUnder', 'classifiedUnder', 'classifiedUnder', '一个标准属于某个领域分类', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-11', '1', 935001, 930001, 20, NULL, 'admin', now(), 'admin', now(), '0'),
  (960012, 'http://example.org/standard-ontology#standardizes', 'standardizes', 'standardizes', '一个标准标准化某个标准化对象', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-12', '1', 935001, 930001, 21, NULL, 'admin', now(), 'admin', now(), '0'),
  (960013, 'http://example.org/standard-ontology#hasNormativeElement', 'hasNormativeElement', 'hasNormativeElement', '一个标准包含某个规范性要素', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-13', '1', 935001, 930001, 22, NULL, 'admin', now(), 'admin', now(), '0'),
  (960014, 'http://example.org/standard-ontology#hasStructuralElement', 'hasStructuralElement', 'hasStructuralElement', '标准或层次结构包含层次结构元素', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-14', '1', 935001, 930001, 23, NULL, 'admin', now(), 'admin', now(), '0'),
  (960015, 'http://example.org/standard-ontology#hasClause', 'hasClause', 'hasClause', '要素或层次结构包含条款', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-15', '1', 935001, 930001, 24, NULL, 'admin', now(), 'admin', now(), '0'),
  (960016, 'http://example.org/standard-ontology#hasSubClause', 'hasSubClause', 'hasSubClause', '一个条款包含子条款', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-16', '1', 935001, 930001, 25, '非逆属性声明', 'admin', now(), 'admin', now(), '0'),
  (960017, 'http://example.org/standard-ontology#defines', 'defines', 'defines', '标准或要素界定术语', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-17', '1', 935001, 930001, 26, NULL, 'admin', now(), 'admin', now(), '0'),
  (960018, 'http://example.org/standard-ontology#usesTerm', 'usesTerm', 'usesTerm', '信息单元提及术语', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-18', '1', 935001, 930001, 27, NULL, 'admin', now(), 'admin', now(), '0'),
  (960019, 'http://example.org/standard-ontology#hasRepresentationForm', 'hasRepresentationForm', 'hasRepresentationForm', '信息单元具有表述形式', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-19', '1', 935001, 930001, 28, NULL, 'admin', now(), 'admin', now(), '0'),
  (960020, 'http://example.org/standard-ontology#hasExample', 'hasExample', 'hasExample', '条款包含示例', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-20', '1', 935001, 930001, 29, NULL, 'admin', now(), 'admin', now(), '0'),
  (960021, 'http://example.org/standard-ontology#hasNote', 'hasNote', 'hasNote', '条款包含注释', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-21', '1', 935001, 930001, 30, NULL, 'admin', now(), 'admin', now(), '0'),
  (960022, 'http://example.org/standard-ontology#citesStandard', 'citesStandard', 'citesStandard', '条款引用标准', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-22', '1', 935001, 930001, 31, NULL, 'admin', now(), 'admin', now(), '0'),
  (960023, 'http://example.org/standard-ontology#referencesClause', 'referencesClause', 'referencesClause', '信息单元引用章条', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-23', '1', 935001, 930001, 32, NULL, 'admin', now(), 'admin', now(), '0'),
  (960024, 'http://example.org/standard-ontology#involvesObject', 'involvesObject', 'involvesObject', '条款涉及对象', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-24', '1', 935001, 930001, 33, NULL, 'admin', now(), 'admin', now(), '0'),
  (960025, 'http://example.org/standard-ontology#specifiesCharacteristic', 'specifiesCharacteristic', 'specifiesCharacteristic', '条款规定特性', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-25', '1', 935001, 930001, 34, NULL, 'admin', now(), 'admin', now(), '0'),
  (960026, 'http://example.org/standard-ontology#hasCharacteristic', 'hasCharacteristic', 'hasCharacteristic', '对象具有特性', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-26', '1', 935001, 930001, 35, NULL, 'admin', now(), 'admin', now(), '0'),
  (960027, 'http://example.org/standard-ontology#imposesConstraint', 'imposesConstraint', 'imposesConstraint', '信息单元或特性施加约束', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-27', '1', 935001, 930001, 36, NULL, 'admin', now(), 'admin', now(), '0'),
  (960028, 'http://example.org/standard-ontology#constrainsObject', 'constrainsObject', 'constrainsObject', '约束逻辑约束对象', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-28', '1', 935001, 930001, 37, NULL, 'admin', now(), 'admin', now(), '0'),
  (960029, 'http://example.org/standard-ontology#constrainsCharacteristic', 'constrainsCharacteristic', 'constrainsCharacteristic', '约束逻辑约束特性', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-29', '1', 935001, 930001, 38, NULL, 'admin', now(), 'admin', now(), '0'),
  (960030, 'http://example.org/standard-ontology#describesAction', 'describesAction', 'describesAction', '信息单元描述行动', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-30', '1', 935001, 930001, 39, NULL, 'admin', now(), 'admin', now(), '0'),
  (960031, 'http://example.org/standard-ontology#referencesExternalResource', 'referencesExternalResource', 'referencesExternalResource', '标准或条款引用外部资源', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-31', '1', 935001, 930001, 40, NULL, 'admin', now(), 'admin', now(), '0'),
  (960032, 'http://example.org/standard-ontology#isRelatedToPatent', 'isRelatedToPatent', 'isRelatedToPatent', '条款与专利有关', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-32', '1', 935001, 930001, 41, NULL, 'admin', now(), 'admin', now(), '0'),
  (960033, 'http://example.org/standard-ontology#hasDevelopmentStage', 'hasDevelopmentStage', 'hasDevelopmentStage', '标准处于某个制定程序阶段', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-33', '1', 935001, 930001, 42, NULL, 'admin', now(), 'admin', now(), '0'),
  (960034, 'http://example.org/standard-ontology#includesStandard', 'includesStandard', 'includesStandard', '制定程序阶段包含标准', NULL, '0', '0', '0', '0', 'GB_TABLE1', '表1-34', '1', 935001, 930001, 43, NULL, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 表1第2项语义说明派生逆属性（1项，id 960035）
-- ----------------------------
INSERT INTO ont_object_property (id, iri, iri_local_name, name, definition, inverse_of_id, is_functional, is_inverse_functional, is_transitive, is_symmetric, source_type, source_reference, is_builtin, ontology_id, namespace_id, sort_order, remarks, create_by, create_time, update_by, update_time, del_flag) VALUES
  (960035, 'http://example.org/standard-ontology#isReplacedBy', 'isReplacedBy', 'isReplacedBy', '表示某标准被另一标准代替', NULL, '0', '0', '0', '0', 'GB_TABLE1_DERIVED', '表1-2语义说明', '1', 935001, 930001, 44, NULL, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 双向逆属性指针（插入全部35条主记录后更新，避免自引用外键顺序问题）
-- ----------------------------
UPDATE ont_object_property SET inverse_of_id = 960035 WHERE id = 960002 AND del_flag = '0';
UPDATE ont_object_property SET inverse_of_id = 960002 WHERE id = 960035 AND del_flag = '0';
UPDATE ont_object_property SET inverse_of_id = 960034 WHERE id = 960033 AND del_flag = '0';
UPDATE ont_object_property SET inverse_of_id = 960033 WHERE id = 960034 AND del_flag = '0';

-- ----------------------------
-- 中文标签（35条）
-- ----------------------------
INSERT INTO ont_object_property_label (object_property_id, locale, label) VALUES
  (960001, 'zh', '采用'), (960002, 'zh', '代替'), (960003, 'zh', '引用'), (960004, 'zh', '参考'),
  (960005, 'zh', '有部分'), (960006, 'zh', '发布于'), (960007, 'zh', '提出于'), (960008, 'zh', '归口于'),
  (960009, 'zh', '起草于'), (960010, 'zh', '出版于'), (960011, 'zh', '属于领域'), (960012, 'zh', '标准化对象'),
  (960013, 'zh', '包含要素'), (960014, 'zh', '包含层次'), (960015, 'zh', '包含条款'), (960016, 'zh', '包含子条'),
  (960017, 'zh', '界定'), (960018, 'zh', '提及术语'), (960019, 'zh', '具有表述形式'), (960020, 'zh', '有示例'),
  (960021, 'zh', '有注'), (960022, 'zh', '引用标准'), (960023, 'zh', '引用章条'), (960024, 'zh', '涉及对象'),
  (960025, 'zh', '规定特性'), (960026, 'zh', '有特性'), (960027, 'zh', '施加约束'), (960028, 'zh', '约束对象'),
  (960029, 'zh', '约束特性'), (960030, 'zh', '描述行动'), (960031, 'zh', '引用外部资源'), (960032, 'zh', '与专利有关'),
  (960033, 'zh', '处于阶段'), (960034, 'zh', '包含标准'), (960035, 'zh', '被代替')
ON CONFLICT (object_property_id, locale) DO NOTHING;

-- ----------------------------
-- 定义域关联（按§4.2精确映射，斜杠为并集）
-- ----------------------------
INSERT INTO ont_object_property_domain (object_property_id, entity_type_id, sort_order) VALUES
  (960001, 940001, 1),
  (960002, 940001, 1),
  (960003, 940001, 1),
  (960004, 940001, 1),
  (960005, 940001, 1),
  (960006, 940001, 1),
  (960007, 940001, 1),
  (960008, 940001, 1),
  (960009, 940001, 1),
  (960010, 940001, 1),
  (960011, 940001, 1),
  (960012, 940001, 1),
  (960013, 940001, 1),
  (960014, 940001, 1), (960014, 940038, 2),
  (960015, 940027, 1), (960015, 940038, 2),
  (960016, 940040, 1),
  (960017, 940001, 1), (960017, 940027, 2),
  (960018, 940048, 1),
  (960019, 940048, 1),
  (960020, 940040, 1),
  (960021, 940040, 1),
  (960022, 940040, 1),
  (960023, 940048, 1),
  (960024, 940040, 1),
  (960025, 940040, 1),
  (960026, 940063, 1),
  (960027, 940048, 1), (960027, 940066, 2),
  (960028, 940067, 1),
  (960029, 940067, 1),
  (960030, 940048, 1),
  (960031, 940001, 1), (960031, 940040, 2),
  (960032, 940040, 1),
  (960033, 940001, 1),
  (960034, 940077, 1),
  (960035, 940001, 1)
ON CONFLICT (object_property_id, entity_type_id) DO NOTHING;

-- ----------------------------
-- 值域关联（按§4.2精确映射，斜杠为并集）
-- ----------------------------
INSERT INTO ont_object_property_range (object_property_id, entity_type_id, sort_order) VALUES
  (960001, 940001, 1),
  (960002, 940001, 1),
  (960003, 940001, 1),
  (960004, 940001, 1),
  (960005, 940001, 1),
  (960006, 940012, 1),
  (960007, 940012, 1),
  (960008, 940012, 1),
  (960009, 940012, 1), (960009, 940021, 2),
  (960010, 940012, 1),
  (960011, 940024, 1),
  (960012, 940010, 1),
  (960013, 940027, 1),
  (960014, 940038, 1),
  (960015, 940040, 1),
  (960016, 940040, 1),
  (960017, 940047, 1),
  (960018, 940047, 1),
  (960019, 940055, 1),
  (960020, 940049, 1),
  (960021, 940049, 1),
  (960022, 940001, 1),
  (960023, 940038, 1),
  (960024, 940063, 1),
  (960025, 940066, 1),
  (960026, 940066, 1),
  (960027, 940067, 1),
  (960028, 940063, 1),
  (960029, 940066, 1),
  (960030, 940068, 1),
  (960031, 940072, 1),
  (960032, 940074, 1),
  (960033, 940077, 1),
  (960034, 940001, 1),
  (960035, 940001, 1)
ON CONFLICT (object_property_id, entity_type_id) DO NOTHING;

-- ----------------------------
-- 核心种子完整性断言
-- ----------------------------
DO $$
BEGIN
  -- 1. 表1核心对象属性恰好34条
  IF (SELECT count(*) FROM ont_object_property WHERE source_type = 'GB_TABLE1' AND del_flag = '0') <> 34 THEN
    RAISE EXCEPTION '表1核心对象属性数量不是34条';
  END IF;
  -- 2. 表1派生逆属性恰好1条且为960035；受保护预置总数35
  IF (SELECT count(*) FROM ont_object_property WHERE source_type = 'GB_TABLE1_DERIVED' AND del_flag = '0') <> 1 THEN
    RAISE EXCEPTION '表1派生逆属性数量不是1条';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_object_property WHERE id = 960035 AND source_type = 'GB_TABLE1_DERIVED') THEN
    RAISE EXCEPTION '表1派生逆属性ID必须为960035';
  END IF;
  IF (SELECT count(*) FROM ont_object_property WHERE is_builtin = '1' AND del_flag = '0') <> 35 THEN
    RAISE EXCEPTION '受保护预置对象属性总数不是35条';
  END IF;
  -- 3. 35条均有非空定义、至少一个定义域和值域
  IF EXISTS (SELECT 1 FROM ont_object_property WHERE is_builtin = '1' AND del_flag = '0' AND btrim(definition) IS NULL) THEN
    RAISE EXCEPTION '受保护对象属性存在空定义';
  END IF;
  IF EXISTS (
    SELECT 1 FROM ont_object_property op WHERE op.is_builtin = '1' AND op.del_flag = '0'
    AND NOT EXISTS (SELECT 1 FROM ont_object_property_domain d WHERE d.object_property_id = op.id)
  ) THEN
    RAISE EXCEPTION '受保护对象属性存在缺少定义域的记录';
  END IF;
  IF EXISTS (
    SELECT 1 FROM ont_object_property op WHERE op.is_builtin = '1' AND op.del_flag = '0'
    AND NOT EXISTS (SELECT 1 FROM ont_object_property_range r WHERE r.object_property_id = op.id)
  ) THEN
    RAISE EXCEPTION '受保护对象属性存在缺少值域的记录';
  END IF;
  -- 4. 中文标签35条
  IF (SELECT count(*) FROM ont_object_property_label WHERE locale = 'zh' AND object_property_id IN (SELECT id FROM ont_object_property WHERE is_builtin = '1' AND del_flag = '0')) <> 35 THEN
    RAISE EXCEPTION '对象属性中文标签数量不是35条';
  END IF;
  -- 5. 复合声明精确存在：960009双值域
  IF (SELECT count(*) FROM ont_object_property_range WHERE object_property_id = 960009) <> 2 THEN
    RAISE EXCEPTION '960009 draftedBy值域必须为2条（Organization或Individual）';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_object_property_range WHERE object_property_id = 960009 AND entity_type_id = 940021) THEN
    RAISE EXCEPTION '960009 draftedBy值域必须包含Individual(940021)';
  END IF;
  -- 960014、960015、960017、960027、960031双定义域
  IF (SELECT count(*) FROM ont_object_property_domain WHERE object_property_id = 960014) <> 2 THEN
    RAISE EXCEPTION '960014 hasStructuralElement定义域必须为2条';
  END IF;
  IF (SELECT count(*) FROM ont_object_property_domain WHERE object_property_id = 960015) <> 2 THEN
    RAISE EXCEPTION '960015 hasClause定义域必须为2条';
  END IF;
  IF (SELECT count(*) FROM ont_object_property_domain WHERE object_property_id = 960017) <> 2 THEN
    RAISE EXCEPTION '960017 defines定义域必须为2条';
  END IF;
  IF (SELECT count(*) FROM ont_object_property_domain WHERE object_property_id = 960027) <> 2 THEN
    RAISE EXCEPTION '960027 imposesConstraint定义域必须为2条';
  END IF;
  IF (SELECT count(*) FROM ont_object_property_domain WHERE object_property_id = 960031) <> 2 THEN
    RAISE EXCEPTION '960031 referencesExternalResource定义域必须为2条';
  END IF;
  -- 6. issuedBy(960006)仅明确标记功能性
  IF NOT EXISTS (SELECT 1 FROM ont_object_property WHERE id = 960006 AND is_functional = '1') THEN
    RAISE EXCEPTION '960006 issuedBy必须标记为功能性';
  END IF;
  -- 7. 960001～960035中不存在预设传递性或对称性
  IF EXISTS (SELECT 1 FROM ont_object_property WHERE id BETWEEN 960001 AND 960035 AND (is_transitive = '1' OR is_symmetric = '1')) THEN
    RAISE EXCEPTION '核心对象属性不应预设传递性或对称性';
  END IF;
  -- hasDevelopmentStage/includesStandard不预设功能/反功能
  IF EXISTS (SELECT 1 FROM ont_object_property WHERE id = 960033 AND (is_functional = '1' OR is_inverse_functional = '1')) THEN
    RAISE EXCEPTION '960033 hasDevelopmentStage不应预设功能/反功能性';
  END IF;
  IF EXISTS (SELECT 1 FROM ont_object_property WHERE id = 960034 AND (is_functional = '1' OR is_inverse_functional = '1')) THEN
    RAISE EXCEPTION '960034 includesStandard不应预设功能/反功能性';
  END IF;
  -- 8. replaces(960002)与isReplacedBy(960035)双向互逆
  IF NOT EXISTS (SELECT 1 FROM ont_object_property WHERE id = 960002 AND inverse_of_id = 960035) THEN
    RAISE EXCEPTION '960002 replaces的逆属性必须指向960035';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_object_property WHERE id = 960035 AND inverse_of_id = 960002) THEN
    RAISE EXCEPTION '960035 isReplacedBy的逆属性必须指向960002';
  END IF;
  -- 9. hasDevelopmentStage(960033)与includesStandard(960034)双向互逆
  IF NOT EXISTS (SELECT 1 FROM ont_object_property WHERE id = 960033 AND inverse_of_id = 960034) THEN
    RAISE EXCEPTION '960033 hasDevelopmentStage的逆属性必须指向960034';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_object_property WHERE id = 960034 AND inverse_of_id = 960033) THEN
    RAISE EXCEPTION '960034 includesStandard的逆属性必须指向960033';
  END IF;
  -- hasSubClause(960016)不是逆属性声明
  IF EXISTS (SELECT 1 FROM ont_object_property WHERE id = 960016 AND inverse_of_id IS NOT NULL) THEN
    RAISE EXCEPTION '960016 hasSubClause不应声明逆属性';
  END IF;
  -- 10. 所有IRI等于命名空间URI加iri_local_name，与现有实体类型/数据属性IRI无冲突
  IF EXISTS (
    SELECT 1 FROM ont_object_property op
    JOIN ont_namespace ns ON op.namespace_id = ns.id
    WHERE op.del_flag = '0' AND op.iri <> ns.uri || op.iri_local_name
  ) THEN
    RAISE EXCEPTION '对象属性IRI不等于命名空间URI加iri_local_name';
  END IF;
  -- 对象属性IRI与实体类型IRI无冲突
  IF EXISTS (
    SELECT 1 FROM ont_object_property op
    JOIN ont_entity_type et ON op.iri = et.iri
    WHERE op.del_flag = '0' AND et.del_flag = '0'
  ) THEN
    RAISE EXCEPTION '对象属性IRI与实体类型IRI冲突';
  END IF;
  -- 对象属性IRI与数据属性IRI无冲突
  IF EXISTS (
    SELECT 1 FROM ont_object_property op
    JOIN ont_data_property dp ON op.iri = dp.iri
    WHERE op.del_flag = '0' AND dp.del_flag = '0'
  ) THEN
    RAISE EXCEPTION '对象属性IRI与数据属性IRI冲突';
  END IF;
  -- 11. 不存在传递且功能/反功能的属性；不存在自逆引用或单向逆引用
  IF EXISTS (SELECT 1 FROM ont_object_property WHERE is_transitive = '1' AND (is_functional = '1' OR is_inverse_functional = '1') AND del_flag = '0') THEN
    RAISE EXCEPTION '存在传递且功能/反功能的对象属性';
  END IF;
  IF EXISTS (SELECT 1 FROM ont_object_property WHERE inverse_of_id = id AND del_flag = '0') THEN
    RAISE EXCEPTION '存在自逆引用的对象属性';
  END IF;
  IF EXISTS (
    SELECT 1 FROM ont_object_property a
    JOIN ont_object_property b ON a.inverse_of_id = b.id
    WHERE a.del_flag = '0' AND b.del_flag = '0'
    AND (b.inverse_of_id IS NULL OR b.inverse_of_id <> a.id)
  ) THEN
    RAISE EXCEPTION '存在单向逆引用的对象属性';
  END IF;
  -- 12. 菜单900500～900504及管理员授权完整
  IF (SELECT count(*) FROM sys_menu WHERE menu_id BETWEEN 900500 AND 900504 AND del_flag = '0') <> 5 THEN
    RAISE EXCEPTION '对象属性菜单900500～900504不完整';
  END IF;
  IF (SELECT count(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 900500 AND 900504) <> 5 THEN
    RAISE EXCEPTION '对象属性管理员授权不完整';
  END IF;
END $$;
