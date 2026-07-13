# 火灾报警系统 Ontology 建模设计文档

> 依据：《docs/ontology/需求与业务场景/消防系统/火灾报警系统业务场景.md》
> 本程序 ontology 建模体系：V4~V15 Flyway 迁移脚本（单位字典→命名空间→实体类型→数据属性→对象属性→公理规则→实例→校验→序列化→扩展→可视化）

## 1. 业务场景概述

火灾报警系统是消防系统的第一个子场景，核心业务包括：

### 1.1 设备探测

感烟探测器、感温探测器、手动报警按钮等火警探测设备，通过物联采集实时上报"事件状态"。事件状态枚举为：**火警、故障、屏蔽、正常**。

### 1.2 工单流转

| 工单类型 | 用途 |
|---------|------|
| 服务申请工单 | 设备故障/屏蔽后发起服务申请，可派单生成服务工单 |
| 服务工单 | 维修工人接单/被派单后执行维修、更换等 |
| 火警现场复核工单 | 感应设备触发火警后，派单给现场附近人员到现场确认火警真实性 |
| 火警值班处置工单 | 派单给值班室值班人员，根据现场反馈进行火警结论提报 |

### 1.3 设备与工单关联

设备故障、屏蔽、火警信息均由设备"事件状态"物联采集实时上报，触发对应工单。

### 1.4 维保质量

火警探测设备维保：必须在维保时间段内"事件状态"按顺序出现过"正常→火警→正常"方为合格，否则为不合格。

## 2. 建模方案

### 2.1 扩展方式

消防系统作为行业扩展，遵循 V13 扩展管理机制：

- 新建扩展命名空间 `fire`（`is_builtin='0'`）
- 新建扩展模块 `fire-protection`，归属核心工程 `935001`
- 所有扩展实体类型、数据属性、对象属性、公理规则均以 `source_type='EXTENSION', is_builtin='0'` 创建，归属 `ontology_id=935001`
- 扩展实例以 `source_type='EXTENSION', is_builtin='0'` 创建
- 所有扩展资源通过 `ont_extension_resource` 关联到扩展模块

可视化页面固定查询 `ontologyId=935001`，扩展数据归属核心工程后自动出现在 Schema 图谱和实例图谱中。

### 2.2 ID 分配规划

| 资源类型 | ID 区间 | 数量 |
|---------|---------|------|
| 命名空间 | 930100 | 1 |
| 扩展模块 | 939001 | 1 |
| 实体类型 | 941001~941014 | 14 |
| 数据属性 | 953001~953020 | ~20 |
| 对象属性 | 961001~961010 | ~10 |
| 公理规则 | 971001~971003 | 3 |
| 实体实例 | 981001~981020 | ~20 |
| 实例数据值 | 98100101~ | ~60 |
| 实例对象断言 | 982001~ | ~15 |
| 扩展资源关联 | 983001~ | ~48 |

> 所有 ID 避开已用区间（940xxx/950xxx/960xxx/970xxx/980xxx 为内置数据）。

### 2.3 实体类型设计（14 个）

消防系统实体类型继承自核心本体的 `Object`（940063）和 `ActionClass`（940068），形成两个分支：

```
Object (940063, 核心内置)
├── FireProtectionDevice (941001) 消防设备（抽象）
│   ├── SmokeDetector (941002) 感烟探测器
│   ├── HeatDetector (941003) 感温探测器
│   └── ManualCallPoint (941004) 手动报警按钮
├── DeviceEventState (941005) 设备事件状态
├── FireWorkOrder (941006) 消防工单（抽象）
│   ├── ServiceRequestOrder (941007) 服务申请工单
│   ├── ServiceWorkOrder (941008) 服务工单
│   ├── FireAlarmSiteCheckOrder (941009) 火警现场复核工单
│   └── FireAlarmDutyHandleOrder (941010) 火警值班处置工单
└── MaintenanceRecord (941011) 维保记录

ActionClass (940068, 核心内置)
└── FireAlarmEvent (941012) 火警事件

Stakeholder (940011, 核心内置)
├── Organization (940012)
│   └── FireDutyRoom (941013) 消防值班室
└── Individual (940021)
    └── MaintenanceWorker (941014) 维修工人
```

**继承关系（hierarchy）**：

| 父类 | 子类 | 说明 |
|------|------|------|
| Object(940063) | FireProtectionDevice(941001) | 设备继承自对象 |
| FireProtectionDevice(941001) | SmokeDetector(941002) | |
| FireProtectionDevice(941001) | HeatDetector(941003) | |
| FireProtectionDevice(941001) | ManualCallPoint(941004) | |
| Object(940063) | DeviceEventState(941005) | |
| Object(940063) | FireWorkOrder(941006) | |
| FireWorkOrder(941006) | ServiceRequestOrder(941007) | |
| FireWorkOrder(941006) | ServiceWorkOrder(941008) | |
| FireWorkOrder(941006) | FireAlarmSiteCheckOrder(941009) | |
| FireWorkOrder(941006) | FireAlarmDutyHandleOrder(941010) | |
| Object(940063) | MaintenanceRecord(941011) | |
| ActionClass(940068) | FireAlarmEvent(941012) | |
| Organization(940012) | FireDutyRoom(941013) | |
| Individual(940021) | MaintenanceWorker(941014) | |

**不相交关系**：FireProtectionDevice ⇄ FireWorkOrder（设备与工单互斥）

### 2.4 数据属性设计

| ID | IRI本地名 | 名称 | 定义域 | 类型 | 值模式 | 枚举值 |
|----|----------|------|--------|------|--------|--------|
| 953001 | deviceCode | 设备编码 | FireProtectionDevice | TEXT | FREE | |
| 953002 | deviceLocation | 设备位置 | FireProtectionDevice | TEXT | FREE | |
| 953003 | deviceStatus | 设备状态 | FireProtectionDevice | TEXT | CLOSED_ENUM | 在线/离线 |
| 953004 | eventStateValue | 事件状态值 | DeviceEventState | TEXT | CLOSED_ENUM | 正常/火警/故障/屏蔽 |
| 953005 | eventTimestamp | 事件时间戳 | DeviceEventState | DATE | FREE | |
| 953006 | workOrderCode | 工单编号 | FireWorkOrder | TEXT | FREE | |
| 953007 | workOrderStatus | 工单状态 | FireWorkOrder | TEXT | CLOSED_ENUM | 待派单/待接单/处理中/已完成/已取消 |
| 953008 | workOrderType | 工单类型 | FireWorkOrder | TEXT | CLOSED_ENUM | 服务申请/服务/火警现场复核/火警值班处置 |
| 953009 | workOrderDesc | 工单描述 | FireWorkOrder | TEXT | FREE | |
| 953010 | alarmConclusion | 火警结论 | FireAlarmDutyHandleOrder | TEXT | CLOSED_ENUM | 真实火警/误报/设备故障 |
| 953011 | siteCheckResult | 现场复核结果 | FireAlarmSiteCheckOrder | TEXT | CLOSED_ENUM | 确认火警/误报/无法确认 |
| 953012 | maintenanceResult | 维保结果 | MaintenanceRecord | TEXT | CLOSED_ENUM | 合格/不合格 |
| 953013 | maintenancePeriod | 维保时间段 | MaintenanceRecord | TEXT | FREE | |
| 953014 | alarmEventCode | 火警事件编码 | FireAlarmEvent | TEXT | FREE | |
| 953015 | workerName | 维修工人姓名 | MaintenanceWorker | TEXT | FREE | |
| 953016 | dutyRoomName | 值班室名称 | FireDutyRoom | TEXT | FREE | |

### 2.5 对象属性设计

| ID | IRI本地名 | 名称 | 定义域 | 值域 | 说明 |
|----|----------|------|--------|------|------|
| 961001 | reportsEventState | 上报事件状态 | FireProtectionDevice | DeviceEventState | 设备上报事件状态 |
| 961002 | triggersWorkOrder | 触发工单 | DeviceEventState | FireWorkOrder | 事件状态触发工单 |
| 961003 | dispatchesOrder | 派单生成 | ServiceRequestOrder | ServiceWorkOrder | 服务申请派单生成服务工单 |
| 961004 | checksOnSite | 现场复核 | FireAlarmEvent | FireAlarmSiteCheckOrder | 火警事件触发现场复核 |
| 961005 | handledByDuty | 值班处置 | FireAlarmEvent | FireAlarmDutyHandleOrder | 火警事件触发值班处置 |
| 961006 | siteCheck feedsDuty | 现场复核反馈值班 | FireAlarmSiteCheckOrder | FireAlarmDutyHandleOrder | 复核结果反馈给值班处置 |
| 961007 | assignedWorker | 指派维修工人 | ServiceWorkOrder | MaintenanceWorker | 工单指派给维修工人 |
| 961008 | maintainedBy | 维保关联设备 | MaintenanceRecord | FireProtectionDevice | 维保记录关联设备 |
| 961009 | belongsToDutyRoom | 隶属值班室 | FireAlarmDutyHandleOrder | FireDutyRoom | 处置工单归属值班室 |
| 961010 | deviceInAlarmEvent | 火警事件关联设备 | FireAlarmEvent | FireProtectionDevice | 火警事件涉及的设备 |

### 2.6 公理规则设计

| ID | 规则代码 | 名称 | 类别 | 说明 |
|----|---------|------|------|------|
| 971001 | FIRE_WO_STATUS_FLOW | 工单状态流转约束 | RELATION | 服务申请工单状态必须为"待派单→已派单"，服务工单必须为"待接单→处理中→已完成" |
| 971002 | FIRE_MAINT_SEQ | 维保事件状态顺序约束 | RELATION | 维保时间段内事件状态须按"正常→火警→正常"顺序出现方为合格 |
| 971003 | FIRE_ALARM_DUTY_DEPENDENCY | 值班处置依赖复核结果 | RELATION | 火警值班处置工单须依赖现场复核工单的反馈结果 |

> 公理规则 `source_type='EXTENSION'`，`formalization_mode='CUSTOM_DRAFT'`，`validation_mode='APPLICATION'`，`status='DRAFT'`，`is_enabled='0'`。

### 2.7 种子实例设计

以一个完整的火警事件场景为例，创建以下种子实例：

| 步骤 | 实例 | 类型 | 说明 |
|------|------|------|------|
| 1 | 走廊感烟探测器 | SmokeDetector | 3号楼2层走廊 |
| 2 | 状态-火警 | DeviceEventState | 该探测器上报的火警状态 |
| 3 | 状态-正常 | DeviceEventState | 维保时的正常状态 |
| 4 | 火警事件-001 | FireAlarmEvent | 触发的火警事件 |
| 5 | 现场复核工单 | FireAlarmSiteCheckOrder | 派单给现场人员 |
| 6 | 值班处置工单 | FireAlarmDutyHandleOrder | 值班室处置 |
| 7 | 服务申请工单 | ServiceRequestOrder | 设备故障发起 |
| 8 | 服务工单 | ServiceWorkOrder | 派单维修 |
| 9 | 维保记录-001 | MaintenanceRecord | 合格维保 |
| 10 | 维修工人-张三 | MaintenanceWorker | |
| 11 | 消防值班室-A | FireDutyRoom | |

实例间通过对象属性断言连接，形成完整业务图谱，可在可视化中查看。

## 3. 与核心本体的关系

消防系统扩展完全复用核心本体的建模框架：

- **命名空间**：独立 `fire` 命名空间，与 `std` 并列
- **本体工程**：归属核心工程 `935001`，确保可视化可见
- **继承体系**：消防实体类型继承自 `Object`/`ActionClass`/`Stakeholder` 等核心类，在 Schema 图谱中与核心类连通
- **扩展管理**：通过 `ont_extension_module` + `ont_extension_resource` 注册，可在扩展管理页面查看和导出

## 4. 后续扩展

消防系统后续还将扩展喷淋系统、消火栓系统等子场景，设计时预留：

- 命名空间 `fire` 覆盖所有消防子场景，子场景通过实体类型命名前缀区分（如 `SprinklerDevice`、`HydrantDevice`）
- 扩展模块 `fire-protection` 作为消防系统统一容器，新增子场景只需追加实体类型和实例
- 所有消防实体类型统一继承 `FireProtectionDevice`，在图谱中形成清晰的消防设备子树
