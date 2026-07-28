-- ============================================================
-- V9__ont_unit_seed.sql
-- 单位注册表：补 SN 列 + 8 量纲种子 + 常用单位种子 + 权限点
-- 对应 PRD v1.2 FR-3 AC-3.1/3.2/3.8、NFR-9
-- 依赖 V4（表结构）、V5（ont_governance_meta.qudt_version + 菜单 10300）
-- 数据精确抽取自 QUDT 3.x：docs/ontology/参考开源本体库/qudt/qudt-all.ttl
-- ============================================================

-- ---------- (a) 补科学计数系数列（AC-3.8 精度，NFR-9/R-6） ----------
-- ALTER ADD COLUMN 不改 V4，遵循"禁改已应用脚本"
ALTER TABLE ont_unit ADD COLUMN conversion_multiplier_sn varchar(32);
ALTER TABLE ont_unit ADD COLUMN conversion_offset_sn     varchar(32);
COMMENT ON COLUMN ont_unit.conversion_multiplier_sn IS 'QUDT 科学计数系数（如 1.0E3），换算引擎优先用此列减少浮点误差';
COMMENT ON COLUMN ont_unit.conversion_offset_sn     IS 'QUDT 偏移科学计数系数（如 4.5967E2）';

-- ---------- (b) 8 量纲种子（ont_quantity_kind，AC-3.1） ----------
-- dimension_vector 来自 QUDT qkdv:xxx，格式 A?E?L?I?M?H?T?D?（A=角度,E=电荷,L=长度,I=电流,M=质量,H=温度,T=时间,D=物质的量）
-- id 用 5xxx 段（量纲），6xxx 段（单位），便于跨脚本引用
INSERT INTO ont_quantity_kind (id, qudt_iri, label, label_cn, dimension_vector, sort_order, create_by, create_time, update_by, update_time) VALUES
(5001, 'http://qudt.org/vocab/quantitykind/Length',                   'Length',                   '长度',     'A0E0L1I0M0H0T0D0', 1, 'admin', now(), 'admin', now()),
(5002, 'http://qudt.org/vocab/quantitykind/Mass',                     'Mass',                     '质量',     'A0E0L0I0M1H0T0D0', 2, 'admin', now(), 'admin', now()),
(5003, 'http://qudt.org/vocab/quantitykind/Time',                     'Time',                     '时间',     'A0E0L0I0M0H0T1D0', 3, 'admin', now(), 'admin', now()),
(5004, 'http://qudt.org/vocab/quantitykind/ThermodynamicTemperature',  'ThermodynamicTemperature', '热力学温度','A0E0L0I0M0H1T0D0', 4, 'admin', now(), 'admin', now()),
(5005, 'http://qudt.org/vocab/quantitykind/Area',                     'Area',                     '面积',     'A0E0L2I0M0H0T0D0', 5, 'admin', now(), 'admin', now()),
(5006, 'http://qudt.org/vocab/quantitykind/Volume',                   'Volume',                   '体积',     'A0E0L3I0M0H0T0D0', 6, 'admin', now(), 'admin', now()),
(5007, 'http://qudt.org/vocab/quantitykind/Currency',                 'Currency',                 '货币',     'A0E0L0I0M0H0T0D0', 7, 'admin', now(), 'admin', now()),
(5008, 'http://qudt.org/vocab/quantitykind/InformationEntropy',       'InformationEntropy',       '数据量',   'A0E0L0I0M0H0T0D0', 8, 'admin', now(), 'admin', now());

-- ---------- (c) 常用单位种子（ont_unit，AC-3.1/3.2） ----------
-- 每量纲含 1 基准 + >=2 派生；conversion_multiplier / SN 精确来自 QUDT
-- quantity_kind_id 指向上层量纲；scaling_of 指向基准单位 qudt_iri（基准单位自身 scaling_of=NULL）
-- source='builtin'（随 Flyway 分发，只读）；source_ref='qudt'（溯源）

-- (c1) 长度（基准 M）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6001, 'http://qudt.org/vocab/unit/M',       'm',  'Metre',      '米',     5001, 1.0,    '1.0E0',  NULL, NULL, NULL,                          'm',  'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6002, 'http://qudt.org/vocab/unit/CentiM',  'cm', 'Centimetre', '厘米',   5001, 0.01,   '1.0E-2', NULL, NULL, 'http://qudt.org/vocab/unit/M', 'cm', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6003, 'http://qudt.org/vocab/unit/MilliM',  'mm', 'Millimetre', '毫米',   5001, 0.001,  '1.0E-3', NULL, NULL, 'http://qudt.org/vocab/unit/M', 'mm', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6004, 'http://qudt.org/vocab/unit/KiloM',   'km', 'Kilometre',  '千米',   5001, 1000.0, '1.0E3',  NULL, NULL, 'http://qudt.org/vocab/unit/M', 'km', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now());

-- (c2) 质量（基准 KiloGM）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6011, 'http://qudt.org/vocab/unit/KiloGM',  'kg', 'Kilogram', '千克', 5002, 1.0,    '1.0E0', NULL, NULL, NULL,                               'kg', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6012, 'http://qudt.org/vocab/unit/GM',      'g',  'Gram',     '克',   5002, 0.001,  '1.0E-3', NULL, NULL, 'http://qudt.org/vocab/unit/KiloGM','g',  'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6013, 'http://qudt.org/vocab/unit/Tonne',   't',  'Tonne',    '吨',   5002, 1000.0, '1.0E3',  NULL, NULL, 'http://qudt.org/vocab/unit/KiloGM','t',  'builtin', 'qudt', '0', 'admin', now(), 'admin', now());

-- (c3) 时间（基准 SEC）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6021, 'http://qudt.org/vocab/unit/SEC',   's',   'Second', '秒',   5003, 1.0,    '1.0E0', NULL, NULL, NULL,                           's',  'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6022, 'http://qudt.org/vocab/unit/MIN',   'min', 'Minute', '分钟', 5003, 60.0,   '6.0E1', NULL, NULL, 'http://qudt.org/vocab/unit/SEC','min','builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6023, 'http://qudt.org/vocab/unit/HR',    'h',   'Hour',   '小时', 5003, 3600.0, '3.6E3', NULL, NULL, 'http://qudt.org/vocab/unit/SEC','h',  'builtin', 'qudt', '0', 'admin', now(), 'admin', now());

-- (c4) 温度（基准 K，有偏移，AC-3.4）
-- QUDT 基准为 Kelvin；DEG_C/DEG_F 带 conversionOffset（相对 Kelvin 基准）
-- 换算公式（QUDT 约定）：base = (value + offset) × multiplier；逆向 value = base/multiplier - offset
-- 例：DEG_F 32°F -> base=(32+459.67)×0.5556=273.15K；-> DEG_C 273.15/1.0-273.15=0°C（32°F=0°C）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6031, 'http://qudt.org/vocab/unit/K',      'K',   'Kelvin',     '开尔文', 5004, 1.0,                  '1.0E0',                  NULL,    NULL,       NULL, 'K',    'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6032, 'http://qudt.org/vocab/unit/DEG_C',  '°C', 'Celsius',    '摄氏度', 5004, 1.0,                  '1.0E0',                  273.15,  '2.7315E2', NULL, 'Cel',  'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6033, 'http://qudt.org/vocab/unit/DEG_F',  '°F', 'Fahrenheit', '华氏度', 5004, 0.5555555555555556,  '5.555555555555556E-1',   459.67,  '4.5967E2', NULL, '[degF]','builtin','qudt', '0','admin', now(), 'admin', now());

-- (c5) 面积（基准 M2）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6041, 'http://qudt.org/vocab/unit/M2',      'm²',  'SquareMetre',      '平方米',   5005, 1.0,     '1.0E0', NULL, NULL, NULL,                          'm2', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6042, 'http://qudt.org/vocab/unit/CentiM2', 'cm²', 'SquareCentimetre', '平方厘米', 5005, 0.0001,  '1.0E-4',NULL, NULL, 'http://qudt.org/vocab/unit/M2','cm2','builtin','qudt', '0','admin', now(), 'admin', now()),
(6043, 'http://qudt.org/vocab/unit/HA',      'ha',  'Hectare',          '公顷',     5005, 10000.0, '1.0E4', NULL, NULL, 'http://qudt.org/vocab/unit/M2','ha', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now());

-- (c6) 体积（基准 M3，L 为派生）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6051, 'http://qudt.org/vocab/unit/M3',    'm³',  'CubicMetre',  '立方米', 5006, 1.0,      '1.0E0',  NULL, NULL, NULL,                          'm3', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6052, 'http://qudt.org/vocab/unit/L',     'L',   'Litre',       '升',     5006, 0.001,    '1.0E-3', NULL, NULL, 'http://qudt.org/vocab/unit/M3','L',  'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6053, 'http://qudt.org/vocab/unit/MilliL','mL',  'Millilitre',  '毫升',   5006, 0.000001, '1.0E-6', NULL, NULL, 'http://qudt.org/vocab/unit/L', 'mL', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now());

-- (c7) 货币（基准 USD，标注"参考汇率"非实时，R-7）
-- 货币非 QUDT 强项，conversion_multiplier 为示例参考汇率，实际换算需接外部源（OoS）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6061, 'http://qudt.org/vocab/unit/USD', 'US$', 'USDollar',    '美元',   5007, 1.0,   '1.0E0',  NULL, NULL, NULL,                          'USD', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6062, 'http://qudt.org/vocab/unit/CNY', '¥',   'ChineseYuan', '人民币', 5007, 0.14,  '1.4E-1', NULL, NULL, 'http://qudt.org/vocab/unit/USD','CNY','builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6063, 'http://qudt.org/vocab/unit/EUR', '€',   'Euro',        '欧元',   5007, 1.08,  '1.08E0', NULL, NULL, 'http://qudt.org/vocab/unit/USD','EUR','builtin', 'qudt', '0', 'admin', now(), 'admin', now());

-- (c8) 数据量（基准 BYTE，二进制前缀）
INSERT INTO ont_unit (id, qudt_iri, symbol, label, label_cn, quantity_kind_id, conversion_multiplier, conversion_multiplier_sn, conversion_offset, conversion_offset_sn, scaling_of, ucum_code, source, source_ref, deprecated, create_by, create_time, update_by, update_time) VALUES
(6071, 'http://qudt.org/vocab/unit/BYTE',    'B',  'Byte',     '字节',   5008, 1.0,       '1.0E0',       NULL, NULL, NULL,                               'B',  'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6072, 'http://qudt.org/vocab/unit/KiloBYTE','KB', 'Kilobyte', '千字节', 5008, 1024.0,    '1.024E3',     NULL, NULL, 'http://qudt.org/vocab/unit/BYTE',  'kB', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now()),
(6073, 'http://qudt.org/vocab/unit/MegaBYTE','MB', 'Megabyte', '兆字节', 5008, 1048576.0, '1.048576E6',  NULL, NULL, 'http://qudt.org/vocab/unit/BYTE',  'MB', 'builtin', 'qudt', '0', 'admin', now(), 'admin', now());

-- ---------- (d) 单位权限点按钮（挂在单位注册表菜单 10300 下） ----------
-- view/manage 对齐 PRD 13.2；换算走 view 权限（建模侧可调供给换算）
INSERT INTO sys_menu VALUES (10301, '单位新增',  'ont_unit_manage', NULL, NULL, 10300, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10302, '单位编辑',  'ont_unit_manage', NULL, NULL, 10300, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10303, '单位删除',  'ont_unit_manage', NULL, NULL, 10300, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10304, '单位查看',  'ont_unit_view',   NULL, NULL, 10300, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
