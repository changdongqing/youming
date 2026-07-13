-- =============================================
-- V16: 消防系统-火灾报警系统 ontology 扩展种子数据
-- 依据：docs/ontology/业务设计文档/消防系统/火灾报警系统Ontology建模设计.md
-- 业务场景：docs/ontology/需求与业务场景/消防系统/火灾报警系统业务场景.md
--
-- 本脚本通过 V13 扩展管理机制创建消防系统扩展：
--   1. 扩展命名空间 fire
--   2. 扩展模块 fire-protection（归属核心工程 935001）
--   3. 14 个扩展实体类型（继承核心 Object/ActionClass/Stakeholder）
--   4. 16 个扩展数据属性
--   5. 10 个扩展对象属性
--   6. 3 条扩展公理规则（DRAFT）
--   7. 11 个种子实例 + 数据属性值 + 对象属性断言
--   8. 扩展资源关联（ont_extension_resource）
--
-- 所有扩展资源 is_builtin='0'，归属 ontology_id=935001，
-- 可视化页面（固定查询 ontologyId=935001）自动展示。
-- =============================================

-- ----------------------------
-- 1. 扩展命名空间 fire
-- ----------------------------
INSERT INTO ont_namespace (id, prefix, uri, is_default, is_builtin, sort_order, description, create_by, create_time, update_by, update_time, del_flag)
VALUES (930100, 'fire', 'http://example.org/fire-ontology#', '0', '0', 200, '消防系统扩展命名空间（火灾报警/喷淋/消火栓等子场景）', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 2. 扩展模块 fire-protection
-- ----------------------------
INSERT INTO ont_extension_module (id, module_code, module_name, namespace_id, ontology_id, description, version, is_builtin, sort_order, remarks, create_by, create_time, update_by, update_time, del_flag)
VALUES (939001, 'fire-protection', '消防系统扩展', 930100, 935001, '消防系统本体扩展模块，首期包含火灾报警系统，后续扩展喷淋系统、消火栓系统等', '1.0.0', '0', 10, NULL, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 3. 扩展实体类型（14 条，id 941001~941014）
-- 命名空间 930100(fire)、本体工程 935001(core)
-- IRI 前缀 http://example.org/fire-ontology#
-- ----------------------------
INSERT INTO ont_entity_type (id, iri, name, definition, is_abstract, is_builtin, ontology_id, namespace_id, sort_order, remarks, create_by, create_time, update_by, update_time, del_flag) VALUES
  -- 设备类
  (941001, 'http://example.org/fire-ontology#FireProtectionDevice', 'FireProtectionDevice', '消防探测设备（抽象基类），感烟/感温/手动报警按钮等火警探测设备的父类', '1', '0', 935001, 930100, 200, NULL, 'admin', now(), 'admin', now(), '0'),
  (941002, 'http://example.org/fire-ontology#SmokeDetector', 'SmokeDetector', '感烟探测器，检测环境中烟雾浓度并触发火警事件状态', '0', '0', 935001, 930100, 201, NULL, 'admin', now(), 'admin', now(), '0'),
  (941003, 'http://example.org/fire-ontology#HeatDetector', 'HeatDetector', '感温探测器，检测环境温度异常上升并触发火警事件状态', '0', '0', 935001, 930100, 202, NULL, 'admin', now(), 'admin', now(), '0'),
  (941004, 'http://example.org/fire-ontology#ManualCallPoint', 'ManualCallPoint', '手动报警按钮，人工按下后触发火警事件状态', '0', '0', 935001, 930100, 203, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 事件状态类
  (941005, 'http://example.org/fire-ontology#DeviceEventState', 'DeviceEventState', '设备事件状态，由火警探测设备物联采集实时上报，枚举为正常/火警/故障/屏蔽', '0', '0', 935001, 930100, 210, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 工单类
  (941006, 'http://example.org/fire-ontology#FireWorkOrder', 'FireWorkOrder', '消防工单（抽象基类），消防系统各类工单的父类', '1', '0', 935001, 930100, 220, NULL, 'admin', now(), 'admin', now(), '0'),
  (941007, 'http://example.org/fire-ontology#ServiceRequestOrder', 'ServiceRequestOrder', '服务申请工单，设备故障/屏蔽后发起服务申请，可派单生成服务工单', '0', '0', 935001, 930100, 221, NULL, 'admin', now(), 'admin', now(), '0'),
  (941008, 'http://example.org/fire-ontology#ServiceWorkOrder', 'ServiceWorkOrder', '服务工单，维修工人接单或被派单后执行维修、更换等服务', '0', '0', 935001, 930100, 222, NULL, 'admin', now(), 'admin', now(), '0'),
  (941009, 'http://example.org/fire-ontology#FireAlarmSiteCheckOrder', 'FireAlarmSiteCheckOrder', '火警现场复核工单，派单给现场附近人员到现场确认火警真实性并反馈结果', '0', '0', 935001, 930100, 223, NULL, 'admin', now(), 'admin', now(), '0'),
  (941010, 'http://example.org/fire-ontology#FireAlarmDutyHandleOrder', 'FireAlarmDutyHandleOrder', '火警值班处置工单，值班人员根据现场复核反馈进行火警结论提报', '0', '0', 935001, 930100, 224, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 维保类
  (941011, 'http://example.org/fire-ontology#MaintenanceRecord', 'MaintenanceRecord', '维保记录，记录火警探测设备的维保过程及结果', '0', '0', 935001, 930100, 230, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 火警事件类
  (941012, 'http://example.org/fire-ontology#FireAlarmEvent', 'FireAlarmEvent', '火警事件，由设备火警状态触发的事件，关联现场复核和值班处置工单', '0', '0', 935001, 930100, 240, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 角色类
  (941013, 'http://example.org/fire-ontology#FireDutyRoom', 'FireDutyRoom', '消防值班室，值班人员值守的场所，接收火警事件并处置', '0', '0', 935001, 930100, 250, NULL, 'admin', now(), 'admin', now(), '0'),
  (941014, 'http://example.org/fire-ontology#MaintenanceWorker', 'MaintenanceWorker', '维修工人，接单执行设备维修、更换等服务的个人', '0', '0', 935001, 930100, 251, NULL, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 4. 实体类型中文标签（14 条）
-- ----------------------------
INSERT INTO ont_entity_type_label (entity_type_id, locale, label) VALUES
  (941001, 'zh', '消防探测设备'), (941002, 'zh', '感烟探测器'), (941003, 'zh', '感温探测器'), (941004, 'zh', '手动报警按钮'),
  (941005, 'zh', '设备事件状态'),
  (941006, 'zh', '消防工单'), (941007, 'zh', '服务申请工单'), (941008, 'zh', '服务工单'),
  (941009, 'zh', '火警现场复核工单'), (941010, 'zh', '火警值班处置工单'),
  (941011, 'zh', '维保记录'),
  (941012, 'zh', '火警事件'),
  (941013, 'zh', '消防值班室'), (941014, 'zh', '维修工人')
ON CONFLICT (entity_type_id, locale) DO NOTHING;

-- ----------------------------
-- 5. 继承关系（14 条）
-- ----------------------------
INSERT INTO ont_entity_type_hierarchy (parent_id, child_id) VALUES
  -- 设备继承自核心 Object(940063)
  (940063, 941001),
  -- 探测设备子类
  (941001, 941002), (941001, 941003), (941001, 941004),
  -- 事件状态继承自核心 Object(940063)
  (940063, 941005),
  -- 工单继承自核心 Object(940063)
  (940063, 941006),
  -- 工单子类
  (941006, 941007), (941006, 941008), (941006, 941009), (941006, 941010),
  -- 维保记录继承自核心 Object(940063)
  (940063, 941011),
  -- 火警事件继承自核心 ActionClass(940068)
  (940068, 941012),
  -- 消防值班室继承自核心 Organization(940012)
  (940012, 941013),
  -- 维修工人继承自核心 Individual(940021)
  (940021, 941014)
ON CONFLICT (parent_id, child_id) DO NOTHING;

-- ----------------------------
-- 6. 不相交关系：消防探测设备 ⇄ 消防工单
-- ----------------------------
INSERT INTO ont_entity_type_disjoint (type_a, type_b) VALUES
  (941001, 941006)
ON CONFLICT (type_a, type_b) DO NOTHING;

-- ----------------------------
-- 7. 扩展数据属性（16 条，id 953001~953016）
-- source_type='EXTENSION', is_builtin='0'
-- 命名空间 930100(fire)、本体工程 935001(core)
-- ----------------------------
INSERT INTO ont_data_property (id, iri, iri_local_name, standard_iri, name, preferred_alias, definition, domain_entity_type_id, base_type, value_mode, value_source_ref, regex_pattern, format_hint, is_unique, unit_category_id, unit_ref_mode, source_type, source_reference, is_builtin, ontology_id, namespace_id, sort_order, remarks, create_by, create_time, update_by, update_time, del_flag) VALUES
  -- 设备属性
  (953001, 'http://example.org/fire-ontology#deviceCode', 'deviceCode', NULL, 'deviceCode', NULL, '设备编码，全局唯一', 941001, 'TEXT', 'FREE', NULL, NULL, NULL, '1', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 200, NULL, 'admin', now(), 'admin', now(), '0'),
  (953002, 'http://example.org/fire-ontology#deviceLocation', 'deviceLocation', NULL, 'deviceLocation', NULL, '设备安装位置', 941001, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 201, NULL, 'admin', now(), 'admin', now(), '0'),
  (953003, 'http://example.org/fire-ontology#deviceStatus', 'deviceStatus', NULL, 'deviceStatus', NULL, '设备在线状态', 941001, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 202, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 事件状态属性
  (953004, 'http://example.org/fire-ontology#eventStateValue', 'eventStateValue', NULL, 'eventStateValue', NULL, '事件状态值：正常/火警/故障/屏蔽', 941005, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 210, NULL, 'admin', now(), 'admin', now(), '0'),
  (953005, 'http://example.org/fire-ontology#eventTimestamp', 'eventTimestamp', NULL, 'eventTimestamp', NULL, '事件上报时间戳', 941005, 'DATE', 'FREE', NULL, NULL, 'YYYY-MM-DD HH:MI:SS', '0', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 211, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 工单属性
  (953006, 'http://example.org/fire-ontology#workOrderCode', 'workOrderCode', NULL, 'workOrderCode', NULL, '工单编号，全局唯一', 941006, 'TEXT', 'FREE', NULL, NULL, NULL, '1', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 220, NULL, 'admin', now(), 'admin', now(), '0'),
  (953007, 'http://example.org/fire-ontology#workOrderStatus', 'workOrderStatus', NULL, 'workOrderStatus', NULL, '工单状态', 941006, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 221, NULL, 'admin', now(), 'admin', now(), '0'),
  (953008, 'http://example.org/fire-ontology#workOrderType', 'workOrderType', NULL, 'workOrderType', NULL, '工单类型', 941006, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 222, NULL, 'admin', now(), 'admin', now(), '0'),
  (953009, 'http://example.org/fire-ontology#workOrderDesc', 'workOrderDesc', NULL, 'workOrderDesc', NULL, '工单描述', 941006, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 223, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 火警结论属性
  (953010, 'http://example.org/fire-ontology#alarmConclusion', 'alarmConclusion', NULL, 'alarmConclusion', NULL, '火警结论：真实火警/误报/设备故障', 941010, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 224, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 现场复核结果属性
  (953011, 'http://example.org/fire-ontology#siteCheckResult', 'siteCheckResult', NULL, 'siteCheckResult', NULL, '现场复核结果：确认火警/误报/无法确认', 941009, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 225, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 维保属性
  (953012, 'http://example.org/fire-ontology#maintenanceResult', 'maintenanceResult', NULL, 'maintenanceResult', NULL, '维保结果：合格/不合格', 941011, 'TEXT', 'CLOSED_ENUM', NULL, NULL, NULL, '0', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 230, NULL, 'admin', now(), 'admin', now(), '0'),
  (953013, 'http://example.org/fire-ontology#maintenancePeriod', 'maintenancePeriod', NULL, 'maintenancePeriod', NULL, '维保时间段描述', 941011, 'TEXT', 'FREE', NULL, NULL, '起始时间-结束时间', '0', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 231, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 火警事件属性
  (953014, 'http://example.org/fire-ontology#alarmEventCode', 'alarmEventCode', NULL, 'alarmEventCode', NULL, '火警事件编码，全局唯一', 941012, 'TEXT', 'FREE', NULL, NULL, NULL, '1', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 240, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 角色属性
  (953015, 'http://example.org/fire-ontology#workerName', 'workerName', NULL, 'workerName', NULL, '维修工人姓名', 941014, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 250, NULL, 'admin', now(), 'admin', now(), '0'),
  (953016, 'http://example.org/fire-ontology#dutyRoomName', 'dutyRoomName', NULL, 'dutyRoomName', NULL, '值班室名称', 941013, 'TEXT', 'FREE', NULL, NULL, NULL, '0', NULL, NULL, 'EXTENSION', 'fire-fas', '0', 935001, 930100, 251, NULL, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 8. 数据属性中文标签（16 条）
-- ----------------------------
INSERT INTO ont_data_property_label (data_property_id, locale, label) VALUES
  (953001, 'zh', '设备编码'), (953002, 'zh', '设备位置'), (953003, 'zh', '设备状态'),
  (953004, 'zh', '事件状态值'), (953005, 'zh', '事件时间戳'),
  (953006, 'zh', '工单编号'), (953007, 'zh', '工单状态'), (953008, 'zh', '工单类型'), (953009, 'zh', '工单描述'),
  (953010, 'zh', '火警结论'),
  (953011, 'zh', '现场复核结果'),
  (953012, 'zh', '维保结果'), (953013, 'zh', '维保时间段'),
  (953014, 'zh', '火警事件编码'),
  (953015, 'zh', '维修工人姓名'), (953016, 'zh', '值班室名称')
ON CONFLICT (data_property_id, locale) DO NOTHING;

-- ----------------------------
-- 9. 数据属性枚举值（6 个枚举属性，19 条）
-- ----------------------------
-- 设备状态（2 条）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (953003, '在线', NULL, '1', 'fire-fas', 1),
  (953003, '离线', NULL, '1', 'fire-fas', 2)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- 事件状态值（4 条：正常/火警/故障/屏蔽）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (953004, '正常', NULL, '1', 'fire-fas', 1),
  (953004, '火警', NULL, '1', 'fire-fas', 2),
  (953004, '故障', NULL, '1', 'fire-fas', 3),
  (953004, '屏蔽', NULL, '1', 'fire-fas', 4)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- 工单状态（5 条）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (953007, '待派单', NULL, '1', 'fire-fas', 1),
  (953007, '待接单', NULL, '1', 'fire-fas', 2),
  (953007, '处理中', NULL, '1', 'fire-fas', 3),
  (953007, '已完成', NULL, '1', 'fire-fas', 4),
  (953007, '已取消', NULL, '1', 'fire-fas', 5)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- 工单类型（4 条）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (953008, '服务申请', NULL, '1', 'fire-fas', 1),
  (953008, '服务', NULL, '1', 'fire-fas', 2),
  (953008, '火警现场复核', NULL, '1', 'fire-fas', 3),
  (953008, '火警值班处置', NULL, '1', 'fire-fas', 4)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- 火警结论（3 条）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (953010, '真实火警', NULL, '1', 'fire-fas', 1),
  (953010, '误报', NULL, '1', 'fire-fas', 2),
  (953010, '设备故障', NULL, '1', 'fire-fas', 3)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- 现场复核结果（3 条）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (953011, '确认火警', NULL, '1', 'fire-fas', 1),
  (953011, '误报', NULL, '1', 'fire-fas', 2),
  (953011, '无法确认', NULL, '1', 'fire-fas', 3)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- 维保结果（2 条）
INSERT INTO ont_data_property_enum (data_property_id, enum_value, canonical_value, is_standard, source_reference, sort_order) VALUES
  (953012, '合格', NULL, '1', 'fire-fas', 1),
  (953012, '不合格', NULL, '1', 'fire-fas', 2)
ON CONFLICT (data_property_id, enum_value) DO NOTHING;

-- ----------------------------
-- 10. 扩展对象属性（10 条，id 961001~961010）
-- source_type='EXTENSION', is_builtin='0'
-- 命名空间 930100(fire)、本体工程 935001(core)
-- ----------------------------
INSERT INTO ont_object_property (id, iri, iri_local_name, name, definition, inverse_of_id, is_functional, is_inverse_functional, is_transitive, is_symmetric, source_type, source_reference, is_builtin, ontology_id, namespace_id, sort_order, remarks, create_by, create_time, update_by, update_time, del_flag) VALUES
  (961001, 'http://example.org/fire-ontology#reportsEventState', 'reportsEventState', 'reportsEventState', '消防探测设备上报事件状态', NULL, '0', '0', '0', '0', 'EXTENSION', 'fire-fas', '0', 935001, 930100, 200, NULL, 'admin', now(), 'admin', now(), '0'),
  (961002, 'http://example.org/fire-ontology#triggersWorkOrder', 'triggersWorkOrder', 'triggersWorkOrder', '设备事件状态触发工单', NULL, '0', '0', '0', '0', 'EXTENSION', 'fire-fas', '0', 935001, 930100, 201, NULL, 'admin', now(), 'admin', now(), '0'),
  (961003, 'http://example.org/fire-ontology#dispatchesOrder', 'dispatchesOrder', 'dispatchesOrder', '服务申请工单派单生成服务工单', NULL, '0', '0', '0', '0', 'EXTENSION', 'fire-fas', '0', 935001, 930100, 202, NULL, 'admin', now(), 'admin', now(), '0'),
  (961004, 'http://example.org/fire-ontology#triggersSiteCheck', 'triggersSiteCheck', 'triggersSiteCheck', '火警事件触发火警现场复核工单', NULL, '0', '0', '0', '0', 'EXTENSION', 'fire-fas', '0', 935001, 930100, 203, NULL, 'admin', now(), 'admin', now(), '0'),
  (961005, 'http://example.org/fire-ontology#triggersDutyHandle', 'triggersDutyHandle', 'triggersDutyHandle', '火警事件触发火警值班处置工单', NULL, '0', '0', '0', '0', 'EXTENSION', 'fire-fas', '0', 935001, 930100, 204, NULL, 'admin', now(), 'admin', now(), '0'),
  (961006, 'http://example.org/fire-ontology#siteCheckFeedsDuty', 'siteCheckFeedsDuty', 'siteCheckFeedsDuty', '现场复核工单反馈结果给值班处置工单', NULL, '0', '0', '0', '0', 'EXTENSION', 'fire-fas', '0', 935001, 930100, 205, NULL, 'admin', now(), 'admin', now(), '0'),
  (961007, 'http://example.org/fire-ontology#assignedWorker', 'assignedWorker', 'assignedWorker', '服务工单指派维修工人', NULL, '0', '0', '0', '0', 'EXTENSION', 'fire-fas', '0', 935001, 930100, 206, NULL, 'admin', now(), 'admin', now(), '0'),
  (961008, 'http://example.org/fire-ontology#maintainedBy', 'maintainedBy', 'maintainedBy', '维保记录关联维保设备', NULL, '0', '0', '0', '0', 'EXTENSION', 'fire-fas', '0', 935001, 930100, 207, NULL, 'admin', now(), 'admin', now(), '0'),
  (961009, 'http://example.org/fire-ontology#belongsToDutyRoom', 'belongsToDutyRoom', 'belongsToDutyRoom', '火警值班处置工单归属消防值班室', NULL, '0', '0', '0', '0', 'EXTENSION', 'fire-fas', '0', 935001, 930100, 208, NULL, 'admin', now(), 'admin', now(), '0'),
  (961010, 'http://example.org/fire-ontology#deviceInAlarmEvent', 'deviceInAlarmEvent', 'deviceInAlarmEvent', '火警事件关联触发设备', NULL, '0', '0', '0', '0', 'EXTENSION', 'fire-fas', '0', 935001, 930100, 209, NULL, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 11. 对象属性中文标签（10 条）
-- ----------------------------
INSERT INTO ont_object_property_label (object_property_id, locale, label) VALUES
  (961001, 'zh', '上报事件状态'), (961002, 'zh', '触发工单'), (961003, 'zh', '派单生成'),
  (961004, 'zh', '触发现场复核'), (961005, 'zh', '触发值班处置'), (961006, 'zh', '现场复核反馈值班'),
  (961007, 'zh', '指派维修工人'), (961008, 'zh', '维保关联设备'), (961009, 'zh', '隶属值班室'),
  (961010, 'zh', '火警事件关联设备')
ON CONFLICT (object_property_id, locale) DO NOTHING;

-- ----------------------------
-- 12. 对象属性定义域关联（10 条）
-- ----------------------------
INSERT INTO ont_object_property_domain (object_property_id, entity_type_id, sort_order) VALUES
  (961001, 941001, 1),  -- FireProtectionDevice → DeviceEventState
  (961002, 941005, 1),  -- DeviceEventState → FireWorkOrder
  (961003, 941007, 1),  -- ServiceRequestOrder → ServiceWorkOrder
  (961004, 941012, 1),  -- FireAlarmEvent → FireAlarmSiteCheckOrder
  (961005, 941012, 1),  -- FireAlarmEvent → FireAlarmDutyHandleOrder
  (961006, 941009, 1),  -- FireAlarmSiteCheckOrder → FireAlarmDutyHandleOrder
  (961007, 941008, 1),  -- ServiceWorkOrder → MaintenanceWorker
  (961008, 941011, 1),  -- MaintenanceRecord → FireProtectionDevice
  (961009, 941010, 1),  -- FireAlarmDutyHandleOrder → FireDutyRoom
  (961010, 941012, 1)   -- FireAlarmEvent → FireProtectionDevice
ON CONFLICT (object_property_id, entity_type_id) DO NOTHING;

-- ----------------------------
-- 13. 对象属性值域关联（10 条）
-- ----------------------------
INSERT INTO ont_object_property_range (object_property_id, entity_type_id, sort_order) VALUES
  (961001, 941005, 1),  -- reportsEventState → DeviceEventState
  (961002, 941006, 1),  -- triggersWorkOrder → FireWorkOrder
  (961003, 941008, 1),  -- dispatchesOrder → ServiceWorkOrder
  (961004, 941009, 1),  -- triggersSiteCheck → FireAlarmSiteCheckOrder
  (961005, 941010, 1),  -- triggersDutyHandle → FireAlarmDutyHandleOrder
  (961006, 941010, 1),  -- siteCheckFeedsDuty → FireAlarmDutyHandleOrder
  (961007, 941014, 1),  -- assignedWorker → MaintenanceWorker
  (961008, 941001, 1),  -- maintainedBy → FireProtectionDevice
  (961009, 941013, 1),  -- belongsToDutyRoom → FireDutyRoom
  (961010, 941001, 1)   -- deviceInAlarmEvent → FireProtectionDevice
ON CONFLICT (object_property_id, entity_type_id) DO NOTHING;

-- ----------------------------
-- 14. 扩展公理规则（3 条，id 971001~971003）
-- source_type='EXTENSION', is_builtin='0', status='DRAFT', is_enabled='0'
-- formalization_mode='CUSTOM_DRAFT', validation_mode='APPLICATION', severity='VIOLATION'
-- ----------------------------
INSERT INTO ont_axiom_rule (id, rule_code, name, category, sub_type, description, template_code, template_version, formalization_mode, validation_mode, executor_code, config_json, owl_axiom, shacl_shape, status, is_enabled, severity, source_type, source_reference, blocked_reason, is_builtin, ontology_id, sort_order, remarks, create_by, create_time, update_by, update_time, del_flag) VALUES
  (971001, 'FIRE_WO_STATUS_FLOW', '消防工单状态流转约束', 'RELATION', 'STATUS_FLOW',
   '服务申请工单状态流转须为：待派单→已完成（派单后生成服务工单）；服务工单状态流转须为：待接单→处理中→已完成；火警现场复核/值班处置工单状态流转须为：待接单→处理中→已完成',
   'STATUS_FLOW', 1, 'CUSTOM_DRAFT', 'APPLICATION', 'STATUS_FLOW',
   '{"domainProperty": "workOrderStatus", "validTransitions": {"服务申请": ["待派单", "已完成"], "服务": ["待接单", "处理中", "已完成"], "火警现场复核": ["待接单", "处理中", "已完成"], "火警值班处置": ["待接单", "处理中", "已完成"]}}'::jsonb,
   NULL, NULL, 'DRAFT', '0', 'VIOLATION', 'EXTENSION', 'fire-fas', NULL, '0', 935001, 200, NULL, 'admin', now(), 'admin', now(), '0'),
  (971002, 'FIRE_MAINT_SEQ', '维保事件状态顺序约束', 'RELATION', 'SEQUENCE',
   '火警探测设备维保：必须在维保时间段内事件状态按顺序出现过"正常（初始状态）→火警→正常"方为合格，否则为不合格',
   'SEQUENCE', 1, 'CUSTOM_DRAFT', 'APPLICATION', 'SEQUENCE',
   '{"domainProperty": "eventStateValue", "requiredSequence": ["正常", "火警", "正常"], "resultProperty": "maintenanceResult", "passValue": "合格", "failValue": "不合格"}'::jsonb,
   NULL, NULL, 'DRAFT', '0', 'VIOLATION', 'EXTENSION', 'fire-fas', NULL, '0', 935001, 201, NULL, 'admin', now(), 'admin', now(), '0'),
  (971003, 'FIRE_ALARM_DUTY_DEPENDENCY', '值班处置依赖现场复核结果', 'RELATION', 'DEPENDENCY',
   '火警值班处置工单须依赖现场复核工单的反馈结果，即值班处置工单的火警结论须基于现场复核结果填写，现场复核结果为"确认火警"时火警结论不得为"误报"',
   'DEPENDENCY', 1, 'CUSTOM_DRAFT', 'APPLICATION', 'DEPENDENCY',
   '{"sourceProperty": "siteCheckResult", "targetProperty": "alarmConclusion", "dependencyRules": {"确认火警": {"forbidden": ["误报"]}, "误报": {"required": ["误报", "设备故障"]}, "无法确认": {"forbidden": ["真实火警"]}}}'::jsonb,
   NULL, NULL, 'DRAFT', '0', 'VIOLATION', 'EXTENSION', 'fire-fas', NULL, '0', 935001, 202, NULL, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 15. 种子实例（11 条，id 981001~981011）
-- source_type='EXTENSION', is_builtin='0', declaration_mode='EXPLICIT'
-- 命名空间 930100(fire)、本体工程 935001(core)
-- 场景：3号楼2层走廊感烟探测器触发火警事件，经现场复核和值班处置，及设备故障维修
-- ----------------------------
INSERT INTO ont_entity_instance (id, iri, iri_local_name, rdf_type_id, label, namespace_id, ontology_id, source_type, source_reference, declaration_mode, is_builtin, sort_order, remarks, create_by, create_time, update_by, update_time, del_flag) VALUES
  -- 1. 感烟探测器实例
  (981001, 'http://example.org/fire-ontology#SmokeDetector_3B2F_Corridor', 'SmokeDetector_3B2F_Corridor', 941002, '3号楼2层走廊感烟探测器', 930100, 935001, 'EXTENSION', 'fire-fas-seed', 'EXPLICIT', '0', 10, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 2. 事件状态-火警
  (981002, 'http://example.org/fire-ontology#EventState_FireAlarm_001', 'EventState_FireAlarm_001', 941005, '事件状态-火警', 930100, 935001, 'EXTENSION', 'fire-fas-seed', 'EXPLICIT', '0', 20, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 3. 事件状态-正常（维保时）
  (981003, 'http://example.org/fire-ontology#EventState_Normal_001', 'EventState_Normal_001', 941005, '事件状态-正常（维保）', 930100, 935001, 'EXTENSION', 'fire-fas-seed', 'EXPLICIT', '0', 21, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 4. 火警事件
  (981004, 'http://example.org/fire-ontology#FireAlarmEvent_001', 'FireAlarmEvent_001', 941012, '火警事件-001', 930100, 935001, 'EXTENSION', 'fire-fas-seed', 'EXPLICIT', '0', 30, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 5. 火警现场复核工单
  (981005, 'http://example.org/fire-ontology#SiteCheckOrder_001', 'SiteCheckOrder_001', 941009, '火警现场复核工单-001', 930100, 935001, 'EXTENSION', 'fire-fas-seed', 'EXPLICIT', '0', 40, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 6. 火警值班处置工单
  (981006, 'http://example.org/fire-ontology#DutyHandleOrder_001', 'DutyHandleOrder_001', 941010, '火警值班处置工单-001', 930100, 935001, 'EXTENSION', 'fire-fas-seed', 'EXPLICIT', '0', 50, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 7. 服务申请工单（设备故障）
  (981007, 'http://example.org/fire-ontology#ServiceRequestOrder_001', 'ServiceRequestOrder_001', 941007, '服务申请工单-001', 930100, 935001, 'EXTENSION', 'fire-fas-seed', 'EXPLICIT', '0', 60, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 8. 服务工单（维修）
  (981008, 'http://example.org/fire-ontology#ServiceWorkOrder_001', 'ServiceWorkOrder_001', 941008, '服务工单-001', 930100, 935001, 'EXTENSION', 'fire-fas-seed', 'EXPLICIT', '0', 70, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 9. 维保记录
  (981009, 'http://example.org/fire-ontology#MaintenanceRecord_001', 'MaintenanceRecord_001', 941011, '维保记录-001', 930100, 935001, 'EXTENSION', 'fire-fas-seed', 'EXPLICIT', '0', 80, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 10. 维修工人
  (981010, 'http://example.org/fire-ontology#Worker_ZhangSan', 'Worker_ZhangSan', 941014, '维修工人-张三', 930100, 935001, 'EXTENSION', 'fire-fas-seed', 'EXPLICIT', '0', 90, NULL, 'admin', now(), 'admin', now(), '0'),
  -- 11. 消防值班室
  (981011, 'http://example.org/fire-ontology#DutyRoom_A', 'DutyRoom_A', 941013, '消防值班室-A', 930100, 935001, 'EXTENSION', 'fire-fas-seed', 'EXPLICIT', '0', 100, NULL, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 16. 实例数据属性值（29 条）
-- ----------------------------

-- 981001 感烟探测器（3 条：deviceCode/deviceLocation/deviceStatus）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98100101, 981001, 953001, 'SD-3B2F-C01', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98100102, 981001, 953002, '3号楼2层走廊东侧', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98100103, 981001, 953003, '在线', 'STRING', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 981002 事件状态-火警（2 条：eventStateValue/eventTimestamp）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98100201, 981002, 953004, '火警', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98100202, 981002, 953005, '2026-07-13 14:30:00', 'DATE', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 981003 事件状态-正常（维保）（2 条：eventStateValue/eventTimestamp）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98100301, 981003, 953004, '正常', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98100302, 981003, 953005, '2026-07-13 10:00:00', 'DATE', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 981004 火警事件（1 条：alarmEventCode）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98100401, 981004, 953014, 'FA-20260713-001', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 981005 火警现场复核工单（4 条：workOrderCode/workOrderStatus/workOrderType/workOrderDesc + siteCheckResult）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98100501, 981005, 953006, 'WO-SC-20260713-001', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98100502, 981005, 953007, '已完成', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98100503, 981005, 953008, '火警现场复核', 'STRING', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  (98100504, 981005, 953011, '确认火警', 'STRING', NULL, NULL, 4, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 981006 火警值班处置工单（5 条：workOrderCode/workOrderStatus/workOrderType + alarmConclusion + workOrderDesc）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98100601, 981006, 953006, 'WO-DH-20260713-001', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98100602, 981006, 953007, '已完成', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98100603, 981006, 953008, '火警值班处置', 'STRING', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  (98100604, 981006, 953010, '真实火警', 'STRING', NULL, NULL, 4, 'admin', now(), 'admin', now(), '0'),
  (98100605, 981006, 953009, '现场复核确认火警，值班室提报真实火警结论', 'STRING', NULL, NULL, 5, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 981007 服务申请工单（4 条：workOrderCode/workOrderStatus/workOrderType/workOrderDesc）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98100701, 981007, 953006, 'WO-SR-20260713-001', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98100702, 981007, 953007, '已完成', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98100703, 981007, 953008, '服务申请', 'STRING', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  (98100704, 981007, 953009, '感烟探测器故障，申请维修', 'STRING', NULL, NULL, 4, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 981008 服务工单（4 条：workOrderCode/workOrderStatus/workOrderType/workOrderDesc）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98100801, 981008, 953006, 'WO-SV-20260713-001', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98100802, 981008, 953007, '已完成', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  (98100803, 981008, 953008, '服务', 'STRING', NULL, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  (98100804, 981008, 953009, '更换感烟探测器探测模块', 'STRING', NULL, NULL, 4, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 981009 维保记录（2 条：maintenanceResult/maintenancePeriod）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98100901, 981009, 953012, '合格', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  (98100902, 981009, 953013, '2026-07-01~2026-07-13', 'STRING', NULL, NULL, 2, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 981010 维修工人（1 条：workerName）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98101001, 981010, 953015, '张三', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 981011 消防值班室（1 条：dutyRoomName）
INSERT INTO ont_instance_data_value (id, instance_id, data_property_id, literal_value, literal_type, unit_id, literal_symbol, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  (98101101, 981011, 953016, 'A栋消防值班室', 'STRING', NULL, NULL, 1, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 17. 实例对象属性断言（13 条，id 982001~982013）
-- 串联完整业务链路：设备→事件状态→工单→角色
-- ----------------------------
INSERT INTO ont_instance_object_relation (id, subject_instance_id, object_property_id, object_kind, object_instance_id, object_entity_type_id, sort_order, create_by, create_time, update_by, update_time, del_flag) VALUES
  -- 961001 reportsEventState: 感烟探测器→事件状态-火警
  (982001, 981001, 961001, 'INSTANCE', 981002, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  -- 961001 reportsEventState: 感烟探测器→事件状态-正常（维保）
  (982002, 981001, 961001, 'INSTANCE', 981003, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  -- 961010 deviceInAlarmEvent: 火警事件→感烟探测器
  (982003, 981004, 961010, 'INSTANCE', 981001, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  -- 961002 triggersWorkOrder: 事件状态-火警→火警现场复核工单
  (982004, 981002, 961002, 'INSTANCE', 981005, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  -- 961002 triggersWorkOrder: 事件状态-火警→火警值班处置工单
  (982005, 981002, 961002, 'INSTANCE', 981006, NULL, 2, 'admin', now(), 'admin', now(), '0'),
  -- 961002 triggersWorkOrder: 事件状态-火警→服务申请工单（设备故障）
  (982006, 981002, 961002, 'INSTANCE', 981007, NULL, 3, 'admin', now(), 'admin', now(), '0'),
  -- 961006 siteCheckFeedsDuty: 现场复核工单→值班处置工单
  (982007, 981005, 961006, 'INSTANCE', 981006, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  -- 961003 dispatchesOrder: 服务申请工单→服务工单
  (982008, 981007, 961003, 'INSTANCE', 981008, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  -- 961007 assignedWorker: 服务工单→维修工人
  (982009, 981008, 961007, 'INSTANCE', 981010, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  -- 961009 belongsToDutyRoom: 值班处置工单→消防值班室
  (982010, 981006, 961009, 'INSTANCE', 981011, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  -- 961008 maintainedBy: 维保记录→感烟探测器
  (982011, 981009, 961008, 'INSTANCE', 981001, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  -- 961001 reportsEventState: 维保记录关联的事件状态-正常（维保时上报）
  -- 注：维保记录→事件状态通过 maintainedBy→设备→reportsEventState 间接关联，此处不再直接断言
  -- 961002 triggersWorkOrder: 事件状态-火警→火警事件（通过 deviceInAlarmEvent 逆推，此处直接断言火警事件关联）
  -- 961004 triggersSiteCheck: 火警事件→现场复核工单
  (982012, 981004, 961004, 'INSTANCE', 981005, NULL, 1, 'admin', now(), 'admin', now(), '0'),
  -- 961005 triggersDutyHandle: 火警事件→值班处置工单
  (982013, 981004, 961005, 'INSTANCE', 981006, NULL, 1, 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 18. 扩展资源关联（ont_extension_resource）
-- 将所有扩展资源注册到 fire-protection 模块（id=939001）
-- ----------------------------
-- 实体类型（14 条）
INSERT INTO ont_extension_resource (id, module_id, resource_type, resource_id, resource_iri, resource_name, create_by, create_time, update_by, update_time, del_flag) VALUES
  (983001, 939001, 'ENTITY_TYPE', 941001, 'http://example.org/fire-ontology#FireProtectionDevice', 'FireProtectionDevice', 'admin', now(), 'admin', now(), '0'),
  (983002, 939001, 'ENTITY_TYPE', 941002, 'http://example.org/fire-ontology#SmokeDetector', 'SmokeDetector', 'admin', now(), 'admin', now(), '0'),
  (983003, 939001, 'ENTITY_TYPE', 941003, 'http://example.org/fire-ontology#HeatDetector', 'HeatDetector', 'admin', now(), 'admin', now(), '0'),
  (983004, 939001, 'ENTITY_TYPE', 941004, 'http://example.org/fire-ontology#ManualCallPoint', 'ManualCallPoint', 'admin', now(), 'admin', now(), '0'),
  (983005, 939001, 'ENTITY_TYPE', 941005, 'http://example.org/fire-ontology#DeviceEventState', 'DeviceEventState', 'admin', now(), 'admin', now(), '0'),
  (983006, 939001, 'ENTITY_TYPE', 941006, 'http://example.org/fire-ontology#FireWorkOrder', 'FireWorkOrder', 'admin', now(), 'admin', now(), '0'),
  (983007, 939001, 'ENTITY_TYPE', 941007, 'http://example.org/fire-ontology#ServiceRequestOrder', 'ServiceRequestOrder', 'admin', now(), 'admin', now(), '0'),
  (983008, 939001, 'ENTITY_TYPE', 941008, 'http://example.org/fire-ontology#ServiceWorkOrder', 'ServiceWorkOrder', 'admin', now(), 'admin', now(), '0'),
  (983009, 939001, 'ENTITY_TYPE', 941009, 'http://example.org/fire-ontology#FireAlarmSiteCheckOrder', 'FireAlarmSiteCheckOrder', 'admin', now(), 'admin', now(), '0'),
  (983010, 939001, 'ENTITY_TYPE', 941010, 'http://example.org/fire-ontology#FireAlarmDutyHandleOrder', 'FireAlarmDutyHandleOrder', 'admin', now(), 'admin', now(), '0'),
  (983011, 939001, 'ENTITY_TYPE', 941011, 'http://example.org/fire-ontology#MaintenanceRecord', 'MaintenanceRecord', 'admin', now(), 'admin', now(), '0'),
  (983012, 939001, 'ENTITY_TYPE', 941012, 'http://example.org/fire-ontology#FireAlarmEvent', 'FireAlarmEvent', 'admin', now(), 'admin', now(), '0'),
  (983013, 939001, 'ENTITY_TYPE', 941013, 'http://example.org/fire-ontology#FireDutyRoom', 'FireDutyRoom', 'admin', now(), 'admin', now(), '0'),
  (983014, 939001, 'ENTITY_TYPE', 941014, 'http://example.org/fire-ontology#MaintenanceWorker', 'MaintenanceWorker', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 数据属性（16 条）
INSERT INTO ont_extension_resource (id, module_id, resource_type, resource_id, resource_iri, resource_name, create_by, create_time, update_by, update_time, del_flag) VALUES
  (983015, 939001, 'DATA_PROPERTY', 953001, 'http://example.org/fire-ontology#deviceCode', 'deviceCode', 'admin', now(), 'admin', now(), '0'),
  (983016, 939001, 'DATA_PROPERTY', 953002, 'http://example.org/fire-ontology#deviceLocation', 'deviceLocation', 'admin', now(), 'admin', now(), '0'),
  (983017, 939001, 'DATA_PROPERTY', 953003, 'http://example.org/fire-ontology#deviceStatus', 'deviceStatus', 'admin', now(), 'admin', now(), '0'),
  (983018, 939001, 'DATA_PROPERTY', 953004, 'http://example.org/fire-ontology#eventStateValue', 'eventStateValue', 'admin', now(), 'admin', now(), '0'),
  (983019, 939001, 'DATA_PROPERTY', 953005, 'http://example.org/fire-ontology#eventTimestamp', 'eventTimestamp', 'admin', now(), 'admin', now(), '0'),
  (983020, 939001, 'DATA_PROPERTY', 953006, 'http://example.org/fire-ontology#workOrderCode', 'workOrderCode', 'admin', now(), 'admin', now(), '0'),
  (983021, 939001, 'DATA_PROPERTY', 953007, 'http://example.org/fire-ontology#workOrderStatus', 'workOrderStatus', 'admin', now(), 'admin', now(), '0'),
  (983022, 939001, 'DATA_PROPERTY', 953008, 'http://example.org/fire-ontology#workOrderType', 'workOrderType', 'admin', now(), 'admin', now(), '0'),
  (983023, 939001, 'DATA_PROPERTY', 953009, 'http://example.org/fire-ontology#workOrderDesc', 'workOrderDesc', 'admin', now(), 'admin', now(), '0'),
  (983024, 939001, 'DATA_PROPERTY', 953010, 'http://example.org/fire-ontology#alarmConclusion', 'alarmConclusion', 'admin', now(), 'admin', now(), '0'),
  (983025, 939001, 'DATA_PROPERTY', 953011, 'http://example.org/fire-ontology#siteCheckResult', 'siteCheckResult', 'admin', now(), 'admin', now(), '0'),
  (983026, 939001, 'DATA_PROPERTY', 953012, 'http://example.org/fire-ontology#maintenanceResult', 'maintenanceResult', 'admin', now(), 'admin', now(), '0'),
  (983027, 939001, 'DATA_PROPERTY', 953013, 'http://example.org/fire-ontology#maintenancePeriod', 'maintenancePeriod', 'admin', now(), 'admin', now(), '0'),
  (983028, 939001, 'DATA_PROPERTY', 953014, 'http://example.org/fire-ontology#alarmEventCode', 'alarmEventCode', 'admin', now(), 'admin', now(), '0'),
  (983029, 939001, 'DATA_PROPERTY', 953015, 'http://example.org/fire-ontology#workerName', 'workerName', 'admin', now(), 'admin', now(), '0'),
  (983030, 939001, 'DATA_PROPERTY', 953016, 'http://example.org/fire-ontology#dutyRoomName', 'dutyRoomName', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 对象属性（10 条）
INSERT INTO ont_extension_resource (id, module_id, resource_type, resource_id, resource_iri, resource_name, create_by, create_time, update_by, update_time, del_flag) VALUES
  (983031, 939001, 'OBJECT_PROPERTY', 961001, 'http://example.org/fire-ontology#reportsEventState', 'reportsEventState', 'admin', now(), 'admin', now(), '0'),
  (983032, 939001, 'OBJECT_PROPERTY', 961002, 'http://example.org/fire-ontology#triggersWorkOrder', 'triggersWorkOrder', 'admin', now(), 'admin', now(), '0'),
  (983033, 939001, 'OBJECT_PROPERTY', 961003, 'http://example.org/fire-ontology#dispatchesOrder', 'dispatchesOrder', 'admin', now(), 'admin', now(), '0'),
  (983034, 939001, 'OBJECT_PROPERTY', 961004, 'http://example.org/fire-ontology#triggersSiteCheck', 'triggersSiteCheck', 'admin', now(), 'admin', now(), '0'),
  (983035, 939001, 'OBJECT_PROPERTY', 961005, 'http://example.org/fire-ontology#triggersDutyHandle', 'triggersDutyHandle', 'admin', now(), 'admin', now(), '0'),
  (983036, 939001, 'OBJECT_PROPERTY', 961006, 'http://example.org/fire-ontology#siteCheckFeedsDuty', 'siteCheckFeedsDuty', 'admin', now(), 'admin', now(), '0'),
  (983037, 939001, 'OBJECT_PROPERTY', 961007, 'http://example.org/fire-ontology#assignedWorker', 'assignedWorker', 'admin', now(), 'admin', now(), '0'),
  (983038, 939001, 'OBJECT_PROPERTY', 961008, 'http://example.org/fire-ontology#maintainedBy', 'maintainedBy', 'admin', now(), 'admin', now(), '0'),
  (983039, 939001, 'OBJECT_PROPERTY', 961009, 'http://example.org/fire-ontology#belongsToDutyRoom', 'belongsToDutyRoom', 'admin', now(), 'admin', now(), '0'),
  (983040, 939001, 'OBJECT_PROPERTY', 961010, 'http://example.org/fire-ontology#deviceInAlarmEvent', 'deviceInAlarmEvent', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- 公理规则（3 条）
INSERT INTO ont_extension_resource (id, module_id, resource_type, resource_id, resource_iri, resource_name, create_by, create_time, update_by, update_time, del_flag) VALUES
  (983041, 939001, 'AXIOM_RULE', 971001, NULL, 'FIRE_WO_STATUS_FLOW', 'admin', now(), 'admin', now(), '0'),
  (983042, 939001, 'AXIOM_RULE', 971002, NULL, 'FIRE_MAINT_SEQ', 'admin', now(), 'admin', now(), '0'),
  (983043, 939001, 'AXIOM_RULE', 971003, NULL, 'FIRE_ALARM_DUTY_DEPENDENCY', 'admin', now(), 'admin', now(), '0')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 19. 迁移完整性断言
-- ----------------------------
DO $$
BEGIN
  -- 1. 扩展命名空间存在
  IF NOT EXISTS (SELECT 1 FROM ont_namespace WHERE id = 930100 AND prefix = 'fire'
      AND is_builtin = '0' AND del_flag = '0') THEN
    RAISE EXCEPTION '消防扩展命名空间(930100)不存在或内容不正确';
  END IF;

  -- 2. 扩展模块存在
  IF NOT EXISTS (SELECT 1 FROM ont_extension_module WHERE id = 939001 AND module_code = 'fire-protection'
      AND namespace_id = 930100 AND ontology_id = 935001 AND is_builtin = '0' AND del_flag = '0') THEN
    RAISE EXCEPTION '消防扩展模块(939001)不存在或内容不正确';
  END IF;

  -- 3. 扩展实体类型 14 条
  IF (SELECT count(*) FROM ont_entity_type WHERE namespace_id = 930100 AND is_builtin = '0' AND del_flag = '0') <> 14 THEN
    RAISE EXCEPTION '消防扩展实体类型数量不是14条';
  END IF;
  -- 中文标签 14 条
  IF (SELECT count(*) FROM ont_entity_type_label WHERE entity_type_id BETWEEN 941001 AND 941014 AND locale = 'zh') <> 14 THEN
    RAISE EXCEPTION '消防扩展实体类型中文标签数量不是14条';
  END IF;
  -- 继承关系 14 条
  IF (SELECT count(*) FROM ont_entity_type_hierarchy WHERE child_id BETWEEN 941001 AND 941014) <> 14 THEN
    RAISE EXCEPTION '消防扩展继承关系数量不是14条';
  END IF;

  -- 4. 扩展数据属性 16 条
  IF (SELECT count(*) FROM ont_data_property WHERE namespace_id = 930100 AND is_builtin = '0' AND del_flag = '0') <> 16 THEN
    RAISE EXCEPTION '消防扩展数据属性数量不是16条';
  END IF;
  -- 中文标签 16 条
  IF (SELECT count(*) FROM ont_data_property_label WHERE data_property_id BETWEEN 953001 AND 953016 AND locale = 'zh') <> 16 THEN
    RAISE EXCEPTION '消防扩展数据属性中文标签数量不是16条';
  END IF;
  -- 枚举值 23 条（设备状态2+事件状态值4+工单状态5+工单类型4+火警结论3+现场复核结果3+维保结果2）
  IF (SELECT count(*) FROM ont_data_property_enum WHERE data_property_id BETWEEN 953001 AND 953016) <> 23 THEN
    RAISE EXCEPTION '消防扩展数据属性枚举值数量不是23条';
  END IF;

  -- 5. 扩展对象属性 10 条
  IF (SELECT count(*) FROM ont_object_property WHERE namespace_id = 930100 AND is_builtin = '0' AND del_flag = '0') <> 10 THEN
    RAISE EXCEPTION '消防扩展对象属性数量不是10条';
  END IF;
  -- 中文标签 10 条
  IF (SELECT count(*) FROM ont_object_property_label WHERE object_property_id BETWEEN 961001 AND 961010 AND locale = 'zh') <> 10 THEN
    RAISE EXCEPTION '消防扩展对象属性中文标签数量不是10条';
  END IF;
  -- 定义域 10 条
  IF (SELECT count(*) FROM ont_object_property_domain WHERE object_property_id BETWEEN 961001 AND 961010) <> 10 THEN
    RAISE EXCEPTION '消防扩展对象属性定义域数量不是10条';
  END IF;
  -- 值域 10 条
  IF (SELECT count(*) FROM ont_object_property_range WHERE object_property_id BETWEEN 961001 AND 961010) <> 10 THEN
    RAISE EXCEPTION '消防扩展对象属性值域数量不是10条';
  END IF;

  -- 6. 扩展公理规则 3 条，全部 DRAFT/is_enabled=0
  IF (SELECT count(*) FROM ont_axiom_rule WHERE ontology_id = 935001 AND source_type = 'EXTENSION' AND is_builtin = '0' AND del_flag = '0') <> 3 THEN
    RAISE EXCEPTION '消防扩展公理规则数量不是3条';
  END IF;
  IF EXISTS (SELECT 1 FROM ont_axiom_rule WHERE id BETWEEN 971001 AND 971003 AND (status <> 'DRAFT' OR is_enabled = '1')) THEN
    RAISE EXCEPTION '消防扩展公理规则必须为DRAFT且is_enabled=0';
  END IF;

  -- 7. 种子实例 11 条
  IF (SELECT count(*) FROM ont_entity_instance WHERE namespace_id = 930100 AND source_type = 'EXTENSION' AND is_builtin = '0' AND del_flag = '0') <> 11 THEN
    RAISE EXCEPTION '消防种子实例数量不是11条';
  END IF;
  -- 实例数据值 29 条
  IF (SELECT count(*) FROM ont_instance_data_value WHERE instance_id BETWEEN 981001 AND 981011 AND del_flag = '0') <> 29 THEN
    RAISE EXCEPTION '消防实例数据属性值数量不是29条';
  END IF;
  -- 实例对象断言 13 条
  IF (SELECT count(*) FROM ont_instance_object_relation WHERE subject_instance_id BETWEEN 981001 AND 981011 AND del_flag = '0') <> 13 THEN
    RAISE EXCEPTION '消防实例对象断言数量不是13条';
  END IF;

  -- 8. 扩展资源关联 43 条（14+16+10+3）
  IF (SELECT count(*) FROM ont_extension_resource WHERE module_id = 939001 AND del_flag = '0') <> 43 THEN
    RAISE EXCEPTION '消防扩展资源关联数量不是43条';
  END IF;

  -- 9. IRI 唯一性校验：扩展 IRI 不与核心 IRI 冲突
  IF EXISTS (
    SELECT iri FROM ont_entity_type WHERE del_flag = '0' GROUP BY iri HAVING count(*) > 1
  ) THEN
    RAISE EXCEPTION '实体类型IRI存在重复';
  END IF;
  IF EXISTS (
    SELECT iri FROM ont_data_property WHERE del_flag = '0' GROUP BY iri HAVING count(*) > 1
  ) THEN
    RAISE EXCEPTION '数据属性IRI存在重复';
  END IF;
  IF EXISTS (
    SELECT iri FROM ont_object_property WHERE del_flag = '0' GROUP BY iri HAVING count(*) > 1
  ) THEN
    RAISE EXCEPTION '对象属性IRI存在重复';
  END IF;

  -- 10. 所有扩展实体类型 IRI = 命名空间 URI + iri_local_name
  IF EXISTS (
    SELECT 1 FROM ont_entity_type et
    JOIN ont_namespace ns ON et.namespace_id = ns.id
    WHERE et.namespace_id = 930100 AND et.del_flag = '0' AND et.iri <> ns.uri || et.name
  ) THEN
    RAISE EXCEPTION '消防扩展实体类型IRI不等于命名空间URI加name';
  END IF;

  -- 11. 扩展实体类型不引用抽象类型作为实例的 rdf:type
  IF EXISTS (
    SELECT 1 FROM ont_entity_instance i
    JOIN ont_entity_type t ON i.rdf_type_id = t.id
    WHERE i.namespace_id = 930100 AND i.del_flag = '0' AND t.is_abstract = '1'
  ) THEN
    RAISE EXCEPTION '消防种子实例引用了抽象实体类型';
  END IF;

  -- 12. 实例 IRI = 命名空间 URI + iri_local_name
  IF EXISTS (
    SELECT 1 FROM ont_entity_instance i
    JOIN ont_namespace n ON i.namespace_id = n.id
    WHERE i.namespace_id = 930100 AND i.del_flag = '0' AND i.iri <> (n.uri || i.iri_local_name)
  ) THEN
    RAISE EXCEPTION '消防实例IRI不等于命名空间URI与本地名的拼接';
  END IF;

  RAISE NOTICE 'V16 消防系统-火灾报警系统 ontology 扩展种子数据加载完成';
END $$;
