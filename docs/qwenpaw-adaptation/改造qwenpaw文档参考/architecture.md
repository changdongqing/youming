# QwenPaw 接入 pig 授权服务端 + 服务端同步架构方案

> **文档定位**：以高级软件架构师视角，分析 QwenPaw（本地优先个人 AI 助手平台）接入开源授权服务端 [pig](https://gitee.com/log4j/pig)（基于 Spring Authorization Server 的 OAuth2 认证中心），并实现技能、MCP、ApiKey、记忆四大模块以服务端为母版、本地优先执行的同步方案。
>
> **核心原则**：本地是执行主体，服务端是母版与同步源。所有读写优先走本地，断网完全可用，联网后异步同步。

---

## 目录

- [一、现状架构评估](#一现状架构评估)
- [二、pig 服务端概况](#二pig-服务端概况)
- [三、可行性结论](#三可行性结论)
- [四、核心设计原则](#四核心设计原则)
- [五、四大模块详细设计](#五四大模块详细设计)
- [六、授权层对接 pig](#六授权层对接-pig)
- [七、同步层统一设计](#七同步层统一设计)
- [八、数据一致性策略](#八数据一致性策略)
- [九、配置模型](#九配置模型)
- [十、好处总结](#十好处总结)
- [十一、风险与挑战](#十一风险与挑战)
- [十二、实施优先级建议](#十二实施优先级建议)

---

## 一、现状架构评估

### 1.1 QwenPaw 当前架构特征

| 维度 | 现状 |
|------|------|
| **定位** | 本地优先的个人 AI 助手平台（"all data runs on your machine"） |
| **形态** | Python FastAPI 后端 + React/Tauri 桌面壳 + TUI + 10+ 消息渠道 |
| **授权** | 自实现单用户登录（`auth.py`，HMAC-SHA256 token，默认关闭，仅允许 1 个账号） |
| **密钥存储** | OS keychain（keyring）→ 回退 `~/.qwenpaw.secret/.master_key`（Fernet AES-128） |
| **Skill** | 本地文件系统 + manifest JSON（`skill_pool/`、`workspace/skills/`） |
| **MCP** | 驱动配置体系（YAML），凭据引用 credential store |
| **ApiKey** | 每工作区 YAML 文件（`AsyncCredentialStore`），敏感字段加密 |
| **Memory** | SQLite（`history.db` + FTS5）+ 可选 ADBPG 向量库 + ReMe 轻量记忆 + agent.md |
| **pig 对接** | **无**（代码中无任何 pig/OAuth2-Server/SSO 业务模块） |

### 1.2 关键架构优势（对改造有利）

代码的抽象程度**非常好**，这是本次需求可行的基础：

**① 记忆层已有注册表 + 工厂模式**

```python
# src/qwenpaw/agents/memory/base_memory_manager.py:534
memory_registry: Registry[BaseMemoryManager] = Registry()
# 已注册: "adbpg"(远程向量库) / "remelight"(远程ReMe) / "none"
```

> 新增一个 `"pig"` 或 `"server"` backend 只需继承 `BaseMemoryManager` 并注册，**零侵入**。

**② 凭据层已有抽象接口**

```python
# src/qwenpaw/drivers/credentials/store.py — AsyncCredentialStore
# 已是纯接口（get/put/delete/list_refs），内部用 asyncio.to_thread 隔离同步 IO
```

> 可实现一个 `EnterpriseCredentialCache` 适配器，在 `get()` 中增加 fallback 查找，不改现有 YAML 逻辑。

**③ 记忆已有远程后端先例**

ADBPG（AnalyticDB PostgreSQL）和 ReMe 都是**远程存储后端**，说明项目设计之初就预留了"记忆不一定要在本地"的架构空间。

**④ 授权中间件可插拔**

`AuthMiddleware`（`src/qwenpaw/app/auth.py:690`）是标准 Starlette 中间件，`_should_skip_auth` + `_extract_token` + `verify_token` 逻辑清晰，可替换为 OAuth2 introspection / JWT 验签。

**⑤ 技能 manifest 有版本与来源分类**

```python
# src/qwenpaw/agents/skill_system/store.py
classify_pool_skill_source() → ("builtin" | "customized", ...)
manifest 含 schema_version, version, builtin_skill_names
```

> 天然支持扩展 `"enterprise"` 来源，与现有 `"builtin"` / `"customized"` 并存。

**⑥ 记忆写入有幂等去重**

```python
# src/qwenpaw/agents/context/scroll/history.py
# conversation_history 表有 UNIQUE INDEX (session_id, dedup_key)
# ON CONFLICT DO NOTHING
```

> 天然适合同步场景的去重幂等，同一记录多次同步不会重复。

---

## 二、pig 服务端概况

| 维度 | 说明 |
|------|------|
| **技术栈** | Spring Boot 4.0 + Spring Cloud 2025.1.2 + Spring Cloud Alibaba，前端 Vue 3.5 |
| **认证中心** | pig-auth 模块，基于 Spring Authorization Server 落地生产级 OAuth2 |
| **OAuth2 支持** | 授权码、密码、刷新令牌等常见场景 |
| **用户权限** | 内置 RBAC 通用用户权限管理模块 |
| **SSO** | 提供统一认证中心，支持多种登录场景 |
| **数据库** | PostgreSQL（业务表）+ Redis（缓存）+ Nacos（配置与注册中心） |
| **多租户** | 开源版不支持（商业版才有），单用户场景不涉及 |

---

## 三、可行性结论

> **可以实现，且架构契合度高。**

QwenPaw 的代码抽象质量很高——记忆层有注册表工厂、凭据层有接口抽象、授权层有可插拔中间件、技能层有版本与来源分类——这意味着接入 pig 服务端不需要大重构，而是"新增适配器 + 旁路同步"级别的扩展。

最大的好处是**记忆和配置的跨设备统一**，最大的挑战是**记忆实时同步的冲突处理**和 **ApiKey 的安全传输/存储**。

---

## 四、核心设计原则

> **本地是执行主体，服务端是母版与同步源。** 所有读写优先走本地，断网完全可用，联网后异步同步。

```
┌─────────────────────────────────────────────────────────────┐
│                      QwenPaw 本地实例                        │
│                                                             │
│  ┌─────────┐   ┌───────────┐   ┌──────────┐  ┌──────────┐  │
│  │ Skill   │   │ Memory    │   │ ApiKey   │  │ MCP配置   │  │
│  │ 本地执行 │   │ SQLite    │   │ 本地缓存 │  │ 本地YAML  │  │
│  └────┬────┘   └─────┬─────┘   └────┬─────┘  └────┬─────┘  │
│       │              │              │             │         │
│       ▼              ▼              ▼             ▼         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           Sync Layer (同步适配层)                     │   │
│  │  登录拉取 ↓  ↑ 后台异步上行                            │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                         │ OAuth2 Bearer Token               │
└─────────────────────────┼───────────────────────────────────┘
                          │ HTTPS
                    ┌─────┴──────┐
                    │  pig 服务端  │
                    │  (母版存储)  │
                    └────────────┘
```

### 三级可靠性

```
① 本地 SQLite/YAML  —— 执行时唯一可信源（本地优先）
② 服务端存储         —— 母版与跨设备同步源
③ 本地同步队列       —— 断网缓冲，联网补传
```

---

## 五、四大模块详细设计

### 5.1 技能：分层合并

**策略：** 服务端企业技能 + 本地个人技能共存，前端统一展示。企业技能只读，个人技能可编辑。类似 Git 的 upstream + local 分支。

```
本地技能目录结构（复用现有）:
  ~/.qwenpaw/
    skill_pool/              ← 企业技能（服务端拉取，只读）
      manifest.json          ← source: "enterprise", 版本号
      pdf/                   ← 技能文件
      browser/
    workspace/skills/        ← 个人技能（本地可编辑，可选上传）
      manifest.json          ← source: "personal"
      my-custom-skill/

技能加载 = enterprise(只读) ∪ personal(可编辑)
同名时: personal 优先（允许覆盖企业默认）
```

**同步策略：**

| 事件 | 动作 |
|------|------|
| **登录时** | 对比本地 manifest `version` 与服务端 `version`，增量拉取变更的企业技能（zip 包下载到 `skill_pool/`） |
| **个人技能** | 纯本地，用户主动点"上传"才推送到服务端（作为个人云备份） |
| **离线** | 完全使用本地技能，无任何网络依赖 |

**改造点：** 新增 `skill_system/sync.py`，复用现有 `store.py` 的 `read_skill_pool_manifest()` / `compute_skill_md_hash()` 做差异比对，不改现有执行路径。manifest 的 `source` 字段扩展 `"enterprise"` 值，与现有 `"builtin"` / `"customized"` 并存。

### 5.2 记忆：登录拉取 + 后台异步上行

**策略：** 登录时拉取服务端记忆全量/增量到本地 SQLite，之后所有读写走本地，后台异步队列增量推送到服务端。断网完全可用。

```
时间轴:
  登录 ────────────────────────────────────── 工作中 ──── 持续
   │                                            │          │
   ▼                                            ▼          ▼
 服务端记忆 ──增量──► SQLite          Agent读写SQLite   异步队列
 (按updated_at拉取)    (本地母版)      (零网络延迟)     推送到服务端
```

**关键设计：**

| 环节 | 机制 |
|------|------|
| **登录拉取** | 携带本地 `last_sync_seq`，服务端返回 `seq > last_sync` 的增量记录，插入本地 SQLite（复用现有 `dedup_key` 去重） |
| **本地工作** | `HistoryStore.append()` 完全不变，零侵入。SQLite 是唯一读写对象 |
| **异步上行** | 在 `append()` 后挂一个 `asyncio.create_task()`，将记录推入服务端队列。失败重试 3 次后放弃，下次登录全量对账修复 |
| **冲突处理** | 记忆是**追加型数据**（conversation_history），几乎无写冲突。同一 `dedup_key` 服务端用 `ON CONFLICT DO NOTHING`，天然幂等 |
| **断网** | 本地正常工作，上行队列堆积，联网后逐条补传 |

**改造点：** 新增 `memory/pig_sync_manager.py`，在 `HistoryStore.append()` 末尾增加一个可选 hook（通过配置开关），不改变现有写入逻辑。`BaseMemoryManager` 的 `start()` / `close()` 天然对应"建立同步连接" / "flush 上行队列"。

### 5.3 ApiKey/凭据：企业凭据 + 个人凭据共存

**策略：** 服务端存企业统一 API Key（只读拉取），本地可添加个人 Key。使用时优先个人 Key，回退企业 Key。

```
凭据解析优先级（复用现有 AsyncCredentialStore 接口）:
  1. personal (本地YAML, 现有逻辑不变)
  2. enterprise (服务端拉取, 本地加密缓存)
  3. env: 环境变量 (现有逻辑不变)

AsyncCredentialStore.get(ref) 改造:
  ① 先查本地 personal → 命中则返回
  ② 再查 enterprise 缓存 → 命中则返回
  ③ env: → 环境变量
```

**同步策略：**

| 事件 | 动作 |
|------|------|
| **登录时** | 拉取企业凭据列表，用本地 master_key 加密后缓存到 `~/.qwenpaw.secret/enterprise_credentials.yaml`（复用现有 Fernet 加密） |
| **日常使用** | 读本地缓存，不请求服务端 |
| **定期同步** | 每 N 分钟检查服务端凭据变更（或登录时全量拉取） |
| **个人凭据** | 纯本地，不上传服务端（安全隔离） |

**改造点：** `AsyncCredentialStore` 已是纯接口，新增 `EnterpriseCredentialCache` 层，`get()` 方法增加 fallback 查找。现有个人凭据 YAML 逻辑完全不变。

### 5.4 MCP 配置：服务端模板 + 本地实例

**策略：** 服务端管理企业 MCP 模板（只读，含接入地址、默认策略），本地可基于模板创建实例并个性化修改。

```
MCP驱动配置:
  服务端: 企业MCP模板（只读，含接入地址、默认策略）
  本地: 实例配置（可基于模板创建，可个性化修改）

登录时: 拉取企业MCP模板 → 本地缓存
使用时: 读本地配置（可能基于模板个性化过）
```

**改造点：** 复用 `DriverConfigService`，新增 `sync_enterprise_drivers()` 在登录时拉取模板，本地配置增加 `source_template` 字段标记来源。

---

## 六、授权层对接 pig

**OAuth2 授权码模式登录流程：**

```
  ┌────────┐    1.跳转授权页     ┌──────────┐
  │ QwenPaw│───────────────────►│ pig-auth │
  │ 前端   │◄───────────────────│ SSO登录页 │
  └───┬────┘    2.回调带code      └──────────┘
      │
      │ 3. code换token
      ▼
  ┌──────────┐  4.access_token   ┌──────────┐
  │ QwenPaw  │◄─────────────────►│ pig-auth │
  │ 后端     │  5.introspect/JWT  └──────────┘
  └──────────┘  验签
```

**改造方案（保留本地 auth 作为离线 fallback）：**

```python
# auth.py 改造思路（伪代码）
def verify_token(token: str) -> Optional[str]:
    if is_oauth2_enabled():
        # pig 模式: JWT 本地验签（拉公钥缓存）或 introspect
        return verify_pig_jwt(token)
    else:
        # 现有逻辑: 本地 HMAC 验签（离线/未对接pig时）
        return _verify_local_hmac_token(token)
```

配置驱动，`QWENPAW_AUTH_MODE=local|pig`，两种模式共存。`pig` 模式下：

- 前端登录跳转 pig 授权码 URL，回调后拿 access_token 存入 localStorage，后续请求带 `Bearer` 头（现有逻辑已支持）
- `AuthMiddleware` 的 `verify_token()` 内部从"本地 HMAC 验签"改为"调用 pig 的 `/oauth2/introspect` 或本地验证 JWT 签名（pig 的公钥，定期刷新缓存）"
- 单用户特性与 pig 不冲突：pig 管用户身份，QwenPaw 仍是单实例单用户使用

---

## 七、同步层统一设计

新增一个统一的同步层模块，避免四个模块各写一套：

```
src/qwenpaw/sync/
  ├── __init__.py
  ├── client.py          # pig HTTP 客户端（Bearer token, 重试, 退避）
  ├── auth_adapter.py    # OAuth2 token 管理（刷新, 缓存）
  ├── skill_sync.py      # 技能增量拉取
  ├── memory_sync.py     # 记忆异步上行 + 登录拉取
  ├── credential_sync.py # 企业凭据拉取缓存
  └── mcp_sync.py        # MCP模板拉取
```

**生命周期挂钩：**

| 事件 | 触发动作 |
|------|---------|
| **登录成功** | 并发拉取：技能增量 + 记忆增量 + 企业凭据 + MCP 模板 |
| **工作中** | 记忆异步上行（后台队列） |
| **退出/关闭** | flush 记忆上行队列 |
| **定时** | 每 30 分钟检查技能/凭据变更 |
| **断网恢复** | 补传上行队列 + 检查服务端变更 |

---

## 八、数据一致性策略

### 8.1 冲突场景分析

| 数据类型 | 冲突可能性 | 策略 |
|---------|-----------|------|
| **记忆**（追加型） | 极低 | `dedup_key` 幂等，`ON CONFLICT DO NOTHING` |
| **技能**（文件型） | 低 | 企业技能只读无冲突；个人技能本地为准 |
| **凭据**（键值型） | 中 | 企业凭据服务端为准（只读）；个人凭据本地为准 |
| **MCP 配置** | 中 | 本地实例配置为准，模板更新时提示用户 |

### 8.2 对账机制

每次登录时执行一次**全量对账**：

- 本地有、服务端无的记忆 → 补传服务端（上次断网未传完的）
- 服务端有、本地无的记忆 → 拉取到本地
- 以 `seq` + `updated_at` 为增量游标

---

## 九、配置模型

在现有配置体系（`src/qwenpaw/config/config.py`）中新增：

```yaml
# ~/.qwenpaw/config.yaml 新增段
sync:
  enabled: true                    # 是否启用服务端同步
  auth_mode: pig                   # local | pig
  pig:
    base_url: https://pig.example.com
    client_id: qwenpaw
    client_secret: ENC:xxx         # 复用现有Fernet加密
    token_endpoint: /oauth2/token
    introspect_endpoint: /oauth2/introspect
  memory:
    sync_enabled: true             # 记忆异步上行
    batch_size: 50                 # 每批上行条数
    retry_times: 3
  skill:
    auto_pull_enterprise: true     # 登录自动拉取企业技能
  credential:
    cache_enterprise: true         # 缓存企业凭据到本地
    sync_interval_minutes: 30
```

`enabled: false` 时完全退化为现有纯本地模式，**零影响**。

---

## 十、好处总结

| # | 好处 | 机制 |
|---|------|------|
| **1** | **断网完全可用** | 本地 SQLite/YAML 是执行主体，同步层只是后台旁路 |
| **2** | **零延迟体验** | 所有读写走本地，无网络往返 |
| **3** | **跨设备记忆连续** | 登录拉取母版，本地逐步积累，异步上行 |
| **4** | **企业技能统一分发** | 只读拉取到 `skill_pool/`，个人技能独立于 `workspace/skills/` |
| **5** | **凭据安全隔离** | 企业 Key 服务端管 + 本地加密缓存；个人 Key 纯本地不上传 |
| **6** | **渐进式部署** | `sync.enabled` 开关控制，可先开记忆同步、后开技能同步 |
| **7** | **冲突极少** | 记忆追加型 + dedup_key 幂等；技能/凭据各管各的来源 |
| **8** | **对现有代码侵入极小** | 四大模块均有抽象层，同步逻辑是"挂载旁路"而非"替换主干" |
| **9** | **多端一致性** | 桌面端、TUI、手机、消息渠道共享同一份配置/记忆/技能 |
| **10** | **数据安全与备份** | ApiKey、记忆等服务端集中加密存储，设备丢失/损坏不丢数据 |
| **11** | **企业级身份治理** | pig 提供 RBAC + OAuth2 + SSO，可统一对接企业现有身份体系 |
| **12** | **审计与合规** | 服务端可记录 API Key 使用、技能调用、记忆访问，满足企业审计需求 |

---

## 十一、风险与挑战

| 风险 | 严重度 | 应对 |
|------|--------|------|
| **本地优先理念冲突** | ⚠️ 中 | 必须保留离线降级能力，服务端不可达时用本地缓存；`sync.enabled` 可关闭 |
| **记忆实时同步的冲突** | ⚠️ 中 | 多设备并发写入需 Last-Write-Wins 或 CRDT 策略，`dedup_key` 字段已存在可复用 |
| **ApiKey 安全传输** | 🔴 高 | 服务端存储密钥需独立加密层（pig 侧），传输走 TLS，考虑用 pig 的密钥管理而非明文 |
| **Skill 文件同步体积** | ⚠️ 中 | 技能包含 zip 包/多文件，需增量同步 + 对象存储，不宜走数据库 BLOB |
| **pig 开源版无多租户** | ⚠️ 低 | 单用户场景不涉及；若未来多用户需 pig 商业版 |
| **网络延迟影响体验** | ⚠️ 中 | 记忆写入必须异步（`asyncio.create_task`），不阻塞 Agent 主循环 |
| **token 验证性能** | ⚠️ 低 | pig JWT 可本地验签（拉公钥缓存），不必每次 introspect |

---

## 十二、实施优先级建议

```
阶段1 → 授权对接 pig OAuth2（基础前置）
         └─ AuthMiddleware 支持 JWT 验签
         └─ 前端登录跳转
         └─ sync/client.py + auth_adapter.py

阶段2 → 记忆同步（价值最高）
         └─ 登录增量拉取
         └─ append() 后台异步上行
         └─ 断网缓冲 + 补传

阶段3 → 企业凭据拉取（安全价值高）
         └─ 登录拉取 + 本地加密缓存
         └─ credential store fallback 查找

阶段4 → 企业技能分发
         └─ manifest 版本比对 + 增量拉取
         └─ skill_pool 只读保护

阶段5 → MCP模板同步（可选）
```

每个阶段独立可交付，`sync.enabled` 开关确保任何阶段未完成时不影响现有功能。

---

## 附：关键代码位置参考

| 模块 | 文件 | 关键符号 |
|------|------|---------|
| 授权 | `src/qwenpaw/app/auth.py` | `AuthMiddleware`, `verify_token()`, `create_token()` |
| 密钥加密 | `src/qwenpaw/security/secret_store.py` | `encrypt()`, `decrypt()`, `_get_master_key()` |
| 记忆抽象 | `src/qwenpaw/agents/memory/base_memory_manager.py` | `BaseMemoryManager`, `memory_registry` |
| 记忆存储 | `src/qwenpaw/agents/context/scroll/history.py` | `HistoryStore`, `append()`, `dedup_key` |
| 记忆远程后端 | `src/qwenpaw/agents/memory/adbpg_memory_manager.py` | `ADBPGMemoryManager` (远程后端先例) |
| 凭据存储 | `src/qwenpaw/drivers/credentials/store.py` | `AsyncCredentialStore` (纯接口) |
| 技能存储 | `src/qwenpaw/agents/skill_system/store.py` | `read_skill_pool_manifest()`, `classify_pool_skill_source()` |
| MCP 配置 | `src/qwenpaw/app/mcp/config_service.py` | `DriverConfigService` |
| 配置体系 | `src/qwenpaw/config/config.py` | 各 Config 模型 |

---

*文档创建日期：2026-07-30*
*分析基于 QwenPaw 当前分支：mingwanwu*
