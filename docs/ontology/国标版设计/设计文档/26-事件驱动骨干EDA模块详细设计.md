# 事件驱动骨干 EDA 模块详细设计

> 设计编号：26
> 设计状态：评审修订版（仅设计，尚未开发）
> 设计依据：`标准本体建模平台PRD.md` §3.2、§8、§9；`现有设计与IoT场景落地差距分析.md` §3.35、§5.2、§7；`本体与iot场景实现沟通.md` 中告警/触发/规则流协作边界
> 前置模块：04～14；36-安全与合规作为管理操作审批和敏感事件治理能力，EDA 本身不反向成为 36 的启动前提
> 基础设施：当前项目已引入 Spring Data Redis 并配置 Redis；开发环境使用 `spring.data.redis.database=5`，模块不得在代码中硬编码数据库号
> 评审日期：2026-07-13

---

## 1. 评审结论

### 1.1 结论

事件驱动骨干应建设，但必须明确它是**低至中等吞吐的领域事件传输层**，不是 IoT 高频遥测总线，也不把所有同步业务强行异步化。

本期采用：

- PostgreSQL Transactional Outbox 作为待投递事件的可靠事实记录。
- Redis Streams 作为进程间传输介质。
- 每个逻辑订阅者独立 Consumer Group；同一订阅者的多个实例共享 Group 做负载均衡。
- 手动 ACK、PEL、过期消息 reclaim、有限重试和死信。
- 消费记录（Inbox）保证正常重复投递时的业务幂等。
- 至少一次（at-least-once）语义；不宣称端到端 exactly-once。

### 1.2 对原设计的关键修正

| 原设计问题 | 修正 |
|---|---|
| 把 Redis Streams 描述成“保证不丢事件” | Outbox 保证 DB 事务内事件不遗漏；DB→Redis 之间仍可能重复投递，整体为 at-least-once，可靠性还依赖 Redis 持久化配置 |
| 多实例轮询直接查 PENDING，无锁 | 使用 `FOR UPDATE SKIP LOCKED` 领取批次，设置 PROCESSING 租约；租约过期可恢复 |
| XADD 成功后宕机再投会重复，但没有幂等 | 增加 `ont_event_consume_record`，消费者按 `consumer_group + event_id + replay_no` 原子领取和幂等 |
| 超过 60 秒未 ACK 就直接死信 | 先 reclaim 并按 delivery count/处理结果重试；只有达到最大次数才入死信，避免误杀长任务 |
| 同一 Consumer Group 下放多个不同业务消费者 | 每个逻辑订阅者必须使用独立 Group；Group 内多个实例才是同一消费者的横向扩展 |
| 回放“通过 XCLAIM 或 XADD”未定，且会被幂等拦截 | 明确使用新 `replay_no` 重新 XADD，指定目标消费者组；正常重复仍使用相同 replay_no=0 并被去重 |
| Redis Stream 只保留 10000 条却称可历史回放 | Redis 仅作有限保留传输；回放源优先使用保留期内的 Outbox 事件，回放范围受控 |
| 高频 IoT 更新每次发布 `INSTANCE_UPDATED` | 高频遥测不进入本骨干；模块 19/21 仅在状态变化、告警、聚合窗口完成等业务事件上发布 |
| 自研 `@OntEventListener` + BeanPostProcessor | 首期使用 Spring Data Redis `StreamMessageListenerContainer` + 显式 Handler Registry，减少自定义框架复杂度 |
| Outbox/死信表使用 `BIGSERIAL`、时间字段不符合仓库规范 | 使用 `bigint + IdType.ASSIGN_ID` 和统一审计字段；菜单列对齐现有 `sys_menu` |
| 迁移给不存在的角色 3/4 授权 | 只授予当前真实存在的管理员角色 1 |

---

## 2. 设计边界

### 2.1 本期包含

- 领域事件统一 Envelope 和版本规则。
- 事务 Outbox 写入与轮询投递。
- Redis Stream、Consumer Group 初始化。
- 手动 ACK、PEL reclaim、重试、死信。
- Inbox 幂等记录。
- 指定事件、指定消费者组的受控回放。
- 事件积压、延迟、失败、死信、Outbox 指标。
- 管理控制台。

### 2.2 本期不包含

- 高频原始遥测、视频流、音频流、大文件和二进制 payload。
- Kafka/Pulsar/Flink 的替代实现。
- 跨地域强一致消息。
- 业务命令与领域事件混用。
- 自动把全量校验、导入导出等同步 API 改为异步 Job；是否异步由对应模块单独设计。
- 通过数据库日志自动 CDC 捕获所有变更。
- 任意用户自助回放全部事件。

### 2.3 事件与命令边界

- **领域事件**：已发生事实，如 `ONTOLOGY_INSTANCE_CHANGED`、`ONTOLOGY_VERSION_PUBLISHED`。
- **命令**：要求某方执行动作，如“远程开门”“重新校验”。命令有接收者、授权和可拒绝结果，后续模块 24 单独设计。
- 本模块首期 Stream 只承载领域事件和系统通知，不承载安全攸关设备命令。

### 2.4 高频 IoT 边界

以下数据不得逐条进入 Outbox：

- 每秒/毫秒级传感器采样。
- 设备心跳。
- 原始时序点位变化。

模块 19/21 应在边缘/时序层消化原始数据，仅发布：状态跃迁、窗口聚合完成、告警产生/恢复、设备上线/离线、数据质量异常等低频业务事件。

---

## 3. 总体架构

```text
业务 Service（同一 PostgreSQL 事务）
  ├─ 写业务表
  └─ OntDomainEventPublisher.append(event)
            │
            ▼
       ont_event_outbox
            │ 领取：FOR UPDATE SKIP LOCKED + lease
            ▼
       OutboxRelay
            │ XADD
            ▼
 youming:ontology:domain-events
      ├─ group: ontology-sparql-cache-v1
      ├─ group: ontology-visualization-v1
      ├─ group: ontology-security-audit-v1
      └─ group: ...
            │
            ▼
 StreamMessageListenerContainer
      → Inbox 原子领取
      → HandlerRegistry 分发
      → 成功：Inbox SUCCEEDED + XACK
      → 失败：保留 PEL / reclaim 重试
      → 超限：Dead Letter + XACK
```

### 3.1 Stream 规划

首期只创建一个领域事件 Stream：

```text
youming:ontology:domain-events
```

理由：当前规模较小，单 Stream 更易管理、回放和保持大致顺序。后续出现独立吞吐或隔离需求时再拆分：

```text
youming:alert:events
youming:command:events
youming:telemetry:state-events
```

原始遥测不进入上述 domain-events Stream。

### 3.2 Consumer Group 规则

- 一个“逻辑订阅者”一个 Group，例如 SPARQL 缓存失效使用 `ontology-sparql-cache-v1`。
- 同一逻辑订阅者的多个应用实例使用相同 Group、不同 consumer name。
- 不同业务 Handler 不得共用同一 Group，否则一条消息只会被其中一个 Handler 实例消费。
- Group 名带消费者契约主版本，发生不兼容处理逻辑时创建 `-v2`，并规划旧 Group 下线。
- Group 首次创建的 start id 必须配置化：全新部署且 Stream 尚空时可用 `0-0`；后加消费者默认从 `$` 接收新事件，历史补数走受控回放，避免无意处理全部保留事件。
- 部署流程应先完成 Stream/Group 初始化和 Handler readiness，再启用 Outbox Relay。

---

## 4. 事件契约

### 4.1 Envelope

```json
{
  "eventId": "7f183b2e-1d4f-4e57-8e16-8e4295dce3ce",
  "replayNo": 0,
  "eventType": "ONTOLOGY_INSTANCE_CHANGED",
  "eventVersion": 1,
  "occurredAt": "2026-07-13T08:30:00.123Z",
  "source": "pig-ontology-biz",
  "ontologyId": 935001,
  "aggregateType": "ENTITY_INSTANCE",
  "aggregateId": "980001",
  "aggregateRevision": 17,
  "operation": "UPDATED",
  "actorId": 1,
  "traceId": "...",
  "payload": {},
  "metadata": {}
}
```

规则：

- 时间统一 UTC `Instant`，序列化为 ISO-8601 `Z`。
- `eventId` 为领域事件唯一 ID，正常重复投递保持不变。
- `replayNo=0` 为原始投递；人工回放递增。
- `aggregateId` 使用字符串，兼容雪花 ID、IRI 或外部业务 ID。
- `aggregateRevision` 在有顺序要求的聚合上必填；消费者收到旧 revision 时应幂等忽略。Redis Stream 在多实例并发处理下不承诺严格的同聚合处理顺序。
- payload 只放消费所需最小信息，优先放 ID/IRI，不放完整敏感实体。
- 事件大小默认不超过 64 KiB；大内容存对象存储，只传受控引用和 hash。

### 4.2 版本演化

同一 `eventType + eventVersion` 必须向后兼容：

- 可新增可选字段。
- 不删除字段、不改变字段类型和原含义。
- 破坏性变化增加 `eventVersion` 主版本；消费者明确声明支持范围。
- 未知字段必须可忽略。
- 未支持版本进入失败重试/死信，不能静默 ACK。

### 4.3 首期事件类型

采用稳定的粗粒度事件类型，具体资源和动作放 Envelope/payload，避免为 CRUD 每个动作创建大量类。

| 事件类型 | 发布模块 | 典型消费者 |
|---|---|---|
| `ONTOLOGY_SCHEMA_CHANGED` | 03/04/05/06/07/11 | SPARQL 投影失效、可视化刷新、版本工作区标脏 |
| `ONTOLOGY_INSTANCE_CHANGED` | 08/10 导入 | SPARQL 投影失效、可视化刷新、后续告警/映射 |
| `ONTOLOGY_VALIDATION_COMPLETED` | 09 | 版本候选、审计、通知 |
| `ONTOLOGY_SERIALIZATION_COMPLETED` | 10 | 审计、通知 |
| `ONTOLOGY_VERSION_PREPARED` | 14 | 审计、工作流 |
| `ONTOLOGY_VERSION_PUBLISHED` | 14 | 缓存失效、映射兼容性检查 |
| `ONTOLOGY_MIGRATION_COMPLETED` | 14 | 校验、审计 |
| `ONTOLOGY_SECURITY_EVENT` | 36 | 可观测/告警；payload 不含秘密 |
| `EXTENSION_BINDING_CHANGED` | 11 | 版本工作区标脏、SPARQL/可视化失效 |

批量 RDF 导入发布一个批次事件，payload 带批次 ID 和影响统计；不默认为每个实例发送一条事件。

---

## 5. 数据模型

### 5.1 Outbox `ont_event_outbox`

```sql
CREATE TABLE ont_event_outbox (
  id bigint NOT NULL,
  event_id varchar(36) NOT NULL,
  event_type varchar(96) NOT NULL,
  event_version integer NOT NULL DEFAULT 1,
  ontology_id bigint DEFAULT NULL,
  aggregate_type varchar(64) DEFAULT NULL,
  aggregate_id varchar(256) DEFAULT NULL,
  operation varchar(32) DEFAULT NULL,
  occurred_at timestamp NOT NULL,
  payload jsonb NOT NULL,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  trace_id varchar(128) DEFAULT NULL,
  actor_id bigint DEFAULT NULL,
  status varchar(16) NOT NULL DEFAULT 'PENDING',
  available_at timestamp NOT NULL DEFAULT now(),
  lease_until timestamp DEFAULT NULL,
  locked_by varchar(128) DEFAULT NULL,
  delivery_attempt integer NOT NULL DEFAULT 0,
  stream_record_id varchar(64) DEFAULT NULL,
  published_at timestamp DEFAULT NULL,
  last_error_code varchar(64) DEFAULT NULL,
  last_error_message varchar(512) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT uk_ont_event_outbox_event UNIQUE (event_id),
  CONSTRAINT ck_ont_outbox_status
    CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED')),
  CONSTRAINT ck_ont_outbox_del CHECK (del_flag IN ('0', '1'))
);

CREATE INDEX idx_ont_outbox_poll
  ON ont_event_outbox (status, available_at, create_time)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_outbox_lease
  ON ont_event_outbox (lease_until)
  WHERE del_flag = '0' AND status = 'PROCESSING';
CREATE INDEX idx_ont_outbox_aggregate
  ON ont_event_outbox (aggregate_type, aggregate_id, create_time)
  WHERE del_flag = '0';
```

Outbox 行与业务写入在同一事务插入。`published_at` 只表示已确认 XADD 返回，不表示所有消费者已处理。

### 5.2 消费记录 `ont_event_consume_record`

```sql
CREATE TABLE ont_event_consume_record (
  id bigint NOT NULL,
  consumer_group varchar(128) NOT NULL,
  event_id varchar(36) NOT NULL,
  replay_no integer NOT NULL DEFAULT 0,
  event_type varchar(96) NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'PROCESSING',
  lease_until timestamp DEFAULT NULL,
  consumer_name varchar(128) DEFAULT NULL,
  process_attempt integer NOT NULL DEFAULT 1,
  started_at timestamp NOT NULL DEFAULT now(),
  completed_at timestamp DEFAULT NULL,
  last_error_code varchar(64) DEFAULT NULL,
  last_error_message varchar(512) DEFAULT NULL,
  create_by varchar(64) DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT 'system',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT ck_ont_consume_status
    CHECK (status IN ('PROCESSING', 'SUCCEEDED', 'FAILED')),
  CONSTRAINT ck_ont_consume_del CHECK (del_flag IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_ont_consume_idempotency
  ON ont_event_consume_record (consumer_group, event_id, replay_no)
  WHERE del_flag = '0';
CREATE INDEX idx_ont_consume_lease
  ON ont_event_consume_record (status, lease_until)
  WHERE del_flag = '0';
```

### 5.3 死信 `ont_event_dead_letter`

```sql
CREATE TABLE ont_event_dead_letter (
  id bigint NOT NULL,
  consumer_group varchar(128) NOT NULL,
  event_id varchar(36) NOT NULL,
  replay_no integer NOT NULL DEFAULT 0,
  event_type varchar(96) NOT NULL,
  stream_record_id varchar(64) NOT NULL,
  payload jsonb NOT NULL,
  failure_category varchar(32) NOT NULL,
  error_code varchar(64) DEFAULT NULL,
  error_message varchar(512) DEFAULT NULL,
  delivery_count integer NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'OPEN',
  replayed_as_no integer DEFAULT NULL,
  first_failed_at timestamp NOT NULL,
  last_failed_at timestamp NOT NULL,
  resolved_at timestamp DEFAULT NULL,
  create_by varchar(64) DEFAULT 'system',
  create_time timestamp NOT NULL DEFAULT now(),
  update_by varchar(64) DEFAULT 'system',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT ck_ont_dlq_status CHECK (status IN ('OPEN', 'REPLAYED', 'RESOLVED', 'IGNORED')),
  CONSTRAINT ck_ont_dlq_del CHECK (del_flag IN ('0', '1'))
);

CREATE UNIQUE INDEX uk_ont_dlq_event_group
  ON ont_event_dead_letter (consumer_group, event_id, replay_no)
  WHERE del_flag = '0';
```

### 5.4 回放日志 `ont_event_replay_log`

```sql
CREATE TABLE ont_event_replay_log (
  id bigint NOT NULL,
  event_id varchar(36) NOT NULL,
  source_replay_no integer NOT NULL,
  target_replay_no integer NOT NULL,
  target_consumer_group varchar(128) NOT NULL,
  reason varchar(512) NOT NULL,
  approval_request_no varchar(36) DEFAULT NULL,
  replayed_by bigint NOT NULL,
  replayed_at timestamp NOT NULL DEFAULT now(),
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uk_ont_replay_target
    UNIQUE (event_id, target_replay_no, target_consumer_group)
);
```

---

## 6. 核心流程

### 6.1 事件写入

```java
@Transactional(propagation = Propagation.MANDATORY)
public void append(OntologyDomainEvent event) {
    // 校验类型、版本、大小和敏感字段策略
    // 将完整 envelope 写入 ont_event_outbox
}
```

- `MANDATORY` 保证调用方已有业务事务；无事务调用直接失败，避免业务成功而 Outbox 独立提交。
- 业务 Service 在完成数据库写入后、事务提交前 append。
- 不通过 Spring `ApplicationEvent` 作为可靠持久化路径。
- 允许在事务提交后发本地性能提示，但不能替代 Outbox。

### 6.2 Outbox 领取与投递

领取阶段使用自定义 Mapper SQL，而不是 `.last("LIMIT ...")`：

```sql
SELECT id
FROM ont_event_outbox
WHERE del_flag = '0'
  AND (
    (status = 'PENDING' AND available_at <= now())
    OR (status = 'PROCESSING' AND lease_until < now())
  )
ORDER BY create_time
FOR UPDATE SKIP LOCKED
LIMIT :batchSize;
```

同一短事务内把领取行更新为：

- `status=PROCESSING`
- `locked_by=instanceId`
- `lease_until=now()+leaseDuration`
- `delivery_attempt=delivery_attempt+1`

之后逐条 XADD：

- Stream 使用 `StringRedisTemplate`，避免当前 `RedisTemplate<String,Object>` 的 Java 序列化与跨语言不兼容。
- Redis 字段存 `eventId/replayNo/eventType/eventVersion/envelopeJson`。
- XADD 成功后更新 `PUBLISHED + stream_record_id`。
- XADD 成功、数据库更新前宕机会导致重复 XADD；消费者 Inbox 必须去重。
- 投递超过最大次数后 Outbox 标记 FAILED，并产生运维告警；不把“投递失败”混入消费死信。

### 6.3 消费与幂等

```text
收到 Stream 消息
  → 校验 envelope 和支持版本
  → 原子领取 Inbox (group,eventId,replayNo)
      ├─ 已 SUCCEEDED：直接 XACK
      ├─ PROCESSING 且租约有效：不重复并发处理
      └─ 无记录/租约过期：领取并处理
  → Handler 成功
      → Inbox SUCCEEDED
      → XACK
  → Handler 失败
      → Inbox FAILED/记录稳定错误码
      → 不 ACK，等待 reclaim
```

业务 Handler 仍需使用领域幂等键，例如“缓存失效”天然幂等，“发送通知”需避免重复发送。

### 6.4 PEL reclaim 与死信

- 定时扫描各 Group 的 Pending Summary。
- 对空闲超过 `claim-idle` 的消息使用 XAUTOCLAIM（实施时按当前 Spring Data Redis API 或低层命令实现）。
- reclaim 后增加处理次数并重试。
- 只有达到 `consumer-max-attempts`，或错误被分类为 `NON_RETRYABLE`，才写死信。
- 死信落库使用唯一键保证幂等，然后 ACK 原消息。
- Redis 与 PostgreSQL 无分布式事务，任何步骤都按可重复执行设计。
- 长任务不得在 Listener 线程中一直占用；应转为模块自己的 Job，并尽快 ACK“已受理”事件。

### 6.5 回放

回放必须指定：

- `eventId` 或受限时间范围。
- 目标 `consumerGroup`。
- 原因。
- 最大条数。
- 模块 36 审批号（批量或高风险回放）。

流程：

1. 从 Outbox 读取原始已发布事件。
2. 计算该事件面向目标 Group 的下一 `replayNo`。
3. 写 `ont_event_replay_log`。
4. 将相同 eventId、新 replayNo 的 Envelope 重新 XADD。
5. 仅目标 Group 的 Dispatcher 处理该 replay；其他 Group 检测 metadata.targetConsumerGroup 后 ACK 跳过。

禁止：

- 不指定目标 Group 的全局广播回放。
- 无限时间范围回放。
- 修改原始 payload 后仍沿用原 eventId 伪装历史事实。
- 回放设备控制命令。

### 6.6 Stream 保留与清理

- Redis Stream 是传输缓冲，不是永久事件仓库。
- 单 Stream 只保证记录 ID 顺序；多个 Consumer、异步 Handler 和重试会造成处理完成顺序变化。需要顺序的消费者必须使用 `aggregateRevision` 或自身状态版本校验。
- 默认近似 `MAXLEN=100000`，按压测和积压告警调整；不能小于“最大可容忍停机期间事件量”。
- 生产 Redis 必须由运维明确 AOF/RDB、内存淘汰和备份策略。
- Outbox PUBLISHED 默认保留 30 天，FAILED 保留更久；清理由配置化 Job 分批硬删除/归档。
- 回放超过 Outbox 保留期需要外部归档，不承诺仅靠 Redis 恢复。

---

## 7. 事件处理器设计

### 7.1 显式 Handler Registry

```java
public interface OntologyEventHandler {
    String consumerGroup();
    Set<String> supportedEventTypes();
    Set<Integer> supportedVersions(String eventType);
    void handle(OntologyEventEnvelope event);
}
```

`OntologyEventDispatcher` 按 consumer group 创建 Listener Container，并在组内路由到 Handler。首期不设计自定义注解扫描和 BeanPostProcessor。

### 7.2 首期消费者

| Group | 事件 | 行为 |
|---|---|---|
| `ontology-sparql-cache-v1` | Schema/Instance/Version changed | 模块 13 后续缓存失效；当前无共享缓存时可只记录指标 |
| `ontology-visualization-v1` | Schema/Instance changed | 清理后续可视化缓存/通知刷新 |
| `ontology-version-workspace-v1` | Schema changed | 标记当前工作区相对已发布版本已变化 |
| `ontology-security-audit-v1` | Security/Version/Serialization events | 补充异步安全事件审计；敏感访问主审计仍同步落库 |

安全访问审计不能只依赖异步事件，否则 Redis 不可用时会丢失合规记录。

---

## 8. 配置设计

复用现有 `spring.data.redis.*`，不在本模块重复配置 host、password、database。

```yaml
ontology:
  event:
    enabled: true
    stream-key: youming:ontology:domain-events
    max-event-bytes: 65536
    stream-max-length: 100000
    outbox:
      poll-delay: 2s
      batch-size: 100
      lease-duration: 30s
      max-attempts: 10
      published-retention-days: 30
    consumer:
      batch-size: 20
      poll-timeout: 2s
      claim-idle: 60s
      processing-lease: 120s
      max-attempts: 5
    replay:
      max-events-per-request: 1000
```

所有值绑定到 `@ConfigurationProperties`，不得散落硬编码常量。

---

## 9. 接口设计

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/ontology/events/overview` | `ontology_event_view` | Stream/Outbox/PEL/DLQ 概览 |
| GET | `/ontology/events/outbox/page` | `ontology_event_view` | Outbox 分页 |
| POST | `/ontology/events/outbox/{id}/retry` | `ontology_event_admin` | FAILED 投递重试 |
| GET | `/ontology/events/consumers` | `ontology_event_view` | Group、lag、pending |
| GET | `/ontology/events/dead-letters/page` | `ontology_event_view` | 死信分页 |
| POST | `/ontology/events/dead-letters/{id}/replay` | `ontology_event_admin` | 指定 Group 死信回放 |
| POST | `/ontology/events/replays` | `ontology_event_admin` | 受控批量回放 |
| POST | `/ontology/events/dead-letters/{id}/resolve` | `ontology_event_admin` | 人工标记已处理/忽略 |

- 批量回放和强制重试接入模块 36 审批。
- 指标优先通过 Actuator/Micrometer 暴露，不另造一套与监控系统不兼容的 metrics API。
- Controller 不允许客户端指定任意 Redis Key，只能选择服务端白名单 Stream/Group。

---

## 10. 前端设计

```text
web/src/api/ontology/event.ts
web/src/types/ontology/event.ts
web/src/views/ontology/event/index.vue
web/src/views/ontology/event/components/EventOverview.vue
web/src/views/ontology/event/components/OutboxTable.vue
web/src/views/ontology/event/components/ConsumerGroupTable.vue
web/src/views/ontology/event/components/DeadLetterTable.vue
web/src/views/ontology/event/components/EventReplayDialog.vue
```

页面：

- 概览：Outbox PENDING/FAILED、Stream 长度、各 Group lag/PEL、DLQ OPEN。
- Outbox：只显示 payload 摘要，敏感字段受模块 36 控制。
- 死信：错误分类、处理次数、目标 Group、重放状态。
- 回放：必须显示影响条数、目标 Group、原因和审批状态。
- 禁止前端直接执行 Redis 命令或输入任意 Stream Key。

---

## 11. Flyway 设计

迁移文件：

```text
V18__ontology_event_backbone.sql
```

创建：

- `ont_event_outbox`
- `ont_event_consume_record`
- `ont_event_dead_letter`
- `ont_event_replay_log`
- 菜单与权限

菜单使用 901500 段，写全现有列，只给角色 1 授权：

```sql
INSERT INTO sys_menu
(menu_id, name, permission, path, component, parent_id, icon, visible,
 sort_order, keep_alive, embedded, menu_type,
 create_by, create_time, update_by, update_time, del_flag)
VALUES
(901500, '事件中心', NULL, '/ontology/event/index', NULL, 900000,
 'ele-Connection', '1', 15, '0', NULL, '0',
 'admin', now(), 'admin', now(), '0'),
(901501, '事件查看', 'ontology_event_view', NULL, NULL, 901500,
 NULL, '1', 1, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0'),
(901502, '事件管理', 'ontology_event_admin', NULL, NULL, 901500,
 NULL, '1', 2, '0', NULL, '1',
 'admin', now(), 'admin', now(), '0')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
VALUES (1, 901500), (1, 901501), (1, 901502)
ON CONFLICT (role_id, menu_id) DO NOTHING;
```

迁移不创建 Redis Consumer Group；Group 在应用启动时幂等创建，以便不同环境使用配置化 Stream Key。

---

## 12. 现有模块改造

### 12.1 改造原则

- 在业务 Service 的现有事务中追加 Outbox。
- 发布最小事件，不复制整个实体对象。
- 事件发布失败必须使业务事务失败；不能吞异常后提交业务数据。
- 批量操作发布批次事件，避免事件风暴。
- 事件消费者不得反向调用同一写 Service 造成无限事件循环；需要 metadata 标识来源并做业务幂等。

### 12.2 改造清单

| 模块 | 改造点 | 事件 |
|---|---|---|
| 03 | 命名空间新增/修改/删除 | `ONTOLOGY_SCHEMA_CHANGED` |
| 04 | 类型及层次/不相交/等价变更 | `ONTOLOGY_SCHEMA_CHANGED` |
| 05 | 数据属性、标签、枚举变更 | `ONTOLOGY_SCHEMA_CHANGED` |
| 06 | 对象属性、domain/range 变更 | `ONTOLOGY_SCHEMA_CHANGED` |
| 07 | 公理规则状态和目标变更 | `ONTOLOGY_SCHEMA_CHANGED` |
| 08 | 实例/值/关系事务完成 | `ONTOLOGY_INSTANCE_CHANGED` |
| 09 | 校验报告完成 | `ONTOLOGY_VALIDATION_COMPLETED` |
| 10 | 导入/导出完成；批量导入只发批次事件 | `ONTOLOGY_SERIALIZATION_COMPLETED` / `ONTOLOGY_INSTANCE_CHANGED` |
| 11 | 扩展模块和资源绑定变更 | `EXTENSION_BINDING_CHANGED` |
| 14 | 候选、发布、迁移完成 | 对应版本事件 |
| 36 | 认证异常、审计链异常等非敏感摘要 | `ONTOLOGY_SECURITY_EVENT` |

模块 13 首期没有共享 Model 缓存，因此无需为“缓存失效”强制依赖 26；未来增加缓存时再启用对应消费者。

---

## 13. 可观测性

Micrometer 指标：

- `ontology.event.outbox.pending`
- `ontology.event.outbox.failed`
- `ontology.event.publish.total{eventType}`
- `ontology.event.publish.latency`
- `ontology.event.consume.total{group,eventType,outcome}`
- `ontology.event.consume.latency{group,eventType}`
- `ontology.event.pending{group}`
- `ontology.event.dead_letter.open{group}`
- `ontology.event.replay.total{group}`

告警建议：

- Outbox 最老 PENDING 超过 1 分钟。
- Group lag/PEL 持续增长。
- FAILED 或 OPEN DLQ 增量。
- Redis 不可用。
- 不支持事件版本。
- 消费处理 P95 超过 lease 的 50%。

---

## 14. 测试与验收

### 14.1 必测项

- 业务事务回滚时 Outbox 同时回滚。
- 多实例 Relay 使用 SKIP LOCKED 不重复领取。
- XADD 成功后模拟 DB 状态更新失败，重复消息被 Inbox 去重。
- Inbox PROCESSING 租约过期可被另一实例接管。
- 不同逻辑消费者使用不同 Group，均收到事件。
- 同一 Group 多实例仅一实例处理同一 delivery。
- Handler 重试、不可重试错误、最大次数死信。
- 死信落库与 ACK 重复执行幂等。
- 正常重复 replayNo=0 不重复业务副作用。
- 指定 replayNo 的人工回放只到目标 Group。
- 超大 payload、未知 eventVersion、敏感 payload 被拒绝。
- Redis 故障期间业务事务仍可写 Outbox，恢复后补投；Outbox 积压有告警。

### 14.2 验收用例

| 用例 | 预期 |
|---|---|
| 创建实例并提交 | 业务数据与 Outbox 同事务提交，随后投递事件 |
| 创建实例事务回滚 | 无业务数据、无 Outbox 行 |
| Relay 宕机于 XADD 后 | Redis 可能有重复记录，但消费者业务只成功一次 |
| 消费者宕机未 ACK | 消息留在 PEL，超时后被 reclaim |
| 连续处理失败 | 达到上限后进入 OPEN 死信并 ACK 原消息 |
| 管理员重放死信 | 新 replayNo，只投目标 Group，记录原因和审批号 |
| 高频遥测输入 | 不逐点写 Outbox；只产生状态变化/聚合事件 |

---

## 15. 与后续模块的关系

| 模块 | 关系 |
|---|---|
| 19 IoT 接入 | 只发布设备生命周期和状态事件，不发布原始遥测 |
| 21 时序数据 | 时序库负责点位写入；EDA 负责窗口完成、质量异常等事件 |
| 23 告警 | 监听状态/聚合事件，发布告警产生、确认、恢复事件 |
| 24 服务触发 | 命令使用独立安全状态机，不与普通领域事件混用 |
| 25 CEP | 可消费领域事件；高吞吐 CEP 是否继续使用 Redis Streams需专项压测 |
| 36 安全 | 回放/强制重试接审批；事件 payload 先做数据最小化 |
| 37 可观测 | 接入指标、日志和 trace；外部监控不依赖管理页面轮询 |

---

> 最终定位：本模块提供可恢复、可观测、至少一次的领域事件传输骨干。Outbox 解决“业务事务内应产生事件”的可靠记录，Redis Streams 解决跨进程传输，Inbox 和业务幂等解决重复投递；三者缺一不可。它不替代时序平台、命令总线或专业高吞吐消息平台。
