# 事件驱动骨干 EDA 模块详细设计

> 对应 PRD：§3.2（技术架构-接口层 WebSocket）、§8（关键业务流程-实例创建校验流程）
>
> 对应差距分析：§3.35 维度㉟ 事件驱动架构 EDA（🔴 缺失）、§5.2 C 运行时智能层-模块 26、§7.1 原则6 横切地基先行
>
> 前置模块：01-模块化架构、02-单位字典、03-命名空间与IRI、04-实体类型、05-数据属性、06-对象属性、07-公理与规则、08-实体对象实例、09-校验引擎、10-序列化与交换、11-扩展管理、12-可视化、13-SPARQL查询端点、14-本体版本演化、36-安全与合规
>
> 基础设施：pig-common-data `RedisTemplateConfiguration`（`RedisTemplate` 已配置）、`spring-boot-starter-data-redis`（已引入，支持 Redis Streams）、Redis 8.8.0（容器 `1Panel-redis-2G4M`，端口 6379，database 5）
>
> 设计日期：2026-07-13

---

## 1. 评审结论与设计边界

### 1.1 现状评审

截至本设计时点，前述 15 个模块（01-14 + 36）已全部完成设计。在事件驱动架构方面，代码库现状如下：

| 缺口 | 现状 | 影响 |
|------|------|------|
| **同步调用为主** | 模块 09（校验引擎）同步执行 SHACL 校验；模块 10（序列化）同步导出/导入；模块 13（SPARQL）同步查询；模块 14（版本演化）同步发布/回滚 | 所有模块间通信为同步 RPC 调用，无法解耦、无法削峰、无法回放；高耗时操作（全量校验、大文件导入）阻塞请求线程 |
| **Spring ApplicationEvent 是进程内事件** | 模块 13（SPARQL 缓存失效）和模块 14（版本变更通知）使用 Spring `ApplicationEvent`，仅在**同一 JVM 内**传播 | pig-boot 单体部署下可工作，但无法跨服务/跨进程传播；未来微服务化后事件丢失；无持久化、无重试、无消费确认 |
| **无事件总线** | Redis 已部署（容器 `1Panel-redis-2G4M`，8.8.0），但仅作缓存（OAuth2 Token、Session），未用作消息流（Redis Streams） | 差距分析 §3.35：IoT/告警/触发/CEP 本质都是事件驱动的，需要事件总线连接各模块；没有事件总线，模块间只能同步 RPC |
| **无事务发件箱模式** | 数据库写入与事件发布无一致性保证——如果 DB 事务提交后、事件发布前崩溃，事件丢失 | 差距分析 §3.35：事务发件箱模式（Transaction Outbox）保证 DB 写入与事件发布一致性 |
| **无 CDC（变更数据捕获）** | 实体实例/Schema 变更无变更事件自动捕获——模块 13/14 的缓存失效依赖各 Service 代码手动 `publishEvent`，容易遗漏 | 变更事件散落在各 Service 代码中，新增模块需手动接入，容易遗漏导致缓存不一致 |
| **无事件 schema 演化管理** | 事件格式无版本化定义，消费端与生产端无契约约束 | 事件格式变更后消费端可能解析失败；无 schema 注册与兼容性检查 |
| **无死信队列** | 消费失败的事件无重试与死信处理机制 | 消费失败的事件静默丢弃，无法追溯和重放 |
| **无事件回放** | 无法对历史事件进行回放（如调试时重放某次校验事件） | 排查问题需手动复现，无法从事件流回放 |

### 1.2 优化结论

| 级别 | 评审发现 | 优化结论 |
|------|----------|----------|
| P0 | 无事件总线，模块间同步耦合 | **以 Redis Streams 为事件总线**：Redis 已部署且 `spring-boot-starter-data-redis` 已引入，`StreamOperations` 原生支持消费者组/消费确认/死信/回放；一期不引入 Kafka（运维成本高），Redis Streams 足够支撑当前规模 |
| P0 | 无事务发件箱 | **Outbox 模式**：`ont_event_outbox` 表 + 事务内写入 + 轮询投递器（`OutboxPublisher`）；DB 事务提交时事件写入 outbox 表（同一事务），独立线程轮询 outbox 表投递到 Redis Streams，保证不丢事件 |
| P0 | 事件无 schema | **事件 schema 定义**：每个事件类型定义结构化 schema（事件 ID、类型、时间戳、来源、负载 JSON、版本号）；事件版本化（`event_version` 字段），消费端按版本兼容处理 |
| P1 | 变更事件散落各 Service | **统一领域事件基类 + 事件发布门面**：`OntologyDomainEvent` 基类 + `OntEventPublisher` 门面；各 Service 调用 `eventPublisher.publish(event)` 统一发布，门面内部走 Outbox |
| P1 | 无消费确认与死信 | **消费者组 + PEL + 死信流**：Redis Streams 消费者组（Consumer Group）+ Pending Entry List（PEL）消费确认 + 超时未确认自动转死信流（`ontology:dlq`）；死信流提供重放接口 |
| P1 | 无事件回放 | **事件流持久化 + 回放接口**：Redis Streams 默认持久化（`MAXLEN` 限制保留条数，默认 10000）；提供 `POST /ontology/events/replay` 接口从指定位置回放事件 |
| P2 | 无事件可观测 | **事件计数 + 延迟监控**：发布数/消费数/积压数/失败数通过 actuator 暴露；事件 trace ID 贯穿发布→消费链路 |

### 1.3 本期范围

**本期包含：**

1. 事件总线基础设施（Redis Streams 消费者组 + 消费确认 + 死信流）
2. 事务发件箱模式（`ont_event_outbox` 表 + `OutboxPublisher` 轮询投递器）
3. 领域事件模型（`OntologyDomainEvent` 基类 + 15 个具体事件类型）
4. 事件发布门面（`OntEventPublisher`——统一发布入口，内部走 Outbox）
5. 事件消费框架（`OntEventListener` 注解 + 消费者自动注册 + 重试 + 死信）
6. 事件 schema 版本化（`event_version` 字段 + 兼容性策略）
7. 事件回放接口（`POST /ontology/events/replay`）
8. 事件可观测（计数 + 积压监控 + trace ID）
9. Flyway V18 迁移脚本（outbox 表 + 死信记录表 + 菜单权限）
10. 前端事件监控页（事件流概览 + 积压监控 + 死信查询 + 回放操作）
11. 现有模块改造（模块 13/14 的 Spring `ApplicationEvent` 迁移到 EDA 事件总线）

**本期不包含：**

- Kafka 迁移——一期用 Redis Streams，当事件吞吐超 10000 msg/s 或需跨数据中心时评估迁移 Kafka
- 跨服务事件传播——pig-boot 单体部署下全部消费者在同一 JVM；未来微服务化后消费者拆分到独立服务，Redis Streams 天然支持跨进程消费
- 事件溯源（Event Sourcing）——本期事件不作为系统状态的唯一真相源（关系库仍为真相源），事件仅用于通知与解耦
- CQRS 读写分离——本期不做读模型物化（远期可基于事件流构建读模型）
- CDC 自动捕获（Debezium）——一期用 Outbox 模式（应用层发事件），不引入 Debezium（运维复杂度高）；远期评估 Debezium 替代 Outbox 轮询
- 事件 schema 注册中心（Schema Registry）——一期 schema 版本内嵌在事件 JSON 中，不引入独立 Schema Registry
- WebSocket 实时推送——本期事件消费为后端内部消费，不向前端推送；前端通过轮询或 SSE 获取异步操作结果
- 复杂事件路由/过滤引擎——一期消费者按事件类型静态匹配（`@OntEventListener(eventType="INSTANCE_CREATED")`），不做动态路由

### 1.4 与前置模块的边界

| 能力 | 前置模块 | 本模块（EDA） |
|------|----------|--------------|
| Spring ApplicationEvent | 模块 13（SPARQL 缓存失效）、模块 14（版本变更通知） | **替代**——将 Spring `ApplicationEvent` 迁移到 EDA 事件总线，统一走 Outbox + Redis Streams |
| 异步校验 | 模块 09 `@Async("applicationAsyncTaskExecutor")` | **保留**——校验引擎的异步执行继续用 Spring `@Async`；EDA 事件总线用于校验完成后的结果通知（`VALIDATION_COMPLETED` 事件） |
| 序列化日志 | 模块 10 `ont_serialization_log` | **保留**——序列化操作日志继续由模块 10 管理；EDA 事件总线用于序列化完成后的通知（`SERIALIZATION_COMPLETED` 事件） |
| 安全审计 | 模块 36 `ont_data_access_log` | **保留**——安全审计日志继续由模块 36 管理；EDA 事件总线用于安全事件传播（如 `DEVICE_AUTH_FAILED` 事件触发告警） |
| Redis | pig-common-data `RedisTemplateConfiguration` | **复用**——使用已配置的 `RedisTemplate`，通过 `StreamOperations` 操作 Redis Streams |

### 1.5 权威语义口径

1. EDA 事件总线是**横切基础设施层**，所有业务模块的事件通信都通过本模块的事件发布门面和消费框架实现——事件发布走 `OntEventPublisher.publish()`，事件消费标注 `@OntEventListener`，不直接操作 Redis Streams API。
2. **事件总线选型为 Redis Streams**——理由：(a) Redis 已部署且 `spring-boot-starter-data-redis` 已引入，零新增依赖；(b) Redis Streams 原生支持消费者组、消费确认（PEL）、死信、回放、持久化，满足事件总线全部核心需求；(c) pig-boot 单体部署下无需跨数据中心，Redis Streams 性能足够；(d) Kafka 运维成本高（Zookeeper/KRaft + Topic 管理 + 监控），一期不引入。
3. **事务发件箱模式保证不丢事件**——DB 事务提交时将事件写入 `ont_event_outbox` 表（同一事务内），独立线程轮询 outbox 表投递到 Redis Streams；即使应用在事务提交后、投递前崩溃，轮询线程重启后仍会投递未处理事件。
4. **事件 schema 版本化**——每个事件包含 `event_version` 字段（如 `1.0`），消费端按版本号兼容处理；版本升级时新增字段不破坏旧消费端（向后兼容），删除/重命名字段需新版本号 + 消费端适配。
5. **事件类型分区**——Redis Streams 按事件类别分区到不同 Stream Key：`ontology:stream:schema`（Schema 变更事件）、`ontology:stream:instance`（实例变更事件）、`ontology:stream:validation`（校验事件）、`ontology:stream:system`（系统事件：版本/安全/序列化）；消费组按 Stream Key 独立消费。
6. **消费者组 + PEL 消费确认**——每个消费者组（Consumer Group）有独立名称，消费者处理完事件后发送 `XACK` 确认；未确认的事件留在 PEL（Pending Entry List）中，超时（默认 60s）未确认自动转死信流 `ontology:dlq`。
7. **死信处理**——死信流 `ontology:dlq` 中的事件提供 `POST /ontology/events/dlq/{id}/replay` 接口手动重放；死信流保留 7 天自动清理。
8. **事件回放**——`POST /ontology/events/replay` 接口接收 Stream Key + 起始 ID + 事件类型过滤器，从指定位置重新投递事件到消费者；回放是幂等的（消费端需处理重复事件）。
9. **事件不作为系统状态的真相源**——关系库（PostgreSQL）仍是唯一真相源；事件仅用于通知与解耦，消费端处理失败不影响数据一致性（最终一致性 + 重试 + 死信兜底）。
10. **权限模型**：事件监控需 `ontology_event_view` 权限（管理员+审核员）；事件回放需 `ontology_event_admin` 权限（仅管理员）；事件发布为内部 API，不暴露 REST 端点（各 Service 通过 `OntEventPublisher` 编程式发布）。

---

## 2. 总体架构

### 2.1 架构分层

```
┌──────────────────────────────────────────────────────────────────┐
│                     前端层 (Vue 3 + Element Plus)                    │
│  ┌──────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ │
│  │事件流概览│ │ 积压监控     │ │ 死信查询     │ │ 事件回放     │ │
│  │(各Stream │ │(各消费者组   │ │(死信流列表+  │ │(选择Stream+ │ │
│  │ 消息数)  │ │ PEL积压数)   │ │ 失败原因)    │ │ 起始ID回放)  │ │
│  └──────────┘ └──────────────┘ └──────────────┘ └──────────────┘ │
├──────────────────────────────────────────────────────────────────┤
│                     接口层 (REST API, 仅监控+管理)                    │
│  GET  /ontology/events/streams              (Stream概览)           │
│  GET  /ontology/events/pending              (PEL积压)              │
│  GET  /ontology/events/dlq                  (死信列表)             │
│  POST /ontology/events/dlq/{id}/replay      (死信重放)             │
│  POST /ontology/events/replay               (事件回放)             │
├──────────────────────────────────────────────────────────────────┤
│                   事件驱动骨干层 (EDA)                                │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  OntEventPublisher          ← 事件发布门面 (各Service调用)    │ │
│  │  OutboxPublisher            ← Outbox轮询投递器 (DB→Streams)   │ │
│  │  EventStreamRouter          ← 事件路由 (按类型→Stream Key)    │ │
│  │  OntEventListenerContainer  ← 消费者容器 (管理消费者生命周期)  │ │
│  │  DeadLetterHandler          ← 死信处理 (超时→DLQ)             │ │
│  │  EventReplayService         ← 事件回放                        │ │
│  │  EventMetricsCollector      ← 事件计数+延迟监控               │ │
│  └─────────────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────────┤
│                   事件存储层                                         │
│  ont_event_outbox (事务发件箱表)                                    │
│  ont_event_dead_letter (死信记录表)                                 │
│  Redis Streams (事件流持久化)                                      │
│    ├── ontology:stream:schema     (Schema变更事件)                │
│    ├── ontology:stream:instance   (实例变更事件)                  │
│    ├── ontology:stream:validation (校验事件)                      │
│    ├── ontology:stream:system     (系统事件:版本/安全/序列化)     │
│    └── ontology:dlq               (死信流)                        │
├──────────────────────────────────────────────────────────────────┤
│                   基础设施层                                         │
│  RedisTemplate (pig-common-data) │ StreamOperations │ Redis 8.8.0 │
└──────────────────────────────────────────────────────────────────┘
```

### 2.2 模块定位

EDA 事件总线位于横切基础设施层（差距分析 §5.2 E 层），是运行时地基的第二块：

```
┌─────────────────────────────────────────────────────────────┐
│  E. 横切基础设施层 (§5.2 E 层)                                │
│  36. 安全与合规 │ 26. 事件驱动骨干EDA(本模块) │ 37. 可观测性 │
├─────────────────────────────────────────────────────────────┤
│  A. 本体能力增强层: 13.SPARQL │ 14.版本演化 │ 15.对齐       │
├─────────────────────────────────────────────────────────────┤
│  可视化展示层: 可视化(12)                                     │
├─────────────────────────────────────────────────────────────┤
│  输出层: 序列化(10) │ 扩展(11)                                │
├─────────────────────────────────────────────────────────────┤
│  约束层: 校验引擎(09)                                         │
├─────────────────────────────────────────────────────────────┤
│  实例层: 实体对象实例(08)                                     │
├─────────────────────────────────────────────────────────────┤
│  Schema层: 命名空间(03)│实体类型(04)│数据属性(05)             │
│           │对象属性(06)│公理规则(07)│单位字典(02)             │
└─────────────────────────────────────────────────────────────┘
```

EDA 事件总线是连接所有运行时模块的"神经系统"——IoT 接入、告警规则、服务触发、CEP 编排等后续模块都通过事件总线通信。

### 2.3 技术选型

| 组件 | 选型 | 版本 | 选型理由 |
|------|------|------|----------|
| 事件总线 | Redis Streams | Redis 8.8.0（已部署） | 原生支持消费者组/PEL/死信/回放/持久化；零新增依赖（`spring-boot-starter-data-redis` 已引入）；pig-boot 单体部署下性能足够 |
| 事件序列化 | Jackson JSON | Spring Boot 自带 | 事件负载为 JSON，与现有 `R<T>` 响应一致 |
| Outbox 投递 | Spring `@Scheduled` 轮询 | Spring Boot 自带 | 定时轮询 outbox 表（每 2s），投递到 Redis Streams 后标记已处理 |
| 消费者框架 | Spring `StreamMessageListenerContainer` | Spring Data Redis | Spring Data Redis 原生支持 Redis Streams 消费者容器，自动管理消费者生命周期、消费确认、异常处理 |
| 事件监控 | Spring Boot Actuator + 自定义 Meter | 已有 | 通过 `MeterRegistry` 注册计数器/计时器，actuator 暴露 |
| Kafka（远期） | 不引入 | — | 一期不引入；当事件吞吐超 10000 msg/s 或需跨数据中心时评估迁移 |

### 2.4 Redis Streams 拓扑

```
                    ┌──────────────────────┐
                    │  ont_event_outbox    │
                    │  (PostgreSQL 表)     │
                    └──────────┬───────────┘
                               │ OutboxPublisher 轮询 (每2s)
                               ▼
                    ┌──────────────────────┐
                    │  EventStreamRouter   │
                    │  (按事件类型路由)     │
                    └──┬───────┬───────┬───┘
                       │       │       │
          ┌────────────┘       │       └────────────┐
          ▼                    ▼                    ▼
  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐
  │ontology:stream│  │ontology:stream│  │ontology:stream│
  │   :schema     │  │  :instance    │  │ :validation   │  ... :system
  └───────┬───────┘  └───────┬───────┘  └───────┬───────┘
          │                  │                  │
          ▼                  ▼                  ▼
  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐
  │Consumer Group │  │Consumer Group │  │Consumer Group │
  │ "sparql-cache"│  │ "viz-cache"   │  │"audit-logger" │
  └───────┬───────┘  └───────┬───────┘  └───────┬───────┘
          │                  │                  │
          ▼                  ▼                  ▼
  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐
  │@OntEventListener│ │@OntEventListener│ │@OntEventListener│
  │ (SPARQL缓存   │  │ (可视化刷新   │  │ (安全审计    │
  │  失效)        │  │  等)          │  │  记录)       │
  └───────────────┘  └───────────────┘  └───────────────┘

  超时未ACK → ┌───────────────┐
              │ontology:dlq   │ → POST /ontology/events/dlq/{id}/replay
              │(死信流)       │
              └───────────────┘
```

---

## 3. 数据模型

### 3.1 事务发件箱表（ont_event_outbox）

```sql
-- 事务发件箱表 (保证 DB 事务与事件发布一致性)
CREATE TABLE ont_event_outbox (
  id BIGSERIAL PRIMARY KEY,
  event_id VARCHAR(64) NOT NULL UNIQUE,           -- 事件唯一ID (UUID)
  event_type VARCHAR(64) NOT NULL,                -- 事件类型 (如 INSTANCE_CREATED)
  event_version VARCHAR(16) NOT NULL DEFAULT '1.0', -- 事件 schema 版本
  stream_key VARCHAR(128) NOT NULL,               -- 目标 Stream Key (如 ontology:stream:instance)
  payload JSONB NOT NULL,                          -- 事件负载 JSON
  trace_id VARCHAR(128),                           -- 链路追踪 ID
  ontology_id BIGINT,                              -- 关联本体工程 ID
  published_by VARCHAR(64),                        -- 发布人 (用户名或 system)
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',   -- PENDING / PUBLISHED / FAILED
  published_at TIMESTAMP,                          -- 投递到 Streams 的时间
  retry_count INTEGER DEFAULT 0,                   -- 重试次数
  error_message TEXT,                              -- 失败原因
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),      -- 创建时间 (事务内写入)
  del_flag CHAR(1) DEFAULT '0' NOT NULL
);
CREATE INDEX idx_event_outbox_status ON ont_event_outbox (status, created_at);
CREATE INDEX idx_event_outbox_stream ON ont_event_outbox (stream_key, status);
COMMENT ON TABLE ont_event_outbox IS '事务发件箱表';
```

### 3.2 死信记录表（ont_event_dead_letter）

```sql
-- 死信记录表 (消费失败的事件记录)
CREATE TABLE ont_event_dead_letter (
  id BIGSERIAL PRIMARY KEY,
  event_id VARCHAR(64) NOT NULL,                   -- 原始事件ID
  event_type VARCHAR(64) NOT NULL,
  event_version VARCHAR(16) NOT NULL,
  stream_key VARCHAR(128) NOT NULL,                -- 原 Stream Key
  consumer_group VARCHAR(128) NOT NULL,            -- 消费者组名
  consumer_name VARCHAR(128),                      -- 消费者名
  payload JSONB NOT NULL,
  error_message TEXT NOT NULL,                     -- 失败原因
  error_count INTEGER NOT NULL DEFAULT 1,          -- 失败次数
  first_failed_at TIMESTAMP NOT NULL DEFAULT NOW(),
  last_failed_at TIMESTAMP NOT NULL DEFAULT NOW(),
  replayed BOOLEAN DEFAULT FALSE,                  -- 是否已重放
  replayed_at TIMESTAMP,
  del_flag CHAR(1) DEFAULT '0' NOT NULL
);
CREATE INDEX idx_dead_letter_stream ON ont_event_dead_letter (stream_key, consumer_group);
CREATE INDEX idx_dead_letter_replayed ON ont_event_dead_letter (replayed) WHERE replayed = false;
COMMENT ON TABLE ont_event_dead_letter IS '死信记录表';
```

### 3.3 领域事件基类

```java
/**
 * 本体领域事件基类。
 * <p>
 * 所有业务事件继承此基类，通过 OntEventPublisher 发布。
 * </p>
 */
@Getter
@Setter
public abstract class OntologyDomainEvent {

    /** 事件唯一 ID (UUID) */
    private String eventId = UUID.randomUUID().toString().replace("-", "");

    /** 事件类型 (子类提供) */
    private String eventType;

    /** 事件 schema 版本 */
    private String eventVersion = "1.0";

    /** 事件发生时间 */
    private LocalDateTime occurredAt = LocalDateTime.now();

    /** 发布人 (用户名或 system) */
    private String publishedBy;

    /** 链路追踪 ID */
    private String traceId;

    /** 关联本体工程 ID */
    private Long ontologyId;

    /**
     * 获取 Stream Key (按事件类型路由)。
     */
    public String getStreamKey() {
        return EventStreamRouter.route(getEventType());
    }

    /**
     * 转为 JSON 负载。
     */
    public String toJson() {
        return JsonUtils.toJson(this);
    }
}
```

### 3.4 事件类型清单（15 个）

| 事件类型 | Stream Key | 发布方 | 消费方 | 说明 |
|---------|------------|--------|--------|------|
| `ENTITY_TYPE_CREATED` | `ontology:stream:schema` | 模块 04 | SPARQL 缓存失效(13)、可视化刷新(12) | 新增实体类型 |
| `ENTITY_TYPE_UPDATED` | `ontology:stream:schema` | 模块 04 | SPARQL 缓存失效(13)、可视化刷新(12) | 修改实体类型 |
| `ENTITY_TYPE_DELETED` | `ontology:stream:schema` | 模块 04 | SPARQL 缓存失效(13)、可视化刷新(12) | 删除实体类型 |
| `DATA_PROPERTY_CHANGED` | `ontology:stream:schema` | 模块 05 | SPARQL 缓存失效(13) | 数据属性变更（新增/修改/删除合并） |
| `OBJECT_PROPERTY_CHANGED` | `ontology:stream:schema` | 模块 06 | SPARQL 缓存失效(13) | 对象属性变更 |
| `AXIOM_RULE_CHANGED` | `ontology:stream:schema` | 模块 07 | SPARQL 缓存失效(13) | 公理规则变更 |
| `INSTANCE_CREATED` | `ontology:stream:instance` | 模块 08 | SPARQL 缓存失效(13)、安全审计(36) | 新增实例 |
| `INSTANCE_UPDATED` | `ontology:stream:instance` | 模块 08 | SPARQL 缓存失效(13)、安全审计(36) | 修改实例 |
| `INSTANCE_DELETED` | `ontology:stream:instance` | 模块 08 | SPARQL 缓存失效(13)、安全审计(36) | 删除实例 |
| `VALIDATION_COMPLETED` | `ontology:stream:validation` | 模块 09 | 安全审计(36) | 校验完成（含结果摘要） |
| `SERIALIZATION_COMPLETED` | `ontology:stream:system` | 模块 10 | 安全审计(36) | 序列化导出/导入完成 |
| `VERSION_PUBLISHED` | `ontology:stream:system` | 模块 14 | SPARQL 缓存失效(13)、安全审计(36) | 版本发布 |
| `VERSION_ROLLBACK` | `ontology:stream:system` | 模块 14 | SPARQL 缓存失效(13)、安全审计(36) | 版本回滚 |
| `SECURITY_EVENT` | `ontology:stream:system` | 模块 36 | 安全审计(36) | 安全事件（设备认证失败、审计链断裂等） |
| `EXTENSION_CHANGED` | `ontology:stream:schema` | 模块 11 | SPARQL 缓存失效(13)、可视化刷新(12) | 扩展模块变更 |

### 3.5 具体事件示例

```java
/**
 * 实例创建事件。
 */
@Getter
@Setter
public class InstanceCreatedEvent extends OntologyDomainEvent {

    private String instanceIri;
    private String rdfTypeIri;
    private String label;

    public InstanceCreatedEvent() {
        setEventType("INSTANCE_CREATED");
    }
}

/**
 * 版本发布事件。
 */
@Getter
@Setter
public class VersionPublishedEvent extends OntologyDomainEvent {

    private String versionNumber;
    private String compatibility;
    private Long versionId;

    public VersionPublishedEvent() {
        setEventType("VERSION_PUBLISHED");
    }
}

/**
 * 校验完成事件。
 */
@Getter
@Setter
public class ValidationCompletedEvent extends OntologyDomainEvent {

    private Long reportId;
    private Boolean conforms;
    private Integer violationCount;
    private Integer warningCount;
    private Long executionTimeMs;

    public ValidationCompletedEvent() {
        setEventType("VALIDATION_COMPLETED");
    }
}
```

---

## 4. 核心设计

### 4.1 事件发布门面

```java
/**
 * 事件发布门面。
 * <p>
 * 各 Service 通过此门面发布事件，门面内部走 Outbox 模式：
 * 1. 将事件写入 ont_event_outbox 表 (与业务操作同一事务)
 * 2. OutboxPublisher 轮询投递到 Redis Streams
 * </p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OntEventPublisher {

    private final OntEventOutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;

    /**
     * 发布事件 (写入 Outbox 表)。
     * <p>
     * 必须在 @Transactional 事务内调用，保证与业务操作原子性。
     * </p>
     */
    public void publish(OntologyDomainEvent event) {
        PigUser user = SecurityUtils.getUser();
        event.setPublishedBy(user != null ? user.getUsername() : "system");

        OntEventOutbox outbox = new OntEventOutbox();
        outbox.setEventId(event.getEventId());
        outbox.setEventType(event.getEventType());
        outbox.setEventVersion(event.getEventVersion());
        outbox.setStreamKey(event.getStreamKey());
        outbox.setPayload(objectMapper.writeValueAsString(event));
        outbox.setTraceId(MDC.get("traceId"));
        outbox.setOntologyId(event.getOntologyId());
        outbox.setPublishedBy(event.getPublishedBy());
        outbox.setStatus("PENDING");

        outboxMapper.insert(outbox);
        log.debug("事件写入Outbox: eventId={}, type={}, stream={}",
            event.getEventId(), event.getEventType(), event.getStreamKey());
    }

    /**
     * 批量发布事件。
     */
    public void publishAll(List<OntologyDomainEvent> events) {
        for (OntologyDomainEvent event : events) {
            publish(event);
        }
    }
}
```

### 4.2 事件流路由

```java
/**
 * 事件流路由器。
 * <p>
 * 按事件类型路由到对应的 Redis Stream Key。
 * </p>
 */
public class EventStreamRouter {

    private static final Map<String, String> ROUTE_MAP = Map.of(
        // Schema 变更事件
        "ENTITY_TYPE_CREATED", "ontology:stream:schema",
        "ENTITY_TYPE_UPDATED", "ontology:stream:schema",
        "ENTITY_TYPE_DELETED", "ontology:stream:schema",
        "DATA_PROPERTY_CHANGED", "ontology:stream:schema",
        "OBJECT_PROPERTY_CHANGED", "ontology:stream:schema",
        "AXIOM_RULE_CHANGED", "ontology:stream:schema",
        "EXTENSION_CHANGED", "ontology:stream:schema",
        // 实例变更事件
        "INSTANCE_CREATED", "ontology:stream:instance",
        "INSTANCE_UPDATED", "ontology:stream:instance",
        "INSTANCE_DELETED", "ontology:stream:instance",
        // 校验事件
        "VALIDATION_COMPLETED", "ontology:stream:validation",
        // 系统事件
        "SERIALIZATION_COMPLETED", "ontology:stream:system",
        "VERSION_PUBLISHED", "ontology:stream:system",
        "VERSION_ROLLBACK", "ontology:stream:system",
        "SECURITY_EVENT", "ontology:stream:system"
    );

    /** 死信流 */
    public static final String DLQ_STREAM = "ontology:dlq";

    /** Stream 保留最大条数 */
    public static final long MAXLEN = 10_000;

    /** 死信流保留最大条数 */
    public static final long DLQ_MAXLEN = 5_000;

    public static String route(String eventType) {
        return ROUTE_MAP.getOrDefault(eventType, "ontology:stream:system");
    }
}
```

### 4.3 Outbox 轮询投递器

```java
/**
 * Outbox 轮询投递器。
 * <p>
 * 定时轮询 ont_event_outbox 表中 PENDING 状态的事件，
 * 投递到 Redis Streams 后标记为 PUBLISHED。
 * </p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OntEventOutboxMapper outboxMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY = 3;

    /**
     * 每 2 秒轮询一次 Outbox 表。
     */
    @Scheduled(fixedDelay = 2000)
    public void publishPendingEvents() {
        List<OntEventOutbox> pending = outboxMapper.selectList(
            Wrappers.<OntEventOutbox>lambdaQuery()
                .eq(OntEventOutbox::getStatus, "PENDING")
                .lt(OntEventOutbox::getRetryCount, MAX_RETRY)
                .orderByAsc(OntEventOutbox::getCreatedAt)
                .last("LIMIT " + BATCH_SIZE));

        if (pending.isEmpty()) return;

        for (OntEventOutbox entry : pending) {
            try {
                // 投递到 Redis Streams
                String streamKey = entry.getStreamKey();
                MapRecord<String, String, String> record = StreamRecords.newRecord()
                    .ofMap(Map.of(
                        "eventId", entry.getEventId(),
                        "eventType", entry.getEventType(),
                        "eventVersion", entry.getEventVersion(),
                        "payload", entry.getPayload(),
                        "traceId", entry.getTraceId() != null ? entry.getTraceId() : "",
                        "publishedAt", LocalDateTime.now().toString()
                    ))
                    .withStreamKey(streamKey);

                RecordId recordId = redisTemplate.opsForStream()
                    .add(record);

                // 限制 Stream 长度 (保留最近 MAXLEN 条)
                redisTemplate.opsForStream().trim(streamKey, EventStreamRouter.MAXLEN);

                // 标记为已发布
                entry.setStatus("PUBLISHED");
                entry.setPublishedAt(LocalDateTime.now());
                outboxMapper.updateById(entry);

                log.debug("事件投递成功: eventId={}, stream={}, recordId={}",
                    entry.getEventId(), streamKey, recordId);
            } catch (Exception e) {
                log.error("事件投递失败: eventId={}, retry={}",
                    entry.getEventId(), entry.getRetryCount(), e);
                entry.setRetryCount(entry.getRetryCount() + 1);
                entry.setErrorMessage(e.getMessage());
                if (entry.getRetryCount() >= MAX_RETRY) {
                    entry.setStatus("FAILED");
                }
                outboxMapper.updateById(entry);
            }
        }
    }
}
```

### 4.4 消费者框架

```java
/**
 * 事件监听注解。
 * <p>
 * 标注在方法上，声明该方法为事件消费者。
 * 框架自动注册到对应的 Redis Stream 消费者组。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OntEventListener {
    /** 监听的事件类型 (空字符串表示监听该 Stream 全部事件) */
    String eventType() default "";
    /** 消费者组名 (默认自动生成: 类名#方法名) */
    String consumerGroup() default "";
    /** 消费者名 (默认自动生成: hostname#pid) */
    String consumerName() default "";
}

/**
 * 事件监听注解处理器。
 * <p>
 * 启动时扫描所有 @OntEventListener 标注的方法，
 * 自动创建消费者组和消费者，绑定到 Redis Stream。
 * </p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OntEventListenerPostProcessor implements BeanPostProcessor {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final OntEventDeadLetterMapper dlqMapper;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        for (Method method : bean.getClass().getDeclaredMethods()) {
            OntEventListener annotation = AnnotationUtils.findAnnotation(method, OntEventListener.class);
            if (annotation != null) {
                registerListener(bean, method, annotation);
            }
        }
        return bean;
    }

    /**
     * 注册消费者。
     */
    private void registerListener(Object bean, Method method, OntEventListener annotation) {
        String streamKey = determineStreamKey(method, annotation);
        String consumerGroup = annotation.consumerGroup().isEmpty()
            ? bean.getClass().getSimpleName() + "#" + method.getName()
            : annotation.consumerGroup();
        String consumerName = annotation.consumerName().isEmpty()
            ? generateConsumerName()
            : annotation.consumerName();

        // 确保消费者组存在 (从 Stream 起始位置消费)
        try {
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0"), consumerGroup);
        } catch (Exception e) {
            // 消费者组已存在，忽略
            if (!e.getMessage().contains("BUSYGROUP")) {
                log.warn("创建消费者组失败: stream={}, group={}", streamKey, consumerGroup, e);
            }
        }

        // 启动消费循环 (异步线程)
        startConsuming(bean, method, annotation, streamKey, consumerGroup, consumerName);

        log.info("注册事件消费者: stream={}, group={}, consumer={}, eventType={}",
            streamKey, consumerGroup, consumerName, annotation.eventType());
    }

    /**
     * 消费循环。
     */
    private void startConsuming(Object bean, Method method, OntEventListener annotation,
                                 String streamKey, String consumerGroup, String consumerName) {
        // 使用 StreamMessageListenerContainer 或手动消费循环
        // 每次读取 1 条消息，处理完后 XACK
        // 处理失败 → 重试 3 次 → 转死信
        // ...
    }
}
```

### 4.5 消费者使用示例

```java
/**
 * SPARQL 缓存失效消费者。
 * <p>
 * 监听 Schema/实例变更事件，失效 SPARQL Model 缓存。
 * 替代原有的 Spring ApplicationEvent 监听器。
 * </p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SparqlCacheEvictionConsumer {

    private final OntSparqlCacheManager cacheManager;

    @OntEventListener(eventType = "ENTITY_TYPE_CREATED")
    public void onEntityTypeCreated(OntologyDomainEvent event) {
        log.info("接收 ENTITY_TYPE_CREATED 事件: ontologyId={}", event.getOntologyId());
        cacheManager.evict(event.getOntologyId());
    }

    @OntEventListener(eventType = "INSTANCE_CREATED")
    public void onInstanceCreated(OntologyDomainEvent event) {
        log.info("接收 INSTANCE_CREATED 事件: ontologyId={}", event.getOntologyId());
        cacheManager.evict(event.getOntologyId());
    }

    @OntEventListener(eventType = "VERSION_PUBLISHED")
    public void onVersionPublished(VersionPublishedEvent event) {
        log.info("接收 VERSION_PUBLISHED 事件: version={}", event.getVersionNumber());
        cacheManager.evict(event.getOntologyId());
    }
}

/**
 * 安全审计消费者。
 * <p>
 * 监听实例变更和校验事件，记录到数据访问审计日志。
 * </p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityAuditConsumer {

    private final OntDataAccessLogService auditLogService;

    @OntEventListener(eventType = "INSTANCE_CREATED")
    public void onInstanceCreated(InstanceCreatedEvent event) {
        // 记录数据访问审计
        // ...
    }

    @OntEventListener(eventType = "SECURITY_EVENT")
    public void onSecurityEvent(OntologyDomainEvent event) {
        // 安全事件告警
        // ...
    }
}
```

### 4.6 死信处理

```java
/**
 * 死信处理器。
 * <p>
 * 监控 PEL (Pending Entry List) 中超时未确认的事件，
 * 超过 60s 未确认的自动转死信流 ontology:dlq。
 * </p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DeadLetterHandler {

    private final StringRedisTemplate redisTemplate;
    private final OntEventDeadLetterMapper dlqMapper;
    private final ObjectMapper objectMapper;

    private static final long PENDING_TIMEOUT_MS = 60_000; // 60 秒

    /**
     * 每 10 秒扫描一次 PEL。
     */
    @Scheduled(fixedDelay = 10_000)
    public void scanPendingEntries() {
        for (String streamKey : EventStreamRouter.getAllStreamKeys()) {
            for (String consumerGroup : getConsumerGroups(streamKey)) {
                // 读取 PEL 中超时的消息
                PendingMessages pending = redisTemplate.opsForStream()
                    .pending(streamKey, consumerGroup, Range.unbounded(), 100);

                for (PendingMessage pm : pending) {
                    if (pm.getElapsedTimeSinceDelivery().toMillis() > PENDING_TIMEOUT_MS) {
                        moveToDeadLetter(streamKey, consumerGroup, pm);
                    }
                }
            }
        }
    }

    /**
     * 将超时消息转入死信流。
     */
    private void moveToDeadLetter(String streamKey, String consumerGroup, PendingMessage pm) {
        // 1. 读取原始消息
        MapRecord<String, String, String> record = redisTemplate.opsForStream()
            .records(streamKey, RecordId.of(pm.getIdAsString()));

        // 2. 投递到死信流
        MapRecord<String, String, String> dlqRecord = StreamRecords.newRecord()
            .ofMap(Map.of(
                "eventId", record.getValue().get("eventId"),
                "eventType", record.getValue().get("eventType"),
                "originalStream", streamKey,
                "consumerGroup", consumerGroup,
                "payload", record.getValue().get("payload"),
                "errorMessage", "消费超时未确认",
                "failedAt", LocalDateTime.now().toString()
            ))
            .withStreamKey(EventStreamRouter.DLQ_STREAM);

        redisTemplate.opsForStream().add(dlqRecord);
        redisTemplate.opsForStream().trim(EventStreamRouter.DLQ_STREAM, EventStreamRouter.DLQ_MAXLEN);

        // 3. 写入死信记录表
        OntEventDeadLetter dlq = new OntEventDeadLetter();
        dlq.setEventId(record.getValue().get("eventId"));
        dlq.setEventType(record.getValue().get("eventType"));
        dlq.setStreamKey(streamKey);
        dlq.setConsumerGroup(consumerGroup);
        dlq.setPayload(record.getValue().get("payload"));
        dlq.setErrorMessage("消费超时未确认");
        dlqMapper.insert(dlq);

        // 4. ACK 原始消息 (从 PEL 移除)
        redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, pm.getIdAsString());

        log.warn("事件转入死信: eventId={}, stream={}, group={}",
            record.getValue().get("eventId"), streamKey, consumerGroup);
    }
}
```

### 4.7 事件回放

```java
/**
 * 事件回放服务。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventReplayService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 从指定位置回放事件。
     * @param streamKey Stream Key
     * @param fromId 起始消息 ID (如 "1234567890-0")
     * @param eventTypeFilter 事件类型过滤器 (null 表示全部)
     * @param targetConsumerGroup 目标消费者组 (null 表示全部消费者组)
     */
    public int replay(String streamKey, String fromId, String eventTypeFilter, String targetConsumerGroup) {
        int count = 0;
        List<MapRecord<String, String, String>> records = redisTemplate.opsForStream()
            .range(streamKey, Range.open(fromId, "+"));

        for (MapRecord<String, String, String> record : records) {
            String eventType = record.getValue().get("eventType");
            if (eventTypeFilter != null && !eventTypeFilter.equals(eventType)) {
                continue;
            }
            // 重新投递到目标消费者组
            // (通过 XCLAIM 或重新 XADD 实现)
            // ...
            count++;
        }
        log.info("事件回放完成: stream={}, from={}, count={}", streamKey, fromId, count);
        return count;
    }

    /**
     * 重放死信。
     */
    public void replayDeadLetter(String deadLetterId) {
        OntEventDeadLetter dlq = dlqMapper.selectById(deadLetterId);
        if (dlq == null || dlq.getReplayed()) {
            throw new OntEventException("死信不存在或已重放");
        }
        // 重新投递到原 Stream
        MapRecord<String, String, String> record = StreamRecords.newRecord()
            .ofMap(Map.of(
                "eventId", dlq.getEventId(),
                "eventType", dlq.getEventType(),
                "payload", dlq.getPayload(),
                "replayedAt", LocalDateTime.now().toString()
            ))
            .withStreamKey(dlq.getStreamKey());
        redisTemplate.opsForStream().add(record);

        // 标记为已重放
        dlq.setReplayed(true);
        dlq.setReplayedAt(LocalDateTime.now());
        dlqMapper.updateById(dlq);
    }
}
```

### 4.8 事件监控

```java
/**
 * 事件指标收集器。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EventMetricsCollector {

    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    /**
     * 每 30 秒采集一次事件指标。
     */
    @Scheduled(fixedDelay = 30_000)
    public void collectMetrics() {
        for (String streamKey : EventStreamRouter.getAllStreamKeys()) {
            // Stream 消息总数
            Long streamSize = redisTemplate.opsForStream().size(streamKey);
            meterRegistry.gauge("ontology.event.stream.size",
                Tags.of("stream", streamKey), streamSize);

            // 各消费者组 PEL 积压数
            for (String group : getConsumerGroups(streamKey)) {
                PendingMessagesSummary summary = redisTemplate.opsForStream()
                    .pending(streamKey, group);
                if (summary != null) {
                    meterRegistry.gauge("ontology.event.pending",
                        Tags.of("stream", streamKey, "group", group),
                        summary.getTotalPendingMessages());
                }
            }
        }

        // Outbox 积压数
        long outboxPending = outboxMapper.selectCount(
            Wrappers.<OntEventOutbox>lambdaQuery()
                .eq(OntEventOutbox::getStatus, "PENDING"));
        meterRegistry.gauge("ontology.event.outbox.pending", outboxPending);

        // 死信流消息数
        Long dlqSize = redisTemplate.opsForStream().size(EventStreamRouter.DLQ_STREAM);
        meterRegistry.gauge("ontology.event.dlq.size", dlqSize);
    }
}
```

---

## 5. 接口设计

### 5.1 REST API

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/ontology/events/streams` | `ontology_event_view` | Stream 概览（各 Stream 消息数、消费者组数） |
| GET | `/ontology/events/pending` | `ontology_event_view` | PEL 积压详情（各消费者组未确认消息） |
| GET | `/ontology/events/dlq` | `ontology_event_view` | 死信列表（分页） |
| POST | `/ontology/events/dlq/{id}/replay` | `ontology_event_admin` | 死信重放 |
| POST | `/ontology/events/replay` | `ontology_event_admin` | 事件回放（从指定位置） |
| GET | `/ontology/events/outbox` | `ontology_event_view` | Outbox 表状态（PENDING/FAILED 计数） |
| GET | `/ontology/events/metrics` | `ontology_event_view` | 事件监控指标 |

### 5.2 接口详情

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/ontology/events")
@Tag(description = "事件驱动", name = "EDA事件总线")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntEventController {

    private final EventReplayService replayService;
    private final OntEventOutboxService outboxService;
    private final EventMetricsCollector metricsCollector;

    @GetMapping("/streams")
    @Operation(summary = "Stream概览")
    @HasPermission("ontology_event_view")
    public R<List<StreamOverviewVO>> streams() {
        return R.ok(metricsCollector.getStreamOverview());
    }

    @GetMapping("/pending")
    @Operation(summary = "PEL积压详情")
    @HasPermission("ontology_event_view")
    public R<List<PendingMessageVO>> pending(@RequestParam String streamKey,
                                              @RequestParam String consumerGroup) {
        return R.ok(metricsCollector.getPendingMessages(streamKey, consumerGroup));
    }

    @GetMapping("/dlq")
    @Operation(summary = "死信列表")
    @HasPermission("ontology_event_view")
    public R<Page<DeadLetterVO>> dlq(Page page) {
        return R.ok(outboxService.getDeadLetters(page));
    }

    @PostMapping("/dlq/{id}/replay")
    @Operation(summary = "死信重放")
    @HasPermission("ontology_event_admin")
    @SysLog("死信重放")
    @RequireConfirm(operationType = "DEAD_LETTER_REPLAY")
    public R<Void> replayDeadLetter(@PathVariable Long id, @RequestParam String confirmToken) {
        replayService.replayDeadLetter(id);
        return R.ok();
    }

    @PostMapping("/replay")
    @Operation(summary = "事件回放")
    @HasPermission("ontology_event_admin")
    @SysLog("事件回放")
    @RequireConfirm(operationType = "EVENT_REPLAY")
    public R<Integer> replay(@Valid @RequestBody EventReplayRequest req,
                              @RequestParam String confirmToken) {
        int count = replayService.replay(req.getStreamKey(), req.getFromId(),
            req.getEventTypeFilter(), req.getTargetConsumerGroup());
        return R.ok(count);
    }
}
```

---

## 6. 前端设计

### 6.1 页面布局

```
┌────────────────────────────────────────────────────────────────────┐
│  事件驱动监控                                                      │
├────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │
│  │ Outbox 积压 │ │ Stream 消息 │ │ PEL 积压    │ │ 死信        │ │
│  │    0 条     │ │  3,452 条   │ │    0 条     │ │    2 条     │ │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ │
├────────────────────────────────────────────────────────────────────┤
│  Tab: [Stream概览] [PEL积压] [死信列表] [Outbox状态] [事件回放]   │
├────────────────────────────────────────────────────────────────────┤
│  ... Tab 内容区 ...                                               │
│                                                                    │
│  Stream 概览:                                                     │
│  ┌──────────────────────┬──────────┬──────────┬──────────────┐   │
│  │ Stream Key           │ 消息总数 │ 消费者组 │ PEL 积压     │   │
│  ├──────────────────────┼──────────┼──────────┼──────────────┤   │
│  │ ontology:stream:schema│ 1,234   │ 2        │ 0            │   │
│  │ ontology:stream:instance│ 2,100 │ 3        │ 0            │   │
│  │ ontology:stream:validation│ 118 │ 1        │ 0            │   │
│  │ ontology:stream:system│ 56     │ 2        │ 0            │   │
│  │ ontology:dlq         │ 2       │ —        │ —            │   │
│  └──────────────────────┴──────────┴──────────┴──────────────┘   │
└────────────────────────────────────────────────────────────────────┘
```

### 6.2 前端组件

| 组件 | 路径 | 职责 |
|------|------|------|
| `index.vue` | `web/src/views/ontology/event/index.vue` | 事件监控主页面 + Tab |
| `StreamOverview.vue` | `web/src/views/ontology/event/components/StreamOverview.vue` | Stream 概览表 |
| `PendingMessages.vue` | `web/src/views/ontology/event/components/PendingMessages.vue` | PEL 积压详情 |
| `DeadLetterList.vue` | `web/src/views/ontology/event/components/DeadLetterList.vue` | 死信列表 + 重放 |
| `EventReplay.vue` | `web/src/views/ontology/event/components/EventReplay.vue` | 事件回放操作 |

---

## 7. Flyway 迁移脚本（V18）

```sql
-- ============================================================
-- V18__ontology_event_driven.sql
-- 事件驱动骨干 EDA 模块
-- 对应差距分析: §3.35 维度㉟ 事件驱动架构 EDA
-- ============================================================

-- 1. 事务发件箱表
CREATE TABLE IF NOT EXISTS ont_event_outbox (
  id BIGSERIAL PRIMARY KEY,
  event_id VARCHAR(64) NOT NULL UNIQUE,
  event_type VARCHAR(64) NOT NULL,
  event_version VARCHAR(16) NOT NULL DEFAULT '1.0',
  stream_key VARCHAR(128) NOT NULL,
  payload JSONB NOT NULL,
  trace_id VARCHAR(128),
  ontology_id BIGINT,
  published_by VARCHAR(64),
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  published_at TIMESTAMP,
  retry_count INTEGER DEFAULT 0,
  error_message TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  del_flag CHAR(1) DEFAULT '0' NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_event_outbox_status ON ont_event_outbox (status, created_at);
CREATE INDEX IF NOT EXISTS idx_event_outbox_stream ON ont_event_outbox (stream_key, status);
COMMENT ON TABLE ont_event_outbox IS '事务发件箱表';
COMMENT ON COLUMN ont_event_outbox.event_id IS '事件唯一ID (UUID)';
COMMENT ON COLUMN ont_event_outbox.event_type IS '事件类型';
COMMENT ON COLUMN ont_event_outbox.event_version IS '事件schema版本';
COMMENT ON COLUMN ont_event_outbox.stream_key IS '目标Redis Stream Key';
COMMENT ON COLUMN ont_event_outbox.payload IS '事件负载JSON';
COMMENT ON COLUMN ont_event_outbox.status IS 'PENDING/PUBLISHED/FAILED';

-- 2. 死信记录表
CREATE TABLE IF NOT EXISTS ont_event_dead_letter (
  id BIGSERIAL PRIMARY KEY,
  event_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  event_version VARCHAR(16) NOT NULL,
  stream_key VARCHAR(128) NOT NULL,
  consumer_group VARCHAR(128) NOT NULL,
  consumer_name VARCHAR(128),
  payload JSONB NOT NULL,
  error_message TEXT NOT NULL,
  error_count INTEGER NOT NULL DEFAULT 1,
  first_failed_at TIMESTAMP NOT NULL DEFAULT NOW(),
  last_failed_at TIMESTAMP NOT NULL DEFAULT NOW(),
  replayed BOOLEAN DEFAULT FALSE,
  replayed_at TIMESTAMP,
  del_flag CHAR(1) DEFAULT '0' NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_dead_letter_stream ON ont_event_dead_letter (stream_key, consumer_group);
CREATE INDEX IF NOT EXISTS idx_dead_letter_replayed ON ont_event_dead_letter (replayed) WHERE replayed = false;
COMMENT ON TABLE ont_event_dead_letter IS '死信记录表';

-- 3. 菜单与权限注册
INSERT INTO sys_menu (menu_id, name, permission, path, component, parent_id, menu_type, sort_order, del_flag, create_time) VALUES
(901500, '事件驱动', NULL, '/ontology/event/index', 'ontology/event/index', 900000, '0', 15, '0', NOW()),
(901501, '事件查看', 'ontology_event_view', NULL, NULL, 901500, '1', 1, '0', NOW()),
(901502, '事件管理', 'ontology_event_admin', NULL, NULL, 901500, '1', 2, '0', NOW())
ON CONFLICT (menu_id) DO NOTHING;

-- 4. 角色权限分配
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 901500), (1, 901501), (1, 901502),  -- 管理员
(3, 901500), (3, 901501)                 -- 审核员（查看）
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 5. 迁移完整性校验
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_event_outbox') THEN
    RAISE EXCEPTION 'V18: ont_event_outbox 表创建失败';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ont_event_dead_letter') THEN
    RAISE EXCEPTION 'V18: ont_event_dead_letter 表创建失败';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 901500) THEN
    RAISE EXCEPTION 'V18: 事件驱动菜单注册失败';
  END IF;
  RAISE NOTICE 'V18__ontology_event_driven 完成';
END $$;
```

---

## 8. 配置变更

### 8.1 application-dev.yml 新增配置

```yaml
# 事件驱动骨干 EDA 配置
ontology:
  event:
    # Outbox 轮询间隔 (毫秒)
    outbox-poll-interval: 2000
    # Outbox 批量大小
    outbox-batch-size: 100
    # Outbox 最大重试次数
    outbox-max-retry: 3
    # 消费者 PEL 超时 (毫秒)
    pending-timeout: 60000
    # PEL 扫描间隔 (毫秒)
    pending-scan-interval: 10000
    # Stream 最大保留条数
    stream-maxlen: 10000
    # 死信流最大保留条数
    dlq-maxlen: 5000
    # Redis Stream Key 前缀
    stream-prefix: ontology:stream
    # 死信 Stream Key
    dlq-stream: ontology:dlq
```

### 8.2 Redis 数据库说明

- Redis 已在 database 5 运行（AGENTS.md 记录）
- Redis Streams 消耗额外内存，每条消息约 500B，10000 条约 5MB，对 Redis 8.8.0 容量无压力
- 不需要额外创建 Redis 数据库

---

## 9. 后端类清单

| 层 | 类 | 路径 | 职责 |
|----|----|------|------|
| Controller | `OntEventController` | `controller/OntEventController.java` | REST API |
| Service | `EventReplayService` | `event/service/` | 事件回放 |
| Service | `OntEventOutboxService` | `event/service/` | Outbox 管理 |
| Component | `OntEventPublisher` | `event/publisher/` | 事件发布门面 |
| Component | `OutboxPublisher` | `event/publisher/` | Outbox 轮询投递 |
| Component | `EventStreamRouter` | `event/router/` | 事件流路由 |
| Component | `OntEventListenerPostProcessor` | `event/listener/` | 消费者注册 |
| Component | `DeadLetterHandler` | `event/handler/` | 死信处理 |
| Component | `EventMetricsCollector` | `event/metrics/` | 事件监控 |
| Base | `OntologyDomainEvent` | `event/model/` | 领域事件基类 |
| Event | (15 个具体事件类) | `event/model/` | 具体事件 |
| Annotation | `@OntEventListener` | `event/annotation/` | 消费者注解 |
| Entity | `OntEventOutbox` | `event/entity/` | Outbox 实体 |
| Entity | `OntEventDeadLetter` | `event/entity/` | 死信实体 |
| Mapper | `OntEventOutboxMapper` | `event/mapper/` | Outbox Mapper |
| Mapper | `OntEventDeadLetterMapper` | `event/mapper/` | 死信 Mapper |
| VO | `StreamOverviewVO` | `vo/` | Stream 概览 VO |
| VO | `PendingMessageVO` | `vo/` | PEL 消息 VO |
| VO | `DeadLetterVO` | `vo/` | 死信 VO |
| Config | `OntEventConfiguration` | `event/config/` | EDA 配置类 |

---

## 10. 现有模块改造

### 10.1 模块 13（SPARQL）改造

| 改造点 | 原实现 | 新实现 |
|--------|--------|--------|
| 缓存失效监听 | `OntSparqlCacheListener` 实现 `@EventListener` 监听 `OntologyDataChangedEvent`（Spring 进程内事件） | 改为 `SparqlCacheEvictionConsumer` 标注 `@OntEventListener` 监听 Redis Streams 事件 |
| 事件发布 | 模块 04/05/06/07/08 各 Service 手动 `applicationEventPublisher.publishEvent(new OntologyDataChangedEvent(...))` | 改为 `ontEventPublisher.publish(new InstanceCreatedEvent(...))`（走 Outbox） |

### 10.2 模块 14（版本演化）改造

| 改造点 | 原实现 | 新实现 |
|--------|--------|--------|
| 版本发布通知 | Spring `ApplicationEvent` | `ontEventPublisher.publish(new VersionPublishedEvent(...))` |
| 版本回滚通知 | Spring `ApplicationEvent` | `ontEventPublisher.publish(new VersionRollbackEvent(...))` |

### 10.3 模块 04/05/06/07/08 改造

在各 Service 的写入方法（save/update/remove）末尾，将 `applicationEventPublisher.publishEvent(...)` 替换为 `ontEventPublisher.publish(...)`。改造范围：

| 模块 | 改造类 | 改造方法 | 事件类型 |
|------|--------|----------|----------|
| 04 | `OntEntityTypeServiceImpl` | save/update/remove | `ENTITY_TYPE_CREATED/UPDATED/DELETED` |
| 05 | `OntDataPropertyServiceImpl` | save/update/remove | `DATA_PROPERTY_CHANGED` |
| 06 | `OntObjectPropertyServiceImpl` | save/update/remove | `OBJECT_PROPERTY_CHANGED` |
| 07 | `OntAxiomRuleServiceImpl` | save/update/remove/toggle | `AXIOM_RULE_CHANGED` |
| 08 | `OntEntityInstanceServiceImpl` | save/update/remove | `INSTANCE_CREATED/UPDATED/DELETED` |
| 09 | `ValidationOrchestratorImpl` | validate | `VALIDATION_COMPLETED` |
| 10 | `SerializationServiceImpl` | export/import | `SERIALIZATION_COMPLETED` |
| 11 | `OntExtensionModuleServiceImpl` | save/update/remove | `EXTENSION_CHANGED` |
| 36 | `TokenDeviceAuthService` | authenticate(失败时) | `SECURITY_EVENT` |

> **改造影响**：每个类 2-3 行代码变更（替换事件发布方式），不影响现有功能。改造后事件通过 Outbox + Redis Streams 传播，不再依赖 Spring 进程内事件。

---

## 11. 测试设计

| 测试类 | 测试内容 |
|--------|----------|
| `OntEventPublisherTest` | 事件写入 Outbox 表、事务一致性 |
| `OutboxPublisherTest` | 轮询投递、批量处理、重试、失败标记 |
| `EventStreamRouterTest` | 事件类型→Stream Key 路由正确性 |
| `OntEventListenerPostProcessorTest` | 消费者自动注册、消费者组创建 |
| `DeadLetterHandlerTest` | PEL 超时检测、死信转入、ACK 清理 |
| `EventReplayServiceTest` | 事件回放、死信重放、幂等性 |
| `EventMetricsCollectorTest` | 指标采集正确性 |
| `OntEventControllerIT` | 端到端：发布事件→Outbox→投递→消费→监控→死信→回放 |

### 验收用例

| 用例 | 操作 | 预期 |
|------|------|------|
| 事件发布-消费 | 模块 08 创建实例 | `INSTANCE_CREATED` 事件经 Outbox→Streams→`SparqlCacheEvictionConsumer` 消费→SPARQL 缓存失效 |
| 事务一致性 | 模块 08 创建实例后事务回滚 | Outbox 事件随事务回滚，不投递 |
| 消费超时→死信 | 消费者模拟处理超时(>60s) | 事件转入 `ontology:dlq`，死信记录表写入 |
| 死信重放 | 调用 `/events/dlq/{id}/replay` | 事件重新投递到原 Stream，消费成功 |
| 事件回放 | 调用 `/events/replay` 指定起始 ID | 从指定位置重新投递事件 |
| 监控指标 | 调用 `/events/metrics` | 返回各 Stream 消息数、PEL 积压数、Outbox 积压数、死信数 |

---

## 12. 与后续模块的关系

| 后续模块 | 关系 |
|---------|------|
| 18. 数据源映射 | 数据源映射写入实例后，发布 `INSTANCE_CREATED` 事件通知下游 |
| 19. IoT 接入层 | IoT 遥测数据更新实例属性后，发布 `INSTANCE_UPDATED` 事件；IoT 设备认证失败发布 `SECURITY_EVENT` 事件 |
| 23. 告警规则 | 告警引擎监听 `INSTANCE_UPDATED` 事件（实时数据变更），触发告警规则评估；告警生成后发布 `ALERT_GENERATED` 事件 |
| 24. 服务触发 | 服务触发引擎监听 `ALERT_GENERATED` 事件，触发动作执行；动作完成后发布 `ACTION_EXECUTED` 事件 |
| 25. CEP/规则流 | CEP 引擎监听事件流，做时序因果链匹配 |
| 37. 可观测性 | 事件指标接入 Prometheus；事件 trace ID 接入 Jaeger/SkyWalking |

---

> 本模块是运行时地基的第二块砖——连接所有运行时模块的"神经系统"。Redis Streams 事件总线使平台从"同步 RPC 耦合"升级为"事件驱动解耦"，事务发件箱保证不丢事件，消费者组+PEL 保证消费可靠性，死信+回放保证可恢复性。这是 IoT 接入、告警规则、服务触发、CEP 编排等后续运行时智能模块的通信基础设施——没有事件总线，这些模块只能同步互相调用，无法解耦、无法削峰、无法回放。
