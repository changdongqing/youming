-- ----------------------------
-- youming ontology unit dictionary
-- ----------------------------

CREATE TABLE ont_unit_category (
  id bigint NOT NULL,
  category_code varchar(64) NOT NULL,
  category_name varchar(128) NOT NULL,
  base_unit_symbol varchar(32) NOT NULL,
  is_builtin char(1) NOT NULL DEFAULT '0',
  sort_order integer NOT NULL DEFAULT 0,
  remarks varchar(255) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (id)
);

COMMENT ON TABLE ont_unit_category IS '本体建模-单位分类表';
COMMENT ON COLUMN ont_unit_category.id IS '分类ID';
COMMENT ON COLUMN ont_unit_category.category_code IS '分类编码';
COMMENT ON COLUMN ont_unit_category.category_name IS '分类名称';
COMMENT ON COLUMN ont_unit_category.base_unit_symbol IS '基准单位符号';
COMMENT ON COLUMN ont_unit_category.is_builtin IS '是否内置，1是0否';
COMMENT ON COLUMN ont_unit_category.sort_order IS '排序值';
COMMENT ON COLUMN ont_unit_category.remarks IS '备注';
COMMENT ON COLUMN ont_unit_category.create_by IS '创建人';
COMMENT ON COLUMN ont_unit_category.create_time IS '创建时间';
COMMENT ON COLUMN ont_unit_category.update_by IS '修改人';
COMMENT ON COLUMN ont_unit_category.update_time IS '更新时间';
COMMENT ON COLUMN ont_unit_category.del_flag IS '删除标志，0未删除，1已删除';

CREATE UNIQUE INDEX uk_ont_unit_category_code ON ont_unit_category (category_code) WHERE del_flag = '0';

CREATE TABLE ont_unit (
  id bigint NOT NULL,
  category_id bigint NOT NULL,
  unit_code varchar(64) NOT NULL,
  unit_symbol varchar(32) NOT NULL,
  unit_name varchar(128) NOT NULL,
  is_base_unit char(1) NOT NULL DEFAULT '0',
  is_builtin char(1) NOT NULL DEFAULT '0',
  factor numeric(30,12) DEFAULT NULL,
  offset_value numeric(30,12) DEFAULT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  namespace varchar(255) DEFAULT NULL,
  remarks varchar(255) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT fk_ont_unit_category FOREIGN KEY (category_id) REFERENCES ont_unit_category (id)
);

COMMENT ON TABLE ont_unit IS '本体建模-单位字典表';
COMMENT ON COLUMN ont_unit.id IS '单位ID';
COMMENT ON COLUMN ont_unit.category_id IS '分类ID';
COMMENT ON COLUMN ont_unit.unit_code IS '单位编码';
COMMENT ON COLUMN ont_unit.unit_symbol IS '单位符号';
COMMENT ON COLUMN ont_unit.unit_name IS '单位名称';
COMMENT ON COLUMN ont_unit.is_base_unit IS '是否基准单位，1是0否';
COMMENT ON COLUMN ont_unit.is_builtin IS '是否内置，1是0否';
COMMENT ON COLUMN ont_unit.factor IS '换算乘系数，base=factor*value+offset_value';
COMMENT ON COLUMN ont_unit.offset_value IS '换算偏移，base=factor*value+offset_value';
COMMENT ON COLUMN ont_unit.sort_order IS '排序值';
COMMENT ON COLUMN ont_unit.namespace IS '命名空间';
COMMENT ON COLUMN ont_unit.remarks IS '备注';
COMMENT ON COLUMN ont_unit.create_by IS '创建人';
COMMENT ON COLUMN ont_unit.create_time IS '创建时间';
COMMENT ON COLUMN ont_unit.update_by IS '修改人';
COMMENT ON COLUMN ont_unit.update_time IS '更新时间';
COMMENT ON COLUMN ont_unit.del_flag IS '删除标志，0未删除，1已删除';

CREATE INDEX idx_ont_unit_category_id ON ont_unit (category_id) WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_unit_code ON ont_unit (unit_code) WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_unit_category_symbol ON ont_unit (category_id, unit_symbol) WHERE del_flag = '0';
CREATE UNIQUE INDEX uk_ont_unit_category_base ON ont_unit (category_id) WHERE del_flag = '0' AND is_base_unit = '1';

-- ----------------------------
-- 菜单：本体建模 / 单位字典
-- ----------------------------
INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES (900000, '本体建模', NULL, '/ontology', NULL, -1, 'iconfont icon-zujian', '1', 3, '0', NULL, '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES (900100, '单位字典', NULL, '/ontology/unit/index', NULL, 900000, 'iconfont icon-zidianguanli', '1', 1, '0', NULL, '0', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag)
VALUES
  (900101, '单位查看', 'ontology_unit_view', NULL, NULL, 900100, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900102, '单位新增', 'ontology_unit_add', NULL, NULL, 900100, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900103, '单位修改', 'ontology_unit_edit', NULL, NULL, 900100, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0'),
  (900104, '单位删除', 'ontology_unit_del', NULL, NULL, 900100, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
  (1, 900000),
  (1, 900100),
  (1, 900101),
  (1, 900102),
  (1, 900103),
  (1, 900104)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ----------------------------
-- 内置单位分类
-- ----------------------------
INSERT INTO ont_unit_category (id, category_code, category_name, base_unit_symbol, is_builtin, sort_order, remarks, create_by, create_time, update_by, update_time, del_flag)
VALUES
  (910001, 'voltage', '电压', 'V', '1', 10, 'GB/T 48000.3 常用量纲：电压', 'admin', now(), 'admin', now(), '0'),
  (910002, 'power', '功率', 'W', '1', 20, 'GB/T 48000.3 常用量纲：功率', 'admin', now(), 'admin', now(), '0'),
  (910003, 'temperature', '温度', 'K', '1', 30, 'GB/T 48000.3 常用量纲：温度', 'admin', now(), 'admin', now(), '0'),
  (910004, 'dimensionless', '无量纲', '%', '1', 40, 'GB/T 48000.3 常用量纲：无量纲', 'admin', now(), 'admin', now(), '0'),
  (910005, 'current', '电流', 'A', '1', 50, 'GB/T 48000.3 常用量纲：电流', 'admin', now(), 'admin', now(), '0'),
  (910006, 'mass', '质量', 'kg', '1', 60, 'GB/T 48000.3 常用量纲：质量', 'admin', now(), 'admin', now(), '0'),
  (910007, 'length', '长度', 'm', '1', 70, 'GB/T 48000.3 常用量纲：长度', 'admin', now(), 'admin', now(), '0'),
  (910008, 'time', '时间', 's', '1', 80, 'GB/T 48000.3 常用量纲：时间', 'admin', now(), 'admin', now(), '0'),
  (910009, 'battery', '电池容量/能量', 'Ah', '1', 90, 'GB/T 48000.3 常用量纲：电池容量/能量', 'admin', now(), 'admin', now(), '0'),
  (910010, 'count', '计数', '个', '1', 100, 'GB/T 48000.3 常用量纲：计数', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 内置单位条目
-- factor/offset_value 用于同分类可换算单位；无法无上下文换算的单位保留 NULL，换算接口会明确提示未配置换算参数。
-- ----------------------------
INSERT INTO ont_unit (id, category_id, unit_code, unit_symbol, unit_name, is_base_unit, is_builtin, factor, offset_value, sort_order, namespace, remarks, create_by, create_time, update_by, update_time, del_flag)
VALUES
  (920001, 910001, 'volt', 'V', '伏特', '1', '1', 1, 0, 10, NULL, '电压基准单位', 'admin', now(), 'admin', now(), '0'),
  (920002, 910002, 'watt', 'W', '瓦特', '1', '1', 1, 0, 10, NULL, '功率基准单位', 'admin', now(), 'admin', now(), '0'),
  (920003, 910002, 'kilowatt', 'kW', '千瓦', '0', '1', 1000, 0, 20, NULL, '1 kW = 1000 W', 'admin', now(), 'admin', now(), '0'),
  (920004, 910003, 'kelvin', 'K', '开尔文', '1', '1', 1, 0, 10, NULL, '热力学温度基准单位', 'admin', now(), 'admin', now(), '0'),
  (920005, 910003, 'celsius', '℃', '摄氏度', '0', '1', 1, 273.15, 20, NULL, 'K = ℃ + 273.15', 'admin', now(), 'admin', now(), '0'),
  (920006, 910004, 'percent', '%', '百分比', '1', '1', 1, 0, 10, NULL, '无量纲百分比', 'admin', now(), 'admin', now(), '0'),
  (920007, 910005, 'ampere', 'A', '安培', '1', '1', 1, 0, 10, NULL, '电流基准单位', 'admin', now(), 'admin', now(), '0'),
  (920008, 910006, 'kilogram', 'kg', '千克', '1', '1', 1, 0, 10, NULL, '质量基准单位', 'admin', now(), 'admin', now(), '0'),
  (920009, 910006, 'gram', 'g', '克', '0', '1', 0.001, 0, 20, NULL, '1 g = 0.001 kg', 'admin', now(), 'admin', now(), '0'),
  (920010, 910007, 'meter', 'm', '米', '1', '1', 1, 0, 10, NULL, '长度基准单位', 'admin', now(), 'admin', now(), '0'),
  (920011, 910007, 'millimeter', 'mm', '毫米', '0', '1', 0.001, 0, 20, NULL, '1 mm = 0.001 m', 'admin', now(), 'admin', now(), '0'),
  (920012, 910007, 'centimeter', 'cm', '厘米', '0', '1', 0.01, 0, 30, NULL, '1 cm = 0.01 m', 'admin', now(), 'admin', now(), '0'),
  (920013, 910008, 'second', 's', '秒', '1', '1', 1, 0, 10, NULL, '时间基准单位', 'admin', now(), 'admin', now(), '0'),
  (920014, 910008, 'minute', 'min', '分钟', '0', '1', 60, 0, 20, NULL, '1 min = 60 s', 'admin', now(), 'admin', now(), '0'),
  (920015, 910008, 'hour', 'h', '小时', '0', '1', 3600, 0, 30, NULL, '1 h = 3600 s', 'admin', now(), 'admin', now(), '0'),
  (920016, 910009, 'ampere_hour', 'Ah', '安时', '1', '1', NULL, NULL, 10, NULL, '电池容量单位，无法与 Wh 无上下文换算', 'admin', now(), 'admin', now(), '0'),
  (920017, 910009, 'watt_hour', 'Wh', '瓦时', '0', '1', NULL, NULL, 20, NULL, '电池能量单位，无法与 Ah 无上下文换算', 'admin', now(), 'admin', now(), '0'),
  (920018, 910010, 'count_piece', '个', '个', '1', '1', NULL, NULL, 10, NULL, '计数单位', 'admin', now(), 'admin', now(), '0'),
  (920019, 910010, 'count_times', '次', '次', '0', '1', NULL, NULL, 20, NULL, '次数单位', 'admin', now(), 'admin', now(), '0'),
  (920020, 910010, 'count_set', '台', '台', '0', '1', NULL, NULL, 30, NULL, '设备台数单位', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;
