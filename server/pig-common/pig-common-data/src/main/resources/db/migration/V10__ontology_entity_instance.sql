-- ----------------------------
-- youming ontology entity instance management
-- 依据：GB/T 48000.3—2026 附录D实例化示例（GB/T 31486—2024 动力蓄电池标准9步骤）
-- 前置：V4~V9（单位字典、命名空间、实体类型、数据属性、对象属性、公理规则）
-- ----------------------------

-- ----------------------------
-- 1. 增补扩展命名空间930006（附录D实例资源命名空间）
-- ----------------------------
INSERT INTO ont_namespace (id, prefix, uri, is_default, is_builtin, sort_order, description, create_by, create_time, update_by, update_time, del_flag)
VALUES (930006, 'gbt31486', 'http://example.org/standard/GB-T-31486-2024/', '0', '1', 60, '附录D实例资源命名空间（GB/T 31486—2024）', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 2. 实例主表 ont_entity_instance
-- ----------------------------
CREATE TABLE ont_entity_instance (
  id bigint NOT NULL,
  iri varchar(512) NOT NULL,
  iri_local_name varchar(128) NOT NULL,
  rdf_type_id bigint NOT NULL,
  label varchar(255) DEFAULT NULL,
  namespace_id bigint NOT NULL,
  ontology_id bigint NOT NULL,
  source_type varchar(32) NOT NULL,
  source_reference varchar(128) DEFAULT NULL,
  declaration_mode varchar(16) NOT NULL,
  is_builtin char(1) NOT NULL DEFAULT '0',
  sort_order integer NOT NULL DEFAULT 0,
  remarks varchar(255) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_entity_instance_type FOREIGN KEY (rdf_type_id) REFERENCES ont_entity_type (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_entity_instance_namespace FOREIGN KEY (namespace_id) REFERENCES ont_namespace (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_entity_instance_ontology FOREIGN KEY (ontology_id) REFERENCES ont_ontology_project (id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_entity_instance_source_type CHECK (source_type IN ('APPENDIX_D', 'EXTENSION')),
  CONSTRAINT ck_ont_entity_instance_declaration_mode CHECK (declaration_mode IN ('EXPLICIT', 'REFERENCE_ONLY')),
  CONSTRAINT ck_ont_entity_instance_builtin CHECK (is_builtin IN ('0', '1')),
  CONSTRAINT ck_ont_entity_instance_del_flag CHECK (del_flag IN ('0', '1')),
  CONSTRAINT ck_ont_entity_instance_sort_order CHECK (sort_order >= 0),
  CONSTRAINT ck_ont_entity_instance_source_builtin CHECK (
    (source_type = 'APPENDIX_D' AND is_builtin = '1') OR
    (source_type = 'EXTENSION' AND is_builtin = '0')
  )
);

COMMENT ON TABLE ont_entity_instance IS '本体建模-实体对象实例表';
COMMENT ON COLUMN ont_entity_instance.id IS '实例ID';
COMMENT ON COLUMN ont_entity_instance.iri IS '完整IRI，由namespace.uri+iri_local_name拼接';
COMMENT ON COLUMN ont_entity_instance.iri_local_name IS 'IRI本地标识符';
COMMENT ON COLUMN ont_entity_instance.rdf_type_id IS 'rdf:type实体类型ID，引用ont_entity_type.id';
COMMENT ON COLUMN ont_entity_instance.label IS 'UI显示标签，不替代本体数据属性';
COMMENT ON COLUMN ont_entity_instance.namespace_id IS '命名空间ID，引用ont_namespace.id';
COMMENT ON COLUMN ont_entity_instance.ontology_id IS '本体工程ID，引用ont_ontology_project.id';
COMMENT ON COLUMN ont_entity_instance.source_type IS '来源类型：APPENDIX_D/EXTENSION';
COMMENT ON COLUMN ont_entity_instance.source_reference IS '来源引用，如D-Step5';
COMMENT ON COLUMN ont_entity_instance.declaration_mode IS '声明模式：EXPLICIT/REFERENCE_ONLY';
COMMENT ON COLUMN ont_entity_instance.is_builtin IS '是否内置，1是0否';
COMMENT ON COLUMN ont_entity_instance.sort_order IS '排序值';
COMMENT ON COLUMN ont_entity_instance.remarks IS '治理说明/源异常说明';
COMMENT ON COLUMN ont_entity_instance.create_by IS '创建人';
COMMENT ON COLUMN ont_entity_instance.create_time IS '创建时间';
COMMENT ON COLUMN ont_entity_instance.update_by IS '修改人';
COMMENT ON COLUMN ont_entity_instance.update_time IS '更新时间';
COMMENT ON COLUMN ont_entity_instance.del_flag IS '删除标志，0未删除，1已删除';

CREATE UNIQUE INDEX uk_ont_entity_instance_iri ON ont_entity_instance (iri) WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_entity_instance_local ON ont_entity_instance (ontology_id, namespace_id, iri_local_name) WHERE del_flag = '0';
CREATE INDEX idx_ont_entity_instance_type ON ont_entity_instance (ontology_id, rdf_type_id) WHERE del_flag = '0';

-- ----------------------------
-- 3. 数据属性值表 ont_instance_data_value
-- ----------------------------
CREATE TABLE ont_instance_data_value (
  id bigint NOT NULL,
  instance_id bigint NOT NULL,
  data_property_id bigint NOT NULL,
  literal_value text NOT NULL,
  literal_type varchar(16) NOT NULL,
  unit_id bigint DEFAULT NULL,
  literal_symbol varchar(32) DEFAULT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_instance_value_instance FOREIGN KEY (instance_id) REFERENCES ont_entity_instance (id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_instance_value_property FOREIGN KEY (data_property_id) REFERENCES ont_data_property (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_instance_value_unit FOREIGN KEY (unit_id) REFERENCES ont_unit (id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_instance_value_literal_type CHECK (literal_type IN ('STRING', 'URI', 'DATE', 'INTEGER', 'DECIMAL', 'BOOLEAN')),
  CONSTRAINT ck_ont_instance_value_del_flag CHECK (del_flag IN ('0', '1')),
  CONSTRAINT ck_ont_instance_value_sort_order CHECK (sort_order >= 0),
  CONSTRAINT ck_ont_instance_value_unit_symbol CHECK (
    (unit_id IS NULL AND literal_symbol IS NULL) OR
    (unit_id IS NOT NULL AND literal_symbol IS NOT NULL)
  )
);

COMMENT ON TABLE ont_instance_data_value IS '本体建模-实例数据属性值表';
COMMENT ON COLUMN ont_instance_data_value.id IS '数据值ID';
COMMENT ON COLUMN ont_instance_data_value.instance_id IS '实例ID，引用ont_entity_instance.id';
COMMENT ON COLUMN ont_instance_data_value.data_property_id IS '数据属性ID，引用ont_data_property.id';
COMMENT ON COLUMN ont_instance_data_value.literal_value IS '规范化词法值';
COMMENT ON COLUMN ont_instance_data_value.literal_type IS '字面量类型：STRING/URI/DATE/INTEGER/DECIMAL/BOOLEAN';
COMMENT ON COLUMN ont_instance_data_value.unit_id IS '单位ID，UNIT_REF必填，引用ont_unit.id';
COMMENT ON COLUMN ont_instance_data_value.literal_symbol IS 'UNIT_REF词法快照';
COMMENT ON COLUMN ont_instance_data_value.sort_order IS '同属性多值排序';
COMMENT ON COLUMN ont_instance_data_value.create_by IS '创建人';
COMMENT ON COLUMN ont_instance_data_value.create_time IS '创建时间';
COMMENT ON COLUMN ont_instance_data_value.update_by IS '修改人';
COMMENT ON COLUMN ont_instance_data_value.update_time IS '更新时间';
COMMENT ON COLUMN ont_instance_data_value.del_flag IS '删除标志，0未删除，1已删除';

CREATE INDEX idx_ont_instance_value_instance ON ont_instance_data_value (instance_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_instance_value_property ON ont_instance_data_value (data_property_id) WHERE del_flag = '0';

-- ----------------------------
-- 4. 对象属性断言表 ont_instance_object_relation
-- ----------------------------
CREATE TABLE ont_instance_object_relation (
  id bigint NOT NULL,
  subject_instance_id bigint NOT NULL,
  object_property_id bigint NOT NULL,
  object_kind varchar(16) NOT NULL,
  object_instance_id bigint DEFAULT NULL,
  object_entity_type_id bigint DEFAULT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_relation_subject FOREIGN KEY (subject_instance_id) REFERENCES ont_entity_instance (id) ON DELETE CASCADE,
  CONSTRAINT fk_ont_relation_property FOREIGN KEY (object_property_id) REFERENCES ont_object_property (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_relation_object_instance FOREIGN KEY (object_instance_id) REFERENCES ont_entity_instance (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ont_relation_object_type FOREIGN KEY (object_entity_type_id) REFERENCES ont_entity_type (id) ON DELETE RESTRICT,
  CONSTRAINT ck_ont_relation_object_kind CHECK (object_kind IN ('INSTANCE', 'ENTITY_TYPE')),
  CONSTRAINT ck_ont_relation_del_flag CHECK (del_flag IN ('0', '1')),
  CONSTRAINT ck_ont_relation_sort_order CHECK (sort_order >= 0),
  CONSTRAINT ck_ont_relation_exactly_one_target CHECK (
    (object_kind = 'INSTANCE' AND object_instance_id IS NOT NULL AND object_entity_type_id IS NULL) OR
    (object_kind = 'ENTITY_TYPE' AND object_instance_id IS NULL AND object_entity_type_id IS NOT NULL)
  )
);

COMMENT ON TABLE ont_instance_object_relation IS '本体建模-实例对象属性断言表';
COMMENT ON COLUMN ont_instance_object_relation.id IS '断言ID';
COMMENT ON COLUMN ont_instance_object_relation.subject_instance_id IS '主体实例ID';
COMMENT ON COLUMN ont_instance_object_relation.object_property_id IS '对象属性ID（谓词）';
COMMENT ON COLUMN ont_instance_object_relation.object_kind IS '客体类型：INSTANCE/ENTITY_TYPE';
COMMENT ON COLUMN ont_instance_object_relation.object_instance_id IS '客体实例ID，INSTANCE时必填';
COMMENT ON COLUMN ont_instance_object_relation.object_entity_type_id IS '客体实体类型ID，ENTITY_TYPE时必填';
COMMENT ON COLUMN ont_instance_object_relation.sort_order IS '多值顺序';
COMMENT ON COLUMN ont_instance_object_relation.create_by IS '创建人';
COMMENT ON COLUMN ont_instance_object_relation.create_time IS '创建时间';
COMMENT ON COLUMN ont_instance_object_relation.update_by IS '修改人';
COMMENT ON COLUMN ont_instance_object_relation.update_time IS '更新时间';
COMMENT ON COLUMN ont_instance_object_relation.del_flag IS '删除标志，0未删除，1已删除';

CREATE UNIQUE INDEX uk_ont_relation_instance_target ON ont_instance_object_relation
  (subject_instance_id, object_property_id, object_instance_id)
  WHERE del_flag = '0' AND object_kind = 'INSTANCE';
CREATE UNIQUE INDEX uk_ont_relation_type_target ON ont_instance_object_relation
  (subject_instance_id, object_property_id, object_entity_type_id)
  WHERE del_flag = '0' AND object_kind = 'ENTITY_TYPE';
CREATE INDEX idx_ont_relation_subject ON ont_instance_object_relation (subject_instance_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_relation_object_instance ON ont_instance_object_relation (object_instance_id)
  WHERE del_flag = '0' AND object_kind = 'INSTANCE';
CREATE INDEX idx_ont_relation_property ON ont_instance_object_relation (object_property_id) WHERE del_flag = '0';

-- ----------------------------
-- 5. 为fileType(950042)增补"产品技术条件"枚举值（附录D步骤8要求）
-- ----------------------------
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order)
VALUES (950042, '产品技术条件', NULL, '1', 'D-Step8', 5)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- ----------------------------
-- 6. 菜单：本体建模 / 实体对象实例管理
-- ----------------------------
INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES (900700, '实体对象实例管理', NULL, '/ontology/instance/index', NULL, 900000, 'iconfont icon-shujujiegou', '1', 7, '0', NULL, '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES
  (900701, '实例查看', 'ontology_instance_view', NULL, NULL, 900700, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900702, '实例新增', 'ontology_instance_add', NULL, NULL, 900700, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900703, '实例修改', 'ontology_instance_edit', NULL, NULL, 900700, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900704, '实例删除', 'ontology_instance_del', NULL, NULL, 900700, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
  (1, 900700), (1, 900701), (1, 900702), (1, 900703), (1, 900704)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ----------------------------
-- 7. 附录D种子数据：29个实例主记录（980001~980029）
-- 命名空间：930006(gbt31486, http://example.org/standard/GB-T-31486-2024/)
-- 本体工程：935001(core)
-- 所有APPENDIX_D/is_builtin='1'
-- 980019为REFERENCE_ONLY（步骤4引用、步骤5未定义，无数据值）
-- ----------------------------
INSERT INTO ont_entity_instance (id, iri, iri_local_name, rdf_type_id, label, namespace_id, ontology_id, source_type, source_reference, declaration_mode, is_builtin, sort_order, remarks, create_by, create_time, update_by, update_time, del_flag) VALUES
  -- 步骤1：标准实体
  (980001, 'http://example.org/standard/GB-T-31486-2024/GB-T_31486-2024', 'GB-T_31486-2024', 940001, 'GB/T 31486—2024', 930006, 935001, 'APPENDIX_D', 'D-Step1', 'EXPLICIT', '1', 10, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 步骤2：第5章及4个有标题条
  (980002, 'http://example.org/standard/GB-T-31486-2024/Clause_5', 'Clause_5', 940039, '第5章 性能要求', 930006, 935001, 'APPENDIX_D', 'D-Step2', 'EXPLICIT', '1', 20, NULL, 'admin', now(), 'admin', now(), '0'),
  (980003, 'http://example.org/standard/GB-T-31486-2024/Clause_5_1', 'Clause_5_1', 940041, '5.1 外观', 930006, 935001, 'APPENDIX_D', 'D-Step2', 'EXPLICIT', '1', 21, 'TitledClause继承Clause', 'admin', now(), 'admin', now(), '0'),
  (980004, 'http://example.org/standard/GB-T-31486-2024/Clause_5_2', 'Clause_5_2', 940041, '5.2 极性标识', 930006, 935001, 'APPENDIX_D', 'D-Step2', 'EXPLICIT', '1', 22, 'TitledClause继承Clause', 'admin', now(), 'admin', now(), '0'),
  (980005, 'http://example.org/standard/GB-T-31486-2024/Clause_5_3', 'Clause_5_3', 940041, '5.3 质量和外形尺寸', 930006, 935001, 'APPENDIX_D', 'D-Step2', 'EXPLICIT', '1', 23, 'TitledClause继承Clause', 'admin', now(), 'admin', now(), '0'),
  (980006, 'http://example.org/standard/GB-T-31486-2024/Clause_5_4', 'Clause_5_4', 940041, '5.4 室温放电容量', 930006, 935001, 'APPENDIX_D', 'D-Step2', 'EXPLICIT', '1', 24, 'TitledClause继承Clause', 'admin', now(), 'admin', now(), '0'),
  -- 步骤3：电池单体对象
  (980007, 'http://example.org/standard/GB-T-31486-2024/BatteryCell', 'BatteryCell', 940063, '电池单体', 930006, 935001, 'APPENDIX_D', 'D-Step3', 'EXPLICIT', '1', 30, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 步骤4：5个特性
  (980008, 'http://example.org/standard/GB-T-31486-2024/Appearance_Prop', 'Appearance_Prop', 940066, '外观', 930006, 935001, 'APPENDIX_D', 'D-Step4', 'EXPLICIT', '1', 40, NULL, 'admin', now(), 'admin', now(), '0'),
  (980009, 'http://example.org/standard/GB-T-31486-2024/PolarityMark_Prop', 'PolarityMark_Prop', 940066, '极性标识', 930006, 935001, 'APPENDIX_D', 'D-Step4', 'EXPLICIT', '1', 41, NULL, 'admin', now(), 'admin', now(), '0'),
  (980010, 'http://example.org/standard/GB-T-31486-2024/Mass_Prop', 'Mass_Prop', 940066, '质量', 930006, 935001, 'APPENDIX_D', 'D-Step4', 'EXPLICIT', '1', 42, NULL, 'admin', now(), 'admin', now(), '0'),
  (980011, 'http://example.org/standard/GB-T-31486-2024/Dimension_Prop', 'Dimension_Prop', 940066, '外形尺寸', 930006, 935001, 'APPENDIX_D', 'D-Step4', 'EXPLICIT', '1', 43, NULL, 'admin', now(), 'admin', now(), '0'),
  (980012, 'http://example.org/standard/GB-T-31486-2024/Discharge_Capacity_Prop', 'Discharge_Capacity_Prop', 940066, '室温放电容量', 930006, 935001, 'APPENDIX_D', 'D-Step4', 'EXPLICIT', '1', 44, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 步骤5：6个显式定义约束
  (980013, 'http://example.org/standard/GB-T-31486-2024/Appearance_Constraint', 'Appearance_Constraint', 940067, '外观约束', 930006, 935001, 'APPENDIX_D', 'D-Step5', 'EXPLICIT', '1', 50, NULL, 'admin', now(), 'admin', now(), '0'),
  (980014, 'http://example.org/standard/GB-T-31486-2024/PolarityMark_Constraint', 'PolarityMark_Constraint', 940067, '极性标识约束', 930006, 935001, 'APPENDIX_D', 'D-Step5', 'EXPLICIT', '1', 51, NULL, 'admin', now(), 'admin', now(), '0'),
  (980015, 'http://example.org/standard/GB-T-31486-2024/Mass_Constraint', 'Mass_Constraint', 940067, '质量约束', 930006, 935001, 'APPENDIX_D', 'D-Step5', 'EXPLICIT', '1', 52, NULL, 'admin', now(), 'admin', now(), '0'),
  (980016, 'http://example.org/standard/GB-T-31486-2024/Dimension_Constraint', 'Dimension_Constraint', 940067, '外形尺寸约束', 930006, 935001, 'APPENDIX_D', 'D-Step5', 'EXPLICIT', '1', 53, NULL, 'admin', now(), 'admin', now(), '0'),
  (980017, 'http://example.org/standard/GB-T-31486-2024/Capacity_Constraint_1', 'Capacity_Constraint_1', 940067, '容量约束1（不低于额定容量）', 930006, 935001, 'APPENDIX_D', 'D-Step5', 'EXPLICIT', '1', 54, NULL, 'admin', now(), 'admin', now(), '0'),
  (980018, 'http://example.org/standard/GB-T-31486-2024/Capacity_Constraint_2', 'Capacity_Constraint_2', 940067, '容量约束2（极差≤5%）', 930006, 935001, 'APPENDIX_D', 'D-Step5', 'EXPLICIT', '1', 55, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 步骤4/5异常：第三个容量约束——步骤4引用、步骤5未定义，REFERENCE_ONLY
  (980019, 'http://example.org/standard/GB-T-31486-2024/Capacity_Constraint_3', 'Capacity_Constraint_3', 940067, '容量约束3（仅被引用）', 930006, 935001, 'APPENDIX_D', 'D-Step4 dangling reference', 'REFERENCE_ONLY', '1', 56, '仅被引用，原文未定义', 'admin', now(), 'admin', now(), '0'),
  -- 步骤6：4个行动
  (980020, 'http://example.org/standard/GB-T-31486-2024/Test_6_2_1', 'Test_6_2_1', 940068, '外观检验', 930006, 935001, 'APPENDIX_D', 'D-Step6', 'EXPLICIT', '1', 60, NULL, 'admin', now(), 'admin', now(), '0'),
  (980021, 'http://example.org/standard/GB-T-31486-2024/Test_6_2_2', 'Test_6_2_2', 940068, '极性标识检验', 930006, 935001, 'APPENDIX_D', 'D-Step6', 'EXPLICIT', '1', 61, NULL, 'admin', now(), 'admin', now(), '0'),
  (980022, 'http://example.org/standard/GB-T-31486-2024/Test_6_2_3', 'Test_6_2_3', 940068, '质量和外形尺寸检验', 930006, 935001, 'APPENDIX_D', 'D-Step6', 'EXPLICIT', '1', 62, NULL, 'admin', now(), 'admin', now(), '0'),
  (980023, 'http://example.org/standard/GB-T-31486-2024/Test_6_2_5', 'Test_6_2_5', 940068, '室温放电容量试验', 930006, 935001, 'APPENDIX_D', 'D-Step6', 'EXPLICIT', '1', 63, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 步骤7：4个要求型条款
  (980024, 'http://example.org/standard/GB-T-31486-2024/Appearance_Requirement', 'Appearance_Requirement', 940040, '外观要求', 930006, 935001, 'APPENDIX_D', 'D-Step7', 'EXPLICIT', '1', 70, NULL, 'admin', now(), 'admin', now(), '0'),
  (980025, 'http://example.org/standard/GB-T-31486-2024/PolarityMark_Requirement', 'PolarityMark_Requirement', 940040, '极性标识要求', 930006, 935001, 'APPENDIX_D', 'D-Step7', 'EXPLICIT', '1', 71, NULL, 'admin', now(), 'admin', now(), '0'),
  (980026, 'http://example.org/standard/GB-T-31486-2024/MassDimension_Requirement', 'MassDimension_Requirement', 940040, '质量外形尺寸要求', 930006, 935001, 'APPENDIX_D', 'D-Step7', 'EXPLICIT', '1', 72, NULL, 'admin', now(), 'admin', now(), '0'),
  (980027, 'http://example.org/standard/GB-T-31486-2024/Discharge_Capacity_Requirement', 'Discharge_Capacity_Requirement', 940040, '室温放电容量要求', 930006, 935001, 'APPENDIX_D', 'D-Step7', 'EXPLICIT', '1', 73, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 步骤8：外部资源
  (980028, 'http://example.org/standard/GB-T-31486-2024/ManufacturerTechSpec', 'ManufacturerTechSpec', 940072, '制造商技术条件', 930006, 935001, 'APPENDIX_D', 'D-Step8', 'EXPLICIT', '1', 80, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 步骤9：标准化对象
  (980029, 'http://example.org/standard/GB-T-31486-2024/BatteryStandardizationObject', 'BatteryStandardizationObject', 940010, '电动汽车用动力蓄电池', 930006, 935001, 'APPENDIX_D', 'D-Step9', 'EXPLICIT', '1', 90, NULL, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 8. 附录D种子数据：83条数据属性值
-- ----------------------------

-- 步骤1：980001 Standard（8条）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98000101, 980001, 950001, '规定电动汽车用动力蓄电池的电性能要求及试验方法', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98000102, 980001, 950002, '中文', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98000103, 980001, 950003, '现行', 'STRING', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  (98000104, 980001, 950004, '推荐性', 'STRING', NULL, NULL, 4, 'admin', now(), 'admin', now(), '0'),
  (98000105, 980001, 950005, '电动汽车用动力蓄电池电性能要求及试验方法', 'STRING', NULL, NULL, 5, 'admin', now(), 'admin', now(), '0'),
  (98000106, 980001, 950006, 'GB/T 31486—2024', 'STRING', NULL, NULL, 6, 'admin', now(), 'admin', now(), '0'),
  (98000107, 980001, 950007, '2024-09-29', 'DATE', NULL, NULL, 7, 'admin', now(), 'admin', now(), '0'),
  (98000108, 980001, 950008, '2025-04-01', 'DATE', NULL, NULL, 8, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 步骤2：980002 Section + 980003~980006 TitledClause（10条）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98000201, 980002, 950024, '5', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98000202, 980002, 950025, '性能要求', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98000301, 980003, 950026, '5.1', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98000302, 980003, 950027, '外观', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98000401, 980004, 950026, '5.2', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98000402, 980004, 950027, '极性标识', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98000501, 980005, 950026, '5.3', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98000502, 980005, 950027, '质量和外形尺寸', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98000601, 980006, 950026, '5.4', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98000602, 980006, 950027, '室温放电容量', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 步骤3：980007 Object（2条）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98000701, 980007, 950032, '电池单体', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98000702, 980007, 950033, '产品', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 步骤4：5个特性（17条 = 5×propertyName + 5×propertyDescription + 4×propertyType + 3×propertyValue）
-- propertyName(950034)、propertyDescription(952001)、propertyType(950036)、propertyValue(950035, TEXT_OR_NUMERIC)
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  -- 980008 外观
  (98000801, 980008, 950034, '外观', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98000802, 980008, 952001, '电池单体的外观状态', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98000803, 980008, 950036, '描述型', 'STRING', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  -- 980009 极性标识
  (98000901, 980009, 950034, '极性标识', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98000902, 980009, 952001, '电池端子极性标识的正确性和清晰度', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98000903, 980009, 950036, '描述型', 'STRING', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  -- 980010 质量
  (98001001, 980010, 950034, '质量', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98001002, 980010, 952001, '电池单体的质量', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98001003, 980010, 950036, '描述型', 'STRING', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  (98001004, 980010, 950035, '符合制造商提供的产品技术条件', 'STRING', NULL, NULL, 4, 'admin', now(), 'admin', now(), '0'),
  -- 980011 外形尺寸
  (98001101, 980011, 950034, '外形尺寸', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98001102, 980011, 952001, '电池单体的外部尺寸', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98001103, 980011, 950036, '描述型', 'STRING', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  (98001104, 980011, 950035, '符合制造商提供的产品技术条件', 'STRING', NULL, NULL, 4, 'admin', now(), 'admin', now(), '0'),
  -- 980012 室温放电容量
  (98001201, 980012, 950034, '室温放电容量', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98001202, 980012, 952001, '电池单体在室温下的放电容量性能', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98001203, 980012, 950036, '能力型', 'STRING', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 步骤5：6个显式约束（18条）
-- 980019无数据值（REFERENCE_ONLY）
-- constraintType(950037, OPEN_ENUM)、allowedValue(952002)、conformsTo(952003)、comparedTo(952004)、constraintTarget(952005)
-- minValue(950039)、maxValue(950038)、unit(950041, UNIT_REF → unit_id=920006百分比单位, symbol='%')
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  -- 980013 外观约束：枚举值 + 1个组合允许值（7项以顿号分隔，多值属性存储为单条字面量）
  (98001301, 980013, 950037, '枚举值', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98001302, 980013, 952002, '无变形、无裂纹、无毛刺、干燥、无外伤、无污物、有清晰正确的标志', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  -- 980014 极性标识约束：枚举值 + 1个组合允许值
  (98001401, 980014, 950037, '枚举值', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98001402, 980014, 952002, '正确、清晰', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  -- 980015 质量约束：符合性 + conformsTo
  (98001501, 980015, 950037, '符合性', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98001502, 980015, 952003, '制造商提供的产品技术条件', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  -- 980016 外形尺寸约束：符合性 + conformsTo
  (98001601, 980016, 950037, '符合性', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98001602, 980016, 952003, '制造商提供的产品技术条件', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  -- 980017 容量约束1：数值区间 + minValue + maxValue + unit(UNIT_REF) + comparedTo
  (98001701, 980017, 950037, '数值区间', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98001702, 980017, 950039, '100', 'INTEGER', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98001703, 980017, 950038, '110', 'INTEGER', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  (98001704, 980017, 950041, '%', 'STRING', 920006, '%', 4, 'admin', now(), 'admin', now(), '0'),
  (98001705, 980017, 952004, '额定容量', 'STRING', NULL, NULL, 5, 'admin', now(), 'admin', now(), '0'),
  -- 980018 容量约束2：相对值 + maxValue + unit(UNIT_REF) + comparedTo + constraintTarget
  (98001801, 980018, 950037, '相对值', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98001802, 980018, 950038, '5', 'INTEGER', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98001803, 980018, 950041, '%', 'STRING', 920006, '%', 3, 'admin', now(), 'admin', now(), '0'),
  (98001804, 980018, 952004, '初始容量平均值', 'STRING', NULL, NULL, 4, 'admin', now(), 'admin', now(), '0'),
  (98001805, 980018, 952005, '所有测试对象初始容量极差', 'STRING', NULL, NULL, 5, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 步骤6：4个行动（8条）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98002001, 980020, 952006, '外观检验', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98002002, 980020, 952007, '按6.2.1规定的方法进行外观检验', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98002101, 980021, 952006, '极性标识检验', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98002102, 980021, 952007, '按6.2.2规定的方法进行极性标识检验', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98002201, 980022, 952006, '质量和外形尺寸检验', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98002202, 980022, 952007, '按6.2.3规定的方法进行质量和外形尺寸检验', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98002301, 980023, 952006, '室温放电容量试验', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98002302, 980023, 952007, '按6.2.5规定的方法进行室温放电容量试验', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 步骤7：4个要求型条款（16条 = 4×4属性）
-- uniqueIdentifier(950028)、contentDescription(950029)、clauseType(950030, CLOSED_ENUM)、constraintTypeClause(950031, CLOSED_ENUM)
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98002401, 980024, 950028, 'IU-5-1001', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98002402, 980024, 950029, '电池单体按6.2.1检验，外观应无变形及裂纹，表面应无毛刺、干燥、无外伤、无污物，且应有清晰、正确的标志', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98002403, 980024, 950030, '要求型', 'STRING', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  (98002404, 980024, 950031, '强制', 'STRING', NULL, NULL, 4, 'admin', now(), 'admin', now(), '0'),
  (98002501, 980025, 950028, 'IU-5-2001', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98002502, 980025, 950029, '电池单体按6.2.2检验，端子极性标识应正确、清晰', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98002503, 980025, 950030, '要求型', 'STRING', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  (98002504, 980025, 950031, '强制', 'STRING', NULL, NULL, 4, 'admin', now(), 'admin', now(), '0'),
  (98002601, 980026, 950028, 'IU-5-3001', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98002602, 980026, 950029, '电池单体按6.2.3检验，电池质量、外形尺寸应符合制造商提供的产品技术条件', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98002603, 980026, 950030, '要求型', 'STRING', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  (98002604, 980026, 950031, '强制', 'STRING', NULL, NULL, 4, 'admin', now(), 'admin', now(), '0'),
  (98002701, 980027, 950028, 'IU-5-4001', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98002702, 980027, 950029, '电池单体按6.2.5试验，其初始容量不应低于额定容量，并且不超过额定容量的110%，同时所有测试对象初始容量极差不大于初始容量平均值的5%', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98002703, 980027, 950030, '要求型', 'STRING', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  (98002704, 980027, 950031, '强制', 'STRING', NULL, NULL, 4, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 步骤8：外部资源（2条）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98002801, 980028, 950042, '产品技术条件', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98002802, 980028, 950044, '制造商', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 步骤9：标准化对象（2条）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98002901, 980029, 950009, '电动汽车用动力蓄电池', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98002902, 980029, 950010, 'C3841 锂离子电池制造', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 9. 附录D种子数据：39条显式对象属性断言
-- 35条INSTANCE客体 + 4条ENTITY_TYPE客体(hasRepresentationForm→940056)
-- 断言ID：981001~981039
-- ----------------------------
INSERT INTO ont_instance_object_relation (id, subject_instance_id, object_property_id, object_kind, object_instance_id, object_entity_type_id, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  -- hasStructuralElement(960014): 980001(Standard)→980002(Section) — 1条
  (981001, 980001, 960014, 'INSTANCE', 980002, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  -- hasClause(960015): 980002(Section)→980003~980006(TitledClause) — 4条
  (981002, 980002, 960015, 'INSTANCE', 980003, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981003, 980002, 960015, 'INSTANCE', 980004, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (981004, 980002, 960015, 'INSTANCE', 980005, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  (981005, 980002, 960015, 'INSTANCE', 980006, NULL, 4, 'admin', now(), 'admin', now(), '0'),
  -- hasSubClause(960016): 980003~980006→980024~980027 — 4条
  (981006, 980003, 960016, 'INSTANCE', 980024, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981007, 980004, 960016, 'INSTANCE', 980025, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981008, 980005, 960016, 'INSTANCE', 980026, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981009, 980006, 960016, 'INSTANCE', 980027, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  -- hasCharacteristic(960026): 980007(Object)→980008~980012(Property) — 5条
  (981010, 980007, 960026, 'INSTANCE', 980008, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981011, 980007, 960026, 'INSTANCE', 980009, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (981012, 980007, 960026, 'INSTANCE', 980010, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  (981013, 980007, 960026, 'INSTANCE', 980011, NULL, 4, 'admin', now(), 'admin', now(), '0'),
  (981014, 980007, 960026, 'INSTANCE', 980012, NULL, 5, 'admin', now(), 'admin', now(), '0'),
  -- imposesConstraint(960027): 980008→980013; 980009→980014; 980010→980015; 980011→980016; 980012→980017/980018/980019 — 7条
  (981015, 980008, 960027, 'INSTANCE', 980013, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981016, 980009, 960027, 'INSTANCE', 980014, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981017, 980010, 960027, 'INSTANCE', 980015, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981018, 980011, 960027, 'INSTANCE', 980016, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981019, 980012, 960027, 'INSTANCE', 980017, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981020, 980012, 960027, 'INSTANCE', 980018, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (981021, 980012, 960027, 'INSTANCE', 980019, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  -- involvesObject(960024): 980024~980027(Clause)→980007(Object) — 4条
  (981022, 980024, 960024, 'INSTANCE', 980007, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981023, 980025, 960024, 'INSTANCE', 980007, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981024, 980026, 960024, 'INSTANCE', 980007, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981025, 980027, 960024, 'INSTANCE', 980007, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  -- specifiesCharacteristic(960025): 980024~980027→980008/980009/980010/980011/980012 — 5条
  (981026, 980024, 960025, 'INSTANCE', 980008, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981027, 980025, 960025, 'INSTANCE', 980009, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981028, 980026, 960025, 'INSTANCE', 980010, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981029, 980026, 960025, 'INSTANCE', 980011, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (981030, 980027, 960025, 'INSTANCE', 980012, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  -- describesAction(960030): 980024~980027→980020~980023 — 4条
  (981031, 980024, 960030, 'INSTANCE', 980020, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981032, 980025, 960030, 'INSTANCE', 980021, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981033, 980026, 960030, 'INSTANCE', 980022, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (981034, 980027, 960030, 'INSTANCE', 980023, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  -- hasRepresentationForm(960019): 980024~980027→ENTITY_TYPE 940056(TextualParagraph) — 4条
  (981035, 980024, 960019, 'ENTITY_TYPE', NULL, 940056, 1, 'admin', now(), 'admin', now(), '0'),
  (981036, 980025, 960019, 'ENTITY_TYPE', NULL, 940056, 1, 'admin', now(), 'admin', now(), '0'),
  (981037, 980026, 960019, 'ENTITY_TYPE', NULL, 940056, 1, 'admin', now(), 'admin', now(), '0'),
  (981038, 980027, 960019, 'ENTITY_TYPE', NULL, 940056, 1, 'admin', now(), 'admin', now(), '0'),
  -- standardizes(960012): 980001(Standard)→980029(StandardizationObject) — 1条
  (981039, 980001, 960012, 'INSTANCE', 980029, NULL, 1, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 10. 迁移完整性断言
-- ----------------------------
DO $$
BEGIN
  -- 1. 命名空间930006内容准确
  IF NOT EXISTS (SELECT 1 FROM ont_namespace WHERE id = 930006 AND prefix = 'gbt31486'
      AND uri = 'http://example.org/standard/GB-T-31486-2024/' AND is_builtin = '1') THEN
    RAISE EXCEPTION '命名空间930006(gbt31486)内容不正确';
  END IF;

  -- 2. 980001~980029连续存在且共29项，其中980019是REFERENCE_ONLY Constraint且无数据值
  IF (SELECT count(*) FROM ont_entity_instance WHERE is_builtin = '1' AND del_flag = '0') <> 29 THEN
    RAISE EXCEPTION '内置实例总数不是29条';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM ont_entity_instance WHERE id = 980019 AND declaration_mode = 'REFERENCE_ONLY'
      AND rdf_type_id = 940067 AND source_reference = 'D-Step4 dangling reference') THEN
    RAISE EXCEPTION '980019必须为REFERENCE_ONLY Constraint且来源为D-Step4 dangling reference';
  END IF;
  IF EXISTS (SELECT 1 FROM ont_instance_data_value WHERE instance_id = 980019 AND del_flag = '0') THEN
    RAISE EXCEPTION '980019(REFERENCE_ONLY)不应有数据属性值';
  END IF;

  -- 3. 无扩展实例TextualParagraph
  IF EXISTS (SELECT 1 FROM ont_entity_instance WHERE iri_local_name = 'TextualParagraph' AND del_flag = '0') THEN
    RAISE EXCEPTION '不应存在TextualParagraph扩展实例';
  END IF;

  -- 4. 所有实例类型有效、非抽象、同工程
  IF EXISTS (
    SELECT 1 FROM ont_entity_instance i
    JOIN ont_entity_type t ON i.rdf_type_id = t.id
    WHERE i.del_flag = '0' AND (t.is_abstract = '1' OR t.ontology_id <> i.ontology_id)
  ) THEN
    RAISE EXCEPTION '存在实例引用了抽象类型或跨工程类型';
  END IF;

  -- 5. IRI=namespace URI+local name，且与全部Schema IRI无冲突
  IF EXISTS (
    SELECT 1 FROM ont_entity_instance i
    JOIN ont_namespace n ON i.namespace_id = n.id
    WHERE i.del_flag = '0' AND i.iri <> (n.uri || i.iri_local_name)
  ) THEN
    RAISE EXCEPTION '存在实例IRI不等于命名空间URI与本地名的拼接';
  END IF;
  IF EXISTS (
    SELECT 1 FROM ont_entity_instance i
    WHERE i.del_flag = '0' AND i.iri IN (
      SELECT iri FROM ont_entity_type WHERE del_flag = '0'
      UNION SELECT iri FROM ont_data_property WHERE del_flag = '0'
      UNION SELECT iri FROM ont_object_property WHERE del_flag = '0'
    )
  ) THEN
    RAISE EXCEPTION '实例IRI与Schema IRI冲突';
  END IF;

  -- 6. 数据值总数83，步骤1恰8条，步骤2恰10条，步骤4恰17条，步骤5恰18条
  IF (SELECT count(*) FROM ont_instance_data_value WHERE del_flag = '0') <> 83 THEN
    RAISE EXCEPTION '数据属性值总数不是83条';
  END IF;
  IF (SELECT count(*) FROM ont_instance_data_value WHERE instance_id = 980001 AND del_flag = '0') <> 8 THEN
    RAISE EXCEPTION '步骤1(980001)数据值数量不是8条';
  END IF;
  IF (SELECT count(*) FROM ont_instance_data_value WHERE instance_id IN (980002, 980003, 980004, 980005, 980006) AND del_flag = '0') <> 10 THEN
    RAISE EXCEPTION '步骤2数据值数量不是10条';
  END IF;
  IF (SELECT count(*) FROM ont_instance_data_value WHERE instance_id IN (980008, 980009, 980010, 980011, 980012) AND del_flag = '0') <> 17 THEN
    RAISE EXCEPTION '步骤4数据值数量不是17条';
  END IF;
  IF (SELECT count(*) FROM ont_instance_data_value WHERE instance_id IN (980013, 980014, 980015, 980016, 980017, 980018) AND del_flag = '0') <> 18 THEN
    RAISE EXCEPTION '步骤5数据值数量不是18条';
  END IF;

  -- 7. 950042存在"产品技术条件"有效枚举且来源为D-Step8
  IF NOT EXISTS (SELECT 1 FROM ont_data_property_enum WHERE data_property_id = 950042 AND enum_value = '产品技术条件' AND source_reference = 'D-Step8') THEN
    RAISE EXCEPTION '950042(fileType)必须存在来源为D-Step8的"产品技术条件"枚举值';
  END IF;

  -- 8. 980017/980018的UNIT_REF引用有效百分比单位，值/快照/字典符号均为%
  IF NOT EXISTS (
    SELECT 1 FROM ont_instance_data_value v
    JOIN ont_unit u ON v.unit_id = u.id
    WHERE v.instance_id = 980017 AND v.data_property_id = 950041
      AND v.literal_value = '%' AND v.literal_symbol = '%' AND u.unit_symbol = '%' AND u.id = 920006
  ) THEN
    RAISE EXCEPTION '980017的UNIT_REF引用必须为百分比单位(920006)且值/快照/符号均为%%';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM ont_instance_data_value v
    JOIN ont_unit u ON v.unit_id = u.id
    WHERE v.instance_id = 980018 AND v.data_property_id = 950041
      AND v.literal_value = '%' AND v.literal_symbol = '%' AND u.unit_symbol = '%' AND u.id = 920006
  ) THEN
    RAISE EXCEPTION '980018的UNIT_REF引用必须为百分比单位(920006)且值/快照/符号均为%%';
  END IF;

  -- 9. 4个条款正文非空且不含省略占位符
  IF EXISTS (
    SELECT 1 FROM ont_instance_data_value
    WHERE data_property_id = 950029 AND del_flag = '0'
      AND (literal_value IS NULL OR literal_value = '' OR literal_value LIKE '%...%')
  ) THEN
    RAISE EXCEPTION '条款正文(contentDescription)不能为空或含省略占位符';
  END IF;

  -- 10. 显式断言总数39，谓词分布为1/4/4/5/7/4/5/4/4/1
  IF (SELECT count(*) FROM ont_instance_object_relation WHERE del_flag = '0') <> 39 THEN
    RAISE EXCEPTION '显式对象断言总数不是39条';
  END IF;
  IF (SELECT count(*) FROM ont_instance_object_relation WHERE object_property_id = 960014 AND del_flag = '0') <> 1 THEN
    RAISE EXCEPTION 'hasStructuralElement(960014)断言数量不是1条';
  END IF;
  IF (SELECT count(*) FROM ont_instance_object_relation WHERE object_property_id = 960015 AND del_flag = '0') <> 4 THEN
    RAISE EXCEPTION 'hasClause(960015)断言数量不是4条';
  END IF;
  IF (SELECT count(*) FROM ont_instance_object_relation WHERE object_property_id = 960016 AND del_flag = '0') <> 4 THEN
    RAISE EXCEPTION 'hasSubClause(960016)断言数量不是4条';
  END IF;
  IF (SELECT count(*) FROM ont_instance_object_relation WHERE object_property_id = 960026 AND del_flag = '0') <> 5 THEN
    RAISE EXCEPTION 'hasCharacteristic(960026)断言数量不是5条';
  END IF;
  IF (SELECT count(*) FROM ont_instance_object_relation WHERE object_property_id = 960027 AND del_flag = '0') <> 7 THEN
    RAISE EXCEPTION 'imposesConstraint(960027)断言数量不是7条';
  END IF;
  IF (SELECT count(*) FROM ont_instance_object_relation WHERE object_property_id = 960024 AND del_flag = '0') <> 4 THEN
    RAISE EXCEPTION 'involvesObject(960024)断言数量不是4条';
  END IF;
  IF (SELECT count(*) FROM ont_instance_object_relation WHERE object_property_id = 960025 AND del_flag = '0') <> 5 THEN
    RAISE EXCEPTION 'specifiesCharacteristic(960025)断言数量不是5条';
  END IF;
  IF (SELECT count(*) FROM ont_instance_object_relation WHERE object_property_id = 960030 AND del_flag = '0') <> 4 THEN
    RAISE EXCEPTION 'describesAction(960030)断言数量不是4条';
  END IF;
  IF (SELECT count(*) FROM ont_instance_object_relation WHERE object_property_id = 960012 AND del_flag = '0') <> 1 THEN
    RAISE EXCEPTION 'standardizes(960012)断言数量不是1条';
  END IF;

  -- 11. 4条hasRepresentationForm均object_kind='ENTITY_TYPE'且目标940056
  IF (SELECT count(*) FROM ont_instance_object_relation
      WHERE object_property_id = 960019 AND object_kind = 'ENTITY_TYPE' AND object_entity_type_id = 940056 AND del_flag = '0') <> 4 THEN
    RAISE EXCEPTION 'hasRepresentationForm(960019)必须为4条ENTITY_TYPE客体且目标为940056(TextualParagraph)';
  END IF;

  -- 12. 不存在constrainsObject/constrainsCharacteristic/referencesExternalResource附录D种子断言
  IF EXISTS (
    SELECT 1 FROM ont_instance_object_relation r
    JOIN ont_object_property p ON r.object_property_id = p.id
    WHERE p.iri_local_name IN ('constrainsObject', 'constrainsCharacteristic', 'referencesExternalResource')
      AND r.del_flag = '0'
  ) THEN
    RAISE EXCEPTION '不应存在constrainsObject/constrainsCharacteristic/referencesExternalResource种子断言';
  END IF;

  -- 13. 所有INSTANCE客体和ENTITY_TYPE客体有效
  IF EXISTS (
    SELECT 1 FROM ont_instance_object_relation r
    LEFT JOIN ont_entity_instance i ON r.object_instance_id = i.id
    WHERE r.object_kind = 'INSTANCE' AND r.del_flag = '0'
      AND (i.id IS NULL OR i.del_flag = '1')
  ) THEN
    RAISE EXCEPTION '存在INSTANCE客体指向已删除或不存在的实例';
  END IF;
  IF EXISTS (
    SELECT 1 FROM ont_instance_object_relation r
    LEFT JOIN ont_entity_type t ON r.object_entity_type_id = t.id
    WHERE r.object_kind = 'ENTITY_TYPE' AND r.del_flag = '0'
      AND (t.id IS NULL OR t.del_flag = '1')
  ) THEN
    RAISE EXCEPTION '存在ENTITY_TYPE客体指向已删除或不存在的实体类型';
  END IF;

  -- 14. 菜单900700~900704和角色1授权完整
  IF (SELECT count(*) FROM sys_menu WHERE menu_id BETWEEN 900700 AND 900704 AND del_flag = '0') <> 5 THEN
    RAISE EXCEPTION '实例管理菜单900700~900704不完整';
  END IF;
  IF (SELECT count(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 900700 AND 900704) <> 5 THEN
    RAISE EXCEPTION '实例管理管理员授权不完整';
  END IF;
END $$;
