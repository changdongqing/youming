-- ----------------------------
-- youming ontology data property management
-- 依据：GB/T 48000.3—2026 附录C 47项 + 第6.2.5.1条条款直采2项 + 附录D兼容7项
-- ----------------------------

-- 数据属性主表
CREATE TABLE ont_data_property (
  id bigint NOT NULL,
  iri varchar(512) NOT NULL,
  iri_local_name varchar(128) NOT NULL,
  standard_iri varchar(512) DEFAULT NULL,
  name varchar(128) NOT NULL,
  preferred_alias varchar(128) DEFAULT NULL,
  definition varchar(512) DEFAULT NULL,
  domain_entity_type_id bigint NOT NULL,
  base_type varchar(32) NOT NULL,
  value_mode varchar(32) NOT NULL,
  value_source_ref varchar(64) DEFAULT NULL,
  regex_pattern varchar(512) DEFAULT NULL,
  format_hint varchar(255) DEFAULT NULL,
  is_unique char(1) NOT NULL DEFAULT '0',
  unit_category_id bigint DEFAULT NULL,
  unit_ref_mode varchar(32) DEFAULT NULL,
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
  CONSTRAINT fk_ont_data_property_ontology FOREIGN KEY (ontology_id) REFERENCES ont_ontology_project (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_data_property_namespace FOREIGN KEY (namespace_id) REFERENCES ont_namespace (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_data_property_domain FOREIGN KEY (domain_entity_type_id) REFERENCES ont_entity_type (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_data_property_unit_category FOREIGN KEY (unit_category_id) REFERENCES ont_unit_category (id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_data_property_base_type CHECK (base_type IN ('BOOLEAN', 'DATE', 'NUMERIC', 'TEXT', 'URI', 'UNIT_REF', 'TEXT_OR_NUMERIC')),
  CONSTRAINT ck_ont_data_property_value_mode CHECK (value_mode IN ('FREE', 'CLOSED_ENUM', 'OPEN_ENUM', 'EXTERNAL_DICTIONARY', 'UNIT_DICTIONARY')),
  CONSTRAINT ck_ont_data_property_source_type CHECK (source_type IN ('APPENDIX_C', 'CLAUSE_REQUIRED', 'APPENDIX_D_COMPAT', 'EXTENSION')),
  CONSTRAINT ck_ont_data_property_is_unique CHECK (is_unique IN ('0', '1')),
  CONSTRAINT ck_ont_data_property_builtin CHECK (is_builtin IN ('0', '1')),
  CONSTRAINT ck_ont_data_property_del_flag CHECK (del_flag IN ('0', '1')),
  CONSTRAINT ck_ont_data_property_sort_order CHECK (sort_order >= 0),
  CONSTRAINT ck_ont_data_property_unit_ref_mode CHECK (
    (unit_ref_mode IS NULL AND base_type <> 'UNIT_REF') OR
    (unit_ref_mode = 'DICTIONARY_SYMBOL' AND base_type = 'UNIT_REF')
  ),
  CONSTRAINT ck_ont_data_property_unit_category CHECK (
    (unit_category_id IS NULL) OR
    (base_type IN ('NUMERIC', 'UNIT_REF'))
  ),
  CONSTRAINT ck_ont_data_property_unit_dict CHECK (
    (value_mode <> 'UNIT_DICTIONARY') OR
    (base_type = 'UNIT_REF' AND unit_ref_mode = 'DICTIONARY_SYMBOL')
  ),
  CONSTRAINT ck_ont_data_property_source_builtin CHECK (
    (source_type = 'EXTENSION' AND is_builtin = '0') OR
    (source_type <> 'EXTENSION' AND is_builtin = '1')
  )
);

COMMENT ON TABLE ont_data_property IS '本体建模-数据属性表';
COMMENT ON COLUMN ont_data_property.id IS '数据属性ID';
COMMENT ON COLUMN ont_data_property.iri IS '平台内部全局唯一IRI';
COMMENT ON COLUMN ont_data_property.iri_local_name IS 'IRI本地标识符，用于拼接iri';
COMMENT ON COLUMN ont_data_property.standard_iri IS '国标原始IRI，允许因国标重名而重复';
COMMENT ON COLUMN ont_data_property.name IS '附录A.2 Name，核心值保持国标原名';
COMMENT ON COLUMN ont_data_property.preferred_alias IS 'UI/兼容导入导出的首选别名，如measurementUnit';
COMMENT ON COLUMN ont_data_property.definition IS '定义（附录A.2描述项4）';
COMMENT ON COLUMN ont_data_property.domain_entity_type_id IS '定义域实体类型ID，引用ont_entity_type.id';
COMMENT ON COLUMN ont_data_property.base_type IS '基本数据类型：BOOLEAN/DATE/NUMERIC/TEXT/URI/UNIT_REF/TEXT_OR_NUMERIC';
COMMENT ON COLUMN ont_data_property.value_mode IS '值模式：FREE/CLOSED_ENUM/OPEN_ENUM/EXTERNAL_DICTIONARY/UNIT_DICTIONARY';
COMMENT ON COLUMN ont_data_property.value_source_ref IS '外部值源代码，如ICS、CCS';
COMMENT ON COLUMN ont_data_property.regex_pattern IS '正则约束';
COMMENT ON COLUMN ont_data_property.format_hint IS '人类可读格式提示';
COMMENT ON COLUMN ont_data_property.is_unique IS '实例值是否在本体工程内唯一，1是0否';
COMMENT ON COLUMN ont_data_property.unit_category_id IS '单位分类ID，引用ont_unit_category.id，NULL表示不限分类';
COMMENT ON COLUMN ont_data_property.unit_ref_mode IS '单位引用模式，仅UNIT_REF使用DICTIONARY_SYMBOL';
COMMENT ON COLUMN ont_data_property.source_type IS '来源类型：APPENDIX_C/CLAUSE_REQUIRED/APPENDIX_D_COMPAT/EXTENSION';
COMMENT ON COLUMN ont_data_property.source_reference IS '来源引用，如C.41、6.2.5.1、D-Step5';
COMMENT ON COLUMN ont_data_property.is_builtin IS '是否内置，1是0否';
COMMENT ON COLUMN ont_data_property.ontology_id IS '本体工程ID，引用ont_ontology_project.id';
COMMENT ON COLUMN ont_data_property.namespace_id IS '命名空间ID，引用ont_namespace.id';
COMMENT ON COLUMN ont_data_property.sort_order IS '排序值';
COMMENT ON COLUMN ont_data_property.remarks IS '平台治理备注，不作为标准定义导出';
COMMENT ON COLUMN ont_data_property.create_by IS '创建人';
COMMENT ON COLUMN ont_data_property.create_time IS '创建时间';
COMMENT ON COLUMN ont_data_property.update_by IS '修改人';
COMMENT ON COLUMN ont_data_property.update_time IS '更新时间';
COMMENT ON COLUMN ont_data_property.del_flag IS '删除标志，0未删除，1已删除';

CREATE UNIQUE INDEX uk_ont_data_property_iri ON ont_data_property (iri) WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_data_property_local_name ON ont_data_property (ontology_id, namespace_id, iri_local_name) WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_data_property_domain_name ON ont_data_property (ontology_id, namespace_id, domain_entity_type_id, name) WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_data_property_preferred_alias ON ont_data_property (ontology_id, namespace_id, preferred_alias) WHERE preferred_alias IS NOT NULL AND del_flag = '0';
CREATE INDEX idx_ont_data_property_standard_iri ON ont_data_property (standard_iri) WHERE standard_iri IS NOT NULL AND del_flag = '0';
CREATE INDEX idx_ont_data_property_domain ON ont_data_property (domain_entity_type_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_data_property_ontology ON ont_data_property (ontology_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_data_property_namespace ON ont_data_property (namespace_id) WHERE del_flag = '0';

-- 多语言标签
CREATE TABLE ont_data_property_label (
  data_property_id bigint NOT NULL,
  locale varchar(16) NOT NULL,
  label varchar(128) NOT NULL,
  PRIMARY KEY (data_property_id, locale),
  CONSTRAINT fk_ont_data_property_label_prop FOREIGN KEY (data_property_id) REFERENCES ont_data_property (id) ON DELETE CASCADE,
  CONSTRAINT ck_ont_data_property_label_locale CHECK (btrim(locale) <> ''),
  CONSTRAINT ck_ont_data_property_label_text CHECK (btrim(label) <> '')
);

COMMENT ON TABLE ont_data_property_label IS '本体建模-数据属性多语言标签';
COMMENT ON COLUMN ont_data_property_label.data_property_id IS '数据属性ID';
COMMENT ON COLUMN ont_data_property_label.locale IS '语言区域，如zh、en';
COMMENT ON COLUMN ont_data_property_label.label IS '标签文本';

-- 枚举值
CREATE TABLE ont_data_property_enum (
  data_property_id bigint NOT NULL,
  enum_value varchar(256) NOT NULL,
  canonical_value varchar(256) DEFAULT NULL,
  is_standard char(1) NOT NULL DEFAULT '1',
  source_reference varchar(64) DEFAULT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  PRIMARY KEY (data_property_id, enum_value),
  CONSTRAINT fk_ont_data_property_enum_prop FOREIGN KEY (data_property_id) REFERENCES ont_data_property (id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_data_property_enum_canonical FOREIGN KEY (data_property_id, canonical_value) REFERENCES ont_data_property_enum (data_property_id, enum_value),
  CONSTRAINT ck_ont_data_property_enum_is_standard CHECK (is_standard IN ('0', '1')),
  CONSTRAINT ck_ont_data_property_enum_sort_order CHECK (sort_order >= 0)
);

COMMENT ON TABLE ont_data_property_enum IS '本体建模-数据属性枚举值';
COMMENT ON COLUMN ont_data_property_enum.data_property_id IS '数据属性ID';
COMMENT ON COLUMN ont_data_property_enum.enum_value IS '可接受的枚举词法值';
COMMENT ON COLUMN ont_data_property_enum.canonical_value IS '归一后的标准值，标准值本身为NULL';
COMMENT ON COLUMN ont_data_property_enum.is_standard IS '1来自附录C，0为兼容别名或平台推荐';
COMMENT ON COLUMN ont_data_property_enum.source_reference IS '来源引用，如C.37、D-Step5';
COMMENT ON COLUMN ont_data_property_enum.sort_order IS '排序值';

-- ----------------------------
-- 菜单：本体建模 / 数据属性管理（复用 900000 顶级目录）
-- ----------------------------
INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES (900400, '数据属性管理', NULL, '/ontology/data-property/index', NULL, 900000, 'iconfont icon-shujujiegou', '1', 4, '0', NULL, '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES
  (900401, '数据属性查看', 'ontology_data_property_view', NULL, NULL, 900400, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900402, '数据属性新增', 'ontology_data_property_add', NULL, NULL, 900400, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900403, '数据属性修改', 'ontology_data_property_edit', NULL, NULL, 900400, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900404, '数据属性删除', 'ontology_data_property_del', NULL, NULL, 900400, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
  (1, 900400),
  (1, 900401),
  (1, 900402),
  (1, 900403),
  (1, 900404)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ----------------------------
-- 附录C 47项（id 950001-950047，source_type=APPENDIX_C）
-- 依据：GB/T 48000.3—2026 附录C 表C.1～C.47
-- 命名空间930001(std)、本体工程935001(core)
-- ----------------------------
INSERT INTO ont_data_property (id, iri, iri_local_name, standard_iri, name, preferred_alias, definition, domain_entity_type_id, base_type, value_mode, value_source_ref, regex_pattern, format_hint, is_unique, unit_category_id, unit_ref_mode, source_type, source_reference, is_builtin, ontology_id, namespace_id, sort_order, remarks, create_by, create_time, update_by, update_time, del_flag) VALUES
  -- C.1～C.8：标准实体（8项）
  (950001, 'http://example.org/standard-ontology#purpose', 'purpose', 'http://example.org/standard-ontology#purpose', 'purpose', NULL, '说明该标准的制定目标', 940001, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.1', '1', 935001, 930001, 10, NULL, 'admin', now(), 'admin', now(), '0'),
  (950002, 'http://example.org/standard-ontology#languageVersion', 'languageVersion', 'http://example.org/standard-ontology#languageVersion', 'languageVersion', NULL, '标准发布的语言版本', 940001, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.2', '1', 935001, 930001, 11, NULL, 'admin', now(), 'admin', now(), '0'),
  (950003, 'http://example.org/standard-ontology#status', 'status', 'http://example.org/standard-ontology#status', 'status', NULL, '标识标准的状态', 940001, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.3', '1', 935001, 930001, 12, NULL, 'admin', now(), 'admin', now(), '0'),
  (950004, 'http://example.org/standard-ontology#constraintTypeStandard', 'constraintTypeStandard', 'http://example.org/standard-ontology#constraintType', 'constraintType', NULL, '规定标准的约束级别', 940001, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.4', '1', 935001, 930001, 13, '内部本地名消歧：Standard定义域', 'admin', now(), 'admin', now(), '0'),
  (950005, 'http://example.org/standard-ontology#documentName', 'documentName', 'http://example.org/standard-ontology#documentName', 'documentName', NULL, '标准的完整名称', 940001, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.5', '1', 935001, 930001, 14, NULL, 'admin', now(), 'admin', now(), '0'),
  (950006, 'http://example.org/standard-ontology#standardNumber', 'standardNumber', 'http://example.org/standard-ontology#standardNumber', 'standardNumber', NULL, '符合GB/T1.1的编号格式', 940001, 'TEXT', 'FREE', NULL, NULL, '标准代号+顺序号+发布年份', '1', NULL, NULL, 'APPENDIX_C', 'C.6', '1', 935001, 930001, 15, NULL, 'admin', now(), 'admin', now(), '0'),
  (950007, 'http://example.org/standard-ontology#issuedDate', 'issuedDate', 'http://example.org/standard-ontology#issuedDate', 'issuedDate', NULL, '标准的官方发布日期，符合GB/T7408日期格式', 940001, 'DATE', 'FREE', NULL, NULL, 'YYYYMMDD', '0', NULL, NULL, 'APPENDIX_C', 'C.7', '1', 935001, 930001, 16, NULL, 'admin', now(), 'admin', now(), '0'),
  (950008, 'http://example.org/standard-ontology#effectiveDate', 'effectiveDate', 'http://example.org/standard-ontology#effectiveDate', 'effectiveDate', NULL, '标准的实施日期', 940001, 'DATE', 'FREE', NULL, NULL, 'YYYYMMDD', '0', NULL, NULL, 'APPENDIX_C', 'C.8', '1', 935001, 930001, 17, '后续公理：不早于发布日期', 'admin', now(), 'admin', now(), '0'),
  -- C.9～C.10：标准化对象（2项）
  (950009, 'http://example.org/standard-ontology#subjectName', 'subjectName', 'http://example.org/standard-ontology#subjectName', 'subjectName', NULL, '标准化对象的名称', 940010, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.9', '1', 935001, 930001, 20, NULL, 'admin', now(), 'admin', now(), '0'),
  (950010, 'http://example.org/standard-ontology#industrialSector', 'industrialSector', 'http://example.org/standard-ontology#industrialSector', 'industrialSector', NULL, '标准化对象所属行业，参考GB/T4754', 940010, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.10', '1', 935001, 930001, 21, NULL, 'admin', now(), 'admin', now(), '0'),
  -- C.11～C.17：相关方（7项，机构4项+个人3项）
  (950011, 'http://example.org/standard-ontology#orgName', 'orgName', 'http://example.org/standard-ontology#orgName', 'orgName', NULL, '机构名称', 940012, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.11', '1', 935001, 930001, 30, NULL, 'admin', now(), 'admin', now(), '0'),
  (950012, 'http://example.org/standard-ontology#creditCode', 'creditCode', 'http://example.org/standard-ontology#creditCode', 'creditCode', NULL, '统一社会信用代码', 940012, 'TEXT', 'FREE', NULL, '^[A-Z0-9]{18}$', NULL, '1', NULL, NULL, 'APPENDIX_C', 'C.12', '1', 935001, 930001, 31, NULL, 'admin', now(), 'admin', now(), '0'),
  (950013, 'http://example.org/standard-ontology#orgLocation', 'orgLocation', 'http://example.org/standard-ontology#orgLocation', 'orgLocation', NULL, '机构所在地', 940012, 'TEXT', 'FREE', NULL, NULL, '省/市', '0', NULL, NULL, 'APPENDIX_C', 'C.13', '1', 935001, 930001, 32, NULL, 'admin', now(), 'admin', now(), '0'),
  (950014, 'http://example.org/standard-ontology#personName', 'personName', 'http://example.org/standard-ontology#personName', 'personName', NULL, '个人姓名', 940021, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.14', '1', 935001, 930001, 33, NULL, 'admin', now(), 'admin', now(), '0'),
  (950015, 'http://example.org/standard-ontology#affiliation', 'affiliation', 'http://example.org/standard-ontology#affiliation', 'affiliation', NULL, '所属单位', 940021, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.15', '1', 935001, 930001, 34, NULL, 'admin', now(), 'admin', now(), '0'),
  (950016, 'http://example.org/standard-ontology#phone', 'phone', 'http://example.org/standard-ontology#phone', 'phone', NULL, '联系电话', 940021, 'TEXT', 'FREE', NULL, NULL, '电话号码格式', '0', NULL, NULL, 'APPENDIX_C', 'C.16', '1', 935001, 930001, 35, NULL, 'admin', now(), 'admin', now(), '0'),
  (950017, 'http://example.org/standard-ontology#address', 'address', 'http://example.org/standard-ontology#address', 'address', NULL, '联系地址', 940021, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.17', '1', 935001, 930001, 36, NULL, 'admin', now(), 'admin', now(), '0'),
  -- C.18～C.21：领域类别（4项，ICS 2项+CCS 2项）
  (950018, 'http://example.org/standard-ontology#ICS_code', 'ICS_code', 'http://example.org/standard-ontology#ICS_code', 'ICS_code', NULL, '国际标准分类代码', 940025, 'TEXT', 'EXTERNAL_DICTIONARY', 'ICS', NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.18', '1', 935001, 930001, 40, NULL, 'admin', now(), 'admin', now(), '0'),
  (950019, 'http://example.org/standard-ontology#ICS_name', 'ICS_name', 'http://example.org/standard-ontology#ICS_name', 'ICS_name', NULL, '国际标准分类名称', 940025, 'TEXT', 'EXTERNAL_DICTIONARY', 'ICS', NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.19', '1', 935001, 930001, 41, NULL, 'admin', now(), 'admin', now(), '0'),
  (950020, 'http://example.org/standard-ontology#CCS_code', 'CCS_code', 'http://example.org/standard-ontology#CCS_code', 'CCS_code', NULL, '中国标准文献分类代码', 940026, 'TEXT', 'EXTERNAL_DICTIONARY', 'CCS', NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.20', '1', 935001, 930001, 42, NULL, 'admin', now(), 'admin', now(), '0'),
  (950021, 'http://example.org/standard-ontology#CCS_name', 'CCS_name', 'http://example.org/standard-ontology#CCS_name', 'CCS_name', NULL, '中国标准文献分类名称', 940026, 'TEXT', 'EXTERNAL_DICTIONARY', 'CCS', NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.21', '1', 935001, 930001, 43, NULL, 'admin', now(), 'admin', now(), '0'),
  -- C.22～C.23：要素（2项）
  (950022, 'http://example.org/standard-ontology#elementStatus', 'elementStatus', 'http://example.org/standard-ontology#elementStatus', 'elementStatus', NULL, '要素的状态', 940027, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.22', '1', 935001, 930001, 50, NULL, 'admin', now(), 'admin', now(), '0'),
  (950023, 'http://example.org/standard-ontology#scopeOfEffect', 'scopeOfEffect', 'http://example.org/standard-ontology#scopeOfEffect', 'scopeOfEffect', NULL, '要素的作用范围', 940027, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.23', '1', 935001, 930001, 51, NULL, 'admin', now(), 'admin', now(), '0'),
  -- C.24～C.27：层次（4项，章2项+条2项）
  (950024, 'http://example.org/standard-ontology#sectionNumber', 'sectionNumber', 'http://example.org/standard-ontology#sectionNumber', 'sectionNumber', NULL, '章的层级编号', 940039, 'TEXT', 'FREE', NULL, NULL, '数字或字母组合', '0', NULL, NULL, 'APPENDIX_C', 'C.24', '1', 935001, 930001, 55, NULL, 'admin', now(), 'admin', now(), '0'),
  (950025, 'http://example.org/standard-ontology#sectionTitle', 'sectionTitle', 'http://example.org/standard-ontology#sectionTitle', 'sectionTitle', NULL, '章的标题', 940039, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.25', '1', 935001, 930001, 56, NULL, 'admin', now(), 'admin', now(), '0'),
  (950026, 'http://example.org/standard-ontology#clauseNumber', 'clauseNumber', 'http://example.org/standard-ontology#clauseNumber', 'clauseNumber', NULL, '条的层级编号', 940040, 'TEXT', 'FREE', NULL, NULL, '数字或字母组合', '0', NULL, NULL, 'APPENDIX_C', 'C.26', '1', 935001, 930001, 57, NULL, 'admin', now(), 'admin', now(), '0'),
  (950027, 'http://example.org/standard-ontology#clauseTitle', 'clauseTitle', 'http://example.org/standard-ontology#clauseTitle', 'clauseTitle', NULL, '有标题条的标题', 940041, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.27', '1', 935001, 930001, 58, '定义域为TitledClause(940041)', 'admin', now(), 'admin', now(), '0'),
  -- C.28～C.31：信息单元（4项）
  (950028, 'http://example.org/standard-ontology#uniqueIdentifier', 'uniqueIdentifier', 'http://example.org/standard-ontology#uniqueIdentifier', 'uniqueIdentifier', NULL, '信息单元的唯一编码', 940048, 'TEXT', 'FREE', NULL, NULL, NULL, '1', NULL, NULL, 'APPENDIX_C', 'C.28', '1', 935001, 930001, 60, '定义域InformationUnit，Clause通过继承获得', 'admin', now(), 'admin', now(), '0'),
  (950029, 'http://example.org/standard-ontology#contentDescription', 'contentDescription', 'http://example.org/standard-ontology#contentDescription', 'contentDescription', NULL, '信息单元的内容描述', 940048, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.29', '1', 935001, 930001, 61, NULL, 'admin', now(), 'admin', now(), '0'),
  (950030, 'http://example.org/standard-ontology#clauseType', 'clauseType', 'http://example.org/standard-ontology#clauseType', 'clauseType', NULL, '条款的类型', 940040, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.30', '1', 935001, 930001, 62, NULL, 'admin', now(), 'admin', now(), '0'),
  (950031, 'http://example.org/standard-ontology#constraintTypeClause', 'constraintTypeClause', 'http://example.org/standard-ontology#constraintType', 'constraintType', NULL, '条款的约束类型', 940040, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.31', '1', 935001, 930001, 63, '内部本地名消歧：Clause定义域', 'admin', now(), 'admin', now(), '0'),
  -- C.32～C.33：对象（2项）
  (950032, 'http://example.org/standard-ontology#objectName', 'objectName', 'http://example.org/standard-ontology#objectName', 'objectName', NULL, '对象的名称', 940063, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.32', '1', 935001, 930001, 70, NULL, 'admin', now(), 'admin', now(), '0'),
  (950033, 'http://example.org/standard-ontology#objectCategory', 'objectCategory', 'http://example.org/standard-ontology#objectCategory', 'objectCategory', NULL, '对象的类别', 940063, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.33', '1', 935001, 930001, 71, NULL, 'admin', now(), 'admin', now(), '0'),
  -- C.34～C.36：特性（3项）
  (950034, 'http://example.org/standard-ontology#propertyName', 'propertyName', 'http://example.org/standard-ontology#propertyName', 'propertyName', NULL, '特性的名称', 940066, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.34', '1', 935001, 930001, 75, NULL, 'admin', now(), 'admin', now(), '0'),
  (950035, 'http://example.org/standard-ontology#propertyValue', 'propertyValue', 'http://example.org/standard-ontology#propertyValue', 'propertyValue', NULL, '特性的具体值', 940066, 'TEXT_OR_NUMERIC', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.35', '1', 935001, 930001, 76, '复合值域：xsd:string或xsd:decimal', 'admin', now(), 'admin', now(), '0'),
  (950036, 'http://example.org/standard-ontology#propertyType', 'propertyType', 'http://example.org/standard-ontology#propertyType', 'propertyType', NULL, '标识特性类型', 940066, 'TEXT', 'OPEN_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.36', '1', 935001, 930001, 77, '开放枚举：描述型、能力型、约束型……', 'admin', now(), 'admin', now(), '0'),
  -- C.37～C.41：约束逻辑（5项，含测量单位）
  (950037, 'http://example.org/standard-ontology#constraintTypeConstraint', 'constraintTypeConstraint', 'http://example.org/standard-ontology#constraintType', 'constraintType', NULL, '约束的形式', 940067, 'TEXT', 'OPEN_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.37', '1', 935001, 930001, 80, '开放枚举：C.37三值+附录D符合性/相对值', 'admin', now(), 'admin', now(), '0'),
  (950038, 'http://example.org/standard-ontology#maxValue', 'maxValue', 'http://example.org/standard-ontology#maxValue', 'maxValue', NULL, '数值约束的最大允许值', 940067, 'NUMERIC', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.38', '1', 935001, 930001, 81, '不预绑定单位分类', 'admin', now(), 'admin', now(), '0'),
  (950039, 'http://example.org/standard-ontology#minValue', 'minValue', 'http://example.org/standard-ontology#minValue', 'minValue', NULL, '数值约束的最小允许值', 940067, 'NUMERIC', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.39', '1', 935001, 930001, 82, '不预绑定单位分类', 'admin', now(), 'admin', now(), '0'),
  (950040, 'http://example.org/standard-ontology#thresholdRange', 'thresholdRange', 'http://example.org/standard-ontology#thresholdRange', 'thresholdRange', NULL, '数值的有效范围', 940067, 'TEXT', 'FREE', NULL, NULL, '最小值-最大值', '0', NULL, NULL, 'APPENDIX_C', 'C.40', '1', 935001, 930001, 83, NULL, 'admin', now(), 'admin', now(), '0'),
  (950041, 'http://example.org/standard-ontology#unit', 'unit', 'http://example.org/standard-ontology#unit', 'unit', 'measurementUnit', '数值的单位', 940067, 'UNIT_REF', 'UNIT_DICTIONARY', NULL, NULL, NULL, '0', NULL, 'DICTIONARY_SYMBOL', 'APPENDIX_C', 'C.41', '1', 935001, 930001, 84, '别名measurementUnit；不限单位分类', 'admin', now(), 'admin', now(), '0'),
  -- C.42～C.44：外部约束（3项）
  (950042, 'http://example.org/standard-ontology#fileType', 'fileType', 'http://example.org/standard-ontology#fileType', 'fileType', NULL, '外部文件的分类', 940072, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.42', '1', 935001, 930001, 90, NULL, 'admin', now(), 'admin', now(), '0'),
  (950043, 'http://example.org/standard-ontology#effectiveTime', 'effectiveTime', 'http://example.org/standard-ontology#effectiveTime', 'effectiveTime', NULL, '文件的生效日期', 940072, 'DATE', 'FREE', NULL, NULL, 'YYYYMMDD', '0', NULL, NULL, 'APPENDIX_C', 'C.43', '1', 935001, 930001, 91, NULL, 'admin', now(), 'admin', now(), '0'),
  (950044, 'http://example.org/standard-ontology#responsibleParty', 'responsibleParty', 'http://example.org/standard-ontology#responsibleParty', 'responsibleParty', NULL, '文件的责任主体', 940072, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.44', '1', 935001, 930001, 92, NULL, 'admin', now(), 'admin', now(), '0'),
  -- C.45～C.47：制定程序（3项）
  (950045, 'http://example.org/standard-ontology#stageCode', 'stageCode', 'http://example.org/standard-ontology#stageCode', 'stageCode', NULL, '制定程序的阶段代码', 940077, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_C', 'C.45', '1', 935001, 930001, 95, NULL, 'admin', now(), 'admin', now(), '0'),
  (950046, 'http://example.org/standard-ontology#startDate', 'startDate', 'http://example.org/standard-ontology#startDate', 'startDate', NULL, '制定程序的开始日期', 940077, 'DATE', 'FREE', NULL, NULL, 'YYYYMMDD', '0', NULL, NULL, 'APPENDIX_C', 'C.46', '1', 935001, 930001, 96, '后续公理：开始日期≤结束日期', 'admin', now(), 'admin', now(), '0'),
  (950047, 'http://example.org/standard-ontology#endDate', 'endDate', 'http://example.org/standard-ontology#endDate', 'endDate', NULL, '制定程序的结束日期', 940077, 'DATE', 'FREE', NULL, NULL, 'YYYYMMDD', '0', NULL, NULL, 'APPENDIX_C', 'C.47', '1', 935001, 930001, 97, NULL, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 条款直采属性（2项，id 951001-951002，source_type=CLAUSE_REQUIRED）
-- 依据：第6.2.5.1条"术语名称、术语定义"
-- ----------------------------
INSERT INTO ont_data_property (id, iri, iri_local_name, standard_iri, name, preferred_alias, definition, domain_entity_type_id, base_type, value_mode, value_source_ref, regex_pattern, format_hint, is_unique, unit_category_id, unit_ref_mode, source_type, source_reference, is_builtin, ontology_id, namespace_id, sort_order, remarks, create_by, create_time, update_by, update_time, del_flag) VALUES
  (951001, 'http://example.org/standard-ontology#termName', 'termName', NULL, 'termName', NULL, '术语的名称', 940047, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'CLAUSE_REQUIRED', '6.2.5.1', '1', 935001, 930001, 100, NULL, 'admin', now(), 'admin', now(), '0'),
  (951002, 'http://example.org/standard-ontology#termDefinition', 'termDefinition', NULL, 'termDefinition', NULL, '术语的定义', 940047, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'CLAUSE_REQUIRED', '6.2.5.1', '1', 935001, 930001, 101, NULL, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 附录D兼容属性（7项，id 952001-952007，source_type=APPENDIX_D_COMPAT）
-- 依据：附录D实例化示例中实际使用但未列入附录C的属性
-- ----------------------------
INSERT INTO ont_data_property (id, iri, iri_local_name, standard_iri, name, preferred_alias, definition, domain_entity_type_id, base_type, value_mode, value_source_ref, regex_pattern, format_hint, is_unique, unit_category_id, unit_ref_mode, source_type, source_reference, is_builtin, ontology_id, namespace_id, sort_order, remarks, create_by, create_time, update_by, update_time, del_flag) VALUES
  (952001, 'http://example.org/standard-ontology#propertyDescription', 'propertyDescription', NULL, 'propertyDescription', NULL, '特性的描述', 940066, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_D_COMPAT', 'D-Step4', '1', 935001, 930001, 110, NULL, 'admin', now(), 'admin', now(), '0'),
  (952002, 'http://example.org/standard-ontology#allowedValue', 'allowedValue', NULL, 'allowedValue', NULL, '约束的允许值', 940067, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_D_COMPAT', 'D-Step5', '1', 935001, 930001, 111, '枚举值约束使用', 'admin', now(), 'admin', now(), '0'),
  (952003, 'http://example.org/standard-ontology#conformsTo', 'conformsTo', NULL, 'conformsTo', NULL, '符合性依据', 940067, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_D_COMPAT', 'D-Step5', '1', 935001, 930001, 112, '符合性约束使用', 'admin', now(), 'admin', now(), '0'),
  (952004, 'http://example.org/standard-ontology#comparedTo', 'comparedTo', NULL, 'comparedTo', NULL, '比较对象', 940067, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_D_COMPAT', 'D-Step5', '1', 935001, 930001, 113, '数值/相对值约束使用', 'admin', now(), 'admin', now(), '0'),
  (952005, 'http://example.org/standard-ontology#constraintTarget', 'constraintTarget', NULL, 'constraintTarget', NULL, '约束目标', 940067, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_D_COMPAT', 'D-Step5', '1', 935001, 930001, 114, '相对值约束使用', 'admin', now(), 'admin', now(), '0'),
  (952006, 'http://example.org/standard-ontology#actionName', 'actionName', NULL, 'actionName', NULL, '行动的名称', 940068, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_D_COMPAT', 'D-Step6', '1', 935001, 930001, 120, NULL, 'admin', now(), 'admin', now(), '0'),
  (952007, 'http://example.org/standard-ontology#actionDescription', 'actionDescription', NULL, 'actionDescription', NULL, '行动的描述', 940068, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'APPENDIX_D_COMPAT', 'D-Step6', '1', 935001, 930001, 121, NULL, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 中文标签（zh locale，56条）
-- ----------------------------
INSERT INTO ont_data_property_label (data_property_id, locale, label) VALUES
  -- 附录C 47条
  (950001, 'zh', '编制目的'), (950002, 'zh', '语言版本'), (950003, 'zh', '标准状态'), (950004, 'zh', '约束类型'),
  (950005, 'zh', '文件名称'), (950006, 'zh', '文件编号'), (950007, 'zh', '发布日期'), (950008, 'zh', '实施日期'),
  (950009, 'zh', '主题名称'), (950010, 'zh', '所属行业'),
  (950011, 'zh', '名称'), (950012, 'zh', '统一信用代码'), (950013, 'zh', '所在地'), (950014, 'zh', '姓名'),
  (950015, 'zh', '所属单位'), (950016, 'zh', '联系电话'), (950017, 'zh', '联系地址'),
  (950018, 'zh', 'ICS分类代码'), (950019, 'zh', 'ICS分类名称'), (950020, 'zh', 'CCS分类代码'), (950021, 'zh', 'CCS分类名称'),
  (950022, 'zh', '要素状态'), (950023, 'zh', '作用范围'),
  (950024, 'zh', '章编号'), (950025, 'zh', '章标题'), (950026, 'zh', '条编号'), (950027, 'zh', '条标题'),
  (950028, 'zh', '唯一标识符'), (950029, 'zh', '内容描述'), (950030, 'zh', '条款类型'), (950031, 'zh', '约束类型'),
  (950032, 'zh', '对象名称'), (950033, 'zh', '对象类别'),
  (950034, 'zh', '特性名称'), (950035, 'zh', '特性值'), (950036, 'zh', '特性类型'),
  (950037, 'zh', '约束类型'), (950038, 'zh', '最大值'), (950039, 'zh', '最小值'), (950040, 'zh', '阈值范围'),
  (950041, 'zh', '测量单位'),
  (950042, 'zh', '文件类型'), (950043, 'zh', '生效时间'), (950044, 'zh', '责任主体'),
  (950045, 'zh', '阶段代码'), (950046, 'zh', '开始日期'), (950047, 'zh', '结束日期'),
  -- 条款直采2条
  (951001, 'zh', '术语名称'), (951002, 'zh', '术语定义'),
  -- 附录D兼容7条
  (952001, 'zh', '特性描述'), (952002, 'zh', '允许值'), (952003, 'zh', '符合性依据'),
  (952004, 'zh', '比较对象'), (952005, 'zh', '约束目标'),
  (952006, 'zh', '行动名称'), (952007, 'zh', '行动描述')
ON CONFLICT (data_property_id, locale) DO NOTHING;

-- ----------------------------
-- 枚举值（9个枚举属性，32条词法值）
-- ----------------------------
-- C.2 语言版本（4条：2标准+2兼容别名）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (950002, '中文版本', NULL, '1', 'C.2', 1),
  (950002, '英文版本', NULL, '1', 'C.2', 2),
  (950002, '中文', '中文版本', '0', 'D-Step1', 3),
  (950002, '英文', '英文版本', '0', 'D-Step1', 4)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- C.3 标准状态（4条标准）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (950003, '草案', NULL, '1', 'C.3', 1),
  (950003, '现行', NULL, '1', 'C.3', 2),
  (950003, '废止', NULL, '1', 'C.3', 3),
  (950003, '修订中', NULL, '1', 'C.3', 4)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- C.4 约束类型(Standard)（2条标准）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (950004, '强制性', NULL, '1', 'C.4', 1),
  (950004, '推荐性', NULL, '1', 'C.4', 2)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- C.22 要素状态（2条标准）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (950022, '必备', NULL, '1', 'C.22', 1),
  (950022, '可选', NULL, '1', 'C.22', 2)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- C.30 条款类型（5条标准）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (950030, '要求型', NULL, '1', 'C.30', 1),
  (950030, '推荐型', NULL, '1', 'C.30', 2),
  (950030, '指示型', NULL, '1', 'C.30', 3),
  (950030, '允许型', NULL, '1', 'C.30', 4),
  (950030, '陈述型', NULL, '1', 'C.30', 5)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- C.31 约束类型(Clause)（3条标准）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (950031, '强制', NULL, '1', 'C.31', 1),
  (950031, '推荐', NULL, '1', 'C.31', 2),
  (950031, '描述', NULL, '1', 'C.31', 3)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- C.36 特性类型（3条标准，开放枚举）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (950036, '描述型', NULL, '1', 'C.36', 1),
  (950036, '能力型', NULL, '1', 'C.36', 2),
  (950036, '约束型', NULL, '1', 'C.36', 3)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- C.37 约束类型(Constraint)（5条：3标准+2附录D兼容，开放枚举）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (950037, '数值区间', NULL, '1', 'C.37', 1),
  (950037, '枚举值', NULL, '1', 'C.37', 2),
  (950037, '逻辑表达式', NULL, '1', 'C.37', 3),
  (950037, '符合性', NULL, '0', 'D-Step5', 4),
  (950037, '相对值', NULL, '0', 'D-Step5', 5)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- C.42 文件类型（4条标准）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (950042, '法规', NULL, '1', 'C.42', 1),
  (950042, '专利', NULL, '1', 'C.42', 2),
  (950042, '文献', NULL, '1', 'C.42', 3),
  (950042, '公共数据库', NULL, '1', 'C.42', 4)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- ----------------------------
-- 核心种子完整性断言
-- ----------------------------
DO $$
BEGIN
  -- 1. 附录C 47项精确数量校验
  IF (SELECT count(*) FROM ont_data_property WHERE source_type = 'APPENDIX_C' AND is_builtin = '1' AND del_flag = '0') <> 47 THEN
    RAISE EXCEPTION '附录C数据属性数量不是47条';
  END IF;
  -- 2. 受保护预置总数56项
  IF (SELECT count(*) FROM ont_data_property WHERE is_builtin = '1' AND del_flag = '0') <> 56 THEN
    RAISE EXCEPTION '受保护预置数据属性总数不是56条';
  END IF;
  -- 3. 中文标签56条
  IF (SELECT count(*) FROM ont_data_property_label WHERE data_property_id IN (SELECT id FROM ont_data_property WHERE is_builtin = '1' AND del_flag = '0') AND locale = 'zh') <> 56 THEN
    RAISE EXCEPTION '数据属性中文标签数量不是56条';
  END IF;
  -- 4. 三个constraintType的IRI互不相同，standard_iri均为#constraintType
  IF NOT EXISTS (SELECT 1 FROM ont_data_property WHERE id = 950004 AND iri_local_name = 'constraintTypeStandard' AND standard_iri = 'http://example.org/standard-ontology#constraintType') THEN
    RAISE EXCEPTION 'constraintType(Standard)的IRI消歧不正确';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_data_property WHERE id = 950031 AND iri_local_name = 'constraintTypeClause' AND standard_iri = 'http://example.org/standard-ontology#constraintType') THEN
    RAISE EXCEPTION 'constraintType(Clause)的IRI消歧不正确';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_data_property WHERE id = 950037 AND iri_local_name = 'constraintTypeConstraint' AND standard_iri = 'http://example.org/standard-ontology#constraintType') THEN
    RAISE EXCEPTION 'constraintType(Constraint)的IRI消歧不正确';
  END IF;
  -- 5. C.41记录为name=unit、别名measurementUnit、UNIT_REF/UNIT_DICTIONARY、unit_category_id IS NULL
  IF NOT EXISTS (
    SELECT 1 FROM ont_data_property WHERE id = 950041
      AND name = 'unit' AND preferred_alias = 'measurementUnit'
      AND base_type = 'UNIT_REF' AND value_mode = 'UNIT_DICTIONARY' AND unit_ref_mode = 'DICTIONARY_SYMBOL'
      AND unit_category_id IS NULL
  ) THEN
    RAISE EXCEPTION 'C.41测量单位属性配置不正确';
  END IF;
  -- 6. C.27定义域为TitledClause(940041)，C.28定义域为InformationUnit(940048)
  IF NOT EXISTS (SELECT 1 FROM ont_data_property WHERE id = 950027 AND domain_entity_type_id = 940041) THEN
    RAISE EXCEPTION 'C.27条标题定义域必须为TitledClause(940041)';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_data_property WHERE id = 950028 AND domain_entity_type_id = 940048) THEN
    RAISE EXCEPTION 'C.28唯一标识符定义域必须为InformationUnit(940048)';
  END IF;
  -- 7. 不存在Standard/Clause重复uniqueIdentifier
  IF EXISTS (
    SELECT 1 FROM ont_data_property WHERE name = 'uniqueIdentifier' AND domain_entity_type_id NOT IN (940048)
      AND del_flag = '0'
  ) THEN
    RAISE EXCEPTION '不应存在Standard/Clause定义域的重复uniqueIdentifier';
  END IF;
  -- 8. C.35为TEXT_OR_NUMERIC
  IF NOT EXISTS (SELECT 1 FROM ont_data_property WHERE id = 950035 AND base_type = 'TEXT_OR_NUMERIC') THEN
    RAISE EXCEPTION 'C.35特性值必须为TEXT_OR_NUMERIC类型';
  END IF;
  -- C.43 effectiveTime存在
  IF NOT EXISTS (SELECT 1 FROM ont_data_property WHERE id = 950043 AND name = 'effectiveTime') THEN
    RAISE EXCEPTION 'C.43生效时间属性不存在';
  END IF;
  -- 9. 枚举值数量校验（9个枚举属性共32条）
  IF (SELECT count(*) FROM ont_data_property_enum) <> 32 THEN
    RAISE EXCEPTION '数据属性枚举值总数不是32条';
  END IF;
  -- 验证关键枚举值存在
  IF NOT EXISTS (SELECT 1 FROM ont_data_property_enum WHERE data_property_id = 950036 AND enum_value = '约束型') THEN
    RAISE EXCEPTION 'C.36特性类型缺少约束型枚举值';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_data_property_enum WHERE data_property_id = 950037 AND enum_value = '符合性') THEN
    RAISE EXCEPTION 'C.37约束类型缺少符合性枚举值';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_data_property_enum WHERE data_property_id = 950037 AND enum_value = '相对值') THEN
    RAISE EXCEPTION 'C.37约束类型缺少相对值枚举值';
  END IF;
  -- 10. 所有内部IRI唯一
  IF EXISTS (SELECT iri FROM ont_data_property WHERE del_flag = '0' GROUP BY iri HAVING count(*) > 1) THEN
    RAISE EXCEPTION '数据属性内部IRI存在重复';
  END IF;
  -- 11. 所有IRI均等于命名空间URI加iri_local_name
  IF EXISTS (
    SELECT 1 FROM ont_data_property dp
    JOIN ont_namespace ns ON dp.namespace_id = ns.id
    WHERE dp.del_flag = '0' AND dp.iri <> ns.uri || dp.iri_local_name
  ) THEN
    RAISE EXCEPTION '数据属性IRI不等于命名空间URI加iri_local_name';
  END IF;
  -- 12. 不存在别名与其他内部本地名冲突
  IF EXISTS (
    SELECT 1 FROM ont_data_property a
    JOIN ont_data_property b ON a.ontology_id = b.ontology_id AND a.namespace_id = b.namespace_id
      AND a.id <> b.id AND a.del_flag = '0' AND b.del_flag = '0'
    WHERE a.preferred_alias IS NOT NULL AND a.preferred_alias = b.iri_local_name
  ) THEN
    RAISE EXCEPTION '数据属性别名与其他内部本地名冲突';
  END IF;
END $$;
